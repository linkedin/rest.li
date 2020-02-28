---
layout: guide
title: How Data is Serialized for Transport
permalink: /how_data_is_serialized_for_transport
excerpt: Rest.li how data is serialized for transport.
---

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

  ```
  {
    "name" : "unionField",
    "type" : [
      "int",
      { "type" : "typeref", "name" : "a.b.c.d.Foo", "ref"  : "string" }
    ]
  }
  ```

the JSON encoding for the typeref member should look like

```
{ “string” : “Correct key” }
```

NOT

```
{ “a.b.c.d.Foo” : “Wrong key” }
```

Similarly, for a union with aliased members the key for the members will
be its corresponding alias. For example,

```json
{
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

```java
DataMap dataMap = new DataMap();
dataMap.put("message", "Hi!");
byte[] jsonBytes = DataMapUtils.mapToBytes(dataMap);
String json = new String(jsonBytes, "UTF-8");

```

To serialize from a RecordTemplate instance to JSON:

```java
Greeting greeting = new Greeting().setMessage("Hi!"); // Where Greeting is class extending RecordTemplate
byte[] jsonBytes = DataMapUtils.dataTemplateToBytes(greeting, true);
String json = new String(jsonBytes, "UTF-8");
```

#### How to Deserialize JSON to Data

To deserialize from JSON to a DataMap:

```java
InputStream in = IOUtils.toInputStream("{'message':'Hi!'}");
DataMap dataMap = DataMapUtils.readMap(in);
```

To deserialize from JSON to a RecordTemplate:

```java
InputStream in = IOUtils.toInputStream("{'message':'Hi!'}");
Greeting deserialized = DataMapUtils.read(in, Greeting.class); // Where Greeting is class extending RecordTemplate
```

#### How to Serialize Data to PSON

PSON is a binary format that can represent any JSON data but is more
compact, requires less computation to serialize and deserialize, and can
transmit byte strings directly.

PSON serialization/deserialization works similar to JSON (as described
above) but uses these two methods:

```java
DataMapUtils.readMapPson()
DataMapUtils.mapToPsonBytes()
```