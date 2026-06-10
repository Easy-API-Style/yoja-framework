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
package com.easygoingapi.yoja.core.worker;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.core.YojaApp;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.WorkerExecutor;

/**
 * Off-loads blocking work to background threads so the Vert.x event loop is
 * never starved. Yoja exposes two flavours through two nested namespaces:
 * <ul>
 *   <li>{@link singleThread} — one dedicated thread per worker ID, tasks are
 *       queued and executed sequentially in submission order.</li>
 *   <li>{@link parallelThread} — a shared pool of N threads per worker ID,
 *       tasks run concurrently.</li>
 * </ul>
 *
 * <p>A {@code Worker} instance is essentially a typed handle to a
 * {@link WorkerExecutor}: it carries the worker's {@link Type}, its
 * application-level {@code id}, its pool size, and the {@link #execute}
 * methods that submit work.
 *
 * <p><b>Threading contract.</b> Handlers registered via {@code onSuccess} /
 * {@code onFailure} / {@code onComplete} on the {@link Future} returned by
 * {@code execute(...)} run on the <b>event-loop thread</b> that was active at
 * registration time, not on the worker thread. Never block inside those
 * handlers; submit a new worker task instead.
 *
 * <p>Typical use:
 * <pre>{@code
 * // sequential, ID-scoped — perfect for an "import-once" pipeline
 * Worker.singleThread.once("import", () -> {
 *     for (Item item : repository.listAll()) {
 *         processBlocking(item);
 *     }
 * });
 *
 * // shared pool, fire-and-forget
 * Worker.parallelThread.execute(() -> writeAuditLogBlocking());
 *
 * // a longer-lived single-thread worker, retrieved by ID
 * Worker importer = Worker.singleThread.get("import");
 * importer.execute(() -> step1());
 * importer.execute(() -> step2());
 * importer.remove();
 * }</pre>
 *
 * @see Timer
 * @see com.easygoingapi.yoja.core.util.FutureUtil
 */
public class Worker {

    private final static Logger LOGGER = LoggerFactory.getLogger(Worker.class);

    /**
     * Worker flavour: {@link #onceThread} (internal, used by {@code once(...)}
     * helpers), {@link #singleThread} (sequential, ID-scoped) or
     * {@link #parallelThread} (shared pool).
     */
    public static enum Type {
        /** Internal — used by {@code once(…)} helpers; not intended for user code. */
        onceThread,
        /** One dedicated thread per worker ID; tasks run sequentially. */
        singleThread,
        /** Shared thread pool per worker ID; tasks run concurrently. */
        parallelThread
    }

    /**
     * Event delivered to {@link #onRemove(Handler)} listeners when a worker is
     * removed.
     *
     * @param type     the worker {@link Type}
     * @param id       the worker's application-level identifier
     * @param poolSize the size of the underlying executor pool
     */
    public static record WorkerEvent(Type type,
                                     String id,
                                     int poolSize) {}

    private final String id;
    private final Type type;
    private final int poolSize;
    private final WorkerExecutor workerExecutor;

    private final List<Handler<WorkerEvent>> onRemoveActions = new ArrayList<>();

    /**
     * Package-internal constructor. Workers are created and registered by
     * {@link WorkerService}; user code obtains them via
     * {@link singleThread#get(String)} or {@link parallelThread#create(String, int)}.
     *
     * @param type           the worker flavour
     * @param id             the application-level ID
     * @param poolSize       size of the underlying {@link WorkerExecutor}
     * @param workerExecutor the underlying Vert.x executor
     */
    protected Worker(final Type type,
                     final String id,
                     final int poolSize,
                     final WorkerExecutor workerExecutor) {
        super();
        this.id = id;
        this.type = type;
        this.poolSize = poolSize;
        this.workerExecutor = workerExecutor;
    }

    /**
     * Returns a {@link Key} identifying this worker as a (type, id) pair —
     * used as a map key inside {@link WorkerService} and by {@link Timer}.
     *
     * @return a fresh key, never {@code null}
     */
    public Key key() {
        return new Key(type, id);
    }

    /**
     * Returns the application-level identifier passed at creation time.
     *
     * @return the application-level identifier passed at creation time
     */
    public String id() {
        return id;
    }

    /**
     * Returns the flavour of this worker.
     *
     * @return the flavour of this worker
     */
    public Type type() {
        return type;
    }

    /**
     * Returns the size of the underlying thread pool.
     *
     * @return the size of the underlying thread pool ({@code 1} for
     *         {@link Type#singleThread})
     */
    public int poolSize() {
        return poolSize;
    }

    /**
     * Tells whether this worker is still registered (i.e. has not been
     * removed). Equivalent to {@link #isActive(Worker) isActive(this)}.
     *
     * @return {@code true} if the worker is still usable
     */
    public boolean isActive() {
        return Worker.isActive(this);
    }

    /**
     * Submits a fire-and-forget blocking task. The task runs on the worker
     * thread; the returned future completes (successfully) when the task
     * finishes, or fails if the worker has been removed.
     *
     * @param runnable the blocking work to execute
     * @return a future completing when {@code runnable} returns
     */
    public Future<Void> execute(final Runnable runnable) {
        return execute(() -> MainWorker.run(runnable));
    }

    /**
     * Submits a blocking task that produces a value via an explicit
     * {@link Promise}. Use this form when you need to differentiate
     * success and failure paths from inside the task body.
     *
     * @param handler completion handler invoked on the worker thread
     * @param <T>     the result type
     * @return a future carrying the value the handler completes its promise with
     */
    public <T> Future<T> execute(final Handler<Promise<T>> handler) {
        return execute(() -> MainWorker.promise(handler)).compose(f -> f);
    }

    /**
     * Submits a blocking task that produces a value via a plain
     * {@link Callable}. Tasks are executed in <b>un-ordered</b> mode
     * ({@code ordered=false} on the underlying executor): on a single-thread
     * worker the queue is already serial, on a parallel worker tasks run
     * concurrently.
     *
     * @param callable the blocking computation
     * @param <T>      the result type
     * @return a future carrying the callable's return value, or a failed
     *         future {@code "worker not active"} if the worker has been removed
     */
    public <T> Future<T> execute(final Callable<T> callable) {
        final Future<T> result;
        if (isActive()) {
            result = workerExecutor.executeBlocking(callable, false);
        }
        else {
            result = Future.failedFuture("worker not active");
        }
        return result;
    }

    /**
     * Returns a defensive copy of the currently-registered removal listeners.
     * Package-internal: used by {@link WorkerService} when firing events.
     *
     * @return a snapshot of the listener list
     */
    protected List<Handler<WorkerEvent>> onRemoveActions() {
        synchronized (onRemoveActions) {
            return new ArrayList<>(onRemoveActions);
        }
    }

    /**
     * Registers a listener notified when this worker is removed (via
     * {@link #remove()} or {@link Worker#removeWorkers()}).
     *
     * <p>Listeners are stored on the worker instance — they survive across
     * future submissions but are dropped together with the worker.
     *
     * @param workerEvent the listener to register
     */
    public void onRemove(final Handler<WorkerEvent> workerEvent) {
        synchronized (onRemoveActions) {
            onRemoveActions.add(workerEvent);
        }
    }

    /**
     * Removes this worker from the framework registry and closes its
     * underlying executor.
     *
     * @return a future completing once the executor has shut down
     */
    public Future<Void> remove() {
        return Worker.remove(this);
    }

    /**
     * Closes the underlying {@link WorkerExecutor}. Package-internal: invoked
     * by {@link WorkerService} as part of {@link #remove()}.
     *
     * @return a future completing when the executor is closed
     */
    protected Future<Void> close() {
        return workerExecutor.close();
    }

    /**
     * @return a debug string in the form
     *         {@code "Worker [id=…, type=…, poolSize=…]"}
     */
    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(Worker.class.getSimpleName());
        result.append(" [id=");
        result.append(id);
        result.append(", type=");
        result.append(type);
        result.append(", poolSize=");
        result.append(poolSize);
        result.append("]");
        return result.toString();
    }

    /*
     *
     * STATIC
     *
     */

    /**
     * Removes the given workers in parallel.
     *
     * @param workers the workers to remove; {@code null} returns an
     *                already-succeeded future
     * @return a future completing when every worker has been removed
     */
    public static Future<Void> remove(final Worker... workers) {
        return workers != null
                 ? remove(Set.of(workers))
                 : Future.succeededFuture();
    }

    /**
     * Removes a set of workers in parallel.
     *
     * @param workers the workers to remove; {@code null} or empty returns a
     *                successful future immediately
     * @return a future completing when every worker has been removed
     */
    public static Future<Void> remove(final Set<Worker> workers) {
        final List<Future<Void>> futures = new ArrayList<>();
        if (workers != null) {
            for (final Worker worker : workers) {
                futures.add(WorkerService.remove(worker.type(), worker.id()));
            }
        }
        return Future.all(futures).mapEmpty();
    }

    /**
     * Removes a single worker. Equivalent to {@link #remove()} but accepts
     * a possibly-{@code null} reference.
     *
     * @param worker the worker to remove; {@code null} returns a successful
     *               future immediately
     * @return a future completing once the executor has shut down
     */
    public static Future<Void> remove(final Worker worker) {
        if (worker != null) {
            return WorkerService.remove(worker.type(), worker.id());
        }
        return Future.succeededFuture();
    }

    /**
     * Tells whether the given worker is still registered in the framework.
     *
     * @param worker the worker to check
     * @return {@code true} if a worker with the same (type, id) is registered
     */
    public static boolean isActive(final Worker worker) {
        return WorkerService.has(worker.type(), worker.id());
    }

    /**
     * Logs every active worker (single- and parallel-thread) to SLF4J at
     * INFO level. When no worker is active, logs an explicit "no Yoja
     * Workers active" line.
     */
    public static void log() {
        final List<String> lines = new ArrayList<>();
        if (!singleThread.ids().isEmpty()) {
            lines.add("singleThread:");
            for (final String id : singleThread.ids()) {
                lines.add(singleThread.get(id).toString());
            }
        }
        if (!parallelThread.ids().isEmpty()) {
            lines.add("parallelThread:");
            for (final String id : parallelThread.ids()) {
                lines.add(parallelThread.get(id).toString());
            }
        }
        if (!lines.isEmpty()) {
            LOGGER.info("Yoja Workers active:\n{}", String.join("\n", lines));
        }
        else {
            LOGGER.info("no Yoja Workers active");
        }
    }

    /*
     *
     * SingleThread
     *
     */

    /**
     * Namespace for single-thread workers. Each {@code (type=singleThread,
     * id)} pair maps to one dedicated thread; tasks submitted with the same
     * ID are executed sequentially.
     *
     * <p>The {@code once(...)} helpers create a transient executor that
     * shuts down right after the task completes — ideal for "fire one task
     * on its own thread and forget about it" use cases.
     */
    public static class singleThread {

        private singleThread() {}

        /**
         * Returns an immutable view of the IDs of every currently-registered single-thread worker.
         *
         * @return an immutable view of the IDs of every currently-registered
         *         single-thread worker
         */
        public static Set<String> ids() {
            return WorkerService.ids(Worker.Type.singleThread);
        }

        /**
         * Returns {@code true} if a single-thread worker with this ID is registered.
         *
         * @param id the worker ID to test
         * @return {@code true} if a single-thread worker with this ID is registered
         */
        public static boolean has(final String id) {
            return WorkerService.has(Worker.Type.singleThread, id);
        }

        /**
         * Returns the single-thread worker with the given ID, creating it
         * on first call.
         *
         * @param id the worker ID
         * @return the registered worker, never {@code null}
         */
        public static Worker get(final String id) {
            return WorkerService.create(Worker.Type.singleThread, id, 1);
        }

        /**
         * Removes the single-thread worker with the given ID. No-op if no
         * such worker is registered.
         *
         * @param id the worker ID
         * @return a future completing once the executor has shut down
         */
        public static Future<Void> remove(final String id) {
            return WorkerService.remove(Worker.Type.singleThread, id);
        }

        /*
         *
         * main worker
         *
         */

        /**
         * Runs a one-shot blocking task on a freshly-created single-thread
         * executor named {@code "onceSingleThread_<id>"} and closes it right
         * after the task finishes. Use this when you do not need to retain a
         * reference to the worker.
         *
         * @param id      a short identifier (used only to name the executor)
         * @param handler the task body, called with an explicit {@link Promise}
         * @param <T>     the result type
         * @return a future carrying the value the handler completes its promise with
         */
        public static <T> Future<T> once(final String id,
                                         final Handler<Promise<T>> handler) {
            return once(id, () -> MainWorker.promise(handler)).compose(f -> f);
        }

        /**
         * Runs a fire-and-forget one-shot blocking task — see
         * {@link #once(String, Handler)} for the lifecycle.
         *
         * @param id       a short identifier
         * @param runnable the blocking work to execute
         * @return a future completing when {@code runnable} returns
         */
        public static Future<Void> once(final String id,
                                        final Runnable runnable) {
            return once(id, () -> MainWorker.run(runnable));
        }

        /**
         * Runs a value-producing one-shot blocking task. See
         * {@link #once(String, Handler)} for the lifecycle and naming convention.
         *
         * @param id       a short identifier (used only to name the executor)
         * @param callable the blocking computation
         * @param <T>      the result type
         * @return a future carrying the callable's return value
         */
        public static <T> Future<T> once(final String id,
                                         final Callable<T> callable) {
            final WorkerExecutor workerExecutor = YojaApp.vertx().createSharedWorkerExecutor("onceSingleThread_" + id, 1);
            return workerExecutor.executeBlocking(callable, false)
                                 .andThen(h -> workerExecutor.close());
        }

    }

    /*
     *
     * ParallelThread
     *
     */

    /**
     * Namespace for parallel-thread workers. Each ID is backed by a thread
     * pool of the size declared at {@link #create(String, int)} time; tasks
     * submitted to the same worker run concurrently.
     */
    public static class parallelThread {

        private parallelThread() {}

        /**
         * Returns an immutable view of the IDs of every currently-registered parallel-thread worker.
         *
         * @return an immutable view of the IDs of every currently-registered
         *         parallel-thread worker
         */
        public static Set<String> ids() {
            return WorkerService.ids(Worker.Type.parallelThread);
        }

        /**
         * Returns {@code true} if a parallel-thread worker with this ID is registered.
         *
         * @param id the worker ID to test
         * @return {@code true} if a parallel-thread worker with this ID is registered
         */
        public static boolean has(final String id) {
            return WorkerService.has(Worker.Type.parallelThread, id);
        }

        /**
         * Returns the parallel-thread worker with the given ID, or
         * {@code null} if none exists.
         *
         * @param id the worker ID
         * @return the registered worker, or {@code null}
         */
        public static Worker get(final String id) {
            return WorkerService.get(Worker.Type.parallelThread, id);
        }

        /**
         * Returns the parallel-thread worker with the given ID, creating it
         * with the given {@code poolSize} on first call. Subsequent calls
         * return the existing worker — the {@code poolSize} argument is then
         * ignored.
         *
         * @param id       the worker ID
         * @param poolSize size of the underlying thread pool on creation
         * @return the registered worker, never {@code null}
         */
        public static Worker create(final String id,
                                    final int poolSize) {
            final Worker worker = get(id);
            if (worker != null) {
                return worker;
            }
            return WorkerService.create(Worker.Type.parallelThread, id, poolSize);
        }

        /**
         * Removes the parallel-thread worker with the given ID. No-op if no
         * such worker is registered.
         *
         * @param id the worker ID
         * @return a future completing once the pool has shut down
         */
        public static Future<Void> remove(final String id) {
            return WorkerService.remove(Worker.Type.parallelThread, id);
        }

        /*
         *
         * main worker
         *
         */

        /**
         * Submits a fire-and-forget blocking task to the framework's default
         * parallel pool ({@code workerPoolSize} of {@link YojaApp#options()}).
         *
         * @param runnable the blocking work to execute
         * @return a future completing when {@code runnable} returns
         */
        public static Future<Void> execute(final Runnable runnable) {
            return MainWorker.execute(runnable);
        }

        /**
         * Submits a value-producing blocking task to the default parallel pool.
         *
         * @param callable the blocking computation
         * @param <T>      the result type
         * @return a future carrying the callable's return value
         */
        public static <T> Future<T> execute(final Callable<T> callable) {
            return MainWorker.execute(callable);
        }

        /**
         * Submits a blocking task driven by an explicit {@link Promise} to
         * the default parallel pool.
         *
         * @param handler completion handler invoked on a worker thread
         * @param <T>     the result type
         * @return a future carrying the value the handler completes its promise with
         */
        public static <T> Future<T> execute(final Handler<Promise<T>> handler) {
            return MainWorker.execute(handler);
        }

    }

    /*
     *
     * global
     *
     */

    /**
     * Tells whether the current thread has been interrupted, typically as a
     * side-effect of {@link #remove()} closing its underlying executor.
     * Long-running blocking tasks can poll this to exit cooperatively.
     *
     * @return {@code true} if the current thread is interrupted
     */
    public static boolean isClosed() {
        return Thread.currentThread().isInterrupted();
    }

    /**
     * Removes every registered worker (single- and parallel-thread). Called
     * by {@link YojaApp#stop()} during framework shutdown.
     *
     * @return a future completing when every worker has been removed
     */
    public static Future<Void> removeWorkers() {
        final List<Future<?>> futures = new ArrayList<>();
        if (!singleThread.ids().isEmpty()) {
            for (final String id : singleThread.ids()) {
                final Worker worker = singleThread.get(id);
                futures.add(worker.remove());
            }
        }
        if (!parallelThread.ids().isEmpty()) {
            for (final String id : parallelThread.ids()) {
                final Worker worker = parallelThread.get(id);
                futures.add(worker.remove());
            }
        }
        return Future.all(futures).mapEmpty();
    }

    /*
     *
     * CLASS
     *
     */

    /**
     * Immutable {@code (type, id)} pair used as a worker registry key. Two
     * keys are equal iff both their {@link Type} and {@code id} match.
     */
    public static class Key {

        private final Type type;
        private final String id;

        /**
         * Package-internal constructor. User code obtains a {@code Key} via
         * {@link Worker#key()}.
         *
         * @param type the worker flavour
         * @param id   the application-level identifier
         */
        protected Key(final Type type,
                      final String id) {
            super();
            this.type = type;
            this.id = id;
        }

        /**
         * Returns the worker flavour.
         *
         * @return the worker flavour
         */
        public Type type() {
            return type;
        }

        /**
         * Returns the worker's application-level identifier.
         *
         * @return the worker's application-level identifier
         */
        public String id() {
            return id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, type);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Key other = (Key) obj;
            return Objects.equals(id, other.id) && type == other.type;
        }

    }

}
