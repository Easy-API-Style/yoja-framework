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
package com.easygoingapi.yoja.web.test.app_03;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import com.easygoingapi.yoja.selenium.TestContext;

public class Test_app_03 {

    public static Consumer<TestContext> test_config = testContext -> {
        final String result = testContext.seleniumService()
                                         .executeScript(""" 
            return JSON.stringify(yojaWeb.config)
        """);
        assertEquals("{\"version\":\"0.0.0\"}", 
                     result);
    };
    
    public static Consumer<TestContext> test_controlers = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript(""" 
            return yojaWeb.controlerService.find()
                          .flatMap(c => c.constructor.name)
        """);
        assertEquals(List.of("HomeControler", "UserControler_1", "UserControler_2"), result);
    };
    
    public static Consumer<TestContext> test_controler_1 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript(""" 
            return yojaWeb.controlerService.find({predicate: c => c.constructor.name === 'UserControler_1'})
                          .flatMap(c => c.constructor.name)
        """);
        assertEquals(List.of("UserControler_1"), result);
    };
    
    public static Consumer<TestContext> test_controler_2 = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript(""" 
            return yojaWeb.controlerService.find({predicate: c => c.constructor.name === 'UserControler_2'})
                          .flatMap(c => c.constructor.name)
        """);
        assertEquals(List.of("UserControler_2"), result);
    };
    
    
    public static Consumer<TestContext> test_user_1_label_id = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript(""" 
            return yojaWeb.findTags('.user-1 .id-section label')
                          .flatMap(v => window.getComputedStyle(v).backgroundColor)
        """);
        assertEquals(List.of("rgb(0, 255, 0)", "rgb(0, 255, 0)"), result);
    };

    public static Consumer<TestContext> test_user_1_label_address = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript(""" 
            return yojaWeb.findTags('.user-1 .address-section label')
                          .flatMap(v => window.getComputedStyle(v).backgroundColor)
        """);
        assertEquals(List.of("rgb(255, 165, 0)", "rgb(255, 165, 0)", "rgb(255, 165, 0)"), result);
    };
    
    public static Consumer<TestContext> test_user_2_label_id = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript(""" 
            return yojaWeb.findTags('.user-2 .id-section label')
                          .flatMap(v => window.getComputedStyle(v).backgroundColor)
        """);
        assertEquals(List.of("rgb(255, 192, 203)", "rgb(255, 192, 203)"), result);
    };

    public static Consumer<TestContext> test_user_2_label_address = testContext -> {
        final List<String> result = testContext.seleniumService()
                                               .executeScript(""" 
            return yojaWeb.findTags('.user-2 .address-section label')
                          .flatMap(v => window.getComputedStyle(v).backgroundColor)
        """);
        assertEquals(List.of("rgb(255, 192, 203)", "rgb(255, 192, 203)", "rgb(255, 192, 203)"), result);
    };

    
    @TestFactory
    public Stream<DynamicNode> factory() {
         return ResourceUtil.initialize_app()
                          
                            .test("test_config", test_config)
                            .test("test_controlers", test_controlers)
                            .test("test_controler_1", test_controler_1)
                            .test("test_controler_2", test_controler_2)
                            
                            .test("test_user_1_label_id", test_user_1_label_id)
                            .test("test_user_1_label_address", test_user_1_label_address)
                            
                            .test("test_user_2_label_id", test_user_2_label_id)
                            .test("test_user_2_label_address", test_user_2_label_address)
                            
                            .stream();
    }

}
