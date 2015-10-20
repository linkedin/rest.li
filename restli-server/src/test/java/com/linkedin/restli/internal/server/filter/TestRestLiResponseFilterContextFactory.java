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

package com.linkedin.restli.internal.server.filter;


import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.internal.server.RestLiResponseEnvelope;
import com.linkedin.restli.internal.server.RestLiResponseHandler;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.response.RecordResponseEnvelope;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.filter.FilterResponseContext;

import java.net.HttpCookie;
import java.util.Collections;
import java.util.Map;

import com.google.common.collect.Maps;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


/**
 * @author nshankar
 */
public class TestRestLiResponseFilterContextFactory
{
  @Mock
  private RestRequest _restRequest;
  @Mock
  private RoutingResult _routingResult;
  @Mock
  private RestLiResponseHandler _responseHandler;
  private RestLiResponseFilterContextFactory<Object> _responseFilterContextFactory;

  @BeforeTest
  protected void setUp() throws Exception
  {
    MockitoAnnotations.initMocks(this);
    _responseFilterContextFactory = new RestLiResponseFilterContextFactory<Object>(_restRequest,
                                                                                   _routingResult,
                                                                                   _responseHandler);
  }

  @BeforeMethod
  protected void resetMocks()
  {
    reset(_restRequest, _routingResult, _responseHandler);
  }

  @AfterMethod
  protected void verifyMocks()
  {
    verifyNoMoreInteractions(_restRequest, _routingResult, _responseHandler);
  }

  @Test
  public void testFromResult() throws Exception
  {
    DataMap dataMap = new DataMap();
    dataMap.put("foo", "bar");
    Map<String, String> headers = Maps.newHashMap();
    headers.put("x", "y");
    RecordTemplate entity1 = new Foo(dataMap);

    RestLiResponseEnvelope responseData = new RecordResponseEnvelope(HttpStatus.S_200_OK,
                                                                     entity1,
                                                                     headers,
                                                                     Collections.<HttpCookie>emptyList());
    when(_responseHandler.buildRestLiResponseData(_restRequest, _routingResult, entity1)).thenReturn(responseData);

    FilterResponseContext responseContext = _responseFilterContextFactory.fromResult(entity1);
    assertEquals(responseContext.getResponseData(), responseData);
    verify(_responseHandler).buildRestLiResponseData(_restRequest, _routingResult, entity1);
  }

  @DataProvider(name = "provideExceptionsAndStatuses")
  private Object[][] provideExceptionsAndStatuses()
  {
    return new Object[][]{{new RuntimeException("Test runtime exception"), HttpStatus.S_500_INTERNAL_SERVER_ERROR},
        {new RoutingException("Test routing exception", 404), HttpStatus.S_404_NOT_FOUND},
        {new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, "Test service exception"),
            HttpStatus.S_400_BAD_REQUEST}, {new RestLiServiceException(HttpStatus.S_403_FORBIDDEN,
                                                                       "Wrapped runtime exception with custom status",
                                                                       new RuntimeException("Original cause")),
        HttpStatus.S_403_FORBIDDEN}};
  }

  @SuppressWarnings("unchecked")
  @Test(dataProvider = "provideExceptionsAndStatuses")
  public void testFromThrowable(Exception e, HttpStatus status)
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
    RestLiResponseEnvelope responseData = new RecordResponseEnvelope(serviceException,
                                                                     Collections.<String, String>emptyMap(),
                                                                     Collections.<HttpCookie>emptyList());
    ArgumentCaptor<RestLiServiceException> exceptionArgumentCaptor = ArgumentCaptor.forClass(RestLiServiceException.class);

    // Setup.
    when(_responseHandler.buildExceptionResponseData(eq(_restRequest),
                                                     eq(_routingResult),
                                                     exceptionArgumentCaptor.capture(),
                                                     anyMap(),
                                                     anyList())).thenReturn(responseData);
    when(_restRequest.getHeaders()).thenReturn(null);

    // Invoke.
    FilterResponseContext responseContext = _responseFilterContextFactory.fromThrowable(e);

    // Verify.
    verify(_responseHandler).buildExceptionResponseData(eq(_restRequest),
                                                        eq(_routingResult),
                                                        exceptionArgumentCaptor.capture(),
                                                        anyMap(),
                                                        anyList());
    verify(_restRequest).getHeaders();
    // RestLiCallback should pass the original exception to the response handler.
    RestLiServiceException exceptionArgument = exceptionArgumentCaptor.getValue();
    assertTrue(exceptionArgument.equals(e) || exceptionArgument.getCause().equals(e));
    assertEquals(exceptionArgument.getStatus(), status);
    // The end result should also contain the original exception.
    assertTrue(responseContext.getResponseData().isErrorResponse());
    assertTrue(responseContext.getResponseData().getServiceException().equals(e) || responseContext.getResponseData()
                                                                                                   .getServiceException()
                                                                                                   .getCause()
                                                                                                   .equals(e));
    assertEquals(responseContext.getResponseData().getServiceException().getStatus(), status);
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
