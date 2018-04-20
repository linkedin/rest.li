---
layout: get_started
title: Get Started
permalink: /get_started/quick_start
index: 1
---

# Quick start guide

## Contents

Follow the steps below to try Pegasus quickly and get a basic idea of how it works:

* [Build](https://github.com/linkedin/rest.li/wiki/Quick-Start-Guide#build)
* [Run the Examples](https://github.com/linkedin/rest.li/wiki/Quick-Start-Guide#run-the-examples)
* [API](https://github.com/linkedin/rest.li/wiki/Quick-Start-Guide#api)
* [Code Generator](https://github.com/linkedin/rest.li/wiki/Quick-Start-Guide#code-generator)

## Build

Pegasus uses [Gradle](http://www.gradle.org/) as the build system. The following points summarize some basic tasks you can do:

1. Build (implies test)

        ./gradlew build

2. Test

        ./gradlew test

3. Clean

        ./gradlew clean

4. Generate and clean [IntelliJ IDEA](http://www.jetbrains.com/idea/) project stub.

        ./gradlew idea
        ./gradlew cleanIdea

5. Generate and clean [Eclipse](http://www.eclipse.org/) project stub.

        ./gradlew eclipse
        ./gradlew cleanEclipse

Tasks can be executed on a per-module basis. For example, do this to only build restli-server and its dependencies:

    ./gradlew :restli-server:build

## Run the Examples

Pegasus comes with a set of examples to illustrate how server and client interact. We have created Gradle tasks to run the server and client. There are 4 variants; all of them reside in restli-example-server and restli-example-client modules:

* Basic example server without D2 (startExampleBasicServer)
* Example server with D2 (startExampleD2Server)
* Basic example client without D2 (startExampleBasicClient)
* Example client with D2 (startExampleD2Client)

### Basic
To start with, run the basic example server by doing this:

    ./gradlew startExampleBasicServer

The build will be paused after printing "Basic example server running on port 7279. Press any key to stop server." until you hit return. To quickly verify, use [cURL](http://curl.haxx.se/) as:

    curl http://localhost:7279/photos/1

You should see a JSON object with some "photo" information. Do this to run the client:

    ./gradlew startExampleBasicClient

The client will make variety of requests to the server, print informative messages and then shutdown. Each time the result may be slightly different.

### D2

To use the D2 variants, you need [ZooKeeper](http://zookeeper.apache.org/) 3.3.4 and upward to be downloaded and running on port 2121. Before starting the server, some D2 related data need to be initialized in ZooKeeper:

    ./gradlew exampleConfigDiscovery

The D2 example server and client are started by:

    ./gradlew startExampleD2Server  
    ./gradlew startExampleD2Client

The client should successfully retrieve some "album" information from server and intentionally make bad request to retrieve non-existent photo, followed by a stack trace.

## API

Throughout the examples, we can frequently see "photo" and "album" object. These data schemas are defined in the restli-example-api module. API module are the interface modules with contents shared by or exchanged between the server and client. Generally speaking, we usually put 3 kinds of files in API:

  - pdsc files: these files define the data schemas such as "photo" and "album" above. The syntax of pdsc resembles [Apache Avro](http://avro.apache.org/). Take a look at [Photo.pdsc](restli-example-api/src/main/pegasus/com/linkedin/restli/example/Photo.pdsc) and the comments inside could be useful. For more information, check [DATA](DATA-Data-Schema-and-Templates).
  - restspc.json: these files are Rest.li [IDL](http://en.wikipedia.org/wiki/Interface_description_language) that defines the interface and protocol a Rest.li resource exposes. You can find the "photo" resource idl at [com.linkedin.restli.example.photos.photos.restspec.json](restli-example-api/src/main/idl/com.linkedin.restli.example.photos.photos.restspec.json). For more information, check [Rest.li User Guide](Rest.li-User-Guide).
  - Common Java classes shared by server and client.

## Code Generator

Pegasus comes with many code generators:

  - Schema(pdsc) binding generator. Java classes can be generated from all pdsc files. These generated classes come with methods and fields to interact with the underlying data object and provide native Java interop interface.
  - restspec.json generator. While pdsc files are usually handwritten, restspec.json files are generated from the resource class using `com.linkedin.restli.tools.idlgen.RestLiResourceModelExporterCmdLineApp` class.
  - Builder generator. Java classes can also be generated from all .restspec.json files. These [builder](http://en.wikipedia.org/wiki/Builder_pattern) classes provide convenient method to construct Rest.li request with various parameters.

You can find example Gradle scripts of how to call the generators in the "build_script" directory.
