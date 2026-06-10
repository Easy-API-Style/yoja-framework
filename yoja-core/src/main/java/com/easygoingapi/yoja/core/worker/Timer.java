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

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.easygoingapi.yoja.core.YojaApp;

import io.vertx.core.Handler;

/**
 * One-shot and periodic task scheduler. Every task is identified by a
 * String ID — scheduling a new task with an existing ID atomically cancels
 * the previous one, which makes the API idempotent and safe to call from
 * multiple code paths.
 *
 * <p>Tasks are dispatched by the Vert.x scheduler ({@link io.vertx.core.Vertx#setTimer}
 * / {@link io.vertx.core.Vertx#setPeriodic}) and then handed off to a
 * {@link Worker}: either the one passed to {@link Builder#worker(Worker)} or,
 * by default, the framework's default parallel-thread pool
 * ({@link Worker.parallelThread#execute(Runnable)}). The task body therefore
 * always runs on a worker thread — safe for blocking operations.
 *
 * <p>The class is entirely static; the private constructor exists only to
 * prevent instantiation. The internal registry is backed by a
 * {@link ConcurrentHashMap} so {@code schedule(...)} and {@link #cancel(String)}
 * are safe to call from any thread.
 *
 * <p>Examples:
 * <pre>{@code
 * // one-shot, 5 seconds from now
 * Timer.schedule("welcome-mail", task -> sendWelcomeMail())
 *      .delay(Duration.ofSeconds(5))
 *      .build();
 *
 * // periodic every minute, starting at 02:00 tomorrow
 * Timer.schedule("nightly-report", task -> generateReport())
 *      .firstTime(Date.from(tomorrowAt2am()))
 *      .period(Duration.ofMinutes(1))
 *      .build();
 *
 * // periodic, executed on a dedicated single-thread worker
 * Timer.schedule("sync", task -> syncBlocking())
 *      .worker(Worker.singleThread.get("sync"))
 *      .period(Duration.ofMinutes(5))
 *      .build();
 *
 * Timer.cancel("sync");
 * }</pre>
 *
 * @see Worker
 */
public class Timer {

//    private final static Logger LOGGER = LoggerFactory.getLogger(Timer.class);

    private static final Map<String, Task> tasksById = new ConcurrentHashMap<>();

    private Timer() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * Tells whether a task with the given ID is currently registered.
     *
     * @param id the task ID
     * @return {@code true} if a task with this ID is scheduled
     */
    public static boolean has(final String id) {
        return tasksById.containsKey(id);
    }

    /**
     * Returns the IDs of every currently-scheduled task, sorted lexicographically.
     *
     * @return a fresh, mutable set — callers may modify it without affecting the registry
     */
    public static Set<String> ids() {
        return new TreeSet<>(tasksById.keySet());
    }

    /**
     * Returns the {@link Task} object associated with the given ID, or
     * {@code null} if no such task is scheduled.
     *
     * @param id the task ID
     * @return the registered task, or {@code null}
     */
    public static Task task(final String id) {
        return tasksById.get(id);
    }

    /**
     * Cancels the task with the given ID. Removes it from the registry, marks
     * the {@link Task} object as cancelled (see {@link Task#isCanceled()}),
     * and tells Vert.x to drop its timer.
     *
     * <p>The cancellation is best-effort: a task that is already executing
     * when {@code cancel} is called will run to completion.
     *
     * @param id the task ID
     * @return {@code true} if Vert.x successfully cancelled its underlying
     *         timer; {@code false} if no such task was registered or if
     *         Vert.x refused (e.g. the timer had already fired one last time)
     */
    public static boolean cancel(final String id) {
        final Task task = tasksById.remove(id);
        boolean result = false;
        if (task != null) {
            task.cancel();
            result = YojaApp.vertx().cancelTimer(task.taskId);
        }
        return result;
    }

    /**
     * Cancels every currently-registered task. Useful at shutdown to make
     * sure no straggler keeps firing after {@link YojaApp#stop()}.
     */
    public static void cancel() {
        final Set<String> idsToRemove = ids();
        for (final String id : idsToRemove) {
            cancel(id);
        }
    }

    /**
     * Opens a fluent builder for a task identified by {@code id}.
     *
     * <p>The terminal call is {@link Builder#build()}. Calling
     * {@code build()} with an ID that is already in use atomically cancels
     * the previous task before installing the new one — schedule-or-replace
     * semantics by default.
     *
     * @param id     the task ID (must be unique; identical IDs replace each other)
     * @param action the work to run when the timer fires; receives the {@link Task}
     * @return a fresh builder
     */
    public static Builder schedule(final String id,
                                   final Handler<Task> action) {
        return new Builder(id, action);
    }

    /*
     *
     * CLASS
     *
     */

    /**
     * Handle to a scheduled task. Built by {@link Builder#build()} and stored
     * in the static registry until {@link Timer#cancel(String)} runs.
     */
    public static class Task {

        private final String id;
        private final Worker.Key workerKey;
        private final Date firstTime;
        private final Duration period;
        private final long taskId;

        private boolean cancel;

        /**
         * Package-internal constructor. {@code Task} instances are created
         * by {@link Builder#build()} only.
         *
         * @param id        the task's application-level ID
         * @param workerKey the {@link Worker} key the action runs on, or
         *                  {@code null} for the default parallel pool
         * @param firstTime the wall-clock time of the first execution
         * @param period    the period between repetitions, or {@code null} for one-shot
         * @param taskId    the underlying Vert.x timer ID
         */
        protected Task(final String id,
                       final Worker.Key workerKey,
                       final Date firstTime,
                       final Duration period,
                       final long taskId) {
            super();
            this.id = id;
            this.workerKey = workerKey;
            this.firstTime = firstTime;
            this.period = period;
            this.taskId = taskId;
        }

        /**
         * Returns the task's application-level ID.
         *
         * @return the task's application-level ID
         */
        public String id() {
            return id;
        }

        /**
         * Returns the {@link Worker.Key} the action runs on.
         *
         * @return the {@link Worker.Key} the action runs on, or {@code null}
         *         when the action is dispatched to the default parallel pool
         */
        public Worker.Key workerKey() {
            return workerKey;
        }

        /**
         * Returns the wall-clock time of the first execution.
         *
         * @return the wall-clock time of the first execution
         */
        public Date firstTime() {
            return firstTime;
        }

        /**
         * Returns the period between repetitions.
         *
         * @return the period between repetitions, or {@code null} for a
         *         one-shot task
         */
        public Duration period() {
            return period;
        }

        /**
         * Marks the task as cancelled. Package-internal: called from
         * {@link Timer#cancel(String)} before the Vert.x timer is dropped.
         */
        protected void cancel() {
            cancel = true;
        }

        /**
         * Returns {@code true} after {@link Timer#cancel(String)} has been called.
         *
         * @return {@code true} after {@link Timer#cancel(String)} has been
         *         called for this task's ID
         */
        public boolean isCanceled() {
            return cancel;
        }

        /**
         * Returns the underlying Vert.x timer ID (internal use only).
         *
         * @return the underlying Vert.x timer ID
         */
        protected long taskId() {
            return taskId;
        }

        /**
         * @return a debug string in the form
         *         {@code "Task [id=…, workerKey=…, firstTime=…, period=…, cancel=…]"}
         */
        @Override
        public String toString() {
            final StringBuilder result = new StringBuilder();
            result.append(Task.class.getSimpleName());
            result.append(" [");
            result.append("id=");
            result.append(id);
            if (workerKey != null) {
                result.append(", workerKey=");
                result.append(workerKey);
            }
            if (firstTime != null) {
                result.append(", firstTime=");
                result.append(firstTime);
            }
            if (period != null) {
                result.append(", period=");
                result.append(period);
            }
            result.append(", cancel=");
            result.append(cancel);
            result.append("]");
            return result.toString();
        }

    }

    /*
     *
     * BUILDER
     *
     */

    /**
     * Fluent configurator for a single {@link Task}. The interaction of
     * {@link #firstTime(Date)}, {@link #delay(Duration)} and
     * {@link #period(Duration)} on {@link #build()} is:
     *
     * <ul>
     *   <li><b>firstTime + period</b>: periodic, first fire at {@code firstTime}
     *       (offset by {@code delay} if also set).</li>
     *   <li><b>firstTime</b> (no period): one-shot, fires at {@code firstTime}
     *       (offset by {@code delay} if also set).</li>
     *   <li><b>delay + period</b>: periodic, first fire after {@code delay},
     *       repeating every {@code period}.</li>
     *   <li><b>delay</b> (no period): one-shot, fires after {@code delay}.</li>
     *   <li><b>period</b> (alone): periodic, first fire immediately.</li>
     *   <li>none of the above: one-shot, fires immediately.</li>
     * </ul>
     */
    public static class Builder {

        private final String id;
        private final Handler<Task> action;

        private Worker worker;
        private Date firstTime;
        private Duration delay;
        private Duration period;

        /**
         * Package-internal constructor. Builders are obtained via
         * {@link Timer#schedule(String, Handler)}.
         *
         * @param id     the task ID
         * @param action the action to run on each firing
         */
        protected Builder(final String id,
                          final Handler<Task> action) {
            super();
            this.id = id;
            this.action = action;
        }

        /**
         * Pins the task to a specific {@link Worker}. When unset, the action
         * runs on the framework's default parallel pool.
         *
         * @param worker the worker to dispatch the action to
         * @return this builder
         */
        public Builder worker(final Worker worker) {
            this.worker = worker;
            return this;
        }

        /**
         * Sets the wall-clock time of the first execution. Combined with
         * {@link #delay(Duration)}, the delay is added to the wall-clock time.
         *
         * @param firstTime first execution time (must be {@code Date}-aware)
         * @return this builder
         */
        public Builder firstTime(final Date firstTime) {
            this.firstTime = firstTime;
            return this;
        }

        /**
         * Sets a delay before the first execution. Interpreted relative to
         * {@link #firstTime(Date)} when both are set, or to "now" otherwise.
         *
         * @param delay delay before the first firing
         * @return this builder
         */
        public Builder delay(final Duration delay) {
            this.delay = delay;
            return this;
        }

        /**
         * Sets the period between repetitions, turning the task into a
         * periodic one. When unset, the task fires once and stops.
         *
         * @param period period between two consecutive firings
         * @return this builder
         */
        public Builder period(final Duration period) {
            this.period = period;
            return this;
        }

        /**
         * Terminal builder call: installs the task in the registry, cancels
         * any previous task with the same ID, and arms the underlying Vert.x
         * timer. See the class-level Javadoc for how
         * {@code firstTime / delay / period} combine.
         *
         * @return the freshly-registered {@link Task}, never {@code null}
         */
        public Task build() {
            return tasksById.compute(id, (k, v) -> {
                if (v != null) {
                    v.cancel();
                    YojaApp.vertx().cancelTimer(v.taskId);
                }
                final AtomicReference<Task> task = new AtomicReference<>();
                final Worker.Key workerKey = worker != null ? worker.key() : null;
                final Runnable runnableTask = () -> {
                    if (worker != null) {
                        worker.execute(() -> action.handle(task.get()));
                    }
                    else {
                        Worker.parallelThread.execute(() -> action.handle(task.get()));
                    }
                };
                if (firstTime != null) {
                    final AtomicReference<Date> date = new AtomicReference<>(firstTime);
                    long delayTask = Math.max(0, Duration.between(Instant.now(), firstTime.toInstant()).toMillis());
                    if (delay != null) {
                        delayTask = delayTask + delay.toMillis();
                        date.set(Date.from(date.get().toInstant().plusMillis(delay.toMillis())));
                    }
                    if (period != null) {
                        task.set(new Task(k,
                                          workerKey,
                                          date.get(),
                                          period,
                                          YojaApp.vertx().setPeriodic(delayTask, period.toMillis(), i -> runnableTask.run())));
                    }
                    else {
                        task.set(new Task(k,
                                          workerKey,
                                          date.get(),
                                          period,
                                          YojaApp.vertx().setTimer(delayTask, i -> runnableTask.run())));
                    }
                }
                else if (delay != null) {
                    Date date = Date.from(Instant.now().plusMillis(delay.toMillis()));
                    if (period != null) {
                        task.set(new Task(k,
                                          workerKey,
                                          date,
                                          period,
                                          YojaApp.vertx().setPeriodic(delay.toMillis(), period.toMillis(), i -> runnableTask.run())));
                    }
                    else {
                        task.set(new Task(k,
                                          workerKey,
                                          date,
                                          period,
                                          YojaApp.vertx().setTimer(delay.toMillis(), i -> runnableTask.run())));
                    }
                }
                else if (period != null) {
                    Date date = Date.from(Instant.now());
                    task.set(new Task(k,
                                      workerKey,
                                      date,
                                      period,
                                      YojaApp.vertx().setPeriodic(0, period.toMillis(), i -> runnableTask.run())));
                }
                else {
                    Date date = Date.from(Instant.now());
                    task.set(new Task(k,
                                      workerKey,
                                      date,
                                      period,
                                      YojaApp.vertx().setTimer(0, i -> runnableTask.run())));
                }
                return task.get();
            });
        }

        /**
         * @return a debug string in the form
         *         {@code "Builder [worker=…, firstTime=…, delay=…, period=…]"}
         */
        @Override
        public String toString() {
            final StringBuilder result = new StringBuilder();
            result.append(Builder.class.getSimpleName());
            result.append(" [");
            result.append("worker=");
            result.append(worker);
            if (firstTime != null) {
                result.append(", firstTime=");
                result.append(firstTime);
                result.append(", ");
            }
            if (delay != null) {
                result.append(", delay=");
                result.append(delay);
                result.append(", ");
            }
            if (period != null) {
                result.append(", period=");
                result.append(period);
            }
            result.append("]");
            return result.toString();
        }

    }

}
