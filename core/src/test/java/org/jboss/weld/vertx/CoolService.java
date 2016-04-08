package org.jboss.weld.vertx;

import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@Dependent
public class CoolService {

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

}
