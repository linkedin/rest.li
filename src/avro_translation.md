---
layout: guide
title: Avro Translation
permalink: /avro_translation
excerpt: Rest.li Avro translation.
---

# Avro Translation

## Contents

-   [Translating Schemas to and from Avro](#translating_schemas_to_and_from_avro)
-   [Translating Data to and from Avro](#translating_data_to_and_from_avro)

## Translating Schemas to and from Avro

The schema and data translators inspect your classpath to determine
which version of avro you are using and require you have the matching
pegasus \`data-avro-<avro_version>\` adapter module in your classpath.

For example, if you are using avro 1.6, you must add a dependency on the
pegasus \`data-avro-1_6\` module:

```java
com.linkedin.pegasus:data-avro-1_6:<current-version>  
```

If you are using avro 1.4, it’s adaptor module is included by default so
you don’t need to depend on it explicitly.

Schema translation is implemented by the
\`com.linkedin.data.avro.SchemaTranslator\` class.

For example, to convert from a avro schema, do:

```java
DataSchema pegasusDataSchema = SchemaTranslator.avroToDataSchema(avroSchema, options);  
```

And to convert to an avro schema, do:

```java
Schema avroSchema = SchemaTranslator.dataToAvroSchema(pegasusDataSchema, options);  
```

## Translating Data to and from Avro

Data translation is implemented by the
\`com.linkedin.data.avro.DataTranslator\` class. Translating data
requires that one has schemas for both formats (.avsc and .pdsc). Please
see above section section about translating schemas for details. Once
both schemas are available, data can be converted.

For example, to convert avro data, do:

```java
DataTranslator.dataMapToGenericRecord(data, pegasusDataSchema,
avroSchema); // for dataMaps  
// OR  
GenericRecord avroRecord = DataTranslator.dataMapToGenericRecord(recordTemplate.data(), recordTemplate(), avroSchema); // for record templates  
```

And to convert from avro data, do:

```  java
DataMap pegasusData = genericRecordToDataMap(avroRecord, pegasusDataSchema, avroSchema);  
```
