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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class PaymentService {

    private List<Payment> payments;

    @PostConstruct
    void init() {
        payments = new ArrayList<>();
        payments.add(new Payment("foo", BigDecimal.ONE));
        payments.add(new Payment("bar", new BigDecimal("100")));
        payments = Collections.unmodifiableList(payments);
    }

    List<Payment> getPayments() {
        return payments;
    }

    Payment getPayment(String id) {
        for (Payment payment : payments) {
            if (payment.getId().equals(id)) {
                return payment;
            }
        }
        return null;
    }

    JsonObject encode(Payment payment) {
        JsonObject result = new JsonObject();
        result.put("id", payment.getId());
        result.put("amount", payment.getAmount().toString());
        return result;

    }

}
