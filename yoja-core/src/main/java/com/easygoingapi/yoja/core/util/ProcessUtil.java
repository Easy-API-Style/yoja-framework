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
package com.easygoingapi.yoja.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.function.Consumer;

import com.easygoingapi.yoja.core.YojaAppException;

/**
 * Utility class for launching and monitoring external OS processes.
 */
public class ProcessUtil {

    /** Not instantiable. */
    private ProcessUtil() {}

    /**
     * Executes an OS command and streams its output to the provided consumers.
     *
     * @param command the command and its arguments
     * @param console consumer that receives each line of standard output
     * @param error   consumer that receives each line of standard error
     * @param wait    if {@code true}, blocks until the process completes and returns its exit code;
     *                if {@code false}, returns {@code null} immediately after starting the process
     * @return the process exit code, or {@code null} if {@code wait} is {@code false}
     * @throws com.easygoingapi.yoja.core.YojaAppException if the process cannot be started
     */
    public static Integer execute(final List<String> command,
                                  final Consumer<String> console,
                                  final Consumer<String> error,
                                  final boolean wait) {
        try {
            final ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(command);
            final Process process = processBuilder.start();

            stream(process.getInputStream(), console);
            stream(process.getErrorStream(), error);
            if (wait) {
                return process.waitFor();
            }
            return null;
        } 
        catch (final Exception e) {
            throw new YojaAppException("execute command faided " + String.join(" ", command), e);
        }
    }

    private static void stream(final InputStream inputStream, 
                               final Consumer<String> handler) throws IOException {
        final BufferedReader consoleReader = new BufferedReader(new InputStreamReader(inputStream));
        String consoleLine;
        while ((consoleLine = consoleReader.readLine()) != null) {
            handler.accept(consoleLine);
        }
    }

}
