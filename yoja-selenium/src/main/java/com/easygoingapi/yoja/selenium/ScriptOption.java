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
package com.easygoingapi.yoja.selenium;

/**
 * Two-flag option bundle passed to {@code startJavascript}/{@code startYojaWeb}
 * variants on {@link TestBuilder} and to {@link SeleniumService#reload(ScriptOption)}.
 * <p>
 * The two flags control which optional JS helpers are loaded into the page
 * after the navigation completes:
 * <ul>
 *   <li>{@link #wyLogger} — load the {@code ywLogger.js} helper so subsequent
 *       browser logs can be retrieved via {@link SeleniumService#logs()};</li>
 *   <li>{@link #ywAssert} — load the {@code ywAssert.js} helper so test code
 *       running inside the page can use the {@code ywAssert} API.</li>
 * </ul>
 * The fields are package-private; toggle them through the fluent setters and
 * obtain an instance via {@link #apply()}.
 */
public class ScriptOption {

    /** When {@code true}, install the browser-side log capture helper. */
    boolean wyLogger;
    /** When {@code true}, install the browser-side assertion helper. */
    boolean ywAssert;

    /** Package-visible; use {@link #apply()} from outside the package. */
    ScriptOption() {
        super();
    }

    /**
     * Enables installation of the browser-side log capture helper.
     *
     * @return this option for chaining
     */
    public ScriptOption saveLogs() {
        this.wyLogger = true;
        return this;
    }

    /**
     * Enables installation of the browser-side {@code ywAssert} helper.
     *
     * @return this option for chaining
     */
    public ScriptOption loadYwAssert() {
        this.ywAssert = true;
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(ScriptOption.class.getSimpleName());
        result.append(" [wyLogger=");
        result.append(wyLogger);
        result.append(", ywAssert=");
        result.append(ywAssert);
        result.append("]");
        return result.toString();
    }

    /*
     *
     * STATIC
     *
     */
    /**
     * Returns a fresh option bundle with both flags off.
     *
     * @return a fresh option bundle with both flags off
     */
    public static ScriptOption apply() {
        return new ScriptOption();
    }

}
