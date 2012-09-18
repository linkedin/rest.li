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
import com.linkedin.data.it.Predicate;
import com.linkedin.data.it.Predicates;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.SchemaParser;
import com.linkedin.data.schema.util.Filters;
import java.io.IOException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Test schemas with fields removed.
 */
public class TestFilteredSchemaDataTranslation
{
  /**
   * Removed derived field from Avro schema.
   */
  @Test
  public void testFilteredAvroSchemaDataTranslation() throws IOException
  {
    Object inputs[][] = {
      {
        "{ " +
        "  \"type\" : \"record\", " +
        "  \"name\" : \"Foo\", " +
        "  \"fields\" : [ " +
        "    { \"name\" : \"a\", \"type\" : \"int\" }, " +
        "    { \"name\" : \"b\", \"type\" : \"int\", \"optional\" : true }, " +
        "    { \"name\" : \"c\", \"type\" : \"int\", \"optional\" : true, \"derived\" : true } " +
        "  ] " +
        "}",
        Predicates.hasChildWithNameValue("derived", true),
        "{ " +
        "  \"type\" : \"record\", " +
        "  \"name\" : \"Foo\", " +
        "  \"fields\" : [ " +
        "    { \"name\" : \"a\", \"type\" : \"int\" }, " +
        "    { \"name\" : \"b\", \"type\" : [ \"null\", \"int\" ], \"default\" : null } " +
        "  ] " +
        "}",
        // "c" is dropped from output because it is not in the output schema
        "{ \"a\" : 1, \"b\" : 2, \"c\" : 3 }",
        "{ \"a\" : 1, \"b\" : { \"int\" : 2 } }",
        // "b" is translated to null and "c" is dropped from output because it is not in the output schema
        "{ \"a\" : 1, \"c\" : 3 }",
        "{ \"a\" : 1, \"b\" : null }",
      }
    };

    for (Object[] row : inputs)
    {
      int i = 0;
      String schemaText = (String) row[i++];
      Predicate predicate = (Predicate) row[i++];
      String avroSchemaText = (String) row[i++];

      RecordDataSchema schema = (RecordDataSchema) TestUtil.dataSchemaFromString(schemaText);
      NamedDataSchema filteredSchema = Filters.removeByPredicate(schema, predicate, new SchemaParser());
      Schema filteredAvroSchema = SchemaTranslator.dataToAvroSchema(filteredSchema);

      Schema expectedAvroSchema = Schema.parse(avroSchemaText);
      assertEquals(filteredAvroSchema, expectedAvroSchema);

      while (i < row.length)
      {
        String translationSourceJson = (String) row[i++];
        String translationResultJson = (String) row[i++];

        DataMap dataMap = TestUtil.dataMapFromString(translationSourceJson);
        GenericRecord genericRecord = DataTranslator.dataMapToGenericRecord(dataMap, schema, filteredAvroSchema);

        String avroJson = AvroUtil.jsonFromGenericRecord(genericRecord);
        DataMap avroJsonAsDataMap = TestUtil.dataMapFromString(avroJson);

        assertEquals(avroJsonAsDataMap, TestUtil.dataMapFromString(translationResultJson));
      }
    }
  }

  /**
   * Removed field from Pegasus schema.
   */
  @Test
  public void testFilteredDataSchemaDataTranslation() throws IOException
  {
    Object inputs[][] = {
      {
        "{ " +
        "  \"type\" : \"record\", " +
        "  \"name\" : \"Foo\", " +
        "  \"fields\" : [ " +
        "    { \"name\" : \"a\", \"type\" : \"int\" }, " +
        "    { \"name\" : \"b\", \"type\" : [ \"null\", \"int\" ], \"default\" : null }, " +
        "    { \"name\" : \"removeMe\", \"type\" : \"int\" } " +
        "  ] " +
        "}",
        Predicates.hasChildWithNameValue("name", "removeMe"),
        "{ " +
        "  \"type\" : \"record\", " +
        "  \"name\" : \"Foo\", " +
        "  \"fields\" : [ " +
        "    { \"name\" : \"a\", \"type\" : \"int\" }, " +
        "    { \"name\" : \"b\", \"type\" : \"int\", \"optional\" : true } " +
        "  ] " +
        "}",
        // "removeMe" is dropped from output because it is not in output schema
        "{ \"a\" : 1, \"b\" : { \"int\" : 2 }, \"removeMe\" : 3 }",
        "{ \"a\" : 1, \"b\" : 2 }",
        // "b" has null value is dropped from output, "removeMe" is dropped from output because it is not in output schema
        "{ \"a\" : 1, \"b\" : null, \"removeMe\" : 3 }",
        "{ \"a\" : 1 }",
      }
    };

    for (Object[] row : inputs)
    {
      int i = 0;
      String avroSchemaText = (String) row[i++];
      Predicate predicate = (Predicate) row[i++];
      String schemaText = (String) row[i++];

      Schema avroSchema = Schema.parse(avroSchemaText);
      System.out.println(avroSchema);
      RecordDataSchema schema = (RecordDataSchema) SchemaTranslator.avroToDataSchema(avroSchema);
      RecordDataSchema filteredSchema = (RecordDataSchema) Filters.removeByPredicate(schema, predicate, new SchemaParser());

      DataSchema expectedSchema = TestUtil.dataSchemaFromString(schemaText);
      System.out.println(filteredSchema);
      assertEquals(filteredSchema, expectedSchema);

      while (i < row.length)
      {
        String translationSourceJson = (String) row[i++];
        String translationExpectedJson = (String) row[i++];

        GenericRecord genericRecord = AvroUtil.genericRecordFromJson(translationSourceJson, avroSchema);
        DataMap dataMap = DataTranslator.genericRecordToDataMap(genericRecord, filteredSchema, avroSchema);

        assertEquals(dataMap, TestUtil.dataMapFromString(translationExpectedJson));
      }
    }
  }
}
