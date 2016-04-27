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

import javax.enterprise.event.Event;

import org.jboss.weld.config.ConfigurationKey;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.vertx.VertxEvent.VertxMessage;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 *
 * @author Martin Kouba
 */
public class WeldVerticle extends AbstractVerticle {

    public static final int OBSERVER_FAILURE_CODE = 0x1B00;

    private static final Logger LOGGER = LoggerFactory.getLogger(WeldVerticle.class.getName());

    private WeldContainer weldContainer;

    @Override
    public void start() throws Exception {
        VertxExtension vertxExtension = new VertxExtension(vertx, context);
        WeldContainer weldContainer = new Weld().containerId(deploymentID()).property(ConfigurationKey.CONCURRENT_DEPLOYMENT.get(), false)
                .addExtension(vertxExtension).initialize();
        for (String address : vertxExtension.getConsumerAddresses()) {
            vertx.eventBus().consumer(address, VertxHandler.from(vertx, weldContainer, address));
        }
        this.weldContainer = weldContainer;
        LOGGER.info("Weld verticle started for deployment {0}", deploymentID());
    }

    @Override
    public void stop() throws Exception {
        if (weldContainer != null) {
            weldContainer.shutdown();
        }
    }

    static class VertxHandler implements Handler<Message<Object>> {

        private final Vertx vertx;

        private final Event<VertxEvent> event;

        static VertxHandler from(Vertx vertx, WeldContainer weldContainer, String address) {
            return new VertxHandler(vertx, weldContainer.event().select(VertxEvent.class, VertxConsumer.Literal.of(address)));
        }

        private VertxHandler(Vertx vertx, Event<VertxEvent> event) {
            this.vertx = vertx;
            this.event = event;
        }

        @Override
        public void handle(Message<Object> message) {
            vertx.<Object> executeBlocking(future -> {
                try {
                    VertxEventImpl vertxEvent = new VertxEventImpl(message, vertx.eventBus());
                    event.fire(vertxEvent);
                    if (vertxEvent.isFailure()) {
                        future.fail(new RecipientFailureException(vertxEvent.getFailureCode(), vertxEvent.getFailureMessage()));
                    } else {
                        future.complete(vertxEvent.reply);
                    }
                } catch (Exception e) {
                    future.fail(e);
                }
            }, result -> {
                if (result.succeeded()) {
                    message.reply(result.result());
                } else {
                    Throwable cause = result.cause();
                    if (cause instanceof RecipientFailureException) {
                        RecipientFailureException recipientFailure = (RecipientFailureException) cause;
                        message.fail(recipientFailure.code, recipientFailure.getMessage());
                    } else {
                        message.fail(OBSERVER_FAILURE_CODE, cause.getMessage());
                    }
                }
            });
        }

    }

    static class VertxEventImpl implements VertxEvent {

        private static final Logger LOGGER = LoggerFactory.getLogger(VertxEventImpl.class.getName());

        private final EventBus eventBus;

        private final String address;

        private final MultiMap headers;

        private final Object messageBody;

        private final String replyAddress;

        private Object reply;

        private Integer failureCode;

        private String failureMessage;

        VertxEventImpl(Message<Object> message, EventBus eventBus) {
            this.address = message.address();
            this.headers = message.headers();
            this.messageBody = message.body();
            this.replyAddress = message.replyAddress();
            this.eventBus = eventBus;
        }

        @Override
        public String getAddress() {
            return address;
        }

        @Override
        public MultiMap getHeaders() {
            return headers;
        }

        @Override
        public Object getMessageBody() {
            return messageBody;
        }

        @Override
        public String getReplyAddress() {
            return replyAddress;
        }

        @Override
        public void setReply(Object reply) {
            if (replyAddress == null) {
                LOGGER.warn("The message was sent without a reply handler - the reply will be ignored");
            }
            this.reply = reply;
        }

        @Override
        public void fail(int code, String message) {
            this.failureCode = code;
            this.failureMessage = message;
        }

        boolean isFailure() {
            return failureCode != null;
        }

        Integer getFailureCode() {
            return failureCode;
        }

        String getFailureMessage() {
            return failureMessage;
        }

        @Override
        public VertxMessage messageTo(String address) {
            return new VertxMessageImpl(address, eventBus);
        }

    }

    static class VertxMessageImpl implements VertxMessage {

        private final String address;

        private final EventBus eventBus;

        private DeliveryOptions deliveryOptions;

        VertxMessageImpl(String address, EventBus eventBus) {
            this.address = address;
            this.eventBus = eventBus;
        }

        @Override
        public VertxMessage setDeliveryOptions(DeliveryOptions deliveryOptions) {
            this.deliveryOptions = deliveryOptions;
            return this;
        }

        @Override
        public void send(Object message) {
            if (deliveryOptions != null) {
                eventBus.send(address, message);
            } else {
                eventBus.send(address, message, deliveryOptions);
            }

        }

        @Override
        public void send(Object message, Handler<AsyncResult<Message<Object>>> replyHandler) {
            if (deliveryOptions != null) {
                eventBus.send(address, message, deliveryOptions, replyHandler);
            } else {
                eventBus.send(address, message, replyHandler);
            }
        }

        @Override
        public void publish(Object message) {
            if (deliveryOptions != null) {
                eventBus.publish(address, message, deliveryOptions);
            } else {
                eventBus.publish(address, message);
            }
        }

    }

    private static class RecipientFailureException extends Exception {

        private static final long serialVersionUID = 1L;

        private final Integer code;

        RecipientFailureException(Integer code, String message) {
            super(message);
            this.code = code;
        }

    }

}
