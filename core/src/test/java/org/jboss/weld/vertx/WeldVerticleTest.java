package org.jboss.weld.vertx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 *
 * @author Martin Kouba
 */
@RunWith(VertxUnitRunner.class)
public class WeldVerticleTest {

    private Vertx vertx;

    @Before
    public void init(TestContext context) {
        vertx = Vertx.vertx();
        vertx.deployVerticle(new WeldVerticle(), context.asyncAssertSuccess());
        vertx.createHttpServer().requestHandler(request -> {
            request.response().end("Hello world");
        }).listen(8080);
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
        assertEquals("pong", VertxObservers.SYNCHRONIZER.poll(2, TimeUnit.SECONDS));
        vertx.eventBus().publish(VertxObservers.TEST_PING, "hello");
        assertEquals("pong", VertxObservers.SYNCHRONIZER.poll(2, TimeUnit.SECONDS));
    }

    @Test
    public void testEchoConsumer() throws InterruptedException {
        vertx.eventBus().send(VertxObservers.TEST_ECHO, "hello", (r) -> {
            if (r.succeeded()) {
                VertxObservers.SYNCHRONIZER.add(r.result().body());
            }
        });
        assertEquals("hello", VertxObservers.SYNCHRONIZER.poll(2, TimeUnit.SECONDS));
    }

    @Test
    public void testEchoConsumerFails() throws InterruptedException {
        vertx.eventBus().send(VertxObservers.TEST_ECHO, "fail", (r) -> {
            if (r.failed()) {
                VertxObservers.SYNCHRONIZER.add(r.cause());
            }
        });
        Object cause = VertxObservers.SYNCHRONIZER.poll(2, TimeUnit.SECONDS);
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
        cause = VertxObservers.SYNCHRONIZER.poll(2, TimeUnit.SECONDS);
        assertNotNull(cause);
        replyException = (ReplyException) cause;
        assertEquals(WeldVerticle.OBSERVER_FAILURE_CODE, replyException.failureCode());
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
        Object result1 = VertxObservers.SYNCHRONIZER.poll(60, TimeUnit.SECONDS);
        vertx.eventBus().send(VertxObservers.TEST_DEP, "ok", (r) -> {
            if (r.succeeded()) {
                VertxObservers.SYNCHRONIZER.add(r.result().body());
            }
        });
        Object result2 = VertxObservers.SYNCHRONIZER.poll(2, TimeUnit.SECONDS);
        assertNotEquals(result1, result2);
    }

    @Test
    public void testConsumerEventBus() throws InterruptedException {
        vertx.eventBus().send(VertxObservers.TEST_BUS, "oops");
        // cdi observer sends a message to TEST_BUS_NEXT
        assertEquals("huhu", VertxObservers.SYNCHRONIZER.poll(2, TimeUnit.SECONDS));
    }

}