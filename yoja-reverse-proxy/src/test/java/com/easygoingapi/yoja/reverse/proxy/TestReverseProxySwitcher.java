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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.easygoingapi.yoja.core.http.HttpProtocole;
import com.easygoingapi.yoja.core.http.HttpUrl;
import com.easygoingapi.yoja.http.client.WebSocketEngine;
import com.easygoingapi.yoja.reverse.proxy.ReverseProxyRule.Url;

import io.vertx.core.Handler;

public class TestReverseProxySwitcher {

    private static ReverseProxySwitcher newSwitcher(final Set<ReverseProxyRule> rules,
                                                    final Function<HttpUrl, HttpUrl> resolver,
                                                    final List<Handler<ReverseProxyResult>> onResolveActions) {
        return new ReverseProxySwitcher("admin-token",
                                        false,
                                        7777,
                                        new WebSocketEngine(),
                                        rules,
                                        resolver,
                                        onResolveActions);
    }

    private static HttpUrl url(final String host, final String path) {
        return HttpUrl.builder(host)
                      .protocol(HttpProtocole.http)
                      .port(7777)
                      .path(path)
                      .build();
    }

    @Test
    public void test_01_resolve_matches_rule() {
        final ReverseProxyRule rule =
            new ReverseProxyRule(Url.from("localhost", "/api"),
                                 Url.to(false, "backend.local").port(8080).build());
        final ReverseProxySwitcher switcher = newSwitcher(Set.of(rule), null, List.of());
        
        final ReverseProxyResult result = switcher.resolve(url("localhost", "/api/users"));

        assertNotNull(result);
        assertTrue(result.isResolved());
        assertEquals("backend.local", result.toUrl().host());
        assertEquals(Integer.valueOf(8080), result.toUrl().port());
        assertEquals("localhost", result.fromUrl().host());
        assertEquals("/api/users", result.fromUrl().path());
    }

    @Test
    public void test_02_resolve_no_match_no_resolver() {
        final ReverseProxyRule rule = 
            new ReverseProxyRule(Url.from("localhost", "/api"),
                                 Url.to(false, "backend.local").port(8080).build());
        final ReverseProxySwitcher switcher = newSwitcher(Set.of(rule), null, List.of());

        final ReverseProxyResult result = switcher.resolve(url("localhost", "/other/path"));

        assertNotNull(result);
        assertFalse(result.isResolved());
        assertEquals("localhost", result.fromUrl().host());
        assertEquals("/other/path", result.fromUrl().path());
    }

    @Test
    public void test_03_resolve_falls_back_to_resolver() {
        final HttpUrl fallback = url("fallback.local", "/anywhere");
        final Function<HttpUrl, HttpUrl> resolver = from -> fallback;
        final ReverseProxySwitcher switcher = newSwitcher(Set.of(), resolver, List.of());

        final ReverseProxyResult result = switcher.resolve(url("localhost", "/anything"));

        assertTrue(result.isResolved());
        assertEquals("fallback.local", result.toUrl().host());
        assertEquals("/anywhere", result.toUrl().path());
        assertEquals("localhost", result.fromUrl().host());
        assertEquals("/anything", result.fromUrl().path());
    }

    @Test
    public void test_04_resolver_returning_null_means_unresolved() {
        final Function<HttpUrl, HttpUrl> resolver = from -> null;
        final ReverseProxySwitcher switcher = newSwitcher(Set.of(), resolver, List.of());

        final ReverseProxyResult result = switcher.resolve(url("localhost", "/anything"));

        assertFalse(result.isResolved());
        assertEquals("localhost", result.fromUrl().host());
        assertEquals("/anything", result.fromUrl().path());
    }

    @Test
    public void test_05_longest_prefix_wins() {
        final ReverseProxyRule shortRule = 
            new ReverseProxyRule(Url.from("localhost", "/a"),
                                 Url.to(false, "short.backend").port(1111).build());
        final ReverseProxyRule longRule = 
            new ReverseProxyRule(Url.from("localhost", "/a/b"),
                                 Url.to(false, "long.backend").port(2222).build());
        final ReverseProxySwitcher switcher =
            newSwitcher(Set.of(shortRule, longRule), null, List.of());

        final ReverseProxyResult result = switcher.resolve(url("localhost", "/a/b/c"));

        assertTrue(result.isResolved());
        assertEquals("long.backend", result.toUrl().host());
        assertEquals(Integer.valueOf(2222), result.toUrl().port());
    }

    @Test
    public void test_06_short_path_falls_back_to_short_rule() {
        final ReverseProxyRule shortRule = 
            new ReverseProxyRule(Url.from("localhost", "/a"),
                                 Url.to(false, "short.backend").port(1111).build());
        final ReverseProxyRule longRule =
            new ReverseProxyRule(Url.from("localhost", "/a/b"),
                                 Url.to(false, "long.backend").port(2222).build());
        final ReverseProxySwitcher switcher =
            newSwitcher(Set.of(shortRule, longRule), null, List.of());

        final ReverseProxyResult result = switcher.resolve(url("localhost", "/a/x"));

        assertTrue(result.isResolved());
        assertEquals("short.backend", result.toUrl().host());
        assertEquals(Integer.valueOf(1111), result.toUrl().port());
    }

    @Test
    public void test_07_host_must_match() {
        final ReverseProxyRule rule = 
            new ReverseProxyRule(Url.from("localhost", "/api"),
                                 Url.to(false, "backend.local").port(8080).build());
        final ReverseProxySwitcher switcher = newSwitcher(Set.of(rule), null, List.of());

        final ReverseProxyResult result = switcher.resolve(url("other.host", "/api/users"));

        assertFalse(result.isResolved());
        assertEquals("other.host", result.fromUrl().host());
        assertEquals("/api/users", result.fromUrl().path());
    }

    @Test
    public void test_08_proxyPort_and_silent_reflect_ctor() {
        final ReverseProxySwitcher silent = new ReverseProxySwitcher("t", 
                                                                     true,
                                                                     9999, 
                                                                     new WebSocketEngine(), 
                                                                     Set.of(),
                                                                     null, 
                                                                     List.of());
        assertEquals(9999, silent.getProxyPort());
        assertTrue(silent.isSilent());

        final ReverseProxySwitcher loud = new ReverseProxySwitcher(
            "t", false, 1234, new WebSocketEngine(), Set.of(), null, List.of());
        assertEquals(1234, loud.getProxyPort());
        assertFalse(loud.isSilent());
    }

    @Test
    public void test_09_onResolve_actions_not_fired_by_protected_resolve() {
        final ReverseProxyRule rule = 
            new ReverseProxyRule(Url.from("localhost", "/api"),
                                 Url.to(false, "backend.local").port(8080).build());
        final AtomicReference<ReverseProxyResult> captured = new AtomicReference<>();
        final Handler<ReverseProxyResult> handler = captured::set;
        final ReverseProxySwitcher switcher = newSwitcher(Set.of(rule), null, List.of(handler));

        switcher.resolve(url("localhost", "/api/users"));

        assertNull(captured.get());
    }

    @Test
    public void test_10_toHttpUrl_static_parses_uri() {
        final HttpUrl parsed = ReverseProxySwitcher.toHttpUrl("http://localhost:8080/api/users?x=1");
        assertEquals("localhost", parsed.host());
        assertEquals(Integer.valueOf(8080), parsed.port());
        assertEquals("/api/users", parsed.path());
        assertEquals(HttpProtocole.http, parsed.protocol());
    }

    @Test
    public void test_11_toHttpUrl_invalid_throws() {
        ReverseProxyException thrown = null;
        try {
            ReverseProxySwitcher.toHttpUrl("not a uri");
        }
        catch (final ReverseProxyException e) {
            thrown = e;
        }
        assertNotNull(thrown);
        assertEquals("reverse proxy traffic uri syntax wrong", thrown.getMessage());
    }

    @Test
    public void test_12_resolve_returns_rule_result_when_rule_matched() {
        final ReverseProxyRule rule = 
            new ReverseProxyRule(Url.from("localhost", "/api"),
                                 Url.to(false, "backend.local").port(8080).build());
        final ReverseProxySwitcher switcher = newSwitcher(Set.of(rule), null, List.of());

        final ReverseProxyResult result = switcher.resolve(url("localhost", "/api/users"));

        assertTrue(result instanceof ReverseProxyRuleResult);
    }

    @Test
    public void test_13_resolve_returns_plain_result_when_resolver_used() {
        final HttpUrl fallback = url("fallback.local", "/anywhere");
        final ReverseProxySwitcher switcher = newSwitcher(Set.of(), from -> fallback, List.of());

        final ReverseProxyResult result = switcher.resolve(url("localhost", "/anything"));

        assertTrue(result.isResolved());
        assertFalse(result instanceof ReverseProxyRuleResult);
    }

}
