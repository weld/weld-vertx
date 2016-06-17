package org.jboss.weld.vertx.web;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@WebRoute(value = "/chain", order = 1)
public class AlphaHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext ctx) {
        ctx.response().setChunked(true).write("alpha");
        ctx.next();
    }

}
