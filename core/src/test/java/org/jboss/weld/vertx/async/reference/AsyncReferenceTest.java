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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.util.TypeLiteral;

import org.awaitility.Awaitility;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.vertx.AsyncReference;
import org.jboss.weld.vertx.Timeouts;
import org.jboss.weld.vertx.WeldVerticle;
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
public class AsyncReferenceTest {

    private WeldContainer weld;

    private Vertx vertx;

    @Rule
    public Timeout globalTimeout = Timeout.millis(Timeouts.GLOBAL_TIMEOUT);

    @Before
    public void init(TestContext context) throws InterruptedException {
        final WeldVerticle weldVerticle = new WeldVerticle();
        Async async = context.async();
        vertx = Vertx.vertx();
        vertx.deployVerticle(weldVerticle, r -> {
            if (r.succeeded()) {
                weld = weldVerticle.container();
                async.complete();
            } else {
                context.fail(r.cause());
            }
        });
    }

    @After
    public void close(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testAsyncReference() throws InterruptedException, ExecutionException {
        BlockingFoo.reset();
        BlockingBarProducer.reset();
        Boss boss = weld.select(Boss.class).get();
        assertFalse(boss.foo.isDone());
        assertEquals("", boss.foo.orElse(BlockingFoo.EMPTY).getMessage());
        boss.foo.ifDone((r, t) -> fail("BlockingFoo not complete yet"));
        BlockingFoo.complete("Foo");
        BlockingBarProducer.complete(152);
        Awaitility.await().atMost(Timeouts.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS).until(() -> boss.isReadyToTest());
        assertEquals("Foo", boss.foo.get().getMessage());
        boss.foo.ifDone((r, t) -> assertEquals("Foo", r.getMessage()));
        Throwable cause = boss.unsatisfied.cause();
        assertNotNull(cause);
        assertTrue(cause instanceof UnsatisfiedResolutionException);
        boss.unsatisfied.ifDone((r, t) -> {
            assertNotNull(t);
            assertTrue(t instanceof UnsatisfiedResolutionException);
        });
        assertNull(boss.noBing.get());
        assertEquals(55, boss.juicyBing.get().value);
        assertNull(boss.juicyBar.cause());
        assertEquals(152, boss.juicyBar.get().code);
        assertTrue(BlockingBarProducer.PRODUCER_USED.get());
    }

    @SuppressWarnings("serial")
    @Test
    public void testAsyncReferenceDynamicLookup() throws InterruptedException, ExecutionException {
        BlockingFoo.reset();
        BlockingBarProducer.reset();
        Boss.DESTROYED.set(false);
        List<Boolean> stageResults = new CopyOnWriteArrayList<>();

        AsyncReference<Boss> ref = weld.select(new TypeLiteral<AsyncReference<Boss>>() {
        }).get();
        ref.thenAccept((boss) -> stageResults.add(true));
        assertEquals(0, stageResults.size());

        Awaitility.await().atMost(Timeouts.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS).until(() -> ref.isDone());

        BlockingFoo.complete("Foo");
        BlockingBarProducer.complete(152);

        Awaitility.await().atMost(Timeouts.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS).until(() -> ref.get().isReadyToTest());

        assertEquals(1, stageResults.size());

        ref.whenComplete((r, t) -> {
            if (r != null)
                stageResults.add(true);
        });

        assertEquals(2, stageResults.size());
        assertEquals(152, ref.get().juicyBar.get().code);
        weld.destroy(ref);
        assertTrue(Boss.DESTROYED.get());
    }

    @SuppressWarnings("serial")
    @Test
    public void testAsyncReferenceDynamicLookupSimple(TestContext context) throws InterruptedException {
        BlockingFoo.reset();
        Async async = context.async();
        AtomicBoolean created = new AtomicBoolean(false);

        weld.select(new TypeLiteral<AsyncReference<BlockingFoo>>() {
        }).get().thenAccept((foo) -> {
            context.assertEquals("Foo", foo.getMessage());
            created.set(true);
            async.complete();
        });

        assertFalse(created.get());
        BlockingFoo.complete("Foo");
    }

    @Test
    public void testAsyncReferenceConstructorInject(TestContext context) throws InterruptedException {
        BlockingFoo.reset();
        Baz baz = weld.select(Baz.class).get();
        assertEquals("", baz.getFoo().getMessage());
        BlockingFoo.complete("Foo");
        Awaitility.await().atMost(Timeouts.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS).until(() -> "Foo".equals(baz.getFoo().getMessage()));
    }

    @Test
    public void testAsyncReferenceCompletionStage(TestContext context) throws InterruptedException {
        BlockingFoo.reset();
        Async async = context.async(2);

        Hello hello = weld.select(Hello.class).get();
        hello.hello().thenAccept((m) -> {
            context.assertEquals("Foo", m);
            async.complete();
        });
        BlockingFoo.complete("Foo");
        Awaitility.await().atMost(Timeouts.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS).until(() -> hello.foo.isDone());
        hello.hello().thenAccept((m) -> {
            context.assertEquals("Foo", m);
            async.complete();
        });
    }

    @SuppressWarnings("serial")
    @Test
    public void testAsyncReferenceNormalScoped(TestContext context) throws InterruptedException {
        NormalScopedBlockingFoo.reset();
        Async async = context.async();
        AtomicBoolean created = new AtomicBoolean(false);

        weld.select(new TypeLiteral<AsyncReference<NormalScopedBlockingFoo>>() {
        }).get().thenAccept((foo) -> {
            context.assertTrue(NormalScopedBlockingFoo.created.get());
            context.assertEquals("Foo", foo.getMessage());
            created.set(true);
            async.complete();
        });
        assertFalse(created.get());
        NormalScopedBlockingFoo.complete("Foo");
        Awaitility.await().atMost(Timeouts.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS).until(() -> created.get());
    }
}
