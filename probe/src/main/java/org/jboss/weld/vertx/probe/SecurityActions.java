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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.enterprise.inject.Vetoed;

/**
 *
 * @author Martin Kouba
 */
@Vetoed
final class SecurityActions {

    private SecurityActions() {
    }

    /**
     * Set the {@code accessible} flag for this accessible object. Does not perform {@link PrivilegedAction} unless necessary.
     *
     * @param accessibleObject
     */
    static void ensureAccessible(AccessibleObject accessibleObject) {
        if (accessibleObject != null) {
            if (!accessibleObject.isAccessible()) {
                if (System.getSecurityManager() != null) {
                    AccessController.doPrivileged(new PrivilegedAction<AccessibleObject>() {
                        @Override
                        public AccessibleObject run() {
                            accessibleObject.setAccessible(true);
                            return accessibleObject;
                        }
                    });
                } else {
                    accessibleObject.setAccessible(true);
                }
            }
        }
    }

    /**
     * Does not perform {@link PrivilegedAction} unless necessary.
     *
     * @param javaClass
     * @param methodName
     * @param parameterTypes
     * @return returns a method from the class or any class/interface in the inheritance hierarchy
     * @throws NoSuchMethodException
     */
    static Method getDeclaredMethod(Class<?> javaClass, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(new PrivilegedAction<Method>() {
                @Override
                public Method run() {
                    try {
                        return javaClass.getDeclaredMethod(name, parameterTypes);
                    } catch (NoSuchMethodException | SecurityException e) {
                        return null;
                    }
                }
            });
        } else {
            return javaClass.getDeclaredMethod(name, parameterTypes);
        }
    }

}
