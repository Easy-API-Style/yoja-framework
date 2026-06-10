/*
 * Copyright 2026 easy api <easy.api.contact@gmail.com>
 * https://easygoingapi.com
 * https://github.com/Easy-API-Style/yoja-framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.easygoingapi.yoja.reverse.proxy;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.core.YojaApp;
import com.easygoingapi.yoja.core.http.HttpUrl;
import com.easygoingapi.yoja.core.http.HttpEncoding.Format;
import com.easygoingapi.yoja.core.worker.Worker;
import com.easygoingapi.yoja.http.client.HttpEngine;
import com.easygoingapi.yoja.http.client.WebSocketEngine;
import com.google.common.base.Throwables;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * {@link ReverseProxySwitcher} extension that adds HTTP dispatching on top of
 * the WebSocket dispatching inherited from the parent.
 * <p>
 * Exposes a Vert.x {@link Router} via {@link #proxyRouter()} that:
 * <ol>
 *   <li>buffers the inbound body (Vert.x' {@link BodyHandler});</li>
 *   <li>resolves the inbound URL through the inherited switcher logic;</li>
 *   <li>fires a Vert.x {@code WebClient} request to the resolved upstream,
 *       copying every inbound header verbatim;</li>
 *   <li>once the upstream responds, copies the status code, headers and body
 *       back to the original client, and decrements the upstream port's
 *       usage counter.</li>
 * </ol>
 * Unresolved requests yield {@code 404} unless the switcher is in silent mode
 * (in which case they are dropped). Any thrown exception ends the response
 * with {@code 500} and a textual stack trace.
 */
public class ReverseProxySwitcherWebClient extends ReverseProxySwitcher {

	private final static Logger LOGGER = LoggerFactory.getLogger(ReverseProxySwitcherWebClient.class);

	/** HTTP engine used to dial the upstream. */
	private final HttpEngine proxyHttpEngine;

	/**
	 * Constructs a new web-client-based reverse proxy switcher.
	 *
	 * @param adminToken         initial admin bearer token
	 * @param silent             when {@code true}, unresolved requests are silently dropped
	 * @param proxyPort          port the proxy listens on
	 * @param proxyHttpEngine    HTTP engine used for upstream HTTP calls
	 * @param webSocketEngine    WebSocket engine used for upstream WS calls
	 * @param reverseProxyRules  initial rule set
	 * @param resolver           optional fallback resolver
	 * @param onResolveActions   resolution listeners
	 */
	protected ReverseProxySwitcherWebClient(final String adminToken,
	                                        final boolean silent,
			                                final int proxyPort,
			                                final HttpEngine proxyHttpEngine,
			                                final WebSocketEngine webSocketEngine,
			                                final Set<ReverseProxyRule> reverseProxyRules,
			                                final Function<HttpUrl, HttpUrl> resolver,
			                                final List<Handler<ReverseProxyResult>> onResolveActions) {
		super(adminToken, silent, proxyPort,
		      webSocketEngine,
		      reverseProxyRules, resolver, onResolveActions);
		this.proxyHttpEngine = proxyHttpEngine;
	}

	/**
	 * Builds the Vert.x router used as the request handler of the proxy HTTP
	 * server. See the class-level Javadoc for the exact dispatch sequence.
	 *
	 * @return a router whose unique route forwards every request to the
	 *         resolved upstream
	 */
	public Router proxyRouter() {
		final Router proxyRouter = Router.router(YojaApp.vertx());
		proxyRouter.route().handler(BodyHandler.create()).blockingHandler(routingContext -> {
			try {
				final Buffer buffer = routingContext.body().buffer();
				final HttpServerRequest httpServerRequest = routingContext.request();
				final ReverseProxyResult reverseProxyResult = resolve(httpServerRequest);
				if (reverseProxyResult.isResolved()) {
					final HttpUrl httpUrl = reverseProxyResult.toUrl();
					final HttpRequest<Buffer> httpRequest =
					   proxyHttpEngine.webClient()
					                  .request(httpServerRequest.method(),
								               httpUrl.port(),
								               httpUrl.host(),
								               httpUrl.pathAndQuery(Format.encoded));
					httpRequest.putHeaders(httpServerRequest.headers());
					usePort(httpUrl.port());
					final Future<HttpResponse<Buffer>> httpResponse;
					if (buffer != null) {
						httpResponse = httpRequest.sendBuffer(buffer);
					}
					else {
						httpResponse = httpRequest.sendBuffer(Buffer.buffer());
					}
					httpResponse.onComplete(h -> {
						Worker.parallelThread.execute(() -> {
							try {
								releasePort(httpUrl.port());
								if (h.failed()) {
									serverError(routingContext, h.cause());
								}
								else {
									final HttpResponse<Buffer> response = h.result();
									final HttpServerResponse httpServerResponse = routingContext.response();
									httpServerResponse.headers().addAll(response.headers());
									httpServerResponse.setStatusCode(response.statusCode());
									final Buffer body = response.body();
									if (body != null) {
										httpServerResponse.send(body);
									}
									else {
										httpServerResponse.send();
									}
								}
							}
							catch (final Exception e) {
								serverError(routingContext, e);
							}
						});
					});
				}
				else if (!isSilent()) {
				    StatusCode.notFound(routingContext);
				}
			}
			catch (final Exception e) {
				serverError(routingContext, e);
			}
		}, false);
		return proxyRouter;
	}

	/**
	 * Ends the response with {@code 500} and a textual stack trace.
	 *
	 * @param routingContext routing context to respond on
	 * @param throwable      cause whose stack trace becomes the body
	 */
	private static void serverError(final RoutingContext routingContext,
			                        final Throwable throwable) {
		routingContext.response().setStatusCode(500);
		routingContext.response().putHeader("Content-Type", "text/plain");
		routingContext.response().send(Throwables.getStackTraceAsString(throwable));
	}

	/**
	 * Closes the HTTP engine before delegating to the parent (which closes the
	 * WebSocket engine).
	 *
	 * @return a future completing when both engines have closed
	 */
	@Override
	protected Future<Void> close() {
	    this.proxyHttpEngine.close();
	    return super.close();
	}

}
