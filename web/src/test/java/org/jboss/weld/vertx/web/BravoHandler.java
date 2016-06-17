package org.jboss.weld.vertx.web;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@WebRoute(value = "/chain", order = 2)
public class BravoHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext ctx) {
        ctx.response().write("bravo").end();
    }

}
