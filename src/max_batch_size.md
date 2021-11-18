---
layout: guide
title: Configure Max Batch Size in Java
permalink: /max_batch_size
excerpt: This page describes how to set up max batch size limitation for resource batch methods in Java.
---

# Configure Max Batch Size in Java
{{ page.excerpt }}

## Contents
-   [Define Max Batch Size](#define-max-batch-size)
-   [Batch Size Validation](#batch-size-validation)
-   [Backward Compatibility Rules](#backward_compatibility_rules)

For Rest.li resources, we provide infrastructure support that enables users to define the `@MaxBatchSize` annotation on the batch methods and provides the opt-in batch size validation based on the defined `@MaxBatchSize`.

## Define Max Batch Size

Users can use `@MaxBatchSize` annotation to define the max batch size on Rest.li batch methods.

```java
@MaxBatchSize(value = 100, validate = true)
public BatchResult<Long, Photo> batchGet(Set<Long> ids)
{
 // Logic here...
}

```
The `@MaxBatchSize` annotation contains two elements: `value` and `validate`.
`value` is an integer specifying the max batch size.
`validate` is a boolean flag for validating the request batch size, which is an optional field. If the `validate` is not provided, the value of `validate` is `false` by default, which means there is no request batch size validation. 

List of methods which can use `@MaxBatchSize` annotation: `BATCH_GET`, `BATCH_UPDATE`, `BATCH_PARTIAL_UPDATE`, `BATCH_DELETE`, `BATCH_CREATE` and `BATCH_FINDER`.

*Note*: for `BATCH_FINDER`, the batch size means the number of criteria, ***not*** the collection size for each criteria.

### Documenting max batch size information in the IDL.
Once the `@MaxBatchSize` annotation is defined, the batch size information will be exposed in the IDL(`restspec.json`), for example:


```js
{ "name" : "someResource",
   ...
  "collection" : {
	"methods" : [ {
      "method" : "batch_get",
      "maxBatchSize" : {
        "value" : 100,
        "validate" : true
      }
    }, {
      "method" : "batch_update",
      "maxBatchSize" : {
        "value" : 100,
        "validate" : false
      }
    } ],
    ...
  }
  ...
}
```

## Batch Size validation
If `validate` in the `@MaxBatchSize` is specified as `true`, on the Rest.li server, when it processes the request, it will check the request that has `@MaxBatchSize` annotation, and compare the batch size of each request with the defined max batch size of such method. If the actual batch size is larger than the defined max batch size, it will fail the request with http status *400*.

Example of enabling batch size validation:
```java
@RestMethod.BatchGet
@MaxBatchSize(value = 100, validate = true)
public Map<Long, Greeting> batchGet(Set<Long> ids)
{
 // Logic here...
}

```

Batch size validation is an opt-in feafture. If `validate` in the `@MaxBatchSize` is specified as `false` or `validate` field is not provided, the `@MaxBatchSize` is used to provide batch size information, the Rest.li server will not take any action based on the value of max batch size.

Examples of disabling batch size validation:
```java
@RestMethod.BatchDelete
@MaxBatchSize(value = 50, validate = false)
public BatchUpdateResult<Long, Greeting> batchDelete(BatchDeleteRequest<Long, Greeting> deleteRequest)
{
 // Logic here...
}

```

```java
@RestMethod.BatchPartialUpdate
@MaxBatchSize(value = 100)
public BatchUpdateResult<Long, Greeting> batchUpdate(BatchPatchRequest<Long, Greeting> entityUpdates)
{
 // Logic here...
}

```
## Backward Compatibility Rules
Making changes to the max batch size information in a resource's IDL has an impact on the
[compatibility checker](/rest.li/modeling/compatibility_check).

The following changes are considered **backward compatible**:

- Adding a `@MaxBatchSize` annotation with validation disabled.
- Removing an existing `@MaxBatchSize` annotation.
- Increasing the value of max batch size.
- Decreasing the value of max batch size when validation is disabled.
- Updating `validate` value from `true` to `false`.


The following changes are considered **backward incompatible**:

- Adding a `@MaxBatchSize` annotation with validation enabled.
- Updating `validate` value from `false` to `true`.
- Decreasing the value of max batch size when validation is enabled.
