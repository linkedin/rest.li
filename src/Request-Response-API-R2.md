---
layout: api_reference
title: Rest.li Request Response API (R2)
permalink: /Request-Response-API-(R2)
excerpt: R2 is the request / response API underlying Rest.li.  It includes abstractions for REST requests and responses, filter chains for customized processing, and transport abstraction. It is designed so that it can be easily customized for non-open source use cases.
---

# Rest.li Request Response API (R2)

## Contents

- [Introduction](#introduction)
- [Layers](#layers)
- [Requests and Responses](#requests-and-responses)

## Introduction	

R2 is the request / response API underlying Rest.li.  It includes abstractions for REST requests and responses, filter chains for customized processing, and transport abstraction. It is designed so that it can be easily customized for non-open source use cases. 

R2 can be used independently, with D2 (our Dynamic Discovery system), or with both D2 and Rest.li. 

## Layers
The following diagram shows the layers involved in the R2 system. Each layer will be described in detail below.

<p><img src="/rest.li/images/r2.png"></p>

## Requests and Responses

In this section, we describe messages in the R2 system. The message hierarchy has ben designed to make it possible to add and work with broader message abstractions when appropriate. Originally, REST was the basic type of message in R2, but with the addition of R2 Streaming, STREAM became the most basic type of messages in R2, with REST built on top of STREAM. Because REST is the most common use case, we will stick to describing the REST model here, and leave STREAM as a separate doc.

Messages have a few properties that are worth describing here:
- They are immutable. It is not possible to change a message after it has been created. It is, however, possible to copy a message and make changes using builders, which will be described later.
- They are thread-safe due to immutability.
- New messages are created using builders.
- Existing messages can be copied and modified using builders.

### Messages

"RestMessage" is the root of the message hierarchy. All messages in R2 contain an "entity" (which may be empty), and corresponds to the request or response data. For REST, the R2 entity is equivalent to a REST entity. For Streaming, because of the nature of streaming, the R2 entity is replaced by an EntityStream. R2 Streaming will be discussed more fully in a separate doc.
 
All REST messages add headers in addition to the base message properties. Headers must conform to the definition in RFC 2616 (described in section 4.2 and associated sections). The RestMessage interface is (Note: all code snippets have been simplified here for documentation purposes, and may not reflect the full complexity of the class hierarchy):

```java
public interface RestMessage extend MessageHeaders
{
  /**
   * Returns the entity for this message.
   *
   * @return the entity for this message
   */
  ByteString getEntity();
 
  /**
   * Returns a {@link RestMessageBuilder}, which provides a means of constructing a new message using
   * this message as a starting point. Changes made with the builder are not reflected by this
   * message instance.
   *
   * @return a builder for this message
   */
  RestMessageBuilder<? extends RestMessageBuilder<?>> builder();
}
```

In addition to an entity, all messages provide a builder that can be used to copy the message and modify its copy. In the case of the Message above, the builder is a RestMessageBuilder.
RestMessages are subdivided into RestRequests and RestResponses. The interfaces for these are described below.

### RestRequest
A request has a URI. This provides information to the client about how to direct the request - for example, which protocol to use, which server to connect to, what service to invoke, etc. R2 can be used with D2 (Dynamic Discovery), so in those cases URNs will be used for the URI. These URNs will be resolved internally by the Dynamic Discovery system.
RestRequests add a method property, which matches the semantics for a REST message (i.e. the method is one of GET, PUT, POST, DELETE):

```java
public interface RestRequest extends RestMessage, Request
{
  /**
   * Returns the URI for this request.
   *
   * @return the URI for this request
   */
  URI getURI();

  /**
   * Returns the REST method for this request.
   *
   * @return the REST method for this request
   * @see com.linkedin.r2.message.rest.RestMethod
   */
  String getMethod();
 
  /**
   * Returns a {@link RestRequestBuilder}, which provides a means of constructing a new request using
   * this request as a starting point. Changes made with the builder are not reflected by this
   * request instance.
   *
   * @return a builder for this request
   */
  RestRequestBuilder builder();
}
```

### RestResponse

RestResponses add a status property, which matches the semantics of a REST status code (e.g. 200 - OK, see RFC 2616 for details about HTTP status codes):

```java
public interface RestResponse extends RestMessage, Response
{
  /**
   * Returns the status for this response.
   *
   * @return the status for this response
   * @see com.linkedin.r2.message.rest.RestStatus
   */
  int getStatus();
 
  /**
   * Returns a {@link RestResponseBuilder}, which provides a means of constructing a new response using
   * this response as a starting point. Changes made with the builder are not reflected by this
   * response instance.
   *
   * @return a builder for this response
   */
  @Override
  RestResponseBuilder builder();
}
```

### ByteStrings

Entities are stored as ByteStrings in R2. ByteStrings provide a mechanism to ensure that the byte data is immutable and not copied unless absolutely necessary. The ByteString interface looks like the following:

```java
public final class ByteString
{
  /**
   * Returns an empty {@link ByteString}.
   *
   * @return an empty {@link ByteString}
   */
  public static ByteString empty();
 
  /**
   * Returns a new {@link ByteString} that wraps a copy of the supplied bytes. Changes to the supplied bytes
   * will not be reflected in the returned {@link ByteString}.
   *
   * @param bytes the bytes to copy
   * @return a {@link ByteString} that wraps a copy of the supplied bytes
   * @throws NullPointerException if {@code bytes} is {@code null}.
   */
  public static ByteString copy(byte[] bytes);
 
  /**
   * Returns a new {@link ByteString} that wraps the bytes generated from the supplied string with the
   * given charset.
   *
   * @param str the string to copy
   * @param charset the charset used to encode the bytes
   * @return a {@link ByteString} that wraps a copy of the supplied bytes
   */
  public static ByteString copyString(String str, Charset charset);
 
  /**
   * Returns a new {@link ByteString} with bytes read from an {@link InputStream}.
   *
   * If size is zero, then this method will always return the {@link ByteString#empty()},
   * and no bytes will be read from the {@link InputStream}.
   * If size is less than zero, then {@link NegativeArraySizeException} will be thrown
   * when this method attempt to create an array of negative size.
   *
   * @param inputStream that will provide the bytes.
   * @param size provides the number of bytes to read.
   * @return a ByteString that contains the read bytes.
   * @throws IOException from InputStream if requested number of bytes
   *                     cannot be read.
   */
  public static ByteString read(InputStream inputStream, int size) throws IOException;
 
  /**
   * Returns a copy of the bytes in this {@link ByteString}. Changes to the returned byte[] will not be
   * reflected in this {@link ByteString}.
   *
   * Where possible prefer other methods for accessing the underlying bytes, such as
   * {@link #asByteBuffer()}, {@link #write(java.io.OutputStream)}, or {@link #asString(Charset)}.
   * The first two make no copy of the byte array, while the last minimizes the amount of copying
   * (constructing a String from a byte[] always involves copying).
   *
   * @return a copy of the bytes in this {@link ByteString}
   */
  public byte[] copyBytes();
 
  /**
   * Copy the bytes in this {@link ByteString} to the provided byte[] starting at the specified offset.
   *
   * Where possible prefer other methods for accessing the underlying bytes, such as
   * {@link #asByteBuffer()}, {@link #write(java.io.OutputStream)}, or {@link #asString(Charset)}.
   * The first two make no copy of the byte array, while the last minimizes the amount of copying
   * (constructing a String from a byte[] always involves copying).
   *
   * @param dest is the destination to copy the bytes in this {@link ByteString} to.
   * @param offset is the starting offset in the destination to receive the copy.
   */
  public void copyBytes(byte[] dest, int offset);
 
  /**
   * Returns a read only {@link ByteBuffer} view of this {@link ByteString}. This method makes no copy.
   *
   * @return read only {@link ByteBuffer} view of this {@link ByteString}.
   */
  public ByteBuffer asByteBuffer();
 
  /**
   * Return a String representation of the bytes in this {@link ByteString}, decoded using the supplied
   * charset.
   *
   * @param charset the charset to use to decode the bytes
   * @return the String representation of this {@link ByteString}
   */
  public String asString(Charset charset);
 
  /**
   * Return an {@link InputStream} view of the bytes in this {@link ByteString}.
   *
   * @return an {@link InputStream} view of the bytes in this {@link ByteString}
   */
  public InputStream asInputStream();
 
  /**
   * Writes this {@link ByteString} to a stream without copying the underlying byte[].
   *
   * @param out the stream to write the bytes to
   *
   * @throws IOException if an error occurs while writing to the stream
   */
  public void write(OutputStream out) throws IOException;
```

### Builders

As mentioned previously, builders provide the following basic functionality:

- Create a new message
- Copy a message, modify it, and create a new immutable copy

To create a new message, use RestRequestBuilder / RestResponseBuilder as appropriate. Builder methods are designed to be chained. Here is an example of chaining:

```java
final RestResponse res = new RestResponseBuilder()
  .setEntity(new byte[] {1,2,3,4})
  .setHeader("k1", "v1")
  .setStatus(300)
  .build()
```

To copy a message, it is sufficient to ask the message for its builder. Typically this can be done with the builder method, but in some cases a special builder method must be used (when working with abstract messages).
Here is an example of copying and modifying a message:

```java
final RestRequest req = ...;
final RestRequest newReq = req.builder()
                             .setEntity(new byte[] {5,6,7,8})
                             .setURI(URI.create("anotherURI"))
                             .build();
```

Here is an example of copying and modifying an abstract request:

```java
final Request req = ...;
final Request newReq = req.requestBuilder()
                          .setEntity(new byte[] {5,6,7,8})
                          .setURI(URI.create("anotherURI"))
                          .build();
```

### Callbacks

R2 is, by design, asynchronous in nature. As will be shown below, R2 provides two mechanisms to wait for an asynchronous operation to complete: callbacks and Futures. Futures should be familiar to most Java developers, so we will not discuss them further in this document. Callbacks are less common in Java and warrant some quick discussion.
In R2, the Callback interface looks like:

```java
public interface Callback
{
  /**
   * Called if the asynchronous operation completed with a successful result.
   *
   * @param t the result of the asynchronous operation
   */
  void onSuccess(T t);
 
  /**
   * Called if the asynchronous operation failed with an error.
   *
   * @param e the error
   */
  void onError(Exception e);
}
```

In some cases it is only possible to invoke an asynchronous operation with a callback (and not a Future). In those cases, which are not common for external users, it is possible to use a FutureCallback as shown in this example:

```java
final FutureCallback future = new FutureCallback();
asyncOp(..., future);
return future.get();
```

In some cases, code does not need to wait for completion of an event. In the case of the future, simply do not call get(). In the case of callbacks, use Callbacks.empty(), as shown in this example:

```java
asyncOp(..., Callbacks.empty());
```

Keep in mind that it will not be possible to know when the operation completed - or even if it completed successfully.
Sometimes code will want to know when an operation has completed, but is not concerned with the result. In this case, a SimpleCallback can be used or adapted to a Callback with Callbacks.adaptSimple(...).

### Client API

The R2 client API provides the mechanism for sending request and responses to a remote service or resource handler. The diagram below shows where the client sits in the R2 stack.

The main interface in this layer is the Client interface, shown here:

```java
public interface Client
{
  /**
   * Asynchronously issues the given request and returns a {@link Future} that can be used to wait
   * for the response.
   *
   * @param request the request to issue
   * @return a future to wait for the response
   */
  Future restRequest(RestRequest request);
 
  /**
   * Asynchronously issues the given request. The given callback is invoked when the response is
   * received. This event driven approach is typically more complicated to use and is appropriate
   * for building other abstractions, such as a DAG based resolver.
   *
   * @param request the request to issue
   * @param callback the callback to invoke with the response
   */
  void restRequest(RestRequest request, Callback callback);
 
  /**
   * Starts asynchronous shutdown of the client. This method should block minimally, if at all.
   *
   * @param callback a callback to invoke when the shutdown is complete
   */
  void shutdown(Callback callback);
}
```

Requests are made asynchronously using either Futures or Callbacks (see Callback section for details).

### Request Handler API

The Request Handler API is the server-side counterpart to the client, as shown in this diagram:

Request Handlers are used for two purposes:
Determining the rules for dispatching a request to a service / resource manager or another dispatcher
Handling a request (as a service or a resource manager)

The REST Request Handler interface looks like:

```java
public interface RestRequestHandler
{
  void handleRequest(RestRequest request, Callback callback);
}
```

### Filter Chains

The filter chain provides a mechanism for doing special processing for each request and response in the system. For example, logging, statistics collections, etc., are appropriate for this layer.

The FilterChain provides methods for adding new filters and for processing requests, responses, and errors.
Requests pass through the filter chain starting from the beginning and move towards the end. Responses and errors start from the end of the filter chain and move towards the beginning.

If an error occurs while a request moves through the filter chain (either due to a thrown Exception or due to an onError(...) call), then the error is first sent to the filter that raised the error and then it moves back towards the beginning of the filter chain. Any filters that show up after the filter that threw the exception will not get a chance to process the request.

### Filters

R2 provides a set of interfaces that can be implemented to intercept different types of messages. They are:

- Message filters
 - MessageFilter - intercepts both requests and responses
 - RequestFilter
 - ResponseFilter
- REST filters
 - RestFilter - intercepts both REST requests and REST responses
 - RestRequestFilter
 - RestResponseFilter

Messages filters can be used to handle messages in an abstract way (as Requests and Responses). REST filters can be used to handle messages of the specific type (REST or possibly another type, like STREAM). Different types of filters should not be used together, as they override the hooks provided by the Message filters. 

#### ClientQueryTunnelFilter / ServerQueryTunnelFilter

One notable set of filters is the ClientQueryTunnelFilter and the ServerQueryTunnelFilter. These filters allow long queries to be transformed by moving the query parameters into the body, and reformulating the request as a POST. The original method is specified by the X-HTTP-Method-Override header. See QueryTunnelUtil.java for more details.

### Wire Attributes

Wire attributes provide a mechanism to send "side-band" data to a remote endpoint along with a request or response. They are exposed at the filter chain layer and can be queried or modified by filters. They are not made available at the request / response layer because the entity (and headers, for REST) should supply all of the data necessary to process a request or response.
Wire attributes are sent as headers with the R2 HTTP transport.

### Local Attributes

Local attributes are used by filters during response processing to get data stored during the request. Filters use this mechanism because responses are not guaranteed to be processed on the same thread as their requests.

### Transports

The transport bridges convert our abstract requests, response, and wire attributes into transport-specific requests. We have support for an asynchronous HTTP transport and possibly other transports at this layer.

#### HTTP Transport

In the HTTP transport there is a standard transformation of our REST messages to equivalent HTTP messages.
Wire attributes are transported as headers, using the attribute name.

### Transport Protocol
Under the hood, the request will be encoded based on the [Rest.li protocol](/rest.li/spec/protocol) and sent over the wire to the server.
