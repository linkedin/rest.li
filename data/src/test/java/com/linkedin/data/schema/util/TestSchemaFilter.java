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

package com.linkedin.data.schema.util;


import com.linkedin.data.TestUtil;
import com.linkedin.data.it.Predicate;
import com.linkedin.data.it.Predicates;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.SchemaParser;
import java.io.IOException;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


/**
 * Test {@link com.linkedin.data.schema.util.Filters}
 */
public class TestSchemaFilter
{
  @Test
  public void testDataSchemaFilter() throws IOException
  {
    Object inputs[][] =
      {
        {
          "{ \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [ ] }",
          Predicates.alwaysFalse(),
          "{ \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [ ] }",
        },
        {
          "{ \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\", \"derived\" : true } ] }",
          Predicates.hasChildWithNameValue("derived", Boolean.TRUE),
          "{ \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [ ] }",
        },
        {
          "{ \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : [ \"string\", \"int\" ], \"default\" : { \"string\" : \"abc\" } } ] }",
          Predicates.valueEquals("int"),
          "{ \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : [ \"string\" ], \"default\" : { \"string\" : \"abc\" } } ] }",
        },
        {
          "{ \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : \"int\", \"derived\" : true } ] }",
          Predicates.and(
            Predicates.hasChildWithNameValue("derived", Boolean.TRUE),
            Predicates.parent(
              Predicates.and(
                Predicates.nameEquals("fields"),
                Predicates.parent(Predicates.hasChildWithNameValue("type", "record"))
              )
            )
          ),
          "{ \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [ ] }",
        },
        {
          "{ \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [ ] }",
          Predicates.nameEquals("fields"),
          Filters.INVALID_SCHEMA_LEFT
        },
        {
          "{ \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [ ] }",
          Predicates.alwaysTrue(),
          Filters.NO_SCHEMA_LEFT
        },
        {
          "{ \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [ ] }",
          Predicates.alwaysTrue(),
          Filters.NO_SCHEMA_LEFT
        }
      };

    testInputs(inputs, false);
  }

  @Test
  public void testAvroSchemaFilter() throws IOException
  {
    Object inputs[][] =
      {
        {
          "{ \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : [ \"string\", \"int\" ], \"default\" : \"abc\" } ] }",
          Predicates.valueEquals("int"),
          "{ \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [ { \"name\" : \"f1\", \"type\" : [ \"string\" ], \"default\" : \"abc\" } ] }",
        },
      };

    testInputs(inputs, true);
  }

  private static NamedDataSchema dataSchemaFromString(String s, boolean isAvroUnionMode) throws IOException
  {
    SchemaParser parser = new SchemaParser();
    parser.getValidationOptions().setAvroUnionMode(isAvroUnionMode);
    parser.parse(TestUtil.inputStreamFromString(s));
    if (parser.hasError())
    {
      TestUtil.out.println("ERROR: " + parser.errorMessage());
      return null;
    }
    return (NamedDataSchema) parser.topLevelDataSchemas().get(parser.topLevelDataSchemas().size() - 1);
  }

  private static void testInputs(Object inputs[][], boolean isAvroUnionMode) throws IOException
  {
    for (Object[] row : inputs)
    {
      String schemaText = (String) row[0];
      Predicate predicate = (Predicate) row[1];
      String expected = (String) row[2];
      NamedDataSchema schema = dataSchemaFromString(schemaText, isAvroUnionMode);

      DataSchema filteredSchema = null;
      SchemaParser parser = new SchemaParser();
      parser.getValidationOptions().setAvroUnionMode(isAvroUnionMode);
      filteredSchema = Filters.removeByPredicate(schema, predicate, parser);
      if (filteredSchema != null)
      {
        // Schema string match
        String expectedSchemaText = expected;
        DataSchema expectedSchema = dataSchemaFromString(expectedSchemaText, isAvroUnionMode);
        assertEquals(filteredSchema.toString(), expectedSchema.toString());
        assertEquals(filteredSchema, expectedSchema);
      }
      else
      {
        String parserMessage = parser.errorMessage();
        assertTrue(parserMessage.contains(expected), "\nContains :" + expected + "\nActual   :" + parserMessage);
      }
    }
  }
}
