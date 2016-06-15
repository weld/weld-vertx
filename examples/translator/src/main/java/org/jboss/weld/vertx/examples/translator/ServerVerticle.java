/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.vertx.examples.translator;

import static org.jboss.weld.vertx.examples.translator.Addresses.TRANSLATE;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * {@link Verticle} responsible for starting the HTTP server and routing HTTP requests.
 *
 * @author Martin Kouba
 */
public class ServerVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerVerticle.class.getName());

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.post("/translate").handler(this::handleTranslate);
        router.get("/").handler((r) -> r.response().setStatusCode(200).end("Weld Vert.x translator example running"));
        vertx.createHttpServer().requestHandler(router::accept).listen(8080);
    }

    private void handleTranslate(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        String sentence = routingContext.request().getFormAttribute("sentence");
        if (sentence != null) {
            LOGGER.debug("Handle translation: {0}", sentence);
            // See also Translator#translate()
            vertx.eventBus().<JsonArray> send(TRANSLATE, sentence, r -> {
                if (r.succeeded()) {
                    response.putHeader("Content-type", "application/json");
                    response.setStatusCode(200).end(r.result().body().encode());
                } else {
                    response.setStatusCode(500).end(r.cause().toString());
                }
            });
        } else {
            response.end("Use \"application/x-www-form-urlencoded\" content type and specify value for name \"sentence\"");
        }
    }

}
