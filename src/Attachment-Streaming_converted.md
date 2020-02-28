---
layout: guide
title: Attachment Streaming
permalink: /Attachment-Streaming
---

# Attachment Streaming

**This is an experimental feature at this point. Please consult Rest.li
team before using it.**

## Contents

  - [Goals](#goals)
  - [Inspiration](#inspiration)
  - [New Wire Format](#new-wire-format)
  - [Data Modeling](#data-modeling)
  - [Creating Attachments](#creating-attachments)
  - [Reading Attachments](#reading-attachments)
  - [Chaining (Proxying) Attachments Across Services](#chaining-proxying-attachments-across-services)
  - [Client Streaming APIs](#client-streaming-apis)
  - [Server Streaming APIs](#server-streaming-apis)
  - [Good Programming Practice](#good-programming-practice)
  - [Additional Developer Notes](#additional-developer-notes)
  - [Attachment Streaming without Rest.li](#attachment-streaming-without-restli)
  - [Future Enhancements](#future-enhancements)

## Goals

Rest.li is the high performance platform on which web services operate
at LinkedIn. As our company moved closer to the formal adoption of large
unstructured blobs of data, such as media, we needed a highly performant
way to move all this data around. Therefore it became apparent that we
needed to perform an overhaul of our existing service-to-service
architecture.

The goals of Rest.li attachment streaming therefore are the following:

  - Provide the ability to pass large blobs of bytes around our data
    centers between multiple services seamlessly
  - No one service should hold the entire payload in memory at once
  - Fully asynchronous and event driven
  - Zero copy write and read for high performance
  - Leverage existing web services infrastructure, namely R2/D2/Rest.li
  - Allow multiple blobs to be sent in a single request or response
  - Establish a wire format that can foster a high adoption rate for
    external members and platforms
  - Solve the immediate business need of our services interacting with
    our custom distributed object store - LinkedIn’s version of S3 -
    [Ambry](https://github.com/linkedin/ambry)
  - Provide clean and intuitive async APIs for our engineers

## Inspiration

Rest.li attachment streaming is inspired by the [Reactive Streaming
Manifesto](http://www.reactive-streams.org/)

Therefore attachment streaming in Rest.li, from the bottom (R2) to the
top, is based on the following:

  - Allow processing a potentially unbounded number of elements
  - Ensure that data elements are handled in sequence
  - Asynchronously pass elements between components
  - Mandatory non-blocking backpressure from the bottom (TCP) up
  - The reader should never be forced to buffer data

## New Wire Format

In order to support attachment streaming, Rest.li leverages a different
wire format then what traditional Rest.li traffic uses. This is for a
number of reasons:

  - The current wire formats JSON and PSON do not work well for
    expressing binary attachments. They were meant for structured data,
    so providing big blobs of bytes in them violates the spirit of their
    original intent.
  - Multiple attachments need to be supported, which is not possible in
    the current state of Rest.li.
  - The regular Rest.li payload (JSON/PSON) needs to continue to be
    supported and to be fully read/written before attachments are
    read/written by developer code.
  - Arbitrarily large attachments need to be supported. This requires a
    format that is welcoming to very large blobs of bytes.
  - Each attachment needs some metadata associated with it as well. This
    is outside of the regular Rest.li payload.
  - It is desirable to have a wire format that is set up for easy
    adoption by external consumers and platforms.

In order to satisfy these requirements, Rest.li makes use of
`multipart/mime` as the streaming wire format. Therefore
Rest.li attachment streaming’s implementation is build upon the formal
adoption of the [RFC.](https://tools.ietf.org/html/rfc2046)

Within `multipart/mime` there are several `mime`
subtypes that can be chosen. Rest.li attachment streaming uses
`multipart/related` as the value for the
`Content-Type` header since the JSON/PSON payload will be the
first part in the mime envelope. A more detailed explanation as to the
justification of `multipart/related` is explained
[here.](https://en.wikipedia.org/wiki/MIME#Related)

### Sample Traditional Rest.li Wire Format

For reference, here are examples of sample regular Rest.li payloads.

```http
POST /widgets?action=purge HTTP/1.1
Content-Type: application/json

{
  "reason": "spam",
  "purgedByAdminId": 1
}
```

```http
POST /widgets?ids=List(1,2) HTTP/1.1
Content-Type: application/json
X-RestLi-Method: BATCH_PARTIAL_UPDATE

{
  "entities": {
    "1": {"patch": { "$set": { "name":"Sam"}}},
    "2": {"patch": { "$delete": ["name"]}}
  }
}
```

### Wire Format for Attachment Streaming

Note that the current wire format described above will still be
supported as there are no backward incompatible changes being made.

However for clients sending requests with attachments present or for
servers responding with attachments, the wire protocol will change.

If attachments are present in either a request or a response, the
content type becomes `multipart/related`. If a client can
handle attachments back from a server, then an accept type of
`multipart/related` is also added to the `Accept`
header.

For example:

```http
PUT /widgets?ids=List(1,2,3) HTTP/1.1
X-RestLi-Method: BATCH_UPDATE
Content-Type: multipart/related; boundary=--km6cltxBQgkYRIwT8lAgFGfNV0AmQFwDB
Accept: multipart/related; application/json

--km6cltxBQgkYRIwT8lAgFGfNV0AmQFwDB
Content-Type: application/json
{
  "entities": {
    "1": {
      "widgetName": "Trebuchet",
      "myVideo": "cid:725c0319-b1f1-4b9c-b618-7ee9468870f0"
    },
    "2": {
      "widgetName": "Gear",
      "myVideo": "cid:a4d4133b-0546-4f7b-8104-ffdd644168c6"
    }
    "3": {
      "widgetName": "Slider",
      "myVideo": "cid:725c0319-b1f1-4b9c-b618-7ee9468870f0"
    }
  }
}
--km6cltxBQgkYRIwT8lAgFGfNV0AmQFwDB
Content-ID: <725c0319-b1f1-4b9c-b618-7ee9468870f0>
binary data...
--km6cltxBQgkYRIwT8lAgFGfNV0AmQFwDB
Content-ID: <a4d4133b-0546-4f7b-8104-ffdd644168c6>
binary data...
--km6cltxBQgkYRIwT8lAgFGfNV0AmQFwDB--
```

Note that the regular Rest.li payload becomes the first part in a
`multipart/related` envelope. Each attachment then becomes
its own subsequent part separated by the multipart boundary. Since each
part in a multipart envelope can have its own headers, the
`Content-Type` header from the regular payload now appears as
a header in the first part. Both JSON and PSON are supported as valid
`Content-Types` for the first part.

Each part after the regular Rest.li payload represents a blob of data
attached to the request or response. Each attachment part has a
`Content-ID` header which uniquely identifies that attachment
in the payload. References to this unique identifier should then be
placed as fields in the JSON (RecordTemplate backing) payload as
pointers to the blobs in the attachments. In this particular example we
have two attachments. `Trebuchet` and `Slider`
both point to the same attachment while `Gear` points to the
other attachment.

## Data Modeling

Data modeling is no different for streaming, with the exception of the
following recommendation. This recommendation is simply to serve as a
visual cue and does not impact the generated RecordTemplates or the
processing of a streaming request or response.

Our recommendation is that anytime you have a field in a schema
referencing an attachment, that an `@attachment` annotation is present.
This should further be expanded to include documentation mentioning that
this field represents a pointer to an attachment. Once again it is important
to note that the purpose of these fields is simply to convey that an
attachment could be present.

```pdl
namespace com.linkedin.greetings.api

/**
 * A greeting
 */
record Greeting {
  id: long

  /**
   * Type 1 UUID representing a video attachment
   */
  @attachment
  content: string
}
```


In terms of the actual data supplied at runtime we suggest using Type 1
UUIDs.

[Technical Details on Type 1
UUIDs](https://en.wikipedia.org/wiki/Universally_unique_identifier#Version_1_.28date-time_.26_MAC_address.29)

The reason for suggesting Type 1 UUIDs is because it provides the best
guarantee of producing a globally unique identifier. This is important
since, as shown further below, attachments can be coalesced from
different machines/services which may lead to an identifier collision.

## Creating Attachments

In order to create an attachment, developers must implement the
following interface(s):

```java
/**
 * Represents a custom data source that can serve as an attachment.
 */
public interface RestLiAttachmentDataSourceWriter extends Writer
{
  /**
   * Denotes a unique identifier for this attachment. It is recommended to choose 
   * identifiers with a high degree of uniqueness, such as Type 1 UUIDs. 
   * For most use cases there should be a corresponding String field in a PDSC
   * to indicate affiliation.
   *
   * @return the {@link java.lang.String} representing this attachment.
   */
  public String getAttachmentID();
}
```

You’ll notice this extends `Writer` which is defined as the
following:

```java
/**
 * Writer is the producer of data for an EntityStream.
 */
public interface Writer
{
  /**
   * This is called when a Reader is set for the EntityStream.
   *
   * @param wh the handle to write data to the EntityStream.
   */
  void onInit(final WriteHandle wh);

  /**
   * Invoked when it it possible to write data.
   *
   * This method will be invoked the first time as soon as data can be written to the WriteHandle.
   * Subsequent invocations will only occur if a call to {@link WriteHandle#remaining()} has returned 0
   * and it has since become possible to write data.
   */
  void onWritePossible();

  /**
   * Invoked when the entity stream is aborted.
   * Usually writer could do clean up to release any resource it has acquired.
   *
   * @param e the throwable that caused the entity stream to abort
   */
  void onAbort(Throwable e);
}
```

The `Writer` class leverages an interface called
`WriteHandle` which is defined as follows:

```java
/**
 * This is the handle to write data to an EntityStream.
 */
public interface WriteHandle
{
  /**
   * This writes data into the EntityStream. This call may have no effect if the stream has been aborted
   * @param data the data chunk to be written
   * @throws java.lang.IllegalStateException if remaining capacity is 0, or done() or error() has been called
   * @throws java.lang.IllegalStateException if called after done() or error() has been called
   */
  void write(final ByteString data);

  /**
   * Signals that Writer has finished writing.
   * This call has no effect if the stream has been aborted or done() or error() has been called
   */
  void done();

  /**
   * Signals that the Writer has encountered an error.
   * This call has no effect if the stream has been aborted or done() or error() has been called
   * @param throwable the cause of the error.
   */
  void error(final Throwable throwable);

  /**
   * Returns the remaining capacity in number of data chunks. Always returns 0 if the stream is aborted or 
   * finished with done() or error()
   *
   * @return the remaining capacity in number of data chunks
   */
  int remaining();
}
```

These are the essential interfaces to keep in mind when defining an
attachment as they represent how your custom data source will be asked
to produce both the metadata as well as the raw bytes for your
attachment.

When it is time for a `RestLiAttachmentDataSourceWriter` to
produce data, it will first be invoked on
`RestLiAttachmentDataSourceWriter\#getAttachmentID()`.
Implementations should return a unique identifier that should be the
same identifier placed in the strongly typed `RecordTemplate`
payload as described earlier.

Next, the attachment will be invoked on
`onInit(WriteHandle)`. The provided `WriteHandle`
is the object that will be used to perform the actual writing of bytes
later, so implementations should save a reference to it.

Subsequently, at some point in time in the future, the attachment will
be invoked on `Writer\#onWritePossible()`. It is at this
point that implementations should write raw bytes on
`WriteHandle\#write(ByteString)`. The amount of times that
the writer may write will be based on what is returned from
`WriteHandle\#remaining()`. Once the number of writes
remaining has been honored, then again at some time in the future, the
attachment will be invoked again on
`Writer\#onWritePossible()`. Then the attachment simply
repeats the logic above. The Javadoc is clear about this behavior for
developers to follow.

The size of the chunk written is up to the developer but keep in mind
that the larger the chunks written, the more memory that may be used by
the application at any given time. Furthermore, in order to minimize
copies, developers should use
`ByteString\#unsafeWrap(byte\[\])` to wrap byte arrays that
need to be written out.

## Reading Attachments

Reading attachments is a multi-step callback driven process which allows
developers to asynchronously walk through each attachment. It begins
with a top level `RestLiAttachmentReader` and a
`SingleRestLiAttachmentReader` for each individual attachment
encountered. This applies whether a client is reading a server’s
response attachments or a server reading a client’s incoming request
attachments.

There are two callbacks involved, one for the
`RestLiAttachmentReader` and one for the
`SingleRestLiAttachmentReader`. The relevant interfaces are
as follows:

```java
/**
 * Used to register with {@link com.linkedin.restli.common.attachments.RestLiAttachmentReader}
 * to asynchronously drive through the reading of multiple attachments.
 */
public interface RestLiAttachmentReaderCallback
{
  /**
   * Invoked (at some time in the future) upon a registration with a {@link RestLiAttachmentReader}.
   * Also invoked when previous attachments are finished and new attachments are available.
   *
   * @param singleRestLiAttachmentReader the {@link RestLiAttachmentReader.SingleRestLiAttachmentReader}
   *                                     which can be used to walk through this attachment.
   */
  public void onNewAttachment(RestLiAttachmentReader.SingleRestLiAttachmentReader singleRestLiAttachmentReader);

  /**
   * Invoked when this reader is finished which means all attachments have been consumed.
   */
  public void onFinished();

  /**
   * Invoked as a result of calling {@link RestLiAttachmentReader#drainAllAttachments()}.
   * This will be invoked at some time in the future when all the attachments in this
   * reader have been drained.
   */
  public void onDrainComplete();

  /**
   * Invoked when there was an error reading attachments.
   *
   * @param throwable the Throwable that caused this to happen.
   */
  public void onStreamError(Throwable throwable);
}
```

```java
/**
 * Used to register with
 * {@link com.linkedin.restli.common.attachments.RestLiAttachmentReader.SingleRestLiAttachmentReader}
 * to asynchronously drive through the reading of a single attachment.
 */
public interface SingleRestLiAttachmentReaderCallback
{
  /**
   * Invoked when data is available to be read on the attachment.
   *
   * @param attachmentData the {@link com.linkedin.data.ByteString} representing the current
   *                       window of attachment data.
   */
  public void onAttachmentDataAvailable(ByteString attachmentData);

  /**
   * Invoked when the current attachment is finished being read.
   */
  public void onFinished();

  /**
   * Invoked when the current attachment is finished being drained.
   */
  public void onDrainComplete();

  /**
   * Invoked when there was an error reading the attachments.
   *
   * @param throwable the Throwable that caused this to happen.
   */
  public void onAttachmentError(Throwable throwable);
}
```

The process begins by registering a callback of type
`RestLiAttachmentReaderCallback` as shown above with the
provided `RestLiAttachmentReader`. At some point in time in
the future, the `RestLiAttachmentReaderCallback` will be
invoked on
`RestLiAttachmentReaderCallback\#onNewAttachment(SingleRestLiAttachmentReader)`.
This `SingleRestLiAttachmentReader` is what is used to
traverse through each individual attachment.

The process then continues for each attachment as developers must
register a `SingleRestLiAttachmentReaderCallback` with the
provided `SingleRestLiAttachmentReader`. Once registered, the
`SingleRestLiAttachmentReader` can then be told to produce
attachment data via
`SingleRestLiAttachmentReader\#requestAttachmentData()`. Once
this is invoked, at some point in time in the future, the
`SingleRestLiAttachmentReaderCallback` will be invoked on
`SingleRestLiAttachmentReaderCallback\#onAttachmentDataAvailable(ByteString)`
representing the data for the reader to consume. Once the attachment
data is consumed, another call may be made to
`SingleRestLiAttachmentReader\#requestAttachmentData()`
thereby driving through all the data in that attachment.

Refer to the Javadocs provided for each class to obtain technical
details as to how to use each API. Additional features, such as
attachment draining and exception handling, are also described.

## Chaining (Proxying) Attachments Across Services

Rest.li streaming supports the ability to proxy attachments meaning
that:

  - A server can take an incoming request and send one or more of its
    attachments as a request further downstream.
  - A server can take one or more attachments from a response to a
    downstream request, and then send them back to the original request.
  - Clients and servers can coalesce multiple attachments from different
    sources.
  - This then becomes useful for observer or authentication patterns.

Here is an outline of what it may look like across multiple
services:

![](https://cloud.githubusercontent.com/assets/8562437/15308616/1a858502-1b94-11e6-90e2-1d315dfc6c0d.png)

Note how these big blobs of data move seamlessly between multiple
services without any one service holding the entire blob in memory at
once.

The APIs listed below in the client and server API sections highlight
how to perform attachment chaining across multiple services.

## Client Streaming APIs

### Specifying Outgoing Attachments on the Client

The generated request builders have been augmented to allow developers
to append attachments to outgoing requests. These APIs are:

1\.
`appendSingleAttachment(RestLiAttachmentDataSourceWriter)` to
append a single attachment or to proxy an incoming
`SingleRestLiAttachmentReader` further downstream.  
2\. `appendMultipleAttachments(RestLiDataSourceIterator)` to
append multiple attachments (as defined by the
`RestLiDataSourceIterator` interface) or to proxy an incoming
`RestLiAttachmentReader` further downstream.

Here is sample code on the client side on what attachment creation would
look like when constructing a request:


```java
final byte[] clientSuppliedBytes = "ClientSupplied".getBytes();
final GreetingWriter greetingAttachment = new GreetingWriter(ByteString.copy(clientSuppliedBytes));
final StreamingGreetingsCreateBuilder createBuilder = new StreamingGreetingsBuilders().create();
createBuilder.appendSingleAttachment(greetingAttachment);
final Greeting greeting = new Greeting().setMessage("A greeting with an attachment");
final Request<EmptyRecord> createRequest = createBuilder.input(greeting).build();
try
{
  final Response<EmptyRecord> createResponse = getClient().sendRequest(createRequest).getResponse();
  Assert.assertEquals(createResponse.getStatus(), 201);
}
catch (final RestLiResponseException responseException)
{
  Assert.fail("We should not reach here!", responseException);
}
```

The implementation of `GreetingWriter` has not been provided
but it is a trivial implementation of
`RestLiAttachmentDataSourceWriter`.

### Accessing Response Attachments on the Client

By default the server will not send response attachments back unless a
client specifies it can handle them. To explicitly allow response
attachments to come back, developers will need to use
`RestLiRequestOptions` as follows when constructing the
request builders:

```java
final RestliRequestOptions defaultOptions = new RestliRequestOptionsBuilder()
  .setProtocolVersionOption(ProtocolVersionOption.USE_LATEST_IF_AVAILABLE)
  .setAcceptResponseAttachments(true)
  .build();
final StreamingGreetingsBuilders builders = new StreamingGreetingsBuilders(defaultOptions);
```

Without specifying this explicitly, a server will not be able to send
any attachments back. This is because by default the
`Accept-Type` header will not include
`multipart/related` as a valid accept type for the client to
handle.

Subsequently, in order for the client to access any response attachments
from the server, the `Response` object has been augmented
with two new APIs, `Response\#hasAttachments()` and
`Response\#getAttachmentReader()`.

Clients should first see if the response has any attachments, and if so,
call `getAttachmentReader()` to return the
`RestLiAttachmentReader` to walk through all the attachment
data.

## Server Streaming APIs

### Specifying Response Attachments on the Server

Even before assigning response attachments, the resource method should
first check to see if the client can handle any response attachments.
This is done via:

```java
if (getContext().responseAttachmentsSupported())
{
   //Client can handle response attachments
}
```

Since the server does not have response builders or any opposite
equivalent of the client side, resource methods must assign response
attachments using an instance of `RestLiResponseAttachments`.
The APIs exposed by `RestLiResponseAttachments` are very
similar to the client side request builders:

```java
/**
 * Append a {@link com.linkedin.restli.common.attachments.RestLiAttachmentDataSourceWriter}
 * to be placed as an attachment.
 *
 * @param dataSource the data source to be added.
 * @return the builder to continue building.
 */
public Builder appendSingleAttachment(final RestLiAttachmentDataSourceWriter dataSource)
{
  AttachmentUtils.appendSingleAttachmentToBuilder(_responseAttachmentsBuilder, dataSource);
  return this;
}

/**
 * Append a {@link com.linkedin.restli.common.attachments.RestLiDataSourceIterator} to be used
 * as a data source within the newly constructed attachment list. All the individual attachments
 * produced from the {@link com.linkedin.restli.common.attachments.RestLiDataSourceIterator}
 * will be chained and placed as attachments in the new attachment list.
 *
 * @param dataSourceIterator
 * @return the builder to continue building.
 */
public Builder appendMultipleAttachments(final RestLiDataSourceIterator dataSourceIterator)
{
  AttachmentUtils.appendMultipleAttachmentsToBuilder(_responseAttachmentsBuilder, dataSourceIterator);
  return this;
}
```

Once this is created, the `RestLiResponseAttachments` can be
assigned to the response via
`ResourceContext\#setResponseAttachments`.

Here is a complete example on how a server might respond with
attachments:

```java
if (getContext().responseAttachmentsSupported())
{
    final GreetingWriter greetingWriter = new GreetingWriter(ByteString.copy(greetingBytes));
    final RestLiResponseAttachments streamingAttachments = new RestLiResponseAttachments.Builder()
      .appendSingleAttachment(greetingWriter)
      .build();
    getContext().setResponseAttachments(streamingAttachments);
    callback.onSuccess(new Greeting().setMessage("Your greeting has an attachment since you were kind and "
      + "decided you wanted to read it!").setId(key));
}
```

Once again, the implementation of `GreetingWriter` has not
been provided but it is a trivial implementation of
`RestLiAttachmentDataSourceWriter`.

### Accepting Request Attachments on the Server

A resource method expresses that it can accept request attachments by
declaring `RestLiAttachmentReader` as a parameter in it’s
method signature. This parameter must be accompanied by the presence of
a new annotation: `RestLiAttachmentsParam`. Note that only
one parameter of this type can be declared and the
`RestLiAttachmentsParam` cannot be used with any other
parameter type.

Here is an example of what a method signature may look like:

```java
public void create(Greeting greeting, @CallbackParam Callback<CreateResponse> callback,
                   @RestLiAttachmentsParam RestLiAttachmentReader attachmentReader)
```

If the provided `RestLiAttachmentReader` is not null, the
resource method may walk through it absorbing attachment data as
described earlier above.

It is important to note that attachments cannot be sent to the server
for that endpoint if the resource method does explicitly allow for them
via the parameter declaration shown above. Therefore:  
1\. If resource method asks for them and client sends them, then there
is no problem and this is completely normal.  
2\. If resource method asks for them and client does not send them, the
resource method will see null for the
`RestLiAttachmentReader`.  
3\. If resource method does not ask for them and client sends them, a
bad request is sent back and the attachments are drained by the Rest.li
framework.  
4\. If resource method does not ask for them and client does not send
them, then there is no problem and this is completely normal.

## Good Programming Practice

Given that attachment streaming will be a new experience for Rest.li
developers, we encourage all engineers to consider the following when
writing their services:

  - On the server side, application developers need to fully drain any
    incoming multipart requests. The Rest.li framework will only abosrb
    the first part of the incoming request (the regular Rest.li
    payload). The rest of the payload will need to be consumed by the
    resource method. Failure to do so can lead to resource leaks (open
    connections) which can cause server instability.
  - Similarly on the client side, application developers should fully
    drain response attachments, otherwise the connection pool on the
    client side will hit its limit.
  - In the event of an exception thrown by the resource method, the
    Rest.li framework will attempt to drain all the request bytes.
    However such behavior should not be relied upon. Server developers
    should make every attempt to fully drain the incoming request even
    in the face of an exception.
  - If an exception is thrown (i.e bad URL in the request) before a
    resource method is invoked, Rest.li will fully absorb and drop the
    payload on the ground.
  - Reduce bandwidth when possible; i.e use partial update to avoid
    resending unnecessary data across the wire.

## Additional Developer Notes

  - There is an `InputStream` wrapper available that can
    allow a user specified Java `InputStream` to function as
    a data source for a `RestLiAttachmentDataSourceWriter`.
    Although its use is discouraged since it is an async wrapper around
    a synchronous library.
  - For clients issuing requests, only HTTP PUT or POST methods are
    allowed for attachment streaming:
    1.  Create 
    2.  Batch Create
    3.  Update
    4.  Batch Update
    5.  Partial Update
    6.  Batch Partial Update
    7.  Actions
  - Servers are allowed to send back attachments for ALL types of
    endpoints
  - A streaming supported server will need to be deployed
  - Client must explicitly specify that they can handle response
    attachments. Request builders by default will not specify
    attachments should be sent back.

## Attachment Streaming without Rest.li

Rest.li attachment streaming is built atop of a general purpose
multipart/mime layer. This layer allows application developers to:

  - Deploy a service to receive and send back attachments directly on
    top of R2 
  - Deploy clients to send and receive attachments directly on top of R2
  - Create multipart/mime requests (client) and responses (server) that
    conform to the RFC
  - Read multipart/mime responses (client) and requests (server) that
    conform to the RFC

Please see source, tests and examples in the `multipart-mime`
module for more details.

## Future Enhancements

LinkedIn will continue to improve and enhance the Rest.li streaming
experience. We have plans for:

  - Full parseq integration - The current APIs are callback centric
  - Streaming support for Rest.li filters - Allow the filters to view
    windows of bytes as attachments flow in an out of the server
  - IDL integration + request builders - Elevating the
    `RestLiAttachmentReader` parameter on the server side to
    the IDL and providing a request builder specifically targeted for
    this parameter. This would require the compat checker to detect any
    backward incompatible changes as well.
