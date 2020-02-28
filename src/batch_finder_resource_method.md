---
layout: api_reference
title: Batch Finder Resource Method
permalink: /batch_finder_resource_method
excerpt: This documentation describes how restli framework support batch of search requirement.
---

# Batch Finder Resource Method

## Contents

-   [Overview](#overview)
-   [Protocol](#protocol)
    -   [Request](#request)
    -   [Response](#response)
-   [Client](#client)
    -   [Java Request Builders](#java-request-builders)    
-   [Resource API](#resource-api)
    -   [Criteria Filter](#criteria-filter)
    -   [Method Annotation and Parameters](#method-annotation-and-parameters)
    -   [BatchFinderResult](#batchfinderresult)
    -   [Error Handling](#error-handling)
-   [FAQ](#faq) 
    
## Overview   
The BATCH_FINDER resource method accepts a list of filters set. 
Instead of callings multiple finders with different filter values, we call 1 BATCH_FINDER method with a list of filters.

For example, a client might want to call the same FINDER with different search criteria.
Combining multiple individual requests into a single batch request can save the application significant network latency.
Also, the server can execute searches more efficiently if they are combined as a single query.

BATCH FINDER should not have any visible side effects.
For example, it should be safe to call whenever the client wishes.
However, this is not something enforced by the framework, and it is up to the application developer that there are no side effects.

It is important to note that:
- the operations may execute on the server out of order
- the response objects are expected to be returned in the same order and position as the respective input search criteria.
- The BATCH_FINDER will require implementing a resource method to handle a BATCH_FINDER requests. It won't behave like a multiplexer that will call automatically existing finders

## Protocol

### Request

See more details here [BatchFinderUri](/rest.li/spec/protocol#batch-finders).

### Response

See more details here [BatchCollectionResponse](/rest.li/spec/protocol#batch-collection-response).
    
    
## Client

### Java Request Builders

The client framework includes a code-generation tool that reads the IDL (see [Restspec IDL](/rest.li/user_guide/restli_client#restspec-idl) for details) and generates type-safe Java binding for each resource and its supported methods.
The bindings are represented as RequestBuilder classes.  
For each resource described in an IDL file, a corresponding builder factory will be generated.
The factory contains a factory method for each resource method supported by the resource. 
The factory method returns a request builder object with type-safe bindings for the given method.
More details in [Resource Builder Factory](/rest.li/user_guide/restli_client#resource-builder-factory).

For example:

```java
// Request builders factory class
// which provides all the specific request builders to corresponding resource method in defined resource class
public class GreetingsRequestBuilders extends BuilderBase {
    public GreetingsRequestBuilders()
    public GreetingsRequestBuilders(String primaryResourceName)
    public GreetingsCreateRequestBuilder create()
    public GreetingsGetRequestBuilder get()
    ...
    public GreetingsFindBySearchRequestBuilder findBySearch()
    // the BATCH_FINDER resource method named "SearchGreetings"
    public GreetingsBatchFindBySearchGreetingsRequestBuilder batchFindBySearchGreetings() 
}
```

The request builders factory class provides a method to generate the corresponding BATCH_FINDER request builder.
More details about generated class and method declaration in
[BATCH FINDER Request Builder](/rest.li/user_guide/restli_client#batch-finder-request-builder).

```java
@Generated(value = "com.linkedin.pegasus.generator.JavaCodeUtil", comments = "Rest.li Request Builder")
public class BatchfindersBatchFindBySearchGreetingsBuilder
    extends BatchFindRequestBuilderBase<Long, Greeting, BatchfindersBatchFindBySearchGreetingsBuilder>
{

    public BatchfindersBatchFindBySearchGreetingsBuilder(java.lang.String baseUriTemplate, ResourceSpec resourceSpec, RestliRequestOptions requestOptions) {
        super(baseUriTemplate, Greeting.class, resourceSpec, requestOptions);
        super.name("searchGreetings");
    }
    
    // the 1st way to set the batch query parameter
    public BatchfindersBatchFindBySearchGreetingsBuilder criteriaParam(Iterable<GreetingCriteria> value) {
        super.setReqParam("criteria", value, GreetingCriteria.class);
        return this;
    }

    // the 2nd way to set the batch query parameter
    public BatchfindersBatchFindBySearchGreetingsBuilder addCriteriaParam(GreetingCriteria value) {
        super.addReqParam("criteria", value, GreetingCriteria.class);
        return this;
    }

    // set common query parameter
    public BatchfindersBatchFindBySearchGreetingsBuilder messageParam(java.lang.String value) {
        super.setReqParam("message", value, java.lang.String.class);
        return this;
    }
}
```

Here is an example to show how to use request builder to build BATCH_FINDER request.

```java
  @Test
  public void testUsingResourceBuilder() throws RemoteInvocationException {
    // define batch search criteria
    GreetingCriteria c1 = new GreetingCriteria().setId(1L).setTone(Tone.SINCERE);
    GreetingCriteria c2 = new GreetingCriteria().setId(2L).setTone(Tone.FRIENDLY);
    GreetingCriteria c3 = new GreetingCriteria().setId(100);
    
    //set batch query parameter and common query parameter
    Request<BatchCollectionResponse<Greeting>> req = new GreetingsRequestBuilders().batchFindBySearchGreetings()
        .criteriaParam(Arrays.asList(c1, c2, c3)).messageParam("hello world").build();
    Response<BatchCollectionResponse<Greeting>> resp = REST_CLIENT.sendRequest(req).getResponse();
    BatchCollectionResponse<Greeting> response = resp.getEntity();
  
    List<BatchFinderCriteriaResult<Greeting>> batchResult = response.getResults();
  
    Assert.assertEquals(batchResult.size(), 3);
  
    // on success
    List<Greeting> greetings1 = batchResult.get(0).getElements();
    Assert.assertTrue(greetings1.get(0).hasTone());
    Assert.assertTrue(greetings1.get(0).getTone().equals(Tone.SINCERE));
  
    // on error
    ErrorResponse error = batchResult.get(2).getError();
    Assert.assertTrue(batchResult.get(2).isError());
    Assert.assertEquals(error.getMessage(), "Fail to find Greeting!");
  }
```

## Resource API
BATCH_FINDER is supported on Collection and Association Resources only(See more details about
[Collection Resource](/rest.li/spec/protocol#collection-resources) and
[Association Resource](/rest.li/spec/protocol#association-resources)).
Resources may provide zero or more BATCH_FINDER resource methods. Each BATCH_FINDER method must
be annotated with the `@BatchFinder` annotation.

Pagination default to start=0 and count=10. Clients may set both of these parameters to any desired value.
 
### Criteria Filter
To implement a batch finder, the resource owner has to define a `RecordTemplate` to define a criteria filter parameter.
The batch finder method will have to accept a array of this criteria filter.

Example:

The resource owner need to define their own search criteria `GreetingCriteria.pdl` file.

```pdl
namespace com.linkedin.restli.examples.greetings.api

/**
 * A search criteria to filter greetings.
 */
record GreetingCriteria {

  /**
   * Greeting ID to filter on
   */
  id: long

  /**
   * Greeting tone to filter on
   */
  tone: Tone
}
```

The `GreetingCriteria` class represents a set of criteria (`id` and `tone`) by which to filter.
This Java class is auto-generated from the schema.

```java
public class GreetingCriteria extends RecordTemplate
{

    private final static GreetingCriteria.Fields _fields = new GreetingCriteria.Fields();
    private final static RecordDataSchema SCHEMA = ((RecordDataSchema) DataTemplateUtil.parseSchema("{\"type\":\"record\",\"name\":\"GreetingCriteria\",\"namespace\":\"com.linkedin.restli.examples.greetings.api\",\"doc\":\"A search criteria to filter greetings.\",\"fields\":[{\"name\":\"id\",\"type\":\"long\",\"doc\":\"Greeting ID to filter on\"},{\"name\":\"tone\",\"type\":{\"type\":\"enum\",\"name\":\"Tone\",\"symbols\":[\"FRIENDLY\",\"SINCERE\",\"INSULTING\"]},\"doc\":\"Greeting tone to filter on\"}]}"));
    private final static RecordDataSchema.Field FIELD_Id = SCHEMA.getField("id");
    private final static RecordDataSchema.Field FIELD_Tone = SCHEMA.getField("tone");
    ...
}
```
 
### Method Annotation and Parameters

The `@BatchFinder` annotation takes 2 required parameter:
- `value` : which indicates the BATCH_FINDER method name
- `batchParam` : which indicates the name of the batch criteria parameter, each BATCH_FINDER method must have and can only have one batch parameter

For example: 

```java
// eg. The curl call for this resource method is like:
// curl -v -X GET http://localhost:8080/greetings?bq=searchGreetings&criteria=List((id:1,tone:SINCERE),(id:2,tone:FRIENDLY))&message=hello -H 'X-RestLi-Protocol-Version: 2.0.0' 

@BatchFinder(value = "searchGreetings", batchParam = "criteria")
public Task<BatchFinderResult<GreetingCriteria, Greeting, EmptyRecord>> searchGreetings(@PagingContextParam PagingContext context,
                                                             @QueryParam("criteria") GreetingCriteria[] criteria,
                                                             @QueryParam("message") String message)
{
 return Task.blocking("searchGreetings", () -> {
     BatchFinderResult<GreetingCriteria, Greeting, EmptyRecord> batchFinderResult = new BatchFinderResult<>();
 
     for (GreetingCriteria currentCriteria: criteria) {
       if (currentCriteria.getId() == 1L) {
         // on success
         CollectionResult<Greeting, EmptyRecord> c1 = new CollectionResult<Greeting, EmptyRecord>(Arrays.asList(g1), 1);
         batchFinderResult.putResult(currentCriteria, c1);
       } else if (currentCriteria.getId() == 2L) {
         CollectionResult<Greeting, EmptyRecord> c2 = new CollectionResult<Greeting, EmptyRecord>(Arrays.asList(g2), 1);
         batchFinderResult.putResult(currentCriteria, c2);
       } else if (currentCriteria.getId() == 100L){
         // on error: to construct error response for test
         batchFinderResult.putError(currentCriteria, new RestLiServiceException(HttpStatus.S_404_NOT_FOUND, "Fail to find Greeting!"));
       }
     }
 
     return batchFinderResult;
 }, _executor);

}
```

Every parameter of a BATCH_FINDER method must be annotated with one of:

-   `@Context` - indicates that the parameter provides framework context
   to the method. Currently all `@Context` parameters must be of type
   `PagingContext`.
-   `@QueryParam` - indicates that the value of the parameter is
   obtained from a request query parameter. The value of the annotation
   indicates the name of the query parameter. Duplicate names are not
   allowed for the same BATCH_FINDER method.
   For the batch parameter, the name must match the name in the method annotation.
-   `@AssocKey` - indicates that the value of the parameter is a partial
   association key, obtained from the request. The value of the
   annotation indicates the name of the association key, which must
   match the name of an `@Key` provided in the `assocKeys` field of the
   `@RestLiAssociation` annotation.

Parameters marked with `@QueryParam` and `@AssocKey`
may also be annotated with `@Optional`, which indicates that the
parameter is not required. *caution*: the batch parameter can not be optional.
The `@Optional` annotation may specify a String value, indicating the default value to be used if the parameter
is not provided in the request. If the method parameter is of primitive
type, a default value must be specified in the `@Optional` annotation.

Valid types for regular query parameters are:

-   `String`
-   `boolean` / `Boolean`
-   `int` / `Integer`
-   `long` / `Long`
-   `float` / `Float`
-   `double` / `Double`
-   A Pegasus Enum (any enum defined in a `.pdl` schema)
-   Custom types (see the bottom of this section)
-   Record template types (any subclass of `RecordTemplate` generated
   from a `.pdl` schema)
-   Arrays of one of the types above, e.g. `String[]`, `long[]`, ...

Valid type for batch criteria parameter:

- Can only be Arrays of Record template type, if have to use some other data types like Pegasus Enum, etc as the array item,
need to wrap it into a Record Template (`.pdl` schema)
  
### BatchFinderResult

BATCH_Finder methods must return `BatchFinderResult<QK extends RecordTemplate, V extends RecordTemplate, MD extends RecordTemplate>`:

- `QK` : The type of the BATCH_FINDER criteria filter
- `V` :  The type of the resource, aka, the entity type
- `MD` : The type of the meta data, if do not need metadata, just set it `EmptyRecord`

For each search criteria in the BatchFinderRequest, it can get either a successful reponse
which is a `CollectionResult`(a list of entities), Or an error/failure which maybe represented by
a `RestLiServiceException`, which will be wrapped into an `ErrorResponse` later when building BatchFinderResponse
to return to client.

```java
public class BatchFinderResult<QK,V extends RecordTemplate,MD extends RecordTemplate>
{
   private final Map<QK,CollectionResult<V,R>> _elements;
   private final Map<QK,RestLiServiceException> _errors;
   ...
}
```

### Error Handling

#### 1) Custom error per search criteria

For each input criteria, the developer is responsible to update either the "_elements" or the "_errors" map in `BatchFinderResult`.
If the developers set a customized error which is wrapped into a `RestLiServiceException` for one search criteria,
Rest.li framework will not treat it as a failure for the whole BATCH_FINDER request, but just the failure for that specific criteria.
The return http status for the BATCH_FINDER request is still 200. An example is below
[Resource API](/rest.li/batch_finder_resource_method#resource-api).

#### 2) Rest.li framework will cover the non-present criteria error

When processing the `BatchFinderResult` in the ResponseBuilder, if a criteria is not present, either in _elements, nor in _errors, 
the framework will generate a "404" error for this criteria.
The whole http status is still 200.

```java
new RestLiServiceException(S_404_NOT_FOUND, "The server didn't find a representation for this criteria"));
```

#### 3) Return nulls

In some situation, the return results may contain null value. Resource methods should never explicitly return null. 
If the Rest.li framework detects this, it will return an HTTP 500 back to the client with a message indicating
‘Unexpected null encountered’. See more details in [Returning Nulls](/rest.li/user_guide/restli_server#returning-nulls).

Here are some possible cases:
- `BatchFinderResult` is null.
- Element is null in the returned list of entities in the successful case.
- For one criteria, the whole list of entities is null.

## FAQ

#### Does Batch_Finder support primitive type like String, Integer as a batch criteria filter?

No. 
We currently don’t support primitive data type as batch criteria, even `Enum` type.
That criteria must be a type which extends from `RecordTemplate` which is actually a record.
The reason is that using a record will be easier to support any evolution .i.e. adding additional criteria to the same batch finder method.