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
package org.jboss.weld.vertx.serviceproxy;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jboss.weld.vertx.Timeouts;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 *
 * @author Martin Kouba
 */
@RunWith(VertxUnitRunner.class)
public class EchoServiceProxyTest {

    static final BlockingQueue<Object> SYNCHRONIZER = new LinkedBlockingQueue<>();

    private Vertx vertx;

    private EchoServiceVerticle echoVerticle;

    @Rule
    public Timeout globalTimeout = Timeout.millis(Timeouts.GLOBAL_TIMEOUT);

    @Before
    public void init(TestContext context) throws InterruptedException {
    	Async async = context.async();
    	vertx = Vertx.vertx();
        echoVerticle = new EchoServiceVerticle();
        vertx.deployVerticle(echoVerticle, r -> {
            if (r.succeeded()) {
                async.complete();
            } else {
                context.fail(r.cause());
            }
        });
        SYNCHRONIZER.clear();
    }

    @After
    public void close(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testEchoServiceProxy() throws InterruptedException {
        String message = "foooo";
        echoVerticle.container().select(EchoServiceConsumer.class).get().doEchoBusiness(message);
        assertEquals(message, poll());
    }

    @Test
    public void testEchoServiceProxyDynamicLookup() throws InterruptedException {
        String message = "foooo";
        // Lookup service proxy with ServiceProxy qualifier literal
        echoVerticle.container().select(EchoService.class, ServiceProxy.Literal.of("echo-service-address")).get().echo(message,
                (r) -> SYNCHRONIZER.add(r.result()));
        assertEquals(message, poll());
    }

    private Object poll() throws InterruptedException {
        return SYNCHRONIZER.poll(Timeouts.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

}
