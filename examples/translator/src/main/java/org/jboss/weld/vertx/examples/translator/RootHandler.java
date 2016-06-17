package org.jboss.weld.vertx.examples.translator;

import org.jboss.weld.vertx.web.WebRoute;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@WebRoute("/")
public class RootHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext ctx) {
        ctx.response().setStatusCode(200).end("Weld Vert.x translator example running...");
    }

}
