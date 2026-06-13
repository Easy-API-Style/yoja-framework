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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import com.easygoingapi.yoja.selenium.ScriptOption;
import com.easygoingapi.yoja.selenium.TestBuilder;
import com.easygoingapi.yoja.web.test.util.TestConfig;

public class TestUrlParameterService {

    public static TestBuilder test_01 =    
        TestConfig.initialize()
                  .webResource("com.easygoingapi.yoja.web.test.url.parameter")
                  .test("load page 1", c -> c.getHttpPage(Duration.ofSeconds(2), 
                                                          c.httpUrlBuilder().path("/page_1.html").build()))
                  .reload(ScriptOption.apply().loadYwAssert())
                  .testAsyncModule("/TestUrlParameterService_01.js")
                  .reload(ScriptOption.apply().loadYwAssert())
                  .testAsyncModule("/TestUrlParameterService_02.js")
                  .reload(ScriptOption.apply().loadYwAssert())
                  .testAsyncModule("/TestUrlParameterService_03.js")
                  .reload(ScriptOption.apply().loadYwAssert())
                  .testAsyncModule("/TestUrlParameterService_04.js")
                  .reload(ScriptOption.apply().loadYwAssert())
                  .testAsyncModule("/TestUrlParameterService_05.js")
                  .reload(ScriptOption.apply().loadYwAssert())
                  .testAsyncModule("/TestUrlParameterService_06.js")
                  .reload(ScriptOption.apply().loadYwAssert())
                  .testAsyncModule("/TestUrlParameterService_07.js")
                  .reload(ScriptOption.apply().loadYwAssert())
                  .testAsyncModule("/TestUrlParameterService_08.js")
                  .reload(ScriptOption.apply().loadYwAssert())
                  .testAsyncModule("/TestUrlParameterService_09.js")
                  .reload(ScriptOption.apply().loadYwAssert())
                  .testAsyncModule("/TestUrlParameterService_10.js")
                  
// chrome failed but they are checked with factory_05
//                  .reload(ScriptOption.apply().loadYwAssert())
//                  .testAsyncModule("/TestUrlParameterService_11.js")
//                  .reload(ScriptOption.apply().loadYwAssert())
//                  .testAsyncModule("/TestUrlParameterService_12.js")
                  
                  .reload(ScriptOption.apply().loadYwAssert())
                  .testAsyncModule("/TestUrlParameterService_15.js")
                  .reload(ScriptOption.apply().loadYwAssert())
                  .testAsyncModule("/TestUrlParameterService_16.js")
    ;
    
    @TestFactory
    public Stream<DynamicNode> testUrlParameterService_01() {
        return test_01.stream();
    }
    
    @TestFactory
    public Stream<DynamicNode> testUrlParameterService_02() {
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.url.parameter")
                         .test("load page 1", c -> c.getHttpPage(Duration.ofSeconds(2),
                                                                 c.httpUrlBuilder()
                                                                  .path("/page_1.html")
                                                                  .build()))
                         .loadYwAssert()
                         .testAsyncModule("/TestUrlParameterService_14.js")
                         .stream();
    }

    @TestFactory
    public Stream<DynamicNode> testUrlParameterService_03() {
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.url.parameter")
                         .test("load page 1", c -> c.getHttpPage(Duration.ofSeconds(2),
                                                                 c.httpUrlBuilder()
                                                                  .path("/page_1.html")
                                                                  .build()))
                         .loadYwAssert()
                         .testAsyncModule("/TestUrlParameterService_13.js")
                         .stream();
    }
    
    final static List<String> expected = new ArrayList<>();
    static {
        expected.add("event={event=append, key=key_1, value=value_1}");
        expected.add("state={data=null, hash=null, url=/page_3.html, urlParameter={}}");
        expected.add("event={event=before-push}");
        expected.add("state={data=null, hash=null, url=/page_3.html, urlParameter={}}");
        expected.add("event={event=after-push}");
        expected.add("state={data={key=key_1, value=value_1}, hash=null, url=/page_3.html?key_1=value_1, urlParameter={0={key=key_1, value=value_1}}}");
        expected.add("event={event=append, key=key_1, value=value_2}");
        expected.add("state={data={key=key_1, value=value_1}, hash=null, url=/page_3.html?key_1=value_1, urlParameter={0={key=key_1, value=value_1}}}");
        expected.add("event={event=before-push}");
        expected.add("state={data={key=key_1, value=value_1}, hash=null, url=/page_3.html?key_1=value_1, urlParameter={0={key=key_1, value=value_1}}}");
        expected.add("event={event=after-push}");
        expected.add("state={data={key=key_1, value=value_2}, hash=null, url=/page_3.html?key_1=value_1&key_1=value_2, urlParameter={0={key=key_1, value=value_1}, 1={key=key_1, value=value_2}}}");
        expected.add("event={event=append, key=key_1, value=value_3}");
        expected.add("state={data={key=key_1, value=value_2}, hash=null, url=/page_3.html?key_1=value_1&key_1=value_2, urlParameter={0={key=key_1, value=value_1}, 1={key=key_1, value=value_2}}}");
        expected.add("event={event=before-replace}");
        expected.add("state={data={key=key_1, value=value_2}, hash=null, url=/page_3.html?key_1=value_1&key_1=value_2, urlParameter={0={key=key_1, value=value_1}, 1={key=key_1, value=value_2}}}");
        expected.add("event={event=after-replace}");
        expected.add("state={data={key=key_1, value=value_3}, hash=null, url=/page_3.html?key_1=value_1&key_1=value_2&key_1=value_3, urlParameter={0={key=key_1, value=value_1}, 1={key=key_1, value=value_2}, 2={key=key_1, value=value_3}}}");
        expected.add("event={event=append, key=key_1, value=value_4}");
        expected.add("state={data={key=key_1, value=value_3}, hash=null, url=/page_3.html?key_1=value_1&key_1=value_2&key_1=value_3, urlParameter={0={key=key_1, value=value_1}, 1={key=key_1, value=value_2}, 2={key=key_1, value=value_3}}}");
        expected.add("event={event=before-push}");
        expected.add("state={data={key=key_1, value=value_3}, hash=null, url=/page_3.html?key_1=value_1&key_1=value_2&key_1=value_3, urlParameter={0={key=key_1, value=value_1}, 1={key=key_1, value=value_2}, 2={key=key_1, value=value_3}}}");
        expected.add("event={event=after-push}");
        expected.add("state={data={key=key_1, value=value_4}, hash=null, url=/page_3.html?key_1=value_1&key_1=value_2&key_1=value_3&key_1=value_4, urlParameter={0={key=key_1, value=value_1}, 1={key=key_1, value=value_2}, 2={key=key_1, value=value_3}, 3={key=key_1, value=value_4}}}");
        expected.add("event={event=pop}");
        expected.add("state={data={key=key_1, value=value_3}, hash=null, url=/page_3.html?key_1=value_1&key_1=value_2&key_1=value_3, urlParameter={0={key=key_1, value=value_1}, 1={key=key_1, value=value_2}, 2={key=key_1, value=value_3}}}");
        expected.add("event={event=pop}");
        expected.add("state={data={key=key_1, value=value_1}, hash=null, url=/page_3.html?key_1=value_1, urlParameter={0={key=key_1, value=value_1}}}");
        expected.add("event={event=pop}");
        expected.add("state={data={key=key_1, value=value_3}, hash=null, url=/page_3.html?key_1=value_1&key_1=value_2&key_1=value_3, urlParameter={0={key=key_1, value=value_1}, 1={key=key_1, value=value_2}, 2={key=key_1, value=value_3}}}");
    }
    
    @TestFactory
    public Stream<DynamicNode> testUrlParameterService_04() {
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.url.parameter")
                         .test("load page 1", c -> c.getHttpPage(Duration.ofSeconds(2),
                                                                    c.httpUrlBuilder()
                                                                     .path("/page_3.html")
                                                                     .build()))
                         .loadYwAssert()
                         .loadModule("/TestUrlParameterService_17.js")
                         .test("append", c -> {
                             final WebDriver webDriver = c.seleniumService().webDriver();
                             webDriver.findElement(By.id("append_11")).click();
                             webDriver.findElement(By.id("append_12")).click();
                             webDriver.findElement(By.id("append_13")).click();
                             webDriver.findElement(By.id("append_14")).click();
                             webDriver.navigate().back();
                             webDriver.navigate().back();
                             webDriver.navigate().forward();
                         })
                         .test("check append history", c -> {
                             final List<Map<String, Object>> result = c.seleniumService().executeScript("return window.yojaWebTest");
                             assertEquals(15, result.size());
                             final List<String> actual = new ArrayList<>();
                             for (final Map<String, Object> map : result) {
                                 for (final Entry<String, Object> entry : map.entrySet()) {
//                                     System.out.println("expected.add(\"" + entry.toString() + "\");");
                                     actual.add(entry.toString());
                                 }
                             }
                             assertEquals(expected, actual);
                         })
                         .stream();
    }
    
    @TestFactory
    public Stream<DynamicNode> testUrlParameterService_05() {
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.url.parameter")
                         .test("load page 1", c -> c.getHttpPage(Duration.ofSeconds(2),
                                                                 c.httpUrlBuilder()
                                                                  .path("/page_3.html")
                                                                  .build()))
                         .loadYwAssert()
                         .loadModule("/TestUrlParameterService_17.js")
                         .test("append", c -> {
                             final WebDriver webDriver = c.seleniumService().webDriver();
                             webDriver.findElement(By.id("append_11")).click();
                             webDriver.findElement(By.id("append_12")).click();
                             webDriver.findElement(By.id("append_13")).click();
                             webDriver.findElement(By.id("append_14")).click();
                             webDriver.findElement(By.id("back")).click();
                             webDriver.findElement(By.id("back")).click();
                             webDriver.findElement(By.id("forward")).click();
                         })
                         .test("check append history", c -> {
                             final List<Map<String, Object>> result = c.seleniumService().executeScript("return window.yojaWebTest");
                             assertEquals(15, result.size());
                             final List<String> actual = new ArrayList<>();
                             for (final Map<String, Object> map : result) {
                                 for (final Entry<String, Object> entry : map.entrySet()) {
//                                     System.out.println("expected.add(\"" + entry.toString() + "\");");
                                     actual.add(entry.toString());
                                 }
                             }
                             assertEquals(expected, actual);
                         })
                         .stream();
    }
    
}
