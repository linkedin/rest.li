---
layout: api_reference
title: Spring dependency injection with Rest.li
permalink: /Spring-Dependency-Injection
excerpt: Spring dependency injection can be used with Rest.li.
---
# Spring Dependency Injection With Rest.li

Spring dependency injection may be used with Rest.li by using the
[restli-spring-bridge](https://github.com/linkedin/rest.li/tree/master/restli-spring-bridge)
module.

An example project using spring dependency injection is available in the
Rest.li codebase:

<https://github.com/linkedin/rest.li/tree/master/examples/spring-server>

To use it, first add a dependency to the restli-spring-bridge module,
for example:

```groovy  
dependencies {  
compile “com.linkedin.pegasus:restli-spring-bridge:1.9.23”  
}  
```

Then, wire in a Rest.li server and a RestliHttpRequestHandler. For an
example, see
[/examples/spring-server/server/src/main/webapp/WEB-INF/beans.xml](https://github.com/linkedin/rest.li/blob/master/examples/spring-server/server/src/main/webapp/WEB-INF/beans.xml)

Lastly, use Spring’s HttpRequestHandlerServlet to run the
RestliHttpRequestHandler as a servlet. For an example, see
[examples/spring-server/server/src/main/webapp/WEB-INF/web.xml](https://github.com/linkedin/rest.li/blob/master/examples/spring-server/server/src/main/webapp/WEB-INF/web.xml)
