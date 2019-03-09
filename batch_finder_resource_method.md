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
        -   [URI](#uri)
        -   [Java Request Builders](#java-request-builders)
    -   [Response](#response)
        -   [Reponse Body](#batchcollectionresponse)
        -   [Error Handling](#error-handling)
-   [Resource API](#resource-api)
    -   [Method Annotation and Parameters](#method-annotation-and-parameters)
    -   [Criteria Filter](#criteria-filter)
    -   [BatchFinderResult](#batchfinderresult)
-   [Future consideration](#future-consideration) 
    
## Overview   
The BATCH_FINDER allows combining multiple requests to one FINDER within a Rest.li resource into a single call. 

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
#### URI
The URI templates below assume variables with types as follows:

    batch_finder : simple string identifying a batch_finder method name
    resource : simple string identifying a resource 
    search_criteria : simple string identifying the criteria filter name

| Resource                           | URI Template          | Example                   | Method | Semantics                    |
| ---------------------------------- | --------------------- | ------------------------- | ------ | ---------------------------- |
| Collection, Association            | {resource}?bq={batch_finder}&{search_criteria}=| /PhotoResource?bq=searchPhotos&photoCriteria=List((id:1, format:JPG),(id:2, format:BMP)) | GET    | invokes the specified batch_finder |

At least, 2 query parameters will have to be set for a batch finder:

- The "bq" query parameter is reserved for passing the batch finder method name
- A second query parameter will be used to pass a set of different search criteria. The name of this query parameter is set in the [BatchFinder method annotation](/rest.li/batch_finder_resource_method#method-annotation-and-parameters).
For example, with @BatchFinder(value="findUsers", batchParam="batchCriteria"), the batch query parameter name is "batchCriteria". 
The type of this query parameter is a List.

Eg.

    curl "http://localhost:8080/userSearchResults?bq=findUsers&batchCriteria=List((firstName:pauline, age:12),(lastName:iglou))" -X GET

The other query parameters will be applied as common filters across all batch requests.

Here is an example batch request with two individual finders using the following criteria:

- filter by first name and age
- filter by last name and age

Eg.

    curl "http://localhost:8080/userSearchResults?bq=findUsers&batchCriteria=List((firstName:pauline),(lastName:iglou))&age=21" -X GET

#### Pagination support
##### 1) Common pagination for all search criteria  
The developer can pass additional parameters to specify a common pagination. It will be more efficient than adding a pagination context inside each criteria object.  
Eg.

    curl "http://localhost:8080/userSearchResults?q=findUsers&batchCriteria=List((firstName:pauline, age:12),(lastName:iglou))&firstName=max&start=10&count=10" -X GET

The "start" and "count" params will be automatically mapped to a `PagingContext` object that will be passed to the resource method. 
```java
public BatchFinderResult<SearchCriteria, User, EmptyRecord> findUsers(@PagingContextParam PagingContext context, 
                                                                      @QueryParam("batchCriteria") SearchCriteria[] criteria, 
                                                                      @QueryParam("firstName") String firstName)
```

##### 2) Custom pagination per criteria object 
If the developer wants to apply a custom pagination for each search criteria, the pagination information can be passed into the the search criteria object itself.  
**Caution:** Rest.li doesn't validate how the developer models the pagination in the Search criteria RecordTemple. For consistency purpose, we recommend to use a `PagingContext`.  
It's the developer responsibility to apply the right pagination (common or custom) based on its need in the resource method implementation.

#### Java Request Builders
The client framework includes a code-generation tool that reads the IDL(see [Restspec IDL](/rest.li/user_guide/restli_client#restspec-idl) for details) and generates type-safe Java binding for each resource and its supported methods.
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
More details about generated class and method declaration in [BATCH FINDER Request Builder](/rest.li/user_guide/restli_client#batch-finder-request-builder).
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

### Response
#### BatchCollectionResponse
A list of `BatchFinderCriteriaResult` are returned in a `BatchCollectionResponse` wrapper. 
It is used for returning an ordered, variable-length, navigable collection of resources for BATCH_FINDER.
This means, `BatchFinderCriteriaResult` objects are expected to be returned in the same order and position as the respective input search criteria.

For each batchFinder search criteria, it will either return a successful `CollectionResponse` which contains a list of entities Or 
an `ErrorResponse` in failing case. Such 2 kinds cases are wrapped into `BatchFinderCriteriaResult` corresponding to 
each search criteria.

`BatchFinderCriteriaResult` fields:

- (optional) "elements" : JSON serialized list of entity types (in success case)
- (optional) "metadata":
- (optional) "paging" : JSON serialized CollectionMetadata object
- (optional) "error" : it's an ErrorResponse which fail to get a list of entities to corresponding search criteria(in failure)
- "isError" : which indicates whether the result is a successful case or not

E.g.

    HTTP/1.1 200 OK Content-Type: application/jsonX-RestLi-Protocol-Version: 2.0.0
    {
      "elements" : [ {
        "elements" : [ { // in success case: return a list of entities
          "urn" : "foo",
          "format" : "JPG",
          "id" : 9,
          "title" : "baz",
          "exif" : { }
        }, {
          "urn" : "foo",
          "format" : "JPG",
          "id" : 10,
          "title" : "bar",
          "exif" : { }
        } ],
        "paging" : {
          "total" : 2,
          "count" : 10,
          "start" : 0,
          "links" : [ 
          {           
            "href": "/PhotoResource?PhotoCriteria=List((urn:foo, format:JPG))&start=1&count=1&bq=searchPhotos",
            "type": "application/json",
            "rel": "next"
          ]
        }
      }, { // in failure : return an ErrorResponse
        "isError" : true,
        "elements" : [ ],
        "error" : {
          "exceptionClass" : "com.linkedin.restli.server.RestLiServiceException",
          "stackTrace" : "com.linkedin.restli.server.RestLiServiceException [HTTP Status:404]: The server didn't find a representation for this criteria\n\tat com.linkedin.restli.internal.server.response.BatchFinderResponseBuilder.buildRestLiResponseData(BatchFinderResponseBuilder.java:127)\n\tat com.linkedin.restli.internal.server.response.RestLiResponseHandler.buildRestLiResponseData(RestLiResponseHandler.java:232)\n\tat com.linkedin.restli.docgen.examplegen.ExampleRequestResponseGenerator.buildResponse(ExampleRequestResponseGenerator.java:600)\n\tat com.linkedin.restli.docgen.examplegen.ExampleRequestResponseGenerator.buildRequestResponse(ExampleRequestResponseGenerator.java:528)\n\tat com.linkedin.restli.docgen.examplegen.ExampleRequestResponseGenerator.batchFinder(ExampleRequestResponseGenerator.java:282)\n\tat com.linkedin.restli.docgen.RestLiHTMLDocumentationRenderer.renderResource(RestLiHTMLDocumentationRenderer.java:144)\n\tat com.linkedin.restli.docgen.DefaultDocumentationRequestHandler.processDocumentationRequest(DefaultDocumentationRequestHandler.java:190)\n\tat com.linkedin.restli.docgen.DefaultDocumentationRequestHandler.handleRequest(DefaultDocumentationRequestHandler.java:87)\n\tat com.linkedin.restli.server.RestRestLiServer.doHandleRequest(RestRestLiServer.java:115)\n\tat com.linkedin.restli.server.RestRestLiServer.handleRequest(RestRestLiServer.java:95)\n\tat com.linkedin.restli.server.RestLiServer.handleRequest(RestLiServer.java:130)\n\tat com.linkedin.restli.server.DelegatingTransportDispatcher.handleRestRequest(DelegatingTransportDispatcher.java:70)\n\tat com.linkedin.r2.filter.transport.DispatcherRequestFilter.onRestRequest(DispatcherRequestFilter.java:67)\n\tat com.linkedin.r2.filter.TimedRestFilter.onRestRequest(TimedRestFilter.java:61)\n\tat com.linkedin.r2.filter.FilterChainIterator$FilterChainRestIterator.doOnRequest(FilterChainIterator.java:146)\n\tat com.linkedin.r2.filter.FilterChainIterator$FilterChainRestIterator.doOnRequest(FilterChainIterator.java:132)\n\tat com.linkedin.r2.filter.FilterChainIterator.onRequest(FilterChainIterator.java:62)\n\tat com.linkedin.r2.filter.TimedNextFilter.onRequest(TimedNextFilter.java:55)\n\tat com.linkedin.r2.filter.transport.ServerQueryTunnelFilter.onRestRequest(ServerQueryTunnelFilter.java:58)\n\tat com.linkedin.r2.filter.TimedRestFilter.onRestRequest(TimedRestFilter.java:61)\n\tat com.linkedin.r2.filter.FilterChainIterator$FilterChainRestIterator.doOnRequest(FilterChainIterator.java:146)\n\tat com.linkedin.r2.filter.FilterChainIterator$FilterChainRestIterator.doOnRequest(FilterChainIterator.java:132)\n\tat com.linkedin.r2.filter.FilterChainIterator.onRequest(FilterChainIterator.java:62)\n\tat com.linkedin.r2.filter.TimedNextFilter.onRequest(TimedNextFilter.java:55)\n\tat com.linkedin.r2.filter.message.rest.RestFilter.onRestRequest(RestFilter.java:50)\n\tat com.linkedin.r2.filter.TimedRestFilter.onRestRequest(TimedRestFilter.java:61)\n\tat com.linkedin.r2.filter.FilterChainIterator$FilterChainRestIterator.doOnRequest(FilterChainIterator.java:146)\n\tat com.linkedin.r2.filter.FilterChainIterator$FilterChainRestIterator.doOnRequest(FilterChainIterator.java:132)\n\tat com.linkedin.r2.filter.FilterChainIterator.onRequest(FilterChainIterator.java:62)\n\tat com.linkedin.r2.filter.FilterChainImpl.onRestRequest(FilterChainImpl.java:96)\n\tat com.linkedin.r2.filter.transport.FilterChainDispatcher.handleRestRequest(FilterChainDispatcher.java:70)\n\tat com.linkedin.r2.transport.http.server.HttpDispatcher.handleRequest(HttpDispatcher.java:95)\n\tat com.linkedin.r2.transport.http.server.AbstractR2Servlet.service(AbstractR2Servlet.java:105)\n\tat javax.servlet.http.HttpServlet.service(HttpServlet.java:790)\n\tat org.eclipse.jetty.servlet.ServletHolder.handle(ServletHolder.java:848)\n\tat org.eclipse.jetty.servlet.ServletHandler.doHandle(ServletHandler.java:584)\n\tat org.eclipse.jetty.server.session.SessionHandler.doHandle(SessionHandler.java:224)\n\tat org.eclipse.jetty.server.handler.ContextHandler.doHandle(ContextHandler.java:1180)\n\tat org.eclipse.jetty.servlet.ServletHandler.doScope(ServletHandler.java:512)\n\tat org.eclipse.jetty.server.session.SessionHandler.doScope(SessionHandler.java:185)\n\tat org.eclipse.jetty.server.handler.ContextHandler.doScope(ContextHandler.java:1112)\n\tat org.eclipse.jetty.server.handler.ScopedHandler.handle(ScopedHandler.java:141)\n\tat org.eclipse.jetty.server.handler.HandlerWrapper.handle(HandlerWrapper.java:134)\n\tat org.eclipse.jetty.server.Server.handle(Server.java:534)\n\tat org.eclipse.jetty.server.HttpChannel.handle(HttpChannel.java:333)\n\tat org.eclipse.jetty.server.HttpConnection.onFillable(HttpConnection.java:251)\n\tat org.eclipse.jetty.io.AbstractConnection$ReadCallback.succeeded(AbstractConnection.java:283)\n\tat org.eclipse.jetty.io.FillInterest.fillable(FillInterest.java:108)\n\tat org.eclipse.jetty.io.SelectChannelEndPoint$2.run(SelectChannelEndPoint.java:93)\n\tat org.eclipse.jetty.util.thread.strategy.ExecuteProduceConsume.executeProduceConsume(ExecuteProduceConsume.java:303)\n\tat org.eclipse.jetty.util.thread.strategy.ExecuteProduceConsume.produceConsume(ExecuteProduceConsume.java:148)\n\tat org.eclipse.jetty.util.thread.strategy.ExecuteProduceConsume.run(ExecuteProduceConsume.java:136)\n\tat org.eclipse.jetty.util.thread.QueuedThreadPool.runJob(QueuedThreadPool.java:671)\n\tat org.eclipse.jetty.util.thread.QueuedThreadPool$2.run(QueuedThreadPool.java:589)\n\tat java.lang.Thread.run(Thread.java:745)\n",
          "message" : "The server didn't find a representation for this criteria",
          "status" : 404
        }
      } ]
    }

#### Error Handling
##### 1) Custom error per search criteria
For each input criteria, the developer is responsible to update either the "_elements" or the "_errors" map in `BatchFinderResult`.
If the developers set a customized error which is wrapped into a `RestLiServiceException` for one search criteria,
Rest.li framework will not treat it as a failure for the whole BATCH_FINDER request, but just the failure for that specific criteria.
The return http status for the BATCH_FINDER request is still 200. An example is below [Resource API](/rest.li/batch_finder_resource_method#resource-api).

##### 2) Rest.li framework will cover the non-present criteria error
When processing the `BatchFinderResult` in the ResponseBuilder, if a criteria is not present, either in _elements, nor in _errors,  the framework will generate a "404" error for this criteria.
The whole http status is still 200.
```
new RestLiServiceException(S_404_NOT_FOUND, "The server didn't find a representation for this criteria"));
```

##### 3) return nulls
In some situation, the return results may contain null value. Resource methods should never explicitly return null. 
If the Rest.li framework detects this, it will return an HTTP 500 back to the client with a message indicating ‘Unexpected null encountered’. 
See more details in [Returning Nulls](/rest.li/user_guide/restli_server#returning-nulls).

Here are some possible cases:
- `BatchFinderResult` is null.
- Element is null in the returned list of entities in the successful case.
- For one criteria, the whole list of entities is null.

## Resource API
Resources may provide zero or more BATCH_FINDER resource methods. Each BATCH_FINDER method must be annotated with the @`BatchFinder` annotation.

Pagination default to start=0 and count=10. Clients may set both of these parameters to any desired value.
### Method Annotation and Parameters
The @`BatchFinder` annotation takes 2 required parameter:
- `value` : which indicates the BATCH_FINDER method name
- `batchParam` : which indicates the name of the batch criteria parameter, each BATCH_FINDER method must have and can only have one batch parameter

For example: 
```
  @BatchFinder(value = "searchGreetings", batchParam = "criteria")
  public BatchFinderResult<GreetingCriteria, Greeting, EmptyRecord> searchGreetings(@PagingContextParam PagingContext context,
                                                                @QueryParam("criteria") GreetingCriteria[] criteria,
                                                                @QueryParam("message") String message)
  {
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
  }
```
Every parameter of a BATCH_FINDER method must be annotated with one of:

-   @`Context` - indicates that the parameter provides framework context
    to the method. Currently all @`Context` parameters must be of type
    `PagingContext`.
-   @`QueryParam` - indicates that the value of the parameter is
    obtained from a request query parameter. The value of the annotation
    indicates the name of the query parameter. Duplicate names are not
    allowed for the same BATCH_FINDER method.
    For the batch parameter, the name must match the name in the method annotation.
-   @`AssocKey` - indicates that the value of the parameter is a partial
    association key, obtained from the request. The value of the
    annotation indicates the name of the association key, which must
    match the name of an @`Key` provided in the `assocKeys` field of the
    @`RestLiAssociation` annotation.

Parameters marked with @`QueryParam` and @`AssocKey`
may also be annotated with @`Optional`, which indicates that the
parameter is not required. *caution*: the batch parameter can not be optional.
The @`Optional` annotation may specify a String value, indicating the default value to be used if the parameter
is not provided in the request. If the method parameter is of primitive
type, a default value must be specified in the @`Optional` annotation.

Valid types for regular query parameters are:

-   `String`
-   `boolean` / `Boolean`
-   `int` / `Integer`
-   `long` / `Long`
-   `float` / `Float`
-   `double` / `Double`
-   A Pegasus Enum (any enum defined in a `.pdsc` schema)
-   Custom types (see the bottom of this section)
-   Record template types (any subclass of `RecordTemplate` generated
    from a `.pdsc` schema)
-   Arrays of one of the types above, e.g. `String[]`, `long[]`, ...

Valid type for batch criteria parameter:

- Can only be Arrays of Record template type, if have to use some other data types like Pegasus Enum, etc as the array item,
 need to wrap it into a Record Template (`.pdsc` schema)
 
### Criteria Filter
To implement a batch finder, the resource owner has to define a `RecordTemplate` to define a criteria filter parameter.
The batch finder method will have to accept a array of this criteria filter.

Example:

The "GreetingCriteria" class represent a criteria filter to filter by "id" or by "tone".
This class is auto-generated from the pdsc. 
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

The resource owner need to define their own search criteria `.pdsc` file.
```
{
 "type" : "record",
 "name" : "GreetingCriteria",
 "namespace" : "com.linkedin.restli.examples.greetings.api",
 "doc" : "A search criteria to filter greetings.",
 "fields" : [
   {
     "name" : "id",
     "doc": "Greeting ID to filter on",
     "type" : "long"
   },
   {
     "name" : "tone",
     "doc" : "Greeting tone to filter on",
     "type" : "Tone"
   }
 ]
}
```
 
### BatchFinderResult
BATCH_Finder methods must return `BatchFinderResult<QK extends RecordTemplate, V extends RecordTemplate, MD extends RecordTemplate>`:

- `QK` : The type of the BATCH_FINDER criteria filter
- `V` :  The type of the resource, aka, the entity type
- `MD` : The type of the meta data, if do not need metadata, just set it `EmptyRecord`

For each search criteria in the BatchFinderRequest, it can get either a successful reponse
which is a `CollectionResult`(a list of entities), Or an error/failure which maybe represented by
a `RestLiServiceException`, which will be wrapped into an `ErrorResponse` later when building BatchFinderResponse
to return to client.
```
public class BatchFinderResult<QK,V extends RecordTemplate,MD extends RecordTemplate>
{
   private final Map<QK,CollectionResult<V,R>> _elements;
   private final Map<QK,RestLiServiceException> _errors;
   ...
}
```

## Future consideration
### Support primitive type(not ready)
We can have a simple use case where we want to filter each individual query on the same attribute but with different values.

For this use case, the developer can use an array of primitive type.
It will avoid him to create another schema just to be able to filter on 1 attribute.  

In this example, each individual request will be filtered on the first name.
```java
@BatchFinder(name="findUsers", batchParam="criteria")
public BatchFinderResult<SearchCriteria, User, EmptyRecord> findUsers(@PagingContextParam PagingContext context, @QueryParam("criteria") string[] firstNames) throws InternalException
{
    ...
}
```