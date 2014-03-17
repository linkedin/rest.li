/*
   Copyright (c) 2014 LinkedIn Corp.

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


import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.GreetingsBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsCallbackBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsCallbackRequestBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsPromiseBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsPromiseCtxBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsPromiseCtxRequestBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsPromiseRequestBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsRequestBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsTaskBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsTaskRequestBuilders;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.FilterResponseContext;
import com.linkedin.restli.server.filter.RequestFilter;
import com.linkedin.restli.server.filter.ResponseFilter;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import static com.linkedin.restli.examples.TestConstants.FORCE_USE_NEXT_OPTIONS;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @author nshankar
 */
public class TestFilters extends RestLiIntegrationTest
{
  private static final Client CLIENT =
      new TransportClientAdapter(new HttpClientFactory().getClient(Collections.<String, String> emptyMap()));
  private static final String URI_PREFIX = "http://localhost:1338/";
  private static final RestClient REST_CLIENT = new RestClient(CLIENT, URI_PREFIX);
  private static final String REQ_FILTER_ERROR_MESSAGE = "You are forbidden from creating an insulting greeting.";
  private static final HttpStatus REQ_FILTER_ERROR_STATUS = HttpStatus.S_403_FORBIDDEN;
  private static final String RESP_FILTER_ERROR_MESSAGE = "Thou shall not insult other";
  private static final HttpStatus RESP_FILTER_ERROR_STATUS = HttpStatus.S_400_BAD_REQUEST;
  private static final BiMap<Tone, Tone> toneMapper;
  static
  {
    toneMapper = HashBiMap.create();
    toneMapper.put(Tone.FRIENDLY, Tone.SINCERE);
    toneMapper.put(Tone.SINCERE, Tone.INSULTING);
  }

  @Mock
  private RequestFilter _requestFilter;
  @Mock
  private ResponseFilter _responseFilter;

  @BeforeClass
  public void initClass() throws Exception
  {
    MockitoAnnotations.initMocks(this);
  }

  @AfterMethod
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  /**
   * This is a simple test that verifies the behavior of request and response filters. This test
   * hooks up two filters, one request filter and one response filter to the greeting resource.
   *
   * The behavior of the request filter is such that if the incoming request is of type create, the
   * filter modifies the incoming create request as follows:
   *
   * 1. If the tone of the incoming greeting is friendly, the filter modifies it to sincere.
   *
   * 2. If the tone of the incoming greeting is sincere, the filter modifies it to insulting.
   *
   * 3. If the tone of the incoming greeting is insulting, the filter throws an exception saying
   * that creation of a greeting with an insulting tone is not permitted. The HTTP status code is
   * set to 403.
   *
   * The behavior of the response filter is as follows:
   *
   * 1. If the response is an error, and the HTTP status code is 403, the filter updates the
   * outgoing error message and sets the status code to 400.
   *
   * 2. If the response is not an error, and the incoming request is a get, then the response filter
   * modifies the tone of the outgoing greeting message as follows:
   *
   * a. If the tone of the outgoing greeting from the resource is sincere, the filter modifies it to
   * friendly.
   *
   * b. If the tone of the outgoing greeting from the resource is insulting, the filter modifies it
   * to sincere.
   *
   * @param builders
   *          Type of request builder.
   * @param tone
   *          Tone of the greeting to be created.
   * @param responseFilter
   *          flag indicating whether or not the response filter is to be hooked up. NOTE: The
   *          request filter is always hooked up.
   * @throws Exception
   *           If anything unexpected happens.
   */
  @Test(dataProvider = "requestBuilderDataProvider")
  public void testGet(RootBuilderWrapper<Long, Greeting> builders, Tone tone, boolean responseFilter) throws Exception
  {
    setupFilters(responseFilter);
    Greeting greeting = generateTestGreeting("Test greeting.....", tone);
    Long createdId = null;
    try
    {
      createdId = createTestData(builders, greeting);
    }
    catch (RestLiResponseException e)
    {
      if (tone != Tone.INSULTING)
      {
        fail();
      }
      if (responseFilter)
      {
        assertEquals(e.getServiceErrorMessage(), RESP_FILTER_ERROR_MESSAGE);
        assertEquals(e.getResponse().getStatus(), RESP_FILTER_ERROR_STATUS.getCode());
      }
      else
      {
        assertEquals(e.getServiceErrorMessage(), REQ_FILTER_ERROR_MESSAGE);
        assertEquals(e.getResponse().getStatus(), REQ_FILTER_ERROR_STATUS.getCode());
      }
      verifyFilters(tone, responseFilter);
      return;
    }
    if (tone == Tone.INSULTING)
    {
      fail();
    }
    if (!responseFilter)
    {
      greeting.setTone(mapToneForIncomingRequest(tone));
    }
    greeting.setId(createdId);
    Request<Greeting> getRequest = builders.get().id(createdId).build();
    Greeting getReturnedGreeting = REST_CLIENT.sendRequest(getRequest).getResponse().getEntity();
    ValidateDataAgainstSchema.validate(getReturnedGreeting.data(),
                                       getReturnedGreeting.schema(),
                                       new ValidationOptions());
    assertEquals(getReturnedGreeting, greeting);
    deleteAndVerifyTestData(builders, createdId);
    verifyFilters(tone, responseFilter);
  }

  private Greeting generateTestGreeting(String message, Tone tone)
  {
    return new Greeting().setMessage(message).setTone(tone);
  }

  private void deleteAndVerifyTestData(RootBuilderWrapper<Long, Greeting> builders, Long id) throws RemoteInvocationException
  {
    Request<EmptyRecord> request = builders.delete().id(id).build();
    ResponseFuture<EmptyRecord> future = REST_CLIENT.sendRequest(request);
    Response<EmptyRecord> response = future.getResponse();
    assertEquals(response.getStatus(), HttpStatus.S_204_NO_CONTENT.getCode());
  }

  private Long createTestData(RootBuilderWrapper<Long, Greeting> builders, Greeting greeting) throws RemoteInvocationException
  {
    Request<EmptyRecord> request = builders.create().input(greeting).build();
    String createdId = REST_CLIENT.sendRequest(request).getResponse().getId();
    return Long.parseLong(createdId);
  }

  private void verifyFilters(Tone tone, boolean respFilter)
  {
    int count = tone == Tone.INSULTING ? 1 : 3;
    verify(_requestFilter, times(count)).onRequest(any(FilterRequestContext.class));
    verifyNoMoreInteractions(_requestFilter);
    if (respFilter)
    {
      verify(_responseFilter, times(count)).onResponse(any(FilterRequestContext.class),
                                                       any(FilterResponseContext.class));
      verifyNoMoreInteractions(_responseFilter);
    }
  }

  private void setupFilters(boolean responseFilter) throws IOException
  {
    reset(_requestFilter);
    final Integer spValue = new Integer(100);
    final String spKey = "Counter";
    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterRequestContext requestContext = (FilterRequestContext) args[0];
        requestContext.getFilterScratchpad().put(spKey, spValue);
        if (requestContext.getMethodType() == ResourceMethod.CREATE)
        {
          RecordTemplate entity = requestContext.getRequestData().getEntity();
          if (entity != null && entity instanceof Greeting)
          {
            Greeting greeting = (Greeting) entity;
            if (greeting.hasTone())
            {
              Tone tone = greeting.getTone();
              if (tone == Tone.INSULTING)
              {
                throw new RestLiServiceException(REQ_FILTER_ERROR_STATUS, REQ_FILTER_ERROR_MESSAGE);
              }
              greeting.setTone(mapToneForIncomingRequest(tone));
            }
          }
        }
        return null;
      }
    }).when(_requestFilter).onRequest(any(FilterRequestContext.class));
    List<RequestFilter> reqFilters = Arrays.asList(_requestFilter);

    List<ResponseFilter> respFilters = null;
    if (responseFilter)
    {
      reset(_responseFilter);
      doAnswer(new Answer<Object>()
      {
        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable
        {
          Object[] args = invocation.getArguments();
          FilterRequestContext requestContext = (FilterRequestContext) args[0];
          FilterResponseContext responseContext = (FilterResponseContext) args[1];
          RecordTemplate entity = responseContext.getResponseEntity();
          // Verify the scratch pad value.
          assertTrue(requestContext.getFilterScratchpad().get(spKey) == spValue);
          if (entity != null)
          {
            if (requestContext.getMethodType() == ResourceMethod.CREATE && entity instanceof ErrorResponse
                && responseContext.getHttpStatus() == REQ_FILTER_ERROR_STATUS)
            {
              ErrorResponse errorResponse = (ErrorResponse) entity;
              errorResponse.setMessage(RESP_FILTER_ERROR_MESSAGE);
              responseContext.setHttpStatus(RESP_FILTER_ERROR_STATUS);
            }
            else if (requestContext.getMethodType() == ResourceMethod.GET
                && responseContext.getHttpStatus() == HttpStatus.S_200_OK)
            {
              Greeting greeting = new Greeting(entity.data());
              if (greeting.hasTone())
              {
                greeting.setTone(mapToneForOutgoingResponse(greeting.getTone()));
                responseContext.setResponseEntity(greeting);
              }
            }
          }
          return null;
        }
      }).when(_responseFilter).onResponse(any(FilterRequestContext.class), any(FilterResponseContext.class));
      respFilters = Arrays.asList(_responseFilter);
    }
    init(reqFilters, respFilters);
  }

  private static Tone mapToneForIncomingRequest(Tone inputTone)
  {
    return toneMapper.get(inputTone);
  }

  private static Tone mapToneForOutgoingResponse(Tone outputTone)
  {
    return toneMapper.inverse().get(outputTone);
  }

  @DataProvider
  private Object[][] requestBuilderDataProvider()
  {
    return new Object[][] {
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders()), Tone.FRIENDLY, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders()), Tone.FRIENDLY, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseBuilders()), Tone.FRIENDLY, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseRequestBuilders()), Tone.FRIENDLY, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackBuilders()), Tone.FRIENDLY, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackRequestBuilders()), Tone.FRIENDLY, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxBuilders()), Tone.FRIENDLY, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxRequestBuilders()), Tone.FRIENDLY, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskBuilders()), Tone.FRIENDLY, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskRequestBuilders()), Tone.FRIENDLY, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders()), Tone.INSULTING, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders()), Tone.INSULTING, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseBuilders()), Tone.INSULTING, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseRequestBuilders()), Tone.INSULTING, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackBuilders()), Tone.INSULTING, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackRequestBuilders()), Tone.INSULTING, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxBuilders()), Tone.INSULTING, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxRequestBuilders()), Tone.INSULTING, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskBuilders()), Tone.INSULTING, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskRequestBuilders()), Tone.INSULTING, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders()), Tone.FRIENDLY, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders()), Tone.FRIENDLY, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseBuilders()), Tone.FRIENDLY, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseRequestBuilders()), Tone.FRIENDLY, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackBuilders()), Tone.FRIENDLY, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackRequestBuilders()), Tone.FRIENDLY, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxBuilders()), Tone.FRIENDLY, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxRequestBuilders()), Tone.FRIENDLY, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskBuilders()), Tone.FRIENDLY, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskRequestBuilders()), Tone.FRIENDLY, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders()), Tone.INSULTING, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders()), Tone.INSULTING, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseBuilders()), Tone.INSULTING, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseRequestBuilders()), Tone.INSULTING, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackBuilders()), Tone.INSULTING, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackRequestBuilders()), Tone.INSULTING, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxBuilders()), Tone.INSULTING, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxRequestBuilders()), Tone.INSULTING, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskBuilders()), Tone.INSULTING, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskRequestBuilders()), Tone.INSULTING, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders(FORCE_USE_NEXT_OPTIONS)), Tone.FRIENDLY, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(FORCE_USE_NEXT_OPTIONS)), Tone.FRIENDLY,
            false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseBuilders(FORCE_USE_NEXT_OPTIONS)), Tone.FRIENDLY,
            false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseRequestBuilders(FORCE_USE_NEXT_OPTIONS)),
            Tone.FRIENDLY, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackBuilders(FORCE_USE_NEXT_OPTIONS)), Tone.FRIENDLY,
            false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackRequestBuilders(FORCE_USE_NEXT_OPTIONS)),
            Tone.FRIENDLY, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxBuilders(FORCE_USE_NEXT_OPTIONS)),
            Tone.FRIENDLY, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxRequestBuilders(FORCE_USE_NEXT_OPTIONS)),
            Tone.FRIENDLY, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskBuilders(FORCE_USE_NEXT_OPTIONS)), Tone.FRIENDLY,
            false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskRequestBuilders(FORCE_USE_NEXT_OPTIONS)),
            Tone.FRIENDLY, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders(FORCE_USE_NEXT_OPTIONS)), Tone.INSULTING, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(FORCE_USE_NEXT_OPTIONS)), Tone.INSULTING,
            false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseBuilders(FORCE_USE_NEXT_OPTIONS)), Tone.INSULTING,
            false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseRequestBuilders(FORCE_USE_NEXT_OPTIONS)),
            Tone.INSULTING, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackBuilders(FORCE_USE_NEXT_OPTIONS)),
            Tone.INSULTING, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackRequestBuilders(FORCE_USE_NEXT_OPTIONS)),
            Tone.INSULTING, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxBuilders(FORCE_USE_NEXT_OPTIONS)),
            Tone.INSULTING, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxRequestBuilders(FORCE_USE_NEXT_OPTIONS)),
            Tone.INSULTING, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskBuilders(FORCE_USE_NEXT_OPTIONS)), Tone.INSULTING,
            false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskRequestBuilders(FORCE_USE_NEXT_OPTIONS)),
            Tone.INSULTING, false },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders(FORCE_USE_NEXT_OPTIONS)), Tone.FRIENDLY, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(FORCE_USE_NEXT_OPTIONS)), Tone.FRIENDLY,
            true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseBuilders(FORCE_USE_NEXT_OPTIONS)), Tone.FRIENDLY,
            true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseRequestBuilders(FORCE_USE_NEXT_OPTIONS)),
            Tone.FRIENDLY, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackBuilders(FORCE_USE_NEXT_OPTIONS)), Tone.FRIENDLY,
            true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackRequestBuilders(FORCE_USE_NEXT_OPTIONS)),
            Tone.FRIENDLY, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxBuilders(FORCE_USE_NEXT_OPTIONS)),
            Tone.FRIENDLY, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxRequestBuilders(FORCE_USE_NEXT_OPTIONS)),
            Tone.FRIENDLY, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskBuilders(FORCE_USE_NEXT_OPTIONS)), Tone.FRIENDLY,
            true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskRequestBuilders(FORCE_USE_NEXT_OPTIONS)),
            Tone.FRIENDLY, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsBuilders(FORCE_USE_NEXT_OPTIONS)), Tone.INSULTING, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsRequestBuilders(FORCE_USE_NEXT_OPTIONS)), Tone.INSULTING,
            true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseBuilders(FORCE_USE_NEXT_OPTIONS)), Tone.INSULTING,
            true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseRequestBuilders(FORCE_USE_NEXT_OPTIONS)),
            Tone.INSULTING, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackBuilders(FORCE_USE_NEXT_OPTIONS)),
            Tone.INSULTING, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsCallbackRequestBuilders(FORCE_USE_NEXT_OPTIONS)),
            Tone.INSULTING, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxBuilders(FORCE_USE_NEXT_OPTIONS)),
            Tone.INSULTING, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseCtxRequestBuilders(FORCE_USE_NEXT_OPTIONS)),
            Tone.INSULTING, true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskBuilders(FORCE_USE_NEXT_OPTIONS)), Tone.INSULTING,
            true },
        { new RootBuilderWrapper<Long, Greeting>(new GreetingsTaskRequestBuilders(FORCE_USE_NEXT_OPTIONS)),
            Tone.INSULTING, true } };
  }
}
