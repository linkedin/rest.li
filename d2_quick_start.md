---
layout: get_started
title: Rest.li D2 Dynamic discovery tutorial
permalink: /start/d2_quick_start
excerpt:  Rest.li D2 Dynamic discovery tutorial
index: 1
---
# Dynamic Discovery (D2) Quick Start 

## Contents

In this tutorial, we will explain the basic concepts of D2 using a
simple client server project. [Apache
Zookeeper](http://zookeeper.apache.org/) is required for doing this
tutorial. The completed code for this tutorial is available in Rest.li’s
examples/d2-quickstart:

  - [What is D2 in a Nutshell](#what-is-d2-in-a-nutshell)
  - [The Tutorial](#the-tutorial)
      - [Step 1. Create a Server](#step-1-create-a-server)
      - [Step 2. Create a Config Runner](#step-2-create-a-config-runner)
      - [Step 3. Create a Client](#step-3-create-a-client)
  - [Next Steps](#next-steps)

## What is D2 in a Nutshell

Imagine we have a Service Oriented Architecture. Let’s say we have
hundreds of servers. Each server can host different set of services.
Some of those services may be partitioned, so a server may belong to some
specific partitions for those services. We use D2 to store information
about which server can serve what service. So this means with D2, a
client requesting a particular service doesn’t need to know where the
physical servers are. The client can ask D2 to route a request to the
right server. D2 is similar to DNS in some ways. At the core, D2 is a layer of indirection between a client and a
server. However, D2 supports many other goodies like client side load
balancing, partitioning, and multi-data-center routing.

Our smallest unit of indirection is called a **service**. A service can
be a URL endpoint, a Rest.li resource, or anything else as long as the
name of the service is *unique*. A collection of services is called a
**cluster**. A service that belongs to one cluster cannot belong to a
different cluster. A cluster has *one-to-many* relationship to a
service. All this information about clusters and services is stored in
Zookeeper.

A server joins a cluster by creating an ephemeral node in Zookeeper.
When a server dies, Zookeeper will notice because the heart beat message
is not refreshed. Then, the ephemeral node is automatically removed.

A client attempting to send a request to a service first consults
Zookeeper to find out which cluster owns the service. Then, the client
queries Zookeeper for all the ephemeral nodes (servers) for that
cluster. Given a list of ephemeral nodes, the client will deliberately
choose a server to send the request to.

That is all you need to know about D2 in a nutshell.

## The Tutorial

We will create a basic client server application in Java. We use gradle
for our build process. The top level structure of our project will have
3 subdirectories:

    /server
    /client
    /config

You also need a settings.gradle and build.gradle file in the root
directory. 

For settings.gradle:

    include 'server'
    include 'client'
    include 'config'

This will tell gradle that gradle should search for `server`, `client`,
`config` directories and mark them as part of the project.

For build.gradle:

    allprojects {
        apply plugin: 'idea'
        apply plugin: 'eclipse'
    }
    
    final pegasusVersion = '1.20.0'
    ext.spec = [
        'product' : [
            'pegasus' : [
                    'r2' : 'com.linkedin.pegasus:r2:' + pegasusVersion,
                    'd2' : 'com.linkedin.pegasus:d2:' + pegasusVersion
            ]
        ]
    ]
    
    subprojects {
        repositories {
            mavenLocal()
            mavenCentral()
        }
    }

This tells gradle that it should use pegasus artifact version 1.20.0
from the maven central repository. This also tells gradle we have
dependencies to r2 and d2 libraries.

### Step 1. Create a Server

Create the following project structure in the `server` sub-directory:

  - **d2-quickstart/**
      - **client/**
      - **config/**
      - **server/**
          - **build.gradle**
          - **src/**
              - **main/**
                  - **java/**
                      - **com/**
                          - **example/**
                              - **d2/**
                                  - **server/**
                                      - **EchoServer.java**
                                      - **ExampleD2Server.java**
                  - **config/**
                      - **server.json**

In this example, we are creating an echo server to illustrate a real
production server. The echo server always returns HTTP status code 200
and prints to stdout when a request comes in.

First, we add our compile dependencies to the server’s build.gradle:

    apply plugin: 'java'
    
    dependencies {
        compile 'com.googlecode.json-simple:json-simple:1.1.1'
        compile spec.product.pegasus.r2
        compile spec.product.pegasus.d2
    }
    

Here is the implementation of the echo server:

    package com.example.d2.server;
    
    import com.sun.net.httpserver.HttpExchange;
    import com.sun.net.httpserver.HttpHandler;
    import com.sun.net.httpserver.HttpServer;
    
    import java.io.IOException;
    import java.io.OutputStream;
    import java.net.InetSocketAddress;
    import java.util.Date;
    import java.util.List;
    
    public class EchoServer
    {
      private final int        _port;
      private final HttpServer _server;
    
      public EchoServer (int port, final String name, List<String> contextPaths)
          throws IOException
      {
        _port = port;
        _server = HttpServer.create(new InetSocketAddress(_port), 0);
        for (String contextPath : contextPaths)
        {
          _server.createContext(contextPath, new MyHandler(contextPath, name));
        }
        _server.setExecutor(null);
      }
    
      static class MyHandler implements HttpHandler
      {
        private final String _name;
        private final String _serverName;
    
        private MyHandler(String name, String serverName)
        {
          _name = name;
          _serverName = serverName;
        }
    
        public void handle(HttpExchange t) throws IOException
        {
          System.out.println(new Date().toString() + ": " + _serverName
                                 + " received a request for the context handler = " + _name );
          String response = "Successfully contacted server " + _serverName;
          t.sendResponseHeaders(200, response.length());
          OutputStream os = t.getResponseBody();
          os.write(response.getBytes());
          os.close();
        }
      }
    
      public void start()
          throws IOException
      {
        _server.start();
      }
    
      public void stop()
          throws IOException
      {
        _server.stop(0);
      }
    
    }

We store the configuration for the servers in server.json. Here is the
content of server.json:

    {
        "echoServers" :
            [
                {
                    "name" : "RecommendationService-1",
                    "port" : 39901,
                    "threadPoolSize" : 1,
                    "contextPaths" : [
                        "/articleRecommendation",
                        "/jobRecommendation"
                    ]
                },
                {
                    "name" : "RecommendationService-2",
                    "port" : 39902,
                    "threadPoolSize" : 1,
                    "contextPaths" : [
                        "/articleRecommendation",
                        "/jobRecommendation"
                    ]
                },
                {
                    "name" : "RecommendationService-3",
                    "port" : 39903,
                    "threadPoolSize" : 1,
                    "contextPaths" : [
                        "/articleRecommendation",
                        "/jobRecommendation"
                    ]
                },
                {
                    "name" : "NewsService-1",
                    "port" : 39904,
                    "threadPoolSize" : 1,
                    "contextPaths" : [
                        "/newsArticle"
                    ]
                },
                {
                    "name" : "NewsService-2",
                    "port" : 39905,
                    "threadPoolSize" : 1,
                    "contextPaths" : [
                        "/newsArticle"
                    ]
                },
                {
                    "name" : "NewsService-3",
                    "port" : 39906,
                    "threadPoolSize" : 1,
                    "contextPaths" : [
                        "/newsArticle"
                    ]
                }
            ],
        "d2Servers" :
            [
                {
                    "serverUri" : "http://localhost:39901",
                    "d2Cluster" : "RecommendationService",
                    "partitionData" : {
                        "0" : {
                            "weight" : "1.0"
                        }
                    }
                },
                {
                    "serverUri" : "http://localhost:39902",
                    "d2Cluster" : "RecommendationService",
                    "partitionData" : {
                        "0" : {
                            "weight" : "1.0"
                        }
                    }
                },
                {
                    "serverUri" : "http://localhost:39903",
                    "d2Cluster" : "RecommendationService",
                    "partitionData" : {
                        "0" : {
                            "weight" : "1.0"
                        }
                    }
                },
                {
                    "serverUri" : "http://localhost:39904",
                    "d2Cluster" : "NewsService",
                    "partitionData" : {
                        "0" : {
                            "weight" : "1.0"
                        }
                    }
                },
                {
                    "serverUri" : "http://localhost:39905",
                    "d2Cluster" : "NewsService",
                    "partitionData" : {
                        "0" : {
                            "weight" : "1.0"
                        }
                    }
                },
                {
                    "serverUri" : "http://localhost:39906",
                    "d2Cluster" : "NewsService",
                    "partitionData" : {
                        "0" : {
                            "weight" : "1.0"
                        }
                    }
                }
            ],
        "zkConnectString" : "localhost:2181",
        "zkSessionTimeout" : 5000,
        "zkBasePath" : "/d2",
        "zkRetryLimit" : 10,
        "announcerStartTimeout" : 5000,
        "announcerShutdownTimeout" : 5000
    }

In the configuration, above we have 6 echo servers and 6 d2 announcers.
The first 3 echo servers belong to RecommendationService, and the
remaining echo servers belong to NewsService.

Finally, we add the task of running this server to the server’s
build.gradle:

    
    task runServer(type: JavaExec) {
        main = 'com.example.d2.server.ExampleD2Server'
        classpath = sourceSets.main.runtimeClasspath
        standardInput = System.in
    }
    

In order to run the server, you run this command:

```../../gradlew runServer
```

### Step 2. Create a Config Runner

Create the following project structure in the ‘config’ sub-directory:

  - **d2-quickstart/**
      - **client/**
      - **config/**
          - **build.gradle**
          - **src/**
              - **main/**
                  - **java/**
                      - **com/**
                          - **example/**
                              - **d2/**
                                  - **config/**
                                      - **ConfigRunner.java**
                  - **d2Config/**
                      - **d2Config.json**
      - **server/**

We specify the mapping of clusters and services in d2Config.json. In
real production scenario, we can do a lot more with d2Config. We can
configure how the load balancer behaves. We can also set up partitioning
and sticky routings.

But for simplicity we won’t include all these in this example. Here is
how our d2Config.json going to look like:

    
    {
        "d2Clusters" : {
            "RecommendationService": {
                "services":
                    {
                        "articleRecommendation": {
                            "path" : "/articleRecommendation"
                        },
                        "jobRecommendation": {
                            "path" : "/jobRecommendation"
                        }
                    }
    
            },
            "NewsService": {
                "services":
                    {
                        "newsArticle" : {
                            "path" : "/newsArticle"
                        }
                    }
    
            }
        },
        "defaultServiceProperties" : {
            "loadBalancerStrategyList" : [
                "degraderV3",
                "degraderV2"
            ],
            "prioritizedSchemes" : [
                "http"
            ],
            "loadBalancerStrategyProperties" : {
                "http.loadBalancer.updateIntervalMs" : "5000",
                "http.loadBalancer.pointsPerWeight" : "100"
            },
            "transportClientProperties" : {
                "http.requestTimeout" : "10000"
            },
            "degraderProperties" : {
                "degrader.minCallCount" : "10",
                "degrader.lowErrorRate" : "0.01",
                "degrader.highErrorRate" : "0.1"
            }
        },
        "zkConnectString" : "localhost:2181",
        "zkSessionTimeout" : 5000,
        "zkBasePath" : "/d2",
        "zkRetryLimit" : 10
    }
    

From reading the configuration above you probably have questions about
these properties. So here are some explanation for some non-obvious
ones.

  - **loadBalancerStrategyList** was set to a list consisting of
    **degraderV3** and **degraderV2**. This means, we try to use
    **degraderV3** if possible. The difference between **degraderV3**
    and **degraderV2** is degraderV3 supports *partitioning* while
    degraderV2 does not. 
  - For schemes we support **https** and **http**, but for simplicity
    we’ll use only **http**. Using **https** require us to wire in SSL
    parameter and that’s beyond the scope of this example. 

The rest of the config values should be pretty obvious. For the list of
all configuration please see [D2 Zookeeper Properties](/rest.li/D2-Zookeeper-Properties).

Then we modify the config’s build.gradle to add our java dependencies.

    
    apply plugin: 'java'
    
    dependencies {
        compile 'com.googlecode.json-simple:json-simple:1.1.1'
        compile spec.product.pegasus.d2
    }
    

Next we will create a java class that reads this config and publish it
to zookeeper. We have a utility class called D2Config that does this for
you. But we have to feed D2Config some parameters in order for it to
work.

Here’s the java class for running the D2Config.

    
    package com.example.d2.config;
    import com.linkedin.d2.discovery.util.D2Config;
    import org.json.simple.JSONObject;
    import org.json.simple.parser.JSONParser;
    
    import java.io.File;
    import java.io.FileReader;
    import java.util.Collections;
    import java.util.Map;
    
    public class ConfigRunner
    {
      public static void main(String[] args)
          throws Exception
      {
        //get server configuration
        String path = new File(new File(".").getAbsolutePath()).getCanonicalPath() +
            "/src/main/d2Config/d2Config.json";
        JSONParser parser = new JSONParser();
        Object object = parser.parse(new FileReader(path));
        JSONObject json = (JSONObject) object;
        System.out.println("Finished parsing d2 topology config");
    
        String zkConnectString = (String)json.get("zkConnectString");
        int zkSessionTimeout = ((Long)json.get("zkSessionTimeout")).intValue();
        String zkBasePath = (String)json.get("zkBasePath");
        int zkRetryLimit = ((Long)json.get("zkRetryLimit")).intValue();
    
        Map<String,Object> serviceDefaults = (Map<String, Object>)json.get(
            "defaultServiceProperties");
    
        //this contains the topology of our system
        Map<String,Object> clusterServiceConfigurations =
            (Map<String, Object>)json.get("d2Clusters");
    
        System.out.println("Populating zookeeper with d2 configuration");
    
        //d2Config is the utility class for populating zookeeper with our topology
        //some the params are not needed for this simple example so we will just use
        //default value by passing an empty map
        D2Config d2Config = new D2Config(zkConnectString, zkSessionTimeout, zkBasePath,
                                         zkSessionTimeout, zkRetryLimit,
                                         (Map<String, Object>)Collections.EMPTY_MAP,
                                         serviceDefaults,
                                         clusterServiceConfigurations,
                                         (Map<String, Object>)Collections.EMPTY_MAP,
                                         (Map<String, Object>)Collections.EMPTY_MAP);
    
        //populate zookeeper
        d2Config.configure();
        System.out.println("Finished populating zookeeper with d2 configuration");
      }
    }
    

Finally, we add a task in config’s build.gradle to run the above Java
class.

    
    task runConfigRunner(type: JavaExec) {
        main = 'com.example.d2.config.ConfigRunner'
        classpath = sourceSets.main.runtimeClasspath
        standardInput = System.in
    }
    

In order to run D2Config run ../../gradlew runConfigRunner

### Step 3. Create a Client

Create the following project structure in the ‘client’ subdirectory:

  - **d2-quickstart/**
      - **client/**
          - **build.gradle**
          - **src/**
              - **main/**
                  - **java/**
                      - **com/**
                          - **example/**
                              - **d2/**
                                  - **client/**
                                      - **ExampleD2Client.java**
                  - **config/**
                      - **client.json**
      - **config/**
      - **server/**

First we create build.gradle to declare our java dependencies

    
    apply plugin: 'java'
    dependencies {
        compile 'com.googlecode.json-simple:json-simple:1.1.1'
        compile spec.product.pegasus.d2
    }
    

Then we need the client code to instantiate D2 client and to send
traffic through the D2 client. Here’s how our ExampleD2Client looks
like:

    
    package com.example.d2.client;
    
    import com.linkedin.common.callback.Callback;
    import com.linkedin.common.util.None;
    import com.linkedin.d2.balancer.D2Client;
    import com.linkedin.d2.balancer.D2ClientBuilder;
    import com.linkedin.r2.message.rest.RestRequest;
    import com.linkedin.r2.message.rest.RestRequestBuilder;
    import org.json.simple.JSONObject;
    import org.json.simple.parser.JSONParser;
    import org.json.simple.parser.ParseException;
    
    import java.io.File;
    import java.io.FileReader;
    import java.io.IOException;
    import java.net.URI;
    import java.net.URISyntaxException;
    import java.util.Map;
    import java.util.concurrent.CountDownLatch;
    import java.util.concurrent.ExecutorService;
    import java.util.concurrent.Executors;
    import java.util.concurrent.ScheduledExecutorService;
    import java.util.concurrent.ScheduledFuture;
    import java.util.concurrent.TimeUnit;
    
    public class ExampleD2Client
    {
      public static void main(String[] args)
          throws IOException, ParseException, InterruptedException
      {
        //get client configuration
        JSONObject json = parseConfig();
        String zkConnectString = (String) json.get("zkConnectString");
        Long zkSessionTimeout = (Long) json.get("zkSessionTimeout");
        String zkBasePath = (String) json.get("zkBasePath");
        Long zkStartupTimeout = (Long) json.get("zkStartupTimeout");
        Long zkLoadBalancerNotificationTimeout = (Long) json.get("zkLoadBalancerNotificationTimeout");
        String zkFlagFile = (String) json.get("zkFlagFile");
        String fsBasePath = (String) json.get("fsBasePath");
        final Map<String, Long> trafficProportion = (Map<String, Long>) json.get("trafficProportion");
        final Long clientShutdownTimeout = (Long) json.get("clientShutdownTimeout");
        final Long clientStartTimeout = (Long) json.get("clientStartTimeout");
        Long rate = (Long) json.get("rateMillisecond");
        System.out.println("Finished parsing client config");
    
        //create d2 client
        final D2Client d2Client = new D2ClientBuilder().setZkHosts(zkConnectString)
                                                          .setZkSessionTimeout(
                                                              zkSessionTimeout,
                                                              TimeUnit.MILLISECONDS)
                                                          .setZkStartupTimeout(
                                                              zkStartupTimeout,
                                                              TimeUnit.MILLISECONDS)
                                                          .setLbWaitTimeout(
                                                              zkLoadBalancerNotificationTimeout,
                                                              TimeUnit.MILLISECONDS)
                                                          .setFlagFile(zkFlagFile)
                                                          .setBasePath(zkBasePath)
                                                          .setFsBasePath(fsBasePath)
                                                          .build();
    
        System.out.println("Finished creating d2 client, starting d2 client...");
    
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        final CountDownLatch latch = new CountDownLatch(1);
    
        //start d2 client by connecting to zookeeper
        startClient(d2Client, executorService, clientStartTimeout,
                    new Callback<None>()
                    {
                      @Override
                      public void onError (Throwable e)
                      {
                        System.exit(1);
                      }
    
                      @Override
                      public void onSuccess (None result)
                      {
                        latch.countDown();
                      }
                    });
        latch.await();
        System.out.println("D2 client is sending traffic");
    
        ScheduledFuture task = executorService.scheduleAtFixedRate(new Runnable()
        {
          @Override
          public void run ()
          {
            try
            {
              sendTraffic(trafficProportion, d2Client);
            }
            catch (URISyntaxException e)
            {
              e.printStackTrace();
            }
          }
        }, 0, rate, TimeUnit.MILLISECONDS);
    
        System.out.println("Press enter to stop D2 client...");
        System.in.read();
        task.cancel(false);
        System.out.println("Shutting down...");
        shutdown(d2Client, executorService, clientShutdownTimeout);
      }
    
      private static void startClient(final D2Client d2Client,
                                      ExecutorService executorService,
                                      Long timeout,
                                      final Callback<None> callback)
      {
        try
        {
          executorService.submit(new Runnable()
          {
            @Override
            public void run ()
            {
              d2Client.start(new Callback<None>()
              {
                @Override
                public void onError (Throwable e)
                {
                  System.err.println("Error starting d2Client. Aborting... ");
                  e.printStackTrace();
                  System.exit(1);
                }
    
                @Override
                public void onSuccess (None result)
                {
                  System.out.println("D2 client started");
                  callback.onSuccess(None.none());
                }
              });
            }
          }).get(timeout, TimeUnit.MILLISECONDS);
        }
        catch (Exception e)
        {
          System.err.println("Cannot start d2 client. Timeout is set to " +
                                 timeout + " ms");
          e.printStackTrace();
        }
      }
    
      private static void shutdown(final D2Client d2Client,
                                   ExecutorService executorService,
                                   Long timeout)
      {
        try
        {
          executorService.submit(new Runnable()
          {
            @Override
            public void run ()
            {
              d2Client.shutdown(new Callback<None>()
              {
                @Override
                public void onError (Throwable e)
                {
                  System.err.println("Error shutting down d2Client.");
                  e.printStackTrace();
                }
    
                @Override
                public void onSuccess (None result)
                {
                  System.out.println("D2 client stopped");
                }
              });
            }
          }).get(timeout, TimeUnit.MILLISECONDS);
        }
        catch (Exception e)
        {
          System.err.println("Cannot stop d2 client. Timeout is set to " +
                                 timeout + " ms");
          e.printStackTrace();
        }
        finally
        {
          executorService.shutdown();
        }
      }
    
      private static JSONObject parseConfig()
          throws IOException, ParseException
      {
        String path = new File(new File(".").getAbsolutePath()).getCanonicalPath() +
            "/src/main/config/client.json";
        JSONParser parser = new JSONParser();
        Object object = parser.parse(new FileReader(path));
        return (JSONObject) object;
      }
    
      private static void sendTraffic(Map<String, Long> trafficProportion, D2Client d2Client)
          throws URISyntaxException
      {
        for (Map.Entry<String, Long> entry : trafficProportion.entrySet())
        {
          URI uri = new URI("d2://" + entry.getKey());
          RestRequest request = new RestRequestBuilder(uri).setMethod("get").build();
          for (long i = 0; i < entry.getValue(); i++)
          {
            //we don't care about the result from the server after all,
            //you can see the traffic hits the echo server from stdout
            d2Client.restRequest(request);
          }
        }
      }
    }
    

In the above code, sendTraffic() will send request based on the
trafficProportion configured in client.json. Let’s configure the config
so that the client sends:

  - 3 requests to “newsArticle” service every 1000 ms. 
  - 2 requests to “jobRecommendation” service every 1000 ms.
  - 1 request to “articleRecommendation” service every 1000 ms.

Here’s our client.json:

    
    {
        "zkConnectString" : "localhost:2181",
        "zkSessionTimeout" : 5000,
        "zkStartupTimeout" : 5000,
        "zkLoadBalancerNotificationTimeout" : 5000,
        "zkFlagFile" : "/tmp/suppressZkFlag",
        "zkBasePath" : "/d2",
        "fsBasePath" : "/tmp/backup",
        "clientShutdownTimeout" : 5000,
        "clientStartTimeout" : 5000,
        "trafficProportion" : {
            "newsArticle": 3,
            "jobRecommendation": 2,
            "articleRecommendation" : 1
        },
        "rateMillisecond" : 1000
    }
    

Now we are ready to add the following task to build.gradle:

    
    task runClient(type: JavaExec) {
        main = 'com.example.d2.client.ExampleD2Client'
        classpath = sourceSets.main.runtimeClasspath
        standardInput = System.in
    }
    

To run the client, run the following command in a different terminal
console: 

    ../../gradlew runClient

## Next Steps

Congratulations! You have finished this tutorial. Now you can build
your own D2 client/server applications. Next, you can learn the advanced
features of D2 examples like the following:

  - Partitioning and sticky routing
  - Tuning load balancer
  - Overriding client properties
  - And many more

To do so, check out the examples in the Res.tli source code. Go to
[/example/d2-advanced-examples](https://github.com/linkedin/rest.li/tree/master/examples/d2-advanced-example).

See also [Dynamic Discovery](/rest.li/Dynamic_Discovery).
