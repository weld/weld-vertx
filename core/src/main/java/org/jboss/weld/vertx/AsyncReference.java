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

import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

/**
 * Asynchronously processed wrapper of an injectable reference. Can be used to obtain an injectable reference of a bean whose creation involves potentially
 * blocking operations:
 *
 * <pre>
 * &#64;ApplicationScoped
 * class Hello {
 *
 *     &#64;Inject
 *     AsynReference&lt;ServiceWithBlockingInit&gt; service;
 *
 *     CompletionStage&lt;String&gt; hello() {
 *         return service.thenApply((s) -> "Hello" + s.getName() + "!");
 *     }
 * }
 * </pre>
 *
 * <p>
 * If there is a producer method whose return type is {@link CompletionStage} where the result type matches the required type and has all the required qualifers
 * (according to type-safe resolution rules) then {@link CompletionStage#whenComplete(java.util.function.BiConsumer)} is used to process the reference.
 * Otherwise, a worker thread is used so that the processing does not block the event loop thread.
 * </p>
 *
 * <p>
 * For a normal scoped bean the contextual instance is initialized eagerly (unlike when performing normal dependency injection).
 * </p>
 *
 * <p>
 * No method in this interface waits for completion. This interface also implements {@link CompletionStage} with an injectable reference as the result.
 * </p>
 *
 * @author Martin Kouba
 * @param <T> the required type
 */
public interface AsyncReference<T> extends CompletionStage<T> {

    /**
     *
     * @return {@code true} if an injectable reference was obtained, {@code false} otherwise
     */
    boolean isDone();

    /**
     * Gets the reference.
     *
     * @return the reference, might be {@code null}
     */
    T get();

    /**
     * Gets the reference or the default value.
     *
     * @param defaultValue
     * @return the reference or the default value if the reference is {@code null}
     */
    default T orElse(T defaultValue) {
        T value = get();
        return value != null ? value : defaultValue;
    }

    /**
     * If {@link #isDone()} returns <code>true</code>, invoke the specified consumer with the reference (may be {@code null}) and the exception (or {@code null}
     * if processed successfully).
     *
     * @param consumer
     */
    default void ifDone(BiConsumer<? super T, ? super Throwable> consumer) {
        if (isDone()) {
            consumer.accept(get(), cause());
        }
    }

    /**
     * Gets the cause in case of a failure occurs during processing.
     *
     * @return the cause or {@code null} if processed sucessfully
     */
    Throwable cause();

}
