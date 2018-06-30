---
layout: guide
title: DATA-Data-Schema-and-Templates
permalink: /rest.li/DATA-Data-Schema-and-Templates
---

# Contents

  - [Introduction](#introduction)
  - [Schema Definition](#schema-definition)
  - [How Data is Serialized for
    Transport](#how-data-is-serialized-for-transport)
  - [How Data is Represented in
    Memory](#how-data-is-represented-in-memory)
  - [Java Binding](#java-binding)
  - [Avro Translation](#avro-translation)

# Introduction

The Pegasus Data layer includes the following:

  - The schema definition language for specifying the data that will be
    exchanged among systems
  - How this data will be serialized for transport
  - How this data will be represented in memory
  - The generic and type-safe Java API’s for manipulating data. These
    generated Java classes are referred to as the ‘data templates’.

# Schema Definition

The Pegasus schema definition language is inspired by the [Avro 1.4.1
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

    ```{
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

    ```{
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

    ```{
      "type" : "enum",
      "name" : "Fruits",
      "namespace" : "com.linkedin.pegasus.generator.examples",
      "doc" : "A fruit",
      "symbols" : [ "APPLE", "BANANA", "ORANGE", "PINEAPPLE" ],
      "symbolDocs" : { "APPLE":"A red, yellow or green fruit.", "BANANA":"A yellow fruit.", "ORANGE":"An orange fruit.", "PINEAPPLE":"A yellow fruit."} 
    }
    ```

com/linkedin/pegasus/generator/examples/MD5.pdsc

    ```{
      "type" : "fixed",
      "name" : "MD5",
      "namespace" : "com.linkedin.pegasus.generator.examples",
      "doc" : "MD5",
      "size" : 16
    }
    ```

com/linkedin/pegasus/generator/examples/StringList.pdsc

    ```{
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

    ```{
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

Typerefs use the type name “typeref” and support the following
attributes:

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

    ```
    {
      "type" : "typeref",
      "name" : "URN",
      "ref"  : "string",
      "doc"  : "A URN, the format is defined by RFC 2141"
    }
    ```

Differentiate time from long

    ```
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

    ```
    {
      "type" : "record",
      "name" : "Optional",
      "namespace" : "com.linkedin.pegasus.generator.examples",
      "fields" :
      [
        {
          "name"     : "foo",
          "type"     : "string",
          "optional" : true
        }
      ]
    }
    ```

Optional field present

    ```
    {
      "foo" : "abcd"
    }
    ```

Optional field absent

    ```
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

    ```
    {
      "type" : "record",
      "name" : "OptionalWithUnion",
      "namespace" : "com.linkedin.pegasus.generator.examples",
      "fields" :
      [
        {
          "name"    : "foo",
          "type"    : ["string", "null"]
        }
      ]
    }
    ```

optional field present

    ```
    {
      "foo" : { "string" : "abcd" }
    }
    ```

optional field absent

    ```
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

    ```
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

    ```
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

    ```
    {
      "type" : "record",
      "name" : "Default",
      "namespace" : "com.linkedin.pegasus.generator.examples",
      "fields" :
      [
        {
          "name"     : "mandatoryWithDefault",
          "type"     : "string",
          "default"  : "this is the default string"
        },
        {
          "name"     : "optionalWithDefault",
          "type"     : "string",
          "optional" : true,
          "default"  : "this is the default string"
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

    ```
    {
      ...
      "fields" :
      [
        {
          "name"     : "foo",
          "type"     : [ "int", "string" ],
          "default"  : 42
        }
      ]
    }
    ```

A Pegasus default value for a union type must include the member
discriminator. This allows the same typeref’ed union to have default
values of different member types.

    ```
    {
      ...
      "fields" :
      [
        {
          "name"     : "foo",
          "type"     : [ "int", "string" ]
          "default"  : { "int" : 42 }
        }
      ]
    }
    ```

For unions with aliased members, the specified alias is used as member
discriminator instead of the type name.

    ```
    {
      ...
      "fields" :
      [
        {
          "name"     : "foo",
          "type"     : [
            { "type" : "int", "alias" : "count" },
            { "type" : "string", "alias" : "message" }
          ],
          "default"  : { "count" : 42 }
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

    ```
    {
      "doc"  : "Bar includes fields of Foo, Bar will have fields f1 from itself and b1 from Bar",
      "type" : "record",
      "name" : "Bar",
      "include" : [ "Foo" ],
      "fields" : [
        {
          "name"     : "b1",
          "type"     : "string",
        }
      ]
    }
    
    {
      "type" : "record",
      "name" : "Foo",
      "fields" : [
        {
          "name"     : "f1",
          "type"     : "string",
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

    ```
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

    ```
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

    ```
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

<a name="HowDataisSerializedforTransport"></a>

# How Data is Serialized for Transport

The data is serialized for transport using JSON encoding in following
the Avro 1.4.1 specification. This JSON encoding is also the same as the
JSON expression used to describe default values.

**One notable difference from the Avro spec is that optional fields with
no value are represented by its omission in the serialized data. To
phrase it differently, optional fields are never explicitly set to
`null` in the serialized body.** As such, `null` is never a valid value
to appear in the serialized data. The only exception to this rule is if
the schema for the data is a union that has a `null` member.

The following table summarizes the JSON encoding.

<table>
<thead>
<tr class="header">
<th>Schema Type <br /></th>
<th>JSON Type <br /></th>
<th>JSON Encoding Examples <br /></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td>int <br /></td>
<td>number <br /></td>
<td>123 <br /></td>
</tr>
<tr class="even">
<td>long <br /></td>
<td>number <br /></td>
<td>123456789000 <br /></td>
</tr>
<tr class="odd">
<td>float <br /></td>
<td>number <br /></td>
<td>3.5 <br /></td>
</tr>
<tr class="even">
<td>double <br /></td>
<td>number <br /></td>
<td>3.5555555 <br /></td>
</tr>
<tr class="odd">
<td>boolean <br /></td>
<td>true or false <br /></td>
<td>true <br /></td>
</tr>
<tr class="even">
<td>string <br /></td>
<td>string <br /></td>
<td>“hello” <br /></td>
</tr>
<tr class="odd">
<td>bytes <br /></td>
<td>string (bytes encoded as least significant 8-bits of 16-bit character) <br /></td>
<td>“\u00ba\u00db\u00ad” <br /></td>
</tr>
<tr class="even">
<td>enum <br /></td>
<td>string <br /></td>
<td>“APPLE” <br /></td>
</tr>
<tr class="odd">
<td>fixed <br /></td>
<td>string (bytes encoded as least significant 8-bits of 16-bit character)</td>
<td>“\u0001\u0002\u0003\u0004” (fixed of size 4) <br /></td>
</tr>
<tr class="even">
<td>array <br /></td>
<td>array <br /></td>
<td>[ 1, 2, 3 ] <br /></td>
</tr>
<tr class="odd">
<td>map <br /></td>
<td>object <br /></td>
<td>{ “a” : 95, “b” : 90, “c” : 85 } <br /></td>
</tr>
<tr class="even">
<td>record (error) <br /></td>
<td>object (each field is encoded using a name/value pair in the object) <br /></td>
<td>{ “intField” : 1, “stringField” : “abc”, “fruitsField” : “APPLE” }</td>
</tr>
<tr class="odd">
<td>union <br /></td>
<td>null if value is null. <br /><br />
object if member value is not null with only one name/value pair in the object. The name will be the member discriminator (<strong>NOTE Member discriminator will be the member’s alias if one is specified, else it is the member’s fully qualified type name.</strong>) and value is the JSON encoded value. <br /></td>
<td>null <br /><br />
{ “int” : 1 } <br /><br />
{ “float” : 3.5 } <br /><br />
{ “string” : “abc” } <br /><br />
{ “array” : { “s1”, “s2”, “s3” } } <br /><br />
{ “map” : { “key1” : 10, “key2” : 20, “key3” : 30 } } <br /><br />
{ “com.linkedin.generator.examples.Fruits” : “APPLE” }</td>
</tr>
</tbody>
</table>

If a union schema has a typeref member, then the key for that member is
the dereferenced type. E.g. for union

    ```{
      "name" : "unionField",
      "type" : [
        "int",
        { "type" : "typeref", "name" : "a.b.c.d.Foo", "ref"  : "string" }
      ]
    }
    ```

the JSON encoding for the typeref member should look like

```{ “string” : “Correct key” }```

NOT

```{ “a.b.c.d.Foo” : “Wrong key” }```

Similarly, for a union with aliased members the key for the members will
be its corresponding alias. For example,

    ```{
      "name" : "unionField",
      "type" : [
        { "type" : "int", "alias" : "count" },
        { "type" : { "type" : "typeref", "name" : "a.b.c.d.Foo", "ref"  : "string" }, "alias" : "foo" }
      ]
    }
    ```

the JSON encoding for the typeref member should look like

```{ “foo” : “Correct key” }```

#### How to serialize data to JSON

`DataMapUtils` provides convenience methods to serialize and deserialize
between data and JSON using `JacksonDataCodec`.

To serialize from a DataMap to JSON:

    ```
    DataMap dataMap = new DataMap();
    dataMap.put("message", "Hi!");
    byte[] jsonBytes = DataMapUtils.mapToBytes(dataMap);
    String json = new String(jsonBytes, "UTF-8");

```

To serialize from a RecordTemplate instance to JSON:

    ```
    Greeting greeting = new Greeting().setMessage("Hi!"); // Where Greeting is class extending RecordTemplate
    byte[] jsonBytes = DataMapUtils.dataTemplateToBytes(greeting, true);
    String json = new String(jsonBytes, "UTF-8");

```

#### How to Deserialize JSON to Data

To deserialize from JSON to a DataMap:

    ```
    InputStream in = IOUtils.toInputStream("{'message':'Hi!'}");
    DataMap dataMap = DataMapUtils.readMap(in);

```

To deserialize from JSON to a RecordTemplate:

    ```
    InputStream in = IOUtils.toInputStream("{'message':'Hi!'}");
    Greeting deserialized = DataMapUtils.read(in, Greeting.class); // Where Greeting is class extending RecordTemplate

```

#### How to Serialize Data to PSON

PSON is a binary format that can represent any JSON data but is more
compact, requires less computation to serialize and deserialize, and can
transmit byte strings directly.

PSON serialization/deserialization works similar to JSON (as described
above) but uses these two methods:

    ```
    DataMapUtils.readMapPson()
    DataMapUtils.mapToPsonBytes()
    ```

# How Data is Represented in Memory

There are three architectural layers that define how data is stored
in-memory and provide the API’s used to access this data.

  - The first layer is the Data layer. This is the storage layer and is
    totally generic, for example, not schema aware.
  - The second layer is the Data Schema layer. This layer provides the
    in-memory representation of the data schema.
  - The third layer is the Data Template layer. This layer provides Java
    type-safe access to data stored by the Data layer.

## The Data Layer

At the conceptual level, the Data layer provides generic in-memory
representations of JSON objects and arrays. A `DataMap` and a `DataList`
provide the in-memory representation of a JSON object and a JSON array
respectively. These DataMaps and DataLists are the primary in-memory
data structures that store and manage data belonging to instances of
complex schema types. This layer allows data to be serialized and
de-serialized into in-memory representations without requiring the
schema to be known. In fact, the Data layer is not aware of schemas and
do not require a schema to access the underlying data.

The main motivations behind the Data layer are:

  - To allow generic access to the underlying data for building generic
    assembly and query engines. These engines need a generic data
    representation to data access. Furthermore, they may need to
    construct new instances from dynamically executed expressions, such
    as joins and projections. The schema of these instances depend on
    the expression executed, and could not be known in advance.
  - To facilitate schema evolution. The Data layer enables “use what you
    know and pass on what you don’t”. It allows new fields to be added
    and passed through intermediate nodes in the service graph without
    requiring these nodes to also have their schemas updated to include
    these new fields.
  - To permit some Java Virtual Machine service calls to be optimized by
    avoiding serialization.

### Constraints

The Data layer implements the following constraints:

  - It permits only allowed types to be stored as values.
  - All non-container values (not `DataMap` and not `DataList`) are
    immutable.
  - Null is not a value. The `Data.NULL` constant is used to represent
    null deserialized from or to be serialized to JSON. Avoiding null
    Java values reduces complexity by reducing the number of states a
    field may have. Without null values, a field can have two states,
    “absent” or “has valid value”. If null values are permitted, a
    field can have three states, “absent”, “has null value”, and “has
    valid value”.
  - The object graph is always acyclic. The object graph is the graph of
    objects connected by DataMaps and DataLists.
  - The key type for a `DataMap` is always `java.lang.String`.

### Additional Features

The Data layer provides the following additional features (above and
beyond what the Java library provides.)

  - A `DataMap` and `DataList` may be made read-only. Once it is
    read-only, mutations will no longer be allowed and will throw
    `java.lang.UnsupportedOperationException`. There is no way to revert
    a read-only instance to read-write.
  - Access instrumentation. See `com.linkedin.data.Instrumentable` for
    details.
  - Implements deep copy that should return a object graph that is
    isomorphic with the source, i.e. the copy will retain the directed
    acyclic graph structure of the source.

### Allowed Value Types

  - `java.lang.Integer`
  - `java.lang.Long`
  - `java.lang.Float`
  - `java.lang.Double`
  - `java.lang.Boolean`
  - `java.lang.String`
  - `com.linkedin.data.ByteString`
  - `com.linkedin.data.DataMap`
  - `com.linkedin.data.DataList`

Note Enum types are not allowed because enum types are not generic and
portable. Enum values are stored as a string.

### DataComplex

Both `DataMap` and `DataList` implement the
`com.linkedin.data.DataComplex` interface. This interface declares the
methods that supports the additional features common to a `DataMap` and
a `DataList`. These methods are:

<table>
<thead>
<tr class="header">
<th>Method</th>
<th>Declared by <br /></th>
<th>Description <br /></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td>```DataComplex clone()``` <br /></td>
<td>```DataComplex``` <br /></td>
<td>A shallow copy of the instance. <br /><br />
The read-only state is not copied, the clone will be mutable. <br /><br />
The instrumentation state is also not copied. <br /><br />
Although ```java.lang.CloneNotSupportedException``` is declared in the throws clause, the method should not throw this exception. <br /></td>
</tr>
<tr class="even">
<td>```DataComplex copy()``` <br /></td>
<td>```DataComplex``` <br /></td>
<td>A deep copy of the object graph rooted at the instance. The copy will be isomorphic to the original. <br /><br />
The read-only state is not deep copied, and the new ```DataComplex``` copies will be mutable. <br /><br />
The instrumentation state is also not copied. <br /><br />
Although ```java.lang.CloneNotSupportedException``` is declared in the throws clause, the method should not throw this exception.</td>
</tr>
<tr class="odd">
<td>```void setReadOnly()``` <br /></td>
<td>```CowCommon``` <br /></td>
<td>Make the instance read-only. It does not affect the read-only state of contained ```DataComplex``` values. <br /></td>
</tr>
<tr class="even">
<td>```boolean isReadOnly()``` <br /></td>
<td>```CowCommon``` <br /></td>
<td>Whether the instance is in read-only state. <br /></td>
</tr>
<tr class="odd">
<td>```void makeReadOnly()``` <br /></td>
<td>```DataComplex``` <br /></td>
<td>Make the object graph rooted at this instance read-only. <br /></td>
</tr>
<tr class="even">
<td>void ```isMadeReadOnly()``` <br /></td>
<td>```DataComplex``` <br /></td>
<td>Whether the object graph rooted at this instance has been made read-only. <br /></td>
</tr>
<tr class="odd">
<td>```Collection&lt;Object&gt; values()``` <br /></td>
<td>```DataComplex``` <br /></td>
<td>Returns the values stored in the ```DataComplex``` instance, i.e. returns the values of a ```DataMap``` or the elements of a ```DataList```. <br /></td>
</tr>
<tr class="even">
<td>```void startInstrumentatingAccess()``` <br /></td>
<td>```Instrumentable``` <br /></td>
<td>Starts instrumenting access. <br /></td>
</tr>
<tr class="odd">
<td>```void stopInstrumentingAccess()``` <br /></td>
<td>```Instrumentable``` <br /></td>
<td>Stops instrumenting access. <br /></td>
</tr>
<tr class="even">
<td>```void clearInstrumentedData()``` <br /></td>
<td>```Instrumentable``` <br /></td>
<td>Clears instrumentation data collected. <br /></td>
</tr>
<tr class="odd">
<td>```void collectInstrumentedData(...)``` <br /></td>
<td>```Instrumentable``` <br /></td>
<td>Collect data gathered when instrumentation was enabled. <br /></td>
</tr>
</tbody>
</table>

Note: Details on `CowCommon`, `CowMap`, and `CowList` have been omitted
or covered under `DataComplex`. Cow provides copy-on-write
functionality. The semantics of `CowMap` and `CowList` is similar to
`HashMap` and `ArrayList`.

### DataMap

The `com.linkedin.data.DataMap` class has the following characteristics:

  - `DataMap` implements `java.util.Map<String, Object>`.
  - Its `entrySet()`, `keySet()`, and `values()` methods return
    unmodifiable set and collection views.
  - Its `clone()` and `copy()` methods returns a `DataMap`.

### DataList

The `com.linkedin.data.DataList` class has the following
characteristics.

  - `DataList` implements `java.util.List<Object>`.
  - Its `clone()` and `copy()` method return a `DataList`.

## The Data Schema Layer

The Data Schema layer provides the in-memory representation of the data
schema. The Data Schema Layer provides the following main features:

  - Parse a JSON encoded schema into in-memory representation using
    classes in this layer
  - Validate a Data object against a schema

Their common base class for Data Schema classes is
`com.linkedin.data.schema.DataSchema`. It defines the following methods:

|*. Method <br /> |*. Description <br /> |  
| `Type getType()` <br /> | Provide the type of the schema, can be
`BOOLEAN`, `INT`, `LONG`, `FLOAT`, `DOUBLE`, `BYTES`, `STRING`, `FIXED`,
`ENUM`, `NULL`, `ARRAY`, `RECORD`, `MAP`, `UNION`. <br /> |  
| `boolean hasError()` <br /> | Whether the schema definition contains
at least one error. <br /> |  
| `boolean isPrimitive()` <br /> | Whether the schema type is a
primitive schema type. <br /> |  
| `boolean isComplex()` <br /> | Whether the schema type is a complex
schema type, i.e. not primitive type. <br /> |  
| `Map<String,Object> getProperties()` <br /> | Return the properties of
the schema. These properties are the keys and values from the JSON
fields in complex schema definitions that are not processed and
interpreted by the schema parser. For primitive types, this method
always return an immutable empty map. <br /> |  
| `String getUnionMemberKey()` <br /> | If this type is used as a member
of a union without an alias, this will be the key that uniquely
identifies/selects this type within the union. This value of this key is
as defined by the Avro 1.4.1 specification for JSON serialization.
<br /> |  
| `String toString()` <br /> | A more human consumable formatting of the
schema in JSON encoding. Space will added between fields, items, names,
values, … etc. <br /> |  
| `Type getDereferencedType` <br /> | If the type is a typeref, it will
follow the typeref reference chain and return the type referenced at the
end of the typeref chain. <br /> |  
| `DataSchema getDereferencedSchema` <br /> | If the type is a typeref,
it will follow the typeref reference chain and return the DataSchema
referenced at the end of the typeref chain. <br /> |  
The following table shows the mapping of schema types to Data Schema
classes.

<table>
<thead>
<tr class="header">
<th>Schema Type <br /></th>
<th>Data Schema class <br /></th>
<th>Relevant Specific Attributes <br /></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td>int <br /></td>
<td>```IntegerDataSchema``` <br /></td>
<td></td>
</tr>
<tr class="even">
<td>long <br /></td>
<td>```LongDataSchema``` <br /></td>
<td></td>
</tr>
<tr class="odd">
<td>float <br /></td>
<td>```FloatDataSchema``` <br /></td>
<td></td>
</tr>
<tr class="even">
<td>double <br /></td>
<td>```DoubleDataSchema``` <br /></td>
<td></td>
</tr>
<tr class="odd">
<td>boolean <br /></td>
<td>```BooleanDataSchema``` <br /></td>
<td></td>
</tr>
<tr class="even">
<td>string <br /></td>
<td>```StringDataSchema``` <br /></td>
<td></td>
</tr>
<tr class="odd">
<td>bytes <br /></td>
<td>```BytesDataSchema``` <br /></td>
<td></td>
</tr>
<tr class="even">
<td>enum <br /></td>
<td>```EnumDataSchema``` <br /></td>
<td>```List&lt;String&gt; getSymbols()``` <br /><br />
```int index(String symbol)``` <br /><br />
```boolean contains(String symbol)``` <br /></td>
</tr>
<tr class="odd">
<td>array <br /></td>
<td>```ArrayDataSchema``` <br /></td>
<td>```DataSchema getItems()``` <br /></td>
</tr>
<tr class="even">
<td>map <br /></td>
<td>```MapDataSchema``` <br /></td>
<td>```DataSchema getValues()```</td>
</tr>
<tr class="odd">
<td>fixed <br /></td>
<td>```FixedDataSchema``` <br /></td>
<td>```int getSize()``` <br /></td>
</tr>
<tr class="even">
<td>record, error <br /></td>
<td>```RecordDataSchema``` <br /></td>
<td>```RecordType recordType()``` (record or error) <br /><br />
```boolean isErrorRecord()``` <br /><br />
```List&lt;Field&gt; getFields()``` <br /><br />
```int index(String fieldName)``` <br /><br />
```boolean contains(String fieldName)``` <br /><br />
```Field getField(String fieldName)``` <br /></td>
</tr>
<tr class="odd">
<td>union <br /></td>
<td>```UnionDataSchema``` <br /></td>
<td>```List&lt;Member&gt; getMembers()``` <br /><br />
```boolean contains(String memberKey)``` <br /><br />
```DataSchema getTypeByMemberKey(String memberKey)``` <br /><br />
```boolean areMembersAliased()``` <br /></td>
</tr>
<tr class="even">
<td>null <br /></td>
<td>```NullDataSchema``` <br /></td>
<td></td>
</tr>
</tbody>
</table>

## Data to Schema Validation

The `ValidateDataAgainstSchema` class provides methods for validating
Data layer instances with a Data Schema. The `ValidationOption` class is
used to specify how validation should be performed and how to fix-up the
input Data layer objects to conform to the schema. There are two
independently configuration options:

  - `RequiredMode` option indicates how required fields should be
    handled during validation.
  - `CoercionMode` option indicates how to coerce Data layer objects to
    the Java type corresponding to their schema type.

Example Usage:

```java  
ValidationResult validationResult =
ValidateDataAgainstSchema.validate(dataTemplate, dataTemplate.schema(),
new ValidationOptions());  
if (\!validationResult.isValid())  
{  
// do something  
}  
```

### RequiredMode

The available RequiredModes are:

  - `IGNORE`  
    Required fields may be absent. Do not indicate a validation error if
    a required field is absent.
  - `MUST_BE_PRESENT`  
    If a required field is absent, then validation fails. Validation
    will fail even if the required field has been declared with a
    default value.
  - `CAN_BE_ABSENT_IF_HAS_DEFAULT`  
    If a required field is absent and the field has not been declared
    with a default value, then validation fails. Validation will not
    attempt to modify the field to provide it with the default value.
  - `FIXUP_ABSENT_WITH_DEFAULT`  
    If a required field is absent and it cannot be fixed-up with a
    default value, then validation fails.  
    This mode will attempt to modify an absent field to provide it with
    the field’s default value.  
    If the field does not have a default value, validation fails.  
    If the field has a default value, validation will attempt to set the
    field’s value to the default value.  
    This attempt may fail if fixup is not enabled or the `DataMap`
    containing the field cannot be modified because it is read-only.  
    The provided default value will be read-only.

### CoercionMode

Since JSON does not have or encode enough information on the actual
types of primitives, and schema types like bytes and fixed are not
represented by native types in JSON, the initial de-serialized in-memory
representation of instances of these types may not be the actual type
specified in the schema. For example, when de-serializing the number 52,
it will be de-serialized into an `Integer` even though the schema type
may be a `Long`. This is because a schema is not required to serialize
or de-serialize.

When the data is accessed via schema aware language binding like the
Java binding, the conversion/coercion can occur at the language binding
layer. In cases when the language binding is not used, it may be
desirable to fix-up a Data layer object by coercing it the Java type
corresponding to the object’s schema. For example, the appropriate Java
type the above example would be a `Long`. Another fix-up would be to
fixup Avro-specified string encoding of binary data (bytes or fixed)
into a `ByteString`. In another case, it may be desirable to coerce the
string representation of a value to the Java type corresponding to the
object’s schema. For example, coerce “65” to 65, the integer, if the
schema type is “int”.

Whether an how coercion is performed is specified by `CoercionMode`. The
available CoercionModes are:

  - `OFF`  
    No coercion is performed.
  - `NORMAL`  
    Numeric types are coerced to the schema’s corresponding Java numeric
    type. Avro-encoded binary strings are coerced to ByteString if the
    schema type is bytes or fixed.
  - `STRING_TO_PRIMITIVE`  
    Includes all the coercions performed by `NORMAL`. In addition, also
    coerces string representations of numbers to the schema’s
    corresponding numeric type, and string representation of booleans
    (“true” or “false” case-insenstive) to `Boolean`.

<a name="NormalMode"></a>

#### `NORMAL` Coercion Mode

The following table provides additional details on the `NORMAL`
validation and coercion mode.

<table>
<thead>
<tr class="header">
<th>Schema Type <br /></th>
<th>Post-coercion Java Type <br /></th>
<th>Pre-coercion Input Java Types <br /></th>
<th>Validation Performed <br /></th>
<th>Coercion Method</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td>int <br /></td>
<td>```java.lang.Integer``` <br /></td>
<td>```java.lang.Number``` (1) <br /></td>
<td>Value must be a ```Number```. <br /></td>
<td>```Number.intValue()``` <br /></td>
</tr>
<tr class="even">
<td>long <br /></td>
<td>```java.lang.Long``` <br /></td>
<td>```java.lang.Number``` (1)</td>
<td>Value must be a ```Number```. <br /></td>
<td>```Number.longValue()``` <br /></td>
</tr>
<tr class="odd">
<td>float <br /></td>
<td>```java.lang.Float``` <br /></td>
<td>```java.lang.Number``` (1)</td>
<td>Value must be a ```Number```.</td>
<td>```Number.floatValue()``` <br /></td>
</tr>
<tr class="even">
<td>double <br /></td>
<td>```java.lang.Double``` <br /></td>
<td>```java.lang.Number``` (1)</td>
<td>Value must be a ```Number```.</td>
<td>```Number.doubleValue()``` <br /></td>
</tr>
<tr class="odd">
<td>boolean <br /></td>
<td>```java.lang.Boolean``` <br /></td>
<td>```java.lang.Boolean``` (2) <br /></td>
<td>Value must be a ```Boolean```. <br /></td>
<td></td>
</tr>
<tr class="even">
<td>string <br /></td>
<td>```java.lang.String``` <br /></td>
<td>```java.lang.String``` (2) <br /></td>
<td>Value must be a ```String```. <br /></td>
<td></td>
</tr>
<tr class="odd">
<td>bytes <br /></td>
<td>```com.linkedin.data.ByteString``` <br /></td>
<td>```com.linkedin.data.ByteString```, ```java.lang.String``` (3) <br /></td>
<td>If the value is a ```String```, the ```String``` must be a valid encoding of binary data as specified by the Avro specification for encoding bytes into a JSON string. <br /></td>
<td>```ByteString.copyFromAvroString()``` <br /></td>
</tr>
<tr class="even">
<td>enum <br /></td>
<td>```java.lang.String``` <br /></td>
<td>```java.lang.String``` <br /></td>
<td>The value must be a symbol defined by the enum schema. <br /></td>
<td></td>
</tr>
<tr class="odd">
<td>array <br /></td>
<td>```com.linkedin.data.DataList``` <br /></td>
<td>```com.linkedin.data.DataList``` (2) <br /></td>
<td>Each element in the ```DataList``` must be a valid Java type for the schema’s item type. For example, if the schema is an array of longs, then every element in the ```DataList``` must be a ```Number```. <br /></td>
<td></td>
</tr>
<tr class="even">
<td>map <br /></td>
<td>```com.linkedin.data.DataMap``` <br /></td>
<td>```com.linkedin.data.DataMap``` (2) <br /></td>
<td>Each value in the ```DataMap``` must be a valid Java type for the schema’s value type. For example, if the schema is a map of longs, then every value in the ```DataMap``` must be a ```Number```.</td>
<td></td>
</tr>
<tr class="odd">
<td>fixed <br /></td>
<td>```com.linkedin.data.ByteString``` <br /></td>
<td>```com.linked.data.ByteString``` (2), ```java.lang.String``` (3) <br /></td>
<td>If the value is a ```String```, the ```String``` must be a valid encoding of binary data as specified by the Avro specification for encoding bytes into a JSON string and the correct size for the fixed schema type. <br /><br />
If the value is a ```ByteString```, the ```ByteString``` must be the correct size for the fixed schema type. <br /></td>
<td>```ByteString.copyFromAvroString()```</td>
</tr>
<tr class="even">
<td>record <br /></td>
<td>```com.linkedin.data.DataMap``` <br /></td>
<td>```com.linkedin.data.DataMap``` (2) <br /></td>
<td>Each key in the ```DataMap``` will be used lookup a field in the record schema. The value associated with this key must be a valid Java type for the field’s type. <br /><br />
If the required validation option is enabled, then all required fields must also be present. <br /></td>
<td></td>
</tr>
<tr class="odd">
<td>union <br /></td>
<td>```com.linkedin.data.DataMap``` <br /></td>
<td>```java.lang.String```, ```com.linkedin.data.DataMap``` (2) <br /></td>
<td>f the value is a ```String```, the value must be ```Data.NULL```. <br /><br />
If the value is a ```DataMap```, then the ```DataMap``` must have exactly one entry. The key of the entry must identify a member of the union schema, and the value must be a valid type for the identified union member’s type. <br /></td>
<td></td>
</tr>
</tbody>
</table>

(1) Even though `Number` type is allowed and used for fixing up to the
desired type, the Data layer only allows `Integer`, `Long`, `Float`, and
`Double` values to be held in a `DataMap` or `DataList`.  
(2) No fix-up is performed.  
(3) the `String` must be a valid encoding of binary data as specified by
the Avro specification for encoding bytes into a JSON string.

#### `STRING_TO_PRIMITIVE` Coercion Mode

This mode includes allowed input types and associated validation and
coercion’s of `NORMAL`. In addition, it allows the following additional
input types and performs the following coercions on these additional
allowed input types.

|*. Schema Type <br /> |*. Post-coercion Java Type <br /> |*.
Pre-coercion Input Java Types <br /> |*. Validation Performed <br />
|_. Coercion Method <br /> |  
| int <br /> | `java.lang.Integer` <br /> | `java.lang.String` <br /> |
If value is a `String`, it must be acceptable to `BigDecimal(String
val)`, else it has to be a `Number` (see [`NORMAL`](#NormalMode.\))
<br /> | `(new BigDecimal(value)).intValue()` <br /> |  
| long <br /> | `java.lang.Long` <br /> | `java.lang.String` | If value
is a `String`, it must be acceptable to `BigDecimal(String val)`, else
it has to be a `Number` (see [`NORMAL`](#NormalMode.\)) | `(new
BigDecimal(value)).longValue()` |  
| float <br /> | `java.lang.Float` <br /> | `java.lang.String` | If
value is a `String`, it must be acceptable to `BigDecimal(String val)`,
else it has to be a `Number` (see [`NORMAL`](#NormalMode.\)) <br /> |
`(new BigDecimal(value)).floatValue()` |  
| double <br /> | `java.lang.Double` <br /> | `java.lang.String` | If
value is a `String`, it must be acceptable to `BigDecimal(String val)`,
else it has to be a `Number` (see [`NORMAL`](#NormalMode.\)) <br /> |
`(new BigDecimal(value)).doubleValue()` |  
| boolean <br /> | `java.lang.Boolean` <br /> | `java.lang.String` | if
value is a `String`, its value must be either `"true"` or `"false"`
ignoring case, else it has to be a `Boolean` (see
[`NORMAL`](#NormalMode.\)) <br /> |

    ```if ("true".equalsIgnoreCase(value))
        return Boolean.TRUE;
    else if ("false".equalsIgnoreCase(value))
        return Boolean.FALSE;
    else 
         // invalid string representation
         // of boolean ```

|

### ValidationResult

The result of validation is returned through an instance of the
`ValidationResult` class. This class has the following methods:

<table>
<thead>
<tr class="header">
<th>Method</th>
<th>Description <br /></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td>```boolean hasFix()``` <br /></td>
<td>Whether any fix-ups (i.e., modification or replacement of input Data layer objects) have been proposed. Fixes may be proposed but not applied because fixes cannot be applied to read-only complex objects. <br /></td>
</tr>
<tr class="even">
<td>```boolean hasFixupReadOnlyError()``` <br /></td>
<td>Whether any fix-ups could not be applied because of read-only complex objects. <br /></td>
</tr>
<tr class="odd">
<td>```Object getFixed()``` <br /></td>
<td>Return a fixed object. In-place fixes may or may not be possible because some objects are immutable. For example, if the schema type is “fixed” and String object is provided as the Data object, the fixed-up object that would be returned will be a ByteString. Since String and ByteString are both immutable and have different types, the fixed object will be a different object, i.e. the fix-up cannot be done in-place. <br /><br />
<br /><br />
For complex objects, the fix-ups can be applied in place. This is because the new values can replace the old values in a ```DataMap``` or ```DataList```. <br /></td>
</tr>
<tr class="even">
<td>```boolean isValid()``` <br /></td>
<td>Whether the fixed object returns by ```getFixed()``` contains any errors. If it returns ```false```, then the fixed object and its dependents are fixed up according to the provided schema. <br /></td>
</tr>
<tr class="odd">
<td>```String getMessage()``` <br /></td>
<td>Provides details on validation and fix-up failures. Returns empty string if ```isValid()``` is ```true``` and fix-ups/validation have occurred without problems. <br /></td>
</tr>
</tbody>
</table>

Note: Schema validation and coercion are currently explicit operations.
They are not implicitly performed when data are de-serialized as part of
remote invocations.

## The Data Template Layer

The Data Template layer provides Java type-safe access to the underlying
data stored in the Data layer. It has explicit knowledge of the schema
of the data stored. The code generator generates classes for complex
schema types that derive from base classes in this layer. The common
base of these generated is `com.linkedin.data.DataTemplate`. Typically,
a `DataTemplate` instance is an overlay or wrapper for a `DataMap` or
`DataList` instance. It allows type-safe access to the underlying data
in the `DataMap` or `DataList`. (The exception is the `FixedTemplate`
which is a subclass of `DataTemplate` for fixed schema types.)

The Data Template layer provides the following abstract base classes
that are used to construct Java bindings for different complex schema
types.

| Class                          | Underlying Data <br /> | Description <br />                                                                                  |
| ------------------------------ | ---------------------- | --------------------------------------------------------------------------------------------------- |
| `AbstractArrayTemplate` <br /> | `DataList` <br />      | Base class for array types. <br />                                                                  |
| `DirectArrayTemplate` <br />   | `DataList` <br />      | Base class for array types containing unwrapped item types, extends `AbstractArrayTemplate`. <br /> |
| `WrappingArrayTemplate` <br /> | `DataList` <br />      | Base class for array types containing wrapped item types, extends `AbstractArrayTemplate`. <br />   |
| `AbstractMapTemplate` <br />   | `DataMap` <br />       | Base class for map types. <br />                                                                    |
| `DirectMapTemplate` <br />     | `DataMap` <br />       | Base class for map types containing unwrapped value types, extends `AbstractMapTemplate`. <br />    |
| `WrappingMapTemplate` <br />   | `DataMap` <br />       | Base class for map types containing wrapped value types, extends `AbstractMapTemplate`. <br />      |
| `FixedTemplate` <br />         | `ByteString` <br />    | Base class for fixed types. <br />                                                                  |
| `RecordTemplate` <br />        | `DataMap` <br />       | Base class for record types. <br />                                                                 |
| `ExceptionTemplate` <br />     | `DataMap` <br />       | Base class for record types that declared as errors. <br />                                         |
| `UnionTemplate` <br />         | `DataMap` <br />       | Base class for union types. <br />                                                                  |

The unwrapped schema types are:

  - int
  - long
  - float
  - double
  - boolean
  - string
  - bytes
  - enum

The wrapped schema types are types whose Java type-safe bindings are not
the same as their data type in the Data layer. These types require a
`DataTemplate` wrapper to provide type-safe access to the underlying
data managed by the Data layer. The wrapped types are:

  - array
  - map
  - fixed
  - record and error
  - union

`Enum` is an unwrapped type even though its Java type-safe binding is
not the same as its storage type in the Data layer. This is because enum
conversions are done through coercing to and from `java.lang.String` s
implemented by the Data Template layer. This is similar to coercing
between different numeric types also implemented by the Data Template
layer.

The following table shows the relationships among types defined in the
data schema, types stored and managed by the Data layer, and the types
of the Java binding in the Data Template
layer.

| Schema Type <br /> | Data Layer <br />                                                                         | Data Template Layer <br />                                   |
| ------------------ | ----------------------------------------------------------------------------------------- | ------------------------------------------------------------ |
| int <br />         | `java.lang.Integer` <br />                                                                | Coerced to `java.lang.Integer` or `int` (2) <br />           |
| long <br />        | `java.lang.Integer` or `java.lang.Long` (1) <br />                                        | Coerced to `java.lang.Long` or `long` (2) <br />             |
| float <br />       | `java.lang.Integer`, `java.lang.Long`, `java.lang.Float` or `java.lang.Double` (1) <br /> | Coerced to `java.lang.Float` or `float` (2) <br />           |
| double <br />      | `java.lang.Integer`, `java.lang.Long`, `java.lang.Float` or `java.lang.Double` (1) <br /> | Coerced to `java.lang.Double` or `double` (2) <br />         |
| boolean <br />     | `java.lang.Boolean` <br />                                                                | Coerced to `java.lang.Boolean` or `boolean` (2) <br />       |
| string <br />      | `java.lang.String` <br />                                                                 | `java.lang.String` <br />                                    |
| bytes <br />       | `java.lang.String` or `com.linkedin.data.ByteString` (3) <br />                           | `com.linkedin.data.ByteString` <br />                        |
| enum <br />        | `java.lang.String` <br />                                                                 | Generated enum class. <br />                                 |
| array <br />       | `com.linkedin.data.DataList` <br />                                                       | Generated or built-in array class. <br />                    |
| map <br />         | `com.linkedin.data.DataMap` <br />                                                        | Generated or built-in map class. <br />                      |
| fixed <br />       | `java.lang.String` or `com.linkedin.data.ByteString` <br />                               | Generated class that derives from `FixedTemplate` <br />     |
| record <br />      | `com.linkedin.data.DataMap` <br />                                                        | Generated class that derives from `RecordTemplate` <br />    |
| error <br />       | `com.linkedin.data.DataMap` <br />                                                        | Generated class that derives from `ExceptionTemplate` <br /> |
| union <br />       | `com.linkedin.data.DataMap` <br />                                                        | Generated class that derives from `UnionTemplate` <br />     |

(1) When a JSON object is deserialized, the actual schema type is not
known. Typically, the smallest sized type that can represent the
deserialized value will be used to store the value in-memory.  
(2) Depending on the method, un-boxed types will be preferred to boxed
types if applicable and the input or output arguments can never be
null.  
(3) When a JSON object is deserialized, the actual schema type is not
known for bytes and fixed. Values of bytes and fixed types are stored as
strings as serialized representation is a string. However, `ByteString`
is an equally valid Java type for these schema types.

# Java Binding

This section describes the details of the Java classes (dataModels)
generated by the code generator. These bindings are built on the classes
provided by the Data Template layer described above.

## Package and Class Names

The Java binding determines the package and class names of the generated
and/or built-in classes using the following rules.

|*. Schema Type <br /> |*. Java Package and Class Name <br /> |  
| maps and arrays of primitive types <br /> | Package name is
`com.linkedin.data.template`. <br />  
Class name is computed by appending “Map” or “Array” to the
corresponding boxed type’s class name. <br />  
For multi-dimensional maps and arrays, a “Map” or “Array” is appended
for each dimension starting with the inner most dimension first.
<br />  
<br />  
**Example Schema**

    ```
    { "type" : "map", "values" : "boolean" }
    { "type" : "array", "items" : { "type" : "map" : "values" : "string" } }
    ```

  
**Java package and class**

    ```
    package com.linkedin.data.templates;
    public class BooleanMap extends DirectArrayTemplate<Boolean> ...
    public class StringMapArray extends DirectArrayTemplate<StringMap> ...
    ```

<br /> |  
| enum, fixed, record types <br /> (named schema types) | Package name
is the package of the named schema type if it is specified, otherwise
package name will use the namespace of the named schema type by default.
<br />  
Class name is the name of the named schema type. <br />  
<br />  
**Example Schema**

    ```
    { "type" : "record", "name" : "a.b.c.d.Foo", "fields" : ... }
    { "type" : "enum", "name" : "Bar", "namespace" : "x.y.z", "package": "x.y.z.test", symbols" : ... }
    ```

  
**Java package and class**

    ```
    package a.b.c.d;
    public class Foo extends RecordTemplate ...
    
    package x.y.z.test;
    public enum class Bar ...
    ```

|  
| maps and arrays of enum, fixed, record <br />  
(maps and arrays of named schema types) <br /> | Package name is the
package name of the named schema type, which follows the rule documented
in this table for named schema. <br />  
Class name is computed by appending “Map” or “Array” to name of
generated class for the named schema type. <br />  
For multi-dimensional maps and arrays, a “Map” or “Array” is appended
for each dimension starting with the inner most dimension first.
<br />  
<br />  
**Example Schema**

    ```
    { "type" : "map", "values" : "a.b.c.d.Foo" }
    { "type" : "map", "values" : { "type" : "array", "items" : "a.b.c.d.Foo" } }
    
    { "type" : "array", "items" : "x.y.z.Bar" }
    { "type" : "array", "items" : { "type" : "map", "values" : "x.y.z.Bar" } }
    ```

  
**Java package and class**

    ```
    package a.b.c.d;
    
    public class FooMap extends WrappingMapTemplate<Foo> ...
    public class FooArrayMap extends WrappingMapTemplate<Foo> ...
    
    package x.y.z.test;
    public class BarArray extends DirectArrayTemplate<Bar> ...
    public class BarMapArray extends DirectArrayTemplate<Bar> ...```

|  
| unions <br /> | The name of the union class is determined in two ways.
<br />  
<br />  
**1. Union without typeref** <br />  
If there is no typeref for the union, the code generator makes up the
class name from the name of the closest enclosing field that declared
the union type. <br />  
Package name is the package name of the closest outer record type, which
follows the rule documented in this table for that closest outer record
type. <br />  
The generated union class will be declared in the generated class of the
closest outer record type. <br />  
Class name will be name of the field in the closest outer record that
declared the union with the first character capitalized. <br />  
<br />  
**Example Schema**

    ```
    {
      "type" : "record",
      "name" : "a.b.c.d.Foo",
      "fields" : [ { "name" : "bar", "type" : [ "int", "string" ] } ]
    }
    ```

  
**Java package and class**

    ```
    package a.b.c.d;
    public class Foo extends RecordTemplate {
      public class Bar extends UnionTemplate ...
    }
    ```

**2. Union with typeref** <br />  
If there is a typeref for the union, the code generator will use the
name of typeref for the generated union class. <br />  
Package name is the package of the typeref if it is specified, otherwise
package name is the namespace of the typeref by default. <br />  
Class name is the name of the typeref.  
<br />  
**Example Schema**

    ```
    {
      "type" : "typeref",
      "name" : "a.b.c.d.Bar",
      "package" : "a.b.c.d.test",
      "ref"  : [ "int", "string" ] 
    }
    ```

**Java package and class** <br />  
<br />

    ```
    package a.b.c.d.test;
    public class Bar extends UnionTemplate implements HasTyperefInfo {
      ...
      public TyperefInfo typerefInfo() 
      {
        ... 
      }
    }
    ```

  
When the typeref provides the name of the generated union class. The
generated class will also implement the `HasTyperefInfo` interface. This
interface declares the `typerefInfo()` method that will be implemented
by the generated class.

To avoid generating duplicate classes for the duplicate declarations of
unions, it is a good practice to declare unions with a typeref when the
same union type is used more than once.<br /> |  
| maps and arrays of unions <br /> | Package name is the package name of
the union, which follows the rule documented in this table for unions.
<br />  
The generated class will be declared in the same outer class (for unions
without typeref) or same package (for unions with typeref) as the
generated class for the union. <br />  
Class name is computed by appending “Map” or “Array” to the name of the
generated class for the union. <br />  
For multi-dimensional maps and arrays, a “Map” or “Array” is appended
for each dimension starting the inner most dimension first. <br />  
<br />  
**1. Union without typeref** <br />  
<br />  
**Example Schema**

    ```
    {
      "type" : "record",
      "name" : "a.b.c.d.Foo",
      "fields" : [
        { "name" : "members", "type" : { "type" : "array", "items" : [ "int", "string" ] } }
        { "name" : "locations", "type" : { "type" : "map", "values" : [ "int", "string" ]  } }
      ]
    }
    ```

  
**Java package and class**

    ```
    package a.b.c.d;
    public class Foo extends RecordTemplate {
      public class Members extends UnionTemplate ...
      public class MembersArray extends WrappingArrayTemplate<Members> ...
      public class Locations extends UnionTemplate ...
      public class LocationsMap extends WrappingMapTemplate<Locations> ...
    
      public MembersArray getMembers() ...
      public LocationsMap getLocations() ...
    }
    ```

**2. Union with typeref**  
**Example Schema** <br />

    ```
    {
      "type" : "typeref",
      "name" : "a.b.c.d.Bar",
      "package": "a.b.c.d.test",
      "ref"  : [ "int", "string" ] 
    }
    
    {
      "type" : "record",
      "name" : "a.b.c.d.Foo",
      "package" : "a.b.c.d.test",
      "fields" : [
        { "name" : "members", "type" : { "type" : "array", "items" : "Bar" } }
        { "name" : "locations", "type" : { "type" : "map", "values" : "Bar" } }
      ]
    }
    ```

**Java package and class** <br />  
<br />

    ```
    package a.b.c.d.test;
    
    public class Bar extends UnionTemplate ...
    public class BarArray extends WrappingArrayTemplate<Bar> ...
    public class BarMap extends WrappingMapTemplate<Bar> ...
    
    public class Foo extends RecordTemplate
    {
      public BarArray getMembers() ...
      public BarMap getLocations() ...
    }
    ```

<br /> |

## Primitive Types

The Java binding for primitive schema types are as follows:

| Schema Type <br /> | Java Type <br />                            |
| ------------------ | ------------------------------------------- |
| int <br />         | `java.lang.Integer` or `int` (1) <br />     |
| long <br />        | `java.lang.Long` or `long` (1) <br />       |
| float <br />       | `java.lang.Float` or `float` (1) <br />     |
| double <br />      | `java.lang.Double` or `double` (1) <br />   |
| boolean <br />     | `java.lang.Boolean` or `boolean` (1) <br /> |
| string <br />      | `java.lang.String` <br />                   |
| bytes <br />       | `com.linkedin.data.ByteString` <br />       |

(1) Depending on the method, un-boxed types will be preferred to boxed
types if applicable when input or output arguments can never be null.

In addition to the standard bindings, custom Java class bindings may be
defined for these types to specify a user-defined class as substitute
for the standard Java class bindings. For additional details, see
[Custom Java Class Binding for Primitive
Types](#CustomJavaClassBindingforPrimitiveTypes).

## Enum Type

The code generator generates a Java enum class. There will be a
corresponding symbol in the Java enum class for each symbol in the enum
schema. In addition, the code generator will add a `$UNKNOWN` symbol to
the generated enum class. `$UNKNOWN` will be returned if the value
stored in the Data layer cannot be mapped to a symbol present in the
Java enum class. For example, this may occur if an enum symbol has been
added to a new version of the enum schema and is transmitted to client
that has not been updated with the new enum schema.

Enums also supports a symbolDocs attribute to provide documentation for
each enum symbol. E.g.

    ```
    ...
      "symbols" : [ "APPLE", "BANANA", ... ],
      "symbolDocs" : { "APPLE":"A red, yellow or green fruit.", "BANANA":"A yellow fruit.", ... } 
    ...

```

    ```
    package com.linkedin.pegasus.generator.examples;
    
    ...
    /**
    * A fruit
    *
    */
    public enum Fruits {
    
        /**
         * A red, yellow or green fruit.
         * 
         */
        APPLE,
    
        /**
         * A yellow fruit.
         * 
         */
        BANANA,
    
        /**
         * An orange fruit.
         * 
         */
        ORANGE,
    
        /**
         * A yellow fruit.
         * 
         */
        PINEAPPLE,
        $UNKNOWN;
    }
    ```

**Note:** Due to the addition of doclint in JDK8, anything under the
`symbolDocs` attribute must be W3C HTML 4.01 compliant. This is because
the contents of this string will appear as Javadocs in the generated
Java ‘data template’ classes later. Please take this into consideration
when writing your documentation.

## Fixed Type

The code generator generates a class that extends
`com.linkedin.data.template.FixedTemplate`. This class provides the
following
methods:

| Method <br />                     | Implemented by <br />  | Description <br />                                                                                                                   |
| --------------------------------- | ---------------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| *Constructor*(String arg) <br />  | Generated class <br /> | Construct with an instance whose value is provided by the input string representing the bytes in the fixed. <br />                   |
| *Constructor*(Object obj) <br />  | Generated class <br /> | Construct with an instance whose value is provided by the input string representing the bytes in the fixed or a `ByteString`. <br /> |
| `ByteString bytes()` <br />       | Base class <br />      | Returns the bytes of the fixed type. <br />                                                                                          |
| `FixedDataSchema schema()` <br /> | Generated class <br /> | Returns the `DataSchema` of the instance. The size of the fixed type can be obtained from this schema. <br />                        |
| `Object data()` <br />            | Base class <br />      | Returns the underlying data of the instance. This is the same as bytes(). <br />                                                     |
| `String toString()` <br />        | Base class <br />      | Returns the string representation of the bytes in the instance. <br />                                                               |

A fixed instance is immutable once constructed.

## Array Type

The code generator generates a class that extends
`com.linkedin.data.template.DirectArrayTemplate<E>` or
`com.linkedin.data.template.WrappingArrayTemplate<E extends
DataTemplate<?>>`. The latter is used for item types whose Java binding
require wrapping. The former is used for items types whose Java binding
that do not require wrapping. The `E` generic type variable is the Java
class of the array’s item type. By creating a concrete subclass per map
type, this binding avoids lost of item type information due to Java
generics type erasure.

The primary characteristics of both base classes are as follows:

  - They implement `java.util.List<E>`.
  - Their methods perform runtime checks on updates and inserts to
    ensure that the value types of arguments are either the exact type
    specified by the type variable or types that can be coerced to the
    specified type.

The methods with more specific behavior are described
below.

| Method                                 | Implemented by <br />  | Description <br />                                                                                                                                                                                                                                |
| -------------------------------------- | ---------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| *Constructor*()                        | Generated class <br /> | Constructs an empty array. <br />                                                                                                                                                                                                                 |
| *Constructor*(int initialCapacity)     | Generated class        | Constructs an empty array with the specified initial capacity. <br />                                                                                                                                                                             |
| *Constructor*(Collection<E> c)         | Generated class        | Constructs an array by inserting each element of the provided collection into the constructed array. <br />                                                                                                                                       |
| *Constructor*(DataList list)           | Generated class        | Constructs an array that wraps the provided `DataList`. <br />                                                                                                                                                                                    |
| `ArrayDataSchema schema()`             | Generated class        | Returns the `DataSchema` of the instance. The schema of the items in the array can be obtained from this schema.                                                                                                                                  |
| `int hashCode()` <br />                | Base class <br />      | Returns the `hashCode()` of the underlying `DataList` wrapped by this instance. <br />                                                                                                                                                            |
| `boolean equals(Object object)` <br /> | Base class <br />      | If object is an instance of `AbstractArrayTemplate`, invoke equals on the underlying `DataList` of this instance and the object’s underlying `DataList`. Otherwise, invoke `super.equals(object)` which is `AbstractMap` ’s equals method. <br /> |
| `String toString()` <br />             | Base class <br />      | Returns the result of calling `toString()` on the underlying `DataList` wrapped by this instance. <br />                                                                                                                                          |
| *java.util.List methods*               | Base class <br />      | See `java.util.List`. <br />                                                                                                                                                                                                                      |

## Map Type

The code generator generates a class that extends
`com.linkedin.data.template.DirectMapTemplate<E>` or
`com.linkedin.data.template.WrappingMapTemplate<E extends
DataTemplate<?>>`. The latter is used for item types whose Java binding
require wrapping. The former is used for items types whose Java binding
that do not require wrapping. The `E` generic type variable is the Java
class of the map’s value type. By creating a concrete subclass per map
type, this binding avoids lost of value type information due to Java
generics type erasure.

The primary characteristics of both base classes are as follows:

  - They implement `java.util.Map<String, E>`.
  - Their methods perform runtime checks on updates and inserts to
    ensure that the value types of arguments are either the exact type
    specified by the type variable or are types that can be coerced to
    the specified type.

The methods with somewhat specialized behavior are described
below.

| Method                                               | Implemented by <br />  | Description <br />                                                                                                                                                                                                                          |
| ---------------------------------------------------- | ---------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| *Constructor*()                                      | Generated class <br /> | Constructs an empty map. <br />                                                                                                                                                                                                             |
| *Constructor*(int initialCapacity)                   | Generated class        | Constructs an empty map with the specified initial capacity. <br />                                                                                                                                                                         |
| *Constructor*(int initialCapacity, float loadFactor) | Generated class        | Constructs an empty map with the specified initial capacity and load factor. <br />                                                                                                                                                         |
| *Constructor*(Map\<String, E\> c)                    | Generated class        | Constructs a map by inserting each entry of the provided map into new instance. <br />                                                                                                                                                      |
| *Constructor*(DataMap map)                           | Generated class        | Constructs an array that wraps the provided `DataMap`. <br />                                                                                                                                                                               |
| `MapDataSchema schema()`                             | Generated class        | Returns the `DataSchema` of the instance. The schema of the values in the map can be obtained from this schema.                                                                                                                             |
| `int hashCode()` <br />                              | Base class <br />      | Returns the `hashCode()` of the underlying `DataMap` wrapped by this instance. <br />                                                                                                                                                       |
| `boolean equals(Object object)` <br />               | Base class <br />      | If object is an instance of `AbstractMapTemplate`, invoke equals on the underlying `DataMap` of this instance and the object’s underlying `DataMap`. Otherwise, invoke super.equals(object) which is `AbstractMap` ’s equals method. <br /> |
| String toString() <br />                             | Base class <br />      | Returns the result of calling `toString()` on the underlying `DataMap` wrapped by this instance. <br />                                                                                                                                     |
| *java.util.Map methods*                              | Base class <br />      | See `java.util.Map`. <br />                                                                                                                                                                                                                 |

## Record Type

The code generator generates a class that extends
`com.linkedin.data.template.RecordTemplate`. This class provides the
following
methods:

| Method <br />                      | Implemented by <br />  | Description <br />                                                                                                                                          |
| ---------------------------------- | ---------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| *Constructor*() <br />             | Generated class <br /> | Construct instance that wraps an empty `DataMap`. Even mandatory fields are not present. <br />                                                             |
| *Constructor*(DataMap map) <br />  | Generated class        | Construct instance that wraps the provided `DataMap`. Method invocations on the `RecordTemplate` translates to accesses to the underlying `DataMap`. <br /> |
| `RecordDataSchema schema()` <br /> | Generated class        | Returns the `DataSchema` of this instance. The fields of the record can be obtained from this schema. <br />                                                |
| `static Fields fields()` <br />    | Generated class        | Returns a generated `Fields` class that provides identifiers for fields of this record and certain nested types. See Fields section below. <br />           |
| `DataMap data()` <br />            | Base class             | Returns the underlying `DataMap` wrapped by this instance. <br />                                                                                           |
| `String toString()` <br />         | Base class             | Equivalent to `data().toString()`. <br />                                                                                                                   |

The code generator generates the following methods in the generated
class for each field. *FieldName* is the name of field with the first
character
capitalized.

| Method                                  | Description <br />                                                                                                                                                                                                                                                                    |
| --------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `boolean hasFieldName()` <br />         | Returns whether the field is present in the underlying `DataMap`. <br />                                                                                                                                                                                                              |
| `void removeFieldName()` <br />         | Removes the field from the underlying `DataMap`. <br />                                                                                                                                                                                                                               |
| `T getFieldName(GetMode mode)` <br />   | Returns the value of the field. The mode parameter allows the client to specify the desired behavior if the field is not present. `T` is the Java type of the field.                                                                                                                  |
| `T getFieldName()` <br />               | Returns the value of the field. This is equivalent to `getFieldName(GetMode.STRICT)`. `T` is the Java type of the field. <br />                                                                                                                                                       |
| `R setFieldName(T value, SetMode mode)` | Sets the specified value into the field. The mode parameter allows the client to specify the desired behavior if the provided value is null. Returns this. `R` is the generated Java class. `T` is the Java type of the field. <br />                                                 |
| `R setFieldName(T value)` <br />        | Sets the specified value into the field. This is equivalent to `setFieldName(value, SetMode.DISALLOW_NULL)`. Returns this. `R` is the generated Java class. `T` is the native type rather than the corresponding boxed type where applicable, e.g. `int` instead of `Integer`. <br /> |

<a name="GetMode"></a>

### GetMode

When getting a field from a record, the caller must specify the behavior
of the function in case the requested field does not exist in the
record.

The available GetModes are:

  - `NULL`  
    If the field is present, then return the value of the field. If the
    field is not present, then return null (even if there is a default
    value).
  - `DEFAULT`  
    If the field is present, then return the value of the field. If the
    field is not present and there is a default value, then return the
    default value. If the field is not present and there is no default
    value, then return null.
  - `STRICT`  
    If the field is present, then return the value of the field.  
    If the field is not present and the field has a default value, then
    return the default value.  
    If the field is not present and the field is not optional, then
    throw
    `com.linkedin.data.template.RequiredFieldNotPresentException`.  
    If the field is not present and the field is optional, then return
    null.

### SetMode

When setting a field in a record, the caller must specify the behavior
of the function in case the field is attempted to be set to `null`.

The available SetModes are:

  - `IGNORE_NULL`  
    If the provided value is null, then do nothing i.e. the value of the
    field is not changed. The field may or may be present.
  - `REMOVE_IF_NULL`  
    If the provided value is null, then remove the field. This occurs
    regardless of whether the field is optional.
  - `REMOVE_OPTIONAL_IF_NULL`  
    If the provided value is null and the field is an optional field,
    then remove the field. If the provided value is null and the field
    is a mandatory field, then throw
    `java.lang.IllegalArgumentException`.
  - `DISALLOW_NULL`  
    The provided value cannot be null. If the provided value is null,
    then throw `java.lang.NullPointerException`.

<!-- end list -->

    ```
    package com.linkedin.pegasus.generator.examples;
    
    ...
    
    public class Foo extends RecordTemplate
    {
        public Foo() ...
        public Foo(DataMap data) ...
        ...
    
        // intField - field of int type
        public boolean hasIntField() ...
        public void removeIntField() ...
        public Integer getIntField(GetMode mode) ...
        public Integer getIntField() { return getIntField(GetMode.STRICT); }
        public Foo setIntField(int value) { ... ; return this; }
        public Foo setIntField(Integer value, SetMode mode { ... ; return this; }
        ...
    
        // bytesField - field of bytes, Java binding for bytes is ByteString
        public boolean hasBytesField() ...
        public void removeBytesField() ...
        public ByteString getBytesField(GetMode mode) { return getBytesField(GetMode.STRICT); }
        public ByteString getBytesField() ...
        public Foo setBytesField(ByteString value) { ... ; return this; }
        public Foo setBytesField(ByteString value, SetMode mode) { ... ; return this; }
        ...
    
        // fruitsField - field of enum
        public boolean hasFruitsField() ...
        public void removeFruitsField() ...
        public Fruits getFruitsField(GetMode mode) ...
        public Fruits getFruitsField() { return getFruitsField(GetMode.STRICT); }
        public Foo setFruitsField(Fruits value) { ... ; return this; }
        public Foo setFruitsField(Fruits value, SetMode mode) { ... ; return this; }
        ...
    
        // intArrayField - field of { "type" : "array", "items" : "int" }
        public boolean hasIntArrayField() ...
        public void removeIntArrayField() ...
        public IntegerArray getIntArrayField(GetMode mode) ...
        public IntegerArray getIntArrayField() { return getIntArrayField(GetMode.STRICT); }
        public Foo setIntArrayField(IntegerArray value) { ... ; return this; }
        public Foo setIntArrayField(IntegerArray value, SetMode mode) { ... ; return this; }
    
        // stringMapField - field of { "type" : "map", "values" : "string" }
        public boolean hasStringMapField() ...
        public void removeStringMapField() ...
        public StringMap getStringMapField(GetMode mode) ...
        public StringMap getStringMapField() { return getIntStringMapField(GetMode.STRICT); }
        public Foo setStringMapField(StringMap value) { ... ; return this; }
        public Foo setStringMapField(StringMap value, SetMode mode) { ... ; return this; }
        ...
    
        // unionField - field of union
        public boolean hasUnionField() ...
        public void removeUnionField() ...
        public Foo.UnionField getUnionField(GetMode mode) ...
        public Foo.UnionField getUnionField() { return getUnionField(GetMode.STRICT); }
        public Foo setUnionField(Foo.UnionField value) { ... ; return this; }
        public Foo setUnionField(Foo.UnionField value, SetMode mode) { ... ; return this; }
    
        // get fields
        public static Foo.Fields fields() {
            ...;
        }
    
        public static class Fields
            extends PathSpec
        {
            ...
            public PathSpec intField() { ... }
            public PathSpec longField() { ... }
            public PathSpec bytesField() { ... }
            public PathSpec fruitsField() { ... }
            public PathSpec intArrayField() { ... }
            public PathSpec stringMapField() { ... }
            public Foo.UnionField.Fields unionField() { ... }
        }
    }
    ```

## Error Type

Error types are specialized record types. The code generator generates a
class that extends `com.linkedin.data.template.ExceptionTemplate`. The
generated class has the same methods as a generated class for a record
with the same fields. Unlike `RecordTemplate` instances,
`ExceptionTemplate` instances can be thrown and caught.
`ExceptionTemplate` extends `java.lang.Exception`.

## Union Type

The code generator generates a class that extends
`com.linkedin.data.template.UnionTemplate`. This class provides the
following methods:

<table>
<thead>
<tr class="header">
<th>Method <br /></th>
<th>Implemented by <br /></th>
<th>Description <br /></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><em>Constructor</em>() <br /></td>
<td>Generated class <br /></td>
<td>Construct a union with null as it value. An instance with null as its value cannot be assigned another value. <br /></td>
</tr>
<tr class="even">
<td><em>Constructor</em>(DataMap map) <br /></td>
<td>Generated class</td>
<td>If the argument is null or ```Data.NULL```, then construct a union with a null value. <br /><br />
If the argument is not null, then construct a union whose value is provided by the ```DataMap```. Method invocations on the ```UnionTemplate``` translates to accesses to the underlying ```DataMap```. <br /><br />
An instance with null as its value cannot be assigned another value. <br /><br />
An instance that has a non-null value cannot be later assigned a null value. <br /><br />
Note: This limitation is because the underlying data types that back the union for null verus non-null values are different. For non-null values, the underlying data type is a ```DataMap```. For null values, the underlying data type is a string. <br /></td>
</tr>
<tr class="odd">
<td>```UnionDataSchema schema()``` <br /></td>
<td>Generated class</td>
<td>Returns the ```DataSchema``` of the instance. The members of the union can be obtained from this schema. <br /></td>
</tr>
<tr class="even">
<td>```DataScheme memberType()``` <br /></td>
<td>Base class <br /></td>
<td>Returns ```DataSchemaConstants.NULL_TYPE``` if the union has a null value, else return the schema for the value. <br /><br />
If the schema cannot be determined, then throw ```TemplateOutputCastException```. The schema cannot be determined if the content of the underlying ```DataMap``` cannot be resolved to a known member type of the union schema. See serialization format for details. This exception is thrown if the ```DataMap``` has more than one entry and the key of the only entry does not identify one of the member types of the union. <br /></td>
</tr>
<tr class="odd">
<td>```boolean memberIs(String key)``` <br /></td>
<td>Base class <br /></td>
<td>Returns whether the union member key of the current value is equal the specified key. The type of the current value is identified by the specified key if the underlying ```DataMap``` has a single entry and the entry’s key equals the specified key. <br /></td>
</tr>
<tr class="even">
<td>```boolean isNull()``` <br /></td>
<td>Base class <br /></td>
<td>Returns whether the value of the union is null. <br /></td>
</tr>
<tr class="odd">
<td>```Object data()``` <br /></td>
<td>Base class</td>
<td>Returns ```Data.NULL``` if the union has a null value, else return the underlying ```DataMap``` fronted by the instance. <br /></td>
</tr>
<tr class="even">
<td>```String toString()``` <br /></td>
<td>Base class</td>
<td>Equivalent to ```data().toString()```. <br /></td>
</tr>
</tbody>
</table>

The code generator generates the following methods in the generated
class for each member type of the union. In the following table,
*MemberKey* is either the member’s alias (if specified) or the member’s
non-fully qualified type name with the first character
capitalized.

| Method                                  | Description <br />                                                                                                                                                                                                 |
| --------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `U createWithMemberKey(T value)` <br /> | Create a union instance with the specified value for the member identified by *MemberKey*. <br />                                                                                                                  |
| `boolean isMemberKey()` <br />          | Returns whether the value of the union is of the member identified by the *MemberKey*. <br />                                                                                                                      |
| `T getMemberKey()` <br />               | Returns the value of the union if it is for the member identified by *MemberKey*. `T` is the Java type of the value and if the current value is not of this type, then throw `TemplateOutputCastException`. <br /> |
| `void setMemberKey(T value)` <br />     | Sets the specified value into the union. <br />                                                                                                                                                                    |

Here is an example generated class for a union who’s members are not
aliased.

    ```
    package com.linkedin.pegasus.examples;
    
    ...
    public class Foo extends RecordTemplate
    {
        ...
        public final static class UnionField extends UnionTemplate
        {
            public UnionField() ...
            public UnionField(Object data) ...
    
            // int value
            public boolean isInt() ...
            public Integer getInt() ...
            public void setInt(Integer value) ...
    
            // string value
            public boolean isString() ...
            public String getString() ...
            public void setString(String value) ...
    
            // com.linkedin.pegasus.generator.examples.Fruits enum value
            public boolean isFruits() ...
            public Fruits getFruits() ...
            public void setFruits(Fruits value) ...
    
            // com.linkedin.pegasus.generator.examples.Foo record value
            public boolean isFoo() ...
            public Foo getFoo() ...
            public void setFoo(Foo value) ...
    
            // array value ({ "type" : "array", "items" : "string" })
            public boolean isArray() ...
            public StringArray getArray() ...
            public void setArray(StringArray value) ...
    
            // map value ({ "type" : "map", "values" : "long" })
            public boolean isMap() ...
            public LongMap getMap() ...
            public void setMap(LongMap value) ...
        }
    
        public static class Fields extends PathSpec
        {
            ...
            public Foo.Fields Foo() { ... }
        }
    }
    ```

For a union who’s members are aliased, the generated methods will use
the alias instead of the member’s type name like illustrated below.

    ```
    package com.linkedin.pegasus.examples;
    
    ...
    public class Foo extends RecordTemplate
    {
        ...
        public final static class UnionField extends UnionTemplate
        {
            public UnionField() ...
            public UnionField(Object data) ...
    
            // int with alias ({ "type" : "int", "alias" : "count" })
            public UnionField createWithCount(Integer value) ...
            public boolean isCount() ...
            public Integer getCount() ...
            public void setCount(Integer value) ...
    
            // string with alias ({ "type" : "string", "alias" : "message" })
            public UnionField createWithMessage(String value) ...
            public boolean isMessage() ...
            public String getMessage() ...
            public void setMessage(String value) ...
    
            // another string with alias ({ "type" : "string", "alias" : "greeting" })
            public UnionField createWithGreeting(String value) ...
            public boolean isGreeting() ...
            public String getGreeting() ...
            public void setGreeting(String value) ...
        }
    
        public static class Fields extends PathSpec
        {
            ...
            public Foo.Fields Foo() { ... }
        }
    }
    ```

## Custom Java Class Binding for Primitive Types

A typeref can also be used to define a custom Java class binding for a
primitive type. The primary intended use is to provide a more developer
friendly experience by having the framework perform conversions from
primitive type to a more friendly Java class that can implement methods
for manipulating the underlying primitive data. Custom Java class
binding also provides additional type-safety by allowing typerefs of the
same primitive type to be bound to different custom Java classes. This
enables compile time type-checking to disambiguate typeref’s, e.g. a Urn
typeref to a string can be bound to a different Java class than a
FileName typeref to a string.

When a typeref has a custom Java binding, the generated Java data
templates that reference this type will accept and return parameters of
the custom Java class instead of standard Java class for the primitive
type. The value stored in the underlying DataMap or DataList will always
be of the corresponding primitive Java type (not the custom Java type.)

A custom Java class binding is declared by:

  - defining a typeref of the primitive type,
  - adding a “java” attribute whose value is a map to the typeref
    declaration,
  - adding a “class” attribute to the “java” map whose value is a string
    that identifies the name of custom Java class.

A custom class must meet the following requirements:

1.  Instances of the custom class must be immutable.
2.  A coercer must be defined that can coerce the primitive Java class
    of the type to the custom Java class of the type, in both the input
    and output directions. The coercer implements the `DirectCoercer`
    interface.
3.  An instance of the coercer must be registered with the data template
    framework.

<!-- end list -->

    ```
    {
      "type" : "typeref",
      "name" : "CustomPoint",
      "ref"  : "string",
      "java" : {
        "class" : "CustomPoint"
      }
    }
    ```

    ```
    /
    // The custom class
    // It has to be immutable.
    //
    public class CustomPoint
    {
      private int _x;
      private int _y;
    
      public CustomPoint(String s)
      {
        String parts[] = s.split(",");
        _x = Integer.parseInt(parts"0":0);
        _y = Integer.parseInt(parts"1":1);
      }
    
      public CustomPoint(int x, int y)
      {
        _x = x;
        _y = y;
      }
    
      public int getX()
      {
        return _x;
      }
    
      public int getY()
      {
        return _y;
      }
    
      // Implement equals, hashCode, toString, ...
    
      //
      // The custom class's DirectCoercer.
      //
      public static class CustomPointCoercer implements DirectCoercer<CustomPoint>
      {
        @Override
        public Object coerceInput(CustomPoint object)
          throws ClassCastException
        {
          return object.toString();
        }
    
        @Override
        public CustomPoint coerceOutput(Object object)
          throws TemplateOutputCastException
        {
          if (object instanceof String == false)
          {
            throw new TemplateOutputCastException("Output " + object + 
                                                  " is not a string, and cannot be coerced to " + 
                                                  CustomPoint.class.getName());
          }
          return new CustomPoint((String) object);
        }
      }
    
      //
      // Automatically register Java custom class and its coercer.
      //
      static
      {
        Custom.registerCoercer(CustomPoint.class, new CustomPointCoercer());
      }
    }
    ```

## Fields class

The code generator also generates a `Fields` class within the generated
class for certain complex types. The primary use case for the `Fields`
class is to provide a type-safe way to refer or identify a field within
a record or a member of a union. The `Fields` class of a record is
accessed through the generated `fields()` method of the generated class
for a record. Only record types have the generated `fields()` method.

If there are nested complex types, a path to a particular nested field
may be obtained by chaining method invocations on `Fields` classes along
the path to the field, e.g. `Foo.fields().barField().bazField()`. The
path may be used to specify the nested fields to return from a resource
request, i.e. deep projection.

The following table summarizes which complex types will have a generated
`Fields` class and the content of the `Fields`
class.

| Complex type <br /> | Whether type will have a generated Fields class <br />                                                           | Content of generated Fields class <br />                                               |
| ------------------- | ---------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------- |
| record <br />       | A `Fields` class always generated. <br />                                                                        | A method returning a `PathSpec` will be generated for each field of the record. <br /> |
| union <br />        | A `Fields` class always generated. <br />                                                                        | A method returning a `PathSpec` will be generated for each member of the union. <br /> |
| array <br />        | A `Fields` class will be generated if the array directly or indirectly contains a nested record or union. <br /> | An `items()` method returning a `PathSpec` will be generated for the array. <br />     |
| map <br />          | A `Fields` class will be generated if the map directly or indirectly contains a nested record or union. <br />   | A `values()` method returning a `PathSpec` will be generated for the map.              |

## Clone Method

For the classes that wrap `DataMap` or `DataList`, their clone method
will clone the underlying `DataMap` or `DataList` and then create and
return a new `DataTemplate` of the same class to wrap the clone. This
clone operation performs a shallow copy.

## Escaping for Reserved Words

When symbols such as schema names or enum symbol names are the same as
one of the reserved words in Java, the code generator will escape these
symbols by appending an underscore (“_”) to the name to obtain the Java
name of the symbol.

## Exceptions

The Data layer can throw two exceptions:

  - `java.lang.ClassCastException` - This exception is thrown if the
    input argument to a method is not the expected type, cannot be cast
    or coerced to the expected type.
  - `com.linkedin.data.TemplateOutputCastException` - This exception if
    the underlying data cannot be wrapped, cast or coerced to the type
    to type of the output argument.

<a name="RunningtheCodeGenerator"></a>

## Running the Code Generator

The code generator that generates the Java bindings is the
`com.linkedin.pegasus.generator.PegasusDataTemplateGenerator` class.

The arguments to the main method of this class are `targetDirectoryPath
[sourceFile or schemaName]+`".

  - `targetDirectoryPath` provides the root of the output directory for
    Java source files generated by the code generator. The output
    directory structure will follow the Java convention, with Java
    source files residing in sub-directories corresponding to the
    package name of the classes in the Java source files.
  - `sourceFile` provides the name of a file. Files containing schemas
    should have an `.pdsc` extension. Although a file name provided as
    an argument to the code generator need not end with `.pdsc`, only
    `.pdsc` files will be read by the schema resolver when trying to
    resolve a name to schema. Java type: `String`.
  - `schemaName` provides the fully qualified name of a schema. The
    schema resolver computes a relative path name from this argument and
    enumerate through resolver paths to locate a file with this relative
    name. If a file is found, the code generator will parse the file
    looking for a schema with the specified name. Java type: `String[]`.

The resolver path is provided by the “generator.resolver.path” property.
Its format is the same as the format for Java classpath. Each path is
separated by a colon (“:”). Only file system directory paths may be
specified (i.e. the resolver does not comprehend `.jar` files in the
resolver path.). You can set this in java by `System.setProperty()`.

The dependencies of the code generator are:

  - `com.sun.codemodel:codemodel:2.2`
  - `org.codehaus.jackson-core-asl:jackson-core-asl:1.4.0`
  - `com.linkedin.pegasus:cow`
  - `com.linkedin.pegasus:r2`
  - `com.linkedin.pegasus:generator`

### Running the Code Generator from Command Line

### Running the Code Generator with Gradle

A `dataTemplate.gradle` script is available in the `build_script/`
directory of pegasus. To use it, add the script to your project, then
add this to your `build.gradle` file:

    ```
    apply from: "${buildScriptDirPath}/dataTemplate.gradle"
    ```

and put the `.pdsc` files in a directory structure of the form:
‘src/\\\<sourceset\\\>/pegasus’, where typically it would be
`src/main/pegasus`. The plugin is set to trigger on this sort of
directory structure and have the files laid out like a java source tree,
ie if the namespace of my foo schema is “com.linkedin.foo.rest.api”, the
file would be located at
`src/main/pegasus/com/linkedin/foo/rest/api/Foo.pdsc`. See
`restli-example-api/build.gradle` in the Pegasus codebase for an
example. This script will generate the required Java classes before the
compileJava task, so that other classes can refer to it.

Note this will only generate the data templates, but further steps will
be needed to generate the rest.li IDL and the clientModel.

# Avro Translation

## Translating Schemas to and from Avro

The schema and data translators inspect your classpath to determine
which version of avro you are using and require you have the matching
pegasus \`data-avro-<avro_version>\` adapter module in your classpath.

For example, if you are using avro 1.6, you must add a dependency on the
pegasus \`data-avro-1_6\` module:

```  
com.linkedin.pegasus:data-avro-1_6:<current-version>  
```

If you are using avro 1.4, it’s adaptor module is included by default so
you don’t need to depend on it explicitly.

Schema translation is implemented by the
\`com.linkedin.data.avro.SchemaTranslator\` class.

For example, to convert from a avro schema, do:

```  
DataSchema pegasusDataSchema =
SchemaTranslator.avroToDataSchema(avroSchema, options);  
```

And to convert to an avro schema, do:

```  
Schema avroSchema = SchemaTranslator.dataToAvroSchema(pegasusDataSchema,
options);  
```

## Translating Data to and from Avro

Data translation is implemented by the
\`com.linkedin.data.avro.DataTranslator\` class. Translating data
requires that one has schemas for both formats (.avsc and .pdsc). Please
see above section section about translating schemas for details. Once
both schemas are available, data can be converted.

For example, to convert avro data, do:

```  
DataTranslator.dataMapToGenericRecord(data, pegasusDataSchema,
avroSchema); // for dataMaps  
// OR  
GenericRecord avroRecord =
DataTranslator.dataMapToGenericRecord(recordTemplate.data(),
recordTemplate(), avroSchema); // for record templates  
```

And to convert from avro data, do:  
```  
DataMap pegasusData = genericRecordToDataMap(avroRecord,
pegasusDataSchema, avroSchema);  
```
