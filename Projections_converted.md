---
layout: guide
title: Rest.li Projections
permalink: /Projections
excerpt: Rest.li projection
---

# Rest.li Projections

## Contents

  - [Motivation](#motivation)
  - [Goals](#goals)
  - [Proposed Solution](#proposed-solution)
  - [REST API](#rest-api)
  - [Client API](#client-api)

For details on how to use Projections from Java, see [How to use
projections in Java](How-to-use-projections-in-Java).

## Motivation

Many real life use cases require filtering of objects solely based on
their structure. Two main use cases are:

  - Selecting only a subset of object contents for performance reasons.
    For example, select only names from User profile objects.
  - Removing certain fields based on security policy. For example,
    remove private email and phone number fields from User profile
    objects.

We call this type of filtering - structural filtering or projections.
The following section describes design and implementation of structural
filtering in Pegasus.

## Goals

Two main goals of structural filtering design are:

  - Ease of use
  - Efficiency

In order to satisfy those goals, it is desirable that:

1.  Order in which filters are applied is not important. This simplifies
    life on the client side.
2.  Filters are composable. This adds flexibility on the server side and
    allow efficient implementation.

More formally, filters are functions on objects; if f, g and h are
filters and x is an object, we would like the have:

1.  Commutativity: f(g(x)) = g(f(x))
2.  Semantically valid composition function: having f, g define (fg)
3.  If possible, associativity: (fg)h(x) = f(gh)(x)

## Proposed Solution

Before going into details, one note on terminology. There is a big
overlap between Map and Object types (in general). In many languages,
they are treated interchangeably. You can think of an object as a map,
where map key is field name and map value is a field value. Moreover,
JSON does not distinguish between the two. Thus, this document describes
only how to define filters for Map data type. All definitions related to
Map type also apply to Object type.

There are two types of structural filters users might want to express:

1.  Positive mask: select only specified fields from the object
2.  Negative mask: remove specified fields from the object

### Negative Projection Support

It is important to note that even though the Rest.li framework can
process negative masks for negative projections, the generated client
builders do **NOT** allow such negative masks to be created. Only
positive masks can be created by the generated request builders and
hence all projection use cases within LinkedIn use positive projections.

The documentation listed below here for positive and negative masks is
provided simply for reference purposes. At some point in the future, it
may be possible that the Rest.li framework supports negative projection.

### Syntax

Structural filtering can be expressed as a JSON or any equivalent in
memory representation. Ability of expressing filters as JSON objects is
beneficial because it can be easily processed by clients and is language
agnostic.

#### Positive Mask Syntax

Positive mask is a JSON object, with number 1 assigned to selected
fields, for example:

```
{
  "person": {
    "phone": 1,
    "firstname": 1,
    "lastname": 1,
    "current_position": {
      "job_title": 1
    }
  }
}
```

is a mask, which selects only phone, first name, last name and current
position’s job title from User Profile.

#### Negative Mask Syntax

Negative mask is a JSON object, with number 0 assigned to selected
fields, for example:

```
{
  "profile": {
    "phone": 0
  }
}
```

is a mask, which hides phone number in the User Profile.

#### Complex data structures

##### Array

Syntax for array is the following:

```
"array_field": {
  "$start": 10,
  "$count": 15,
  "$*": {
    (...)
  }
}
```

where $\* is a wildcard mask, applied to every element in Array. $start
and $count are optional fields which specify range of array that will be
returned. First element of an array has index 0. Semantic of $start and
$count is intuitive:

  - $start specify first element of an array, which will be returned
  - $count specifies how many elements, starting from $start will be
    returned

Specifying range in an array is treated as a positive mask e.g. it is
equivalent to selecting elements of array using positive mask.

If mask contains only $start, then $count is implicitly evaluated to
Integer.MAX_INT.

If mask contains only $count, then $start is implicitly evaluated to 0.

If entire array needs to be masked, then syntax is simply:

```
"array_field": 1
```

for positive mask or

```
"array_field": 0
```

for negative mask.

##### Map

Syntax for the map is the following:

```
"map_field": {
    "$*": {
      /* mask for every values in the map */
    },
    "key1": {
      /* mask for value of key1 */
    },
    "key2": {
      /* mask for value of key2 */
    }
  }
}
```

where $\* is a wildcard mask, applied to every value in Map.

If mask for map contains both wildcard $\* and values for specific key
e.g. key1, then mask, which will be applied to the value of key1 is a
composition of wildcard mask and mask for key1. See next chapters for
semantics of mask composition.

If entire value for a specific key needs to be masked, syntax is:

```
"map_field": {
  "key1": 1,
  "key2": 1
}
```

for positive mask and:

```
"map_field": {
  "key1": 0,
  "key2": 0
}
```

for negative mask.

Finally, if entire map needs to be masked, syntax is:

```
"map_field": 1
```

for positive mask and:

```
"map_field": 0
```

for negative mask.

###### Escaping

Mask syntax defines meta-fields: $**, $start and $count. They do not
represent mask for fields with names: $**, $start, $count and they have
special semantics. In order to be able to represent mask for fields,
which names start with ‘$’ character, ‘$’ character is escaped in all
field names e.g.:

```
{
  "$$field": 1
}
```

  
is a mask, which selects only field with name “$field”.

### Masks semantics

Positive mask is used in situations where user wants to limit the
results only to specified fields (most likely for performance reasons).

If positive mask is applied to an object, all fields not specified in
the mask will be permanently removed from the object, leaving only
fields specified in the mask.

Negative mask is used in situations where user wants to restrict access
to some fields. One use case might be applying security policies on the
server side, where certain confidential fields are removed from objects.
Other use case is that client might want to fetch entire object
excluding some specified fields e.g. for performance reasons - see REST
API section for use case example.

If negative mask is applied to an object, all fields specified in the
mask will be permanently removed from the object, leaving all other
fields untouched in the object.

In case all fields of an object get removed by the negative mask, then
the result is an empty object. Similarly, if object does not have any of
fields specified in positive mask, the result is an empty object.

### Masks Composition

#### Motivation

Two main use cases for mask composition are:

  - Server side service implementation wants to apply various security
    policies on top of positive mask obtained from request; for
    performance reasons it is better to compose masks and apply them in
    one go, especially if object is big
  - Assembly-like engine wants to compose multiple positive masks, send
    single request to fetch data and return result to requesters; with
    proposed mask composition semantics this would be possible

#### Masks of the Same Kind

In terms of JSON representation, composition of 2 or more masks of same
kind is just union of objects that JSON represents.  
Composition of two positive masks is mask equivalent to sum of fields
requested in separate masks.

The following table summarizes the semantics of composition of two
positive masks (1 means that field exists in positive mask, - means that
it does not exist in mask, v in result means that field exist in an
object after applying mask, - means that field is missing):

| positive mask | positive mask | result |
| ------------- | ------------- | ------ |
| \-            | \-            | \-     |
| \-            | 1             | v      |
| 1             | \-            | v      |
| 1             | 1             | v      |

The following table summarizes the semantics of composition of two
negative masks (0 means that field exists in negative mask, - means that
it does not exist in mask, v in result means that field exist in an
object after applying mask, - means that field is missing):

| negative mask | negative mask | result |
| ------------- | ------------- | ------ |
| \-            | \-            | v      |
| \-            | 0             | \-     |
| 0             | \-            | \-     |
| 0             | 0             | \-     |

For example, composition of two positive masks:

mask1:

```
{
  "a": 1,
  "c": 1
}
```

mask2:

```
{
  "b": 1,
  "d": 1
}
```

composition of mask1 and mask2:

```
{
  "a": 1,
  "b": 1,
  "c": 1,
  "d": 1
}
```

Case of array’s $start and $count meta-fields requires extra
explanation. Since $start and $count are treated as a positive mask, for
example selection of sub-range of array, composition of two ranges is
smallest range that contains both ranges:

mask1:

```
"array_field": {
  "$start": 15,
  "$count": 20,
  "$*": {
    (...)
  }
}
```

mask2:

```
"array_field": {
  "$start": 20,
  "$count": 30,
  "$*": {
    (...)
  }
}
```

composition of mask1 and mask2:

```
"array_field": {
  "$start": 15,
  "$count": 35,
  "$*": {
    /* composition of masks from mask1 and mask2 */
    (...)
  }
}
```

another example, when ranges are disjoint:

mask1:

```
"array_field": {
  "$start": 10
  "$count": 5,
  "$*": {
    (...)
  }
}
```

mask2:

```
"array_field": {
  "$start": 20,
  "$count": 5,
  "$*": {
    (...)
  }
}
```

composition of mask1 and mask2:

```
"array_field": {
  "$start": 10,
  "$count": 15,
  "$*": {
    /* composition of masks from mask1 and mask2 */
    (...)
  }
}
```

#### Masks of different kinds

Intuitively the output of composition of positive and negative masks is
as if at first, positive mask was applied and after that, negative mask
was applied.

The following table summarizes the semantics of composition of positive
and negative masks (1 means that field exists in positive mask, 0 means
that field exists in negative mask, v in result means that field exist
in an object after applying mask, - means that field is missing):

| positive mask | negative mask | result |
| ------------- | ------------- | ------ |
| \-            | \-            | \-     |
| \-            | 0             | \-     |
| 1             | \-            | v      |
| 1             | 0             | \-     |

Here's a few examples.

mask1 (positive):

```
{
  "a": 1,
  "b": 1
}
```

mask2 (negative):

```
{
  "b": 0,
  "c": 0
}
```

input object:

```
{
  "a": "value1",
  "b": "value2",
  "c": "value3",
  "d": "value4"
}
```

result after applying composition of mask1 and mask2:

```
{
  "a": "value1"
}
```

#### Composition of simple mask with complex mask

It is possible that simple mask (0 or 1) is composed with a complex
mask, represented as an object.

##### Composition of negative mask 0 with complex mask

Since negative mask (0) has the higher priority than positive mask, then
result of composition of mask equal to 0 with any other mask is 0 e.g.
composition of mask:

```
{
  "a": 0
}
```

with

```
{
  "a": {
    "$*": 1
    "b": 0,
  }
}
```

is equal to:

```
{
  "a": 0
}
```

##### Composition of positive mask 1 with complex mask

The semantics of positive mask is: select this field and all it’s
children. Hence, the following two masks are semantically equivalent:

```
{
  "a": 1
}
```

```
{
  "a": {
    "$*": 1
  }
}
```

In other words, positive mask is recursive. If positive mask was not
recursive, then it would not be possible to express the following
filter: “select field ”a" and all it’s children" without prior knowledge
of all available fields in field “a”.

Composition of positive mask with a complex mask propagates positive
mask recursively e.g. composition of:

```
{
  "a": 1
}
```

with:

```
{
  "a": {
    "b": 0
  }
}
```

yields result:

```
{
  "a": {
    "$*": 1,
    "b": 0
  }
}
```

If complex mask already contains wildcard mask, then $\*=1 is
recursively pushed down to the wildcard of the wildcard e.g. composition
of positive mask:

```
{
  "profile": 1
}
```

with negative mask:

```
{
  "profile": {
    "$*": {
      "password": 0
    }
  }
}
```

yields result:

```
{
  "profile": {
    "$*": {
      "$*": 1,
      "password": 0
    }
  }
}
```

The reason why pushing down the $\*=1 mask preserves correct semantics
is because simple mask 1 is equivalent to the complex mask: { “$\*”: 1
}.

It means that the following masks are equivalent:

```
{
  "a": 1
}
```

and

```
{
  "a": {
    "$*": 1
  }
}
```

#### Mask composition properties

Mask composition is commutative. It means that it doesn’t matter in
which order masks are composed. However, application of masks, which are
or were built using positive masks, on the object is not associative
operation. Consider the following example:

mask1:

```
{
  "a": 1
}
```

mask2:

```
{
  "b": 1
}
```

input object:

```
{
  "a": "value1",
  "b": "value2"
}
```

If we first compose masks and apply mask obtained from composition on
input object the result will contain both fields “a” and “b”. However,
if we first apply mask1 on input object and then apply mask2 on the
result, then final result will be empty object.

## REST API

REST API uses syntax similar to LinkedIn public REST API extended with
concept of negative masks. The need for concept of negative mask is
required to accommodate the following scenario.

Let’s assume that User Profile contains a field “emails” which contains
mailbox contents in it. Client might want to request User Profile data
(without knowing exactly which fields are available) but definitely
without “emails” field, which might be huge. The notion of not needing
to know which fields are available allows backward compatible evolution
of data model without modification of all clients. This use case might
appear in mid-tier, where service retrieves objects, do
calculations/modify them and return to requesters. It would be
beneficial if mid-tier services were oblivious (to some extent) to data
model evolution.

The reasons for choosing LinkedIn public API syntax in REST API, instead
of encoding JSON are the following:

  - It is concise, so filters can be written by hand when needed (for
    example, during development and testing).
  - It is familiar to LinkedIn public API users.
  - It is expressive.

Internally, filter expressions and JSON filter expressions will be
parsed into the same data structure. This will allow composition of
filters (for example, security policies). REST API will not be fully
compatible with original LinkeIn public API syntax.

Proposed syntax will be used only in REST interface. Composition of
simple masks can generate complicated masks. Pegasus will automatically
generate correct expression and will be able to parse it. The only time,
when developer will deal with projections syntax is during
experimenting, debugging and so on.

For example, the following JSON positive mask:

```
{
  "person": {
    "firstname": 1,
    "lastname": 1
  }
}
```

would translate to the following expression:

```
:(person:(firstname,lastname))
```

Projections are passed as a value of ‘fields’ request parameter in URL
e.g.

```
http://host:port/context/resource/id?fields=person:(firstname,lastname)
```

Please note that, as mentioned above, negative projections **should
not** be used.

### Array

The syntax for array representation is the following:

```
"array_field": {
"$start": 10,
"$count": 15,
"$*": {
  "field1": 1,
  "field2": 1
  }
}
```

will be represented as:

```
array_field:($*:(field1,field2),$start=10,$count=15)
```

### Map

The syntax for map representation follows similarity between Object and
Map data structure. The syntax is the following, for mask on map:

```
 "map_field": {
    "$*": {
      "field1": 1
    },
    "key1": {
      "field2": 1
    },
    "key2": {
      "field3": 1
    }
  }
}
```

will be represented as:

```
map_field:($*:(field1),key1:(field2),key2:(field3))
```

## Client API

Java client should use helper classes in:

```
com.linkedin.data.transform.filter.request
```

to build and manipulate projections.
