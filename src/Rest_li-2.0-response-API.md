---
layout: api_reference
title: Rest.li 2.0 response API
permalink: /Rest_li-2_0-response-API
excerpt: Rest.li 2.0 response API
---

# Rest.li 2.0 response API



Pegasus 1.17.2 features a new set of response APIs for various batch operations. The motivation of this change is to simplify the old response APIs. This change focuses on two sets of operations: `BatchGet` and `BatchUpdate`/`BatchPartialUpdate`/`BatchDelete`.

BatchGet
--------
The following table summarizes the Rest.li response types:

| Response name       | Returned by                          | Strong-typed key? | Value type        | New in 1.17.2? |
|---------------------|--------------------------------------|-------------------|-------------------|----------------|
| BatchResponse       | BatchGetRequestBuilder.build()       | No                | T                 | No      |
| BatchKVResponse     | BatchGetRequestBuilder.buildKV()     | Yes               | T                 | No      |
| BatchEntityResponse | BatchGetEntityRequestBuilder.build() | Yes               | EntityResponse<T> | Yes     |

`EntityResponse` is a new `RecordTemplate` class, which contains three fields:
 - `entity` provides an entity record if the server resource finds a corresponding value for the key.
 - `status` provides an optional status code.
 - `error` provides the error detail from the server resource (generally `entity` and `error` are mutually exclusive as `null`, but it is ultimately up to the server resource).

Note that since `EntityResponse` contains `error` field, the `Map<K, V>` returned by `BatchEntityResponse.getResults()` contains both successful as well as failed entries. `BatchEntityResponse.getErrors()` will only return failed entries.

BatchUpdate/BatchPartialUpdate/BatchDelete
------------------------------------------
The response type of the BatchUpdate series methods are not changed. However, similar to `EntityResponse`, we added a new `error` field to `UpdateStatus` (the value type of the `BatchUpdate` series methods). Furthermore, `BatchKVResponse<K, UpdateStatus>.getResults()` will returns both successful as well as failed entries. `getErrors()` will only return failed entries.