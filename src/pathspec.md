---
layout: guide
title: Rest.Li PathSpecs
permalink: /pathspec
excerpt: Rest.Li PathSpecs
---

# Rest.li PathSpecs
- [Rest.li PathSpecs](#restli-pathspecs)
  - [What is PathSpec](#what-is-pathspec)
  - [Applications](#applications)
    - [Specifying Projections](#specifying-projections)
    - [Request Validation](#request-validation)
  - [PathSpec Syntax in its string form](#pathspec-syntax-in-its-string-form)
    - [Primitive type fields](#primitive-type-fields)
    - [Record type fields](#record-type-fields)
    - [Map and Array type fields](#map-and-array-type-fields)
        - [Map type](#map-type)
        - [Array type](#array-type)
    - [Union and UnionArray, Alias and Alias in Unions](#union-and-unionarray-alias-and-alias-in-unions)
        - [Union](#union)
        - [UnionArray](#unionarray)
        - [Alias](#alias)
        - [Alias in Unions](#alias-in-unions)
    - [TypeRef and Fixed](#typeref-and-fixed)
        - [TypeRef](#typeref)
      - [Fixed](#fixed)
  - [PathSpec Syntax in its java binded class form](#pathspec-syntax-in-its-java-binded-class-form)
  - [More resources and examples](#more-resources-and-examples)

## What is PathSpec
PathSpec represents a path to a component within a complex data object within Rest.li framework. It generates uniform references in hierarchical [datamap](https://github.com/linkedin/rest.li/blob/master/data/src/main/java/com/linkedin/data/DataMap.java) components. It is an abstract path concept and has its specification. It currently has two concrete forms in Rest.li, but it is not language-specific.

1. The PathSpec path can be represented as a string, current example usages of its string form can be found in [Validation annotation](/rest.li/Validation-in-Rest_li#specifying-restli-validation-annotations), where the PathSpec string is used in annotation and the [Annotation Reader](https://github.com/linkedin/rest.li/blob/master/restli-server/src/main/java/com/linkedin/restli/internal/server/model/RestLiAnnotationReader.java#L185) would interpret it.

2. The PathSpec path also have a binded java class: [PathSpec.java](https://github.com/linkedin/rest.li/blob/master/data/src/main/java/com/linkedin/data/schema/PathSpec.java). This java class can be passed as arguments in Rest.li Framework. Its Java class form is now mainly used for [Projection in Rest.li framework](/rest.li/How-to-use-projections-in-Java). The PathSpec java class has convenient method to return the correct pathspec string by calling `toString()`, which returns the string form.

For example:
given a data schema
```pdl
record User {
  firstName: string
  birthday: optional Date
  isActive: boolean = true
  address: record Address {
    state: string
    zipcode: string
  }
}
```
both `/address/zipcode` and `new PathSpec("addressa", "zipcodeb")` are pathspecs referring to the inner `zipcode` field.
## Applications 
### Specifying Projections
PathSpec's java class binding can be used for projection. Users can get PathSpec object that represents the fields in data object. For example, it could be obtained from generated RecordTemplate subclasses using the .fields() method. 
For example:
```java
PathSpec pathSpec = Foo.fields().bar();
```

More concrete examples can be found [here in the wiki](/rest.li/How-to-use-projections-in-Java#getting-the-pathspec-of-a-field).

This capability is provided by Rest.li auto-generated code, as data object representation for data in Pegasus schema should extend [RecordTemplate.java](/rest.li/blob/master/data/src/main/java/com/linkedin/data/template/RecordTemplate.java), and will define a inner class called "fields", which extended PathSpec.java. By passing PathSpec object to the reqeust builder, the PathSpec is then used by [MaskCreator](https://github.com/linkedin/rest.li/blob/master/data-transform/src/main/java/com/linkedin/data/transform/filter/request/MaskCreator.java) to create a [MaskTree](https://github.com/linkedin/rest.li/blob/master/data-transform/src/main/java/com/linkedin/data/transform/filter/request/MaskTree.java). Thus, PathSpec can be used to control the projection behavior.

### Request Validation
PathSpec's string form is used for [Request Validation](/rest.li/Validation-in-Rest_li) in Rest.li Resource. A string path can be added in annotation such as "CreateOnly" and "ReadOnly". For example:
```java
@CreateOnly({"/id", "/EXIF"})
public class PhotoResource extends CollectionResourceTemplate<Long, Photo>
{
    // ...
}
```

This string is then parsed by [RestLiAnnotationReader](https://github.com/linkedin/rest.li/blob/master/restli-server/src/main/java/com/linkedin/restli/internal/server/model/RestLiAnnotationReader.java), and [DataSchemaUtil](https://github.com/linkedin/rest.li/blob/master/data/src/main/java/com/linkedin/data/schema/DataSchemaUtil.java) will do corresponding validation against this path within the DataMap.

You can also invoke [RestliDataValidator](https://github.com/linkedin/rest.li/blob/master/restli-common/src/main/java/com/linkedin/restli/common/validation/RestLiDataValidator.java) and pass PathSpec string to it. [TestRestLiValidation.java](https://github.com/linkedin/rest.li/blob/master/restli-int-test/src/test/java/com/linkedin/restli/examples/TestRestLiValidation.java)'s testCustomValidatorMap() is such an example
```java
    ...

    Map<String, List<String>> annotations = new HashMap<>();
    annotations.put("createOnly", Arrays.asList("stringB", 
        "intB", "UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo2", "MapWithTyperefs/*/id"));
    annotations.put("readOnly", Arrays.asList("stringA", 
        "intA", "UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo1", "ArrayWithInlineRecord/*/bar1", "validationDemoNext/stringB", "validationDemoNext/UnionFieldWithInlineRecord"));
    ...

    validator = new RestLiDataValidator(annotations, ValidationDemo.class, ResourceMethod.CREATE, validatorClassMap);

```

## PathSpec Syntax in its string form

Each PathSpec has a corresponding string form. PathSpec has been defined as a path to a component within a complex data object path. This provides a way to traverse the Pegasus data object. The abstract data object, in most case, is a form of [DataMap](https://github.com/linkedin/rest.li/blob/master/data/src/main/java/com/linkedin/data/DataMap.java) internally in Rest.li framework, but PathSpec should be meaningful for the same data object in other forms, for example, it could provide path reference to a json representing the data object.

The PathSpec string format is represented by separators(`'/'`) and segments in between.
```
/demoRecord/innerRecordField/nestedInnerRecordField
```

The path segment could use attribute syntax to carry some meaningful attributes. These attributes added to the string form using '?' and '&' separators. Users can add any attributes but for some types, there are reserved attributes. For example, for array type, one can specify `start` and `count` attributes and these two attributes are used in [specifying projections](/rest.li/Projections#array).
```
/arrayOfIntFieldE?start=0&count=10
```

For collection types, such as maps and arrays, the path segment could also be replaced by the wildcard. A wildcard means that this segment path string can be replaced by any applicable segment string, for example.
```
/mapOfRecordField/*/innerRecordField
```
Above examples points to the `innerRecordField` field of the `map` value in a `map` schema. `map` is a collection schema type, here `*` wildcarded its keys.

Pegasus schema has defined various kinds of types, the full specification about the supported types can be found from the document [Rest.li Data Schema And Templates](/rest.li/DATA-Data-Schema-and-Templates). There are mainly following supported types in Pegasus and following sections list the example pathspecs.

### Primitive type fields
Primitive types includes type such as bytes, string, boolean, double, float, long, int. In the reocrd form, they came with a name to the field in record, so the reference to the primitive types, in most cases are just a PathSpec string which specify the field name of this type. 

For example for a Pegasus pdl schema file as such
```pdl
namespace com.linkedin.pegasus.examples

/**
 * example Pegasus schema of a record containing primitive types
 */
record RecordTest {
  intField: int
  intOptionalField: optional int
  intDefaultField: int = 17
  intDefaultOptionalField: optional int = 42
  longField: long
  floatField: float
  doubleField: double
  booleanField: boolean
  stringField: string
  bytesField: bytes
}
```
The example PathSpec for above fields in this record example would be
```
/initField
/intOptionalField
/intDefaultField
/intDefaultOPtionalField
/longField
/floatField
/doubleField
/booleanField
/stringField
/bytesField
```

### Record type fields
If a field in a record is of another record type, in this case  you have "nested field", then again the reference for the nested path component is the record's field name, 

For example the above PDL schema example now hava a record field,

```pdl
namespace com.linkedin.pegasus.examples

/**
 * example Pegasus schema of a record containing record field
 */
record RecordTest {
  recordField: RecordBar
}
```
And that record expands to
```pdl
namespace com.linkedin.pegasus.examples

record RecordBar {
  location: string
}
```

Then the PathSpec reference to the nested field "location" is
```
/recordField/location
```
### Map and Array type fields
Map and Array are two collection type used in Pagasus. 

##### Map type
The map has a key followed by the value referenced. Recall PathSpec's semantic is a reference to a component within a complex data object. The map entries were referenced by their keys. 

For example, here is a example PathSpec that can be used to traverse the map.

```
/mapField/<AKeyInMap>
```
Note that the above PathSpec is referring to the component key-ed by the key in the map. If the value is of a primitive type, the above PathSpec can be implicitly used to refer to the primitive type value.

If you want to specifically refer to all the key fields, the syntax is to use the keyword "$key"
```
/mapField/$key
```

if the value held in the map is a record type and you want to refer to that record, the PathSpec could be

```
/mapField/<AKeyInMap>/<someRecordField>
```

Another common use case is that you want to use wildcard (represented by symbol "*" ) to "select all keys", if the interested path is to a field for all entries in this map, then it would be

```
/mapField/*/<recordField>
```

It is worth noting that in Pegasus, the map keys are always of string types.

Here is an concrete example of a record, containing a map field, and that map field's map has value of record type. The example also defines a "recordInlineMap" field for similar demonstration purpose
```pdl
namespace com.linkedin.pegasus.examples

/**
 * a record containing a map field, which holds values of record type
 */
record RecordWithMapFields {

  recordMap: map[string, RecordBar]

  recordInlineMap: map[string, record RecordInMap {
    f: int
  }]
}
```
The PathSpec to refer to the record field, and the inline map field, are
```
/recordMap/*/location
/recordInlineMap/*/f
```

##### Array type
Array type is somewhat similar to Map type in the sense that the length might not be definite. For example it could be defined as such
```pdl
namespace com.linkedin.pegasus.examples

/**
 * a record containing an array field, which holds values of record type
 */
record RecordWithArrayFields {

  recordArray: array[RecordBar]

  recordInlineArray: array[record RecordInArray {
    f: int
  }]
}
```
Then similarly if you are interested in some fields only, you can use wildCard to select all elements so you can refer to a field
```
/recordArray/*/location
/recordInlineArray/*/f
```

One big difference between Array and Map is that in Array, we support reference to the range. It is achieved by using the 'start' and 'count' attributes.

Say you want to select some elements in within a range in the array, you can do so by using the "start" and "count" attribute, 
```
/intArray?start=10&count=5
/recordInlineArray?count=2
```


It worth noting that single element indexing is currently not defined yet. For example indexing the first element of an array is currently not defined. Alternatively this can be achieved by using the range PathSpec.
```
/recordArray/0 # This sytanx is not defined yet
/recordArray?start=0&count=1 # This syntax has been defined
```



### Union and UnionArray, Alias and Alias in Unions

##### Union
The use case for the PathSpec for union would be a path to one of the types within the union.

Here is an example of a record containa a union field, and that union is with a null type
```pdl
namespace com.linkedin.pegasus.examples

/**
 * a record that has a field contains a union with null type
 */
record UnionExample {

  unionWithNull: union[
    int,
    string,
    bytes,
    RecordBar,
    array[string],
    map[string, long],
    null
  ]
}
```

The PathSpecs for types within the uinos are
```
/unionWithNull/null
/unionWithNull/int
/unionWithNull/string
```

##### UnionArray
Union can be used as Array's item type. A common use case is a reference to all the same  type within an union.
```pdl
namespace com.linkedin.pegasus.examples

/**
 * a record that has a field contains a union array
 */
record UnionArrayExample {

  unionArray: array[union[
    null,
    int,
    string,
    map[string, string],
    array[int],
    RecordBar,
    FixedMD5
  ]]
}
```

```
/unionArray/*/null
/unionArray/*/int
/unionArray/*/string
/unionArray/*/RecordBar

```
In this example, the union also contains array and map, and the reference to them would be 
```
/unionArray/*/map
/unionArray/*/array
```
If the schema defines more than one array or more than one map in the union, they need to have defined alias for each (see Alias usage in next section).

##### Alias
Alias are used for refer to types (such as record type) that cannot be directly defined in the field due to same name conflict. Here is an example PDL with name "AliasTest" in "AliasTest.pdl"
```pdl
namespace com.linkedin.pegasus.examples

/**
 * Aliase examples
 */
@aliases = ["com.linkedin.pegasus.examples.AliasTest2"]
record AliasTest {
  a1: AliasTest
  a2: AliasTest
}
```
Here the AliasTest2 is an alias for another record with same name "AliasTest" in "AliasTest2.pdl"

Then here are the PathSpec can be used
```
/a1/a1
/a1/a2
/a1/a1/a2
```

##### Alias in Unions

It is worth mentioning that in most cases you will want to use alias in Union. For example in Union you can define two arrays with 
```pdl
namespace com.linkedin.pegasus.examples

record RecordWithAliasedUnion {

  result: union[
    message: string,

    successResults: array[string],

    failureResults: array[string]
  ]

  unionArray: array[union[
    null,

    successResults: array[string],

    failureResults: array[string]
  ]]
}
```
Then the PathSpec will be 
```
/result/successResults
/result/failureResults
/unionArray/*/successResults
/unionArray/*/failureResults
```

### TypeRef and Fixed
The reference to TypeRef and fixed are following a similar rule: Use its name in the field.

##### TypeRef
The "TypeRef" itself can be treated as just another type, so should use field name to refer to its path. 

For example
```pdl
namespace com.linkedin.pegasus.examples

record TyperefTest {

  intRefField: optional typeref IntRef = int
  intRefField2: IntRef

  bar1: typeref RecordBarRef = RecordBar
  bar2: RecordBarRef

  barRefMap: map[string, RecordBarRef]
}
```

The paths to the example integer fields here (used TypeRef) are 
```
/intRefField
/intRefField2
```
And for Record field reference within TypeRefed field
```
/bar1/location
/bar2/location
```
In the Map type example
```
/barRefMap/*/location
```


#### Fixed#
Fixed type can be defined in a separate file and then refered in another schema by name, for example in schema files:
```pdl
namespace com.linkedin.pegasus.example

fixed FixedMD5 16
```
Then `FixedMD5` can be used as a type. 
```pdl
namespace com.linkedin.pegasus.examples

/**
 * a record containing a map field, which holds values of union of fix type and inline fix type
 */
record RecordExampleForFixType {

  unionMap: map[string, union[fixed InlineFixedField 1, FixedMD5]]
}
```
As above example shows, when fixed type are defined inline, they will still have field name, so the PathSpec reference to it uses this name, therefore below are PathSpec to the types in the UnionMap defined in this record
```
/unionMap/*/InlineFixedField
/unionMap/*/FixedMD5
```

## PathSpec Syntax in its java binded class form
All auto-generated `RecordTemplate` class has a static nested class `fields` which extends `PathSpec`. To find out, after you build the Rest.li project, you could check such `RecordTemplate` classes in `GeneratedDataTemplate` folder and to find following codes.
```java
  public static class Fields extends PathSpec
  {
    ....
  }
```
[Check example code here](https://github.com/linkedin/rest.li/blob/master/generator-test/src/test/java/com/linkedin/pegasus/generator/test/pdl/fixtures/CustomRecord.java#L62). 
Therefore it is very easy to get the PathSpec java binded class. Let's say you have a `Foo` schema which has `bar` fields. You can get the PathSpec by following
```java
PathSpec pathSpec = Foo.fields().bar();
```
This has also been documented in [How to Use Projections in Java](/rest.li/How-to-use-projections-in-Java#getting-the-pathspec-of-a-field)


## More resources and examples
More example can be referred from our Rest.li Framework test code example. [TestPathSpec.java](https://github.com/linkedin/rest.li/blob/master/generator-test/src/test/java/com/linkedin/pegasus/generator/test/TestPathSpec.java) is a very uesful file that shows and tests what string should look like for the fields defined.
