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
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.testng.annotations.Test;

import static com.linkedin.data.TestUtil.dataSchemaFromString;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;


public class TestCustomAvroSchema
{
  private static final String ANYRECORD_AVRO_FULL_NAME =
    "\"com.linkedin.data.avro.test.avro.AvroAnyRecord\"";

  private static final String ANYRECORD_AVRO_UNION_MEMBER =
    "\"##NS(com.linkedin.data.avro.test.avro.)AvroAnyRecord\"";

  private static final String ANYRECORD_AVRO_SCHEMA_JSON =
    "    {\n" +
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
    "    }\n";

  private static final String ANYRECORD_DATA_SCHEMA_JSON =
    "{\n" +
    "  \"type\" : \"record\",\n" +
    "  \"name\" : \"AnyRecord\",\n" +
    "  \"namespace\" : \"com.linkedin.data.avro.test\",\n" +
    "  \"fields\" : [],\n" +
    "  \"avro\" : {\n" +
    "    \"translator\" : {\n" +
    "      \"class\" : \"com.linkedin.data.avro.AnyRecordTranslator\"\n" +
    "    },\n" +
    "    \"schema\" : " +
    ANYRECORD_AVRO_SCHEMA_JSON +
    "  }\n" +
    "}\n";

  private static final String DATA_SCHEMA_JSON_TEMPLATE =
    ANYRECORD_DATA_SCHEMA_JSON +
    "{\n" +
    "  \"type\" : \"typeref\",\n" +
    "  \"name\" : \"a.b.AnyRecordRef\",\n" +
    "  \"ref\" : \"com.linkedin.data.avro.test.AnyRecord\"\n" +
    "}\n" +
    "{\n" +
    "  \"type\" : \"record\",\n" +
    "  \"name\" : \"FooRecord\",\n" +
    "  \"namespace\" : \"com.linkedin.data.avro.test\",\n" +
    "  \"fields\" : [\n" +
    "    ##FIELDS" +
    "  ]\n" +
    "}";

  private static final String AVRO_SCHEMA_JSON_TEMPLATE =
    "{\n" +
    "  \"type\" : \"record\",\n" +
    "  \"name\" : \"FooRecord\",\n" +
    "  \"namespace\" : \"com.linkedin.data.avro.test\",\n" +
    "  \"fields\" : [\n" +
    "    ##FIELDS" +
    "  ]\n" +
    "}";

  @Test
  public void testSchemaTranslation() throws IOException
  {
    Object inputs[][] =
      {
        {
          ANYRECORD_DATA_SCHEMA_JSON,
          ANYRECORD_AVRO_SCHEMA_JSON
        },
        {
          // Verify that fields are processed, such as default value translation
          // does not occur when avro override is present
          "{\n" +
          "  \"type\" : \"record\",\n" +
          "  \"name\" : \"Foo\",\n" +
          "  \"avro\" :\n" +
          "  {\n" +
          "    \"schema\" :\n" +
          "    {\n" +
          "      \"type\" : \"record\",\n" +
          "      \"name\" : \"Foo\",\n" +
          "      \"fields\" : [\n" +
           "      ]\n" +
          "    },\n" +
          "    \"translator\" :\n" +
          "    {\n" +
          "      \"class\" : \"FooTranslator\"\n" +
          "    }\n" +
          "  },\n" +
          "  \"fields\" : [\n" +
          "    {\n" +
          "      \"name\" : \"f1\",\n" +
          "      \"type\" : {\n" +
          "        \"type\" : \"record\",\n" +
          "        \"name\" : \"Bar\",\n" +
          "        \"fields\" : [\n" +
          "          {\n" +
          "            \"name\" : \"bf1\",\n" +
          "            \"type\" : \"string\",\n" +
          "            \"optional\" : true\n" +
          "          }\n" +
          "        ]\n" +
          "      },\n" +
          "      \"default\" : { \"bf1\" : \"defaultValue\" }\n" +
          "    }\n" +
          "  ]\n" +
          "}\n",
          "{\n" +
          "  \"name\" : \"Foo\",\n" +
          "  \"type\" : \"record\",\n" +
          "  \"fields\" : []\n" +
          "}"
        }
      };

    for (Object[] row : inputs)
    {
      String dataSchemaJson = (String) row[0];
      String avroSchemaJson = (String) row[1];

      DataSchema schema = dataSchemaFromString(dataSchemaJson);
      String avroJsonOutput = SchemaTranslator.dataToAvroSchemaJson(schema);
      assertEquals(TestUtil.dataMapFromString(avroJsonOutput), TestUtil.dataMapFromString(avroSchemaJson));
      Schema avroSchema = Schema.parse(avroJsonOutput);
      Schema avroSchema2 = SchemaTranslator.dataToAvroSchema(schema);
      assertEquals(avroSchema, avroSchema2);
      String avroSchemaToString = avroSchema.toString();
      assertEquals(Schema.parse(avroSchemaToString), Schema.parse(avroSchemaJson));
    }
  }

  @Test
  public void testCustomSchemaAndDataTranslation() throws IOException
  {
    Object[][] inputs =
      {
        {
          // required field
          "{ \"name\" : \"required\", \"type\" : \"AnyRecord\" }",
          "{ \"name\" : \"required\", \"type\" : ##ANYRECORD_FULL_JSON }",
          "{ " +
          "  \"required\" : { \"Foo\" : { \"int\" : 1 } } " +
          "}",
          "{ " +
          "  \"required\" : { \"type\" : \"Foo\", \"value\" : \"{\\\"int\\\":1}\" } " +
          "}"
        },
        {
          // required field, appears more than once
          "{ \"name\" : \"required\", \"type\" : \"AnyRecord\" }, " +
          "{ \"name\" : \"again\", \"type\" : \"AnyRecord\" }",
          "{ \"name\" : \"required\", \"type\" : ##ANYRECORD_FULL_JSON }," +
          "{ \"name\" : \"again\", \"type\" : ##ANYRECORD_NAME }",
          "{ " +
          "  \"required\" : { \"Foo\" : { \"int\" : 1 } }, " +
          "  \"again\" : { \"Bar\" : { \"string\" : \"s\" } }" +
          "}",
          "{ " +
          "  \"required\" : { \"type\" : \"Foo\", \"value\" : \"{\\\"int\\\":1}\" }, " +
          "  \"again\" : { \"type\" : \"Bar\", \"value\" : \"{\\\"string\\\":\\\"s\\\"}\" } " +
          "}"
        },
        {
          // optional field
          "{ \"name\" : \"optional\", \"type\" : \"AnyRecord\", \"optional\" : true }",
          "{ \"name\" : \"optional\", \"type\" : [ \"null\", ##ANYRECORD_FULL_JSON ], \"default\" : null }",
          "{ " +
          "  \"optional\" : { \"Foo\" : { \"int\" : 2 } } " +
          "}",
          "{ " +
          "  \"optional\" : { ##ANYRECORD_AVRO_UNION_MEMBER : { \"type\" : \"Foo\", \"value\" : \"{\\\"int\\\":2}\" } }, " +
          "}"
        },
        {
          // array element
          "{ \"name\" : \"array\", \"type\" : { \"type\" : \"array\", \"items\" : \"AnyRecord\" } }",
          "{ \"name\" : \"array\", \"type\" : { \"type\" : \"array\", \"items\" : ##ANYRECORD_FULL_JSON } }",
          "{ " +
          "  \"array\" : [ { \"Foo\" : { \"int\" : 2 } } ] " +
          "}",
          "{ " +
          "  \"array\" : [ { \"type\" : \"Foo\", \"value\" : \"{\\\"int\\\":2}\" } ] " +
          "}"
        },
        {
          // map entry
          "{ \"name\" : \"map\", \"type\" : { \"type\" : \"map\", \"values\" : \"AnyRecord\" } }",
          "{ \"name\" : \"map\", \"type\" : { \"type\" : \"map\", \"values\" : ##ANYRECORD_FULL_JSON } }",
          "{ " +
          "  \"map\" : { \"2\" : { \"Foo\" : { \"int\" : 2 } } } " +
          "}",
          "{ " +
          "  \"map\" : { \"2\" : { \"type\" : \"Foo\", \"value\" : \"{\\\"int\\\":2}\" } } " +
          "}"
        },
        {
          // union member
          "{ \"name\" : \"union\", \"type\" : [ \"AnyRecord\", \"string\" ] }",
          "{ \"name\" : \"union\", \"type\" : [ ##ANYRECORD_FULL_JSON, \"string\" ] }",
          "{ " +
          "  \"union\" : { \"com.linkedin.data.avro.test.AnyRecord\" : { \"Foo\" : { \"int\" : 2 } } } " +
          "}",
          "{ " +
          "  \"union\" : { ##ANYRECORD_AVRO_UNION_MEMBER : { \"type\" : \"Foo\", \"value\" : \"{\\\"int\\\":2}\" } } " +
          "}"
        },
        {
          // optional union field, field value is custom AnyRecord
          "{ \"name\" : \"union\", \"type\" : [ \"AnyRecord\", \"string\" ], \"optional\" : true }",
          "{ \"name\" : \"union\", \"type\" : [ \"null\", ##ANYRECORD_FULL_JSON, \"string\" ], \"default\" : null }",
          "{ " +
          "  \"union\" : { \"com.linkedin.data.avro.test.AnyRecord\" : { \"Foo\" : { \"int\" : 2 } } } " +
          "}",
          "{ " +
          "  \"union\" : { ##ANYRECORD_AVRO_UNION_MEMBER : { \"type\" : \"Foo\", \"value\" : \"{\\\"int\\\":2}\" } } " +
          "}"
        },
        {
          // optional union field, field is absent.
          "{ \"name\" : \"union\", \"type\" : [ \"AnyRecord\", \"string\" ], \"optional\" : true }",
          "{ \"name\" : \"union\", \"type\" : [ \"null\", ##ANYRECORD_FULL_JSON, \"string\" ], \"default\" : null }",
          "{ }",
          "{ " +
          "  \"union\" : null " +
          "}"
        }
      };

    for (Object[] row : inputs)
    {
      String dataSchemaFieldsJson = (String) row[0];
      String avroSchemaFieldsJson = (String) row[1];
      String dataJson = (String) row[2];
      String avroDataJson = (String) row[3];
      avroDataJson = avroDataJson.replaceAll("##ANYRECORD_AVRO_UNION_MEMBER", ANYRECORD_AVRO_UNION_MEMBER);
      avroDataJson = TestAvroUtil.namespaceProcessor(avroDataJson);

      translate(dataSchemaFieldsJson, avroSchemaFieldsJson, dataJson, avroDataJson);

      String dataSchemaFieldsJsonWithTyperef = dataSchemaFieldsJson.replace("AnyRecord", "a.b.AnyRecordRef");
      assertTrue(dataSchemaFieldsJsonWithTyperef.contains("a.b.AnyRecordRef"));
      translate(dataSchemaFieldsJsonWithTyperef, avroSchemaFieldsJson, dataJson, avroDataJson);
    }
  }

  private void translate(String dataSchemaFieldsJson, String avroSchemaFieldsJson, String dataJson, String avroDataJson)
    throws IOException
  {
    boolean debug = false;

    String fullSchemaJson = DATA_SCHEMA_JSON_TEMPLATE.replace("##FIELDS", dataSchemaFieldsJson);
    String avroSchemaFieldsJsonAfterVariableExpansion =
      avroSchemaFieldsJson
        .replace("##ANYRECORD_FULL_JSON", ANYRECORD_AVRO_SCHEMA_JSON)
        .replace("##ANYRECORD_NAME", ANYRECORD_AVRO_FULL_NAME);
    String fullAvroSchemaJson = AVRO_SCHEMA_JSON_TEMPLATE.replace("##FIELDS", avroSchemaFieldsJsonAfterVariableExpansion);

    SchemaParser parser = TestUtil.schemaParserFromString(fullSchemaJson);
    assertFalse(parser.hasError(), parser.errorMessage());
    RecordDataSchema schema = (RecordDataSchema) parser.topLevelDataSchemas().get(2);

    String avroJsonOutput = SchemaTranslator.dataToAvroSchemaJson(schema);
    assertEquals(TestUtil.dataMapFromString(avroJsonOutput), TestUtil.dataMapFromString(fullAvroSchemaJson));
    Schema avroSchema = Schema.parse(avroJsonOutput);
    Schema avroSchema2 = SchemaTranslator.dataToAvroSchema(schema);
    assertEquals(avroSchema, avroSchema2);
    String avroSchemaToString = avroSchema.toString();
    assertEquals(Schema.parse(avroSchemaToString), Schema.parse(fullAvroSchemaJson));

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
