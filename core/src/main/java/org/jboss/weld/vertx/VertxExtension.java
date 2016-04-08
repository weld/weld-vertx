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

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessObserverMethod;

/**
 * This extension detects all the observer methods that should become message consumers.
 *
 * @author Martin Kouba
 */
public class VertxExtension implements Extension {

    private static final Logger LOGGER = Logger.getLogger(VertxExtension.class.getName());

    private final Set<String> consumerAddresses;

    public VertxExtension() {
        this.consumerAddresses = new HashSet<>();
    }

    public void detectMessageConsumers(@Observes ProcessObserverMethod<VertxEvent, ?> event) {
        String vertxAddress = getVertxAddress(event.getObserverMethod());
        if (vertxAddress == null) {
            LOGGER.warning("VertxEvent observer found but no @VertxConsumer declared: " + event.getObserverMethod().toString());
            return;
        }
        LOGGER.info("Vertx message consumer found: " + event.getObserverMethod().toString());
        consumerAddresses.add(vertxAddress);
    }

    Set<String> getConsumerAddresses() {
        return consumerAddresses;
    }

    private String getVertxAddress(ObserverMethod<?> observerMethod) {
        Annotation qualifier = getQualifier(observerMethod, VertxConsumer.class);
        return qualifier != null ? ((VertxConsumer) qualifier).value() : null;
    }

    private Annotation getQualifier(ObserverMethod<?> observerMethod, Class<? extends Annotation> annotationType) {
        for (Annotation qualifier : observerMethod.getObservedQualifiers()) {
            if (qualifier.annotationType().equals(annotationType)) {
                return qualifier;
            }
        }
        return null;
    }

}
