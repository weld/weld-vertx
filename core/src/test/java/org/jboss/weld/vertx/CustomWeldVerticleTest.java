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
package org.jboss.weld.vertx;

import static org.jboss.weld.vertx.WeldVerticle.createDefaultWeld;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 *
 * @author Martin Kouba
 */
@RunWith(VertxUnitRunner.class)
public class CustomWeldVerticleTest {

    private Vertx vertx;

    private WeldVerticle weldVerticle;

    @Before
    public void init(TestContext context) {
        vertx = Vertx.vertx();
        weldVerticle = new WeldVerticle(createDefaultWeld().disableDiscovery().beanClasses(CoolHelloService.class));
        vertx.deployVerticle(weldVerticle, context.asyncAssertSuccess());
        vertx.createHttpServer().requestHandler(request -> {
            request.response().end("Hello world");
        }).listen(8080, context.asyncAssertSuccess());
    }

    @After
    public void close(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testDiscovery() throws InterruptedException {
        assertTrue(weldVerticle.container().select(CoolService.class).isUnsatisfied());
        assertTrue(weldVerticle.container().select(CoolHelloService.class).isResolvable());
    }

}
