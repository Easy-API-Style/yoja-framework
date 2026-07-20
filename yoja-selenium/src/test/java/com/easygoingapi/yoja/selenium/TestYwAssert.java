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

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

public class TestYwAssert {

    @TestFactory
    Stream<DynamicNode> jsUnitDemo() {
        return YojaSeleniumBuilder.builder()
                          .browser(Browser.builder(Browser.CHROME)
                                          .mode(Browser.Mode.HEADLESS)
                                          .build())
                          .browser(Browser.builder(Browser.FIREFOX)
                                          .mode(Browser.Mode.HEADLESS)
                                          .build())
                          .contentType("js", "text/javascript")
                          .webResource("com/easygoingapi/yoja/selenium/js")
                          .startJavascript()
                          .loadYwAssert()
                          .testJsUnit("/jsUnitAssertTest.js",
                                      List.of("test_assertEquals_primitives",
                                              "test_assertEquals_deepStructures",
                                              "test_assertEquals_keyOrderIndependent",
                                              "test_assertEquals_specialValues",
                                              "test_assertEquals_failsOnDifference",
                                              "test_assertEquals_boxedPrimitives",
                                              "test_assertTrue",
                                              "test_assertFalse",
                                              "test_assertNull",
                                              "test_assertNotNull",
                                              "test_assertUndefined",
                                              "test_assertNotUndefined",
                                              "test_assertArrayEquals",
                                              "test_assertArrayEquals_deepElements",
                                              "test_assertArrayEquals_failsOnDifference"))
                          .stream();
    }
    
}
