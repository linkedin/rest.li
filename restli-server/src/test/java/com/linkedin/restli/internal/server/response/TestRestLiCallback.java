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

package com.linkedin.restli.internal.server.response;


import com.linkedin.common.callback.Callback;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.HeaderUtil;
import com.linkedin.restli.internal.server.ResourceContextImpl;
import com.linkedin.restli.internal.server.ResponseType;
import com.linkedin.restli.internal.server.RestLiCallback;
import com.linkedin.restli.internal.server.RestLiMethodInvoker;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.filter.FilterChainCallback;
import com.linkedin.restli.internal.server.filter.FilterChainCallbackImpl;
import com.linkedin.restli.internal.server.filter.FilterChainDispatcher;
import com.linkedin.restli.internal.server.filter.FilterChainDispatcherImpl;
import com.linkedin.restli.internal.server.filter.RestLiFilterChain;
import com.linkedin.restli.internal.server.filter.RestLiFilterResponseContextFactory;
import com.linkedin.restli.internal.server.methods.arguments.RestLiArgumentBuilder;
import com.linkedin.restli.server.RestLiResponseAttachments;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.filter.Filter;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.FilterResponseContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.Maps;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;


/**
 * @author nshankar
 */
public class TestRestLiCallback
{
  @Mock
  private RestRequest _restRequest;
  @Mock
  private RoutingResult _routingResult;
  @Mock
  private RestLiMethodInvoker _methodInvoker;
  @Mock
  private RestLiArgumentBuilder _argumentBuilder;
  @Mock
  private RestLiResponseHandler _responseHandler;
  @Mock
  private Callback<RestLiResponse> _callback;

  private RestLiCallback _noFilterRestLiCallback;

  private RestLiCallback _oneFilterRestLiCallback;

  private RestLiCallback _twoFilterRestLiCallback;

  @Mock
  private FilterRequestContext _filterRequestContext;

  @Mock
  private Filter _filter;

  private RestLiFilterChain _zeroFilterChain;
  private RestLiFilterChain _oneFilterChain;
  private RestLiFilterChain _twoFilterChain;

  private RestLiFilterResponseContextFactory _filterResponseContextFactory;

  @BeforeTest
  protected void setUp() throws Exception
  {
    MockitoAnnotations.initMocks(this);

    _filterResponseContextFactory = new RestLiFilterResponseContextFactory(_restRequest, _routingResult,
                                                                                   _responseHandler);
    when(_routingResult.getContext()).thenReturn(new ResourceContextImpl());
    ErrorResponseBuilder errorResponseBuilder = new ErrorResponseBuilder();
    FilterChainDispatcher filterChainDispatcher = new FilterChainDispatcherImpl(_routingResult,
        _methodInvoker, _argumentBuilder);
    FilterChainCallback filterChainCallback = new FilterChainCallbackImpl(_routingResult,
        _responseHandler,
        _callback, errorResponseBuilder);

    _zeroFilterChain = new RestLiFilterChain(null, filterChainDispatcher, filterChainCallback);
    _oneFilterChain = new RestLiFilterChain(Arrays.asList(_filter), filterChainDispatcher, filterChainCallback);
    _twoFilterChain = new RestLiFilterChain(Arrays.asList(_filter, _filter), filterChainDispatcher, filterChainCallback);

    _noFilterRestLiCallback =
        new RestLiCallback(_filterRequestContext, _filterResponseContextFactory,
                                   _zeroFilterChain);
    _oneFilterRestLiCallback =
        new RestLiCallback(_filterRequestContext, _filterResponseContextFactory,
                                   _oneFilterChain);
    _twoFilterRestLiCallback =
        new RestLiCallback(_filterRequestContext, _filterResponseContextFactory,
                                   _twoFilterChain);
  }

  @AfterMethod
  protected void resetMocks() throws Exception
  {
    reset(_filter, _filterRequestContext, _restRequest, _routingResult, _responseHandler, _callback);
    when(_routingResult.getContext()).thenReturn(new ResourceContextImpl());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testOnSuccessWithUnstructuredDataResponse() throws Exception
  {
    String result = "foo";
    RestLiResponseAttachments restLiResponseAttachments = new RestLiResponseAttachments.Builder().build();
    RestLiResponseData<UpdateResponseEnvelope> responseData = ResponseDataBuilderUtil.buildUpdateResponseData(HttpStatus.S_200_OK);
    RestLiResponse partialResponse = new RestLiResponse.Builder().build();
    // Set up.
    when((RestLiResponseData<UpdateResponseEnvelope>) _responseHandler.buildRestLiResponseData(_restRequest, _routingResult, result)).thenReturn(responseData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseData)).thenReturn(partialResponse);

    // Invoke.
    _noFilterRestLiCallback.onSuccess(result);

    // Verify.
    verify(_responseHandler).buildPartialResponse(_routingResult, responseData);
    verify(_responseHandler).buildRestLiResponseData(_restRequest, _routingResult, result);
    verify(_callback).onSuccess(partialResponse);
    verifyZeroInteractions(_restRequest);
    verifyNoMoreInteractions(_responseHandler, _callback);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testOnSuccessNoFilters() throws Exception
  {
    String result = "foo";
    RestLiResponseAttachments restLiResponseAttachments = new RestLiResponseAttachments.Builder().build();
    RestLiResponseData<UpdateResponseEnvelope> responseData = ResponseDataBuilderUtil.buildUpdateResponseData(HttpStatus.S_200_OK);
    RestLiResponse partialResponse = new RestLiResponse.Builder().build();
    // Set up.
    when((RestLiResponseData<UpdateResponseEnvelope>) _responseHandler.buildRestLiResponseData(_restRequest, _routingResult, result)).thenReturn(responseData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseData)).thenReturn(partialResponse);

    // Invoke.
    _noFilterRestLiCallback.onSuccess(result);

    // Verify.
    verify(_responseHandler).buildPartialResponse(_routingResult, responseData);
    verify(_responseHandler).buildRestLiResponseData(_restRequest, _routingResult, result);
    verify(_callback).onSuccess(partialResponse);
    verifyZeroInteractions(_restRequest);
    verifyNoMoreInteractions(_responseHandler, _callback);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testOnSuccessBuildPartialResponseFailure() throws Exception
  {
    String result = "foo";
    RestLiResponseData<UpdateResponseEnvelope> responseData = ResponseDataBuilderUtil.buildUpdateResponseData(HttpStatus.S_200_OK);
    RestResponse restResponse = new RestResponseBuilder().build();
    // Set up.
    when((RestLiResponseData<UpdateResponseEnvelope>) _responseHandler.buildRestLiResponseData(_restRequest, _routingResult, result)).thenReturn(responseData);
    Exception e = new RuntimeException("Error1");
    when(_responseHandler.buildPartialResponse(_routingResult, responseData)).thenThrow(e);

    // Invoke.
    _noFilterRestLiCallback.onSuccess(result);

    // Verify.
    verify(_responseHandler).buildPartialResponse(_routingResult, responseData);
    verify(_responseHandler).buildRestLiResponseData(_restRequest, _routingResult, result);
    ArgumentCaptor<RestLiResponseException> partialRestResponseExceptionCaptor =
        ArgumentCaptor.forClass(RestLiResponseException.class);
    verify(_callback).onError(partialRestResponseExceptionCaptor.capture());
    verifyZeroInteractions(_restRequest);
    verifyNoMoreInteractions(_responseHandler, _callback);

    RestLiResponse restLiResponse = partialRestResponseExceptionCaptor.getValue().getRestLiResponse();
    assertEquals(restLiResponse.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);
    assertEquals(RestConstants.HEADER_VALUE_ERROR,
        restLiResponse.getHeader(HeaderUtil.getErrorResponseHeaderName(Collections.emptyMap())));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testOnErrorRestLiServiceExceptionNoFilters() throws Exception
  {
    RestLiServiceException ex = new RestLiServiceException(HttpStatus.S_404_NOT_FOUND);
    Map<String, String> inputHeaders = Maps.newHashMap();
    inputHeaders.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION,
                     AllProtocolVersions.BASELINE_PROTOCOL_VERSION.toString());

    Map<String, String> restExceptionHeaders = Maps.newHashMap();
    restExceptionHeaders.put("foo", "bar");

    @SuppressWarnings("rawtypes")
    ArgumentCaptor<Map> augErrorHeadersCapture = ArgumentCaptor.forClass(Map.class);
    RestLiResponseData<?> responseData = new RestLiResponseDataImpl<>(new GetResponseEnvelope(ex),
        restExceptionHeaders, Collections.emptyList());

    RestLiResponse partialResponse = new RestLiResponse.Builder().build();
    // Set up.
    when(_restRequest.getHeaders()).thenReturn(inputHeaders);
    when(_responseHandler.buildExceptionResponseData(eq(_routingResult), eq(ex),
                                                     augErrorHeadersCapture.capture(), anyList())).thenReturn(responseData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseData)).thenReturn(partialResponse);

    // Invoke.
    _noFilterRestLiCallback.onError(ex);

    // Verify.
    verify(_responseHandler).buildExceptionResponseData(eq(_routingResult), eq(ex),
                                                        augErrorHeadersCapture.capture(), anyList());
    verify(_responseHandler).buildPartialResponse(_routingResult, responseData);
    ArgumentCaptor<RestLiResponseException> exceptionCaptor = ArgumentCaptor.forClass(RestLiResponseException.class);
    verify(_callback).onError(exceptionCaptor.capture());
    assertEquals(exceptionCaptor.getValue().getCause(), ex);
    assertEquals(exceptionCaptor.getValue().getRestLiResponse(), partialResponse);
    verify(_restRequest, times(1)).getHeaders();
    verifyNoMoreInteractions(_restRequest, _responseHandler, _callback);
    Map<String, String> augErrorHeaders = augErrorHeadersCapture.getValue();
    assertNotNull(augErrorHeaders);
    assertFalse(augErrorHeaders.isEmpty());
    assertTrue(augErrorHeaders.containsKey(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION));
    assertEquals(augErrorHeaders.get(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION),
                 AllProtocolVersions.BASELINE_PROTOCOL_VERSION.toString());
    String errorHeaderName = HeaderUtil.getErrorResponseHeaderName(inputHeaders);
    assertTrue(augErrorHeaders.containsKey(errorHeaderName));
    assertEquals(augErrorHeaders.get(errorHeaderName), RestConstants.HEADER_VALUE_ERROR);
  }

  @DataProvider(name = "provideExceptions")
  private Object[][] provideExceptions()
  {
    return new Object[][] { { new RuntimeException("Test runtime exception") },
        { new RoutingException("Test routing exception", 404) },
        { new RestException(new RestResponseBuilder().setStatus(404).build()) },
        { new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, "Test service exception") },
        { new RestLiServiceException(HttpStatus.S_403_FORBIDDEN, "Wrapped runtime exception with custom status",
                                     new RuntimeException("Original cause")) } };
  }

  @SuppressWarnings("unchecked")
  @Test(dataProvider = "provideExceptions")
  public void testOnErrorOtherExceptionNoFilters(Exception ex) throws Exception
  {
    ArgumentCaptor<RestLiServiceException> exCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    RestLiResponse partialResponse = new RestLiResponse.Builder().build();
    RestLiServiceException wrappedEx = new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, ex);
    RestLiResponseData<?> responseData = ResponseDataBuilderUtil.buildGetResponseData(wrappedEx);

    Map<String, String> inputHeaders = Maps.newHashMap();
    inputHeaders.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, "2.0.0");

    // Set up.
    when(_restRequest.getHeaders()).thenReturn(inputHeaders);
    when(_responseHandler.buildExceptionResponseData(eq(_routingResult), exCapture.capture(),
                                                     anyMap(), anyList())).thenReturn(responseData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseData)).thenReturn(partialResponse);

    // Invoke.
    _noFilterRestLiCallback.onError(ex);

    // Verify.
    verify(_responseHandler).buildExceptionResponseData(eq(_routingResult),
                                                        exCapture.capture(), anyMap(), anyList());
    verify(_responseHandler).buildPartialResponse(_routingResult, responseData);
    ArgumentCaptor<RestLiResponseException> exceptionCaptor = ArgumentCaptor.forClass(RestLiResponseException.class);
    verify(_callback).onError(exceptionCaptor.capture());
    assertEquals(exceptionCaptor.getValue().getCause(), wrappedEx);
    assertEquals(exceptionCaptor.getValue().getRestLiResponse(), partialResponse);
    verify(_restRequest, times(1)).getHeaders();
    verifyNoMoreInteractions(_restRequest, _responseHandler, _callback);
    RestLiServiceException restliEx = exCapture.getValue();
    assertNotNull(restliEx);
    if (ex instanceof RoutingException)
    {
      assertEquals(HttpStatus.fromCode(((RoutingException) ex).getStatus()), restliEx.getStatus());
    }
    else if (ex instanceof RestLiServiceException)
    {
      assertEquals(((RestLiServiceException) ex).getStatus(), restliEx.getStatus());
    }
    else
    {
      assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, restliEx.getStatus());
    }
    assertEquals(ex.getMessage(), restliEx.getMessage());
    if (ex instanceof RestLiServiceException)
    {
      assertEquals(ex, restliEx);
    }
    else
    {
      assertEquals(ex, restliEx.getCause());
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testOnSuccessWithExceptionBuildingResponseNoFilters() throws Exception
  {
    String result = "foo";
    RestLiServiceException ex = new RestLiServiceException(HttpStatus.S_422_UNPROCESSABLE_ENTITY);
    Map<String, String> inputHeaders = Maps.newHashMap();
    inputHeaders.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION,
                     AllProtocolVersions.BASELINE_PROTOCOL_VERSION.toString());
    RestLiResponseData<?> responseData = ResponseDataBuilderUtil.buildGetResponseData(ex);

    RestLiResponse partialResponse = new RestLiResponse.Builder().build();
    // Set up.
    // Throw an exception when we try to build the response data.
    when(_responseHandler.buildRestLiResponseData(_restRequest, _routingResult, result)).thenThrow(ex);
    when(_restRequest.getHeaders()).thenReturn(inputHeaders);
    when(_responseHandler.buildExceptionResponseData(eq(_routingResult), eq(ex),
                                                     anyMap(), anyList())).thenReturn(responseData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseData)).thenReturn(partialResponse);

    // invoke request filters so cursor is in correct place
    when(_filter.onRequest(any(FilterRequestContext.class))).thenReturn(CompletableFuture.completedFuture(null));
    _twoFilterChain.onRequest(_filterRequestContext, _filterResponseContextFactory);
    // Invoke.
    _noFilterRestLiCallback.onSuccess(result);

    // Verify.
    verify(_responseHandler).buildRestLiResponseData(_restRequest, _routingResult, result);
    verify(_responseHandler).buildExceptionResponseData(eq(_routingResult), eq(ex),
                                                        anyMap(), anyList());
    verify(_responseHandler).buildPartialResponse(_routingResult, responseData);
    verify(_callback).onError(any(RestLiResponseException.class));
    verify(_restRequest).getHeaders();
    verifyNoMoreInteractions(_restRequest, _responseHandler, _callback);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testOnSuccessWithFiltersSuccessful() throws Exception
  {
    String result = "foo";
    final RestLiResponseAttachments restLiResponseAttachments = new RestLiResponseAttachments.Builder().build();
    final RecordTemplate entityFromApp = Foo.createFoo("Key", "One");
    final Map<String, String> headersFromApp = Maps.newHashMap();
    headersFromApp.put("Key", "Input");
    final RecordTemplate entityFromFilter1 = Foo.createFoo("Key", "Two");
    final RecordTemplate entityFromFilter2 = Foo.createFoo("Key", "Three");
    final Map<String, String> headersFromFilters = Maps.newHashMap();
    headersFromFilters.put("Key", "Output");

    RestLiResponseData<CreateResponseEnvelope> appResponseData = new RestLiResponseDataImpl<>(
        new CreateResponseEnvelope(HttpStatus.S_200_OK, entityFromApp, false), headersFromApp, Collections.emptyList());

    RestLiResponse partialResponse = new RestLiResponse.Builder().build();

    // Setup.
    when((RestLiResponseData<CreateResponseEnvelope>)_responseHandler.buildRestLiResponseData(_restRequest, _routingResult, result)).thenReturn(appResponseData);
    when(_responseHandler.buildPartialResponse(_routingResult, appResponseData)).thenReturn(partialResponse);
    // Mock the behavior of the first filter.
    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterRequestContext requestContext = (FilterRequestContext) args[0];
        FilterResponseContext responseContext = (FilterResponseContext) args[1];

        // Verify incoming data.
        RestLiResponseData<CreateResponseEnvelope> responseData = (RestLiResponseData<CreateResponseEnvelope>) responseContext.getResponseData();
        assertEquals(HttpStatus.S_200_OK, responseData.getResponseEnvelope().getStatus());
        assertEquals(headersFromApp, responseData.getHeaders());
        assertEquals(entityFromApp, responseData.getResponseEnvelope().getRecord());
        // Modify data in filter.
        setStatus(responseContext, HttpStatus.S_400_BAD_REQUEST);
        responseData.getResponseEnvelope().setRecord(entityFromFilter1,
                                                                                HttpStatus.S_400_BAD_REQUEST);
        responseData.getHeaders().clear();
        return CompletableFuture.completedFuture(null);
      }
    }).doAnswer(new Answer<Object>()
        // Mock the behavior of the second filter.
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterRequestContext requestContext = (FilterRequestContext) args[0];
        FilterResponseContext responseContext = (FilterResponseContext) args[1];

        // Verify incoming data.
        RestLiResponseData<CreateResponseEnvelope> responseData = (RestLiResponseData<CreateResponseEnvelope>) responseContext.getResponseData();
        assertEquals(HttpStatus.S_400_BAD_REQUEST, responseData.getResponseEnvelope().getStatus());
        assertTrue(responseData.getHeaders().isEmpty());
        assertEquals(responseData.getResponseEnvelope().getRecord(), entityFromFilter1);
        // Modify data in filter.
        setStatus(responseContext, HttpStatus.S_403_FORBIDDEN);
        responseData.getResponseEnvelope().setRecord(entityFromFilter2,
                                                                                HttpStatus.S_403_FORBIDDEN);
        responseData.getHeaders().putAll(headersFromFilters);
        return CompletableFuture.completedFuture(null);
      }
    }).when(_filter).onResponse(eq(_filterRequestContext), any(FilterResponseContext.class));

    // invoke request filters so cursor is in correct place
    when(_filter.onRequest(any(FilterRequestContext.class))).thenReturn(CompletableFuture.completedFuture(null));
    _twoFilterChain.onRequest(_filterRequestContext, _filterResponseContextFactory);
    // Invoke with some response attachments.
    _twoFilterRestLiCallback.onSuccess(result);

    // Verify.
    assertNotNull(appResponseData);
    assertEquals(HttpStatus.S_403_FORBIDDEN, appResponseData.getResponseEnvelope().getStatus());
    assertEquals(entityFromFilter2, appResponseData.getResponseEnvelope().getRecord());
    assertEquals(headersFromFilters, appResponseData.getHeaders());
    verify(_responseHandler).buildRestLiResponseData(_restRequest, _routingResult, result);
    verify(_responseHandler).buildPartialResponse(_routingResult, appResponseData);
    verify(_callback).onSuccess(partialResponse);
    verifyZeroInteractions(_restRequest);
    verifyNoMoreInteractions(_responseHandler, _callback);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testOnSuccessWithFiltersExceptionFromFirstFilterSecondFilterHandlesEx() throws Exception
  {
    // App stuff.
    final RecordTemplate entityFromApp = Foo.createFoo("Key", "One");

    RestLiResponseData<CreateResponseEnvelope>
        appResponseData = ResponseDataBuilderUtil.buildCreateResponseData(HttpStatus.S_200_OK, entityFromApp);

    // Filter stuff.
    final Map<String, String> errorHeaders = buildErrorHeaders();

    final RecordTemplate entityFromFilter = Foo.createFoo("Key", "Two");
    RestLiResponse partialFilterErrorResponse = new RestLiResponse.Builder().build();
    final Exception exFromFilter = new RuntimeException("Exception From Filter");
    // Setup.
    when((RestLiResponseData<CreateResponseEnvelope>)_responseHandler.buildRestLiResponseData(_restRequest, _routingResult, entityFromApp)).thenReturn(appResponseData);
    when(_restRequest.getHeaders()).thenReturn(null);
    when(_responseHandler.buildPartialResponse(_routingResult, appResponseData)).thenReturn(partialFilterErrorResponse);

    // Mock filter behavior.
    doThrow(exFromFilter).when(_filter).onResponse(eq(_filterRequestContext), any(FilterResponseContext.class));

    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        Throwable t = (Throwable) args[0];
        FilterRequestContext requestContext = (FilterRequestContext) args[1];
        FilterResponseContext responseContext = (FilterResponseContext) args[2];

        // The second filter should be invoked with details of the exception thrown by the first
        // filter.
        RestLiResponseData<CreateResponseEnvelope> responseData = (RestLiResponseData<CreateResponseEnvelope>) responseContext.getResponseData();
        assertEquals(responseData.getResponseEnvelope().getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);
        assertNull(responseData.getResponseEnvelope().getRecord());
        assertEquals(responseData.getHeaders(), errorHeaders);
        assertEquals(responseData.getResponseEnvelope().getException().getStatus(),
                     HttpStatus.S_500_INTERNAL_SERVER_ERROR);

        // Modify data.
        setStatus(responseContext, HttpStatus.S_402_PAYMENT_REQUIRED);
        // The second filter handles the exception thrown by the first filter (i.e.) sets an entity
        // response in the response data.
        responseData.getResponseEnvelope().setRecord(entityFromFilter,
                                                                                HttpStatus.S_402_PAYMENT_REQUIRED);
        responseData.getHeaders().put("error-fixed", "second-filter");
        return CompletableFuture.completedFuture(null);
      }
    }).when(_filter).onError(any(Throwable.class), eq(_filterRequestContext), any(FilterResponseContext.class));

    // invoke request filters so cursor is in correct place
    when(_filter.onRequest(any(FilterRequestContext.class))).thenReturn(CompletableFuture.completedFuture(null));
    _twoFilterChain.onRequest(_filterRequestContext, _filterResponseContextFactory);
    // Invoke.
    _twoFilterRestLiCallback.onSuccess(entityFromApp);

    // Verify.
    verify(_responseHandler).buildRestLiResponseData(_restRequest, _routingResult, entityFromApp);
    verify(_responseHandler).buildPartialResponse(_routingResult, appResponseData);
    verify(_callback).onSuccess(any(RestLiResponse.class));

    Map<String, String> expectedHeaders = Maps.newHashMap();
    expectedHeaders.put("X-RestLi-Protocol-Version", "1.0.0");
    expectedHeaders.put("error-fixed", "second-filter");

    verifyNoMoreInteractions(_responseHandler, _callback);
    assertFalse(appResponseData.getResponseEnvelope().isErrorResponse());
    assertEquals(appResponseData.getResponseEnvelope().getRecord(), entityFromFilter);
    assertNotNull(appResponseData);
    assertEquals(HttpStatus.S_402_PAYMENT_REQUIRED, appResponseData.getResponseEnvelope().getStatus());
    assertEquals(appResponseData.getHeaders(), expectedHeaders);
    assertNull(appResponseData.getResponseEnvelope().getException());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testOnSuccessWithFiltersExceptionFromFirstFilterSecondFilterDoesNotHandleEx() throws Exception
  {
    // App stuff.
    final RecordTemplate entityFromApp = Foo.createFoo("Key", "Two");
    RestLiResponseData<CreateResponseEnvelope>
        appResponseData = ResponseDataBuilderUtil.buildCreateResponseData(HttpStatus.S_200_OK, entityFromApp);

    // Filter stuff.
    ArgumentCaptor<RestLiServiceException> exFromFilterCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    final Map<String, String> headersFromFilter = Maps.newHashMap();
    headersFromFilter.put("Key", "Error from filter");
    RestLiServiceException exceptionFromFilter = new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR);
    RestLiResponseData<?> responseErrorData =
        new RestLiResponseDataImpl<>(new CreateResponseEnvelope(exceptionFromFilter, false), headersFromFilter,
            Collections.emptyList());
    RestLiResponse partialFilterErrorResponse = new RestLiResponse.Builder().build();
    final Exception exFromFilter = new RuntimeException("Exception From Filter");
    Map<String, String> errorHeaders = buildErrorHeaders();

    // Common stuff.
    RestException finalRestException = new RestException(new RestResponseBuilder().build());
    // Setup.
    when((RestLiResponseData<CreateResponseEnvelope>)_responseHandler.buildRestLiResponseData(_restRequest, _routingResult, entityFromApp)).thenReturn(appResponseData);
    when(_restRequest.getHeaders()).thenReturn(null);
    when(_responseHandler.buildExceptionResponseData(eq(_routingResult),
                                                     exFromFilterCapture.capture(), anyMap(), anyList())).thenReturn(responseErrorData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseErrorData)).thenReturn(partialFilterErrorResponse);

    // Mock filter behavior.
    doThrow(exFromFilter).when(_filter).onResponse(eq(_filterRequestContext), any(FilterResponseContext.class));

    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        Throwable t = (Throwable) args[0];
        FilterRequestContext requestContext = (FilterRequestContext) args[1];
        FilterResponseContext responseContext = (FilterResponseContext) args[2];

        // The second filter should be invoked with details of the exception thrown by the first
        // filter.
        RestLiResponseData<CreateResponseEnvelope> responseData = (RestLiResponseData<CreateResponseEnvelope>) responseContext.getResponseData();
        assertEquals(responseData.getResponseEnvelope().getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);
        assertNull(responseData.getResponseEnvelope().getRecord());
        assertEquals(responseData.getHeaders(), errorHeaders);
        assertEquals(responseData.getResponseEnvelope().getException().getStatus(),
                     HttpStatus.S_500_INTERNAL_SERVER_ERROR);

        // Modify data.
        setStatus(responseContext, HttpStatus.S_402_PAYMENT_REQUIRED);
        return completedFutureWithError(responseData.getResponseEnvelope().getException());
      }
    }).when(_filter).onError(any(Throwable.class), eq(_filterRequestContext), any(FilterResponseContext.class));

    // invoke request filters so cursor is in correct place
    when(_filter.onRequest(any(FilterRequestContext.class))).thenReturn(CompletableFuture.completedFuture(null));
    _twoFilterChain.onRequest(_filterRequestContext, _filterResponseContextFactory);
    // Invoke.
    _twoFilterRestLiCallback.onSuccess(entityFromApp);

    // Verify.
    verify(_responseHandler).buildRestLiResponseData(_restRequest, _routingResult, entityFromApp);
    verify(_responseHandler).buildPartialResponse(_routingResult, appResponseData);
    ArgumentCaptor<RestLiResponseException> partialRestResponseExceptionCaptor =
        ArgumentCaptor.forClass(RestLiResponseException.class);
    verify(_callback).onError(partialRestResponseExceptionCaptor.capture());
    verifyNoMoreInteractions(_responseHandler, _callback);

    RestLiResponseException restLiResponseException = partialRestResponseExceptionCaptor.getValue();
    assertTrue(restLiResponseException.getCause() instanceof RestLiServiceException);
    RestLiServiceException restliEx1 = (RestLiServiceException) restLiResponseException.getCause();
    assertNotNull(restliEx1);
    assertEquals(HttpStatus.S_402_PAYMENT_REQUIRED, restliEx1.getStatus());
    // exceptions should not be equal because in new logic we are replacing the exception with new one
    assertNotEquals(exFromFilter.getMessage(), restliEx1.getMessage());
    assertNotEquals(exFromFilter, restliEx1.getCause());

    assertNotNull(appResponseData);
    assertEquals(HttpStatus.S_402_PAYMENT_REQUIRED, appResponseData.getResponseEnvelope().getStatus());
    assertEquals(appResponseData.getHeaders(), errorHeaders);
    assertNull(appResponseData.getResponseEnvelope().getRecord());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testOnSuccessWithFilterThrowable() throws Exception
  {
    // App stuff.
    final RecordTemplate entityFromApp = Foo.createFoo("Key", "Two");

    RestLiResponseData<CreateResponseEnvelope>
        appResponseData = ResponseDataBuilderUtil.buildCreateResponseData(HttpStatus.S_200_OK, entityFromApp);

    // Filter stuff.
    final Map<String, String> headersFromFilter = Maps.newHashMap();
    headersFromFilter.put("Key", "Error from filter");
    RestLiServiceException exception = new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR);
    RestLiResponseData<CreateResponseEnvelope> responseErrorData =
        new RestLiResponseDataImpl<>(new CreateResponseEnvelope(exception, false), headersFromFilter,
            Collections.emptyList());
    RestLiResponse partialFilterErrorResponse = new RestLiResponse.Builder().build();
    final Throwable throwableFromFilter = new NoSuchMethodError("Method foo not found!");

    // Common stuff.
    RestException finalRestException = new RestException(new RestResponseBuilder().build());
    // Setup.
    when((RestLiResponseData<CreateResponseEnvelope>)_responseHandler.buildRestLiResponseData(_restRequest, _routingResult, entityFromApp)).thenReturn(appResponseData);
    when(_restRequest.getHeaders()).thenReturn(null);
    when(_responseHandler.buildExceptionResponseData(eq(_routingResult),
                                                     any(RestLiServiceException.class), anyMap(), anyList()))
        .thenReturn(responseErrorData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseErrorData)).thenReturn(partialFilterErrorResponse);
    // Mock filter behavior.
    doThrow(throwableFromFilter).when(_filter)
        .onResponse(eq(_filterRequestContext), any(FilterResponseContext.class));

    // invoke request filters so cursor is in correct place
    when(_filter.onRequest(any(FilterRequestContext.class))).thenReturn(CompletableFuture.completedFuture(null));
    _oneFilterChain.onRequest(_filterRequestContext, _filterResponseContextFactory);
    // Invoke.
    _oneFilterRestLiCallback.onSuccess(entityFromApp);

    // Verify.
    verify(_responseHandler).buildRestLiResponseData(_restRequest, _routingResult, entityFromApp);
    verify(_responseHandler).buildPartialResponse(_routingResult, appResponseData);
    ArgumentCaptor<RestLiResponseException> partialRestResponseExceptionCaptor =
        ArgumentCaptor.forClass(RestLiResponseException.class);
    verify(_callback).onError(partialRestResponseExceptionCaptor.capture());

    verifyNoMoreInteractions(_responseHandler, _callback);

    RestLiResponseException restLiResponseException = partialRestResponseExceptionCaptor.getValue();
    assertTrue(restLiResponseException.getCause() instanceof RestLiServiceException);
    RestLiServiceException restliEx1 = (RestLiServiceException) restLiResponseException.getCause();
    assertNotNull(restliEx1);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, restliEx1.getStatus());
    assertEquals(throwableFromFilter.getMessage(), restliEx1.getMessage());
    assertEquals(throwableFromFilter, restliEx1.getCause());

    assertNotNull(responseErrorData);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, responseErrorData.getResponseEnvelope().getStatus());
    assertEquals(responseErrorData.getHeaders(), headersFromFilter);
    assertNull(responseErrorData.getResponseEnvelope().getRecord());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testOnSuccessWithFiltersExceptionFromSecondFilter() throws Exception
  {
    // App stuff.
    String result = "foo";

    RestLiResponseData<GetResponseEnvelope> appResponseData = ResponseDataBuilderUtil.buildGetResponseData(HttpStatus.S_200_OK, null);

    // Filter stuff.
    ArgumentCaptor<RestLiServiceException> exFromFilterCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    final Map<String, String> headersFromFilter = Maps.newHashMap();
    headersFromFilter.put("Key", "Error from filter");
    RestLiServiceException exception = new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR);

    RestLiResponseData<?> filterResponseData =
        new RestLiResponseDataImpl<>(new GetResponseEnvelope(exception), headersFromFilter, Collections.emptyList());
    RestLiResponse partialFilterErrorResponse = new RestLiResponse.Builder().build();
    final Exception exFromFilter = new RuntimeException("Exception From Filter");

    // Common stuff.
    RestException finalRestException = new RestException(new RestResponseBuilder().build());
    // Setup.
    when((RestLiResponseData<GetResponseEnvelope>)_responseHandler.buildRestLiResponseData(_restRequest, _routingResult, result)).thenReturn(appResponseData);
    when(_restRequest.getHeaders()).thenReturn(null);
    when(_responseHandler.buildExceptionResponseData(eq(_routingResult), exFromFilterCapture.capture(), anyMap(), anyList())).thenReturn(filterResponseData);
    when(_responseHandler.buildPartialResponse(_routingResult, appResponseData)).thenReturn(partialFilterErrorResponse);
    // Mock filter behavior.
    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterRequestContext requestContext = (FilterRequestContext) args[0];
        FilterResponseContext responseContext = (FilterResponseContext) args[1];
        RestLiResponseData<GetResponseEnvelope> responseData = (RestLiResponseData<GetResponseEnvelope>) responseContext.getResponseData();
        // The second filter should be invoked with details of the exception thrown by the first
        // filter. Verify incoming data.
        assertEquals(responseData.getResponseEnvelope().getStatus(), HttpStatus.S_200_OK);
        assertNull(responseData.getResponseEnvelope().getRecord());
        assertTrue(responseData.getHeaders().isEmpty());
        // Modify data.
        setStatus(responseContext, HttpStatus.S_402_PAYMENT_REQUIRED);

        responseData.getHeaders().put("first-filter", "success");
        return CompletableFuture.completedFuture(null);
      }
    }).doThrow(exFromFilter)
        .when(_filter)
        .onResponse(eq(_filterRequestContext), any(FilterResponseContext.class));

    // invoke request filters so cursor is in correct place
    when(_filter.onRequest(any(FilterRequestContext.class))).thenReturn(CompletableFuture.completedFuture(null));
    _twoFilterChain.onRequest(_filterRequestContext, _filterResponseContextFactory);
    // Invoke.
    _twoFilterRestLiCallback.onSuccess(result);

    // Verify.
    verify(_responseHandler).buildPartialResponse(_routingResult, appResponseData);
    verify(_responseHandler).buildRestLiResponseData(_restRequest, _routingResult, result);
    verify(_responseHandler).buildPartialResponse(_routingResult, appResponseData);
    ArgumentCaptor<RestLiResponseException> partialRestResponseExceptionCaptor =
        ArgumentCaptor.forClass(RestLiResponseException.class);
    verify(_callback).onError(partialRestResponseExceptionCaptor.capture());

    verifyNoMoreInteractions(_responseHandler, _callback);

    RestLiResponseException restLiResponseException = partialRestResponseExceptionCaptor.getValue();
    assertTrue(restLiResponseException.getCause() instanceof RestLiServiceException);
    RestLiServiceException restliEx1 = (RestLiServiceException) restLiResponseException.getCause();
    assertNotNull(restliEx1);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, restliEx1.getStatus());
    assertEquals(exFromFilter.getMessage(), restliEx1.getMessage());
    assertEquals(exFromFilter, restliEx1.getCause());

    Map<String, String> expectedHeaders = buildErrorHeaders();
    expectedHeaders.put("first-filter", "success");

    assertNotNull(appResponseData);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, appResponseData.getResponseEnvelope().getStatus());
    assertEquals(appResponseData.getHeaders(), expectedHeaders);
    assertNull(appResponseData.getResponseEnvelope().getRecord());
  }

  @SuppressWarnings({"unchecked", "deprecation"})
  @Test
  public void testOnErrorWithFiltersNotHandlingAppEx() throws Exception
  {
    Exception exFromApp = new RuntimeException("Runtime exception from app");
    RestLiServiceException appException = new RestLiServiceException(HttpStatus.S_404_NOT_FOUND);

    final Map<String, String> headersFromApp = Maps.newHashMap();
    headersFromApp.put("Key", "Input");
    final Map<String, String> headersFromFilter = Maps.newHashMap();
    headersFromFilter.put("Key", "Output");

    RestLiResponseData<CreateResponseEnvelope> responseData =
        new RestLiResponseDataImpl<>(new CreateResponseEnvelope(appException, false), headersFromApp,
            Collections.emptyList());
    RestLiResponse partialResponse = new RestLiResponse.Builder().build();
    when(_responseHandler.buildExceptionResponseData(eq(_routingResult), any(RestLiServiceException.class),
                                                     anyMap(), anyList())).thenReturn(responseData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseData)).thenReturn(partialResponse);

    // Mock the behavior of the first filter.
    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        Throwable t = (Throwable) args[0];
        FilterRequestContext requestContext = (FilterRequestContext) args[1];
        FilterResponseContext responseContext = (FilterResponseContext) args[2];

        // Verify incoming data.
        RestLiResponseData<CreateResponseEnvelope> responseData = (RestLiResponseData<CreateResponseEnvelope>) responseContext.getResponseData();
        assertEquals(HttpStatus.S_404_NOT_FOUND, responseData.getResponseEnvelope().getStatus());
        assertEquals(headersFromApp, responseData.getHeaders());
        assertNull(responseData.getResponseEnvelope().getRecord());
        // Modify data in filter.
        setStatus(responseContext, HttpStatus.S_400_BAD_REQUEST);
        responseData.getHeaders().clear();
        return completedFutureWithError(responseData.getResponseEnvelope().getException());
      }
    }).doAnswer(new Answer<Object>()
        // Mock the behavior of the second filter.
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        Throwable t = (Throwable) args[0];
        FilterRequestContext requestContext = (FilterRequestContext) args[1];
        FilterResponseContext responseContext = (FilterResponseContext) args[2];

        // Verify incoming data.
        RestLiResponseData<CreateResponseEnvelope> responseData = (RestLiResponseData<CreateResponseEnvelope>) responseContext.getResponseData();
        assertEquals(HttpStatus.S_400_BAD_REQUEST, responseData.getResponseEnvelope().getStatus());
        assertNull(responseData.getResponseEnvelope().getRecord());
        // Modify data in filter.
        setStatus(responseContext, HttpStatus.S_403_FORBIDDEN);
        responseData.getHeaders().putAll(headersFromFilter);
        return completedFutureWithError(responseData.getResponseEnvelope().getException());
      }
    }).when(_filter).onError(any(Throwable.class),
                             eq(_filterRequestContext),
                             any(FilterResponseContext.class));

    // invoke request filters so cursor is in correct place
    when(_filter.onRequest(any(FilterRequestContext.class))).thenReturn(CompletableFuture.completedFuture(null));
    _twoFilterChain.onRequest(_filterRequestContext, _filterResponseContextFactory);
    // Invoke.
    _twoFilterRestLiCallback.onError(exFromApp);
    // Verify.
    assertNotNull(responseData);
    assertEquals(HttpStatus.S_403_FORBIDDEN, responseData.getResponseEnvelope().getStatus());
    assertNull(responseData.getResponseEnvelope().getRecord());
    assertTrue(responseData.getResponseEnvelope().isErrorResponse());
    assertEquals(responseData.getResponseEnvelope().getException().getErrorDetails(), appException.getErrorDetails());
    assertEquals(responseData.getResponseEnvelope().getException().getOverridingFormat(), appException.getOverridingFormat());
    assertEquals(responseData.getResponseEnvelope().getException().getServiceErrorCode(), appException.getServiceErrorCode());
    assertEquals(responseData.getResponseEnvelope().getException().getMessage(), appException.getMessage());

    Map<String, String> expectedHeaders = buildErrorHeaders();
    expectedHeaders.put("Key", "Output");

    assertEquals(expectedHeaders, responseData.getHeaders());
    ArgumentCaptor<RestLiServiceException> exCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    verify(_responseHandler, times(1)).buildExceptionResponseData(eq(_routingResult),
                                                                  exCapture.capture(), anyMap(), anyList());
    verify(_responseHandler).buildPartialResponse(_routingResult, responseData);
    ArgumentCaptor<RestLiResponseException> partialRestResponseExceptionCaptor =
        ArgumentCaptor.forClass(RestLiResponseException.class);
    verify(_callback).onError(partialRestResponseExceptionCaptor.capture());
    verify(_restRequest, times(1)).getHeaders();
    verifyNoMoreInteractions(_restRequest, _responseHandler, _callback);

    final RestLiServiceException restliEx1 = exCapture.getAllValues().get(0);
    assertNotNull(restliEx1);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, restliEx1.getStatus());
    assertEquals(exFromApp.getMessage(), restliEx1.getMessage());
    assertEquals(exFromApp, restliEx1.getCause());

    RestLiResponseException restLiResponseException = partialRestResponseExceptionCaptor.getValue();
    assertEquals(restLiResponseException.getRestLiResponse(), partialResponse);
    assertTrue(restLiResponseException.getCause() instanceof RestLiServiceException);
    RestLiServiceException restliEx2 = (RestLiServiceException) restLiResponseException.getCause();
    assertNotNull(restliEx2);
    assertEquals(HttpStatus.S_403_FORBIDDEN, restliEx2.getStatus());
  }


  @SuppressWarnings("unchecked")
  @Test
  public void testOnErrorWithFiltersSuccessfullyHandlingAppEx() throws Exception
  {
    Exception exFromApp = new RuntimeException("Runtime exception from app");
    RestLiServiceException appException = new RestLiServiceException(HttpStatus.S_404_NOT_FOUND);

    final Map<String, String> headersFromApp = Maps.newHashMap();
    headersFromApp.put("Key", "Input");
    final RecordTemplate entityFromFilter = Foo.createFoo("Key", "Two");
    final Map<String, String> headersFromFilter = Maps.newHashMap();
    headersFromFilter.put("Key", "Output");

    RestLiResponseData<CreateResponseEnvelope> responseData =
        new RestLiResponseDataImpl<>(new CreateResponseEnvelope(appException, false), headersFromApp,
            Collections.emptyList());

    RestLiResponse partialResponse = new RestLiResponse.Builder().build();
    ArgumentCaptor<RestLiServiceException> exCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    when(
        _responseHandler.buildExceptionResponseData(eq(_routingResult), exCapture.capture(),
                                                    anyMap(), anyList())).thenReturn(responseData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseData)).thenReturn(partialResponse);

    // Mock the behavior of the first filter.
    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        Throwable t = (Throwable) args[0];
        FilterRequestContext requestContext = (FilterRequestContext) args[1];
        FilterResponseContext responseContext = (FilterResponseContext) args[2];

        // Verify incoming data.
        RestLiResponseData<CreateResponseEnvelope> responseData = (RestLiResponseData<CreateResponseEnvelope>) responseContext.getResponseData();
        assertEquals(HttpStatus.S_404_NOT_FOUND, responseData.getResponseEnvelope().getStatus());
        assertEquals(headersFromApp, responseData.getHeaders());
        assertNull(responseData.getResponseEnvelope().getRecord());
        // Modify data in filter.
        setStatus(responseContext, HttpStatus.S_400_BAD_REQUEST);
        responseData.getHeaders().clear();
        return CompletableFuture.completedFuture(null);
      }
    }).when(_filter).onError(any(Throwable.class), eq(_filterRequestContext), any(FilterResponseContext.class));

    doAnswer(new Answer<Object>()
        // Mock the behavior of the second filter.
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterRequestContext requestContext = (FilterRequestContext) args[0];
        FilterResponseContext responseContext = (FilterResponseContext) args[1];

        // Verify incoming data.
        RestLiResponseData<CreateResponseEnvelope> responseData = (RestLiResponseData<CreateResponseEnvelope>) responseContext.getResponseData();
        assertEquals(HttpStatus.S_400_BAD_REQUEST, responseData.getResponseEnvelope().getStatus());
        assertTrue(responseData.getHeaders().isEmpty());
        assertNull(responseData.getResponseEnvelope().getRecord());
        // Modify data in filter.
        setStatus(responseContext, HttpStatus.S_403_FORBIDDEN);
        responseData.getResponseEnvelope().setRecord(entityFromFilter,
                                                                                HttpStatus.S_403_FORBIDDEN);
        responseData.getHeaders().putAll(headersFromFilter);
        return CompletableFuture.completedFuture(null);
      }
    }).when(_filter).onResponse(eq(_filterRequestContext), any(FilterResponseContext.class));

    // invoke request filters so cursor is in correct place
    when(_filter.onRequest(any(FilterRequestContext.class))).thenReturn(CompletableFuture.completedFuture(null));
    _twoFilterChain.onRequest(_filterRequestContext, _filterResponseContextFactory);
    // Invoke.
    _twoFilterRestLiCallback.onError(exFromApp);
    // Verify.
    assertNotNull(responseData);
    assertEquals(HttpStatus.S_403_FORBIDDEN, responseData.getResponseEnvelope().getStatus());
    assertEquals(entityFromFilter, responseData.getResponseEnvelope().getRecord());
    assertEquals(headersFromFilter, responseData.getHeaders());
    verify(_responseHandler).buildExceptionResponseData(eq(_routingResult),
                                                        exCapture.capture(), anyMap(), anyList());
    verify(_responseHandler).buildPartialResponse(_routingResult, responseData);
    verify(_callback).onSuccess(partialResponse);
    verify(_restRequest, times(1)).getHeaders();
    verifyNoMoreInteractions(_restRequest, _responseHandler, _callback);
    RestLiServiceException restliEx = exCapture.getValue();
    assertNotNull(restliEx);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, restliEx.getStatus());
    assertEquals(exFromApp.getMessage(), restliEx.getMessage());
    assertEquals(exFromApp, restliEx.getCause());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testOnErrorWithFiltersExceptionFromFirstFilterSecondFilterDoesNotHandle() throws Exception
  {
    // App stuff.
    RestLiServiceException exFromApp = new RestLiServiceException(HttpStatus.S_404_NOT_FOUND, "App failure");
    RestLiResponseData<CreateResponseEnvelope> responseAppData = ResponseDataBuilderUtil.buildCreateResponseData(exFromApp);

    // Filter stuff.
    final Exception exFromFirstFilter = new RuntimeException("Runtime exception from first filter");

    RestLiResponse partialResponse = new RestLiResponse.Builder().build();

    // Setup.
    when(_responseHandler.buildExceptionResponseData(eq(_routingResult),
                                                     any(RestLiServiceException.class), anyMap(), anyList())).thenReturn(responseAppData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseAppData)).thenReturn(partialResponse);
    when(_restRequest.getHeaders()).thenReturn(null);

    Map<String, String> errorHeaders = buildErrorHeaders();

    // Mock filter behavior.
    doThrow(exFromFirstFilter).doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        Throwable t = (Throwable) args[0];
        FilterRequestContext requestContext = (FilterRequestContext) args[1];
        FilterResponseContext responseContext = (FilterResponseContext) args[2];

        // The second filter should be invoked with original exception
        RestLiResponseData<CreateResponseEnvelope> responseData = (RestLiResponseData<CreateResponseEnvelope>) responseContext.getResponseData();
        assertEquals(responseData.getResponseEnvelope().getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);
        assertNull(responseData.getResponseEnvelope().getRecord());
        assertEquals(responseData.getHeaders(), errorHeaders);
        assertTrue(responseData.getResponseEnvelope().isErrorResponse());

        // Modify data.
        setStatus(responseContext, HttpStatus.S_402_PAYMENT_REQUIRED);
        // The second filter does not handle the exception thrown by the first filter (i.e.) the
        // response data still has the error response corresponding to the exception from the first
        // filter.
        return completedFutureWithError(responseData.getResponseEnvelope().getException());
      }
    }).when(_filter).onError(any(Throwable.class), eq(_filterRequestContext), any(FilterResponseContext.class));

    // invoke request filters so cursor is in correct place
    when(_filter.onRequest(any(FilterRequestContext.class))).thenReturn(CompletableFuture.completedFuture(null));
    _twoFilterChain.onRequest(_filterRequestContext, _filterResponseContextFactory);
    // Invoke.
    _twoFilterRestLiCallback.onError(exFromApp);

    // Verify.
    ArgumentCaptor<RestLiServiceException> exCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    verify(_responseHandler).buildExceptionResponseData(eq(_routingResult),
                                                        exCapture.capture(), anyMap(), anyList());
    verify(_responseHandler).buildPartialResponse(_routingResult, responseAppData);
    ArgumentCaptor<RestLiResponseException> partialRestResponseExceptionCaptor =
        ArgumentCaptor.forClass(RestLiResponseException.class);
    verify(_callback).onError(partialRestResponseExceptionCaptor.capture());
    verify(_restRequest).getHeaders();
    verifyNoMoreInteractions(_restRequest, _responseHandler, _callback);
    assertNotNull(responseAppData);
    assertEquals(HttpStatus.S_402_PAYMENT_REQUIRED, responseAppData.getResponseEnvelope().getStatus());
    assertEquals(responseAppData.getHeaders(), errorHeaders);
    assertNull(responseAppData.getResponseEnvelope().getRecord());
    RestLiServiceException restliEx = exCapture.getAllValues().get(0);
    assertNotNull(restliEx);
    assertEquals(exFromApp.getStatus(), restliEx.getStatus());
    assertEquals(exFromApp.getMessage(), restliEx.getMessage());
    RestLiResponseException restLiResponseException = partialRestResponseExceptionCaptor.getValue();
    assertEquals(restLiResponseException.getRestLiResponse(), partialResponse);
    assertTrue(restLiResponseException.getCause() instanceof RestLiServiceException);
    restliEx = (RestLiServiceException) restLiResponseException.getCause();
    assertEquals(HttpStatus.S_402_PAYMENT_REQUIRED, restliEx.getStatus());
  }

  @DataProvider(name = "provideResponseEntities")
  private Object[][] provideResponseEntities()
  {
    RestLiServiceException appException = new RestLiServiceException(HttpStatus.S_404_NOT_FOUND);
    List<Foo> fooCollection = new ArrayList<>();
    fooCollection.add(Foo.createFoo("Key", "One"));
    fooCollection.add(Foo.createFoo("Key", "Two"));
    fooCollection.add(Foo.createFoo("Key", "Three"));
    Map<String, Foo> fooBatch = new HashMap<>();
    fooBatch.put("batchKey1", Foo.createFoo("Key", "One"));
    fooBatch.put("batchKey2", Foo.createFoo("Key", "Two"));
    return new Object[][] {
        { ResourceMethod.GET,
            ResponseDataBuilderUtil.buildGetResponseData(appException),
            Foo.createFoo("Key", "One") },
        { ResourceMethod.FINDER,
            ResponseDataBuilderUtil.buildFinderResponseData(appException),
            fooCollection },
        { ResourceMethod.BATCH_GET,
            ResponseDataBuilderUtil.buildBatchGetResponseData(appException),
            fooBatch }
    };
  }

  @SuppressWarnings({"unchecked", "deprecation"})
  @Test(dataProvider = "provideResponseEntities")
  public void testOnErrorWithFiltersExceptionFromFirstFilterSecondFilterHandles(final ResourceMethod resourceMethod,
                                                                                final RestLiResponseData<?> responseAppData,
                                                                                final Object entityFromFilter2) throws Exception
  {
    // App stuff.
    RestLiServiceException exFromApp = new RestLiServiceException(HttpStatus.S_404_NOT_FOUND, "App failure");

    // Filter stuff.
    final Exception exFromFirstFilter = new RuntimeException("Runtime exception from first filter");
    final Map<String, String> headersFromFilter = Maps.newHashMap();
    headersFromFilter.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, AllProtocolVersions.LATEST_PROTOCOL_VERSION.toString());
    String errorResponseHeaderName = HeaderUtil.getErrorResponseHeaderName(AllProtocolVersions.LATEST_PROTOCOL_VERSION);
    headersFromFilter.put(errorResponseHeaderName, RestConstants.HEADER_VALUE_ERROR);

    RestLiResponse partialResponse = new RestLiResponse.Builder().build();
    ArgumentCaptor<RestLiServiceException> wrappedExCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    final String customHeader = "Custom-Header";
    final String customHeaderValue = "CustomValue";

    // Setup.
    when(_responseHandler.buildExceptionResponseData(eq(_routingResult),
                                                     wrappedExCapture.capture(),
                                                     anyMap(),
                                                     anyList()))
        .thenReturn(responseAppData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseAppData)).thenReturn(partialResponse);
    when(_restRequest.getHeaders()).thenReturn(null);

    // Mock filter behavior.
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterRequestContext requestContext = (FilterRequestContext) args[1];
        FilterResponseContext responseContext = (FilterResponseContext) args[2];
        responseContext.getResponseData().getHeaders().putAll(headersFromFilter);
        return completedFutureWithError(exFromFirstFilter);
      }
    }).doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterRequestContext requestContext = (FilterRequestContext) args[1];
        FilterResponseContext responseContext = (FilterResponseContext) args[2];

        // The second filter should be invoked with details of the exception thrown by the first
        // filter. Verify incoming data.
        assertEquals(responseContext.getResponseData().getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);

        switch (ResponseTypeUtil.fromMethodType(resourceMethod))
        {
          case SINGLE_ENTITY:
            assertNull(responseContext.getResponseData().getRecordResponseEnvelope().getRecord());
            break;
          case GET_COLLECTION:
            assertNull(responseContext.getResponseData().getCollectionResponseEnvelope().getCollectionResponse());
            break;
          case CREATE_COLLECTION:
            assertNull(responseContext.getResponseData().getBatchCreateResponseEnvelope().getCreateResponses());
            break;
          case BATCH_ENTITIES:
            assertNull(responseContext.getResponseData().getBatchResponseEnvelope().getBatchResponseMap());
            break;
          case STATUS_ONLY:
            break;
        }

        assertEquals(responseContext.getResponseData().getHeaders(), headersFromFilter);
        assertTrue(responseContext.getResponseData().isErrorResponse());

        // Modify data.
        responseContext.getResponseData().getHeaders().put(customHeader, customHeaderValue);
        setStatus(responseContext, HttpStatus.S_402_PAYMENT_REQUIRED);
        // The second filter does handles the exception thrown by the first filter (i.e.) clears the
        // error response corresponding to the exception from the first
        // filter.
        if (entityFromFilter2 instanceof RecordTemplate)
        {
          responseContext.getResponseData().getRecordResponseEnvelope().setRecord((RecordTemplate) entityFromFilter2,
                                                                                  HttpStatus.S_402_PAYMENT_REQUIRED);
        }
        else if (entityFromFilter2 instanceof List)
        {
          responseContext.getResponseData()
              .getCollectionResponseEnvelope()
              .setCollectionResponse((List<? extends RecordTemplate>) entityFromFilter2, new CollectionMetadata(), null,
                                     HttpStatus.S_402_PAYMENT_REQUIRED);
        }
        else
        {
          Map<Object, BatchResponseEnvelope.BatchResponseEntry> responseMap = new HashMap<>();
          for (Map.Entry<?, RecordTemplate> entry : ((Map<?, RecordTemplate>) entityFromFilter2).entrySet())
          {
            responseMap.put(entry.getKey(), new BatchResponseEnvelope.BatchResponseEntry(HttpStatus.S_200_OK, entry.getValue()));
          }

          responseContext.getResponseData().getBatchResponseEnvelope().setBatchResponseMap(responseMap,
                                                                                           HttpStatus.S_402_PAYMENT_REQUIRED);
        }
        return CompletableFuture.completedFuture(null);
      }
    }).when(_filter).onError(any(Throwable.class), eq(_filterRequestContext), any(FilterResponseContext.class));

    // invoke request filters so cursor is in correct place
    when(_filter.onRequest(any(FilterRequestContext.class))).thenReturn(CompletableFuture.completedFuture(null));
    _twoFilterChain.onRequest(_filterRequestContext, _filterResponseContextFactory);
    // Invoke.
    _twoFilterRestLiCallback.onError(exFromApp);

    // Verify.
    verify(_responseHandler).buildExceptionResponseData(eq(_routingResult),
                                                        wrappedExCapture.capture(), anyMap(), anyList());
    verify(_responseHandler).buildPartialResponse(_routingResult, responseAppData);
    verify(_callback).onSuccess(partialResponse);
    verify(_restRequest).getHeaders();
    verifyNoMoreInteractions(_restRequest, _responseHandler, _callback);
    assertNotNull(responseAppData);
    assertEquals(HttpStatus.S_402_PAYMENT_REQUIRED, responseAppData.getResponseEnvelope().getStatus());
    // Only the error header should have been cleared.
    assertFalse(responseAppData.getHeaders().containsKey(errorResponseHeaderName));
    assertEquals(responseAppData.getHeaders().get(customHeader), customHeaderValue);
    if (entityFromFilter2 instanceof RecordTemplate)
    {
      assertTrue(responseAppData.getResponseType() == ResponseType.SINGLE_ENTITY);
      assertEquals(responseAppData.getRecordResponseEnvelope().getRecord(), entityFromFilter2);
    }
    else if (entityFromFilter2 instanceof List)
    {
      if (responseAppData.getResponseType() == ResponseType.GET_COLLECTION)
      {
        assertEquals(responseAppData.getCollectionResponseEnvelope().getCollectionResponse(), entityFromFilter2);
      }
      else
      {
        fail();
      }
    }
    else
    {
      assertTrue(responseAppData.getResponseType() == ResponseType.BATCH_ENTITIES);

      Map<Object, RecordTemplate> values = new HashMap<>();
      for(Map.Entry<?, BatchResponseEnvelope.BatchResponseEntry> entry: responseAppData.getBatchResponseEnvelope().getBatchResponseMap().entrySet())
      {
        values.put(entry.getKey(), entry.getValue().getRecord());
      }

      assertEquals(values, entityFromFilter2);
    }
    assertFalse(responseAppData.getResponseEnvelope().isErrorResponse());
    RestLiServiceException restliEx = wrappedExCapture.getAllValues().get(0);
    assertNotNull(restliEx);
    assertEquals(exFromApp.getStatus(), restliEx.getStatus());
    assertEquals(exFromApp.getMessage(), restliEx.getMessage());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testOnErrorWithFiltersExceptionFromSecondFilter() throws Exception
  {
    // App stuff.
    RestLiServiceException exFromApp = new RestLiServiceException(HttpStatus.S_404_NOT_FOUND, "App failure");
    RestLiResponseData<CreateResponseEnvelope> responseAppData = ResponseDataBuilderUtil.buildCreateResponseData(exFromApp);

    // Filter stuff.
    final Exception exFromSecondFilter = new RuntimeException("Runtime exception from second filter");
    RestLiServiceException exception = new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                                                                  exFromSecondFilter);

    RestLiResponseData<CreateResponseEnvelope> responseFilterData = ResponseDataBuilderUtil.buildCreateResponseData(exception);
    RestLiResponse partialResponse = new RestLiResponse.Builder().build();
    // Setup.
    when(_responseHandler.buildExceptionResponseData(eq(_routingResult),
                                                     any(RestLiServiceException.class), anyMap(), anyList()))
        .thenReturn(responseAppData)
        .thenReturn(responseFilterData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseAppData)).thenReturn(partialResponse);
    when(_restRequest.getHeaders()).thenReturn(null);

    // Mock filter behavior.
    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        Throwable t = (Throwable) args[0];
        FilterRequestContext requestContext = (FilterRequestContext) args[1];
        FilterResponseContext responseContext = (FilterResponseContext) args[2];

        RestLiResponseData<CreateResponseEnvelope> responseData = (RestLiResponseData<CreateResponseEnvelope>) responseContext.getResponseData();
        assertEquals(responseData.getResponseEnvelope().getStatus(), HttpStatus.S_404_NOT_FOUND);
        assertNull(responseData.getResponseEnvelope().getRecord());
        assertTrue(responseData.getHeaders().isEmpty());

        // Modify data.
        setStatus(responseContext, HttpStatus.S_402_PAYMENT_REQUIRED);
        return completedFutureWithError(t);
      }
    }).doThrow(exFromSecondFilter)
        .when(_filter)
        .onError(any(Throwable.class), eq(_filterRequestContext), any(FilterResponseContext.class));

    // invoke request filters so cursor is in correct place
    when(_filter.onRequest(any(FilterRequestContext.class))).thenReturn(CompletableFuture.completedFuture(null));
    _twoFilterChain.onRequest(_filterRequestContext, _filterResponseContextFactory);
    // Invoke.
    _twoFilterRestLiCallback.onError(exFromApp);

    // Verify.
    ArgumentCaptor<RestLiServiceException> wrappedExCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    verify(_responseHandler).buildExceptionResponseData(eq(_routingResult),
                                                        wrappedExCapture.capture(), anyMap(), anyList());
    verify(_responseHandler).buildPartialResponse(_routingResult, responseAppData);
    ArgumentCaptor<RestLiResponseException> partialRestResponseExceptionCaptor =
        ArgumentCaptor.forClass(RestLiResponseException.class);
    verify(_callback).onError(partialRestResponseExceptionCaptor.capture());
    verify(_restRequest).getHeaders();
    verifyNoMoreInteractions(_restRequest, _responseHandler, _callback);
    assertNotNull(responseAppData);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, responseAppData.getResponseEnvelope().getStatus());
    assertNull(responseAppData.getResponseEnvelope().getRecord());

    final RestLiServiceException restliEx1 = wrappedExCapture.getAllValues().get(0);
    assertEquals(exFromApp, restliEx1);

    RestLiResponseException restLiResponseException = partialRestResponseExceptionCaptor.getValue();
    assertEquals(restLiResponseException.getRestLiResponse(), partialResponse);
    assertTrue(restLiResponseException.getCause() instanceof RestLiServiceException);
    RestLiServiceException restliEx2 = (RestLiServiceException) restLiResponseException.getCause();
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, restliEx2.getStatus());
    assertEquals(exFromSecondFilter.getMessage(), restliEx2.getMessage());
    assertEquals(exFromSecondFilter, restliEx2.getCause());
  }

  private static class Foo extends RecordTemplate
  {
    private Foo(DataMap map)
    {
      super(map, null);
    }

    static Foo createFoo(String key, String value)
    {
      DataMap dataMap = new DataMap();
      dataMap.put(key, value);
      return new Foo(dataMap);
    }
  }

  /**
   * Helper method for generating completed futures that have errors.
   *
   * @param t The error.
   * @return A completed exceptionally future.
   */
  private static CompletableFuture<Void> completedFutureWithError(Throwable t)
  {
    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(t);
    return future;
  }

  /**
   * Helper method for generating error headers.
   *
   * @return Map of error headers.
   */
  private static Map<String, String> buildErrorHeaders()
  {
    Map<String, String> errorHeaders = Maps.newHashMap();
    errorHeaders.put("X-LinkedIn-Error-Response", "true");
    errorHeaders.put("X-RestLi-Protocol-Version", "1.0.0");
    return errorHeaders;
  }

  // Helper method to transition legacy test cases
  private static void setStatus(FilterResponseContext context, HttpStatus status)
  {
    if (context.getResponseData().getResponseEnvelope().isErrorResponse())
    {
      RestLiServiceException exception = new RestLiServiceException(status);
      context.getResponseData().getResponseEnvelope().setExceptionInternal(exception);
    }
    else
    {
      context.getResponseData().getResponseEnvelope().setStatus(status);
    }
  }
}
