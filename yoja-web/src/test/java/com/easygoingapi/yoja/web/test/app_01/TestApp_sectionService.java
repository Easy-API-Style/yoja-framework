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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import com.easygoingapi.yoja.selenium.TestContext;

public class TestApp_sectionService {
    
    public static Consumer<TestContext> test_first_01 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript(""" 
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'user-section')
            return section.tag.getAttribute('class')
        """);
        assertEquals("user-section", result);
    };
    
    public static Consumer<TestContext> test_first_02 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript(""" 
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'article-section')
            return section.tag.getAttribute('class')
        """);
        assertEquals("article-section", result);
    };
    
    public static Consumer<TestContext> test_first_03 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript(""" 
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'article-section')
            return section.controler.label
        """);
        assertEquals("bread", result);
    };

    public static Consumer<TestContext> test_first_04 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript(""" 
            const section = yojaWeb.sectionService
                                   .first(document)
            return section.tag.getAttribute('class')
        """);
        assertEquals("home-section", result);
    };
    
    public static Consumer<TestContext> test_first_05 = testContext -> {
        final String result = testContext.seleniumService()
                                          .executeScript(""" 
            const tag = yojaWeb.firstTag('.id-section')
            const section = yojaWeb.sectionService
                                   .first(tag)
            return section
        """);
        assertNull(result);
    };
    
    public static Consumer<TestContext> test_first_06 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript(""" 
            const tag = yojaWeb.firstTag('.user-section')
            const section = yojaWeb.sectionService
                                   .first(tag,
                                          s => s.tag.getAttribute('class') === 'article-section')
            return section.tag.getAttribute('class')
        """);
        assertEquals("article-section", result);
    };
    
    public static Consumer<TestContext> test_first_07 = testContext -> {
        final Boolean result = testContext.seleniumService()
                                          .executeScript(""" 
            const tag = yojaWeb.firstTag('.id-section')
            const section = yojaWeb.sectionService
                                   .first(tag,
                                          s => s.tag.getAttribute('class') === 'home-section')
            return section === null
        """);
        assertTrue(result);
    };
    
    public static Consumer<TestContext> test_find_01 = testContext -> {
        final Long result = testContext.seleniumService()
                                       .executeScript(""" 
            const sections = yojaWeb.sectionService
                                    .find(document,
                                          s => s.tag.getAttribute('class') === 'article-section')
            return sections.length
        """);
        assertEquals(2, result);
    };

    public static Consumer<TestContext> test_find_02 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript(""" 
            const sections = yojaWeb.sectionService
                                    .find(document)
            const result = []
            for (const section of sections) {
                result.push(section.tag.getAttribute('class'))
            }
            return result
        """);
        assertEquals(List.of("home-section", "user-section", "address-section", "order-section", 
                             "article-section", "article-section"), 
                     result);
    };

    public static Consumer<TestContext> test_find_03 = testContext -> {
        final Long result = testContext.seleniumService()
                                       .executeScript(""" 
            const sections = yojaWeb.sectionService
                                    .find(document,
                                          s => s.tag.getAttribute('class') === '???')
            return sections.length
        """);
        assertEquals(0, result);
    };
    
    public static Consumer<TestContext> test_parent_01 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript(""" 
            const sectionService = yojaWeb.sectionService
            const tag = yojaWeb.firstTag('.user-section')
            const section = sectionService.parent(tag)
            return section.tag.getAttribute('class')
        """);
        assertEquals("home-section", result);
    };
    
    public static Consumer<TestContext> test_parent_02 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript(""" 
            const sectionService = yojaWeb.sectionService
            const tag = yojaWeb.firstTag('.id-section')
            const section = sectionService.parent(tag)
            return section.tag.getAttribute('class')
        """);
        assertEquals("user-section", result);
    };

    public static Consumer<TestContext> test_parent_03 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript(""" 
            const sectionService = yojaWeb.sectionService
            const tag = yojaWeb.firstTag('.address-section')
            const section = sectionService.parent(tag)
            return section.tag.getAttribute('class')
        """);
        assertEquals("user-section", result);
    };

    public static Consumer<TestContext> test_parent_04 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript(""" 
            const sectionService = yojaWeb.sectionService
            const tag = yojaWeb.firstTag('.address-section label')
            const section = sectionService.parent(tag)
            return section.tag.getAttribute('class')
        """);
        assertEquals("address-section", result);
    };

    public static Consumer<TestContext> test_parent_05 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript(""" 
            const sectionService = yojaWeb.sectionService
            const tag = yojaWeb.firstTag('.address-section label')
            const section = sectionService.parent(tag,
                                                  s => s.tag.getAttribute('class') === 'home-section')
            return section.tag.getAttribute('class')
        """);
        assertEquals("home-section", result);
    };
    
    public static Consumer<TestContext> test_closest_01 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript(""" 
            const sectionService = yojaWeb.sectionService
            const tag = yojaWeb.firstTag('.user-section')
            const section = sectionService.closest(tag)
            return section.tag.getAttribute('class')
        """);
        assertEquals("user-section", result);
    };

    public static Consumer<TestContext> test_closest_02 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript(""" 
            const sectionService = yojaWeb.sectionService
            const tag = yojaWeb.firstTag('.id-section')
            const section = sectionService.closest(tag)
            return section.tag.getAttribute('class')
        """);
        assertEquals("user-section", result);
    };
    
    public static Consumer<TestContext> test_closest_03 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript(""" 
            const sectionService = yojaWeb.sectionService
            const tag = yojaWeb.firstTag('.address-section')
            const section = sectionService.closest(tag)
            return section.tag.getAttribute('class')
        """);
        assertEquals("address-section", result);
    };

    public static Consumer<TestContext> test_closest_04 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript(""" 
            const sectionService = yojaWeb.sectionService
            const tag = yojaWeb.firstTag('.address-section fieldset')
            const section = sectionService.closest(tag)
            return section.tag.getAttribute('class')
        """);
        assertEquals("address-section", result);
    };

    public static Consumer<TestContext> test_root_01 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript(""" 
            const sectionService = yojaWeb.sectionService
            const tag = yojaWeb.firstTag('.address-section fieldset')
            const section = sectionService.root(tag)
            return section.tag.getAttribute('class')
        """);
        assertEquals("home-section", result);
    };
    
    public static Consumer<TestContext> test_children_01 = testContext -> {
        final Long result = testContext.seleniumService()
                                       .executeScript(""" 
            const sectionService = yojaWeb.sectionService
            const tag = yojaWeb.firstTag('.order-section')
            const sections = sectionService.children(tag)
            return sections.length
        """);
        assertEquals(2, result);
    };
    
    public static Consumer<TestContext> test_children_02 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript(""" 
            const sectionService = yojaWeb.sectionService
            const tag = yojaWeb.firstTag('.order-section')
            const sections = sectionService.children(tag, 
                                                     s => 'milk' === s.controler?.label)
            const result = []
            for (const section of sections) {
                result.push(section.controler.label)
            }
            return result
        """);
        assertEquals(List.of("milk"), result);
    };
    
    public static Consumer<TestContext> test_parents_01 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript(""" 
            const sectionService = yojaWeb.sectionService
            const tag = yojaWeb.firstTag('.article-section')
            const sections = sectionService.parents(tag)
            const result = []
            for (const section of sections) {
                result.push(section.tag.getAttribute('class'))
            }
            return result
        """);
        assertEquals(List.of("order-section", "user-section", "home-section"),
                     result);
    };
    
    public static Consumer<TestContext> test_parents_02 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript(""" 
            const sectionService = yojaWeb.sectionService
            const tag = yojaWeb.firstTag('.article-section')
            const sections = sectionService.parents(tag,
                                                    s => s.tag.getAttribute('class') === 'order-section' 
                                                              || s.tag.getAttribute('class') === 'home-section')
            const result = []
            for (const section of sections) {
                result.push(section.tag.getAttribute('class'))
            }
            return result
        """);
        assertEquals(List.of("order-section", "home-section"),
                     result);
    };
    
    public static Consumer<TestContext> test_walk_01 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript(""" 
            const sectionService = yojaWeb.sectionService
            const tag = yojaWeb.firstTag('.article-section')
            const result = []
            sectionService.walk(document, 
                                s => result.push(s.tag.getAttribute('class')))
            return result
        """);
        assertEquals(List.of("home-section", "user-section", 
                             "address-section", "order-section", 
                             "article-section", "article-section"),
                     result);
    };
    
    @TestFactory
    public Stream<DynamicNode> testSectionService() {
        return ResourceUtil.initialize_app()
                         
                           .test("test_first_01", test_first_01)
                           .test("test_first_02", test_first_02)
                           .test("test_first_02", test_first_02)
                           .test("test_first_03", test_first_03)
                           .test("test_first_04", test_first_04)
                           .test("test_first_05", test_first_05)
                           .test("test_first_06", test_first_06)
                           .test("test_first_07", test_first_07)
                            
                           .test("test_find_01", test_find_01)
                           .test("test_find_02", test_find_02)
                           .test("test_find_03", test_find_03)
                           
                           .test("test_parent_01", test_parent_01)
                           .test("test_parent_02", test_parent_02)
                           .test("test_parent_03", test_parent_03)
                           .test("test_parent_04", test_parent_04)
                           .test("test_parent_05", test_parent_05)
                           
                           .test("test_closest_01", test_closest_01)
                           .test("test_closest_02", test_closest_02)
                           .test("test_closest_03", test_closest_03)
                           .test("test_closest_04", test_closest_04)
                           
                           .test("test_root_01", test_root_01)
                           
                           .test("test_children_01", test_children_01)
                           .test("test_children_02", test_children_02)
                           
                           .test("test_parents_01", test_parents_01)
                           .test("test_parents_02", test_parents_02)
                           
                           .test("test_walk_01", test_walk_01)
                           
                           .stream();
    }
    
}
