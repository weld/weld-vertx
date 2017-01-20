package org.jboss.weld.vertx.probe;

import static io.restassured.RestAssured.get;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsNot.not;
import static org.jboss.weld.vertx.WeldVerticle.createDefaultWeld;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.vertx.web.WeldWebVerticle;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import io.restassured.response.Response;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * 
 * @author Martin Kouba
 */
@RunWith(VertxUnitRunner.class)
public class ProbeHandlersTest {

    static final String URL_BASE = "http://localhost:8080/weld-probe/";

    private Vertx vertx;

    @Rule
    public Timeout globalTimeout = Timeout.millis(500000);

    @Before
    public void init(TestContext context) throws InterruptedException {
        vertx = Vertx.vertx();
        Async async = context.async();
        final WeldWebVerticle weldVerticle = new WeldWebVerticle(createDefaultWeld().property(Weld.DEV_MODE_SYSTEM_PROPERTY, true));
        vertx.deployVerticle(weldVerticle, deploy -> {
            if (deploy.succeeded()) {
                Router router = Router.router(vertx);
                router.route().handler(BodyHandler.create());
                weldVerticle.registerRoutes(router);
                vertx.createHttpServer().requestHandler(router::accept).listen(8080, (listen) -> {
                    if (listen.succeeded()) {
                        async.complete();
                    } else {
                        listen.cause().printStackTrace();
                    }
                });
            } else {
                deploy.cause().printStackTrace();
            }
        });
    }

    @After
    public void close(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testDeployment() {
        Response response = get(URL_BASE + "deployment");
        response.then().assertThat().statusCode(200).body("configuration", not(empty()));
    }

    @Test
    public void testBeans() {
        Response response = get(URL_BASE + "beans");
        response.then().assertThat().statusCode(200).body("data", not(empty()));
    }

}
