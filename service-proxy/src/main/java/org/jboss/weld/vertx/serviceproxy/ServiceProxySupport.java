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
package org.jboss.weld.vertx.serviceproxy;

import java.util.concurrent.Executor;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;

/**
 * A bean with this type and {@link javax.enterprise.inject.Default} qualifier must be available if using <tt>weld-vertx-service-proxy</tt>.
 *
 * @author Martin Kouba
 */
public interface ServiceProxySupport {

    /**
     *
     * @return the vertx instance
     * @see #getExecutor()
     */
    Vertx getVertx();

    /**
     *
     * @param serviceInterface
     * @return the delivery options used for a particular service proxy bean instance or <code>null</code>
     */
    default DeliveryOptions getDefaultDeliveryOptions(Class<?> serviceInterface) {
        return null;
    }

    /**
     *
     * @return the executor used to execute a service result handler
     */
    default Executor getExecutor() {
        return new Executor() {

            @Override
            public void execute(Runnable command) {
                getVertx().executeBlocking((f) -> {
                    try {
                        command.run();
                        f.complete();
                    } catch (Exception e) {
                        f.fail(e);
                    }
                }, null);
            }
        };
    }

}
