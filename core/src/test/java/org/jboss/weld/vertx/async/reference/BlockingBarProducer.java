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
package org.jboss.weld.vertx.async.reference;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class BlockingBarProducer {

    static final AtomicBoolean PRODUCER_USED = new AtomicBoolean(false);

    static CompletableFuture<BlockingBar> future;

    static void complete(int code) {
        future.complete(new BlockingBar(code));
    }

    static void reset() {
        future = new CompletableFuture<>();
        PRODUCER_USED.set(false);
    }

    @Produces
    @Dependent
    @Juicy
    CompletionStage<BlockingBar> juicyBlockingBar() {
        PRODUCER_USED.set(true);
        return future;
    }

    // Just to verify VertxExtension.AsyncProducerMetadata.matches(Type, Set<Annotation>)
    @Produces
    @Dependent
    @Default
    CompletionStage<BlockingBar> defaultBlockingBar() {
        return CompletableFuture.completedFuture(new BlockingBar(Integer.MIN_VALUE));
    }

    static class BlockingBar {

        int code;

        public BlockingBar(int code) {
            this.code = code;
        }

    }

}
