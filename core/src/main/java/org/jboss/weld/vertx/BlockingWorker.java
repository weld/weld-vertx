/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.vertx;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import io.vertx.core.Vertx;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

/**
 * Allows to perform potentially blocking operations using a Vertx worker thread.
 *
 * <pre>
 * &#64;ApplicationScoped
 * class Hello {
 *
 *     &#64;Inject
 *     BlockingWorker worker;
 *
 *     &#64;Inject
 *     Service service;
 *
 *     CompletionStage&lt;String&gt; hello() {
 *         return worker.perform(() -> service.getNameFromFile()).thenApply((name) -> "Hello " + name + "!");
 *     }
 *
 * }
 * </pre>
 *
 * @author Martin Kouba
 * @see Vertx#executeBlocking(io.vertx.core.Handler, io.vertx.core.Handler)
 */
@Dependent
public class BlockingWorker {

    /**
     * Returns a non-contextual instance.
     *
     * @param vertx
     * @return a new worker instance
     */
    public static BlockingWorker from(Vertx vertx) {
        return new BlockingWorker(vertx);
    }

    private final Vertx vertx;

    @Inject
    public BlockingWorker(Vertx vertx) {
        this.vertx = vertx;
    }

    /**
     *
     * @param action
     * @return a completion stage with the result of the specified action
     */
    public <V> CompletionStage<V> perform(Callable<V> action) {
        VertxCompletableFuture<V> future = new VertxCompletableFuture<>(vertx);
        vertx.<V> executeBlocking((f -> {
            try {
                f.complete(action.call());
            } catch (Exception e) {
                f.fail(e);
            }
        }), (r) -> {
            if (r.succeeded()) {
                future.complete(r.result());
            } else {
                future.completeExceptionally(r.cause());
            }
        });
        return future;
    }

}
