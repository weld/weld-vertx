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
package org.jboss.weld.vertx.web;

import java.util.List;

import javax.inject.Inject;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;

public class PaymentResource {

    @WebRoute("/payments")
    static class PaymentsHandler implements Handler<RoutingContext> {

        @Inject
        PaymentService paymentService;

        @Override
        public void handle(RoutingContext ctx) {
            List<Payment> payments = paymentService.getPayments();
            JsonArray array = new JsonArray();
            for (Payment payment : payments) {
                array.add(paymentService.encode(payment));
            }
            ctx.response().setStatusCode(200).end(array.encode());
        }
    }

    @WebRoute("/payments/:paymentId")
    static class PaymentHandler implements Handler<RoutingContext> {

        @Inject
        PaymentService paymentService;

        @Override
        public void handle(RoutingContext ctx) {
            Payment payment = paymentService.getPayment(ctx.request().getParam("paymentId"));
            if (payment == null) {
                ctx.response().setStatusCode(404).end();
            } else {
                ctx.response().setStatusCode(200).end(paymentService.encode(payment).encode());
            }
        }

    }

    // This must be ignored
    @WebRoute("/payments/inner")
    class InnerClassHandler implements Handler<RoutingContext> {

        @Override
        public void handle(RoutingContext ctx) {
            ctx.response().setStatusCode(200).end("OK");
        }

    }

    // This must be ignored
    @WebRoute("/payments/string")
    static class WrongParameterHandler implements Handler<String> {

        @Override
        public void handle(String ctx) {
        }

    }

}
