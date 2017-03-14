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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.eventbus.ReplyFailure;

/**
 *
 * @author Martin Kouba
 */
@ApplicationScoped
public class VertxObservers {

    static final BlockingQueue<Object> SYNCHRONIZER = new LinkedBlockingQueue<>();

    static final String TEST_PING = "test.ping";
    static final String TEST_ECHO = "test.echo";
    static final String TEST_DEP = "test.dependencies";
    static final String TEST_BUS = "test.bus";
    static final String TEST_BUS_NEXT = "test.bus.next";
    static final String TEST_BUS_TIMEOUT = "test.bus.timeout";
    static final String TEST_SLOW_HANDLER = "test.slow.handler";

    public void pingConsumer(@Observes @VertxConsumer(TEST_PING) VertxEvent event) {
        assertEquals(TEST_PING, event.getAddress());
        assertNull(event.getReplyAddress());
        SYNCHRONIZER.add("pong");
    }

    public void echoConsumer(@Observes @VertxConsumer(TEST_ECHO) VertxEvent event) {
        assertEquals(TEST_ECHO, event.getAddress());
        assertNotNull(event.getReplyAddress());
        if ("fail".equals(event.getMessageBody())) {
            event.fail(10, "My failure!");
        } else if ("exception".equals(event.getMessageBody())) {
            throw new IllegalStateException("oops");
        } else {
            event.reply(event.getMessageBody());
        }
    }

    public void consumerWithDependencies(@Observes @VertxConsumer(TEST_DEP) VertxEvent event, CoolService coolService) {
        assertEquals(TEST_DEP, event.getAddress());
        assertNotNull(event.getReplyAddress());
        assertNotNull(coolService);
        assertNotNull(coolService.getCacheService());
        assertNotNull(coolService.getVertx().eventBus());
        assertNotNull(coolService.getContext().deploymentID());
        event.setReply(coolService.getId() + "_" + coolService.getCacheService().getId());
        assertTrue(event.isReplied());
        assertFalse(event.isFailure());
    }

    public void consumerStrikesBack(@Observes @VertxConsumer(TEST_BUS) VertxEvent event) {
        assertEquals(TEST_BUS, event.getAddress());
        event.messageTo(TEST_BUS_NEXT).send("ping", r -> {
            if (r.succeeded())
                SYNCHRONIZER.add("huhu");
        });
    }

    public void consumerNext(@Observes @VertxConsumer(TEST_BUS_NEXT) VertxEvent event) {
        assertEquals(TEST_BUS_NEXT, event.getAddress());
        assertNotNull(event.getReplyAddress());
    }

    public void consumerSendTimeout(@Observes @VertxConsumer(TEST_BUS_TIMEOUT) VertxEvent event) {
        assertEquals(TEST_BUS_TIMEOUT, event.getAddress());
        event.messageTo(TEST_SLOW_HANDLER).setDeliveryOptions(new DeliveryOptions().setSendTimeout(10)).send("foo", (r) -> {
            if (r.failed()) {
                ReplyException exception = (ReplyException) r.cause();
                if (exception.failureType().equals(ReplyFailure.TIMEOUT)) {
                    SYNCHRONIZER.add("timeout");
                }
            }
        });
    }

    public void consumerSlow(@Observes @VertxConsumer(TEST_SLOW_HANDLER) VertxEvent event) throws InterruptedException {
        assertEquals(TEST_SLOW_HANDLER, event.getAddress());
        Thread.sleep(100);
    }

}
