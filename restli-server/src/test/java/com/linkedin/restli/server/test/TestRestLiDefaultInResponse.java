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

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.SchemaFormatType;
import com.linkedin.data.schema.generator.AbstractGenerator;
import com.linkedin.data.schema.resolver.MultiFormatDataSchemaResolver;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.restli.internal.server.response.ResponseUtils;
import java.io.File;
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
    // case 1
    DataMap case1Input = new DataMap();
    case1Input.put("field1", 1);
    case1Input.put("field2", "2");
    DataMap case1Expect = new DataMap();
    case1Expect.put("field1", 1);
    case1Expect.put("field2", "2");
    case1Expect.put("field3", 0L);
    case1Expect.put("field4", "default");

    // case 2
    DataMap case2Input = new DataMap();
    DataMap case2Expect = new DataMap();
    DataList case2F1 = new DataList(1);
    case2F1.add(-1);
    case2Expect.put("field1", case2F1);
    DataMap case2F2 = new DataMap();
    DataList case2F2Default = new DataList(2);
    case2F2Default.add("defaultValue1");
    case2F2Default.add("defaultValue2");
    case2F2.put("defaultKey", case2F2Default);
    case2Expect.put("field2", case2F2);

    // case 3
    DataMap case3Input = new DataMap();
    case3Input.put("name", "not-a-default");
    case3Input.put("personalRecordD", new DataMap());
    DataMap case3Expect = new DataMap();
    DataMap case3RecordDExpect = new DataMap();
    case3RecordDExpect.put("field3", 0L);
    case3RecordDExpect.put("field4", "default");
    case3RecordDExpect.put("field5", "a-typeref-default");
    case3Expect.put("name", "not-a-default");
    case3Expect.put("personalRecordD", case3RecordDExpect);

    // case 4
    DataMap case4B1 = new DataMap();
    case4B1.put("f1", 1);
    DataMap case4Input = new DataMap();
    case4Input.put("b1", case4B1);
    case4Input.put("b2", new DataMap());
    DataMap case4Expect = new DataMap();
    DataMap case4ExpectB1 = new DataMap();
    case4ExpectB1.put("f1", 1);
    DataMap case4ExpectB2 = new DataMap();
    case4ExpectB2.put("f1", 5);
    case4Expect.put("b1", case4ExpectB1);
    case4Expect.put("b2", case4ExpectB2);

    // case 5
    DataMap case5B3 = new DataMap();
    case5B3.put("f2", 1);
    DataMap case5Input = new DataMap();
    case5Input.put("b3", case5B3);
    DataMap case5Expect = new DataMap();
    DataMap case5ExpectB1 = new DataMap();
    case5ExpectB1.put("f1", 5);
    case5ExpectB1.put("f2", 10);
    DataMap case5ExpectB3 = new DataMap();
    case5ExpectB3.put("f2", 1);
    case5ExpectB3.put("f1", 5);
    case5Expect.put("b1", case5ExpectB1);
    case5Expect.put("b3", case5ExpectB3);


    // Each test case has 3 elements:
    // Index 0: PDL file name
    // Index 1: data before filling default
    // Index 2: expected data after filling default
    return new Object[][]{
        {
            "RecordA.pdl",
            case1Input,
            case1Expect
        },
        {
            "RecordB.pdl",
            case2Input,
            case2Expect
        },
        {
            "RecordC.pdl",
            case3Input,
            case3Expect
        },
        {
            "Bar.pdl",
            case4Input,
            case4Expect
        },
        {
            "Bar.pdl",
            case5Input,
            case5Expect
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
      data = ResponseUtils.fillInDefaultValues(schema, data);
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

