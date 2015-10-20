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
import com.linkedin.restli.internal.server.methods.response.PartialRestResponse;
import com.linkedin.restli.internal.server.response.BatchResponseEnvelope;
import com.linkedin.restli.internal.server.response.CollectionResponseEnvelope;
import com.linkedin.restli.internal.server.response.CreateCollectionResponseEnvelope;
import com.linkedin.restli.internal.server.response.EmptyResponseEnvelope;
import com.linkedin.restli.internal.server.response.RecordResponseEnvelope;
import com.linkedin.restli.server.RequestExecutionCallback;
import com.linkedin.restli.server.RequestExecutionReport;
import com.linkedin.restli.server.RequestExecutionReportBuilder;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.FilterResponseContext;
import com.linkedin.restli.server.filter.NextResponseFilter;
import com.linkedin.restli.server.filter.ResponseFilter;

import java.net.HttpCookie;
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
import static org.mockito.Matchers.anyList;
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
import static org.testng.Assert.fail;


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

  private RestLiCallback<Object> _oneFilterRestLiCallback;

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
    _oneFilterRestLiCallback =
        new RestLiCallback<Object>(_restRequest, _routingResult, _responseHandler, _callback, Arrays.asList(_filter),
                                   _filterRequestContext);
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
    RestLiResponseEnvelope responseData = new RecordResponseEnvelope(HttpStatus.S_200_OK, null, Collections.<String, String>emptyMap(),
                                                                     Collections.<HttpCookie>emptyList());
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
    RestLiResponseEnvelope responseData = new RecordResponseEnvelope(ex, restExceptionHeaders,
                                                                     Collections.<HttpCookie>emptyList());

    PartialRestResponse partialResponse = new PartialRestResponse.Builder().build();
    RestException restException = new RestException(new RestResponseBuilder().build());
    // Set up.
    when(_restRequest.getHeaders()).thenReturn(inputHeaders);
    when(_responseHandler.buildExceptionResponseData(eq(_restRequest), eq(_routingResult), eq(ex),
             augErrorHeadersCapture.capture(), anyList())).thenReturn(responseData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseData)).thenReturn(partialResponse);
    when(_responseHandler.buildRestException(ex, partialResponse)).thenReturn(restException);

    // Invoke.
    _noFilterRestLiCallback.onError(ex, executionReport);

    // Verify.
    verify(_responseHandler).buildRestException(ex, partialResponse);
    verify(_responseHandler).buildExceptionResponseData(eq(_restRequest), eq(_routingResult), eq(ex),
        augErrorHeadersCapture.capture(), anyList());
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
    RestLiServiceException wrappedEx = new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, ex);
    RestLiResponseEnvelope responseData = new RecordResponseEnvelope(wrappedEx,
                                                                     Collections.<String, String>emptyMap(),
                                                                     Collections.<HttpCookie>emptyList());

    RestException restException = new RestException(new RestResponseBuilder().build());
    Map<String, String> inputHeaders = Maps.newHashMap();
    inputHeaders.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, "2.0.0");

    // Set up.
    when(_restRequest.getHeaders()).thenReturn(inputHeaders);
    when(_responseHandler.buildExceptionResponseData(eq(_restRequest), eq(_routingResult), exCapture.capture(),
             anyMap(), anyList())).thenReturn(responseData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseData)).thenReturn(partialResponse);
    when(_responseHandler.buildRestException(wrappedEx, partialResponse)).thenReturn(restException);

    // Invoke.
    _noFilterRestLiCallback.onError(ex, executionReport);

    // Verify.
    verify(_responseHandler).buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
        exCapture.capture(), anyMap(), anyList());
    verify(_responseHandler).buildPartialResponse(_routingResult, responseData);
    verify(_responseHandler).buildRestException(wrappedEx, partialResponse);
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

  @SuppressWarnings("unchecked")
  @Test
  public void testOnSuccessWithExceptionBuildingResponseNoFilters() throws Exception
  {
    String result = "foo";
    RequestExecutionReport executionReport = new RequestExecutionReportBuilder().build();
    RestLiServiceException ex = new RestLiServiceException(HttpStatus.S_422_UNPROCESSABLE_ENTITY);
    Map<String, String> inputHeaders = Maps.newHashMap();
    inputHeaders.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION,
                     AllProtocolVersions.BASELINE_PROTOCOL_VERSION.toString());
    RestLiResponseEnvelope responseData = new RecordResponseEnvelope(ex,
                                                                     new HashMap<String, String>(),
                                                                     Collections.<HttpCookie>emptyList());
    PartialRestResponse partialResponse = new PartialRestResponse.Builder().build();
    RestException restException = new RestException(new RestResponseBuilder().build());
    // Set up.
    // Throw an exception when we try to build the response data.
    when(_responseHandler.buildRestLiResponseData(_restRequest, _routingResult, result)).thenThrow(ex);
    when(_restRequest.getHeaders()).thenReturn(inputHeaders);
    when(_responseHandler.buildExceptionResponseData(eq(_restRequest), eq(_routingResult), eq(ex),
                                                     anyMap(), anyList())).thenReturn(responseData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseData)).thenReturn(partialResponse);
    when(_responseHandler.buildRestException(ex, partialResponse)).thenReturn(restException);

    // Invoke.
    _noFilterRestLiCallback.onSuccess(result, executionReport);

    // Verify.
    verify(_responseHandler).buildRestLiResponseData(_restRequest, _routingResult, result);
    verify(_responseHandler).buildRestException(ex, partialResponse);
    verify(_responseHandler).buildExceptionResponseData(eq(_restRequest), eq(_routingResult), eq(ex),
                                                        anyMap(), anyList());
    verify(_responseHandler).buildPartialResponse(_routingResult, responseData);
    verify(_callback).onError(restException, executionReport);
    verify(_restRequest).getHeaders();
    verifyZeroInteractions(_routingResult);
    verifyNoMoreInteractions(_restRequest, _responseHandler, _callback);
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
    RestLiResponseEnvelope appResponseData = new RecordResponseEnvelope(HttpStatus.S_200_OK, entityFromApp, headersFromApp,
                                                                        Collections.<HttpCookie>emptyList());
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
        FilterRequestContext requestContext = (FilterRequestContext) args[0];
        FilterResponseContext responseContext = (FilterResponseContext) args[1];
        NextResponseFilter nextResponseFilter = (NextResponseFilter) args[2];
        // Verify incoming data.
        assertEquals(HttpStatus.S_200_OK, responseContext.getResponseData().getStatus());
        assertEquals(headersFromApp, responseContext.getResponseData().getHeaders());
        assertEquals(entityFromApp, responseContext.getResponseData().getRecordResponseEnvelope().getRecord());
        // Modify data in filter.
        setStatus(responseContext, HttpStatus.S_400_BAD_REQUEST);
        responseContext.getResponseData().getRecordResponseEnvelope().setRecord(entityFromFilter1,
            HttpStatus.S_400_BAD_REQUEST);
        responseContext.getResponseData().getHeaders().clear();
        nextResponseFilter.onResponse(requestContext, responseContext);
        return null;
      }
    }).doAnswer(new Answer<Object>()
    // Mock the behavior of the first filter.
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterRequestContext requestContext = (FilterRequestContext) args[0];
        FilterResponseContext responseContext = (FilterResponseContext) args[1];
        NextResponseFilter nextResponseFilter = (NextResponseFilter) args[2];
        // Verify incoming data.
        assertEquals(HttpStatus.S_400_BAD_REQUEST, responseContext.getResponseData().getStatus());
        assertTrue(responseContext.getResponseData().getHeaders().isEmpty());
        assertEquals(responseContext.getResponseData().getRecordResponseEnvelope().getRecord(), entityFromFilter1);
        // Modify data in filter.
        setStatus(responseContext, HttpStatus.S_403_FORBIDDEN);
        responseContext.getResponseData().getRecordResponseEnvelope().setRecord(entityFromFilter2, HttpStatus.S_403_FORBIDDEN);
        responseContext.getResponseData().getHeaders().putAll(headersFromFilters);
        nextResponseFilter.onResponse(requestContext, responseContext);
        return null;
      }
    }).when(_filter).onResponse(eq(_filterRequestContext), any(FilterResponseContext.class), any(NextResponseFilter.class));

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
    RestLiResponseEnvelope appResponseData = new RecordResponseEnvelope(HttpStatus.S_200_OK, entityFromApp, Collections.<String, String>emptyMap(),
                                                                        Collections.<HttpCookie>emptyList());

    // Filter suff.
    ArgumentCaptor<RestLiServiceException> exFromFilterCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    final Map<String, String> headersFromFilter = Maps.newHashMap();
    headersFromFilter.put("Key", "Error from filter");
    final RestLiServiceException exceptionFromFilter = new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR);
    RestLiResponseEnvelope responseErrorData = new RecordResponseEnvelope(exceptionFromFilter, headersFromFilter,
                                                                          Collections.<HttpCookie>emptyList());
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
             exFromFilterCapture.capture(), anyMap(), anyList())).thenReturn(responseErrorData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseErrorData)).thenReturn(partialFilterErrorResponse);

    when(_responseHandler.buildResponse(_routingResult, partialFilterErrorResponse)).thenReturn(restResponse);
    // Mock filter behavior.
    doThrow(exFromFilter).doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterRequestContext requestContext = (FilterRequestContext) args[0];
        FilterResponseContext responseContext = (FilterResponseContext) args[1];
        NextResponseFilter nextResponseFilter = (NextResponseFilter) args[2];

        // The second filter should be invoked with details of the exception thrown by the first
        // filter.
        assertEquals(responseContext.getResponseData().getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);
        assertNull(responseContext.getResponseData().getRecordResponseEnvelope().getRecord());
        assertEquals(responseContext.getResponseData().getHeaders(), headersFromFilter);
        assertEquals(responseContext.getResponseData().getServiceException(), exceptionFromFilter);

        // Modify data.
        setStatus(responseContext, HttpStatus.S_402_PAYMENT_REQUIRED);
        // The second filter handles the exception thrown by the first filter (i.e.) sets an entity
        // response in the response data.
        responseContext.getResponseData().getRecordResponseEnvelope().setRecord(entityFromFilter,
            HttpStatus.S_402_PAYMENT_REQUIRED);
        nextResponseFilter.onResponse(requestContext, responseContext);
        return null;
      }
    }).when(_filter).onResponse(eq(_filterRequestContext), any(FilterResponseContext.class), any(NextResponseFilter.class));

    // Invoke.
    _twoFilterRestLiCallback.onSuccess(entityFromApp, executionReport);

    // Verify.
    verify(_responseHandler).buildRestLiResponseData(_restRequest, _routingResult, entityFromApp);
    verify(_responseHandler).buildPartialResponse(_routingResult, responseErrorData);
    verify(_responseHandler).buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
        exFromFilterCapture.capture(), anyMap(), anyList());
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
    RestLiResponseEnvelope appResponseData = new RecordResponseEnvelope(HttpStatus.S_200_OK, entityFromApp, Collections.<String, String>emptyMap(),
                                                                        Collections.<HttpCookie>emptyList());

    // Filter suff.
    ArgumentCaptor<RestLiServiceException> exFromFilterCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    final Map<String, String> headersFromFilter = Maps.newHashMap();
    headersFromFilter.put("Key", "Error from filter");
    RestLiServiceException exceptionFromFilter = new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR);
    RestLiResponseEnvelope responseErrorData = new RecordResponseEnvelope(exceptionFromFilter, headersFromFilter,
                                                                          Collections.<HttpCookie>emptyList());
    PartialRestResponse partialFilterErrorResponse = new PartialRestResponse.Builder().build();
    final Exception exFromFilter = new RuntimeException("Exception From Filter");

    // Common stuff.
    RestException finalRestException = new RestException(new RestResponseBuilder().build());
    // Setup.
    when(_responseHandler.buildRestLiResponseData(_restRequest, _routingResult, entityFromApp)).thenReturn(appResponseData);
    when(_restRequest.getHeaders()).thenReturn(null);
    when(_responseHandler.buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
             exFromFilterCapture.capture(), anyMap(), anyList())).thenReturn(responseErrorData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseErrorData)).thenReturn(partialFilterErrorResponse);
    when(_responseHandler.buildRestException(any(RestLiServiceException.class), eq(partialFilterErrorResponse))).thenReturn(
        finalRestException);
    // Mock filter behavior.
    doThrow(exFromFilter).doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterRequestContext requestContext = (FilterRequestContext) args[0];
        FilterResponseContext responseContext = (FilterResponseContext) args[1];
        NextResponseFilter nextResponseFilter = (NextResponseFilter) args[2];

        // The second filter should be invoked with details of the exception thrown by the first
        // filter.
        assertEquals(responseContext.getResponseData().getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);
        assertNull(responseContext.getResponseData().getRecordResponseEnvelope().getRecord());
        assertEquals(responseContext.getResponseData().getHeaders(), headersFromFilter);

        // Modify data.
        setStatus(responseContext, HttpStatus.S_402_PAYMENT_REQUIRED);
        // The second filter handles the exception thrown by the first filter (i.e.) does not throw
        // another exception.
        nextResponseFilter.onResponse(requestContext, responseContext);
        return null;
      }
    }).when(_filter).onResponse(eq(_filterRequestContext), any(FilterResponseContext.class), any(NextResponseFilter.class));

    // Invoke.
    _twoFilterRestLiCallback.onSuccess(entityFromApp, executionReport);

    // Verify.
    verify(_responseHandler).buildRestLiResponseData(_restRequest, _routingResult, entityFromApp);
    verify(_responseHandler).buildPartialResponse(_routingResult, responseErrorData);
    verify(_responseHandler).buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
        exFromFilterCapture.capture(), anyMap(), anyList());
    verify(_responseHandler).buildPartialResponse(_routingResult, responseErrorData);
    verify(_responseHandler).buildRestException(exFromFilterCapture.capture(), eq(partialFilterErrorResponse));
    verify(_callback).onError(finalRestException, executionReport);
    verify(_restRequest, times(1)).getHeaders();
    verifyZeroInteractions(_routingResult);
    verifyNoMoreInteractions(_responseHandler, _callback);
    final RestLiServiceException restliEx1 = exFromFilterCapture.getAllValues().get(0);
    assertNotNull(restliEx1);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, restliEx1.getStatus());
    assertEquals(exFromFilter.getMessage(), restliEx1.getMessage());
    assertEquals(exFromFilter, restliEx1.getCause());

    final RestLiServiceException restliEx2 = exFromFilterCapture.getAllValues().get(1);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, restliEx2.getStatus());

    assertNotNull(responseErrorData);
    assertEquals(HttpStatus.S_402_PAYMENT_REQUIRED, responseErrorData.getStatus());
    assertEquals(responseErrorData.getHeaders(), headersFromFilter);
    assertNull(responseErrorData.getRecordResponseEnvelope().getRecord());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testOnSuccessWithFilterThrowable() throws Exception
  {
    // App stuff.
    final RecordTemplate entityFromApp = Foo.createFoo("Key", "Two");
    RequestExecutionReport executionReport = new RequestExecutionReportBuilder().build();
    RestLiResponseEnvelope appResponseData = new RecordResponseEnvelope(HttpStatus.S_200_OK, entityFromApp, Collections.<String, String>emptyMap(),
                                                                        Collections.<HttpCookie>emptyList());
    // Filter suff.
    final Map<String, String> headersFromFilter = Maps.newHashMap();
    headersFromFilter.put("Key", "Error from filter");
    RestLiResponseEnvelope responseErrorData = new RecordResponseEnvelope(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR),
                                                                          headersFromFilter,
                                                                          Collections.<HttpCookie>emptyList());
    PartialRestResponse partialFilterErrorResponse = new PartialRestResponse.Builder().build();
    final Throwable throwableFromFilter = new NoSuchMethodError("Method foo not found!");

    // Common stuff.
    RestException finalRestException = new RestException(new RestResponseBuilder().build());
    // Setup.
    when(_responseHandler.buildRestLiResponseData(_restRequest, _routingResult, entityFromApp)).thenReturn(appResponseData);
    when(_restRequest.getHeaders()).thenReturn(null);
    when(_responseHandler.buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
                                                    any(RestLiServiceException.class), anyMap(), anyList())).thenReturn(
        responseErrorData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseErrorData)).thenReturn(partialFilterErrorResponse);
    when(_responseHandler.buildRestException(any(RestLiServiceException.class), eq(partialFilterErrorResponse))).thenReturn(
        finalRestException);
    // Mock filter behavior.
    doThrow(throwableFromFilter).when(_filter)
        .onResponse(eq(_filterRequestContext), any(FilterResponseContext.class), any(NextResponseFilter.class));

    // Invoke.
    _oneFilterRestLiCallback.onSuccess(entityFromApp, executionReport);

    // Verify.
    verify(_responseHandler).buildRestLiResponseData(_restRequest, _routingResult, entityFromApp);
    verify(_responseHandler).buildPartialResponse(_routingResult, responseErrorData);
    ArgumentCaptor<RestLiServiceException> exFromFilterCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    verify(_responseHandler).buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
                                                        exFromFilterCapture.capture(), anyMap(), anyList());
    verify(_responseHandler).buildPartialResponse(_routingResult, responseErrorData);
    verify(_responseHandler).buildRestException(exFromFilterCapture.capture(), eq(partialFilterErrorResponse));
    verify(_callback).onError(finalRestException, executionReport);
    verify(_restRequest, times(1)).getHeaders();
    verifyZeroInteractions(_routingResult);
    verifyNoMoreInteractions(_responseHandler, _callback);
    final RestLiServiceException restliEx1 = exFromFilterCapture.getAllValues().get(0);
    assertNotNull(restliEx1);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, restliEx1.getStatus());
    assertEquals(throwableFromFilter.getMessage(), restliEx1.getMessage());
    assertEquals(throwableFromFilter, restliEx1.getCause());

    final RestLiServiceException restliEx2 = exFromFilterCapture.getAllValues().get(1);
    assertEquals(responseErrorData.getServiceException(), restliEx2);

    assertNotNull(responseErrorData);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, responseErrorData.getStatus());
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
    RestLiResponseEnvelope appResponseData = new RecordResponseEnvelope(HttpStatus.S_200_OK, null, Collections.<String, String>emptyMap(),
                                                                        Collections.<HttpCookie>emptyList());

    // Filter suff.
    ArgumentCaptor<RestLiServiceException> exFromFilterCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    final Map<String, String> headersFromFilter = Maps.newHashMap();
    headersFromFilter.put("Key", "Error from filter");
    RestLiResponseEnvelope filterResponseData = new RecordResponseEnvelope(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR), headersFromFilter,
                                                                           Collections.<HttpCookie>emptyList());
    PartialRestResponse partialFilterErrorResponse = new PartialRestResponse.Builder().build();
    final Exception exFromFilter = new RuntimeException("Excepiton From Filter");

    // Common stuff.
    RestException finalRestException = new RestException(new RestResponseBuilder().build());
    // Setup.
    when(_responseHandler.buildRestLiResponseData(_restRequest, _routingResult, result)).thenReturn(appResponseData);
    when(_restRequest.getHeaders()).thenReturn(null);
    when(
         _responseHandler.buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
             exFromFilterCapture.capture(), anyMap(), anyList())).thenReturn(filterResponseData);
    when(_responseHandler.buildPartialResponse(_routingResult, filterResponseData)).thenReturn(partialFilterErrorResponse);
    when(_responseHandler.buildRestException(any(RestLiServiceException.class), eq(partialFilterErrorResponse))).thenReturn(finalRestException);
    // Mock filter behavior.
    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterRequestContext requestContext = (FilterRequestContext) args[0];
        FilterResponseContext responseContext = (FilterResponseContext) args[1];
        NextResponseFilter nextResponseFilter = (NextResponseFilter) args[2];
        // The second filter should be invoked with details of the exception thrown by the first
        // filter. Verify incoming data.
        assertEquals(responseContext.getResponseData().getStatus(), HttpStatus.S_200_OK);
        assertNull(responseContext.getResponseData().getRecordResponseEnvelope().getRecord());
        assertTrue(responseContext.getResponseData().getHeaders().isEmpty());
        // Modify data.
        setStatus(responseContext, HttpStatus.S_402_PAYMENT_REQUIRED);
        nextResponseFilter.onResponse(requestContext, responseContext);
        return null;
      }
    }).doThrow(exFromFilter)
        .when(_filter)
        .onResponse(eq(_filterRequestContext), any(FilterResponseContext.class), any(NextResponseFilter.class));

    // Invoke.
    _twoFilterRestLiCallback.onSuccess(result, executionReport);

    // Verify.
    verify(_responseHandler).buildPartialResponse(_routingResult, filterResponseData);
    verify(_responseHandler).buildRestLiResponseData(_restRequest, _routingResult, result);
    verify(_responseHandler).buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
        exFromFilterCapture.capture(), anyMap(), anyList());
    verify(_responseHandler).buildPartialResponse(_routingResult, filterResponseData);
    verify(_responseHandler).buildRestException(exFromFilterCapture.capture(), eq(partialFilterErrorResponse));
    verify(_callback).onError(finalRestException, executionReport);
    verify(_restRequest, times(1)).getHeaders();
    verifyZeroInteractions(_routingResult);
    verifyNoMoreInteractions(_responseHandler, _callback);
    final RestLiServiceException restliEx1 = exFromFilterCapture.getAllValues().get(0);
    assertNotNull(restliEx1);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, restliEx1.getStatus());
    assertEquals(exFromFilter.getMessage(), restliEx1.getMessage());
    assertEquals(exFromFilter, restliEx1.getCause());

    final RestLiServiceException restliEx2 = exFromFilterCapture.getAllValues().get(1);
    assertNotNull(restliEx2);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, restliEx2.getStatus());

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

    RestLiResponseEnvelope responseData = new RecordResponseEnvelope(appException, headersFromApp,
                                                                     Collections.<HttpCookie>emptyList());
    PartialRestResponse partialResponse = new PartialRestResponse.Builder().build();
    when(_responseHandler.buildExceptionResponseData(eq(_restRequest), eq(_routingResult), any(RestLiServiceException.class),
             anyMap(), anyList())).thenReturn(responseData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseData)).thenReturn(partialResponse);

    // Mock the behavior of the first filter.
    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterRequestContext requestContext = (FilterRequestContext) args[0];
        FilterResponseContext responseContext = (FilterResponseContext) args[1];
        NextResponseFilter nextResponseFilter = (NextResponseFilter) args[2];

        // Verify incoming data.
        assertEquals(HttpStatus.S_404_NOT_FOUND, responseContext.getResponseData().getStatus());
        assertEquals(headersFromApp, responseContext.getResponseData().getHeaders());
        assertNull(responseContext.getResponseData().getRecordResponseEnvelope().getRecord());
        // Modify data in filter.
        setStatus(responseContext, HttpStatus.S_400_BAD_REQUEST);
        responseContext.getResponseData().getHeaders().clear();
        nextResponseFilter.onResponse(requestContext, responseContext);
        return null;
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
        NextResponseFilter nextResponseFilter = (NextResponseFilter) args[2];

        // Verify incoming data.
        assertEquals(HttpStatus.S_400_BAD_REQUEST, responseContext.getResponseData().getStatus());
        assertTrue(responseContext.getResponseData().getHeaders().isEmpty());
        assertNull(responseContext.getResponseData().getRecordResponseEnvelope().getRecord());
        // Modify data in filter.
        setStatus(responseContext, HttpStatus.S_403_FORBIDDEN);
        responseContext.getResponseData().getHeaders().putAll(headersFromFilter);
        nextResponseFilter.onResponse(requestContext, responseContext);
        return null;
      }
    }).when(_filter).onResponse(eq(_filterRequestContext),
                                any(FilterResponseContext.class),
                                any(NextResponseFilter.class));
    RestException restException = new RestException(new RestResponseBuilder().build());
    when(_responseHandler.buildRestException(any(RestLiServiceException.class), eq(partialResponse))).thenReturn(
        restException);
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
    ArgumentCaptor<RestLiServiceException> exCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    verify(_responseHandler).buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
        exCapture.capture(), anyMap(), anyList());
    verify(_responseHandler).buildPartialResponse(_routingResult, responseData);
    verify(_responseHandler).buildRestException(exCapture.capture(), eq(partialResponse));
    verify(_callback).onError(restException, executionReport);
    verify(_restRequest, times(1)).getHeaders();
    verifyZeroInteractions(_routingResult);
    verifyNoMoreInteractions(_restRequest, _responseHandler, _callback);
    final RestLiServiceException restliEx1 = exCapture.getAllValues().get(0);
    assertNotNull(restliEx1);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, restliEx1.getStatus());
    assertEquals(exFromApp.getMessage(), restliEx1.getMessage());
    assertEquals(exFromApp, restliEx1.getCause());

    final RestLiServiceException restliEx2 = exCapture.getAllValues().get(1);
    assertNotNull(restliEx2);
    assertEquals(HttpStatus.S_403_FORBIDDEN, restliEx2.getStatus());
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

    RestLiResponseEnvelope responseData = new RecordResponseEnvelope(appException, headersFromApp,
                                                                     Collections.<HttpCookie>emptyList());

    PartialRestResponse partialResponse = new PartialRestResponse.Builder().build();
    ArgumentCaptor<RestLiServiceException> exCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    when(
         _responseHandler.buildExceptionResponseData(eq(_restRequest), eq(_routingResult), exCapture.capture(),
             anyMap(), anyList())).thenReturn(responseData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseData)).thenReturn(partialResponse);

    // Mock the behavior of the first filter.
    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterRequestContext requestContext = (FilterRequestContext) args[0];
        FilterResponseContext responseContext = (FilterResponseContext) args[1];
        NextResponseFilter nextResponseFilter = (NextResponseFilter) args[2];

        // Verify incoming data.
        assertEquals(HttpStatus.S_404_NOT_FOUND, responseContext.getResponseData().getStatus());
        assertEquals(headersFromApp, responseContext.getResponseData().getHeaders());
        assertNull(responseContext.getResponseData().getRecordResponseEnvelope().getRecord());
        // Modify data in filter.
        setStatus(responseContext, HttpStatus.S_400_BAD_REQUEST);
        responseContext.getResponseData().getHeaders().clear();
        nextResponseFilter.onResponse(requestContext, responseContext);
        return null;
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
        NextResponseFilter nextResponseFilter = (NextResponseFilter) args[2];

        // Verify incoming data.
        assertEquals(HttpStatus.S_400_BAD_REQUEST, responseContext.getResponseData().getStatus());
        assertTrue(responseContext.getResponseData().getHeaders().isEmpty());
        assertNull(responseContext.getResponseData().getRecordResponseEnvelope().getRecord());
        // Modify data in filter.
        setStatus(responseContext, HttpStatus.S_403_FORBIDDEN);
        responseContext.getResponseData().getRecordResponseEnvelope().setRecord(entityFromFilter, HttpStatus.S_403_FORBIDDEN);
        responseContext.getResponseData().getHeaders().putAll(headersFromFilter);
        nextResponseFilter.onResponse(requestContext, responseContext);
        return null;
      }
    }).when(_filter).onResponse(eq(_filterRequestContext), any(FilterResponseContext.class), any(NextResponseFilter.class));

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
        exCapture.capture(), anyMap(), anyList());
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
    RestLiResponseEnvelope responseAppData = new RecordResponseEnvelope(exFromApp, Collections.<String, String>emptyMap(),
                                                                        Collections.<HttpCookie>emptyList());

    // Filter stuff.
    final Exception exFromFirstFilter = new RuntimeException("Runtime exception from first filter");
    RestLiServiceException filterException = new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR);
    RestLiResponseEnvelope responseFilterData = new RecordResponseEnvelope(filterException, Collections.<String, String>emptyMap(),
                                                                           Collections.<HttpCookie>emptyList());

    PartialRestResponse partialResponse = new PartialRestResponse.Builder().build();
    RestException restException = new RestException(new RestResponseBuilder().build());

    // Setup.
    when(_responseHandler.buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
             any(RestLiServiceException.class), anyMap(), anyList())).thenReturn(responseAppData)
        .thenReturn(responseFilterData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseFilterData)).thenReturn(partialResponse);
    when(_restRequest.getHeaders()).thenReturn(null);
    when(_responseHandler.buildRestException(any(Exception.class), eq(partialResponse))).thenReturn(restException);

    // Mock filter behavior.
    doThrow(exFromFirstFilter).doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterRequestContext requestContext = (FilterRequestContext) args[0];
        FilterResponseContext responseContext = (FilterResponseContext) args[1];
        NextResponseFilter nextResponseFilter = (NextResponseFilter) args[2];

        // The second filter should be invoked with details of the exception thrown by the first
        // filter. Verify incoming data.
        assertEquals(responseContext.getResponseData().getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);
        assertNull(responseContext.getResponseData().getRecordResponseEnvelope().getRecord());
        assertTrue(responseContext.getResponseData().getHeaders().isEmpty());
        assertTrue(responseContext.getResponseData().isErrorResponse());

        // Modify data.
        setStatus(responseContext, HttpStatus.S_402_PAYMENT_REQUIRED);
        // The second filter does not handle the exception thrown by the first filter (i.e.) the
        // response data still has the error response corresponding to the exception from the first
        // filter.
        nextResponseFilter.onResponse(requestContext, responseContext);
        return null;
      }
    }).when(_filter).onResponse(eq(_filterRequestContext), any(FilterResponseContext.class), any(NextResponseFilter.class));

    // Invoke.
    _twoFilterRestLiCallback.onError(exFromApp, executionReport);

    // Verify.
    ArgumentCaptor<RestLiServiceException> exCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    verify(_responseHandler, times(2)).buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
        exCapture.capture(), anyMap(), anyList());
    verify(_responseHandler).buildRestException(exCapture.capture(), eq(partialResponse));
    assertEquals(exCapture.getValue().getStatus(), HttpStatus.S_402_PAYMENT_REQUIRED);
    verify(_responseHandler).buildPartialResponse(_routingResult, responseFilterData);
    verify(_callback).onError(restException, executionReport);
    verify(_restRequest, times(2)).getHeaders();
    verifyZeroInteractions(_routingResult);
    verifyNoMoreInteractions(_restRequest, _responseHandler, _callback);
    assertNotNull(responseFilterData);
    assertEquals(HttpStatus.S_402_PAYMENT_REQUIRED, responseFilterData.getStatus());
    assertTrue(responseFilterData.getHeaders().isEmpty());
    assertNull(responseFilterData.getRecordResponseEnvelope().getRecord());
    RestLiServiceException restliEx = exCapture.getAllValues().get(0);
    assertNotNull(restliEx);
    assertEquals(exFromApp.getStatus(), restliEx.getStatus());
    assertEquals(exFromApp.getMessage(), restliEx.getMessage());
    restliEx = exCapture.getAllValues().get(1);
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
        responseAppData = new RecordResponseEnvelope(appException, Collections.<String, String>emptyMap(),
                                                     Collections.<HttpCookie>emptyList());
        break;
      case GET_COLLECTION:
        responseAppData = new CollectionResponseEnvelope(appException, Collections.<String, String>emptyMap(),
                                                         Collections.<HttpCookie>emptyList());
        break;
      case CREATE_COLLECTION:
        responseAppData = new CreateCollectionResponseEnvelope(appException, Collections.<String, String>emptyMap(),
                                                               Collections.<HttpCookie>emptyList());
        break;
      case BATCH_ENTITIES:
        responseAppData = new BatchResponseEnvelope(appException, Collections.<String, String>emptyMap(),
                                                    Collections.<HttpCookie>emptyList());
        break;
      case STATUS_ONLY:
        responseAppData = new EmptyResponseEnvelope(appException, Collections.<String, String>emptyMap(), Collections.<HttpCookie>emptyList());
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
        responseFilterData = new RecordResponseEnvelope(filterException, headersFromFilter,
                                                        Collections.<HttpCookie>emptyList());
        break;
      case GET_COLLECTION:
        responseFilterData = new CollectionResponseEnvelope(filterException, headersFromFilter,
                                                            Collections.<HttpCookie>emptyList());
        break;
      case CREATE_COLLECTION:
        responseFilterData = new CreateCollectionResponseEnvelope(filterException, headersFromFilter,
                                                                  Collections.<HttpCookie>emptyList());
        break;
      case BATCH_ENTITIES:
        responseFilterData = new BatchResponseEnvelope(filterException, headersFromFilter,
                                                       Collections.<HttpCookie>emptyList());
        break;
      case STATUS_ONLY:
        responseFilterData = new EmptyResponseEnvelope(filterException, headersFromFilter, Collections.<HttpCookie>emptyList());
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
    when(_responseHandler.buildExceptionResponseData(eq(_restRequest),
                                                     eq(_routingResult),
                                                     wrappedExCapture.capture(),
                                                     anyMap(),
                                                     anyList())).thenReturn(responseAppData)
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
        FilterRequestContext requestContext = (FilterRequestContext) args[0];
        FilterResponseContext responseContext = (FilterResponseContext) args[1];
        NextResponseFilter nextResponseFilter = (NextResponseFilter) args[2];

        // The second filter should be invoked with details of the exception thrown by the first
        // filter. Verify incoming data.
        assertEquals(responseContext.getResponseData().getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);

        switch (ResponseType.fromMethodType(resourceMethod))
        {
          case SINGLE_ENTITY:
            assertNull(responseContext.getResponseData().getRecordResponseEnvelope().getRecord());
            break;
          case GET_COLLECTION:
            assertNull(responseContext.getResponseData().getCollectionResponseEnvelope().getCollectionResponse());
            break;
          case CREATE_COLLECTION:
            assertNull(responseContext.getResponseData().getCreateCollectionResponseEnvelope().getCreateResponses());
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
          responseContext.getResponseData().getCollectionResponseEnvelope().setCollectionResponse(HttpStatus.S_402_PAYMENT_REQUIRED,
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

          responseContext.getResponseData().getBatchResponseEnvelope().setBatchResponseMap(HttpStatus.S_402_PAYMENT_REQUIRED,
              responseMap);
        }
        nextResponseFilter.onResponse(requestContext, responseContext);
        return null;
      }
    }).when(_filter).onResponse(eq(_filterRequestContext), any(FilterResponseContext.class), any(NextResponseFilter.class));

    // Invoke.
    _twoFilterRestLiCallback.onError(exFromApp, executionReport);

    // Verify.
    verify(_responseHandler, times(2)).buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
        wrappedExCapture.capture(), anyMap(), anyList());
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
    RestLiResponseEnvelope responseAppData = new RecordResponseEnvelope(exFromApp, Collections.<String, String>emptyMap(),
                                                                        Collections.<HttpCookie>emptyList());

    // Filter stuff.
    final Exception exFromSecondFilter = new RuntimeException("Runtime exception from second filter");
    RestLiResponseEnvelope responseFilterData = new RecordResponseEnvelope(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                                                                                                      exFromSecondFilter),
                                                                           Collections.<String, String>emptyMap(),
                                                                           Collections.<HttpCookie>emptyList());
    PartialRestResponse partialResponse = new PartialRestResponse.Builder().build();
    RestException restException = new RestException(new RestResponseBuilder().build());
    // Setup.
    when(_responseHandler.buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
             any(RestLiServiceException.class), anyMap(), anyList())).thenReturn(responseAppData)
                                                                                       .thenReturn(responseFilterData);
    when(_responseHandler.buildPartialResponse(_routingResult, responseFilterData)).thenReturn(partialResponse);
    when(_restRequest.getHeaders()).thenReturn(null);
    when(_responseHandler.buildRestException(any(RestLiServiceException.class), eq(partialResponse))).thenReturn(
        restException);

    // Mock filter behavior.
    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        FilterRequestContext requestContext = (FilterRequestContext) args[0];
        FilterResponseContext responseContext = (FilterResponseContext) args[1];
        NextResponseFilter nextResponseFilter = (NextResponseFilter) args[2];

        assertEquals(responseContext.getResponseData().getStatus(), HttpStatus.S_404_NOT_FOUND);
        assertNull(responseContext.getResponseData().getRecordResponseEnvelope().getRecord());
        assertTrue(responseContext.getResponseData().getHeaders().isEmpty());

        // Modify data.
        setStatus(responseContext, HttpStatus.S_402_PAYMENT_REQUIRED);
        nextResponseFilter.onResponse(requestContext, responseContext);
        return null;
      }
    }).doThrow(exFromSecondFilter)
        .when(_filter)
        .onResponse(eq(_filterRequestContext), any(FilterResponseContext.class), any(NextResponseFilter.class));

    // Invoke.
    _twoFilterRestLiCallback.onError(exFromApp, executionReport);

    // Verify.
    ArgumentCaptor<RestLiServiceException> wrappedExCapture = ArgumentCaptor.forClass(RestLiServiceException.class);
    verify(_responseHandler, times(2)).buildExceptionResponseData(eq(_restRequest), eq(_routingResult),
        wrappedExCapture.capture(), anyMap(), anyList());
    verify(_responseHandler).buildPartialResponse(_routingResult, responseFilterData);
    verify(_responseHandler).buildRestException(wrappedExCapture.capture(), eq(partialResponse));
    verify(_callback).onError(restException, executionReport);
    verify(_restRequest, times(2)).getHeaders();
    verifyZeroInteractions(_routingResult);
    verifyNoMoreInteractions(_restRequest, _responseHandler, _callback);
    assertNotNull(responseFilterData);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, responseFilterData.getStatus());
    assertTrue(responseFilterData.getHeaders().isEmpty());
    assertNull(responseFilterData.getRecordResponseEnvelope().getRecord());

    final RestLiServiceException restliEx1 = wrappedExCapture.getAllValues().get(0);
    assertEquals(exFromApp, restliEx1);

    final RestLiServiceException restliEx2 = wrappedExCapture.getAllValues().get(1);
    assertNotNull(restliEx2);
    assertEquals(HttpStatus.S_500_INTERNAL_SERVER_ERROR, restliEx2.getStatus());
    assertEquals(exFromSecondFilter.getMessage(), restliEx2.getMessage());
    assertEquals(exFromSecondFilter, restliEx2.getCause());

    final RestLiServiceException restliEx3 = wrappedExCapture.getAllValues().get(2);
    assertEquals(responseFilterData.getServiceException(), restliEx3);
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
