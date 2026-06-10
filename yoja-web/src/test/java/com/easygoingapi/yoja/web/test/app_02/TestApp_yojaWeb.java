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
package com.easygoingapi.yoja.web.test.app_02;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import com.easygoingapi.yoja.selenium.TestContext;
import com.easygoingapi.yoja.selenium.SeleniumService.Storage;

public class TestApp_yojaWeb {
    
    public static Consumer<TestContext> test_config = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript(""" 
            return JSON.stringify(yojaWeb.config)
        """);
        assertEquals("{\"defaultLanguage\":\"fr\",\"version\":\"0.0.0\"}", 
                     result);
    };
    
    public static Consumer<TestContext> test_controlerService = testContext -> {
        final Boolean result = testContext.seleniumService()
                                          .executeScript(""" 
            return yojaWeb.controlerService.constructor.name === 'ControlerService'
        """);
        assertTrue(result);
    };
    
    public static Consumer<TestContext> test_sectionService = testContext -> {
        final Boolean result = testContext.seleniumService()
                                          .executeScript(""" 
            return yojaWeb.sectionService.constructor.name === 'SectionService'
        """);
        assertTrue(result);
    };

    public static Consumer<TestContext> test_cssSheetService = testContext -> {
        final Boolean result = testContext.seleniumService()
                                          .executeScript(""" 
            return yojaWebApi.cssSheetService !== null
        """);
        assertTrue(result);
    };
    
    public static Consumer<TestContext> test_walkTag = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript(""" 
            const result = []
            yojaWeb.walkTag(t => result.push(t.tagName))
            return result
        """);
        assertEquals(List.of("HTML", "HEAD", "TITLE", "META", "META", "META", "SCRIPT", "SCRIPT", "SCRIPT",
                             "BODY", "DIV", 
                                     "DIV", "LABEL", "INPUT", 
                                            "LABEL", "INPUT", 
                                     "DIV", "FIELDSET", 
                                            "LEGEND", 
                                            "LABEL", "INPUT", 
                                            "LABEL", "INPUT", 
                                            "LABEL", "INPUT",
                                     "DIV", "FIELDSET", 
                                            "LEGEND", 
                                            "DIV", "LABEL", "INPUT", 
                                            "DIV", "LABEL", "INPUT"), 
                     result);
    };

    public static Consumer<TestContext> test_walkSectionTag_01 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript(""" 
            const result = []
            yojaWeb.walkSectionTag(t => result.push(t.tagName))
            return result
        """);
        assertEquals(List.of("HTML", "HEAD", "TITLE", 
                             "META", "META", "META", 
                             "SCRIPT", "SCRIPT", "SCRIPT"), 
                     result);
    };
    
    public static Consumer<TestContext> test_walkSectionTag_02 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript(""" 
            const tag = yojaWeb.firstTag('.order-section')
            const result = []
            yojaWeb.walkSectionTag(t => result.push(t.tagName), tag)
            return result
        """);
        assertEquals(List.of("FIELDSET", "LEGEND"), 
                     result);
    };
    
    public static Consumer<TestContext> test_localStorage = testContext -> {
        final Boolean result = testContext.seleniumService()
                                          .localStorage("documentReadyDone", 
                                                        Storage.value);
        assertTrue(result);
    };
    
    public static Consumer<TestContext> test_sessionStorage = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .sessionStorage("tagReadyClassNames", 
                                                               Storage.value);
        assertEquals(List.of("article-section", "article-section"), result);
    };
    
    @TestFactory
    public Stream<DynamicNode> factory() {
         return ResourceUtil.initialize_app()
                          
                            .test("test_config", test_config)
                            .test("test_sectionService", test_sectionService)
                            .test("test_controlerService", test_controlerService)
                            .test("test_cssSheetService", test_cssSheetService)
                            .test("test_walkTag", test_walkTag)
                            .test("test_walkSectionTag_01", test_walkSectionTag_01)
                            .test("test_walkSectionTag_02", test_walkSectionTag_02)
                            
                            .test("test_localStorage", test_localStorage)
                            .test("test_sessionStorage", test_sessionStorage)
                            .stream();
    }
    
}
