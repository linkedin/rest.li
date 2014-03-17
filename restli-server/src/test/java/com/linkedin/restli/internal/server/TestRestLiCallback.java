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
package com.linkedin.restli.internal.server;

import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.HeaderUtil;
import com.linkedin.restli.internal.server.methods.response.PartialRestResponse;
import com.linkedin.restli.server.RequestExecutionCallback;
import com.linkedin.restli.server.RequestExecutionReport;
import com.linkedin.restli.server.RequestExecutionReportBuilder;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.FilterResponseContext;
import com.linkedin.restli.server.filter.ResponseFilter;

import java.util.Arrays;
import java.util.Map;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.beust.jcommander.internal.Maps;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

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
  private RestLiResponseHandler _responseHandler;
  @Mock
  private RequestExecutionCallback<RestResponse> _callback;

  private RestLiCallback<Object> _noFilterRestLiCallback;

  private RestLiCallback<Object> _twoFilterRestLiCallback;

  @Mock
  private FilterRequestContext _filterRequestContext;

  @Mock
  private ResponseFilter _filter;

  @BeforeTest
  protected void setUp() throws Exception
  {
    MockitoAnnotations.initMocks(this);
    _noFilterRestLiCallback =
        new RestLiCallback<Object>(_restRequest, _routingResult, _responseHandler, _callback, null, null);
    _twoFilterRestLiCallback =
        new RestLiCallback<Object>(_restRequest,
                                   _routingResult,
                                   _responseHandler,
                                   _callback,
                                   Arrays.asList(_filter, _filter),
                                   _filterRequestContext);
  }

  @AfterMethod
  protected void resetMocks()
  {
    reset(_filter, _filterRequestContext, _restRequest, _routingResult, _responseHandler, _callback);
  }

  @Test
  public void testOnSuccessNoFilters() throws Exception
  {
    String result = "foo";
    RequestExecutionReport executionReport = new RequestExecutionReportBuilder().build();
    PartialRestResponse partialResponse = new PartialRestResponse.Builder().build();
    RestResponse restResponse = new RestResponseBuilder().build();
    // Set up.
    when(_responseHandler.buildPartialResponse(_restRequest, _routingResult, result)).thenReturn(partialResponse);
    when(_responseHandler.buildResponse(_routingResult, partialResponse)).thenReturn(restResponse);

    // Invoke.
    _noFilterRestLiCallback.onSuccess(result, executionReport);

    // Verify.
    verify(_responseHandler).buildPartialResponse(_restRequest, _routingResult, result);
    verify(_responseHandler).buildResponse(_routingResult, partialResponse);
    verify(_callback).onSuccess(restResponse, executionReport);
    verifyZeroInteractions(_restRequest, _routingResult);
    verifyNoMoreInteractions(_responseHandler, _callback);
  }

  @Test
  public void testOnErrorRestExceptionNoFilters() throws Exception
  {
    RestException ex = new RestException(new RestResponseBuilder().build());
    RequestExecutionReport executionReport = new RequestExecutionReportBuilder().build();
    // Invoke.
    _noFilterRestLiCallback.onError(ex, executionReport);
    // Verify.
    verify(_callback).onError(ex, executionReport);
    verifyZeroInteractions(_responseHandler, _restRequest, _routingResult);
    verifyNoMoreInteractions(_callback);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testOnErrorRestLiServiceExceptionNoFilters() throws Exception
  {
    RestLiServiceException ex = new RestLiServiceException(HttpStatus.S_404_NOT_FOUND);
    RequestExecutionReport executionReport = new RequestExecutionReportBuilder().build();
    Map<String, String> inputHeaders = Maps.newHashMap();
    inputHeaders.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION,
                     AllProtocolVersions.BASELINE_PROTOCOL_VERSION.toString());

    Map<String, String> restExceptionHeaders = Maps.newHashMap();
    restExceptionHeaders.put("foo", "bar");

    @SuppressWarnings("rawtypes")
    ArgumentCaptor<Map> augErrorHeadersCapture = ArgumentCaptor.forClass(Map.class);
    PartialRestResponse partialResponse =
        new PartialRestResponse.Builder().status(ex.getStatus()).headers(restExceptionHeaders).build();
    RestException restException = new RestException(new RestResponseBuilder().build());
    // Set up.
    when(_restRequest.getHeaders()).thenReturn(inputHeaders);
    when(_responseHandler.buildErrorResponse(eq((RestRequest) null),
                                             eq((RoutingResult) null),
                                             eq(ex),
                                             augErrorHeadersCapture.capture())).thenReturn(partialResponse);
    when(_responseHandler.buildRestException(ex, partialResponse)).thenReturn(restException);

    // Invoke.
    _noFilterRestLiCallback.onError(ex, executionReport);

    // Verify.
    verify(_responseHandler).buildRestException(ex, partialResponse);
    verify(_responseHandler).buildErrorResponse(eq((RestRequest) null),
                                                eq((RoutingResult) null),
                                                eq(ex),
                                                augErrorHeadersCapture.capture());
    verify(_callback).onError(restException, executionReport);
    verify(_restRequest, times(2)).getHeaders();
    verifyZeroInteractions(_routingResult);
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
        { new RoutingException("Test routing exception", 404) } };
  }

  @SuppressWarnings("unchecked")
  @Test(dataProvider = "provideExceptions")
  public void testOnErrorOtherExceptionNoFilters(Exception ex) throws Exception
  {
    ArgumentCaptor<RestLiServiceException> exCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    RequestExecutionReport executionReport = new RequestExecutionReportBuilder().build();
    PartialRestResponse partialResponse = new PartialRestResponse.Builder().build();
    RestException restException = new RestException(new RestResponseBuilder().build());
    Map<String, String> inputHeaders = Maps.newHashMap();
    inputHeaders.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, "2.0.0");

    // Set up.
    when(_restRequest.getHeaders()).thenReturn(inputHeaders);
    when(_responseHandler.buildErrorResponse(eq((RestRequest) null),
                                             eq((RoutingResult) null),
                                             exCapture.capture(),
                                             anyMap())).thenReturn(partialResponse);
    when(_responseHandler.buildRestException(ex, partialResponse)).thenReturn(restException);

    // Invoke.
    _noFilterRestLiCallback.onError(ex, executionReport);

    // Verify.
    verify(_responseHandler).buildErrorResponse(eq((RestRequest) null),
                                                eq((RoutingResult) null),
                                                exCapture.capture(),
                                                anyMap());
    verify(_responseHandler).buildRestException(ex, partialResponse);
    verify(_callback).onError(restException, executionReport);
    verify(_restRequest, times(2)).getHeaders();
    verifyZeroInteractions(_routingResult);
    verifyNoMoreInteractions(_restRequest, _responseHandler, _callback);
    RestLiServiceException restliEx = exCapture.getValue();
    assertNotNull(restliEx);
    if (ex instanceof RoutingException)
    {
      assertEquals(HttpStatus.fromCode(((RoutingException) ex).getStatus()), restliEx.getStatus());
    }
    else
    {
      assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, restliEx.getStatus());
    }
    assertEquals(ex.getMessage(), restliEx.getMessage());
    assertEquals(ex, restliEx.getCause());
  }

  @Test
  public void testOnSuccessWithFiltersSuccessful() throws Exception
  {
    String result = "foo";
    RequestExecutionReport executionReport = new RequestExecutionReportBuilder().build();
    final RecordTemplate entityFromApp = Foo.createFoo("Key", "One");
    final Map<String, String> headersFromApp = Maps.newHashMap();
    headersFromApp.put("Key", "Input");
    final RecordTemplate entityFromFilters = Foo.createFoo("Key", "Two");
    final Map<String, String> headersFromFilters = Maps.newHashMap();
    headersFromFilters.put("Key", "Output");
    PartialRestResponse partialResponse =
        new PartialRestResponse.Builder().status(HttpStatus.S_200_OK)
                                         .headers(headersFromApp)
                                         .entity(entityFromApp)
                                         .build();

    // Setup.
    when(_responseHandler.buildPartialResponse(_restRequest, _routingResult, result)).thenReturn(partialResponse);
    // Mock the behavior of the first filter.
    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterResponseContext context = (FilterResponseContext) args[1];
        // Verify incoming data.
        assertEquals(HttpStatus.S_200_OK, context.getHttpStatus());
        assertEquals(headersFromApp, context.getResponseHeaders());
        assertEquals(entityFromApp, context.getResponseEntity());
        // Modify data in filter.
        context.setHttpStatus(HttpStatus.S_400_BAD_REQUEST);
        context.setResponseEntity(null);
        context.getResponseHeaders().clear();
        return null;
      }
    }).doAnswer(new Answer<Object>()
    // Mock the behavior of the first filter.
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterResponseContext context = (FilterResponseContext) args[1];
        // Verify incoming data.
        assertEquals(HttpStatus.S_400_BAD_REQUEST, context.getHttpStatus());
        assertTrue(context.getResponseHeaders().isEmpty());
        assertNull(context.getResponseEntity());
        // Modify data in filter.
        context.setHttpStatus(HttpStatus.S_403_FORBIDDEN);
        context.setResponseEntity(entityFromFilters);
        context.getResponseHeaders().putAll(headersFromFilters);
        return null;
      }
    }).when(_filter).onResponse(eq(_filterRequestContext), any(FilterResponseContext.class));

    ArgumentCaptor<PartialRestResponse> partialRespCapture = ArgumentCaptor.forClass(PartialRestResponse.class);
    RestResponse restResponse = new RestResponseBuilder().build();
    when(_responseHandler.buildResponse(eq(_routingResult), partialRespCapture.capture())).thenReturn(restResponse);

    // Invoke.
    _twoFilterRestLiCallback.onSuccess(result, executionReport);

    // Verify.
    PartialRestResponse updatedResp = partialRespCapture.getValue();
    assertNotNull(updatedResp);
    assertEquals(HttpStatus.S_403_FORBIDDEN, updatedResp.getStatus());
    assertEquals(entityFromFilters, updatedResp.getEntity());
    assertEquals(headersFromFilters, updatedResp.getHeaders());
    verify(_responseHandler).buildPartialResponse(_restRequest, _routingResult, result);
    verify(_responseHandler).buildResponse(_routingResult, updatedResp);
    verify(_callback).onSuccess(restResponse, executionReport);
    verifyZeroInteractions(_restRequest, _routingResult);
    verifyNoMoreInteractions(_responseHandler, _callback);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testOnSuccessWithFiltersExceptionFromFirstFilter() throws Exception
  {
    // App stuff.
    String result = "foo";
    RequestExecutionReport executionReport = new RequestExecutionReportBuilder().build();
    PartialRestResponse partialAppResponse = new PartialRestResponse.Builder().status(HttpStatus.S_200_OK).build();

    // Filter suff.
    ArgumentCaptor<RestLiServiceException> exFromFilterCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    final Map<String, String> headersFromFilter = Maps.newHashMap();
    headersFromFilter.put("Key", "Error from filter");
    PartialRestResponse partialFilterErrorResponse =
        new PartialRestResponse.Builder().status(HttpStatus.S_500_INTERNAL_SERVER_ERROR)
                                         .headers(headersFromFilter)
                                         .build();
    final Exception exFromFilter = new RuntimeException("Exception From Filter");

    // Common stuff.
    RestResponse restResponse = new RestResponseBuilder().build();
    ArgumentCaptor<PartialRestResponse> finalPartialRespCapture = ArgumentCaptor.forClass(PartialRestResponse.class);

    // Setup.
    when(_responseHandler.buildPartialResponse(_restRequest, _routingResult, result)).thenReturn(partialAppResponse);
    when(_restRequest.getHeaders()).thenReturn(null);
    when(_responseHandler.buildErrorResponse(eq((RestRequest) null),
                                             eq((RoutingResult) null),
                                             exFromFilterCapture.capture(),
                                             anyMap())).thenReturn(partialFilterErrorResponse);
    when(_responseHandler.buildResponse(eq(_routingResult), finalPartialRespCapture.capture())).thenReturn(restResponse);
    // Mock filter behavior.
    doThrow(exFromFilter).doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterResponseContext context = (FilterResponseContext) args[1];
        // The second filter should be invoked with details of the exception thrown by the first
        // filter.
        assertEquals(context.getHttpStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);
        assertNull(context.getResponseEntity());
        assertEquals(context.getResponseHeaders(), headersFromFilter);

        // Modify data.
        context.setHttpStatus(HttpStatus.S_402_PAYMENT_REQUIRED);
        // The second filter handles the exception thrown by the first filter (i.e.) does not throw
        // another exception.
        return null;
      }
    }).when(_filter).onResponse(eq(_filterRequestContext), any(FilterResponseContext.class));

    // Invoke.
    _twoFilterRestLiCallback.onSuccess(result, executionReport);

    // Verify.
    verify(_responseHandler).buildPartialResponse(_restRequest, _routingResult, result);
    verify(_responseHandler).buildErrorResponse(eq((RestRequest) null),
                                                eq((RoutingResult) null),
                                                exFromFilterCapture.capture(),
                                                anyMap());
    verify(_responseHandler).buildResponse(eq(_routingResult), finalPartialRespCapture.capture());
    verify(_callback).onSuccess(restResponse, executionReport);
    verify(_restRequest, times(2)).getHeaders();
    verifyZeroInteractions(_routingResult);
    verifyNoMoreInteractions(_responseHandler, _callback);
    RestLiServiceException restliEx = exFromFilterCapture.getValue();
    assertNotNull(restliEx);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, restliEx.getStatus());
    assertEquals(exFromFilter.getMessage(), restliEx.getMessage());
    assertEquals(exFromFilter, restliEx.getCause());
    PartialRestResponse finalPartialResp = finalPartialRespCapture.getValue();
    assertNotNull(finalPartialResp);
    assertEquals(HttpStatus.S_402_PAYMENT_REQUIRED, finalPartialResp.getStatus());
    assertEquals(finalPartialResp.getHeaders(), headersFromFilter);
    assertNull(finalPartialResp.getEntity());
  }


  @SuppressWarnings("unchecked")
  @Test
  public void testOnSuccessWithFiltersExceptionFromSecondFilter() throws Exception
  {
    // App stuff.
    String result = "foo";
    RequestExecutionReport executionReport = new RequestExecutionReportBuilder().build();
    PartialRestResponse partialAppResponse = new PartialRestResponse.Builder().status(HttpStatus.S_200_OK).build();

    // Filter suff.
    ArgumentCaptor<RestLiServiceException> exFromFilterCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    final Map<String, String> headersFromFilter = Maps.newHashMap();
    headersFromFilter.put("Key", "Error from filter");
    PartialRestResponse partialFilterErrorResponse =
        new PartialRestResponse.Builder().status(HttpStatus.S_500_INTERNAL_SERVER_ERROR)
                                         .headers(headersFromFilter)
                                         .build();
    final Exception exFromFilter = new RuntimeException("Excepiton From Filter");

    // Common stuff.
    RestException finalRestException = new RestException(new RestResponseBuilder().build());
    ArgumentCaptor<PartialRestResponse> finalPartialRespCapture = ArgumentCaptor.forClass(PartialRestResponse.class);

    // Setup.
    when(_responseHandler.buildPartialResponse(_restRequest, _routingResult, result)).thenReturn(partialAppResponse);
    when(_restRequest.getHeaders()).thenReturn(null);
    when(_responseHandler.buildErrorResponse(eq((RestRequest) null),
                                             eq((RoutingResult) null),
                                             exFromFilterCapture.capture(),
                                             anyMap())).thenReturn(partialFilterErrorResponse);
    when(_responseHandler.buildRestException(eq(exFromFilter), finalPartialRespCapture.capture())).thenReturn(finalRestException);
    // Mock filter behavior.
    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterResponseContext context = (FilterResponseContext) args[1];
        // The second filter should be invoked with details of the exception thrown by the first
        // filter. Verify incoming data.
        assertEquals(context.getHttpStatus(), HttpStatus.S_200_OK);
        assertNull(context.getResponseEntity());
        assertTrue(context.getResponseHeaders().isEmpty());
        // Modify data.
        context.setHttpStatus(HttpStatus.S_402_PAYMENT_REQUIRED);
        return null;
      }
    }).doThrow(exFromFilter).when(_filter).onResponse(eq(_filterRequestContext), any(FilterResponseContext.class));

    // Invoke.
    _twoFilterRestLiCallback.onSuccess(result, executionReport);

    // Verify.
    verify(_responseHandler).buildPartialResponse(_restRequest, _routingResult, result);
    verify(_responseHandler).buildErrorResponse(eq((RestRequest) null),
                                                eq((RoutingResult) null),
                                                exFromFilterCapture.capture(),
                                                anyMap());
    verify(_responseHandler).buildRestException(eq(exFromFilter), finalPartialRespCapture.capture());
    verify(_callback).onError(finalRestException, executionReport);
    verify(_restRequest, times(2)).getHeaders();
    verifyZeroInteractions(_routingResult);
    verifyNoMoreInteractions(_responseHandler, _callback);
    RestLiServiceException restliEx = exFromFilterCapture.getValue();
    assertNotNull(restliEx);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, restliEx.getStatus());
    assertEquals(exFromFilter.getMessage(), restliEx.getMessage());
    assertEquals(exFromFilter, restliEx.getCause());
    PartialRestResponse finalParialResp = finalPartialRespCapture.getValue();
    assertNotNull(finalParialResp);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, finalParialResp.getStatus());
    assertEquals(finalParialResp.getHeaders(), headersFromFilter);
    assertNull(finalParialResp.getEntity());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testOnErrorWithFiltersSuccessful() throws Exception
  {
    Exception exFromApp = new RuntimeException("Runtime exception from app");
    RequestExecutionReport executionReport = new RequestExecutionReportBuilder().build();
    final Map<String, String> headersFromApp = Maps.newHashMap();
    headersFromApp.put("Key", "Input");
    final RecordTemplate entityFromFilter = Foo.createFoo("Key", "Two");
    final Map<String, String> headersFromFilter = Maps.newHashMap();
    headersFromFilter.put("Key", "Output");

    PartialRestResponse partialResponse =
        new PartialRestResponse.Builder().status(HttpStatus.S_404_NOT_FOUND).headers(headersFromApp).build();
    ArgumentCaptor<RestLiServiceException> exCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    when(_responseHandler.buildErrorResponse(eq((RestRequest) null),
                                             eq((RoutingResult) null),
                                             exCapture.capture(),
                                             anyMap())).thenReturn(partialResponse);
    // Mock the behavior of the first filter.
    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterResponseContext context = (FilterResponseContext) args[1];
        // Verify incoming data.
        assertEquals(HttpStatus.S_404_NOT_FOUND, context.getHttpStatus());
        assertEquals(headersFromApp, context.getResponseHeaders());
        assertNull(context.getResponseEntity());
        // Modify data in filter.
        context.setHttpStatus(HttpStatus.S_400_BAD_REQUEST);
        context.getResponseHeaders().clear();
        return null;
      }
    }).doAnswer(new Answer<Object>()
    // Mock the behavior of the first filter.
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterResponseContext context = (FilterResponseContext) args[1];
        // Verify incoming data.
        assertEquals(HttpStatus.S_400_BAD_REQUEST, context.getHttpStatus());
        assertTrue(context.getResponseHeaders().isEmpty());
        assertNull(context.getResponseEntity());
        // Modify data in filter.
        context.setHttpStatus(HttpStatus.S_403_FORBIDDEN);
        context.setResponseEntity(entityFromFilter);
        context.getResponseHeaders().putAll(headersFromFilter);
        return null;
      }
    }).when(_filter).onResponse(eq(_filterRequestContext), any(FilterResponseContext.class));
    RestException restException = new RestException(new RestResponseBuilder().build());
    ArgumentCaptor<PartialRestResponse> partialRespCapture = ArgumentCaptor.forClass(PartialRestResponse.class);
    when(_responseHandler.buildRestException(eq(exFromApp), partialRespCapture.capture())).thenReturn(restException);

    // Invoke.
    _twoFilterRestLiCallback.onError(exFromApp, executionReport);

    // Verify.
    PartialRestResponse updatedResp = partialRespCapture.getValue();
    assertNotNull(updatedResp);
    assertEquals(HttpStatus.S_403_FORBIDDEN, updatedResp.getStatus());
    assertEquals(entityFromFilter, updatedResp.getEntity());
    assertEquals(headersFromFilter, updatedResp.getHeaders());
    verify(_responseHandler).buildErrorResponse(eq((RestRequest) null),
                                                eq((RoutingResult) null),
                                                exCapture.capture(),
                                                anyMap());
    verify(_responseHandler).buildRestException(exFromApp, updatedResp);
    verify(_callback).onError(restException, executionReport);
    verify(_restRequest, times(2)).getHeaders();
    verifyZeroInteractions(_routingResult);
    verifyNoMoreInteractions(_restRequest, _responseHandler, _callback);
    RestLiServiceException restliEx = exCapture.getValue();
    assertNotNull(restliEx);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, restliEx.getStatus());
    assertEquals(exFromApp.getMessage(), restliEx.getMessage());
    assertEquals(exFromApp, restliEx.getCause());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testOnErrorWithFiltersExceptionFromFirstFilter() throws Exception
  {
    // App stuff.
    RestLiServiceException exFromApp = new RestLiServiceException(HttpStatus.S_404_NOT_FOUND, "App failure");
    RequestExecutionReport executionReport = new RequestExecutionReportBuilder().build();
    PartialRestResponse partialAppErrorResponse =
        new PartialRestResponse.Builder().status(HttpStatus.S_404_NOT_FOUND).build();

    // Filter stuff.
    final Exception exFromFirstFilter = new RuntimeException("Runtime exception from first filter");
    PartialRestResponse partialFilterErrorResponse =
        new PartialRestResponse.Builder().status(HttpStatus.S_500_INTERNAL_SERVER_ERROR).build();

    ArgumentCaptor<RestLiServiceException> wrappedExCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    RestException restException = new RestException(new RestResponseBuilder().build());

    ArgumentCaptor<PartialRestResponse> finalPartialRespCapture = ArgumentCaptor.forClass(PartialRestResponse.class);

    // Setup.
    when(_responseHandler.buildErrorResponse(eq((RestRequest) null),
                                             eq((RoutingResult) null),
                                             wrappedExCapture.capture(),
                                             anyMap())).thenReturn(partialAppErrorResponse)
                                                       .thenReturn(partialFilterErrorResponse);
    when(_restRequest.getHeaders()).thenReturn(null);
    when(_responseHandler.buildRestException(eq(exFromApp), finalPartialRespCapture.capture())).thenReturn(restException);

    // Mock filter behavior.
    doThrow(exFromFirstFilter).doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterResponseContext context = (FilterResponseContext) args[1];
        // The second filter should be invoked with details of the exception thrown by the first
        // filter. Verify incoming data.
        assertEquals(context.getHttpStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);
        assertNull(context.getResponseEntity());
        assertTrue(context.getResponseHeaders().isEmpty());

        // Modify data.
        context.setHttpStatus(HttpStatus.S_402_PAYMENT_REQUIRED);
        // The second filter handles the exception thrown by the first filter (i.e.) does not throw
        // another exception.
        return null;
      }
    }).when(_filter).onResponse(eq(_filterRequestContext), any(FilterResponseContext.class));

    // Invoke.
    _twoFilterRestLiCallback.onError(exFromApp, executionReport);

    // Verify.
    verify(_responseHandler, times(2)).buildErrorResponse(eq((RestRequest) null),
                                                          eq((RoutingResult) null),
                                                          wrappedExCapture.capture(),
                                                          anyMap());
    verify(_responseHandler).buildRestException(eq(exFromApp), finalPartialRespCapture.capture());
    verify(_callback).onError(restException, executionReport);
    verify(_restRequest, times(4)).getHeaders();
    verifyZeroInteractions(_routingResult);
    verifyNoMoreInteractions(_restRequest, _responseHandler, _callback);
    PartialRestResponse finalParialResp = finalPartialRespCapture.getValue();
    assertNotNull(finalParialResp);
    assertEquals(HttpStatus.S_402_PAYMENT_REQUIRED, finalParialResp.getStatus());
    assertTrue(finalParialResp.getHeaders().isEmpty());
    assertNull(finalParialResp.getEntity());
    RestLiServiceException restliEx = wrappedExCapture.getAllValues().get(0);
    assertNotNull(restliEx);
    assertEquals(exFromApp.getStatus(), restliEx.getStatus());
    assertEquals(exFromApp.getMessage(), restliEx.getMessage());
    restliEx = wrappedExCapture.getAllValues().get(1);
    assertNotNull(restliEx);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, restliEx.getStatus());
    assertEquals(exFromFirstFilter.getMessage(), restliEx.getMessage());
    assertEquals(exFromFirstFilter, restliEx.getCause());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testOnErrorWithFiltersExceptionFromSecondFilter() throws Exception
  {
    // App stuff.
    RestLiServiceException exFromApp = new RestLiServiceException(HttpStatus.S_404_NOT_FOUND, "App failure");
    RequestExecutionReport executionReport = new RequestExecutionReportBuilder().build();
    PartialRestResponse partialAppErrorResponse =
        new PartialRestResponse.Builder().status(HttpStatus.S_404_NOT_FOUND).build();

    // Filter stuff.
    final Exception exFromFirstFilter = new RuntimeException("Runtime exception from first filter");
    PartialRestResponse partialFilterErrorResponse =
        new PartialRestResponse.Builder().status(HttpStatus.S_500_INTERNAL_SERVER_ERROR).build();

    ArgumentCaptor<RestLiServiceException> wrappedExCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    RestException restException = new RestException(new RestResponseBuilder().build());

    ArgumentCaptor<PartialRestResponse> finalPartialRespCapture = ArgumentCaptor.forClass(PartialRestResponse.class);

    // Setup.
    when(_responseHandler.buildErrorResponse(eq((RestRequest) null),
                                             eq((RoutingResult) null),
                                             wrappedExCapture.capture(),
                                             anyMap())).thenReturn(partialAppErrorResponse)
                                                       .thenReturn(partialFilterErrorResponse);
    when(_restRequest.getHeaders()).thenReturn(null);
    when(_responseHandler.buildRestException(eq(exFromFirstFilter), finalPartialRespCapture.capture())).thenReturn(restException);

    // Mock filter behavior.
    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterResponseContext context = (FilterResponseContext) args[1];
        assertEquals(context.getHttpStatus(), HttpStatus.S_404_NOT_FOUND);
        assertNull(context.getResponseEntity());
        assertTrue(context.getResponseHeaders().isEmpty());

        // Modify data.
        context.setHttpStatus(HttpStatus.S_402_PAYMENT_REQUIRED);
        return null;
      }
    }).doThrow(exFromFirstFilter).when(_filter).onResponse(eq(_filterRequestContext), any(FilterResponseContext.class));

    // Invoke.
    _twoFilterRestLiCallback.onError(exFromApp, executionReport);

    // Verify.
    verify(_responseHandler, times(2)).buildErrorResponse(eq((RestRequest) null),
                                                          eq((RoutingResult) null),
                                                          wrappedExCapture.capture(),
                                                          anyMap());
    verify(_responseHandler).buildRestException(eq(exFromFirstFilter), finalPartialRespCapture.capture());
    verify(_callback).onError(restException, executionReport);
    verify(_restRequest, times(4)).getHeaders();
    verifyZeroInteractions(_routingResult);
    verifyNoMoreInteractions(_restRequest, _responseHandler, _callback);
    PartialRestResponse finalParialResp = finalPartialRespCapture.getValue();
    assertNotNull(finalParialResp);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, finalParialResp.getStatus());
    assertTrue(finalParialResp.getHeaders().isEmpty());
    assertNull(finalParialResp.getEntity());
    RestLiServiceException restliEx = wrappedExCapture.getAllValues().get(0);
    assertNotNull(restliEx);
    assertEquals(exFromApp.getStatus(), restliEx.getStatus());
    assertEquals(exFromApp.getMessage(), restliEx.getMessage());
    restliEx = wrappedExCapture.getAllValues().get(1);
    assertNotNull(restliEx);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, restliEx.getStatus());
    assertEquals(exFromFirstFilter.getMessage(), restliEx.getMessage());
    assertEquals(exFromFirstFilter, restliEx.getCause());
  }

  @Test
  public void testFilterResponseContextAdapter()
  {
    DataMap dataMap = new DataMap();
    dataMap.put("foo", "bar");
    Map<String, String> headers = Maps.newHashMap();
    headers.put("x", "y");
    RecordTemplate entity = new Foo(dataMap);
    PartialRestResponse partialResponse =
        new PartialRestResponse.Builder().status(HttpStatus.S_200_OK).headers(headers).entity(entity).build();
    FilterResponseContext context = new RestLiCallback.FilterResponseContextAdapter(partialResponse);
    assertEquals(headers, context.getResponseHeaders());
    assertEquals(entity, context.getResponseEntity());
    assertEquals(HttpStatus.S_200_OK, context.getHttpStatus());

    context.setHttpStatus(HttpStatus.S_404_NOT_FOUND);
    context.setResponseEntity(null);
    assertNull(context.getResponseEntity());
    assertEquals(HttpStatus.S_404_NOT_FOUND, context.getHttpStatus());
  }

  private static class Foo extends RecordTemplate
  {
    private Foo(DataMap map)
    {
      super(map, null);
    }

    public static Foo createFoo(String key, String value)
    {
      DataMap dataMap = new DataMap();
      dataMap.put(key, value);
      return new Foo(dataMap);
    }
  }
}
