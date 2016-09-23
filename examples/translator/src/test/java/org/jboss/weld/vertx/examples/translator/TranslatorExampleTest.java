package org.jboss.weld.vertx.examples.translator;

import org.jboss.weld.vertx.web.WeldWebVerticle;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 *
 * @author Martin Kouba
 */
@RunWith(VertxUnitRunner.class)
public class TranslatorExampleTest {

    private Vertx vertx;

    @Before
    public void init(TestContext context) {
        vertx = Vertx.vertx();
        Async serverVerticleAsync = context.async();
        Async dummyDataVerticleAsync = context.async();
        final WeldWebVerticle weldVerticle = new WeldWebVerticle();
        vertx.deployVerticle(weldVerticle, result -> {
            if (result.succeeded()) {
                vertx.deployVerticle(new ServerVerticle(weldVerticle), (serverResult) -> serverVerticleAsync.complete());
                vertx.deployVerticle(new DummyDataVerticle(), (dummyResult) -> dummyDataVerticleAsync.complete());
            }
        });
    }

    @After
    public void close(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test(timeout = 2000)
    public void testTranslator(TestContext context) throws InterruptedException {
        Async async = context.async();
        HttpClient client = vertx.createHttpClient();
        HttpClientRequest request = client.request(HttpMethod.POST, 8080, "localhost", "/translate");
        request.putHeader("Content-type", "application/x-www-form-urlencoded");
        request.handler((response) -> {
            if (response.statusCode() == 200) {
                response.bodyHandler((buffer) -> {
                    context.assertEquals("[{\"word\":\"Hello\",\"translations\":[\"ahoj\",\"dobry den\"]},{\"word\":\"world\",\"translations\":[\"svet\"]}]",
                            buffer.toString());
                    client.close();
                    async.complete();
                });
            }
        });
        request.end("sentence=Hello world");
    }

}
