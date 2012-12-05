What is Pegasus?
================
Pegasus is a framework for building robust, scalable service architectures
using dynamic discovery and simple asychronous type-checked REST + JSON APIs.

Components
----------
Pegasus comprises the following major components:

### data/

The pegasus data layer, which provides an in-memory data
representation structurally equivalent to JSON, serialization to/from JSON
format, and a schema-definition language for specifying data format.

### generator/

The pegasus data template code generation tool, which generates
type-safe Java APIs for manipulating pegasus data objects.

### r2/

The pegasus request/response layer, which provides fully asynchronous
abstractions for transport protocols, along with implementations
for HTTP based on Netty and Jetty.

### d2/

The pegasus dynamic discovery layer, which uses Apache Zookeeper to
maintain cluster information.  D2 provides client-side software load balancing.

### restli-*/

The pegasus Rest.li framework, which provides a simple framework
for building and consuming RESTful resources.  Rest.li provides resource
patterns which streamline common CRUD use cases for collections of entities and
associations between entities.

Wiki
----
TBD - add link to github wiki here

Getting Started
---------------

### Build

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

Tasks can be executed on per module basis. For example, to only build restli-server and its dependencies,

    ./gradlew :restli-server:build

### Run the examples

Pegasus comes with a set of examples to illustrate how server and client interact. We have created Gradle tasks to run the server and client. There are 4 variants, all reside in restli-example-server and restli-example-client modules:

* Basic Example server without D2 (startExampleBasicServer)
* Example server with D2 (startExampleD2Server)
* Basic Example client without D2 (startExampleBasicClient)
* Example client with D2 (startExampleD2Client)

#### Basic
To start with, run the basic example server by

    ./gradlew startExampleBasicServer

The build will be paused after printing "Basic example server running on port 7279. Press any key to stop server." until you hit return. To quickly verify, use [cURL](http://curl.haxx.se/) as

    curl http://localhost:7279/photos/1

You should see a JSON object with some "photo" information. To run the client,

    ./gradlew startExampleBasicClient

The client will make variety of requests to the server, print informative messages and then shutdown. Each time the result may be slightly different.

#### D2

To use the D2 variants, you need [ZooKeeper](http://zookeeper.apache.org/) 3.3.4 and upward to be downloaded and running on port 2121. Before starting the server, some D2 related data need to be initialized in ZooKeeper:

    ./gradlew exampleConfigDiscovery

The D2 example server and client are started by

    ./gradlew startExampleD2Server  
    ./gradlew startExampleD2Client

The client should successfully retrieve some "album" information from server and intentionally make bad request to retrieve non-existent photo, followed by a stack trace.

#### API

Wonder how the "photo" and "album" objects are defined? Check the restli-example-api module. The .pdsc files define the object schema exchanged between server and client, while the .restspec.json files define the methods and capabilities the server supports. For more information about these files, check the wiki.