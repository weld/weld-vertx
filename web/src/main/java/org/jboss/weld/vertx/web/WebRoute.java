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

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.inject.Stereotype;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * This annotation is used to configure a {@link Route} in a declarative way. The target can be a class which implements {@link Handler} with
 * {@link RoutingContext} as an event type:
 *
 * <pre>
 * &#64;WebRoute("/hello")
 * class HelloHandler implements Handler&lt;RoutingContext&gt; {
 *
 *     public void handle(RoutingContext ctx) {
 *         ctx.response().setStatusCode(200).end("Hello!");
 *     }
 * }
 * </pre>
 *
 * or an observer method which observes {@link RoutingContext}:
 *
 * <pre>
 * &#64;ApplicationScoped
 * class Hello {
 *
 *     &#64;WebRoute("/hello")
 *     void hello(&#64;Observes RoutingContext ctx) {
 *         ctx.response().setStatusCode(200).end("Hello!");
 *     }
 * }
 * </pre>
 *
 * <p>
 * The annotation is repeatable, i.e. multiple annotations may be declared on a handler class or an observer method. In this case, a handler instance or
 * observer method is used for multiple routes.
 * </p>
 *
 * <p>
 * An annotated class which is an inner class or does not implement {@link Handler} with {@link RoutingContext} is ignored.
 * </p>
 *
 * <p>
 * Constructed handler instances are not CDI contextual intances. In other words, they're not managed by the CDI container (similarly as Java EE components like
 * servlets). However, dependency injection, interceptors and decorators are supported.
 * </p>
 *
 * <p>
 * This annotation is annotated with {@link Stereotype} to workaround the limitations of the <code>annotated</code> bean discovery mode.
 * </p>
 *
 * <p>
 * If both {@link #value()} and {@link #regex()} are empty strings a route that matches all requests or failures is created.
 * </p>
 *
 * @author Martin Kouba
 * @see Router
 */
@Repeatable(WebRoutes.class)
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
@Stereotype
public @interface WebRoute {

    /**
     *
     * @see Router#route(String)
     */
    String value() default "";

    /**
     *
     * @see Route#method(HttpMethod)
     */
    HttpMethod[] methods() default {};

    /**
     *
     * @see Router#routeWithRegex(String)
     */
    String regex() default "";

    HandlerType type() default HandlerType.NORMAL;

    /**
     * If set to {@link Integer#MIN_VALUE} the order of the route is not modified.
     */
    int order() default Integer.MIN_VALUE;

    String[] produces() default {};

    String[] consumes() default {};

    enum HandlerType {

        /**
         * A request handler.
         *
         * @see Route#handler(Handler)
         */
        NORMAL,
        /**
         * A blocking request handler.
         *
         * @see Route#blockingHandler(Handler)
         */
        BLOCKING,
        /**
         * A failure handler.
         *
         * @see Route#failureHandler(Handler)
         */
        FAILURE

    }

}
