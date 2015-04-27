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


import com.linkedin.data.DataMap;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.restli.client.ErrorHandlingBehavior;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.Exceptions2Builders;
import com.linkedin.restli.examples.greetings.client.Exceptions2RequestBuilders;
import com.linkedin.restli.internal.server.util.DataMapUtils;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class TestExceptionsResource2 extends RestLiIntegrationTest
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

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "exceptionHandlingModesDataProvider")
  public void testGet(boolean explicit, ErrorHandlingBehavior errorHandlingBehavior, RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Response<Greeting> response = null;
    RestLiResponseException exception = null;

    try
    {
      final Request<Greeting> req = builders.get().id(1L).build();
      ResponseFuture<Greeting> future;

      if (explicit)
      {
       future = getClient().sendRequest(req, errorHandlingBehavior);
      }
      else
      {
        future = getClient().sendRequest(req);
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
      Assert.assertEquals(response.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
      Assert.assertNotNull(response.getEntity());
      Assert.assertEquals(response.getEntity(), new Greeting().setMessage("Hello, sorry for the mess"));
    }

    Assert.assertNotNull(exception);
    Assert.assertTrue(exception.hasDecodedResponse());
    Assert.assertEquals(exception.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
    final DataMap respEntityMap = DataMapUtils.readMap(exception.getResponse());
    Assert.assertEquals(respEntityMap, new Greeting().setMessage("Hello, sorry for the mess").data());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "exceptionHandlingModesDataProvider")
  public void testExceptionWithValue(boolean explicit, ErrorHandlingBehavior errorHandlingBehavior, RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Response<Integer> response = null;
    RestLiResponseException exception = null;

    final Request<Integer> req = builders.<Integer>action("ExceptionWithValue").build();
    try
    {
      ResponseFuture<Integer> future;

      if (explicit)
      {
        future = getClient().sendRequest(req, errorHandlingBehavior);
      }
      else
      {
        future = getClient().sendRequest(req);
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
      Assert.assertEquals(response.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
      Assert.assertNotNull(response.getEntity());
      Assert.assertSame(response.getEntity(), 42);
    }

    Assert.assertNotNull(exception);
    Assert.assertTrue(exception.hasDecodedResponse());
    Assert.assertEquals(exception.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
    final DataMap respEntityMap = DataMapUtils.readMap(exception.getResponse());
    Assert.assertSame(respEntityMap.getInteger("value"), 42);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "exceptionHandlingModesDataProvider")
  public void testExceptionWithoutValue(boolean explicit, ErrorHandlingBehavior errorHandlingBehavior, RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Response<Void> response = null;
    RestLiResponseException exception = null;

    final Request<Void> req = builders.<Void>action("ExceptionWithoutValue").build();
    try
    {
      ResponseFuture<Void> future;

      if (explicit)
      {
        future = getClient().sendRequest(req, errorHandlingBehavior);
      }
      else
      {
        future = getClient().sendRequest(req);
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
      Assert.assertEquals(response.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
      Assert.assertNull(response.getEntity());
    }

    Assert.assertNotNull(exception);
    Assert.assertFalse(exception.hasDecodedResponse());
    Assert.assertEquals(exception.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "exceptionHandlingModesDataProvider")
  public void testNonRestException(boolean explicit, ErrorHandlingBehavior errorHandlingBehavior, RootBuilderWrapper<Long, Greeting> builders)
  {
    Response<Greeting> response = null;
    RestClient brokenClient = new RestClient(getDefaultTransportClient(), "http://localhost:8888/");
    try
    {
      final Request<Greeting> req = builders.get().id(1L).build();
      ResponseFuture<Greeting> future;

      if (explicit)
      {
        future = brokenClient.sendRequest(req, errorHandlingBehavior);
      }
      else
      {
        future = brokenClient.sendRequest(req);
      }

      response = future.getResponse();

      Assert.fail("expected exception");
    }
    catch (RemoteInvocationException e)
    {
      Assert.assertEquals(e.getClass(), RemoteInvocationException.class);
    }
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "exceptionHandlingModesDataProvider")
  public Object[][] exceptionHandlingModesDataProvider()
  {
    return new Object[][]
      {
        { true, ErrorHandlingBehavior.FAIL_ON_ERROR, new RootBuilderWrapper<Long, Greeting>(new Exceptions2Builders()) },
        { true, ErrorHandlingBehavior.FAIL_ON_ERROR, new RootBuilderWrapper<Long, Greeting>(new Exceptions2Builders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
        { true, ErrorHandlingBehavior.FAIL_ON_ERROR, new RootBuilderWrapper<Long, Greeting>(new Exceptions2RequestBuilders()) },
        { true, ErrorHandlingBehavior.FAIL_ON_ERROR, new RootBuilderWrapper<Long, Greeting>(new Exceptions2RequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
        { true, ErrorHandlingBehavior.TREAT_SERVER_ERROR_AS_SUCCESS, new RootBuilderWrapper<Long, Greeting>(new Exceptions2Builders()) },
        { true, ErrorHandlingBehavior.TREAT_SERVER_ERROR_AS_SUCCESS, new RootBuilderWrapper<Long, Greeting>(new Exceptions2Builders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
        { true, ErrorHandlingBehavior.TREAT_SERVER_ERROR_AS_SUCCESS, new RootBuilderWrapper<Long, Greeting>(new Exceptions2RequestBuilders()) },
        { true, ErrorHandlingBehavior.TREAT_SERVER_ERROR_AS_SUCCESS, new RootBuilderWrapper<Long, Greeting>(new Exceptions2RequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
        { false, null, new RootBuilderWrapper<Long, Greeting>(new Exceptions2Builders()) },
        { false, null, new RootBuilderWrapper<Long, Greeting>(new Exceptions2Builders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
        { false, null, new RootBuilderWrapper<Long, Greeting>(new Exceptions2RequestBuilders()) },
        { false, null, new RootBuilderWrapper<Long, Greeting>(new Exceptions2RequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) }
      };
  }
}
