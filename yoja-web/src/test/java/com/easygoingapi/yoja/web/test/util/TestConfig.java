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
package com.easygoingapi.yoja.web.test.util;

import com.easygoingapi.yoja.selenium.Browser;
import com.easygoingapi.yoja.selenium.TestBuilder;

public class TestConfig {

    public static String host = "localhost";
    
    public static TestBuilder initialize() {
        return TestBuilder.builder()
                          .host(host)
                          .contentType("css", "text/css")
                          .contentType("html", "text/html")
                          .contentType("json", "application/json")
                          .contentType("js", "application/javascript")
                          .contentType("txt", "text/plain")

                          .webResource("com.easygoingapi.yoja.web", "/yoja")
                          
                          .browser(Browser.builder(Browser.FIREFOX)
                                          .mode(Browser.Mode.HEADLESS)
                                          .build())                          
                          .browser(Browser.builder(Browser.CHROME)
                                          .mode(Browser.Mode.HEADLESS)
                                          .build())
//                          .browser(Browser.builder(Browser.EDGE)
//                                          .mode(Browser.Mode.HEADLESS)
//                                          .build())
                          ;
    }

}
