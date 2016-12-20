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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

/**
 *
 * @author Martin Kouba
 */
@ApplicationScoped
public class EchoServiceConsumer {

    // Injects a service proxy for a service with the given address
    @Inject
    @ServiceProxy("echo-service-address")
    EchoService service;

    @Inject
    @Echo
    Event<String> event;

    public void doEchoBusiness(String value) {
        // By default, the result handler is executed by means of Vertx.executeBlocking()
        // In this case, we fire a CDI event observed by EchoObserver bean
        service.echo(value, (r) -> event.fire(r.result()));
    }

}
