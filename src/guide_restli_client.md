---
layout: guide
title: Rest.li client user guide
permalink: /user_guide/restli_client
excerpt: The Rest.li client framework provides support for accessing resources defined using Rest.li. The client framework consists of a request builder and a Rest client
index: 2
---

# Rest.li Client User Guide

## Contents

-   [Introduction](#introduction)
-   [Depending on a Service's Client
    Bindings](#depending-on-a-services-client-bindings)
-   [Depending on Data Templates](#depending-on-data-templates)
-   [Type-Safe Builders](#type-safe-builders)
-   [Built-in Request and RequestBuilder
    classes](#built-in-request-and-requestbuilder-classes)
-   [Restspec IDL](#restspec-idl)
-   [RestClient](#restclient)
-   [Request Options](#request-options)
-   [ParSeq Integrated Rest Client](#parseq-integrated-rest-client)
-   [Client Code Generator Tool](#client-code-generator-tool)
-   [Rest.li-extras](#restli-extras)

## Introduction

The Rest.li client framework provides support for accessing resources
defined using Rest.li. The client framework consists of two parts:

-   <code>RequestBuilder</code> classes, which provide an interface for
    creating REST requests to access a specific method of a resource.
    Request builders work entirely in-memory and do not communicate with
    remote endpoints.
-   <code>RestClient</code>, which provides an interface for sending
    requests to remote endpoints and receiving responses.

The request builder portion of the framework can be further divided into
two layers:

-   Built-in request builder classes, which provide generic support for
    accessing Rest.li resources. The built-in request builders
    understand how to construct requests for the different Rest.li
    resource methods, but they do not have knowledge of any specific
    resources or the methods they support. Therefore, the built-in
    request builders cannot validate that a request will be supported by
    the remote endpoint.
-   Type-safe request builder classes, which are generated from the
    server resource's IDL. The type-safe request builders are tailored
    to the specific resource methods supported by each resource. The
    type-safe builders provide an API that guides the developer towards
    constructing valid requests.

Most developers should work with the type-safe request builders, unless
there is a specific need to work with arbitrary resources whose
interfaces are unknown at the time the code is written.

<a id="wiki-CLientBindings"></a>

## Depending on a Service's Client Bindings

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
```groovy
...
dependencies {
// for a local project:
compile project(path: ':example-api', configuration: 'restClient')
// for a versioned artifact:
compile group: 'org.somegroup', name: 'example-api', version: '1.0', configuration: 'restClient'
}
...
```

<a id="wiki-DataTemplates"></a>

## Depending on Data Templates

To add a dependency to Java bindings for data models, add a
<code>dataTemplate</code> configured dependency in your build.gradle,
for example:

build.gradle:
```groovy
...
﻿dependencies {
    // for a local project:
    compile project(path: ':example-api', configuration: 'dataTemplate')
    // for a versioned artifact:
    compile group: 'org.somegroup', name: 'example-api', version: '1.0', configuration: 'dataTemplate'
}
...
```

Note that you should not usually need to add such a dependency when
adding a <code>restClient</code> dependency, as the
<code>restClient</code> should bring in the <code>dataTemplate</code>
transitively.

Note: If you are writing pegasus schemas (`.pdl` files) and need to add a
dependency on other pegasus schemas, you need to add a
<code>dataModel</code> dependency:

build.gradle
```groovy
...
    dataModel spec.product.example.data
...
```

<a id="wiki-Builders"></a>

## Type-Safe Builders

The client framework includes a code-generation tool that reads the IDL
and generates type-safe Java binding for each resource and its supported
methods. The bindings are represented as RequestBuilder classes.

<a id="wiki-BuilderFactories"></a>

### Resource Builder Factory

For each resource described in an IDL file, a corresponding builder
factory will be generated. For Rest.li version < 1.24.4, the builder
factory will be named `<Resource name>Builders`. For Rest.li version \>=
1.24.4, the builder factory is named `<Resource name>RequestBuilders`.
The factory contains a factory method for each resource method supported
by the resource. The factory method returns a request builder object
with type-safe bindings for the given method.

Standard CRUD methods are named `create()`, `get()`, `update()`,
`partialUpdate()`, `delete()`, and `batchGet()`. Action methods use the
name of the action, prefixed by "action", `action<ActionName>()`. Finder
methods use the name of the finder, prefixed by "findBy",
`findBy<FinderName>()`. BatchFinder methods use the name of the batchFinder, 
prefixed by "batchFindBy", `batchFindBy<BatchFinderName>()`.

An example for a resource named "Greetings" is shown below. Here is the
builder factory for Rest.li < 1.24.4:

```java
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
    public GreetingsBatchFindBySomeSearchCriteriaBuilder batchFindBySomeSearchCriteria()
}
```

Here is the builder factory for Rest.li >= 1.24.4:

```java
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
    public GreetingsBatchFindBySomeSearchCriteriaRequestBuilder batchFindBySomeSearchCriteria()
}
```

### GET Request Builder

In Rest.li < 1.24.4, the generated GET request builder for a resource
is named `<Resource>GetBuilder`. In Rest.li >= 1.24.4, the generated
GET request builder is named `<Resource>GetRequestBuilder`. Both support
the full interface of the built-in `GetRequestBuilder`.

If the resource class is a child resource, the generated builder will
include a type-safe path-key binding method for each of the resource's
ancestors (recursively following parent resources). Each binding method
is declared as:
```java
    public <BuilderType> <pathKeyName>Key(<KeyType> key);
```
e.g., for a parent pathKey named "groupId" of type `Integer` in the
"Contacts" resource, the binding method in Rest.li < 1.24.4 would be:
```java
    public ContactsGetBuilder groupIdKey(Integer key)
```

In Rest.li >= 1.24.4, it would be:
```java
    public ContactsGetRequestBuilder groupIdKey(Integer key)
```

### BATCH_GET Request Builder

In Rest.li < 1.24.4, the generated BATCH_GET request builder for a
resource is named `<Resource>BatchGetBuilder`. The generated builder
supports the full interface of the built-in `BatchGetRequestBuilder`.

In Rest.li >= 1.24.4, the generated BATCH_GET request builder for a
resource is named `<Resource>BatchGetRequestBuilder`. The generated
builder extends the built-in `BatchGetEntityRequestBuilder`.

When building requests with `BatchGetRequestBuilder`, use the
`buildKV()` method (`build()` is deprecated), for example:
```java
    new FortunesBuilders().batchGet().ids(...).buildKV()
```

When building requests with the `BatchGetEntityRequestBuilder`, the
`build()` method is used.

If the resource class is a child resource, the generated builder will
include a type-safe path-key binding method for each of the resource's
ancestors (recursively following parent resources). Each binding method
is declared as:
```java
    public <BuilderType> <pathKeyName>Key(<KeyType> key);
```
For example, a parent pathKey named "groupId" of type `Integer` in the
"Contacts" resource will have the binding method in Rest.li < 1.24.4 be
this:
```java
    public ContactsBatchGetBuilder groupIdKey(Integer key)
```

In Rest.li >= 1.24.4, it would be:
```java
    public ContactsBatchGetRequestBuilder groupIdKey(Integer key)
```

### FINDER Request Builder

In Rest.li < 1.24.4, the generated FINDER request builder for a
resource is named `<Resource>FindBy<FinderName>Builder`, while in
Rest.li >= 1.24.4 it is named
`<Resource>FindBy<FinderName>RequestBuilder`. Both builders support the
full interface of the built-in `FindRequestBuilder`.

If the resource class is a child resource, the generated builder will
include a type-safe path-key binding method for each of the resource's
ancestors (recursively following parent resources). Each binding method
is declared as:
```java
    public <BuilderType> <pathKeyName>Key(<KeyType> key);
```

The generated builder will contain a method to set each of the finder's
query parameters, of the form:
```java
    public <BuilderType> <paramName>Param(<ParamType> value);
```

The value **must** be non-null.

If the finder specifies `AssocKey` parameters, the builder will contain
a method to set each of them, of the form:
```java
    public <BuilderType> <assocKeyName>Key(<AssocKeyType> value);
```

### BATCH FINDER Request Builder

In Rest.li < 1.24.4, the generated BATCH_FINDER request builder for a
resource is named `<Resource>BatchFindBy<BatchFinderName>Builder`, while in
Rest.li >= 1.24.4 it is named
`<Resource>BatchFindBy<BatchFinderName>RequestBuilder`. Both builders support the
full interface of the built-in `BatchFindRequestBuilder`.

If the resource class is a child resource, the generated builder will
include a type-safe path-key binding method for each of the resource's
ancestors (recursively following parent resources). Each binding method
is declared as:
```java
    public <BuilderType> <pathKeyName>Key(<KeyType> key);
```

The generated builder will contain a method to set each of the batchFinder's
query parameters, of the form:
```java
    public <BuilderType> <paramName>Param(<ParamType> value);
```

The value **must** be non-null. For the batch query parameter, it also uses the form above
like the other regular parameters.

If the batchFinder specifies `AssocKey` parameters, the builder will contain
a method to set each of them, of the form:
```java
    public <BuilderType> <assocKeyName>Key(<AssocKeyType> value);
```
See more details about the BATCH_FINDER java request builder [here](/rest.li/batch_finder_resource_method#java-request-builders).

### CREATE Request Builder

In Rest.li < 1.24.4, the generated CREATE request builder for a
resource is named `<Resource>CreateBuilder`. The generated builder
supports the full interface of the built-in `CreateRequestBuilder`.

In Rest.li >= 1.24.4, the generated CREATE request builder for a
resource is named `<Resource>CreateRequestBuilder`. The generated
builder extends the built-in `CreateIdRequestBuilder`.

If the resource class is a child resource, the generated builder will
include a type-safe path-key binding method for each of the resource's
ancestors (recursively following parent resources). Each binding method
is declared as:
```java
    public <BuilderType> <pathKeyName>Key(<KeyType> key);
```

If @`ReturnEntity` annotation is specified for CREATE implementation, an
additional `CreateAndGet` request builder will be generated. Note that
`Create` request builder is still available so that adding
@`ReturnEntity` is backward compatible for a Java client.
```java
public class <Resource>RequestBuilders
{
...
    public <Resource>CreateRequestBuilder create();
    public <Resource>CreateAndGetRequestBuilder createAndGet();
...
}
```
The response will be of type `IdEntityResponse<K, V>` which has a
`getEntity()` method:
```java
...
    // "greeting" is defined in previous context\
    CreateIdEntityRequest\<Long, Greeting\> createIdEntityRequest =
    builders.createAndGet().input(greeting).build();
    Response\<IdEntityResponse\<Long, Greeting\>\> response =
    restClient.sendRequest(createIdEntityRequest).getResponse();
    ...
    IdEntityResponse\<Long, Greeting\> idEntityResponse =
    response.getEntity();
    // The returned entity from server\
    Greeting resultEntity = idEntityResponse.getEntity();
```

The projection for returned entity is supported.
```java
...
    // "greeting" is defined in previous context\
    CreateIdEntityRequest\<Long, Greeting\> createIdEntityRequest =
    builders.createAndGet().fields(Greeting.fields().tone(),
    Greeting.fields().id()).input(greeting).build();
```

### BATCH_CREATE Request Builder

In Rest.li < 1.24.4, the generated BATCH_CREATE request builder for a
resource is named `<Resource>BatchCreateBuilder`. The generated builder
supports the full interface of the built-in `BatchCreateRequestBuilder`.

In Rest.li >= 1.24.4, the generated BATCH_CREATE request builder for a
resource is named `<Resource>BatchCreateRequestBuilder`. The generated
builder extends the built-in `BatchCreateIdRequestBuilder`.

If the resource class is a child resource, the generated builder will
include a type-safe path-key binding method for each of the resource's
ancestors (recursively following parent resources). Each binding method
is declared as:
```java
    public <BuilderType> <pathKeyName>Key(<KeyType> key);
```

If @`ReturnEntity` annotation is specified for BATCH_CREATE
implementation, an additional `BatchCreateAndGet` request builder will
be generated. Note that `BatchCreate` request builder will still be
generated so that adding @`ReturnEntity` annotation is backward
compatible for a Java client.
```java
public class <Resource>RequestBuilders\
{
    ...
    public <Resource>BatchCreateRequestBuilder batchCreate();
    public <Resource>BatchCreateAndGetRequestBuilder batchCreateAndGet();
    ...
}
```

The response will be of type `BatchCreateIdEntityResponse` whose
elements are `CreateIdEntityStatus` object containing the returned
entity. Here is a code example.
```java
// "greetings" is defined in previous context
BatchCreateIdEntityRequest<Long, Greeting> batchCreateIdEntityRequest = builders.batchCreateAndGet().inputs(greetings).build();
Response\<BatchCreateIdEntityResponse<Long, Greeting>> response = restClient.sendRequest(batchCreateIdEntityRequest).getResponse();
BatchCreateIdEntityResponse<Long, Greeting> entityResponses = response.getEntity();
for (CreateIdEntityStatus<?, ?> individualResponse : entityResponses.getElements())
{
    Greeting entity = (Greeting)individualResponse.getEntity();// The returned individual entity from server
}
```

The projection for returned entities is supported.

```java
...
// "greetings" is defined as a list of greeting in previous context\
BatchCreateIdEntityRequest<Long, Greeting> batchCreateIdEntityRequest = builders.batchCreateAndGet().fields(Greeting.fields().tone(),
Greeting.fields().id()).inputs(greetings).build();
```

### PARTIAL_UPDATE Request Builder

In Rest.li < 1.24.4, the generated PARTIAL_UPDATE request builder for
a resource is named `<Resource>PartialUpdateBuilder`. Whereas in Rest.li >= 1.24.4, it is called `<Resource>PartialUpdateRequestBuilder`. Both
builders support the full interface of the built-in
`PartialUpdateRequestBuilder`.

If the resource class is a child resource, the generated builder will
include a type-safe path-key binding method for each of the resource's
ancestors (recursively following parent resources). Each binding method
is declared as:
```java
public <BuilderType> <pathKeyName>Key(<KeyType> key);
```

See [Creating partial
updates](/rest.li/user_guide/restli_server#creating-partial-updates)
for details on how to create a request for a partial update.

If the PARTIAL_UPDATE method is annotated with a @`ReturnEntity` annotation,
an additional `PartialUpdateAndGet` request builder will be generated. Note that
the `PartialUpdate` request builder is still available so that adding
@`ReturnEntity` is backward compatible for Java clients.

```java
public class <Resource>RequestBuilders
{
...
    public <Resource>PartialUpdateRequestBuilder partialUpdate();
    public <Resource>PartialUpdateAndGetRequestBuilder partialUpdateAndGet();
...
}
```

The returned entity will be directly accessible from the response using `getEntity()`:

```java
...
// "greeting" is defined in previous context
PartialUpdateEntityRequest<Greeting> partialUpdateEntityRequest = builders.partialUpdateAndGet()
    .id(1L)
    .input(greeting)
    .build();
Response<Greeting> response = restClient.sendRequest(partialUpdateEntityRequest).getResponse();
...
// The returned entity from server
Greeting resultEntity = response.getEntity();
```

Using projections on the returned entity is supported:

```java
...
// "greeting" is defined in previous context\
PartialUpdateEntityRequest<Greeting> partialUpdateEntityRequest = builders.partialUpdateAndGet()
        .fields(Greeting.fields().tone(), Greeting.fields().id())
        .id(1L)
        .input(greeting)
        .build();
```

### BATCH_PARTIAL_UPDATE Request Builder

In Rest.li < 1.24.4, the generated BATCH_PARTIAL_UPDATE request
builder for a resource is named `<Resource>BatchPartialUpdateBuilder`.
Whereas in Rest.li >= 1.24.4, it is
`<Resource>BatchPartialUpdateRequestBuilder`. Both support the full
interface of the built-in `BatchPartialUpdateRequestBuilder`.

If the resource class is a child resource, the generated builder will
include a type-safe path-key binding method for each of the resource's
ancestors (recursively following parent resources). Each binding method
is declared as:
```java
public <BuilderType> <pathKeyName>Key(<KeyType> key);
```

### UPDATE Request Builder

In Rest.li < 1.24.4, the generated UPDATE request builder for a
resource is named `<Resource>UpdateBuilder`. Whereas in Rest.li \>=
1.24.4, it is named `<Resource>UpdateRequestBuilder`. Both builders
support the full interface of the built-in `UpdateRequestBuilder`.

If the resource class is a child resource, the generated builder will
include a type-safe path-key binding method for each of the resource's
ancestors (recursively following parent resources). Each binding method
is declared as:
```java
public <BuilderType> <pathKeyName>Key(<KeyType> key);
```

### BATCH_UPDATE Request Builder

In Rest.li < 1.24.4, the generated BATCH_UPDATE request builder for a
resource is named `<Resource>BatchUpdateBuilder`. Whereas in Rest.li >=
1.24.4, it is named `<Resource>BatchUpdateRequestBuilder`. Both builders
support the full interface of the built-in `BatchUpdateRequestBuilder`.

If the resource class is a child resource, the generated builder will
include a type-safe path-key binding method for each of the resource's
ancestors (recursively following parent resources). Each binding method
is declared as:
```java
public <BuilderType> <pathKeyName>Key(<KeyType> key);
```

### DELETE Request Builder

The generated DELETE request builder for a resource is named
`<Resource>DeleteBuilder`. The generated builder supports the full
interface of the built-in `DeleteRequestBuilder`.

If the resource class is a child resource, the generated builder will
include a type-safe path-key binding method for each of the resource's
ancestors (recursively following parent resources). Each binding method
is declared as:
```java
public <BuilderType> <pathKeyName>Key(<KeyType> key);
```

### BATCH_DELETE Request Builder

In Rest.li < 1.24.4, the generated BATCH_DELETE request builder for a
resource is named `<Resource>BatchDeleteBuilder`. Whereas in Rest.li \>=
1.24.4, the builder is called `<Resource>BatchDeleteRequestBuilder`.
Both builders support the full interface of the built-in
`BatchDeleteRequestBuilder`.

If the resource class is a child resource, the generated builder will
include a type-safe path-key binding method for each of the resource's
ancestors (recursively following parent resources). Each binding method
is declared as:
```java
public <BuilderType> <pathKeyName>Key(<KeyType> key);
```

### ACTION Request Builder

In Rest.li < 1.24.4, the generated ACTION request builder for a
resource is named `<Resource>Do<ActionName>Builder`. Whereas in Rest.li
>= 1.24.4, it is `<Resource>Do<ActionName>RequestBuilder`. Both
builders support the full interface of the built-in
`ActionRequestBuilder`.

If the resource class is a child resource, the generated builder will
include a type-safe path-key binding method for each of the resource's
ancestors (recursively following parent resources). Each binding method
is declared as:
```java
public <BuilderType> <pathKeyName>Key(<KeyType> key);
```

The generated builder will contain a method to set each of the action's
parameters. It Rest.li < 1.24.4, it is of the form:
```java
public <BuilderType> param<ParamName>(<ParamType> value);
```

In Rest.li >= 1.24.4, it is of the form:
```java
public <BuilderType> <paramName>Param(<ParamType> value);
```

The value **must** be non-null.

<a id="wiki-CallingSubResources"></a>

### Calling Sub-Resources

To call a subresource of the fortunes resource, for example:

```
GET /fortunes/1/subresource/100
```

The parent keys can be specified by calling generated setters on the
builder. In this case, the `fortunesIdKey()` method, for example:

```java
new SubresourceBuilders().get().fortunesIdKey(1l).id(100l).build()
```

Parent path keys can also be set directly builder classes using the
`setPathKey()` method on the builders classes, for example:

```java
.setPathKey("dest", "dest").setPathKey("src", "src")
```

<a id="wiki-BuiltinRequestBuilders"></a>

## Built-in Request and RequestBuilder classes

The built-in RequestBuilder classes provide generic support for
constructing Rest.li requests. This layer is independent of the IDL for
specific resources; therefore, the interface does not enforce that only
"valid" requests are constructed.

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

-   `header(String key, String value)` - sets a request header
-   `addCookie(HttpCookie cookie)` - adds a cookie
-   `id(K id)` - sets the entity key for the resource
-   `ids(Collection<K> ids)` - sets a list of entity keys
-   `name(String name)` - sets the name for a named resource method
-   `setParam(String name, Object value)` - sets a query param named
    `name` to `value`
-   `addParam(String name, Object value)` - adds `value` to the query
    param named `name`
-   `assocKey(String key, Object value)` - sets an association key
    parameter
-   `pathKey(String key, Object value)` - sets a path key parameter
    (entity key of a parent resource)
-   `paginate(int start, int count)` - sets pagination parameters
-   `fields(PathSpec... fieldPaths)` - sets the fields projection mask
-   `input(V entity)` - sets the input payload for the request
-   `inputs(Map<K, V> entities)` - sets the input payloads for batch
    requests
-   `returnEntity(boolean value)` - sets the [`$returnEntity` query parameter](/rest.li/spec/return_entity#client-specified-behavior)

The following table summarizes the methods supported by each
RequestBuilder type.

| Request Builder    | header | id  | ids | name | setParam | addParam | assocKey | pathKey | paginate | fields | input | inputs | returnEntity |
|--------------------|--------|-----|-----|------|----------|----------|----------|---------|----------|--------|-------|--------|--------------|
| Action             | -      | -   |     | -    | -        | -        |          | -       |          |        |       |        |              |
| Find               | -      |     |     | -    | -        | -        | -        | -       | -        | -      |       |        |              |
| Get                | -      | -\* |     |      | -        | -        |          | -       |          | -      |       |        |              |
| Create             | -      |     |     |      | -        | -        |          | -       |          |        | -     |        | -\*\*        |
| Delete             | -      | -\* |     |      | -        | -        |          | -       |          |        |       |        |              |
| PartialUpdate      | -      | -   |     |      | -        | -        |          | -       |          |        | -     |        | -\*\*        |
| Update             | -      | -\* |     |      | -        | -        |          | -       |          |        | -     |        |              |
| BatchGet           | -      |     | -   |      | -        | -        |          | -       |          | -      |       |        |              |
| BatchCreate        | -      |     |     |      | -        | -        |          | -       |          |        |       | -      | -\*\*        |
| BatchDelete        | -      |     | -   |      | -        | -        |          | -       |          |        |       |        |              |
| BatchPartialUpdate | -      |     |     |      | -        | -        |          | -       |          |        |       | -      | -\*\*        |
| BatchUpdate        | -      |     |     |      | -        | -        |          | -       |          |        |       | -      |              |
| BatchFinder        | -      |     |     | -    | -        | -        | -        | -       | -        | -      |       |        |              |

\* It is not supported, if the method is defined on a simple resource.

\*\* Supported if the resource method is annotated with @`ReturnEntity`. See [more about this feature](/rest.li/spec/return_entity).

Refer to the JavaDocs for specific details of RequestBuilder and Request
interfaces.

<a id="RestspecIDL"></a>

<a id="wiki-IDL"></a>

## Restspec IDL

Rest.li uses a custom format called REST Specification (Restspec) as its
interface description language (IDL). The Restspec provides a succinct
description of the URI paths, HTTP methods, query parameters, and JSON
format. Together, these form the interface contract between the server
and the client.

Restspec files are JSON format and use the file suffix \*.restspec.json.

At a high level, the restspec contains the following information:

-   name of the resource
-   path to the resource
-   schema type (value type) of the resource
-   resource pattern (collection / simple / association / actionsSet)
-   name and type of the resource key(s)
-   list of supported CRUD methods (CREATE, GET, UPDATE,
    PARTIAL_UPDATE, DELETE, and corresponding batch methods)
-   description of each FINDER, including
    -   name
    -   parameter names, types, and optionality
    -   response metadata type (if applicable)
-   description of each BATCH_FINDER, including
    -   name
    -   parameter names, types, and optionality
    -   batch parameter name
    -   response metadata type (if applicable)
-   description of each ACTION, including
    -   name
    -   parameter names, types, and optionality
    -   response type
    -   exception types
-   a description of each subresource, containing the information
    described above

Additional details on the Restspec format may be found in the
[design documents](/rest.li/spec/restspec_format).
The Restspec format is formally described by the data schema schema files in
"com.linkedin.restli.restspec.* " distributed in the restli-common module.

<a id="IDLGeneratorTool"></a>

### IDL Generator Tool

The IDL generator is used to create the language-independent interface
description (IDL) from Rest.li resource implementations (annotated Java
code).

The IDL generator is available as part of the restli-tools JAR, as the
`com.linkedin.restli.tools.idlgen.RestLiResourceModelExporterCmdLineApp`
class.

For details on how to use the IDL Generator, see [Gradle build integration](/rest.li/setup/gradle).

<a id="Client"></a>
<a id="RestClient"></a>

<a id="wiki-RestClient"></a>

## RestClient

`RestClient` encapsulates the communication with the remote resource.
`RestClient` accepts a `Request` object as input and provides a
`Response` object as output. The `Request` objects should usually be
built using the [generated type-safe client builders](#type-safe-builders). Since the
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

### ResponseFuture

The `RestClient` future-based interface returns `ResponseFuture`, which
implements the standard `Future` interface and extends it with a
`getResponse()` method. The advantage of `getResponse()` is that it is
aware of Rest.li exception semantics, throwing
`RemoteInvocationException` instead of `ExecutionException`.

### Making requests using the RestClient and generated RequestBuilders

The standard pattern for making requests using the RestClient is as
follows:

1.  Build the request using the generated request builders
2.  Use the `RestClient#sendRequest` method to send the request and get
    back a `ResponseFuture`
3.  Call `ResponseFuture#getResponse` to get the `Response` that the
    server returned. **Note that this call blocks until the server
    responds or there is an error!**

Here is a more concrete example, where a client is making a GET request
to the /greetings resource -

```java
// First we build the Request. builders is either a GreetingsBuilder or GreetingsRequestBuilder
Request<Greeting> getRequest = builders.get().id(id).build();

// Send the Request and get back a ResponseFuture representing the response. This call is non-blocking.
ResponseFuture<Greeting> responseFuture = restClient.sendRequest(getRequest);

// Like the standard Java Future semantics, calling getResponse() here IS blocking!
Response<Greeting> getResponse = responseFuture.getResponse();

// Get the entity from the Response
Greeting responseGreeting = getResponse.getEntity();
```

Look at the `com.linkedin.restli.client.Response` interface to see what
other methods are available for use.

### Request API changes in Rest.li >= 1.24.4

There are two major changes:

-   `CreateIdRequestBuilder`, which is the super class for all CREATE
    request builders, now returns a `CreateIdRequest<K, V>` when the
    `build()` method is called.
-   `BatchCreateIdRequestBuilder`, which is the super class for all
    BATCH_CREATE request builders, now returns a
    `BatchCreateIdRequest<K, V>` when the `build()` method is called.

### Response API Changes in Rest.li >= 1.24.4

Starting with Rest.li 1.24.4, we have introduced a few changes to the
`Response` API.

#### Response from a CREATE and BATCH_CREATE Request

As mentioned in the section above, calling `build()` on a
`CreateIdRequestBuilder` gives us a `CreateIdRequest<K, V>`.
When this is sent using a `RestClient` we get back (after calling
`sendRequest(...).getResponse().getEntity()`) an `IdResponse<K>` that
gives us a single, strongly-typed key.

Similarly, when a `RestClient` is used to send out a
`BatchCreateIdRequest<K, V>` we get back a `BatchCreateIdResponse<K>`,
which contains a `List` of strongly-typed keys.

#### Response from a BATCH_GET Request

When a `BatchGetEntityRequest` is sent using a `RestClient` we get back
(after calling `sendRequest(...).getResponse().getEntity()`) a
`BatchKVResponse<K,EntityResponse<V>>` where `K` is the key type and `V`
is the value (which extends `RecordTemplate`) for the resource we are
calling.

`EntityResponse` is a `RecordTemplate` with three fields:

-   `entity` provides an entity record if the server resource finds a
    corresponding value for the key;
-   `status` provides an optional status code;
-   `error` provides the error details from the server resource
    (generally `entity` and `error` are mutually exclusive as `null`,
    but it is ultimately up to the server resource).

Note that since `EntityResponse` contains an `error` field, the
`Map<K, V>` returned by `BatchEntityResponse#getResults()` contains both
successful as well as failed entries. `BatchEntityResponse#getErrors()`
will only return failed entries.

#### Response from a BATCH_UPDATE, BATCH_PARTIAL_UPDATE, and BATCH_DELETE Request

The response type of the `BatchUpdate` series methods are not changed.
However, similar to `EntityResponse`, we added a new `error` field to
`UpdateStatus` (the value type of the `BatchUpdate` series methods).
Furthermore, `BatchKVResponse<K, UpdateStatus>#getResults()` will
returns both successful as well as failed entries. `getErrors()` will
only return failed entries.

### Error Semantics

The following diagram illustrates the request/response flow for a
client/server interaction. The call may fail at any point during this
flow, as described below.

<center>
<b>Rest.li Request Flow</b><br><img src="/rest.li/images/RequestFlow.png">
</center>

The following list describes the failures scenarios as observed by a
client calling `ResponseFuture.getResponse()`

Failure Scenarios

-   Client Framework (outbound)
    -   `ServiceUnavailableException` - if D2 cannot locate a node for
        the requested service URI
    -   `RemoteInvocationException` - if R2 cannot connect to the remote
        endpoint or send the request
-   Network Transport (outbound)
    -   `TimeoutException` - if a network failure prevents the request
        from reaching the server
-   Server Framework (inbound)
    -   `RestLiResponseException` - if an error occurs within the
        framework, resulting in a non-200 response
    -   `TimeoutException` - if an error prevents the server from
        sending a response
-   Server Application
    -   `RestLiResponseException` - if the application throws an
        exception the server framework will convert it into a non-200
        response
    -   `TimeoutException` - if an application error prevents the server
        from sending a response in a timely manner
-   Server Framework (outbound)
    -   `RestLiResponseException` - if an error occurs within the
        framework, resulting in a non-200 response
    -   `TimeoutException` - if an error prevents the server from
        sending a response
-   Network Transport (inbound)
    -   `TimeoutException` - if a network failure prevents the response
        from reaching the client
-   Client Framework (inbound)
    -   `RestLiDecodingException` - if the client framework cannot
        decode the response document
    -   `RemoteInvocationException` - if an error occurs within the
        client framework while processing the response.

<a id="wiki-ParSeq"></a>

## Request Options

Each request sent to a Rest.li server can be configured with custom
options by using an instance of `RestliRequestOptions`.
`RestliRequestOptionsBuilder` is required to construct an instance of
`RestliRequestOptions`. Once constructed, an instance of
`RestliRequestOptions` can then be passed to Rest.li generated type-safe
request builders. Subsequently, `RestClient` will construct a
`RestRequest` based on these custom options to send to the Rest.li
server. Currently we support specifying the following custom options per
Request:

### ProtocolVersionOption

When sending a Request, the caller can specify what protocol version
option is to be used. The available ProtocolVersionOption(s) are:

#### FORCE_USE_NEXT

Use the next version of the Rest.li protocol to encode requests,
regardless of the version running on the server. The next version of the
Rest.li protocol is the version currently under development. This option
should typically NOT be used for production services.
**CAUTION**: this can cause requests to fail if the server does not
understand the next version of the protocol.
"Next version" is defined as
`com.linkedin.restli.internal.common.AllProtocolVersions.NEXT_PROTOCOL_VERSION`.

#### FORCE_USE_LATEST

Use the latest version of the Rest.li protocol to encode requests,
regardless of the version running on the server.
**CAUTION**: this can cause requests to fail if the server does not
understand the latest version of the protocol. "Latest version" is defined as
`com.linkedin.restli.internal.common.AllProtocolVersions.LATEST_PROTOCOL_VERSION`.

#### USE_LATEST_IF_AVAILABLE

Use the latest version of the Rest.li protocol if the server supports
it. If the server version is less than the baseline Rest.li protocol
version then fail the request. If the server version is greater than the
next Rest.li protocol version then fail the request. If the server is
between the baseline and the latest version then use the server version
to encode the request. If the server version is greater than or equal to
the latest protocol version then use that to encode the request.

-   "Baseline version" is defined as
    `com.linkedin.restli.internal.common.AllProtocolVersions.BASELINE_PROTOCOL_VERSION`.
-   "Latest version" is defined as
    `com.linkedin.restli.internal.common.AllProtocolVersions.LATEST_PROTOCOL_VERSION`.
-   "Next version" is defined as
    `com.linkedin.restli.internal.common.AllProtocolVersions.NEXT_PROTOCOL_VERSION`.

**CAUTION**: Please be very careful setting the non-default
**FORCE_USE_NEXT** or **FORCE_USE_LATEST** options as the protocol
version option in `RestLiRequestOptions`, since they may cause requests
to fail if the server does not understand the desired protocol request.
This form of configuration is normally used in migration cases.

### CompressionOption

When sending a Request, the caller can force compression on or off for
each request.

#### FORCE_ON

Compress the request.

#### FORCE_OFF

Do not compress the request.

If `null` is specified, Rest.li `ClientCompressionFilter` will determine
whether we need to do client side compression based on request entity
length.

### ContentType

When sending a Request, the caller can also specify what content type is
to be used. The specified value will be set to the HTTP header
"Content-Type" for the request.

#### JSON

This will set "Content-Type" header value as "application/json".

#### PSON

This will set "Content-Type" header value as "application/x-pson"

**NOTE**: Besides `RestliRequestOption`, the caller can also specify the
`ContentType` through the `RestClient` constructor by passing the
contentType parameter (as shown below), which will apply to all requests
sent through that client instance.

```java
public RestClient(Client client, String uriPrefix, ContentType contentType, List<AcceptType> acceptTypes)
```

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

### AcceptType

When sending a Request, the caller can also specify what media types it
can accept. The specified value will be set to the HTTP header "Accept"
for the request. If more than one AcceptType is specified, we will
generate an Accept header by appending each media type by a "q"
parameter for indicating a relative quality factor. For example:

    Accept: application/*; q=0.2, application/json

Quality factors allow the user or user agent to indicate the relative
degree of preference for that media type, using the scale from 0 to 1.
The default value is q=1. In our case, the quality factor generated is
based on the order of each accept type we specified in the list. See
http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html for details.

#### JSON

This will accept media type of "application/json".

#### PSON

This will accept media type of "application/x-pson".

#### ANY

This will accept any media type.

**NOTE**: Besides `RestliRequestOption`, the caller can also specify
AcceptType through the `RestClient` constructor by passing the
acceptTypes parameter (as shown below), which will apply to all requests
sent through that client instance.

```java
public RestClient(Client client, String uriPrefix, List<AcceptType> acceptTypes)
public RestClient(Client client, String uriPrefix, ContentType contentType, List<AcceptType> acceptTypes)
```
However, this form of configuration has been DEPRECATED. Please use
`RestliRequestOptions` instead to set such custom options. In cases
where the caller has configured accept types from multiple places,
RestClient will resolve request accept type based on the following
precedence order:

1.  Request header.
2.  RestliRequestOptions.
3.  RestClient configuration.

If `null` is specified for the accept type from these 3 sources,
`RestClient` will not set the HTTP "Accept" header. If no accept header
field is present, then it is assumed by the Rest.li server that the
client accepts all media types based on the HTTP Spec (RFC 2616).

If `RestliRequestOptions` is not set, or is set to null, the request
builders will use
`RestliRequestOptions.DEFAULT_OPTIONS(ProtocolVersionOption.USE_LATEST_IF_AVAILABLE, null /*compression*/, null /*content type*/, null /*accept type*/)`
to generate the Request.

## ParSeq Integrated Rest Client

The `ParSeqRestClient` wrapper facilitates usage with ParSeq by
providing methods that return a `Promise` or a `Task`. For example,
users can create multiple requests and use ParSeq to send them in
parallel. This feature is independent of the asynchronous resources; in
particular, the server resource does not have to be asynchronous.

```java
ParSeqRestClient client = new ParSeqRestClient(plain rest client);
// send some requests in parallel
Task<Response<?>> task1 = client.createTask(request1);
Task<Response<?>> task2 = client.createTask(request2);
Task<Response<?>> combineResults = ...;
// after we get our parallel requests, combine them
engine.run(Tasks.seq(Tasks.par(task1, task2), combineResults))
```
Users of `createTask` are required to instantiate their own ParSeq
engine and start the task themselves.

<a id="ClientCodeGeneratorTool"></a>

<a id="wiki-CodeGenTool"></a>

## Client Code Generator Tool

As described above, the Rest.li client framework includes a
code-generation tool that creates type-safe Request Builder classes
based on resource IDL files.

The code generator is available as part of the restli-tools JAR, as
`com.linkedin.restli.tools.clientgen.RestRequestBuilderGenerator`. The
generator is invoked by providing an output directory and a list of
input IDL files as command-line arguments.

In addition, the generator recognizes the following system properties:

-   `generator.rest.generate.datatemplates` - boolean property
    indicating whether the generator should generate Java RecordTemplate
    classes for the data schemas referenced by the IDL file.
-   `generator.default.package` - the default package name for generated
    classes
-   `generator.resolver.path` - a colon-separated list of filesystem
    paths to search when resolving references to named schemas. See
    "Data Template Generator" for more details.

The Rest.li client code generator is integrated as part of the `pegasus`
gradle plugin. For details, see [Gradle build integration](/rest.li/setup/gradle).

<a id="wiki-Extras"></a>

## Rest.li-extras

Rest.li can be used with the D2 layer for dynamic discovery and
client-side load balancing. The use of D2 is normally transparent at the
Rest.li layer. However, for applications wishing to make more
sophisticated use of Rest.li and D2, the `restli-extras` module is
provided.

### Scatter / Gather

The main feature supported in `restli-extras` is the ability to make
parallel "scatter/gather" requests across all the nodes in a cluster.
Currently, scatter/gather functionality is only supported for BATCH_GET
methods.

Scatter/gather makes use of D2's support for consistent hashing, to
ensure that a given key is routed to the same server node when possible.
The `ScatterGatherBuilder` interface can be used to partition a single
large `BatchGetRequest` into `N` `BatchGetRequests`, one for each node
in the cluster. The key partitioning is done according to the D2
consistent hashing policy, using a `KeyMapper` object obtained from the
D2 `Facilities` interface. Batch updates and deletes are also supported.
