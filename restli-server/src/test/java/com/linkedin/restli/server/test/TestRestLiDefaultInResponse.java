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
package com.linkedin.restli.server.test;

import com.google.common.collect.ImmutableMap;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.SchemaFormatType;
import com.linkedin.data.schema.SchemaParserFactory;
import com.linkedin.data.schema.generator.AbstractGenerator;
import com.linkedin.data.schema.resolver.FileDataSchemaResolver;
import com.linkedin.data.schema.resolver.MultiFormatDataSchemaResolver;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.restli.internal.server.response.ResponseUtils;
import java.io.File;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.reporters.Files;


public class TestRestLiDefaultInResponse
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
    DataList recordBField1 = new DataList(1);
    recordBField1.add(-1);

    DataMap recordBField2 = new DataMap();
    DataList recordBField2DefaultValues = new DataList(2);
    recordBField2DefaultValues.add("defaultValue1");
    recordBField2DefaultValues.add("defaultValue2");
    recordBField2.put("defaultKey", recordBField2DefaultValues);

    return new Object[][]{
        {
            "RecordA.pdl",
            new DataMap(new ImmutableMap.Builder<String, Object>()
                .put("field1", 1)
                .put("field2", "2")
                .build()),
            new DataMap(new ImmutableMap.Builder<String, Object>()
                .put("field1", 1)
                .put("field2", "2")
                .put("field3", 0L)
                .put("field4", "default")
                .build())
        },
        {
            "RecordB.pdl",
            new DataMap(new ImmutableMap.Builder<String, Object>()
                .build()),
            new DataMap(new ImmutableMap.Builder<String, Object>()
                .put("field1", recordBField1)
                .put("field2", recordBField2)
                .build())
        },
        {
            "RecordC.pdl",
            new DataMap(),
            new DataMap(new ImmutableMap.Builder<String, Object>()
                .put("name", "default+")
                .build()),
        }
    };
  }

  @Test(dataProvider = "default_serialization")
  public void testSerializingDefaultValue(String filename, DataMap data, DataMap expected)
  {
    try
    {
      MultiFormatDataSchemaResolver schemaResolver = MultiFormatDataSchemaResolver.withBuiltinFormats(resolverDir);

      String schemaFileText = Files.readFile(new File(pegasusDir + FS + filename));
      DataSchema schema = DataTemplateUtil.parseSchema(schemaFileText, schemaResolver, SchemaFormatType.PDL);
      ResponseUtils.getAbsentFieldsDefaultValues((RecordDataSchema) schema, data);
      Assert.assertEquals(data, expected);
    }
    catch (Exception e)
    {
      Assert.fail("Read test schema file failure, check file read successful \n" + e);
    }
  }

  @AfterTest
  public void afterTest()
  {
    System.clearProperty(AbstractGenerator.GENERATOR_RESOLVER_PATH);
  }
}
