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

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.openqa.selenium.TimeoutException;

import com.easygoingapi.yoja.web.test.util.TestConfig;

/**
 * YojaWeb's core: a controler failing in its constructor, the other sections built all the same; a page failing,
 * shown all the same; the paths -- another origin's left as it is, parameters and hash kept, a bare path resolved
 * from the file declaring it.
 */
public class TestYojaWebCore {

    @TestFactory
    public Stream<DynamicNode> testYojaWebCore_01() {
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.core_01")
                         .test("load page", c -> c.getHttpPage(Duration.ofSeconds(2),
                                                               c.httpUrlBuilder().path("/home.html").build()))
                         .loadYwAssert()
                         .testJsUnit("/TestCore.js",
                                     List.of("test_01",
                                             "test_02",
                                             "test_03"))
                         .stream();
    }

    @TestFactory
    public Stream<DynamicNode> testYojaWebCore_02() {
        return TestConfig.initialize()
                         .webResource("com.easygoingapi.yoja.web.test.core_01")
                         .test("load page", c -> {
                             try {
                                 c.getHttpPage(Duration.ofSeconds(2), c.httpUrlBuilder().path("/missing.html").build());
                             }
                             catch (final TimeoutException neverReady) {
                                 // failing, the page never gets ready: what is looked at is that it is shown
                             }
                         })
                         .test("the page failing is shown all the same", c -> assertEquals("still shown",
                                 c.seleniumService().repeatScript(Duration.ofSeconds(10), """
                                     return document.body.style.display === '' && document.querySelector('.absent')
                                            ? document.querySelector('.absent').textContent.trim() : null;
                                 """)))
                         .stream();
    }

}
