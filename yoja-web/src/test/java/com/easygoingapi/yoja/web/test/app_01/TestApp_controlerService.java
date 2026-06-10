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

public class TestApp_controlerService {
    
    public static Consumer<TestContext> test_first_01 = testContext -> {
        final String result = testContext.seleniumService()
                                          .executeScript(""" 
            const controler = yojaWeb.controlerService
                                     .first(document,
                                            c => c.constructor.name === 'UserControler')
            return controler.constructor.name
        """);
        assertEquals("UserControler", result);
    };
    
    public static Consumer<TestContext> test_first_02 = testContext -> {
        final String result = testContext.seleniumService()
                                          .executeScript(""" 
            const controler = yojaWeb.controlerService
                                     .first(document,
                                            c => c.constructor.name === 'ArticleControler')
            return controler.constructor.name
        """);
        assertEquals("ArticleControler", result);
    };
    
    public static Consumer<TestContext> test_first_03 = testContext -> {
        final String result = testContext.seleniumService()
                                          .executeScript(""" 
            const controler = yojaWeb.controlerService
                                     .first(document,
                                            c => c.constructor.name === 'ArticleControler')
            return controler.label
        """);
        assertEquals("bread", result);
    };

    public static Consumer<TestContext> test_first_04 = testContext -> {
        final String result = testContext.seleniumService()
                                          .executeScript(""" 
            const controler = yojaWeb.controlerService
                                     .first(document)
            return controler.constructor.name
        """);
        assertEquals("HomeControler", result);
    };
    
    public static Consumer<TestContext> test_first_05 = testContext -> {
        final String result = testContext.seleniumService()
                                          .executeScript(""" 
            const tag = yojaWeb.firstTag('.id-section')
            const controler = yojaWeb.controlerService
                                     .first(tag)
            return controler
        """);
        assertNull(result);
    };
    
    public static Consumer<TestContext> test_first_06 = testContext -> {
        final String result = testContext.seleniumService()
                                          .executeScript(""" 
            const tag = yojaWeb.firstTag('.user-section')
            const controler = yojaWeb.controlerService
                                     .first(tag,
                                            c => c.constructor.name === 'ArticleControler')
            return controler.constructor.name
        """);
        assertEquals("ArticleControler", result);
    };
    
    public static Consumer<TestContext> test_first_07 = testContext -> {
        final Boolean result = testContext.seleniumService()
                                          .executeScript(""" 
            const tag = yojaWeb.firstTag('.id-section')
            const controler = yojaWeb.controlerService
                                     .first(tag,
                                            c => c.constructor.name === 'HomeControler')
            return controler === null
        """);
        assertTrue(result);
    };
    
    public static Consumer<TestContext> test_find_01 = testContext -> {
        final Long result = testContext.seleniumService()
                                       .executeScript(""" 
            const controlers = yojaWeb.controlerService
                                      .find(document,
                                            c => c.constructor.name === 'ArticleControler')
            return controlers.length
        """);
        assertEquals(2, result);
    };

    public static Consumer<TestContext> test_find_02 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript(""" 
            const controlers = yojaWeb.controlerService
                                      .find(document)
            const result = []
            for (const controler of controlers) {
                result.push(controler.constructor.name)
            }
            return result
        """);
        assertEquals(List.of("HomeControler", "UserControler", "OrderControler", 
                             "ArticleControler", "ArticleControler"), 
                     result);
    };

    public static Consumer<TestContext> test_find_03 = testContext -> {
        final Long result = testContext.seleniumService()
                                       .executeScript(""" 
            const controlers = yojaWeb.controlerService
                                      .find(document,
                                            c => c.constructor.name === 'Controler')
            return controlers.length
        """);
        assertEquals(0, result);
    };
    
    public static Consumer<TestContext> test_parent_01 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            const tag = yojaWeb.firstTag('.user-section')
            const controler = controlerService.parent(tag)
            return controler.constructor.name
        """);
        assertEquals("HomeControler", result);
    };
    
    public static Consumer<TestContext> test_parent_02 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            const tag = yojaWeb.firstTag('.id-section')
            const controler = controlerService.parent(tag)
            return controler.constructor.name
        """);
        assertEquals("UserControler", result);
    };

    public static Consumer<TestContext> test_parent_03 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            const tag = yojaWeb.firstTag('.address-section')
            const controler = controlerService.parent(tag)
            return controler.constructor.name
        """);
        assertEquals("UserControler", result);
    };

    public static Consumer<TestContext> test_parent_04 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            const tag = yojaWeb.firstTag('.address-section label')
            const controler = controlerService.parent(tag)
            return controler.constructor.name
        """);
        assertEquals("UserControler", result);
    };

    public static Consumer<TestContext> test_parent_05 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            const tag = yojaWeb.firstTag('.address-section label')
            const controler = controlerService.parent(tag,
                                                      c => c.constructor.name === 'HomeControler')
            return controler.constructor.name
        """);
        assertEquals("HomeControler", result);
    };
    
    public static Consumer<TestContext> test_closest_01 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            const tag = yojaWeb.firstTag('.user-section')
            const controler = controlerService.closest(tag)
            return controler.constructor.name
        """);
        assertEquals("UserControler", result);
    };

    public static Consumer<TestContext> test_closest_02 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            const tag = yojaWeb.firstTag('.id-section')
            const controler = controlerService.closest(tag)
            return controler.constructor.name
        """);
        assertEquals("UserControler", result);
    };
    
    public static Consumer<TestContext> test_closest_03 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            const tag = yojaWeb.firstTag('.address-section')
            const controler = controlerService.closest(tag)
            return controler.constructor.name
        """);
        assertEquals("UserControler", result);
    };

    public static Consumer<TestContext> test_closest_04 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            const tag = yojaWeb.firstTag('.address-section fieldset')
            const controler = controlerService.closest(tag)
            return controler.constructor.name
        """);
        assertEquals("UserControler", result);
    };

    public static Consumer<TestContext> test_root_01 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            const tag = yojaWeb.firstTag('.address-section fieldset')
            const controler = controlerService.root(tag)
            return controler.constructor.name
        """);
        assertEquals("HomeControler", result);
    };
    
    public static Consumer<TestContext> test_children_01 = testContext -> {
        final Long result = testContext.seleniumService()
                                       .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            const tag = yojaWeb.firstTag('.order-section')
            const controlers = controlerService.children(tag)
            return controlers.length
        """);
        assertEquals(2, result);
    };
    
    public static Consumer<TestContext> test_children_02 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            const tag = yojaWeb.firstTag('.order-section')
            const controlers = controlerService.children(tag, 
                                                         c => 'milk' === c.label)
            const result = []
            for (const controler of controlers) {
                result.push(controler.label)
            }
            return result
        """);
        assertEquals(List.of("milk"), result);
    };

    public static Consumer<TestContext> test_parents_01 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            const tag = yojaWeb.firstTag('.article-section')
            const controlers = controlerService.parents(tag)
            const result = []
            for (const controler of controlers) {
                result.push(controler.className())
            }
            return result
        """);
        assertEquals(List.of("OrderControler", "UserControler", "HomeControler"),
                     result);
    };
    
    public static Consumer<TestContext> test_parents_02 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            const tag = yojaWeb.firstTag('.article-section')
            const controlers = controlerService.parents(tag,
                                                        c => c.className() === 'OrderControler' 
                                                            || c.className() === 'HomeControler')
            const result = []
            for (const controler of controlers) {
                result.push(controler.className())
            }
            return result
        """);
        assertEquals(List.of("OrderControler", "HomeControler"),
                     result);
    };
    
    public static Consumer<TestContext> test_walk_01 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript(""" 
            const controlerService = yojaWeb.controlerService
            const tag = yojaWeb.firstTag('.article-section')
            const result = []
            controlerService.walk(document, 
                                  c => result.push(c.className()))
            return result
        """);
        assertEquals(List.of("HomeControler", "UserControler", "OrderControler", 
                             "ArticleControler", "ArticleControler"),
                     result);
    };
    
    @TestFactory
    public Stream<DynamicNode> testControlerService() {
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
