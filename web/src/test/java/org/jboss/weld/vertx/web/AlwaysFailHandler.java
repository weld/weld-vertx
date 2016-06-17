package org.jboss.weld.vertx.web;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@WebRoute("/fail")
public class AlwaysFailHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext ctx) {
        ctx.fail(500);
    }

}
