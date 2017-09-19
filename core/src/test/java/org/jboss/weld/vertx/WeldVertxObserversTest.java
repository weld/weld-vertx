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
package org.jboss.weld.vertx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Context;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.core.metrics.Measured;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 *
 * @author Martin Kouba
 */
@RunWith(VertxUnitRunner.class)
public class WeldVertxObserversTest {

    private Vertx vertx;

    @Before
    public void init(TestContext context) {
        vertx = Vertx.vertx();
        Async async = context.async();
        vertx.deployVerticle(new WeldVerticle(), r -> {
            if (r.succeeded()) {
                async.complete();
            } else {
                context.fail(r.cause());
            }
        });
        // We don't expect the tests to run in parallel
        VertxObservers.SYNCHRONIZER.clear();
    }

    @After
    public void close(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testPingConsumer() throws InterruptedException {
        vertx.eventBus().send(VertxObservers.TEST_PING, "hello");
        assertEquals("pong", poll());
        vertx.eventBus().publish(VertxObservers.TEST_PING, "hello");
        assertEquals("pong", poll());
    }

    @Test
    public void testEchoConsumer() throws InterruptedException {
        vertx.eventBus().send(VertxObservers.TEST_ECHO, "hello", (r) -> {
            if (r.succeeded()) {
                VertxObservers.SYNCHRONIZER.add(r.result().body());
            }
        });
        assertEquals("hello", poll());
    }

    @Test
    public void testEchoConsumerFails() throws InterruptedException {
        vertx.eventBus().send(VertxObservers.TEST_ECHO, "fail", (r) -> {
            if (r.failed()) {
                VertxObservers.SYNCHRONIZER.add(r.cause());
            }
        });
        Object cause = poll();
        assertNotNull(cause);
        ReplyException replyException = (ReplyException) cause;
        assertEquals(10, replyException.failureCode());
        assertEquals("My failure!", replyException.getMessage());
        assertEquals(ReplyFailure.RECIPIENT_FAILURE, replyException.failureType());
        vertx.eventBus().send(VertxObservers.TEST_ECHO, "exception", (r) -> {
            if (r.failed()) {
                VertxObservers.SYNCHRONIZER.add(r.cause());
            }
        });
        cause = poll();
        assertNotNull(cause);
        replyException = (ReplyException) cause;
        assertEquals(VertxEvent.OBSERVER_FAILURE_CODE, replyException.failureCode());
        assertEquals("oops", replyException.getMessage());
        assertEquals(ReplyFailure.RECIPIENT_FAILURE, replyException.failureType());
    }

    @Test
    public void testConsumerDependencies() throws InterruptedException {
        vertx.eventBus().send(VertxObservers.TEST_DEP, "ok", (r) -> {
            if (r.succeeded()) {
                VertxObservers.SYNCHRONIZER.add(r.result().body());
            }
        });
        Object result1 = poll();
        vertx.eventBus().send(VertxObservers.TEST_DEP, "ok", (r) -> {
            if (r.succeeded()) {
                VertxObservers.SYNCHRONIZER.add(r.result().body());
            }
        });
        Object result2 = poll();
        assertNotEquals(result1, result2);
    }

    @Test
    public void testConsumerEventBus() throws InterruptedException {
        vertx.eventBus().send(VertxObservers.TEST_BUS, "oops");
        // cdi observer sends a message to TEST_BUS_NEXT
        assertEquals("huhu", poll());
        vertx.eventBus().send(VertxObservers.TEST_BUS_OPTIONS, "ignored");
        Object headers = poll();
        assertNotNull(headers);
        assertEquals("bar", ((MultiMap) headers).get("foo"));
    }

    @Test
    public void testConsumerEventBusTimeout() throws InterruptedException {
        vertx.eventBus().send(VertxObservers.TEST_BUS_TIMEOUT, "time out!");
        assertEquals("timeout", poll());
    }

    @Test
    public void testVertxBean() {
        BeanManager beanManager = CDI.current().getBeanManager();
        Bean<?> bean = beanManager.resolve(beanManager.getBeans(Vertx.class));
        assertTrue(bean.getTypes().contains(Vertx.class));
        assertTrue(bean.getTypes().contains(Measured.class));
        Class<?> vertxClass = vertx.getClass();
        for (Class<?> intf : vertxClass.getInterfaces()) {
            assertTrue(bean.getTypes().contains(intf));
        }
    }

    @Test
    public void testContextBean() {
        BeanManager beanManager = CDI.current().getBeanManager();
        Bean<?> bean = beanManager.resolve(beanManager.getBeans(Context.class));
        assertTrue(bean.getTypes().contains(Context.class));
    }

    private Object poll() throws InterruptedException {
        return VertxObservers.SYNCHRONIZER.poll(Timeouts.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

}
