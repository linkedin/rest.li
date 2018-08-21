/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.restli.internal.server.util;

import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.server.RestLiServiceException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Tests for {@link ArgumentUtils}.
 *
 * @author Evan Williams
 */
public class TestArgumentUtils
{
  @DataProvider(name = "parseReturnEntityParameterData")
  public Object[][] provideParseReturnEntityParameterData()
  {
    return new Object[][]
        {
            { "true", true, false },
            { "TRUE", true, false },
            { "false", false, false },
            { "False", false, false },
            { "foo", null, true }
        };
  }

  @Test(dataProvider = "parseReturnEntityParameterData")
  public void testParseReturnEntityParameter(String paramValue, Boolean expectedValue, boolean expectException)
  {
    try
    {
      boolean value = ArgumentUtils.parseReturnEntityParameter(paramValue);

      if (expectException)
      {
        Assert.fail("Expected \"" + RestConstants.RETURN_ENTITY_PARAM + "\" parameter parse to fail for value: " + paramValue);
      }

      Assert.assertEquals(value, (boolean) expectedValue);
    }
    catch (RestLiServiceException e)
    {
      if (!expectException)
      {
        Assert.fail("Expected \"" + RestConstants.RETURN_ENTITY_PARAM + "\" parameter parse to succeed for value: " + paramValue);
      }

      Assert.assertEquals(e.getStatus(), HttpStatus.S_400_BAD_REQUEST);
      Assert.assertTrue(e.getMessage().contains(String.format("Invalid \"%s\" parameter: %s", RestConstants.RETURN_ENTITY_PARAM, paramValue)));
    }
  }
}
