---
layout: api_reference
title: Rest.li with Netty
permalink: /Rest_li-with-Netty
excerpt: Rest.li can be run in a variety of HTTP frameworks. Out of the box, Rest.li supports both Netty and Servlet containers, such as Jetty.
---

# Rest.li with Netty

## Contents

  - [Basics](#basics)
  - [Programmatically calling the Netty Launcher](#programmatically-calling-the-netty-launcher)
  - [Calling the Netty Launcher from the Command Line](#calling-the-netty-launcher-from-the-command-line)
  - [Calling the Netty Launcher from a Gradle Task](#calling-the-netty-launcher-from-a-gradle-task)

## Basics

Rest.li can be run in a variety of HTTP frameworks. Out of the box, Rest.li supports both Netty and Servlet containers, such as Jetty.

Rest.li includes a `restli-netty-standalone` artifact containing a single class: `com.linkedin.restli.server.NettyStandaloneLauncher`.  This launcher class configures a Netty server to dispatch requests to all Rest.li resources that are both in the current classpath and in the list of package names the launcher is provided when it is created.

## Programmatically calling the Netty Launcher

```java
    import com.linkedin.restli.server.NettyStandaloneLauncher;
    ...
    NettyStandaloneLauncher launcher = new NettyStandaloneLauncher(
      8080 /*port*/,
      "com.example.fortunes" /* resource package(s) */
    );
    launcher.start();
    // ... server is running.  launcher.stop() can be called to stop it.
```

Thread pool sizes may also optionally be configured using the overloaded constructor.

## Calling the Netty Launcher from the Command Line

    java -cp <classpath> com.linkedin.restli.server.NettyStandaloneLauncher 8080 com.example.fortunes

## Calling the Netty Launcher from a Gradle Task

```groovy
    task startFortunesServer(type: JavaExec) {
      main = 'com.linkedin.restli.server.NettyStandaloneLauncher'
      args = ['-port', '8080', '-packages', 'com.example.fortune.impl']
      classpath = sourceSets.main.runtimeClasspath
      standardInput = System.in
    }
```

Thread pool sizes may also optionally be configured by providing a number to the '-thread' and '-parseqthreads' args.