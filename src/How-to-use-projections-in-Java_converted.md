---
layout: guide
title: Rest.li Projections in Java
permalink: /How-to-use-projections-in-Java
excerpt: Projections are a way for a client to request only specific fields from an object instead of the entire object. Using projections when the client needs a few fields from an object is a good way to self-document the code, reduce payload of responses, and even allow the server to relax an otherwise time consuming computation or IO operation.
---
# How to Use Projections in Java

## Contents

  - [What Are Projections](#what-are-projections)
  - [What Can Be Projected](#what-can-be-projected)
  - [Getting the PathSpec of a Field](#getting-the-pathspec-of-a-field)
  - [How To Make a REST Request with Projections using the Java
    Client](#how-to-make-a-rest-request-with-projections-using-the-java-client)
  - [Turning Off the Rest.li Framework’s AUTOMATIC
    Projection](#turning-off-the-restli-frameworks-automatic-projection)
  - [FAQ](#faq)

## What Are Projections

Projections are a way for a client to request only specific fields from an object instead of the entire object. Using projections when the client needs a few fields from an object is a good way to self-document the code, reduce payload of responses, and even allow the server to relax an otherwise time consuming computation or IO operation. You can read more about projections at [Projections](Projections).

## What Can Be Projected

Clients can project the entity object(s) in a response. For example, one
can project:

  - The RecordTemplate value provided in a GetResult from a GET
  - Each of the RecordTemplate objects in the values returned in a map
    by a BATCH_GET
  - Each of the RecordTemplate objects in a list returned by a FINDER
  - Each of the RecordTemplate objects in each CollectionResult returned by a BATCH_FINDER

For resource methods returning CollectionResult, the Metadata and Paging
that is sent back to the client may also be projected.

## Getting the PathSpec of a Field

Using projections in Java code relies heavily on `PathSpec` objects,
which represent specific fields of an object. To get a `PathSpec` of a
field *bar* of a RecordTemplate object *Foo*, you would write the
following:

```java
PathSpec pathSpec = Foo.fields().bar();
```

For Paging projection, here is an example on how to get the `PathSpec`
of the *total* field:

```java
PathSpec pathSpec = CollectionMetadata.fields().total();
```

It is not possible to set projections for non-RecordTemplate objects.

## How To Make a REST Request with Projections using the Java Client

Projections are set by the request builder. To set a request projection
for entity objects in the response, create your builder as you normally
would and then add your projection to it:

```java
builder.fields(pathSpec);
```

the `fields()` method can take as arguments any number of PathSpecs, or
an array of them.

```java
builder.fields(pathSpec1, pathSpec2, pathSpec3);

builder.fields(pathSpecArray);
```

This will create a positive projection for your given fields. The
request will only return fields that you have specified with
`.fields(...)`.

Similarly, you can do the same for custom Metadata projection and Paging
projection for resource methods that return `CollectionResult`, for
example:

```java
builder.metadataFields(pathSpec1, pathSpec2);
builder.pagingFields(CollectionMetadata.fields().total());
```

## Turning Off the Rest.li Framework’s `AUTOMATIC` Projection

If you choose to examine and apply projections manually, or if you
simply would like to disable them for performance optimization, you can
turn off the framework’s `AUTOMATIC` projection processing.

This can be done by setting the “projection mode” to `MANUAL` on the
ResourceContext:

```java
//For entity objects in the response
getContext().setProjectionMode(ProjectionMode.MANUAL);

//For custom Metadata projection (CollectionResult only)
getContext().setMetadataProjectionMode(ProjectionMode.MANUAL);
```

For example:

```java
public Greeting get(Long key)
{
  MaskTree mask = context.getProjectionMask();
  if (mask != null)
  {
    // client has requested a projection of the entity
    getContext().setProjectionMode(ProjectionMode.MANUAL); // since we’re manually applying the projection
    // manually examine the projection and apply it entity before returning
    // here we can take advantage of the information the projection provides to only load the data the
    // client requested
  }
  else
  {
    // client is requesting the full entity
    // construct and return the full entity
  }
}
```

and in the case of CollectionResult, you could do the following as well:

```java
@Finder("myFinder")
public CollectionResult<SomeEntity, SomeCustomEntity> myFinderResourceMethod(
  final @PagingContextParam PagingContext ctx,
  final @ProjectionParam MaskTree entityObjectProjection,
  final @MetadataProjectionParam MaskTree metadataProjection,
  final @PagingProjectionParam MaskTree pagingProjection)
{

  final List<SomeEntity> responseList = new ArrayList<>();
  if (entityObjectProjection != null)
  {
    // client has requested a projection of the entity
    getContext().setProjectionMode(ProjectionMode.MANUAL); // since we’re manually applying the projection

    // manually examine the projection and apply it entity before returning
    // here we can take advantage of the information the projection provides to only load the data the
    // client requested
    responseList.addAll(fetchFiltereredEntities());
  }
  else
  {
    // client is requesting the full entities
    // construct the full entities
    responseList.addAll(fetchEntities());
  }

  final SomeCustomEntity customEntity;
  if (metadataProjection != null)
  {
    // client has requested a projection of the custom metadata
    getContext().setMetadataProjectionMode(ProjectionMode.MANUAL); // since we’re manually applying the meta data projection

    // manually examine the projection and apply it entity before returning
    // here we can take advantage of the information the projection provides to only load the data the
    // client requested
    customEntity = fetchSomeFilteredCustomEntity();
  }
  else
  {
    // client is requesting the full metadata entity
    // construct and return the full metadata
    customEntity = fetchSomeCustomEntity();
  }

  final Integer total;
  if (pagingProjection != null)
  {
    // client has requested a projection of the paging information
    // since the rest.li framework will always automatically project paging,
    // we can still selectively calculate the total based on the path spec
    if(pagingProjections.getOperations.get(CollectionMetadata.fields().total()) == MaskOperation.POSITIVE_MASK_OP)
    {
      total = calculateTimeConsumingTotal();
    }
  }
  else
  {
    total = null;
  }

  return new CollectionResult(responseList, total, customEntity);
}

```
Note that Paging projection is always automatically applied by the
Rest.li framework if there is a request by the client to do so. This is
because it is the Rest.li framework who is responsible for constructing
the pagination (`CollectionMetadata`) which includes items such as the
next/prev links . The `MaskTree` provided for Paging is simply provided
as a reference to the resource method with the most common use case
being whether or not to pass `null` for the total in the construction of
`CollectionResult`.

## FAQ

#### Is it possible to create a negative projection if I want all but a few fields?

No. If you want a large number of fields, you will need to include them
all in the `.fields(...)` method
call.

#### If a field’s type is itself a RecordTemplate, can I create a projection on it?

Yes, the simplest way to is to use the `RecordTemplate.fields()` method
to help construct the appropriate pathspec to pass to the builder’s
`.fields(...)` method call. For example:

```java
new ExampleBuilders(options).get()
  .id(id)
  .fields(RootRecord.fields().message().id())
  .build()
```

Applies projection on a GET request to a resource where the `message`
field of `RootRecord.pdl` is a record type called `Message.pdl`, and
only the `id` fields of the message is being projected. The same logic
can be applied to RecordTemplates within custom Metadata and Paging
projection.

#### Can I examine a request’s projections on the server side?

In general, examining a request’s projections on the server side will
not be necessary. When the server returns an object to the client, the
REST framework will take care of stripping all unrequested fields. It is
not necessary for the server to examine the projection and strip fields
itself.

However, it is possible for the server to examine a request’s
projection.

```java
MaskTree entityProjection = getContext().getProjectionMask();
MaskTree metadataProjection = getContext().getMetadataProjectionMask();
MaskTree pagingProjection = getContext().getPagingProjectionMask();
```

Or, if you are using free-form resources, you can get the same
`MaskTree` by having it injected in, for example:

```java
@RestMethod.Get
public Greeting get(Long key, @ProjectionParam MaskTree projection)
{
  // …
}
```

This will get you all possible projections of a request. If there were
no projections available, the respective `MaskTrees` would be `null`.
Note that the use of these annotations is mandatory if you specify
`MaskTrees` in your method signatures.

If there were projections, you can check the status of each field.

```java
MaskOperation mask = projections.getOperations.get(pathSpec);

if (mask == MaskOperation.POSITIVE_MASK_OP)
{
  // field is requested.
}
else
{
  // field is not requested
}

MaskOperation totalMask = pagingProjections.getOperations.get(CollectionMetadata.fields().total());

if (totalMask == MaskOperation.POSITIVE_MASK_OP)
{
  // the total field in pagination is requested.
}
else
{
  // total is not requested
}
```

You can use this information in whatever way you wish to. For example,
resource methods may choose to exclude the calculation of ‘total’
(thereby passing `null` for total into `CollectionResult`) if the client
decided they didn’t need it.
