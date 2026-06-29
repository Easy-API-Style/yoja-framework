package com.easygoingapi.yoja.selenium;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

public class TestYwAssert {

    @TestFactory
    Stream<DynamicNode> jsUnitDemo() {
        return TestBuilder.builder()
                          .browser(Browser.builder(Browser.CHROME)
                                          .mode(Browser.Mode.HEADLESS)
                                          .build())
                          .browser(Browser.builder(Browser.FIREFOX)
                                          .mode(Browser.Mode.HEADLESS)
                                          .build())
                          .contentType("js", "text/javascript")
                          .webResource("com/easygoingapi/yoja/selenium/js")
                          .startJavascript()
                          .loadYwAssert()
                          .testJsUnit("/jsUnitAssertTest.js",
                                      List.of("test_assertEquals_primitives",
                                              "test_assertEquals_deepStructures",
                                              "test_assertEquals_keyOrderIndependent",
                                              "test_assertEquals_specialValues",
                                              "test_assertEquals_failsOnDifference",
                                              "test_assertEquals_boxedPrimitives",
                                              "test_assertTrue",
                                              "test_assertFalse",
                                              "test_assertNull",
                                              "test_assertNotNull",
                                              "test_assertUndefined",
                                              "test_assertNotUndefined",
                                              "test_assertArrayEquals",
                                              "test_assertArrayEquals_deepElements",
                                              "test_assertArrayEquals_failsOnDifference"))
                          .stream();
    }
    
}
