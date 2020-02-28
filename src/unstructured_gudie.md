---
layout: guide
title: Unstructured data (blob) user guide
permalink: /user_guide/unstructured_data
excerpt: This user guide is about working with unstructured data (BLOB) in Rest.li framework.
index: 2
---

# Unstructured Data (BLOB) User Guide

## Contents

* [Summary](#summary)
* [About Unstructured Data](#about)
* [Features Overview](#features)
* [Unstructured Data Resources](#create)
* [Consume Unstructured Data](#consume)
* [FAQs](#faq)


## Summary

Rest.li applications are built around *Resources*. The key ingredient to creating a resource is _data model_, whose internal structure is defined by [Pegasus Data Schema](/rest.li/pdl_schema) in key-values style. A fundamental presumption was that such structure exists for every Rest.li data model. However, it's not the case for _unstructured_ data such as images or PDFs, which are usually consumed in raw binary forms without a containing data structure.

This user guide is about working with _unstructured data_ in Rest.li framework. This is _not_ a comprehensive guide to building Rest.li resources in general, which is already covered in great details at [Writing Resources](/rest.li/user_guide/restli_server#writing-resources). This guide focuses on the differences of unstructured data resource.

See also [Unstructured Data in Rest.li Quick Start](/rest.li/start/unstructured).

## About Unstructured Data

To Rest.li, the key difference about unstructured data is that they don't have any defined schema and don't have to be represented by a single generated class in Rest.li like schema-base data does (RecordTemplate). Unstructured data can be handled in the rawest form as a bytes array or a more advanced form as InputStream/ByteBuffer in Java for example.

Additionally, there are several other differences that set them apart from the typical structured data:

* **Different Wire Protocol.** Rest.li transports _structured_ data as JSON content. But _unstructured_ data should be transported in their own MIME type with the body contains only the binaries.
* **Larger Data Size.** Unstructured data are usually larger in sheer size. Buffering the entire payload in system memory may not be a good idea and not as necessary as for structured data (in order for the codec to work).
* **Different Data Handling.** Application logic is much less likely to have reasons to peek into or even mutate unstructured data on the fly. Once the data is minted or fetched, they should remain immutable during transportation. (except maybe for special handling like compression/decompression which is taken care by the framework anyway)
* **Different Types of Client.** The query for unstructured data is usually initiated by end-user clients in some native manner. A good example is that a web browser could initiate a binary upload/download without invoking any JavaScript logic.
* **Breakable.** Unstructured data can be broken down and processed as a series of byte chunks. This is perfect for streaming which is necessary to reduce the memory footprint.


## Features Overview

By default, unstructured data enjoys the same level of support as structured data in Rest.li: they can be modeled as various resource types and most resource-supporting features and tooling should work. **However**, because of the lack of RecordTemplate-based data model, any feature that works on the structure of the resource value, such as Field Projections, Entity Validation etc, do _not_ apply to unstructured data resources, although they will continue to work with structured data resources that live in the same Rest.li application.

**Features Highlights:**

* Model as Collection, Association or Simple (Singleton) with both Sync and Async I/O
* Model as sub-resource of structured data resources
* Download/Get, Post/Upload, Put and Delete methods
* Rest.li and R2 Filters
* Unstructured Data Streaming
* Generated Rest APIs documentation (limited)

**Not Supported Features:**

* Model as Action or Free-Form resource
* Model as parent-resource
* APIs of BATCH_*, FINDERS, PARTIAL_UPDATES
* Field projections (skipped)
* Decoration

**Streaming Support**

In this context, _streaming_ means the ability to transport and process _unstructured data_ in small chunks without the need to buffering the whole content in memory. It sounds appealing, but it introduces complexities for the app developers that might be unnecessary in most simple use cases. Therefore, Rest.li supports both non-streaming and streaming method.


## Unstructured Data Resources


### Base Resource Interfaces

Base interface determines the resource type, the resource key and value type. Unstructured data has its own set of base interfaces. The main difference is the absence of the resource value type. Each resource type has two variants: Non-Streaming and Streaming version. Non-streaming comes with synchronous and asynchronous style, while there is no such distinction for streaming.

**Non-Streaming base interfaces**
- Collection
    - `UnstructuredDataCollectionResource`
    - Async
        - `UnstructuredDataCollectionResourceAsync`
        - `UnstructuredDataCollectionResourceTask`
        - `UnstructuredDataCollectionResourcePromise`
- Association
    - `UnstructuredDataAssociationResource`
    - Async
        - `UnstructuredDataAssociationResourceAsync`
        - `UnstructuredDataAssociationResourceTask`
        - `UnstructuredDataAssociationResourcePromise`
- Simple
    - `UnstructuredDataSimpleResource`
    - Async
        - `UnstructuredDataSimpleResourceAsync`
        - `UnstructuredDataSimpleResourceTask`
        - `UnstructuredDataSimpleResourcePromise`

**Streaming base interfaces**
* `UnstructuredDataCollectionResourceReactive`
* `UnstructuredDataAssociationResourceReactive` 
* `UnstructuredDataSimpleResourceReactive`


### Working with Streaming Resources

**Highlights:**
* Streaming resources are _relatively_ more complicated to write, maintain and debug. It's recommended for high performance demand. Still, performance gain with using streaming is not guaranteed. Benchmarking is the best way to tell. 
* Creating a streaming resource doesn't automatically make it end-to-end streaming across network nodes. It only guarantees that no buffering will happen within the Rest.li application that host this resource. For example, the data source must provide a way to allow partial data fetching, same to the other end: the destination node must be able to consume the data partially.
* Rest.li streaming adapts the [EntityStream interface](https://github.com/linkedin/rest.li/wiki/EntityStream)


#### Resource Definition

The definition of streaming unstructured data resource is similar to a regular resource. However, no value type is needed.

```java
@RestLiCollection(name = "resumes", namespace = "com.mycompany")
public class ResumesResource extends UnstructuredDataCollectionResourceReactiveTemplate<String> { ... }
```

#### Download/Get API

The interface of streaming resource is similar to the [asynchronous resources](/rest.li/Asynchronous-Servers-and-Clients-in-Rest_li) with a callback parameter that's used to return the result.

```java
@Override
public void get(String resumeId, @CallbackParam Callback<UnstructuredDataReactiveResult> callback) {
    Writer<ByteString> writer = new SingletonWriter<>(ByteString.copy(UNSTRUCTURED_DATA_BYTES));
    callback.onSuccess(new UnstructuredDataReactiveResult(EntityStreams.newEntityStream(writer), MIME_TYPE));
}
```

**Get Response**

_UnstructuredDataReactiveResult_ represents the download response which encapsulates the unstructured data EntityStream as well as the metadata needed to return a successful response. Its merely a container and could be subclassed if desires.

**Writing Unstructured Data**

Streaming requires the data to be read/write in continuous chunks manner. Simple bytes array or InputStream won't do the job. Rest.li adopt the [EntityStream interface](https://github.com/linkedin/rest.li/wiki/EntityStream)

ByteString is essentially Rest.li's immutable bytes array implementation and is used here to represent a single _chunk_. EntityStream is the interface that provides the chunks when they are requested. Note that the chunk size is not enforced, however, it's recommended to make the size reasonable and consistent.

*Writing Unstructured Data w. R2 Writer*

Rest.li's R2 layer has its own similar EntityStream implementation. If a writer is already provided, it can be easily converted to Rest.Li Writer using [EntityStreamAdapters](https://github.com/linkedin/rest.li/blob/master/r2-core/src/main/java/com/linkedin/r2/message/stream/entitystream/adapter/EntityStreamAdapters.java) util. 

```java
Writer dataWriter = new ResumeDataWriter(id);
com.linkedin.entitystream.Writer<ByteString> writer = EntityStreamAdapters.toGenericWriterx(dataWriter);
```

**Setting the Content-Type**

A content-specific [MIME content-type](https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types) is required for the unstructured response to be handled correctly by its clients. It is required as part of the _UnstructuredDataReactiveResult_ and is used _as it-is_ in the HTTP response header. No validation is done by Rest.li.

**Setting Additional Headers**

More headers/metadata can be set using the ResourceContext. Here is an example to add a 'disposition' header to the response:

```java
getContext().setResponseHeader("Content-Disposition", "attachment; filename=\"filename.jpg\"");
```


### Rest.li Filters and Unstructured Data

Rest.li [filters](/rest.li/Rest_li-Filters) currently don't support access to the unstructured data payload. Any existing or new filter that tries to access the payload will get an empty record. (No, they won't just fail.)


### Resource IDL for Unstructured Data

Resource IDLs are also generated for unstructured data resources, with a few minor differences in the generated IDL and Restspec files:

* A new "entityType" field to indicate the resource entity type being UNSTRUCTURED_DATA
* The existing "schema" field is empty when the "entityType" field is UNSTRUCTURED_DATA



### Online Documentation

Rest.li generates online [API documentation](/rest.li/user_guide/restli_server#online-documentation) for every resource. It also works for unstructured data resource, however, in the API page, unstructured data is currently treated as an empty missing model.


## Consume Unstructured Data

**Highlights**
* Unstructured data resources are designed to be invoked by any HTTP client natively without the need of decoding/unwrapping by a _rich client_ like Rest.li's RestClient. In fact, no request builders are generated at all for unstructured data resources. 
* Use a D2 client directly for use cases that need the dynamic host feature from RestClient.


### Response Anatomy

A simple unstructured data GET response:

```sh
curl 'http://myhost/resumes/1'

HTTP/1.1 200 OK
Content-Type: application/pdf
Content:
<<< bytes >>>
```


### Using an Http Client

One common Http client is a native browser (not the JavaScript client _lives_ in a browser). Unstructured data resource endpoints can be used in place wherever a standard web resource link is expected.

```html
<html>
  <a src="http://myhost/resumes/1">Download Resume</a>
</html>
```

### Using a D2 Client

[D2](/rest.li/Dynamic_Discovery) is what powers the host finding capability of RestClient under the hood. With a [D2 client](/rest.li/start/d2_quick_start#step-3-create-a-client), you can send a request without having to specify the actual hostname of your resources:

```java
URI uri = URI.create("d2://resumes/1");
StreamRequest req = new StreamRequestBuilder(uri).build(...);
d2Client.streamRequest(req, responseCallback)
...
```

### Error Handlings

A request to unstructured data resource could fail in the [same ways](/rest.li/user_guide/restli_server#returning-errors) a regular resource does. Moreover, even when a request such as Get is successful, the data flow could still be interrupted or timeout. When that happens, you could receive a successful HTTP status like 200 and still get an incomplete response or long hanging that results in a client timeout.


## FAQs

* *Q: Is there a size limit on how large an unstructured data could be served by Rest.li resource?*
A: No. But practically, the size will be cap base on your server's timeout value. If you are seeing incompleted content on the client, it could be caused by an undersize server timeout value.

* *Q: What should be a reasonable server timeout?*
A: It depends on if the Rest.li application hosts a mix of structured and unstructured resources. Currently, Rest.li only allow one timeout setting for the entire app. You may not want a long timeout for APIs that serve small structured data. On the other hand, a short timeout for APIs that serve large unstructured data.

* *Q: Should I create a streaming or non-streaming resource for my unstructured data?*
A: First of all, streaming doesn't come for free and true end-to-end streaming also depends on your other nodes in the network, so make sure you understand what you are getting into. Secondly, the performance depends on many factors such as the size of the data and I/O performances etc.

* *Q: What is reactive streaming and how can I leverage it?*
A: [EntityStream](https://github.com/linkedin/rest.li/wiki/EntityStream)
