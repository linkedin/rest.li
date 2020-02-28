---
layout: guide
title: PDSC Syntax
permalink: /pdsc_syntax
excerpt: Rest.li PDSC Syntax.
---

# PDSC Syntax

## Contents

-   [PDSC Schema Definition](#pdsc__schema_definition)
-   [Creating a Schema](#creating_a_schema)
-   [Typeref](#typeref)
-   [Record Field Attributes](#record_field_attributes)
-   [Optional Fields](#optional_fields)
-   [Union](#union)
-   [Default Values](#default_values)
-   [Including Fields from Another Record](#including_fields_from_another_record)
-   [Deprecation](#deprecation)


## PDSC Schema Definition

PDSC is a Pegasus schema definition language, and it is inspired by the [Avro 1.4.1
specification](http://avro.apache.org/docs/1.4.1/spec.html).The Pegasus schema definition language is inspired by the [Avro 1.4.1
specification](http://avro.apache.org/docs/1.4.1/spec.html).

  - Pegasus leverages Avro’s JSON-based schema definition syntax, type
    system, and JSON serialization format.
  - Pegasus does not implement or use Avro’s binary data serialization
    format, object container files, protocol wire format, and schema
    resolution.
  - Pegasus does not require or re-use any Avro implementation.
  - The code generator supports all of Avro’s types: primitive types,
    records, enums, arrays, maps, unions, fixed.

The Pegasus schema differs from Avro in the following ways:

  - Pegasus uses “optional” to indicate that a field in a record is
    optional. Union with null should not be used to indicate an optional
    field. Future versions of Pegasus may remove the null type from the
    schema definition language.
  - Pegasus unions can have more than one member of the same type. This
    is supported by specifying an alias for each member which is unique
    within that union.
  - Pegasus default values for union types must include the member
    discriminator. Avro default values for union types do not include
    the member discriminator. The type of the Avro union default value
    is always the 1st member type of the union.
  - Pegasus implements a mechanism for including all the fields from
    another record into the current record.
  - Pegasus schema files have file names that ends with `.pdsc` instead
    of `.avsc`.
  - The Pegasus code generator also implements resolvers similar to Java
    class loaders that allow each named schema to be specified in a
    different source file.  
    (Note the Avro code generator requires all referenced named schemas
    to be in the same source file. The schema resolver of the Pegasus
    code generator is unrelated to the schema resolution feature of
    Avro.)
  - Pegasus ignores the aliases (but allows them in the schema
    definition). Future versions of Pegasus may remove aliases.
  - Pegasus schema resolution and compatibility rules are different.
  - Pegasus named schema provides option to override language binding
    namespace for generated data model from the schema. **Note that this
    binding namespace override will not change how data is transported
    over the wire.**

## Creating a Schema

Each schema should be stored in its own file with a `.pdsc` extension.
The Pegasus code generator implements a resolver that is similar to Java
class loaders. If there is a reference to a named schema, the code
generator will try to look for a file in the code generator’s resolver
path. The resolver path is similar to a Java classpath. The fully
qualified name of the named schema will be translated to a relative file
name. The relative file name is computed by replacing dots (“.”) in the
fully qualified name by the directory path separator (typically “/”) and
appending a `.pdsc` extension. This relative file name is appended to
each path in the resolver path. The resolver opens each of these files
until it finds a file that contains the named schema.

The named schema declarations support the following attributes:

  - `type` a JSON string providing the type of the named schema
    (required).
  - `name` a JSON string providing the name of the named schema
    (required).
  - `namespace` a JSON string that qualifies the namespace for the named
    schema.
  - `package` a JSON string that qualifies the language binding
    namespace for the named schema (optional). If this is not specified,
    language binding class for the named schema will use `namespace` as
    its default namespace.
  - `doc` a JSON string providing documentation to the user of this
    named schema (optional). 

The named schemas with type “enum” also supports a `symbolDocs`
attribute to provide documentation for each enum symbol.

**Note:** Due to the addition of doclint in JDK8, anything under the
`doc` or `symbolDocs` attribute must be W3C HTML 4.01 compliant. This is
because the contents of this string will appear as Javadocs in the
generated Java ‘data template’ classes later. Please take this into
consideration when writing your documentation.

The following are a few example schemas and their file names.

com/linkedin/pegasus/generator/examples/Foo.pdsc

```json
{
  "type" : "record",
  "name" : "Foo",
  "namespace" : "com.linkedin.pegasus.generator.examples",
  "doc" : "A foo record",
  "fields" : [
    { "name" : "intField",       "type" : "int" },
    { "name" : "longField",      "type" : "long" },
    { "name" : "floatField",     "type" : "float" },
    { "name" : "doubleField",    "type" : "double" },
    { "name" : "bytesField",     "type" : "bytes" },
    { "name" : "stringField",    "type" : "string" },
    { "name" : "fruitsField",    "type" : "Fruits" },
    { "name" : "intArrayField",  "type" : { "type" : "array", "items" : "int" } },
    { "name" : "stringMapField", "type" : { "type" : "map", "values" : "string" } },
    {
      "name" : "unionField",
      "type" : [
        "int",
        "string",
        "Fruits",
        "Foo",
        { "type" : "array", "items" : "string" },
        { "type" : "map", "values" : "long" },
        "null"
      ]
    }
  ]
}
```

com/linkedin/pegasus/generator/examples/FooWithNamespaceOverride.pdsc
(please see Java Binding section to see how this “package” affects
generated java class.)

```json
{
  "type" : "record",
  "name" : "FooWithNamespaceOverride",
  "namespace" : "com.linkedin.pegasus.generator.examples",
  "package" : "com.linkedin.pegasus.generator.examples.record",
  "doc" : "A foo record",
  "fields" : [
    { "name" : "intField",       "type" : "int" },
    { "name" : "longField",      "type" : "long" },
    { "name" : "floatField",     "type" : "float" },
    { "name" : "doubleField",    "type" : "double" },
    { "name" : "bytesField",     "type" : "bytes" },
    { "name" : "stringField",    "type" : "string" },
    { "name" : "fruitsField",    "type" : "Fruits" },
    { "name" : "intArrayField",  "type" : { "type" : "array", "items" : "int" } },
    { "name" : "stringMapField", "type" : { "type" : "map", "values" : "string" } },
    {
      "name" : "unionField",
      "type" : [
        "int",
        "string",
        "Fruits",
        "Foo",
        { "type" : "array", "items" : "string" },
        { "type" : "map", "values" : "long" },
        "null"
      ]
    }
  ]
}
```

com/linkedin/pegasus/generator/examples/Fruits.pdsc

```json
{
  "type" : "enum",
  "name" : "Fruits",
  "namespace" : "com.linkedin.pegasus.generator.examples",
  "doc" : "A fruit",
  "symbols" : [ "APPLE", "BANANA", "ORANGE", "PINEAPPLE" ],
  "symbolDocs" : { "APPLE":"A red, yellow or green fruit.", "BANANA":"A yellow fruit.", "ORANGE":"An orange fruit.", "PINEAPPLE":"A yellow fruit."} 
}
```

com/linkedin/pegasus/generator/examples/MD5.pdsc

```json
{
  "type" : "fixed",
  "name" : "MD5",
  "namespace" : "com.linkedin.pegasus.generator.examples",
  "doc" : "MD5",
  "size" : 16
}
```

com/linkedin/pegasus/generator/examples/StringList.pdsc

```json
{
  "type" : "record",
  "name" : "StringList",
  "namespace" : "com.linkedin.pegasus.generator.examples",
  "doc" : "A list of strings",
  "fields" : [
    { "name" : "element", "type" : "string" },
    { "name" : "next"   , "type" : "StringList", "optional" : true }
  ]

}
```

com/linkedin/pegasus/generator/examples/InlinedExample.pdsc

```json
{
  "type": "record",
  "name": "InlinedExample",
  "namespace": "com.linkedin.pegasus.generator.examples",
  "doc": "Example on how you can declare an enum and a record inside another record",
  "fields": [
    {
      "name": "myEnumField",
      "type": {
      "type" : "enum",
      "name" : "EnumDeclarationInTheSameFile",
      "symbols" : ["FOO", "BAR", "BAZ"]
      },
      "doc": "This is how we inline enum declaration without creating a new pdsc file",
      "symbolDocs": {"FOO":"It's a foo!", "HASH":"It's a bar!", "NONE":"It's a baz!"}
    },
    {
      "name": "stringField",
      "type": "string",
      "doc": "A regular string"
    },
    {
      "name": "intField",
      "type": "int",
      "doc": "A regular int"
    },
    {
      "name": "UnionFieldWithInlineRecordAndEnum",
      "doc": "In this example we will declare a record and an enum inside a union",
      "type": [
        {
          "type" : "record",
          "name" : "myRecord",
          "fields": [
            {
              "name": "foo1",
              "type": "int",
              "doc": "random int field"
            },
            {
              "name": "foo2",
              "type": "int",
              "doc": "random int field"
            }
          ]
        },
        {
          "name": "anotherEnum",
          "type" : "enum",
          "symbols" : ["FOOFOO", "BARBAR"],
          "doc": "Random enum",
          "symbolDocs": {"FOOFOO":"description about FOOFOO", "BARBAR":"description about BARBAR"}
        }
      ],
      "optional": true
    }
  ]
}
```

## Typeref

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

Typerefs use the type name `typeref` and support the following attributes:

  - `name` a JSON string providing the name of the typeref (required).
  - `namespace` a JSON string that qualifies the name;
  - `package` a JSON string that qualifies the language binding
    namespace for this typeref (optional). If this is not specified,
    language binding class for the typeref will use `namespace` as its
    default namespace.
  - `doc` a JSON string providing documentation to the user of this
    schema (optional).
  - `ref` the schema that the typeref refers to.

**Note:** Due to the addition of doclint in JDK8, anything under the
`doc` attribute must be W3C HTML 4.01 compliant. This is because the
contents of this string will appear as Javadocs in the generated Java
‘data template’ classes later. Please take this into consideration
when writing your documentation.

Here are a few examples:

Differentiate URN from string

```json
{
  "type" : "typeref",
  "name" : "URN",
  "ref"  : "string",
  "doc"  : "A URN, the format is defined by RFC 2141"
}
```

Differentiate time from long

```json
{
  "type" : "typeref",
  "name" : "time",
  "ref"  : "long",
  "doc"  : "Time in milliseconds since Jan 1, 1970 UTC"
}
```
  
Typerefs (by default) do not alter the serialization format or in-memory
representation.

## Record Field Attributes

You can use the following record field attributes to annotate fields
with metadata.

  - `optional` - marks the field as optional, meaning it doesn’t require
    a value. See [Optional Fields](#OptionalFields).

<a name="OptionalFields"></a>

## Optional Fields

Pegasus supports optional fields implicitly through the “optional” flag
in the field definition. A field is required unless the field is
declared with the optional flag set to true. A field without the
optional flag is required. A field with the optional flag set to false
is also required. An optional field may be present or absent in the
in-memory data structure or serialized data.

The Java binding provides methods to determine if an optional field is
present and a specialized get accessor allows the caller to specify
whether the default value or null should be returned when an absent
optional field is accessed. The has field accessor may also be used to
determine if an optional field is present.

```json
{
  "type" : "record",
  "name" : "Optional",
  "namespace" : "com.linkedin.pegasus.generator.examples",
  "fields" :
  [
    {
      "name" : "foo",
      "type" : "string",
      "optional" : true
    }
  ]
}
```

Optional field present

```json
{
  "foo" : "abcd"
}
```

Optional field absent

```json
{
}
```

See [GetMode](#GetMode) for more detailed information on how the code
generated data stubs access optional/default fields.

### Avro’s Approach to Declaring Optional Fields

DO NOT USE UNION WITH NULL TO DECLARE AN OPTIONAL FIELD IN PEGASUS.

Avro’s approach to declaring an optional field is to use a union type
whose values may be null or the desired value type. Pegasus discourages
this practice and may remove support for union with null as well as the
null type in the future. The reason for this is that declaring an
optional field using a union causes the optional field to be always
present in the underlying in-memory data structure and serialized data.
Absence of a value is represented by a null value. Presence of a value
is represented by a value in the union of the value’s type.

```json
{
  "type" : "record",
  "name" : "OptionalWithUnion",
  "namespace" : "com.linkedin.pegasus.generator.examples",
  "fields" :
  [
    {
      "name" : "foo",
      "type" : ["string", "null"]
    }
  ]
}
```

optional field present

```json
{
  "foo" : { "string" : "abcd" }
}
```

optional field absent

```json
{
  "foo" : null
}
```

Note Avro uses the union approach because Avro serialization is
optimized to not include a field identifier in the serialized data.

## Union

Union is a powerful way to model data that can be of different types at
different scenarios. Record fields that have this behavior can be
defined as a Union type with the expected value types as its members.
Here is an example of a record field defined an a union with `string`
and `array` as its members.

```json
{
  "type" : "record",
  "name" : "RecordWithUnion",
  "namespace" : "com.linkedin.pegasus.examples",
  "fields" :
  [
    {
      "name" : "result",
      "type" : [
        "string",
        {
          "type" : "array",
          "items" : "Result"
        }
      ]
    }
  ]
}
```

### Union With Members Of The Same Type

In addition to the unions with disparate member types, Pegasus unions
can have more than one member of the same type inside a single union
definition. For example a union can have multiple members that are of,

  - same primitive type
  - same named complex type
  - array of same or different item types
  - map of same or different value types
  - typeref dereferenced to same type.

Such unions **must** specify an unique alias for each member in the
union definition. When an alias is specified, it acts as the member’s
discriminator unlike the member type name on the standard unions
[defined above](#union).

In the example below, the union definition has a `string` and two
`array` members with unique aliases for each of them.

```json
{
  "type" : "record",
  "name" : "RecordWithAliasedUnion",
  "namespace" : "com.linkedin.pegasus.examples",
  "fields" :
  [
    {
      "name" : "result",
      "type" : [
        {
          "type" : "string",
          "alias" : "message"
        },
        {
          "type": {
            "type" : "array",
            "items" : "Result"
          },
          "alias" : "successResults"
        },
        {
          "type": {
            "type" : "array",
            "items" : "Result"
          },
          "alias" : "failureResults"
        }
      ]
    }
  ]
}
```

There are few constraints that must be taken in consideration while
specifying aliases for union members,

1.  Aliases must be unique for each member in a union definition.
2.  Aliases must be specified for either all or none of the members in a
    union definition.
3.  Aliases cannot be specified for `null` member types which means
    there can only be one null member inside a union definition.

## Default Values

A field may be declared to have a default value. The default value will
be validated against the schema type of the field. For example, if the
type is record and it has non-optional fields, then the default value
must include the non-optional fields in the default value.

A default value may be declared for a required or an optional field. The
default value is used by the get accessor. Unlike Avro, default values
are not assigned to absent fields on de-serialization.

If a default value is declared for a required field, for all three
[GetMode](#GetMode), the get accessor behaves the same as that for an
optional field with a default value. More specifically, the get accessor
doesn’t fail even if the field is absent. However, in this case, [data
validation](#data-to-schema-validation) may fail if
[RequiredMode](#requiredmode) is set to `MUST_BE_PRESENT`.

```json
{
  "type" : "record",
  "name" : "Default",
  "namespace" : "com.linkedin.pegasus.generator.examples",
  "fields" :
  [
    {
      "name" : "mandatoryWithDefault",
      "type" : "string",
      "default" : "this is the default string"
    },
    {
      "name" : "optionalWithDefault",
      "type" : "string",
      "optional" : true,
      "default" : "this is the default string"
    }
  ]
}
```

See [GetMode](#GetMode) for more detailed information on how the code
generated data stubs access optional/default fields.

### Default Values for Union Types

An Avro default value for a union type does not include the member
discriminator and the type of the default value must be the first member
type in the list of member types.

```js
{
  ...
  "fields" :
  [
    {
      "name" : "foo",
      "type" : [ "int", "string" ],
      "default"  : 42
    }
  ]
}
```

A Pegasus default value for a union type must include the member
discriminator. This allows the same typeref’ed union to have default
values of different member types.

```js
{
  ...
  "fields" :
  [
    {
      "name" : "foo",
      "type" : [ "int", "string" ]
      "default" : { "int" : 42 }
    }
  ]
}
```

For unions with aliased members, the specified alias is used as member
discriminator instead of the type name.

```js
{
  ...
  "fields" :
  [
    {
      "name" : "foo",
      "type" : [
        { "type" : "int", "alias" : "count" },
        { "type" : "string", "alias" : "message" }
      ],
      "default" : { "count" : 42 }
    }
  ]
}
```

Note the Avro syntax optimizes the most common union with null pattern
that is used to represent optional fields to be less verbose. However,
Pegasus has explicit support for optional fields and discourages the use
of union with null. This significantly reduces the syntactical sugar
benefit of the Avro optimization.

If a union type may be translated to Avro, all default values provided
for the union type must be of the first member type in the list of
member types. This restriction is because Avro requires default values
of a union type to be of the first member type of the union.

## Including Fields from Another Record

Pegasus implements the “include” attribute. It is used to include all
fields from another record into the current record. It does not include
any other attribute of the record. Include is transitive, if record A
includes record B and record B includes record C, record A contains all
the fields declared in record A, record B and record C.

The value of the “include” attribute should be a list of records or
typerefs of records. It is an error to specify non-record types in this
list.

```json
{
  "doc"  : "Bar includes fields of Foo, Bar will have fields f1 from itself and b1 from Bar",
  "type" : "record",
  "name" : "Bar",
  "include" : [ "Foo" ],
  "fields" : [
    {
      "name" : "b1",
      "type" : "string",
    }
  ]
}

{
  "type" : "record",
  "name" : "Foo",
  "fields" : [
    {
      "name" : "f1",
      "type" : "string",
    }
  ]
}
```

## Deprecation

Named schema, record field, and enum symbol can be deprecated using
`deprecated` property or `deprecatedSymbols` property in enum
declaration. The property value can be a string describing why the
schema element is deprecated or an alternative, or simply boolean
`true`. However, the latter use is discouraged and may be removed in the
future. The Java binding generated for these elements will be marked as
deprecated.

### Named Schema

To deprecate a named schema, add `deprecated` property to its
declaration.

```json
{
  "type" : "record",
  "name" : "Deprecated",
  "namespace" : "com.linkedin.pegasus.generator.test",
  "deprecated": "Use Foo instead.",
  "fields" : [
    ...
  ]
}
```

### Record Field

To deprecate a record field, add `deprecated` property to its
declaration.

```json
{
  "type" : "record",
  "name" : "Foo",
  "namespace" : "com.linkedin.pegasus.generator.test",
  "fields" : [
    {
      "name" : "deprecatedInt",
      "type" : "int",
      "deprecated": "Reason for int deprecation."
    }
  ]
}
```

### Enum Symbol

To deprecate an enum symbol, add `deprecatedSymbols` property to the
enum declaration. The value of the property is a map from the symbol
name to a string description.

```json
{
  "name" : "Planet",
  "namespace" : "com.linkedin.pegasus.generator.test",
  "type" : "enum",
  "symbols" : [ "MERCURY", "VENUS", "EARTH", "MARS", "JUPITER", "SATURN", "URANUS", "NEPTUNE", "PLUTO" ],
  "deprecatedSymbols": {
    "PLUTO": "Reclassified as dwarf planet."
  }
}
```