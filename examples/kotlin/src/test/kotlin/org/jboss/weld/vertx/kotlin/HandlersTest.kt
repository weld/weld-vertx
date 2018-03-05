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

import io.vertx.core.Vertx
import io.vertx.core.http.HttpClientResponse
import io.vertx.core.http.HttpMethod
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.jboss.weld.vertx.web.WeldWebVerticle
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

const val DEFAULT_TIMEOUT: Long = 5000;

@RunWith(VertxUnitRunner::class)
class HandlersTest {

    lateinit var vertx: Vertx;

    @Before
    fun setup(context: TestContext) {
        val async = context.async()
        vertx = Vertx.vertx()
        val weldVerticle = WeldWebVerticle()
        vertx.deployVerticle(weldVerticle, {
            if (it.succeeded()) {
                vertx.createHttpServer().requestHandler(weldVerticle.createRouter()::accept).listen(8080)
                async.complete()
            } else {
                context.fail(it.cause());
            }
        })
    }

    @After
    fun close(context: TestContext) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test(timeout = DEFAULT_TIMEOUT)
    fun testUppercase(context: TestContext) {
        val async = context.async()
        val client = vertx.createHttpClient()
        val request = client.request(HttpMethod.POST, 8080, "localhost", "/upperCase?value=Lu")

        request.handler({ response: HttpClientResponse ->
            if (response.statusCode() == 200) {
                response.bodyHandler({ buffer ->
                    context.assertEquals("LU", buffer.toString());
                    client.close();
                    async.complete();
                })
            }
        })
        request.end()
    }

    @Test(timeout = DEFAULT_TIMEOUT)
    fun testLowercase(context: TestContext) {
        val async = context.async()
        val client = vertx.createHttpClient()
        val request = client.request(HttpMethod.POST, 8080, "localhost", "/lowerCase?value=Lu")

        request.handler({ response: HttpClientResponse ->
            if (response.statusCode() == 200) {
                response.bodyHandler({ buffer ->
                    context.assertEquals("lu", buffer.toString());
                    client.close();
                    async.complete();
                })
            }
        })
        request.end()
    }

}