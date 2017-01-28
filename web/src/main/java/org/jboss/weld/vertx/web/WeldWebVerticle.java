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
package org.jboss.weld.vertx.web;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.vertx.WeldVerticle;

import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * This verticle extends the {@link WeldVerticle} functionality and allows to register {@link Route} handlers discovered during container initialization.
 * <p>
 *
 * <pre>
 * class MyApp {
 *     public static void main(String[] args) {
 *         final Vertx vertx = Vertx.vertx();
 *         final WeldWebVerticle weldVerticle = new WeldWebVerticle();
 *         vertx.deployVerticle(weldVerticle, result -> {
 *             if (result.succeeded()) {
 *                 // Configure the router after Weld bootstrap finished
 *                 vertx.createHttpServer().requestHandler(weldVerticle.createRouter()::accept).listen(8080);
 *             }
 *         });
 *     }
 * }
 * </pre>
 *
 * @author Martin Kouba
 * @see WebRoute
 */
public class WeldWebVerticle extends WeldVerticle {

    public WeldWebVerticle() {
        super();
    }

    public WeldWebVerticle(Weld weld) {
        super(weld);
    }

    @Override
    protected void configureWeld(Weld weld) {
        weld.addExtension(new RouteExtension());
    }

    /**
     * Registers all the route handlers discovered.
     *
     * @param router
     */
    public void registerRoutes(Router router) {
        container().getBeanManager().getExtension(RouteExtension.class).registerRoutes(router);
    }

    /**
     * Creates a router with {@link BodyHandler} and all discovered route handlers registered.
     * 
     * @return a new router instance
     */
    public Router createRouter() {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        registerRoutes(router);
        return router;
    }

}
