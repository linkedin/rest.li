---
layout: guide
title: Migrating from PDSC to PDL
permalink: /pdl_migration
excerpt: Guide for migrating from PDSC to PDL with a side-by-side comparison showing notable differences.
---

# Migrating from PDSC to PDL

{{ page.excerpt }}

## Contents
- [Why Use PDL?](#why-use-pdl)
- [How to Convert Schemas to PDL](#how-to-convert-schemas-to-pdl)
- [Notable Differences Between PDSC and PDL](#notable-differences-between-pdsc-and-pdl)
    - [PDL is More Java-Like](#pdl-is-more-java-like)
    - [Import Statements](#import-statements)
    - [Shorthand for Custom Properties](#shorthand-for-custom-properties)

## Why Use PDL?

PDL as a schema definition language was designed to be read and written by humans, making it a much more enjoyable experience for developers
to define their service's models. PDSC, despite its syntax being simply a subset of JSON, cannot boast this human-readability factor.

In addition to its inherently more readable syntax, PDL also features some extra shorthand which developers can leverage to write less and
make their schemas more readable for others. See [below](#notable-differences-between-pdsc-and-pdl) for more information on this.

## How to Convert Schemas to PDL

If you have an existing project with PDSC schemas, converting everything to PDL is pretty straightforward.
Rest.li's Gradle plugin provides a task `convert<sourceSet>ToPdl` which will automatically convert your schemas for you.
It should be noted that this task verifies the converted schemas against the original schemas.
If this verification is failed, then the whole conversion will be aborted.

The following command will convert every PDSC schema in your project to PDL.

```
gradle convertToPdl
```

You can selectively migrate only one particular module of your project with the following:

```
gradle :<moduleName>:convertToPdl
```

This task also takes in a few options:

```
gradle :<moduleName>:convertToPdl \
    [-PconvertToPdl.reverse=(true|false)] \
    [-PconvertToPdl.keepOriginal=(true|false)] \
    [-PconvertToPdl.preserveSrcCmd]
```

| Property | Type | Description
|----------|------|------------
| `convertToPdl.reverse` | boolean | If true, converts PDL schemas back to PDSC (and vice versa if false).
| `convertToPdl.keepOriginal` | boolean | If true, keeps the source schemas (the source schemas are deleted by default).
| `convertToPdl.preserveSrcCmd` | string | Command which is run for each file, useful for running special VCS logic. The command should be a template string containing `$src` and `$dst` as references to the source and destination filename, respectively.

## Notable Differences Between PDSC and PDL

You can find in-depth documentation on [PDSC syntax](/rest.li/pdsc_syntax) and [PDL syntax](/rest.li/pdl_syntax) elsewhere,
but this section will point out notable differences between PDSC and PDL.

### PDL is More Java-Like

PDL is arguably much more human-readable than PDSC because of its Java-like syntax.
Whereas reading and writing PDSC is like reading and writing plain JSON, reading and
writing PDL is like reading and writing a Java interface definition.

<table>
    <tr>
        <th>PDSC</th>
        <th>PDL</th>
    </tr>
    <tr>
        <td><pre>
{
    "namespace": "com.example.models",
    "type": "record",
    "name": "Foo",
    "doc": "Foo is a record.",
    "fields": [
        {
            "name": "x",
            "type": "int",
            "default": 1
        }
    ]
}
        </pre></td>
        <td><pre>
namespace com.example.models

/**
 * Foo is a record.
 */
record Foo {
    x: int = 1
}
        </pre></td>
    </tr>
</table>

### Import Statements

In PDSC, all references to types outside the schema's own namespace have to be written as fully-qualified type names.
PDL, on the other hand, features imports statements (similar to those in Java) which allow the user to specify types
that can be referenced by their simple name rather than their full name. This can help to reduce the amount of
redundant data written in schemas that refer to the same type numerous times.

<table>
    <tr>
        <th>PDSC</th>
        <th>PDL</th>
    </tr>
    <tr>
        <td><pre>
{
    "namespace": "com.example.models",
    "type": "record",
    "name": "Redundancies",
    "doc": "Imports help to reduce redundant FQNs.",
    "fields": [
        {
            "name": "a",
            "type": "org.external.types.SomeType"
        },
        {
            "name": "b",
            "type": "org.external.types.SomeType"
        },
        {
            "name": "c",
            "type": {
                "type": "array",
                "items": "org.external.types.SomeType"
            }
        }
    ]
}
        </pre></td>
        <td><pre>
namespace com.example.models

import org.external.types.SomeType

/**
 * Imports help to reduce redundant FQNs.
 */
record Redundancies {
    a: SomeType,
    b: SomeType,
    c: array[SomeType]
}
        </pre></td>
    </tr>
</table>

### Shorthand for Custom Properties

Custom properties (also referred to as "annotations") were supported in PDSC as just arbitrary values keyed at anything that's not a reserved keyword.
In PDL, the syntax for custom properties is cleaner and more Java-like.

<table>
    <tr>
        <th>PDSC</th>
        <th>PDL</th>
    </tr>
    <tr>
        <td><pre>
{
    "namespace": "com.example.models",
    "type": "record",
    "name": "CustomProperties",
    "doc": "PDL has more flexible support for custom properties.",
    "something": [ 1, 2, 3 ],
    "fields": []
}
        </pre></td>
        <td><pre>
namespace com.example.models

@something = [ 1, 2, 3 ]
record CustomProperties {}
        </pre></td>
    </tr>
</table>

Furthermore, PDL supports a path-like shorthand, where dot-separated keys can be used to specify nested custom properties.
Some property written as such:

```pdl
@prop = {
    "nested": {
        "foo": 1,
        "bar": 2
    }
}
```

May alternatively be written as such:

```pdl
@prop.nested.foo = 1
@prop.nested.bar = 2
```

One can easily imagine a scenario in which this would really come in handy:

```pdl
// Not so pretty...
@a = {
    "b": {
        "c": {
            "d": {
                "e": {
                    "f": "hello"
                }
            }
        }
    }
}

// That's better
@a.b.c.d.e.f = "hello"
```

Another interesting shorthand for custom properties is omission of an explicit value to indicate `true`:

<table>
    <tr>
        <th>PDSC</th>
        <th>PDL</th>
    </tr>
    <tr>
        <td><pre>
{
    "namespace": "com.example.models",
    "type": "record",
    "name": "ImplicitProperty",
    "truthy": true,
    "fields": []
}
        </pre></td>
        <td><pre>
namespace com.example.models

@truthy
record ImplicitProperty {}
        </pre></td>
    </tr>
</table>
