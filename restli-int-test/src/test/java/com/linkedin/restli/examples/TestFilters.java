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
import com.linkedin.restli.client.CreateIdRequest;
import com.linkedin.restli.client.CreateIdRequestBuilder;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.client.response.CreateResponse;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.IdResponse;
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
import com.linkedin.restli.internal.server.response.RecordResponseEnvelope;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.filter.Filter;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.FilterResponseContext;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.util.concurrent.CompletableFuture;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Sets;

import static com.linkedin.restli.examples.TestConstants.FORCE_USE_NEXT_OPTIONS;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


/**
 * @author nshankar
 */
public class TestFilters extends RestLiIntegrationTest
{
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

  private TestFilter _filter;

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
   * @param builders type of request builder.
   * @param tone tone of the greeting to be created.
   * @param responseFilter flag indicating whether or not the response filter is to be hooked up. NOTE: The
   *        request filter is always hooked up.
   * @param responseFilterException the exception the response filter will throw.
   * @throws Exception if anything unexpected happens.
   */
  @Test(dataProvider = "requestBuilderDataProvider")
  public void testGetOldBuilders(RootBuilderWrapper<Long, Greeting> builders, Tone tone, boolean responseFilter,
                                 RuntimeException responseFilterException) throws Exception
  {
    setupFilters(responseFilter, responseFilterException);
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
    Greeting getReturnedGreeting = getClient().sendRequest(getRequest).getResponse().getEntity();
    ValidateDataAgainstSchema.validate(getReturnedGreeting.data(), getReturnedGreeting.schema(),
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
    ResponseFuture<EmptyRecord> future = getClient().sendRequest(request);
    Response<EmptyRecord> response = future.getResponse();
    assertEquals(response.getStatus(), HttpStatus.S_204_NO_CONTENT.getCode());
  }

  private Long createTestData(RootBuilderWrapper<Long, Greeting> builders, Greeting greeting) throws RemoteInvocationException
  {
    RootBuilderWrapper.MethodBuilderWrapper<Long, Greeting, EmptyRecord> createBuilderWrapper = builders.create();
    Long createdId;
    if (createBuilderWrapper.isRestLi2Builder())
    {
      Object objBuilder = createBuilderWrapper.getBuilder();
      @SuppressWarnings("unchecked")
      CreateIdRequestBuilder<Long, Greeting> createIdRequestBuilder =
          (CreateIdRequestBuilder<Long, Greeting>) objBuilder;
      CreateIdRequest<Long, Greeting> request = createIdRequestBuilder.input(greeting).build();
      Response<IdResponse<Long>> response = getClient().sendRequest(request).getResponse();
      createdId = response.getEntity().getId();
    }
    else
    {
      Request<EmptyRecord> request = createBuilderWrapper.input(greeting).build();
      Response<EmptyRecord> response = getClient().sendRequest(request).getResponse();
      @SuppressWarnings("unchecked")
      CreateResponse<Long> createResponse = (CreateResponse<Long>) response.getEntity();
      createdId = createResponse.getId();
    }
    return createdId;

  }

  private void verifyFilters(Tone tone, boolean respFilter)
  {
    int count = tone == Tone.INSULTING ? 1 : 3;
    assertEquals(_filter.getNumRequests(), count);
    if (respFilter)
    {
      if (tone == Tone.INSULTING)
      {
        assertEquals(_filter.getNumErrors(), count);
      }
      else
      {
        assertEquals(_filter.getNumResponses(), count);
      }
    }
  }

  private void setupFilters(boolean responseFilter, final RuntimeException responseFilterException) throws IOException
  {
    if (responseFilter)
    {
      _filter = new TestFilterWithResponse(responseFilterException);
    }
    else
    {
      _filter = new TestFilter();
    }
    List<Filter> filters = Arrays.asList(_filter);
    init(filters);
  }

  private static Tone mapToneForIncomingRequest(Tone inputTone)
  {
    return toneMapper.get(inputTone);
  }

  private static Tone mapToneForOutgoingResponse(Tone outputTone)
  {
    return toneMapper.inverse().get(outputTone);
  }

  private Object[][] to2DArray(Set<List<Object>> objectSet)
  {
    Object[][] result = new Object[objectSet.size()][];
    int i = 0;
    for (List<Object> objects : objectSet)
    {
      result[i] = objects.toArray();
      i++;
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  @DataProvider
  private Object[][] requestBuilderDataProvider()
  {
    Object[] builders = new Object[]{
        new GreetingsBuilders(),
        new GreetingsRequestBuilders(),
        new GreetingsPromiseBuilders(),
        new GreetingsPromiseRequestBuilders(),
        new GreetingsCallbackBuilders(),
        new GreetingsCallbackRequestBuilders(),
        new GreetingsPromiseCtxBuilders(),
        new GreetingsPromiseCtxRequestBuilders(),
        new GreetingsTaskBuilders(),
        new GreetingsTaskRequestBuilders(),
        new GreetingsBuilders(FORCE_USE_NEXT_OPTIONS),
        new GreetingsRequestBuilders(FORCE_USE_NEXT_OPTIONS),
        new GreetingsPromiseBuilders(FORCE_USE_NEXT_OPTIONS),
        new GreetingsPromiseRequestBuilders(FORCE_USE_NEXT_OPTIONS),
        new GreetingsCallbackBuilders(FORCE_USE_NEXT_OPTIONS),
        new GreetingsCallbackRequestBuilders(FORCE_USE_NEXT_OPTIONS),
        new GreetingsPromiseCtxBuilders(FORCE_USE_NEXT_OPTIONS),
        new GreetingsPromiseCtxRequestBuilders(FORCE_USE_NEXT_OPTIONS),
        new GreetingsTaskBuilders(FORCE_USE_NEXT_OPTIONS),
        new GreetingsTaskRequestBuilders(FORCE_USE_NEXT_OPTIONS)
    };
    Set<Object> builderWrapperSet = new HashSet<>();
    for (Object builder : builders)
    {
      builderWrapperSet.add(new RootBuilderWrapper<Long, Greeting>(builder));
    }
    Set<Tone> toneSet = new HashSet<>(Arrays.asList(Tone.FRIENDLY, Tone.INSULTING));
    Set<Boolean> responseFilterSet = new HashSet<>(Arrays.asList(false, true));
    Set<Exception> exceptionSet = new HashSet<>(Arrays.asList(
        new RestLiServiceException(RESP_FILTER_ERROR_STATUS, RESP_FILTER_ERROR_MESSAGE),
        new RestLiServiceException(RESP_FILTER_ERROR_STATUS, RESP_FILTER_ERROR_MESSAGE, new RuntimeException("Original cause")),
        new RoutingException(RESP_FILTER_ERROR_MESSAGE, RESP_FILTER_ERROR_STATUS.getCode())
    ));
    List<Set<? extends Object>> sets = Arrays.asList(builderWrapperSet, toneSet, responseFilterSet, exceptionSet);
    Object[][] dataSources = to2DArray(Sets.cartesianProduct(sets));
    // Sanity check for array dimensions.
    assertEquals(dataSources.length, builderWrapperSet.size() * toneSet.size() * responseFilterSet.size() * exceptionSet.size());
    for (int i = 0; i < dataSources.length; i++)
    {
      assertEquals(dataSources[i].length, sets.size());
    }
    return dataSources;
  }

  // Filter for testing purposes. Keeps track of number of calls to each function and verifies data in each call
  private class TestFilter implements Filter
  {
    protected final Integer spValue = Integer.valueOf(100);
    protected final String spKey = "Counter";
    protected int numRequests;
    protected int numResponses;
    protected int numErrors;

    public TestFilter()
    {
      numRequests = 0;
      numResponses = 0;
      numErrors = 0;
    }

    public int getNumRequests()
    {
      return numRequests;
    }

    public int getNumResponses()
    {
      return numResponses;
    }

    public int getNumErrors()
    {
      return numErrors;
    }

    @Override
    public CompletableFuture<Void> onRequest(FilterRequestContext requestContext)
    {
      numRequests++;
      requestContext.getFilterScratchpad().put(spKey, spValue);
      if (requestContext.getMethodType() == ResourceMethod.CREATE)
      {
        RecordTemplate entity = requestContext.getRequestData().getEntity();
        if (entity != null && entity instanceof Greeting)
        {
          Greeting greeting = (Greeting) entity;
          if (greeting.hasTone())
          {
            // filter does not allow insulting greetings, so should throw exception if one is found
            Tone tone = greeting.getTone();
            if (tone == Tone.INSULTING)
            {
              throw new RestLiServiceException(REQ_FILTER_ERROR_STATUS, REQ_FILTER_ERROR_MESSAGE);
            }
            greeting.setTone(mapToneForIncomingRequest(tone));
          }
        }
      }
      return CompletableFuture.completedFuture(null);
    }
  }

  private class TestFilterWithResponse extends TestFilter
  {
    private RuntimeException _responseFilterException;

    public TestFilterWithResponse(RuntimeException responseFilterException)
    {
      _responseFilterException = responseFilterException;
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Void> onResponse(FilterRequestContext requestContext,
                                              FilterResponseContext responseContext)
    {
      numResponses++;
      // Verify the scratch pad value.
      assertTrue(requestContext.getFilterScratchpad().get(spKey) == spValue);
      RecordTemplate entity;

      switch (responseContext.getResponseData().getResponseType())
      {
        case SINGLE_ENTITY:
          entity = ((RestLiResponseData<RecordResponseEnvelope>)responseContext.getResponseData()).getResponseEnvelope().getRecord();
          break;
        case STATUS_ONLY:
          entity = null;
          break;
        default:
          throw new RuntimeException("Unexpected resolver type.");
      }
      if (entity != null && requestContext.getMethodType() == ResourceMethod.GET
          && responseContext.getResponseData().getResponseEnvelope().getStatus() == HttpStatus.S_200_OK)
      {
        Greeting greeting = new Greeting(entity.data());
        if (greeting.hasTone())
        {
          greeting.setTone(mapToneForOutgoingResponse(greeting.getTone()));
          ((RestLiResponseData<RecordResponseEnvelope>)responseContext.getResponseData()).getResponseEnvelope().setRecord(greeting, HttpStatus.S_200_OK);
        }
      }
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> onError(Throwable t, final FilterRequestContext requestContext,
                                           final FilterResponseContext responseContext)
    {
      numErrors++;
      if (requestContext.getMethodType() == ResourceMethod.CREATE
          && responseContext.getResponseData().getResponseEnvelope().getStatus() == REQ_FILTER_ERROR_STATUS)
      {
        throw _responseFilterException;
      }
      CompletableFuture<Void> future = new CompletableFuture<>();
      future.completeExceptionally(t);
      return future;
    }
  }
}
