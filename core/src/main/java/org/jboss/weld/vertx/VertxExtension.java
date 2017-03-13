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
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.inject.spi.ProcessObserverMethod;
import javax.enterprise.util.AnnotationLiteral;

import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.util.reflection.Reflections;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * The central point of integration. Its task is to find all CDI observer methods that should become Vert.x message consumers - see also {@link VertxEvent} and
 * {@link VertxConsumer}. And if a Vertx instance is available add custom beans for {@link Vertx} and {@link Context} and register consumers for all the
 * addresses found.
 *
 * <p>
 * {@link #registerConsumers(Vertx, Event)} could be also used after the CDI bootstrap, e.g. when a Vertx instance is only available after the CDI bootstrap
 * finishes.
 * </p>
 *
 * @author Martin Kouba
 * @see VertxEvent
 * @see VertxConsumer
 */
public class VertxExtension implements Extension {

    private static final Logger LOGGER = LoggerFactory.getLogger(VertxExtension.class.getName());

    private final Set<String> consumerAddresses;

    private final Vertx vertx;

    private final Context context;

    public VertxExtension() {
        this(null, null);
    }

    public VertxExtension(Vertx vertx, Context context) {
        this.consumerAddresses = new HashSet<>();
        this.vertx = vertx;
        this.context = context;
    }

    public void processVertxEventObserver(@Observes ProcessObserverMethod<VertxEvent, ?> event) {
        String vertxAddress = getVertxAddress(event.getObserverMethod());
        if (vertxAddress == null) {
            LOGGER.warn("VertxEvent observer found but no @VertxConsumer declared: {0}", event.getObserverMethod());
            return;
        }
        LOGGER.debug("Vertx message consumer found: {0}", event.getObserverMethod());
        consumerAddresses.add(vertxAddress);
    }

    public void registerBeansAfterBeanDiscovery(@Observes AfterBeanDiscovery event) {
        if (vertx == null) {
            // Do no register beans - no Vertx instance available during bootstrap
            return;
        }
        // Allow to inject Vertx used to deploy the WeldVerticle
        event.addBean(new VertxBean<Vertx>(getBeanTypes(vertx.getClass(), Vertx.class)) {
            @Override
            public Vertx create(CreationalContext<Vertx> creationalContext) {
                return vertx;
            }
        });
        // Allow to inject Context of the WeldVerticle
        event.addBean(new VertxBean<Context>(getBeanTypes(context.getClass(), Context.class)) {
            @Override
            public Context create(CreationalContext<Context> creationalContext) {
                return context;
            }
        });
    }

    public void registerConsumersAfterDeploymentValidation(@Observes AfterDeploymentValidation afterDeploymentValidation, BeanManager beanManager) {
        if (vertx != null) {
            registerConsumers(vertx, BeanManagerProxy.unwrap(beanManager).event());
        }
    }

    public void registerConsumers(Vertx vertx, Event<Object> event) {
        for (String address : consumerAddresses) {
            vertx.eventBus().consumer(address, VertxHandler.from(vertx, event, address));
        }
    }

    private Set<Type> getBeanTypes(Class<?> implClazz, Type... types) {
        Set<Type> beanTypes = new HashSet<>();
        Collections.addAll(beanTypes, types);
        beanTypes.add(implClazz);
        // Add all the interfaces (and extended interfaces) implemented directly by the impl class
        beanTypes.addAll(Reflections.getInterfaceClosure(implClazz));
        return beanTypes;
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

    private abstract class VertxBean<T> implements Bean<T>, PassivationCapable {

        private final Set<Type> beanTypes;

        private final Set<Annotation> qualifiers;

        private VertxBean(Set<Type> beanTypes) {
            beanTypes.add(Object.class);
            this.beanTypes = Collections.unmodifiableSet(beanTypes);
            Set<Annotation> qualifiers = new HashSet<>();
            qualifiers.add(new AnnotationLiteral<Any>() {
                private static final long serialVersionUID = 1L;
            });
            qualifiers.add(new AnnotationLiteral<Default>() {
                private static final long serialVersionUID = 1L;
            });
            this.qualifiers = Collections.unmodifiableSet(qualifiers);
        }

        @Override
        public void destroy(T instance, CreationalContext<T> creationalContext) {
        }

        @Override
        public Set<Type> getTypes() {
            return beanTypes;
        }

        @Override
        public Set<Annotation> getQualifiers() {
            return qualifiers;
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
            return VertxExtension.class;
        }

        @Override
        public Set<InjectionPoint> getInjectionPoints() {
            return Collections.emptySet();
        }

        @Override
        public boolean isNullable() {
            return false;
        }

        @Override
        public String getId() {
            return VertxExtension.class.getName() + "_" + beanTypes;
        }

    }

}
