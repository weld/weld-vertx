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

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.vertx.Timeouts;
import org.jboss.weld.vertx.web.HelloHandler;
import org.jboss.weld.vertx.web.RouteExtension;
import org.jboss.weld.vertx.web.SayHelloService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

/**
 *
 * @author Martin Kouba
 */
@RunWith(VertxUnitRunner.class)
public class RouteExtensionTest {

    @Rule
    public Timeout globalTimeout = Timeout.millis(Timeouts.GLOBAL_TIMEOUT);

    private WeldContainer weld;

    private Vertx vertx;

    @Before
    public void init(TestContext context) throws InterruptedException {
        weld = new Weld().disableDiscovery().addExtension(new RouteExtension()).beanClasses(HelloHandler.class, SayHelloService.class).initialize();
        vertx = Vertx.vertx();
        Async async = context.async();
        Router router = Router.router(vertx);
        weld.select(RouteExtension.class).get().registerRoutes(router);
        router.route().handler(BodyHandler.create());
        vertx.createHttpServer().requestHandler(router::accept).listen(8080, (r) -> {
            if (r.succeeded()) {
                async.complete();
            } else {
                context.fail(r.cause());
            }
        });
    }

    @After
    public void close(TestContext context) {
        if (vertx != null) {
            vertx.close(context.asyncAssertSuccess());
        }
        if (weld != null) {
            weld.shutdown();
        }
    }

    @Test
    public void testHandlers(TestContext context) throws InterruptedException {
        Async async = context.async();
        HttpClient client = vertx.createHttpClient(new HttpClientOptions().setDefaultPort(8080));
        client.get("/hello").handler(response -> response.bodyHandler(b -> {
            context.assertEquals(SayHelloService.MESSAGE, b.toString());
            async.complete();
        })).end();
    }

}
