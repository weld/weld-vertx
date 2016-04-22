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

import static org.jboss.weld.vertx.examples.translator.TranslatorAddresses.CLEAR_CACHE;
import static org.jboss.weld.vertx.examples.translator.TranslatorAddresses.TRANSLATION_DATA_FOUND;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.jboss.weld.vertx.VertxConsumer;
import org.jboss.weld.vertx.VertxEvent;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 *
 * @author Martin Kouba
 */
@ApplicationScoped
public class TranslationDataCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(TranslationDataCache.class.getName());

    private final ConcurrentMap<String, List<String>> cache;

    public TranslationDataCache() {
        this.cache = new ConcurrentHashMap<>();
    }

    void clear(@Observes @VertxConsumer(CLEAR_CACHE) VertxEvent event) {
        LOGGER.info("Clear dictionary cache");
        cache.clear();
    }

    void putIfAbsent(@Observes @VertxConsumer(TRANSLATION_DATA_FOUND) VertxEvent event) {

        Object data = event.getMessageBody();

        if (data instanceof JsonObject) {

            JsonObject dataObject = (JsonObject) data;
            String word = dataObject.getString("word");

            if (word != null) {
                List<String> foundMatches;
                JsonArray matchesArray = dataObject.getJsonArray("translations");
                if (matchesArray != null) {
                    foundMatches = new ArrayList<>();
                    for (Object element : matchesArray) {
                        foundMatches.add(element.toString());
                    }
                } else {
                    foundMatches = Collections.emptyList();
                }
                putIfAbsent(word, foundMatches);
            }
        }
    }

    /**
     *
     * @param word
     * @return a list of matching translations or <code>null</code> if no data available yet
     */
    List<String> getTranslations(String word) {
        return cache.get(word.toLowerCase());
    }

    void putIfAbsent(String word, List<String> matches) {
        if (cache.putIfAbsent(word.toLowerCase(), matches) == null) {
            LOGGER.info("Caching data for: {0}", word);
        }
    }

}
