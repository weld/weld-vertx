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

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.enterprise.util.AnnotationLiteral;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * This extension attempts to find all service proxy interfaces and for each one register a custom bean implementation with {@link ServiceProxy} qualifier.
 *
 * @author Martin Kouba
 */
public class ServiceProxyExtension implements Extension {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProxyExtension.class.getName());

    private Set<Class<?>> serviceInterfaces;

    void init(@Observes BeforeBeanDiscovery event) {
        serviceInterfaces = new HashSet<>();
    }

    void findServiceInterfaces(@Observes @WithAnnotations(ProxyGen.class) ProcessAnnotatedType<?> event, BeanManager beanManager) {
        AnnotatedType<?> annotatedType = event.getAnnotatedType();
        if (annotatedType.isAnnotationPresent(ProxyGen.class) && annotatedType.getJavaClass().isInterface()) {
            LOGGER.debug("Service interface {0} discovered", annotatedType.getJavaClass());
            serviceInterfaces.add(annotatedType.getJavaClass());
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    void registerServiceProxyBeans(@Observes AfterBeanDiscovery event, BeanManager beanManager) {
        for (Class<?> serviceInterface : serviceInterfaces) {
            event.addBean(new ServiceProxyBean(serviceInterface) {
                @Override
                public Object create(CreationalContext creationalContext) {
                    InjectionPoint injectionPoint = (InjectionPoint) beanManager.getInjectableReference(new InjectionPointMetadataInjectionPoint(),
                            creationalContext);
                    Set<Annotation> qualifiers = injectionPoint.getQualifiers();
                    String address = null;
                    for (Annotation qualifier : qualifiers) {
                        if (ServiceProxy.class.equals(qualifier.annotationType())) {
                            ServiceProxy serviceProxy = (ServiceProxy) qualifier;
                            address = serviceProxy.value();
                            break;
                        }
                    }
                    if (address == null) {
                        throw new IllegalStateException("Service proxy address is not declared");
                    }
                    Instance<ServiceProxySupport> supportInstance = CDI.current().select(ServiceProxySupport.class);
                    if (supportInstance.isUnsatisfied() || supportInstance.isAmbiguous()) {
                        throw new IllegalStateException("ServiceProxySupport cannot be resolved");
                    }
                    ServiceProxySupport serviceProxySupport = supportInstance.get();
                    return Proxy.newProxyInstance(ServiceProxyExtension.class.getClassLoader(), new Class[] { serviceInterface },
                            new ServiceProxyInvocationHandler(serviceProxySupport, serviceInterface, address));
                }
            });
            LOGGER.info("Custom bean for service interface {0} registered", serviceInterface);
        }
    }

    private abstract class ServiceProxyBean<T> implements Bean<T>, PassivationCapable {

        private final Class<?> serviceInterface;

        private final Set<Type> beanTypes;

        private final Set<Annotation> qualifiers;

        @SuppressWarnings("serial")
        private ServiceProxyBean(Class<?> serviceInterface) {
            this.serviceInterface = serviceInterface;
            Set<Type> beanTypes = new HashSet<>();
            beanTypes.add(Object.class);
            beanTypes.add(serviceInterface);
            this.beanTypes = Collections.unmodifiableSet(beanTypes);
            Set<Annotation> qualifiers = new HashSet<>();
            qualifiers.add(new AnnotationLiteral<Any>() {
            });
            qualifiers.add(ServiceProxy.Literal.EMPTY);
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
            return Dependent.class;
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
            return ServiceProxyExtension.class;
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
            return ServiceProxyExtension.class.getName() + "_" + serviceInterface.getName();
        }

    }

    private static class InjectionPointMetadataInjectionPoint implements InjectionPoint {

        @Override
        public Type getType() {
            return InjectionPoint.class;
        }

        @SuppressWarnings("serial")
        @Override
        public Set<Annotation> getQualifiers() {
            return Collections.<Annotation> singleton(new AnnotationLiteral<Default>() {
            });
        }

        @Override
        public Bean<?> getBean() {
            return null;
        }

        @Override
        public Member getMember() {
            return null;
        }

        @Override
        public Annotated getAnnotated() {
            return null;
        }

        @Override
        public boolean isDelegate() {
            return false;
        }

        @Override
        public boolean isTransient() {
            return false;
        }

    }

}
