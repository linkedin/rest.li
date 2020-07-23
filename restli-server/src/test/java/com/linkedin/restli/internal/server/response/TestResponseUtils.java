/*
   Copyright (c) 2020 LinkedIn Corp.

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
package com.linkedin.restli.internal.server.response;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.SchemaFormatType;
import com.linkedin.data.schema.generator.AbstractGenerator;
import com.linkedin.data.schema.resolver.MultiFormatDataSchemaResolver;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.restli.internal.server.util.DataMapUtils;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Collections;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.reporters.Files;


public class TestResponseUtils
{
  final static String FS = File.separator;
  final static String testDir = System.getProperty("testDir", new File("src/test").getAbsolutePath());
  final static String pegasusDir = testDir + FS + "pegasus" + FS + "com" + FS + "linkedin" + FS + "restli" + FS + "server" + FS + "defaults";
  final static String resolverDir = testDir + FS + "pegasus";

  @BeforeTest
  public void beforeTest()
  {
    System.setProperty(AbstractGenerator.GENERATOR_RESOLVER_PATH, resolverDir);
  }

  @DataProvider(name = "default_serialization")
  public Object[][] schemaFilesForDefaultSerializationTest()
  {
    return new Object[][]{
        {"case1.json"},
        {"case2.json"},
        {"case3.json"},
        {"case4.json"},
        {"case5.json"},
        {"case6.json"},
        {"case7.json"},
        {"case8.json"},
    };
  }

  @Test(dataProvider = "default_serialization")
  public void testGetAbsentFieldsDefaultValues(String caseFilename)
  {
    try
    {
      MultiFormatDataSchemaResolver schemaResolver = MultiFormatDataSchemaResolver.withBuiltinFormats(resolverDir);
      String expectedDataJsonFile = Files.readFile(new File(pegasusDir + FS + caseFilename));
      DataMap caseData = DataMapUtils.readMap(new ByteArrayInputStream(expectedDataJsonFile.getBytes()), Collections.emptyMap());

      String schemaFileText = Files.readFile(new File(pegasusDir + FS + caseData.get("schema")));
      DataMap caseInput = (DataMap) caseData.get("input");
      DataMap caseExpect = (DataMap) caseData.get("expect");
      DataSchema schema = DataTemplateUtil.parseSchema(schemaFileText, schemaResolver, SchemaFormatType.PDL);
      DataMap dataWithDefault = ResponseUtils.fillInDefaultValues(schema, caseInput);
      Assert.assertEquals(dataWithDefault, caseExpect, (String) caseData.get("context"));
    }
    catch (Exception e)
    {
      Assert.fail("Test failed with exception: \n" + e);
    }
  }

  @AfterTest
  public void afterTest()
  {
    System.clearProperty(AbstractGenerator.GENERATOR_RESOLVER_PATH);
  }
}

