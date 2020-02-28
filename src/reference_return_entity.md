---
layout: api_reference
title: Return entity in Rest.li
permalink: /spec/return_entity
index: 2
excerpt: This page describes returning the entity for resource methods that are not originally intended to return the entity.
---

# Return Entity in Rest.li

## Contents

-   [Supported Methods](#supported-methods)
-   [How to Enable](#how-to-enable)
-   [Client-Specified Behavior](#client-specified-behavior)
    -   [Query Parameter](#query-parameter)
    -   [Access from Resource Method](#access-from-resource-method)
    -   [Optimizations](#optimizations)

This page describes returning the entity for resource methods that are not originally intended to return the entity.
For example, returning the entity is normal behavior for GET and FINDER, so this page does not apply to them.

For methods such as CREATE, however, the created entity is not returned in the response because the client
already has the entity when sending the request. Despite this, there are use cases where the server will
attach additional data to the new entity. Returning the entity in the CREATE response saves the client
from having to make an extra GET request.

## Supported Methods

Currently, this extra functionality is supported for the following resource methods:

- CREATE
- PARTIAL_UPDATE
- BATCH_CREATE
- BATCH_PARTIAL_UPDATE

## How to Enable

To enable returning the entity for a given resource method, there are two requirements
that must be fulfilled.

First, the resource method must be annotated with the @`ReturnEntity` annotation.
This applies to all resource methods.

Second, the return type of the method must be a valid "Return Entity" return type.
This is specific to each resource method. The following table lists which "Return Entity"
return type corresponds to which resource method:

| Resource Method         | Standard Return Type | "Return Entity" Return Type     | More Info                                                                                       |
|-------------------------|----------------------|---------------------------------|-------------------------------------------------------------------------------------------------|
| CREATE                  | `CreateResponse`     | `CreateKVResponse`              | [Link](/rest.li/user_guide/restli_server#returning-entity-in-create-response)                   |
| PARTIAL_UPDATE          | `UpdateResponse`     | `UpdateEntityResponse`          | [Link](/rest.li/user_guide/restli_server#returning-entity-in-partial_update-response)           |
| BATCH_CREATE            | `BatchCreateResult`  | `BatchCreateKVResult`           | [Link](/rest.li/user_guide/restli_server#returning-entities-in-batch_create-response)           |
| BATCH_PARTIAL_UPDATE    | `BatchUpdateResult`  | `BatchUpdateEntityResult`       | [Link](/rest.li/user_guide/restli_server#returning-entities-in-batch_partial_update-response)   |

If both of these requirements are fulfilled, then the entity will be returned in the response by default.

For a resource method that has this behavior enabled, the application developer must make sure to populate
the returned object with the entity that's being returned. Returning an object without a non-null entity
when one is expected will cause a runtime exception.

#### Examples

Here is an example method signature for a CREATE resource method that will enable the entity to be returned.
Note how both the annotation and the required return type are present:

```java
@ReturnEntity
public CreateKVResponse create(V entity);
```

Here is an example implementation for the above method signature. Note how the returned entity is included
in the constructor of the returned `CreateKVResponse` object:

```java
@ReturnEntity
public CreateKVResponse<Long, Greeting> create(Greeting entity)
{
    Long id = 1L;
    entity.setId(id);
    return new CreateKVResponse<Long, Greeting>(entity.getId(), entity);
}
```

For more information on how to implement a "Return Entity" method for each resource method, see the "More Info"
links in the above table.

## Client-Specified Behavior

By default, all requests to a "Return Entity" resource method will return the entity in the response.
However, if the client decides that it doesn't want the entity to be returned (to reduce network traffic, for instance),
then the query parameter `$returnEntity` can be used to indicate this.

### Query Parameter

The value of this query parameter must be a boolean value, otherwise the server will treat it
as a bad request. A value of `true` indicates that the entity should be returned, a value of
`false` indicates that the entity shouldn't be returned, and omitting the query parameter
altogether defaults to treating the value as if it were `true`. Note that if the resource
method doesn't have a "Return Entity" return type, then the `$returnEntity` parameter will
be ignored, regardless of its value.

#### Examples

Here is an example of a PARTIAL_UPDATE curl request indicating that the entity shouldn't be returned in the response:

<code>
curl -X POST localhost:/fortunes/1?$returnEntity=false -d '{"patch": {"$set": {"fortune": "you will strike it rich!"}}}'
</code>

Here is an example in Java of how one would use this parameter when building a CREATE request,
making use of the request builder's `returnEntity(boolean value)` method:

```java
CreateIdEntityRequest<Long, Greeting> request = builders.createAndGet()
    .input(greeting)
    .returnEntity(false)
    .build();
```

See [more about request builders](/rest.li/user_guide/restli_client#built-in-request-and-requestbuilder-classes).

### Access from Resource Method

An application developer can access this query parameter in order to define their own
conditional behavior based on whether the entity should be returned. For ease of use,
the `ResourceContext` class provides a helper method that determines whether the entity is
to be returned or not. From within the resource method, the `ResourceContext#shouldReturnEntity`
method on the current resource context can be used to determine this. The method returns a boolean
value consistent with the logic specified in the above "Query Parameter" section of this documentation.

#### Examples

Here is an example implementation of a CREATE method that makes use of this helper method
to form conditional logic:

```java
@ReturnEntity
public CreateKVResponse<Long, Greeting> create(Greeting entity)
{
    
    final ResourceContext resourceContext = getContext();
    if (resourceContext.shouldReturnEntity())
    {
        // make upstream call..
        Long id = _upstream.getId(entity);
        entity.setId(id);
        return new CreateKVResponse<Long, Greeting>(entity.getId(), entity);
    }
    else
    {
        Long id = 1L;
        entity.setId(id);
        return new CreateKVResponse<Long, Greeting>(entity.getId(), null);
    }
}
```

### Optimizations

This feature can be harnessed by an application developer to optimize their service.
The obvious optimization is that potentially large payloads don't have to be
transmitted over the wire, reducing latency and network traffic. Another possible
optimization comes from the fact that the application developer can access this
query parameter from the resource method, allowing them to conditionally avoid
upstream service calls that would cause unnecessary slowdown.

<a id="BATCH_PARTIAL_UPDATE"></a>