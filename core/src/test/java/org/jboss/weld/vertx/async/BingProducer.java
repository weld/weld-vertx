package org.jboss.weld.vertx.async;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class BingProducer {

    @Dependent
    @Produces
    Bing noBing() {
        return null;
    }

    @Dependent
    @Produces
    @Juicy
    Bing juicyBing() {
        return new Bing(55);
    }

    static class Bing {

        int value;

        public Bing(int value) {
            this.value = value;
        }

    }

}
