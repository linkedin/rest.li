package com.linkedin.data.schema.generator;


import com.linkedin.pegasus.generator.test.IntUnionRecord;
import com.linkedin.pegasus.generator.test.StringUnionRecord;

import java.io.IOException;

import org.testng.annotations.Test;


/**
 * @author Keren Jin
 */
public class TestPegasusDataTemplateGenerator
{
  // this methods tests if the nested union class is generated in the defining class
  // if the compilation fails, the test fails
  // see also build.gradle
  @Test
  public void testIncludeUnion() throws IOException
  {
    // add usage to ensure the import will not be automatically trimmed by IDE
    IntUnionRecord.IntUnion intUnion;
    StringUnionRecord.StringUnion stringUnion;
  }
}
