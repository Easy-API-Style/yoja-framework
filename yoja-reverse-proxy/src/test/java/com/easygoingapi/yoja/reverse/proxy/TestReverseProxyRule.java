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
package com.easygoingapi.yoja.reverse.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.easygoingapi.yoja.reverse.proxy.ReverseProxyException;
import com.easygoingapi.yoja.reverse.proxy.ReverseProxyRule;
import com.easygoingapi.yoja.reverse.proxy.ReverseProxyRule.Url;
import com.easygoingapi.yoja.reverse.proxy.ReverseProxyRule.Url.From;

public class TestReverseProxyRule {
    
    @Test
    public void test_1() {
        final Url.To to = Url.to(false, "localhost")
                             .cutsPathWith("/seb")
                             .port(7777, 5555)
                             .build();
        final ReverseProxyRule reverseProxyRule = new ReverseProxyRule(Url.from("localhost", "/seb"), to);
        assertEquals("/tom", to.apply("/seb/tom"));
    }
    
    @Test
    public void test_2() {
        final Url.To to = Url.to(false, "localhost")
                             .cutsPathWith("/seb")
                             .startsPathWith("/jack")
                             .build();
        final ReverseProxyRule reverseProxyRule = new ReverseProxyRule(Url.from("localhost", "/seb"), to);
        assertEquals("/jack/tom", to.apply("/seb/tom"));
    }
    
    @Test
    public void test_3() {
        final Url.To to = Url.to(false, "localhost")
                             .startsPathWith("/jack")
                             .build();
        final ReverseProxyRule reverseProxyRule = new ReverseProxyRule(Url.from("localhost", "/seb"), to);
        assertEquals("/jack/seb/tom", to.apply("/seb/tom"));
    }
    
    @Test
    public void test_4() {
        Exception exception = null;
        try {
            final Url.To to = Url.to(false, "localhost")
                                 .cutsPathWith("/jack/tom")
                                 .build();
            final ReverseProxyRule reverseProxyRule = new ReverseProxyRule(Url.from("localhost", "/seb"), to);
        } 
        catch (final Exception e) {
            exception = e;
        }
        assertEquals(ReverseProxyException.class, exception.getClass());
        assertEquals("reserve proxy rule error from:"
                   + " From [host=localhost, path=/seb]"
                   + " to: To [ssl=false, host=localhost, ports=null, cutsPathWith=/jack/tom, startsPathWith=null]",
                    exception.getMessage());
    }
    
    @Test
    public void test_5() {
        Exception exception = null;
        try {
            final Url.To to = Url.to(false, "localhost")
                                 .cutsPathWith("/seb/jack/tom")
                                 .build();
            final ReverseProxyRule reverseProxyRule = new ReverseProxyRule(Url.from("localhost", "/seb"), to);
        } 
        catch (final Exception e) {
            exception = e;
        }
        assertEquals(ReverseProxyException.class, exception.getClass());
        assertEquals("reserve proxy rule error from:"
                   + " From [host=localhost, path=/seb]"
                   + " to: To [ssl=false, host=localhost, ports=null, cutsPathWith=/seb/jack/tom, startsPathWith=null]", 
                     exception.getMessage());
    }
    
    @Test
    public void test_6() {
        final Url.To to = Url.to(false, "localhost")
                             .cutsPathWith("/seb/jack/tom")
                             .build();
        final ReverseProxyRule reverseProxyRule = new ReverseProxyRule(Url.from("localhost"), to);
        assertEquals("/", to.apply("/seb/jack/tom"));   
    }
    
    @Test
    public void test_7() {
        final Url.To to = Url.to(false, "localhost").build();
        final ReverseProxyRule reverseProxyRule = new ReverseProxyRule(Url.from("localhost", "/seb"), to);
        assertEquals("/seb/tom", to.apply("/seb/tom"));
    }
    
    @Test
    public void test_8() {
        final Url.To to = Url.to(false, "localhost").build();
        final ReverseProxyRule reverseProxyRule = new ReverseProxyRule(Url.from("localhost"), to);
        assertEquals("/seb/tom", to.apply("/seb/tom"));
    }
    
    @Test
    public void test_9() {
        Exception exception = null;
        try {
            final Url.To to = Url.to(false, "localhost")
                                 .cutsPathWith("seb")
                                 .build();
        }
        catch (final Exception e) {
            exception = e;
        }
        assertEquals(ReverseProxyException.class, exception.getClass());
        assertEquals("path must be absolute", exception.getMessage());
    }
    
    @Test
    public void test_10() {
        Exception exception = null;
        try {
            final Url.To to = Url.to(false, "localhost")
                                 .startsPathWith("/seb")
                                 .build();
            final ReverseProxyRule reverseProxyRule = new ReverseProxyRule(Url.from("localhost", "seb"), to);
        }
        catch (final Exception e) {
            exception = e;
        }
        assertEquals(ReverseProxyException.class, exception.getClass());
        assertEquals("path must be absolute", exception.getMessage());
    }
    
    @Test
    public void test_11() {
        Exception exception = null;
        try {
            final From from = Url.from("localhost", "seb");
        }
        catch (final Exception e) {
            exception = e;
        }
        assertEquals(ReverseProxyException.class, exception.getClass());
        assertEquals("path must be absolute", exception.getMessage());
    }
    
    @Test
    public void test_12() {
        Exception exception = null;
        try {
            final Url.To to = Url.to(false, "localhost")
                                 .cutsPathWith("/seb/jack/tom")
                                 .build();
            final ReverseProxyRule reverseProxyRule = new ReverseProxyRule(Url.from("localhost"), to);
            assertEquals("/", to.apply("/seb"));
        } 
        catch (final Exception e) {
            exception = e;
        }
        assertEquals(ReverseProxyException.class, exception.getClass());
        assertEquals("to cut: the path must begin with /seb/jack/tom -> /seb", exception.getMessage());
    }

}
