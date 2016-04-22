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
import static org.jboss.weld.vertx.examples.translator.TranslatorAddresses.TRANSLATE;

import java.util.List;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.weld.vertx.VertxConsumer;
import org.jboss.weld.vertx.VertxEvent;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 *
 * @author Martin Kouba
 */
@Singleton
public class Translator {

    private final SentenceParser parser;

    private final TranslationDataCache cache;

    @Inject
    Translator(SentenceParser parser, TranslationDataCache dictionary) {
        this.parser = parser;
        this.cache = dictionary;
    }

    public void translate(@Observes @VertxConsumer(TRANSLATE) VertxEvent event) {

        JsonArray results = new JsonArray();
        List<String> words = parser.parse(event.getMessageBody().toString());

        for (String word : words) {

            JsonObject result = new JsonObject();
            result.put("word", word);

            List<String> translations = cache.getTranslations(word);

            if (translations == null) {
                // No data found in cache
                event.messageTo(NO_TRANSLATION_DATA).publish(word);
                
            } else {
                result.put("translations", new JsonArray(translations));
            }
            results.add(result);
        }
        event.setReply(results);
    }

}
