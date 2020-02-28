---
layout: guide
title: How Data is Represented in Memory
permalink: /how_data_is_represented_in_memory
excerpt: Rest.li how data is represented in memory.
---

# How Data is Represented in Memory

## Contents

-   [The Data Layer](#the-data-layer)
-   [The Data Schema Layer](#the-data-schema-layer)
-   [The Data Template Layer](#the-data-template-layer)

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


|Method |Declared by|Description|
| --- | --- |--- |
| `DataComplex clone()` | `DataComplex` | A shallow copy of the instance. <br />The read-only state is not copied, the clone will be mutable. <br />The instrumentation state is also not copied. <br />Although `java.lang.CloneNotSupportedException` is declared in the throws clause, the method should not throw this exception.|
| `DataComplex copy()` <br /> | `DataComplex` <br /> | A deep copy of the object graph rooted at the instance. The copy will be isomorphic to the original. <br />The read-only state is not deep copied, and the new `DataComplex` copies will be mutable. <br />The instrumentation state is also not copied. <br />Although `java.lang.CloneNotSupportedException` is declared in the throws clause, the method should not throw this exception. |
| `void setReadOnly()` <br /> | `CowCommon` <br /> | Make the instance read-only. It does not affect the read-only state of contained `DataComplex` values. <br /> |
| `boolean isReadOnly()` <br /> | `CowCommon` <br /> | Whether the instance is in read-only state. <br /> |
| `void makeReadOnly()` <br /> | `DataComplex` <br /> | Make the object graph rooted at this instance read-only. <br /> |
| `void isMadeReadOnly()` <br /> | `DataComplex` <br /> | Whether the object graph rooted at this instance has been made read-only. <br /> |
| `Collection<Object> values()` <br /> | `DataComplex` <br /> | Returns the values stored in the `DataComplex` instance, i.e. returns the values of a `DataMap` or the elements of a `DataList`. <br /> |
| `void startInstrumentatingAccess()` <br /> | `Instrumentable` <br /> | Starts instrumenting access. <br /> |
| `void stopInstrumentingAccess()` <br /> | `Instrumentable` <br /> | Stops instrumenting access. <br /> |
| `void clearInstrumentedData()` <br /> | `Instrumentable` <br /> | Clears instrumentation data collected. <br /> |
| `void collectInstrumentedData(...)` <br /> | `Instrumentable` <br /> | Collect data gathered when instrumentation was enabled. <br /> |


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

| Method | Description |
| ---    | ---  |
| `Type getType()` <br /> | Provide the type of the schema, can be `BOOLEAN`, `INT`, `LONG`, `FLOAT`, `DOUBLE`, `BYTES`, `STRING`, `FIXED`, `ENUM`, `NULL`, `ARRAY`, `RECORD`, `MAP`, `UNION`. <br /> |
| `boolean hasError()` <br /> | Whether the schema definition contains at least one error. <br /> |
| `boolean isPrimitive()` <br /> | Whether the schema type is a primitive schema type. <br /> |
| `boolean isComplex()` <br /> | Whether the schema type is a complex schema type, i.e. not primitive type. <br /> |
| `Map<String,Object> getProperties()` <br /> | Return the properties of the schema. These properties are the keys and values from the JSON fields in complex schema definitions that are not processed and interpreted by the schema parser. For primitive types, this method always return an immutable empty map. <br /> |
| `String getUnionMemberKey()` <br /> | If this type is used as a member of a union without an alias, this will be the key that uniquely identifies/selects this type within the union. This value of this key is as defined by the Avro 1.4.1 specification for JSON serialization. <br /> |
| `String toString()` <br /> | A more human consumable formatting of the schema in JSON encoding. Space will added between fields, items, names, values, ... etc. <br /> |
| `Type getDereferencedType` <br /> | If the type is a typeref, it will follow the typeref reference chain and return the type referenced at the end of the typeref chain. <br /> |
| `DataSchema getDereferencedSchema` <br /> | If the type is a typeref, it will follow the typeref reference chain and return the DataSchema referenced at the end of the typeref chain. <br /> |

The following table shows the mapping of schema types to Data Schema
classes.

|Schema Type <br /> |Data Schema class <br /> |Relevant Specific Attributes <br /> |
| --- | --- | --- |
| int <br /> | `IntegerDataSchema` <br /> | |
| long <br /> | `LongDataSchema` <br /> | |
| float <br /> | `FloatDataSchema` <br /> | |
| double <br /> | `DoubleDataSchema` <br /> | |
| boolean <br /> | `BooleanDataSchema` <br /> | |
| string <br /> | `StringDataSchema` <br /> | |
| bytes <br /> | `BytesDataSchema` <br /> | |
| enum <br /> | `EnumDataSchema` <br /> | `List<String> getSymbols()` <br />`int index(String symbol)` <br />`boolean contains(String symbol)` <br /> |
| array <br /> | `ArrayDataSchema` <br /> | `DataSchema getItems()` <br /> |
| map <br /> | `MapDataSchema` <br /> | `DataSchema getValues()` |
| fixed <br /> | `FixedDataSchema` <br /> | `int getSize()` <br /> |
| record, error <br /> | `RecordDataSchema` <br /> | `RecordType recordType()` (record or error) <br />`boolean isErrorRecord()` <br />`List<Field> getFields()` <br />`int index(String fieldName)` <br />`boolean contains(String fieldName)` <br />`Field getField(String fieldName)` <br /> |
| union <br /> | `UnionDataSchema` <br /> | `List<Member> getMembers()` <br />`boolean contains(String memberKey)` <br />`DataSchema getTypeByMemberKey(String memberKey)` <br />`boolean areMembersAliased()` <br /> |
| null <br /> | `NullDataSchema` <br /> | |


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
if (!validationResult.isValid())  
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

|Schema Type <br /> |Post-coercion Java Type <br /> |Pre-coercion Input Java Types <br /> |Validation Performed <br /> |Coercion Method |
| --- | --- | --- | ----- | --- |
| int <br /> | `java.lang.Integer` <br /> | `java.lang.Number` (1) <br /> | Value must be a `Number`. <br /> | `Number.intValue()` <br /> |
| long <br /> | `java.lang.Long` <br /> | `java.lang.Number` (1) | Value must be a `Number`. <br /> | `Number.longValue()` <br /> |
| float <br /> | `java.lang.Float` <br /> | `java.lang.Number` (1) | Value must be a `Number`. | `Number.floatValue()` <br /> |
| double <br /> | `java.lang.Double` <br /> | `java.lang.Number` (1) | Value must be a `Number`. | `Number.doubleValue()` <br /> |
| boolean <br /> | `java.lang.Boolean` <br /> | `java.lang.Boolean` (2) <br /> | Value must be a `Boolean`. <br /> | |
| string <br /> | `java.lang.String` <br /> | `java.lang.String` (2) <br /> | Value must be a `String`. <br /> | |
| bytes <br /> | `com.linkedin.data.ByteString` <br /> | `com.linkedin.data.ByteString`, `java.lang.String` (3) <br /> | If the value is a `String`, the `String` must be a valid encoding of binary data as specified by the Avro specification for encoding bytes into a JSON string. <br /> | `ByteString.copyFromAvroString()` <br /> |
| enum <br /> | `java.lang.String` <br /> | `java.lang.String` <br /> | The value must be a symbol defined by the enum schema. <br /> | |
| array <br /> | `com.linkedin.data.DataList` <br /> | `com.linkedin.data.DataList` (2) <br /> | Each element in the `DataList` must be a valid Java type for the schema's item type. For example, if the schema is an array of longs, then every element in the `DataList` must be a `Number`. <br /> | |
| map <br /> | `com.linkedin.data.DataMap` <br /> | `com.linkedin.data.DataMap` (2) <br /> | Each value in the `DataMap` must be a valid Java type for the schema's value type. For example, if the schema is a map of longs, then every value in the `DataMap` must be a `Number`. | |
| fixed <br /> | `com.linkedin.data.ByteString` <br /> | `com.linked.data.ByteString` (2), `java.lang.String` (3) <br /> | If the value is a `String`, the `String` must be a valid encoding of binary data as specified by the Avro specification for encoding bytes into a JSON string and the correct size for the fixed schema type. <br />If the value is a `ByteString`, the `ByteString` must be the correct size for the fixed schema type. <br /> | `ByteString.copyFromAvroString()` |
| record <br /> | `com.linkedin.data.DataMap` <br /> | `com.linkedin.data.DataMap` (2) <br /> | Each key in the `DataMap` will be used lookup a field in the record schema. The value associated with this key must be a valid Java type for the field's type. <br />If the required validation option is enabled, then all required fields must also be present. <br /> | |
| union <br /> | `com.linkedin.data.DataMap` <br /> | `java.lang.String`, `com.linkedin.data.DataMap` (2) <br /> | If the value is a `String`, the value must be `Data.NULL`. <br />If the value is a `DataMap`, then the `DataMap` must have exactly one entry. The key of the entry must identify a member of the union schema, and the value must be a valid type for the identified union member's type. <br /> | |

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

|Schema Type <br /> |Post-coercion Java Type <br /> |Pre-coercion Input Java Types <br /> |Validation Performed <br /> |_. Coercion Method <br /> |
| --- | --- | --- | --- | --- |
| int <br /> | `java.lang.Integer` <br /> | `java.lang.String` <br /> | If value is a `String`, it must be acceptable to `BigDecimal(String val)`, else it has to be a `Number` (see "[`NORMAL`](#normal-coercion-mode)") <br /> | `(new BigDecimal(value)).intValue()` <br /> |
| long <br /> | `java.lang.Long` <br /> | `java.lang.String` | If value is a `String`, it must be acceptable to `BigDecimal(String val)`, else it has to be a `Number` (see "[`NORMAL`](#normal-coercion-mode)") | `(new BigDecimal(value)).longValue()` |
| float <br /> | `java.lang.Float` <br /> | `java.lang.String` | If value is a `String`, it must be acceptable to `BigDecimal(String val)`, else it has to be a `Number` (see "[`NORMAL`](#normal-coercion-mode)") <br /> | `(new BigDecimal(value)).floatValue()` |
| double <br /> | `java.lang.Double` <br /> | `java.lang.String` | If value is a `String`, it must be acceptable to `BigDecimal(String val)`, else it has to be a `Number` (see "[`NORMAL`](#normal-coercion-mode)") <br /> | `(new BigDecimal(value)).doubleValue()` |
| boolean <br /> | `java.lang.Boolean` <br /> | `java.lang.String` | if value is a `String`, its value must be either `"true"` or `"false"` ignoring case, else it has to be a `Boolean` (see "[`NORMAL`](#normal-coercion-mode)") <br /> |<pre><code>if ("true".equalsIgnoreCase(value))<br /> return Boolean.TRUE;<br />else if ("false".equalsIgnoreCase(value))<br />  return Boolean.FALSE;<br />else <br />  // invalid string representation<br />  // of boolean </code></pre>|

#### ValidationResult

The result of validation is returned through an instance of the
`ValidationResult` class. This class has the following methods:

|Method |Description <br /> |
| --- | --- |
| `boolean hasFix()` <br /> | Whether any fix-ups (i.e., modification or replacement of input Data layer objects) have been proposed. Fixes may be proposed but not applied because fixes cannot be applied to read-only complex objects. <br /> |
| `boolean hasFixupReadOnlyError()` <br /> | Whether any fix-ups could not be applied because of read-only complex objects. <br /> |
| `Object getFixed()` <br /> | Return a fixed object. In-place fixes may or may not be possible because some objects are immutable. For example, if the schema type is "fixed" and String object is provided as the Data object, the fixed-up object that would be returned will be a ByteString. Since String and ByteString are both immutable and have different types, the fixed object will be a different object, i.e. the fix-up cannot be done in-place. <br /><br />For complex objects, the fix-ups can be applied in place. This is because the new values can replace the old values in a `DataMap` or `DataList`. <br /> |
| `boolean isValid()` <br /> | Whether the fixed object returns by `getFixed()` contains any errors. If it returns `false`, then the fixed object and its dependents are fixed up according to the provided schema. <br /> |
| `String getMessage()` <br /> | Provides details on validation and fix-up failures. Returns empty string if `isValid()` is `true` and fix-ups/validation have occurred without problems. <br /> |


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
