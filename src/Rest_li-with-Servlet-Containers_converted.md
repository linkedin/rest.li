---
layout: api_reference
title: Rest.li with servlet containers
permalink: /Rest_li-with-Servlet-Containers
excerpt: Rest.li may be run in a variety of http frameworks. Out of the box, Rest.li supports both Netty and Servlet containers, such as Jetty.
---

# Rest.li With Servlet Containers

## Contents

  - [Introduction](#introduction)
  - [Step 1. Creating a web.xml File](#step-1-creating-a-webxml-file)
  - [Step 2. Building a War](#step-2-building-a-war)
  - [Jetty](#jetty)

## Introduction

Rest.li may be run in a variety of http frameworks. Out of the box,
Rest.li supports both [Netty](Rest_li-with-Netty) and Servlet
containers, such as Jetty.

This describes how to run Rest.li on a servlet container by building a
war containing a rest.li servlet. It also covers how to run Rest.li with
Jetty.

## Step 1. Creating a web.xml File

Rest.li provides a RestliServlet class to integrate rest.li into any
Java servlet container.

To use RestliServlet, you will add a web.xml file.

For example, below is
`example-standalone-app/server/src/main/webapp/WEB-INF/web.xml`, an
extremely simple web.xml example that creates a RestliServlet and
configures it to load all rest.li resources in the
`com.example.fortune.impl` package.

Notice that RestliServlet automatically scans all resource classes in
the specified package and initializes the REST endpoints/routes without
any hard-coded connection. Adding additional resources or operations can
be done simply by expanding your data schema and providing additional
functionality in your Resource
class(es).

##### file: example-standalone-app/server/src/main/webapp/WEB-INF/web.xml

```xml

<?xml version="1.0" encoding="UTF-8"?>

\<\!DOCTYPE web-app PUBLIC ‘-//Sun Microsystems, Inc.//DTD Web
Application 2.3//EN’ ‘http://java.sun.com/dtd/web-app_2_3.dtd’\>

<web-app>  
<display-name>Fortunes App</display-name>  
<description>Tells
fortunes</description>

<!-- servlet definitions -->

<servlet>  
<servlet-name>FortunesServlet</servlet-name>  
<servlet-class>com.linkedin.restli.server.RestliServlet</servlet-class>  
<init-param>  
<param-name>resourcePackages</param-name>  
<param-value>com.example.fortune.impl</param-value>  
</init-param>  
<load-on-startup>1</load-on-startup>  
</servlet>

<!-- servlet mappings -->

<servlet-mapping>  
<servlet-name>FortunesServlet</servlet-name>  
<url-pattern>/\*</url-pattern>  
</servlet-mapping>

</web-app>  
```

The parseq thread count (use to make outbound requests) may optionally
be configured by setting the parSeqThreadPoolSize servlet init param,
for example:

```xml  
<init-param>  
<param-name>parseqThreadPoolSize</param-name>  
<param-value>10</param-value>  
</init-param>  
```

To configure jetty’s inbound request thread pool size, see Jetty’s
documentation.

## Step 2. Building a War

To build a war, we need a `build.gradle` file in the server directory,
which should look like this:

##### file: example-standalone-app/server/build.gradle

```groovy  
apply plugin: ‘war’  
apply plugin: ‘pegasus’

ext.apiProject = project(‘:api’)

dependencies {  
compile project(path: ‘:api’, configuration: ‘dataTemplate’)  
compile spec.product.pegasus.restliServer  
}  
```

For on the gradle ‘war’ plugin, see: [Gradle War
Plugin](http://www.gradle.org/docs/current/userguide/war_plugin.html)

## Jetty

To run Rest.li on Jetty, use the war plugin like you would for any other
servlet (above). For convenience, you can optionally setup a Gradle task
to run your application in Jetty. Here’s an example:

##### file: example-standalone-app/server/build.gradle

```groovy  
apply plugin: ‘war’  
apply plugin: ‘pegasus’

ext.apiProject = project(‘:api’)

dependencies {  
compile project(path: ‘:api’, configuration: ‘dataTemplate’)  
compile spec.product.pegasus.restliServer  
}

configurations {  
jetty8  
}

dependencies {  
jetty8 “org.mortbay.jetty:jetty-runner:8.1.15.v20140411” // set to
whatever version of jetty you want to test with  
}

task JettyRunWar(type: JavaExec) {  
main = “org.mortbay.jetty.runner.Runner”  
args = \[war.archivePath\]  
classpath configurations.jetty8  
}  
```

To start Rest.li on Jetty run:

```  
gradle JettyRunWar  
```

The server will start on port 8080 under the /server context path, for
example:

```  
curli http://localhost:8080/fortunes/1  
```
