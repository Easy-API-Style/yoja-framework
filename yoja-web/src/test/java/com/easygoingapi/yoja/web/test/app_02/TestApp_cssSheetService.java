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

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

public class TestApp_cssSheetService {
    
    @TestFactory
    public Stream<DynamicNode> factory() {
        return ResourceUtil.initialize_app()
                           .webResource("com.easygoingapi.yoja.web.test.tool", "/tool")
                           .testJsUnit("/TestCssSheetService.js", 
                                       List.of("test_cssStyleSheet_01",
                                               "test_cssStyleSheet_02",
                                               "test_cssStyleSheet_03",
                                               "test_cssStyleSheet_04"))
                            .stream();
    }
    
}
