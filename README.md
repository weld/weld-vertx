# Weld Vert.x integration

Vert.x makes use of a light-weight distributed messaging system to allow application components to communicate in a loosely coupled way. `weld-vertx` allows to automatically register certain observer methods as Vert.x message consumers and also to inject relevant `io.vertx.core.Vertx` and `io.vertx.core.Context` instances.

A simple echo message consumer could look like this:

```java
import org.jboss.weld.vertx.VertxConsumer;
import org.jboss.weld.vertx.VertxEvent;

class Foo {
    public void echoConsumer(@Observes @VertxConsumer("test.echo.address") VertxEvent event) {
        event.setReply(event.getMessageBody());
    }
}
```
* `@VertxConsumer` - a qualifier used to specify the address the consumer will be registered to: test.echo.address
* `VertxEvent` - a wrapper of a Vert.x message

Since we’re working with a regular observer method, additional parameters may be declared (next to the event parameter). These parameters are injection points. So it’s easy to declare a message consumer dependencies:

```java
public void consumerWithDependencies(@Observes @VertxConsumer("test.dependencies.address") VertxEvent event, CoolService coolService, StatsService statsService) {
    coolService.process(event.getMessageBody());
    statsService.log(event);
}
```
**NOTE**: If you inject a dependent bean, it will be destroyed when the invocation completes. 

Last but not least - an observer may also send/publish messages using the Vert.x event bus:

```java
public void consumerStrikesBack(@Observes @VertxConsumer("test.publish.address") VertxEvent event) {
    event.messageTo("test.huhu.address").publish("huhu");
}
```

## How does it work?

The central point of integration is the `org.jboss.weld.vertx.WeldVerticle`. This Verticle starts Weld SE container and automatically registers `org.jboss.weld.vertx.VertxExtension` to process all observer methods and detect observers which should become message consumers. Then a special handler is registered for each address to bridge the event bus to the CDI world. Handlers use `Vertx.executeBlocking()` since we expect the code to be blocking. Later on, whenever a new message is delivered to the handler, `Event.fire()` is used to notify all relevant observers.

See also http://weld.cdi-spec.org/news/2016/04/11/weld-meets-vertx/
