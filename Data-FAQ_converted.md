---
layout: guide
title: Rest.li FAQ
permalink: /Data-FAQ
excerpt: Rest.li FAQ
---

# Rest.li FAQ


## How do I validate a record with default values filled in?

```java  
Foo foo = new Foo();  
ValidationResult result = ValidateDataAgainstSchema.validate(foo.data(),
foo.schema(), new
ValidationOptions(RequiredMode.FIXUP_ABSENT_WITH_DEFAULT));  
assert(result.isValid());  
```

This will fail if the underlying data is read-only and default values
cannot be set for absent fields.

This will also work for partially filled in records. It will only add
default values to fields that are absent.

## How do I convert a Pegasus data schema to an Avro data schema?

It requires the data-avro module, or data-avro-\*.jar.

At runtime, use:

```java  
Foo foo = new Foo();  
DataSchema pegasusSchema = foo.schema();  
org.apache.avro.Schema avroSchema =
SchemaTranslator.dataToAvroSchema(pegasusSchema);  
String avsoSchemaInJson = avroSchema.toString();  
```

From the command line, to create text files with Avro schema from PDSC
files (version 0.17.1 or higher):

```
# syntax:
# java [-Dgenerator.resolver.path=<path>] com.linkedin.data.avro.AvroSchemaGenerator <targetDir> [<pdscFile|schemaFullName>]...
java -Dgenerator.resolver.path=src/main/pegasus com.linkedin.data.avro.generator.AvroSchemaGenerator ../build/main/codegen/avro src/main/pegasus/com/linkedin/foo/*.pdsc
# or
java -Dgenerator.resolver.path=src/main/pegasus com.linkedin.data.avro.generator.AvroSchemaGenerator ../build/main/codegen/avro com.linkedin.foo.Foo
```

Classpath must be setup to include `data-avro.jar` and its dependencies.

## How do I use a data type embedded inside a data schema file?

You may experience errors, such as the
    following:

```Type cannot be resolved: 1,1: "a.b.D" cannot be resolved.```

when ```a.b.D```’s definition is embedded in another type, for
example, ```a.b.C```.

Embedded data types do not have their own data schema file. As a result, such
data types can only be referenced within the containing data schema file; it can
not be referenced externally. To change this behavior, pull out the
definition of the internal type to a separate data schema file.

Internally, this behavior is due to reason that the schema parser
references the data types by filenames. The schema file for `a.b.C`
is expected to be found at `pegasus/a/b/C.pdl`. Since Rest.li
does not prefix the containing data type’s name to the embedded type’s
name, when type `a.b.D` is embedded inside `C.pdl`, the
schema parser will not be able to find `pegasus/a/b/D.pdl`.

## Why is my Java 8 build generating all sorts of Javadoc warnings/errors due to doclint?

When using Java 8, you may experience build failures due to the
following:

```
[error] /home/myuser/data-template/src/main/codegen/com/linkedin/PegasusFormSchema.java:192: error: malformed HTML
* return type: java.util.Set<java.lang.Integer>
[error] /home/myuser/data-template/src/main/codegen/com/linkedin/PegasusFormSchema.java:192: error: bad use of '>'
* return type: java.util.Set<java.lang.Integer>
```

The root cause is incorrectly formatted string values for anything
within the `doc` or `symbolDocs` (in `.pdsc` files) attributes within your data schemas. Due to
the addition of doclint in JDK8, anything under the doc or symbolDocs
attribute must be W3C HTML 4.01 compliant. This is because the contents
of this string will appear as Javadocs in the generated Java ‘data
template’ classes later. Please take this into consideration when
writing your documentation.

The alternative is to disable doclint altogether:

```  
if (JavaVersion.current().isJava8Compatible()) {
  allprojects {
    tasks.withType(Javadoc) {
      options.addStringOption('Xdoclint:none', '-quiet')
    }
  }
}
```

More details on doclint can be found here: [Javadoc has become very
strict](http://stackoverflow.com/questions/22528767/jdk8-and-javadoc-has-become-very-strict)

More details on `doc` and `symbolDocs` are at [Data Schemas](/rest.li/pdl_schema).

## How does Rest.li full update method work with schema evolution?

There is a potential pitfall to be aware of. Consider the following
scenario:

1.  Suppose a schema begins on version 0 with a single optional field A.
    We write a client that sends an update request with the field A
    populated.
2.  At some point, we add an optional field B in version 1.
3.  Our old client still sends a request with A populated, but it is not
    clear how to interpret its request. From the server’s perspective,
    we cannot distinguish a new client that wants to null out field B
    (e.g. schema version 1) vs. a client that is not aware of field B
    (e.g. version 0).
