package org.jboss.weld.vertx.serviceproxy;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class EchoObserver {

    public void observeEcho(@Observes @Echo String result) {
        EchoServiceProxyTest.SYNCHRONIZER.add(result);
    }

}
