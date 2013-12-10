package com.linkedin.pegasus.generator.test;


import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertTrue;


public class TestLongStringLiteral
{
  @Test
  public void testSchema()
  {
    DataSchema schema = DataTemplateUtil.getSchema(LongStringLiteral.class);
    String schemaText = schema.toString();
    assertTrue(schemaText.length() > 65536);
  }
}
