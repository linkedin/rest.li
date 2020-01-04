---
layout: guide
title: PDL Syntax
permalink: /pdl_syntax
excerpt: Rest.li PDL Syntax.
---

# PDL Syntax

## Contents
-   [PDL Schema Definition](#pdl__schema_definition)
-   [Creating a Schema](#creating_a_schema)
-   [Record Type](#record_type)
-   [Primitive Types](#primitive_type)
-   [Array Type](#array_type)
-   [Union Type](#union_type)
-   [Enum Type](#enum_type)
-   [Typerefs](#typerefs)

## PDL Schema Definition

PDL is a Pegasus schema definition language, which is developer friendly and less verbose compare to JSON based schema format.

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

The named schema declarations support the following attributes: 

  - `type` provides the type of the named schema
    (required).
  - `name` provides the name of the named schema
    (required).
  - `namespace` qualifies the namespace for the named
    schema.
  - `package` qualifies the language binding
    namespace for the named schema (optional). If this is not specified,
    language  bindingclass for the named schema will use `namespace` as
    its default namespace.
  - `doc` provides documentation to the user of this
    named schema (optional).
  - `import` qualifies the name of object which is used in the schema.(optional).

The named schemas with type “enum” also supports a `doc`
attribute to provide documentation for each enum symbol.

**Note:** Due to the addition of doclint in JDK8, anything under the
`doc`  attribute must be W3C HTML 4.01 compliant. This is
because the contents of this string will appear as Javadocs in the
generated Java ‘data template’ classes later. Please take this into
consideration when writing your documentation.

The following are a few example schemas and their file names.


com/linkedin/pegasus/generator/examples/Foo.pdl

```
namespace com.linkedin.pegasus.generator.examples

/**
 * A foo record
 */
record Foo {
  intField: int
  longField: long
  floatField: float
  doubleField: double
  bytesField: bytes
  stringField: string
  intArrayField: array[int]
  stringMapField: map[string, string]
  unionField: union[int, string, 
    /**
     * A foo record
     */
    record Foo {
      intField: int
      longField: long
      floatField: float
      doubleField: double
      bytesField: bytes
      stringField: string
      intArrayField: array[int]
      stringMapField: map[string, string]
      unionField: union[int, string, Foo, array[string], map[string, long]]
    }, array[string], map[string, long]]
}
```

com/linkedin/pegasus/generator/examples/FooWithNamespaceOverride.pdl

```
namespace com.linkedin.pegasus.generator.examples
package com.linkedin.pegasus.generator.examples.`record`

/**
 * A foo record
 */
record FooWithNamespaceOverride {
  intField: int
  longField: long
  floatField: float
  doubleField: double
  bytesField: bytes
  stringField: string
  fruitsField: Fruits
  intArrayField: array[int]
  stringMapField: map[string, string]
  unionField: union[int, string, Fruits, array[string], map[string, long]]
}
```

com/linkedin/pegasus/generator/examples/Fruits.pdl

```
namespace com.linkedin.pegasus.generator.examples

/**
 * A fruit
 */
enum Fruits {

  /**
   * A red, yellow or green fruit.
   */
  APPLE

  /**
   * A yellow fruit.
   */
  BANANA

  /**
   * An orange fruit.
   */
  ORANGE

  /**
   * A yellow fruit.
   */
  PINEAPPLE
}
```

com/linkedin/pegasus/generator/examples/MD5.pdl

```
namespace com.linkedin.pegasus.generator.examples

/**
 * MD5
 */
fixed MD5 16
```

com/linkedin/pegasus/generator/examples/StringList.pdl

```
namespace com.linkedin.pegasus.generator.examples

/**
 * A list of strings
 */
record StringList {
  element: string
  next: optional 

    /**
     * A list of strings
     */
    record StringList {
      element: string
      next: optional StringList
    }
}
```

com/linkedin/pegasus/generator/examples/InlinedExample.pdl

```
namespace com.linkedin.pegasus.generator.examples

/**
 * Example on how you can declare an enum and a record inside another record
 */
record InlinedExample {

  /**
   * This is how we inline enum declaration without creating a new pdl file
   */
  @symbolDocs.FOO = "It's a foo!"
  @symbolDocs.NONE = "It's a baz!"
  @symbolDocs.HASH = "It's a bar!"
  myEnumField: enum EnumDeclarationInTheSameFile {
    FOO
    BAR
    BAZ
  }

  /**
   * A regular string
   */
  stringField: string

  /**
   * A regular int
   */
  intField: int

  /**
   * In this example we will declare a record and an enum inside a union
   */
  UnionFieldWithInlineRecordAndEnum: optional union[record myRecord {

    /**
     * random int field
     */
    foo1: int

    /**
     * random int field
     */
    foo2: int
  }, 
    /**
     * Random enum
     */
    enum anotherEnum {

      /**
       * description about FOOFOO
       */
      FOOFOO

      /**
       * description about BARBAR
       */
      BARBAR
    }]
}
```

## Record Type
Records contain any number of fields, which can be primitive type, enums, unions, maps and arrays.

A basic record type can contain a few fields.

For example:
```
import org.example.time.DateTime

record Example {
  field1: string
  field2: optional int
  field3: DateTime
}
```
Record fields can be optional.

For example :
```
namespace com.linkedin.pegasus.generator.examples

/**
 * A foo record
 */
record Foo {
   field1: string
   field2: optional string
}
```

Record fields may have default values.

For example :
```
namespace com.linkedin.pegasus.generator.examples

/**
 * A foo record
 */
record Foo {
   field1: string
   field2: string = "message"
}
```

A optional field may have default value.

For example : 
```
namespace com.linkedin.pegasus.generator.test.idl.records

record WithOptionalPrimitiveDefault {
  intWithDefault: optional int = 1
}
```

### Doc Strings

Types and fields may be documented using “doc strings”.

For example : 
```
/**
 * Doc strings may be added to types. This doc should describe the purposes
 * of the Example type.
 */
record Example {
  /**
   * Doc strings may also be added to fields.
   */
   field1: string

   /** Doc strings can be single line.*/
   field2: int
}
```

**Note:**
If you use Java comment style for doc string, e.g "// Doc String", those doc strings will not be stored in in-memory schema.


### Import

1. If the type is outside the root namespace of the document, you need to add it as an import.

2. If the type is declared outside the document, you need to add it as an import.

For example :
```
namespace com.linkedin.pegasus.generator.test.idl.imports

import com.linkedin.pegasus.generator.test.idl.records.Simple

record Example {
  /**
   * Requires an import since this type is outside the root namespace and is not declared in this file.
   */
  externalOutsideNS: Simple
}
```

**Note:** 
- When multiple referenced types with the same unqualified name may be imported, 
the type with the alphabetically first namespace is chosen. 
(e.g. "com.a.b.c.Foo" is chosen over "com.x.y.z.Foo") 

- Any type that is not imported and is not within the namespace from which it's referenced must be referenced by
fully qualified name.


### Deprecation
Types and fields can be deprecated by adding @deprecated annotation.

For example :

```
@deprecated = "Use record X instead."
record Example {
  @deprecated = "Use field x instead."
  field: string
}
```

### Including fields

Record can include fields from other record.

For example : 

```
namespace com.linkedin.pegasus.generator.examples

/**
 * Bar includes fields of Foo, Bar will have fields f1 from itself and b1 from Bar
 */
record Bar includes Foo {
  b1: string
}
```
```
namespace com.linkedin.pegasus.generator.examples

record Foo {
  f1: string
}
```
In pegasus, field inclusion does not imply inheritance, it is merely a convenience to reduce duplication when writing schemas.


### Properties
Properties can be used to present arbitrary datas and added to records, fields and enums.

For example : 
```
@prop = "value"
record Fruits {
  @validate.regex.regex = "^(yes|no)$"
  field: string
}
```
#### Property values can be any valid JSON type:

For example : 
```
@prop = 1
```
```
@prop = "string"
```
```
@prop = [1, 2, 3]
```
```
@prop = { "a": 1", "b": { "c": true }}
```

#### Property keys can expressed as paths:

For example : 
```
@java.class = "org.joda.DateTime"
@java.coercerClass = "org.example.DateTimeCoercer"
```

The dot seperate keys format is equivalent to JSON value format.

The above dot seperate keys example is equivalent to the following JSON value example :
```
@java = {
  "class": "org.joda.DateTime",
  "coercerClass": "org.example.DateTimeCoercer"
}
```

### Escaping
There are some keywords which are reserved in Pegasus. If you have to use them to define any names, you need to put them in backticks : ` `.

#### Keyword Escaping

```
namespace com.linkedin.pegasus.generator.test.idl.escaping

record PdlKeywordEscaping {
  `namespace`: string
  `record`: string
  `null`: string
  `enum`: string
  recordName: record `record` { }
}
```

#### Namespace/Package escaping

```
namespace com.linkedin.pegasus.generator.test.idl.escaping.`record`
package com.linkedin.pegasus.generator.test.idl.escaping.override.`typeref`

/**
 * Ensures that the namespace and package are properly escaped at the root as well as in scoped named-type declarations.
 */
record NamespacePackageEscaping {
  x: {
    namespace com.x.y.z.`enum`
    package com.a.b.c.`fixed`

    record Foo {}
  }
}
```

#### Property key escaping 

If you want Pegasus to treat property key name with dots as one string key, please use backticks to escape such string.
For example : 

```
namespace com.linkedin.pegasus.generator.test.idl.escaping

record PropertyKeyEscaping {
  @`namespace` = "foo.bar"
  @`test.path` = 1
  @validate.`com.linkedin.CustomValidator` = "foo"
  aField: string
}
```


## Primitive Types

The Pegasus primitive types are : int, long, float, double, boolean, string and bytes.

For Example :
```
namespace com.linkedin.pegasus.generator.test.idl.records

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

## Array Type

Pegesus Arrays are defined within a items type.

For Example : 
```
namespace com.linkedin.pegasus.generator.test.idl.arrays

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

Record Arrays :

```
namespace com.linkedin.pegasus.generator.test.idl.arrays

import com.linkedin.pegasus.generator.test.idl.enums.Fruits
import com.linkedin.pegasus.generator.test.idl.records.Empty

record WithRecordArray {
  empties: array[Empty]
  fruits: array[Fruits]
}
```


## Map Type

Pegasus maps are defined with a values type and an optional key type.

For example : 
```
namespace com.linkedin.pegasus.generator.test.idl.maps

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
Custom Types Map :
```
namespace com.linkedin.pegasus.generator.test.idl.maps

import com.linkedin.pegasus.generator.test.idl.customtypes.CustomInt

record WithCustomTypesMap {
  ints: map[string, CustomInt]
}
```

Complex Types Map :
```
namespace com.linkedin.pegasus.generator.test.idl.maps

import com.linkedin.pegasus.generator.test.idl.enums.Fruits
import com.linkedin.pegasus.generator.test.idl.records.Empty
import com.linkedin.pegasus.generator.test.idl.records.Simple
import com.linkedin.pegasus.generator.test.idl.`fixed`.Fixed8

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

**Note**: 
The key must always be "string".


## Union Type

A union type may be defined with any number of member types. Each member may be any pegasus type except union type. 
Each member can be primitive, record, enum map or array type.

For example :
```
namespace com.linkedin.pegasus.generator.test.idl.unions

record WithPrimitivesUnion {
  `union`: union[int, long, float, double, boolean, string, bytes]
}
```

The member type names also serve as the “member keys” (also called as “union tags”), and identify which union member type data holds.
To define a field of a record containing a union of two other records, we would define:
```
namespace com.linkedin.pegasus.generator.examples

record Question {
  answerFormat: union[MultipleChoice, TextEntry]
}
```

### Union with aliases
Union members can optionally be given an alias. Aliases can be used to create unions with members of the same type or to give better naming for union members.

Aliased unions would be defines as :
```
union [alias: type, ...]
```

For example :
```
namespace com.linkedin.pegasus.generator.examples

record Question {
 answerFormat: union[   
   multipleChoice: MultipleChoice,
   shortAnswer: string,
   longAnswer: string
 ]
}
```
In the above example, the union answerFormat has three members, with two string type members differentiated using the aliases (shortAnswer and longAnswer). When aliases are used, the alias becomes the "member key" for the union members and will be used in the wire format. 

## Enum Type

Enums types may contain any number of symbols. 

For example :

```
namespace com.linkedin.pegasus.generator.examples

enum Fruits {
  APPLE
  BANANA
  ORANGE
  PINEAPPLE
}
```

Enums can be referenced in other schemas by name.

For example :

```
namespace com.linkedin.pegasus.generator.examples

record FruitBasket {
  fruit: Fruits
}
```

Enums can also be referenced in other schemas by inlining their type definition. 

For example :

```
namespace com.linkedin.pegasus.generator.examples

record FruitBasket {
  fruit: enum Fruits { APPLE, BANANA, ORANGE }
} 
```

### Enum documentation, deprecation and properties
Doc comments, @deprecation and properties can be added directly to enum symbols. 

For example :

```
namespace com.linkedin.pegasus.generator.examples

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
To specify defaults, specify the enum value as string.

For example :

```
namespace com.linkedin.pegasus.generator.examples

record FruitBasket {
  fruit: enum Fruits { APPLE, BANANA, ORANGE } = "APPLE"
} 
```


The default value can also be defined as following exmaple:

```
namespace com.linkedin.pegasus.generator.examples

record FruitBasket {
  fruit: Fruits = "APPLE"
} 
```

## Typerefs
Pegasus supports a new schema type known as a typeref. A typeref is like
a typedef in C. It does not declare a new type but declares an alias to
an existing type.

  - Typerefs are useful for differentiating different uses of the same
    type. For example, we can use to a typeref to differentiate a string
    field that holds an URN (uniform resource name) from an arbitrary
    string value or a long field that holds an epoch time in
    milliseconds from a generic long value.
  - A typeref allows additional meta-data to be associated with
    primitive and unnamed types. This meta-data can be used to provide
    documentation or support custom properties.
  - A typeref provides a way to refer to common unnamed types such as
    arrays, maps, and unions. Without typerefs, users may have to wrap
    these unnamed types with a record in order to address them.
    Alternatively, users may cut-and-paste common type declarations,
    resulting in unnecessary duplication and potentially causing
    inconsistencies if future changes are not propagated correctly to
    all copies.


### Provide a name for a union, map, or array. So that it can be referenced by name。

It is very useful, because unions, maps and arrays cannot be named directly like records and enums.

For example :

```
namespace com.linkedin.pegasus.generator.examples

typeref AnswerTypes = union[MultipleChoice, TextEntry]

```

Typerefs can be referred to from any other type using the name。

For example:

```
namespace com.linkedin.pegasus.generator.examples

record Question {
  answerFormat: AnswerTypes
}
```
      

### Provide additional clarity when using primitive types for specific purposes.

For example :

```
namespace com.linkedin.pegasus.generator.examples

typeref UnixTimestamp = long
```

### Custom Types and Coercers
For example, Joda time has a convenient DateTime class. If we wish to use this class in Java to represent date times, all we need to do is define a pegasus custom type that binds to it:

```
namespace com.linkedin.pegasus.generator.examples

@java.class = "org.joda.time.DateTime"
@java.coercerClass = "com.linkedin.example.DateTimeCoercer"
typeref DateTime = string
```
The coercer is responsible for converting the pegasus “referenced” type, in this case "string" to the Joda DateTime class:

Once a custom type is defined, it can be used in any type. 

For example, to use the DateTime custom type in a record:

```
namespace com.linkedin.pegasus.generator.examples

record Fortune {
  createdAt: DateTime
} 
```