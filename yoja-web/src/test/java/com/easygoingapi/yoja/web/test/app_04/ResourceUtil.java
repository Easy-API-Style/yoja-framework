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
package com.easygoingapi.yoja.web.test.app_04;

import java.util.function.Consumer;

import com.easygoingapi.yoja.selenium.TestBuilder;
import com.easygoingapi.yoja.selenium.TestContext;
import com.easygoingapi.yoja.web.test.util.TestConfig;

public class ResourceUtil {
    
    public static TestBuilder initialize_app() {
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.app_04")
                         .test("getYojaWebPage", getYojaWebPage)
                         .loadYwAssert();
    }

    public static Consumer<TestContext> getYojaWebPage = testContext -> {
        testContext.getHttpPage("/home.html");
    };

    
}
