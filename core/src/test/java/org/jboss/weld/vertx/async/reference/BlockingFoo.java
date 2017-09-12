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

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;

import org.jboss.weld.vertx.Timeouts;

@Dependent
public class BlockingFoo {

    static final BlockingFoo EMPTY;

    static {
        EMPTY = new BlockingFoo();
        EMPTY.message = "";
    }

    private static CompletableFuture<String> future;

    static void complete(String value) {
        future.complete(value);
    }

    static void reset() {
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
    }

    String getMessage() {
        return message;
    }

}
