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
package org.jboss.weld.vertx.examples.translator;

import static org.jboss.weld.vertx.examples.translator.Addresses.CLEAR_CACHE;
import static org.jboss.weld.vertx.examples.translator.Addresses.REQUEST_DATA;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.weld.vertx.VertxConsumer;
import org.jboss.weld.vertx.VertxEvent;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 *
 * @author Martin Kouba
 */
@ApplicationScoped
public class DataCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataCache.class.getName());

    private final Vertx vertx;

    private final ConcurrentMap<String, List<String>> cache;

    @Inject
    public DataCache(Vertx vertx) {
        this.cache = new ConcurrentHashMap<>();
        this.vertx = vertx;
    }

    void clear(@Observes @VertxConsumer(CLEAR_CACHE) VertxEvent event) {
        LOGGER.info("Clear dictionary cache");
        cache.clear();
    }

    /**
     *
     * @param word
     * @return a list of matching translations or <code>null</code> if no data available yet
     */
    List<String> getTranslations(String word) {
        List<String> translations = cache.get(word.toLowerCase());
        if (translations == null) {
            // No translations available - send request
            // We use synchronizer to block for 2 seconds
            final BlockingQueue<List<String>> synchronizer = new LinkedBlockingQueue<>();
            vertx.eventBus().send(REQUEST_DATA, word, (r) -> {
                if (r.succeeded()) {
                    synchronizer.add(putIfAbsent(r.result().body()));
                }
            });
            try {
                translations = synchronizer.poll(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOGGER.warn("No translation data available for {0} right now...", word);
            }
        }
        return translations;
    }

    private List<String> putIfAbsent(Object data) {
        if (data instanceof JsonObject) {
            JsonObject dataObject = (JsonObject) data;
            String word = dataObject.getString("word");
            if (word != null) {
                List<String> translations;
                JsonArray translationsArray = dataObject.getJsonArray("translations");
                if (translationsArray != null) {
                    translations = new ArrayList<>();
                    for (Object element : translationsArray) {
                        translations.add(element.toString());
                    }
                    translations = Collections.unmodifiableList(translations);
                } else {
                    translations = Collections.emptyList();
                }
                putIfAbsent(word, translations);
                return translations;
            }
        }
        return null;
    }

    void putIfAbsent(String word, List<String> matches) {
        if (cache.putIfAbsent(word.toLowerCase(), matches) == null) {
            LOGGER.info("Caching data for: {0}", word);
        }
    }

}
