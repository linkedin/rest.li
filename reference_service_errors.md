---
layout: api_reference
title: Service Errors
permalink: /spec/service_errors
index: 2
excerpt: This page describes how service errors are returned by Rest.li and how they are documented in a resource's IDL.
---

# Service Errors

{{ page.excerpt }}

See [Configuring Service Errors in Java](/rest.li/user_guide/service_errors_java) for a step-by-step guide showing how
to configure service errors in Rest.li Java.

## Contents

-   [Error Responses](#error-responses)
    - [Fields](#fields)
    - [Example](#example)
    - [Returning a Subset of Fields](#returning-a-subset-of-fields)
-   [Documenting Service Errors in the IDL](#documenting-service-errors-in-the-idl)
    - [Resource-Level Errors](#resource-level-errors)
    - [Method-Level Errors](#method-level-errors)
    - [Success Codes](#success-codes)
    - [Backward Compatibility](#backward-compatibility)

## Error Responses

All error responses are returned by Rest.li in a format conforming to the
[ErrorResponse schema]({{site.data.urls.repo}}/restli-common/src/main/pegasus/com/linkedin/restli/common/ErrorResponse.pdsc),
which contains various fields describing the service failure.

### Fields

| Field             | Description |
|-------------------|-------------|
| `status`          | The HTTP status code. |
| `code`            | The canonical error code, e.g. for '400 Bad Request' it can be 'INPUT_VALIDATION_FAILED'. Only predefined codes should be used. |
| `message`         | A human-readable explanation of the error. |
| `docUrl`          | URL to a page that describes this particular error in more detail. |
| `requestId`       | The unique identifier that would identify this error. For example, it can be used to identify requests in the service's logs. |
| `exceptionClass`  | The FQCN of the exception thrown by the server. |
| `stackTrace`      | The full stack trace of the exception thrown by the server. |
| `errorDetailType` | The type of the error detail model, e.g. com.example.api.BadRequest. Clients can use this field to identify the actual error detail schema. |
| `errorDetails`    | This field should be used for communicating extra error details to clients. |

### Example

Here is an example error response serialized to JSON as it would be sent over the wire and received by the client. This
scenario involves a client which has used up its daily quota for a particular service, triggering the resource method to
throw a fictitious `QuotaExceededException`. The error details object conforms to the fictitious `QuotaDetails` schema,
which contains relevant information about the quota usage.

Please note that the stack trace has been truncated for the sake of space.

```json
{
    "status": 429,
    "code": "QUOTA_EXCEEDED",
    "message": "You've exceeded your daily request quota.",
    "docUrl": "https://example.com/docs/errors/QUOTA_EXCEEDED",
    "requestId": "cgA4qNoE48AJabrC",
    "exceptionClass": "com.example.QuotaExceededException",
    "stackTrace": "Exception in thread \"main\" com.example.QuotaExceededException: ...",
    "errorDetailType": "com.example.api.QuotaDetails",
    "errorDetails": {
        "interval": "DAILY",
        "quota": 10000,
        "usage": 10034
    }
}
```

### Returning a Subset of Fields

By default, Rest.li returns the error response containing all the fields described above, in addition to
an `X-RestLi-Error-Response` HTTP header. However, Rest.li supports returning only a subset of these fields.

#### Java

In Java, the `RestLiConfig` class is used to configure which `ErrorResponse` fields should be included
in the response.

Using a predefined error response format to only return the status, code, and message:

```java
restLiConfig.setErrorResponseFormat(ErrorResponseFormat.MESSAGE_AND_SERVICECODE);
```

```json
{
    "status": 429,
    "code": "QUOTA_EXCEEDED",
    "message": "You've exceeded your daily request quota."
}
```

Creating a custom subset of fields to return:

```java
EnumSet<ErrorResponseFormat.ErrorResponsePart> parts = EnumSet.of(HEADERS,
                                                                  STATUS_CODE_IN_BODY,
                                                                  DOC_URL,
                                                                  STACKTRACE);
restLiConfig.setErrorResponseFormat(new ErrorResponseFormat(parts));
```

```json
{
    "status": 429,
    "docUrl": "https://example.com/docs/errors/QUOTA_EXCEEDED",
    "stackTrace": "Exception in thread \"main\" com.example.QuotaExceededException: ..."
}
```

## Documenting Service Errors in the IDL

Semantically, an error response is still a valid API response – it’s one of the possible outputs of an API call - so it
must be part of the formal contract. Having well-documented service errors as part of an API enables clients to handle
failures resiliently, and allows developers to intelligently prepare their clients for failures.

For a full, in-depth example of what an IDL looks like documented with service errors and success codes, see
[AlbumEntryResource]({{site.data.urls.repo}}/restli-example-server/src/main/java/com/linkedin/restli/example/impl/AlbumEntryResource.java)
and its [generated IDL]({{site.data.urls.repo}}/restli-example-api/src/main/idl/com.linkedin.restli.example.photos.albumEntry.restspec.json).

### Resource-Level Errors

If a resource has been given a set of service errors (done in Java using `@ServiceErrors`, see
[here](/rest.li/user_guide/service_errors_java)), those service errors will be documented in the IDL as a list of
objects, each containing the HTTP status code, the application-specific string error code, the message, and the
error detail type (for each that is present). For example, a resource `someResource` that may fail due to some
rate-limiting restrictions will be documented in the IDL as:

```js
{
  "name" : "someResource",
  ...
  "collection" : {
    "serviceErrors" : [ {
      "status" : 429,
      "code" : "QUOTA_EXCEEDED",
      "message" : "You've exceeded your daily request quota.",
      "errorDetailType" : "com.example.api.QuotaDetails"
    } ],
    ...
  }
  ...
}
```

### Method-Level Errors

If a resource has been given a set of service errors (done in Java using `@ServiceErrors` or `@ParamErrors`, see
[here](/rest.li/user_guide/service_errors_java)), those service errors will be documented in the IDL as a list of
objects similar to what is shown above. The difference here is that these will be documented per-method, and also
support indicating specific parameter names. For example, a `finder` method that may fail on some parameter `albumId`
will documented in the IDL as:

```js
{
  "serviceErrors" : [ {
    "status" : 422,
    "code" : "INVALID_ID",
    "message" : "Id cannot be less than 0",
    "errorDetailType" : "com.example.api.IdError",
    "parameters" : [ "albumId" ]
  } ],
  "name" : "search",
  "parameters" : [ {
    "name" : "albumId",
    "type" : "long",
  }
}
```

### Success Codes

If a resource method has been given a set of success codes (done in Java using `@SuccessResponse`), those success codes
will be documented in the IDL as a list of integers. For example, a `get` method that returns an HTTP `200` response on
success will be documented in the IDL as:

```json
{
  "success" : [ 200 ],
  "method" : "get",
  "doc" : "Gets a thing."
}
```

### Backward Compatibility

Making changes to the service error information in a resource's IDL has an impact on the
[compatibility checker](/rest.li/modeling/compatibility_check).

The following changes are considered **backward compatible**:

- Removing a service error code from a resource or a method.
- Removing a service error code from a resource then adding it to a subset of its methods.

The following changes are considered **backward incompatible**:

- Adding a new service error code to a resource or a method.
  - *Note:* Changing an existing code is treated semantically as removing one and adding another.
- Changing the `errorDetailType` for an existing service error code.
- Changing the HTTP `status` code for an existing service error code.
- Changing the `message` for an existing service error code.
