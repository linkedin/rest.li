---
layout: guide
title: Rest.li server user guide
permalink: /user_guide/restli_server
excerpt: This document describes Rest.li support for implementing servers.
index: 2
---

# Rest.li Server User Guide

## Contents

-   [Runtimes](#runtimes)
-   [R2 Filter Configuration](#r2-filter-configuration)
-   [Defining Data Models](#defining-data-models)
-   [Writing Resources](#writing-resources)
-   [Documenting Resources](#documenting-resources)
-   [Resource Annotations](#resource-annotations)
-   [Asynchronous Resources](#asynchronous-resources)
-   [Sub-Resources](#sub-resources)
-   [Resource Methods](#resource-methods)
    -  [Get](#get)
    -  [Batch Get](#batch_get)
    -  [Get All](#get_all)
    -  [Finder](#finder)
    -  [Batch Finder](#batch-finder)
    -  [Create](#create)
    -  [Batch Create](#batch_create)
    -  [Update](#update)
    -  [Batch Update](#batch_update)
    -  [Partial Update](#partial_update)
    -  [Batch Partial Update](#batch_partial_update)
    -  [Delete](#delete)
    -  [Batch Delete](#batch_delete)
    -  [Action](#action)
-   [ResourceContext](#resourcecontext)
-   [Resource Templates](#resource-templates)
-   [Free-form Resources](#free-form-resources)
-   [Returning Errors](#returning-errors)
-   [Field Projection](#field-projection)
-   [Collection Pagination](#collection-pagination)
-   [Dependency Injection](#dependency-injection)
-   [Online Documentation](#online-documentation)

This document describes Rest.li support for implementing servers. 

## Runtimes

Rest.li supports the following runtimes:

1.  [Servlet containers](/rest.li/Rest_li-with-Servlet-Containers) (for example, Jetty)
2.  [Netty](/rest.li/Rest_li-with-Netty)

## R2 Filter Configuration

Rest.li servers can be configured with different R2 filters, according
to your use case. How the filters are configured depends on which
dependency injection framework (if any) you are using. For example, see [Compression](/rest.li/Compression) to understand how to configure a server for
compression. Another example is to add a
`SimpleLoggingFilter` with Spring, which requires you to do
the following (full file
<a href="https://github.com/linkedin/rest.li/blob/master/examples/spring-server/server/src/main/webapp/WEB-INF/beans.xml">here</a>):

```xml
<!-- Example of how to add filters; here we'll enable logging and snappy compression support -->
<bean id="loggingFilter" class="com.linkedin.r2.filter.logging.SimpleLoggingFilter" />
```

[Other R2 filters](https://github.com/linkedin/rest.li/tree/master/r2-core/src/main/java/com/linkedin/r2/filter)
can also be configured in a similar way.

<a id="wiki-DefiningDataModels"></a>

## Defining Data Models

The first step in building a Rest.li application is to define your data
schema using [Pegasus Data Schemas](/rest.li/pdl_schema). The
Pegasus Data Schema format uses a simple Avro-like syntax to define your
data model in a language-independent way. Rest.li provides code
generators to create Java classes that implement your data model.

<a id="wiki-WritingResources"></a>

## Writing Resources

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

-   The class must have the default constructor. The default constructor
    will be used by Rest.li to instantiate the resource class for each
    request execution.
-   The class must be annotated with one of the Resource Annotations.
-   If required by the annotation, the class must `implement` the
    necessary Resource interface or extend one of the convenience base
    classes that implements the interface.
-   To expose methods on the resource, each method must either:
    -   Override a standard method from the Resource interface
    -   Include the necessary method-level annotation as described in
        the Resource Methods section below
-   For each exposed method, each parameter must either:
    -   Be part of the standard signature, for overridden methods
    -   Be annotated with one of the parameter-level annotations
        described for the Resource Method.
-   All documentation is written in the resource source file using
    javadoc (or scaladoc, see below for details).

Here is a simple example of a Resource class. It extends a convenience
base class, uses an annotation to define a REST end-point ("fortunes"),
and provides a GET endpoint by overriding the standard signature of the
`get()` method of the base class:

```java
/**
 * A collection of fortunes, keyed by random number.
 */
@RestLiCollection(name = "fortunes", namespace = "com.example.fortune")
public class FortunesResource extends CollectionResourceTemplate<Long, Fortune>
{
  /**
   * Gets a fortune for a random number.
   */
  @Override
  public Fortune get(Long key)
  {
    // retrieve data and return a Fortune object ...
  }
}
```

This interface implements an HTTP GET:

```
> GET /fortunes/1
...
< { "fortune": "Your lucky color is purple" }
```

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

## Documenting Resources

Rest.li resources are documented in the resource source files using
javadoc. When writing resources, developers simply add any documentation
as javadoc to their java resource classes, methods, and method params.
It is recommended that developers follow the [javadoc style
guidelines](http://www.oracle.com/technetwork/java/javase/documentation/index-137868.html)
for all formatting so that their documentation is displayed correctly.

Rest.li will automatically extract this javadoc and include it in all
generated "interface definitions" (.restspec.json files) and generated
client bindings. This approach allows REST API clients and tools to
easily gain access to the documentation. For example, [Rest.li API
Hub](https://github.com/linkedin/rest.li-api-hub) is an opensource web
UI that displays REST API documentation, including all javadoc, for
Rest.li APIs.


<a id="wiki-ResourceAnnotations"></a>

## Resource Annotations

Resource annotations are used to mark and register a class as providing
as Rest.li resource. One of a number of annotations may be used,
depending on the [Interface Pattern](/rest.li/modeling/modeling#collection)
the resource is intended to implement. Briefly, here are the options:

<a id="wiki-ResourceTypes"></a>


| Resource Type | Annotation | Interface or Base Class |
| ------------- | -----------| ------------------------|
| Collection    | @RestLiCollection     | For simple keys, implement `CollectionResource` or extend `CollectionResourceTemplate`. For complex key implement `ComplexKeyResource`, extend `ComplexKeyResourceTemplate`, or implement `KeyValueResource` for use cases requiring extensive customization |
| Simple        | @RestLiSimpleResource | Implement `SimpleResource`, extend `SimpleResourceTemplate` or implement `SingleObjectResource` for use cases requiring extensive customization |
| Association   | @RestLiAssociation    | Implement `AssociationResource`, extend `AssociationResourceTemplate`, or implement `KeyValueResource` for use cases requiring extensive customization  |
| Actions       | @RestLiActions        | N/A |

#### @RestLiCollection

The `@RestLiCollection` annotation is applied to classes to mark them as
providing a Rest.li collection resource. Collection resources model a
collection of entities, where each entity is referenced by a key. See
[Collection Resource Pattern](/rest.li/modeling/modeling#collection) for more details.



The supported annotation parameters are:

-   `name` - required, defines the name of the resource.
-   `namespace` - optional, defines the namespace for the resource.
    Default is empty (root namespace). The namespace of the resource
    appears in the IDL, and is used as the package name for the
    generated client builders.
-   `keyName` - optional, defines the key name for the resource. Default
    is "<ResourceName&gt;Id".
-   `parent` - optional, defines the parent resource for this resource.
    Default is root.

Classes annotated with `@RestLiCollection` must implement the
`CollectionResource` interface. The `CollectionResource` interface
requires two generic type parameters:

-   `K` - the key type for the resource.
-   `V` - the value type for the resource (also known as, the entity
    type).

The key type for a collection resource must be one of:

-   `String`
-   `Boolean`
-   `Integer`
-   `Long`
-   A Pegasus Enum (any enum defined in a `.pdl` schema)
-   Custom Type (see below for details)
-   Complex Key (A pegasus record, any subclass of `RecordTemplate`
    generated from a `.pdl` schema)

The value type for a collection resource must be a pegasus record, any
subclass of `RecordTemplate` generated from a `.pdl` schema.

For convenience, collection resources may extend
`CollectionResourceTemplate` rather than directly implementing the
`CollectionResource` interface.

Example:

```java
@RestLiCollection(name = "fortunes", namespace = "com.example.fortune", keyName = "fortuneId")
public class FortunesResource extends CollectionResourceTemplate<Long, Fortune>
{
  ...
}
```

<a id="wiki-AsynchResources"></a>

## Asynchronous Resources

Rest.li allows resources to return results asynchronously through a
[ParSeq](https://github.com/linkedin/parseq/wiki) `Task`, or
`Callback`. For example, a getter can be declared in either of the
following ways:
```java
@RestMethod.Get
public Task<Greeting> get(Long key)
{
  // set up some ParSeq tasks and return the final Task
  return Tasks.seq(Tasks.par(...), ...);
}
```

```java
@RestMethod.Get
public void get(Long key, @CallbackParam Callback<Greeting> callback)
{
  // use the callback asynchronously
}
```

These method signatures can be mixed arbitrarily with the synchronous
signatures, including in the same resource class. For instance, simple
methods can be implemented synchronously and slow methods can be
implemented asynchronously. However, multiple implementations of the
same REST method with different signatures may **not** be provided.

You can also use the asynchronous resource templates in order to
implement asynchronous Rest.li resources. The templates are:

-   `AssociationResourceAsyncTemplate`
-   `AssociationResourceTaskTemplate`
-   `CollectionResourceAsyncTemplate`
-   `CollectionResourceTaskTemplate`
-   `ComplexKeyResourceAsyncTemplate`
-   `ComplexKeyResourceTaskTemplate`
-   `SimpleResourceAsyncTemplate`
-   `SimpleResourceTaskTemplate`

The Rest.li server will automatically start any `Task` that is returned
by a `Task` based method by running it through a ParSeq engine.`Callback`-based methods do not receive special treatment.

## Sub-Resources

Sub-resources may be defined by setting the `parent` field on
`@RestLiCollection` to the class of the parent resource of
the sub-resource.

For example, a sub-resource of the fortunes resource would have a URI
path of the form:

```
/fortunes/{fortuneId}/subresource
```

Parent resource keys can be accessed by sub-resources, as shown in the
following example:

```java
@RestLiCollection(name = "subresource", namespace = "com.example.fortune", parent = FortunesResource.class)
public class SubResource extends CollectionResourceTemplate<Long, SubResourceEntity>
{
  @RestMethod.Get
  public Greeting get(Long key, @Keys PathKeys keys) {
      Long parentId = keys.getAsLong("fortuneId");
      ...
  }
...
}
```

Alternatively, if not using free form methods, the path key can
retrieved from the resource context. This approach may be deprecated in
future versions in favor of `@Keys`.

```java
public SubResourceEntity get(Long subresourceKey)
{
  Long parentId = getContext().getPathKeys().getAsLong("fortuneId");
  ...
}
```

For details on how to make requests to sub-resources from a client, see [Calling Sub-resources](/rest.li/user_guide/restli_client#calling-sub-resources)

#### @RestLiCollection with Complex Key

Classes implementing `ComplexKeyResource` can use a record type as key.
This allows for arbitrary complex hierarchical structures to be used to
key a collection resource, unlike CollectionResources, which only
support primitive type keys (or typerefs to primitive types).
`ComplexKeyResourceTemplate` is a convenient base class to extend when
implementing a `ComplexKeyResource`.

The full interface is:
```java
public interface ComplexKeyResource<K extends RecordTemplate, P extends
RecordTemplate, V extends RecordTemplate> ...
```

A complex key consists of a `Key` and `Parameter` part. The `Key` should
uniquely identify the entities of the collection while the parameters
may optionally be added to allow additional information that is not used
to lookup an entity, such as a version tag for concurrency control.

Since the parameters are often not needed, an `EmptyRecord` may be used
in the generic signature of a `ComplexKeyResource` to
indicate that no "Parameters" are used to key the collection.

Example:

```java
@RestLiCollection(name = "widgets", namespace = "com.example.widgets")
public class WidgetResource extends ComplexKeyResourceTemplate<WidgetKey, EmptyRecord, Widget>
{
  public Widget get(ComplexResourceKey<WidgetKey, EmptyRecord> ck)
  {
    WidgetKey key = ck.getKey();
    int number = key.getNumber();
    String make = key.getThing().getMake();
    String model = key.getThing().getModel();
    return lookupWidget(number, make, model);
  }
}
```

To use `EmptyRecord`, `restli-common` must be in
the `dataModel` dependencies for the api project where client
bindings are generated, as shown in the following example:

`api/build.gradle`:

```gradle
dependencies {
  // ...
  dataModel spec.product.pegasus.restliCommon
}
```

where `WidgetKey` is defined by the schema:

```pdl
namespace com.example.widget

record WidgetKey {
  number: string

  thing: record Thing {
    make: string
    model: string
  }
}
```

Example request:

```bash 
curl "http://<hostname:port>/widgets/number=1&thing.make=adruino&thing.model=uno"
``` 

If params are added, they are represented in the URL under the
"$params" prefix like this:

```bash
curl "http://<hostname:port>/widgets/number=1&thing.make=adruino&thing.model=uno&$params.version=1"
``` 

The implementation of complex key collection is identical to the regular
`RestLiCollection` with the exception that it extends
`ComplexKeyResourceTemplate` (or directly implements
`ComplexKeyResource`) and takes three ﻿type parameters instead of two:
key type, key parameter type, and value type --- each extending
@RecordTemplate.

For details on how a complex key is represented in a request URL, see
[Rest.li Protocol: Complex Types](/rest.li/spec/protocol#complex-types)

#### @RestLiSimpleResource

The `@RestLiSimpleResource` annotation is applied to classes to mark
them as providing a Rest.li simple resource. Simple resources model an
entity which is a singleton in a particular scope. See the description
of the [Simple Resource Pattern](/rest.li/modeling/modeling#simple) for more details.

The supported annotation parameters are:

-   `name` - required, defines the name of the resource.
-   `namespace` - optional, defines the namespace for the resource.
    Default is empty (root namespace). The namespace of the resource
    appears in the IDL, and is used as the package name for the
    generated client builders.
-   `parent` - optional, defines the parent resource for this resource.
    Default is root.

Classes annotated with `@RestLiSimpleResource` must implement the
`SimpleResource` interface. The `SimpleResource` interface requires a
generic type parameter `V`, which is the value type for the resource
(also known as, the entity type). The value type for a simple resource
must be a pegasus record, any subclass of `RecordTemplate` generated
from a `.pdl` schema.

For convenience, simple resources may extend `SimpleResourceTemplate`
rather than directly implementing the `SimpleResource` interface.

Examples:

```java
@RestLiSimpleResource(name = "todaysPromotedProduct", namespace = "com.example.product")
public class TodaysPromotedProductResource extends SimpleResourceTemplate<Product>
{
    ...
}
```

#### @RestLiAssociation

The `@RestLiAssociation` annotation is applied to classes to mark them
as providing a Rest.li association resource. Association resources model
a collection of relationships between entities. Each relationship is
referenced by the keys of the entities it relates and may define
attributes on the relation itself. See
[Association Resource Pattern](/rest.li/modeling/modeling#association)

For Example:

```java
@RestLiAssociation(name = "memberships", namespace = "com.example",
  assocKeys = {
    @Key(name = "memberId", type = Long.class), 
    @Key(name = "groupId", type = Long.class)
  }
)
public class MembershipsAssociation extends AssociationResourceTemplate<Membership>
{
  @Override
  public Membership get(CompoundKey key)
  {
    return lookup(key.getPartAsLong("memberId", key.getPartAsLong("groupId"));
  }
}
```

```bash
curl http://<hostname:port>/memberships/memberId=1&groupId=10
```

The supported annotation parameters are:

-   `name` - required, defines the name of the resource.
-   `namespace` - optional, defines the namespace for the resource.
    Default is empty (root namespace). The namespace of the resource
    appears in the IDL, and is used as the package name for the
    generated client builders.
-   `parent` - optional, defines the parent resource for this resource.
    Default is root.
-   `assocKeys` - required, defines the list of keys for the association
    resource. Each key must declare its name and type.

Classes annotated with `@RestLiAssociation` must implement the
`AssociationResource` interface. The `AssociationResource` interface
requires a single generic type parameter:

-   `V`, which is the value type for the resource, a.k.a., the entity
    type.

The value type for an association resource must be a subclass of
`RecordTemplate` generated from a `.pdl` schema.

Note that for association resources, they key type is always
`CompoundKey`, with key parts as defined in the `assocKeys` parameter of
the class' annotation.

For convenience, Association resources may extend
`AssociationResourceTemplate` rather than directly implementing the
`AssociationResource` interface.

#### @RestLiActions

The `@RestLiActions` annotation is applied to classes to mark them as
providing a Rest.li action set resource. Action set resources do not
model any resource pattern. They simply group together a set of custom
actions.

For example:

```java
@RestLiActions(name = "simpleActions", namespace = "com.example")
public class SimpleActionsResource {

  @Action(name="echo")
  public String echo(@ActionParam("input") String input)
  {
    return input;
  }
}
```

The supported annotation parameters are:

-   `name` - required, defines the name of the resource.
-   `namespace` - optional, defines the namespace for the resource.
    Default is empty (root namespace).

Action set resources do not have a key or value type, and do not need to
`implement` any framework interfaces.

<a id="wiki-ResourceMethods"></a>

## Resource Methods

Resource methods are operations a resource can perform. Rest.li defines
a standard set of resource methods, each with its own interface pattern
and intended semantics.

The set of possible resource methods is constrained by the resource
type, as described in the table below:

  
  | Resource Type             | Collection |  Simple | Association | Action Set  | 
  | --------------------------|------------|---------|-------------|-------------| 
  | GET                       | x          |  x      |  x          |             |
  | --------------------------|------------|---------|-------------|-------------| 
  | BATCH_GET / GET_ALL       | x          |         |  x          |             |
  | --------------------------|------------|---------|-------------|-------------| 
  | FINDER                    | x          |         |  x          |             |
  | --------------------------|------------|---------|-------------|-------------| 
  | BATCH_FINDER              | x          |         |  x          |             |
  | --------------------------|------------|---------|-------------|-------------| 
  | CREATE / BATCH_CREATE     | x          |         |             |             |
  | --------------------------|------------|---------|-------------|-------------| 
  | UPDATE / PARTIAL_UPDATE   | x          |  x      | x           |             |
  | --------------------------|------------|---------|-------------|-------------| 
  | BATCH_UPDATE \ BATCH_PARTIAL_UPDATE           | x          |         |  x          |             |
  | --------------------------|------------|---------|-------------|-------------| 
  | DELETE                    | x          |  x      |  x          |             |   
  | --------------------------|------------|---------|-------------|-------------| 
  | BATCH_DELETE              | x          |         |  x          |             |
  | --------------------------|------------|---------|-------------|-------------| 
  | ACTION                    | x          |  x      |  x          |   x
  

In the section below, `K` is used to denote the resource's key type, and
`V` is used to denote the resource's value type. Remember that for
association resources, `K` is always `CompoundKey`.

<a id="GET"></a>

#### GET

The GET resource method is intended to retrieve a single entity
representation based upon its key or without a key from a simple
resource. GET should not have any visible side effects. For example, it
should be safe to call whenever the client wishes.

Resources providing the GET resource method must override one of the
following method signatures.

For collection and association resources:

```java
public V get(K key);
```

For simple resources:

```java
public V get();
```

For asynchronous resources:

```java
public Task<V> get(K key);
```

Get methods can also be annotated if not overriding a base class method.
GET supports a method signature with a wrapper return type.

For collection and association resources:

```java
@RestMethod.Get
public GetResult<V> getWithStatus(K key);
```

For simple resources:

```java
@RestMethod.Get
public GetResult<V> getWithStatus();
```

For asynchronous resources:

```java
@RestMethod.Get
public Task<GetResult<V>> getWithStatus(K key);
```

An annotated get method may also have arbitrary query params added:

```java
@RestMethod.Get
public GetResult<V> get(K key, @QueryParam("viewerId") String viewerId);
```

The return type `GetResult<V>` allows users to set an arbitrary HTTP
status code for the response. For more information about the
`RestMethod.Get` annotation, see [Free-Form
Resources](#free-form-resources).

<a id="BATCH_GET"></a>

#### BATCH_GET

The BATCH_GET resource method retrieves multiple entity representations
given their keys. BATCH_GET should not have any visible side effects.
For example, it should be safe to call whenever the client wishes.
However, this is not something enforced by the framework, and it is up
to the application developer that there are no side effects.

Resources providing the BATCH_GET resource method must override the
following method signature:

```java
public Map<K, V> batchGet(Set<K> ids);
```

An asynchronous method must override the following method signature:
```java
public Task<Map<K, V>> batchGet(Set<K> ids);
```

@`RestMethod.BatchGet` may be used to indicate a batch get method
instead of overriding the batchGet method of a base class.

Resources may also return `BatchResult`, which allows errors to be
returned along with entities that were successfully retrieved.

Example of a batch get:
```java
public BatchResult<Long, Greeting> batchGet(Set<Long> ids)
{
  Map<Long, Greeting> batch = new HashMap<Long, Greeting>();
  Map<Long, RestLiServiceException> errors = new HashMap<Long, RestLiServiceException>();
  for (long id : ids)
  {
    Greeting g = _db.get(id);
    if (g != null)
    {
      batch.put(id, g);
    }
    else
    {
      errors.put(id, new RestLiServiceException(HttpStatus.S_404_NOT_FOUND));
    }
  }
  return new BatchResult<Long, Greeting>(batch, errors);
}
```

Clients should make requests to a batch resource using `buildKV()` (not
`build()`, it is deprecated), for example:

```java
new FortunesBuilders().batchGet().ids(...).buildKV();
```

<a id="GET_ALL"></a>

#### GET_ALL

When a GET is requested on a collection or association resource with no
key provided (for example, /myResource), the GET_ALL resource method is
invoked, if present. The GET_ALL resource method retrieves all entities
for the collection and supports the same pagination facilities as a
FINDER.

```java
public List<V> getAll(@Context PagingContext pagingContext);
```

An asynchronous resource would implement the following method signature:
```java
public Task<List<V>> getAll(@Context PagingContext pagingContext);
```

@`RestMethod.GetAll` may be used to indicate a get all method instead of
overriding the getAll method of a base class.

To directly control the total and metadata returned by a get all method,
do not override getAll. Instead, create a new method with the
@`RestMethod.GetAll` annotation and return a `CollectionResult` rather
than a list, for example:

```java
@RestMethod.GetAll
public CollectionResult<Widgets, WidgetsMetadata> getAllWidgets(@Context PagingContext pagingContext)
{
  // ...
  return new CollectionResult<Widgets, WidgetsMetadata>(pageOfWidgets, total, metadata);
}
```

When returning a CollectionResult from GetAll, the behavior is identical
to a FINDER. See the FINDER documentation below for additional details
about CollectionResult.

<a id="FINDER"></a>

#### FINDER

FINDER methods model query operations. For example, they retrieve an
ordered list of 0 or more entities based on criteria specified in the
query parameters. Finder results will automatically be paginated by the
Rest.li framework. Like GET methods, FINDER methods should not have side
effects.

Resources may provide zero or more FINDER resource methods. Each finder
method must be annotated with the `@Finder` annotation.

Pagination default to start=0 and count=10. Clients may set both of
these parameters to any desired value.

The `@Finder` annotation takes a single required parameter, which
indicates the name of the finder method.

For example:

```java
/*
You can access this FINDER method via
/resources/order?q=findOrder&buyerType=1&buyerId=309&orderId=1208210101
*/
@RestLiCollection(name="order",keyName="orderId")
public class OrderResource extends CollectionResourceTemplate<Integer,Order>
{
  @Finder("findOrder")
  public List<Order> findOrder(@Context PagingContext context,
                               @QueryParam("buyerId") Integer buyerId,
                               @QueryParam("buyerType") Integer buyerType,
                               @QueryParam("orderId") Integer orderId)
                              throws InternalException
  {
  ...
  }
}
```

Finder methods must return either:

-   `List<V>`
-   `CollectionResult<V, MetaData>`
-   `BasicCollectionResult<V>`, a subclass of `CollectionResult`
-   a subclass of one the above

Every parameter of a finder method must be annotated with one of:

-   `@Context` - indicates that the parameter provides framework context
    to the method. Currently all `@Context` parameters must be of type
    `PagingContext`.
-   `@QueryParam` - indicates that the value of the parameter is
    obtained from a request query parameter. The value of the annotation
    indicates the name of the query parameter. Duplicate names are not
    allowed for the same finder method.
-   `@ActionParam` - similar to Query Param, but the parameter
    information will be located in the request body. Generally,
    `@QueryParam` is preferred over `@ActionParam`.
-   `@AssocKey` - indicates that the value of the parameter is a partial
    association key, obtained from the request. The value of the
    annotation indicates the name of the association key, which must
    match the name of an `@Key` provided in the `assocKeys` field of the
    `@RestLiAssociation` annotation.

Parameters marked with `@QueryParam`, `@ActionParam`, and `@AssocKey`
may also be annotated with `@Optional`, which indicates that the
parameter is not required. The `@Optional` annotation may specify a
String value, indicating the default value to be used if the parameter
is not provided in the request. If the method parameter is of primitive
type, a default value must be specified in the `@Optional` annotation.

Valid types for query parameters are:

-   `String`
-   `boolean` / `Boolean`
-   `int` / `Integer`
-   `long` / `Long`
-   `float` / `Float`
-   `double` / `Double`
-   `ByteString`
-   A Pegasus Enum (any enum defined in a `.pdl` schema)
-   Custom types (see the bottom of this section)
-   Record template types (any subclass of `RecordTemplate` generated
    from a `.pdl` schema)
-   Arrays of one of the types above, e.g. `String[]`, `long[]`, ...

```java
@Finder("simpleFinder")
public List<V> simpleFind(`Context PagingContext context);

@Finder("complexFinder")
public CollectionResult<V, MyMetaData> complexFinder(@Context(defaultStart= 10, defaultCount = 100) PagingContext context,
                                                     @AssocKey("key1") Long key,
                                                     @QueryParam("param1") String requiredParam,
                                                     @QueryParam("param2") @Optional String optionalParam);
```

A Finder method in an asynchronous resource could implement the following method signatures:
```java
@Finder("simpleAsyncFinder")
public Task<List<V>> simpleFind(`Context PagingContext context);

@Finder("complexAsyncFinder")
public Task<CollectionResult<V, MyMetaData>> complexFinder(@Context(defaultStart= 10, defaultCount = 100) PagingContext context,
                                                     @AssocKey("key1") Long key,
                                                     @QueryParam("param1") String requiredParam,
                                                     @QueryParam("param2") @Optional String optionalParam);
```

<a id="TyperefSchema"></a>

#### Typerefs (Custom Types)

Custom types can be any Java type, as long as it has a coercer and a
typeref schema, even Java classes from libraries such as Date. To create
a query parameter that uses a custom type, you will need to write a
coercer and a typeref schema for the type you want to use. See the
[typeref documentation](/rest.li/pdl_schema#typerefs) for details.

First for the coercer, you will need to write an implementation of
DirectCoercer that converts between your custom type and some simpler
underlying type, like String or Double. By convention, the coercer
should be an internal class of the custom type it coerces. Additionally,
the custom type should register its own coercer in a static code block.

If this is not possible (for example, if you want to use a Java built-in
class like Date or URI as a custom type), then you can write a separate
coercer class and register the coercer with the private variable
declaration:

```java
private static final Object REGISTER_COERCER = Custom.registerCoercer(new ObjectCoercer(), CustomObject.class);
```

Typeref Schema

The purpose of the typeref schemas is to keep track of the underlying
type of the custom Type and the location of the custom type's class,
and, if necessary, the location of its coercer. The basic appearance of
the typeref schema is shown below:

```pdl
namespace com.linkedin.example // namespace of the typeref

@java.class = "com.linkedin.example.CustomObject" // location of the custom type class
@java.coercerClass = "com.linkedin.example.CustomObjectCoercer" // only needed if the custom type itself cannot contain the coercer as an internal class.
typeref CustomObjectRef = string // underlying type that the coercer converts to/from
```

This typeref can then be referenced in other schemas:

```pdl
import com.linkedin.example.CustomObjectRef

record ExampleRecord {
  member: CustomObjectRef
}
```

The generated Java data templates will automatically coerce from
CustomObjectRef to CustomObject when accessing the member field:

```java
CustomObject o = exampleRecord.getMember();
```

Once Java data templates are generated, the typeref may also be used in
Keys, query parameters, or action parameters.

Keys:

```java
@RestLiCollection(name="entities", 
                  namespace = "com.example",
                  keyTyperefClass = CustomObjectRef.class)
public class EntitiesResource extends CollectionResourceTemplate<CustomObject, Urn>
```

Compound keys:

```java
@RestLiAssociation(name="entities", 
                   namespace="com.example",
                   assocKeys={@Key(name="o", type=CustomObject.class, typeref=CustomObjectRef.class)})
```

Query parameters:

```java
@QueryParam(value="o", typeref=CustomObjectRef.class) CustomObject o

@QueryParam(value="oArray", typeref=CustomObjectRef.class) CustomObject[] oArray
```

<a id="BATCHFINDER"></a>

#### BATCH FINDER

The BATCH_FINDER resource method accepts a list of filters set. Instead of callings multiple finders with different filter values, we call 1 BATCH_FINDER method with a list of filters.

Resources may provide zero or more BATCH_FINDER resource methods. Each BATCH_FINDER method must be annotated with the `@BatchFinder` annotation.
And this method must return a `BatchFinderResult`.
For example: 

```java
@BatchFinder(value = "searchGreetings", batchParam = "criteria")
public BatchFinderResult<GreetingCriteria, Greeting, EmptyRecord> searchGreetings(@PagingContextParam PagingContext context,
                                                                                  @QueryParam("criteria") GreetingCriteria[] criteria,
                                                                                  @QueryParam("message") String message)
```

An asynchronous BATCH_FINDER must return a `Task<BatchFinderResult>`. For example:

```java
@BatchFinder(value = "searchGreetings", batchParam = "criteria")
public Task<BatchFinderResult<GreetingCriteria, Greeting, EmptyRecord>> searchGreetings(@PagingContextParam PagingContext context,
                                                                                  @QueryParam("criteria") GreetingCriteria[] criteria,
                                                                                  @QueryParam("message") String message)
```
See more details about BATCH_FINDER resource method api here: [BatchFinder Resource API](/rest.li/batch_finder_resource_method#resource-api)



<a id="CREATE"></a>

#### CREATE

CREATE methods model the creation of new entities from their
representation. In CREATE, the resource implementation is responsible
for assigning a new key to the created entity. CREATE methods are
neither safe nor idempotent.

Resources providing the CREATE resource method must override the
following method signature:

```java
public CreateResponse create(V entity);
```

An asynchronous method would override the following method signature:
```java
public Task<CreateResponse> create(V entity);
```

The returned `CreateResponse` object indicates the HTTP status code to
be returned (defaults to 201 CREATED), as well as an optional ID for the
newly created entity. If provided, the ID will be written into the
"X-LinkedIn-Id" header by calling `toString()` on the ID object.

@`RestMethod.Create` may be used to indicate a create method instead of
overriding the create method of a base class.

#### Returning entity in CREATE response

By default, the newly created entity is not returned in the CREATE
response because the client already has the entity when sending the
CREATE request. However, there are use cases where the server will
attach additional data to the new entity. Returning the entity in the
CREATE response saves the client another GET request.

Starting in Rest.li version 2.10.3, we provide the developer the option to
return the newly created entity. To use this feature, add a `@ReturnEntity`
annotation to the method that implements CREATE. The return type of the
method must be `CreateKVResponse`.

```java
@ReturnEntity
public CreateKVResponse create(V entity);
```

An example implementation for resource is like below, note that the return type will be  ``CreateKVResponse`` :

```java
@ReturnEntity
public CreateKVResponse<Long, Greeting> create(Greeting entity)
{
  Long id = 1L;
  entity.setId(id);
  return new CreateKVResponse<Long, Greeting>(entity.getId(), entity);
}
```

There may be circumstances in which you want to prevent the server from returning the entity, for example to reduce network traffic.
Here is an example curl request that makes use of the [`$returnEntity` query parameter](/rest.li/spec/return_entity#client-specified-behavior) to indicate that the entity should not be returned:

```bash
curl -X POST 'localhost:/greetings?$returnEntity=false' \
  -H 'X-RestLi-Method: CREATE' \
  -d '{"message": "Hello, world!", "tone": "FRIENDLY"}'
```

<a id="BATCH_CREATE"></a>

#### BATCH_CREATE

BATCH_CREATE methods model the creation of a group of new entities from
their representations. In BATCH_CREATE, the resource implementation is
responsible for assigning a new key to each created entity.
BATCH_CREATE methods are neither safe nor idempotent.

Resources providing the BATCH_CREATE resource method must override the
following method signature:

```java
public BatchCreateResult<K, V> batchCreate(BatchCreateRequest<K, V> entities);
```

An asynchronous resource providing the BATCH_CREATE resource method must override the
following method signature:
```java
public Task<BatchCreateResult<K, V>> batchCreate(BatchCreateRequest<K, V> entities);
```

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

```java
public BatchCreateResult<Long, Greeting> batchCreate(BatchCreateRequest<Long, Greeting> entities)
{
  List<CreateResponse> responses = new ArrayList<CreateResponse>(entities.getInput().size());

  for (Greeting g : entities.getInput())
  {
    responses.add(create(g));
  }
  return new BatchCreateResult<Long, Greeting>(responses);
}

public CreateResponse create(Greeting entity)
{
  entity.setId(_idSeq.incrementAndGet());
  _db.put(entity.getId(), entity);
  return new CreateResponse(entity.getId());
}
```

Error details can be returned in any CreateResponse by providing a
RestLiServiceException, for example:

```java
public BatchCreateResult<Long, Greeting> batchCreate(BatchCreateRequest<Long, Greeting> entities) 
{
  List<CreateResponse> responses = new ArrayList<CreateResponse>(entities.getInput().size());

  ...
  if (...)
  {
    RestLiServiceException exception = new RestLiServiceException(HttpStatus.S_406_NOT_ACCEPTABLE, "...");
    exception.setServiceErrorCode(...);
    exception.setErrorDetails(...);
    responses.add(new CreateResponse(exception));
  }
  ...

  return new BatchCreateResult<Long, Greeting>(responses);
}
```

#### Returning entities in BATCH_CREATE response

Similar to CREATE, BATCH_CREATE also could return the newly created
entities in the response. To do that, add a `@ReturnEntity` annotation to
the method implementing BATCH_CREATE. The return type of the method
must be `BatchCreateKVResult`.

```java
@ReturnEntity
public BatchCreateKVResult<K, V> batchCreate(BatchCreateRequest<K, V> entities);
```

An example implementation for resource is like below, note that the
return type will be `BatchCreateKVResult`:

```java
@ReturnEntity
public BatchCreateKVResult<Long, Greeting> batchCreate(BatchCreateRequest<Long, Greeting> entities)
{
  List<CreateKVResponse<Long, Greeting>> responses = new ArrayList<CreateKVResponse<Long, Greeting>>(entities.getInput().size());
  for (Greeting greeting : entities.getInput())
  {
    responses.add(create(greeting)); // Create function should return CreateKVResponse
  }
  return BatchCreateKVResult<Long, Greeting>(responses);
}
```

There may be circumstances in which you want to prevent the server from returning the entity, for example to reduce network traffic.
Here is an example curl request that makes use of the [`$returnEntity` query parameter](/rest.li/spec/return_entity#client-specified-behavior) to indicate that the entity should not be returned:

```bash
curl -X POST 'localhost:/greetings?$returnEntity=false' \
  -H 'X-RestLi-Method: BATCH_CREATE' \
  -d '{"elements":[{"message": "Hello, world!", "tone": "FRIENDLY"},{"message": "Again!", "tone": "FRIENDLY"}]}'
```

<a id="UPDATE"></a>

#### UPDATE

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

```java
public UpdateResponse update(K key, V entity);
```

For simple resources:

```java
public UpdateResponse update(V entity);
```

For asynchronous resources:
```java
public Task<UpdateResponse> update(K key, V entity);
```

The returned `UpdateResponse` object indicates the HTTP status code to
be returned.

@`RestMethod.Update` may be used to indicate a update method instead of
overriding the update method of a base class.

<a id="BATCH_UPDATE"></a>

#### BATCH_UPDATE

BATCH_UPDATE methods model updating a set of entities with specified
keys by setting their values (overwriting each entity entirely).
BATCH_UPDATE has side effects but is idempotent. For example, repeating
the same batch update operation has the same effect as calling it once.

Resources may choose whether to allow BATCH_UPDATE for entities that do
not already exist, in which case each entity should be created. This is
different from BATCH_CREATE because the client specifies the keys for
the entities to be created.

Resources providing the BATCH_UPDATE resource method must override the
following method signature:

```java
public BatchUpdateResult<K, V> batchUpdate(BatchUpdateRequest<K, V> entities);
```

An asynchronous resource must override the following method signature:
```java
public Task<BatchUpdateResult<K, V>> batchUpdate(BatchUpdateRequest<K, V> entities);
```

`BatchUpdateRequest` contains a map of entity key to entity value.

The returned `BatchUpdateResult` object indicates the `UpdateResponse`
for each key in the `BatchUpdateRequest`. In the case of failures,
`RestLiServiceException` objects may be added to the `BatchUpdateResult`
for the failed keys.

@`RestMethod.BatchUpdate` may be used to indicate a batch update method
instead of overriding the batchUpdate method of a base class.

Example of a batch update:

```java
public BatchUpdateResult<Long, Greeting> batchUpdate(BatchUpdateRequest<Long, Greeting> entities)
{
  Map<Long, UpdateResponse> responseMap = new HashMap<Long, UpdateResponse>();
  for (Map.Entry<Long, Greeting> entry : entities.getData().entrySet())
  {
    responseMap.put(entry.getKey(), update(entry.getKey(),
    entry.getValue()));
  }
  return new BatchUpdateResult<Long, Greeting>(responseMap);
}

public UpdateResponse update(Long key, Greeting entity)
{
  Greeting g = _db.get(key);
  if (g == null)
  {
    return new UpdateResponse(HttpStatus.S_404_NOT_FOUND);
  }

  _db.put(key, entity);

  return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
}
```

<a id="PARTIAL_UPDATE"></a>

#### PARTIAL_UPDATE

PARTIAL_UPDATE methods model updating part of the entity with a given
key. PARTIAL_UPDATE has side effects. In general, it is not guaranteed
to be idempotent.

Resources providing the PARTIAL_UPDATE resource method must override
the following method signature:

```java
public UpdateResponse update(K key, PatchRequest<V> patch);
```

An asynchronous resource must override the following method signature:
```java
public Task<UpdateResponse> update(K key, PatchRequest<V> patch);
```

The returned `UpdateResponse` object indicates the HTTP status code to
be returned.

Rest.li provides tools to make it easy to handle partial updates to your
resources. A typical update function should look something like this:

```java
@Override
public UpdateResponse update(String key, PatchRequest<YourResource> patch)
{
  YourResource resource = _db.get(key); // Retrieve the resource object
  from somewhere
  if (resource == null)
  {
    return new UpdateResponse(HttpStatus.S_404_NOT_FOUND);
  }
  try
  {
    PatchApplier.applyPatch(resource, patch); // Apply the patch.
    // Be sure to save the resource if necessary
  }
  catch (DataProcessingException e)
  {
    return new UpdateResponse(HttpStatus.S_400_BAD_REQUEST);
  }
  return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
}
```

The PatchApplier automatically updates resources defined using the
Pegasus Data format. The Rest.li client classes provide support for
constructing patch requests, but here is an example update request using
curl:

```bash
curl -X POST 'localhost:/fortunes/1' \
  -d '{"patch": {"$set": {"fortune": "you will strike it rich!"}}}'
```

@`RestMethod.PartialUpdate` may be used to indicate a partial update
method instead of overriding the partialUpdate method of a base class.

#### Inspecting Partial Updates to Selectively Update Fields in a Backing Store

It is possible to inspect the partial update and selectively write only
the changed fields to a store.

For example, to update only the street field of this address entity:

```json
{
  "address": {
    "street": "10th",
    "city": "Sunnyvale"
  }
}
```

The partial update to change just the street field is:

```json
{
  "patch": {
    "address": {
      "$set": {
        "street": "9th"
      }
    }
  }
}
```

For the service code to selectively update just the street field (e.g.,
UPDATE addresses SET street=:street WHERE key=:key). The partial update
can be inspected and the selective update if only the street field is
changed:

```java
@Override
public UpdateResponse update(String key, PatchRequest<YourResource> patchRequest)
{
  try
  {
    DataMap patch = patchRequest.getPatchDocument();
    boolean selectivePartialUpdateApplied = false;
    if(patch.containsKey("address") && patch.size() >= 1)
    {
      DataMap address = patch.getDataMap("address");
      if(address.containsKey("$set") && address.size()  1)
      {
        DataMap set = address.getDataMap("$set");
        if(address.containsKey("street") && address.size()  1)
        {
          String street = address.getString("street");
          selectivePartialUpdateApplied = true;
          // update only the street, since its the only thing this patch requests to change
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
    return new UpdateResponse(HttpStatus.S_400_BAD_REQUEST);
  }

  return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
}
```

#### Creating Partial Updates

To create a request to modify field(s), PatchGenerator can be used, for
example:

```java
Fortune fortune = new Fortune().setMessage("Today's your lucky day.");
PatchRequest<Fortune> patch = PatchGenerator.diffEmpty(fortune);
Request<Fortune> request = new FortunesBuilders().partialUpdate().id(1L).input(patch).build();
```

`PatchGenerator.diff(original, revised)` can also be used to create a
minimal partial update.

#### Returning entity in PARTIAL_UPDATE response

By default, the patched entity is not returned in the PARTIAL_UPDATE response because
the client already has the patch data and possibly has the rest of the entity as well.
However, there are use cases where the server will attach additional data to the new entity or the user
simply doesn't have the whole entity. Returning the entity in the PARTIAL_UPDATE response saves  the client
another GET request.

Starting in Rest.li version 24.0.0, we provide the developer the option to
return the patched entity. To use this feature, add a `@ReturnEntity`
annotation to the method that implements PARTIAL_UPDATE. The return type of the
method must be `UpdateEntityResponse`.

```java
@ReturnEntity
@RestMethod.PartialUpdate
public UpdateEntityResponse<V> partialUpdate(K key, PatchRequest<V> patch);
```

An example resource method implementation is as follows, note that the return type will be  `UpdateEntityResponse`:

```java
@ReturnEntity
@RestMethod.PartialUpdate
public UpdateEntityResponse<Greeting> update(Long key, PatchRequest<Greeting> patch)
{
  Greeting greeting = _db.get(key);

  if (greeting == null)
  {
    throw new RestLiServiceException(HttpStatus.S_404_NOT_FOUND);
  }

  try
  {
    PatchApplier.applyPatch(greeting, patch);
  }
  catch (DataProcessingException e)
  {
    throw new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST);
  }

  return new UpdateEntityResponse<Greeting>(HttpStatus.S_200_OK, greeting);
}
```

There may be circumstances in which you want to prevent the server from returning the entity, for example to reduce network traffic.
Here is an example curl request that makes use of the [`$returnEntity` query parameter](/rest.li/spec/return_entity#client-specified-behavior) to indicate that the entity should not be returned:

```bash
curl -X POST 'localhost:/greetings/1?$returnEntity=false' \
  -d '{"patch": {"$set": {"message": "Hello, world!"}}}'
```

<a id="BATCH_PARTIAL_UPDATE"></a>

#### BATCH_PARTIAL_UPDATE

BATCH_PARTIAL_UPDATE methods model partial updates of multiple
entities given their keys. BATCH_PARTIAL_UPDATE has side effects. In
general, it is not guaranteed to be idempotent.

Resources providing the BATCH_PARTIAL_UPDATE resource method must
override the following method signature:

```java
public BatchUpdateResult<K, V> batchUpdate(BatchPatchRequest<K, V> patches);
```

An asynchronous resource providing the BATCH_PARTIAL_UPDATE resource method must
override the following method signature:

```java
public Task<BatchUpdateResult<K, V>> batchUpdate(BatchPatchRequest<K, V> patches);
```

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

```java
public BatchUpdateResult<Long, Greeting> batchUpdate(BatchPatchRequest<Long, Greeting> entityUpdates)
{
  Map<Long, UpdateResponse> responseMap = new HashMap<Long,
  UpdateResponse>();
  for (Map.Entry<Long, PatchRequest<Greeting>> entry :
  entityUpdates.getData().entrySet())
  {
    responseMap.put(entry.getKey(), update(entry.getKey(),
    entry.getValue()));
  }
  return new BatchUpdateResult<Long, Greeting>(responseMap);
}

public UpdateResponse update(Long key, PatchRequest<Greeting> patch)
{
  Greeting g = _db.get(key);
  if (g == null)
  {
    return new UpdateResponse(HttpStatus.S_404_NOT_FOUND);
  }

  try
  {
    PatchApplier.applyPatch(g, patch);
  }
  catch (DataProcessingException e)
  {
    return new UpdateResponse(HttpStatus.S_400_BAD_REQUEST);
  }

  _db.put(key, g);

  return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
}
```

#### Returning entities in BATCH_PARTIAL_UPDATE response

By default, the patched entities are not returned in the BATCH_PARTIAL_UPDATE response because
the client already has the patch data and possibly has the rest of the entities as well.
However, there are use cases where the server will attach additional data to the new entities or the user
simply doesn't have the whole entities. Returning the entities in the BATCH_PARTIAL_UPDATE response saves the client
another GET request.

Starting in Rest.li version 25.0.5, we provide the developer the option to
return the patched entities. To use this feature, add a `@ReturnEntity`
annotation to the method that implements BATCH_PARTIAL_UPDATE. The return type of the
method must be `BatchUpdateEntityResult`.

```java
@ReturnEntity
@RestMethod.BatchPartialUpdate
public BatchUpdateEntityResult<K, V> batchPartialUpdate(BatchPatchRequest<Long, Greeting> patches);
```

An example resource method implementation is as follows, note that the return type will be  `BatchUpdateEntityResult`:

```java
@ReturnEntity
@RestMethod.BatchPartialUpdate
public BatchUpdateEntityResult<Long, Greeting> batchPartialUpdate(BatchPatchRequest<Long, Greeting> patches)
{
  Map<Long, UpdateEntityResponse<Greeting>> responseMap = new HashMap<>();
  Map<Long, RestLiServiceException> errorMap = new HashMap<>();
  for (Map.Entry<Long, PatchRequest<Greeting>> entry : patches.getData().entrySet())
  {
    try
    {
      UpdateEntityResponse<Greeting> updateEntityResponse = partialUpdate(entry.getKey(), entry.getValue());
      responseMap.put(entry.getKey(), updateEntityResponse);
    }
    catch (RestLiServiceException e)
    {
      errorMap.put(entry.getKey(), e);
    }
  }
  return new BatchUpdateEntityResult<>(responseMap, errorMap);
}
```

There may be circumstances in which you want to prevent the server from returning the entities, for example to reduce network traffic.
Here is an example curl request that makes use of the [`$returnEntity` query parameter](/rest.li/spec/return_entity#client-specified-behavior) to indicate that the entity should not be returned:

```bash
curl -X POST 'localhost:/greetings?ids=List(1)&$returnEntity=false' \
  -d '{"entities":{"1":{"patch": {"$set": {"message": "Hello, world!"}}}}}' \
  -H 'X-RestLi-Method: BATCH_PARTIAL_UPDATE' \
  -H 'X-RestLi-Protocol-Version: 2.0.0'
```

<a id="DELETE"></a>

#### DELETE

DELETE methods model deleting (removing) an entity with a given key on
collection and association resources or without a key on simple
resources. DELETE has side effects but is idempotent.

Resources providing the DELETE resource method must override one of the
following method signatures.

For collection and association resources:

```java
public UpdateResponse delete(K key);
```

For simple resources:

```java
public UpdateResponse delete();
```

For asynchronous resources:
```java
public Task<UpdateResponse> delete(K key);
```

The returned `UpdateResponse` object indicates the HTTP status code to
be returned.

@`RestMethod.Delete` may be used to indicate a delete method instead of
overriding the delete method of a base class.

<a id="BATCH_DELETE"></a>

#### BATCH_DELETE

BATCH_DELETE methods model deleting (removing) multiple entities given
their keys. BATCH_DELETE has side effects but is idempotent.

Resources providing the BATCH_DELETE resource method must override the
following method signature:

```java
public BatchUpdateResult<K, V> batchDelete(BatchDeleteRequest<K, V> ids);
```

Asynchronous resources providing the BATCH_DELETE resource method must override the
following method signature:

```java
public Task<BatchUpdateResult<K, V>> batchDelete(BatchDeleteRequest<K, V> ids);
```

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

```java
public BatchUpdateResult<Long, Greeting> batchDelete(BatchDeleteRequest<Long, Greeting> deleteRequest)
{
  Map<Long, UpdateResponse> responseMap = new HashMap<Long, UpdateResponse>();
  for (Long id : deleteRequest.getKeys())
  {
    responseMap.put(id, delete(id));
  }
  return new BatchUpdateResult<Long, Greeting>(responseMap);
}

public UpdateResponse delete(Long key)
{
  boolean removed = _db.remove(key) != null;

  return new UpdateResponse(removed ? HttpStatus.S_204_NO_CONTENT : HttpStatus.S_404_NOT_FOUND);
}
```

<a id="ACTION"></a>

#### ACTION

ACTION methods are very flexible and do not specify any standard
behavior.

Resources may provide zero or more ACTION resource methods. Each action
must be annotated with the `@Action` annotation.

The `@Action` annotation supports the following parameters:

-   `name` Required, the name of the action resource method.
-   `resourceLevel` Optional, defaults to `ResourceLevel.ANY`, which
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
-   `returnTyperef` Optional, defaults to no typeref. Indicates a
    Typeref to be used in the IDL for the action's return parameter.
    Useful for actions that return primitive types.

Each parameter to an action method must be annotated with
`@ActionParam`, which takes the following annotation parameters:

-   `value` Required, string name for the action parameter. If this is
    the only annotation, parameter, it may be specified without being
    explicitly named, for example, @`ActionParam("paramName")`.
-   `typeref` Optional, Typeref to be used in the IDL for the parameter.

Parameters of action methods may also be annotated with `@Optional`,
which indicates that the parameter is not required in the request. The
`@Optional` annotation may specify a String value, which specifies the
default value to be used if the parameter is not provided in the
request. If the method parameter is of primitive type, a default value
must be specified in the `@Optional` annotation.

Valid parameter types and return types for action are:

-   `String`
-   `boolean` / `Boolean`
-   `int` / `Integer`
-   `long` / `Long`
-   `float` / `Float`
-   `double` / `Double`
-   `ByteString`
-   A Pegasus Enum (any enum defined in a `.pdl` schema)
-   `RecordTemplate` or a subclass of `RecordTemplate` generated from a
    record schema
-   `FixedTemplate` or a subclass of `FixedTemplate` generated from a
    fixed schema
-   `AbstractArrayTemplate` or a subclass of `AbstractArrayTemplate`,
    for example, `StringArray`, `LongArray`, and so on.
-   `AbstractMapTemplate` or a subclass of `AbstractMapTemplate`, for
    example, `StringMap`, `LongMap`, and so on.
-   Custom types

Similar to `GetResult<V>`, since 1.5.8, Rest.li supports an
`ActionResult<V>` wrapper return type that allows you to specify an
arbitrary HTTP status code for the response.

Simple example:

```java
@Action(name="action")
public void doAction();
```

A more complex example, illustrating multiple parameters:

```java
@Action(name="sendTestAnnouncement",resourceLevel= ResourceLevel.ENTITY)
public void sendTestAnnouncement(@ActionParam("subject") String subject, 
                                 @ActionParam("message") String message, 
                                 @ActionParam("emailAddress") String emailAddress)
```

<a id="ActionParamVQueryParam"></a>

#### ActionParam vs. QueryParam

`@ActionParam` and `@QueryParam` are used in different methods.
`@ActionParam` is only allowed in Action methods, while `@QueryParam` is
allowed in all non-Action methods. Besides, they are also different in
terms of how the parameter data is sent to the server. If a parameter is
annotated with `@QueryParam`, the information will be sent in the
request url. If a parameter is annotated with `@ActionParam`, the
information will be sent in the request body. Therefore, one advantage
of using `@ActionParam` would be that the sent parameter can be encoded.
One disadvantage is that the purpose of the request itself can become
less clear if one only examines the url.

<a id="wiki-ReturningNulls"></a>

#### Returning Nulls

Resource methods should never explicitly return `null`. If the Rest.li
framework detects this, it will return an HTTP `500` back to the client
with a message indicating 'Unexpected null encountered'. The only
exceptions to this rule are ACTION and GET. If an ACTION resource method
returns `null`, the rest.li framework will return an HTTP `200`. If a
GET returns `null`, the Rest.li framework will return an HTTP `404`.

Also note that the HTTP `500` will also be generated by the Rest.li
framework if subsequent data structures inside of resource method
responses are null or contain null. This applies to any data structure
that is not a RecordTemplate. For example, all of the the following
would cause an HTTP `500` to be returned. Note this list is not
exhaustive:

-   A `BatchCreateResult` returning a `null` results list.
-   A `BatchCreateResult` returning a valid list that as a `null`
    element inside of it.
-   A `CreateResponse` returning a `null` for the `HttpStatus`.
-   A `BatchUpdateResult` returning a `null` key in the results map.
-   A `BatchUpdateResult` returning a `null` errors map.
-   A `BatchUpdateResult` returning a valid errors map, but with a
    `null` key or `null` value inside of it.

It is good practice to make sure that `null` is never returned in any
part of resource method responses, with the exception of RecordTemplate
classes, ACTION methods and GET methods.

<a id="wiki-ResourceContext"></a>

## ResourceContext

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

## Resource Templates

Resource Templates provide convenient methods for implementing resource
classes by extending them. Subclasses may selectively override relevant
methods and for methods that are not overridden, the framework will
recognize that your resource does not support this method and will
return a 404 if clients attempt to invoke it. Note that unsupported
methods will be omitted from your resources IDL (see [Restspec IDL](/rest.li/user_guide/restli_client#restspec-idl)
for details).

#### CollectionResourceTemplate

`CollectionResourceTemplate` provides a convenient base class for
collection resources. `CollectionResourceTemplate` defines methods for
all of the CRUD operations. Subclasses may also implement FINDER, BATCH_FINDER and
ACTION methods by annotating as described above. The asynchronous collection resource templates are `CollectionResourceTaskTemplate` and `CollectionResourceAsyncTemplate`.

```java
public CreateResponse create(V entity);
public BatchCreateResult<K, V> batchCreate(BatchCreateRequest<K, V> entities);
public V get(K key);
public Map<K, V>batchGet(Set<K> ids);
public UpdateResponse update(K key, V entity);
public BatchUpdateResult<K, V> batchUpdate(BatchUpdateRequest<K, V> entities);
public UpdateResponse update(K key, PatchRequest<V> patch);
public BatchUpdateResult<K, V> batchUpdate(BatchPatchRequest<K, V> patches);
public UpdateResponse delete(K key);
public BatchUpdateResult<K, V> batchDelete(BatchDeleteRequest<K, V> ids);
```

#### SimpleResourceTemplate

`SimpleResourceTemplate` provides a convenient base class for simple
resources. `SimpleResourceTemplate` defines methods for GET, UPDATE, and
DELETE methods. Subclasses may also implement ACTION methods by
annotating as described above. The asynchronous simple resource templates are `SimpleResourceTaskTemplate` and `SimpleResourceAsyncTemplate`.

```java
public V get();
public UpdateResponse update(V entity);
public UpdateResponse delete();
```

#### AssociationResourceTemplate

`AssociationResourceTemplate` provides a convenient base class for
association resources. `AssociationResourceTemplate` defines methods for
all of the CRUD operations except CREATE. Association resources should
implement CREATE by providing up-sert semantics on UPDATE. Subclasses
may also implement FINDER, BATCH_FINDER and ACTION methods by annotating as described
above. The asynchronous association resource templates are `AssociationResourceTaskTemplate` and `AssociationResourceAsyncTemplate`.

```java
public CreateResponse create(V entity);
public BatchCreateResult<CompoundKey, V>batchCreate(BatchCreateRequest<CompoundKey, V> entities);
public V get(CompoundKey key);
public Map<CompoundKey, V> batchGet(Set<CompoundKey> ids);
public UpdateResponse update(CompoundKey key, V entity);
public BatchUpdateResult<CompoundKey, V>batchUpdate(BatchUpdateRequest<CompoundKey, V> entities);
public UpdateResponse update(CompoundKey key, PatchRequest<V> patch);
public BatchUpdateResult<CompoundKey, V> batchUpdate(BatchPatchRequest<CompoundKey, V> patches);
public UpdateResponse delete(CompoundKey key);
public BatchUpdateResult<CompoundKey, V> batchDelete(BatchDeleteRequest<CompoundKey, V> ids);
```

<a id="wiki-FreeFormResources"></a>

## Free-Form Resources

Resource Templates provide a convenient way to implement the recommended
signatures for the basic CRUD operations (CREATE, GET, UPDATE,
PARTIAL_UPDATE, DELETE, and respective batch operations). When
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

```java
public class FreeFormCollectionResource implements KeyValueResource<K, V>
{
  @RestMethod.Create
  public CreateResponse myCreate(V entity);

  @RestMethod.BatchCreate
  public BatchCreateResult<K, V> myBatchCreate(BatchCreateRequest<K, V> entities);

  @RestMethod.Get
  public V myGet(K key);

  @RestMethod.GetAll
  public CollectionResult<V, M> myGetAll(@Context PagingContext pagingContex);

  @RestMethod.BatchGet
  public Map<K, V> myBatchGet(Set<K> ids);

  @RestMethod.Update
  public UpdateResponse myUpdate(K key, V entity);

  @RestMethod.BatchUpdate
  public BatchUpdateResult<K, V> myBatchUpdate(BatchUpdateRequest<K, V> entities);

  @RestMethod.PartialUpdate
  public UpdateResponse myUpdate(K key, PatchRequest<V> patch);

  @RestMethod.BatchPartialUpdate
  public BatchUpdateResult<K, V> myBatchUpdate(BatchPatchRequest<K, V> patches);

  @RestMethod.Delete
  public UpdateResponse myDelete(K key);

  @RestMethod.BatchDelete
  public BatchUpdateResult<K, V> myBatchDelete(BatchDeleteRequest<K, V> ids);
}
```

```java
public class FreeFormSimpleResource implements SingleObjectResource<V>
{
  @RestMethod.Get
  public V myGet();

  @RestMethod.Update
  public UpdateResponse myUpdate(V entity);

  @RestMethod.Delete
  public UpdateResponse myDelete();
}
```

The advantage of explicitly annotating each resource method is that you
can add custom query parameters (see description of `@QueryParam` for
FINDER resource method) and take advantage of wrapper return types.
Custom query parameters must be defined **after** the fixed parameters
shown above.

```java
@RestMethod.Get
public V myGet(K key, @QueryParam("myParam") String myParam);

@RestMethod.Get
public GetResult<V> getWithStatus(K key);
```

Note that each resource may only provide one implementation of each CRUD
method (for example, it is invalid to annotate two different methods with
@`RestMethod.Get`).

### Things to Remember about Free-Form Resources

-   Free-form resources allow you to add query parameters to CRUD
    methods.
-   Resource Templates should be used whenever possible.
-   Free-form resources must implement one of the `KeyValueResource` and
    `SingleObjectResource` marker interfaces.
-   Methods in free-form resources must be annotated with appropriate
    @`RestMethod.*` annotations.
-   Methods in free-form resources must use the same return type and
    initial signature as the corresponding Resource Template method.
-   Methods in free-form resources may add additional parameters
    **after** the fixed parameters.
-   Free-form resources may not define multiple implementations of the
    same resource method.

<a id="wiki-ReturningErrors"></a>

## Returning Errors

There are several mechanisms available for resources to report errors to
be returned to the caller. Regardless of which mechanism is used,
resources should be aware of the resulting HTTP status code and ensure
that meaningful status codes are used. Remember that `4xx` codes should
be used to report client errors (errors that the client may be able to
resolve), and `5xx` codes should be used to report server errors.

#### Return `null` for GET

If a resource method returns `null` for GET, the framework will
automatically generate a `404` response to be sent to the client.

Note that returning `null` for resource methods is generally forbidden
with the exception of GET and ACTION. Returning a `null` for a GET
returns a 404 and returning a `null` for an ACTION returns 200.

Returning a `null` for any other type of resource method will cause the
rest.li framework to return an HTTP `500` to be sent back to the client
with a message indicating 'Unexpected null encountered'. This is
described in detail above at <a href="#returning-nulls">Returning
Nulls</a>

#### Return Any HTTP Status Code in a CreateResponse/UpdateResponse

`CreateResponse` and `UpdateResponse` allow an [Http Status
Code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes) to be
provided. Status codes in the `4xx` and `5xx` ranges may be used to
report errors.

#### Throw RestLiServiceException to Return a 4xx/5xx HTTP Status Code

The framework defines a special exception class,
`RestLiServiceException`, which contains an [Http Status
Code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes) field, as
well as other fields that are returned to the client in the body of the
HTTP response. Resources may throw `RestLiServiceException` or a
subclass to prompt the framework to return an HTTP error response.

#### Throw Another Exception

All exceptions originating in application code are caught by the
framework and used to generate an HTTP response. If the exception does
not extend `RestLiServiceException`, an HTTP `500` response will be
sent.

#### Return Errors as Part of a BatchResult

BATCH_GET methods may return errors for individual items as part of a
`BatchResult` object. Each error is represented as a `RestLiServiceException`
object. In this case, the overall status will still be an HTTP `200`.

```java
public BatchResult<K, V> batchGet((Set<K> ids)
{
  Map<K, V> results = ...
  Map<K, RestLiServiceException> errors = ...
  ...
  return new BatchResult(results, errors);
}
```

If you want to return an error response with an overall status of `4xx` or `5xx`,
then you can do this by throwing a `RestLiServiceException` in the resource method.
In this case, the response will be an error response and won't contain any batch
information.

```java
public BatchResult<K, V> batchGet((Set<K> ids)
{
  throw new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST);
}
```

The same logic applies to BATCH_UPDATE, BATCH_PARTIAL_UPDATE, and BATCH_DELETE.

For BATCH_FINDER method, it may return errors for individual criteria as part of a `BatchFinderResult` object.
Each error is represented as a RestLiServiceException object when it cannot find a corresponding
response for that search criteria Or the developer can put the customized RestLiServiceException into the BatchFinderResult . 
In this case, the overall status will still be an HTTP 200.

### Handling Errors on the Client

When making requests using `RestClient`, a `ResponseFuture` is always
returned, as shown in this example:

```java
ResponseFuture<Greeting> future = restClient.sendRequest(new GreetingsBuilders.get().id(1L));
```

This future might contain an error response. When calling
`ResponseFuture.getResponse()`, the default behavior is for a
`RestLiResponseException` to be thrown if the response contains an error
response. Error responses are all 400 and 500 series HTTP status code,
as shown in this example:

```java
try
{
  Greeting greeting = restClient.sendRequest(new GreetingsBuilders.get().id(1L)).getResponseEntity();
  // handle successful response
}
catch (RestLiResponseException e)
{
  if(e.getStatus() == 400) {
    // handle 400
  } else {
    // ... handle other status codes or rethrow
  }
}
```

Alternatively, `ErrorHandlingBehavior.TREAT_SERVER_ERROR_AS_SUCCESS` can
be set when making a request. If set, `.getResponse()` will **not**
throw `RestLiResponseException` even if the response contains a 400 or
500 series HTTP status code, as shown in this example:

```java
Response<Greeting> response = restClient.sendRequest(new GreetingsBuilders.get().id(1L),
                                                     ErrorHandlingBehavior.TREAT_SERVER_ERROR_AS_SUCCESS).getResponse();
if(response.getStatus() == 200)
{
  // handle successful response
}
else if (response.getStatus() == 404)
{
  // handle 404
}
else
{
  // ... handle other status codes or rethrow
}
```

However because error responses do not contain an entity, calling
`ResponseFuture.getResponseEntity()` or `Response.getEntity()` will
**always** throw a `RestLiResponseException` for 400 or 500 series HTTP
status code, regardless of `ErrorHandlingBehavior`.

### Configuring How Errors are Represented in an HTTP Response

By default, Rest.li returns an extensive HTTP error response that
includes:

-   HTTP Status Code (manditory)
-   `X-LinkedIn-Error-Response` header (this will be renamed to
    `X-RestLi-Error-Response` shortly)
-   A response body containing:
    -   A full stack trace
    -   A service error code (optional)
    -   Application specific error details (optional)

The error response format configured to return only a subset of these
parts using RestLiConfig, as shown in this example:

```java
restLiConfig.setErrorResponseFormat(ErrorResponseFormat.MESSAGE_AND_DETAILS);
```

When Rest.li server application code throws an exception, if the
exception is of type RestLiServiceException, then the error message
provided by the RestLiServiceException is used for the error message in
the HTTP response. However if any other Java exception is thrown,
Rest.li automatically provides a default error message of "Error in
application code" in the error response. This default error message may
be customized via RestLiConfig as well, as shown in this example:

```java
restLiConfig.setInternalErrorMessage("Internal error, please try again later.");
```

See [this page](/rest.li/spec/service_errors#error-responses) for more information on
error responses in Rest.li.

<a id="wiki-Projections"></a>

## Field Projection

Rest.li provides built-in support for field projections, for example the
structural filtering of responses. The support includes [Java Projection Bindings](/rest.li/How-to-use-projections-in-Java)
and a [JSON Projection wire protocol](/rest.li/Projections). The
projection is applied separately to each entity object in the response
(i.e., to the value-type of the CollectionResource or
AssociationResource). If the invoked method is a FINDER that returns a
List, the projection is applied to each element of the list
individually. Likewise, if the invoked method is a BATCH_GET that
returns a Map<K, V>, the projection is applied to each value in the
map individually. Project can also be applied to CREATE and
BATCH_CREATE when the newly returned entity or entities are returned.

For resource methods that return CollectionResult, the Rest.li framework
also provides the ability to project the Metadata and as well as the
Paging that is sent back to the client. More info on Collection
Pagination is provided below.

The Rest.li server framework recognizes the "fields", "metadataFields",
or "pagingFields" query parameters in the request. If available, the
Rest.li framework then parses each of these as individual `MaskTrees`.
The resulting `MaskTrees` are available through the ResourceContext (see
above) or directly to the resource methods.

Projection can also be toggled between `AUTOMATIC` and `MANUAL`. The
latter precludes the Rest.li framework from performing any projection
while the former forces the Rest.li framework to perform the projection.

Additional details are described in [How to Use Projections in Java](/rest.li/How-to-use-projections-in-Java)

<a id="wiki-Pagination"></a>

## Collection Pagination

Rest.li provides helper methods to implement collection pagination, but
it requires each resource to implement core pagination logic itself.
Rest.li pagination uses positional indices to specify page boundaries.

The Rest.li server framework automatically recognizes the `"start"` and
`"count"` parameters for pagination, parses the values of these
parameters, and makes them available through a `PagingContext` object.
FINDER methods may request the `PagingContext` by declaring a method
parameter annotated with `@Context` (see above).

FINDER methods are expected to honor the `PagingContext` requirements,
for example, to return only the subset of results with logical indices
`>= start` and `< start+count`.

The Rest.li server framework also includes support for returning
CollectionMetadata as part of the response. CollectionMetadata includes
pagination info such as:

-   The requested `start`
-   The requested `count`
-   The `total` number of results (before pagination)
-   Links to the previous and next pages of results

FINDER methods that can provide the `total` number of matching results
should do so by returning an appropriate `CollectionResult` or
`BasicCollectionResult` object.

In order for the Rest.li framework to automatically construct `Link` objects,
certain conditions must be met. For both previous and next links, the `count`
in the request must be greater than `0`. For links to the previous page,
`start` must be greater than `0`. For links to the next page, the sum
of `start` and `count` must be less than the `total` number of results.
It's possible for the `total` property to be unspecified, but in this case
the `PageIncrement` property of the `CollectionResult` must be `RELATIVE`
and the amount of results returned by the resource method must match the
`count` desired in the request. The reasoning here is that the only way
for Rest.li to know that you're reaching the end of a collection of
results is if the amount of results returned differs from the amount
requested.

Here is an example request illustrating the use of start & count pagination parameters,
and resulting links in CollectionMetadata:

```bash
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
```

Note that "start" and "count" returned in CollectionMetadata is REQUEST
start and REQUEST count (that is​, the paging parameter passed from
incoming REQUEST, not metadata for the returned response). If start and
count is not passed in Finder or GetAll request, it will return default
0 for start and 10 for count.The rationale behind this is to make it
easier for a client to subsequently construct requests for additional
pages without having to track the start and count themselves.
Furthermore, there is no point to return a count for number of items
returned, since client can easily get that by calling size() for the
elements array returned.

<a id="wiki-DependencyInjection"></a>

## Dependency Injection

The Rest.li server framework controls the lifecycle of instances of
Resource classes, instantiating a new Resource object for each request.
It is therefore frequently necessary/desirable for resources to use a
dependency-injection mechanism to obtain the objects they depend upon,
for example, database connections or other resources.

Rest.li includes direct support for the following dependency injection frameworks:

-   [Spring](http://www.springsource.org/) using the [rest.li/spring bridge](/rest.li/Spring-Dependency-Injection)
-   [Guice](https://code.google.com/p/google-guice/) using the [rest.li/guicebridge](/rest.li/Guice-Dependency-Injection)

Other dependency injection frameworks can be used as well. Rest.li
provides an extensible dependency-injection mechanism, through the
`ResourceFactory` interface.

The most broadly used dependency injection mechanism is based on mapping
JSR-330 annotations to the Spring ApplicationContext, and it is provided
by the `InjectResourceFactory` from `restli-contrib-spring`. This is the
recommended approach.

Resource classes may annotate fields with `@Inject` or `@Named`. If only
`@Inject` is specified, the field will be bound to a bean from the
Spring ApplicationContext based on the type of the field. If `@Named` is
used, the field will be bound to a bean with the same name. All beans
must be in the root Spring context.


## Online Documentation

Rest.li has an on-line documentation generator that dynamically
generates resource IDL and PDL schemas hosted in the server. The
documentation is available in both HTML and JSON formats, and there are
three ways to access the documentation:

1.  HTML. The relative path to HTML documentation is `restli/docs/`. For
    example, the documentation URI for resource
    `http://<host>:<port>/<context-path>/<resource>` is
    `GET http://<host>:<port>/<context-path>/restli/docs/rest/<resource>`
    (`GET` is the [HTTP GET
    method](http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.3),
    which is the default for the web browser). The root URL, such as
    `http://<host>:<port>/<context-path>/restli/docs`, displays the list
    of all accessible resources and data schemas in the server. Use it
    as a starting point for HTML documentation. Remember to remove the
    `<context-path>` part if there is no context path.
2.  JSON. There are 2 alternative ways to access the raw JSON data:
    1.  Use the `format=json` query parameter on any of the HTML pages
        above. For example,
        `GET http://<host>:<port>/<context-path>/restli/docs/rest/<resource>?format=json`
        for resource documentation and
        `GET http://<host>:<port>/<context-path>/restli/docs/data/<full_name_of_data_schema>?format=json`
        for schema documentation. Homepage
        `GET http://<host>:<port>/<context-path>/restli/docs/?format=json`
        is also available, which aggregates all resources and data
        schemas.
    2.  Use the [HTTP OPTIONS
        method](http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.2).
        Simply replace the HTTP GET method with the OPTIONS method when
        accessing a resource without using the `format` query parameter.
        This approach only works for resources, and there is no need for
        the special `restli/docs/` path. For example,
        `OPTIONS http://<host>:<port>/<context-path>/<resource>`.

The JSON format is structured as following:

```js
{
  "models": {
    "<full_name_of_data_schema_1>": { pdsc_of_data_schema_1 },
    "<full_name_of_data_schema_2>": { pdsc_of_data_schema_2 }
  },
  "resources": {
    "<resource_1>": { idl_of_resource_1 },
    "<resource_2>": { idl_of_resource_2 }
  }
}
```

When accessing the JSON format of data schema, the `resources` key
exists but the value is always empty.

### Initialize Online Documentation Generator

-   `documentationRequestHandler`: instance of
    `RestLiDocumentationRequestHandler` class, default to null. Specify
    which implementation of documentation generator is used in the
    server. If null, the on-line documentation feature is disabled.
-   `serverNodeUri`: URI prefix of the server without trailing slash,
    default to empty string (""). The URI prefix is mainly used in the
    HTML documents by `DefaultDocumentationRequestHandler` to properly
    generate links. Usually, this should be an absolute path.
