Rest.li is a Java framework that allows you to easily create clients and
servers that use a REST style of communication. Rest.li is based on an
inversion-of-control model. The framework handles most of the data flow
and client/server interaction transparently and calls code you supply at
the appropriate time.

This document describes how to use Rest.li to build RESTful clients and
servers. The first section introduces key architectural elements and
provides an overview of the development process. The remainder of the
document serves as a detailed reference to Rest.li features. It is not
necessary to read this entire document before using Rest.li. Once you
understand the basic principles, you can refer to specific sections in
this guide when you have questions. If you just want to get started
exploring a simple sample implementation, go to [Quickstart Guide - a
step-by-step tutorial on the
basics](./Quickstart:-A-Tutorial-Introduction-to-Rest.li).

## Contents

  - [Rest.li Client/Server
    Architecture](#restli-clientserver-architecture)
      - [Introduction](#introduction)
      - [Asynchronous APIs](#asynchronous-apis)
      - [Server Data Flow](#server-data-flow)
      - [Client Data Flow](#client-data-flow)
      - [Development Flow](#development-flow)
  - [Rest.li Server](#restli-server)
      - [Runtimes](#runtimes)
      - [R2 Filter Configuration](#r2-filter-configuration)
      - [Defining Data Models](#defining-data-models)
      - [Writing Resources](#writing-resources)
      - [Documenting Resources](#documenting-resources)
      - [Resource Annotations](#resource-annotations)
      - [Sub-Resources](#sub-resources)
      - [Resource Methods](#resource-methods)
      - [ResourceContext](#resourcecontext)
      - [Resource Templates](#resource-templates)
      - [Free-Form Resources](#free-form-resources)
      - [Returning Errors](#returning-errors)
      - [Field Projection](#field-projection)
      - [Collection Pagination](#collection-pagination)
      - [Dependency Injection](#dependency-injection)
      - [Asynchronous Resources](#asynchronous-resources)
      - [Online Documentation](#online-documentation)
  - [Rest.li Client Framework](#restli-client-framework)
      - [Introduction](#introduction)
      - [Depending on a Service’s Client
        Bindings](#depending-on-a-services-client-bindings)
      - [Depending on Data Templates](#depending-on-data-templates)
      - [Type-Safe Builders](#type-safe-builders)
      - [Built-in Request and RequestBuilder
        classes](#built-in-request-and-requestbuilder-classes)
      - [Restspec IDL](#restspec-idl)
      - [RestClient](#restclient)
      - [Request Options](#request-options)
      - [ParSeq Integrated Rest Client](#parseq-integrated-rest-client)
      - [Client Code Generator Tool](#client-code-generator-tool)
      - [Rest.li-extras](#restli-extras)

<a id="wiki-ArchitectureOverview"></a>

## Rest.li Client/Server Architecture

  - [Introduction](#introduction)
  - [Asynchronous APIs](#asynchronous-apis)
  - [Server Data Flow](#server-data-flow)
  - [Client Data Flow](#client-data-flow)
  - [Development Flow](#development-flow)

### Introduction

Rest.li allows you to build and access RESTful servers and clients,
without worrying too much about the details of HTTP or JSON. You simply
define a *data model* (using an schema definition language) and
*resources* (Java classes that supply or act on the appropriate data in
response to HTTP requests), and Rest.li takes care of everything else.
In this section, we’ll describe the flow of control and data between a
Rest.li server and client. We’ll also look briefly at the development
process, so you understand what tasks you need to do to develop Rest.li
clients and servers, including what Rest.li does for you automatically.

The Rest.li server framework consists of libraries that provide
annotations and helper classes for describing your resources, as well as
an inversion-of-control dispatcher that handles incoming requests and
automatically invokes the appropriate methods in your resources.

The following diagram provides a high-level view of the interaction and
data flow between a Rest.li client and server. The yellow arrows
indicate the flow of requests out of the client and into the server,
while dark blue arrows represent the server’s response. You as a
developer implement the Resource classes in the server. Rest.li provides
the platform code and infrastructure for dispatching and handling
requests. It also generates the Record Templates and RequestBuilder
classes:

<center>

<b>Data and Control Flow Between a Rest.li Server and
Client</b><br><img src=RestLiClientServerFlow.png>

</center>

<a id="wiki-ServerDataFlow"></a>

### Asynchronous APIs

Rest.li is built on simple asynchronous APIs. These APIs allow both
servers to run in non-blocking event based frameworks and allow client
code to be written to make non-blocking calls. This approach has a
couple major benefits. On the server, it means that our servers can be
leaner and scale to high request throughput because we don’t need large,
per request, thread pools. On the client, it makes it easy to stitch
together multiple requests to servers in sophisticated flows where
independent calls can be made in parallel.

Rest.li’s client implementation is Netty-based and is designed to work
seamlessly with [ParSeq](https://github.com/linkedin/parseq) to
construct complex asynchronous request flows.

There are several server
    implementations:

  - [Servlet](Rest.li-with-Servlet-Containers)
    — Battle tested and ready for production use. Containers supporting
    [Servlet 3.0
    API](http://download.oracle.com/otndocs/jcp/servlet-3.0-fr-eval-oth-JSpec/)
    are required to benefit from asynchronous, non-blocking request
    processing. Jetty 8.x supports Servlet 3.0 and has been used in
    large production environments.
  - [Netty](Rest.li-with-Netty)
    — Experimental
  - Embedded Jetty — Primarily for integration testing as it’s trivial
    to spin up as part of a test suite

See [Asynchronous
Resources](Rest.li-User-Guide#asynchronous-resources)
for more details on how to handle requests using non-blocking request
processing.

The remainder of this guide will assume use of the servlet server
implementation.

### Server Data Flow

Starting with the server (on the right in the diagram above), the
following steps occur when a request is submitted to a Rest.li server:

\# The R2 transport layer receives a request (HTTP + JSON) and sends it
on to Rest.li. (R2 is a separate library that provides HTTP transport
services. It is independent of Rest.li but is included with the Rest.li
code base. It’s designed to work well with Rest.li.)  
\# Rest.li’s routing logic inspects the request’s URI path and
determines which target *resource* (a Java class) the server has defined
to handle that request.  
\# Rest.li parses the request to extract any parameters.  
\# Rest.li creates a new instance of the resource class designated to
handle the request.  
\# Rest.li invokes the appropriate methods of the resource object,
passing in any necessary Java parameters.  
\# The resource object instantiates and returns a response, in the form
of a RecordTemplate object.  
\# Rest.li serializes the response object and passes it back to the
requesting client through the R2 transport layer.

We’ll look at what you, as a developer, need to do to support this data
flow shortly. Although, you probably noticed that Rest.li does most of
the work. The primary task of the developer is to define the data model
to be supported by your server and implement the *resource* classes that
can produce that data. Rest.li handles the details of routing requests,
instantiating resource classes, and invoking methods on objects at the
right time.

When writing resource classes, it is important to understand that
Rest.li constructs a new instance of the appropriate resource class to
handle each request. This means that resource objects cannot store state
across multiple requests. Any long-lived resources should be managed
separately (see [Dependency
Injection](/linkedin/rest.li/wiki/Rest.li-User-Guide/#dependency-injection)
below).

<a id="wiki-ClientDataFlow"></a>

### Client Data Flow

Rest.li also provides support for writing clients. Clients issue
requests by instantiating a RequestBuilder object that supports methods
that allow details of the request to be specified. The RequestBuilder
object generates a Request object that can be passed to Rest.li and sent
to the server using the R2 transport layer. When the server responds (as
detailed above), the client receives the request using the R2 transport,
and Rest.li produces a RecordTemplate object (matching the object
instantiated by the server) and provides the object to the client.

Both client and server work with the same Java representations of the
server’s data model. Note that you do not need to use a Rest.li based
client to communicate with a Rest.li server. However, Rest.li supports
type-safe data exchange using Java interfaces when using Rest.li for
both client and server.

<a id="wiki-DevelopmentFlow"></a>

### Development Flow

Next, let’s briefly look at the basic development flow required to
implement a client and server to support the data flow described in the
previous section. Your tasks as a developer are basically to define your
data model using a simple modeling language and to implement Java
classes that act on or produce that data. Rest.li supports these tasks
with a combination of base classes and code generation.

The following diagram illustrates the major steps in building servers
and clients based on the Rest.li framework. The numbers in the diagram
correspond to the sequence in which tasks are done. Blue boxes represent
classes you will write, while green boxes represent components that are
created by Rest.li’s code generators. Black arrows indicate a code
generation process; red dashed lines indicate the use of classes that
allow a server and clients to exchange data.

<center>

<b>Rest.li Development
Flow</b><br><img src=/images/RestLiCodeGen.png>

</center>

Let’s look at each step:

  - **Step 1**. The first step in building a Rest.li application is to
    define your data schema using [Pegasus Data
    Schemas](/linkedin/rest.li/wiki/DATA-Data-Schema-and-Templates). The
    Pegasus Data Schema format uses a simple Avro-like syntax.
  - In **Step 2**, a Rest.li code generator creates Java classes that
    represent the data model defined in Step 1. These RecordTemplate
    classes serve as the Java representation of the data in both the
    server and client.
  - **Step 3** is to implement the server Resource classes and define
    the operations they support. Rest.li provides a set of annotations
    and base classes that allow you to map Resource classes to REST
    endpoints and to specify methods of your Resource classes to respond
    to REST operations, such as GET or PUT. Your Resource classes are
    expected to return data using instances of the RecordTemplate
    classes generated in Step 2.
  - In **Step 4**, Rest.li generates an interface description (IDL) file
    that provides a simple, textual, machine-readable specification of
    the server resources implemented in Step 3. The IDL is considered
    the source of truth for the interface contract between the server
    and its clients. The IDL itself is a language-agnostic JSON format.
    Rest.li uses this IDL along with the original data schema files to
    support automatically generating human-readable documentation, which
    can be requested from a server. See [IDL
    Compatibility](/linkedin/rest.li/wiki/Gradle-build-integration#compatibility)
    for build details and how run the IDL check in “backwards” and
    “ignore” modes.
  - **Step 5** is to create your server application, which involves
    leveraging a few Rest.li classes to instantiate the Rest.li server,
    set up the transport layer, and supply Rest.li with the location
    (class path) of your Resource classes.
  - In **Step 6**, Rest.li generates classes known as RequestBuilders
    that correspond to the server resource classes. These
    RequestBuilders are used by clients to create requests to the
    server. Together with the RecordTemplate and Resource classes,
    RequestBuilders provide convenient and type-safe mechanisms for
    working with the data models supported by the server.
  - Finally, **Step 7** is to implement one or more clients. Clients
    issue requests by instantiating the RequestBuilder classes generated
    in Step 6. These RequestBuilders produce Requests that are passed to
    Rest.li to issue requests to a server.

The [Quickstart
Guide](/linkedin/rest.li/wiki/Quickstart:-A-Tutorial-Introduction-to-Rest.li)
provides a step-by-step walk through of this development process and
demonstrates the nuts and bolts, including build scripts and other
infrastructure required to execute these steps.

<a id="wiki-RestliServer"></a>

## Rest.li Server

This section describes Rest.li support for implementing servers:

  - [Runtimes](#runtimes)
  - [R2 Filter Configuration](#r2-filter-configuration)
  - [Defining Data Models](#defining-data-models)
  - [Writing Resources](#writing-resources)
  - [Documenting Resources](#documenting-resources)
  - [Resource Annotations](#resource-annotations)
  - [Sub-Resources](#sub-resources)
  - [Resource Methods](#resource-methods)
  - [ResourceContext](#resourcecontext)
  - [Resource Templates](#resource-templates)
  - [Free-form Resources](#free-form-resources)
  - [Returning Errors](#returning-errors)
  - [Field Projection](#field-projection)
  - [Collection Pagination](#collection-pagination)
  - [Dependency Injection](#dependency-injection)
  - [Asynchronous Resources](#asynchronous-resources)
  - [Online Documentation](#online-documentation)

### Runtimes

Rest.li supports the following runtimes:

1.  [Servlet
    containers](Rest.li-with-Servlet-Containers)
    (for example, Jetty)
2.  [Netty](Rest.li-with-Netty)

### R2 Filter Configuration

Rest.li servers can be configured with different R2 filters, according
to your use case. How the filters are configured depends on which
dependency injection framework (if any) you are using. For example, take
a look at
<a href="Compression">the
compression wiki page</a> to see how we can configure a server for
compression. Another example is to add a
<code>SimpleLoggingFilter</code> with Spring, which requires you to do
the following (full file
<a href="https://github.com/linkedin/rest.li/blob/master/examples/spring-server/server/src/main/webapp/WEB-INF/beans.xml">here</a>):

\`\`\`xml

<!-- Example of how to add filters,  here we'll enable logging and snappy compression support -->

<bean id="loggingFilter" class="com.linkedin.r2.filter.logging.SimpleLoggingFilter" />  
\`\`\`

[Other R2
filters](List-of-R2-filters)
can also be configured in a similar way.

<a id="wiki-DefiningDataModels"></a>

### Defining Data Models

The first step in building a Rest.li application is to define your data
schema using [Pegasus Data
Schemas](/linkedin/rest.li/wiki/DATA-Data-Schema-and-Templates). The
Pegasus Data Schema format uses a simple Avro-like syntax to define your
data model in a language-independent way. Rest.li provides code
generators to create Java classes that implement your data model. See
[Pegasus Data
Schemas](/linkedin/rest.li/wiki/DATA-Data-Schema-and-Templates) for full
details.

<a id="wiki-WritingResources"></a>

### Writing Resources

After you have defined your data models, the principle programming task
when implementing a Rest.li server is to create resource classes. In
Rest.li, resource classes define the RESTful endpoints your server
provides. You create a resource class by adding a class level annotation
and by implementing or extending a Rest.li interface or base class
corresponding to the annotation. The annotations help describe the
mapping from your Java code to the REST interface protocol. When
possible, the framework uses conventions to help minimize the
annotations you need to write.

Steps to define a resource class:

  - The class must have the default constructor. The default constructor
    will be used by Rest.li to instantiate the resource class for each
    request execution.
  - The class must be annotated with one of the Resource Annotations.
  - If required by the annotation, the class must `implement` the
    necessary Resource interface or extend one of the convenience base
    classes that implements the interface.
  - To expose methods on the resource, each method must either:
      - Override a standard method from the Resource interface
      - Include the necessary method-level annotation as described in
        the Resource Methods section below
  - For each exposed method, each parameter must either:
      - Be part of the standard signature, for overridden methods
      - Be annotated with one of the parameter-level annotations
        described for the Resource Method.
  - All documentation is written in the resource source file using
    javadoc (or scaladoc, see below for details).

Here is a simple example of a Resource class. It extends a convenience
base class, uses an annotation to define a REST end-point (“fortunes”),
and provides a GET endpoint by overriding the standard signature of the
`get()` method of the base class:

\`\`\`java  
/****  
\* A collection of fortunes, keyed by random number.  
\*/  
`RestLiCollection(name = "fortunes", namespace = "com.example.fortune")
public class FortunesResource extends CollectionResourceTemplate<Long,
Fortune>
{
/**
* Gets a fortune for a random number.
*/
`Override  
public Fortune get(Long key)  
{  
// retrieve data and return a Fortune object …  
}  
}  
\`\`\`

This interface implements an HTTP GET:

    <code>
    > GET /fortunes/1
    ...
    < { "fortune": "Your lucky color is purple" }
    </code>

Note that Rest.li does not automatically use the names of your Java
identifiers. Class names, method names, and parameter names have no
direct bearing on the interface your resource exposes through
annotations.

The above example supports the GET operation by overriding the
`CollectionResourceTemplate`, and you can also choose to support other
operations by overriding other methods. However, you can also define any
method of your class as handling operations by using Resource
Annotations, described in detail in the next section.

<a id="wiki-DocumentingResources"></a>

### Documenting Resources

Rest.li resources are documented in the resource source files using
javadoc. When writing resources, developers simply add any documentation
as javadoc to their java resource classes, methods, and method params.
It is recommended that developers follow the [javadoc style
guidelines](http://www.oracle.com/technetwork/java/javase/documentation/index-137868.html)
for all formatting so that their documentation is displayed correctly.

Rest.li will automatically extract this javadoc and include it in all
generated “interface definitions” (.restspec.json files) and generated
client bindings. This approach allows REST API clients and tools to
easily gain access to the documentation. For example, [Rest.li API
Hub](https://github.com/linkedin/rest.li-api-hub) is an opensource web
UI that displays REST API documentation, including all javadoc, for
Rest.li APIs.

Scaladoc is also supported. See [Scala
Integration](Scala-Integration)
for details.

<a id="wiki-ResourceAnnotations"></a>

### Resource Annotations

Resource annotations are used to mark and register a class as providing
as Rest.li resource. One of a number of annotations may be used,
depending on the [Interface
Pattern](Modeling-Resources-with-Rest.li)
the resource is intended to implement. Briefly, here are the
options:

<a id="wiki-ResourceTypes"></a>

| First Header  | Second Header |
| ------------- | ------------- |
| Content Cell  | Content Cell  |
| Content Cell  | Content Cell  |



| Resource Type | Annotation | Interface or Base Class |
| ------------- | -----------| ------------------------|
| Collection    | @RestLiCollection     | For simple keys, implement `CollectionResource` or extend `CollectionResourceTemplate`. For complex key implement `ComplexKeyResource`, extend `ComplexKeyResourceTemplate`, or implement `KeyValueResource` for use cases requiring extensive customization |
| Simple        | @RestLiSimpleResource | Implement `SimpleResource`, extend `SimpleResourceTemplate` or implement `SingleObjectResource` for use cases requiring extensive customization |
| Association   | @RestLiAssociation    | Implement `AssociationResource`, extend `AssociationResourceTemplate`, or implement `KeyValueResource` for use cases requiring extensive customization  |
| Actions       | @RestLiActions        | N/A |

##### @RestLiCollection

The @`RestLiCollection` annotation is applied to classes to mark them as
providing a Rest.li collection resource. Collection resources model a
collection of entities, where each entity is referenced by a key. See
[Collection Resource
Pattern](Modeling-Resources-with-Rest.li#wiki-Collection)
for more details.

The supported annotation parameters are:

  - `name` - required, defines the name of the resource.
  - `namespace` - optional, defines the namespace for the resource.
    Default is empty (root namespace). The namespace of the resource
    appears in the IDL, and is used as the package name for the
    generated client builders.
  - `keyName` - optional, defines the key name for the resource. Default
    is “\<ResourceName\&gt;Id”.
  - `parent` - optional, defines the parent resource for this resource.
    Default is root.

Classes annotated with @`RestLiCollection` must implement the
`CollectionResource` interface. The `CollectionResource` interface
requires two generic type parameters:

  - `K` - the key type for the resource.
  - `V` - the value type for the resource (also known as, the entity
    type).

The key type for a collection resource must be one of:

  - `String`
  - `Boolean`
  - `Integer`
  - `Long`
  - A Pegasus Enum (any enum defined in a `.pdsc` schema)
  - Custom Type (see below for details)
  - Complex Key (A pegasus record, any subclass of `RecordTemplate`
    generated from a `.pdsc` schema)

The value type for a collection resource must be a pegasus record, any
subclass of `RecordTemplate` generated from a `.pdsc` schema.

For convenience, Collection resources may extend
`CollectionResourceTemplate` rather than directly implementing the
`CollectionResource` interface.

For example:

\`\`\`java  
@RestLiCollection(name = “fortunes”, namespace = “com.example.fortune”,
keyName = “fortuneId”)  
public class FortunesResource extends CollectionResourceTemplate\<Long,
Fortune\>  
{  
…  
}  
\`\`\`

### Sub-Resources

Sub-resources may be defined by setting the “parent” field on
<code>@RestLiCollection</code> to the class of the parent resource of
the sub-resource.

For example, a sub-resource of the fortunes resource would have a URI
path of the form:

\`\`\`  
/fortunes/{fortuneId}/subresource  
\`\`\`

Parent resource keys can be accessed by sub-resources, as shown in the
following example:

\`\`\`java  
`RestLiCollection(name = "subresource", namespace =
"com.example.fortune", parent = FortunesResource.class)
public class SubResource extends CollectionResourceTemplate<Long,
SubResourceEntity>
{
`RestMethod.Get  
public Greeting get(Long key, @Keys PathKeys keys) {  
Long parentId = keys.getAsLong(“fortuneId”);  
…  
}  
…  
}  
\`\`\`

Alternatively, if not using free form methods, the path key can
retrieved from the resource context. This approach may be deprecated in
future versions in favor of <code>@Keys</code>.

\`\`\`java  
public SubResourceEntity get(Long subresourceKey)  
{  
Long parentId = getContext().getPathKeys().getAsLong(“fortuneId”);  
…  
}

\`\`\`

For details on how to make requests to sub-resources from a client, see
<a href="#wiki-calling-sub-resources">Calling Sub-resources</a>

##### @RestLiCollection with Complex Key

Classes implementing `ComplexKeyResource` can use a record type as key.
This allows for arbitrary complex hierarchical structures to be used to
key a collection resource, unlike CollectionResources, which only
support primitive type keys (or typerefs to primitive types).
`ComplexKeyResourceTemplate` is a convenient base class to extend when
implementing a `ComplexKeyResource`.

The full interface is:  
\`\`\`java  
public interface ComplexKeyResource\<K extends RecordTemplate, P extends
RecordTemplate, V extends RecordTemplate\> …  
\`\`\`

A complex key consists of a `Key` and `Parameter` part. The `Key` should
uniquely identify the entities of the collection while the parameters
may optionally be added to allow additional information that is not used
to lookup an entity, such as a version tag for concurrency control.

Since the parameters are often not needed, an `EmptyRecord` may be used
in the generic signature of a <code>ComplexKeyResource</code> to
indicate that no “Parameters” are used to key the collection.

Example:

\`\`\`java  
@RestLiCollection(name = “widgets”, namespace = “com.example.widgets”)  
public class WidgetResource implements extends
ComplexKeyResourceTemplate\<WidgetKey, EmptyRecord, Widget\>  
{  
public Widget get(ComplexResourceKey\<WidgetKey, EmptyRecord\> ck)  
{  
WidgetKey key = ck.getKey();  
int number = key.getNumber();  
String make = key.getThing().getMake();  
String model = key.getThing().getModel();  
return lookupWidget(number, make, model);  
}  
}  
\`\`\`

To use <code>EmptyRecord</code>, <code>restli-common</code> must be in
the <code>dataModel</code> dependencies for the api project where client
bindings are generated, as shown in the following example:

api/build.gradle:  
\`\`\`groovy  
dependencies {  
…  
dataModel spec.product.pegasus.restliCommon  
}  
\`\`\`

Where <code>WidgetKey.pdsc</code> is defined by the schema:

    <code>
    {
      "type": "record",
      "name": "WidgetKey",
      "namespace": "com.example.widget",
      "fields": [
        {"name": "number", "type": "string"},
        {
          "name": "thing", "type": {
            "type": "record",
            "name": "Thing",
            "fields": [
               {"name": "make", "type": "string"},
               {"name": "model", "type": "string"}
            ]
          }
        }
      ]
    }
    </code>

Example request:

    <code>
    curl "http://<hostname:port>/widgets/number=1&thing.make=adruino&thing.model=uno
    </code>

If params are added, they are represented in the url under the “$params”
prefix like this:

    <code>
    curl "http://<hostname:port>/widgets/number=1&thing.make=adruino&thing.model=uno&$params.version=1
    </code>

The implementation of complex key collection is identical to the regular
`RestLiCollection` with the exception that it extends
`ComplexKeyResourceTemplate` (or directly implements
`ComplexKeyResource`) and takes three ﻿type parameters instead of two:
key type, key parameter type, and value type — each extending
@RecordTemplate.

For details on how a complex key is represented in a request URL see
[Rest.li Protocol: Complex
Types](/linkedin/rest.li/wiki/Rest.li-Protocol#complex-types)

##### @RestLiSimpleResource

The @`RestLiSimpleResource` annotation is applied to classes to mark
them as providing a Rest.li simple resource. Simple resources model an
entity which is a singleton in a particular scope. See the description
of the [Simple Resource
Pattern](Modeling-Resources-with-Rest.li#wiki-Simple)
for more details.

The supported annotation parameters are:

  - `name` - required, defines the name of the resource.
  - `namespace` - optional, defines the namespace for the resource.
    Default is empty (root namespace). The namespace of the resource
    appears in the IDL, and is used as the package name for the
    generated client builders.
  - `parent` - optional, defines the parent resource for this resource.
    Default is root.

Classes annotated with @`RestLiSimpleResource` must implement the
`SimpleResource` interface. The `SimpleResource` interface requires a
generic type parameter `V`, which is the value type for the resource
(also known as, the entity type). The value type for a simple resource
must be a pegasus record, any subclass of `RecordTemplate` generated
from a `.pdsc` schema.

For convenience, simple resources may extend `SimpleResourceTemplate`
rather than directly implementing the `SimpleResource` interface.

Examples:

\`\`\`java  
@RestLiSimpleResource(name = “todaysPromotedProduct”, namespace =
“com.example.product”)  
public class TodaysPromotedProductResource extends
SimpleResourceTemplate<Product>  
{  
…  
}  
\`\`\`

##### @RestLiAssociation

The @`RestLiAssociation` annotation is applied to classes to mark them
as providing a Rest.li association resource. Association resources model
a collection of relationships between entities. Each relationship is
referenced by the keys of the entities it relates and may define
attributes on the relation itself. See [Association Resource
Pattern](Modeling-Resources-with-Rest.li#wiki-Association)
for more details.

For Example:

\`\`\`java  
`RestLiAssociation(name = "memberships", namespace = "com.example",
assocKeys = {
`Key(name = “memberId”, type = Long.class),  
`Key(name = "groupId", type = Long.class)
}
)
public class MembershipsAssociation extends
AssociationResourceTemplate<Membership>
{
`Override  
public Membership get(CompoundKey key)  
{  
return lookup(key.getPartAsLong(“memberId”,
key.getPartAsLong(“groupId”));  
}  
}  
\`\`\`

    <code>
    curl http://<hostname:port>/memberships/memberId=1&groupId=10
    </code>

The supported annotation parameters are:

  - `name` - required, defines the name of the resource.
  - `namespace` - optional, defines the namespace for the resource.
    Default is empty (root namespace). The namespace of the resource
    appears in the IDL, and is used as the package name for the
    generated client builders.
  - `parent` - optional, defines the parent resource for this resource.
    Default is root.
  - `assocKeys` - required, defines the list of keys for the association
    resource. Each key must declare its name and type.

Classes annotated with @`RestLiAssociation` must implement the
`AssociationResource` interface. The `AssociationResource` interface
requires a single generic type parameter:

  - `V`, which is the value type for the resource, a.k.a., the entity
    type.

The value type for an association resource must be a subclass of
`RecordTemplate` generated from a `.pdsc` schema.

Note that for association resources, they key type is always
`CompoundKey`, with key parts as defined in the `assocKeys` parameter of
the class’ annotation.

For convenience, Association resources may extend
`AssociationResourceTemplate` rather than directly implementing the
`AssociationResource` interface.

##### @RestLiActions

The @`RestLiActions` annotation is applied to classes to mark them as
providing a Rest.li action set resource. Action set resources do not
model any resource pattern. They simply group together a set of custom
actions.

For example:

\`\`\`java  
@RestLiActions(name = “simpleActions”,  
namespace = “com.example”)  
public class SimpleActionsResource {

`Action(name="echo")
public String echo(`ActionParam(“input”) String input)  
{  
return input;  
}  
}  
\`\`\`

The supported annotation parameters are:

  - `name` - required, defines the name of the resource.
  - `namespace` - optional, defines the namespace for the resource.
    Default is empty (root namespace).

Action set resources do not have a key or value type, and do not need to
`implement` any framework interfaces.

<a id="wiki-ResourceMethods"></a>

### Resource Methods

Resource methods are operations a resource can perform. Rest.li defines
a standard set of resource methods, each with its own interface pattern
and intended semantics.

The set of possible resource methods is constrained by the resource
type, as described in the table below:

<table>
<thead>
<tr class="header">
<th>Resource Type</th>
<th>Collection</th>
<th>Simple</th>
<th>Association</th>
<th>Action Set</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td>GET</td>
<td>x</td>
<td>x</td>
<td>x</td>
<td></td>
</tr>
<tr class="even">
<td>BATCH_GET / GET_ALL</td>
<td>x</td>
<td></td>
<td>x</td>
<td></td>
</tr>
<tr class="odd">
<td>FINDER</td>
<td>x</td>
<td></td>
<td>x</td>
<td></td>
</tr>
<tr class="even">
<td>CREATE / BATCH_CREATE</td>
<td>x</td>
<td></td>
<td></td>
<td></td>
</tr>
<tr class="odd">
<td>UPDATE / PARTIAL_UPDATE</td>
<td>x</td>
<td>x</td>
<td>x</td>
<td></td>
</tr>
<tr class="even">
<td>BATCH_UPDATE /<br />
BATCH_PARTIAL_UPDATE</td>
<td>x</td>
<td></td>
<td>x</td>
<td></td>
</tr>
<tr class="odd">
<td>DELETE</td>
<td>x</td>
<td>x</td>
<td>x</td>
<td></td>
</tr>
<tr class="even">
<td>BATCH_DELETE</td>
<td>x</td>
<td></td>
<td>x</td>
<td></td>
</tr>
<tr class="odd">
<td>ACTION</td>
<td>x</td>
<td>x</td>
<td>x</td>
<td>x</td>
</tr>
</tbody>
</table>

In the section below, `K` is used to denote the resource’s key type, and
`V` is used to denote the resource’s value type. Remember that for
association resources, `K` is always `CompoundKey`.

<a id="GET"></a>

##### GET

The GET resource method is intended to retrieve a single entity
representation based upon its key or without a key from a simple
resource. GET should not have any visible side effects. For example, it
should be safe to call whenever the client wishes.

Resources providing the GET resource method must override one of the
following method signatures.

For collection and association resources:  
\`\`\`java  
public V get(K key);  
\`\`\`

For simple resources:  
\`\`\`java  
public V get();  
\`\`\`

Get methods can also be annotated if not overriding a base class method.
GET supports a method signature with a wrapper return type.

For collection and association resources:  
\`\`\`java  
@RestMethod.Get  
public GetResult<V> getWithStatus(K key);  
\`\`\`

For simple resources:  
\`\`\`java  
@RestMethod.Get  
public GetResult<V> getWithStatus();  
\`\`\`

An annotated get method may also have arbitrary query params added:

\`\`\`java  
`RestMethod.Get
public GetResult<V> get(K key, `QueryParam(“viewerId”) String
viewerId);  
\`\`\`

The return type `GetResult<V>` allows users to set an arbitrary HTTP
status code for the response. For more information about the
`RestMethod.Get` annotation, see [Free-Form
Resources](#free-form-resources).

<a id="BATCH_GET"></a>

##### BATCH\_GET

The BATCH\_GET resource method retrieves multiple entity representations
given their keys. BATCH\_GET should not have any visible side effects.
For example, it should be safe to call whenever the client wishes.
However, this is not something enforced by the framework, and it is up
to the application developer that there are no side effects.

Resources providing the BATCH\_GET resource method must override the
following method signature:  
\`\`\`java  
public Map\<K, V\> batchGet(Set<K> ids);  
\`\`\`

@`RestMethod.BatchGet` may be used to indicate a batch get method
instead of overriding the batchGet method of a base class.

Resources may also return `BatchResult`, which allows errors to be
returned along with entities that were successfully retrieved.

Example of a batch get:  
\`\`\`java  
public BatchResult\<Long, Greeting\> batchGet(Set<Long> ids)  
{  
Map\<Long, Greeting\> batch = new HashMap\<Long, Greeting\>();  
Map\<Long, RestLiServiceException\> errors = new HashMap\<Long,
RestLiServiceException\>();  
for (long id : ids)  
{  
Greeting g = \_db.get(id);  
if (g \!= null)  
{  
batch.put(id, g);  
}  
else  
{  
errors.put(id, new
RestLiServiceException(HttpStatus.S\_404\_NOT\_FOUND));  
}  
}  
return new BatchResult\<Long, Greeting\>(batch, errors);  
}  
\`\`\`

Clients should make requests to a batch resource using `buildKV()` (not
`build()`, it is deprecated), for example:

\`\`\`java  
new FortunesBuilders().batchGet().ids(…).buildKV()  
\`\`\`

<a id="GET_ALL"></a>

##### GET\_ALL

When a GET is requested on a collection or association resource with no
key provided (for example, /myResource), the GET\_ALL resource method is
invoked, if present. The GET\_ALL resource method retrieves all entities
for the collection and supports the same pagination facilities as a
finder.

\`\`\`java  
public List<V> getAll(@Context PagingContext pagingContext);  
\`\`\`

@`RestMethod.GetAll` may be used to indicate a get all method instead of
overriding the getAll method of a base class.

To directly control the total and metadata returned by a get all method,
do not override getAll, instead create a new method with the
@`RestMethod.GetAll` annotation and return a `CollectionResult` rather
than a list, for example:

\`\`\`java  
`RestMethod.GetAll
public CollectionResult<Widgets, WidgetsMetadata> getAllWidgets(`Context
PagingContext pagingContext)  
{  
// …  
return new CollectionResult\<Widgets, WidgetsMetadata\>(pageOfWidgets,
total, metadata);  
}  
\`\`\`

When returning a CollectionResult from GetAll, the behavior is identical
to a finder. See the below finder documentation for additional details
about CollectionResult.

<a id="FINDER"></a>

##### FINDER

FINDER methods model query operations. For example, they retrieve an
ordered list of 0 or more entities based on criteria specified in the
query parameters. Finder results will automatically be paginated by the
Rest.li framework. Like GET methods, FINDER methods should not have side
effects.

Resources may provide zero or more FINDER resource methods. Each finder
method must be annotated with the @`Finder` annotation.

Pagination default to start=0 and count=10. Clients may set both of
these parameters to any desired value.

The @`Finder` annotation takes a single required parameter, which
indicates the name of the finder method.

For example:

\`\`\`java  
/\*  
You can access this FINDER method via
/resources/order?q=findOrder\&buyerType=1\&buyerId=309\&orderId=1208210101  
\*/  
`RestLiCollection(name="order",keyName="orderId")
public class OrderResource extends
CollectionResourceTemplate<Integer,Order>
{
`Finder(“findOrder”)  
public List<Order> findOrder(`Context PagingContext context,
`QueryParam(“buyerId”) Integer buyerId,  
`QueryParam("buyerType") Integer buyerType,
`QueryParam(“orderId”) Integer orderId)  
throws InternalException  
{  
…  
}  
…  
\`\`\`

Finder methods must return either:

  - `List<V>`
  - `CollectionResult<V, MetaData>`
  - `BasicCollectionResult<V>`, a subclass of `CollectionResult`
  - a subclass of one the above

Every parameter of a finder method must be annotated with one of:

  - @`Context` - indicates that the parameter provides framework context
    to the method. Currently all @`Context` parameters must be of type
    `PagingContext`.
  - @`QueryParam` - indicates that the value of the parameter is
    obtained from a request query parameter. The value of the annotation
    indicates the name of the query parameter. Duplicate names are not
    allowed for the same finder method.
  - @`ActionParam` - similar to Query Param, but the parameter
    information will be located in the request body. Generally,
    @`QueryParam` is preferred over @`ActionParam`.
  - @`AssocKey` - indicates that the value of the parameter is a partial
    association key, obtained from the request. The value of the
    annotation indicates the name of the association key, which must
    match the name of an @`Key` provided in the `assocKeys` field of the
    @`RestLiAssociation` annotation.

Parameters marked with @`QueryParam`, @`ActionParam`, and @`AssocKey`
may also be annotated with @`Optional`, which indicates that the
parameter is not required. The @`Optional` annotation may specify a
String value, indicating the default value to be used if the parameter
is not provided in the request. If the method parameter is of primitive
type, a default value must be specified in the @`Optional` annotation.

Valid types for query parameters are:

  - `String`
  - `boolean` / `Boolean`
  - `int` / `Integer`
  - `long` / `Long`
  - `float` / `Float`
  - `double` / `Double`
  - `Enum`
  - Custom types (see the bottom of this section)
  - Record template types (any subclass of `RecordTemplate` generated
    from a `.pdsc` schema)
  - Arrays of one of the types above, e.g. `String[]`, `long[]`, …

\`\`\`java  
`Finder("simpleFinder")
public List<V> simpleFind(`Context PagingContext context);

`Finder("complexFinder")
public CollectionResult<V, MyMetaData>
complexFinder(`Context(defaultStart = 10, defaultCount = 100)  
PagingContext context,  
`AssocKey("key1") Long key,
`QueryParam(“param1”) String requiredParam,  
`QueryParam("param2") `Optional String optionalParam);  
\`\`\`

<a id="TyperefSchema"></a>

###### Typerefs (Custom Types)

Custom types can be any Java type, as long as it has a coercer and a
typeref schema, even java classes from libraries such as Date. To create
a query parameter that uses a custom type, you will need to write a
coercer and a typeref schema for the type you want to use. See the
[typeref
documentation](DATA-Data-Schema-and-Templates)
for details.

First, for the coercer you will need to write an implementation of
DirectCoercer that converts between your custom type and some simpler
underlying type, like String or Double. By convention, the coercer
should be an internal class of the custom type it coerces. Additionally,
the custom type should register its own coercer in a static code block.

If this is not possible (for example, if you want to use a java built-in
class like Date or URI as a custom type) then you can write a separate
coercer class and register the coercer with the private variable
declaration:

\`\`\`java  
private static final Object REGISTER\_COERCER =
Custom.registerCoercer(new ObjectCoercer(), CustomObject.class);  
\`\`\`

Typeref Schema

The purpose of the typeref schemas is to keep track of the underlying
type of the custom Type and the location of the custom type’s class,
and, if necessary, the location of its coercer. The basic appearance of
the typeref schema is shown below:

    <code>{
       "type" : "typeref",
       "name" : "CustomObjectRef",
       "namespace" : "com.linkedin.example"  // namespace of the typeref
       "ref" : "string",  // underlying type that the coercer converts to/from
       "java" : {
          "class" : "com.linkedin.example.CustomObject", // location of the custom type class
          "coercerClass" : "com.linkedin.example.CustomObjectCoercer" // only needed if the custom 
                                                                      // type itself cannot contain
                                                                      // the coercer as an internal class.
       }
    }
    </code>

This typeref can then be referenced in other schemas:

    <code>{
      "type": "record",
      "name": "ExampleRecord",
       ...
      "fields": [
                  {"name": "member", "type": "com.linkedin.example.CustomObjectRef"}
                  ...
      ]
    }
    </code>

And the generated Java data templates will automatically coerce from
CustomObjectRef to CustomObject when accessing the member field:

\`\`\`java  
CustomObject o = exampleRecord.getMember();  
\`\`\`

Once Java data templates are generated, the typeref may also be used in
Keys, query parameters, or action parameters:

Keys:

\`\`\`java  
@RestLiCollection(name=“entities”,  
namespace = “com.example”,  
keyTyperefClass = CustomObjectRef.class)  
public class EntitiesResource extends CollectionResourceTemplate\<Urn,
CustomObject\>  
\`\`\`

Compound keys:

\`\`\`java  
`RestLiAssociation(name="entities", namespace="com.example",
assocKeys={`Key(name=“o”, type=CustomObject.class,
typeref=CustomObjectRef.class)})  
\`\`\`

Query parameters:

\`\`\`java  
@QueryParam(value=“o”, typeref=CustomObjectRef.class) CustomObject o

@QueryParam(value=“oArray”, typeref=CustomObjectRef.class)
CustomObject\[\] oArray  
\`\`\`

<a id="CREATE"></a>

##### CREATE

CREATE methods model the creation of new entities from their
representation. In CREATE, the resource implementation is responsible
for assigning a new key to the created entity. CREATE methods are
neither safe nor idempotent.

Resources providing the CREATE resource method must override the
following method signature:  
\`\`\`java  
public CreateResponse create(V entity);  
\`\`\`

The returned `CreateResponse` object indicates the HTTP status code to
be returned (defaults to 201 CREATED), as well as an optional ID for the
newly created entity. If provided, the ID will be written into the
“X-LinkedIn-Id” header by calling `toString()` on the ID object.

@`RestMethod.Create` may be used to indicate a create method instead of
overriding the create method of a base class.

###### Returning entity in CREATE response

By default, the newly created entity is not returned in the CREATE
response because the client already has the entity when sending the
CREATE request. However, there are use cases where the server will
attach additional data to the new entity. Returning the entity in the
CREATE response saves client another GET request.

Starting in Rest.li version 2.10.3, we provide developer the option to
return newly created entity. To use this feature, add @`ReturnEntity`
annotation to the method that implements CREATE. The return type of
method must be `CreateKVResponse`.  
\`\`\`java  
`ReturnEntity
public CreateKVResponse create(V entity);
```
An example implementation for resource is like below, note that the
return type will be `CreateKVResponse`:
```java
`ReturnEntity  
public CreateKVResponse\<Long, Greeting\> create(Greeting entity)  
{  
Long id = 1L;  
entity.setId(id);  
return new CreateKVResponse\<Long, Greeting\>(entity.getId(), entity);  
}  
\`\`\`  
<a id="BATCH_CREATE"></a>

##### BATCH\_CREATE

BATCH\_CREATE methods model the creation of a group of new entities from
their representations. In BATCH\_CREATE, the resource implementation is
responsible for assigning a new key to each created entity.
BATCH\_CREATE methods are neither safe nor idempotent.

Resources providing the BATCH\_CREATE resource method must override the
following method signature:  
\`\`\`java  
public BatchCreateResult\<K, V\> batchCreate(BatchCreateRequest\<K, V\>
entities);  
\`\`\`

The `BatchCreateRequest` object wraps a list of entity representations
of type `V`.

The returned `BatchCreateResult` object wraps a list of `CreateResponse`
objects (see CREATE). The `CreateResponse` objects are expected to be
returned in the same order and position as the respective input objects.

`BatchCreateRequest` and `BatchCreateResult` support the generic type
parameter `K` to allow for future extension.

@`RestMethod.BatchCreate` may be used to indicate a batch create method
instead of overriding the batchCreate method of a base class.

Example of a batch create:

\`\`\`java  
public BatchCreateResult\<Long, Greeting\>
batchCreate(BatchCreateRequest\<Long, Greeting\> entities)  
{  
List<CreateResponse> responses = new
ArrayList<CreateResponse>(entities.getInput().size());

for (Greeting g : entities.getInput())  
{  
responses.add(create(g));  
}  
return new BatchCreateResult\<Long, Greeting\>(responses);  
}

public CreateResponse create(Greeting entity)  
{  
entity.setId(\_idSeq.incrementAndGet());  
\_db.put(entity.getId(), entity);  
return new CreateResponse(entity.getId());  
}  
\`\`\`

Error details can be returned in any CreateResponse by providing a
RestLiServiceException, for example:

\`\`\`java  
public BatchCreateResult\<Long, Greeting\>
batchCreate(BatchCreateRequest\<Long, Greeting\> entities) {  
List<CreateResponse> responses = new
ArrayList<CreateResponse>(entities.getInput().size());

…  
if (…) {  
RestLiServiceException exception = new
RestLiServiceException(HttpStatus.S\_406\_NOT\_ACCEPTABLE, “…”);  
exception.setServiceErrorCode(…);  
exception.setErrorDetails(…);  
responses.add(new CreateResponse(exception));  
}  
…

return new BatchCreateResult\<Long, Greeting\>(responses);  
}  
\`\`\`

###### Returning entities in BATCH\_CREATE response

Similar to CREATE, BATCH\_CREATE also could return the newly created
entities in the response. To do that, add @`ReturnEntity` annotation to
the method implementing BATCH\_CREATE. The return type of the method
must be `BatchCreateKVResult`.  
\`\`\`java  
@ReturnEntity  
public BatchCreateKVResult\<K, V\> batchCreate(BatchCreateRequest\<K,
V\> entities);  
\`\`\`

An example implementation for resource is like below, note that the
return type will be `BatchCreateKVResult`:  
\`\`\`java  
@ReturnEntity  
public BatchCreateKVResult\<Long, Greeting\>
batchCreate(BatchCreateRequest\<Long, Greeting\> entities)  
{  
List\<CreateKVResponse\<Long, Greeting\>\> responses = new
ArrayList\<CreateKVResponse\<Long,
Greeting\>\>(entities.getInput().size());  
for (Greeting greeting : entities.getInput())  
{  
responses.add(create(greeting)); // Create function should return
CreateKVResponse  
}  
return BatchCreateKVResult\<Long, Greeting\>(responses);  
}  
\`\`\`

<a id="UPDATE"></a>

##### UPDATE

UPDATE methods model updating an entity with a given key by setting its
value (overwriting the entire entity). UPDATE has side effects but is
idempotent. For example, repeating the same update operation has the
same effect as calling it once.

Resources may choose whether to allow an UPDATE of an entity that does
not already exist, in which case it should be created. This is different
from CREATE because the client specifies the key for the entity to be
created. Simple resources use UPDATE as a way to create the singleton
entity.

Resources providing the UPDATE resource method must override one of the
following method signatures.

For collection and association resources:  
\`\`\`java  
public UpdateResponse update(K key, V entity);  
\`\`\`

For simple resources:  
\`\`\`java  
public UpdateResponse update(V entity);  
\`\`\`

The returned `UpdateResponse` object indicates the HTTP status code to
be returned.

@`RestMethod.Update` may be used to indicate a update method instead of
overriding the update method of a base class.

<a id="BATCH_UPDATE"></a>

##### BATCH\_UPDATE

BATCH\_UPDATE methods model updating a set of entities with specified
keys by setting their values (overwriting each entity entirely).
BATCH\_UPDATE has side effects but is idempotent. For example, repeating
the same batch update operation has the same effect as calling it once.

Resources may choose whether to allow BATCH\_UPDATE for entities that do
not already exist, in which case each entity should be created. This is
different from BATCH\_CREATE because the client specifies the keys for
the entities to be created.

Resources providing the BATCH\_UPDATE resource method must override the
following method signature:  
\`\`\`java  
public BatchUpdateResult\<K, V\> batchUpdate(BatchUpdateRequest\<K, V\>
entities);  
\`\`\`

`BatchUpdateRequest` contains a map of entity key to entity value.

The returned `BatchUpdateResult` object indicates the `UpdateResponse`
for each key in the `BatchUpdateRequest`. In the case of failures,
`RestLiServiceException` objects may be added to the `BatchUpdateResult`
for the failed keys.

@`RestMethod.BatchUpdate` may be used to indicate a batch update method
instead of overriding the batchUpdate method of a base class.

Example of a batch update:

\`\`\`java  
public BatchUpdateResult\<Long, Greeting\>
batchUpdate(BatchUpdateRequest\<Long, Greeting\> entities)  
{  
Map\<Long, UpdateResponse\> responseMap = new HashMap\<Long,
UpdateResponse\>();  
for (Map.Entry\<Long, Greeting\> entry :
entities.getData().entrySet())  
{  
responseMap.put(entry.getKey(), update(entry.getKey(),
entry.getValue()));  
}  
return new BatchUpdateResult\<Long, Greeting\>(responseMap);  
}

public UpdateResponse update(Long key, Greeting entity)  
{  
Greeting g = \_db.get(key);  
if (g == null)  
{  
return new UpdateResponse(HttpStatus.S\_404\_NOT\_FOUND);  
}

\_db.put(key, entity);

return new UpdateResponse(HttpStatus.S\_204\_NO\_CONTENT);  
}  
\`\`\`

<a id="PARTIAL_UPDATE"></a>

##### PARTIAL\_UPDATE

PARTIAL\_UPDATE methods model updating part of the entity with a given
key. PARTIAL\_UPDATE has side effects. In general, it is not guaranteed
to be idempotent.

Resources providing the PARTIAL\_UPDATE resource method must override
the following method signature:  
\`\`\`java  
public UpdateResponse update(K key, PatchRequest<V> patch);  
\`\`\`

The returned `UpdateResponse` object indicates the HTTP status code to
be returned.

Rest.li provides tools to make it easy to handle partial updates to your
resources. A typical update function should look something like this:

\`\`\`java  
@Override  
public UpdateResponse update(String key, PatchRequest<YourResource>
patch )  
{  
YourResource resource = \_db.get(key); // Retrieve the resource object
from somewhere  
if (resource == null)  
{  
return new UpdateResponse(HttpStatus.S\_404\_NOT\_FOUND);  
}  
try  
{  
PatchApplier.applyPatch(resource, patch); // Apply the patch.  
// Be sure to save the resource if necessary  
}  
catch (DataProcessingException e)  
{  
return new UpdateResponse(HttpStatus.S\_400\_BAD\_REQUEST);  
}  
return new UpdateResponse(HttpStatus.S\_204\_NO\_CONTENT);  
}  
\`\`\`

The PatchApplier automatically updates resources defined using the
Pegasus Data format. The Rest.li client classes provide support for
constructing patch requests, but here is an example update request using
curl:

    <code>
    curl -X POST localhost:/fortunes/1 -d '{"patch": {"$set": {"fortune": "you will strike it rich!"}}}'
    </code>

@`RestMethod.PartialUpdate` may be used to indicate a partial update
method instead of overriding the partialUpdate method of a base
class.

##### Inspecting Partial Updates to Selectively Update Fields in a Backing Store

It is possible to inspect the partial update and selectively write only
the changed fields to a store.

For example, to update only the street field of this address entity:

    <code>
    {
      "address": {
            "street": "10th",
            "city": "Sunnyvale"
        }
    }
    </code>

The partial update to change just the street field is:

    <code>
    {
      "patch": {
        "address": {
          "$set": {
            "street": "9th"
          }
        }
      }
    }
    </code>

For the service code to selectively update just the street field (e.g.
UPDATE addresses SET street=:street WHERE key=:key). The partial update
can be inspected and the selective update if only the street field is
changed:

\`\`\`java  
@Override  
public UpdateResponse update(String key, PatchRequest<YourResource>
patchRequest)  
{  
try  
{  
DataMap patch = patchRequest.getPatchDocument();  
boolean selectivePartialUpdateApplied = false;  
if(patch.containsKey(“address”) && patch.size()  1)
{
DataMap address = patch.getDataMap("address");
if(address.containsKey("$set") && address.size()  1)  
{  
DataMap set = address.getDataMap(“$set”);  
if(address.containsKey(“street”) && address.size()  1)
{
String street = address.getString("street");
selectivePartialUpdateApplied = true;
// update only the street, since its the only thing this patch requests
to change
}
}
}
if(selectivePartialUpdateApplied  false)  
{  
// no selective update available, update the whole record with
PatchApplier and return the result  
}  
}  
catch (DataProcessingException e)  
{  
return new UpdateResponse(HttpStatus.S\_400\_BAD\_REQUEST);  
}  
return new UpdateResponse(HttpStatus.S\_204\_NO\_CONTENT);  
}  
\`\`\`

##### Creating Partial Updates

To create a request to modify field(s), PatchGenerator can be used, for
example:

\`\`\`java  
Fortune fortune = new Fortune().setMessage(“Today’s your lucky day.”);  
PatchRequest<Fortune> patch = PatchGenerator.diffEmpty(fortune);  
Request<Fortune> request = new
FortunesBuilders().partialUpdate().id(1L).input(patch).build();  
\`\`\`

\`PatchGenerator.diff(original, revised)\` can also be used to create a
minimal partial update.

<a id="BATCH_PARTIAL_UPDATE"></a>

##### BATCH\_PARTIAL\_UPDATE

BATCH\_PARTIAL\_UPDATE methods model partial updates of multiple
entities given their keys. BATCH\_PARTIAL\_UPDATE has side effects. In
general, it is not guaranteed to be idempotent.

Resources providing the BATCH\_PARTIAL\_UPDATE resource method must
override the following method signature:  
\`\`\`java  
public BatchUpdateResult\<K, V\> batchUpdate(BatchPatchRequest\<K, V\>
patches);  
\`\`\`

The `BatchPatchRequest` input contains a map of entity key to
`PatchRequest`.

The returned `BatchUpdateResult` object indicates the `UpdateResponse`
for each key in the `BatchPatchRequest`. In the case of failures,
`RestLiServiceException` objects may be added to the `BatchUpdateResult`
for the failed keys.

@`RestMethod.BatchPartialUpdate` may be used to indicate a batch partial
update method instead of overriding the batchPartialUpdate method of a
base class.

Example of a batch partial update:

\`\`\`java  
public BatchUpdateResult\<Long, Greeting\>
batchUpdate(BatchPatchRequest\<Long, Greeting\> entityUpdates)  
{  
Map\<Long, UpdateResponse\> responseMap = new HashMap\<Long,
UpdateResponse\>();  
for (Map.Entry\<Long, PatchRequest<Greeting>\> entry :
entityUpdates.getData().entrySet())  
{  
responseMap.put(entry.getKey(), update(entry.getKey(),
entry.getValue()));  
}  
return new BatchUpdateResult\<Long, Greeting\>(responseMap);  
}

public UpdateResponse update(Long key, PatchRequest<Greeting> patch)  
{  
Greeting g = \_db.get(key);  
if (g == null)  
{  
return new UpdateResponse(HttpStatus.S\_404\_NOT\_FOUND);  
}

try  
{  
PatchApplier.applyPatch(g, patch);  
}  
catch (DataProcessingException e)  
{  
return new UpdateResponse(HttpStatus.S\_400\_BAD\_REQUEST);  
}

\_db.put(key, g);

return new UpdateResponse(HttpStatus.S\_204\_NO\_CONTENT);  
}  
\`\`\`

<a id="DELETE"></a>

##### DELETE

DELETE methods model deleting (removing) an entity with a given key on
collection and association resources or without a key on simple
resources. DELETE has side effects but is idempotent.

Resources providing the DELETE resource method must override one of the
following method signatures.

For collection and association resources:  
\`\`\`java  
public UpdateResponse delete(K key);  
\`\`\`

For simple resources:  
\`\`\`java  
public UpdateResponse delete();  
\`\`\`

The returned `UpdateResponse` object indicates the HTTP status code to
be returned.

@`RestMethod.Delete` may be used to indicate a delete method instead of
overriding the delete method of a base class.

<a id="BATCH_DELETE"></a>

##### BATCH\_DELETE

BATCH\_DELETE methods model deleting (removing) multiple entities given
their keys. BATCH\_DELETE has side effects but is idempotent.

Resources providing the BATCH\_DELETE resource method must override the
following method signature:  
\`\`\`java  
public BatchUpdateResult\<K, V\> batchDelete(BatchDeleteRequest\<K, V\>
ids);  
\`\`\`

The `BatchDeleteRequest` input contains the list of keys to be deleted.
`BatchDeleteRequest` accepts a generic type parameter `V` for future
extension.

The returned `BatchUpdateResult` object indicates the `UpdateResponse`
for each key in the `BatchDeleteRequest`. In the case of failures,
`RestLiServiceException` objects may be added to the `BatchUpdateResult`
for the failed keys.

@`RestMethod.BatchDelete` may be used to indicate a batch delete method
instead of overriding the batchDelete method of a base class.

Example of a batch delete:

\`\`\`java  
public BatchUpdateResult\<Long, Greeting\>
batchDelete(BatchDeleteRequest\<Long, Greeting\> deleteRequest)  
{  
Map\<Long, UpdateResponse\> responseMap = new HashMap\<Long,
UpdateResponse\>();  
for (Long id : deleteRequest.getKeys())  
{  
responseMap.put(id, delete(id));  
}  
return new BatchUpdateResult\<Long, Greeting\>(responseMap);  
}

public UpdateResponse delete(Long key)  
{  
boolean removed = \_db.remove(key) \!= null;

return new UpdateResponse(removed ? HttpStatus.S\_204\_NO\_CONTENT :
HttpStatus.S\_404\_NOT\_FOUND);  
}  
\`\`\`

<a id="ACTION"></a>

##### ACTION

ACTION methods are very flexible and do not specify any standard
behavior.

Resources may provide zero or more ACTION resource methods. Each action
must be annotated with the @`Action` annotation.

The @`Action` annotation supports the following parameters:

  - `name` Required, the name of the action resource method.
  - `resourceLevel` Optional, defaults to `ResourceLevel.ANY`, which
    indicates that the action is defined directly on the containing
    resource and does not support an entity key as a URI parameter.
    `ResourceLevel.COLLECTION` indicates that the action is defined on
    the containing association or collection resource and does not
    support an entity key as a URI parameter. `ResourceLevel.ENTITY`
    indicates that the action is defined on the entity and it requires
    an entity key as a URI parameter when the containing resource is an
    association or collection resource. If the containing resource is a
    simple resource `ResourceLevel.ENTITY` indicates that the action is
    defined directly on the resource and does not support an entity key
    as a URI parameter.
  - `returnTyperef` Optional, defaults to no typeref. Indicates a
    Typeref to be used in the IDL for the action’s return parameter.
    Useful for actions that return primitive types.

Each parameter to an action method must be annotated with
@`ActionParam`, which takes the following annotation parameters:

  - `value` Required, string name for the action parameter. If this is
    the only annotation, parameter, it may be specified without being
    explicitly named, for example, @`ActionParam("paramName")`.
  - `typeref` Optional, Typeref to be used in the IDL for the parameter.

Parameters of action methods may also be annotated with @`Optional`,
which indicates that the parameter is not required in the request. The
@`Optional` annotation may specify a String value, which specifies the
default value to be used if the parameter is not provided in the
request. If the method parameter is of primitive type, a default value
must be specified in the @`Optional` annotation.

Valid parameter types and return types for action are:

  - `String`
  - `boolean` / `Boolean`
  - `int` / `Integer`
  - `long` / `Long`
  - `float` / `Float`
  - `double` / `Double`
  - `ByteString`
  - `Enum`
  - `RecordTemplate` or a subclass of `RecordTemplate` generated from a
    record schema
  - `FixedTemplate` or a subclass of `FixedTemplate` generated from a
    fixed schema
  - `AbstractArrayTemplate` or a subclass of `AbstractArrayTemplate`,
    for example, `StringArray`, `LongArray`, and so on.
  - `AbstractMapTemplate` or a subclass of `AbstractMapTemplate`, for
    example, `StringMap`, `LongMap`, and so on.
  - Custom types

Similar to `GetResult<V>`, since 1.5.8, Rest.li supports an
`ActionResult<V>` wrapper return type that allows you to specify an
arbitrary HTTP status code for the response.

Simple example:  
\`\`\`java  
@Action(name=“action”)  
public void doAction();  
\`\`\`

A more complex example, illustrating multiple parameters:  
\`\`\`java  
`Action(name="sendTestAnnouncement",
resourceLevel= ResourceLevel.ENTITY)
public void sendTestAnnouncement(`ActionParam(“subject”) String
subject,  
`ActionParam("message") String message,
`ActionParam(“emailAddress”) String emailAddress)  
\`\`\`

<a id="ActionParamVQueryParam"></a>

##### `ActionParam vs. `QueryParam

@`ActionParam` and @`QueryParam` are used in different methods.
@`ActionParam` is only allowed in Action methods, while @`QueryParam` is
allowed in all non-Action methods. Besides, they are also different in
terms of how the parameter data is sent to the server. If a parameter is
annotated with @`QueryParam`, the information will be sent in the
request url. If a parameter is annotated with @`ActionParam`, the
information will be sent in the request body. Therefore, one advantage
of using @`ActionParam` would be that the sent parameter can be encoded.
One disadvantage is that the purpose of the request itself can become
less clear if one only examines the url.

<a id="wiki-ReturningNulls"></a>

##### Returning Nulls

Resource methods should never explicitly return `null`. If the Rest.li
framework detects this, it will return an HTTP `500` back to the client
with a message indicating ‘Unexpected null encountered’. The only
exceptions to this rule are ACTION and GET. If an ACTION resource method
returns `null`, the rest.li framework will return an HTTP `200`. If a
GET returns `null`, the Rest.li framework will return an HTTP `404`.

Also note that the HTTP `500` will also be generated by the Rest.li
framework if subsequent data structures inside of resource method
responses are null or contain null. This applies to any data structure
that is not a RecordTemplate. For example, all of the the following
would cause an HTTP `500` to be returned. Note this list is not
exhaustive:

  - A `BatchCreateResult` returning a `null` results list.
  - A `BatchCreateResult` returning a valid list that as a `null`
    element inside of it. 
  - A `CreateResponse` returning a `null` for the `HttpStatus`.
  - A `BatchUpdateResult` returning a `null` key in the results map.
  - A `BatchUpdateResult` returning a `null` errors map.
  - A `BatchUpdateResult` returning a valid errors map, but with a
    `null` key or `null` value inside of it.

It is good practice to make sure that `null` is never returned in any
part of resource method responses, with the exception of RecordTemplate
classes, ACTION methods and GET methods.

<a id="wiki-ResourceContext"></a>

### ResourceContext

`ResourceContext` provides access to the context of the current request.
`ResourceContext` is injected into resources that implement the
`BaseResource` interface, by calling `setContext()`.

For resources extending `CollectionResourceTemplate`,
`AssociationResourceTemplate`, or `ResourceContextHolder`, the current
context is available by calling `getContext()`.

`ResourceContext` provides methods to access the raw request, as well as
parsed values from the request. `ResourceContext` also provides some
control over the generated response, such as the ability to set response
headers.

<a id="wiki-ResourceTemplates"></a>

### Resource Templates

Resource Templates provide convenient methods for implementing resource
classes by extending them. Subclasses may selectively override relevant
methods and for methods that are not overridden, the framework will
recognize that your resource does not support this method and will
return a 404 if clients attempt to invoke it. Note that unsupported
methods will be omitted from your resources IDL (see [Restspec
IDL](Rest.li-User-Guide#wiki-RestspecIDL)
for details).

##### CollectionResourceTemplate

`CollectionResourceTemplate` provides a convenient base class for
collection resources. `CollectionResourceTemplate` defines methods for
all of the CRUD operations. Subclasses may also implement FINDER and
ACTION methods by annotating as described above.

\`\`\`java  
public CreateResponse create(V entity);  
public BatchCreateResult\<K, V\> batchCreate(BatchCreateRequest\<K, V\>
entities);  
public V get(K key);  
public Map\<K, V\> batchGet(Set<K> ids);  
public UpdateResponse update(K key, V entity);  
public BatchUpdateResult\<K, V\> batchUpdate(BatchUpdateRequest\<K, V\>
entities);  
public UpdateResponse update(K key, PatchRequest<V> patch);  
public BatchUpdateResult\<K, V\> batchUpdate(BatchPatchRequest\<K, V\>
patches);  
public UpdateResponse delete(K key);  
public BatchUpdateResult\<K, V\> batchDelete(BatchDeleteRequest\<K, V\>
ids);  
\`\`\`

##### SimpleResourceTemplate

`SimpleResourceTemplate` provides a convenient base class for simple
resources. `SimpleResourceTemplate` defines methods for GET, UPDATE, and
DELETE methods. Subclasses may also implement ACTION methods by
annotating as described above.

\`\`\`java  
public V get();  
public UpdateResponse update(V entity);  
public UpdateResponse delete();  
\`\`\`

##### AssociationResourceTemplate

`AssociationResourceTemplate` provides a convenient base class for
association resources. `AssociationResourceTemplate` defines methods for
all of the CRUD operations except CREATE. Association resources should
implement CREATE by providing up-sert semantics on UPDATE. Subclasses
may also implement FINDER and ACTION methods by annotating as described
above.

\`\`\`java  
public CreateResponse create(V entity);  
public BatchCreateResult\<CompoundKey, V\>
batchCreate(BatchCreateRequest\<CompoundKey, V\> entities);  
public V get(CompoundKey key);  
public Map\<CompoundKey, V\> batchGet(Set<CompoundKey> ids);  
public UpdateResponse update(CompoundKey key, V entity);  
public BatchUpdateResult\<CompoundKey, V\>
batchUpdate(BatchUpdateRequest\<CompoundKey, V\> entities);  
public UpdateResponse update(CompoundKey key, PatchRequest<V> patch);  
public BatchUpdateResult\<CompoundKey, V\>
batchUpdate(BatchPatchRequest\<CompoundKey, V\> patches);  
public UpdateResponse delete(CompoundKey key);  
public BatchUpdateResult\<CompoundKey, V\>
batchDelete(BatchDeleteRequest\<CompoundKey, V\> ids);  
\`\`\`

<a id="wiki-FreeFormResources"></a>

### Free-Form Resources

Resource Templates provide a convenient way to implement the recommended
signatures for the basic CRUD operations (CREATE, GET, UPDATE,
PARTIAL\_UPDATE, DELETE, and respective batch operations). When
possible, we recommend using the resource templates to ensure that your
interface remains simple and uniform.

However, it is sometimes necessary to add custom parameters to CRUD
operations. In these cases, the fixed signatures of resource templates
are too constraining. The solution is to create a free-form resource by
implementing the corresponding marker interface for your resource and
annotating CRUD methods with @`RestMethod.*` annotations.The
`KeyValueResource` interface is the marker interface for collection and
association resources where the `SingleObjectResource` interface is the
marker interface for simple resources.

\`\`\`java  
public class FreeFormCollectionResource implements KeyValueResource\<K,
V\>  
{  
@RestMethod.Create  
public CreateResponse myCreate(V entity);

@RestMethod.BatchCreate  
public BatchCreateResult\<K, V\> myBatchCreate(BatchCreateRequest\<K,
V\> entities);

@RestMethod.Get  
public V myGet(K key);

`RestMethod.GetAll
public CollectionResult<V, M> myGetAll(`Context PagingContext
pagingContex);

@RestMethod.BatchGet  
public Map\<K, V\> myBatchGet(Set<K> ids);

@RestMethod.Update  
public UpdateResponse myUpdate(K key, V entity);

@RestMethod.BatchUpdate  
public BatchUpdateResult\<K, V\> myBatchUpdate(BatchUpdateRequest\<K,
V\> entities);

@RestMethod.PartialUpdate  
public UpdateResponse myUpdate(K key, PatchRequest<V> patch);

@RestMethod.BatchPartialUpdate  
public BatchUpdateResult\<K, V\> myBatchUpdate(BatchPatchRequest\<K, V\>
patches);

@RestMethod.Delete  
public UpdateResponse myDelete(K key);

@RestMethod.BatchDelete  
public BatchUpdateResult\<K, V\> myBatchDelete(BatchDeleteRequest\<K,
V\> ids);  
}  
\`\`\`

\`\`\`java  
public class FreeFormSimpleResource implements SingleObjectResource<V>  
{  
@RestMethod.Get  
public V myGet();

@RestMethod.Update  
public UpdateResponse myUpdate(V entity);

@RestMethod.Delete  
public UpdateResponse myDelete();  
}  
\`\`\`

The advantage of explicitly annotating each resource method is that you
can add custom query parameters (see description of @`QueryParam` for
FINDER resource method) and take advantage of wrapper return types.
Custom query parameters must be defined **after** the fixed parameters
shown above.

\`\`\`java  
`RestMethod.Get
public V myGet(K key, `QueryParam(“myParam”) String myParam);

@RestMethod.Get  
public GetResult<V> getWithStatus(K key);  
\`\`\`

Note that each resource may only provide one implementation of each CRUD
method, e.g., it is invalid to annotate two different methods with
@`RestMethod.Get`.

#### Things to Remember about Free-Form Resources

  - Free-form resources allow you to add query parameters to CRUD
    methods
  - Resource Templates should be used whenever possible
  - Free-form resources must implement one of the `KeyValueResource` and
    `SingleObjectResource` marker interfaces
  - Methods in free-form resources must be annotated with appropriate
    @`RestMethod.*` annotations.
  - Methods in free-form resources must use the same return type and
    initial signature as the corresponding Resource Template method
  - Methods in free-form resources may add additional parameters
    **after** the fixed parameters
  - Free-form resources may not define multiple implementations of the
    same resource method.

<a id="wiki-ReturningErrors"></a>

### Returning Errors

There are several mechanisms available for resources to report errors to
be returned to the caller. Regardless of which mechanism is used,
resources should be aware of the resulting HTTP status code and ensure
that meaningful status codes are used. Remember that `4xx` codes should
be used to report client errors (errors that the client may be able to
resolve), and `5xx` codes should be used to report server errors.

##### Return `null` for GET

If a resource method returns `null` for GET, the framework will
automatically generate a `404` response to be sent to the client.

Note that returning `null` for resource methods is generally forbidden
with the exception of GET and ACTION. Returning a `null` for a GET
returns a 404 and returning a `null` for an ACTION returns 200.

Returning a `null` for any other type of resource method will cause the
rest.li framework to return an HTTP `500` to be sent back to the client
with a message indicating ‘Unexpected null encountered’. This is
described in detail above at <a href="#returning-nulls">Returning
Nulls</a>

##### Return Any HTTP Status Code in a CreateResponse/UpdateResponse

`CreateResponse` and `UpdateResponse` allow an [Http Status
Code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes) to be
provided. Status codes in the `4xx` and `5xx` ranges may be used to
report errors.

##### Throw RestLiServiceException to Return a 4xx/5xx HTTP Status Code

The framework defines a special exception class,
`RestLiServiceException`, which contains an [Http Status
Code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes) field, as
well as other fields that are returned to the client in the body of the
HTTP response. Resources may throw `RestLiServiceException` or a
subclass to prompt the framework to return an HTTP error response.

##### Throw Another Exception

All exceptions originating in application code are caught by the
framework and used to generate an HTTP response. If the exception does
not extend `RestLiServiceException`, an HTTP `500` response will be
sent.

##### Return Errors as Part of a BatchResult

BATCH\_GET methods may return errors for individual items as part of a
`BatchResult` object. Each error is represented as a
`RestLiServiceException` object. In this case, the overall status will
still be an HTTP `200`.

\`\`\`java  
public BatchResult\<K, V\> batchGet((Set<K> ids)  
{  
Map\<K, V\> results = …  
Map\<K, RestLiServiceException\> errors = …  
…  
return new BatchResult(results, errors);  
}  
\`\`\`

#### Handling Errors on the Client

When making requests using `RestClient`, a `ResponseFuture` is always
returned, as shown in this example:

\`\`\`java  
ResponseFuture<Greeting> future = restClient.sendRequest(new
GreetingsBuilders.get().id(1L));  
\`\`\`

This future might contain an error response. When calling
`ResponseFuture.getResponse()`, the default behavior is for a
`RestLiResponseException` to be thrown if the response contains an error
response. Error responses are all 400 and 500 series HTTP status code,
as shown in this example:

\`\`\`java  
try  
{  
Greeting greeting = restClient.sendRequest(new
GreetingsBuilders.get().id(1L)).getResponseEntity();  
// handle successful response  
}  
catch (RestLiResponseException e)  
{  
if(e.getStatus() == 400) {  
// handle 400  
} else {  
// … handle other status codes or rethrow  
}  
}  
\`\`\`

Alternatively, `ErrorHandlingBehavior.TREAT_SERVER_ERROR_AS_SUCCESS` can
be set when making a request. If set, `.getResponse()` will **not**
throw `RestLiResponseException` even if the response contains a 400 or
500 series HTTP status code, as shown in this example:

\`\`\`java

Response<Greeting> response = restClient.sendRequest(new
GreetingsBuilders.get().id(1L),  
ErrorHandlingBehavior.TREAT\_SERVER\_ERROR\_AS\_SUCCESS).getResponse();  
if(response.getStatus()  200)
{
// handle successful response
}
else if (response.getStatus()  404)  
{  
// handle 404  
}  
else  
{  
// … handle other status codes or rethrow  
}  
\`\`\`

However because error responses do not contain an entity, calling
`ResponseFuture.getResponseEntity()` or `Response.getEntity()` will
**always** throw a `RestLiResponseException` for 400 or 500 series HTTP
status code, regardless of `ErrorHandlingBehavior`.

#### Configuring How Errors are Represented in an HTTP Response

By default, Rest.li returns an extensive HTTP error response that
includes:

  - HTTP Status Code (manditory)
  - X-LinkedIn-Error-Response header (this will be renamed to
    X-RestLi-Error-Response shortly)
  - A response body containing:
      - A full stack trace
      - A service error code (optional)
      - Application specific error details (optional)

The error response format configured to return only a subset of these
parts using RestLiConfig, as shown in this
example:

\`\`\`java  
restliConfig.setErrorResponseFormat(ErrorResponseFormat.MESSAGE\_AND\_DETAILS);  
\`\`\`

When Rest.li server application code throws an exception, if the
exception is of type RestLiServiceException, then the error message
provided by the RestLiServiceException is used for the error message in
the HTTP response. However if any other Java exception is thrown,
Rest.li automatically provides a default error message of “Error in
application code” in the error response. This default error message may
be customized via RestLiConfig as well, as shown in this example:

\`\`\`java  
restliConfig.setInternalErrorMessage(“Internal error, please try again
later.”);  
\`\`\`

<a id="wiki-Projections"></a>

### Field Projection

Rest.li provides built-in support for field projections, for example the
structural filtering of responses. The support includes [Java Projection
Bindings](How-to-use-projections-in-Java)
and a [JSON Projection wire
protocol](Projections). The
projection is applied separately to each entity object in the response,
i.e., to the value-type of the CollectionResource or
AssociationResource. If the invoked method is a FINDER that returns a
List, the projection is applied to each element of the list
individually. Likewise, if the invoked method is a BATCH\_GET that
returns a Map\<K, V\>, the projection is applied to each value in the
map individually. Project can also be applied to CREATE and
BATCH\_CREATE when the newly returned entity or entities are returned.

For resource methods that return CollectionResult, the Rest.li framework
also provides the ability to project the Metadata and as well as the
Paging that is sent back to the client. More info on Collection
Pagination is provided below.

The Rest.li server framework recognizes the “fields”, “metadataFields”
or “pagingFields” query parameters in the request. If available, the
Rest.li framework then parses each of these as individual `MaskTrees`.
The resulting `MaskTrees` are available through the ResourceContext (see
above) or directly to the resource methods.

Projection can also be toggled between `AUTOMATIC` and `MANUAL`. The
latter precludes the Rest.li framework from performing any projection
while the former forces the Rest.li framework to perform the projection.

Additional details are described in [How to use projections in
Java](How-to-use-projections-in-Java)

<a id="wiki-Pagination"></a>

### Collection Pagination

Rest.li provides helper methods to implement collection pagination, but
it requires each resource to implement core pagination logic itself.
Rest.li pagination uses positional indices to specify page boundaries.

The Rest.li server framework automatically recognizes the `"start"` and
`"count"` parameters for pagination, parses the values of these
parameters, and makes them available through a `PagingContext` object.
FINDER methods may request the `PagingContext` by declaring a method
parameter annotated with @`Context` (see above).

FINDER methods are expected to honor the `PagingContext` requirements,
for example, to return only the subset of results with logical indices
`>= start` and `< start+count`.

The Rest.li server framework also includes support for returning
CollectionMetadata as part of the response. CollectionMetadata includes
pagination info such as:

  - The requested `start`
  - The requested `count`
  - The `total` number of results (before pagination)
  - Links to the previous and next pages of results

FINDER methods that can provide the `total` number of matching results
should do so by returning an appropriate `CollectionResult` or
`BasicCollectionResult` object.

`total` value must be set in your resources in order for Rest.li
framework to automatically construct `Link` objects to the previous page
(if start \> 0) and the next page (if the response includes count
results).

Example request illustrating use of start & count pagination parameters,
and resulting links in CollectionMetadata:

    <code>
    $ curl "http://localhost:1338/greetings?q=search&start=4&count=2"
    {
        "elements": [ ... ],
        "paging": {
            "count": 2,
            "links": [
              "href": "/greetings?count=10&start=10&q=search",
              "rel": "next",
              "type": "application/json"
            ],
            "start": 4
        }
    }
    </code>

  
NOTE that “start” and “count” returned in CollectionMetadata is REQUEST
start and REQUEST count, that is​, the paging parameter passed from
incoming REQUEST, not metadata for the returned response. If start and
count is not passed in Finder or GetAll request, it will return default
0 for start and 10 for count.The rationale behind this is to make it
easier for a client to subsequently construct requests for additional
pages without having to track the start and count themselves.
Furthermore, there is no point to return a count for number of items
returned, since client can easily get that by calling size() for the
elements array returned.

<a id="wiki-DependencyInjection"></a>

### Dependency Injection

The Rest.li server framework controls the lifecycle of instances of
Resource classes, instantiating a new Resource object for each request.
It is therefore frequently necessary/desirable for resources to use a
dependency-injection mechanism to obtain the objects they depend upon,
for example, database connections or other resources.

Rest.li includes direct support for the following dependency injection
frameworks:

  - [Spring](http://www.springsource.org/) via the [rest.li/spring
    bridge](Spring-Dependency-Injection)
  - [Guice](https://code.google.com/p/google-guice/) via the
    [rest.li/guice
    bridge](Guice-Dependency-Injection)

Other dependency injection frameworks can be used as well. Rest.li
provides an extensible dependency-injection mechanism, through the
`ResourceFactory` interface.

The most broadly used dependency injection mechanism is based on mapping
JSR-330 annotations to the Spring ApplicationContext, and it is provided
by the `InjectResourceFactory` from `restli-contrib-spring`. This is the
recommended approach.

Resource classes may annotate fields with @`Inject` or @`Named`. If only
@`Inject` is specified, the field will be bound to a bean from the
Spring ApplicationContext based on the type of the field. If @`Named` is
used, the field will be bound to a bean with the same name. All beans
must be in the root Spring context.

<a id="wiki-AsynchResources"></a>

### Asynchronous Resources

Rest.li allows resources to return results asynchronously through a
[ParSeq](https://github.com/linkedin/parseq/wiki) `Promise`, `Task`, or
`Callback`. For example, a getter can be declared in any of the
following ways:  
\`\`\`java  
@RestMethod.Get  
public Promise<Greeting> get(Long key)  
{  
// return a promise (e.g. SettablePromise) and set it asynchronously  
}  
\`\`\`

\`\`\`java  
@RestMethod.Get  
public Task<Greeting> get(Long key)  
{  
// set up some ParSeq tasks and return the final Task  
return Tasks.seq(Tasks.par(…), …);  
}  
\`\`\`

\`\`\`java  
`RestMethod.Get
public void get(Long key, `CallbackParam Callback<Greeting> callback)  
{  
// use the callback asynchronously  
}  
\`\`\`

These method signatures can be mixed arbitrarily with the synchronous
signatures, including in the same resource class. For instance, simple
methods can be implemented synchronously and slow methods can be
implemented asynchronously. However, multiple implementations of the
same REST method with different signatures may **not** be provided.

You can also use the asynchronous resource templates in order to
implement asynchronous Rest.li resources. The templates are:

  - <code>AssociationResourceAsyncTemplate</code>
  - <code>AssociationResourcePromiseTemplate</code>
  - <code>AssociationResourceTaskTemplate</code>
  - <code>CollectionResourceAsyncTemplate</code>
  - <code>CollectionResourcePromiseTemplate</code>
  - <code>CollectionResourceTaskTemplate</code>
  - <code>ComplexKeyResourceAsyncTemplate</code>
  - <code>ComplexKeyResourcePromiseTemplate</code>
  - <code>ComplexKeyResourceTaskTemplate</code>
  - <code>SimpleResourceAsyncTemplate</code>
  - <code>SimpleResourcePromiseTemplate</code>
  - <code>SimpleResourceTaskTemplate</code>

The Rest.li server will automatically start any `Task` that is returned
by a `Task` based method by running it through a ParSeq engine. Also,
`Promise` based methods are guaranteed to be run through a `Task` in the
ParSeq engine, including those that do not explicitly take a ParSeq
`Context`. `Callback`-based methods do not receive special treatment.

<a id="wiki-OnlineDocumentation"></a>

### Online Documentation

Rest.li has an on-line documentation generator that dynamically
generates resource IDL and pdsc schemas hosted in the server. The
documentation is available in both HTML and JSON formats, and there are
three ways to access the documentation:

1.  HTML. The relative path to HTML documentation is `restli/docs/`. For
    example, the documentation URI for resource
    `http://<host>:<port>/<context-path>/<resource>` is `GET
    http://<host>:<port>/<context-path>/restli/docs/rest/<resource>`
    (`GET` is the [HTTP GET
    method](http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.3),
    which is the default for the web browser). The root URL, such as
    `http://<host>:<port>/<context-path>/restli/docs`, displays the list
    of all accessible resources and data schemas in the server. Use it
    as a starting point for HTML documentation. Remember to remove the
    `<context-path>` part if there is no context path.
2.  JSON. There are 2 alternative ways to access the raw JSON data:
    1.  Use the `format=json` query parameter on any of the HTML pages
        above. For example, `GET
        http://<host>:<port>/<context-path>/restli/docs/rest/<resource>?format=json`
        for resource documentation and `GET
        http://<host>:<port>/<context-path>/restli/docs/data/<full_name_of_data_schema>?format=json`
        for schema documentation. Homepage `GET
        http://<host>:<port>/<context-path>/restli/docs/?format=json` is
        also available, which aggregates all resources and data schemas.
    2.  Use the [HTTP OPTIONS
        method](http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.2).
        Simply replace the HTTP GET method with the OPTIONS method when
        accessing a resource without using the `format` query parameter.
        This approach only works for resources, and there is no need for
        the special `restli/docs/` path. For example, `OPTIONS
        http://<host>:<port>/<context-path>/<resource>`.

The JSON format is structured as following:

    <code>
    {
      "models": {
        "<full_name_of_data_schema_1>": { <pdsc_of_data_schema_1> },
        "<full_name_of_data_schema_2>": { <pdsc_of_data_schema_2> }
      }
      "resources": {
        "<resource_1>": { <idl_of_resource_1> },
        "<resource_2>": { <idl_of_resource_2> }
      }
    }
    </code>

When accessing the JSON format of data schema, the `resources` key
exists but the value is always empty.

#### Initialize Online Documentation Generator

  - `documentationRequestHandler`: instance of
    `RestLiDocumentationRequestHandler` class, default to null. Specify
    which implementation of documentation generator is used in the
    server. If null, the on-line documentation feature is disabled.
  - `serverNodeUri`: URI prefix of the server without trailing slash,
    default to empty string (“”). The URI prefix is mainly used in the
    HTML documents by `DefaultDocumentationRequestHandler` to properly
    generate links. Usually, this should be an absolute path.

<a id="wiki-ClientFramework"></a>

## Rest.li Client Framework

  - [Introduction](#introduction)
  - [Depending on a Service’s Client
    Bindings](#depending-on-a-services-client-bindings)
  - [Depending on Data Templates](#depending-on-data-templates)
  - [Type-Safe Builders](#type-safe-builders)
  - [Built-in Request and RequestBuilder
    classes](#built-in-request-and-requestbuilder-classes)
  - [Restspec IDL](#restspec-idl)
  - [RestClient](#restclient)
  - [Request Options](#request-options)
  - [ParSeq Integrated Rest Client](#parseq-integrated-rest-client)
  - [Client Code Generator Tool](#client-code-generator-tool)
  - [Rest.li-extras](#restli-extras)

### Introduction

The Rest.li client framework provides support for accessing resources
defined using Rest.li. The client framework consists of two parts:

  - <code>RequestBuilder</code> classes, which provide an interface for
    creating REST requests to access a specific method of a resource.
    Request builders work entirely in-memory and do not communicate with
    remote endpoints.
  - <code>RestClient</code>, which provides an interface for sending
    requests to remote endpoints and receiving responses.

The request builder portion of the framework can be further divided into
two layers:

  - Built-in request builder classes, which provide generic support for
    accessing Rest.li resources. The built-in request builders
    understand how to construct requests for the different Rest.li
    resource methods, but they do not have knowledge of any specific
    resources or the methods they support. Therefore, the built-in
    request builders cannot validate that a request will be supported by
    the remote endpoint.
  - Type-safe request builder classes, which are generated from the
    server resource’s IDL. The type-safe request builders are tailored
    to the specific resource methods supported by each resource. The
    type-safe builders provide an API that guides the developer towards
    constructing valid requests.

Most developers should work with the type-safe request builders, unless
there is a specific need to work with arbitrary resources whose
interfaces are unknown at the time the code is written.

<a id="wiki-CLientBindings"></a>

### Depending on a Service’s Client Bindings

Usually, developers building Rest.li services publish Java client
bindings for the Rest.li resources their service provides as artifacts
into a shared repository, such as a maven repo. By adding a dependency
to these artifacts, other developers can quickly get their hands on the
request builder classes defined in these client bindings to make
requests to the resources provided by that service.

To add a dependency from a gradle project, add the artifact containing
the rest client bindings to your dependency list. If you are unsure of
the name of the artifact, ask the service owners. They are usually the
artifact with a name ending in -client, -api or -rest. Note that the
[configuration](http://gradle.org/docs/current/userguide/dependency_management.html#sec:dependency_configurations)
for the dependency must be set to <code>restClient</code>:

build.gradle:  
\`\`\`groovy  
…  
﻿dependencies {  
// for a local project:  
compile project(path: ‘:example-api’, configuration: ‘restClient’)  
// for a versioned artifact:  
compile group: ‘org.somegroup’, name: ‘example-api’, version: ‘1.0’,
configuration: ‘restClient’  
}  
…  
\`\`\`

<a id="wiki-DataTemplates"></a>

### Depending on Data Templates

To add a dependency to Java bindings for data models, add a
<code>dataTemplate</code> configured dependency in your build.gradle,
for example:

build.gradle:  
\`\`\`groovy  
…  
﻿dependencies {  
// for a local project:  
compile project(path: ‘:example-api’, configuration: ‘dataTemplate’)  
// for a versioned artifact:  
compile group: ‘org.somegroup’, name: ‘example-api’, version: ‘1.0’,
configuration: ‘dataTemplate’  
}  
…  
\`\`\`

Note that you should not usually need to add such a dependency when
adding a <code>restClient</code> dependency, as the
<code>restClient</code> should bring in the <code>dataTemplate</code>
transitively.

Note: If you are writing pegasus schemas (.pdsc files) and need to add a
dependency on other pegasus schemas, you need to add a
<code>dataModel</code> dependency:

build.gradle  
\`\`\`groovy  
…  
dataModel spec.product.example.data  
…  
\`\`\`

<a id="wiki-Builders"></a>

### Type-Safe Builders

The client framework includes a code-generation tool that reads the IDL
and generates type-safe Java binding for each resource and its supported
methods. The bindings are represented as RequestBuilder classes.

<a id="wiki-BuilderFactories"></a>

#### Resource Builder Factory

For each resource described in an IDL file, a corresponding builder
factory will be generated. For Rest.li version \< 1.24.4, the builder
factory will be named `<Resource name>Builders`. For Rest.li version \>=
1.24.4, the builder factory is named `<Resource name>RequestBuilders`.
The factory contains a factory method for each resource method supported
by the resource. The factory method returns a request builder object
with type-safe bindings for the given method.

Standard CRUD methods are named `create()`, `get()`, `update()`,
`partialUpdate()`, `delete()`, and `batchGet()`. Action methods use the
name of the action, prefixed by “action”, `action<ActionName>()`. Finder
methods use the name of the finder, prefixed by “findBy”,
`findBy<FinderName>()`.

An example for a resource named “Greetings” is shown below. Here is the
builder factory for Rest.li \< 1.24.4:

\`\`\`java  
public class GreetingsBuilders {  
public GreetingsBuilders()  
public GreetingsBuilders(String primaryResourceName)  
public GreetingsCreateBuilder create()  
public GreetingsGetBuilder get()  
public GreetingsUpdateBuilder update()  
public GreetingsPartialUpdateBuilder partialUpdate()  
public GreetingsDeleteBuilder delete()  
public GreetingsBatchGetBuilder batchGet()  
public GreetingsBatchCreateBuilder batchCreate()  
public GreetingsBatchUpdateBuilder batchUpdate()  
public GreetingsBatchPartialUpdateBuilder batchPartialUpdate()  
public GreetingsBatchDeleteBuilder batchDelete()  
public GreetingsDoSomeActionBuilder actionSomeAction()  
public GreetingsFindBySearchBuilder findBySearch()  
}  
\`\`\`

Here is the builder factory for Rest.li \>= 1.24.4:

\`\`\`java  
public class GreetingsRequestBuilders extends BuilderBase {  
public GreetingsRequestBuilders()  
public GreetingsRequestBuilders(String primaryResourceName)  
public GreetingsCreateRequestBuilder create()  
public GreetingsGetRequestBuilder get()  
public GreetingsUpdateRequestBuilder update()  
public GreetingsPartialUpdateRequestBuilder partialUpdate()  
public GreetingsDeleteRequestBuilder delete()  
public GreetingsBatchGetRequestBuilder batchGet()  
public GreetingsBatchCreateRequestBuilder batchCreate()  
public GreetingsBatchUpdateRequestBuilder batchUpdate()  
public GreetingsBatchPartialUpdateRequestBuilder batchPartialUpdate()  
public GreetingsBatchDeleteRequestBuilder batchDelete()  
public GreetingsDoSomeActionRequestBuilder actionSomeAction()  
public GreetingsFindBySearchRequestBuilder findBySearch()  
}  
\`\`\`

#### GET Request Builder

In Rest.li \< 1.24.4, the generated GET request builder for a resource
is named `<Resource>GetBuilder`. In Rest.li \>= 1.24.4, the generated
GET request builder is named `<Resource>GetRequestBuilder`. Both support
the full interface of the built-in `GetRequestBuilder`.

If the resource class is a child resource, the generated builder will
include a type-safe path-key binding method for each of the resource’s
ancestors (recursively following parent resources). Each binding method
is declared as:  
\`\`\`java  
public <BuilderType> <pathKeyName>Key(<KeyType> key);  
\`\`\`  
e.g., for a parent pathKey named “groupId” of type `Integer` in the
“Contacts” resource, the binding method in Rest.li \< 1.24.4 would
be:  
\`\`\`java  
public ContactsGetBuilder groupIdKey(Integer key)  
\`\`\`

In Rest.li \>= 1.24.4, it would be:  
\`\`\`java  
public ContactsGetRequestBuilder groupIdKey(Integer key)  
\`\`\`

#### BATCH\_GET Request Builder

In Rest.li \< 1.24.4, the generated BATCH\_GET request builder for a
resource is named `<Resource>BatchGetBuilder`. The generated builder
supports the full interface of the built-in `BatchGetRequestBuilder`.

In Rest.li \>= 1.24.4, the generated BATCH\_GET request builder for a
resource is named `<Resource>BatchGetRequestBuilder`. The generated
builder extends the built-in `BatchGetEntityRequestBuilder`.

When building requests with `BatchGetRequestBuilder`, use the
`buildKV()` method (`build()` is deprecated), for example:  
\`\`\`java  
new FortunesBuilders().batchGet().ids(…).buildKV()  
\`\`\`

When building requests with the `BatchGetEntityRequestBuilder`, the
`build()` method is used.

If the resource class is a child resource, the generated builder will
include a type-safe path-key binding method for each of the resource’s
ancestors (recursively following parent resources). Each binding method
is declared as:  
\`\`\`java  
public <BuilderType> <pathKeyName>Key(<KeyType> key);  
\`\`\`  
For example, a parent pathKey named “groupId” of type `Integer` in the
“Contacts” resource will have the binding method in Rest.li \< 1.24.4
be this:  
\`\`\`java  
public ContactsBatchGetBuilder groupIdKey(Integer key)  
\`\`\`

In Rest.li \>= 1.24.4, it would be:  
\`\`\`java  
public ContactsBatchGetRequestBuilder groupIdKey(Integer key)  
\`\`\`

#### FINDER Request Builder

In Rest.li \< 1.24.4, the generated FINDER request builder for a
resource is named `<Resource>FindBy<FinderName>Builder`, while in
Rest.li \>= 1.24.4 it is named
`<Resource>FindBy<FinderName>RequestBuilder`. Both builders support the
full interface of the built-in `FindRequestBuilder`.

If the resource class is a child resource, the generated builder will
include a type-safe path-key binding method for each of the resource’s
ancestors (recursively following parent resources). Each binding method
is declared as:  
\`\`\`java  
public <BuilderType> <pathKeyName>Key(<KeyType> key);  
\`\`\`

The generated builder will contain a method to set each of the finder’s
query parameters, of the form:  
\`\`\`java  
public <BuilderType> <paramName>Param(<ParamType> value);  
\`\`\`

The value **must** be non-null.

If the finder specifies `AssocKey` parameters, the builder will contain
a method to set each of them, of the form:  
\`\`\`java  
public <BuilderType> <assocKeyName>Key(<AssocKeyType> value);  
\`\`\`

#### CREATE Request Builder

In Rest.li \< 1.24.4, the generated CREATE request builder for a
resource is named `<Resource>CreateBuilder`. The generated builder
supports the full interface of the built-in `CreateRequestBuilder`.

In Rest.li \>= 1.24.4, the generated CREATE request builder for a
resource is named `<Resource>CreateRequestBuilder`. The generated
builder extends the built-in `CreateIdRequestBuilder`.

If the resource class is a child resource, the generated builder will
include a type-safe path-key binding method for each of the resource’s
ancestors (recursively following parent resources). Each binding method
is declared as:  
\`\`\`java  
public <BuilderType> <pathKeyName>Key(<KeyType> key);  
\`\`\`

If @`ReturnEntity` annotation is specified for CREATE implementation, an
additional `CreateAndGet` request builder will be generated. Note that
`Create` request builder is still available so that adding
@`ReturnEntity` is backward compatible for a Java client.  
\`\`\`java  
public class <Resource>RequestBuilders  
{  
…  
public <Resource>CreateRequestBuilder create();  
public <Resource>CreateAndGetRequestBuilder createAndGet();  
…  
}  
\`\`\`  
The response will be of type `IdEntityResponse<K, V>` which has a
`getEntity()` method:  
\`\`\`java  
…  
// “greeting” is defined in previous context  
CreateIdEntityRequest\<Long, Greeting\> createIdEntityRequest =
builders.createAndGet().input(greeting).build();  
Response\<IdEntityResponse\<Long, Greeting\>\> response =
restClient.sendRequest(createIdEntityRequest).getResponse();  
…  
IdEntityResponse\<Long, Greeting\> idEntityResponse =
response.getEntity();  
// The returned entity from server  
Greeting resultEntity = idEntityResponse.getEntity();  
\`\`\`

The projection for returned entity is supported.  
\`\`\`java  
…  
// “greeting” is defined in previous context  
CreateIdEntityRequest\<Long, Greeting\> createIdEntityRequest =
builders.createAndGet().fields(Greeting.fields().tone(),
Greeting.fields().id()).input(greeting).build();  
\`\`\`

#### BATCH\_CREATE Request Builder

In Rest.li \< 1.24.4, the generated BATCH\_CREATE request builder for a
resource is named `<Resource>BatchCreateBuilder`. The generated builder
supports the full interface of the built-in `BatchCreateRequestBuilder`.

In Rest.li \>= 1.24.4, the generated BATCH\_CREATE request builder for a
resource is named `<Resource>BatchCreateRequestBuilder`. The generated
builder extends the built-in `BatchCreateIdRequestBuilder`.

If the resource class is a child resource, the generated builder will
include a type-safe path-key binding method for each of the resource’s
ancestors (recursively following parent resources). Each binding method
is declared as:  
\`\`\`java  
public <BuilderType> <pathKeyName>Key(<KeyType> key);  
\`\`\`

If @`ReturnEntity` annotation is specified for BATCH\_CREATE
implementation, an additional `BatchCreateAndGet` request builder will
be generated. Note that `BatchCreate` request builder will still be
generated so that adding @`ReturnEntity` annotation is backward
compatible for a Java client.  
\`\`\`java  
public class <Resource>RequestBuilders  
{  
…  
public <Resource>BatchCreateRequestBuilder batchCreate();  
public <Resource>BatchCreateAndGetRequestBuilder batchCreateAndGet();  
…  
}  
\`\`\`

The response will be of type `BatchCreateIdEntityResponse` whose
elements are `CreateIdEntityStatus` object containing the returned
entity. Here is a code example.  
\`\`\`java  
…  
// “greetings” is defined in previous context  
BatchCreateIdEntityRequest\<Long, Greeting\> batchCreateIdEntityRequest
= builders.batchCreateAndGet().inputs(greetings).build();  
Response\<BatchCreateIdEntityResponse\<Long, Greeting\>\> response =
restClient.sendRequest(batchCreateIdEntityRequest).getResponse();  
BatchCreateIdEntityResponse\<Long, Greeting\> entityResponses =
response.getEntity();  
for (CreateIdEntityStatus\<?, ?\> individualResponse :
entityResponses.getElements())  
{  
Greeting entity = (Greeting)individualResponse.getEntity();// The
returned individual entity from server  
}  
\`\`\`  
The projection for returned entities is supported.  
\`\`\`java  
…  
// “greetings” is defined as a list of greeting in previous context  
BatchCreateIdEntityRequest\<Long, Greeting\> batchCreateIdEntityRequest
= builders.batchCreateAndGet().fields(Greeting.fields().tone(),
Greeting.fields().id()).inputs(greetings).build();  
\`\`\`

#### PARTIAL\_UPDATE Request Builder

In Rest.li \< 1.24.4, the generated PARTIAL\_UPDATE request builder for
a resource is named `<Resource>PartialUpdateBuilder`. Whereas in Rest.li
\>= 1.24.4, it is called `<Resource>PartialUpdateRequestBuilder`. Both
builders support the full interface of the built-in
`PartialUpdateRequestBuilder`.

If the resource class is a child resource, the generated builder will
include a type-safe path-key binding method for each of the resource’s
ancestors (recursively following parent resources). Each binding method
is declared as:  
\`\`\`java  
public <BuilderType> <pathKeyName>Key(<KeyType> key);  
\`\`\`

See [Creating partial
updates](Rest.li-User-Guide#wiki-creating-partial-updates)
for details on how to create a request for a partial update.

#### BATCH\_PARTIAL\_UPDATE Request Builder

In Rest.li \< 1.24.4, the generated BATCH\_PARTIAL\_UPDATE request
builder for a resource is named `<Resource>BatchPartialUpdateBuilder`.
Whereas in Rest.li \>= 1.24.4, it is
`<Resource>BatchPartialUpdateRequestBuilder`. Both support the full
interface of the built-in `BatchPartialUpdateRequestBuilder`.

If the resource class is a child resource, the generated builder will
include a type-safe path-key binding method for each of the resource’s
ancestors (recursively following parent resources). Each binding method
is declared as:  
\`\`\`java  
public <BuilderType> <pathKeyName>Key(<KeyType> key);  
\`\`\`

#### UPDATE Request Builder

In Rest.li \< 1.24.4, the generated UPDATE request builder for a
resource is named `<Resource>UpdateBuilder`. Whereas in Rest.li \>=
1.24.4, it is named `<Resource>UpdateRequestBuilder`. Both builders
support the full interface of the built-in `UpdateRequestBuilder`.

If the resource class is a child resource, the generated builder will
include a type-safe path-key binding method for each of the resource’s
ancestors (recursively following parent resources). Each binding method
is declared as:  
\`\`\`java  
public <BuilderType> <pathKeyName>Key(<KeyType> key);  
\`\`\`

#### BATCH\_UPDATE Request Builder

In Rest.li \< 1.24.4, the generated BATCH\_UPDATE request builder for a
resource is named `<Resource>BatchUpdateBuilder`. Whereas in Rest.li \>=
1.24.4, it is named `<Resource>BatchUpdateRequestBuilder`. Both builders
support the full interface of the built-in `BatchUpdateRequestBuilder`.

If the resource class is a child resource, the generated builder will
include a type-safe path-key binding method for each of the resource’s
ancestors (recursively following parent resources). Each binding method
is declared as:  
\`\`\`java  
public <BuilderType> <pathKeyName>Key(<KeyType> key);  
\`\`\`

#### DELETE Request Builder

The generated DELETE request builder for a resource is named
`<Resource>DeleteBuilder`. The generated builder supports the full
interface of the built-in `DeleteRequestBuilder`.

If the resource class is a child resource, the generated builder will
include a type-safe path-key binding method for each of the resource’s
ancestors (recursively following parent resources). Each binding method
is declared as:  
\`\`\`java  
public <BuilderType> <pathKeyName>Key(<KeyType> key);  
\`\`\`

#### BATCH\_DELETE Request Builder

In Rest.li \< 1.24.4, the generated BATCH\_DELETE request builder for a
resource is named `<Resource>BatchDeleteBuilder`. Whereas in Rest.li \>=
1.24.4, the builder is called `<Resource>BatchDeleteRequestBuilder`.
Both builders support the full interface of the built-in
`BatchDeleteRequestBuilder`.

If the resource class is a child resource, the generated builder will
include a type-safe path-key binding method for each of the resource’s
ancestors (recursively following parent resources). Each binding method
is declared as:  
\`\`\`java  
public <BuilderType> <pathKeyName>Key(<KeyType> key);  
\`\`\`

#### ACTION Request Builder

In Rest.li \< 1.24.4, the generated ACTION request builder for a
resource is named `<Resource>Do<ActionName>Builder`. Whereas in Rest.li
\>= 1.24.4, it is `<Resource>Do<ActionName>RequestBuilder`. Both
builders support the full interface of the built-in
`ActionRequestBuilder`.

If the resource class is a child resource, the generated builder will
include a type-safe path-key binding method for each of the resource’s
ancestors (recursively following parent resources). Each binding method
is declared as:  
\`\`\`java  
public <BuilderType> <pathKeyName>Key(<KeyType> key);  
\`\`\`

The generated builder will contain a method to set each of the action’s
parameters. It Rest.li \< 1.24.4, it is of the form:  
\`\`\`java  
public <BuilderType> param<ParamName>(<ParamType> value);  
\`\`\`

In Rest.li \>= 1.24.4, it is of the form:  
\`\`\`java  
public <BuilderType> <paramName>Param(<ParamType> value);  
\`\`\`

The value **must** be non-null.

<a id="wiki-CallingSubResources"></a>

#### Calling Sub-Resources

To call a subresource of the fortunes resource, for example:

\`\`\`  
GET /fortunes/1/subresource/100  
\`\`\`

The parent keys can be specified by calling generated setters on the
builder. In this case, the `fortunesIdKey()` method, for example:

\`\`\`java  
new SubresourceBuilders().get().fortunesIdKey(1l).id(100l).build()  
\`\`\`

Parent path keys can also be set directly builder classes using the
`setPathKey()` method on the builders classes, for example:

\`\`\`java  
.setPathKey(“dest”, “dest”).setPathKey(“src”, “src”)  
\`\`\`

<a id="wiki-BuiltinRequestBuilders"></a>

### Built-in Request and RequestBuilder classes

The built-in RequestBuilder classes provide generic support for
constructing Rest.li requests. This layer is independent of the IDL for
specific resources; therefore, the interface does not enforce that only
“valid” requests are constructed.

There is one RequestBuilder subclass for each of the Rest.li resource
methods. Each RequestBuilder provides a `.build()` method that
constructs a `Request` object that can be used to invoke the
corresponding resource method. Each RequestBuilder constructs the
`Request` subclass that corresponds to the Rest.li method, for example,
`BatchGetRequestBuilder.build()` returns a `BatchGetRequest`. The
`Request` subclasses allow framework code to introspect the original
type and parameters for a given request.

Each RequestBuilder class supports a subset of the following methods, as
appropriate for the corresponding resource method:

  - `header(String key, String value)` - sets a request header
  - `addCookie(HttpCookie cookie)` - adds a cookie
  - `id(K id)` - sets the entity key for the resource
  - `ids(Collection<K> ids)` - sets a list of entity keys
  - `name(String name)` - sets the name for a named resource method
  - `setParam(String name, Object value)` - sets a query param named
    `name` to `value`
  - `addParam(String name, Object value)` - adds `value` to the query
    param named `name`
  - `assocKey(String key, Object value)` - sets an association key
    parameter
  - `pathKey(String key, Object value)` - sets a path key parameter
    (entity key of a parent resource)
  - `paginate(int start, int count)` - sets pagination parameters
  - `fields(PathSpec... fieldPaths)` - sets the fields projection mask
  - `input(V entity)` - sets the input payload for the request
  - `inputs(Map<K, V> entities)` - sets the input payloads for batch
    requests

The following table summarizes the methods supported by each
RequestBuilder type.

|*. Request Builder |*. header |*. id |*. ids |*. name |*. setParam |*.
addParam |*. assocKey |*. pathKey |*. paginate |*. fields |*. input |\_.
inputs |  
| Action | - | - | | - | - | - | | - | | | | |  
| Find | - | | | - | - | - | - | - | - | - | | |  
| Get | - | ~~\* | | |~~ | - | | - | | - | | |  
| Create | - | | | | - | - | | - | | | - | |  
| Delete | - | ~~\* | | |~~ | - | | - | | | | |  
| PartialUpdate | - | - | | | - | - | | - | | | - | |  
| Update | - | ~~\* | | |~~ | - | | - | | | - | |  
| BatchGet | - | | - | | - | - | | - | | - | | |  
| BatchCreate | - | | | | - | - | | - | | | | - |  
| BatchDelete | - | | - | | - | - | | - | | | | |  
| BatchPartialUpdate | - | | | | - | - | | - | | | | - |  
| BatchUpdate | - | | | | - | - | | - | | | | - |  
\*:It is not supported, if the method is defined on a simple resource.

Refer to the JavaDocs for specific details of RequestBuilder and Request
interfaces.

<a id="RestspecIDL"></a>

<a id="wiki-IDL"></a>

### Restspec IDL

Rest.li uses a custom format called REST Specification (Restspec) as its
interface description language (IDL). The Restspec provides a succinct
description of the URI paths, HTTP methods, query parameters, and JSON
format. Together, these form the interface contract between the server
and the client.

Restspec files are JSON format and use the file suffix \*.restspec.json.

At a high level, the restspec contains the following information:

  - name of the resource
  - path to the resource
  - schema type (value type) of the resource
  - resource pattern (collection / simple / association / actionsSet)
  - name and type of the resource key(s)
  - list of supported CRUD methods (CREATE, GET, UPDATE,
    PARTIAL\_UPDATE, DELETE, and corresponding batch methods)
  - description of each FINDER, including
      - name
      - parameter names, types, and optionality
      - response metadata type (if applicable)
  - description of each ACTION, including
      - name
      - parameter names, types, and optionality
      - response type
      - exception types
  - a description of each subresource, containing the information
    described above

Additional details on the Restspec format may be found in the [design
documents](Rest.li-.restspec.json-Format).
The Restspec format is formally described by the .pdsc schema files in
“com.linkedin.restli.restspec.\* ” distributed in the restli-common
module.

<a id="IDLGeneratorTool"></a>

#### IDL Generator Tool

The IDL generator is used to create the language-independent interface
description (IDL) from Rest.li resource implementations (annotated Java
code).

The IDL generator is available as part of the restli-tools JAR, as the
`com.linkedin.restli.tools.idlgen.RestLiResourceModelExporterCmdLineApp`
class.

For details on how to use the IDL Generator, see [Gradle build
integration](Gradle-build-integration).

<a id="Client"></a>  
<a id="RestClient"></a>

<a id="wiki-RestClient"></a>

### RestClient

`RestClient` encapsulates the communication with the remote resource.
`RestClient` accepts a `Request` object as input and provides a
`Response` object as output. The `Request` objects should usually be
built using the [generated type-safe client
builders](./Rest.li-User-Guide#type-safe-builders). Since the
`RestClient` interface is fundamentally asynchronous, the `Response`
must be obtained through either a `ResponseFuture` or a `Callback` (both
options are supported).

`RestClient` is a simple wrapper around an R2 transport client. For
standalone / test use cases, the transport client can be obtained
directly from R2, for example, using the `HttpClientFactory`. If you
wish to use D2, the `Client` used by the `RestClient` must be a D2
client.

The `RestClient` constructor also requires a URI prefix that is
prepended to the URIs generated by the Request Builders. When using D2,
a prefix of `"d2://"` should be provided that results in URIs using the
D2 scheme.

#### ResponseFuture

The `RestClient` future-based interface returns `ResponseFuture`, which
implements the standard `Future` interface and extends it with a
`getResponse()` method. The advantage of `getResponse()` is that it is
aware of Rest.li exception semantics, throwing
`RemoteInvocationException` instead of `ExecutionException`.

#### Making requests using the RestClient and generated RequestBuilders

The standard pattern for making requests using the RestClient is as
follows:

1.  Build the request using the generated request builders
2.  Use the `RestClient#sendRequest` method to send the request and get
    back a `ResponseFuture`
3.  Call `ResponseFuture#getResponse` to get the `Response` that the
    server returned. **Note that this call blocks until the server
    responds or there is an error\!**

Here is a more concrete example, where a client is making a GET request
to the /greetings resource -

\`\`\`java  
// First we build the Request. builders is either a GreetingsBuilder or
GreetingsRequestBuilder  
Request<Greeting> getRequest = builders.get().id(id).build();

// Send the Request and get back a ResponseFuture representing the
response. This call is non-blocking.  
ResponseFuture<Greeting> responseFuture =
restClient.sendRequest(getRequest);

// Like the standard Java Future semantics, calling getResponse() here
IS blocking\!  
Response<Greeting> getResponse = responseFuture.getResponse();

// Get the entity from the Response  
Greeting responseGreeting = getResponse.getEntity();  
\`\`\`

Look at the `com.linkedin.restli.client.Response` interface to see what
other methods are available for use.

#### Request API changes in Rest.li \>= 1.24.4

There are two major changes:

  - `CreateIdRequestBuilder`, which is the super class for all CREATE
    request builders, now returns a `CreateIdRequest<K, V>` when the
    `build()` method is called.
  - `BatchCreateIdRequestBuilder`, which is the super class for all
    BATCH\_CREATE request builders, now returns a
    `BatchCreateIdRequest<K, V>` when the `build()` method is called.

#### Response API Changes in Rest.li \>= 1.24.4

Starting with Rest.li 1.24.4, we have introduced a few changes to the
`Response` API.

##### Response from a CREATE and BATCH\_CREATE Request

As mentioned in the section above, calling `build()` on a
`CreateIdRequestBuilder` gives us a `CreateIdRequest<K, V>`.  
When this is sent using a `RestClient` we get back (after calling
`sendRequest(...).getResponse().getEntity()`) an `IdResponse<K>` that
gives us a single, strongly-typed key.

Similarly, when a `RestClient` is used to send out a
`BatchCreateIdRequest<K, V>` we get back a `BatchCreateIdResponse<K>`,
which contains a `List` of strongly-typed keys.

##### Response from a BATCH\_GET Request

When a `BatchGetEntityRequest` is sent using a `RestClient` we get back
(after calling `sendRequest(...).getResponse().getEntity()`) a
`BatchKVResponse<K,EntityResponse<V>>` where `K` is the key type and `V`
is the value (which extends `RecordTemplate`) for the resource we are
calling.

`EntityResponse` is a `RecordTemplate` with three fields:

  - `entity` provides an entity record if the server resource finds a
    corresponding value for the key;
  - `status` provides an optional status code;
  - `error` provides the error details from the server resource
    (generally `entity` and `error` are mutually exclusive as `null`,
    but it is ultimately up to the server resource).

Note that since `EntityResponse` contains an `error` field, the `Map<K,
V>` returned by `BatchEntityResponse#getResults()` contains both
successful as well as failed entries. `BatchEntityResponse#getErrors()`
will only return failed
entries.

##### Response from a BATCH\_UPDATE, BATCH\_PARTIAL\_UPDATE, and BATCH\_DELETE Request

The response type of the `BatchUpdate` series methods are not changed.
However, similar to `EntityResponse`, we added a new `error` field to
`UpdateStatus` (the value type of the `BatchUpdate` series methods).
Furthermore, `BatchKVResponse<K, UpdateStatus>#getResults()` will
returns both successful as well as failed entries. `getErrors()` will
only return failed entries.

#### Error Semantics

The following diagram illustrates the request/response flow for a
client/server interaction. The call may fail at any point during this
flow, as described below.

![Rest.li Request
Flow](/images/RequestFlow.png
"Rest.li Request Flow")

The following list describes the failures scenarios as observed by a
client calling `ResponseFuture.getResponse()`

Failure Scenarios

  - Client Framework (outbound)
      - `ServiceUnavailableException` - if D2 cannot locate a node for
        the requested service URI
      - `RemoteInvocationException` - if R2 cannot connect to the remote
        endpoint or send the request
  - Network Transport (outbound)
      - `TimeoutException` - if a network failure prevents the request
        from reaching the server
  - Server Framework (inbound)
      - `RestLiResponseException` - if an error occurs within the
        framework, resulting in a non-200 response
      - `TimeoutException` - if an error prevents the server from
        sending a response
  - Server Application
      - `RestLiResponseException` - if the application throws an
        exception the server framework will convert it into a non-200
        response
      - `TimeoutException` - if an application error prevents the server
        from sending a response in a timely manner
  - Server Framework (outbound)
      - `RestLiResponseException` - if an error occurs within the
        framework, resulting in a non-200 response
      - `TimeoutException` - if an error prevents the server from
        sending a response
  - Network Transport (inbound)
      - `TimeoutException` - if a network failure prevents the response
        from reaching the client
  - Client Framework (inbound)
      - `RestLiDecodingException` - if the client framework cannot
        decode the response document
      - `RemoteInvocationException` - if an error occurs within the
        client framework while processing the response.

<a id="wiki-ParSeq"></a>

### Request Options

Each request sent to a Rest.li server can be configured with custom
options by using an instance of `RestliRequestOptions`.
`RestliRequestOptionsBuilder` is required to construct an instance of
`RestliRequestOptions`. Once constructed, an instance of
`RestliRequestOptions` can then be passed to Rest.li generated type-safe
request builders. Subsequently, `RestClient` will construct a
`RestRequest` based on these custom options to send to the Rest.li
server. Currently we support specifying the following custom options per
Request:

#### ProtocolVersionOption

When sending a Request, the caller can specify what protocol version
option is to be used. The available ProtocolVersionOption(s) are:

##### FORCE\_USE\_NEXT

Use the next version of the Rest.li protocol to encode requests,
regardless of the version running on the server. The next version of the
Rest.li protocol is the version currently under development. This option
should typically NOT be used for production services.  
**CAUTION**: this can cause requests to fail if the server does not
understand the next version of the protocol.  
“Next version” is defined as
`com.linkedin.restli.internal.common.AllProtocolVersions.NEXT_PROTOCOL_VERSION`.

##### FORCE\_USE\_LATEST

Use the latest version of the Rest.li protocol to encode requests,
regardless of the version running on the server.  
**CAUTION**: this can cause requests to fail if the server does not
understand the latest  
version of the protocol. “Latest version” is defined as
`com.linkedin.restli.internal.common.AllProtocolVersions.LATEST_PROTOCOL_VERSION`.

##### USE\_LATEST\_IF\_AVAILABLE

Use the latest version of the Rest.li protocol if the server supports
it. If the server version is less than the baseline Rest.li protocol
version then fail the request. If the server version is greater than the
next Rest.li protocol version then fail the request. If the server is
between the baseline and the latest version then use the server version
to encode the request. If the server version is greater than or equal to
the latest protocol version then use that to encode the request.

  - “Baseline version” is defined as
    `com.linkedin.restli.internal.common.AllProtocolVersions.BASELINE_PROTOCOL_VERSION`.
  - “Latest version” is defined as
    `com.linkedin.restli.internal.common.AllProtocolVersions.LATEST_PROTOCOL_VERSION`.
    
  - “Next version” is defined as
    `com.linkedin.restli.internal.common.AllProtocolVersions.NEXT_PROTOCOL_VERSION`.

**CAUTION**: Please be very careful setting the non-default
**FORCE\_USE\_NEXT** or **FORCE\_USE\_LATEST** options as the protocol
version option in `RestLiRequestOptions`, since they may cause requests
to fail if the server does not understand the desired protocol request.
This form of configuration is normally used in migration cases.

#### CompressionOption

When sending a Request, the caller can force compression on or off for
each request.

##### FORCE\_ON

Compress the request.

##### FORCE\_OFF

Do not compress the request.

If `null` is specified, Rest.li `ClientCompressionFilter` will determine
whether we need to do client side compression based on request entity
length.

#### ContentType

When sending a Request, the caller can also specify what content type is
to be used. The specified value will be set to the HTTP header
“Content-Type” for the request.

##### JSON

This will set “Content-Type” header value as “application/json”.

##### PSON

This will set “Content-Type” header value as “application/x-pson”

**NOTE**: Besides `RestliRequestOption`, the caller can also specify the
`ContentType` through the `RestClient` constructor by passing the
contentType parameter (as shown below), which will apply to all requests
sent through that client instance.

\`\`\`java  
public RestClient(Client client, String uriPrefix, ContentType
contentType, List<AcceptType> acceptTypes)  
\`\`\`

However, this form of configuration has been DEPRECATED. Please use
`RestliRequestOptions` instead to set such custom options. In cases
where the caller has configured content type from multiple places,
RestClient will resolve request content type based on the following
precedence order:

1.  Request header. 
2.  RestliRequestOptions.
3.  RestClient configuration. 

If `null` is specified for content type from these 3 sources,
`RestClient` will use JSON as default.

#### AcceptType

When sending a Request, the caller can also specify what media types it
can accept. The specified value will be set to the HTTP header “Accept”
for the request. If more than one AcceptType is specified, we will
generate an Accept header by appending each media type by a “q”
parameter for indicating a relative quality factor. For example:

    <code>Accept: application/*; q=0.2, application/json</code>

  
Quality factors allow the user or user agent to indicate the relative
degree of preference for that media type, using the scale from 0 to 1.
The default value is q=1. In our case, the quality factor generated is
based on the order of each accept type we specified in the list. See
http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html for details.

##### JSON

This will accept media type of “application/json”.

##### PSON

This will accept media type of “application/x-pson”.

##### ANY

This will accept any media type.

**NOTE**: Besides `RestliRequestOption`, the caller can also specify
AcceptType through the `RestClient` constructor by passing the
acceptTypes parameter (as shown below), which will apply to all requests
sent through that client instance.

\`\`\`java  
public RestClient(Client client, String uriPrefix, List<AcceptType>
acceptTypes)  
public RestClient(Client client, String uriPrefix, ContentType
contentType, List<AcceptType> acceptTypes)  
\`\`\`  
However, this form of configuration has been DEPRECATED. Please use
`RestliRequestOptions` instead to set such custom options. In cases
where the caller has configured accept types from multiple places,
RestClient will resolve request accept type based on the following
precedence order:

1.  Request header. 
2.  RestliRequestOptions.
3.  RestClient configuration. 

If `null` is specified for the accept type from these 3 sources,
`RestClient` will not set the HTTP “Accept” header. If no accept header
field is present, then it is assumed by the Rest.li server that the
client accepts all media types based on the HTTP Spec (RFC 2616).

If `RestliRequestOptions` is not set, or is set to null, the request
builders will use
`RestliRequestOptions.DEFAULT_OPTIONS(ProtocolVersionOption.USE_LATEST_IF_AVAILABLE,
null /*compression*/, null /*content type*/, null /*accept type*/)` to
generate the Request.

### ParSeq Integrated Rest Client

The `ParSeqRestClient` wrapper facilitates usage with ParSeq by
providing methods that return a `Promise` or a `Task`. For example,
users can create multiple requests and use ParSeq to send them in
parallel. This feature is independent of the asynchronous resources; in
particular, the server resource does not have to be asynchronous.

\`\`\`java  
ParSeqRestClient client = new ParSeqRestClient(plain rest client);  
// send some requests in parallel  
Task\<Response\<?\>\> task1 = client.createTask(request1);  
Task\<Response\<?\>\> task2 = client.createTask(request2);  
Task\<Response\<?\>\> combineResults = …;  
// after we get our parallel requests, combine them  
engine.run(Tasks.seq(Tasks.par(task1, task2), combineResults))  
\`\`\`  
Users of `createTask` are required to instantiate their own ParSeq
engine and start the task themselves.

<a id="ClientCodeGeneratorTool"></a>

<a id="wiki-CodeGenTool"></a>

### Client Code Generator Tool

As described above, the Rest.li client framework includes a
code-generation tool that creates type-safe Request Builder classes
based on resource IDL files.

The code generator is available as part of the restli-tools JAR, as
`com.linkedin.restli.tools.clientgen.RestRequestBuilderGenerator`. The
generator is invoked by providing an output directory and a list of
input IDL files as command-line arguments.

In addition, the generator recognizes the following system properties:

  - `generator.rest.generate.datatemplates` - boolean property
    indicating whether the generator should generate Java RecordTemplate
    classes for the .pdsc schemas referenced by the IDL file.
  - `generator.default.package` - the default package name for generated
    classes
  - `generator.resolver.path` - a colon-separated list of filesystem
    paths to search when resolving references to named schemas. See
    “Data Template Generator” for more details.

The Rest.li client code generator is integrated as part of the `pegasus`
gradle plugin. For details, see [Gradle build
integration](Gradle-build-integration).

<a id="wiki-Extras"></a>

### Rest.li-extras

Rest.li can be used with the D2 layer for dynamic discovery and
client-side load balancing. The use of D2 is normally transparent at the
Rest.li layer. However, for applications wishing to make more
sophisticated use of Rest.li and D2, the `restli-extras` module is
provided.

#### Scatter / Gather

The main feature supported in `restli-extras` is the ability to make
parallel “scatter/gather” requests across all the nodes in a cluster.
Currently, scatter/gather functionality is only supported for BATCH\_GET
methods.

Scatter/gather makes use of D2’s support for consistent hashing, to
ensure that a given key is routed to the same server node when possible.
The `ScatterGatherBuilder` interface can be used to partition a single
large `BatchGetRequest` into `N` `BatchGetRequests`, one for each node
in the cluster. The key partitioning is done according to the D2
consistent hashing policy, using a `KeyMapper` object obtained from the
D2 `Facilities` interface. Batch updates and deletes are also supported.
