---
layout: guide
title: Rest.li Filters
permalink: /Rest_li-Filters
excerpt: Rest.li provides a mechanism to intercept incoming requests and outgoing responses via filters. Each Rest.li filter contains methods that handle both requests and responses.
---
# Rest.li Filters

## Contents

* [Introduction](#introduction)
* [How Filters Work](#how-filters-work)
* [Using Filters](#using-filters)
* [Filter Chaining](#filter-chaining)
* [Transferring State Between Filters](#transferring-state-between-filters)
* [Exception Handing and Filter Chains](#exception-handling-and-filter-chains)
* [Making Asynchronous Blocking Calls from Filters](#making-asynchronous-blocking-calls-from-filters)

## Introduction

On the server side, Rest.li provides a mechanism to intercept incoming requests and outgoing responses via filters. Each
Rest.li filter contains methods that handle both requests and responses.

On the request side, filters can be used for a wide range of use cases, including request validation, admission control,
and throttling.

Similarly on the response side, filters can be used for a wide range of use cases, including augmentation of response
body and encrypting sensitive information in the response payload.

## How Filters Work

When using a filter, you have the option of implementing the interface’s `onRequest`, `onResponse`, and `onError`
methods - here is where you specify what the filter will do. onRequest is invoked on the request before the actual
resource method is invoked. `onResponse` is invoked on the response after the resource method is invoked but before
being passed to the R2 stack. `onError` is invoked when an exception occurs in one of the filter’s methods or if it
receives a response error from the previous filter’s `onResponse` method. `onError` of the first filter in the response
filter chain will also be invoked if the REST resource method returns an error.

If you do not implement these methods, the default behavior for each method is to do nothing. For example, you can
choose to only implement the onRequest method. This way, on responses or errors, the filter will simply pass the
response/error to the next filter.

When a request arrives, the filters intercept it. If onRequest executes successfully, it will pass it to the next
filter. If an exception occurs, all subsequent filters are skipped, the filter’s `onError` will be invoked, and an error
response is passed through the filter chain in reverse and sent back to the client.

When a response is returned from the REST resource method, it is passed into the filter’s onResponse method. If the REST
resource method returns an error response, it will be passed into the `onError` method instead. If a filter’s onResponse
executes successfully, it will pass the response to the next filter. If an exception occurs, the filter will pass it to
the next filter’s `onError` method.

When `onError` is invoked, by default it will pass the error response to the next filter’s `onError` method. You can
specify additional handling (e.g. logging the error) before passing the response on. You can specify logic to fix the
error, whereupon the next filter’s onResponse method will be invoked.

When a Rest.li server is configured to use filters, the filters will be invoked for all incoming requests and outgoing
responses of all resources hosted by that server. Therefore, when implementing filters, please keep in mind that filters
are cross-cutting and should be applicable to all resources that are hosted by the given Rest.li server.

## Using Filters

Creating a concrete filter is simple. All you need to do is implement the `com.linkedin.restli.server.filter.Filter`
interface. Rest.li guarantees that for a given request-response pair, the same instance of `FilterRequestContext` is
made available to both the request filter and response filter.

### Filter Return Type

Each filter method returns a `CompletableFuture<Void>`. A `CompletableFuture` represents the status result of filter
execution and has 3 states - completed, completed with exception, and incomplete. The next filter will not be invoked
until the previous filter has completed (either successfully or exceptionally).

If the filter does not call any asynchronous methods, you can simply return `CompletableFuture.completedFuture(null)` -
this returns an already completed future, and it will cause the filter chain to invoke the next filter. 

If there is an error, you can either throw an Exception or return a future that has already called
`future.completeExceptionally(exception)` - both will do the same thing.

If the filter calls an asynchronous method, you can instantiate an incomplete CompletableFuture and return it from the
filter method. This future should be passed into your asynchronous method - when the method finishes, you can call
`future.complete(null)`. This will trigger the filter chain to invoke the next filter. If there is an error, you can
call `future.completeExceptionally(exception)`. There are more details on this below.

Not completing a future, whether successfully or exceptionally, will cause the filter chain processing to hang
indefinitely.

### Filter Requests

The implementation of the `onRequest` method is free to modify the incoming request. Additionally, it can also reject
the incoming request by throwing an exception or completing the future exceptionally - in this case, a response error is
automatically passed into the filter’s `onError` method.

The onRequest method has access to the `FilterRequestContext`. `FilterRequestContext` is an interface that abstracts
information regarding the incoming request, including the request URI, projection mask, request query parameters, and
request headers. Please see documentation of `FilterRequestContext` for more info.

After all the filters’ `onRequest` method have been successfully invoked, the filter chain passes the request to the
Rest.li resource.

### Filter Responses

The implementation of the `onResponse` method can inspect and modify the outgoing response body, HTTP status, and
headers. Throwing an exception causes the response to be converted into an error response and passed into the next
filter’s `onError` method.

The `onResponse` method has access to the `FilterRequestContext` and `FilterResponseContext`. The
`FilterResponseContext` is an interface that abstracts information regarding the outgoing response, including the
response HTTP status, response body, and response headers. Please see documentation of `FilterResponseContext` for more
info.

After the last filter’s `onResponse` method has been invoked successfully, the filter chain passes the outgoing response
to the underlying R2 stack. If the last filter’s `onResponse` method’s future completes exceptionally, the response is
converted into an error response and is passed into the R2 stack.

### Filter Errors

The implementation of the `onError` method handles errors, and has the capability to alter the response body, HTTP
status, and headers. The onError method has access to the exception that caused the error, `FilterRequestContext`, and
`FilterResponseContext`. 

The `CompletableFuture` that is returned by this method should be completed exceptionally unless this filter fixes the
error, whereupon the future should be completed successfully. The paradigm is that if an error exists in the response at
the end of the filter, the future should be completed exceptionally.

If an exception occurs within the `onError` method itself, the next filter’s `onError` will be invoked. However, the
most recently occurring exception will be passed in as the exception argument.

After the last filter’s `onError` method has been invoked, the filter chain passes the outgoing response error to the
underlying R2 stack. If the last filter’s onError method’s future completes successfully (i.e. the error was fixed), the
error response is converted into a success response and passed to the R2 stack.

### Example Filter

```java
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.FilterResponseContext;
import com.linkedin.restli.server.filter.Filter;

public class RestliExampleFilter implements Filter
{
  @Override
  public CompletableFuture<Void> onRequest(final FilterRequestContext requestContext)
  {
    log.debug(String.format("Received %s request for %s resource.", requestContext.getMethodType(), requestContext.getFilterResourceModel().getResourceName()));
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Void> onResponse(final FilterRequestContext requestContext, final FilterResponseContext responseContext)
  {
    System.out.println(String.format("Responding to %s request for %s resource with status code %d.", requestContext.getMethodType(),
                                     requestContext.getFilterResourceModel().getResourceName(), responseContext.getResponseData().getStatus().getCode()));
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Void> onError(Throwable t, final FilterRequestContext requestContext, final FilterResponseContext responseContext)
  {
    log.debug(t.toString());
    CompletableFuture<Void> future = new CompletableFuture<Void>();
    if (isErrorFixable(t))
    {
      fixError();
      future.complete(null); // success
    }
    else
    {
       future.completeExceptionally(t); // could not fix error, so this filter did not execute successfully
    }
    return future;
  }
}
```

When a request arrives, this filter prints the request type and resource name for every incoming request.

When a response is sent, this filter prints the HTTP response code along with request type and resource name for every
outgoing response. 

When there is an error, this filter logs the exception that caused it. Notice how the filter has the ability to either
fix the error or propagate the error (complete normally vs. complete exceptionally).

### Response Data API

The `FilterResponseContext` has access to a `RestLiResponseData` object. This object contains the response data as a
`RestLiResponseEnvelope`, which also includes the HTTP status (if success) and the error exception (if error). Besides,
it contains headers and cookies, as well as indicators for response type and the resource method.

### Response Envelope API

The `RestLiResponseEnvelope` contains the actual data from the response, as well as HTTP status (if success) and the
error exception (if error). For example, a GET response would store the retrieved resource data in the envelope.

If there is an error, the exception will never be null but the data stored inside of `RestLiResponseEnvelope` will
always be null (the envelope itself will not be null, only the data inside of it). The opposite is true if there is no
error.

The type of response envelope is based on the Rest.li resource method. For example, a GET response would have data
stored in the `GetResponseEnvelope`.

| Resource Method        | Response Envelope                    |
| ---------------------- | ------------------------------------ |
| `GET`                  | `GetResponseEnvelope`                |
| `CREATE`               | `CreateResponseEnvelope`             |
| `ACTION`               | `ActionResponseEnvelope`             |
| `BATCH_GET`            | `BatchGetResponseEnvelope`           |
| `BATCH_PARTIAL_UPDATE` | `BatchPartialUpdateResponseEnvelope` |
| `BATCH_UPDATE`         | `BatchUpdateResponseEnvelope`        |
| `BATCH_DELETE`         | `BatchDeleteResponseEnvelope`        |
| `BATCH_CREATE`         | `BatchCreateResponseEnvelope`        |
| `BATCH_FINDER`         | `BatchFinderResponseEnvelope`        |
| `GET_ALL`              | `GetAllResponseEnvelope`             |
| `FINDER`               | `FinderResponseEnvelope`             |
| `UPDATE`               | `UpdateResponseEnvelope`             |
| `PARTIAL_UPDATE`       | `PartialUpdateResponseEnvelope`      |
| `OPTIONS`              | `OptionsResponseEnvelope`            |
| `DELETE`               | `DeleteResponseEnvelope`             |

Response envelopes are grouped together based on `ResponseTypes`. Each response type shares the same data format, and
thus use the same getters and setters. A parent response envelope is subclassed by the envelopes in the same
ResponseType group.

For example, `GetResponseEnvelope`, `ActionResponseEnvelope`, and `CreateResponseEnvelope` all store a `RecordTemplate`
and all use `getRecord` and `setRecord` as their data access methods. As such, `RecordResponseEnvelope` is the parent
envelope for all three. Grouping them together this way reduces code duplication because you can write code for all
envelopes that share the same interface. 

| Response Type       | Parent Response Envelope                                                             | Child Response Envelopes | 
| ------------------- | ------------------------------------------------------------------------------------ | ------------------------ |
| `SINGLE_ENTITY`     | `RecordResponseEnvelope`                                                             | `GetResponseEnvelope`, `CreateResponseEnvelope`, `ActionResponseEnvelope` |
| `CREATE_COLLECTION` | N/A - only one envelope falls under this response type, <br> so no need for parent   | `BatchCreateResponseEnvelope` |
| `GET_COLLECTION`    | `CollectionResponseEnvelope`                                                         | `GetAllResponseEnvelope`, `FinderResponseEnvelope` |
| `BATCH_COLLECTION`  | N/A - only one envelope falls under this response type, <br> so no need for parent   | `BatchFinderResponseEnvelope` |
| `BATCH_ENTITIES`    | `BatchResponseEnvelope`                                                              | `BatchGetResponseEnvelope`, `BatchUpdateResponseEnvelope`, `BatchPartialUpdateResponseEnvelope`, `BatchDeleteResponseEnvelope` |
| `STATUS_ONLY`       | `EmptyResponseEnvelope`                                                              | `PartialUpdateResponseEnvelope`, `UpdateResponseEnvelope`, `DeleteResponseEnvelope`, `OptionsResponseEnvelope` |

A typical use case is as follows - notice there are 2 ways to handle different response data types, the first using the resource method and the second using the response type:

```java
public class RestliExampleFilter implements Filter
{
  @Override
  public CompletableFuture<Void> onResponse(FilterRequestContext requestContext, FilterResponseContext responseContext)
  {
    RestLiResponseData<?> responseData = responseContext.getResponseData();
    switch (responseData.getResourceMethod()) 
    {
      // Example showing determining code path based on resource method (CREATE, GET, etc.)
      case CREATE:        // Handle CREATE response
        CreateResponseEnvelope envelope = (CreateResponseEnvelope) responseData.getResponseEnvelope();
        someMethod(envelope.getStatus());
        anotherMethod(envelope.getRecord());
        envelope.setRecord(new EmptyRecord()); //Modify the response
        break;
      case GET        // Handles GET responses
        break;
      default:
      // Other types available as well.
    }

    // Another example, this time showing determining code path based on response type (SINGLE_ENTITY, GET_COLLECTION, etc.)
    switch (responseData.getResponseType()) 
    {
      case SINGLE_ENTITY:        // Handle GET, ACTION, and CREATE responses - note how you can apply the same logic to all 3 because they share the same data access interface
        RecordResponseEnvelope envelope = (RecordResponseEnvelope) responseData.getResponseEnvelope();
        someMethod(envelope.getRecord());
        envelope.setRecord(new EmptyRecord()); //Modify the response
        break;
      case GET_COLLECTION        // Handles GET_ALL and FINDER responses
        break;
      default:
      // Other types available as well.
    }
    return CompletableFuture.completedFuture(null);
  }
}
```

## Filter Chaining

Rest.li supports chaining of filters. When a Rest.li server is configured to use multiple filters, the filters are
ordered in the same order specified in the `RestLiConfig`. On requests, filters that are declared closer to the
beginning are invoked first. On responses, filters that are declared closer to the end are invoked first. See diagram at
top of document for visualization.

Approach 1 to chain three filters.

```java
final RestLiConfig config = new RestLiConfig();
config.addFilter(new FilterOne(), new FilterTwo(), new FilterThree());
```

Approach 2 to chain three filters.

```java
final RestLiConfig config = new RestLiConfig();
config.addFilter(new FilterOne());
config.addFilter(new FilterTwo());
config.addFilter(new FilterThree());
```

Approach 3 to chain three filters.

```java
final RestLiConfig config = new RestLiConfig();
config.addFilter(Arrays.asList(new FilterOne(), new FilterTwo(), new FilterThree()));
```

Approach 4 to chain three filters

```xml
<bean class="com.linkedin.restli.server.RestLiConfig">
    <property name=“filters>
        <list>
            <bean class=“FilterOne”/>
            <bean class=“FilterTwo”/>
            <bean class="FilterThree”/>
        </list>
    </property>
</bean>
```

## Transferring State Between Filters

It is recommended that Rest.li filters be stateless. To facilitate transfer of state between filters, Rest.li provides a
scratch pad in the form of a Java Map. This scratch pad can be accessed via the `getFilterScratchpad` method on the
`FilterRequestContext`. See below for an example Rest.li filter that computes the request processing time and print it
to standard out.

## Exception Handling and Filter Chains

The manner in which exceptions are handled in the filter’s request vs. response methods are different.

There are 2 ways a filter can invoke an exception:

1. The filter throws an exception from within one of its methods
2. The filter completes its future exceptionally - i.e. `future.completeExceptionally(throwable)`

### Requests

If an exception is thrown while processing a request or if the future is completed exceptionally, further processing of
the request is terminated and the filter’s onError method is invoked. In other words, in order for the incoming request
to reach the resource implementation, invocation of all filters’ `onRequest` methods needs to be successful.

### Responses

Exception/error handling in the context of response filters is a little more involved than in the case of request
filters. Response filters are applied to both successful responses as well as all types of errors.

Such errors can include:

1. Exceptions thrown by the resource method, including runtime exceptions such as `NullPointerException` or
  `RestLiServiceException`.
2. Exceptions generated by Rest.li due to bugs in resource methods. These could include bugs such as nulls returned
  directly from the resource methods, or indirectly such as null values inside of returned objects (e.g a null element
  list inside of a `CollectionResult`).

Subsequently, response filters can transform a successful response from the resource to an error response and vice
versa. In addition, a successful response from a filter earlier in the filter chain can be transformed into an error
response and vice versa by filters that are subsequent in the filter chain.

The exception/error handling behavior of response filters is summarized as follows:

1. If the last filter in the filter chain throws an exception or completes its future exceptionally, an error response
  is returned to the client corresponding to this exception.
2. If an exception is thrown or the result future is completed exceptionally by any filter except the last filter in the
  filter chain. The subsequent filter’s `onError` method is invoked. You can specify error handling in the onError method
  (i.e. fix the error or propagate it to the next filter).
3. The response that is generated as a result of executing the filter chain is the response that is forwarded to the
  client. Note that the filter chain can transform a successful/error response from the resource to a error/successful
  response that’s sent to the client.

When an exception occurs, the HTTP status code will be automatically set according to this rule:

* If the exception is a `RestLiServiceException`, the status will be taken from the exception.
* If not, the status will be set to 500 (Internal Server Error).

It is recommended that filters throw a `RestLiServiceException`.

Note that response headers will be maintained if an exception is thrown, however some new headers signifying an error may be added.

## Making Asynchronous Blocking Calls from Filters

Situations may arise where you may need to make external calls within your filter code. Say for example, there’s an
external Auth service that your service integrates with. Every call that comes to your service should be first routed to
the Auth service for approval, and only if the Auth service give you a green light, can your resource process the
request. Let’s say you have a RestLi filter that abstracts away the invocation of the Auth service. One way to implement
this Auth filter is as follows:

```java
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.Filter;

public class AuthFilter implements Filter
{
  @Override
  public CompletableFuture<Void> onRequest(FilterRequestContext requestContext)
  {
    String resourceName = requestContext.getResourceModel().getResourceName();
    // Now invoke the auth service.
    Request<Permission> getRequest = builders.get().resourceName(resourceName).build();
    Permission permission = getClient().sendRequest(getRequest).getResponse().getEntity();
    log.debug(String.format("Received permission %s from auth service for request for %s resource.",
                             requestContext.getMethodType(), resourceName));
    if (permission.isGranted()) 
    {
       // Since we have permissions, pass the request along.
       return CompletableFuture.completedFuture(null);
    } 
    else 
    {
      throw new RestLiServiceException(HttpStatus.S_401_UNAUTHORIZED, "Permission denied");
    }
  }
}
```

The above implementation makes a synchronous call to an external auth service to authenticate the incoming request.
Although the above implementation is functionally correct, it is not very efficient. Upon close investigation, you’ll
observe that the request processing thread of your service is now blocked on an outgoing call to the auth service. If
the auth service is slow to respond to requests, very soon it’s possible that all threads of your service is blocked
waiting for response from the auth service.

The Rest.li filters provide a CompletableFuture interface that handles the asynchronous callbacks for you. The
implementation is shown below:

```java
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.Filter;

public class AuthFilter implements Filter
{
  @Override
  public CompletableFuture<Void> onRequest(FilterRequestContext requestContext)
  {
    CompletableFuture<Void> future = new CompletableFuture<Void>();
    String resourceName = requestContext.getResourceModel().getResourceName();
    // Now invoke the auth service.
    Request<Permission> getRequest = builders.get().resourceName(resourceName).build();
    Callback<Response<Permission>> cb = new Callback<Response<Permission>>()
      {
        @Override
        public void onSuccess(Response<Permission> response)
        {
          Permission permission = response.getEntity();
          log.debug(String.format("Received permission %s from auth service for request for %s resource.",
                                  requestContext.getMethodType(), resourceName));
          if (permission.isGranted()) 
          {
            // Since we have permissions, pass the request along.
           future.complete(null);
          } 
          else
          {
            future.completeExceptionally(new RestLiServiceException(HttpStatus.S_401_UNAUTHORIZED, "Permission denied"));
          }
        }
        @Override
        public void onError(Throwable e)
        {
          future.completeExceptionally(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, e));
        }
      }
    // Invoke the auth service asynchronously. 
    getClient().sendRequest(getRequest, requestContext, cb);
    return future;
  }
}
```

The above implementation makes an asynchronous blocking call to the external auth service to authenticate the incoming
request. In this implementation, the request processing thread of your service is NOT blocked on an outgoing call to the
auth service and is free to process more incoming requests for your service. By using `CompletableFuture`, you can make
outgoing asynchronous calls from within RestLi filters.
