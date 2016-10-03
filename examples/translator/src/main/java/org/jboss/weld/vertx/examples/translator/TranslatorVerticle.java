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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * {@link Verticle} responsible for starting the application.
 *
 * @author Martin Kouba
 */
public class TranslatorVerticle extends AbstractVerticle {

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        final WeldWebVerticle weldVerticle = new WeldWebVerticle();
        vertx.deployVerticle(weldVerticle, r -> {
            if (r.succeeded()) {
                Router router = Router.router(vertx);
                router.route().handler(BodyHandler.create());
                weldVerticle.registerRoutes(router);
                vertx.createHttpServer().requestHandler(router::accept).listen(8080, (listen) -> {
                    if (listen.succeeded()) {
                        vertx.deployVerticle(new DummyDataVerticle(), (dummy) -> {
                            if (dummy.failed()) {
                                startFuture.fail("Dummy verticle failure");
                            } else {
                                startFuture.complete();
                            }
                        });
                    } else {
                        startFuture.fail("HTTP server not listening");
                    }
                });
            } else {
                startFuture.fail("Weld verticle failure");
            }
        });

    }

}
