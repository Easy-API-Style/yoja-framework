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

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.easygoingapi.yoja.core.YojaApp;
import com.easygoingapi.yoja.core.YojaAppException;

import io.vertx.core.Future;

/**
 * Bridges asynchronous Vert.x {@link Future}s with synchronous, blocking code
 * running inside a Yoja worker thread.
 *
 * <p>The Vert.x model is non-blocking: every I/O call returns a {@link Future}
 * resolved later on the event loop. Sometimes (legacy code, batch scripts,
 * scheduled imports) you genuinely need to block until the result is ready.
 * That is what this class is for.
 *
 * <p><b>Threading contract.</b> All methods here busy-wait by repeatedly
 * sleeping the current thread. They MUST be called from a Yoja worker thread
 * — never from the event loop. {@link #sleep(long)} silently turns into a
 * no-op when invoked outside a worker thread (see {@link YojaApp#isWorkerThread()}),
 * which means an accidental event-loop call to {@code await(...)} would
 * spin-loop forever; calling code is responsible for staying on a worker.
 *
 * <p>Typical use inside a {@link com.easygoingapi.yoja.core.worker.Worker}:
 * <pre>{@code
 * Worker.singleThread.once("import", () -> {
 *     final String userId = FutureUtil.awaitValue(httpClient.getUserId());
 *     final List<Item> items = FutureUtil.awaitValue(repository.load(userId));
 *     FutureUtil.sleep(500); // gentle backoff before the next call
 *     // ...
 * });
 * }</pre>
 *
 * @see YojaApp#isWorkerThread()
 * @see com.easygoingapi.yoja.core.worker.Worker
 */
public class FutureUtil {

    /** Not instantiable. */
    private FutureUtil() {}

    /**
     * Blocks the current worker thread until {@code future} completes, then
     * returns its result. Equivalent to {@link #await(Future)} followed by
     * {@link Future#result()}.
     *
     * <p>Example:
     * <pre>{@code
     * final JsonObject body = FutureUtil.awaitValue(httpClient.get(url));
     * }</pre>
     *
     * @param future the future to wait on (must not be {@code null})
     * @param <V>    the result type carried by the future
     * @return the value the future was completed with, or {@code null} if
     *         the future was failed (no exception is rethrown — inspect with
     *         {@link Future#failed()} if you need that)
     * @throws YojaAppException if the polling loop is interrupted
     */
    public static <V> V awaitValue(final Future<V> future) {
        await(future);
        return future.result();
    }

    /**
     * Blocks until <b>all</b> the given futures have completed (succeeded or
     * failed). Convenience overload of {@link #await(List)}.
     *
     * <p>Example:
     * <pre>{@code
     * FutureUtil.await(client.send(req1),
     *                  client.send(req2),
     *                  client.send(req3));
     * }</pre>
     *
     * @param futures futures to wait on
     * @throws YojaAppException if the polling loop is interrupted
     */
    public static void await(final Future<?>... futures) {
        await(Future.all(List.of(futures)));
    }

    /**
     * Blocks until <b>all</b> the given futures have completed.
     *
     * @param futures futures to wait on
     * @throws YojaAppException if the polling loop is interrupted
     */
    public static void await(final List<Future<?>> futures) {
        await(Future.all(futures));
    }

    /**
     * Blocks the current worker thread until {@code future} completes.
     *
     * <p>Implementation note: this is a polling loop that checks
     * {@link Future#isComplete()} every 100 ms. The wait must therefore happen
     * on a Yoja worker thread; calling it from the event loop would starve
     * Vert.x. The result of the future is not returned — see
     * {@link #awaitValue(Future)} if you need the value.
     *
     * @param future the future to wait on
     * @throws YojaAppException if the polling loop is interrupted
     */
    public static void await(final Future<?> future) {
        try {
            final AtomicBoolean await = new AtomicBoolean(true);
            while (await.get()) {
                if (future.isComplete()) {
                    await.set(false);
                }
                sleep(100);
            }
        }
        catch (final Exception e) {
            throw new YojaAppException("await future failed", e);
        }
    }

    /**
     * Sleeps the current worker thread for the given duration, in
     * milliseconds. This is a thin wrapper around {@link Thread#sleep(long)}
     * that:
     * <ul>
     *   <li>is a <b>no-op when called outside a worker thread</b> — protecting
     *       the Vert.x event loop from accidental blocking,</li>
     *   <li>wraps {@link InterruptedException} (and any other failure) into a
     *       {@link YojaAppException} so callers do not need to declare it.</li>
     * </ul>
     *
     * @param millis sleep duration in milliseconds; {@code 0} or negative
     *               values behave like {@link Thread#sleep(long)}
     * @throws YojaAppException if the underlying {@code Thread.sleep} fails
     */
    public static void sleep(final long millis) {
        if (YojaApp.isWorkerThread()) {
            try {
                Thread.sleep(millis);
            }
            catch (final Exception e) {
                throw new YojaAppException("sleep failed", e);
            }
        }
    }

}
