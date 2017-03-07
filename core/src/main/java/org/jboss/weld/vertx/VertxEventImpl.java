package org.jboss.weld.vertx;

import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

class VertxEventImpl implements VertxEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(VertxEventImpl.class.getName());

    private final EventBus eventBus;

    private final String address;

    private final MultiMap headers;

    private final Object messageBody;

    private final String replyAddress;

    Object reply;

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