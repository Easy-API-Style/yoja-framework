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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.openqa.selenium.WebElement;

import com.easygoingapi.yoja.selenium.ScriptOption;
import com.easygoingapi.yoja.selenium.TestContext;
import com.easygoingapi.yoja.web.test.util.TestConfig;

public class TestLanguageService {

    private static Consumer<TestContext> onChangeLanguage() {
        return c -> {
            c.seleniumService().executeScript(""" 
                const callback = arguments[arguments.length - 1]
                const controler = yojaWeb.controlerService
                                         .first(document, c => c.constructor.name === 'HomeControler') 
                controler.languageService.onLanguageChange(h => {
                   window.yojaWebLanguage = h
                })   
             """);
            c.seleniumService().await(Duration.ofSeconds(10));
         };
    }
    
    private static Consumer<TestContext> checkDefaultLanguage() {
        return c -> {
             final String language = c.seleniumService().executeScript(""" 
                const controler = yojaWeb.controlerService
                                         .first(document, c => c.constructor.name === 'HomeControler') 
                return controler.languageService.getLanguage()
             """);
             assertEquals("fr", language);
         };
    }
    
    private static Consumer<TestContext> checkLabel_fr() {
        return c -> {
             final WebElement tag_1 = c.seleniumService().firstTag("label[for='familyName']");
             assertEquals("Nom De Famille", tag_1.getText());
             final WebElement tag_2 = c.seleniumService().firstTag("label[for='firstName']");
             assertEquals("Prénom", tag_2.getText());
         };
    }
    
    private static Consumer<TestContext> checkUpdatedLabel_fr() {
        return c -> {
             final WebElement tag_1 = c.seleniumService().firstTag("label[for='familyName']");
             assertEquals("Nom", tag_1.getText());
             final WebElement tag_2 = c.seleniumService().firstTag("label[for='firstName']");
             assertEquals("Prénom", tag_2.getText());
         };
    }
    
    private static Consumer<TestContext> checkEmptyLabel() {
        return c -> {
             final WebElement tag_1 = c.seleniumService().firstTag("label[for='familyName']");
             assertEquals("", tag_1.getText());
             final WebElement tag_2 = c.seleniumService().firstTag("label[for='firstName']");
             assertEquals("", tag_2.getText());
         };
    }
    
    private static Consumer<TestContext> checkPlaceholder_fr() {
        return c -> {
             final WebElement tag_1 = c.seleniumService().firstTag("input[name='familyName']");
             assertEquals("saisir le nom de famille", tag_1.getDomAttribute("placeholder"));
             final WebElement tag_2 = c.seleniumService().firstTag("input[name='firstName']");
             assertEquals("saisir votre prénom", tag_2.getDomAttribute("placeholder"));
         };
    }
    
    private static Consumer<TestContext> changeLanguage_en() {
        return c -> {
            c.seleniumService().executeScript(""" 
                const callback = arguments[arguments.length - 1]
                const controler = yojaWeb.controlerService
                                         .first(document, c => c.constructor.name === 'HomeControler') 
                window.yojaWebLanguage = null
                controler.languageService.setLanguage('en')   
             """);
            assertEquals("en", c.seleniumService().localStorage("ywLanguage"));
            final Map<String, Object> result = c.seleniumService().repeatScript(Duration.ofSeconds(30), """
                 return window.yojaWebLanguage
            """);
            assertEquals(true, result.get("updated"));
            assertEquals("en", result.get("language"));
         };
    }
    
    private static Consumer<TestContext> changeLanguage_fr() {
        return c -> {
            c.seleniumService().executeScript(""" 
                const callback = arguments[arguments.length - 1]
                const controler = yojaWeb.controlerService
                                         .first(document, c => c.constructor.name === 'HomeControler') 
                window.yojaWebLanguage = null
                controler.languageService.setLanguage('fr')                        
             """);
            assertEquals("fr", c.seleniumService().localStorage("ywLanguage"));
            final Map<String, Object> result = c.seleniumService().repeatScript(Duration.ofSeconds(30), """
                 return window.yojaWebLanguage
            """);
            assertEquals(true, result.get("updated"));
            assertEquals("fr", result.get("language"));
         };
    }
    
    private static Consumer<TestContext> checkLabel_en() {
        return c -> {
             final WebElement tag_1 = c.seleniumService().firstTag("label[for='familyName']");
             assertEquals("Family Name", tag_1.getText());
             final WebElement tag_2 = c.seleniumService().firstTag("label[for='firstName']");
             assertEquals("First Name", tag_2.getText());
         };
    }
    
    private static Consumer<TestContext> checkPlaceholder_en() {
        return c -> {
             final WebElement tag_1 = c.seleniumService().firstTag("input[name='familyName']");
             assertEquals("type your family name", tag_1.getDomAttribute("placeholder"));
             final WebElement tag_2 = c.seleniumService().firstTag("input[name='firstName']");
             assertEquals("type your first name", tag_2.getDomAttribute("placeholder"));
         };
    }

    private static Consumer<TestContext> appendAddress() {
        return c -> {
            final Boolean done = c.seleniumService().executeAsyncScript(""" 
                const callback = arguments[arguments.length - 1]
                const controler = yojaWeb.controlerService
                                         .first(document, c => c.constructor.name === 'HomeControler') 
                controler.appendAddress()     
                         .then(() => callback(true))    
             """);
             assertTrue(done);
         };
    }
    
    private static Consumer<TestContext> checkAddress_en() {
        return c -> {
             final WebElement tag_1 = c.seleniumService().firstTag("legend");
             assertEquals("Address", tag_1.getText());
             final WebElement tag_2 = c.seleniumService().firstTag("label[for='street']");
             assertEquals("Street", tag_2.getText());
         };
    }
    
    private static Consumer<TestContext> checkAddress_fr() {
        return c -> {
             final WebElement tag_1 = c.seleniumService().firstTag("legend");
             assertEquals("Adresse", tag_1.getText());
             final WebElement tag_2 = c.seleniumService().firstTag("label[for='street']");
             assertEquals("Rue", tag_2.getText());
         };
    }
    
    private static Consumer<TestContext> checkOnTranslate() {
        return c -> {
            final List<String> events = c.seleniumService().executeScript(""" 
               const controler = yojaWeb.controlerService
                                        .first(document, c => c.constructor.name === 'HomeControler') 
               return controler.languageEvents
            """);
            assertEquals(List.of("{\"tag\":\"LABEL\",\"value\":\"Nom De Famille\",\"type\":\"tag\",\"section\":\"BODY\"}", 
                                 "{\"tag\":\"INPUT\",\"value\":\"saisir le nom de famille\",\"attribute\":\"placeholder\",\"type\":\"attribute\",\"section\":\"BODY\"}", 
                                 "{\"tag\":\"LABEL\",\"value\":\"Prénom\",\"type\":\"tag\",\"section\":\"BODY\"}",
                                 "{\"tag\":\"INPUT\",\"value\":\"saisir votre prénom\",\"attribute\":\"placeholder\",\"type\":\"attribute\",\"section\":\"BODY\"}"), 
                                 events);
        };
    }
    
    private static Consumer<TestContext> languageLoaded() {
        return c -> {
             final boolean result = c.seleniumService().repeatScript(Duration.ofSeconds(3), """
                 const controler = yojaWeb.controlerService
                                          .first(document, c => c.constructor.name === 'HomeControler') 
                 return controler.languageLoaded
            """);
            assertTrue(result);
         };
    }
    
    @TestFactory
    public Stream<DynamicNode> testLanguageService_01() {
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.language_01")
                         .test("load page 1", c -> c.getHttpPage(Duration.ofSeconds(2),
                                                                 c.httpUrlBuilder()
                                                                  .path("/home.html")
                                                                  .build()))
                         .loadYwAssert()
                         .test("onChangeLanguage", onChangeLanguage())
                         .test("check default language", checkDefaultLanguage())
                         .test("check FR label", checkLabel_fr())
                         .test("check FR placeholder", checkPlaceholder_fr())
                         .test("change EN language", changeLanguage_en())
                         .test("check EN label", checkLabel_en())
                         .test("check EN placeholder", checkPlaceholder_en())
                         .test("append address", appendAddress())
                         .test("check EN address", checkAddress_en())
                         .test("change FR language", changeLanguage_fr())
                         .test("check FR address", checkAddress_fr())
                         .test("check FR label", checkLabel_fr())
                         .test("check FR placeholder", checkPlaceholder_fr())
                         .stream();
    }
    
    @TestFactory
    public Stream<DynamicNode> testLanguageService_02() {
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.language_02")
                         .test("load page 1", c -> c.getHttpPage(Duration.ofSeconds(2),
                                                                 c.httpUrlBuilder()
                                                                  .path("/home.html")
                                                                  .build()))
                         .loadYwAssert()
                         .test("onChangeLanguage", onChangeLanguage())
                         .test("check default language", checkDefaultLanguage())
                         .test("check FR label", checkLabel_fr())
                         .test("check FR placeholder", checkPlaceholder_fr())
                         .test("change EN language", changeLanguage_en())
                         .test("check EN label", checkLabel_en())
                         .test("check EN placeholder", checkPlaceholder_en())
                         .test("append address", appendAddress())
                         .test("check EN address", checkAddress_en())
                         .test("change FR language", changeLanguage_fr())
                         .test("check FR address", checkAddress_fr())
                         .test("check FR label", checkLabel_fr())
                         .test("check FR placeholder", checkPlaceholder_fr())
                         .stream();
    }
    
    @TestFactory
    public Stream<DynamicNode> testLanguageService_03() {
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.language_03")
                         .test("load page 1", c -> c.getHttpPage(Duration.ofSeconds(2),
                                                                 c.httpUrlBuilder()
                                                                  .path("/home.html")
                                                                  .build()))
                         .loadYwAssert()
                         .test("onChangeLanguage", onChangeLanguage())
                         .test("check default language", checkDefaultLanguage())
                         .test("check FR label", checkLabel_fr())
                         .test("check FR placeholder", checkPlaceholder_fr())
                         .test("change EN language", changeLanguage_en())
                         .test("check EN label", checkLabel_en())
                         .test("check EN placeholder", checkPlaceholder_en())
                         .test("append address", appendAddress())
                         .test("check EN address", checkAddress_en())
                         .test("change FR language", changeLanguage_fr())
                         .test("check FR address", checkAddress_fr())
                         .test("check FR label", checkLabel_fr())
                         .test("check FR placeholder", checkPlaceholder_fr())
                         .stream();
    }

    @TestFactory
    public Stream<DynamicNode> testLanguageService_04() {
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.language_04")
                         .test("load page 1", c -> c.getHttpPage(Duration.ofSeconds(2),
                                                                 c.httpUrlBuilder()
                                                                  .path("/home.html")
                                                                  .build()))
                         .loadYwAssert()
                         .test("check empty label", checkEmptyLabel())
                         .stream();
    }
    
    @TestFactory
    public Stream<DynamicNode> testLanguageService_05() {
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.language_05")
                         .test("load page 1", c -> c.getHttpPage(Duration.ofSeconds(2),
                                                                 c.httpUrlBuilder()
                                                                  .path("/home.html")
                                                                  .build()))
                         .loadYwAssert()
                         .test("check FR label", checkLabel_fr())
                         .test("check FR placeholder", checkPlaceholder_fr())
                         .stream();
    }
    
    @TestFactory
    public Stream<DynamicNode> testLanguageService_06() {
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.language_06")
                         .test("load page 1", c -> c.getHttpPage(Duration.ofSeconds(2),
                                                                 c.httpUrlBuilder()
                                                                  .path("/home.html")
                                                                  .build()))
                         .loadYwAssert()
                         .test("language loaded", languageLoaded())
                         .test("check FR label", checkUpdatedLabel_fr())
                         .test("check FR placeholder", checkPlaceholder_fr())
                         .test("check on translate", checkOnTranslate())
                         .testJsUnit("/TestTranslatorService.js", 
                                     List.of("test_translator_01",
                                             "test_translator_02",
                                             "test_translator_03"))
                         .stream();
    }
    
    private static List<String> expected_07 = new ArrayList<>();
	static {
		expected_07.add("<xml path=\"/lang_01.xml\" />");
		expected_07.add(" <xml path=\"/lang_02.xml\" />");
		expected_07.add(" <xml path=\"/lang_03.xml\" />");
		expected_07.add("<xml-cascade>");
		expected_07.add(" <xml path=\"/lang_02.xml\" />");
		expected_07.add(" <xml path=\"/lang_03.xml\" />");
		expected_07.add(" <xml path=\"/lang_01.xml\" />");
		expected_07.add("</xml-cascade>");
	}
	
    public static Consumer<TestContext> test_log_07 = testContext -> {
		testContext.seleniumService().executeAsyncScript("""
		      const callback = arguments[arguments.length - 1]
		      yojaWebApi.languageService
		                .log('/lang_01.xml')
		                .then(() => callback())
		""");
		List<String> actual = testContext.logs().stream().map(v -> v.message()).toList();
		assertEquals(expected_07, actual);
	};

    @TestFactory
    public Stream<DynamicNode> testLanguageService_07() {
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.language_07")
                         .startYojaWeb(ScriptOption.apply().saveLogs())
                         .test("test_log", test_log_07)
                         .stream();
    }
    
    private static List<String> expected_08 = new ArrayList<>();
	static {
		expected_08.add("<xml path=\"/lang_01.xml\" />");
		expected_08.add(" <xml path=\"/lang_02.xml\" />");
		expected_08.add("  <xml path=\"/lang_04.xml\" />");
		expected_08.add("   <xml path=\"/lang_02.xml\" recursive />");
		expected_08.add(" <xml path=\"/lang_03.xml\" />");
		expected_08.add("  <xml path=\"/lang_05.xml\" />");
		expected_08.add("   <xml path=\"/lang_02.xml\" />");
		expected_08.add("    <xml path=\"/lang_04.xml\" />");
		expected_08.add("     <xml path=\"/lang_02.xml\" recursive />");
		expected_08.add("   <xml path=\"/lang_03.xml\" recursive />");
		expected_08.add("  <xml path=\"/lang_06.xml\" />");
		expected_08.add("<xml-cascade>");
		expected_08.add(" <xml path=\"/lang_04.xml\" />");
		expected_08.add(" <xml path=\"/lang_02.xml\" />");
		expected_08.add(" <xml path=\"/lang_04.xml\" />");
		expected_08.add(" <xml path=\"/lang_02.xml\" />");
		expected_08.add(" <xml path=\"/lang_05.xml\" />");
		expected_08.add(" <xml path=\"/lang_06.xml\" />");
		expected_08.add(" <xml path=\"/lang_03.xml\" />");
		expected_08.add(" <xml path=\"/lang_01.xml\" />");
		expected_08.add("</xml-cascade>");
	}
	
    public static Consumer<TestContext> test_log_08 = testContext -> {
		testContext.seleniumService().executeAsyncScript("""
		      const callback = arguments[arguments.length - 1]
		      yojaWebApi.languageService
		                .log('/lang_01.xml')
		                .then(() => callback())
		""");
		List<String> actual = testContext.logs().stream().map(v -> v.message()).toList();
		assertEquals(expected_08, actual);
	};

    @TestFactory
    public Stream<DynamicNode> testLanguageService_08() {
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.language_08")
                         .startYojaWeb(ScriptOption.apply().saveLogs())
                         .test("test_log", test_log_08)
                         .stream();
    }
    
}
