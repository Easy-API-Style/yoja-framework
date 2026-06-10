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
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import com.easygoingapi.yoja.selenium.TestContext;
import com.easygoingapi.yoja.web.test.util.TestConfig;

public class TestInclude {
    
    static List<String> expected_01 = new ArrayList<>();
    static {
        expected_01.add("HTML");
        expected_01.add("HEAD");
        expected_01.add("TITLE");
        expected_01.add("META");
        expected_01.add("META");
        expected_01.add("META");
        expected_01.add("SCRIPT");
        expected_01.add("SCRIPT");
        expected_01.add("BODY");
        expected_01.add("LABEL include 1");
        expected_01.add("LABEL include 2");
        expected_01.add("LABEL include 4");
        expected_01.add("LABEL 4 done");
        expected_01.add("LABEL include 5");
        expected_01.add("LABEL 5 done");
        expected_01.add("LABEL include 3");
        expected_01.add("LABEL include 6");
        expected_01.add("LABEL 6 done");
        expected_01.add("LABEL include 7");
        expected_01.add("LABEL include 2");
        expected_01.add("LABEL include 4");
        expected_01.add("LABEL 4 done");
        expected_01.add("LABEL include 5");
        expected_01.add("LABEL 5 done");
        expected_01.add("LABEL include 8");
        expected_01.add("LABEL 8 done");
        
    }
    
    static List<String> expected_02 = new ArrayList<>();
    static {
        expected_02.add("HTML");
        expected_02.add("HEAD");
        expected_02.add("TITLE");
        expected_02.add("META");
        expected_02.add("META");
        expected_02.add("META");
        expected_02.add("SCRIPT");
        expected_02.add("SCRIPT");
        expected_02.add("BODY");
        expected_02.add("LABEL include 1");
        expected_02.add("LABEL include 2");
        expected_02.add("LABEL include 4");
        expected_02.add("INCLUDE");
        expected_02.add("LABEL 4 done");
        expected_02.add("LABEL include 5");
        expected_02.add("LABEL 5 done");
        expected_02.add("LABEL include 3");
        expected_02.add("LABEL include 6");
        expected_02.add("INCLUDE");
        expected_02.add("LABEL 6 done");
        expected_02.add("LABEL include 7");
        expected_02.add("LABEL include 2");
        expected_02.add("LABEL include 4");
        expected_02.add("INCLUDE");
        expected_02.add("LABEL 4 done");
        expected_02.add("LABEL include 5");
        expected_02.add("LABEL 5 done");
        expected_02.add("LABEL include 8");
        expected_02.add("LABEL 8 done");
    }
    
    static List<String> expected_03 = new ArrayList<>();
    static {
        expected_03.add("HTML");
        expected_03.add("HEAD");
        expected_03.add("TITLE");
        expected_03.add("META");
        expected_03.add("META");
        expected_03.add("META");
        expected_03.add("SCRIPT");
        expected_03.add("SCRIPT");
        expected_03.add("BODY");
        expected_03.add("LABEL include 1");
        expected_03.add("LABEL include 2");
        expected_03.add("LABEL include 4");
        expected_03.add("INCLUDE");
        expected_03.add("LABEL 4 done");
        expected_03.add("LABEL include 5");
        expected_03.add("LABEL 5 done");
        expected_03.add("LABEL include 3");
        expected_03.add("LABEL include 6");
        expected_03.add("INCLUDE");
        expected_03.add("LABEL 6 done");
        expected_03.add("LABEL include 7");
        expected_03.add("LABEL include 2");
        expected_03.add("LABEL include 4");
        expected_03.add("INCLUDE");
        expected_03.add("LABEL 4 done");
        expected_03.add("LABEL include 5");
        expected_03.add("LABEL 5 done");
        expected_03.add("LABEL include 8");
        expected_03.add("LABEL include 1 !!!");
        expected_03.add("LABEL include 2");
        expected_03.add("LABEL include 4");
        expected_03.add("INCLUDE");
        expected_03.add("LABEL 4 done");
        expected_03.add("LABEL include 5");
        expected_03.add("LABEL 5 done");
        expected_03.add("LABEL include 3");
        expected_03.add("LABEL include 6");
        expected_03.add("INCLUDE");
        expected_03.add("LABEL 6 done");
    }
    
    static List<String> expected_04 = new ArrayList<>();
    static {
    	expected_04.add("HTML");
    	expected_04.add("HEAD");
    	expected_04.add("TITLE");
    	expected_04.add("META");
    	expected_04.add("META");
    	expected_04.add("META");
    	expected_04.add("SCRIPT");
    	expected_04.add("SCRIPT");
    	expected_04.add("BODY");
    	expected_04.add("LABEL include 1");
    	expected_04.add("LABEL include 2");
    	expected_04.add("LABEL include 3");
    	expected_04.add("LABEL include 2");
    	expected_04.add("LABEL include 3");
    	expected_04.add("INCLUDE");
    }

    public static Consumer<TestContext> test_walkTag(List<String> expected_02) {
        return testContext -> {
            final List<String> actual = testContext.seleniumService()
                                                   .executeScript(""" 
                const result = []
                yojaWeb.walkTag(t => { 
                      let value = t.tagName
                      if (value === 'LABEL') {
                          value = value + ' ' + t.innerHTML
                      }
                      result.push(value)
                })
                return result
            """);
            for (String v : actual) {
                 System.out.println("expected_02.add(\"" + v + "\");");
            }
            assertEquals(expected_02, actual);
        };
    }

    @TestFactory
    public Stream<DynamicNode> testInclude_01() {
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.include_01")
                         
                         .test("load page", c -> c.getHttpPage("/home.html"))
                         .test("test_walkTag", test_walkTag(expected_01))

                         .stream();
    }

    @TestFactory
    public Stream<DynamicNode> testInclude_02() {
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.include_02")
                         
                         .test("load page", c -> c.getHttpPage("/home.html"))
                         .test("test_walkTag", test_walkTag(expected_02))
                         
                         .stream();
    }

    @TestFactory
    public Stream<DynamicNode> testInclude_03() {
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.include_03")
                         
                         .test("load page", c -> c.getHttpPage("/home.html"))
                         .test("test_walkTag", test_walkTag(expected_03))
                         
                         .stream();
    }
    
    @TestFactory
    public Stream<DynamicNode> testInclude_04() {
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.include_04")
                         
                         .test("load page", c -> c.getHttpPage("/home.html"))
                         .test("test_walkTag", test_walkTag(expected_04))
                         
                         .stream();
    }

}
