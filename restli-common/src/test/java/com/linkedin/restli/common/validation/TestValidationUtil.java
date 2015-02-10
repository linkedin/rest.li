/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.common.validation;


import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * Unit tests for ValidationUtil.
 *
 * @author Soojung Ha
 */
public class TestValidationUtil
{
  @DataProvider
  public Object[][] pathData()
  {
    return new Object[][]{
        {"stringA", true},
        {"intA", true},
        {"stringB", true},
        {"intB", true},
        {"UnionFieldWithInlineRecord", true},
        {"UnionFieldWithInlineRecord/com.linkedin.restli.common.validation.myRecord/foo1", true},
        {"ArrayWithInlineRecord", true},
        {"ArrayWithInlineRecord/bar1", true},
        {"ArrayWithInlineRecord/bar2", true},
        {"MapWithTyperefs", true},
        {"MapWithTyperefs/id", true},
        {"validationDemoNext/stringB", true},
        {"validationDemoNext/UnionFieldWithInlineRecord", true},
        {"validationDemoNext/validationDemoNext/validationDemoNext", true},
        // nonexistent field
        {"stringA1", false},
        {"stringA/abc", false},
        {"ArrayWithInlineRecord/bar3", false},
        // valid path but not a field of a record
        {"UnionFieldWithInlineRecord/com.linkedin.restli.common.validation.myRecord", false},
        {"UnionFieldWithInlineRecord/com.linkedin.restli.common.validation.myEnum", false},
        {"UnionFieldWithInlineRecord/com.linkedin.restli.common.validation.myEnum/FOOFOO", false}
    };
  }

  @Test(dataProvider = "pathData")
  public void testContainsPath(String path, boolean expected) throws IOException
  {
    DataSchema validationDemoSchema = pdscToDataSchema(DATA_SCHEMA_PATH);
    Assert.assertEquals(ValidationUtil.containsPath(validationDemoSchema, path), expected);
  }

  private DataSchema pdscToDataSchema(String path) throws IOException
  {
    final InputStream pdscStream = getClass().getClassLoader().getResourceAsStream(path);
    return DataTemplateUtil.parseSchema(IOUtils.toString(pdscStream));
  }

  private static final String DATA_SCHEMA_PATH = "pegasus/com/linkedin/restli/common/validation/ValidationDemo.pdsc";
}