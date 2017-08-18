# Weld Vert.x "Hello" Example

This simple example shows how to start a very simple "hello" webapp.

## HTTP Endpoints

The app has only one `@WebRoute` handler `org.jboss.weld.vertx.examples.hello.HelloMain.HelloHandler` which is discovered and registered automatically.

## Start the App

### IDE

Open `org.jboss.weld.vertx.examples.hello.HelloMain` class and run it as Java app.

### Command Line

1. `mvn clean package`
2. `java -jar target/weld-vertx-hello-example-shaded.jar`

## Say Hello

Open `http://localhost:8080/hello?name=Herbert` in your browser or `curl http://localhost:8080/hello?name=Marv`.
