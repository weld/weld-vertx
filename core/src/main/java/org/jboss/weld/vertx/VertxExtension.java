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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.inject.spi.ProcessBeanAttributes;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.ProcessObserverMethod;
import javax.enterprise.inject.spi.ProcessProducerMethod;
import javax.enterprise.util.AnnotationLiteral;

import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.resolution.BeanTypeAssignabilityRules;
import org.jboss.weld.util.bean.ForwardingBeanAttributes;
import org.jboss.weld.util.collections.ImmutableSet;
import org.jboss.weld.util.reflection.Reflections;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * The central point of integration. Its task is to find all CDI observer methods that should be notified when a message is sent via
 * {@link io.vertx.core.eventbus.EventBus}. See also {@link VertxEvent} and {@link VertxConsumer}.
 * <p>
 * If a {@link Vertx} instance is available:
 * <ul>
 * <li>also add custom beans for {@link Vertx} and {@link Context},</li>
 * <li>and register consumers for all the addresses found.</li>
 * </ul>
 * </p>
 * <p>
 * {@link #registerConsumers(Vertx, Event)} could be also used after the bootstrap, e.g. when a Vertx instance is only available after a CDI container is
 * initialized.
 * </p>
 *
 * @author Martin Kouba
 * @see VertxEvent
 * @see VertxConsumer
 */
public class VertxExtension implements Extension {

    public static final String CONSUMER_REGISTRATION_TIMEOUT_KEY = "weld.vertx.consumer.reg.timeout";

    public static final long DEFAULT_CONSUMER_REGISTRATION_TIMEOUT = 10000l;

    private static final Logger LOGGER = LoggerFactory.getLogger(VertxExtension.class.getName());

    private final Set<String> consumerAddresses;

    private final Set<Annotation> asyncReferenceQualifiers;

    private final Set<AsyncProducerMetadata> asyncProducerMethods;

    private final Vertx vertx;

    private final Context context;

    public VertxExtension() {
        this(null, null);
    }

    public VertxExtension(Vertx vertx, Context context) {
        this.consumerAddresses = new HashSet<>();
        this.asyncReferenceQualifiers = new HashSet<>();
        this.asyncProducerMethods = new HashSet<>();
        this.vertx = vertx;
        this.context = context;
    }

    @SuppressWarnings("rawtypes")
    void processAsyncReferenceInjectionPoints(@Observes ProcessInjectionPoint<?, ? extends AsyncReference> event) {
        asyncReferenceQualifiers.addAll(event.getInjectionPoint().getQualifiers());
    }

    @SuppressWarnings("rawtypes")
    void addAsyncReferenceQualifiers(@Observes ProcessBeanAttributes<AsyncReferenceImpl> event) {
        // Add all discovered qualifiers to AsyncReferenceImpl bean attributes
        if (!asyncReferenceQualifiers.isEmpty()) {
            LOGGER.debug("Adding additional AsyncReference qualifiers: {0}", asyncReferenceQualifiers);
            BeanAttributes<AsyncReferenceImpl> attributes = event.getBeanAttributes();
            event.setBeanAttributes(new ForwardingBeanAttributes<AsyncReferenceImpl>() {

                Set<Annotation> qualifiers = ImmutableSet.<Annotation> builder().addAll(attributes.getQualifiers()).addAll(asyncReferenceQualifiers).build();

                @Override
                public Set<Annotation> getQualifiers() {
                    return qualifiers;
                }

                @Override
                protected BeanAttributes<AsyncReferenceImpl> attributes() {
                    return attributes;
                }
            });
        }
    }

    @SuppressWarnings("rawtypes")
    void collectAsyncProducerMethods(@Observes ProcessProducerMethod<? extends CompletionStage, ?> event) {
        // Discover all producer methods returning CompletionStage<?>
        asyncProducerMethods.add(new AsyncProducerMetadata(event.getAnnotatedProducerMethod().getBaseType(), event.getBean().getQualifiers()));
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
        asyncReferenceQualifiers.clear();
    }

    public void registerConsumers(Vertx vertx, Event<Object> event) {
        CountDownLatch latch = new CountDownLatch(consumerAddresses.size());
        for (String address : consumerAddresses) {
            MessageConsumer<?> consumer = vertx.eventBus().consumer(address, VertxHandler.from(vertx, event, address));
            consumer.completionHandler(ar -> {
                if (ar.succeeded()) {
                    LOGGER.debug("Sucessfully registered event consumer for {0}", address);
                    latch.countDown();
                } else {
                    LOGGER.error("Cannot register event consumer for {0}", ar.cause(), address);
                }
            });
        }
        Context context = this.context;
        if (context == null && vertx != null) {
            context = vertx.getOrCreateContext();
        }
        long timeout = context != null ? context.config().getLong(CONSUMER_REGISTRATION_TIMEOUT_KEY, DEFAULT_CONSUMER_REGISTRATION_TIMEOUT)
                : DEFAULT_CONSUMER_REGISTRATION_TIMEOUT;
        try {
            if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException(String.format("Message consumers not registered within %s ms [registered: %s, total: %s]", timeout,
                        latch.getCount(), consumerAddresses.size()));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    List<AsyncProducerMetadata> getAsyncProducerMetadata(Type requiredType, Set<Annotation> qualifiers) {
        if (asyncProducerMethods.isEmpty()) {
            return Collections.emptyList();
        }
        List<AsyncProducerMetadata> found = new ArrayList<>();
        for (AsyncProducerMetadata metadata : asyncProducerMethods) {
            if (metadata.matches(requiredType, qualifiers)) {
                found.add(metadata);
            }
        }
        return found;
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

    static class AsyncProducerMetadata {

        Type producerType;

        Type resultType;

        Set<Annotation> qualifiers;

        public AsyncProducerMetadata(Type producerType, Set<Annotation> qualifiers) {
            this.producerType = producerType;
            ParameterizedType parameterizedType = (ParameterizedType) producerType;
            this.resultType = parameterizedType.getActualTypeArguments()[0];
            this.qualifiers = qualifiers;
        }

        boolean matches(Type requiredType, Set<Annotation> qualifiers) {
            return BeanTypeAssignabilityRules.instance().matches(requiredType, resultType) && this.qualifiers.containsAll(qualifiers);
        }

    }

}
