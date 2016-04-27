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
