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
package org.jboss.weld.vertx.web.observer;

import static io.vertx.core.http.HttpMethod.GET;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.weld.vertx.web.Payment;
import org.jboss.weld.vertx.web.PaymentResource;
import org.jboss.weld.vertx.web.PaymentService;
import org.jboss.weld.vertx.web.WebRoute;

import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;

/**
 * Observer variant of {@link PaymentResource}.
 */
@ApplicationScoped
class PaymentObserverResource {

    @Inject
    PaymentService paymentService;

    @WebRoute("/payments")
    void getAll(@Observes RoutingContext ctx) {
        List<Payment> payments = paymentService.getPayments();
        JsonArray array = new JsonArray();
        for (Payment payment : payments) {
            array.add(paymentService.encode(payment));
        }
        ctx.response().setStatusCode(200).end(array.encode());
    }

    @WebRoute(value = "/payments/:paymentId", methods = GET)
    void getPayment(@Observes RoutingContext ctx) {
        Payment payment = paymentService.getPayment(ctx.request().getParam("paymentId"));
        if (payment == null) {
            ctx.response().setStatusCode(404).end();
        } else {
            ctx.response().setStatusCode(200).end(paymentService.encode(payment).encode());
        }
    }

}
