/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
package org.jboss.weld.vertx.examples.translator;

import org.jboss.weld.vertx.web.WeldWebVerticle;

import io.vertx.core.Vertx;

/**
 * <pre>
 * curl -d "sentence=Hello world" http://localhost:8080/translate
 * </pre>
 *
 * @author Martin Kouba
 */
public class TranslatorExampleRunner {

    public static void main(String[] args) {
        final Vertx vertx = Vertx.vertx();
        final WeldWebVerticle weldVerticle = new WeldWebVerticle();
        vertx.deployVerticle(weldVerticle, r -> {
            if (r.succeeded()) {
                vertx.deployVerticle(new ServerVerticle(weldVerticle));
            }
        });
        vertx.deployVerticle(new DummyDataVerticle());
    }

}
