/*
   Copyright (c) 2013 LinkedIn Corp.

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

package com.linkedin.pegasus.generator.override;

import com.linkedin.data.DataList;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;


/**
 * @author Min Chen
 */
public class TestLongStringLiteral
{
  private static final String LOREM = "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

  @Test
  public void testSchema()
  {
    DataSchema schema = DataTemplateUtil.getSchema(LongStringLiteral.class);
    String schemaText = schema.toString();
    assertTrue(schemaText.length() > 65536);

    RecordDataSchema recordDataSchema = (RecordDataSchema) schema;
    RecordDataSchema.Field field = recordDataSchema.getField("text");
    DataList defaultValue = (DataList) field.getDefault();
    assertEquals(defaultValue.size(), 400);
    for (Object s : defaultValue)
    {
      assertEquals(s, LOREM);
    }
  }
}
