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

package com.linkedin.restli.examples;

import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.examples.greetings.client.GreetingsRequestBuilders;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class TestEmptyBodyRequests extends RestLiIntegrationTest
{
  @BeforeClass
  public void initClass() throws Exception
  {
    super.init();
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestProviders")
  public <T> void testException(Request<T> request, boolean expectError) throws RemoteInvocationException
  {
    try
    {
      getClient().sendRequest(request).getResponse();

      if (expectError)
      {
        Assert.fail("expected exception");
      }
    }
    catch (RestLiResponseException exception)
    {
      if (expectError)
      {
        Assert.assertFalse(exception.hasDecodedResponse());
        Assert.assertEquals(exception.getStatus(), HttpStatus.S_400_BAD_REQUEST.getCode());
      }
      else
      {
        Assert.fail("not expected exception but got: ",  exception);
      }
    }

  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestProviders")
  public Object[][] exceptionHandlingModesDataProvider()
  {
    return new Object[][] {
        { new GreetingsRequestBuilders().update().id(1L).build(), true },
        // Restli client creates a wrapper with empty "elements" for batch update.
        { new GreetingsRequestBuilders().batchUpdate().build(), false },
        { new GreetingsRequestBuilders().partialUpdate().id(1L).build(), true},
        // Restli client creates a wrapper with empty "elements" for batch partial update.
        { new GreetingsRequestBuilders().batchPartialUpdate().build(), false},
        // Restli client creates a wrapper with empty "elements" for batch create.
        { new GreetingsRequestBuilders().batchCreate().build(), false},
        { new GreetingsRequestBuilders().create().build(), true},

    };
  }
}
