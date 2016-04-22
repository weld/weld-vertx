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

import static org.jboss.weld.vertx.examples.translator.TranslatorAddresses.NO_TRANSLATION_DATA;
import static org.jboss.weld.vertx.examples.translator.TranslatorAddresses.TRANSLATION_DATA_FOUND;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * {@link Verticle} to provide dummy data.
 *
 * @author Martin Kouba
 */
public class DummyDataVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(DummyDataVerticle.class.getName());

    private final Map<String, List<String>> translationData;

    public DummyDataVerticle() {
        this.translationData = new HashMap<>();
    }

    @Override
    public void start() throws Exception {
        // Read data.properties
        Properties properties = new Properties();
        properties.load(DummyDataVerticle.class.getResourceAsStream("data.properties"));
        for (Entry<Object, Object> entry : properties.entrySet()) {
            translationData.put(entry.getKey().toString().toLowerCase(), Arrays.asList(entry.getValue().toString().split(",")));
        }

        // If someone asks for data then publish the available data
        vertx.eventBus().consumer(NO_TRANSLATION_DATA, (m) -> {
            String word = m.body().toString();
            LOGGER.info("Find translation data for {0}");
            List<String> result = translationData.get(word.toLowerCase());
            if (result == null) {
                result = Collections.emptyList();
            }
            if (result.isEmpty()) {
                LOGGER.warn("No translation data available for: {0}", word);
            }
            vertx.eventBus().publish(TRANSLATION_DATA_FOUND, new JsonObject().put("word", word).put("translations", new JsonArray(result)));
        });
    }

}
