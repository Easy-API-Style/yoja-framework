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

import java.util.concurrent.Callable;

import com.easygoingapi.yoja.core.YojaApp;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;

/**
 * Internal implementation of off-event-loop task execution using the Vert.x blocking executor.
 * All methods delegate to {@link io.vertx.core.Vertx#executeBlocking}.
 */
class MainWorker {

    /**
     * Executes a task on a blocking thread by wrapping a {@link Promise}-based handler.
     *
     * @param <T>     the result type
     * @param handler a handler that receives a {@link Promise} to complete or fail
     * @return a {@link Future} that resolves with the handler's result
     */
    protected static <T> Future<T> execute(final Handler<Promise<T>> handler) {
        return execute(() -> MainWorker.promise(handler)).compose(f -> f);
    }
    
    /**
     * Executes a {@link Runnable} on a blocking thread.
     *
     * @param runnable the task to run
     * @return a {@link Future} that completes with {@code null} when the runnable finishes
     */
    protected static Future<Void> execute(final Runnable runnable) {
        return execute(() -> run(runnable));
    }

    /**
     * Executes a {@link Callable} on a blocking thread.
     *
     * @param <T>      the result type
     * @param callable the task to call
     * @return a {@link Future} that resolves with the callable's return value
     */
    protected static <T> Future<T> execute(final Callable<T> callable) {
        return YojaApp.vertx().executeBlocking(callable, false);
    }

    /*
     * 
     * UTIL
     * 
     */
    /**
     * Runs a {@link Runnable} and returns {@code null}, adapting it for use as a {@link Callable}.
     *
     * @param runnable the task to run
     * @return always {@code null}
     */
    protected static Void run(final Runnable runnable) {
        runnable.run();
        return null;
    }
    
    /**
     * Wraps a {@link Handler}-based async operation in a {@link Future}.
     * Any exception thrown synchronously by the handler is captured and propagated as a failure.
     *
     * @param <T>     the result type
     * @param handler the handler that will complete or fail the underlying {@link Promise}
     * @return a {@link Future} backed by the promise
     */
    protected static <T> Future<T> promise(final Handler<Promise<T>> handler) {
        final Promise<T> promise = Promise.promise();
        try {
            handler.handle(promise);
        } 
        catch (final Throwable e) {
            promise.fail(e);
        }
        return promise.future();
    }
    
}
