package org.jboss.weld.vertx.web;

import javax.enterprise.context.Dependent;

@Dependent
public class SayHelloService {

    static final String MESSAGE = "Good bye EE, hello Vert.x!";

    String hello() {
        return MESSAGE;
    }

}
