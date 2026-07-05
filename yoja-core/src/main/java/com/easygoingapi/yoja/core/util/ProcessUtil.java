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
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
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

    /**
     * Executes an OS command, streaming its output to the provided consumers,
     * and waits at most {@code timeout} for it to finish.
     * <p>
     * Output is drained on separate threads so the process cannot dead-lock on
     * a full pipe while we wait. When the timeout elapses the process is
     * forcibly destroyed and a {@link YojaAppException} is thrown.
     *
     * @param command the command and its arguments
     * @param console consumer that receives each line of standard output
     * @param error   consumer that receives each line of standard error
     * @param timeout maximum time to wait for the process to complete
     * @return the process exit code
     * @throws YojaAppException if the process cannot be started, times out, or is interrupted
     */
    public static int execute(final List<String> command,
                              final Consumer<String> console,
                              final Consumer<String> error,
                              final Duration timeout) {
        Process process = null;
        try {
            process = new ProcessBuilder().command(command).start();

            final Thread consoleThread = streamAsync(process.getInputStream(), console);
            final Thread errorThread = streamAsync(process.getErrorStream(), error);

            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new YojaAppException("execute command timed out after " + timeout
                                         + ": " + String.join(" ", command));
            }
            // let the reader threads drain the remaining output
            consoleThread.join(1000);
            errorThread.join(1000);
            return process.exitValue();
        }
        catch (final YojaAppException e) {
            throw e;
        }
        catch (final InterruptedException e) {
            if (process != null) {
                process.destroyForcibly();
            }
            Thread.currentThread().interrupt();
            throw new YojaAppException("execute command interrupted " + String.join(" ", command), e);
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

    /**
     * Drains {@code inputStream} on a dedicated daemon thread, forwarding each
     * line to {@code handler} until the stream is closed.
     */
    private static Thread streamAsync(final InputStream inputStream,
                                      final Consumer<String> handler) {
        final Thread thread = new Thread(() -> {
            try {
                stream(inputStream, handler);
            }
            catch (final IOException e) {
                // stream closed when the process ends: nothing left to read
            }
        });
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

}
