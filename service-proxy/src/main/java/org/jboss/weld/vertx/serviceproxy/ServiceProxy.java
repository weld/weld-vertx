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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;

/**
 * This qualifier is used to:
 * <ul>
 * <li>distinguish a custom service proxy bean from implementation</li>
 * <li>specify the service address on an injection point (non-binding value)</li>
 * <ul>
 *
 * @author Martin Kouba
 */
@Qualifier
@Target({ TYPE, METHOD, PARAMETER, FIELD })
@Retention(RUNTIME)
public @interface ServiceProxy {

    /**
     *
     * @return the address on which the service is published
     */
    @Nonbinding
    String value();

    public final class Literal extends AnnotationLiteral<ServiceProxy> implements ServiceProxy {

        private static final long serialVersionUID = 1L;

        static final Literal EMPTY = new Literal("");

        private final String value;

        public static Literal of(String value) {
            return new Literal(value);
        }

        public String value() {
            return value;
        }

        private Literal(String value) {
            this.value = value;
        }

    }

}
