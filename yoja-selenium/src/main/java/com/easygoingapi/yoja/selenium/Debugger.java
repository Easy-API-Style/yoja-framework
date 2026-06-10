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
 * Marker exception used as a synthetic breakpoint target: when a developer is
 * stepping through a test in their IDE, setting a breakpoint on the
 * {@code throw} statement inside {@link #debugger()} provides a stable
 * pause-point that does not depend on a particular line of test code.
 * <p>
 * The exception is caught inline so {@link #debugger()} never propagates an
 * error — its only effect is the side-effect of triggering the IDE breakpoint
 * (and any catch-all logging hook that may be installed).
 */
public class Debugger extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** No-arg constructor; the exception carries no message of its own. */
    public Debugger() {
        super();
    }

    /**
     * Throws and immediately swallows a {@link Debugger} exception. Intended
     * to be used as a hook point: set a breakpoint on the {@code throw} below
     * (or on every {@code Debugger} exception in the IDE) to pause execution.
     */
    public static void debugger() {
        try {
            throw new Debugger();
        }
        catch (final Exception e) {}
    }

}
