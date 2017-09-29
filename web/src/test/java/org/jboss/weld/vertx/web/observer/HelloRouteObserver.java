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
package org.jboss.weld.vertx.web.observer;

import static org.jboss.weld.vertx.web.WebRoute.HandlerType.BLOCKING;
import static org.jboss.weld.vertx.web.WebRoute.HandlerType.FAILURE;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.jboss.weld.context.activator.ActivateRequestContext;
import org.jboss.weld.vertx.web.RequestHelloService;
import org.jboss.weld.vertx.web.SayHelloService;
import org.jboss.weld.vertx.web.WebRoute;

import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class HelloRouteObserver {

    @WebRoute("/hello")
    void hello(@Observes RoutingContext ctx, SayHelloService service) {
        ctx.response().setStatusCode(200).end(service.hello());
    }

    @WebRoute("/foo")
    @WebRoute("/bar")
    void fooAndBar(@Observes RoutingContext ctx) {
        ctx.response().setStatusCode(200).end("path:" + ctx.request().path());
    }

    @WebRoute("/fail/*")
    void alwaysFail(@Observes RoutingContext ctx) {
        throw new IllegalStateException();
    }

    @WebRoute(type = FAILURE)
    void universalFailure(@Observes RoutingContext ctx) {
        ctx.response().setStatusCode(500).end(ctx.request().path());
    }

    @WebRoute(value = "/request-context-active", type = BLOCKING)
    @ActivateRequestContext
    void withActiveRequestContext(@Observes RoutingContext ctx, RequestHelloService helloService) {
        ctx.response().setStatusCode(200).end(helloService.hello());
    }

    @WebRoute(value = "/hello-chain", order = 2)
    void helloChain(@Observes RoutingContext ctx) {
        ctx.response().setStatusCode(200).end("ok");
    }

    @WebRoute(value = "/hello-chain", order = 1)
    void helloChainIgnored(RoutingContext ctx) {
        ctx.fail(500);
    }

}
