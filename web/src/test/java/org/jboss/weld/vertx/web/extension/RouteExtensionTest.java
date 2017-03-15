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
package org.jboss.weld.vertx.web.extension;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.vertx.web.HelloHandler;
import org.jboss.weld.vertx.web.RouteExtension;
import org.jboss.weld.vertx.web.SayHelloService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

/**
 *
 * @author Martin Kouba
 */
public class RouteExtensionTest {

    static final long DEFAULT_TIMEOUT = 5000;

    static final BlockingQueue<Object> SYNCHRONIZER = new LinkedBlockingQueue<>();

    @Rule
    public Timeout globalTimeout = Timeout.millis(5000);

    @Test
    public void testHandlers() throws InterruptedException {
        try (WeldContainer weld = new Weld().disableDiscovery().addExtension(new RouteExtension()).beanClasses(HelloHandler.class, SayHelloService.class)
                .initialize()) {
            Vertx vertx = Vertx.vertx();
            HttpClient client = vertx.createHttpClient(new HttpClientOptions().setDefaultPort(8080));
            Router router = Router.router(vertx);
            router.route().handler(BodyHandler.create());
            vertx.createHttpServer().requestHandler(router::accept).listen(8080);
            try {
                weld.select(RouteExtension.class).get().registerRoutes(router);
                client.get("/hello").handler(response -> response.bodyHandler(b -> SYNCHRONIZER.add(b.toString()))).end();
                assertEquals(SayHelloService.MESSAGE, SYNCHRONIZER.poll(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS));
            } finally {
                vertx.close();
            }
        }
    }

}
