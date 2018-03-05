/*
 * JBoss, Home of Professional Open Source
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
package org.jboss.weld.vertx.kotlin

import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.ext.web.RoutingContext
import org.jboss.weld.vertx.web.WebRoute
import org.jboss.weld.vertx.web.WeldWebVerticle
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.context.Dependent
import javax.enterprise.event.Observes
import javax.inject.Inject

/**
 * This bean only exists to demonstrate injection into handlers and observers!
 */
@Dependent
class CaseService {

    fun upper(value: String): String = value.toUpperCase()

    fun lower(value: String): String = value.toLowerCase()

}

@ApplicationScoped
open class Observers {

    @WebRoute("/lowerCase")
    fun lower(@Observes ctx: RoutingContext, service: CaseService) {
        val value = ctx.request().getParam("value")
        if (value != null) {
            ctx.response().setStatusCode(200).end(service.lower(value))
        } else {
            ctx.response().setStatusCode(400).end("Value not specified")
        }
    }
}

@WebRoute("/upperCase")
class UpperHandler @Inject constructor(private val service: CaseService) : Handler<RoutingContext> {

    override fun handle(ctx: RoutingContext) {
        val value = ctx.request().getParam("value")
        if (value != null) {
            ctx.response().setStatusCode(200).end(service.upper(value))
        } else {
            ctx.response().setStatusCode(400).end("Value not specified")
        }
    }
}

fun main(args: Array<String>) {
    // Create and deploy Weld verticle, i.e. start CDI container and discover all @WebRoute handlers
    val vertx = Vertx.vertx()
    val weldVerticle = WeldWebVerticle()

    vertx.deployVerticle(weldVerticle, {
        if (it.succeeded()) {
            // If successfull create the router and start the webserver
            vertx.createHttpServer().requestHandler(weldVerticle.createRouter()::accept).listen(8080)
        } else {
            throw IllegalStateException("Weld verticle failure: " + it.cause())
        }
    }
    )
}