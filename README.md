# Weld Vert.x Extensions
<!-- See also https://github.com/badges/shields/issues/1046 -->
<!--[![Maven Central](http://img.shields.io/maven-central/v/org.jboss.weld.vertx/weld-vertx-core.svg)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22weld-vertx-core%22)-->
[![Travis CI Build Status](https://img.shields.io/travis/weld/weld-vertx/master.svg)](https://travis-ci.org/weld/weld-vertx)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jboss.weld.vertx/weld-vertx-core/badge.svg)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22weld-vertx-core%22)
[![License](https://img.shields.io/badge/license-Apache%20License%202.0-yellow.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

The primary purpose of `weld-vertx` is to bring the CDI programming model into the Vert.x ecosystem, i.e. to extend the Vert.x tool-kit for building reactive applications on the JVM.

- [Core - weld-vertx-core](#weld-vertx-core)
- [Web - weld-vertx-web](#weld-vertx-web)
- [Service Proxy - weld-vertx-service-proxy](#weld-vertx-service-proxy)
- [Probe - weld-vertx-probe](#weld-vertx-probe)

## weld-vertx-core

* makes it possible to notify CDI observer methods when a message is sent via Vert.x event bus
* provides `@ApplicationScoped` beans for `io.vertx.core.Vertx` and `io.vertx.core.Context`
* allows to deploy Verticles produced/injected by Weld

```xml
<dependency>
  <groupId>org.jboss.weld.vertx</groupId>
  <artifactId>weld-vertx-core</artifactId>
  <version>${version.weld-vertx}</version>
</dependency>
```

### CDI observers and Vert.x message consumers

Vert.x makes use of a light-weight distributed messaging system to allow application components to communicate in a loosely coupled way. `weld-vertx-core` makes it possible to notify CDI observer methods when a message is sent via Vert.x event bus. A simple echo message consumer example:

```java
import org.jboss.weld.vertx.VertxConsumer;
import org.jboss.weld.vertx.VertxEvent;

class Foo {
    // VertxConsumer - a qualifier used to specify the address of the message consumer
    // VertxEvent - a Vert.x message wrapper
    void echoConsumer(@Observes @VertxConsumer("test.echo.address") VertxEvent event) {
        event.setReply(event.getMessageBody());
    }
}
```

Since we’re working with a regular observer method, additional parameters may be declared (next to the event parameter). These parameters are injection points. So it’s easy to declare a message consumer dependencies:

```java
void consumerWithDependencies(@Observes @VertxConsumer("test.dependencies.address") VertxEvent event, CoolService coolService, StatsService statsService) {
    coolService.process(event.getMessageBody());
    statsService.log(event);
}
```
**NOTE**: If you inject a dependent bean, it will be destroyed when the invocation completes.

Last but not least - an observer may also send/publish messages using the Vert.x event bus:

```java
void consumerStrikesBack(@Observes @VertxConsumer("test.publish.address") VertxEvent event) {
    event.messageTo("test.huhu.address").publish("huhu");
}
```

#### How does it work?

The central point of integration is the `org.jboss.weld.vertx.VertxExtension`.
Its primary task is to find all CDI observer methods that should be notified when a message is sent via `io.vertx.core.eventbus.EventBus`.

If a `Vertx` instance is available during CDI bootstrap, then `VertxExtension` also:
* registers a Vert.x handler for each address found (whenever a new message is delivered to the handler, `Event.fire()` is used to notify all observers bound to a specific address)
* adds custom beans for `io.vertx.core.Vertx` and `io.vertx.core.Context` (thereby allowing to inject relevant instances into beans)

NOTE: Handlers use `Vertx.executeBlocking()` since we expect the code to be blocking.

`org.jboss.weld.vertx.WeldVerticle` starts/stops the Weld SE container and registers `VertxExtension` automatically. However, `VertxExtension.registerConsumers(Vertx, Event<Object>)` could be also used after the bootstrap, e.g. when a Vertx instance is only available after a CDI container is initialized.

### CDI-powered Verticles

It's also possible to deploy Verticles produced/injected by Weld, e.g.:

```java
@Dependent
class MyBeanVerticle extends AbstractVerticle {

     @Inject
     Service service;

     @Override
     public void start() throws Exception {
         vertx.eventBus().consumer("my.address").handler(m -> m.reply(service.process(m.body())));
     }
}

class MyApp {
     public static void main(String[] args) {
         final Vertx vertx = Vertx.vertx();
         final WeldVerticle weldVerticle = new WeldVerticle();
         vertx.deployVerticle(weldVerticle, result -> {
             if (result.succeeded()) {
                 // Deploy Verticle instance produced by Weld
                 vertx.deployVerticle(weldVerticle.container().select(MyBeanVerticle.class).get());
             }
         });
     }
}
```



## weld-vertx-web

* allows to define/register an `io.vertx.ext.web.Route` in a declarative way, using `@org.jboss.weld.vertx.web.WebRoute`

```xml
<dependency>
  <groupId>org.jboss.weld.vertx</groupId>
  <artifactId>weld-vertx-web</artifactId>
  <version>${version.weld-vertx}</version>
</dependency>
```

### Define route in a declarative way

`weld-vertx-web` extends `weld-vertx-core` and `vertx-web` functionality and allows to automatically register `Route` handlers discovered during container initialization. In other words, it's possible to configure a `Route` in a declarative way - using `org.jboss.weld.vertx.web.WebRoute` annotation:

```java
import org.jboss.weld.vertx.web.WebRoute;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@WebRoute("/helloworld") // Matches all HTTP methods
public class HelloWorldHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext ctx) {
        ctx.response().setStatusCode(200).end("Hello world!");
    }
}
```
Handler instances are not contextual intances, i.e. they're not managed by the CDI container (similarly as Java EE components). 
However, dependency injection and interception is supported:

```java
import static org.jboss.weld.vertx.web.WebRoute.HandlerType.BLOCKING;
import javax.inject.Inject;
import javax.enterprise.context.control.ActivateRequestContext;
...

@WebRoute("/hello", type = BLOCKING) // A blocking request handler
public class HelloHandler implements Handler<RoutingContext> {

    @Inject
    SayHelloService service;

    @ActivateRequestContext // This interceptor binding is used to activate the CDI request context within a handle() invocation
    @Override
    public void handle(RoutingContext ctx) {
        ctx.response().setStatusCode(200).end(service.hello());
    }
}
```

`@WebRoute` is a repeatable annotation.
If multiple annotations are declared on a handler class a single handler instance is used for multiple routes:

```java
import org.jboss.weld.vertx.web.WebRoute;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

@WebRoute("/hello", methods = HttpMethod.GET)
@WebRoute("/bye")
public class SuperHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext ctx) {
        // This method will be invoked upon the same handler instance for both routes
        ctx.response().setStatusCode(200).end("I'm super!");
    }
}
```

#### How does it work?

The central point of the module is the `org.jboss.weld.vertx.web.RouteExtension`.
Its primary task is to find all classes annotated with `@WebRoute` and register routes through `RouteExtension.registerRoutes(Router)`.

`org.jboss.weld.vertx.web.WeldWebVerticle` extends `org.jboss.weld.vertx.WeldVerticle`, registers `RouteExtension` automatically, and also provides the `WeldWebVerticle.registerRoutes(Router)` method (which delegates to `RouteExtension`):

```java
 class MyApp {

     public static void main(String[] args) {
         final Vertx vertx = Vertx.vertx();
         final WeldWebVerticle weldVerticle = new WeldWebVerticle();
         vertx.deployVerticle(weldVerticle, result -> {
             if (result.succeeded()) {
                 // Configure the router after Weld bootstrap finished
                 vertx.createHttpServer().requestHandler(weldVerticle.createRouter()::accept).listen(8080);
             }
         });
     }
 }
 ```

## weld-vertx-service-proxy

* allows to inject and invoke service proxies (as defined in https://github.com/vert-x3/vertx-service-proxy)
* the result handler is wrapped and executed using `ServiceProxySupport#getExecutor()` (`Vertx.executeBlocking()` by default)


```xml
<dependency>
  <groupId>org.jboss.weld.vertx</groupId>
  <artifactId>weld-vertx-service-proxy</artifactId>
  <version>${version.weld-vertx}</version>
</dependency>
```

**NOTE**: `weld-vertx-service-proxy` does not depend directly on `weld-vertx-core`. However, the default implementation of `org.jboss.weld.vertx.serviceproxy.ServiceProxySupport` relies on the functionality provided by `weld-vertx-core`.

### Injecting service proxies

A service proxy interface annotated with `io.vertx.codegen.annotations.ProxyGen` is automatically discovered and a custom bean with `@Dependent` scope and `org.jboss.weld.vertx.serviceproxy.ServiceProxy` qualifier is registered.

```java
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
```

## weld-vertx-probe

* allows to use [Probe](http://docs.jboss.org/weld/reference/latest/en-US/html/devmode.html#probe) development tool in a Vert.x application
* depends on `weld-vertx-web`

```xml
<dependency>
  <groupId>org.jboss.weld.vertx</groupId>
  <artifactId>weld-vertx-probe</artifactId>
  <version>${version.weld-vertx}</version>
</dependency>
```

### How does it work?

Just add `weld-vertx-probe` to the classpath, set the `org.jboss.weld.development` system property to `true` and use `WeldWebVerticle` to register the routes defined declaratively (as defined in [weld-vertx-web - How does it work?](#how-does-it-work-1)).

