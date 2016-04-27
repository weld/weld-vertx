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
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessObserverMethod;

import org.jboss.weld.literal.AnyLiteral;
import org.jboss.weld.literal.DefaultLiteral;

import com.google.common.collect.ImmutableSet;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * This extension detects all the observer methods that should become message consumers.
 *
 * @author Martin Kouba
 */
public class VertxExtension implements Extension {

    private static final Logger LOGGER = LoggerFactory.getLogger(VertxExtension.class.getName());

    private final Set<String> consumerAddresses;

    private final Vertx vertx;

    private final Context context;

    public VertxExtension(Vertx vertx, Context context) {
        this.consumerAddresses = new HashSet<>();
        this.vertx = vertx;
        this.context = context;
    }

    public void detectMessageConsumers(@Observes ProcessObserverMethod<VertxEvent, ?> event) {
        String vertxAddress = getVertxAddress(event.getObserverMethod());
        if (vertxAddress == null) {
            LOGGER.warn("VertxEvent observer found but no @VertxConsumer declared: {0}", event.getObserverMethod());
            return;
        }
        LOGGER.debug("Vertx message consumer found: {0}", event.getObserverMethod());
        consumerAddresses.add(vertxAddress);
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event) {
        // Allow to inject Vertx used to deploy the of the verticle
        event.addBean(new VertxBean());
        // Allow to inject Context of the verticle
        event.addBean(new ContextBean());
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

    private class VertxBean implements Bean<Vertx> {

        @Override
        public Vertx create(CreationalContext<Vertx> creationalContext) {
            return vertx;
        }

        @Override
        public void destroy(Vertx instance, CreationalContext<Vertx> creationalContext) {
        }

        @Override
        public Set<Type> getTypes() {
            return ImmutableSet.<Type> of(Vertx.class, Object.class);
        }

        @Override
        public Set<Annotation> getQualifiers() {
            return ImmutableSet.of(AnyLiteral.INSTANCE, DefaultLiteral.INSTANCE);
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return ApplicationScoped.class;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public Set<Class<? extends Annotation>> getStereotypes() {
            return Collections.emptySet();
        }

        @Override
        public boolean isAlternative() {
            return false;
        }

        @Override
        public Class<?> getBeanClass() {
            return VertxBean.class;
        }

        @Override
        public Set<InjectionPoint> getInjectionPoints() {
            return Collections.emptySet();
        }

        @Override
        public boolean isNullable() {
            return false;
        }

    }

    private class ContextBean implements Bean<Context> {

        @Override
        public Context create(CreationalContext<Context> creationalContext) {
            return context;
        }

        @Override
        public void destroy(Context instance, CreationalContext<Context> creationalContext) {
        }

        @Override
        public Set<Type> getTypes() {
            return ImmutableSet.<Type> of(Context.class, Object.class);
        }

        @Override
        public Set<Annotation> getQualifiers() {
            return ImmutableSet.of(AnyLiteral.INSTANCE, DefaultLiteral.INSTANCE);
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return ApplicationScoped.class;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public Set<Class<? extends Annotation>> getStereotypes() {
            return Collections.emptySet();
        }

        @Override
        public boolean isAlternative() {
            return false;
        }

        @Override
        public Class<?> getBeanClass() {
            return ContextBean.class;
        }

        @Override
        public Set<InjectionPoint> getInjectionPoints() {
            return Collections.emptySet();
        }

        @Override
        public boolean isNullable() {
            return false;
        }

    }

}
