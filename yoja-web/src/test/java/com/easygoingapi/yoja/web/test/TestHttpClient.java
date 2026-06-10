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
package com.easygoingapi.yoja.web.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import com.easygoingapi.yoja.core.http.HttpMethod;
import com.easygoingapi.yoja.http.server.WebApp;
import com.easygoingapi.yoja.http.server.WebService;
import com.easygoingapi.yoja.selenium.Browser;
import com.easygoingapi.yoja.selenium.ScriptOption;
import com.easygoingapi.yoja.web.test.util.TestConfig;

public class TestHttpClient {

    @TestFactory
    public Stream<DynamicNode> testHttpClient_01() {
        final AtomicReference<Browser> browser = new AtomicReference<>();
        
        final List<String> expectedContentTypes = 
            List.of("text/plain", 
                    "application/blob", 
                    "application/json", 
                    "application/array-json", 
                    "application/blob", 
                    "text/plain", 
                    "text/plain", 
                    "text/plain", 
                    "uint8");
        final List<String> actualContentTypes = new ArrayList<>();
        
        final List<String> expectedBodies =
            List.of("hello", 
                    "data:application/json;base64,eyJrZXlfMDEiOiJ2YWx1ZV8wMSIsImtleV8wMiI6InZhbHVlXzAyIn0=", 
                    "{\"key_01\":\"value_01\",\"key_02\":\"value_02\"}", 
                    "[\"value_01\",\"value_02\"]", 
                    "data:application/octet-stream;base64,eyJrZXlfMDEiOiJ2YWx1ZV8wMSIsImtleV8wMiI6InZhbHVlXzAyIn0=", 
                    "20100", 
                    "20100", 
                    "1975-08-19T23:15:30.000Z", 
                    "{\"0\":0,\"1\":65,\"2\":0,\"3\":0,\"4\":0,\"5\":0,\"6\":0,\"7\":0}");
        final List<String> actualBodies = new ArrayList<>();
        
        final WebApp webApp = WebApp.jar("com.easygoingapi.yoja.web.test.http.client");
        final WebService webService_file_1 = new WebService(HttpMethod.GET, "/file_1.txt", h -> {
            if (h.request().hasHeader("mode") 
                    && "blob".equals(h.request().header("mode"))) {
                final StringBuilder response = new StringBuilder();
                response.append("data:text/plain;base64,");
                response.append(Base64.getEncoder()
                                      .encodeToString(h.loadResource(webApp, "/file_1.txt")));
                h.response().send(response.toString());
            }
            else {
                h.nextHandler();
            }
        });
        
        final WebService webService_file_1_bi = new WebService(HttpMethod.GET, "/file_1.bi", h -> {
            h.response().send(h.loadResource(webApp, "/file_1.bi"));
        });
        
        final WebService webService_01 = new WebService(HttpMethod.POST, "/post_01", h -> {
            final String contentType = h.request().header("content-type") != null 
                                           ? h.request().header("content-type") 
                                           : "null";
            actualContentTypes.add(contentType);
            actualBodies.add(h.request().bodyAsText());
            if (!"null".equals(contentType)) {
                h.response().putHeader("content-type", contentType);
            }
            if ("application/json".equals(contentType)) {
                h.response().send(h.request().bodyAsJsonObject());
            }
            else if ("application/array-json".equals(contentType)) {
                h.response().send(h.request().bodyAsJsonArray());
            }
            else {
                h.response().send(h.request().bodyAsText());
            }
        });
        
        return TestConfig.initialize()
                         .startYojaWeb(ScriptOption.apply().loadYwAssert().saveLogs())
                         .webService(webService_01)
                         .webService(webService_file_1)
                         .webService(webService_file_1_bi)
                         .webResource("com.easygoingapi.yoja.web.test.http.client")
                         .test("check browser", c -> {
                             if (!c.browser().equals(browser.get())) {
                                 actualContentTypes.clear();
                                 actualBodies.clear();
                                 browser.set(c.browser());
                             }
                         })
                         .testModule("/TestHttClientService_GET_01.js")
                         .testModule("/TestHttClientService_GET_02.js")
                         .testModule("/TestHttClientService_GET_03.js")
                         .testModule("/TestHttClientService_GET_04.js")
                         .testModule("/TestHttClientService_GET_05.js")
                         .testModule("/TestHttClientService_GET_06.js")
                         .testModule("/TestHttClientService_GET_07.js")
                         .testModule("/TestHttClientService_GET_09.js")
                         .testModule("/TestHttClientService_GET_13.js")
                         .testModule("/TestHttClientService_POST_01.js")
                         .testModule("/TestHttClientService_POST_02.js")
                         .testModule("/TestHttClientService_POST_03.js")
                         .testModule("/TestHttClientService_POST_04.js")
                         .testModule("/TestHttClientService_POST_05.js")
                         .testModule("/TestHttClientService_POST_06.js")
                         .testModule("/TestHttClientService_POST_07.js")
                         .testModule("/TestHttClientService_POST_08.js")
                         .testModule("/TestHttClientService_POST_09.js")
                         .testModule("/TestHttClientService_LOAD_01.js")
                         .testModule("/TestHttClientService_FETCH_01.js")
                         .testModule("/TestHttClientService_FETCH_02.js")
                         .test("testRequestBody", c -> assertEquals(expectedBodies, actualBodies))
                         .test("contentType", c -> assertEquals(expectedContentTypes, actualContentTypes))
                         .stream();
    }
    
    @TestFactory
    public Stream<DynamicNode> testHttpClient_02() {
            final WebApp webApp = WebApp.jar("com.easygoingapi.yoja.web.test.http.client");
            final WebService webService_file_1 = new WebService(HttpMethod.GET, "/file_1.txt", h -> {
                if (h.request().hasHeader("mode")) {
                    if ("base64".equals(h.request().header("mode"))) {
                        h.response().putHeader("content-type", "application/base64");
                    }
                    else if ("base64_bis".equals(h.request().header("mode"))) {
                        h.response().putHeader("content-type", "application/base64_bis");
                    }
                    else if ("base64_only".equals(h.request().header("mode"))) {
                        h.response().putHeader("content-type", "base64");
                    }
                    final StringBuilder response = new StringBuilder();
                    response.append("data:text/plain;base64,");
                    response.append(Base64.getEncoder()
                                          .encodeToString(h.loadResource(webApp, "/file_1.txt")));
                   
                    h.response().send(response.toString());
                }
                else {
                    h.nextHandler();
                }
            });
            
            return TestConfig.initialize()
                             .startYojaWeb(ScriptOption.apply().loadYwAssert())
                             .webService(webService_file_1)
                             .webResource("com.easygoingapi.yoja.web.test.http.client")
                             .testModule("/TestHttClientService_GET_08.js")
                             .testModule("/TestHttClientService_GET_10.js")
                             .testModule("/TestHttClientService_GET_11.js")
                             .testModule("/TestHttClientService_GET_12.js")
                             .stream();
        }
    
}
