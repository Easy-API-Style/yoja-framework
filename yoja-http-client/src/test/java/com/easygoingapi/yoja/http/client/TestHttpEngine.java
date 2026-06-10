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
package com.easygoingapi.yoja.http.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.easygoingapi.yoja.core.YojaApp;
import com.easygoingapi.yoja.core.YojaAppException;
import com.easygoingapi.yoja.http.client.HttpEngine;

import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.client.WebClientOptions;

public class TestHttpEngine {

    @Test
    public void test_01() {
        try (final HttpEngine httpEngine = new HttpEngine()) {
            YojaApp.start();
        }
        catch (final Exception e) {
            assertEquals(YojaAppException.class, e.getClass());
            assertEquals("yoja app already started", e.getMessage());
        }
        finally {
            YojaApp.stop();
            YojaApp.awaitStop();
        }
    }

    @Test
    public void test_02() {
        try (final HttpEngine httpEngine = new HttpEngine()) {
            assertEquals("localhost", httpEngine.options().getDefaultHost());
            assertEquals(80, httpEngine.options().getDefaultPort());
        }
        finally {
            YojaApp.stop();
            YojaApp.awaitStop();
        }
    }
    
    @Test
    public void test_03() {
       
        try (final HttpEngine httpEngine = new HttpEngine(new WebClientOptions()
                                                                .setSsl(false)
                                                                .setProtocolVersion(HttpVersion.HTTP_1_1))) {
            httpEngine.logOptions();
            assertEquals(false, httpEngine.options().isSsl());
            assertEquals(HttpVersion.HTTP_1_1, httpEngine.options().getProtocolVersion());
        }
        finally {
            YojaApp.stop();
            YojaApp.awaitStop();
        }
    }
    
}
