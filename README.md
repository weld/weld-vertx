| :warning: This repository is archived. Some of the core features were adopted in the [Quarkus](https://github.com/quarkusio/quarkus/) project. |
| --- |

# Weld Vert.x Extensions

<!-- See also https://github.com/badges/shields/issues/1046 -->
<!--[![Maven Central](http://img.shields.io/maven-central/v/org.jboss.weld.vertx/weld-vertx-core.svg)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22weld-vertx-core%22)-->
[![Travis CI Build Status](https://img.shields.io/travis/weld/weld-vertx/master.svg)](https://travis-ci.org/weld/weld-vertx)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jboss.weld.vertx/weld-vertx-core/badge.svg)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22weld-vertx-core%22)
[![License](https://img.shields.io/badge/license-Apache%20License%202.0-yellow.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

The primary purpose of `weld-vertx` is to bring the CDI programming model into the [Vert.x](http://vertx.io/) ecosystem, i.e. to extend the tool-kit for building reactive applications on the JVM.

## Features

* Provides `WeldVerticle` to start/stop the CDI container (using Weld SE) - see also [Core](https://github.com/weld/weld-vertx/blob/master/doc/src/main/asciidoc/core.adoc)
* Makes it possible to notify CDI observer methods when a message is sent via Vert.x event bus - see also [Core](https://github.com/weld/weld-vertx/blob/master/doc/src/main/asciidoc/core.adoc)
* Provides `@ApplicationScoped` beans for `io.vertx.core.Vertx` and `io.vertx.core.Context` - see also [Core](https://github.com/weld/weld-vertx/blob/master/doc/src/main/asciidoc/core.adoc)
* Provides "async" helpers such as [AsyncReference](https://github.com/weld/weld-vertx/blob/master/core/src/main/java/org/jboss/weld/vertx/AsyncReference.java) and [AsyncWorker](https://github.com/weld/weld-vertx/blob/master/core/src/main/java/org/jboss/weld/vertx/AsyncWorker.java) - see also [Core](https://github.com/weld/weld-vertx/blob/master/doc/src/main/asciidoc/core.adoc)
* Allows to deploy Verticles produced/injected by Weld - see also [Core](https://github.com/weld/weld-vertx/blob/master/doc/src/main/asciidoc/core.adoc)
* Allows to define/register an `io.vertx.ext.web.Route` in a declarative way, using `@org.jboss.weld.vertx.web.WebRoute` - see also [Web](https://github.com/weld/weld-vertx/blob/master/doc/src/main/asciidoc/web.adoc)
* Allows to inject and invoke service proxies (as defined in https://github.com/vert-x3/vertx-service-proxy) - see also [Service Proxy](https://github.com/weld/weld-vertx/blob/master/doc/src/main/asciidoc/service-proxy.adoc)

## Documentation

Brief documentation can be found at: http://docs.jboss.org/weld/weld-vertx/latest/

## Building

To build simply run:

> $ mvn clean install
