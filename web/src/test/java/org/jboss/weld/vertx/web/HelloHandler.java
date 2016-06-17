package org.jboss.weld.vertx.web;

import javax.inject.Inject;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@WebRoute("/hello")
public class HelloHandler implements Handler<RoutingContext> {

    @Inject
    SayHelloService service;

    @Override
    public void handle(RoutingContext ctx) {
        ctx.response().setStatusCode(200).end(service.hello());
    }

}
