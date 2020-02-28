---
layout: guide
title: Configuring Service Errors in Java
permalink: /user_guide/service_errors_java
index: 2
excerpt: This page describes how to configure service errors for a resource in Java.
---

# Configuring Service Errors in Java

{{ page.excerpt }}

See [Service Errors](/rest.li/spec/service_errors) for an in-depth reference of service errors in Rest.li.

## Contents

-   [Background](#background)
-   [Step 1: Define your Service Errors](#step-1-define-your-service-errors)
-   [Step 2: Write Your Service Error Definition](#step-2-write-your-service-error-definition)
-   [Step 3: Apply the Service Errors to your Resource](#step-3-apply-the-service-errors-to-your-resource)
-   [Step 4: Returning Service Errors](#step-4-returning-service-errors)
-   [Step 5: Enabling Service Error Validation](#step-5-enabling-service-error-validation)

## Background

It's important to communicate to the consumers of a service what types of errors may be returned by a particular API.
Fortunately, Rest.li allows application developers to configure service errors for their resources. This makes APIs
clearer by documenting in the IDL what sorts of errors should be expected, allowing clients to effectively handle and
process returned errors. Also, enabling service error validation allows services to guarantee which errors a client
should expect to encounter.

## Step 1: Define your Service Errors

First, you need to determine which service error codes your service may return, and what information will be contained
in error responses corresponding with these codes. You'll need to define four things:

- The application-specific string code associated with some service error (e.g. `QUOTA_EXCEEDED`)
- The HTTP status code it'll return (e.g. `429`)
- The string message it'll return
- The schema defining the format of the error details

For example, say we want to standardize the way errors are returned as a result of violating the service's rate-limiting
constraint. We would need to pick a string error code such as `QUOTA_EXCEEDED`, an HTTP status such as `429`, a string
message describing the failure, and some schema describing the custom error details sent back to the client.

If you need help determining which HTTP status code is right for your service error, please
see [this table of codes](https://www.iana.org/assignments/http-status-codes/http-status-codes.xhtml).

## Step 2: Write Your Service Error Definition

Write a Java enum implementing the
[`ServiceError`](https://github.com/linkedin/rest.li/blob/master/restli-server/src/main/java/com/linkedin/restli/server/errors/ServiceError.java)
interface, which will contain a set of service error definitions for your service. The enum implementation may expose
methods for obtaining the HTTP status, the error code, the error message, and the error detail type. Here is an example
implementation which defines one service error and exposes all four accessor methods:

```java
public enum SampleServiceError implements ServiceError
{
  QUOTA_EXCEEDED(HttpStatus.S_429_TOO_MANY_REQUESTS, "You've exceeded your daily request quota.", QuotaDetails.class),
  INVALID_PERMISSIONS(HttpStatus.S_403_FORBIDDEN, "User does not have valid permissions", null),
  ILLEGAL_PARAM(HttpStatus.S_400_BAD_REQUEST, "Request parameter cannot be used", null);
  // Add more service errors here...

  SampleServiceError(HttpStatus status, String message, Class<? extends RecordTemplate> errorDetailType)
  {
    _httpStatus = status;
    _message = message;
    _errorDetailType = errorDetailType;
  }

  private final HttpStatus _status;
  private final String _message;
  private final Class<? extends RecordTemplate> _errorDetailType;

  /*
   * Suggested: Define string constants for each of the error codes for easy access.
   * String literals or constants must be used as annotation attributes due to the
   * restrictions imposed by Java.
   *
   * These codes should match the enum value names.
   */
  public class Codes
  {
    QUOTA_EXCEEDED = "QUOTA_EXCEEDED";
    INVALID_PERMISSIONS = "INVALID_PERMISSIONS";
    ILLEGAL_PARAM = "ILLEGAL_PARAM";
  }

  @Override
  public HttpStatus httpStatus()
  {
    return _status;
  }

  @Override
  public String code()
  {
    // Note how the application-specific code defers to the enum value name
    return name();
  }

  @Override
  public String message()
  {
    return _message;
  }

  @Override
  public Class<? extends RecordTemplate> errorDetailType()
  {
    return _errorDetailType;
  }
}
```

## Step 3: Apply the Service Errors to your Resource

Now that you have defined your service errors, you can use them to specify which resources
or particular resource methods may return such errors. You can do this by using a few
Java annotations described below.

Keep in mind that adding or removing service error codes impacts the backward compatibility of a resource.
See [here](/rest.li/spec/service_errors#backward-compatibility) for more information.

### `@ServiceErrorDef`

In order to apply your service error definitions to a particular resource, you first need to use the `@ServiceErrorDef`
annotation at the resource level to reference your enum. This class-level annotation is used to reference your service
error enum definition. This is used by Rest.li during build-time when generating IDLs and also during runtime when
validating service errors returned by your resource. Without this annotation, Rest.li will have no way of referencing
the information in your service error definition.

```java
@ServiceErrorDef(SampleServiceError.class)
class MyResource extends CollectionResourceTemplate<Long, MyRecord>
{
  // Resource methods here...
}
```

### `@ServiceErrors`

Next, you can use the `@ServiceErrors` annotation to indicate which service error codes may be returned by a particular
resource or method. It can be used as a class-level annotation (to refer to a resource) or as a method-level annotation
(to refer to a resource method). This is used during build-time when generating IDLs and also during runtime when
validating service errors returned by your resource. In the following example, all methods in the resource may return a
`QUOTA_EXCEEDED` error, but only `getAll` can return an `INVALID_PERMISSIONS` error:

```java
@ServiceErrorDef(SampleServiceError.class)
@ServiceErrors(SampleServiceError.Codes.QUOTA_EXCEEDED)
class MyResource extends CollectionResourceTemplate<Long, MyRecord>
{
  @Override
  public MyRecord get(Long id)
  {
    // Logic here...
  }

  @Override
  @ServiceErrors(SampleServiceError.Codes.INVALID_PERMISSIONS)
  public List<MyRecord> getAll()
  {
    // Logic here...
  }
}
```

### `@ParamError`

Additionally, you can use the `@ParamError` annotation to indicate that a service error may be returned by a particular
method, but also that it references a specific parameter of such method. This is used solely as a means of documentation
in the IDL; no validation at all is done at runtime using this information. In this example, the `search` method may
return an `ILLEGAL_PARAM` error related to either the `foo` parameter or the `bar` parameter:

```java
@ServiceErrorDef(SampleServiceError.class)
class MyResource extends CollectionResourceTemplate<Long, MyRecord>
{
  @Finder("search")
  @ParamError(code = SampleServiceError.Codes.ILLEGAL_PARAM, parameterNames = { "foo", "bar" })
  public List<MyRecord> search(@QueryParam("foo") String foo, @QueryParam("bar") String bar)
  {
    // Logic here...
  }
}
```

### `@SuccessResponse`

In addition to service errors, success response codes can also be specified for a resource method. Note that success
codes (unlike service errors) are not validated by the Rest.li framework, meaning that the framework will allow
unrecognized success codes to be returned. This feature is meant only for enhancing IDL documentation, please see
[this page](/rest.li/spec/service_errors#success-codes) for how this are documented. In this example, the `action`
method may return an HTTP `200` or `204` response:

```java
@ServiceErrorDef(SampleServiceError.class)
class MyResource extends CollectionResourceTemplate<Long, MyRecord>
{
  @Action(name = "action")
  @SuccessResponse(statuses = { HttpStatus.S_200_OK, HttpStatus.S_204_NO_CONTENT })
  public void action()
  {
    // Logic here...
  }
}
```

## Step 4: Returning Service Errors

To return one of your defined service errors, you can simply do the following in your
resource implementation:

```java
throw new RestLiServiceException(SampleServiceError.QUOTA_EXCEEDED);
```

This automatically constructs an `ErrorResponse` using information from your service error definition:

```json
{
  "code": "QUOTA_EXCEEDED",
  "httpStatus": 429,
  "message": "You've exceeded your daily request quota."
}
```

This looks good, except it's missing some extra information such as error details and
a request ID. Fortunately, you can use builder syntax to easily add this data:

```java
RestLiServiceException exception = new RestLiServiceException(SampleServiceError.QUOTA_EXCEEDED);

throw exception.setRequestId(requestId).setErrorDetails(quotaInfo);
```

You can also use purely builder syntax to construct your exception:

```java
throw new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST).setCode(code);
```

## Step 5: Enabling Service Error Validation

Returned service errors can be validated in order to ensure that only specified
service errors are returned. In other words, if a method returns an error that
isn't specified via an annotation, a `500` exception will be thrown.

This validation can be enabled by adding an `ErrorResponseValidationFilter` to
the server filter chain:

```java
restLiConfig.addFilter(new ErrorResponseValidationFilter());
```

Note that only exceptions containing a `code` property will be validated.
