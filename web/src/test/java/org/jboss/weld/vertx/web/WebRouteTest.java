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
package org.jboss.weld.vertx.web;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

/**
 *
 * @author Martin Kouba
 */
@RunWith(VertxUnitRunner.class)
public class WebRouteTest {

    static final BlockingQueue<Object> SYNCHRONIZER = new LinkedBlockingQueue<>();

    private Vertx vertx;

    @Before
    public void init(TestContext context) throws InterruptedException {
        vertx = Vertx.vertx();
        final WeldWebVerticle weldVerticle = new WeldWebVerticle();
        vertx.deployVerticle(weldVerticle, result -> {
            if (result.succeeded()) {
                // Configure router after Weld bootstrap finished
                Router router = Router.router(vertx);
                router.route().handler(BodyHandler.create());
                weldVerticle.registerRoutes(router);
                vertx.createHttpServer().requestHandler(router::accept).listen(8080);
                SYNCHRONIZER.add(true);
            }
        });
        SYNCHRONIZER.poll(200, TimeUnit.SECONDS);
        // We don't expect the tests to run in parallel
        SYNCHRONIZER.clear();
    }

    @After
    public void close(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testHelloHandler() throws InterruptedException {
        HttpClient client = vertx.createHttpClient(new HttpClientOptions().setDefaultPort(8080));
        client.get("/hello").handler(response -> response.bodyHandler(b -> SYNCHRONIZER.add(b.toString()))).end();
        assertEquals(SayHelloService.MESSAGE, SYNCHRONIZER.poll(2, TimeUnit.SECONDS));
        client.get("/fail/me").handler(response -> SYNCHRONIZER.add(response.statusCode())).end();
        assertEquals(500, SYNCHRONIZER.poll(2, TimeUnit.SECONDS));
    }

    @Test
    public void testOrder() throws InterruptedException {
        HttpClient client = vertx.createHttpClient(new HttpClientOptions().setDefaultPort(8080));
        client.get("/chain").handler(response -> response.bodyHandler(b -> SYNCHRONIZER.add(b.toString()))).end();
        assertEquals("alphabravo", SYNCHRONIZER.poll(2, TimeUnit.SECONDS));
    }
}
