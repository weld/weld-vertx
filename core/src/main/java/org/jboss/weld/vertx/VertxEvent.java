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

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;

/**
 * Vertx {@link Message} wrapper.
 * <p>
 * An observer method must declare an event parameter of the type {@link VertxEvent} in order to be recognized as a Vertx message consumer.
 *
 * @author Martin Kouba
 * @see VertxConsumer
 */
public interface VertxEvent {

    /**
     *
     * @return the address the message was sent to
     * @see Message#address()
     */
    String getAddress();

    /**
     *
     * @return the message headers
     * @see Message#headers()
     */
    MultiMap getHeaders();

    /**
     *
     * @return the message body/payload
     * @see Message#body()
     */
    Object getMessageBody();

    /**
     *
     * @return the reply address, or null in case of the message was sent without a reply handler
     * @see Message#replyAddress()
     */
    String getReplyAddress();

    /**
     * If the reply address is null (point-to-point messaging without reply handler) this method is noop.
     *
     * @param reply
     * @see Message#reply(Object)
     */
    void setReply(Object reply);

    /**
     *
     * @param message
     * @see Message#fail(int, String)
     */
    void fail(int code, String message);

    /**
     * Send/publish messages using the Vertx event bus.
     *
     * @param address
     * @return a message
     */
    VertxMessage messageTo(String address);

    /**
     *
     * @author Martin Kouba
     */
    public interface VertxMessage {

        VertxMessage setDeliveryOptions(DeliveryOptions deliveryOptions);

        void send(Object message);

        void send(Object message, Handler<AsyncResult<Message<Object>>> replyHandler);

        void publish(Object message);

    }

}
