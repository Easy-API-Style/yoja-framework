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

import java.time.Duration;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import com.easygoingapi.yoja.selenium.Browser;
import com.easygoingapi.yoja.selenium.ScriptOption;
import com.easygoingapi.yoja.selenium.TestBuilder;
import com.easygoingapi.yoja.web.test.util.TestConfig;

@Disabled
public class TestResponsiveService {

     public static TestBuilder initialize() {
            return TestBuilder.builder()
                              .contentType("css", "text/css")
                              .contentType("html", "text/html")
                              .contentType("json", "application/json")
                              .contentType("js", "application/javascript")
                              .contentType("txt", "text/plain")

                              .webResource("com.easygoingapi.yoja.web", "/yoja")
                              
//                              .browser(Browser.builder(Browser.FIREFOX)
//                                              .mode(Browser.Mode.DEBUGGER)
//                                              .build())                          
                              .browser(Browser.builder(Browser.CHROME)
                                              .mode(Browser.Mode.HEADLESS)
                                              .build())
//                              .browser(Browser.builder(Browser.EDGE)
//                                              .mode(Browser.Mode.HEADLESS)
//                                              .build())
                              ;
        }
    
    
    @TestFactory
    public Stream<DynamicNode> testResponsiveService_01() {
        return initialize()
                         .webResource("com.easygoingapi.yoja.web.test.responsive")
                         .startYojaWeb(ScriptOption.apply().loadYwAssert())
                         .testModule("/TestResponsiveService_01.js")
                         .resizeWindow(428, 926)
                         .await(Duration.ofSeconds(10))
                         .loadModule("/TestResponsiveService_02_a.js")
                         .await(Duration.ofSeconds(10))
                         .resizeWindow(1920, 1080)
                         .await(Duration.ofSeconds(10))
                         .resizeWindow(428, 926)
                         .await(Duration.ofSeconds(10))
                         .repeatTestModuleUntil(Duration.ofSeconds(10), 
                                                "/TestResponsiveService_02_b.js")  
                         .stream();
    }
    
    @TestFactory
    public Stream<DynamicNode> testResponsiveService_02() {
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.responsive")
                         .startYojaWeb(ScriptOption.apply().loadYwAssert())
                         .testModule("/TestResponsiveService_03.js")
                         .stream();
    }
    
}
