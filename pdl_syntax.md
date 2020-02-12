---
layout: guide
title: PDL Syntax
permalink: /pdl_syntax
excerpt: Rest.li PDL Syntax.
---

# PDL Syntax

## Contents
-   [PDL Schema Definition](#pdl-schema-definition)
-   [Creating a Schema](#creating-a-schema)
-   [Record Type](#record-type)
-   [Primitive Types](#primitive-type)
-   [Array Type](#array-type)
-   [Map Type](#map-type)
-   [Union Type](#union-type)
-   [Enum Type](#enum-type)
-   [Fixed Type](#fixed-type)
-   [Typerefs](#typerefs)
-   [Namespace](#namespace)
-   [Import](#import)
-   [Properties](#properties)
-   [Deprecation](#deprecation)
-   [Package](#package)

## PDL Schema Definition

PDL is a Pegasus schema definition language, which is developer friendly and more concise than the JSON based schema format.

## Creating a Schema

Each schema should be stored in its own file with a `.pdl` extension.
The Pegasus code generator implements a resolver that is similar to Java
class loaders. If there is a reference to a named schema, the code
generator will try to look for a file in the code generator’s resolver
path. The resolver path is similar to a Java classpath. The fully
qualified name of the named schema will be translated to a relative file
name. The relative file name is computed by replacing dots (“.”) in the
fully qualified name by the directory path separator (typically “/”) and
appending a `.pdl` extension. This relative file name is appended to
each path in the resolver path. The resolver opens each of these files
until it finds a file that contains the named schema.

Pegasus supports different types of schemas: [Records](#record-type), [Primitive types](#primitive-type), [Enums](#enum-type), [Arrays](#array-type), [Maps](#map-type), [Unions](#union-type), [Fixed](#fixed-type) and [Typerefs](#typerefs). 
Please check the following documentations for details.


## Record Type
Records contain any number of fields, which can be primitive types, enums, unions, maps arrays or other records.

A basic record type can contain a few fields.

For example:
```pdl
import org.example.time.DateTime

record Example {
  field1: string
  field3: DateTime
}
```
Record fields can be optional.

For example:
```pdl
namespace com.example.models

/**
 * A foo record
 */
record Foo {
   field1: string
   /**
    * field2 is an optional field.
    */
   field2: optional string
}
```

Record fields may have default values. The default value for a field is expressed as a JSON value.

For example:
```pdl
namespace com.example.models

/**
 * A foo record
 */
record Foo {
   field1: string
   field2: string = "message"
}
```

An optional field may have default value.

For example: 
```pdl
namespace com.example.models

record WithOptionalPrimitiveDefault {
  intWithDefault: optional int = 1
}
```

### Inline Records

A record can contain inline records.

For example:
```pdl
namespace com.example.models

record WithInlineRecord {
  inline: record InlineRecord {
    someField: int
  }
  inlineOptional: optional record InlineOptionalRecord {
    someField: string
  }
}
```

The default value of inlined records can be expressed using its serialized JSON representation.

For example: 
```pdl
namespace com.example.models

record WithInlineRecord {

    inline: record InlineRecord {
      someField: int
    } = { "someField": 1 }

    inlineOptional: optional record InlineOptionalRecord {
      someField: string
    } = { "someField": "default-value" }
}
```

Inline records can also be union members.

For example:
```pdl
namespace com.example.models

record UnionWithInlineRecord {
  value: union[

    record InlineRecord {
      value: optional int
    },

    record InlineRecord2 {}
  ]
}
```

### Doc Strings

Types and fields may be documented using “doc strings”.

For example: 
```pdl
/**
 * Doc strings may be added to types. This doc should describe the purposes
 * of the Example type.
 */
record Example {
   /**
    * Doc strings may also be added to fields.
    */
   field1: string

   // Non-doc string comments are treated as whitespace

   /** Doc strings can be single line.*/
   field2: int
}
```

**Note:**
If you use the Java comment style for doc strings, e.g "// Doc String", those doc strings will not be stored in the in-memory schema.

### Including fields

Records can include fields from one or more other records.

For example: 

```pdl
namespace com.example.models

/**
 * Bar includes fields of Foo, Bar will have fields b1 from itself and f1 from Foo
 */
record Bar includes Foo {
  b1: string
}
```

```pdl
namespace com.example.models

record Foo {
  f1: string
}
```

Multiple records can be included at once using an `includes` statement:

```pdl
namespace com.example.models

/**
 * Bar includes fields of Foo and Simple, Bar will have fields b1 from itself, f1 from Foo and s1 from Simple
 */
record Bar includes Foo, Simple {
  b1: string
}
```

```pdl
namespace com.example.models

record Simple {
  s1: string
}
```

In pegasus, field inclusion does not imply inheritance, it is merely a convenience to reduce duplication when writing schemas.


### Escaping
There are some keywords which are reserved in Pegasus. If you have to use them to define any names, you need to put them in backticks: ` `.

#### Keyword Escaping

```pdl
namespace com.example.models

record PdlKeywordEscaping {
  `namespace`: string
  `record`: string
  `null`: string
  `enum`: string
  recordName: record `record` { }
}
```

#### Namespace/Package escaping

Reserved keywords also need to be escaped when used in namespace declarations, package declarations, and import statements.

```pdl
namespace com.example.models.`record`
package com.example.models.`typeref`

import com.example.models.`optional`

record NamespacePackageEscaping { }
```

#### Property key escaping 

If you want Pegasus to treat property key name with dots as one string key, please use backticks to escape such string. Escaping property keys can also be used to escape reserved keywords such as "namespace" or "record".

For example: 

```pdl
namespace com.example.models

record PropertyKeyEscaping {
  @`namespace` = "foo.bar"
  @`test.path` = 1
  @validate.`com.linkedin.CustomValidator` = "foo"
  aField: string
}
```


## Primitive Types

The Pegasus primitive types are: int, long, float, double, boolean, string and bytes.

For Example:
```pdl
namespace com.example.models

record WithPrimitives {
  intField: int
  longField: long
  floatField: float
  doubleField: double
  booleanField: boolean
  stringField: string
  bytesField: bytes
}
```

Primitive types with default values:
```pdl
namespace com.example.models

record WithPrimitiveDefaults {
  intWithDefault: int = 1
  longWithDefault: long = 3000000000
  floatWithDefault: float = 3.3
  doubleWithDefault: double = 4.4E38
  booleanWithDefault: boolean = true
  stringWithDefault: string = "DEFAULT"
  bytesWithDefault: bytes = "\u0007"
}
```


## Array Type

Pegasus Arrays are defined as a collection of a particular "items" type.

For Example: 
```pdl
namespace com.example.models

record WithPrimitivesArray {
  ints: array[int]
  longs: array[long]
  floats: array[float]
  doubles: array[double]
  booleans: array[boolean]
  strings: array[string]
  bytes: array[bytes]
}
```

Primitive arrays with default values: 
```pdl
namespace com.example.models

record WithPrimitivesArrayDefaults {
  ints: array[int] = [1, 2, 3]
  longs: array[long] = [3000000000, 4000000000]
  floats: array[float] = [3.3, 2.5]
  doubles: array[double] = [4.4E38, 3.1E24]
  booleans: array[boolean] = [true, false]
  strings: array[string] = ["hello"]
  bytes: array[bytes] = ["\u0007"]
}
```

Record or Enum arrays:
```pdl
namespace com.example.models

import com.example.models.enums.Fruits
import com.example.models.records.Empty

record WithRecordAndEnumArrays {
  empties: array[Empty]
  fruits: array[Fruits]
}
```

Record or Enum arrays with default values: 
```pdl
namespace com.example.models

import com.example.models.enums.Fruits
import com.example.models.records.Simple

record WithRecordAndEnumDefaults {
  empties: array[Simple] = [{ "message": "defaults!" }]
  fruits: array[Fruits] = ["APPLE", "ORANGE"]
}
```

Types can be declared inline within an array definition:

```pdl
namespace com.example.models

record InlineWithinArray {
  vegetables: array[enum Vegetables { TOMATO, CARROT, CABBAGE }]
}
```


## Map Type

Maps are defined with a key type and a value type. The value type can be any valid PDL type, but currently `string` is the only supported key type.

For example: 
```pdl
namespace com.example.models

record WithPrimitivesMap {
  ints: map[string, int]
  longs: map[string, long]
  floats: map[string, float]
  doubles: map[string, double]
  booleans: map[string, boolean]
  strings: map[string, string]
  bytes: map[string, bytes]
}
```

Primitive maps with default values: 
```pdl
namespace com.example.models

record WithPrimitivesMapDefaults {
  ints: map[string, int] = { "int1": 1, "int2": 2, "int3": 3 }
  longs: map[string, long] = { "long1": 3000000000, "long2": 4000000000 }
  floats: map[string, float] = { "float1": 3.3, "float2": 2.1 }
  doubles: map[string, double] = {"double1": 4.4E38, "double2": 3.1E24}
  booleans: map[string, boolean] = { "boolean1": true, "boolean2": true, "boolean3": false }
  strings: map[string, string] = { "string1": "hello", "string2": "world" }
  bytes: map[string, bytes] = { "bytes": "\u0007" }
}
```

Complex types map, demonstrating how types can be declared inline within a map definition:
```pdl
namespace com.example.models

import com.example.models.enums.Fruits
import com.example.models.records.Empty
import com.example.models.records.Simple
import com.example.models.`fixed`.Fixed8

record WithComplexTypesMap {
  empties: map[string, Empty]
  fruits: map[string, Fruits]
  arrays: map[string, array[Simple]]
  maps: map[string, map[string, Simple]]
  unions: map[
    string,
    typeref WithComplexTypesMapUnion = union[int, string, Simple]
  ]
  `fixed`: map[string, Fixed8]
}
```

## Union Type

A union type may be defined with any number of member types. Member type can be primitive, record, enum, map or array. Unions are not allowed as members inside an union.

For example:
```pdl
namespace com.example.models

record WithPrimitivesUnion {
  value: union[int, long, float, double, boolean, string, bytes]
}
```

The member type names also serve as the “member keys” (also called as “union tags”), and identify which union member type data holds.
To define a field of a record containing a union of two other records, we would define:

```pdl
namespace com.example.models

import com.example.models.records.MultipleChoice
import com.example.models.records.TextEntry

record Question {
  answerFormat: union[MultipleChoice, TextEntry]
}
```

Union with default value:

```pdl
namespace com.example.models

import com.example.models.records.MultipleChoice
import com.example.models.records.TextEntry

record Question {
  answerFormat: union[MultipleChoice, TextEntry] = {"com.linkedin.pegasus.generator.examples.MultipleChoice": {"answer": "A"}}
}
```

Types can be declared inline within a union definition:

```pdl
namespace com.example.models

record InlineWithinUnion {
  produce: union[enum Fruits { APPLE, ORANGE }, enum Vegetables { TOMATO, CARROT }]
}
```

### Union with aliases
***Note:*** _Union with aliases is a recent feature in the pegasus schema language and it might not be fully supported in non-java languages. Please check the [support level](/rest.li/multi_language_compatibility_matrix) on all languages you intend to use before using aliases_

Union members can optionally be given an alias. Aliases can be used to create unions with members of the same type or to give better naming for union members. Union with aliases is useful for cases where a union contains multiple different typerefs with the same underlying type. This use case is not supported with a non-aliased union.

Aliased unions are defined as:
```pdl
union [alias: type, /* ... */]
```

For example:
```pdl
namespace com.example.models

record Question {
 answerFormat: union[   
   multipleChoice: MultipleChoice,

   /**
    * Doc for shortAnswer.
    */
   shortAnswer: string,

   @customProperty = "property for longAnswer."
   longAnswer: string
 ]
}
```

**Aliased union members can have doc strings and custom properties. This is not supported for non-aliased union members.**


Union with aliases with default value:
```pdl
namespace com.example.models

import com.example.models.records.MultipleChoice

record QuestionDefault {
 answerFormat: union[ 
   shortAnswer: string,
   longAnswer: string,
   multipleChoice: MultipleChoice
 ] = { "shortAnswer": "short answer." }
}
```

In the above example, the union answerFormat has three members, with two string type members differentiated using the aliases (shortAnswer and longAnswer). When aliases are used, the alias becomes the "member key" for the union members and will be used in the wire format. 

**Note:**
Either ALL union members must be aliased, or NONE at all.

## Enum Type

Enums types may contain any number of symbols. 

For example:

```pdl
namespace com.example.models

enum Fruits {
  APPLE
  BANANA
  ORANGE
  PINEAPPLE
}
```

Enums can be referenced by name in other schemas.

For example:

```pdl
namespace com.example.models

record FruitBasket {
  fruit: Fruits
}
```

Enums can also be defined inline.

For example:

```pdl
namespace com.example.models

record FruitBasket {
  fruit: enum Fruits { APPLE, BANANA, ORANGE }
} 
```

### Enum documentation, deprecation, and properties

Doc comments, deprecation, and properties can be added directly to individual enum symbols. 

For example:

```pdl
namespace com.example.models

/**
 * A fruit
 */
enum Fruits {

  @color = "red"
  APPLE

  /**
   * A yummy fruit.
   */
  @color = "yellow"
  BANANA

  @deprecated
  @color = "orange"
  ORANGE
}
```

### Enum defaults

To specify the default value for an enum field, use a string.

For example:

```pdl
namespace com.example.models

record FruitBasket {
  fruit: enum Fruits { APPLE, BANANA, ORANGE } = "APPLE"
} 
```

The default value can also be defined as in the following example:

```pdl
namespace com.example.models

record FruitBasket {
  fruit: Fruits = "APPLE"
} 
```

## Fixed Type

The Fixed type is used to define schemas with a fixed size in terms of number of bytes. 

For example:
```pdl
namespace com.example.models

fixed MD5 16
```
In the example above, `16` is the defined size for the `MD5` schema.

## Typerefs

Pegasus supports a new schema type known as a typeref. A typeref is like
a typedef in C. It does not declare a new type but rather declares an alias to
an existing type.

### Typerefs can be used to name anonymous types.

It is very useful, because unions, maps, and arrays cannot be named directly like records and enums.

For example:

```pdl
namespace com.example.models

typeref AnswerTypes = union[MultipleChoice, TextEntry]

```

Typerefs can be referred to by name from any other type.

For example:

```pdl
namespace com.example.models

record Question {
  answerFormat: AnswerTypes
}
```

### Typerefs can provide additional clarity when using primitive types.

For example:

```pdl
namespace com.example.models

typeref UnixTimestamp = long
```

### Typerefs can be used to specify custom types and coercers

For example, Joda time has a convenient `DateTime` Java class. If we wish to use this class in Java to represent date-times, all we need to do is define a pegasus custom type that binds to it:

```pdl
namespace com.example.models

@java.class = "org.joda.time.DateTime"
@java.coercerClass = "com.linkedin.example.DateTimeCoercer"
typeref DateTime = string
```
The coercer is responsible for converting the pegasus “referenced” type, in this case "string" to the Joda `DateTime` class:

Once a custom type is defined, it can be used in any type. 

For example, to use the `DateTime` custom type in a record:

```pdl
namespace com.example.models

record Fortune {
  createdAt: DateTime
} 
```

## Namespace

Namespace is used to qualify the namespace for the named schema.

For example:
```pdl
namespace com.example.models

record Foo {}
```

## Import

Imports are optional statements which allow you to avoid writing fully-qualified names.
They function similarly to imports in Java.

For example, the following record can be express _without_ imports like this:

```pdl
namespace com.example.models

record ImportsExample {
  x: com.example.models.records.Simple,
  y: com.example.models.records.Simple,
  z: array[com.example.models.records.Simple]
}
```

Alternatively, the record can be expressed using imports, minimizing the need for repetitive code:

```pdl
namespace com.example.models

import com.example.models.records.Simple

record ImportsExample {
  x: Simple,
  y: Simple,
  z: array[Simple]
}
```

**Note:** 
- Any type that is not imported and is not within the namespace from which it's referenced must be referenced by
fully qualified name.
- Using imports in the following ways will lead to PDL parser errors. You should avoid to do so.
1. Importing types declared inside the document.
2. Importing types within the root namespace of the document.
3. Declaring types that conflict with existing imports.

## Properties
Properties can be used to present arbitrary data and added to records, record fields, enums, enum symbols, aliased union members.

Add properties to record and record field:
```pdl
@prop = "value"
record Fruits {
  @validate.regex.regex = "^(yes|no)$"
  field: string
}
```

Add properties to enum and enum symbols:
```pdl
@prop = "value"
enum Fruits {
  @color = "red"
  APPLE

  @color = "orange"
  ORANGE

  @color = "yellow"
  BANANA
}
```

Add properties to aliased union members:
```pdl
record Question {
 answerFormat: union[
   @prop
   multipleChoice: MultipleChoice,
   shortAnswer: string,
   longAnswer: string
 ]
}
```

### Property values can be any valid JSON type:

For example:
```pdl
@prop = 1
```
```pdl
@prop = "string"
```
```pdl
@prop = [1, 2, 3]
```
```pdl
@prop = { "a": 1, "b": { "c": true }}

```
### Property values can also be empty:

If you don't indicate an explicit property value, it will result in an implicit value of `true`.

For example:
```pdl
@prop
```
### Property keys can be expressed as JSON:

For example:
```pdl
@a = {
  "b": {
    "c": {
      "d": {
        "e": {
          "f": false
        }
      }
    }
  }
}
```

### Property keys can be expressed as paths:

The JSON style property key is complicated to write and read, so we provide a shorthand - the dot separate format to express the property keys.

The following example is equivalent to the previous JSON example:

```pdl
@a.b.c.d.e.f = false
```

## Deprecation

All types, enum symbols and record fields can be deprecated by adding `@deprecated` annotation.
The property value can be a string describing why the schema element is deprecated or an alternative, or simply boolean `true`.

Deprecate record and field:

```pdl
namespace com.example.models

@deprecated = "Use record X instead."
record Example {
  @deprecated = "Use field x instead."
  field: string
}
```

Deprecate enum symbols:
```pdl
namespace com.example.models

enum Fruits {

  APPLE
  
  @deprecated = "Use APPLE instead."
  BANANA

  @deprecated
  ORANGE
}
```

## Package

Package is used to qualify the language binding namespace for the named schema. 

For example:
```pdl
namespace com.linkedin.pegasus.generator.examples
package com.linkedin.pegasus.generator.tests

record Foo {
  intField: int
}
```
If package is not specified, language binding class for the named schema will use `namespace` as its default namespace.
