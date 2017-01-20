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
package org.jboss.weld.vertx.probe;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;

import org.jboss.weld.probe.JsonDataProvider;
import org.jboss.weld.probe.ProbeExtension;

/**
 * 
 * @author Martin Kouba
 */
@Dependent
public class JsonDataProviderProducer {

    @Produces
    @ApplicationScoped
    public JsonDataProvider produce(BeanManager beanManager) {
        ProbeExtension extension = beanManager.getExtension(ProbeExtension.class);
        if (extension == null) {
            throw new IllegalStateException("ProbeExtension not available");
        }
        try {
            // Unfortunately, getJsonDataProvider() is package-private
            Method getProviderMethod = SecurityActions.getDeclaredMethod(ProbeExtension.class, "getJsonDataProvider");
            if (getProviderMethod == null) {
                throw new IllegalStateException("ProbeExtension.getJsonDataProvider() method not found or inaccessible");
            }
            SecurityActions.ensureAccessible(getProviderMethod);
            return (JsonDataProvider) getProviderMethod.invoke(extension);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new IllegalStateException("Unable to get JsonDataProvider", e);
        }
    }

}
