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

import static com.easygoingapi.yoja.core.util.FutureUtil.await;
import static com.easygoingapi.yoja.core.util.FutureUtil.awaitValue;
import static com.easygoingapi.yoja.reverse.proxy.util.TestUtil.newHttpClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.easygoingapi.yoja.core.http.HttpEncoding.Format;
import com.easygoingapi.yoja.core.http.HttpMethod;
import com.easygoingapi.yoja.core.http.HttpProtocole;
import com.easygoingapi.yoja.core.http.HttpUrl;
import com.easygoingapi.yoja.http.client.HttpClient;
import com.easygoingapi.yoja.http.client.HttpGet;
import com.easygoingapi.yoja.http.client.HttpResponse;
import com.easygoingapi.yoja.http.server.HttpRouter;
import com.easygoingapi.yoja.http.server.HttpRouting;
import com.easygoingapi.yoja.http.server.HttpServer;
import com.easygoingapi.yoja.http.server.HttpServer.State;
import com.easygoingapi.yoja.http.server.WebService;
import com.easygoingapi.yoja.reverse.proxy.ReverseProxyRule.Url;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerOptions;

public class TestReverseProxyBuilder {

    @Test
    public void test_elseRule_fallback_resolver() {
        final int backendPort = 9991;
        final int proxyPort = 9992;
        final Handler<HttpRouting> backendHandler = v -> v.response().send("fallback ok");
        final HttpRouter backendRouter = HttpRouter.builder()
                                                   .webService(new WebService(HttpMethod.GET, "/fallback", backendHandler))
                                                   .build();
        final HttpServer backend = awaitValue(HttpServer.builder(backendRouter, backendPort).start());

        final AtomicReference<HttpUrl> resolved = new AtomicReference<>();
        final Function<HttpUrl, HttpUrl> resolver = inbound -> {
            resolved.set(inbound);
            return HttpUrl.builder("localhost")
                          .protocol(HttpProtocole.http)
                          .port(backendPort)
                          .path("/fallback")
                          .build();
        };

        final ReverseProxyServer proxy = awaitValue(ReverseProxyServer.builder(proxyPort)
                                                                      .rules(Set.of())
                                                                      .elseRule(resolver)
                                                                      .start());
        try {
            final HttpClient client = newHttpClient(false, proxyPort, "localhost");
            // no rule matches "/anything" -> the else resolver kicks in
            final HttpResponse response = awaitValue(client.send(HttpGet.of("/anything")));

            assertNotNull(resolved.get(), "the fallback resolver should have been invoked");
            assertEquals(200, response.statusCode());
            assertEquals("fallback ok", response.bodyAsText());
        }
        finally {
            await(proxy.stop());
            await(backend.stop());
            assertTrue(backend.is(State.stopped));
        }
    }

    @Test
    public void test_elseRule_preserves_query_when_resolver_copies_it() {
        final int backendPort = 9996;
        final int proxyPort = 9997;
        // Backend echoes the "name" query parameter.
        final Handler<HttpRouting> echo = v -> v.response().send("hello " + v.request().firstParameter("name"));
        final HttpRouter backendRouter = HttpRouter.builder()
                                                   .webService(new WebService(HttpMethod.GET, "/echo", echo))
                                                   .build();
        final HttpServer backend = awaitValue(HttpServer.builder(backendRouter, backendPort).start());

        // The resolver receives the inbound URL (with its query) and must copy
        // it into the target — the framework does not merge it automatically.
        final Function<HttpUrl, HttpUrl> resolver = inbound -> HttpUrl.builder("localhost")
                                                                      .protocol(HttpProtocole.http)
                                                                      .port(backendPort)
                                                                      .path("/echo")
                                                                      .parameterQuery(inbound.parameterQuery(Format.decoded))
                                                                      .build();
        final ReverseProxyServer proxy = awaitValue(ReverseProxyServer.builder(proxyPort)
                                                                      .rules(Set.of())
                                                                      .elseRule(resolver)
                                                                      .start());
        try {
            final HttpClient client = newHttpClient(false, proxyPort, "localhost");
            final HttpResponse response = awaitValue(client.send(HttpGet.of("/anything", "name=Zoe")));

            assertEquals(200, response.statusCode());
            assertEquals("hello Zoe", response.bodyAsText(), "the resolver-copied query must reach the backend");
        }
        finally {
            await(proxy.stop());
            await(backend.stop());
            assertTrue(backend.is(State.stopped));
        }
    }

    @Test
    public void test_startAdmin_throws_without_admin_port() {
        final ReverseProxyServer proxy = awaitValue(ReverseProxyServer.builder(9993)
                                                                      .rules(Set.of())
                                                                      .start());
        try {
            assertThrows(ReverseProxyException.class, () -> proxy.startAdmin());
        }
        finally {
            await(proxy.stop());
        }
    }

    @Test
    public void test_rule_duplicate_from_throws() {
        final ReverseProxyRule ruleA = new ReverseProxyRule(Url.from("localhost", "/dup"),
                                                            Url.to(false, "localhost").port(1111).build());
        final ReverseProxyRule ruleB = new ReverseProxyRule(Url.from("localhost", "/dup"),
                                                            Url.to(false, "localhost").port(2222).build());

        assertThrows(ReverseProxyException.class,
                     () -> ReverseProxyServer.builder(9994).rule(ruleA).rule(ruleB));
    }

    @Test
    public void test_custom_options_and_toString() {
        final HttpServerOptions options = new HttpServerOptions().setIdleTimeout(30);
        final ReverseProxyServer proxy = awaitValue(ReverseProxyServer.builder(9995)
                                                                      .options(options)
                                                                      .rules(Set.of())
                                                                      .start());
        try {
            assertEquals(State.started, proxy.proxyState());
            assertNotNull(proxy.toString());
            assertTrue(proxy.toString().contains("9995"), "toString should mention the proxy port");
        }
        finally {
            await(proxy.stop());
            assertEquals(State.stopped, proxy.proxyState());
        }
    }

}
