package org.jboss.weld.vertx;

import javax.enterprise.event.Event;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;

/**
 * An instance of this handler is registered per each address found by {@link org.jboss.weld.vertx.VertxExtension}}.
 *
 * @author Martin Kouba
 */
class VertxHandler implements Handler<Message<Object>> {

    private final Vertx vertx;

    private final Event<VertxEvent> event;

    static VertxHandler from(Vertx vertx, Event<Object> event, String address) {
        return new VertxHandler(vertx, event.select(VertxEvent.class, VertxConsumer.Literal.of(address)));
    }

    private VertxHandler(Vertx vertx, Event<VertxEvent> event) {
        this.vertx = vertx;
        this.event = event;
    }

    @Override
    public void handle(Message<Object> message) {
        vertx.<Object> executeBlocking(future -> {
            VertxEventImpl vertxEvent = new VertxEventImpl(message, vertx.eventBus());
            try {
                // Synchronously notify all the observer methods for a specific address
                event.fire(vertxEvent);
                future.complete(vertxEvent.getReply());
            } catch (Exception e) {
                future.fail(e);
            }
        }, result -> {
            if (result.succeeded()) {
                message.reply(result.result());
            } else {
                Throwable cause = result.cause();
                if (cause instanceof RecipientFailure) {
                    RecipientFailure recipientFailure = (RecipientFailure) cause;
                    message.fail(recipientFailure.code, recipientFailure.getMessage());
                } else {
                    message.fail(VertxEvent.OBSERVER_FAILURE_CODE, cause.getMessage());
                }
            }
        });
    }

}