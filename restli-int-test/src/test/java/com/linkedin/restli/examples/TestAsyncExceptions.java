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

package com.linkedin.restli.examples;


import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.AsyncErrorsBuilders;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author erli
 */
public class TestAsyncExceptions extends RestLiIntegrationTest
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

  @Test(dataProvider = "exceptionProvider")
  public void testPromise(String key, int expectedStatus) throws RemoteInvocationException
  {
    AsyncErrorsBuilders builder = new AsyncErrorsBuilders();
    Request<Greeting> request = builder.actionPromise().paramId(key).build();

    try
    {
      getClient().sendRequest(request).getResponse();
      Assert.fail("This request should have failed.");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), expectedStatus);
    }
  }

  @Test(dataProvider = "exceptionProvider")
  public void testCallback(String key, int expectedStatus) throws RemoteInvocationException
  {
    AsyncErrorsBuilders builder = new AsyncErrorsBuilders();
    Request<Greeting> request = builder.actionCallback().paramId(key).build();

    try
    {
      getClient().sendRequest(request).getResponse();
      Assert.fail("This request should have failed.");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), expectedStatus);
    }
  }

  @Test(dataProvider = "exceptionProvider")
  public void testTask(String key, int expectedStatus) throws RemoteInvocationException
  {
    AsyncErrorsBuilders builder = new AsyncErrorsBuilders();
    Request<Greeting> request = builder.actionTask().paramId(key).build();

    try
    {
      getClient().sendRequest(request).getResponse();
      Assert.fail("This request should have failed.");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), expectedStatus);
    }
  }

  @DataProvider(name = "exceptionProvider")
  public Object[][] provideExceptions()
  {
    return new Object[][] {
        {"returnNonService", 500},
        {"returnService", 401},
        {"throwService", 401},
        {"throwNonService", 500}
    };
  }
}
