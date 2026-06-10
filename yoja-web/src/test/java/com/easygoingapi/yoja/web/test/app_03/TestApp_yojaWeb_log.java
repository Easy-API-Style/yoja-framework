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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import com.easygoingapi.yoja.selenium.TestContext;

public class TestApp_yojaWeb_log {
	
	private static List<String> expected = new ArrayList<>();
	static {
		expected.add("------");
		expected.add("<yw-section index=\"0\" deep=\"0\" inside=\"/home.html\" tag=\"body\" id=\"1\" controler=\"/home.ctl.js\" css=\"/home.css\" />");
		expected.add("  <yw-section index=\"0\" deep=\"1\" inside=\"/home.html\" tag=\"div\" id=\"2\" controler=\"/user/user_1.ctl.js\" language=\"/lang.xml\" slot=\"/user/user.html\" css />");
		expected.add("    <yw-section index=\"0\" deep=\"2\" inside=\"/user/user.html\" tag=\"div\" id=\"5\" language=\"/lang.xml\" css />");
		expected.add("  <yw-section index=\"1\" deep=\"1\" inside=\"/home.html\" tag=\"div\" id=\"3\" controler=\"/user/user_2.ctl.js\" language=\"/lang.xml\" slot=\"/user/user.html\" css=\"/user/user_2.css\" />");
		expected.add("    <yw-section index=\"0\" deep=\"2\" inside=\"/user/user.html\" tag=\"div\" id=\"5\" language=\"/lang.xml\" css />");
		expected.add("######");
		expected.add("<yw-section index=\"0\" deep=\"0\" inside=\"/home.html\" tag=\"body\" id=\"1\" controler=\"/home.ctl.js\" css=\"/home.css\" >");
		expected.add(" <css path=\"/home.css\" />");
		expected.add("</yw-section>");
		expected.add("------");
		expected.add("<yw-section index=\"0\" deep=\"0\" inside=\"/home.html\" tag=\"div\" id=\"2\" controler=\"/user/user_1.ctl.js\" language=\"/lang.xml\" slot=\"/user/user.html\" css />");
		expected.add("  <yw-section index=\"0\" deep=\"1\" inside=\"/user/user.html\" tag=\"div\" id=\"5\" language=\"/lang.xml\" css />");
		expected.add("######");
		expected.add("<yw-section index=\"0\" deep=\"0\" inside=\"/home.html\" tag=\"div\" id=\"2\" controler=\"/user/user_1.ctl.js\" language=\"/lang.xml\" slot=\"/user/user.html\" css >");
		expected.add(" <css path=\"/home.css\" inherited />");
		expected.add("</yw-section>");
		expected.add("------");
		expected.add("<yw-section index=\"0\" deep=\"0\" inside=\"/user/user.html\" tag=\"div\" id=\"5\" language=\"/lang.xml\" css />");
		expected.add("######");
		expected.add("<yw-section index=\"0\" deep=\"0\" inside=\"/user/user.html\" tag=\"div\" id=\"5\" language=\"/lang.xml\" css >");
		expected.add(" <css path=\"/home.css\" inherited />");
		expected.add("</yw-section>");
		expected.add("------");
		expected.add("<yw-section index=\"0\" deep=\"0\" inside=\"/home.html\" tag=\"div\" id=\"3\" controler=\"/user/user_2.ctl.js\" language=\"/lang.xml\" slot=\"/user/user.html\" css=\"/user/user_2.css\" />");
		expected.add("  <yw-section index=\"0\" deep=\"1\" inside=\"/user/user.html\" tag=\"div\" id=\"5\" language=\"/lang.xml\" css />");
		expected.add("######");
		expected.add("<yw-section index=\"0\" deep=\"0\" inside=\"/home.html\" tag=\"div\" id=\"3\" controler=\"/user/user_2.ctl.js\" language=\"/lang.xml\" slot=\"/user/user.html\" css=\"/user/user_2.css\" >");
		expected.add(" <css path=\"/user/user_2.css\" />");
		expected.add("  <css path=\"/main.css\" />");
		expected.add(" <css-cascade>");
		expected.add("  <css path=\"/main.css\" />");
		expected.add("  <css path=\"/user/user_2.css\" />");
		expected.add(" </css-cascade>");
		expected.add("</yw-section>");
		expected.add("------");
		expected.add("<yw-section index=\"0\" deep=\"0\" inside=\"/user/user.html\" tag=\"div\" id=\"5\" language=\"/lang.xml\" css />");
		expected.add("######");
		expected.add("<yw-section index=\"0\" deep=\"0\" inside=\"/user/user.html\" tag=\"div\" id=\"5\" language=\"/lang.xml\" css >");
		expected.add(" <css path=\"/user/user_2.css\" inherited />");
		expected.add("  <css path=\"/main.css\" />");
		expected.add(" <css-cascade>");
		expected.add("  <css path=\"/main.css\" />");
		expected.add("  <css path=\"/user/user_2.css\" />");
		expected.add(" </css-cascade>");
		expected.add("</yw-section>");
	}
    
	public static Consumer<TestContext> test_log = testContext -> {
		testContext.seleniumService().executeScript("""
		     yojaWeb.sectionService.walk(document, s => {
		         console.log("------")
		         s.log()
		         console.log("######")
		         s.logCss()
		     })
		""");
		List<String> actual = testContext.logs().stream().map(v -> v.message()).toList();
		assertEquals(expected, actual);
	};

    @TestFactory
    public Stream<DynamicNode> testSectionLog() {
         return ResourceUtil.initialize_app()
                            .saveLogs()
                            .test("test_log", test_log)
                            .stream();
    }
    
}
