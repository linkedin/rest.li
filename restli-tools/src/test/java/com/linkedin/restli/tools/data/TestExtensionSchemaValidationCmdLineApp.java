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
package com.linkedin.restli.tools.data;

import java.io.File;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class TestExtensionSchemaValidationCmdLineApp
{
  private String testDir = System.getProperty("testDir", new File("src/test").getAbsolutePath());
  private String testPegasusDir = testDir + File.separator + "pegasus";
  private String testExtensionDir = testDir + File.separator + "extensions";

  @DataProvider
  private Object[][] extensionSchemaInputFiles()
  {
    return new Object[][]
        {
            // In the array, first element is input directory of extension schema.
            //               second element is whether the input extension schema is valid.
            //               third element is the expected error message if the input extension schema is not valid.
            {
                "validCase",
                true,
                null
            },
            {
                "invalidVersionSuffix",
                false,
                "versionSuffix value: 'V3' does not match the versionSuffix value which was defined in resourceKey annotation"
            },
            {
                "invalidExtensionSchemaName",
                false,
                "Invalid extension schema name: 'FooExtend'. The name of the extension schema must be <baseSchemaName> + 'Extensions'"
            },
            {
                "invalidExtensionAnnotation",
                false,
                "Extension schema annotation is not valid: ERROR :: /bar :: unrecognized field found but not allowed\n"
            },
            {
                "invalidFieldAnnotation",
                false,
                "Field schema: { \"type\" : \"typeref\", \"name\" : \"DummyKeyWithoutAnnotation\", "
                    + "\"doc\" : \"A test schema which is used as a field type in extension schema.\", \"ref\" : \"string\" } is not annotated with 'resourceKey'"
            },
            {
                "invalidFieldType",
                false,
                "Field schema: '{ \"type\" : \"record\", \"name\" : \"DummyKeyWithWrongType\", "
                    + "\"doc\" : \"A test schema which is used as a field type in extension schema.\", \"fields\" : [  ] }' is not a TypeRef type."
            },
            {
                "invalidFieldName",
                false,
                "Field \"injectedField\" defined more than once, with \"int\" defined in \"Baz\" and { \"type\" : \"array\", \"items\" : { \"type\" : "
                    + "\"typeref\", \"name\" : \"DummyKey\", \"doc\" : \"A test schema which is used as a field type in extension schema.\","
                    + " \"ref\" : \"string\", \"resourceKey\" : [ { \"entity\" : \"Profile\", \"keyConfig\" : { \"keys\" : { \"profilesId\" : "
                    + "{ \"assocKey\" : { \"authorId\" : \"fabricName\", \"objectId\" : \"sessionId\" } } } }, \"resourcePath\" : \"/profiles/{profilesId}\" }, "
                    + "{ \"entity\" : \"ProfileV2\", \"keyConfig\" : { \"keys\" : { \"profilesId\" : { \"assocKey\" : { \"authorId\" : \"fabricName\", \"objectId\" : "
                    + "\"sessionId\" } } } }, \"resourcePath\" : \"/profilesV2/{profilesId}\", \"versionSuffix\" : \"V2\" } ] } } defined in \"BazExtensions\".\n"
            },
            {
                "invalidNoExtensionAnnotation",
                false,
                "Field: 'testField' is not annotated with @extension. The @extension annotation is required for 1-to-many relations, but not for 1-to-1 relations."
            }
        };
  }

  @Test(dataProvider = "extensionSchemaInputFiles")
  public void testExtensionSchemaValidation(String inputDir, boolean isValid, String errorMessage)
  {
      String resolverPath = testPegasusDir;
      String inputPath = testExtensionDir + File.separator + inputDir;
      try
      {
        ExtensionSchemaValidationCmdLineApp.parseAndValidateExtensionSchemas(resolverPath, new File(inputPath));
        Assert.assertTrue(isValid);
      }
      catch (Exception e)
      {
        Assert.assertTrue(!isValid);
        Assert.assertEquals(e.getMessage(), errorMessage);
      }
  }
}
