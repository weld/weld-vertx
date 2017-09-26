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
package org.jboss.weld.vertx.web.observer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.vertx.Timeouts;
import org.jboss.weld.vertx.WeldVerticle;
import org.jboss.weld.vertx.web.PaymentService;
import org.jboss.weld.vertx.web.RequestHelloService;
import org.jboss.weld.vertx.web.SayHelloService;
import org.jboss.weld.vertx.web.UniversalFailureHandler;
import org.jboss.weld.vertx.web.WeldWebVerticle;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 *
 * @author Martin Kouba
 */
@RunWith(VertxUnitRunner.class)
public class WebRouteObserversTest {

    static final BlockingQueue<Object> SYNCHRONIZER = new LinkedBlockingQueue<>();

    private Vertx vertx;

    @Rule
    public Timeout globalTimeout = Timeout.millis(Timeouts.GLOBAL_TIMEOUT * 1000);

    @Before
    public void init(TestContext context) throws InterruptedException {
        vertx = Vertx.vertx();
        Async async = context.async();
        Weld weld = WeldVerticle.createDefaultWeld().disableDiscovery().beanClasses(HelloRouteObserver.class, PaymentObserverResource.class,
                SayHelloService.class, PaymentService.class, RequestHelloService.class);
        WeldWebVerticle weldVerticle = new WeldWebVerticle(weld);
        vertx.deployVerticle(weldVerticle, deploy -> {
            if (deploy.succeeded()) {
                // Configure the router after Weld bootstrap finished
                vertx.createHttpServer().requestHandler(weldVerticle.createRouter()::accept).listen(8080, (listen) -> {
                    if (listen.succeeded()) {
                        async.complete();
                    } else {
                        context.fail(listen.cause());
                    }
                });
            } else {
                context.fail(deploy.cause());
            }
        });
        SYNCHRONIZER.clear();
    }

    @After
    public void close(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testHelloObserver() throws InterruptedException {
        HttpClient client = vertx.createHttpClient(new HttpClientOptions().setDefaultPort(8080));
        client.get("/hello").handler(response -> response.bodyHandler(b -> SYNCHRONIZER.add(b.toString()))).end();
        assertEquals(SayHelloService.MESSAGE, poll());
        // Failures
        client.get("/fail/me").handler(response -> response.bodyHandler(b -> SYNCHRONIZER.add(response.statusCode() + ":" + b.toString()))).end();
        assertEquals(500 + ":" + UniversalFailureHandler.TEXT, poll());
    }

    @Test
    public void testPaymentRoutes() throws InterruptedException {
        HttpClient client = vertx.createHttpClient(new HttpClientOptions().setDefaultPort(8080));
        client.get("/payments").handler(r -> r.bodyHandler(b -> SYNCHRONIZER.add(b.toString()))).end();
        String response = poll().toString();
        JsonArray array = new JsonArray(response);
        assertEquals(2, array.size());
        JsonObject fooPayment = array.getJsonObject(0);
        assertEquals("foo", fooPayment.getString("id"));
        assertEquals("1", fooPayment.getString("amount"));
        client.get("/payments/bar").handler(r -> r.bodyHandler(b -> SYNCHRONIZER.add(b.toString()))).end();
        response = poll().toString();
        JsonObject barPayment = new JsonObject(response);
        assertEquals("bar", barPayment.getString("id"));
        assertEquals("100", barPayment.getString("amount"));
    }

    @Test
    public void testRequestContextActive() throws InterruptedException {
        HttpClient client = vertx.createHttpClient(new HttpClientOptions().setDefaultPort(8080));
        client.get(" /request-context-active").handler(response -> response.bodyHandler(b -> SYNCHRONIZER.add(b.toString()))).end();
        String hello1 = poll().toString();
        assertTrue(hello1.startsWith("Hello from"));
        client.get(" /request-context-active").handler(response -> response.bodyHandler(b -> SYNCHRONIZER.add(b.toString()))).end();
        String hello2 = poll().toString();
        assertTrue(hello2.startsWith("Hello from"));
        assertNotEquals(hello1, hello2);
    }

    @Test
    public void testRepeatable() throws InterruptedException {
        HttpClient client = vertx.createHttpClient(new HttpClientOptions().setDefaultPort(8080));
        client.get("/foo").handler(response -> response.bodyHandler(b -> SYNCHRONIZER.add(b.toString()))).end();
        assertEquals("path:/foo", poll());
        client.get("/bar").handler(response -> response.bodyHandler(b -> SYNCHRONIZER.add(b.toString()))).end();
        assertEquals("path:/bar", poll());
    }

    private Object poll() throws InterruptedException {
        return SYNCHRONIZER.poll(Timeouts.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

}
