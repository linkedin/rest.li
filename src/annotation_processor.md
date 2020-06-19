---
layout: guide
title: Introduction of Annotation Processor
permalink: /annotation_processor
excerpt: Documentation of using annotation processor in Rest.li
---

# Introduction of Annotation Processor

- [Introduction of Annotation Processor](#introduction-of-annotation-processor)
  - [Annotating Pegasus Schemas](#annotating-pegasus-schemas)
  - [Inherit and override schema annotations](#inherit-and-override-schema-annotations)
    - [A common application of overriding annotation using PathSpec](#a-common-application-of-overriding-annotation-using-pathspec)
  - [Use the schema annotation processor](#use-the-schema-annotation-processor)

This page introduces a generic schema annotation processing tool in Rest.li. Before reading this, it is recommended that readers be familiar with the concept of [PathSpec](/rest.li/pathspec) in Rest.li.

## Annotating Pegasus Schemas
In Rest.li, schema authors are able to add annotations using '@' sytnax to schema fields or the schema itself.

**Example**: annotate on the schema fields
```pdl
record UserPersonallyIdentifiableInformation {
  @data_classification = "MEDIUM"
  firstName: string,
  @data_classification = "MEDIUM"
  lastName: string,
  @data_classification = "LOW"
  userId: long,
  @data_classification = "HIGH"
  socialSecurityNumber: string,
}
```

**Example**: annotate directly on the schema
```pdl
@data_classification = "HIGH"
record UserPersonallyIdentifiableInformation {
  firstName: string,
  lastName: string,
  userId: long,
  socialSecurityNumber: string,
}
```
Note that the "data_classifcation" annotation is specified at record level.

These annotations in above examples are stored as an attribute inside [DataSchema](https://github.com/linkedin/rest.li/blob/master/data/src/main/java/com/linkedin/data/schema/DataSchema.java)'s class as "property". Just as the example shows, both the field and the DataSchema can have this "property". Rest.li framework did not provide specification on how these annotations should be interpreted and it was left to the user to add logic for interpreting them. 

## Inherit and override schema annotations
Rest.li users found it useful to process annotations during schema processing. One use case is to introduce "inheritance" and "overrides" to annotations so those annotations can be dynamically processed in the way user defines when the schemas were reused. This gives annotation extensibility and adds to schema reusability.

Here are examples: \\
**Example case 1** : Users might want the annotation of a field to be inherited. The fields can inherit the annotations from the upper level.
```pdl
@persistencePolicyInDays = 30
record UserVisitRecord {
  visitedUrls: array[URL]
  visitedUserProfiles: array[UserRecord]
}
```
Reading from the schema, we might find both `visitedUrls` and `visitedUserProfiles` have `persistencePolicyInDays` annotated as 30.

**Example case 2**: Users might want the annotation of a field to be overriden. Override is where the annotation on a field or a schema might get updated when other annotations assign it another value.
```pdl
@persistencePolicyInDays = 30
record  UserVisitRecord {
  @persistencePolicyInDays = 10
  visitedUrls: array[URL]

  visitedUserProfiles: array[UserRecord]
}
```
Reading from the schema, a user might find the field `visitedUserProfiles`s `persistencePolicyInDays` annotation is 30 but `visitedUrls`'s `persistencePolicyInDays` annotation has value 10, which overrides the original value inherited (i.e. the value 30).


**Example case 3**:  Override might also happen when the annotation needs to be updated by another value assigned from other annotation locations
```pdl
record UserVisitRecord {
  //...
  @persistencePolicyInDays = 365
  recycledChatHistories: array[chat]

  @persistencePolicyInDays = 10
  visitedUrls: array[URL]
  //...
}

record EnterpriseUserRecord {
  userName: UserName
  //...
  @persistencePolicyInDays = {"/recycledChatHistories" : 3650}
  visitRecord: UserVisitRecord
  //...
}
```
In this example, the schema `EnterpriseUserRecord` reused `UserVisitRecord` and its annotation in the field `visitRecord`, and overrides the annotation value for the field `recycledChatHistories`.


All above examples shows that inheritance and overrides give more extensibility to schema annotations. Users should be free to define their own rules regarding how those annotations are read. There should be an unambiguous annotation value for the fields after the user's rules are applied. The processing step to figure out the eventual value of a field or schema's annotation is called "resolution" and such value is called "resolved" value.

What is more, users could have their own customized logic to process the annotations and define the custom behavior when annotations are overridden or inherited, and even have the flexibility to set a customized resolved value by calling another local function or remote procedure. The need for such extensibility gives the motivation for Annotation Processor. We aim to build a tool that can process annotations during schema resolution, process all interested annotations by plugging in the user's own logic and saving the "resolved" value back to the corresponding DataSchema.

### A common application of overriding annotation using PathSpec
In the above `EnterpriseUserRecord` example, `@persistencePolicyInDays = {"/recycledChatHistories" : 3650}` were used to denote the field that are being overriden is `recycledChatHistories`. This is basically using PathSpec as a reference to the field or child schema that needs to be overriden. [PathSpec](/rest.li/pathspec) can be used as a path to define relationships between fields among nested schemas. Users can use PathSpec to unambiguously specify all the paths to child fields that they want to override.

The above usage is very common at LinkedIn so an implementation of annotation processing based on this behavior is provided as part of annotation processor.

It assumes \\
(1) All *overrides* to the fields are specified using PathSpecs to the fields. \\
(2) All *overrides* are pointing to fields, and not to the record.

For more examples regarding the syntax, one can read the java doc from [PathSpecBasedSchemaAnnotationVisitor](https://github.com/linkedin/rest.li/blob/master/data/src/main/java/com/linkedin/data/schema/annotation/PathSpecBasedSchemaAnnotationVisitor.java), [schema processor tests](https://github.com/linkedin/rest.li/blob/master/data/src/test/java/com/linkedin/data/schema/annotation/TestSchemaAnnotationProcessor.java) and [schema test examples using PathSpec based overriding](https://github.com/linkedin/rest.li/tree/master/data/src/test/resources/com/linkedin/data/schema/annotation/denormalizedsource).

Users can seek to extend this case to adapt to their own use cases. Please see next section regarding what class to extend to fit the best use cases.

## Use the schema annotation processor
We have created a paradigm of processing Rest.li schema annotations and wrapped them into `com.linkedin.data.schema.annotation` package.
First thing to understand is how [DataSchema](https://github.com/linkedin/rest.li/blob/master/data/src/main/java/com/linkedin/data/schema/DataSchema.java) object is internally stored. We added `resolvedProperties` attribute, in order to store the final resolved annotation.

For an example schema:

```pdl
record Employee {
  id: long
  supervisor: Employee
}
```

Its memory presentation is as followed:
<center>
<b>DataSchema Example</b><br><img src="{{ 'assets/images/DataSchema_with_resolvedProperties.png' | relative_url }}" />
</center>

The [SchemaAnnotationProcessor](https://github.com/linkedin/rest.li/blob/master/data/src/main/java/com/linkedin/data/schema/annotation/SchemaAnnotationProcessor.java) will process [DataSchema](https://github.com/linkedin/rest.li/blob/master/data/src/main/java/com/linkedin/data/schema/DataSchema.java) using [DataSchemaRichContextTraverser](https://github.com/linkedin/rest.li/blob/master/data/src/main/java/com/linkedin/data/schema/annotation/DataSchemaRichContextTraverser.java) and resolve the annotation for fields in the schemas.


<center>
<b>Sequence Diagram</b><br><img src="{{ 'assets/images/AnnotationProcessor_UML_sequence_diagram.png' | relative_url }}"  />
</center>

The [DataSchemaRichContextTraverser](https://github.com/linkedin/rest.li/blob/master/data/src/main/java/com/linkedin/data/schema/annotation/DataSchemaRichContextTraverser.java) traverse the data schema and in turn calls an implementation of [SchemaVisitor](https://github.com/linkedin/rest.li/blob/master/data/src/main/java/com/linkedin/data/schema/annotation/SchemaVisitor.java). It is the `SchemaVisitor` that resolves the annotations for fields in the schemas, based on the context provided by the `DataSchemaRichContextTraverser`. This process would eventually produces a copy of the origianl schema to return, because the original schema should not be modified during traversal.



<center>
<b>Class Diagram</b><br><img src="{{ 'assets/images/AnnotationProcessor_UML_class_diagram.png' | relative_url }}"  />
</center>

If the overriding of annotations is specified using the [PathSpec](/rest.li/pathspec) syntax, the [PathSpecBasedSchemaAnnotationVisitor](https://github.com/linkedin/rest.li/blob/master/data/src/main/java/com/linkedin/data/schema/annotation/PathSpecBasedSchemaAnnotationVisitor.java) and [SchemaAnnotationHandler](https://github.com/linkedin/rest.li/blob/master/data/src/main/java/com/linkedin/data/schema/annotation/SchemaAnnotationHandler.java) class are the ones to use for such use case. If a user wants to customize this, one should either look for re-implementing the `SchemaVisitor`, or extending `PathSpecBasedSchemaAnnotationVisitor`, or simply implement [SchemaAnnotationHandler](https://github.com/linkedin/rest.li/blob/master/data/src/main/java/com/linkedin/data/schema/annotation/SchemaAnnotationHandler.java)

| Situation | Recommendation |
|----------|:-------------:|
| Annotation based on PathSpec based overriding|Create a SchemaAnnotationHandler of your own implementation
| Annotation based on PathSpec with custom traversal context handling |Override PathSpecBasedSchemaAnnotationVisitor and optionally create a SchemaAnnotationHandler of your own implementation
| Annotation not using PathSpec based overriding |Implement your own SchemaVisitor 
| Annotation needs custom way of traversal |Override DataSchemaRichContextTraverser