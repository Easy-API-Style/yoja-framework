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

public class TestStorageService {

    @TestFactory
    public Stream<DynamicNode> testStorageService_01() {
        return TestConfig.initialize()
                         .startYojaWeb()
                         .webResource("com.easygoingapi.yoja.web.test.storage")
                         .reload(ScriptOption.apply().loadYwAssert())
                         .testAsyncModule("/TestStorage_01.js")
                         .reload(ScriptOption.apply().loadYwAssert())
                         .testAsyncModule("/TestStorage_02.js")
                         .reload(ScriptOption.apply().loadYwAssert())
                         .testAsyncModule("/TestStorage_03.js")
                         .reload(ScriptOption.apply().loadYwAssert())
                         .testAsyncModule("/TestStorage_04.js")
                         .reload(ScriptOption.apply().loadYwAssert())
                         .testAsyncModule("/TestStorage_05.js")
                         .reload(ScriptOption.apply().loadYwAssert())
                         .testAsyncModule("/TestStorage_06.js")
                         .reload(ScriptOption.apply().loadYwAssert())
                         .testAsyncModule("/TestStorage_07.js")
                         .reload(ScriptOption.apply().loadYwAssert())
                         .testAsyncModule("/TestStorage_08.js")
                         .reload(ScriptOption.apply().loadYwAssert())
                         .testAsyncModule("/TestStorage_09.js")
                         .reload(ScriptOption.apply().loadYwAssert())
                         .testAsyncModule("/TestStorage_10.js")
                         .reload(ScriptOption.apply().loadYwAssert())
                         .testAsyncModule("/TestStorage_11.js")
                         .reload(ScriptOption.apply().loadYwAssert())
                         .testAsyncModule("/TestStorage_12.js")
                         .reload(ScriptOption.apply().loadYwAssert())
                         .testAsyncModule("/TestStorage_13.js")
                         .reload(ScriptOption.apply().loadYwAssert())
                         .testAsyncModule("/TestStorage_14.js")
                         .reload(ScriptOption.apply().loadYwAssert())
                         .testAsyncModule("/TestStorage_15.js")
                         .reload(ScriptOption.apply().loadYwAssert())
                         .testAsyncModule("/TestStorage_16.js")
                         .reload(ScriptOption.apply().loadYwAssert())
                         .testAsyncModule("/TestStorage_17.js")
                         .reload(ScriptOption.apply().loadYwAssert())
                         .testAsyncModule("/TestStorage_18.js")
                         .reload(ScriptOption.apply().loadYwAssert())
                         .testAsyncModule("/TestStorage_19.js")
                         .reload(ScriptOption.apply().loadYwAssert())
                         .testAsyncModule("/TestStorage_20.js")
                         .reload(ScriptOption.apply().loadYwAssert())
                         .testAsyncModule("/TestStorage_21.js")
                         .reload(ScriptOption.apply().loadYwAssert())
                         .testAsyncModule("/TestStorage_22.js")
                         .stream();
    }
    
    @TestFactory
    public Stream<DynamicNode> testStorageService_02() {
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.storage")
                         .startYojaWeb()    
                         .loadYwAssert()
                         .testJsUnit("/TestStorage_23.js", 
                                     List.of("test_01", 
                                             "test_02", 
                                             "test_03",
                                             "test_04",
                                             "test_05",
                                             "test_06",
                                             "test_07",
                                             "test_08",
                                             "test_09",
                                             "test_10",
                                             "test_11",
                                             "test_12",
                                             "test_13",
                                             "test_14",
                                             "test_15"))
                        .stream();
    }
    
}
