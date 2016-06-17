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

import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import io.vertx.core.Context;
import io.vertx.core.Vertx;

@Dependent
public class CoolService {

    @Inject
    private Vertx vertx;

    @Inject
    private Context context;

    @Inject
    private CacheService cacheService;

    private String id;

    @PostConstruct
    void init() {
        this.id = UUID.randomUUID().toString();
    }

    String getId() {
        return id;
    }

    CacheService getCacheService() {
        return cacheService;
    }

    Vertx getVertx() {
        return vertx;
    }

    Context getContext() {
        return context;
    }

}
