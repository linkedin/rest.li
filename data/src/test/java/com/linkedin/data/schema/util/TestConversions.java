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


import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.SchemaParser;
import java.io.IOException;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;


/**
 * Test {@link Conversions}.
 */
public class TestConversions
{
  private static String goodInputs[] =
    {
      "{ \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [ ] }",
      "{ \"type\" : \"typeref\", \"name\" : \"Foo\", \"ref\" : \"int\" }"
    };

  private static String badInputs[] =
    {
      "{ \"type\" : \"record\", \"name\" : \"Foo\" }",
      "{ \"type\" : \"typeref\", \"ref\" : \"int\" }"
    };

  @Test
  public void testConvertDataSchemaToDataMap() throws IOException
  {
    for (String good : goodInputs)
    {
      NamedDataSchema dataSchema = (NamedDataSchema) TestUtil.dataSchemaFromString(good);

      DataMap mapFromSchema = Conversions.dataSchemaToDataMap(dataSchema);
      DataMap mapFromString = TestUtil.dataMapFromString(good);

      assertEquals(mapFromSchema, mapFromString);
    }
  }

  @Test
  public void testConvertDataMapToDataSchema() throws IOException
  {
    for (String good : goodInputs)
    {
      NamedDataSchema dataSchema = (NamedDataSchema) TestUtil.dataSchemaFromString(good);

      DataMap mapFromString = TestUtil.dataMapFromString(good);
      SchemaParser parser = new SchemaParser();
      DataSchema schemaFromMap = Conversions.dataMapToDataSchema(mapFromString, parser);

      assertEquals(schemaFromMap, dataSchema);
    }

    for (String bad : badInputs)
    {
      DataMap mapFromString = TestUtil.dataMapFromString(bad);
      SchemaParser parser = new SchemaParser();
      DataSchema schemaFromMap = Conversions.dataMapToDataSchema(mapFromString, parser);
      assertNull(schemaFromMap);
      assertTrue(parser.hasError());
    }
  }
}
