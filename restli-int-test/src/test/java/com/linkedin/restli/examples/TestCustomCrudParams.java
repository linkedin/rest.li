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

package com.linkedin.restli.examples;


import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.GreetingsAuthBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsAuthRequestBuilders;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class TestCustomCrudParams extends RestLiIntegrationTest
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

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testCookbookCrudParams(RootBuilderWrapper<Long, Greeting> builders) throws Exception
  {
    try
    {
      Request<Greeting> request = builders.get().id(1L).build();
      ResponseFuture<Greeting> future = getClient().sendRequest(request);
      @SuppressWarnings("unused")
      Response<Greeting> greetingResponse = future.getResponse();
      Assert.fail("expected response exception");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getServiceErrorMessage(), "Invalid auth token");
    }

    // GET
    Request<Greeting> request = builders.get().id(1L).setQueryParam("auth", "PLEASE").build();
    ResponseFuture<Greeting> future = getClient().sendRequest(request);
    Response<Greeting> greetingResponse = future.getResponse();

    // POST
    Greeting greeting = new Greeting(greetingResponse.getEntity().data().copy());
    greeting.setMessage("This is a new message!");

    Request<EmptyRecord>  writeRequest = builders.update().id(1L).input(greeting)
      .setQueryParam("auth", "PLEASE").build();
    getClient().sendRequest(writeRequest).getResponse();

    // GET again, to verify that our POST worked.
    Request<Greeting> request2 = builders.get().id(1L).setQueryParam("auth", "PLEASE").build();
    ResponseFuture<Greeting> future2 = getClient().sendRequest(request2);
    greetingResponse = future2.get();
    Assert.assertEquals(greetingResponse.getEntity().getMessage(), "This is a new message!");
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  private static Object[][] requestBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsAuthBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsAuthBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsAuthRequestBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsAuthRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) }
    };
  }
}
