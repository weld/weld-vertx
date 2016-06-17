package org.jboss.weld.vertx.examples.translator;

import static io.vertx.core.http.HttpMethod.POST;
import static org.jboss.weld.vertx.examples.translator.Addresses.TRANSLATE;

import org.jboss.weld.vertx.web.WebRoute;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

@WebRoute(value = "/translate", httpMethod = POST)
public class TranslateHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TranslateHandler.class.getName());

    @Override
    public void handle(RoutingContext ctx) {
        HttpServerResponse response = ctx.response();
        String sentence = ctx.request().getFormAttribute("sentence");
        if (sentence != null) {
            LOGGER.debug("Handle translation: {0}", sentence);
            // See also Translator#translate()
            ctx.vertx().eventBus().<JsonArray> send(TRANSLATE, sentence, reply -> {
                if (reply.succeeded()) {
                    response.putHeader("Content-type", "application/json");
                    response.setStatusCode(200).end(reply.result().body().encode());
                } else {
                    response.setStatusCode(500).end(reply.cause().toString());
                }
            });
        } else {
            response.end("Use \"application/x-www-form-urlencoded\" content type and specify value for name \"sentence\"");
        }
    }

}
