# Weld Vert.x Kotlin Example

This simple example shows that Weld and Vertx play well together with [Kotlin programming language](https://kotlinlang.org/).

## HTTP Endpoints

The app defines two routes annoted with `@WebRoute`.
`org.jboss.weld.vertx.kotlin.UpperCaseHandler` class and an observer method declared in `org.jboss.weld.vertx.kotlin.Observers`.
Both are discovered and registered automatically.

## Start the App

### IDE

Run `org.jboss.weld.vertx.kotlin.main` function as a Kotlin app.

### Command Line

1. `mvn clean package`
2. `java -jar target/weld-vertx-kotlin-shaded.jar`

## Convert value to uppercase

Open `http://localhost:8080/upperCase?value=Herbert` in your browser or `curl http://localhost:8080/lowerCase?value=Marv`.
