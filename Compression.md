---
layout: guide
title: Rest.li compression
permalink: /Compression
excerpt: Rest.li compression
---
# Rest.li Compression

## Contents
* [Supported Algorithms](#supported-algorithms)
* [Using Guice](#using-guice)
* [Using Spring](#using-spring)

## Supported Algorithms

The list of supported algorithms are posted here [EncodingType](https://github.com/linkedin/rest.li/blob/master/r2-filter-compression/src/main/java/com/linkedin/r2/filter/compression/EncodingType.java).

_**Note**: There is no hand-shake protocol between the server and the client to make sure that the server supports a particular compression algorithm. If the client sends a request compressed using an algorithm that the server is not configured with, then the client request will fail as the server will not be able to decompress it._

## Using Guice

### Server Configuration

To see how the server must be configured, see [FortunesGuiceServletConfig](https://github.com/linkedin/rest.li/blob/master/examples/guice-server/server/src/main/java/com/example/fortune/inject/FortunesGuiceServletConfig.java).

In particular:
```java
FilterChain filterChain = FilterChains.create(
    new ServerCompressionFilter(new EncodingType[] { EncodingType.SNAPPY }),
    new SimpleLoggingFilter());
```

This instantiates a Rest.li server that supports SNAPPY compression. To support SNAPPY and GZIP, for example, you would have to instantiate your server as follows:
```java
FilterChain filterChain = FilterChains.create(
    new ServerCompressionFilter(new EncodingType[] { EncodingType.SNAPPY, EncodingType.GZIP }),
    new SimpleLoggingFilter());
```

When we say that a server supports a particular algorithm for compression, it means that the server can compress and decompress using that algorithm. When to compress and decompress is governed by the client request.

### Client Configuration

To see how the client must be configured, see [RestLiFortunesClient](https://github.com/linkedin/rest.li/blob/master/examples/guice-server/client/src/main/java/com/example/fortune/RestLiFortunesClient.java).

In particular:

```java
    final HttpClientFactory http = new HttpClientFactory(FilterChains.create(
        new ClientCompressionFilter(EncodingType.IDENTITY, new EncodingType[]{ EncodingType.SNAPPY}, "*")
    ));
```

This snippet instructs Rest.li to use IDENTITY (also known as no compression) for requests to the server. In other words, the HTTP "Content-Encoding" header will be set to "identity". Thus, the first argument dictates the value of the "Content-Encoding" header. 

The second argument dictates the value of the "Accept-Encoding" header. In the snippet above, each request to the server will have "Accept-Encoding" set to "snappy". Why every request? Because the third argument is set to "\*". "\*" means that each and every request to the server will have an "Accept-Encoding" header present. The third argument is a String of comma separated values, where the values can be "*", a method name, or a family.

* "*": add the "Accept-Encoding" header to all outgoing requests.
* a method name: add the "Accept-Encoding" header only for requests for the particular method. E.g. "get" would add it for get requests, "get, batch_get" would add it for get and batch_get requests etc.
* a family: add the "Accept-Encoding" header only for requests for a method that matches the family. A family is specified as method:\*. E.g. "action:\*" would add it for all actions, "finder:*" would add it for all finder requests etc.

The above three can be mixed. For example, to have compression on get, batch_get and all finders, the third argument would be "get, batch_get, finder:*"

## Using Spring

### Server Configuration

To see how the server must be configured, see https://github.com/linkedin/rest.li/blob/master/examples/spring-server/server/src/main/webapp/WEB-INF/beans.xml.

The following snippet from the above file configures the server side compression filter
```xml
<bean id="compressionFilter" class="com.linkedin.r2.filter.compression.ServerCompressionFilter" >
    <constructor-arg value="snappy" />
</bean>
```

The `constructor-arg` is a comma separated list of algorithms you want to support on the server. For example to support gzip and snappy, you would do the following:

```xml
<bean id="compressionFilter" class="com.linkedin.r2.filter.compression.ServerCompressionFilter" >
    <constructor-arg value="snappy, gzip" />
</bean>
```

### Client Configuration

It's the same as the configuration for Guice.