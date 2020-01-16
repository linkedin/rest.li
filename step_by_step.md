---
layout: get_started
title: Quickstart - A Tutorial Introduction to Rest.li
permalink: /start/step_by_step
excerpt: Quickstart - A step by step tutorial introduction to Rest.li. Create a Rest.li Server and Client.
index: 1
---

# Tutorial to Create a Rest.li Server and Client

## Contents

  - [Introduction](#introduction)
  - [Example Source Code - Top Level
    Structure](#example-source-code--top-level-structure)
  - [Creating a Server](#creating-a-server)
      - [Step 1. Define Data Schema](#step-1-define-data-schema)
      - [Step 2. Generate Java Bindings](#step-2-generate-java-bindings)
      - [Step 3. Implement Rest.li Server
        Resource](#step-3-implement-restli-server-resource)
      - [Step 4. Build and Run the
        Server](#step-4-build-and-run-the-server)
  - [Publishing Server’s Interface
    Definition](#publishing-servers-interface-definition)
  - [Creating a Client](#creating-a-client)
      - [Step 1. Generate Client Request
        Builders](#step-1-generate-client-request-builders)
      - [Step 2. Implement Client Class](#step-2-implement-client-class)
      - [Step 3. Build and Run the
        Client](#step-3-build-and-run-the-client)
  - [Recap](#recap)

## Introduction

In this tutorial, we’ll take a first look at Rest.li and learn about some of its most basic features. We’ll construct a server that responds with *Fortunes* for GET requests and also creates a client that sends a request to the server and prints a fortune returned by the server.

Rest.li uses an inversion of control model in which Rest.li defines the
client and server architecture and handles many details of constructing,
receiving, and processing RESTful requests. On the server side, Rest.li
calls your code at the appropriate time to respond to requests. You only
need to worry about your application-specific response to requests. On
the client side, Rest.li helps send type-safe requests to the server and
receives type-safe responses.

To allow Rest.li to perform its tasks, you need to conform to a simple
architecture, in which you define a schema for your data, and classes
that support REST operations on that data. Your classes will designate
handlers for REST operations using Annotations and return objects that
represent your data schema. Rest.li will handle mostly everything else.

We’ll see how Rest.li helps you perform these actions using automatic
code generation, supporting base classes and other infrastructure.

**Note**: You will notice references to ‘Pegasus’ in various places as
you work through this tutorial and read other Rest.li documents. Pegasus
is the code name for the project that includes Rest.li and some related
modules. It is also used in some package names.

## Example Source Code - Top Level Structure

If you like to do things yourself, you should be able to enter the code
in this tutorial into whatever editor you like and construct each step
of the process. You can also follow along using the ready-made source in
the repository under
[examples/quickstart](https://github.com/linkedin/rest.li/tree/master/examples/quickstart)
directory. Using the provided source tree frees you from worrying about
the build scripts and directory structure until you want to use Rest.li
in your own projects.

The example can be built using Gradle. Many of the steps involve code
generation that is automated by Gradle plugins provided as part of
Rest.li. We’ll show you the basic build scripts you need for this
example as we go along. For more details about the build process see
[Gradle Build Integration](/rest.li/setup/gradle). You will need
Gradle 1.6+ (run `gradle --version` to check). If you have a different
Gradle version and do not want to install the version required by this
example globally, we recommend quickly setting up a [Gradle
wrapper](http://www.gradle.org/docs/current/userguide/gradle_wrapper.html)
for this project).

Before we get started, you’ll need to create a basic directory structure
to hold your classes. At the root of the example source tree, you should
have three sub-directories, `api/`, `client/` and `server/`.

You will also need `build.gradle` and `settings.gradle` files at the top
level.

The `settings.gradle` file just includes the sub-projects:

##### file: example-standalone-app/settings.gradle

```gradle
include 'api'
include 'server'
include 'client'
```

The file `build.gradle` should contain:

##### file: example-standalone-app/build.gradle

```gradle
buildscript {
  repositories {
    mavenLocal()
    mavenCentral()
  }
  dependencies {
    classpath 'com.linkedin.pegasus:gradle-plugins:11.0.17'
  }
}

task wrapper(type: Wrapper) {
  gradleVersion = '4.1'
}

final pegasusVersion = '11.0.17'
ext.spec = [
  'product' : [
    'pegasus' : [
      'data' : 'com.linkedin.pegasus:data:' + pegasusVersion,
      'generator' : 'com.linkedin.pegasus:generator:' + pegasusVersion,
      'r2Netty' : 'com.linkedin.pegasus:r2-netty:' + pegasusVersion,
      'restliCommon' : 'com.linkedin.pegasus:restli-common:' + pegasusVersion,
      'restliClient' : 'com.linkedin.pegasus:restli-client:' + pegasusVersion,
      'restliServer' : 'com.linkedin.pegasus:restli-server:' + pegasusVersion,
      'restliTools' : 'com.linkedin.pegasus:restli-tools:' + pegasusVersion,
      'gradlePlugins' : 'com.linkedin.pegasus:gradle-plugins:' + pegasusVersion,
      'restliNettyStandalone' : 'com.linkedin.pegasus:restli-netty-standalone:' + pegasusVersion,
      'restliServerStandalone' : 'com.linkedin.pegasus:restli-server-standalone:' + pegasusVersion
    ]
  ]
]

allprojects {
  apply plugin: 'idea'
  apply plugin: 'eclipse'
}

subprojects {
  apply plugin: 'maven'

  afterEvaluate {
    // add the standard pegasus dependencies wherever the plugin is used
    if (project.plugins.hasPlugin('pegasus')) {
      dependencies {
        dataTemplateCompile spec.product.pegasus.data
        restClientCompile spec.product.pegasus.restliClient
      }
    }
  }

  repositories {
    mavenLocal()
    mavenCentral()
  }
}
```

This gradle build file pulls all required jars from a global Maven
repository. It also loads some plugins that facilitate the build process
and various code generation steps. Notice that plugins are also provided
for IntelliJ Idea and Eclipse. For example, executing:

    $ gradle idea

will generate an Idea project ready to open in Idea. Using Idea or
Eclipse is a handy way to explore and follow along as you read this
tutorial.

Here’s how the structure of your top-level project should look as we
begin:

    example-standalone-app/
    +- build.gradle
    +- settings.gradle
    +- api/
    +- client/
    +- server/
    

## Creating a Server

The first thing we will do is implement a very simple server that
responds to GET requests.

The basic steps you will follow to create a Rest.li server are:

1.  Define data schema. Rest.li uses [Pegasus Data Schema](/rest.li/pdl_syntax) to
    define the resource data.

2. Generate language bindings. Rest.li will generate java class
bindings for these data schemas to be used in your server and clients.

3. Implement resource classes containing methods to act on your data.
Rest.li provides a set of base classes and annotations that will map
these methods to URIs and REST operations.

4. Create an HTTP server that instantiates a Rest.li server. The
Rest.li server will automatically locate your resource classes and
invoke the appropriate methods when a request is received.

Rest.li provides tools to make these steps simple, including code
generators that create classes from the data schema, base classes, and
annotations that map entry points in your code to REST operations.

Let’s walk through each step of the process.

### Step 1. Define Data Schema

The first step in creating a Rest.li service is to define a data model
or schema for the data that will be returned by your server. We will
define the data model in the `api/` directory, which serves to define
the API or interface between the server and clients.

All Rest.li data models are defined in Pegasus Data Schema files, which
have a `.pdl` suffix. We’ll define a `Fortune` data model in
`Fortune.pdl`. The location of this file is important. Be sure to place
it in a path corresponding to your namespace, under
`api/src/main/pegasus/`:

##### file: example-standalone-app/api/src/main/pegasus/com/example/fortune/Fortune.pdl

```pdl
namespace com.example.fortune

/**
 * Generate a fortune cookie
 */
record Fortune {

  /**
   * The Fortune cookie string
   */
  fortune: string
}
``` 

`Fortune.pdl` defines a record named Fortune, with an associated
namespace. The record has one field, a string whose name is `fortune`.
Fields as well as the record itself can have optional documentation
strings. This is, of course, a very simple schema. See
[Data Schemas](/rest.li/pdl_syntax) for
details on the syntax and more complex examples.

### Step 2. Generate Java Bindings

Rest.li uses the data model in `.pdl` files to generate java versions
of the model that can be used by the server. The easiest way to generate
these classes is to use the Gradle integration provided as part of
Rest.li. You will need a `build.gradle` file in the `api/` directory
that looks like this:

##### file: example-standalone-app/api/build.gradle

```gradle
apply plugin: 'pegasus'
```

With `Fortune.pdl` and `build.gradle` files in place, you can generate
a Java binding for the data model. This Java version is what will
actually be used by your server to return data to calling clients.
Change into the `api/` directory and run the following command:

    $ gradle build

The `pegasus` Gradle plugin will detect the presence of `Fortune.pdl`
and use the *dataTemplateGenerator* to generate `Fortune.java`. The
generated java classes will be placed under
`api/src/mainGeneratedDataTemplate/` directory.

Your file system structure should now look like this:

    example-standalone-app/
    +- build.gradle
    +- settings.gradle
    +- api/
    |  +- build.gradle
    |  +- src/
    |     +- main/
    |     |  +- pegasus/
    |     |     +- com/
    |     |        +- example/
    |     |           +- fortune/
    |     |              +- Fortune.pdl
    |     +- mainGeneratedDataTemplate/
    |        +- java/
    |           +- com/
    |              +- example/
    |                 +- fortune/
    |                    +- Fortune.java
    +- client/
    +- server/
    

The generated java file contains a java representation of the data model
defined in the schema, and includes `get` and `set` methods for each
element of the model, as well as other supporting methods. You can look
at the generated file to see the full implementation if you are curious;
the following excerpt should give you the general idea. This class is
entirely derived from your data model and should not be
modified.

##### file: example-standalone-app/api/src/mainGeneratedDataTemplate/java/com/example/fortune/Fortune.java

```java
@Generated(...)
public class Fortune extends RecordTemplate {
  public String getFortune() {
    return getFortune(GetMode.STRICT);
  }

  public Fortune setFortune(String value) {
    putDirect(FIELD_Fortune, String.class, String.class, value, SetMode.DISALLOW_NULL);
    return this;
  }

  // ... other methods
}
```    

### Step 3. Implement Rest.li Server Resource

Now that we have defined our data model, the next step is to define a
`resource` class that will be invoked by the Rest.li server in
response to requests from clients. We’ll create a class named
`FortunesResource`. This class is written by hand, and implements any
REST operations you want to support, returning data using the java data
model class generated in the previous step. The file should be placed
according to your package path under
`server/src/main/java`.

##### file: example-standalone-app/server/src/main/java/com/example/fortune/impl/FortunesResource.java

```java
package com.example.fortune.impl;

import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import com.example.fortune.Fortune;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple Rest.li Resource that serves up a fortune cookie.
 */
@RestLiCollection(name = "fortunes", namespace = "com.example.fortune")
public class FortunesResource extends CollectionResourceTemplate<Long, Fortune> {

  // In-memory store for the fortunes
  static Map<Long, String> fortunes = new HashMap<Long, String>();
  static {
    fortunes.put(1L, "Today is your lucky day.");
    fortunes.put(2L, "There's no time like the present.");
    fortunes.put(3L, "Don't worry, be happy.");
  }

  @Override
  public Fortune get(Long key) {
    // Retrieve the requested fortune
    String fortune = fortunes.get(key);
    if (fortune == null) {
      fortune = "Your luck has run out. No fortune for id = " + key;
    }

    // return an object that represents the fortune cookie
    return new Fortune().setFortune(fortune);
  }
}
```    

FortunesResource extends a Rest.li class, `CollectionResourceTemplate`
and, for this simple example, overrides a single method, `get`, which
takes a single argument, an id of a resource to be returned. Rest.li
will call this method when it dispatches a GET request to the Fortune
resource. Additional REST operations could be provided by overriding
other methods. See the [Rest.li User
Guide](/rest.li/user_guide/server_architecture) for more details about
supporting additional REST methods and other types of resources.

Notice that if this GET were to perform any IO it would be `blocking`,
meaning that the thread handling this request will wait for that IO to
complete. Later we will show how we can build async GET methods that
return [ParSeq](https://github.com/linkedin/parseq) `Promise` and `Task` 
classes so that we do not block while performing IO operations.

The `RestLiCollection` annotation at the top of the file marks this class as a REST collection, and declares that this resource handles the `/fortunes` URI. The result is that calling `http://localhost/fortunes/<id>` (assuming your server is running on localhost) will call `FortunesResource.get()`, which should return a `Fortune` object corresponding to the given fortune identifier. For this simple implementation, we will create a static HashMap that maps several fortune strings to ids. If a requested id is found in the HashMap, we will construct a `Fortune` object, set the message and id, and return the object. If the requested id is not found, we’ll return a default message. Rest.li will handle delivering the result to the calling client as a JSON object. (Recall that Fortune.java was generated in a previous step and is found under the `api` directory.)

In a real implementation, you would perform whatever steps
are required to retrieve or construct your response to the request. But
ultimately, you will return an instance of your data model class that
represents the data defined in your schema.

### Step 4. Build and Run the Server

We’ve now completed the bulk of our application specific server side
code. We’ve defined our data model, and implemented a Resource class
that can respond to a GET request by returning data according to the
model. The only thing remaining is to configure a HTTP framework to call
our application logic. We will use Netty, an excellent framework that
works great with Rest.li to build fully async services. For details on
how to configure Rest.li with other servlet containers see
[Rest.li with Servlet Containers](/rest.li/Rest_li-with-Servlet-Containers).

Rest.li also includes a Request Response layer (R2) that provides a
transport abstraction and other services.

Notice that Rest.li automatically scans all resource classes in the
specified package and initializes the REST endpoints/routes without any
hard-coded connection. Adding additional resources or operations can be
done simply by expanding your data schema and providing additional
functionality in your Resource class(es).

To compile and run the server, we need a `build.gradle` file in the
`server/` directory, which should look like this:

##### file: example-standalone-app/server/build.gradle

```gradle
apply plugin: 'pegasus'

ext.apiProject = project(':api')

dependencies {
  compile project(path: ':api', configuration: 'dataTemplate')
  compile spec.product.pegasus.restliServer
  compile spec.product.pegasus.restliNettyStandalone
}

task startFortunesServer(type: JavaExec) {
  main = 'com.linkedin.restli.server.NettyStandaloneLauncher'
  args = ['-port', '8080', '-packages', 'com.example.fortune.impl']
  classpath = sourceSets.main.runtimeClasspath
  standardInput = System.in
}
```

Next, create a `gradle.properties` file containing the following line:

##### file: example-standalone-app/server/gradle.properties

```gradle
rest.model.compatibility=ignore
```

This disables some [compatibility checks](/rest.li/setup/gradle#compatibility) on the generated files. You will need these checks in a real project but to keep this example simple we are disabling these checks.

With these files in place, your server directory structure should look
like this:

    example-standalone-app/
    +- build.gradle
    +- settings.gradle
    +- api/
    |  ...
    +- client/
    +- server/
       +- build.gradle
       +- gradle.properties
       +- src/
          +- main/
             +- java/
                +- com/
                   +- example/
                      +- fortune/
                         +- impl/
                           +- FortuneResource.java
    

Now you can build the server from the `server/` directory with:

    $ gradle build

**Note:** If prompted, run the build command a second time. The first build
runs a bootstrapping code generation process, requiring a second build
to compile the generated code.

After building the server, you can launch the server using the following
command:

    $ gradle startFortunesServer

Once the server is running, you can perform tests using `curl`:

    
    $ curl -v http://localhost:8080/fortunes/1
    
    * About to connect() to localhost port 8080 (#0)
    *   Trying ::1... connected
    * Connected to localhost (::1) port 8080 (#0)
    > GET /fortunes/1 HTTP/1.1
    > User-Agent: curl/7.19.7 (x86_64-redhat-linux-gnu) libcurl/7.19.7 NSS/3.12.9.0 zlib/1.2.3 libidn/1.18 libssh2/1.2.2
    > Host: localhost:8080
    > Accept: */*
    > 
    < HTTP/1.1 200 OK
    < X-LinkedIn-Type: com.example.fortune.Fortune
    < Content-Type: application/json
    < Content-Length: 45
    < Server: Jetty(6.1.26)
    < 
    * Connection #0 to host localhost left intact
    * Closing connection #0
    {"id":1,"fortune":"Today is your lucky day."}[
    

Here, `curl` issued a GET request for `/fortunes/1`. Rest.li routed the
request to the `FortunesResource`, which interpreted the argument `1`,
found the corresponding string, and constructed a `Fortune` object to
return. Rest.li automatically transforms the java data model to JSON and
returns the result to the caller.

## Publishing Server’s Interface Definition

Before we move on to look at the Rest.li’s client support, notice that
the process of building the server generated an additional file. If you
look at your directory structure, you should see an IDL file under
`server/src/mainGeneratedRest/idl/`. The file is in JSON format and
defines the interface supported by the server. The interface is
generated as a result of the annotations in the Resource class, in this
example, `FortunesResource.java`. However if for some reason the file is
not available in your filesystem you can generate it by issuing the
following command

    $ cd server
    $ gradle publishRestliIdl

The `publishRestliIdl` task will first run the `generateRestModel` which
creates
`server/src/mainGeneratedRest/idl/com.example.fortune.fortunes.restspec.json`.
It will then copy this file into the `api` module at
`api/src/main/idl/com.example.fortune.fortunes.restspec.json`.

You may also notice a `snapshot/` directory next to the `idl/`
directory. This is used by the compatibility checker to keep track of
changes. You can ignore that for now.

Here is the generated IDL file. Notice that all of this information was
derived from server’s `FortunesResource.java` including the
documentation
strings.

##### file: example-standalone-app/server/src/mainGeneratedRest/idl/com.example.fortune.fortunes.restspec.json

```json
{
  "name" : "fortune",
  "namespace" : "com.example.fortune",
  "path" : "/fortunes",
  "schema" : "com.linkedin.restli.example.Fortune",
  "doc" : "Simple Rest.li Resource that serves up a fortune cookie.\n\ngenerated from: com.example.fortune.impl.FortunesResource",
  "collection" : {
    "identifier" : {
      "name" : "fortuneId",
      "type" : "long"
    },
    "supports" : [ "get" ],
    "methods" : [ {
      "method" : "get"
    } ],
    "entity" : {
      "path" : "/fortunes/{fortuneId}"
    }
  }
}
``` 

This file represents the contract between the server and the client.
Accordingly, the build also copied the IDL to the `api` module, where it
can be accessed by the client code.

Just to verify that everything is in place, this is how your project’s
`api/` and `server/` directories should look at this point:

    example-standalone-app/
    +- build.gradle
    +- settings.gradle
    +- api/
    |  +- build.gradle
    |  +- src/
    |     +- main/
    |     |  +- idl/
    |     |  |  +- com.example.fortune.fortunes.restspec.json
    |     |  +- pegasus/
    |     |  |  +- com/
    |     |  |     +- example/
    |     |  |        +- fortune/
    |     |  |           +- Fortune.pdl
    |     |  +- snapshot/
    |     |     +- com.example.fortune.fortunes.snapshot.json
    |     +- mainGeneratedDataTemplate/
    |        +- java/
    |           +- com/
    |              +- example/
    |                 +- fortune/
    |                    +- Fortune.java
    +- client/
    +- server/
       +- build.gradle
       +- gradle.properties
       +- src/
          +- main/
          |  +- java/
          |     +- com/
          |        +- example/
          |           +- fortune/
          |              +- impl/
          |                +- FortuneResource.java
          +- mainGeneratedRest/
             +- idl/
             |  +- com.example.fortune.fortunes.restspec.json
             +- snapshot/
                +- com.example.fortune.fortunes.snapshot.json
    

## Creating a Client

Now that we have a server implemented and tested with curl, let’s see
how we can use Rest.li to help build a client.

### Step 1. Generate Client Request Builders

Rest.li uses the IDL published by the server to generate client classes
that can be used to construct requests. The `pegasus` Gradle plugin
provides tools to generate these classes. Let’s start by creating a
`build.gradle` file in the `client/` directory:

##### file: examples-standalone-app/client/build.gradle

```gradle
apply plugin: 'java'

dependencies {
  compile project(path: ':api', configuration: 'restClient')
  compile spec.product.pegasus.r2Netty
}

task startFortunesClient(type: JavaExec) {
  main = 'com.example.fortune.RestLiFortunesClient'
  classpath = sourceSets.main.runtimeClasspath
}
```

To generate the interface classes used by the client, change to the
`client/` directory and type the command:

    $ gradle build

Building in the client directory generates java classes that represent
the resources and operations on those resources supported by the server.
These are basically convenience classes that help you *build* requests
from the client side. In this example, you should see some new java
files, including `FortunesRequestBuilders.java` and
`FortunesGetRequestBuilder.java`. These files are placed in the `api/`
module where they can be shared among multiple clients.
`FortunesRequestBuilders` is a factory class that instantiates any
request builders you may need. In this example, our server resource only
supports GET requests, so the process has just generated a
`FortunesGetRequestBuilder` class. You can look at the generated source
code under the `api/src/mainGeneratedRest/` directory, if you’re
interested, but for this tutorial, let’s just go on to creating a client
and see how a builder is used.

**Note:** You may also see two additional files: `FortuneBuilders.java`
and `FortunesGetBuilder.java`. These are deprecated interfaces that were
used prior to Rest.li v1.24.4. If you are just getting started with
Rest.li and using the latest version you can ignore these files.

### Step 2. Implement Client Class

Creating a client involves using a few classes to handle connecting to
the server, and using the Builder classes generated in the previous step
to construct requests. Let’s see how that works before we look at the
actual client code.

The following lines of code instantiate a `FortunesRequestBuilders`
factory, and then call its `get()` method to create a
`FortunesGetRequestBuilder` object. Finally, the
`FortunesGetRequestBuilder` instance lets you supply the information
that needs to be passed in the request and builds a `Request` object:

```java
FortunesRequestBuilders fortunesBuilders = new FortunesRequestBuilders();
FortunesGetRequestBuilder getBuilder = fortunesBuilders.get();
Request<Fortune> getRequest = getBuilder.id(fortuneId).build();
```

The process of sending a request from a client basically consists of
creating a `RestClient` object and invoking its `sendRequest()` method
to send the request to the
    server:

```java
RestClient restClient = new RestClient(r2Client, "http://localhost:8080/");
ResponseFuture<Fortune> getFuture = restClient.sendRequest(getRequest);
Response<Fortune> response = getFuture.getResponse();
```

`RestClient.sendRequest()` returns a `Future`, which can be used to wait
on and retrieve the response from the server. Note that the response is
type-safe, and parametrized as type Fortune, so we can use the `Fortune`
interface to retrieve the results, like
    this:

```java
String message = response.getEntity().getFortune();
long id = response.getEntity().getId();
```

Here is a completed `RestLiFortunesClient` class, which uses the R2
library to create the transport mechanisms. For this example, the client
will just generate a random ID between 0 and 5, and print the response.
This file should go under `client/src/main/java/` directory with the
appropriate java package
structure.

##### file: example-standalone-app/client/src/main/java/com/example/fortune/RestLiFortunesClient.java

```java
package com.example.fortune;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestClient;
import com.example.fortune.FortunesRequestBuilders;
import java.util.Collections;

public class RestLiFortunesClient {
  /**
   * This stand-alone app demos the client-side Rest.li API.
   * To see the demo, run the server, then start the client
   */
  public static void main(String[] args) throws Exception {

    // Create an HttpClient and wrap it in an abstraction layer
    final HttpClientFactory http = new HttpClientFactory();
    final Client r2Client = new TransportClientAdapter(
        http.getClient(Collections.<String, String>emptyMap()));

    // Create a RestClient to talk to localhost:8080
    RestClient restClient = new RestClient(r2Client, "http://localhost:8080/");

    // Generate a random ID for a fortune cookie, in the range 0 - 5
    long fortuneId = (long) (Math.random() * 5);

    // Construct a request for the specified fortune
    FortunesGetRequestBuilder getBuilder = fortuneBuilders.get();
    Request<Fortune> getRequest = getBuilder.id(fortuneId).build();

    // Send the request and wait for a response
    final ResponseFuture<Fortune> getFuture = restClient.sendRequest(getRequest);
    final Response<Fortune> response = getFuture.getResponse();

    // Print the response
    System.out.println(response.getEntity().getFortune());

    // Shutdown
    restClient.shutdown(new FutureCallback<None>());
    http.shutdown(new FutureCallback<None>());
  }

  private static final FortunesRequestBuilders fortuneBuilders = new FortunesRequestBuilders();
}
```

### Step 3. Build and Run the Client

With your client code in place, your directory structure should look
like this:

    example-standalone-app/
    +- build.gradle
    +- settings.gradle
    +- api/
    |  ...
    +- client/
    |  +- build.gradle
    |  +- src/
    |     +- main/
    |        +- java/
    |           +- com/
    |              +- example/
    |                 +- fortune/
    |                      +- RestLiFortunesClient.java
    +- server/
       ...
    

Build the client by building in the client directory:

    $ gradle build

To test our final client/server pair, start the server in one terminal
window:

    $ gradle startFortunesServer

Then in another window, run:

    $ gradle startFortunesClient

You should see a “fortune cookie” printed out from the client before it
exits.

If you want to inspect the request being sent by the client, stop the
server, and run `netcat` or a similar packet sniffer tool to listen on
port 8080, and then run the client:

    $ netcat -l -p 8080
    
    GET /fortunes/1 HTTP/1.1
    Host: localhost:8080
    X-LI-R2-W-MsgType: REST
    Content-Length: 0
    

## Recap

We’ve now completed a quick tour of a few of the most basic features of
Rest.li. Let’s review the steps we took to create a server and a
corresponding client:

1.  Define a data model (`Fortune.pdl`)
2.  Generate Java language bindings (`Fortune.java RecordTemplate class`)
3.  Create a Resource that responds to REST requests
    (`FortuneResource.java`) by subclassing `CollectionResourceTemplate`
    and using `RestLiAnnotations` to define operations and entry points
4.  Create a server that locates our Resource classes and uses Netty to
    dispatch requests
5.  Generate IDL (`fortune.restpec.json`) and Java client request builders
    from the server Resource file (`FortunesRequestBuilders.java` and
    `FortunesGetRequestBuilder.java`)
6.  Create a client that uses the `RestClient` to send requests
    constructed by calling the builder classes
    (`RestLiFortuneClient.java`)

Notice that (ignoring Gradle build files) there are only three files in
this example that you had to create:

  - The original Pegasus Data Model file (`Fortune.pdl`)
  - The server resource file (`FortunesResource.java`)
  - The client (`RestLiFortuneClient.java`)

Although Rest.li has many more features that can be leveraged when
creating the server and client, most of your focus will usually be on
defining data models and implementing resource classes that provide
and/or manipulate the data.

To learn more about Rest.li, proceed to the more complex examples in the source code and read the [Rest.li User’s Guide](/rest.li/user_guide/server_architecture).
