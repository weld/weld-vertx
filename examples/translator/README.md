# Weld Vert.x Translator Example

This example aims to show the features of `weld-vertx-core` and `weld-vertx-web`.

## Business Logic

The app entry point is `org.jboss.weld.vertx.examples.translator.TranslateHandler` - a `@WebRoute` handler which is discovered and registered automatically.
`TranslateHandler` accepts "translate" requests and sends a message to the Vert.x event bus.
Another CDI bean - `org.jboss.weld.vertx.examples.translator.Translator` - consumes this event and implements the translation logic (using other CDI beans and Vert.x verticle).

## Start the App

### IDE

Open `org.jboss.weld.vertx.examples.translator.TranslatorExampleRunner` class and run it as Java app.

### Command Line

1. `mvn clean package`
2. `java -jar target/weld-vertx-translator-example-shaded.jar`

## Translate

Let's use `curl` to perform a request:

```bash
curl -d "sentence=Hello world" http://localhost:8080/translate
```