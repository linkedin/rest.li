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

package com.linkedin.pegasus.generator.test;


import com.linkedin.data.TestUtil;
import com.linkedin.pegasus.generator.PegasusDataTemplateGenerator;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.testng.annotations.Test;

import static com.linkedin.data.TestUtil.asMap;
import static com.linkedin.data.TestUtil.ensureEmptyOutputDir;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


public class TestBadSchemas
{
  private final Object _badSchemas[][] =
    {
      {
        asMap(
          "com/linkedin/pegasus/generator/test/ArrayNameDuplicateTest.pdsc",
          "{\n" +
          "  \"type\" : \"record\",\n" +
          "  \"name\" : \"ArrayNameDuplicateTest\",\n" +
          "  \"namespace\" : \"com.linkedin.pegasus.generator.test\",\n" +
          "  \"fields\" : [\n" +
          "    {\n" +
          "      \"name\" : \"x\",\n" +
          "      \"type\" : {\n" +
          "        \"type\" : \"enum\",\n" +
          "        \"name\" : \"X\",\n" +
          "        \"symbols\" : [ \"X\" ]\n" +
          "      }\n" +
          "    },\n" +
          "    {\n" +
          "      \"name\" : \"arrayOfX\",\n" +
          "      \"type\" : {\n" +
          "        \"type\" : \"array\",\n" +
          "        \"items\" : \"X\"\n" +
          "      }\n" +
          "    },\n" +
          "    {\n" +
          "      \"name\" : \"xArray\",\n" +
          "      \"type\" : {\n" +
          "        \"type\" : \"enum\",\n" +
          "        \"name\" : \"XArray\",\n" +
          "        \"symbols\" : [ \"Y\" ]\n" +
          "      }\n" +
          "    }\n" +
          "  ]\n" +
          "}"
        ),
        IllegalArgumentException.class,
        "Class name conflict detected, class name: com.linkedin.pegasus.generator.test.XArray, class already bound to schema: { \"type\" : \"array\", \"items\" : { \"type\" : \"enum\", \"name\" : \"X\", \"namespace\" : \"com.linkedin.pegasus.generator.test\", \"symbols\" : [ \"X\" ] } }, attempting to rebind to schema: { \"type\" : \"enum\", \"name\" : \"XArray\", \"namespace\" : \"com.linkedin.pegasus.generator.test\", \"symbols\" : [ \"Y\" ] }"
      },
      {
        asMap(
          "com/linkedin/pegasus/generator/test/FieldDefinedTwice.pdsc",
          "{\n" +
          "  \"type\" : \"record\",\n" +
          "  \"name\" : \"FieldDefinedTwice\",\n" +
          "  \"namespace\" : \"com.linkedin.pegasus.generator.test\",\n" +
          "  \"fields\" : [\n" +
          "    {\n" +
          "      \"name\" : \"foo\",\n" +
          "      \"type\" : \"long\"\n" +
          "    },\n" +
          "    {\n" +
          "      \"name\" : \"foo\",\n" +
          "      \"type\" : \"string\"\n" +
          "    }\n" +
          "  ]\n" +
          "}"
        ),
        IOException.class,
        "build/test/testGeneratorBadSchemas/pegasuscom/linkedin/pegasus/generator/test/FieldDefinedTwice.pdsc,4,54: Field \"foo\" defined more than once, with \"long\" and \"string\"."
      }
    };

  private final static String _sourceDirName = "testGeneratorBadSchemas/pegasus";
  private final static String _targetDirName = "testGeneratorBadSchemas/codegen/out";

  @Test
  public void testBadSchemas() throws IOException
  {
    boolean debug = false;

    System.setProperty("generator.resolver.path", "");

    for (Object[] row : _badSchemas)
    {
      @SuppressWarnings("unchecked")
      Map<String,String> testSchemas = (Map<String,String>) row[0];

      File testDir = TestUtil.testDir(_sourceDirName, debug);
      Map<File, Map.Entry<String,String>> files = TestUtil.createSchemaFiles(testDir, testSchemas, debug);

      File targetDir = TestUtil.testDir(_targetDirName, debug);
      ensureEmptyOutputDir(targetDir, debug);

      String[] args = new String[files.size() + 1];
      int i = 0;
      args[i++] = targetDir.getCanonicalPath();
      for (Map.Entry<File, Map.Entry<String,String>> fileEntry : files.entrySet())
      {
        File file = fileEntry.getKey();
        String fileName = file.getCanonicalPath();
        args[i++] = fileName;
      }

      Class<?> expectedExceptionClass = null;
      if (row.length > 1)
      {
        expectedExceptionClass = (Class<?>) row[1];
      }

      try
      {
        PegasusDataTemplateGenerator.main(args);
        assertTrue(expectedExceptionClass == null);
      }
      catch (Exception exc)
      {
        assertTrue(expectedExceptionClass != null);
        assertEquals(exc.getClass(), expectedExceptionClass);
        String message = exc.getMessage();
        for (int j = 2; j < row.length; j++)
        {
          String expectedString = (String) row[j];
          assertTrue(message.contains(expectedString), message + " does not contain " + expectedString);
        }
      }
    }
  }
}
