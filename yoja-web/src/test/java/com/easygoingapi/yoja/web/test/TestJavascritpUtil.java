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

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import com.easygoingapi.yoja.selenium.ScriptOption;
import com.easygoingapi.yoja.web.test.util.TestConfig;

public class TestJavascritpUtil {

    @TestFactory
    public Stream<DynamicNode> testJavascritpUtil_01() {
        return TestConfig.initialize()
                         .startYojaWeb(ScriptOption.apply().loadYwAssert())
                         .webResource("com.easygoingapi.yoja.web.test.javascript.util")
                         .testJsUnit("/test_javascriptUtil_01.js", 
                                     List.of("test_01",
                                             "test_02",
                                             "test_03",
                                             "test_04"))
                         .stream();
    }
    
}
