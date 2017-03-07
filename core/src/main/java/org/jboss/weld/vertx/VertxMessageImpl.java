package org.jboss.weld.vertx;

import org.jboss.weld.vertx.VertxEvent.VertxMessage;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

class VertxMessageImpl implements VertxMessage {

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