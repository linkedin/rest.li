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
import com.linkedin.restli.client.BatchCreateIdRequest;
import com.linkedin.restli.client.ErrorHandlingBehavior;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.common.BatchCreateIdResponse;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CreateIdStatus;
import com.linkedin.restli.common.CreateStatus;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.ExceptionsBuilders;
import com.linkedin.restli.examples.greetings.client.ExceptionsRequestBuilders;
import com.linkedin.restli.internal.common.ProtocolVersionUtil;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class TestExceptionsResource extends RestLiIntegrationTest
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
  public void testException(boolean explicit, ErrorHandlingBehavior errorHandlingBehavior, RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Response<Greeting> response = null;
    RestLiResponseException exception = null;

    try
    {
      Request<Greeting> readRequest = builders.get().id(1L).build();
      ResponseFuture<Greeting> future;

      if (explicit)
      {
        future = getClient().sendRequest(readRequest, errorHandlingBehavior);
      }
      else
      {
        future = getClient().sendRequest(readRequest);
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
    Assert.assertEquals(exception.getServiceErrorCode(), 42);
    Assert.assertEquals(exception.getServiceErrorMessage(), "error processing request");
    Assert.assertTrue(exception.getServiceErrorStackTrace().contains("at com.linkedin.restli.examples.greetings.server.ExceptionsResource.get("));
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "exceptionHandlingModesDataProvider")
  public void testCreateError(boolean explicit, ErrorHandlingBehavior errorHandlingBehavior, RootBuilderWrapper<Long, Greeting> builders) throws Exception
  {
    Response<EmptyRecord> response = null;
    RestLiResponseException exception = null;

    try
    {
      Request<EmptyRecord> createRequest = builders.create()
          .input(new Greeting().setId(11L).setMessage("@#$%@!$%").setTone(Tone.INSULTING))
          .build();
      ResponseFuture<EmptyRecord> future;

      if (explicit)
      {
        future = getClient().sendRequest(createRequest, errorHandlingBehavior);
      }
      else
      {
        future = getClient().sendRequest(createRequest);
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
      Assert.assertEquals(response.getStatus(), HttpStatus.S_406_NOT_ACCEPTABLE.getCode());
      Assert.assertNull(response.getEntity());
    }

    Assert.assertNotNull(exception);
    Assert.assertFalse(exception.hasDecodedResponse());
    Assert.assertEquals(exception.getStatus(), HttpStatus.S_406_NOT_ACCEPTABLE.getCode());
    Assert.assertEquals(exception.getServiceErrorMessage(), "I will not tolerate your insolence!");
    Assert.assertEquals(exception.getServiceErrorCode(), 999);
    Assert.assertEquals(exception.getErrorSource(), RestConstants.HEADER_VALUE_ERROR);
    Assert.assertEquals(exception.getErrorDetails().getString("reason"), "insultingGreeting");
    Assert.assertTrue(exception.getServiceErrorStackTrace().startsWith("com.linkedin.restli.server.RestLiServiceException [HTTP Status:406, serviceErrorCode:999]: I will not tolerate your insolence!"),
                      "stacktrace mismatch:" + exception.getStackTrace());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  public void testBatchCreateErrors(RestliRequestOptions requestOptions) throws Exception
  {
    ExceptionsBuilders builders = new ExceptionsBuilders(requestOptions);

    Request<CollectionResponse<CreateStatus>> batchCreateRequest = builders.batchCreate()
      .input(new Greeting().setId(10L).setMessage("Greetings.").setTone(Tone.SINCERE))
      .input(new Greeting().setId(11L).setMessage("@#$%@!$%").setTone(Tone.INSULTING))
      .build();

    Response<CollectionResponse<CreateStatus>> response = getClient().sendRequest(batchCreateRequest).getResponse();
    List<CreateStatus> createStatuses = response.getEntity().getElements();
    Assert.assertEquals(createStatuses.size(), 2);

    @SuppressWarnings("unchecked")
    CreateIdStatus<Long> status0 =  (CreateIdStatus<Long>)createStatuses.get(0);
    Assert.assertEquals(status0.getStatus().intValue(), HttpStatus.S_201_CREATED.getCode());
    Assert.assertEquals(status0.getKey(), new Long(10));
    @SuppressWarnings("deprecation")
    String id = status0.getId();
    Assert.assertEquals(BatchResponse.keyToString(status0.getKey(), ProtocolVersionUtil.extractProtocolVersion(response.getHeaders())),
                        id);
    Assert.assertFalse(status0.hasError());

    CreateStatus status1 = createStatuses.get(1);
    Assert.assertEquals(status1.getStatus().intValue(), HttpStatus.S_406_NOT_ACCEPTABLE.getCode());
    Assert.assertTrue(status1.hasError());
    ErrorResponse error = status1.getError();
    Assert.assertEquals(error.getStatus().intValue(), HttpStatus.S_406_NOT_ACCEPTABLE.getCode());
    Assert.assertEquals(error.getMessage(), "I will not tolerate your insolence!");
    Assert.assertEquals(error.getServiceErrorCode().intValue(), 999);
    Assert.assertEquals(error.getExceptionClass(), "com.linkedin.restli.server.RestLiServiceException");
    Assert.assertEquals(error.getErrorDetails().data().getString("reason"), "insultingGreeting");
    Assert.assertTrue(error.getStackTrace().startsWith(
      "com.linkedin.restli.server.RestLiServiceException [HTTP Status:406, serviceErrorCode:999]: I will not tolerate your insolence!"),
                      "stacktrace mismatch:" + error.getStackTrace());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  public void testBatchCreateIdErrors(RestliRequestOptions requestOptions) throws Exception
  {
    ExceptionsRequestBuilders builders = new ExceptionsRequestBuilders(requestOptions);

    BatchCreateIdRequest<Long, Greeting> batchCreateRequest = builders.batchCreate()
      .input(new Greeting().setId(10L).setMessage("Greetings.").setTone(Tone.SINCERE))
      .input(new Greeting().setId(11L).setMessage("@#$%@!$%").setTone(Tone.INSULTING))
      .build();

    Response<BatchCreateIdResponse<Long>> response = getClient().sendRequest(batchCreateRequest).getResponse();
    List<CreateIdStatus<Long>> createStatuses = response.getEntity().getElements();
    Assert.assertEquals(createStatuses.size(), 2);

    @SuppressWarnings("unchecked")
    CreateIdStatus<Long> status0 =  createStatuses.get(0);
    Assert.assertEquals(status0.getStatus().intValue(), HttpStatus.S_201_CREATED.getCode());
    Assert.assertEquals(status0.getKey(), new Long(10));
    @SuppressWarnings("deprecation")
    String id = status0.getId();
    Assert.assertEquals(BatchResponse.keyToString(status0.getKey(), ProtocolVersionUtil.extractProtocolVersion(response.getHeaders())),
                        id);
    Assert.assertFalse(status0.hasError());

    CreateIdStatus<Long> status1 = createStatuses.get(1);
    Assert.assertEquals(status1.getStatus().intValue(), HttpStatus.S_406_NOT_ACCEPTABLE.getCode());
    Assert.assertTrue(status1.hasError());
    ErrorResponse error = status1.getError();
    Assert.assertEquals(error.getStatus().intValue(), HttpStatus.S_406_NOT_ACCEPTABLE.getCode());
    Assert.assertEquals(error.getMessage(), "I will not tolerate your insolence!");
    Assert.assertEquals(error.getServiceErrorCode().intValue(), 999);
    Assert.assertEquals(error.getExceptionClass(), "com.linkedin.restli.server.RestLiServiceException");
    Assert.assertEquals(error.getErrorDetails().data().getString("reason"), "insultingGreeting");
    Assert.assertTrue(error.getStackTrace().startsWith(
      "com.linkedin.restli.server.RestLiServiceException [HTTP Status:406, serviceErrorCode:999]: I will not tolerate your insolence!"),
                      "stacktrace mismatch:" + error.getStackTrace());
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "exceptionHandlingModesDataProvider")
  public Object[][] exceptionHandlingModesDataProvider()
  {
    return new Object[][] {
      { true, ErrorHandlingBehavior.FAIL_ON_ERROR, new RootBuilderWrapper<Long, Greeting>(new ExceptionsBuilders()) },
      { true, ErrorHandlingBehavior.FAIL_ON_ERROR, new RootBuilderWrapper<Long, Greeting>(new ExceptionsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { true, ErrorHandlingBehavior.FAIL_ON_ERROR, new RootBuilderWrapper<Long, Greeting>(new ExceptionsRequestBuilders()) },
      { true, ErrorHandlingBehavior.FAIL_ON_ERROR, new RootBuilderWrapper<Long, Greeting>(new ExceptionsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { true, ErrorHandlingBehavior.TREAT_SERVER_ERROR_AS_SUCCESS, new RootBuilderWrapper<Long, Greeting>(new ExceptionsBuilders()) },
      { true, ErrorHandlingBehavior.TREAT_SERVER_ERROR_AS_SUCCESS, new RootBuilderWrapper<Long, Greeting>(new ExceptionsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { true, ErrorHandlingBehavior.TREAT_SERVER_ERROR_AS_SUCCESS, new RootBuilderWrapper<Long, Greeting>(new ExceptionsRequestBuilders()) },
      { true, ErrorHandlingBehavior.TREAT_SERVER_ERROR_AS_SUCCESS, new RootBuilderWrapper<Long, Greeting>(new ExceptionsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { false, null, new RootBuilderWrapper<Long, Greeting>(new ExceptionsBuilders()) },
      { false, null, new RootBuilderWrapper<Long, Greeting>(new ExceptionsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { false, null, new RootBuilderWrapper<Long, Greeting>(new ExceptionsRequestBuilders()) },
      { false, null, new RootBuilderWrapper<Long, Greeting>(new ExceptionsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) }
    };
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestOptionsDataProvider")
  private static Object[][] requestOptionsDataProvider()
  {
    return new Object[][]
      {
        { RestliRequestOptions.DEFAULT_OPTIONS },
        { TestConstants.FORCE_USE_NEXT_OPTIONS }
      };
  }
}
