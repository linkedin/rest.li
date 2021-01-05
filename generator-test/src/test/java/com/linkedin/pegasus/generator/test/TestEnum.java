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

package com.linkedin.pegasus.generator.test;


import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.pegasus.generator.test.idl.enums.EnumProperties;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


public class TestEnum
{
  private static <T extends Enum<T>> void testEnum(Class<T> enumClass)
  {
    try
    {
      assertTrue(Enum.class.isAssignableFrom(enumClass));

      // has embedded EnumDataSchema
      DataSchema schema = DataTemplateUtil.getSchema(enumClass);
      assertNotNull(schema);
      assertTrue(schema instanceof EnumDataSchema);

      // get symbols
      EnumDataSchema enumSchema = (EnumDataSchema) schema;
      Set<String> schemaSymbols = new HashSet<String>(enumSchema.getSymbols());
      assertNotNull(schemaSymbols);

      for (String symbol : schemaSymbols)
      {
        // IllegalArgumentException thrown if not valid symbol
        Enum.valueOf(enumClass, symbol);
      }

      // IllegalArgumentException thrown if not valid symbol
      Enum.valueOf(enumClass, "$UNKNOWN");
    }
    catch (Exception exc)
    {
      fail("Unexpected exception", exc);
    }
  }

  @Test
  public void testEnum()
  {
    testEnum(EnumFruits.class);
    testEnum(EnumEmpty.class);
  }

  @Test
  public void testEnumProperties()
  {
    EnumDataSchema enumPropertiesSchema = EnumProperties.dataSchema();
    assertNotNull(enumPropertiesSchema);

    Map<String, Object> appleProps = enumPropertiesSchema.getSymbolProperties(EnumProperties.APPLE.name());
    assertNotNull(appleProps);
    assertEquals(appleProps.get("color"), "red");

    assertNull(enumPropertiesSchema.getSymbolProperties("unknown"));

    // Fruits.pdl enum doesn't have symbol properties.
    EnumDataSchema fruitsSchema = Fruits.dataSchema();
    assertNotNull(fruitsSchema);

    appleProps = fruitsSchema.getSymbolProperties(Fruits.APPLE.name());
    assertNotNull(appleProps);
    assertTrue(appleProps.isEmpty());
  }
}
