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

import java.util.Collections;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.GreetingsAuthBuilders;

public class TestCustomCrudParams extends RestLiIntegrationTest
{
  private static final Client CLIENT = new TransportClientAdapter(new HttpClientFactory().getClient(Collections.<String, String>emptyMap()));
  private static final String URI_PREFIX = "http://localhost:1338/";
  private static final RestClient REST_CLIENT = new RestClient(CLIENT, URI_PREFIX);
  private static final GreetingsAuthBuilders GREETINGS_AUTH_BUILDERS = new GreetingsAuthBuilders();

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

  @Test
  public static void testCookbookCrudParams() throws Exception
  {
    try
    {
      Request<Greeting> request = GREETINGS_AUTH_BUILDERS.get().id(1L).build();
      ResponseFuture<Greeting> future = REST_CLIENT.sendRequest(request);
      @SuppressWarnings("unused")
      Response<Greeting> greetingResponse = future.getResponse();
      Assert.fail("expected response exception");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getServiceErrorMessage(), "Invalid auth token");
    }

    // GET
    Request<Greeting> request = GREETINGS_AUTH_BUILDERS.get().id(1L).authParam("PLEASE").build();
    ResponseFuture<Greeting> future = REST_CLIENT.sendRequest(request);
    Response<Greeting> greetingResponse = future.getResponse();

    // POST
    Greeting greeting = new Greeting(greetingResponse.getEntity().data().copy());
    greeting.setMessage("This is a new message!");

    Request<EmptyRecord>  writeRequest = GREETINGS_AUTH_BUILDERS.update().id(1L).input(greeting)
            .authParam("PLEASE").build();
    REST_CLIENT.sendRequest(writeRequest).getResponse();

    // GET again, to verify that our POST worked.
    Request<Greeting> request2 = GREETINGS_AUTH_BUILDERS.get().id(1L).authParam("PLEASE").build();
    ResponseFuture<Greeting> future2 = REST_CLIENT.sendRequest(request2);
    greetingResponse = future2.get();
    Assert.assertEquals(greetingResponse.getEntity().getMessage(), "This is a new message!");
  }
}
