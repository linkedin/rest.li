---
layout: guide
title: Validation in Rest.li
permalink: /Validation-in-Rest_li
excerpt: Rest.li validation
---

# Validation In Rest.li

## Contents

  - [Specifying Validation Rules](#specifying-validation-rules)
  - [Custom Validation Rules](#custom-validation-rules)
  - [Rest.li Validation Annotations](#restli-validation-annotations)
  - [Using the Rest.li Data Validator For Servers](#using-the-restli-data-validator-for-servers)
  - [Using the Rest.li Data Validator For Clients](#using-the-restli-data-validator-for-clients)
  - [Validating Data Without Rest.li Context](#validating-data-without-restli-context)
  - [Backwards Compatibility](#backwards-compatibility)

There are many situations in which incoming or outgoing data should be
validated. For example, when creating a record which stores user
information, you may want to check that the username field doesn’t
contain special characters, or you may want to ensure that all required
(non-optional) fields are present before processing the incoming record.
When a required field is not present (and doesn’t have a default value
either), calling getXXX() will throw a
`RequiredFieldNotPresentException`. So validating the entity beforehand
can simplify the error handling code because you wouldn’t need to call
hasXXX() for every single field before calling getXXX(). Also, you may
want to check that clients are not trying to modify certain fields, like
server-generated ids or timestamps. Rest.li validation provides a
mechanism to perform such validation.

To use Rest.li’s validation feature, you need to specify validation
rules and choose how to enable validation.

## Specifying Validation Rules

Rest.li validates using three types of rules:

1.  Schema validation rules (checking the type of field values and the
    presence of required fields)
2.  Custom validation rules (checking whether a number is in some range,
    matching a string to a regular expression, etc)
3.  Rest.li validation annotations (`@ReadOnly` and `@CreateOnly`)

The first two rules are specified in the data schema (`.pdl` file), and
the third one is specified in the resource implementation. This is
because the first two rules only deal with information in the data
itself, but the third rule also needs additional Rest.li context such as
the method type. For example, a read only field shouldn’t be included in
a create request, but should be included in a get response.

For partial update requests (patches), the goal is to ensure that if the
patch is applied to a valid entity, the modified entity will also be
valid. For example, a patch that deletes a required field is invalid
because the modified entity will be missing that field. If there is a
custom validation rule on the username field that it must be at least 3
characters, a patch setting the username to “AA” is invalid.

You can read more about required and optional fields in [Data Schemas - PDL Syntax - Record Type](pdl_schema#record-type).

## Custom Validation Rules

Rest.li includes [some customizable
validators](https://github.com/linkedin/rest.li/tree/master/data/src/main/java/com/linkedin/data/schema/validator),
such as “strlen” and “regex”, that can be added to schemas. Developers
can write additional validators for any specific need.

For example, to use “strlen” to validate a string between 1 and 20 chars
long, we add it to the “validate” map of the field in the schema, for
example:

```pdl
namespace com.example

record Fortune {
  @validate.strlen = {"min": 1, "max": 20}
  message: string
}
```

Validator names are case sensitive and must have a matching validator
Java class in the current classpath. Rest.li finds the validator class
by uppercasing the first letter of the validator name and appending the
“Validator” suffix. For example, “strlen” maps to
[StrlenValidator](https://github.com/linkedin/rest.li/blob/master/data/src/main/java/com/linkedin/data/schema/validator/StrlenValidator.java).
Developers writing additional validators only need to write a class
extending `AbstractValidator` and include it in the classpath to use it.

Additional details are described in the javadoc for
[DataSchemaAnnotationValidator](https://github.com/linkedin/rest.li/blob/master/data/src/main/java/com/linkedin/data/schema/validator/DataSchemaAnnotationValidator.java).

## Rest.li Validation Annotations

Fields in a pegasus data schema can be either required or optional.
However, there are certain cases where this distinction is not
expressive enough. For example, a client shouldn’t send a
server-generated id in a create request (i.e. id is optional), but the
server must send the id when responding to a get request (i.e. id is
required). To cover cases like this, we introduce two new Rest.li
validation annotations: `@ReadOnly` and `@CreateOnly`.

A `@ReadOnly` field cannot be set or changed by the client, ex.
server-generated ID field. A `@CreateOnly` field can be set the client
when creating the entity, but cannot be changed by the client
afterwards. This annotation implies that the field is immutable, ex. a
purchase price. The client will send the price to the CREATE method
while creating a purchase entry.

As a best practice, Rest.li validation annotation should not conflict
with schema validation rules and custom validation rules specified in
the schema. For example, `@ReadOnly` should only be used to enforce that
an optional field is not present. It should not be specified for a
required field, making missing required field value valid.

The [Rest.li data
validator](https://github.com/linkedin/rest.li/blob/master/restli-common/src/main/java/com/linkedin/restli/common/validation/RestLiDataValidator.java)
will enforce the following rules for fields in request data based on the
annotation:

|                          | Create                                | Partial Update      |
| ------------------------ | ------------------------------------- | ------------------- |
| `@ReadOnly`   | Must not be present (See notes below) | Must not be present |
| `@CreateOnly` | N/A                                   | Must not be present |

Batch create, batch update, and batch partial update are treated the
same as create, update, and partial update respectively. Note that
validation is not turned on by default, and servers have to manually
call the validator or use the validation filter.

### Notes on @ReadOnly Validation Rules for Create Request

If `@ReadOnly` is specified to a field that is required in schema, the
field is treated as optional and the validation rule enforces that the
field is not present in the Create request data.

### Validation for Update Requests

`@ReadOnly` and `@CreateOnly` does not affect input data validation for
Rest.li update requests. This is because update is a PUT method and
should contain the whole entity, unlike partial update where
non-modified fields can be omitted. In the update request, the
`@ReadOnly` or `@CreateOnly` field is expected to have the same value as
the original entity (if the field was missing from the original entity,
it should be missing in the update request too). *However, this is not
checked by the Rest.li framework and should be checked manually in the
resource implementation.*

### Specifying Rest.li Validation Annotations

Rest.li validation annotations are specified on top of the resource. For
example,  
```java  
@RestLiCollection(name = "photos", namespace = "com.linkedin.restli.example.photos")
@ReadOnly("urn")  
@CreateOnly({"/id", "/EXIF"})
public class PhotoResource extends CollectionResourceTemplate<Long, Photo>
{
  ...
}
```
Every path should correspond to a field in a record, and not an enum
constant, a member of a union, etc. Paths should be specified in the
format used by
"PathSpec":https://github.com/linkedin/rest.li/blob/master/data/src/main/java/com/linkedin/data/schema/PathSpec.java.
Note that the first / character can be either specified or omitted. You
can check the correct path for a field by getting its PathSpec and
calling toString(). For example, if the `ValidationDemo` record contains
an array field like this:

```pdl
record ValidationDemo {
  ArrayWithInlineRecord: optional array[record myItem {
    bar1: string,
    bar2: string
  }]
  // ...
}
```

`ValidationDemo.fields().ArrayWithInlineRecord().items().bar1().toString()`
will return `/ArrayWithInlineRecord/*/bar1`.

You can also refer to these rules:  
- For a non-nested field, put the field name. e.g. “stringA”  
- For a nested field, put the full path separated by / characters. e.g.
“location/latitude”  
- For a field of an array item, specify the array name followed by the wildcard and the field name. e.g. “ArrayWithInlineRecord/\*/bar1”  
- Similarly, for a field of a map value, specify the map name followed by the wildcard and the field name.  
- For a field of a record inside a union, specify the union name, followed by the fully qualified record schema name, and then the field name. e.g. “UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo2”

Because full paths are listed, different rules can be specified for
records that have the same schema. For example, if the schema contains
two Photos, you can make the id of photo1 ReadOnly and id of photo2
non-ReadOnly. This is different from the optional/required distinction
specified in data schemas, where if the id of photo1 is required, the
id of photo2 will also be required.

## Using the Rest.li Data Validator For Servers

The Rest.li data validator can be called directly or indirectly using
the validation filters. When the resource calls it directly, it gets
specific error messages about where and why the validation failed, and
can decide how to handle the error. When a filter is used instead, the
resource does not get to examine why validation failed. Instead, the
client gets an error response with a message describing why the
validation failed. The validation filters are convenient if you want to
simply discard invalid requests or responses. On the other hand, if you
need to log the error or fail requests for only certain types of errors,
you need to call the validator directly.

### Request validation before resource handling

The [RestLiValidationFilter](https://github.com/linkedin/rest.li/blob/master/restli-server/src/main/java/com/linkedin/restli/server/validation/RestLiValidationFilter.java)
rejects all invalid requests automatically. It sends a 422
(Unprocessable Entity) error response back to the client if the data is
invalid. A sample error message is:  
```  
ERROR :: /stringA :: ReadOnly field present in a create request
ERROR :: /stringB :: field is required but not found and has no default value
```

### Response validation after resource handling

The RestLiValidationFilter also discards all invalid responses. The
filter sends a 500 error response back to the client if the response is
invalid. A sample error message is:  
```  
ERROR :: /stringA :: length of “Lorem ipsum dolor sit amet” is out of range 1...10
ERROR :: /stringB :: field is required but not found and has no default value
```

[Rest.li Filters](Rest_li-Filters) explains how to install the filter.

### Request validation during resource handling

To use the Rest.li data validator explicitly, it needs to be declared as
a method parameter using the `@ValidatorParam` annotation.
For example, to validate the input of a create request,
```java
@RestMethod.Create  
public CreateResponse create(final Fortune entity, @ValidatorParam RestLiDataValidator validator)
{
  ValidationResult result = validator.validateInput(entity);  
  if (!result.isValid())  
  {  
    throw new RestLiServiceException(HttpStatus.S_422_UNPROCESSABLE_ENTITY, result.getMessages().toString());  
  }  
  ...
}
```

Batch requests have to be validated one by one:  
```java
@RestMethod.BatchPartialUpdate
public BatchUpdateResult<Integer, Fortune> batchUpdate(BatchPatchRequest<Integer, Fortune> updates, @ValidatorParam
RestLiDataValidator validator)
{
  for (Map.Entry<Integer, PatchRequest<Fortune>> entry : entityUpdates.getData().entrySet())
  {
    Integer key = entry.getKey();
    PatchRequest<Fortune> patch = entry.getValue();
    ValidationResult result = validator.validateInput(patch);
    if (result.isValid())
    {
      // update entity
    }
    else
    {
      errors.put(key, new RestLiServiceException(HttpStatus.S_422_UNPROCESSABLE_ENTITY, result.getMessages().toString()));
    }
  }
  ...
}
```

The [ValidationDemoResource](https://github.com/linkedin/rest.li/blob/master/restli-int-test-server/src/main/java/com/linkedin/restli/examples/greetings/server/ValidationDemoResource.java)
class shows how to use the validator for each resource method type.

### Response validation during resource handling

Similar to request validation, the Rest.li data validator needs to be
declared as a method parameter.  
For example:

```java
@RestMethod.BatchGet
public Map<Integer, Fortune> batchGet(Set<Integer> ids, @ValidatorParam RestLiDataValidator validator)
{
  Map<Integer, Fortune> resultMap = new HashMap<Integer, Fortune>();
  ...
  for (Fortune entity : resultMap.values())
  {
    ValidationResult result = validator.validateOutput(entity);
    if (!result.isValid())
    {
      // fix the entity
    }
  }  
  ...
}
```

## Using the Rest.li Data Validator For Clients

### Request validation

Clients can validate requests before sending it to the server, to ensure
that the request wouldn’t be rejected by the server. Request builders
for create, update, partial update and their respective batch operations
have validateInput() methods.

```java
Photo newPhoto = new Photo().setTitle(“New Photo”).setFormat(PhotoFormats.PNG).setExif(newExif);
ValidationResult validationResult = PhotosCreateRequestBuilder.validateInput(newPhoto);
if (validationResult.isValid())
{
  // send request
}
else
{
  // fix photo
}
```

Input data for batch requests have to be validated one by one:

```java  
for (PatchRequest<Photo> patch : patches)
{
  ValidationResult validationResult = PhotosPartialUpdateRequestBuilder.validateInput(patch);
  if (!validationResult.isValid())
  {
    // fix patch
  }
  ...
}
```

### Response validation

When validating data returned by the server, clients have to use the `ValidateDataAgainstSchema` class as explained below.

## Validating Data Without Rest.li Context

When ReadOnly or CreateOnly annotations are used, Rest.li context
(method type, request vs response) is necessary to validate the data.
Otherwise the data and the schema information is enough. [Data to Schema
Validation](how_data_is_represented_in_memory#data-to-schema-validation)
explains how to validate data using the `ValidateDataAgainstSchema`
class. If it is used with `DataSchemaAnnotationValidator`, it will
consider the first two types of rules out of three listed in
[Specifying Validation Rules](#specifying-validation-rules).

For example:

```java
// Send the request to the server and get the response
final Photo photo = restClient.sendRequest(request).getResponse().getEntity();
DataSchemaAnnotationValidator validator = new DataSchemaAnnotationValidator(photo.schema());
ValidationResult result = ValidateDataAgainstSchema.validate(photo.data(), photo.schema(), new ValidationOptions(), validator);
if (!result.isValid())
{
  // handle the error
} 
```

## Backwards Compatibility

Adding or removing a `@ReadOnly` or `@CreateOnly` annotation for a field is considered backwards incompatible. When an annotation is added, old client's create or partial update requests may fail validation because they still contain the field in the request. When an annotation is removed, clients may have to send fields that they didn’t have to before.
