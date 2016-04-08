package org.jboss.weld.vertx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import javax.enterprise.event.Observes;
import javax.inject.Singleton;

/**
 *
 * @author Martin Kouba
 */
@Singleton
public class VertxObservers {

    static final BlockingQueue<Object> SYNCHRONIZER = new LinkedBlockingQueue<>();

    static final String TEST_PING = "test.ping";
    static final String TEST_ECHO = "test.echo";
    static final String TEST_DEP = "test.dependencies";
    static final String TEST_BUS = "test.bus";

    static final AtomicReference<Object> MESSAGE = new AtomicReference<Object>();

    public void pingConsumer(@Observes @VertxConsumer(TEST_PING) VertxEvent event) {
        assertEquals(TEST_PING, event.getAddress());
        assertNull(event.getReplyAddress());
        SYNCHRONIZER.add("pong");
    }

    public void echoConsumer(@Observes @VertxConsumer(TEST_ECHO) VertxEvent event) {
        assertEquals(TEST_ECHO, event.getAddress());
        assertNotNull(event.getReplyAddress());
        if("fail".equals(event.getMessageBody())) {
            event.fail(10, "My failure!");
        } else if("exception".equals(event.getMessageBody())) {
            throw new IllegalStateException("oops");
        }else {
            event.setReply(event.getMessageBody());
        }
    }

    public void consumerWithDependencies(@Observes @VertxConsumer(TEST_DEP) VertxEvent event, CoolService coolService) {
        assertEquals(TEST_DEP, event.getAddress());
        assertNotNull(event.getReplyAddress());
        assertNotNull(coolService);
        assertNotNull(coolService.getCacheService());
        event.setReply(coolService.getId() + "_" + coolService.getCacheService().getId());
    }

    public void consumerStrikesBack(@Observes @VertxConsumer(TEST_BUS) VertxEvent event) {
        assertEquals(TEST_BUS, event.getAddress());
        event.to(TEST_PING).send("ping");
    }

}
