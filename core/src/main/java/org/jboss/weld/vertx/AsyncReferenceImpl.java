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

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import org.jboss.weld.exceptions.AmbiguousResolutionException;
import org.jboss.weld.interceptor.util.proxy.TargetInstanceProxy;
import org.jboss.weld.logging.BeanManagerLogger;
import org.jboss.weld.vertx.VertxExtension.AsyncProducerMetadata;

import io.vertx.core.Vertx;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

/**
 * Implementation notes:
 *
 * <ul>
 * <ol>
 * hold a special creational context so that we're able to correctly destroy dependent bean instances
 * </ol>
 * <ol>
 * the set of qualifiers of this bean is enhanced - see
 * {@link VertxExtension#processAsyncReferenceInjectionPoints(javax.enterprise.inject.spi.ProcessInjectionPoint))}
 * </ol>
 * <ol>
 * the set of bean types of this bean is restricted
 * </ol>
 * <ol>
 * producer methods returning {@link CompletionStage} are discovered in
 * {@link VertxExtension#collectAsyncProducerMethods(javax.enterprise.inject.spi.ProcessProducerMethod)}
 * </ol>
 * </ul>
 *
 * @author Martin Kouba
 * @param <T>
 */
@Typed(AsyncReference.class)
@Dependent
class AsyncReferenceImpl<T> extends ForwardingCompletionStage<T> implements AsyncReference<T> {

    private final CreationalContext<T> creationalContext;

    private final AtomicBoolean isDone;

    private final VertxCompletableFuture<T> future;

    private volatile T reference;

    private volatile Throwable cause;

    @Inject
    public AsyncReferenceImpl(InjectionPoint injectionPoint, Vertx vertx, BeanManager beanManager) {
        this.isDone = new AtomicBoolean(false);
        this.future = new VertxCompletableFuture<>(vertx);
        this.creationalContext = beanManager.createCreationalContext(null);

        ParameterizedType parameterizedType = (ParameterizedType) injectionPoint.getType();
        Type requiredType = parameterizedType.getActualTypeArguments()[0];

        // First check if there is a relevant async producer method available
        List<AsyncProducerMetadata> foundMetadata = beanManager.getExtension(VertxExtension.class).getAsyncProducerMetadata(requiredType,
                injectionPoint.getQualifiers());

        if (foundMetadata.size() > 1) {
            failure(new AmbiguousResolutionException(
                    "Ambiguous async producer methods for type " + requiredType + " with qualifiers " + injectionPoint.getQualifiers()));
        } else if (foundMetadata.size() == 1) {
            // Use the produced CompletionStage
            initWithCompletionStage(foundMetadata.get(0), beanManager);
        } else {
            // Use Vertx worker thread
            initWithWorker(injectionPoint, vertx, beanManager, requiredType);
        }
    }

    @PreDestroy
    void dispose() {
        creationalContext.release();
    }

    @Override
    public boolean isDone() {
        return isDone.get();
    }

    public T get() {
        return reference;
    }

    @Override
    public Throwable cause() {
        return cause;
    }

    @Override
    protected CompletionStage<T> delegate() {
        return future;
    }

    @Override
    public String toString() {
        return "AsyncReferenceImpl [isDone=" + isDone + ", reference=" + reference + ", cause=" + cause + "]";
    }

    @SuppressWarnings("unchecked")
    private void initWithCompletionStage(AsyncProducerMetadata metadata, BeanManager beanManager) {
        Bean<CompletionStage<T>> bean = (Bean<CompletionStage<T>>) beanManager
                .resolve(beanManager.getBeans(metadata.producerType, metadata.qualifiers.toArray(new Annotation[] {})));
        Object possibleStage = beanManager.getReference(bean, metadata.producerType, creationalContext);
        if (possibleStage instanceof CompletionStage) {
            ((CompletionStage<T>) possibleStage).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    failure(throwable);
                } else {
                    sucess(result);
                }
            });
        } else {
            throw new IllegalStateException("The contextual reference of " + bean + " is not a CompletionStage");
        }
    }

    @SuppressWarnings("unchecked")
    private void initWithWorker(InjectionPoint injectionPoint, Vertx vertx, BeanManager beanManager, Type requiredType) {
        vertx.<T> executeBlocking((f -> {
            Set<Bean<?>> beans = beanManager.getBeans(requiredType, injectionPoint.getQualifiers().toArray(new Annotation[] {}));
            if (beans.isEmpty()) {
                f.fail(BeanManagerLogger.LOG.injectionPointHasUnsatisfiedDependencies(injectionPoint.getQualifiers(), requiredType, ""));
                return;
            }
            Bean<T> bean = (Bean<T>) beanManager.resolve(beans);
            T beanInstance = (T) beanManager.getReference(bean, requiredType, creationalContext);
            if (beanManager.isNormalScope(bean.getScope()) && beanInstance instanceof TargetInstanceProxy) {
                // Initialize normal scoped bean instance eagerly
                ((TargetInstanceProxy<?>) beanInstance).getTargetInstance();
            }
            f.complete(beanInstance);
        }), (r) -> {
            if (r.succeeded()) {
                sucess(r.result());
            } else {
                failure(r.cause());
            }
        });
    }

    private void sucess(T result) {
        complete(result, null);
    }

    private void failure(Throwable cause) {
        complete(null, cause);
    }

    private void complete(T result, Throwable cause) {
        if (isDone.compareAndSet(false, true)) {
            if (cause != null) {
                this.cause = cause;
                this.future.completeExceptionally(cause);
            } else {
                this.reference = result;
                this.future.complete(result);
            }
        }
    }

}
