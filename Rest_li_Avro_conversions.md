---
layout: guide
title: Rest.li to Avro conversions
permalink: /Rest_li_Avro_conversions
excerpt: Sometimes it is necessary to convert between Avro and Rest.li formats. That is, either converting schemas or converting data. Rest.li provides ways to do this using the data-avro module.
---

# Rest.li to Avro Conversions

## Contents
* [Converting Schemas](#converting-schemas)
* [Converting Data](#converting-data)
* [FAQ](#faq)

Sometimes it is necessary to convert between Avro and Rest.li formats. That is, either converting schemas (Rest.li `DataSchema`s to Avro `Schema`s and vice versa) or converting data (Rest.li `DataMap`s to Avro `GenericRecord`s and vice versa). Rest.li provides ways to do this using the data-avro module.

## Converting Schemas
The key class for converting schemas is the `SchemaTranslator` class.
### Converting Avro to Rest.li
To convert from Avro to Rest.li, you will use the `avroToDataSchema` methods in SchemaTranslator.
The default method takes in only the Avro schema you wish to convert as input:
```java
com.linkedin.data.avro.SchemaTranslator.avroToDataSchema(avroSchema);
```
This schema can either be a stringified version of the Schema or an `org.apache.avro.Schema`.

There is also a similar method that also accepts an `AvroToDataSchemaTranslationMode`.  Generally, this method doesn't need to be used.  However, if you have embedded your rest.li schema within your Avro schema, and you can use this with the `AvroToDataSchemaTranslationMode` to speed up the translation process. This is normally done when translating from Rest.li format to Avro format.  See the section for converting from Rest.li to Avro to learn more about this.

### Converting Rest.li to Avro
To convert from Rest.li to Avro, you will use the `dataToAvroSchema` methods in SchemaTranslator. Like the `avroToDataSchema` method, it can take in either a stringified restli schema, or a  `DataSchema`, and, optionally, a `DataToAvroSchemaTranslationOptions`
```java
com.linkedin.data.avro.SchemaTranslator.dataToAvroSchema(dataSchema);
com.linkedin.data.avro.SchemaTranslator.dataToAvroSchema(dataSchema, translationOptions);
```

`DataToAvroSchemaTranslationOptions` has four parts:
* The translation mode `OptionalDefaultMode`
* The JSON style `JsonBuilder.Pretty`
* The schema embedding mode `EmbedSchemaMode`
* The namespace override flag `overrideNamespace`

`OptionalDefaultMode` determines how defaults are translated into Rest.li format.  Since Avro requires that a union's default value always be of the same type as the first member type of the union, if a type is not consistently initialized with a single default type, translations may encounter problems.  By default this value is set to `TRANSLATE_DEFAULT`, but if your translations are encountering issues around default values, you may wish to set this to `TRANSLATE_TO_NULL`, which will cause all optional fields with a default value to have their default value set to null in the Avro translation.

`JsonBuilder.Pretty` simply sets the format of the output JSON.  By default, this is set to `COMPACT`.

`EmbedSchemaMode` determines whether or not to embed the original Rest.li schema into the resulting Avro schema.  This can speed translation back (or make a translation back more accurate) to Rest.li format with the correct settings passed to the `avroToDataSchema` method. By default, this is set to `NONE`.

`overrideNamespace` is a boolean flag indicating whether the namespaces of the translated Avro schemas should be overridden. If this flag is set to `true`, then the namespace of each translated Avro schema will be prepended with a special prefix,
`"avro."` (e.g. `com.x.y` becomes `avro.com.x.y`). This is helpful in cases where pegasus schemas and their Avro counterparts are included in the same project, potentially causing namespace/package conflicts.

## Converting Data
The key class for converting data is the `DataTranslator` class.
### Converting Avro to Rest.li
To convert from Avro to Rest.li, you will use the `genericRecordToDataMap` method in `DataTranslator`. You'll need the Avro `GenericRecord` you are converting, the Avro `Schema` the `GenericRecord` conforms to, and the Rest.li `RecordDataSchema` of the Rest.li type you are converting to:
```java
com.linkedin.data.avro.DataTranslator.genericRecordToDataMap(genericRecord, recordDataSchema, avroSchema);
```
There are no versions of this method that accept any special options.

### Converting Rest.li to Avro
To convert from Rest.li to Avro, you will use the `dataMapToGenericRecord` methods in `DataTranslator`.  You will need the Rest.li `DataMap` you are converting, the `RecordDataTemplate` your `DataMap` conforms to, and, optionally, the Avro `Schema` you are converting your data to. If you do not pass in an Avro `Schema`, then the schema translator will be used to convert your passed in `RecordDataSchema` to an Avro `Schema`, using default settings.
```java
com.linkedin.data.avro.DataTranslator(dataMap, dataSchema);
com.linkedin.data.avro.DataTranslator(dataMap, dataSchema, avroSchema);
```

### Automatically generating avro schemas as part of a build

Rest.li will generate avro schemas for all your pegasus schemas (`.pdl` files) automatically if the build is configured to enable this.

See [Gradle generateAvroSchema Task](setup/gradle#generateavroschema) for details on how to enable.

## FAQ
### How do I get the RecordDataSchema of a particular Record type?
The RecordDataSchema field of generated Record classes are private, so you cannot get them directly.  However, there is a helper method in `com.linkedin.data.template.DataTemplateUtil` called `getSchema` that can help you get the Schema.  Simply pass in the class of the Record and it will return a basic `DataSchema`.  If you know this Schema is a `RecordDataSchema`, you can safely cast the result to `RecordDataSchema`.
```java
(RecordDataSchema)com.linkedin.data.template.DataTemplateUtil.getSchema(MyRecord.class);
```