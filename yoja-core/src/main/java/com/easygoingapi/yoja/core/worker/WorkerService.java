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

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.core.YojaApp;
import com.easygoingapi.yoja.core.worker.Worker.Key;
import com.easygoingapi.yoja.core.worker.Worker.Type;
import com.easygoingapi.yoja.core.worker.Worker.WorkerEvent;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.WorkerExecutor;

/**
 * Internal registry for Vert.x worker-executor pools.
 * Manages the lifecycle (creation, retrieval, removal) of {@link Worker} instances
 * keyed by their type and identifier. Used exclusively by {@link Worker}.
 */
public class WorkerService {

    /** Not instantiable. */
    private WorkerService() {}
    
    private final static Logger LOGGER = LoggerFactory.getLogger(WorkerService.class);
    
    private final static Map<Key, Worker> singleThreadWorkers = new ConcurrentHashMap<>();
    private final static Map<Key, Worker> parallelThread = new ConcurrentHashMap<>();
    
    /**
     * Returns the identifiers of all registered workers of the given type.
     *
     * @param type the worker type
     * @return a sorted set of worker IDs
     */
    protected static Set<String> ids(final Worker.Type type) {
        return new TreeSet<>(getMap(type).keySet()
                                         .stream()
                                         .map(Worker.Key::id)
                                         .toList());
    }
    
    /**
     * Returns {@code true} if a worker with the given type and ID is currently registered.
     *
     * @param type the worker type
     * @param id   the worker identifier
     * @return {@code true} if the worker exists
     */
    protected static boolean has(final Worker.Type type,
                                 final String id) {
        return getMap(type).containsKey(new Key(type, id));
    }
    
    /**
     * Returns the registered worker for the given type and ID, or {@code null} if absent.
     *
     * @param type the worker type
     * @param id   the worker identifier
     * @return the {@link Worker}, or {@code null}
     */
    protected static Worker get(final Worker.Type type,
                                final String id) {
        return getMap(type).get(new Key(type, id));
    }
    
    /**
     * Returns the existing worker if its pool size matches, otherwise closes the old one
     * and creates a new shared Vert.x worker executor with the requested pool size.
     *
     * @param type     the worker type
     * @param id       the worker identifier
     * @param poolSize the number of threads in the pool
     * @return the existing or newly created {@link Worker}
     */
    protected static Worker create(final Worker.Type type,
                                   final String id,
                                   final int poolSize) {
        return getMap(type).compute(new Key(type, id), (k, v) -> {
            final Worker result;
            if (v != null && v.poolSize() == poolSize) {
                result = v;
            }
            else { 
                if (v != null) {
                    v.close()
                     .andThen(close -> LOGGER.debug("worker closed; type={}, id={}, poolSize={}",
                                                    type, id, poolSize));
                }
                final String fullId = WorkerService.toId(type, id, poolSize);
                final WorkerExecutor workerExecutor = YojaApp.vertx().createSharedWorkerExecutor(fullId, poolSize);
                result = new Worker(type, id, poolSize, workerExecutor);
            }
            return result;
        });
    }

    /**
     * Removes and closes the worker identified by the given type and ID.
     * Registered {@code onRemove} handlers are invoked asynchronously after the executor is closed.
     *
     * @param type the worker type
     * @param id   the worker identifier
     * @return a {@link Future} that completes when the worker is fully closed
     */
    protected static Future<Void> remove(final Worker.Type type,
                                         final String id) {
        final Worker worker = getMap(type).remove(new Worker.Key(type, id));
        final Future<Void> result;
        if (worker != null) {
            result = worker.close()
            		       .andThen(close -> {
            	Worker.parallelThread.execute(() -> {
            	    for (final Handler<WorkerEvent> handler : worker.onRemoveActions()) {
                        handler.handle(new WorkerEvent(type, id, worker.poolSize()));
                    }
            	});
            });
        }
        else {
            result = Future.succeededFuture();
        }
        return result;
    }
    
    private static Map<Key, Worker> getMap(final Worker.Type type) {
        final Map<Key, Worker> result;
        if (Type.singleThread == type) {
            result = singleThreadWorkers;
        }
        else {
            result = parallelThread;
        }
        return result;
    }
    
    private static String toId(final Worker.Type type, 
                               final String id, 
                               final int poolSize) {
        return type.name().toString() + "_" + poolSize + "_" + id;
    }
    
}
