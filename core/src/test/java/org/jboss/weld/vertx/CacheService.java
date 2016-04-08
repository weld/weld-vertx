package org.jboss.weld.vertx;

import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CacheService {

    private String id;

    @PostConstruct
    void init() {
        this.id = UUID.randomUUID().toString();
    }

    String getId() {
        return id;
    }

}
