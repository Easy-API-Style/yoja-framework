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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import com.easygoingapi.yoja.selenium.TestContext;

public class TestApp_yojaWeb_section {

    public static Consumer<TestContext> test_languagePath_01 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'user-section')
            return section.languagePath
        """);
        assertNull(result);
    };

    public static Consumer<TestContext> test_languagePath_02 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document)
            return section.languagePath
        """);
        assertEquals("/lang.xml", result);
    };

    public static Consumer<TestContext> test_cssPath_01 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'user-section')
            return section.cssPath
        """);
        assertEquals("/user/user.css", result);
    };
    
    public static Consumer<TestContext> test_slotPath_01 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'user-section')
            return section.slotPath
        """);
        assertEquals("/user/user.cpt.html", result);
    };
    
    public static Consumer<TestContext> test_path_01 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'user-section')
            return section.path
        """);
        assertEquals("/home.html", result);
    };
    
    public static Consumer<TestContext> test_path_02 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'address-section')
            return section.path
        """);
        assertEquals("/user/user.cpt.html", result);
    };

    public static Consumer<TestContext> test_path_03 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'article-section')
            return section.path
        """);
        assertEquals("/order/article/article.cpt.html", result);
    };
    
    public static Consumer<TestContext> test_path_04 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document)
            return section.path
        """);
        assertEquals("/home.html", result);
    };

    public static Consumer<TestContext> test_path_05 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'order-section')
            return section.path
        """);
        assertEquals("/user/user.cpt.html", result);
    };

    public static Consumer<TestContext> test_tag_01 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'order-section')
            return section.tag.getAttribute('class')
        """);
        assertEquals("order-section", result);
    };
    
    public static Consumer<TestContext> test_tag_02 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'article-section')
            return section.tag.getAttribute('class')
        """);
        assertEquals("article-section", result);
    };

    public static Consumer<TestContext> test_shadowTag_01 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'article-section')
            return section.shadowTag.host.getAttribute('class')
        """);
        assertEquals("article-section", result);
    };
    
    public static Consumer<TestContext> test_shadowTag_02 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'order-section')
            return section.shadowTag.host.getAttribute('class')
        """);
        assertEquals("order-section", result);
    };

    public static Consumer<TestContext> test_firstTag_01 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'order-section')
            return section.firstTag('label')
        """);
        assertNull(result);
    };

    public static Consumer<TestContext> test_firstTag_02 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'article-section')
            return section.firstTag('label').innerHTML
        """);
        assertEquals("bread", result);
    };

    public static Consumer<TestContext> test_deepFirstTag_01 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'order-section')
            return section.deepFirstTag('label').innerHTML
        """);
        assertEquals("bread", result);
    };
    
    public static Consumer<TestContext> test_deepFindTags_01 = testContext -> {
        final Long result = testContext.seleniumService()
                                       .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'order-section')
            return section.deepFindTags('label').length
        """);
        assertEquals(2, result);
    };
    
    public static Consumer<TestContext> test_findTags_01 = testContext -> {
        final Long result = testContext.seleniumService()
                                       .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'article-section')
            return section.findTags('label').length
        """);
        assertEquals(1, result);
    };
    
    public static Consumer<TestContext> test_findTags_02 = testContext -> {
        final Long result = testContext.seleniumService()
                                       .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'order-section')
            return section.findTags('label').length
        """);
        assertEquals(0, result);
    };
      
    public static Consumer<TestContext> test_walkTag_01 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript(""" 
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'order-section')
            const result = []
            section.walkTag(t => result.push(t.tagName))
            return result
        """);
        assertEquals(List.of("FIELDSET", "LEGEND"), result);
    };

    public static Consumer<TestContext> test_deepWalkTag_01= testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript(""" 
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'order-section')
            const result = []
            section.deepWalkTag(t => result.push(t.tagName))
            return result
        """);
        assertEquals(List.of("FIELDSET", 
                             "LEGEND", 
                                 "DIV", "LABEL", "INPUT", 
                                 "DIV", "LABEL", "INPUT"), 
                     result);
    };
    
    public static Consumer<TestContext> test_firstSection_01 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'user-section')
            return section.firstSection(s => s.tag.getAttribute('class') === 'order-section')
                          .tag.getAttribute('class')
        """);
        assertEquals("order-section", result);
    };
    
    public static Consumer<TestContext> test_firstSection_02 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'user-section')
            return section.firstSection()
                          .tag.getAttribute('class')
        """);
        assertEquals("address-section", result);
    };
    
    public static Consumer<TestContext> test_findSections_01 = testContext -> {
        final Long result = testContext.seleniumService()
                                       .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'user-section')
            return section.findSections(s => s.tag.getAttribute('class') === 'article-section').length
        """);
        assertEquals(2, result);
    };
    
    public static Consumer<TestContext> test_findSections_02 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'user-section')
            const sections = section.findSections(s => true)
            const result = []
            for (const section of sections) {
                 result.push(section.tag.getAttribute('class'))
            }
            return result
        """);
        assertEquals(List.of("address-section", "order-section", 
                             "article-section", "article-section"), 
                     result);
    };

    public static Consumer<TestContext> test_findSections_03 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'user-section')
            const sections = section.findSections()
            const result = []
            for (const section of sections) {
                 result.push(section.tag.getAttribute('class'))
            }
            return result
        """);
        assertEquals(List.of("address-section", "order-section", 
                             "article-section", "article-section"), 
                     result);
    };
    
    public static Consumer<TestContext> test_childrenSections_01 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'order-section')
            const sections = section.childrenSections()
            const result = []
            for (const section of sections) {
                 result.push(section.tag.getAttribute('class'))
            }
            return result
        """);
        assertEquals(List.of("article-section", "article-section"), 
                     result);
    };
    
    public static Consumer<TestContext> test_childrenSections_02 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'user-section')
            const sections = section.childrenSections()
            const result = []
            for (const section of sections) {
                 result.push(section.tag.getAttribute('class'))
            }
            return result
        """);
        assertEquals(List.of("address-section", "order-section"), 
                     result);
    };
    
    public static Consumer<TestContext> test_childrenSections_03 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'user-section')
            const sections = section.childrenSections(s => s.tag.getAttribute('class') === 'order-section')
            const result = []
            for (const section of sections) {
                 result.push(section.tag.getAttribute('class'))
            }
            return result
        """);
        assertEquals(List.of("order-section"), 
                     result);
    };

    public static Consumer<TestContext> test_parentSection_01 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'article-section')
            return section.parentSection(s => s.tag.getAttribute('class') === 'user-section')
                          .tag.getAttribute('class')
        """);
        assertEquals("user-section", result);
    };
    
    public static Consumer<TestContext> test_parentSection_02 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'address-section')
            return section.parentSection()
                          .tag.getAttribute('class')
        """);
        assertEquals("user-section", result);
    };

    public static Consumer<TestContext> test_parentSections_01 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'home-section')
            const sections = section.parentSections()
            const result = []
            for (const section of sections) {
                 result.push(section.tag.getAttribute('class'))
            }
            return result
        """);
        assertEquals(List.of(), result);
    };
    
    public static Consumer<TestContext> test_parentSections_02 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'order-section')
            const sections = section.parentSections()
            const result = []
            for (const section of sections) {
                 result.push(section.tag.getAttribute('class'))
            }
            return result
        """);
        assertEquals(List.of("user-section", "home-section"), 
                     result);
    };
    
    public static Consumer<TestContext> test_parentSections_03 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'order-section')
            const sections = section.parentSections(s => s.tag.getAttribute('class') === 'home-section')
            const result = []
            for (const section of sections) {
                 result.push(section.tag.getAttribute('class'))
            }
            return result
        """);
        assertEquals(List.of("home-section"), result);
    };

    public static Consumer<TestContext> test_rootSection_01 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'article-section')
            return section.rootSection()
                          .tag.getAttribute('class')
        """);
        assertEquals("home-section", result);
    };
    
    public static Consumer<TestContext> test_walkSection_01 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'order-section')
            const result = []
            section.walkSection(s => {
                 if (s?.controler?.label === 'milk') {
                     result.push(s.tag.getAttribute('class'))
                 }
            })
            return result
        """);
        assertEquals(List.of("article-section"), result);
    };

    public static Consumer<TestContext> test_firstControler_01 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'user-section')
            return section.firstControler(c => c.className() === 'OrderControler')
                          .className()
        """);
        assertEquals("OrderControler", result);
    };
    
    public static Consumer<TestContext> test_firstControler_02 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'user-section')
            return section.firstControler()
                          .className('class')
        """);
        assertEquals("OrderControler", result);
    };
    
    public static Consumer<TestContext> test_findControlers_01 = testContext -> {
        final Long result = testContext.seleniumService()
                                       .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'user-section')
            return section.findControlers(c => c.className() === 'ArticleControler').length
        """);
        assertEquals(2, result);
    };
    
    public static Consumer<TestContext> test_findControlers_02 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'user-section')
            const controlers = section.findControlers(c => true)
            const result = []
            for (const controler of controlers) {
                 result.push(controler.className())
            }
            return result
        """);
        assertEquals(List.of("OrderControler", "ArticleControler", "ArticleControler"), 
                     result);
    };

    public static Consumer<TestContext> test_findControlers_03 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'user-section')
            const controlers = section.findControlers()
            const result = []
            for (const controler of controlers) {
                 result.push(controler.className())
            }
            return result
        """);
        assertEquals(List.of("OrderControler", "ArticleControler", "ArticleControler"), 
                     result);
    };
    
    public static Consumer<TestContext> test_childrenControlers_01 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'order-section')
            const controlers = section.childrenControlers()
            const result = []
            for (const controler of controlers) {
                 result.push(controler.className())
            }
            return result
        """);
        assertEquals(List.of("ArticleControler", "ArticleControler"), 
                     result);
    };
    
    public static Consumer<TestContext> test_childrenControlers_02 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'user-section')
            const controlers = section.childrenControlers()
            const result = []
            for (const controler of controlers) {
                 result.push(controler.className())
            }
            return result
        """);
        assertEquals(List.of("OrderControler"), 
                     result);
    };
    
    public static Consumer<TestContext> test_childrenControlers_03 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'user-section')
            const controlers = section.childrenControlers(c => c.className() === 'OrderControler')
            const result = []
            for (const controler of controlers) {
                 result.push(controler.className())
            }
            return result
        """);
        assertEquals(List.of("OrderControler"), 
                     result);
    };

    public static Consumer<TestContext> test_parentControler_01 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'article-section')
            return section.parentControler(c => c.className() === 'UserControler')
                          .className()
        """);
        assertEquals("UserControler", result);
    };
    
    public static Consumer<TestContext> test_parentControler_02 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'address-section')
            return section.parentControler()
                          .className()
        """);
        assertEquals("UserControler", result);
    };

    public static Consumer<TestContext> test_parentControlers_01 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'home-section')
            const controlers = section.parentControlers()
            const result = []
            for (const controler of controlers) {
                 result.push(controler.className())
            }
            return result
        """);
        assertEquals(List.of(), result);
    };
    
    public static Consumer<TestContext> test_parentControlers_02 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'order-section')
            const controlers = section.parentControlers()
            const result = []
            for (const controler of controlers) {
                 result.push(controler.className())
            }
            return result
        """);
        assertEquals(List.of("UserControler", "HomeControler"), 
                     result);
    };
    
    public static Consumer<TestContext> test_parentControlers_03 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'order-section')
            const controlers = section.parentControlers(c => c.className() === 'HomeControler')
            const result = []
            for (const controler of controlers) {
                 result.push(controler.className())
            }
            return result
        """);
        assertEquals(List.of("HomeControler"), result);
    };

    public static Consumer<TestContext> test_rootControler_01 = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'article-section')
            return section.rootControler()
                          .className()
        """);
        assertEquals("HomeControler", result);
    };
    
    public static Consumer<TestContext> test_walkControler_01 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript("""
            const section = yojaWeb.sectionService
                                   .first(document,
                                          s => s.tag.getAttribute('class') === 'order-section')
            const result = []
            section.walkControler(c => {
                 result.push(c.className())
            })
            return result
        """);
        assertEquals(List.of("ArticleControler", "ArticleControler"), result);
    };
    
    @TestFactory
    public Stream<DynamicNode> factory() {
        return ResourceUtil.initialize_app()

                           .test("test_languagePath_01", test_languagePath_01)
                           .test("test_languagePath_02", test_languagePath_02)
                           .test("test_cssPath_01", test_cssPath_01)
                           .test("test_slotPath_01", test_slotPath_01)
                           
                           .test("test_path_01", test_path_01)
                           .test("test_path_02", test_path_02)
                           .test("test_path_03", test_path_03)
                           .test("test_path_04", test_path_04)
                           .test("test_path_05", test_path_05)
                           
                           .test("test_tag_01", test_tag_01)
                           .test("test_tag_02", test_tag_02)
                           
                           .test("test_shadowTag_01", test_shadowTag_01)
                           .test("test_shadowTag_02", test_shadowTag_02)
                           
                           .test("test_firstTag_01", test_firstTag_01)
                           .test("test_firstTag_02", test_firstTag_02)
                           
                           .test("test_deepFirstTag_01", test_deepFirstTag_01)
                           
                           .test("test_deepFindTags_01", test_deepFindTags_01)
                           
                           .test("test_findTags_01", test_findTags_01)
                           .test("test_findTags_01", test_findTags_01)
                           
                           .test("test_walkTag_01", test_walkTag_01)
                           
                           .test("test_deepWalkTag_01", test_deepWalkTag_01)
                           
                           .test("test_firstSection_01", test_firstSection_01)
                           .test("test_firstSection_02", test_firstSection_02)
                           
                           .test("test_findSections_01", test_findSections_01)
                           .test("test_findSections_02", test_findSections_02)
                           .test("test_findSections_03", test_findSections_03)
                           
                           .test("test_childrenSections_01", test_childrenSections_01)
                           .test("test_childrenSections_01", test_childrenSections_01)
                           .test("test_childrenSections_03", test_childrenSections_03)
                           
                           .test("test_parentSection_01", test_parentSection_01)
                           .test("test_parentSection_02", test_parentSection_02)
                           
                           .test("test_parentSections_01", test_parentSections_01)
                           .test("test_parentSections_02", test_parentSections_02)
                           .test("test_parentSections_02", test_parentSections_02)
                           
                           .test("test_rootSection_01", test_rootSection_01)
                           
                           .test("test_walkSection_01", test_walkSection_01)
                           
                           .test("test_firstControler_01", test_firstControler_01)
                           .test("test_firstControler_02", test_firstControler_02)
                           
                           .test("test_findControlers_01", test_findControlers_01)
                           .test("test_findControlers_02", test_findControlers_02)
                           .test("test_findControlers_03", test_findControlers_03)
                           
                           .test("test_childrenControlers_01", test_childrenControlers_01)
                           .test("test_childrenControlers_01", test_childrenControlers_02)
                           .test("test_childrenControlers_03", test_childrenControlers_03)
                          
                          .test("test_parentControler_01", test_parentControler_01)
                          .test("test_parentControler_02", test_parentControler_02)
                          
                          .test("test_parentControlers_01", test_parentControlers_01)
                          .test("test_parentControlers_02", test_parentControlers_02)
                          .test("test_parentControlers_02", test_parentControlers_02)
                          
                          .test("test_rootControler_01", test_rootControler_01)
                          
                          .test("test_walkControler_01", test_walkControler_01)
                          
                          .stream();
    }

}
