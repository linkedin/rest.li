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


import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.ErrorHandlingBehavior;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.Exceptions3Builders;
import com.linkedin.restli.examples.greetings.client.Exceptions3RequestBuilders;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.util.Collections;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class TestExceptionsResource3 extends RestLiIntegrationTest
{
  private static final Client CLIENT = new TransportClientAdapter(new HttpClientFactory().getClient(Collections.<String, String>emptyMap()));
  private static final String URI_PREFIX = "http://localhost:1338/";
  private static final RestClient REST_CLIENT = new RestClient(CLIENT, URI_PREFIX);

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

  @Test(dataProvider = "exceptionHandlingModesDataProvider")
  public void testGet404(boolean explicit, ErrorHandlingBehavior errorHandlingBehavior, RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Response<Greeting> response = null;
    RestLiResponseException exception = null;

    try
    {
      Request<Greeting> readRequest = builders.get().id(1L).build();
      ResponseFuture<Greeting> future;

      if (explicit)
      {
        future = REST_CLIENT.sendRequest(readRequest, errorHandlingBehavior);
      }
      else
      {
        future = REST_CLIENT.sendRequest(readRequest);
      }

      response = future.getResponse();

      if (!explicit || errorHandlingBehavior == ErrorHandlingBehavior.FAIL_ON_ERROR)
      {
        Assert.fail("expected exception");
      }
    }
    catch (RestLiResponseException e)
    {
      if (!explicit || errorHandlingBehavior == ErrorHandlingBehavior.FAIL_ON_ERROR)
      {
        exception = e;
      }
      else
      {
        Assert.fail("not expected exception");
      }
    }

    if (explicit && errorHandlingBehavior == ErrorHandlingBehavior.TREAT_SERVER_ERROR_AS_SUCCESS)
    {
      Assert.assertNotNull(response);
      Assert.assertTrue(response.hasError());
      exception = response.getError();
      Assert.assertEquals(response.getStatus(), HttpStatus.S_404_NOT_FOUND.getCode());
      Assert.assertNull(response.getEntity());
    }

    Assert.assertNotNull(exception);
    Assert.assertFalse(exception.hasDecodedResponse());
    Assert.assertEquals(exception.getStatus(), HttpStatus.S_404_NOT_FOUND.getCode());
  }

  @Test(dataProvider = "exceptionHandlingModesDataProvider")
  public void testUpdate(boolean explicit, ErrorHandlingBehavior errorHandlingBehavior, RootBuilderWrapper<Long, Greeting> builders) throws Exception
  {
    Response<EmptyRecord> response = null;
    RestLiResponseException exception = null;

    try
    {
      Request<EmptyRecord> request = builders.update().id(11L)
          .input(new Greeting().setId(11L).setMessage("@#$%@!$%").setTone(Tone.INSULTING))
          .build();
      ResponseFuture<EmptyRecord> future;

      if (explicit)
      {
        future = REST_CLIENT.sendRequest(request, errorHandlingBehavior);
      }
      else
      {
        future = REST_CLIENT.sendRequest(request);
      }

      response = future.getResponse();

      if (!explicit || errorHandlingBehavior == ErrorHandlingBehavior.FAIL_ON_ERROR)
      {
        Assert.fail("expected exception");
      }
    }
    catch (RestLiResponseException e)
    {
      if (!explicit || errorHandlingBehavior == ErrorHandlingBehavior.FAIL_ON_ERROR)
      {
        exception = e;
      }
      else
      {
        Assert.fail("not expected exception");
      }
    }

    if (explicit && errorHandlingBehavior == ErrorHandlingBehavior.TREAT_SERVER_ERROR_AS_SUCCESS)
    {
      Assert.assertNotNull(response);
      Assert.assertTrue(response.hasError());
      exception = response.getError();
      Assert.assertEquals(response.getStatus(), HttpStatus.S_404_NOT_FOUND.getCode());
      Assert.assertNull(response.getEntity());
    }

    Assert.assertNotNull(exception);
    Assert.assertTrue(exception.hasDecodedResponse());
    Assert.assertEquals(exception.getStatus(), HttpStatus.S_404_NOT_FOUND.getCode());
  }

  @DataProvider
  @SuppressWarnings("rawtypes")
  public Object[][] exceptionHandlingModesDataProvider()
  {
    return new Object[][] {
        { true, ErrorHandlingBehavior.FAIL_ON_ERROR, new RootBuilderWrapper(new Exceptions3Builders()) },
        { true, ErrorHandlingBehavior.FAIL_ON_ERROR, new RootBuilderWrapper(new Exceptions3RequestBuilders()) },
        { true, ErrorHandlingBehavior.TREAT_SERVER_ERROR_AS_SUCCESS, new RootBuilderWrapper(new Exceptions3Builders()) },
        { true, ErrorHandlingBehavior.TREAT_SERVER_ERROR_AS_SUCCESS, new RootBuilderWrapper(new Exceptions3RequestBuilders()) },
        { false, null, new RootBuilderWrapper(new Exceptions3Builders()) },
        { false, null, new RootBuilderWrapper(new Exceptions3RequestBuilders()) }
    };
  }
}
