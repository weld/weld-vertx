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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.jboss.weld.vertx.Timeouts;

@ApplicationScoped
public class NormalScopedBlockingFoo {

    static AtomicBoolean created;

    private static CompletableFuture<String> future;

    static void complete(String value) {
        future.complete(value);
    }

    static void reset() {
        created = new AtomicBoolean(false);
        future = new CompletableFuture<>();
    }

    private String message;

    @PostConstruct
    void init() {
        try {
            // Simulate blocking operation
            message = future.get(Timeouts.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ignored) {
        }
        created.set(true);
    }

    String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "normal";
    }


}
