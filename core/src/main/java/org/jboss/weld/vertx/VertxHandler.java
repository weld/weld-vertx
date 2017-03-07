package org.jboss.weld.vertx;

import javax.enterprise.event.Event;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;

/**
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
            try {
                VertxEventImpl vertxEvent = new VertxEventImpl(message, vertx.eventBus());
                // Synchronously notify all the observer methods for a specific address
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
                    message.fail(VertxEvent.OBSERVER_FAILURE_CODE, cause.getMessage());
                }
            }
        });
    }


    static class RecipientFailureException extends Exception {

        private static final long serialVersionUID = 1L;

        final Integer code;

        RecipientFailureException(Integer code, String message) {
            super(message);
            this.code = code;
        }

    }

}