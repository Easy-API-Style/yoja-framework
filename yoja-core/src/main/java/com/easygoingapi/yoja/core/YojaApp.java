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
package com.easygoingapi.yoja.core;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.core.worker.Worker;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

/**
 * Static façade managing the single Vert.x instance shared by the whole Yoja
 * framework. {@code YojaApp} is the framework's lifecycle entry point — every
 * higher-level module ({@code yoja-http-server}, {@code yoja-http-client},
 * {@code yoja-reverse-proxy}, {@code yoja-selenium}) ultimately relies on the
 * {@link Vertx} instance returned by {@link #vertx()}.
 *
 * <p><b>Default options.</b> A {@link VertxOptions} singleton is pre-built in
 * a static initializer with the following defaults; tweak them through
 * {@link #builder()} or by passing your own {@code VertxOptions} to
 * {@link #start(VertxOptions)}:
 * <ul>
 *   <li>{@code maxEventLoopExecuteTime} = 4 seconds</li>
 *   <li>{@code maxWorkerExecuteTime} = 5 minutes</li>
 *   <li>{@code workerPoolSize} = {@link Runtime#availableProcessors()}</li>
 * </ul>
 *
 * <p><b>Lifecycle.</b> Typical use:
 * <pre>{@code
 * YojaApp.start();                 // start with default options
 * try {
 *     // ... build server / client, schedule timers, etc.
 *     YojaApp.awaitStop();         // block until something calls stop()
 * }
 * finally {
 *     YojaApp.stop();              // close Vert.x and release all workers
 * }
 * }</pre>
 *
 * <p><b>Implicit start.</b> {@link #vertx()} lazily starts the framework if it
 * has not been started yet — useful for short-lived scripts but easy to miss
 * in larger applications. Prefer calling {@link #start()} explicitly so the
 * startup point is visible in the code.
 *
 * <p><b>Thread model.</b> Two helpers distinguish the calling context:
 * {@link #isEventLoopThread()} returns {@code true} when running on a Vert.x
 * event-loop thread (do not block here), {@link #isWorkerThread()} returns
 * {@code true} otherwise (safe to block).
 *
 * @see Vertx
 * @see Worker
 */
public class YojaApp {

    /** Not instantiable. */
    private YojaApp() {}

    private final static Logger LOGGER = LoggerFactory.getLogger(YojaApp.class);

    private static VertxOptions vertxOptions = new VertxOptions();
    private static Vertx vertx;

    static {
        vertxOptions.setMaxEventLoopExecuteTimeUnit(TimeUnit.SECONDS);
        vertxOptions.setMaxEventLoopExecuteTime(4);
        vertxOptions.setMaxWorkerExecuteTimeUnit(TimeUnit.MINUTES);
        vertxOptions.setMaxWorkerExecuteTime(5);
        vertxOptions.setWorkerPoolSize(Runtime.getRuntime()
                                              .availableProcessors());
    }

    /**
     * Returns the live, mutable {@link VertxOptions} singleton the framework
     * uses to (re)start Vert.x.
     *
     * <p>Mutating this object affects subsequent {@link #start()} /
     * {@link #restart()} calls; it does NOT reconfigure a Vert.x instance that
     * is already running. {@link #options()} is an exact alias.
     *
     * @return the current options, never {@code null}
     */
    public static VertxOptions vertxOptions() {
        return vertxOptions;
    }

    /**
     * Returns the live Vert.x instance, starting the framework with
     * {@link #vertxOptions()} if it is not running yet.
     *
     * <p>For applications that need explicit control over the startup point,
     * call {@link #start()} first and treat this method as a getter only.
     *
     * @return the running {@link Vertx}, never {@code null}
     */
    public static Vertx vertx() {
        if (vertx == null) {
            start();
        }
        return vertx;
    }

    /**
     * Alias for {@link #vertxOptions()}: returns the live, mutable
     * {@link VertxOptions} singleton.
     *
     * @return the current options, never {@code null}
     */
    public static VertxOptions options() {
        return vertxOptions;
    }

    /**
     * Logs every {@code getXxx} property of the current {@link VertxOptions}
     * to SLF4J at INFO level, sorted alphabetically. Useful at startup to
     * verify which configuration is actually in effect.
     *
     * <p>Reflection failures on individual getters are logged at ERROR and do
     * not interrupt the dump.
     */
    public static void logOptions() {
        final Set<String> options = new TreeSet<>();
        for (final Method method : VertxOptions.class.getMethods()) {
            if (method.getName().startsWith("get")) {
                final String methodName = method.getName();
                if (!"getClass".equals(methodName)) {
                    try {
                        options.add(methodName.substring(3) + ": " + method.invoke(vertxOptions));
                    }
                    catch (final Exception e) {
                        LOGGER.error("invoke method of vertxOption failed: {}", methodName, e);
                    }
                }
            }
        }
        LOGGER.info("YojaApp vertx configuration: \n{}",
                    String.join(System.lineSeparator(), options));
    }

    /*
     *
     *
     *
     */

    /**
     * Tells whether the current thread is a Vert.x event-loop thread.
     *
     * <p>Detection is based on the thread name (Vert.x event-loop threads are
     * named {@code "vert.x-eventloop-…"}). Use it to refuse blocking calls
     * when the current code might run on the event loop.
     *
     * @return {@code true} on a Vert.x event-loop thread, {@code false} otherwise
     */
    public static boolean isEventLoopThread() {
        return Thread.currentThread().getName().startsWith("vert.x-eventloop");
    }

    /**
     * Tells whether the current thread is safe for blocking operations.
     * Defined as {@code !isEventLoopThread()} — any thread that is not a
     * Vert.x event-loop thread counts as a worker thread, including plain
     * {@code main}-thread callers in unit tests.
     *
     * @return {@code true} when blocking is allowed, {@code false} on the event loop
     */
    public static boolean isWorkerThread() {
        return !isEventLoopThread();
    }

    /**
     * Tells whether the framework is currently running (i.e. a Vert.x
     * instance has been created and not yet stopped).
     *
     * @return {@code true} if {@link #vertx()} would return without starting Vert.x
     */
    public static boolean isRunning() {
        return vertx != null;
    }

    /**
     * Starts the framework with the current {@link #options() VertxOptions}.
     *
     * @throws YojaAppException if the framework is already running
     */
    public static void start() {
        start(options());
    }

    /**
     * Starts the framework with a caller-supplied {@link VertxOptions}.
     * The argument also becomes the new {@link #options()} singleton, so
     * subsequent {@link #restart()} calls reuse it.
     *
     * @param vertxOptions the options to launch Vert.x with
     * @throws YojaAppException if the framework is already running
     */
    public static void start(final VertxOptions vertxOptions) {
        if (isRunning()) {
            throw new YojaAppException("yoja app already started");
        }
        launchVertx(vertxOptions);
    }

    /**
     * Stops the current Vert.x instance (if any) and re-launches it with the
     * current {@link #options() VertxOptions}.
     *
     * @return a future that completes once Vert.x has been re-launched.
     *         If the framework was not running, returns an already-succeeded
     *         future after starting Vert.x.
     */
    public static Future<Void> restart() {
        return restart(options());
    }

    /**
     * Stops the current Vert.x instance (if any) and re-launches it with the
     * given {@code vertxOptions}.
     *
     * @param vertxOptions the options used to re-launch Vert.x
     * @return a future that completes once Vert.x has been re-launched
     */
    public static Future<Void> restart(final VertxOptions vertxOptions) {
        final Future<Void> result;
        if (isRunning()) {
            result = Future.future(h -> {
                stop().andThen(v -> {
                    launchVertx(vertxOptions);
                    h.complete();
                });
            });
        }
        else {
            launchVertx(vertxOptions);
            result = Future.succeededFuture();
        }
        return result;
    }

    /**
     * Stops the framework asynchronously. The returned future completes once
     * Vert.x is closed and every registered {@link Worker} has been removed.
     *
     * <p>Calling {@code stop()} on a non-running framework is a no-op that
     * returns a successful future.
     *
     * @return a future that completes when shutdown is finished
     */
    public static Future<Void> stop() {
        final Future<Void> result;
        if (YojaApp.isRunning()) {
            result = vertx.close()
                          .andThen(v -> Worker.removeWorkers())
                          .andThen(v -> vertx = null);
        }
        else {
            result = Future.succeededFuture();
        }
        return result;
    }

    /**
     * Blocks the current thread until the framework has been stopped by some
     * other thread. The implementation polls {@link #isRunning()} every
     * 250 ms.
     *
     * <p>Use this from {@code main()} to keep a CLI application alive until a
     * shutdown signal triggers {@link #stop()}.
     *
     * <p><b>Do not call this from a Vert.x event-loop thread</b> — it would
     * deadlock the loop and prevent {@link #stop()} from completing.
     *
     * @throws YojaAppException if {@link Thread#sleep(long)} is interrupted
     */
    public static void awaitStop() {
        final AtomicBoolean await = new AtomicBoolean(true);
        while (await.get()) {
            try {
                if (vertx == null) {
                    await.set(false);
                }
                Thread.sleep(250);
            }
            catch (final Exception e) {
                throw new YojaAppException("await stopping YojaApp failed", e);
            }
        };
    }

    /*
     *
     * BUILDER
     *
     */

    /**
     * Returns a fluent builder that mutates the shared {@link #options()}
     * singleton. The terminal call is {@link Builder#start()} (or
     * {@link Builder#start(VertxOptions)}).
     *
     * <p>Example:
     * <pre>{@code
     * YojaApp.builder()
     *        .eventLoopPoolSize(4)
     *        .workerPoolSize(16)
     *        .start();
     * }</pre>
     *
     * @return a fresh builder bound to the current options singleton
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent configurator for the shared {@link YojaApp} options.
     *
     * <p>Every setter mutates the framework-wide {@link VertxOptions}
     * singleton ({@link YojaApp#options()}), so chaining is purely cosmetic —
     * any change persists across builder instances.
     */
    public static class Builder {

        private Builder() {
            super();
        }

        /**
         * Sets the number of event-loop threads. Defaults to Vert.x's own
         * default ({@code 2 × cores}).
         *
         * @param eventLoopPoolSize the new pool size, must be strictly positive
         * @return this builder
         */
        public Builder eventLoopPoolSize(final int eventLoopPoolSize) {
            vertxOptions.setEventLoopPoolSize(eventLoopPoolSize);
            return this;
        }

        /**
         * Sets the size of the worker thread pool used for blocking tasks
         * submitted via {@link Worker.parallelThread}. Defaults to
         * {@link Runtime#availableProcessors()}.
         *
         * @param workerPoolSize the new pool size, must be strictly positive
         * @return this builder
         */
        public Builder workerPoolSize(final int workerPoolSize) {
            vertxOptions.setWorkerPoolSize(workerPoolSize);
            return this;
        }

        /**
         * Configures the maximum time a task is allowed to run on the event
         * loop before Vert.x logs a warning. Default: 4 seconds.
         *
         * <p>Static method on a builder class: the call mutates the shared
         * options singleton regardless of whether you obtained a builder
         * instance first.
         *
         * @param value    the time value
         * @param timeUnit the unit of {@code value}
         */
        public static void maxEventLoopExecuteTime(final long value,
                                                   final TimeUnit timeUnit) {
            vertxOptions.setMaxEventLoopExecuteTimeUnit(timeUnit);
            vertxOptions.setMaxEventLoopExecuteTime(value);
        }

        /**
         * Configures the maximum time a worker task may run before Vert.x
         * logs a warning. Default: 5 minutes.
         *
         * <p>Same static-method-on-a-builder oddity as
         * {@link #maxEventLoopExecuteTime(long, TimeUnit)}: the call mutates
         * the shared options singleton directly.
         *
         * @param value    the time value
         * @param timeUnit the unit of {@code value}
         */
        public static void maxMaxWorkerExecuteTime(final long value,
                                                   final TimeUnit timeUnit) {
            vertxOptions.setMaxWorkerExecuteTimeUnit(timeUnit);
            vertxOptions.setMaxWorkerExecuteTime(value);
        }

        /**
         * Terminal builder call: starts the framework with the current
         * {@link YojaApp#options() options}.
         *
         * @throws YojaAppException if the framework is already running
         */
        public void start() {
            start(options());
        }

        /**
         * Terminal builder call: starts the framework with the given options,
         * bypassing the singleton mutations performed by the other builder
         * methods.
         *
         * @param vertxOptions the options to launch Vert.x with
         * @throws YojaAppException if the framework is already running
         */
        public void start(final VertxOptions vertxOptions) {
            if (isRunning()) {
                throw new YojaAppException("yoja app already started");
            }
            launchVertx(vertxOptions);
        }

        /**
         * Convenience for {@link YojaApp#restart()}.
         *
         * @return a future that completes once Vert.x has been re-launched
         */
        public Future<Void> restart() {
            return YojaApp.restart();
        }

    }

    private static void launchVertx(final VertxOptions vertxOptions) {
        YojaApp.vertxOptions = vertxOptions;
        vertx = Vertx.vertx(vertxOptions);
    }

}
