---
layout: api_reference
title: Guice dependency injection with Rest.li
permalink: /Guice-Dependency-Injection
excerpt: Guice dependency injection can be used with Rest.li
---
# Guice Dependency Injection With Rest.li

Guice dependency injection may be used with Rest.li by using the [restli-guice-bridge](https://github.com/linkedin/rest.li/tree/master/restli-guice-bridge) module.

An example project using guice dependency injection is available in the Rest.li codebase:

https://github.com/linkedin/rest.li/tree/master/examples/guice-server

This example shows how Rest.li's GuiceRestliServlet may be used to run Rest.li servers with full guice dependency injection.

To use it, first add a dependency to the restli-guice-bridge module, for example:

```groovy
dependencies {
  compile "com.linkedin.pegasus:restli-guice-bridge:1.9.23"
}
```

Next, set up a [guice servlet](https://code.google.com/p/google-guice/wiki/Servlets), defining a guice GuiceServletContextListener for your application.  For an example, see [/examples/guice-server/server/src/main/java/com/example/fortune/inject/FortunesGuiceServletConfig.java](https://github.com/linkedin/rest.li/blob/master/examples/guice-server/server/src/main/java/com/example/fortune/inject/FortunesGuiceServletConfig.java)

Lastly, configure your web.xml. For an example, see [/examples/guice-server/server/src/main/webapp/WEB-INF/web.xml](https://github.com/linkedin/rest.li/blob/master/examples/guice-server/server/src/main/webapp/WEB-INF/web.xml)