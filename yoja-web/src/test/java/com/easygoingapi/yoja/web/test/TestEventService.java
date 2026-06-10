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

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import com.easygoingapi.yoja.selenium.ScriptOption;
import com.easygoingapi.yoja.web.test.util.TestConfig;

public class TestEventService {

    @TestFactory
    public Stream<DynamicNode> testEventService_01() {
        return TestConfig.initialize()
                         .startYojaWeb(ScriptOption.apply().loadYwAssert())
                         .webResource("com.easygoingapi.yoja.web.test.event")
                         .testJsUnit("/TestEventService_01.js", 
                                     List.of("test_01",
                                             "test_02",
                                             "test_03",
                                             "test_04",
                                             "test_05"))
                         .stream();
    }
    
    @TestFactory
    public Stream<DynamicNode> testEventService_02() {
        return TestConfig.initialize()
                         .startYojaWeb(ScriptOption.apply().loadYwAssert())
                         .webResource("com.easygoingapi.yoja.web.test.event")
                         .testJsUnit("/TestEventService_02.js", 
                                     List.of("test_01",
                                             "test_02",
                                             "test_03",
                                             "test_04",
                                             "test_05"))
                         .stream();
    }
    
    @TestFactory
    public Stream<DynamicNode> testEventService_03() {
        return TestConfig.initialize()
                         .startYojaWeb(ScriptOption.apply().loadYwAssert())
                         .webResource("com.easygoingapi.yoja.web.test.event")
                         .loadModule("/TestEventService_03.js")
                         .test("trigger test_01", c -> {
                            final List<String> expected = List.of("test_01_action_01",
                                                                  "test_01_action_02", 
                                                                  "test_01_action_03");
                            final List<String> actual = c.seleniumService().executeAsyncScript("""
                                 callBack = arguments[arguments.length - 1]
                                 const result = []
                                 window.eventTest_03 = (l, p) => {
                                     result.push(l)
                                     if (p !== 'hello test_01') {
                                          callBack([])
                                     }
                                     if (result.length === 3) {
                                          callBack(result)
                                     }
                                 }
                                 yojaWebApi.eventService.trigger('test_01', 'hello test_01')
                            """);
                            assertEquals(expected, actual);
                         })
                         .test("trigger test_02", c -> {
                            final List<String> expected = List.of("test_02_action_01",
                                                                  "test_02_action_02");
                            final List<String> actual = c.seleniumService().executeAsyncScript("""
                                 callBack = arguments[arguments.length - 1]
                                 const result = []
                                 window.eventTest_03 = (l, p) => {
                                     result.push(l)
                                     if (p !== 'hello test_02') {
                                          callBack([])
                                     }
                                     if (result.length === 2) {
                                          callBack(result)
                                     }
                                 }
                                 yojaWebApi.eventService.trigger('test_02', 'hello test_02')
                            """);
                            assertEquals(expected, actual);
                         })
                         .test("trigger test_", c -> {
                            final List<String> expected = List.of("test_01_action_01",
                                                                  "test_01_action_02", 
                                                                  "test_01_action_03",
                                                                  "test_02_action_01",
                                                                  "test_02_action_02");
                            final List<String> actual = c.seleniumService().executeAsyncScript("""
                                 callBack = arguments[arguments.length - 1]
                                 const result = []
                                 window.eventTest_03 = (l, p) => {
                                     result.push(l)
                                     if (p !== 'hello test_') {
                                          callBack([])
                                     }
                                     if (result.length === 5) {
                                          callBack(result)
                                     }
                                 }
                                 yojaWebApi.eventService.trigger({startsWith:'test_'}, 'hello test_')
                            """);
                            assertEquals(expected, actual);
                         })
                         .test("trigger AAA", c -> {
                            final List<String> expected = List.of("AAA_action_01");
                            final List<String> actual = c.seleniumService().executeAsyncScript("""
                                 callBack = arguments[arguments.length - 1]
                                 const result = []
                                 window.eventTest_03 = (l, p) => {
                                     result.push(l)
                                     if (p !== 'hello AAA') {
                                          callBack([])
                                     }
                                     if (result.length === 1) {
                                          callBack(result)
                                     }
                                 }
                                 yojaWebApi.eventService.trigger({contains:'A'}, 'hello AAA')
                            """);
                            assertEquals(expected, actual);
                         })
                         .test("trigger test_ pause test_01", c -> {
                            final List<String> expected = List.of("test_02_action_01",
                                                                  "test_02_action_02");
                            final List<String> actual = c.seleniumService().executeAsyncScript("""
                                 yojaWebApi.eventService.pause('test_01')
                                 callBack = arguments[arguments.length - 1]
                                 const result = []
                                 window.eventTest_03 = (l, p) => {
                                     result.push(l)
                                     if (p !== 'hello test_') {
                                          callBack([])
                                     }
                                     if (result.length === 2) {
                                          callBack(result)
                                     }
                                 }
                                 yojaWebApi.eventService.trigger({startsWith:'test_'}, 'hello test_')
                            """);
                            assertEquals(expected, actual);
                         })
                         .test("trigger test_ remove action", c -> {
                            final List<String> expected = List.of("test_02_action_01");
                            final List<String> actual = c.seleniumService().executeAsyncScript("""
                                 yojaWebApi.eventService.event('test_02').removeAction(2)
                                 callBack = arguments[arguments.length - 1]
                                 const result = []
                                 window.eventTest_03 = (l, p) => {
                                     result.push(l)
                                     if (p !== 'hello test_') {
                                          callBack([])
                                     }
                                     if (result.length === 1) {
                                          callBack(result)
                                     }
                                 }
                                 yojaWebApi.eventService.trigger({startsWith:'test_'}, 'hello test_')
                            """);
                            assertEquals(expected, actual);
                         })
                         .stream();
    }

}
