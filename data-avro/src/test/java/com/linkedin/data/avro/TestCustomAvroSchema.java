/*
   Copyright (c) 2012 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.linkedin.data.avro;


import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import com.linkedin.data.avro.util.AvroUtil;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.SchemaParser;
import java.io.IOException;
import java.util.List;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;


public class TestCustomAvroSchema
{
  private static final String DATA_SCHEMA_JSON =
    "{\n" +
    "  \"type\" : \"record\",\n" +
    "  \"name\" : \"AnyRecord\",\n" +
    "  \"namespace\" : \"com.linkedin.data.avro.test\",\n" +
    "  \"fields\" : [],\n" +
    "  \"avro\" : {\n" +
    "    \"translator\" : {\n" +
    "      \"class\" : \"com.linkedin.data.avro.AnyRecordTranslator\"\n" +
    "    },\n" +
    "    \"schema\" : {\n" +
    "      \"type\" : \"record\",\n" +
    "      \"name\" : \"AvroAnyRecord\",\n" +
    "      \"namespace\" : \"com.linkedin.data.avro.test.avro\",\n" +
    "      \"fields\" : [\n" +
    "        {\n" +
    "          \"name\" : \"type\",\n" +
    "          \"type\" : \"string\"\n" +
    "        },\n" +
    "        {\n" +
    "          \"name\" : \"value\",\n" +
    "          \"type\" : \"string\"\n" +
    "        }\n" +
    "      ]\n" +
    "    }\n" +
    "  }" +
    "}" +
    "{\n" +
    "  \"type\" : \"record\",\n" +
    "  \"name\" : \"AnyRecordClient\",\n" +
    "  \"namespace\" : \"com.linkedin.data.avro.test\",\n" +
    "  \"fields\" : [\n" +
    "    {\n" +
    "      \"name\" : \"required\",\n" +
    "      \"type\" : \"AnyRecord\"\n" +
    "    },\n" +
    "    {\n" +
    "      \"name\" : \"optional\",\n" +
    "      \"type\" : \"AnyRecord\",\n" +
    "      \"optional\" : true\n" +
    "    },\n" +
    "    {\n" +
    "      \"name\" : \"array\",\n" +
    "      \"type\" : { \"type\" : \"array\", \"items\" : \"AnyRecord\" }\n" +
    "    },\n" +
    "    {\n" +
    "      \"name\" : \"map\",\n" +
    "      \"type\" : { \"type\" : \"map\", \"values\" : \"AnyRecord\" }\n" +
    "    },\n" +
    "    {\n" +
    "      \"name\" : \"union\",\n" +
    "      \"type\" : [ \"string\", \"AnyRecord\" ]\n" +
    "    },\n" +
    "    {\n" +
    "      \"name\" : \"unionOptional\",\n" +
    "      \"type\" : [ \"string\", \"AnyRecord\" ],\n" +
    "      \"optional\" : true\n" +
    "    }\n" +
    "  ]\n" +
    "}";

  private static final RecordDataSchema ANYRECORD_SCHEMA;
  private static final RecordDataSchema ANYRECORDCLIENT_SCHEMA;

  static
  {
    try
    {
      SchemaParser parser = TestUtil.schemaParserFromString(DATA_SCHEMA_JSON);
      List<DataSchema> schemas = parser.topLevelDataSchemas();
      ANYRECORD_SCHEMA = (RecordDataSchema) schemas.get(0);
      ANYRECORDCLIENT_SCHEMA = (RecordDataSchema) schemas.get(1);
    }
    catch (IOException e)
    {
      throw new IllegalStateException(e);
    }
  }

  public static final String ANYRECORD_SCHEMA_FULLNAME = ANYRECORD_SCHEMA.getFullName();

  private static final String ANYRECORD_AVRO_JSON = "{\"type\":\"record\",\"name\":\"AvroAnyRecord\",\"namespace\":\"com.linkedin.data.avro.test.avro\",\"fields\":[{\"name\":\"type\",\"type\":\"string\"},{\"name\":\"value\",\"type\":\"string\"}]}";
  private static final String ANYRECORDCLIENT_AVRO_JSON = "{\"type\":\"record\",\"name\":\"AnyRecordClient\",\"namespace\":\"com.linkedin.data.avro.test\",\"fields\":[{\"name\":\"required\",\"type\":{\"type\":\"record\",\"name\":\"AvroAnyRecord\",\"namespace\":\"com.linkedin.data.avro.test.avro\",\"fields\":[{\"name\":\"type\",\"type\":\"string\"},{\"name\":\"value\",\"type\":\"string\"}]}},{\"name\":\"optional\",\"type\":[\"null\",\"com.linkedin.data.avro.test.avro.AvroAnyRecord\"],\"default\":null},{\"name\":\"array\",\"type\":{\"type\":\"array\",\"items\":\"com.linkedin.data.avro.test.avro.AvroAnyRecord\"}},{\"name\":\"map\",\"type\":{\"type\":\"map\",\"values\":\"com.linkedin.data.avro.test.avro.AvroAnyRecord\"}},{\"name\":\"union\",\"type\":[\"string\",\"com.linkedin.data.avro.test.avro.AvroAnyRecord\"]},{\"name\":\"unionOptional\",\"type\":[\"null\",\"string\",\"com.linkedin.data.avro.test.avro.AvroAnyRecord\"],\"default\":null}]}";

  @Test
  public void testSchemaTranslation() throws IOException
  {
    Object inputs[][] =
      {
        {
          ANYRECORD_SCHEMA,
          ANYRECORD_AVRO_JSON
        },
        {
          ANYRECORDCLIENT_SCHEMA,
          ANYRECORDCLIENT_AVRO_JSON
        }
      };

    for (Object[] row : inputs)
    {
      DataSchema schema = (DataSchema) row[0];
      String avroSchemaJson = (String) row[1];

      String avroJsonOutput = SchemaTranslator.dataToAvroSchemaJson(schema);
      assertEquals(TestUtil.dataMapFromString(avroJsonOutput), TestUtil.dataMapFromString(avroSchemaJson));
      Schema avroSchema = Schema.parse(avroJsonOutput);
      Schema avroSchema2 = SchemaTranslator.dataToAvroSchema(schema);
      assertEquals(avroSchema, avroSchema2);
      String avroSchemaToString = avroSchema.toString();
      assertEquals(Schema.parse(avroSchemaToString), Schema.parse(avroSchemaJson));
    }
  }

  private static final Schema ANYRECORD_AVRO_SCHEMA = SchemaTranslator.dataToAvroSchema(ANYRECORD_SCHEMA);
  private static final Schema ANYRECORDCLIENT_AVRO_SCHEMA = SchemaTranslator.dataToAvroSchema(ANYRECORDCLIENT_SCHEMA);

  @Test
  public void testCustomDataTranslation() throws IOException
  {
    Object[][] inputs =
      {
        {
          ANYRECORD_SCHEMA,
          ANYRECORD_AVRO_SCHEMA,
          "{ \"Foo\" : { \"int\" : 1 } }",
          "{ \"type\" : \"Foo\", \"value\" : \"{\\\"int\\\":1}\" }"
        },
        {
          // required field
          ANYRECORDCLIENT_SCHEMA,
          ANYRECORDCLIENT_AVRO_SCHEMA,
          "{ " +
          "  \"required\" : { \"Foo\" : { \"int\" : 1 } }, " +
          "  \"array\" : [], \"map\" : {}, " +
          "  \"union\" : { \"string\" : \"u1\" } " +
          "}",
          "{ " +
          "  \"required\" : { \"type\" : \"Foo\", \"value\" : \"{\\\"int\\\":1}\" }, " +
          "  \"optional\" : null, " +
          "  \"array\" : [], \"map\" : {}, " +
          "  \"union\" : { \"string\" : \"u1\" }, " +
          "  \"unionOptional\" : null " +
          "}"
        },
        {
          // optional field
          ANYRECORDCLIENT_SCHEMA,
          ANYRECORDCLIENT_AVRO_SCHEMA,
          "{ " +
          "  \"required\" : { \"Foo\" : { \"int\" : 1 } }, " +
          "  \"optional\" : { \"Foo\" : { \"int\" : 2 } }, " +
          "  \"array\" : [], \"map\" : {}, " +
          "  \"union\" : { \"string\" : \"u1\" } " +
          "}",
          "{ " +
          "  \"required\" : { \"type\" : \"Foo\", \"value\" : \"{\\\"int\\\":1}\" }, " +
          "  \"optional\" : { \"AvroAnyRecord\" : { \"type\" : \"Foo\", \"value\" : \"{\\\"int\\\":2}\" } }, " +
          "  \"array\" : [], \"map\" : {}, " +
          "  \"union\" : { \"string\" : \"u1\" }, " +
          "  \"unionOptional\" : null " +
          "}"
        },
        {
          // array element
          ANYRECORDCLIENT_SCHEMA,
          ANYRECORDCLIENT_AVRO_SCHEMA,
          "{ " +
          "  \"required\" : { \"Foo\" : { \"int\" : 1 } }, " +
          "  \"array\" : [ { \"Foo\" : { \"int\" : 2 } } ], " +
          "  \"map\" : {}, " +
          "  \"union\" : { \"string\" : \"u1\" } " +
          "}",
          "{ " +
          "  \"required\" : { \"type\" : \"Foo\", \"value\" : \"{\\\"int\\\":1}\" }, " +
          "  \"optional\" : null, " +
          "  \"array\" : [ { \"type\" : \"Foo\", \"value\" : \"{\\\"int\\\":2}\" } ], " +
          "  \"map\" : {}, " +
          "  \"union\" : { \"string\" : \"u1\" }, " +
          "  \"unionOptional\" : null " +
          "}"
        },
        {
          // map entry
          ANYRECORDCLIENT_SCHEMA,
          ANYRECORDCLIENT_AVRO_SCHEMA,
          "{ " +
          "  \"required\" : { \"Foo\" : { \"int\" : 1 } }, " +
          "  \"array\" : [], " +
          "  \"map\" : { \"2\" : { \"Foo\" : { \"int\" : 2 } } }, " +
          "  \"union\" : { \"string\" : \"u1\" } " +
          "}",
          "{ " +
          "  \"required\" : { \"type\" : \"Foo\", \"value\" : \"{\\\"int\\\":1}\" }, " +
          "  \"optional\" : null, " +
          "  \"array\" : [], " +
          "  \"map\" : { \"2\" : { \"type\" : \"Foo\", \"value\" : \"{\\\"int\\\":2}\" } }, " +
          "  \"union\" : { \"string\" : \"u1\" }, " +
          "  \"unionOptional\" : null " +
          "}"
        },
        {
          // union member
          ANYRECORDCLIENT_SCHEMA,
          ANYRECORDCLIENT_AVRO_SCHEMA,
          "{ " +
          "  \"required\" : { \"Foo\" : { \"int\" : 1 } }, " +
          "  \"array\" : [], " +
          "  \"map\" : {}, " +
          "  \"union\" : { \"com.linkedin.data.avro.test.AnyRecord\" : { \"Foo\" : { \"int\" : 2 } } } " +
          "}",
          "{ " +
          "  \"required\" : { \"type\" : \"Foo\", \"value\" : \"{\\\"int\\\":1}\" }, " +
          "  \"optional\" : null, " +
          "  \"array\" : [], " +
          "  \"map\" : {}, " +
          "  \"union\" : { \"AvroAnyRecord\" : { \"type\" : \"Foo\", \"value\" : \"{\\\"int\\\":2}\" } }, " +
          "  \"unionOptional\" : null " +
          "}"
        },
        {
          // optional union field, field value is custom AnyRecord
          ANYRECORDCLIENT_SCHEMA,
          ANYRECORDCLIENT_AVRO_SCHEMA,
          "{ " +
          "  \"required\" : { \"Foo\" : { \"int\" : 1 } }, " +
          "  \"array\" : [], " +
          "  \"map\" : {}, " +
          "  \"union\" : { \"string\" : \"u1\" }, " +
          "  \"unionOptional\" : { \"com.linkedin.data.avro.test.AnyRecord\" : { \"Foo\" : { \"int\" : 2 } } } " +
          "}",
          "{ " +
          "  \"required\" : { \"type\" : \"Foo\", \"value\" : \"{\\\"int\\\":1}\" }, " +
          "  \"optional\" : null, " +
          "  \"array\" : [], " +
          "  \"map\" : {}, " +
          "  \"union\" : { \"string\" : \"u1\" }, " +
          "  \"unionOptional\" : { \"AvroAnyRecord\" : { \"type\" : \"Foo\", \"value\" : \"{\\\"int\\\":2}\" } } " +
          "}"
        },
        {
          // optional union field, field is absent.
          ANYRECORDCLIENT_SCHEMA,
          ANYRECORDCLIENT_AVRO_SCHEMA,
          "{ " +
          "  \"required\" : { \"Foo\" : { \"int\" : 1 } }, " +
          "  \"array\" : [], " +
          "  \"map\" : {}, " +
          "  \"union\" : { \"string\" : \"u1\" } " +
          "}",
          "{ " +
          "  \"required\" : { \"type\" : \"Foo\", \"value\" : \"{\\\"int\\\":1}\" }, " +
          "  \"optional\" : null, " +
          "  \"array\" : [], " +
          "  \"map\" : {}, " +
          "  \"union\" : { \"string\" : \"u1\" }, " +
          "  \"unionOptional\" : null " +
          "}"
        }
      };

    boolean debug = false;
    for (Object[] row : inputs)
    {
      RecordDataSchema schema = (RecordDataSchema) row[0];
      Schema avroSchema = (Schema) row[1];
      String dataJson = (String) row[2];
      String avroDataJson = (String) row[3];

      if (debug)
      {
        TestUtil.out.println(schema);
        TestUtil.out.println(avroSchema);
      }

      // translate from Data to Avro generic

      DataMap inputDataMap = TestUtil.dataMapFromString(dataJson);
      GenericRecord genericRecord = DataTranslator.dataMapToGenericRecord(inputDataMap, schema);
      GenericRecord expectedGenericRecord = AvroUtil.genericRecordFromJson(avroDataJson, avroSchema);
      assertEquals(genericRecord.toString(), expectedGenericRecord.toString());

      // translate form Avro generic back to Data

      Object data = DataTranslator.genericRecordToDataMap(genericRecord, schema, avroSchema);
      assertEquals(data, inputDataMap);
    }
  }
}
