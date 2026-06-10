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
package com.easygoingapi.yoja.selenium;

import static com.easygoingapi.yoja.core.util.FutureUtil.awaitValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.core.http.HttpMethod;
import com.easygoingapi.yoja.core.http.HttpProtocole;
import com.easygoingapi.yoja.core.http.HttpUrl;
import com.easygoingapi.yoja.core.http.HttpEncoding.Format;
import com.easygoingapi.yoja.http.server.HttpRouter;
import com.easygoingapi.yoja.http.server.HttpRouting;
import com.easygoingapi.yoja.http.server.HttpServer;
import com.easygoingapi.yoja.http.server.WebService;
import com.easygoingapi.yoja.selenium.Browser;
import com.easygoingapi.yoja.selenium.SeleniumService;
import com.easygoingapi.yoja.selenium.Browser.Mode;

import io.vertx.core.Future;
import io.vertx.core.Handler;

public class TestSelenium {
    
    private static Logger LOGGER = LoggerFactory.getLogger(TestSelenium.class);

    public static String ChromeVersion = "134.0.6998.35";
    
    @Test
    public void test_01() {
        final Browser.Config config = 
            Browser.builder(Browser.FIREFOX)
                   .mode(Mode.HEADLESS)
                   .build();
        final SeleniumService seleniumService = SeleniumService.newInstance(config);   
        final HttpServer httpServer = startHttpServer();
           
        try {
            final HttpUrl httpUrl = HttpUrl.builder("google.com")
                                           .path("/")
                                           .protocol(HttpProtocole.https)
                                           .build();
            seleniumService.webDriver().get(httpUrl.url(Format.encoded));
            assertEquals("Google", seleniumService.webDriver().getTitle());
        }
        finally {
            httpServer.stop();
        }
    }

    @Test
    public void test_02() {
        final Browser.Config config = 
            Browser.builder(Browser.CHROME)
                   .mode(Mode.HEADLESS)
                   .build();
        final SeleniumService seleniumService = SeleniumService.newInstance(config);   
        final HttpServer httpServer = startHttpServer();
           
        try {
            final HttpUrl httpUrl = HttpUrl.builder("google.com")
                                           .path("/")
                                           .protocol(HttpProtocole.https)
                                           .build();
            seleniumService.webDriver().get(httpUrl.url(Format.encoded));
            assertEquals("Google", seleniumService.webDriver().getTitle());
        }
        finally {
            httpServer.stop();
        }
    }
    
    public static HttpServer startHttpServer() {
        final Handler<HttpRouting> httpRouting = v -> {
            v.response().send("Hello");
        };
        
        final WebService webService = new WebService(HttpMethod.GET, "/webService", httpRouting);
        final HttpRouter httpRouter = HttpRouter.builder()
                                                .webService(webService)
                                                .build();
        final HttpServer.Builder httpServerBuilder = HttpServer.builder(httpRouter, 8888);
        final Future<HttpServer> future =
                httpServerBuilder.start()
                                 .onFailure(e -> LOGGER.error("httpServer failed", e));
        return awaitValue(future);
    }
    
}
