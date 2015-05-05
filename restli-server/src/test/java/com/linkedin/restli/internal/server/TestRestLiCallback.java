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
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.HeaderUtil;
import com.linkedin.restli.internal.server.filter.FilterResponseContextInternal;
import com.linkedin.restli.internal.server.methods.response.PartialRestResponse;
import com.linkedin.restli.internal.server.response.BatchResponseEnvelope;
import com.linkedin.restli.internal.server.response.CreateCollectionResponseEnvelope;
import com.linkedin.restli.internal.server.response.CollectionResponseEnvelope;
import com.linkedin.restli.internal.server.response.EmptyResponseEnvelope;
import com.linkedin.restli.internal.server.response.RecordResponseEnvelope;
import com.linkedin.restli.server.RequestExecutionCallback;
import com.linkedin.restli.server.RequestExecutionReport;
import com.linkedin.restli.server.RequestExecutionReportBuilder;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.FilterResponseContext;
import com.linkedin.restli.server.filter.ResponseFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

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
        new RestLiCallback<Object>(_restRequest, _routingResult, _responseHandler, _callback, Arrays.asList(_filter,
                                                                                                            _filter),
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
    RestLiResponseEnvelope responseData = new RecordResponseEnvelope(HttpStatus.S_200_OK, null, Collections.<String, String>emptyMap());
    PartialRestResponse partialResponse = new PartialRestResponse.Builder().build();
    RestResponse restResponse = new RestResponseBuilder().build();
    // Set up.
    when(_responseHandler.buildRestLiResponseData(_restRequest, _routingResult, result)).thenReturn(responseData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseData)).thenReturn(partialResponse);
    when(_responseHandler.buildResponse(_routingResult, partialResponse)).thenReturn(restResponse);

    // Invoke.
    _noFilterRestLiCallback.onSuccess(result, executionReport);

    // Verify.
    verify(_responseHandler).buildPartialResponse(_routingResult, responseData);
    verify(_responseHandler).buildRestLiResponseData(_restRequest, _routingResult, result);
    verify(_responseHandler).buildResponse(_routingResult, partialResponse);
    verify(_callback).onSuccess(restResponse, executionReport);
    verifyZeroInteractions(_restRequest, _routingResult);
    verifyNoMoreInteractions(_responseHandler, _callback);
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
    RestLiResponseEnvelope responseData = new RecordResponseEnvelope(ex, restExceptionHeaders);

    PartialRestResponse partialResponse = new PartialRestResponse.Builder().build();
    RestException restException = new RestException(new RestResponseBuilder().build());
    // Set up.
    when(_restRequest.getHeaders()).thenReturn(inputHeaders);
    when(
         _responseHandler.buildExceptionResponseData(eq(_restRequest), eq(_routingResult), eq(ex),
             augErrorHeadersCapture.capture())).thenReturn(responseData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseData)).thenReturn(partialResponse);
    when(_responseHandler.buildRestException(ex, partialResponse)).thenReturn(restException);

    // Invoke.
    _noFilterRestLiCallback.onError(ex, executionReport);

    // Verify.
    verify(_responseHandler).buildRestException(ex, partialResponse);
    verify(_responseHandler).buildExceptionResponseData(eq(_restRequest), eq(_routingResult), eq(ex),
        augErrorHeadersCapture.capture());
    verify(_responseHandler).buildPartialResponse(_routingResult, responseData);
    verify(_callback).onError(restException, executionReport);
    verify(_restRequest, times(1)).getHeaders();
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
    RequestExecutionReport executionReport = new RequestExecutionReportBuilder().build();
    PartialRestResponse partialResponse = new PartialRestResponse.Builder().build();
    RestLiResponseEnvelope responseData = new RecordResponseEnvelope(HttpStatus.S_200_OK, null, Collections.<String, String>emptyMap());
    RestException restException = new RestException(new RestResponseBuilder().build());
    Map<String, String> inputHeaders = Maps.newHashMap();
    inputHeaders.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, "2.0.0");

    // Set up.
    when(_restRequest.getHeaders()).thenReturn(inputHeaders);
    when(
         _responseHandler.buildExceptionResponseData(eq(_restRequest), eq(_routingResult), exCapture.capture(),
             anyMap())).thenReturn(responseData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseData)).thenReturn(partialResponse);
    when(_responseHandler.buildRestException(ex, partialResponse)).thenReturn(restException);

    // Invoke.
    _noFilterRestLiCallback.onError(ex, executionReport);

    // Verify.
    verify(_responseHandler).buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
        exCapture.capture(), anyMap());
    verify(_responseHandler).buildPartialResponse(_routingResult, responseData);
    verify(_responseHandler).buildRestException(ex, partialResponse);
    verify(_callback).onError(restException, executionReport);
    verify(_restRequest, times(1)).getHeaders();
    verifyZeroInteractions(_routingResult);
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

  @Test
  public void testOnSuccessWithFiltersSuccessful() throws Exception
  {
    String result = "foo";
    RequestExecutionReport executionReport = new RequestExecutionReportBuilder().build();
    final RecordTemplate entityFromApp = Foo.createFoo("Key", "One");
    final Map<String, String> headersFromApp = Maps.newHashMap();
    headersFromApp.put("Key", "Input");
    final RecordTemplate entityFromFilter1 = Foo.createFoo("Key", "Two");
    final RecordTemplate entityFromFilter2 = Foo.createFoo("Key", "Three");
    final Map<String, String> headersFromFilters = Maps.newHashMap();
    headersFromFilters.put("Key", "Output");
    RestLiResponseEnvelope appResponseData = new RecordResponseEnvelope(HttpStatus.S_200_OK, entityFromApp, headersFromApp);
    PartialRestResponse partialResponse = new PartialRestResponse.Builder().build();

    // Setup.
    when(_responseHandler.buildRestLiResponseData(_restRequest, _routingResult, result)).thenReturn(appResponseData);
    when(_responseHandler.buildPartialResponse(_routingResult, appResponseData)).thenReturn(partialResponse);
    // Mock the behavior of the first filter.
    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterResponseContext context = (FilterResponseContext) args[1];
        // Verify incoming data.
        assertEquals(HttpStatus.S_200_OK, context.getResponseData().getStatus());
        assertEquals(headersFromApp, context.getResponseData().getHeaders());
        assertEquals(entityFromApp, context.getResponseData().getRecordResponseEnvelope().getRecord());
        // Modify data in filter.
        setStatus(context, HttpStatus.S_400_BAD_REQUEST);
        context.getResponseData().getRecordResponseEnvelope().setRecord(entityFromFilter1,
            HttpStatus.S_400_BAD_REQUEST);
        context.getResponseData().getHeaders().clear();
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
        assertEquals(HttpStatus.S_400_BAD_REQUEST, context.getResponseData().getStatus());
        assertTrue(context.getResponseData().getHeaders().isEmpty());
        assertEquals(context.getResponseData().getRecordResponseEnvelope().getRecord(), entityFromFilter1);
        // Modify data in filter.
        setStatus(context, HttpStatus.S_403_FORBIDDEN);
        context.getResponseData().getRecordResponseEnvelope().setRecord(entityFromFilter2, HttpStatus.S_403_FORBIDDEN);
        context.getResponseData().getHeaders().putAll(headersFromFilters);
        return null;
      }
    }).when(_filter).onResponse(eq(_filterRequestContext), any(FilterResponseContext.class));

    RestResponse restResponse = new RestResponseBuilder().build();
    when(_responseHandler.buildResponse(_routingResult, partialResponse)).thenReturn(restResponse);

    // Invoke.
    _twoFilterRestLiCallback.onSuccess(result, executionReport);

    // Verify.
    assertNotNull(appResponseData);
    assertEquals(HttpStatus.S_403_FORBIDDEN, appResponseData.getStatus());
    assertEquals(entityFromFilter2, appResponseData.getRecordResponseEnvelope().getRecord());
    assertEquals(headersFromFilters, appResponseData.getHeaders());
    verify(_responseHandler).buildRestLiResponseData(_restRequest, _routingResult, result);
    verify(_responseHandler).buildPartialResponse(_routingResult, appResponseData);
    verify(_responseHandler).buildResponse(_routingResult, partialResponse);
    verify(_callback).onSuccess(restResponse, executionReport);
    verifyZeroInteractions(_restRequest, _routingResult);
    verifyNoMoreInteractions(_responseHandler, _callback);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testOnSuccessWithFiltersExceptionFromFirstFilterSecondFilterHandlesEx() throws Exception
  {
    // App stuff.
    final RecordTemplate entityFromApp = Foo.createFoo("Key", "One");
    RequestExecutionReport executionReport = new RequestExecutionReportBuilder().build();
    RestLiResponseEnvelope appResponseData = new RecordResponseEnvelope(HttpStatus.S_200_OK, entityFromApp, Collections.<String, String>emptyMap());

    // Filter suff.
    ArgumentCaptor<RestLiServiceException> exFromFilterCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    final Map<String, String> headersFromFilter = Maps.newHashMap();
    headersFromFilter.put("Key", "Error from filter");
    final RestLiServiceException exceptionFromFilter = new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR);
    RestLiResponseEnvelope responseErrorData = new RecordResponseEnvelope(exceptionFromFilter, headersFromFilter);
    final RecordTemplate entityFromFilter = Foo.createFoo("Key", "Two");
    PartialRestResponse partialFilterErrorResponse = new PartialRestResponse.Builder().build();
    final Exception exFromFilter = new RuntimeException("Exception From Filter");
    // Common stuff.
    RestResponse restResponse = new RestResponseBuilder().build();
    // Setup.
    when(_responseHandler.buildRestLiResponseData(_restRequest, _routingResult, entityFromApp)).thenReturn(appResponseData);
    when(_restRequest.getHeaders()).thenReturn(null);
    when(
         _responseHandler.buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
             exFromFilterCapture.capture(), anyMap())).thenReturn(responseErrorData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseErrorData)).thenReturn(partialFilterErrorResponse);

    when(_responseHandler.buildResponse(_routingResult, partialFilterErrorResponse)).thenReturn(restResponse);
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
        assertEquals(context.getResponseData().getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);
        assertNull(context.getResponseData().getRecordResponseEnvelope().getRecord());
        assertEquals(context.getResponseData().getHeaders(), headersFromFilter);
        assertEquals(context.getResponseData().getServiceException(), exceptionFromFilter);

        // Modify data.
        setStatus(context, HttpStatus.S_402_PAYMENT_REQUIRED);
        // The second filter handles the exception thrown by the first filter (i.e.) sets an entity
        // response in the response data.
        context.getResponseData().getRecordResponseEnvelope().setRecord(entityFromFilter,
            HttpStatus.S_402_PAYMENT_REQUIRED);
        return null;
      }
    }).when(_filter).onResponse(eq(_filterRequestContext), any(FilterResponseContext.class));

    // Invoke.
    _twoFilterRestLiCallback.onSuccess(entityFromApp, executionReport);

    // Verify.
    verify(_responseHandler).buildRestLiResponseData(_restRequest, _routingResult, entityFromApp);
    verify(_responseHandler).buildPartialResponse(_routingResult, responseErrorData);
    verify(_responseHandler).buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
        exFromFilterCapture.capture(), anyMap());
    verify(_responseHandler).buildPartialResponse(_routingResult, responseErrorData);
    verify(_responseHandler).buildResponse(_routingResult, partialFilterErrorResponse);
    verify(_callback).onSuccess(restResponse, executionReport);
    verify(_restRequest, times(1)).getHeaders();
    verifyZeroInteractions(_routingResult);
    verifyNoMoreInteractions(_responseHandler, _callback);
    assertFalse(responseErrorData.isErrorResponse());
    assertEquals(responseErrorData.getRecordResponseEnvelope().getRecord(), entityFromFilter);
    RestLiServiceException restliEx = exFromFilterCapture.getValue();
    assertNotNull(restliEx);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, restliEx.getStatus());
    assertEquals(exFromFilter.getMessage(), restliEx.getMessage());
    assertEquals(exFromFilter, restliEx.getCause());
    assertNotNull(responseErrorData);
    assertEquals(HttpStatus.S_402_PAYMENT_REQUIRED, responseErrorData.getStatus());
    assertEquals(responseErrorData.getHeaders(), headersFromFilter);
    assertNull(responseErrorData.getServiceException());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testOnSuccessWithFiltersExceptionFromFirstFilterSecondFilterDoesNotHandleEx() throws Exception
  {
    // App stuff.
    final RecordTemplate entityFromApp = Foo.createFoo("Key", "Two");
    RequestExecutionReport executionReport = new RequestExecutionReportBuilder().build();
    RestLiResponseEnvelope appResponseData = new RecordResponseEnvelope(HttpStatus.S_200_OK, entityFromApp, Collections.<String, String>emptyMap());

    // Filter suff.
    ArgumentCaptor<RestLiServiceException> exFromFilterCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    final Map<String, String> headersFromFilter = Maps.newHashMap();
    headersFromFilter.put("Key", "Error from filter");
    RestLiServiceException exceptionFromFilter = new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR);
    RestLiResponseEnvelope responseErrorData = new RecordResponseEnvelope(exceptionFromFilter, headersFromFilter);
    PartialRestResponse partialFilterErrorResponse = new PartialRestResponse.Builder().build();
    final Exception exFromFilter = new RuntimeException("Exception From Filter");

    // Common stuff.
    RestException finalRestException = new RestException(new RestResponseBuilder().build());
    // Setup.
    when(_responseHandler.buildRestLiResponseData(_restRequest, _routingResult, entityFromApp)).thenReturn(appResponseData);
    when(_restRequest.getHeaders()).thenReturn(null);
    when(
         _responseHandler.buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
             exFromFilterCapture.capture(), anyMap())).thenReturn(responseErrorData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseErrorData)).thenReturn(partialFilterErrorResponse);
    when(_responseHandler.buildRestException(exFromFilter, partialFilterErrorResponse)).thenReturn(finalRestException);    // Mock filter behavior.
    doThrow(exFromFilter).doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterResponseContext context = (FilterResponseContext) args[1];
        // The second filter should be invoked with details of the exception thrown by the first
        // filter.
        assertEquals(context.getResponseData().getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);
        assertNull(context.getResponseData().getRecordResponseEnvelope().getRecord());
        assertEquals(context.getResponseData().getHeaders(), headersFromFilter);

        // Modify data.
        setStatus(context, HttpStatus.S_402_PAYMENT_REQUIRED);
        // The second filter handles the exception thrown by the first filter (i.e.) does not throw
        // another exception.
        return null;
      }
    }).when(_filter).onResponse(eq(_filterRequestContext), any(FilterResponseContext.class));

    // Invoke.
    _twoFilterRestLiCallback.onSuccess(entityFromApp, executionReport);

    // Verify.
    verify(_responseHandler).buildRestLiResponseData(_restRequest, _routingResult, entityFromApp);
    verify(_responseHandler).buildPartialResponse(_routingResult, responseErrorData);
    verify(_responseHandler).buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
        exFromFilterCapture.capture(), anyMap());
    verify(_responseHandler).buildPartialResponse(_routingResult, responseErrorData);
    verify(_responseHandler).buildRestException(exFromFilter, partialFilterErrorResponse);
    verify(_callback).onError(finalRestException, executionReport);
    verify(_restRequest, times(1)).getHeaders();
    verifyZeroInteractions(_routingResult);
    verifyNoMoreInteractions(_responseHandler, _callback);
    RestLiServiceException restliEx = exFromFilterCapture.getValue();
    assertNotNull(restliEx);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, restliEx.getStatus());
    assertEquals(exFromFilter.getMessage(), restliEx.getMessage());
    assertEquals(exFromFilter, restliEx.getCause());
    assertNotNull(responseErrorData);
    assertEquals(HttpStatus.S_402_PAYMENT_REQUIRED, responseErrorData.getStatus());
    assertEquals(responseErrorData.getHeaders(), headersFromFilter);
    assertNull(responseErrorData.getRecordResponseEnvelope().getRecord());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testOnSuccessWithFiltersExceptionFromSecondFilter() throws Exception
  {
    // App stuff.
    String result = "foo";
    RequestExecutionReport executionReport = new RequestExecutionReportBuilder().build();
    RestLiResponseEnvelope appResponseData = new RecordResponseEnvelope(HttpStatus.S_200_OK, null, Collections.<String, String>emptyMap());

    // Filter suff.
    ArgumentCaptor<RestLiServiceException> exFromFilterCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    final Map<String, String> headersFromFilter = Maps.newHashMap();
    headersFromFilter.put("Key", "Error from filter");
    RestLiResponseEnvelope filterResponseData = new RecordResponseEnvelope(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR), headersFromFilter);
    PartialRestResponse partialFilterErrorResponse = new PartialRestResponse.Builder().build();
    final Exception exFromFilter = new RuntimeException("Excepiton From Filter");

    // Common stuff.
    RestException finalRestException = new RestException(new RestResponseBuilder().build());
    // Setup.
    when(_responseHandler.buildRestLiResponseData(_restRequest, _routingResult, result)).thenReturn(appResponseData);
    when(_restRequest.getHeaders()).thenReturn(null);
    when(
         _responseHandler.buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
             exFromFilterCapture.capture(), anyMap())).thenReturn(filterResponseData);
    when(_responseHandler.buildPartialResponse(_routingResult, filterResponseData)).thenReturn(partialFilterErrorResponse);
    when(_responseHandler.buildRestException(exFromFilter, partialFilterErrorResponse)).thenReturn(finalRestException);
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
        assertEquals(context.getResponseData().getStatus(), HttpStatus.S_200_OK);
        assertNull(context.getResponseData().getRecordResponseEnvelope().getRecord());
        assertTrue(context.getResponseData().getHeaders().isEmpty());
        // Modify data.
        setStatus(context, HttpStatus.S_402_PAYMENT_REQUIRED);
        return null;
      }
    }).doThrow(exFromFilter).when(_filter).onResponse(eq(_filterRequestContext), any(FilterResponseContext.class));

    // Invoke.
    _twoFilterRestLiCallback.onSuccess(result, executionReport);

    // Verify.
    verify(_responseHandler).buildPartialResponse(_routingResult, filterResponseData);
    verify(_responseHandler).buildRestLiResponseData(_restRequest, _routingResult, result);
    verify(_responseHandler).buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
        exFromFilterCapture.capture(), anyMap());
    verify(_responseHandler).buildPartialResponse(_routingResult, filterResponseData);
    verify(_responseHandler).buildRestException(exFromFilter, partialFilterErrorResponse);
    verify(_callback).onError(finalRestException, executionReport);
    verify(_restRequest, times(1)).getHeaders();
    verifyZeroInteractions(_routingResult);
    verifyNoMoreInteractions(_responseHandler, _callback);
    RestLiServiceException restliEx = exFromFilterCapture.getValue();
    assertNotNull(restliEx);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, restliEx.getStatus());
    assertEquals(exFromFilter.getMessage(), restliEx.getMessage());
    assertEquals(exFromFilter, restliEx.getCause());
    assertNotNull(filterResponseData);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, filterResponseData.getStatus());
    assertEquals(filterResponseData.getHeaders(), headersFromFilter);
    assertNull(filterResponseData.getRecordResponseEnvelope().getRecord());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testOnErrorWithFiltersNotHandlingAppEx() throws Exception
  {
    Exception exFromApp = new RuntimeException("Runtime exception from app");
    RestLiServiceException appException = new RestLiServiceException(HttpStatus.S_404_NOT_FOUND);
    RequestExecutionReport executionReport = new RequestExecutionReportBuilder().build();
    final Map<String, String> headersFromApp = Maps.newHashMap();
    headersFromApp.put("Key", "Input");
    final Map<String, String> headersFromFilter = Maps.newHashMap();
    headersFromFilter.put("Key", "Output");

    RestLiResponseEnvelope responseData = new RecordResponseEnvelope(appException, headersFromApp);
    PartialRestResponse partialResponse = new PartialRestResponse.Builder().build();
    ArgumentCaptor<RestLiServiceException> exCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    when(
         _responseHandler.buildExceptionResponseData(eq(_restRequest), eq(_routingResult), exCapture.capture(),
             anyMap())).thenReturn(responseData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseData)).thenReturn(partialResponse);

    // Mock the behavior of the first filter.
    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterResponseContext context = (FilterResponseContext) args[1];
        // Verify incoming data.
        assertEquals(HttpStatus.S_404_NOT_FOUND, context.getResponseData().getStatus());
        assertEquals(headersFromApp, context.getResponseData().getHeaders());
        assertNull(context.getResponseData().getRecordResponseEnvelope().getRecord());
        // Modify data in filter.
        setStatus(context, HttpStatus.S_400_BAD_REQUEST);
        context.getResponseData().getHeaders().clear();
        return null;
      }
    }).doAnswer(new Answer<Object>()
    // Mock the behavior of the second filter.
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterResponseContext context = (FilterResponseContext) args[1];
        // Verify incoming data.
        assertEquals(HttpStatus.S_400_BAD_REQUEST, context.getResponseData().getStatus());
        assertTrue(context.getResponseData().getHeaders().isEmpty());
        assertNull(context.getResponseData().getRecordResponseEnvelope().getRecord());
        // Modify data in filter.
        setStatus(context, HttpStatus.S_403_FORBIDDEN);
        context.getResponseData().getHeaders().putAll(headersFromFilter);
        return null;
      }
    }).when(_filter).onResponse(eq(_filterRequestContext), any(FilterResponseContext.class));
    RestException restException = new RestException(new RestResponseBuilder().build());
    when(_responseHandler.buildRestException(exFromApp, partialResponse)).thenReturn(restException);
    // Invoke.
    _twoFilterRestLiCallback.onError(exFromApp, executionReport);
    // Verify.
    assertNotNull(responseData);
    assertEquals(HttpStatus.S_403_FORBIDDEN, responseData.getStatus());
    assertNull(responseData.getRecordResponseEnvelope().getRecord());
    assertTrue(responseData.isErrorResponse());
    assertEquals(responseData.getServiceException().getErrorDetails(), appException.getErrorDetails());
    assertEquals(responseData.getServiceException().getOverridingFormat(), appException.getOverridingFormat());
    assertEquals(responseData.getServiceException().getServiceErrorCode(), appException.getServiceErrorCode());
    assertEquals(responseData.getServiceException().getMessage(), appException.getMessage());

    assertEquals(headersFromFilter, responseData.getHeaders());
    verify(_responseHandler).buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
        exCapture.capture(), anyMap());
    verify(_responseHandler).buildPartialResponse(_routingResult, responseData);
    verify(_responseHandler).buildRestException(exFromApp, partialResponse);
    verify(_callback).onError(restException, executionReport);
    verify(_restRequest, times(1)).getHeaders();
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
  public void testOnErrorWithFiltersSuccessfulyHandlingAppEx() throws Exception
  {
    Exception exFromApp = new RuntimeException("Runtime exception from app");
    RestLiServiceException appException = new RestLiServiceException(HttpStatus.S_404_NOT_FOUND);
    RequestExecutionReport executionReport = new RequestExecutionReportBuilder().build();
    final Map<String, String> headersFromApp = Maps.newHashMap();
    headersFromApp.put("Key", "Input");
    final RecordTemplate entityFromFilter = Foo.createFoo("Key", "Two");
    final Map<String, String> headersFromFilter = Maps.newHashMap();
    headersFromFilter.put("Key", "Output");

    RestLiResponseEnvelope responseData = new RecordResponseEnvelope(appException, headersFromApp);

    PartialRestResponse partialResponse = new PartialRestResponse.Builder().build();
    ArgumentCaptor<RestLiServiceException> exCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    when(
         _responseHandler.buildExceptionResponseData(eq(_restRequest), eq(_routingResult), exCapture.capture(),
             anyMap())).thenReturn(responseData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseData)).thenReturn(partialResponse);

    // Mock the behavior of the first filter.
    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterResponseContext context = (FilterResponseContext) args[1];
        // Verify incoming data.
        assertEquals(HttpStatus.S_404_NOT_FOUND, context.getResponseData().getStatus());
        assertEquals(headersFromApp, context.getResponseData().getHeaders());
        assertNull(context.getResponseData().getRecordResponseEnvelope().getRecord());
        // Modify data in filter.
        setStatus(context, HttpStatus.S_400_BAD_REQUEST);
        context.getResponseData().getHeaders().clear();
        return null;
      }
    }).doAnswer(new Answer<Object>()
    // Mock the behavior of the second filter.
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterResponseContext context = (FilterResponseContext) args[1];
        // Verify incoming data.
        assertEquals(HttpStatus.S_400_BAD_REQUEST, context.getResponseData().getStatus());
        assertTrue(context.getResponseData().getHeaders().isEmpty());
        assertNull(context.getResponseData().getRecordResponseEnvelope().getRecord());
        // Modify data in filter.
        setStatus(context, HttpStatus.S_403_FORBIDDEN);
        context.getResponseData().getRecordResponseEnvelope().setRecord(entityFromFilter, HttpStatus.S_403_FORBIDDEN);
        context.getResponseData().getHeaders().putAll(headersFromFilter);
        return null;
      }
    }).when(_filter).onResponse(eq(_filterRequestContext), any(FilterResponseContext.class));

    RestResponse restResponse = new RestResponseBuilder().build();
    when(_responseHandler.buildResponse(_routingResult, partialResponse)).thenReturn(restResponse);


    // Invoke.
    _twoFilterRestLiCallback.onError(exFromApp, executionReport);
    // Verify.
    assertNotNull(responseData);
    assertEquals(HttpStatus.S_403_FORBIDDEN, responseData.getStatus());
    assertEquals(entityFromFilter, responseData.getRecordResponseEnvelope().getRecord());
    assertEquals(headersFromFilter, responseData.getHeaders());
    verify(_responseHandler).buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
        exCapture.capture(), anyMap());
    verify(_responseHandler).buildPartialResponse(_routingResult, responseData);
    verify(_responseHandler).buildResponse(_routingResult, partialResponse);
    verify(_callback).onSuccess(restResponse, executionReport);
    verify(_restRequest, times(1)).getHeaders();
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
  public void testOnErrorWithFiltersExceptionFromFirstFilterSecondFilterDoesNotHandle() throws Exception
  {
    // App stuff.
    RestLiServiceException exFromApp = new RestLiServiceException(HttpStatus.S_404_NOT_FOUND, "App failure");
    RequestExecutionReport executionReport = new RequestExecutionReportBuilder().build();
    RestLiServiceException appException = new RestLiServiceException(HttpStatus.S_404_NOT_FOUND);
    RestLiResponseEnvelope responseAppData = new RecordResponseEnvelope(appException, Collections.<String, String>emptyMap());

    // Filter stuff.
    final Exception exFromFirstFilter = new RuntimeException("Runtime exception from first filter");
    RestLiServiceException filterException = new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR);
    RestLiResponseEnvelope responseFilterData = new RecordResponseEnvelope(filterException, Collections.<String, String>emptyMap());

    PartialRestResponse partialResponse = new PartialRestResponse.Builder().build();
    ArgumentCaptor<RestLiServiceException> wrappedExCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    RestException restException = new RestException(new RestResponseBuilder().build());

    // Setup.
    when(
         _responseHandler.buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
             wrappedExCapture.capture(), anyMap())).thenReturn(responseAppData)
                                                                                       .thenReturn(responseFilterData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseFilterData)).thenReturn(partialResponse);
    when(_restRequest.getHeaders()).thenReturn(null);
    when(_responseHandler.buildRestException(exFromFirstFilter, partialResponse)).thenReturn(restException);

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
        assertEquals(context.getResponseData().getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);
        assertNull(context.getResponseData().getRecordResponseEnvelope().getRecord());
        assertTrue(context.getResponseData().getHeaders().isEmpty());
        assertTrue(context.getResponseData().isErrorResponse());

        // Modify data.
        setStatus(context, HttpStatus.S_402_PAYMENT_REQUIRED);
        // The second filter does not handle the exception thrown by the first filter (i.e.) the
        // response data still has the error response corresponding to the exception from the first
        // filter.
        return null;
      }
    }).when(_filter).onResponse(eq(_filterRequestContext), any(FilterResponseContext.class));

    // Invoke.
    _twoFilterRestLiCallback.onError(exFromApp, executionReport);

    // Verify.
    verify(_responseHandler, times(2)).buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
        wrappedExCapture.capture(), anyMap());
    verify(_responseHandler).buildRestException(exFromFirstFilter, partialResponse);
    verify(_responseHandler).buildPartialResponse(_routingResult, responseFilterData);
    verify(_callback).onError(restException, executionReport);
    verify(_restRequest, times(2)).getHeaders();
    verifyZeroInteractions(_routingResult);
    verifyNoMoreInteractions(_restRequest, _responseHandler, _callback);
    assertNotNull(responseFilterData);
    assertEquals(HttpStatus.S_402_PAYMENT_REQUIRED, responseFilterData.getStatus());
    assertTrue(responseFilterData.getHeaders().isEmpty());
    assertNull(responseFilterData.getRecordResponseEnvelope().getRecord());
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

  @DataProvider(name = "provideResponseEntities")
  private Object[][] provideResponseEntities()
  {
    List<Foo> fooCollection = new ArrayList<Foo>();
    fooCollection.add(Foo.createFoo("Key", "One"));
    fooCollection.add(Foo.createFoo("Key", "Two"));
    fooCollection.add(Foo.createFoo("Key", "Three"));
    Map<String, Foo> fooBatch = new HashMap<String, Foo>();
    fooBatch.put("batchKey1", Foo.createFoo("Key", "One"));
    fooBatch.put("batchKey2", Foo.createFoo("Key", "Two"));
    return new Object[][] {
        { ResourceMethod.GET, Foo.createFoo("Key", "One") },
        { ResourceMethod.FINDER, fooCollection },
        { ResourceMethod.BATCH_GET, fooBatch }
    };
  }

  @SuppressWarnings("unchecked")
  @Test(dataProvider = "provideResponseEntities")
  public void testOnErrorWithFiltersExceptionFromFirstFilterSecondFilterHandles(final ResourceMethod resourceMethod, final Object entityFromFilter2) throws Exception
  {
    // App stuff.
    RestLiServiceException exFromApp = new RestLiServiceException(HttpStatus.S_404_NOT_FOUND, "App failure");
    RequestExecutionReport executionReport = new RequestExecutionReportBuilder().build();
    RestLiServiceException appException = new RestLiServiceException(HttpStatus.S_404_NOT_FOUND);

    RestLiResponseEnvelope responseAppData;
    switch (ResponseType.fromMethodType(resourceMethod))
    {
      case SINGLE_ENTITY:
        responseAppData = new RecordResponseEnvelope(appException, Collections.<String, String>emptyMap());
        break;
      case GET_COLLECTION:
        responseAppData = new CollectionResponseEnvelope(appException, Collections.<String, String>emptyMap());
        break;
      case CREATE_COLLECTION:
        responseAppData = new CreateCollectionResponseEnvelope(appException, Collections.<String, String>emptyMap());
        break;
      case BATCH_ENTITIES:
        responseAppData = new BatchResponseEnvelope(appException, Collections.<String, String>emptyMap());
        break;
      case STATUS_ONLY:
        responseAppData = new EmptyResponseEnvelope(appException, Collections.<String, String>emptyMap());
        break;
      default:
        throw new IllegalStateException();
    }

    // Filter stuff.
    final Exception exFromFirstFilter = new RuntimeException("Runtime exception from first filter");
    RestLiServiceException filterException = new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR);
    final Map<String, String> headersFromFilter = Maps.newHashMap();
    headersFromFilter.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, AllProtocolVersions.LATEST_PROTOCOL_VERSION.toString());
    String errorResponseHeaderName = HeaderUtil.getErrorResponseHeaderName(AllProtocolVersions.LATEST_PROTOCOL_VERSION);
    headersFromFilter.put(errorResponseHeaderName, RestConstants.HEADER_VALUE_ERROR);

    RestLiResponseEnvelope responseFilterData;
    switch (ResponseType.fromMethodType(resourceMethod))
    {
      case SINGLE_ENTITY:
        responseFilterData = new RecordResponseEnvelope(filterException, headersFromFilter);
        break;
      case GET_COLLECTION:
        responseFilterData = new CollectionResponseEnvelope(filterException, headersFromFilter);
        break;
      case CREATE_COLLECTION:
        responseFilterData = new CreateCollectionResponseEnvelope(filterException, headersFromFilter);
        break;
      case BATCH_ENTITIES:
        responseFilterData = new BatchResponseEnvelope(filterException, headersFromFilter);
        break;
      case STATUS_ONLY:
        responseFilterData = new EmptyResponseEnvelope(filterException, headersFromFilter);
        break;
      default:
        throw new IllegalStateException();
    }

    PartialRestResponse partialResponse = new PartialRestResponse.Builder().build();
    ArgumentCaptor<RestLiServiceException> wrappedExCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    RestResponse restResponse = new RestResponseBuilder().build();
    final String customHeader = "Custom-Header";
    final String customHeaderValue = "CustomValue";

    // Setup.
    when(
         _responseHandler.buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
             wrappedExCapture.capture(), anyMap())).thenReturn(responseAppData)
                                                                                       .thenReturn(responseFilterData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseFilterData)).thenReturn(partialResponse);
    when(_responseHandler.buildResponse(_routingResult, partialResponse)).thenReturn(restResponse);
    when(_restRequest.getHeaders()).thenReturn(null);

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
        assertEquals(context.getResponseData().getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);

        switch (ResponseType.fromMethodType(resourceMethod))
        {
          case SINGLE_ENTITY:
            assertNull(context.getResponseData().getRecordResponseEnvelope().getRecord());
            break;
          case GET_COLLECTION:
            assertNull(context.getResponseData().getCollectionResponseEnvelope().getCollectionResponse());
            break;
          case CREATE_COLLECTION:
            assertNull(context.getResponseData().getCreateCollectionResponseEnvelope().getCreateResponses());
            break;
          case BATCH_ENTITIES:
            assertNull(context.getResponseData().getBatchResponseEnvelope().getBatchResponseMap());
            break;
          case STATUS_ONLY:
            break;
        }

        assertEquals(context.getResponseData().getHeaders(), headersFromFilter);
        assertTrue(context.getResponseData().isErrorResponse());

        // Modify data.
        context.getResponseData().getHeaders().put(customHeader, customHeaderValue);
        setStatus(context, HttpStatus.S_402_PAYMENT_REQUIRED);
        // The second filter does handles the exception thrown by the first filter (i.e.) clears the
        // error response corresponding to the exception from the first
        // filter.
        if (entityFromFilter2 instanceof RecordTemplate)
        {
          context.getResponseData().getRecordResponseEnvelope().setRecord((RecordTemplate) entityFromFilter2,
              HttpStatus.S_402_PAYMENT_REQUIRED);
        }
        else if (entityFromFilter2 instanceof List)
        {
          context.getResponseData().getCollectionResponseEnvelope().setCollectionResponse(HttpStatus.S_402_PAYMENT_REQUIRED,
                                                                                          (List<? extends RecordTemplate>) entityFromFilter2,
                                                                                          new CollectionMetadata(),
                                                                                          null);
        }
        else
        {
          Map<Object, BatchResponseEnvelope.BatchResponseEntry> responseMap =  new HashMap<Object, BatchResponseEnvelope.BatchResponseEntry>();
          for (Map.Entry<?, RecordTemplate> entry : ((Map<?, RecordTemplate>) entityFromFilter2).entrySet())
          {
            responseMap.put(entry.getKey(), new BatchResponseEnvelope.BatchResponseEntry(HttpStatus.S_200_OK, entry.getValue()));
          }

          context.getResponseData().getBatchResponseEnvelope().setBatchResponseMap(HttpStatus.S_402_PAYMENT_REQUIRED,
              responseMap);
        }
        return null;
      }
    }).when(_filter).onResponse(eq(_filterRequestContext), any(FilterResponseContext.class));

    // Invoke.
    _twoFilterRestLiCallback.onError(exFromApp, executionReport);

    // Verify.
    verify(_responseHandler, times(2)).buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
        wrappedExCapture.capture(), anyMap());
    verify(_responseHandler).buildPartialResponse(_routingResult, responseFilterData);
    verify(_responseHandler).buildResponse(_routingResult, partialResponse);
    verify(_callback).onSuccess(restResponse, executionReport);
    verify(_restRequest, times(2)).getHeaders();
    verifyZeroInteractions(_routingResult);
    verifyNoMoreInteractions(_restRequest, _responseHandler, _callback);
    assertNotNull(responseFilterData);
    assertEquals(HttpStatus.S_402_PAYMENT_REQUIRED, responseFilterData.getStatus());
    // Only the error header should have been cleared.
    assertFalse(responseFilterData.getHeaders().containsKey(errorResponseHeaderName));
    assertEquals(responseFilterData.getHeaders().get(customHeader), customHeaderValue);
    if (entityFromFilter2 instanceof RecordTemplate)
    {
      assertTrue(responseFilterData.getResponseType() == ResponseType.SINGLE_ENTITY);
      assertEquals(responseFilterData.getRecordResponseEnvelope().getRecord(), entityFromFilter2);
    }
    else if (entityFromFilter2 instanceof List)
    {
      if (responseFilterData.getResponseType() == ResponseType.GET_COLLECTION)
      {
        assertEquals(responseFilterData.getCollectionResponseEnvelope().getCollectionResponse(), entityFromFilter2);
      }
      else
      {
        fail();
      }
    }
    else
    {
      assertTrue(responseFilterData.getResponseType() == ResponseType.BATCH_ENTITIES);

      Map<Object, RecordTemplate> values = new HashMap<Object, RecordTemplate>();
      for(Map.Entry<?, BatchResponseEnvelope.BatchResponseEntry> entry: responseFilterData.getBatchResponseEnvelope().getBatchResponseMap().entrySet())
      {
        values.put(entry.getKey(), entry.getValue().getRecord());
      }

      assertEquals(values, entityFromFilter2);
    }
    assertFalse(responseFilterData.isErrorResponse());
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
    RestLiResponseEnvelope responseAppData = new RecordResponseEnvelope(exFromApp, Collections.<String, String>emptyMap());

    // Filter stuff.
    final Exception exFromSecondFilter = new RuntimeException("Runtime exception from second filter");
    RestLiResponseEnvelope responseFilterData = new RecordResponseEnvelope(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                                                                                                      exFromSecondFilter),
                                                                           Collections.<String, String>emptyMap());

    PartialRestResponse partialResponse = new PartialRestResponse.Builder().build();

    ArgumentCaptor<RestLiServiceException> wrappedExCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    RestException restException = new RestException(new RestResponseBuilder().build());

    // Setup.
    when(
         _responseHandler.buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
             wrappedExCapture.capture(), anyMap())).thenReturn(responseAppData)
                                                                                       .thenReturn(responseFilterData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseFilterData)).thenReturn(partialResponse);
    when(_restRequest.getHeaders()).thenReturn(null);
    when(_responseHandler.buildRestException(exFromSecondFilter, partialResponse)).thenReturn(restException);

    // Mock filter behavior.
    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterResponseContext context = (FilterResponseContext) args[1];
        assertEquals(context.getResponseData().getStatus(), HttpStatus.S_404_NOT_FOUND);
        assertNull(context.getResponseData().getRecordResponseEnvelope().getRecord());
        assertTrue(context.getResponseData().getHeaders().isEmpty());

        // Modify data.
        setStatus(context, HttpStatus.S_402_PAYMENT_REQUIRED);
        return null;
      }
    }).doThrow(exFromSecondFilter).when(_filter)
      .onResponse(eq(_filterRequestContext), any(FilterResponseContext.class));

    // Invoke.
    _twoFilterRestLiCallback.onError(exFromApp, executionReport);

    // Verify.
    verify(_responseHandler, times(2)).buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
        wrappedExCapture.capture(), anyMap());
    verify(_responseHandler).buildPartialResponse(_routingResult, responseFilterData);
    verify(_responseHandler).buildRestException(exFromSecondFilter, partialResponse);
    verify(_callback).onError(restException, executionReport);
    verify(_restRequest, times(2)).getHeaders();
    verifyZeroInteractions(_routingResult);
    verifyNoMoreInteractions(_restRequest, _responseHandler, _callback);
    assertNotNull(responseFilterData);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, responseFilterData.getStatus());
    assertTrue(responseFilterData.getHeaders().isEmpty());
    assertNull(responseFilterData.getRecordResponseEnvelope().getRecord());
    RestLiServiceException restliEx = wrappedExCapture.getAllValues().get(0);
    assertNotNull(restliEx);
    assertEquals(exFromApp.getStatus(), restliEx.getStatus());
    assertEquals(exFromApp.getMessage(), restliEx.getMessage());
    restliEx = wrappedExCapture.getAllValues().get(1);
    assertNotNull(restliEx);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, restliEx.getStatus());
    assertEquals(exFromSecondFilter.getMessage(), restliEx.getMessage());
    assertEquals(exFromSecondFilter, restliEx.getCause());
  }

  @Test
  public void testFilterResponseContextAdapter()
  {
    DataMap dataMap = new DataMap();
    dataMap.put("foo", "bar");
    Map<String, String> headers = Maps.newHashMap();
    headers.put("x", "y");
    RecordTemplate entity1 = new Foo(dataMap);
    RestLiResponseEnvelope responseData = new RecordResponseEnvelope(HttpStatus.S_200_OK, entity1, headers);
    RestLiResponseEnvelope updatedResponseData = new EmptyResponseEnvelope(HttpStatus.S_200_OK, Collections.<String, String>emptyMap());
    FilterResponseContext context = new RestLiCallback.FilterResponseContextAdapter(responseData);
    assertEquals(headers, context.getResponseData().getHeaders());
    assertEquals(entity1, context.getResponseData().getRecordResponseEnvelope().getRecord());
    assertEquals(HttpStatus.S_200_OK, context.getResponseData().getStatus());

    Foo entity2 = Foo.createFoo("boo", "bar");
    context.getResponseData().getRecordResponseEnvelope().setRecord(entity2, HttpStatus.S_404_NOT_FOUND);
    assertEquals(context.getResponseData().getRecordResponseEnvelope().getRecord(), entity2);
    assertEquals(HttpStatus.S_404_NOT_FOUND, context.getResponseData().getStatus());
    assertEquals(HttpStatus.S_404_NOT_FOUND, responseData.getStatus());
    assertEquals(responseData, context.getResponseData());
    assertEquals(responseData, ((FilterResponseContextInternal) context).getRestLiResponseEnvelope());
    ((FilterResponseContextInternal) context).setRestLiResponseEnvelope(updatedResponseData);
    assertEquals(updatedResponseData, ((FilterResponseContextInternal) context).getRestLiResponseEnvelope());
  }

  @DataProvider(name = "provideExceptionsAndStatuses")
  private Object[][] provideExceptionsAndStatuses()
  {
    return new Object[][] {
        { new RuntimeException("Test runtime exception"), HttpStatus.S_500_INTERNAL_SERVER_ERROR },
        { new RoutingException("Test routing exception", 404), HttpStatus.S_404_NOT_FOUND},
        { new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, "Test service exception"), HttpStatus.S_400_BAD_REQUEST },
        { new RestLiServiceException(HttpStatus.S_403_FORBIDDEN, "Wrapped runtime exception with custom status",
            new RuntimeException("Original cause")), HttpStatus.S_403_FORBIDDEN }
    };
  }

  @SuppressWarnings("unchecked")
  @Test(dataProvider = "provideExceptionsAndStatuses")
  public void testConvertExceptionToRestLiResponseData(Exception e, HttpStatus status)
  {
    RestLiServiceException serviceException;
    if (e instanceof RestLiServiceException)
    {
      serviceException = (RestLiServiceException) e;
    }
    else
    {
      serviceException = new RestLiServiceException(status, e);
    }
    RestLiResponseEnvelope responseData = new RecordResponseEnvelope(serviceException, Collections.<String, String>emptyMap());
    ArgumentCaptor<RestLiServiceException> exceptionArgumentCaptor = ArgumentCaptor.forClass(RestLiServiceException.class);

    // Setup.
    when(_responseHandler.buildExceptionResponseData(eq(_restRequest), eq(_routingResult), exceptionArgumentCaptor.capture(),
        anyMap())).thenReturn(responseData);
    when(_restRequest.getHeaders()).thenReturn(null);

    // Invoke.
    RestLiResponseEnvelope resultData = _noFilterRestLiCallback.convertExceptionToRestLiResponseData(e);

    // Verify.
    verify(_responseHandler).buildExceptionResponseData(eq(_restRequest), eq(_routingResult), exceptionArgumentCaptor.capture(),
        anyMap());
    verify(_restRequest).getHeaders();
    verifyNoMoreInteractions(_responseHandler, _restRequest);
    verifyZeroInteractions(_routingResult, _callback);
    // RestLiCallback should pass the original exception to the response handler.
    RestLiServiceException exceptionArgument = exceptionArgumentCaptor.getValue();
    assertTrue(exceptionArgument.equals(e) || exceptionArgument.getCause().equals(e));
    assertEquals(exceptionArgument.getStatus(), status);
    // The end result should also contain the original exception.
    assertTrue(resultData.isErrorResponse());
    assertTrue(resultData.getServiceException().equals(e) || resultData.getServiceException().getCause().equals(e));
    assertEquals(resultData.getServiceException().getStatus(), status);
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

  // Helper method to transition legacy test cases
  private static void setStatus(FilterResponseContext context, HttpStatus status)
  {
    if (context.getResponseData().isErrorResponse())
    {
      RestLiServiceException exception = new RestLiServiceException(status);
      switch (context.getResponseData().getResponseType())
      {
        case SINGLE_ENTITY:
          context.getResponseData().getRecordResponseEnvelope().setException(exception);
          break;
        case GET_COLLECTION:
          context.getResponseData().getCollectionResponseEnvelope().setException(exception);
          break;
        case CREATE_COLLECTION:
          context.getResponseData().getCreateCollectionResponseEnvelope().setException(exception);
          break;
        case BATCH_ENTITIES:
          context.getResponseData().getBatchResponseEnvelope().setException(exception);
          break;
        case STATUS_ONLY:
          context.getResponseData().getEmptyResponseEnvelope().setException(exception);
          break;
      }
    }
    else
    {
      switch (context.getResponseData().getResponseType())
      {
        case SINGLE_ENTITY:
          context.getResponseData().getRecordResponseEnvelope().setStatus(status);
          break;
        case GET_COLLECTION:
          context.getResponseData().getCollectionResponseEnvelope().setStatus(status);
          break;
        case CREATE_COLLECTION:
          context.getResponseData().getCreateCollectionResponseEnvelope().setStatus(status);
          break;
        case BATCH_ENTITIES:
          context.getResponseData().getBatchResponseEnvelope().setStatus(status);
          break;
        case STATUS_ONLY:
          context.getResponseData().getEmptyResponseEnvelope().setStatus(status);
          break;
      }
    }
  }
}
