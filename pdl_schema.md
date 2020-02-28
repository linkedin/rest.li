---
layout: guide
title: PDL Schema
permalink: /pdl_schema
redirect_from: /pdl_syntax
excerpt: Documentation of Pegasus schemas and specification of the PDL syntax.
---

# PDL Schema

## Contents
-   [PDL Schema Definition](#pdl-schema-definition)
-   [Creating a Schema](#creating-a-schema)
-   [Record Type](#record-type)
-   [Primitive Types](#primitive-types)
-   [Enum Type](#enum-type)
-   [Array Type](#array-type)
-   [Map Type](#map-type)
-   [Union Type](#union-type)
-   [Typeref](#typeref)
-   [Fixed Type](#fixed-type)
-   [Imports](#imports)
-   [Properties](#properties)
-   [Package](#package)
-   [Escaping](#escaping)

## PDL Schema Definition

Pegasus is a schema definition and serialization system developed as part of
the Rest.li framework. It provides [multi-language
support](https://linkedin.github.io/rest.li/multi_language_compatibility_matrix)
for services built using Rest.li and handles seemless serialization and
deserialization of data between server and clients.

PDL is a schema definition language for Pegasus, developed
as a user friendly and concise format replacement for the older JSON based
[PDSC schema format](https://linkedin.github.io/rest.li/pdsc_syntax).

## Creating a Schema

Pegasus supports different types of schemas: [Records](#record-type),
[Primitive types](#primitive-types), [Enums](#enum-type), [Arrays](#array-type),
[Maps](#map-type), [Unions](#union-type), [Fixed](#fixed-type) and
[Typerefs](#typerefs). Records, Enums and Typerefs have names (Named schemas)
and thus can be defined as top-level schemas. Named schemas can specify an
optional namespace to avoid naming conflict between schemas with same name.
The name prefixed with the namespace using the dot(`.`) separator becomes the
fully qualified name (FQN) of the schema. Named schemas can be
referenced from other schemas using the fully qualified name.

Each top-level schema should be stored in its own file with a `.pdl` extension.
Name of the file should match the schema name and the directory structure should
match the namespace (similar to how Java classes are organized).

The Pegasus code generator implements a resolver that is similar to Java class
loaders. If there is a reference to a named schema, the code generator will try
to look for a file in the code generator’s resolver path. The resolver path is
similar to a Java classpath. The fully qualified name of the named schema will
be translated to a relative file name. The relative file name is computed by
replacing dots (`.`) in the fully qualified name by the directory path separator
(typically `/`) and appending a `.pdl` extension. This relative file name looked
up using each location in the resolver path until it finds a file that contains
the named schema.


## Record Type
Records are the most common type of Pegasus schemas and usually the starting
point when defining data models using Pegasus. A record represents a Named
entity with fields representing attributes of that entity. The fields can be
primitive types, enums, unions, maps, arrays, other records or any valid Pegasus
type.

For example:
```pdl
namespace com.example.time

record Date {
  day: int
  month: int
  year: int
}
```
The above example is defining a record called `Date`. This is defined in the
namesapce `com.example.time`, giving it the fully qualified name
`com.example.time.Date`. So this schema should be defined in the following file
```
<project-dir>/pegasus/com/example/time/Date.pdl
```

The `Date` record can then be referenced in other schemas:
```pdl
namespace com.example.models

import com.example.time.Date

record User {
  firstName: string
  birthday: Date
}
```
The above example is defining a record called `User`:
* This record is using the namespace `com.example.models`. So the fully
 qualified name is `com.example.models.User`.
* This record is defining two fields, `firstName` and `birthday`.
 * `firstName` is a primitive field of type `string`.
 * `birthday` references the type `com.example.time.Date` from the first example
* PDL supports Java like `import` feature that allows you to define references
 to external types and then use them in the schema using their simple name.

### Record Field attributes
Pegasus supports additional features for defining the behavior of fields in a
record.
#### Record fields can be defined as optional.
In Pegasus, a field is required unless the field is explicitly declared as
optional using the `optional` keyword. An optional field may be present or
absent in the in-memory data structure or serialized data.

In the generated client bindings, Pegasus provides has[Field] methods(eg,
`hasBirthDay()`) to determine if an optional field is present.

For example:
```pdl
namespace com.example.models

import com.example.time.Date

record User {
  firstName: string
  birthday: optional Date
}
```
The above example defines the `birthday` field as optional.

#### Record fields may have default values.
Pegasus supports specifying default values for fields. Though the definition
language allows default values for both required and optional fields, it is
recommended to use default values only for required fields.

The default value for a field is expressed as a JSON value confirming to the
type of the field.

In Pegasus generated bindings, the get[Field] accessors will return the value of
the field it is present or the default value from the schema if the field is
absent. The bindings also provide a specialized get accessor that allows the
caller to specify whether the default value or null should be returned when an
absent field is accessed.

See [GetMode](https://linkedin.github.io/rest.li/java_binding#getmode) for more
details on accessing optional fields and default values.

For example:
```pdl
namespace com.example.models

import org.example.time.Date

record User {
  firstName: string
  birthday: optional Date
  isActive: boolean = true
}
```
The above example defines a boolean field `isActive`, which has a default value
`true`.

### Inlined schemas
In Pegasus, records and other named schemas need not be top-level schemas. They
can be inlined within other record schemas.

For example:
```pdl
namespace com.example.models

import com.example.time.Date

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
`Address` record in the above example is inlined. It inherits the namespace of
the parent record `User`, making its fully qualified name `com.example.models.Address`.

* Namespace of inline types can be specified/overriden by defining a Namespace
surrounding the inlined type.
* The default value of fields using inlined types can be expressed using its
serialized JSON representation.

For example:
```pdl
namespace com.example.models

import com.example.time.Date

record User {
  firstName: string
  birthday: optional Date
  isActive: boolean = true
  address: {
    namespace com.example.models.address

    record Address {
      state: string
      zipCode: string
    }
  } = {
    "state": "CA",
    "zipCode": "12345"
  }
}
```
***Note:*** If a record or a named schema is referenced by other schemas, it should
be a top-level schema. Referencing in-line schemas outside the schema in which
they are defined is not allowed.

### Doc Strings

Pegasus types and fields may be documented using “doc strings” following the
Java style comments.
* Comments using `/** */` syntax are treated as schema documentation. They will
be included in the in-memory representation and the generated binding classes.
* Comments using `/* */` or `//` syntax are allowed but not treated as schema
documentation.

For example:
```pdl
namespace com.example.models

import com.example.time.Date

/**
 * A record representing an user in the system.
 */
record User {
  /** First name of the user */
  firstName: string

  /** User's birth day */
  birthday: optional Date

  // TODO: Can this be an enum?
  /** Status of the user. */
  isActive: boolean = true
}
```

### Deprecation
Pegasus supports marking types or fields as deprecated by adding `@deprecated`
annotation. The deprecation details from the schema will be carried over to the
bindings generated in  different languages.
In Java language, the classes, getter and setter methods generated for
deprecated types will be marked as deprecated.

It is recommended to specify a string describing the reason for deprecation and
the alternative as the value for the `@deprecated` annotation.

Deprecate a field:
```pdl
namespace com.example.models

import com.example.time.Date

record User {
  firstName: string

  @deprecated = "Use birthday instead."
  birthYear: int

  birthday: Date  
}
```
Deprecate a record:
```pdl
namespace com.example.models

@deprecated = "Use Person type instead."
record User {
  firstName: string
}
```

See [Enum documentation and deprecation](#enum-documentation-and-deprecation)
for details on deprecating enum symbols.

### Including fields
Pegasus records support including fields from one or more other records. When
a record is included, all its fields will be included in the current record. It
does not include any other attribute of the other record.

Includes are transitive, if record A includes record B and record B includes
record C, record A contains all the fields declared in record A, record B and
record C.

The value of the “include” attribute should be a list of records or typerefs of
records. It is an error to specify non-record types in this list.

For example:
```pdl
namespace com.example.models

/**
 * User includes fields of AuditStamp, User will have fields firstName from
 * itself and fields createdAt and updatedAt from AuditStamp.
 */
record User includes AuditStamp {
  firstName: string
}
```

```pdl
namespace com.example.models

/**
 * A common record to represent audit stamps.
 */
record AuditStamp {
  /** Time in milliseconds since epoch when this record was created */
  createdAt: long

  /** Time in milliseconds since epoch when this record was last updated */
  updatedAt: long
}
```

Includes feature allows including fields from multiple records.

For example:
```pdl
namespace com.example.models

/** A common record for specifying version tags of a record */
record VersionTag {
  versionTag: string
}
```

```pdl
namespace com.example.models

/**
 * User includes fields of AuditStamp, User will have fields firstName from
 * itself and fields createdAt and updatedAt from AuditStamp.
 */
record User includes AuditStamp, VersionTag {
  firstName: string
}
```

***Note:*** Record inclusion does not imply inheritance, it is merely a
convenience to reduce duplication when writing schemas.


## Primitive Types
The above examples already introduced `int`, `long`, `string` and `boolean`
primitive types that are supported by Pegasus.
The full list of supported Pegasus primitive types are: `int`, `long`, `float`,
`double`, `boolean`, `string` and `bytes`.

The actual types used for the primitives depends on the language specific
binding implementation. For details on Java bindings for Pegasus primitives, see
[Primitive Types](https://linkedin.github.io/rest.li/java_binding#primitive-types)

Primitive types cannot be named (except through [typerefs](#typerefs)) and thus
cannot be defined as top-level schemas.

Some examples showing how different primitive fields can be defined and the
syntax for specifying default values:
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

## Enum Type

Enums, as the name suggests contains an enumeration of symbols. Enums are named
schemas and can be defined as top-level schema or inlined.

For example:
```pdl
namespace com.example.models

enum UserStatus {
  ACTIVE
  SUSPENDED
  INACTIVE
}
```

Enums can be referenced by name in other schemas:
```pdl
namespace com.example.models

record User {
  firstName: string
  status: UserStatus
}
```

Enums can also be defined inline:
```pdl
namespace com.example.models

record User {
  firstName: string
  status: UserStatus
  suspendedReason: enum StatusReason {
    FLAGGED_BY_SPAM_CHECK
    REPORTED_BY_ADMIN
  }
}
```

### Enum documentation and deprecation
Doc comments and deprecation can be added directly to individual enum symbols.

For example:
```pdl
namespace com.example.models

/**
 * Defines the states of a user in the system.
 */
enum UserStatus {
  /**
   * Represents an active user.
   */
  ACTIVE

  /**
   * Represents user suspended for some reason.
   */
  SUSPENDED

  /**
   * Represents an user who had deleted/inactivated their account.
   */
  @deprecated = "Use INACTIVE for users pending deletion. Deleted users should not be in system"
  DELETED

  /**
   * Represents users requested for deletion and in the process of being deleted.
   */
   INACTIVE
}
```

### Enum defaults

To specify the default value for an enum field, use the string representation of
the enum symbol to set as default.

For example:
```pdl
namespace com.example.models

record User {
  firstName: string
  status: UserStatus = "ACTIVE"
  suspendedReason: enum StatusReason {
    FLAGGED_BY_SPAM_CHECK
    REPORTED_BY_ADMIN
  } = "FLAGGED_BY_SPAM_CHECK"
}
```

## Array Type

Pegasus Arrays are defined as a homogeneous collection of "items" type. Arrays
are ordered, as in the items in an array have specific ordering and the ordering
will be honored when the data is serialized, sent over the wire and
de-serialized.

Some examples below for defining arrays and default values for them.
Primitive arrays:
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

record SuspendedUsersReport {
  users: array[User]
  reasons: array[SuspendReason]
}
```

Record or Enum arrays with default values:
```pdl
namespace com.example.models

record SuspendedUsersReport {
  users: array[User] = [{ "firstName": "Joker" }, { "firstName": "Darth"}]
  reasons: array[SuspendReason] = ["FLAGGED_BY_SPAM_CHECK"]
}
```

## Map Type

Maps are defined with a key type and a value type. The value type can be any
valid PDL type, but currently `string` is the only supported key type. Entries
in a Map are not ordered and the order can change when the data is
serialized/deserialized.

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

Maps with complex values:
```pdl
namespace com.example.models

record UserReport {
  /** Users grouped by their status. Key is the string representation of status enum */
  usersByStatus: map[string, array[User]]

  /**
   * Count of users grouped by firstName and then by status.
   * First level key is the firstName of users.
   * Second level key is the string representation of status.
   */
  countByFirstNameAndStatus: map[string, map[string, int]]
}
```

## Union Type
Union is a powerful way to model data that can be of different types at
different scenarios. Record fields that have this behavior can be defined as a
Union type with the expected value types as its members.

A union type may be defined with any number of member types. Member type can be
primitive, record, enum, map or array. Unions are not allowed as members inside
an union.

The fully qualified member type names also serve as the “member keys” (also
called as “union tags”), and identify which union member type data holds. These
are used to represent which member is present when the data is serialized.

For example:
```pdl
namespace com.example.models

record Account {
  /**
   * Owner of this account. Accounts can be owned either by a single User or an
   * user group.
   */
  owner: union[User, UserGroup]
}
```

The above example defines an `owner` field that can either be an User or an
UserGroup.

Union with default value:

```pdl
namespace com.example.models

record Account {
  /**
   * Owner of this account. Accounts can be owned either by a single User or an
   * user group.
   * By default, All your accounts are belong to CATS.
   */
  owner: union[User, UserGroup] = { "com.example.models.User": { "firstName": "CATS" }}
}
```

Types can be declared inline within a union definition:
```pdl
namespace com.example.models

record User {
  firstName: string
  status: UserStatus = "ACTIVE"
  statusReason: union [
    enum ActiveReason {
      NEVER_SUSPENDED
      SUSPENSION_CLEARED
    }
    enum SuspendReason {
      FLAGGED_BY_SPAM_CHECK
      REPORTED_BY_ADMIN
    }
    enum InactiveReason {
      USER_REQUESTED_DELETION
      REQUESTED_BY_ADMIN
    }
  ] = { "com.example.models.ActiveReason": "NEVER_SUSPENDED" }
}
```

### Union with aliases
***Note:*** _Union with aliases is a recent feature in the Pegasus schema
*language and it might not be fully supported in non-java languages. Please
*check the [support level](/rest.li/multi_language_compatibility_matrix) on all
*languages you intend to use before using aliases_

Union members can optionally be given an alias. Aliases can be used to create
unions with members of the same type or to give better naming for union members.

Union with aliases is required for cases where the union contains multiple
members that are:
* of same primitive type
* of same named complex type
* arrays of same or different item types
* maps of same or different value types
* typerefs dereferenced to same type

Such unions must specify an unique alias for each member in the union
definition. When an alias is specified, it acts as the member’s discriminator
unlike the member type name on the standard unions defined above.

Aliased unions are defined as:
```pdl
union [alias: type, /* ... */]
```
There are few constraints that must be taken in consideration while specifying
aliases for union members,

1. Aliases must be unique for each member in a union definition.
2. Aliases must be specified for either all or none of the members in a union
definition.
3. Aliases cannot be specified for `null` member types which means there can
only be one null member inside a union definition.


An example showing union with aliases:
```pdl
namespace com.example.models

/**
 * A record with user's contact information.
 */
record Contacts {
  /** Primary phone number for the user */
  primaryPhoneNumber: union[
    /** A mobile phone number */
    mobile: PhoneNumber,

    /**
     * A work phone number
     */
    work: PhoneNumber,

    /** A home phone number */
    home: PhoneNumber
  ]
}
```
Since all three union members `mobile`, `work` and `home` are of the same type
`PhoneNumber`, aliases are required for this union.

**Aliased union members can have doc strings and custom properties. This is not supported for non-aliased union members.**

When using unions with aliases, the alias should be used as the key within the default value:
```pdl
namespace com.example.models

/**
 * A record with user's contact information.
 */
record Contacts {
  /** Primary phone number for the user */
  primaryPhoneNumber: union[
    /** A mobile phone number */
    mobile: PhoneNumber,

    /**
     * A work phone number
     */
    work: PhoneNumber,

    /** A home phone number */
    home: PhoneNumber
  ] = {"mobile": { "number": "314-159-2653" }}
}
```

## Typeref

Pegasus supports a new schema type known as a typeref. A typeref is like a
typedef in C. It does not declare a new type but declares an alias to an
existing type.

### Typerefs can provide additional clarity when using primitive types.
Typerefs are useful for differentiating different uses of the same type. For
example, we can use to a typeref to differentiate a string field that holds an
URL from an arbitrary string value or a long field that holds an epoch time in
milliseconds from a generic long value.

For example:
```pdl
namespace com.example.models

/** Number of milliseconds since midnight, January 1, 1970 UTC. */
typeref Time = long
```

```pdl
namespace com.example.models

/**
 * A common record to represent audit stamps.
 */
record AuditStamp {
  /** Time when this record was created */
  createdAt: Time

  /** Time when this record was last updated */
  updatedAt: Time
}
```
A typeref allows additional meta-data to be associated with primitive and
unnamed types. This meta-data can be used to provide documentation or support
custom properties.

### Typerefs can be used to name anonymous types.
A typeref provides a way to refer to common unnamed types such as arrays, maps,
and unions. Without typerefs, users may have to wrap these unnamed types with a
record in order to address them. Alternatively, users may cut-and-paste common
type declarations, resulting in unnecessary duplication and potentially causing
inconsistencies if future changes are not propagated correctly to all copies.

For example:
```pdl
namespace com.example.models

typeref PhoneNumber = union[
  /** A mobile phone number */
  mobile: PhoneNumber,

  /**
   * A work phone number
   */
  work: PhoneNumber,

  /** A home phone number */
  home: PhoneNumber
]

```

Typerefs can then be referred to by name from any other type:
```pdl
namespace com.example.models

record Contacts {
  primaryPhone: PhoneNumber
  secondaryPhone: PhoneNumber
}
```

### Typerefs can be used to specify custom types and coercers

For example, Joda time has a convenient `DateTime` Java class. If we wish to use
this class in Java to represent date-times, all we need to do is define a
Pegasus custom type that binds to it:

```pdl
namespace com.example.models

@java.class = "org.joda.time.DateTime"
@java.coercerClass = "com.example.time.DateTimeCoercer"
typeref DateTime = string
```
The coercer is responsible for converting the Pegasus “referenced” type, in this
case "string" to the Joda `DateTime` class.

See [Java Binding]
(https://linkedin.github.io/rest.li/java_binding#custom-java-class-binding-for-primitive-types)
for more details on defining and using custom types.

## Fixed Type

The Fixed type is used to define schemas with a fixed size in terms of number of
bytes.

For example:
```pdl
namespace com.example.models

fixed MD5 16
```
The example above defines a type called `MD5`, which has a size of `16 bytes`.

## Imports
Imports are optional statements which allow you to avoid writing fully-qualified
names. They function similarly to imports in Java.

For example, the following record can be expressed _without_ imports like this:

```pdl
namespace com.example.models

record AuditStamp {
  createdAt: com.example.models.time.Time
  updatedAt: com.example.models.time.Time
}
```

Alternatively, the record can be expressed using imports, minimizing the need
for repetitive code:

```pdl
namespace com.example.models

import com.example.models.time.Time

record AuditStamp {
  createdAt: Time
  updatedAt: Time
}
```

**Note:**
- Any type that is not imported and is not within the namespace from which it's
referenced must be referenced by fully qualified name.
- Imports take precedence when Pegasus resolves identifiers in a schema. So
be careful when adding an import that conflicts with types in the same namespace
of the root-document.
- Using imports in the following ways will lead to PDL parser errors. You should
avoid to do so.
1. Importing types declared inside the document.
2. Importing types within the root namespace of the document.
3. Declaring types that conflict with existing imports.

## Properties
Properties can be used to present arbitrary data and added to records, record
fields, enums, enum symbols or aliased union members. Properties are defined
using the `@propertyName` syntax. They provide custom data and are available
to users for extending the functionality of the Pegasus schema.

It is up to the users to define the semantic meaning for these custom
properties. Pegasus language will treat them as key-value pairs and make them
available with the in-memory representation of the schema.

Add properties to record and record field:
```pdl
@hasPii = true
record User {
  @validate.regex.regex = "^[a-zA-Z]+$"
  firstName: string
}
```
The above example shows a record level property `hasPii` that can be used to
indicate if the record has personally identifiable information. This can then
be used by application specific business logic.

Add properties to enum and enum symbols:
```pdl
namespace com.example.models

/**
 * Defines the states of a user in the system.
 */
@hasPii = false
enum UserStatus {
  /**
   * Represents an active user.
   */
  @stringFormat = "active"
  ACTIVE

  /**
   * Represents user suspended for some reason.
   */
   @stringFormat = "suspended"
  SUSPENDED

  /**
   * Represents an user who had deleted/inactivated their account.
   */
  @deprecated = "Use INACTIVE for users pending deletion. Deleted users should not be in system"
  @stringFormat = "deleted"
  DELETED

  /**
   * Represents users requested for deletion and in the process of being deleted.
   */
   @stringFormat = "not active"
   INACTIVE
}
```

Add properties to aliased union members:
```pdl
namespace com.example.models

typeref PhoneNumber = union[
  /** A mobile phone number */
  @allowText = true
  mobile: PhoneNumber,

  /**
   * A work phone number
   */
  @allowText = false
  work: PhoneNumber,

  /** A home phone number */
  @allowText = false
  home: PhoneNumber
]

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

If you don't indicate an explicit property value, it will result in an implicit
value of `true`.

For example:
```pdl
@hasPii
```

### Property keys can be expressed as JSON:

For example:
```pdl
@validate = {
  "regex": {
    "pattern": "^[a-z]+$"
  }
}
```

### Property keys can be expressed as paths:

The JSON style property key is complicated to write and read, so we provide a
shorthand - the dot separate format to express the property keys.

The following example is equivalent to the previous JSON example:

```pdl
@validate.regex.pattern = "^[a-z]+$"
```

## Package

Package is used to qualify the language binding namespace for the named schema.

For example:
```pdl
namespace com.example.models
package com.example.api

record User {
  firstName: string
}
```
If package is not specified, language binding class for the named schema will
use `namespace` as its default namespace.

In [Java binding](https://linkedin.github.io/rest.li/java_binding) the package
of the generated class will be determined by the package specified in the
schema.

## Escaping
There are [some
keywords](https://github.com/linkedin/rest.li/blob/8ada11feaf9cf7ff3ce8c84f18fd2cc65168663a/data/src/main/java/com/linkedin/data/schema/PdlBuilder.java#L50)
which are reserved in Pegasus. If these reserved keywords are used to define any
names or identifiers, they need to be escaped using  back-ticks: ` `.

### Keyword Escaping

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

### Namespace/Package escaping

Reserved keywords also need to be escaped when used in namespace declarations,
package declarations and import statements.

```pdl
namespace com.example.models.`record`
package com.example.models.`typeref`

import com.example.models.`optional`

record NamespacePackageEscaping { }
```

### Property key escaping

If you want Pegasus to treat property key name with dots as one string key,
please use backticks to escape such string. Escaping property keys can also be
used to escape reserved keywords such as "namespace" or to escape
non-alpha-numberic characters in the keys.

For example:

```pdl
namespace com.example.models

record User {
  @`namespace` = "foo.bar"
  @validate.`com.linkedin.CustomValidator` = "foo"
  firstName: string
}
```
