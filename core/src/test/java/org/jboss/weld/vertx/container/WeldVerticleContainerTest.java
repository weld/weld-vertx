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
package org.jboss.weld.vertx.container;

import org.jboss.weld.vertx.WeldVerticle;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 *
 * @author Martin Kouba
 */
@RunWith(VertxUnitRunner.class)
public class WeldVerticleContainerTest {

    private Vertx vertx;

    @Rule
    public Timeout globalTimeout = Timeout.millis(5000);

    @Before
    public void init(TestContext context) throws InterruptedException {
        final WeldVerticle weldVerticle = new WeldVerticle();
        Async async = context.async();
        vertx = Vertx.vertx();
        vertx.deployVerticle(weldVerticle, deployResult -> {
            if (deployResult.succeeded()) {
                // Deploy Verticle instance produced by Weld
                vertx.deployVerticle(weldVerticle.container().select(BeanVerticle.class).get(), (beanVerticleDeployResult) -> {
                    if (beanVerticleDeployResult.succeeded()) {
                        async.complete();
                    }
                });
            }
        });
    }

    @After
    public void close(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testVerticleBeans(TestContext context) throws InterruptedException {
        Async async = context.async();
        vertx.eventBus().send(BeanVerticle.class.getName(), "hello", r -> {
            if (r.succeeded()) {
                context.assertEquals(SayHelloService.MESSAGE, r.result().body());
                async.complete();
            }
        });
    }

}
