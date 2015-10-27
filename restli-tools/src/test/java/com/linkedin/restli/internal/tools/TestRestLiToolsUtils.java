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

package com.linkedin.restli.internal.tools;


import com.linkedin.restli.restspec.ParameterSchema;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author Soojung Ha
 */
public class TestRestLiToolsUtils
{
  @DataProvider
  public Object[][] params()
  {
    return new Object[][]
        {
            {new ParameterSchema().setOptional(true).setDefault("abcd"), true},
            {new ParameterSchema().setOptional(true), true},
            {new ParameterSchema().setOptional(false).setDefault("abcd"), true},
            {new ParameterSchema().setOptional(false), false},
            {new ParameterSchema(), false},
            {new ParameterSchema().setDefault("abcd"), true}
        };
  }

  @Test(dataProvider = "params")
  public void testIsParameterOptional(ParameterSchema parameterSchema, boolean expected)
  {
    Assert.assertEquals(RestLiToolsUtils.isParameterOptional(parameterSchema), expected);
  }
}
