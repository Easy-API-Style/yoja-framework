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
package com.easygoingapi.yoja.web.test.app_01;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import com.easygoingapi.yoja.selenium.YojaTestContext;

/**
 * Tests {@code yojaWeb.appendTag(fromTag, tags)} / {@code prependTag(fromTag, tags)}.
 * <p>
 * Each test appends/prepends a fresh {@code <label yw-i18n="familyName">} to the
 * user section and, once the returned promise resolves, checks that:
 * <ul>
 *   <li>the node landed at the right position (end for append, start for prepend);</li>
 *   <li>{@code languageService.refreshFrom(...)} ran — the i18n key was translated
 *       to the default language (fr): {@code "Nom De Famille"};</li>
 *   <li>{@code style.display} was restored (revealed) after the call.</li>
 * </ul>
 */
public class TestApp_yojaWeb_appendTag {

    public static Consumer<YojaTestContext> test_appendTag = testContext -> {
        final Map<String, Object> result = testContext.seleniumService()
                                                      .executeAsyncScript(Duration.ofSeconds(10), """
            const callback = arguments[arguments.length - 1]
            try {
                const fromTag = yojaWeb.firstTag('.user-section')
                const el = document.createElement('label')
                el.setAttribute('yw-i18n', 'familyName')
                yojaWeb.appendTag(fromTag, el)
                       .then(() => {
                           const root = fromTag.shadowRoot || fromTag
                           callback({ text: el.textContent,
                                      display: el.style.display,
                                      positioned: root.lastElementChild === el })
                       })
                       .catch(e => callback({ error: '' + (e && e.message ? e.message : e) }))
            }
            catch (e) { callback({ error: '' + e }) }
        """);
        assertNull(result.get("error"), "appendTag failed: " + result.get("error"));
        // languageService.refreshFrom applied -> the i18n key is translated (fr)
        assertEquals("Nom De Famille", result.get("text"));
        // node appended at the end
        assertEquals(Boolean.TRUE, result.get("positioned"));
        // display restored (revealed) after the call
        assertEquals("", result.get("display"));
    };

    public static Consumer<YojaTestContext> test_prependTag = testContext -> {
        final Map<String, Object> result = testContext.seleniumService()
                                                      .executeAsyncScript(Duration.ofSeconds(10), """
            const callback = arguments[arguments.length - 1]
            try {
                const fromTag = yojaWeb.firstTag('.user-section')
                const el = document.createElement('label')
                el.setAttribute('yw-i18n', 'familyName')
                yojaWeb.prependTag(fromTag, el)
                       .then(() => {
                           const root = fromTag.shadowRoot || fromTag
                           callback({ text: el.textContent,
                                      display: el.style.display,
                                      positioned: root.firstElementChild === el })
                       })
                       .catch(e => callback({ error: '' + (e && e.message ? e.message : e) }))
            }
            catch (e) { callback({ error: '' + e }) }
        """);
        assertNull(result.get("error"), "prependTag failed: " + result.get("error"));
        assertEquals("Nom De Famille", result.get("text"));
        // node prepended at the start
        assertEquals(Boolean.TRUE, result.get("positioned"));
        assertEquals("", result.get("display"));
    };

    /** Appends a node that is already {@code display:none}: it must stay hidden. */
    public static Consumer<YojaTestContext> test_appendTag_keeps_already_hidden = testContext -> {
        final Map<String, Object> result = testContext.seleniumService()
                                                      .executeAsyncScript(Duration.ofSeconds(10), """
            const callback = arguments[arguments.length - 1]
            try {
                const fromTag = yojaWeb.firstTag('.user-section')
                const el = document.createElement('label')
                el.setAttribute('yw-i18n', 'familyName')
                el.style.display = 'none'
                yojaWeb.appendTag(fromTag, el)
                       .then(() => callback({ display: el.style.display, text: el.textContent }))
                       .catch(e => callback({ error: '' + (e && e.message ? e.message : e) }))
            }
            catch (e) { callback({ error: '' + e }) }
        """);
        assertNull(result.get("error"), "appendTag failed: " + result.get("error"));
        // still translated
        assertEquals("Nom De Famille", result.get("text"));
        // a node that was already hidden must stay hidden
        assertEquals("none", result.get("display"));
    };

    /**
     * appendTag on a tag located <em>inside</em> a section that has a path
     * ({@code .id-section} lives in {@code .user-section}, loaded from
     * {@code /user/user.cpt.html}). Verifies the section path is derived and
     * that the yojaWeb pipeline (i18n / display) still runs on the added node.
     */
    public static Consumer<YojaTestContext> test_appendTag_in_section_with_path = testContext -> {
        final Map<String, Object> result = testContext.seleniumService()
                                                      .executeAsyncScript(Duration.ofSeconds(10), """
            const callback = arguments[arguments.length - 1]
            try {
                const fromTag = yojaWeb.firstTag('.id-section')
                const parentPaths = yojaWeb.sectionService.parents(fromTag).map(s => s.path)
                const el = document.createElement('label')
                el.setAttribute('yw-i18n', 'familyName')
                yojaWeb.appendTag(fromTag, el)
                       .then(() => callback({ parentPaths: JSON.stringify(parentPaths),
                                              text: el.textContent,
                                              display: el.style.display,
                                              positioned: fromTag.lastElementChild === el }))
                       .catch(e => callback({ error: '' + (e && e.message ? e.message : e),
                                              parentPaths: JSON.stringify(parentPaths) }))
            }
            catch (e) { callback({ error: '' + e }) }
        """);
        assertNull(result.get("error"), "appendTag failed: " + result.get("error"));
        // the fromTag is inside a section that has a path
        assertEquals("[\"/user/user.cpt.html\",\"/home.html\"]", result.get("parentPaths"));
        // pipeline ran on the added node (refreshFrom + reveal)
        assertEquals("Nom De Famille", result.get("text"));
        assertEquals("", result.get("display"));
        assertEquals(Boolean.TRUE, result.get("positioned"));
    };

    /**
     * appendTag derives the path via {@code closest} (which includes the tag's
     * own section) — not {@code parents} (ancestors only). This matters when
     * {@code fromTag} is a section host: its own section path must be used.
     */
    public static Consumer<YojaTestContext> test_closest_is_used_for_section_host = testContext -> {
        final Map<String, Object> result = testContext.seleniumService()
                                                      .executeScript("""
            const host = yojaWeb.firstTag('.user-section')
            return {
                parents: JSON.stringify(yojaWeb.sectionService.parents(host).map(s => s.path)),
                closest: yojaWeb.sectionService.closest(host).path
            }
        """);
        // closest returns the host's OWN section path...
        assertEquals("/user/user.cpt.html", result.get("closest"),
                     "closest must include the host's own section path");
        // ...whereas parents skips it and would return only the ancestor (the former bug).
        assertEquals("[\"/home.html\"]", result.get("parents"));
    };

    /**
     * appendTag with a DocumentFragment: its children must be added, and the
     * language pipeline ({@code updateFrom} flattening the fragment to its
     * children) must translate each one.
     */
    public static Consumer<YojaTestContext> test_appendTag_with_fragment = testContext -> {
        final Map<String, Object> result = testContext.seleniumService()
                                                      .executeAsyncScript(Duration.ofSeconds(10), """
            const callback = arguments[arguments.length - 1]
            try {
                const fromTag = yojaWeb.firstTag('.id-section')
                const fragment = document.createDocumentFragment()
                const a = document.createElement('label'); a.setAttribute('yw-i18n', 'familyName')
                const b = document.createElement('label'); b.setAttribute('yw-i18n', 'firstName')
                fragment.appendChild(a)
                fragment.appendChild(b)
                yojaWeb.appendTag(fromTag, fragment)
                       .then(() => callback({ aText: a.textContent, bText: b.textContent,
                                              aInDom: fromTag.contains(a), bInDom: fromTag.contains(b) }))
                       .catch(e => callback({ error: '' + (e && e.message ? e.message : e) }))
            }
            catch (e) { callback({ error: '' + e }) }
        """);
        assertNull(result.get("error"), "appendTag with fragment failed: " + result.get("error"));
        assertEquals(Boolean.TRUE, result.get("aInDom"));
        assertEquals(Boolean.TRUE, result.get("bInDom"));
        // updateFrom flattened the fragment -> each child translated (fr)
        assertEquals("Nom De Famille", result.get("aText"));
        assertEquals("Prénom", result.get("bText"));
    };

    @TestFactory
    public Stream<DynamicNode> factory() {
        return ResourceUtil.initialize_app()
                           .test("appendTag", test_appendTag)
                           .test("prependTag", test_prependTag)
                           .test("appendTag_keeps_already_hidden", test_appendTag_keeps_already_hidden)
                           .test("appendTag_in_section_with_path", test_appendTag_in_section_with_path)
                           .test("closest_is_used_for_section_host", test_closest_is_used_for_section_host)
                           .test("appendTag_with_fragment", test_appendTag_with_fragment)
                           .stream();
    }

}
