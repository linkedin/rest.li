---
layout: guide
title: PDL Schema
permalink: /annotation_processor
excerpt: Documentation of using annotation processor in Rest.Li
---

# Annotation Processor
This sections introduces a generic schema annotation processing tool in Rest.Li. Before reading this, it is recommended to be familiar with the concept of PathSpec in Rest.Li. [Insert link here]

## Annotating Rest.Li Schemas
In Rest.Li, schema writers are able to add annotations to schema fields or the schema itself.

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
Note that these annotations are on the schema level.

The support that Rest.Li provided for processing annotation in Rest.Li schema has been quite limited. These annotations in above examples are later stored as a `DataSchema`'s class attribute called "property". Just as the example shows, both field and the DataSchema can have this "property". We let the user chooses where they want to have the schema annotated and how they want to interpret. There were no preferrabled way regarding how annotation should be written, read and interpreted.

## Inherit and override schema annotations
But Rest.Li users found it useful to process annotations during schema processing. One use case is to introduce "inheritance" and "overrides" to annotations so those annotations can be dynamically processed in the way user defines, when for example the schemas were reused. This gives annotation extensibility and adds to schema reusability.

Here are examples:
Example case 1: Users might want the annotation of a field be inherited. The fields can reuse the annotations from upper level.
```pdl
@persistencePolicyInDays = 30
record  UserVisitRecord {
  visitedUrls: array[URL]
  visitedUserProfiles: array[UserRecord]
}
```
Reading from the schema, we might find both `visitedUrls` and `visitedUserProfiles` have `persistencePolicyInDays` annotated as 30.
//<inheritance example>

Example case 2: Users might want the annotation of a field be overriden. Override is where the annotation on a field or a schema might get updated when other annotations assign it another value.
//<override example>
```pdl
@persistencePolicyInDays = 30
record  UserVisitRecord {
  @persistencePolicyInDays = 10
  visitedUrls: array[URL]
  visitedUserProfiles: array[UserRecord]
}
```
Reading from the schema, a user might find the field `visitedUserProfiles`s `persistencePolicyInDays` annotation is 30 but `visitedUrls`'s `persistencePolicyInDays` annotation has value 10, which overrides the original value inherited(which was 30).


Example case 3:  Override might also happen when some annotation needs to be updated by another value assigned from another annotation location
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
In this example, schema `EnterpriseUserRecord` reused `UserVisitRecord` and its annotation in the field `visitRecord`, but it overrides the annotation value of `recycledChatHistories`.


All above examples shows that inheritance and overrides gives more extensibility to the schema annotations. Users should be free to define their own rules regarding how those annotations can be read. There should be an imambigous annotation value for the fields after user's rules are applied. We call this processing step to figure out the eventual value of a field or schema's annotation "resolution" and we call such value "resolved" value.

What is more, users could have their own customized logic to process the annotations and define the custom behavior when annotations is overriden or inheritted, and even have the flexibility to set a customized resolved value by calling another local function or remote procedure.  The need for such extensibility gives the motivation for Annotation Processor. We aim to build a tool that can process annotations during schema resolution, process all interested annotations by plugin-ing user's own logic and save the "resolved" value back to corresponding DataSchema.

### A common application of overriding annotation using PathSpec
In the above `EnterpriseUserRecord` example, we use `@persistencePolicyInDays = {"/recycledChatHistories" : 3650}` do denote the field we are overriding is `recycledChatHistories`. This is basically using PathSpec to reference the field or child schema that needs to be overriden. PathSpec <insert-linke-here> can be used as a path to associate relations between fields among nested schemas. Users can use PathSpec to inambigously specifcy all the paths to the child fields that they want to override.


In Linkedin, such usage is so common that we want to use 
we provided `PathSpecBaseVisitor`. 

It assumes \\
(1) All *overrides* to the fields are specified using PathSpecs to the fields. \\
(2) All *overrides* are applied on fields, not on the record

For more examples regarding the syntax, one can read the java doc from `PathSpecBaseVisitor`.//<insert Link about > and the test cases //<insert Link about >


One can seek to extend this case to adapt to their own use cases. Please see next section regarding what class to extend to fit the best use cases.

## Use the schema annotation processor
We have created a paradigm of processing Rest.Li schema annotations and wrapped them into `com.linkedin.data.schema.annotation` package.
First thing to understand is that `DataSchema` object internally is stored recursively. We added `resolvedProperties` attribute, in order to store the final reesolved annotation.

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

The SchemaAnnotationProcessor will process `DataSchema` using `DataSchemaRichContextTraverser` and resolve the annotation for fields in the schemas.


<center>
<b>Sequence Diagram</b><br><img src="{{ 'assets/images/AnnotationProcessor_UML_sequence_diagram.png' | relative_url }}"  />
</center>

The `DataSchemaRichContextTraverser` travsers the data schema and in turn calls an implementation of `SchemaVisitor`. It is the `SchemaVisitor`' that resolves the annotations for fields in the schemas, based on the context provided by the `DataSchemaRichContextTraverser`. `SchemaVisitor` may also create copy of original data schema if the oroginal data schema needs to be immutable.

If the overriding of annotations are specified using the `PathSpec` syntax, the `PathSpecBaseVisitor` and `SchemaHandler` class are implemented for such use case. If a user want to implement their own logic, one should look for reimplementing the `SchemaVisitor`, or extending `PathSpecBaseVisitor`, or simply implement `SchemaHandler`

<center>
<b>Class Diagram</b><br><img src="{{ 'assets/images/AnnotationProcessor_UML_class_diagram.png' | relative_url }}"  />
</center>




For more, please ook at the test cases. <insert-me-the-link>