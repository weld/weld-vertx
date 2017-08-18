/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.jboss.weld.vertx.examples.hello;

import org.jboss.weld.vertx.web.WebRoute;
import org.jboss.weld.vertx.web.WeldWebVerticle;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

/**
 * This simple example shows how to start a very simple "hello" webapp.
 *
 * To access the "hello" endpoint (or just use browser ;-):
 *
 * <pre>
 * curl http://localhost:8080/hello?name=Marv
 * </pre>
 *
 * @author Martin Kouba
 */
public class HelloMain {

	public static void main(String[] args) {
		// First create a new Weld verticle
		// Then deploy the verticle, i.e. start CDI container and discover all @WebRoute
		// handlers
		// If successfull create the router and start the webserver
		Vertx vertx = Vertx.vertx();
		WeldWebVerticle weldVerticle = new WeldWebVerticle();
		vertx.deployVerticle(weldVerticle, result -> {
			if (result.succeeded()) {
				vertx.createHttpServer().requestHandler(weldVerticle.createRouter()::accept).listen(8080);
			} else {
				throw new IllegalStateException("Weld verticle failure: " + result.cause());
			}
		});
	}

	// This handler is registered automatically
	// Matches all HTTP methods and /hello path
	@WebRoute("/hello")
	static class HelloHandler implements Handler<RoutingContext> {

		@Override
		public void handle(RoutingContext event) {
			String name = event.request().getParam("name");
			if (name == null) {
				name = "developer";
			}
			event.response().setStatusCode(200).end("Hello " + name + "!");
		}

	}
}
