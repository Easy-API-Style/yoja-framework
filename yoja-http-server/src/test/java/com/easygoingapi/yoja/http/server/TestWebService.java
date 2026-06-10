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
package com.easygoingapi.yoja.http.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.easygoingapi.yoja.core.http.HttpMethod;
import com.easygoingapi.yoja.http.server.WebApp.Type;

import io.vertx.core.Handler;

public class TestWebService {

    @Test
    public void test_01() {
        Exception exception = null;
        try {
            final Handler<HttpRouting> httpRouting_1 = v -> {
                v.response().send();
            };
            final WebService webService_1 = new WebService(HttpMethod.POST, 
                                                           "webService_1", 
                                                           httpRouting_1);
        }
        catch (Exception e) {
            exception = e;
        }
        assertEquals("WebService path must begin with '/'", exception.getMessage());
    }

    @Test
    public void test_02() {
        Exception exception = null;
        try {
            final WebApp webApp = WebApp.builder(Type.jar, "com.easygoingapi.yoja.http.server.test.web.resource")
                                        .build();
            final WebResource webResource_1 = new WebResource(webApp, "*");
        }
        catch (Exception e) {
            exception = e;
        }
        assertEquals("WebResource path must begin with '/'", exception.getMessage());
    }
    
    @Test
    public void test_03() {
        Exception exception = null;
        try {
            final WebApp webApp = WebApp.builder(Type.jar, "com.easygoingapi.yoja.http.server.test.web.resource")
                                        .build();
            final WebResource webResource_1 = new WebResource(webApp, "path/");
        }
        catch (Exception e) {
            exception = e;
        }
        assertEquals("WebResource path must begin with '/'", exception.getMessage());
    }
    
}
