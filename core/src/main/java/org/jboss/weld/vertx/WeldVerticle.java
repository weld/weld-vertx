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
package org.jboss.weld.vertx;

import javax.enterprise.inject.Vetoed;

import org.jboss.weld.config.ConfigurationKey;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * This Verticle starts/stops the Weld SE container and registers {@link VertxExtension} automatically.
 *
 * @author Martin Kouba
 * @see VertxExtension
 */
@Vetoed
public class WeldVerticle extends AbstractVerticle {

    /**
     *
     * @return a default {@link Weld} builder used to configure the Weld container
     */
    public static Weld createDefaultWeld() {
        return new Weld().property(ConfigurationKey.CONCURRENT_DEPLOYMENT.get(), false);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(WeldVerticle.class.getName());

    private final Weld weld;

    private volatile WeldContainer weldContainer;

    /**
     *
     */
    public WeldVerticle() {
        this(null);
    }

    /**
     *
     * @param weld
     */
    public WeldVerticle(Weld weld) {
        this.weld = weld;
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        Weld weld = this.weld != null ? this.weld : createDefaultWeld();
        if (weld.getContainerId() == null) {
            weld.containerId(deploymentID());
        }
        weld.addExtension(new VertxExtension(vertx, context));
        configureWeld(weld);
        // Bootstrap can take some time to complete
        vertx.executeBlocking(future -> {
            try {
                this.weldContainer = weld.initialize();
                future.complete();
            } catch (Exception e) {
                future.fail(e);
            }
        }, result -> {
            if (result.succeeded()) {
                LOGGER.info("Weld verticle started for deployment {0}", deploymentID());
                startFuture.complete();
            } else {
                startFuture.fail(result.cause());
            }
        });
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        if (weldContainer != null && weldContainer.isRunning()) {
            // Shutdown can take some time to complete
            vertx.executeBlocking(future -> {
                try {
                    weldContainer.shutdown();
                    future.complete();
                } catch (Exception e) {
                    future.fail(e);
                }
            }, result -> {
                if (result.succeeded()) {
                    stopFuture.complete();
                } else {
                    stopFuture.fail(result.cause());
                }
            });
        } else {
            stopFuture.complete();
        }
    }

    /**
     * Provides convenient access to beans, BeanManager and events.
     * <p>
     * E.g. allows to deploy Verticle instances produced/injected by Weld:
     *
     * <pre>
     * &#64;Dependent
     * class MyBeanVerticle extends AbstractVerticle {
     *
     *     &#64;Inject
     *     Service service;
     *
     *     &#64;Override
     *     public void start() throws Exception {
     *         vertx.eventBus().consumer("my.address").handler(m -> m.reply(service.process(m.body())));
     *     }
     * }
     *
     * class MyApp {
     *     public static void main(String[] args) {
     *         final Vertx vertx = Vertx.vertx();
     *         final WeldVerticle weldVerticle = new WeldVerticle();
     *         vertx.deployVerticle(weldVerticle, result -> {
     *             if (result.succeeded()) {
     *                 // Deploy Verticle instance produced by Weld
     *                 vertx.deployVerticle(weldVerticle.container().select(MyBeanVerticle.class).get());
     *             }
     *         });
     *     }
     * }
     * </pre>
     *
     * @return the Weld container
     * @throws IllegalStateException If the container is not initialized or already shut down
     */
    public WeldContainer container() {
        checkContainer();
        return weldContainer;
    }

    /**
     * Subclass may override this method to customize the Weld SE container.
     *
     * @param weld
     */
    protected void configureWeld(Weld weld) {
    }

    private void checkContainer() {
        if (weldContainer == null || !weldContainer.isRunning()) {
            throw new IllegalStateException("Weld container is not initialized or already shut down");
        }
    }

}
