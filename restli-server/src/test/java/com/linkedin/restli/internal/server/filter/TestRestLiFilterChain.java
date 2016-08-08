package com.linkedin.restli.internal.server.filter;

import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.attachments.RestLiAttachmentReader;
import com.linkedin.restli.internal.server.RestLiCallback;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.filter.testfilters.CountFilter;
import com.linkedin.restli.internal.server.filter.testfilters.CountFilterRequestErrorOnError;
import com.linkedin.restli.internal.server.filter.testfilters.CountFilterRequestErrorThrowsError;
import com.linkedin.restli.internal.server.filter.testfilters.CountFilterRequestOnError;
import com.linkedin.restli.internal.server.filter.testfilters.CountFilterRequestThrowsError;
import com.linkedin.restli.internal.server.filter.testfilters.CountFilterResponseErrorFixesError;
import com.linkedin.restli.internal.server.filter.testfilters.CountFilterResponseErrorOnError;
import com.linkedin.restli.internal.server.filter.testfilters.CountFilterResponseErrorThrowsError;
import com.linkedin.restli.internal.server.filter.testfilters.CountFilterResponseOnError;
import com.linkedin.restli.internal.server.filter.testfilters.CountFilterResponseThrowsError;
import com.linkedin.restli.internal.server.filter.testfilters.TestFilterException;
import com.linkedin.restli.internal.server.response.RestLiResponseDataImpl;
import com.linkedin.restli.internal.server.response.RestLiResponseHandler;
import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.RestLiResponseAttachments;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.FilterResponseContext;
import java.util.Arrays;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;


/**
 * Tests the propagation of the RestLiFilterChain
 * Test will call the filter chain with and without errors. Tests include filters which correct, propagate, and create
 * new errors. Based on the expected behavior, the tests use the number of times each method was invoked as the
 * determining factor as to whether the chain is propagating correctly or not
 *
 * @author gye
 */
public class TestRestLiFilterChain
{
  @Mock
  private RestLiRequestData _mockRestLiRequestData;
  @Mock
  private FilterChainCallback _mockFilterChainCallback;
  @Mock
  private RestLiResponseDataImpl _mockRestLiResponseData;
  @Mock
  private FilterRequestContext _mockFilterRequestContext;
  @Mock
  private FilterResponseContext _mockFilterResponseContext;
  @Mock
  private RestLiAttachmentReader _mockRequestAttachmentReader;
  @Mock
  private RestLiResponseAttachments _mockResponseAttachments;
  @Mock
  private RestLiResponseFilterContextFactory<Object> _mockResponseFilterContextFactory;
  @Mock
  private RestLiCallback<Object> _mockRestLiCallback;

  @Mock
  private RestRequest _request;
  @Mock
  private RoutingResult _method;
  @Mock
  private RestLiResponseHandler _responseHandler;

  private CountFilter[] _filters;
  private RestLiFilterChain _restLiFilterChain;

  @BeforeClass
  protected void setUp()
  {
    MockitoAnnotations.initMocks(this);
  }

  @BeforeMethod
  protected void init()
  {
    _filters = new CountFilter[] {
        new CountFilter(),
        new CountFilter(),
        new CountFilter()
    };
  }

  @AfterMethod
  protected void resetMocks()
  {
    reset(_mockFilterChainCallback, _mockFilterRequestContext, _mockFilterResponseContext, _mockRestLiRequestData,
          _mockRestLiResponseData);
    _filters = new CountFilter[] {
        new CountFilter(),
        new CountFilter(),
        new CountFilter()
    };
  }

  @Test
  public void testFilterInvocationSuccess() throws Exception
  {
    _restLiFilterChain = new RestLiFilterChain(Arrays.asList(_filters), _mockFilterChainCallback);
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        _restLiFilterChain.onResponse(_mockFilterRequestContext, _mockFilterResponseContext, _mockResponseAttachments);
        return null;
      }
    }).when(_mockFilterChainCallback).onRequestSuccess(_mockRestLiRequestData, _mockRestLiCallback);

    when(_mockFilterRequestContext.getRequestData()).thenReturn(_mockRestLiRequestData);
    when(_mockFilterResponseContext.getResponseData()).thenReturn(_mockRestLiResponseData);
    when(_mockResponseFilterContextFactory.fromThrowable(any(Throwable.class))).thenReturn(_mockFilterResponseContext);

    _restLiFilterChain.onRequest(_mockFilterRequestContext, _mockResponseFilterContextFactory, _mockRestLiCallback);

    for(CountFilter filter : _filters)
    {
      assertEquals(filter.getNumRequests(), 1);
      assertEquals(filter.getNumResponses(), 1);
      assertEquals(filter.getNumErrors(), 0);
    }

    verify(_mockFilterRequestContext).getRequestData();
    verify(_mockFilterChainCallback).onRequestSuccess(_mockRestLiRequestData, _mockRestLiCallback);
    verify(_mockFilterChainCallback).onResponseSuccess(_mockRestLiResponseData, _mockResponseAttachments);

    verifyNoMoreInteractions(_mockFilterChainCallback, _mockRestLiCallback,
                             _mockFilterRequestContext,
                             _mockRestLiRequestData);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testFilterInvocationRequestOnError() throws Exception
  {
    _restLiFilterChain = new RestLiFilterChain(Arrays.asList(_filters), _mockFilterChainCallback);
    _filters[1] = new CountFilterRequestOnError();
    when(_responseHandler.buildExceptionResponseData(eq(_request), eq(_method), any(Object.class), anyMap(), anyList()))
        .thenReturn(_mockRestLiResponseData);
    when(_mockResponseFilterContextFactory.fromThrowable(any(Throwable.class))).thenReturn(_mockFilterResponseContext);
    when(_mockFilterResponseContext.getResponseData()).thenReturn(_mockRestLiResponseData);

    _restLiFilterChain.onRequest(_mockFilterRequestContext,
                                 new RestLiResponseFilterContextFactory<Object>(_request, _method, _responseHandler),
                                 _mockRestLiCallback);

    verifySecondFilterRequestException();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testFilterInvocationRequestErrorOnError() throws Exception
  {
    _restLiFilterChain = new RestLiFilterChain(Arrays.asList(_filters), _mockFilterChainCallback);
    _filters[1] = new CountFilterRequestErrorOnError();
    when(_responseHandler.buildExceptionResponseData(eq(_request), eq(_method), any(Object.class), anyMap(), anyList()))
        .thenReturn(_mockRestLiResponseData);
    _restLiFilterChain.onRequest(_mockFilterRequestContext,
                                 new RestLiResponseFilterContextFactory<Object>(_request, _method, _responseHandler),
                                 _mockRestLiCallback);

    verifySecondFilterRequestException();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testFilterInvocationRequestThrowsError() throws Exception
  {
    _restLiFilterChain = new RestLiFilterChain(Arrays.asList(_filters), _mockFilterChainCallback);
    _filters[1] = new CountFilterRequestThrowsError();
    when(_responseHandler.buildExceptionResponseData(eq(_request), eq(_method), any(Object.class), anyMap(), anyList()))
        .thenReturn(_mockRestLiResponseData);
    _restLiFilterChain.onRequest(_mockFilterRequestContext,
                                 new RestLiResponseFilterContextFactory<Object>(_request, _method, _responseHandler),
                                 _mockRestLiCallback);

    verifySecondFilterRequestException();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testFilterInvocationRequestErrorThrowsError() throws Exception
  {
    _restLiFilterChain = new RestLiFilterChain(Arrays.asList(_filters), _mockFilterChainCallback);
    _filters[1] = new CountFilterRequestErrorThrowsError();
    when(_responseHandler.buildExceptionResponseData(eq(_request), eq(_method), any(Object.class), anyMap(), anyList()))
        .thenReturn(_mockRestLiResponseData);
    _restLiFilterChain.onRequest(_mockFilterRequestContext,
                                 new RestLiResponseFilterContextFactory<Object>(_request, _method, _responseHandler),
                                 _mockRestLiCallback);

    verifySecondFilterRequestException();
  }

  private void verifySecondFilterRequestException()
  {
    assertFilterCounts(_filters[0], 1, 0, 1);
    assertFilterCounts(_filters[1], 1, 0, 1);
    assertFilterCounts(_filters[2], 0, 0, 0);

    verify(_mockFilterChainCallback).onError(any(TestFilterException.class), any(RestLiResponseData.class),
                                             isNull(RestLiResponseAttachments.class));

    verify(_mockRestLiResponseData, times(2)).setException(any(Throwable.class));

    verifyNoMoreInteractions(_mockFilterChainCallback, _mockRestLiCallback,
                             _mockFilterRequestContext,
                             _mockRestLiRequestData);
  }


  @Test
  public void testFilterInvocationResponseOnError() throws Exception
  {
    _restLiFilterChain = new RestLiFilterChain(Arrays.asList(_filters), _mockFilterChainCallback);
    _filters[1] = new CountFilterResponseOnError();

    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        _restLiFilterChain.onResponse(_mockFilterRequestContext, _mockFilterResponseContext, _mockResponseAttachments);
        return null;
      }
    }).when(_mockFilterChainCallback).onRequestSuccess(_mockRestLiRequestData, _mockRestLiCallback);

    when(_mockFilterRequestContext.getRequestData()).thenReturn(_mockRestLiRequestData);
    when(_mockFilterResponseContext.getResponseData()).thenReturn(_mockRestLiResponseData);
    when(_mockResponseFilterContextFactory.fromThrowable(any(Throwable.class))).thenReturn(_mockFilterResponseContext);

    _restLiFilterChain.onRequest(_mockFilterRequestContext, _mockResponseFilterContextFactory, _mockRestLiCallback);

    verifySecondFilterResponseException();

  }

  @SuppressWarnings("unchecked")
  @Test
  public void testFilterInvocationResponseErrorOnError() throws Exception
  {
    _restLiFilterChain = new RestLiFilterChain(Arrays.asList(_filters), _mockFilterChainCallback);
    _filters[1] = new CountFilterResponseErrorOnError();

    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        _restLiFilterChain.onResponse(_mockFilterRequestContext, _mockFilterResponseContext, _mockResponseAttachments);
        return null;
      }
    }).when(_mockFilterChainCallback).onRequestSuccess(_mockRestLiRequestData, _mockRestLiCallback);

    when(_mockFilterRequestContext.getRequestData()).thenReturn(_mockRestLiRequestData);
    when(_mockFilterResponseContext.getResponseData()).thenReturn(_mockRestLiResponseData);
    when(_responseHandler.buildExceptionResponseData(eq(_request), eq(_method), any(Object.class), anyMap(), anyList()))
        .thenReturn(_mockRestLiResponseData);
    when(_mockResponseFilterContextFactory.fromThrowable(any(Throwable.class))).thenReturn(_mockFilterResponseContext);

    _restLiFilterChain.onRequest(_mockFilterRequestContext, _mockResponseFilterContextFactory, _mockRestLiCallback);

    verifySecondFilterResponseException();
  }

  @Test
  public void testFilterInvocationResponseThrowsError() throws Exception
  {
    _restLiFilterChain = new RestLiFilterChain(Arrays.asList(_filters), _mockFilterChainCallback);
    _filters[1] = new CountFilterResponseThrowsError();

    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        _restLiFilterChain.onResponse(_mockFilterRequestContext, _mockFilterResponseContext, _mockResponseAttachments);
        return null;
      }
    }).when(_mockFilterChainCallback).onRequestSuccess(_mockRestLiRequestData, _mockRestLiCallback);

    when(_mockFilterRequestContext.getRequestData()).thenReturn(_mockRestLiRequestData);
    when(_mockFilterResponseContext.getResponseData()).thenReturn(_mockRestLiResponseData);
    when(_mockResponseFilterContextFactory.fromThrowable(any(Throwable.class))).thenReturn(_mockFilterResponseContext);

    _restLiFilterChain.onRequest(_mockFilterRequestContext, _mockResponseFilterContextFactory, _mockRestLiCallback);

    verifySecondFilterResponseException();
  }

  @Test
  public void testFilterInvocationResponseErrorThrowsError() throws Exception
  {
    _restLiFilterChain = new RestLiFilterChain(Arrays.asList(_filters), _mockFilterChainCallback);
    _filters[1] = new CountFilterResponseErrorThrowsError();

    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        _restLiFilterChain.onResponse(_mockFilterRequestContext, _mockFilterResponseContext, _mockResponseAttachments);
        return null;
      }
    }).when(_mockFilterChainCallback).onRequestSuccess(_mockRestLiRequestData, _mockRestLiCallback);

    when(_mockFilterRequestContext.getRequestData()).thenReturn(_mockRestLiRequestData);
    when(_mockFilterResponseContext.getResponseData()).thenReturn(_mockRestLiResponseData);
    when(_mockResponseFilterContextFactory.fromThrowable(any(Throwable.class))).thenReturn(_mockFilterResponseContext);

    _restLiFilterChain.onRequest(_mockFilterRequestContext, _mockResponseFilterContextFactory, _mockRestLiCallback);

    verifySecondFilterResponseException();
  }

  private void verifySecondFilterResponseException()
  {
    assertFilterCounts(_filters[0], 1, 0, 1);
    assertFilterCounts(_filters[1], 1, 1, 0);
    assertFilterCounts(_filters[2], 1, 1, 0);

    verify(_mockFilterChainCallback).onRequestSuccess(_mockRestLiRequestData, _mockRestLiCallback);
    verify(_mockFilterChainCallback).onError(any(TestFilterException.class), eq(_mockRestLiResponseData),
                                             eq(_mockResponseAttachments));
    verify(_mockFilterRequestContext).getRequestData();
    verify(_mockFilterResponseContext, times(5)).getResponseData();

    verifyNoMoreInteractions(_mockFilterChainCallback, _mockRestLiCallback,
                             _mockFilterRequestContext,
                             _mockRestLiRequestData);
  }

  @Test
  public void testFilterInvocationResponseErrorFixesError() throws Exception
  {
    _restLiFilterChain = new RestLiFilterChain(Arrays.asList(_filters), _mockFilterChainCallback);
    _filters[1] = new CountFilterResponseErrorFixesError();
    _filters[2] = new CountFilterResponseErrorOnError();

    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        _restLiFilterChain.onResponse(_mockFilterRequestContext, _mockFilterResponseContext, _mockResponseAttachments);
        return null;
      }
    }).when(_mockFilterChainCallback).onRequestSuccess(_mockRestLiRequestData, _mockRestLiCallback);

    when(_mockFilterRequestContext.getRequestData()).thenReturn(_mockRestLiRequestData);
    when(_mockFilterResponseContext.getResponseData()).thenReturn(_mockRestLiResponseData);

    _restLiFilterChain.onRequest(_mockFilterRequestContext, _mockResponseFilterContextFactory, _mockRestLiCallback);

    assertFilterCounts(_filters[0], 1, 1, 0);
    assertFilterCounts(_filters[1], 1, 0, 1);
    assertFilterCounts(_filters[2], 1, 1, 0);

    verify(_mockFilterChainCallback).onRequestSuccess(_mockRestLiRequestData, _mockRestLiCallback);
    verify(_mockFilterChainCallback).onResponseSuccess(eq(_mockRestLiResponseData), eq(_mockResponseAttachments));
    verify(_mockFilterRequestContext).getRequestData();
    verify(_mockFilterResponseContext, times(3)).getResponseData();

    verifyNoMoreInteractions(_mockFilterChainCallback, _mockRestLiCallback,
                             _mockFilterRequestContext,
                             _mockRestLiRequestData);
  }

  @Test
  public void testFilterInvocationLastResponseErrorFixesError() throws Exception
  {
    _restLiFilterChain = new RestLiFilterChain(Arrays.asList(_filters), _mockFilterChainCallback);
    _filters[0] = new CountFilterResponseErrorFixesError();
    _filters[1] = new CountFilterResponseErrorOnError();

    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        _restLiFilterChain.onResponse(_mockFilterRequestContext, _mockFilterResponseContext, _mockResponseAttachments);
        return null;
      }
    }).when(_mockFilterChainCallback).onRequestSuccess(_mockRestLiRequestData, _mockRestLiCallback);

    when(_mockFilterRequestContext.getRequestData()).thenReturn(_mockRestLiRequestData);
    when(_mockFilterResponseContext.getResponseData()).thenReturn(_mockRestLiResponseData);

    _restLiFilterChain.onRequest(_mockFilterRequestContext, _mockResponseFilterContextFactory, _mockRestLiCallback);

    assertFilterCounts(_filters[0], 1, 0, 1);
    assertFilterCounts(_filters[1], 1, 1, 0);
    assertFilterCounts(_filters[2], 1, 1, 0);

    verify(_mockFilterChainCallback).onRequestSuccess(_mockRestLiRequestData, _mockRestLiCallback);
    verify(_mockFilterChainCallback).onResponseSuccess(eq(_mockRestLiResponseData), eq(_mockResponseAttachments));
    verify(_mockFilterRequestContext).getRequestData();
    verify(_mockFilterResponseContext, times(3)).getResponseData();

    verifyNoMoreInteractions(_mockFilterChainCallback, _mockRestLiCallback,
                             _mockFilterRequestContext,
                             _mockRestLiRequestData);
  }

  @Test
  public void testFilterInvocationOnError() throws Exception
  {
    _restLiFilterChain = new RestLiFilterChain(Arrays.asList(_filters), _mockFilterChainCallback);
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        _restLiFilterChain.onError(new TestFilterException(), _mockFilterRequestContext,
                                   _mockFilterResponseContext, _mockResponseAttachments);
        return null;
      }
    }).when(_mockFilterChainCallback).onRequestSuccess(_mockRestLiRequestData, _mockRestLiCallback);

    when(_mockFilterRequestContext.getRequestData()).thenReturn(_mockRestLiRequestData);
    when(_mockFilterResponseContext.getResponseData()).thenReturn(_mockRestLiResponseData);

    _restLiFilterChain.onRequest(_mockFilterRequestContext, _mockResponseFilterContextFactory, _mockRestLiCallback);

    assertFilterCounts(_filters[0], 1, 0, 1);
    assertFilterCounts(_filters[1], 1, 0, 1);
    assertFilterCounts(_filters[2], 1, 0, 1);

    verify(_mockFilterChainCallback).onRequestSuccess(_mockRestLiRequestData, _mockRestLiCallback);
    verify(_mockFilterChainCallback).onError(any(TestFilterException.class), eq(_mockRestLiResponseData),
                                             eq(_mockResponseAttachments));
    verify(_mockFilterRequestContext).getRequestData();
    verify(_mockFilterResponseContext, times(7)).getResponseData();

    verifyNoMoreInteractions(_mockFilterChainCallback, _mockRestLiCallback,
                             _mockRequestAttachmentReader,
                             _mockFilterRequestContext,
                             _mockRestLiRequestData);

  }

  @Test
  public void testNoFilters() throws Exception
  {

    final RestLiFilterChain emptyFilterChain = new RestLiFilterChain(_mockFilterChainCallback);

    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        emptyFilterChain.onResponse(_mockFilterRequestContext, _mockFilterResponseContext, _mockResponseAttachments);
        return null;
      }
    }).when(_mockFilterChainCallback).onRequestSuccess(_mockRestLiRequestData, _mockRestLiCallback);

    when(_mockFilterRequestContext.getRequestData()).thenReturn(_mockRestLiRequestData);
    when(_mockFilterResponseContext.getResponseData()).thenReturn(_mockRestLiResponseData);
    when(_mockResponseFilterContextFactory.fromThrowable(any(Throwable.class))).thenReturn(_mockFilterResponseContext);

    emptyFilterChain.onRequest(_mockFilterRequestContext, _mockResponseFilterContextFactory, _mockRestLiCallback);

    verify(_mockFilterChainCallback).onRequestSuccess(_mockRestLiRequestData, _mockRestLiCallback);
    verify(_mockFilterChainCallback).onResponseSuccess(_mockRestLiResponseData, _mockResponseAttachments);
    verify(_mockFilterRequestContext).getRequestData();
    verify(_mockFilterResponseContext).getResponseData();

    verifyNoMoreInteractions(_mockFilterChainCallback, _mockRestLiCallback, _mockFilterRequestContext,
                             _mockRestLiRequestData);
  }

  private void assertFilterCounts(CountFilter filter, int expectedNumRequests, int expectedNumResponses,
                                  int expectedNumErrors)
  {
    assertEquals(filter.getNumRequests(), expectedNumRequests);
    assertEquals(filter.getNumResponses(), expectedNumResponses);
    assertEquals(filter.getNumErrors(), expectedNumErrors);
  }
}
