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
package org.jboss.weld.vertx.probe;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.SessionScoped;

import org.jboss.weld.probe.JsonDataProvider;
import org.jboss.weld.util.collections.ImmutableMap;

/**
 * See also <tt>org.jboss.weld.probe.Exports</tt>. 
 * 
 * @author Martin Kouba
 */
final class Exports {
    
    static final Map<String, Class<? extends Annotation>> INSPECTABLE_SCOPES = ImmutableMap.<String, Class<? extends Annotation>> builder()
            .put("application", ApplicationScoped.class).put("session", SessionScoped.class).put("conversation", ConversationScoped.class).build();

    private Exports() {
    }

    static byte[] exportJsonData(JsonDataProvider jsonDataProvider) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(bytes));
        addEntry(out, "deployment", jsonDataProvider.receiveDeployment());
        addEntry(out, "observers", jsonDataProvider.receiveObservers(0, 0, null, "FULL"));
        addEntry(out, "beans", jsonDataProvider.receiveBeans(0, 0, null, "FULL"));
        addEntry(out, "fired-events", jsonDataProvider.receiveEvents(0, 0, null));
        addEntry(out, "invocation-trees", jsonDataProvider.receiveInvocations(0, 0, null, "FULL"));
        addEntry(out, "contexts", jsonDataProvider.receiveContexts());
        for (String contextKey : INSPECTABLE_SCOPES.keySet()) {
            addEntry(out, "context-" + contextKey, jsonDataProvider.receiveContext(contextKey));
        }
        // Intentionally do not export contextual instances
        out.close();
        return bytes.toByteArray();
    }

    private static void addEntry(ZipOutputStream out, String baseName, String data) throws IOException {
        out.putNextEntry(new ZipEntry(baseName + ".json"));
        out.write(data.getBytes(StandardCharsets.UTF_8));
    }

}
