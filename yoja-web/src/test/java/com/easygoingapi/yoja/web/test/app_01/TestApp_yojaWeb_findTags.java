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
package com.easygoingapi.yoja.web.test.app_01;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.openqa.selenium.remote.RemoteWebElement;

import com.easygoingapi.yoja.selenium.TestContext;

public class TestApp_yojaWeb_findTags {
    
    public static Consumer<TestContext> test_00 = testContext -> {
        final List<RemoteWebElement> result = testContext.seleniumService()
                                                         .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            return yojaWeb.findTags('label')
        """);
        assertEquals(7, result.size());
        assertEquals("label", result.get(0).getTagName());
    };

    public static Consumer<TestContext> test_01 = testContext -> {
        final List<RemoteWebElement> result = testContext.seleniumService()
                                                         .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            return yojaWeb.findTags('.address-section label')
        """);
        assertEquals(3, result.size());
        assertEquals("label", result.get(0).getTagName());
    };
    
    public static Consumer<TestContext> test_02 = testContext -> {
        final List<RemoteWebElement> result = testContext.seleniumService()
                                                         .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            return yojaWeb.findTags('.user-section .id-section label')
        """);
        assertEquals(2, result.size());
        assertEquals("label", result.get(0).getTagName());
    };
    
    public static Consumer<TestContext> test_03 = testContext -> {
        final List<RemoteWebElement> result = testContext.seleniumService()
                                                         .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            return yojaWeb.findTags('.user-section .address-section>fieldset>label')
        """);
        assertEquals(3, result.size());
        assertEquals("label", result.get(0).getTagName());
    };

    public static Consumer<TestContext> test_04 = testContext -> {
        final List<RemoteWebElement> result = testContext.seleniumService()
                                                         .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            return yojaWeb.findTags('.id-section label, .address-section>label')
        """);
        assertEquals(2, result.size());
        assertEquals("label", result.get(0).getTagName());
    };

    public static Consumer<TestContext> test_05 = testContext -> {
        final List<RemoteWebElement> result = testContext.seleniumService()
                                                          .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            return yojaWeb.findTags('.address-section label, .address-section>fieldset>label')
        """);
        assertEquals(3, result.size());
        assertEquals("label", result.get(0).getTagName());
    };

    public static Consumer<TestContext> test_06 = testContext -> {
        final List<RemoteWebElement> result = testContext.seleniumService()
                                                          .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            return yojaWeb.findTags('.id-section label, .address-section>fieldset>label')
        """);
        assertEquals(5, result.size());
        assertEquals("label", result.get(0).getTagName());
    };

    public static Consumer<TestContext> test_07 = testContext -> {
        final List<RemoteWebElement> result = testContext.seleniumService()
                                                          .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            return yojaWeb.findTags('.address-section>fieldset>*')
        """);
        assertEquals(7, result.size());
    };

    public static Consumer<TestContext> test_08 = testContext -> {
        final List<RemoteWebElement> result = testContext.seleniumService()
                                                         .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            const tag = yojaWeb.firstTag('.order-section')
            return yojaWeb.findTags('label', tag)
        """);
        assertEquals(2, result.size());
        assertEquals("label", result.get(0).getTagName());
    };
    
    public static Consumer<TestContext> test_section_00 = testContext -> {
        final List<RemoteWebElement> result = testContext.seleniumService()
                                                         .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            const tag = yojaWeb.firstTag('.user-section')
            return yojaWeb.findSectionTags('label', tag)
        """);
        assertEquals(2, result.size());
    };
    
    public static Consumer<TestContext> test_section_01 = testContext -> {
        final List<RemoteWebElement> result = testContext.seleniumService()
                                                         .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            const tag = yojaWeb.firstTag('.order-section')
            return yojaWeb.findSectionTags('label', tag)
        """);
        assertEquals(0, result.size());
    };

    public static Consumer<TestContext> test_section_02 = testContext -> {
        final List<RemoteWebElement> result = testContext.seleniumService()
                                                         .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            const tag = yojaWeb.firstTag('.article-section')
            return yojaWeb.findSectionTags('label', tag)
        """);
        assertEquals(1, result.size());
    };
    
    public static Consumer<TestContext> test_section_03 = testContext -> {
        final List<RemoteWebElement> result = testContext.seleniumService()
                                                         .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            const tag = yojaWeb.firstTag('.article-section')
            return yojaWeb.findSectionTags('fieldset', tag)
        """);
        assertEquals(0, result.size());
    };

    public static Consumer<TestContext> test_section_04 = testContext -> {
        final List<RemoteWebElement> result = testContext.seleniumService()
                                                         .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            return yojaWeb.findSectionTags('fieldset')
        """);
        assertEquals(0, result.size());
    };
    
    @TestFactory
    public Stream<DynamicNode> testFindTags() {
        return ResourceUtil.initialize_app()
                          
                           .test("test_00", test_00)
                           .test("test_01", test_01)
                           .test("test_02", test_02)
                           .test("test_03", test_03)
                           .test("test_04", test_04)
                           .test("test_05", test_05)
                           .test("test_06", test_06)
                           .test("test_07", test_07)
                           .test("test_08", test_08)
                           
                           .test("test_section_00", test_section_00)
                           .test("test_section_01", test_section_01)
                           .test("test_section_02", test_section_02)
                           .test("test_section_03", test_section_03)
                           .test("test_section_04", test_section_04)
                           
                           .stream();
    }    
    
}
