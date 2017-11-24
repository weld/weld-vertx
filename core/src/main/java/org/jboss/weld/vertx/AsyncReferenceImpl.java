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
import java.util.Arrays;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import org.jboss.weld.exceptions.AmbiguousResolutionException;
import org.jboss.weld.inject.WeldInstance;
import org.jboss.weld.inject.WeldInstance.Handler;
import org.jboss.weld.interceptor.util.proxy.TargetInstanceProxy;
import org.jboss.weld.logging.BeanManagerLogger;
import org.jboss.weld.util.reflection.ParameterizedTypeImpl;

import io.vertx.core.Vertx;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

/**
 * Implementation notes:
 *
 * <ul>
 * <ol>
 * the set of qualifiers of this bean is enhanced so that it satisfies all injection points with required type {@link AsyncReference} - see
 * {@link VertxExtension#processAsyncReferenceInjectionPoints(javax.enterprise.inject.spi.ProcessInjectionPoint))}
 * </ol>
 * <ol>
 * the set of bean types of this bean is restricted
 * </ol>
 * </ul>
 *
 * @author Martin Kouba
 * @param <T>
 */
@Typed(AsyncReference.class)
@Dependent
class AsyncReferenceImpl<T> extends ForwardingCompletionStage<T> implements AsyncReference<T> {

    private final WeldInstance<Object> instance;

    private final AtomicBoolean isDone;

    private final VertxCompletableFuture<T> future;

    private volatile T reference;

    private volatile Throwable cause;

    @Inject
    public AsyncReferenceImpl(InjectionPoint injectionPoint, Vertx vertx, BeanManager beanManager, @Any WeldInstance<Object> instance) {
        this.isDone = new AtomicBoolean(false);
        this.future = new VertxCompletableFuture<>(vertx);
        this.instance = instance;

        ParameterizedType parameterizedType = (ParameterizedType) injectionPoint.getType();
        Type requiredType = parameterizedType.getActualTypeArguments()[0];
        Annotation[] qualifiers = injectionPoint.getQualifiers().toArray(new Annotation[] {});

        // First check if there is a relevant async producer method available
        WeldInstance<Object> completionStage = instance.select(new ParameterizedTypeImpl(CompletionStage.class, requiredType), qualifiers);

        if (completionStage.isAmbiguous()) {
            failure(new AmbiguousResolutionException(
                    "Ambiguous async producer methods for type " + requiredType + " with qualifiers " + injectionPoint.getQualifiers()));
        } else if (!completionStage.isUnsatisfied()) {
            // Use the produced CompletionStage
            initWithCompletionStage(completionStage.getHandler());
        } else {
            // Use Vertx worker thread
            initWithWorker(requiredType, qualifiers, vertx, beanManager);
        }
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
    private void initWithCompletionStage(Handler<Object> completionStage) {
        Object possibleStage = completionStage.get();
        if (possibleStage instanceof CompletionStage) {
            ((CompletionStage<T>) possibleStage).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    failure(throwable);
                } else {
                    sucess(result);
                }
            });
        } else {
            throw new IllegalStateException("The contextual reference of " + completionStage.getBean() + " is not a CompletionStage");
        }
    }

    @SuppressWarnings("unchecked")
    private void initWithWorker(Type requiredType, Annotation[] qualifiers, Vertx vertx, BeanManager beanManager) {
        vertx.<Object> executeBlocking((f -> {
            WeldInstance<Object> asyncInstance = instance.select(requiredType, qualifiers);
            if (asyncInstance.isUnsatisfied()) {
                f.fail(BeanManagerLogger.LOG.injectionPointHasUnsatisfiedDependencies(Arrays.toString(qualifiers), requiredType, ""));
                return;
            } else if (asyncInstance.isAmbiguous()) {
                f.fail(BeanManagerLogger.LOG.injectionPointHasAmbiguousDependencies(Arrays.toString(qualifiers), requiredType, ""));
                return;
            }
            Handler<Object> handler = asyncInstance.getHandler();
            Object beanInstance = handler.get();
            if (beanManager.isNormalScope(handler.getBean().getScope()) && beanInstance instanceof TargetInstanceProxy) {
                // Initialize normal scoped bean instance eagerly
                ((TargetInstanceProxy<?>) beanInstance).getTargetInstance();
            }
            f.complete(beanInstance);
        }), (r) -> {
            if (r.succeeded()) {
                sucess((T) r.result());
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
