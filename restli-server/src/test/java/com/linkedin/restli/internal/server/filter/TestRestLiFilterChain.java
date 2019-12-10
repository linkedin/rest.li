package com.linkedin.restli.internal.server.filter;

import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.attachments.RestLiAttachmentReader;
import com.linkedin.restli.internal.server.ResourceContextImpl;
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
import com.linkedin.restli.internal.server.response.RestLiResponseEnvelope;
import com.linkedin.restli.internal.server.response.RestLiResponseHandler;
import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.RestLiResponseAttachments;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
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
import static org.mockito.Mockito.*;
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
  private FilterChainDispatcher _mockFilterChainDispatcher;
  @Mock
  private FilterChainCallback _mockFilterChainCallback;
  @Mock
  @SuppressWarnings("rawtypes")
  private RestLiResponseData _mockRestLiResponseData;
  @Mock
  private FilterRequestContext _mockFilterRequestContext;
  @Mock
  private FilterResponseContext _mockFilterResponseContext;
  @Mock
  private RestLiAttachmentReader _mockRequestAttachmentReader;
  @Mock
  private RestLiResponseAttachments _mockResponseAttachments;
  @Mock
  private RestLiFilterResponseContextFactory _mockFilterResponseContextFactory;

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
    when(_mockFilterResponseContextFactory.getRequestContext()).thenAnswer(invocation -> new RequestContext());
    when(_method.getContext()).thenAnswer(invocation -> new ResourceContextImpl());
  }

  @AfterMethod
  protected void resetMocks()
  {
    reset(_mockFilterChainDispatcher, _mockFilterChainCallback, _mockFilterRequestContext, _mockFilterResponseContext, _mockRestLiRequestData,
          _mockRestLiResponseData);
    _filters = new CountFilter[] {
        new CountFilter(),
        new CountFilter(),
        new CountFilter()
    };
  }

  @SuppressWarnings(value="unchecked")
  @Test
  public void testFilterInvocationSuccess() throws Exception
  {
    _restLiFilterChain = new RestLiFilterChain(Arrays.asList(_filters),
        _mockFilterChainDispatcher, _mockFilterChainCallback);
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        _restLiFilterChain.onResponse(_mockFilterRequestContext, _mockFilterResponseContext);
        return null;
      }
    }).when(_mockFilterChainDispatcher).onRequestSuccess(eq(_mockRestLiRequestData), any(RestLiCallback.class));

    when(_mockFilterRequestContext.getRequestData()).thenReturn(_mockRestLiRequestData);
    when(_mockFilterResponseContext.getResponseData()).thenReturn(_mockRestLiResponseData);
    when(_mockFilterResponseContextFactory.fromThrowable(any(Throwable.class))).thenReturn(_mockFilterResponseContext);

    _restLiFilterChain.onRequest(_mockFilterRequestContext, _mockFilterResponseContextFactory);

    for(CountFilter filter : _filters)
    {
      assertEquals(filter.getNumRequests(), 1);
      assertEquals(filter.getNumResponses(), 1);
      assertEquals(filter.getNumErrors(), 0);
    }

    verify(_mockFilterRequestContext).getRequestData();
    verify(_mockFilterChainDispatcher).onRequestSuccess(eq(_mockRestLiRequestData), any(RestLiCallback.class));
    verify(_mockFilterChainCallback).onResponseSuccess(_mockRestLiResponseData);

    verifyNoMoreInteractions(_mockFilterChainCallback, _mockFilterRequestContext, _mockRestLiRequestData);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testFilterInvocationRequestOnError() throws Exception
  {
    _restLiFilterChain = new RestLiFilterChain(Arrays.asList(_filters),
        _mockFilterChainDispatcher, _mockFilterChainCallback);
    _filters[1] = new CountFilterRequestOnError();
    when(_responseHandler.buildExceptionResponseData(eq(_method), any(RestLiServiceException.class), anyMap(), anyList()))
        .thenReturn(_mockRestLiResponseData);
    when(_mockFilterResponseContextFactory.fromThrowable(any(Throwable.class))).thenReturn(_mockFilterResponseContext);
    when(_mockFilterResponseContext.getResponseData()).thenReturn(_mockRestLiResponseData);
    when(_mockRestLiResponseData.getResponseEnvelope()).thenReturn(mock(RestLiResponseEnvelope.class));

    _restLiFilterChain.onRequest(_mockFilterRequestContext,
                                 new RestLiFilterResponseContextFactory(_request, _method, _responseHandler));

    verifySecondFilterRequestException();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testFilterInvocationRequestErrorOnError() throws Exception
  {
    _restLiFilterChain = new RestLiFilterChain(Arrays.asList(_filters),
        _mockFilterChainDispatcher, _mockFilterChainCallback);
    _filters[1] = new CountFilterRequestErrorOnError();
    when(_responseHandler.buildExceptionResponseData(eq(_method), any(RestLiServiceException.class), anyMap(), anyList()))
        .thenReturn(_mockRestLiResponseData);
    when(_mockRestLiResponseData.getResponseEnvelope()).thenReturn(mock(RestLiResponseEnvelope.class));

    _restLiFilterChain.onRequest(_mockFilterRequestContext,
                                 new RestLiFilterResponseContextFactory(_request, _method, _responseHandler));

    verifySecondFilterRequestException();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testFilterInvocationRequestThrowsError() throws Exception
  {
    _restLiFilterChain = new RestLiFilterChain(Arrays.asList(_filters),
        _mockFilterChainDispatcher, _mockFilterChainCallback);
    _filters[1] = new CountFilterRequestThrowsError();
    when(_responseHandler.buildExceptionResponseData(eq(_method), any(RestLiServiceException.class), anyMap(), anyList()))
        .thenReturn(_mockRestLiResponseData);
    when(_mockRestLiResponseData.getResponseEnvelope()).thenReturn(mock(RestLiResponseEnvelope.class));
    _restLiFilterChain.onRequest(_mockFilterRequestContext,
                                 new RestLiFilterResponseContextFactory(_request, _method, _responseHandler));

    verifySecondFilterRequestException();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testFilterInvocationRequestErrorThrowsError() throws Exception
  {
    _restLiFilterChain = new RestLiFilterChain(Arrays.asList(_filters),
        _mockFilterChainDispatcher, _mockFilterChainCallback);
    _filters[1] = new CountFilterRequestErrorThrowsError();
    when(_responseHandler.buildExceptionResponseData(eq(_method), any(RestLiServiceException.class), anyMap(), anyList()))
        .thenReturn(_mockRestLiResponseData);
    when(_mockRestLiResponseData.getResponseEnvelope()).thenReturn(mock(RestLiResponseEnvelope.class));
    _restLiFilterChain.onRequest(_mockFilterRequestContext,
                                 new RestLiFilterResponseContextFactory(_request, _method, _responseHandler));

    verifySecondFilterRequestException();
  }

  private void verifySecondFilterRequestException()
  {
    assertFilterCounts(_filters[0], 1, 0, 1);
    assertFilterCounts(_filters[1], 1, 0, 1);
    assertFilterCounts(_filters[2], 0, 0, 0);

    verify(_mockFilterChainCallback).onError(any(TestFilterException.class), any(RestLiResponseData.class));

    verify(_mockRestLiResponseData.getResponseEnvelope(), times(2)).setExceptionInternal(any(RestLiServiceException.class));

    verifyNoMoreInteractions(_mockFilterChainCallback, _mockFilterRequestContext, _mockRestLiRequestData);
  }

  @SuppressWarnings(value="unchecked")
  @Test
  public void testFilterInvocationResponseOnError() throws Exception
  {
    _restLiFilterChain = new RestLiFilterChain(Arrays.asList(_filters),
        _mockFilterChainDispatcher, _mockFilterChainCallback);
    _filters[1] = new CountFilterResponseOnError();

    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        _restLiFilterChain.onResponse(_mockFilterRequestContext, _mockFilterResponseContext);
        return null;
      }
    }).when(_mockFilterChainDispatcher).onRequestSuccess(eq(_mockRestLiRequestData), any(RestLiCallback.class));

    when(_mockFilterRequestContext.getRequestData()).thenReturn(_mockRestLiRequestData);
    when(_mockFilterResponseContext.getResponseData()).thenReturn(_mockRestLiResponseData);
    when(_mockFilterResponseContextFactory.fromThrowable(any(Throwable.class))).thenReturn(_mockFilterResponseContext);
    when(_mockRestLiResponseData.getResponseEnvelope()).thenReturn(mock(RestLiResponseEnvelope.class));

    _restLiFilterChain.onRequest(_mockFilterRequestContext, _mockFilterResponseContextFactory);

    verifySecondFilterResponseException();

  }

  @SuppressWarnings("unchecked")
  @Test
  public void testFilterInvocationResponseErrorOnError() throws Exception
  {
    _restLiFilterChain = new RestLiFilterChain(Arrays.asList(_filters),
        _mockFilterChainDispatcher, _mockFilterChainCallback);
    _filters[1] = new CountFilterResponseErrorOnError();

    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        _restLiFilterChain.onResponse(_mockFilterRequestContext, _mockFilterResponseContext);
        return null;
      }
    }).when(_mockFilterChainDispatcher).onRequestSuccess(eq(_mockRestLiRequestData), any(RestLiCallback.class));

    when(_mockFilterRequestContext.getRequestData()).thenReturn(_mockRestLiRequestData);
    when(_mockFilterResponseContext.getResponseData()).thenReturn(_mockRestLiResponseData);
    when(_responseHandler.buildExceptionResponseData(eq(_method), any(RestLiServiceException.class), anyMap(), anyList()))
        .thenReturn(_mockRestLiResponseData);
    when(_mockFilterResponseContextFactory.fromThrowable(any(Throwable.class))).thenReturn(_mockFilterResponseContext);
    when(_mockRestLiResponseData.getResponseEnvelope()).thenReturn(mock(RestLiResponseEnvelope.class));

    _restLiFilterChain.onRequest(_mockFilterRequestContext, _mockFilterResponseContextFactory);

    verifySecondFilterResponseException();
  }

  @SuppressWarnings(value="unchecked")
  @Test
  public void testFilterInvocationResponseThrowsError() throws Exception
  {
    _restLiFilterChain = new RestLiFilterChain(Arrays.asList(_filters),
        _mockFilterChainDispatcher, _mockFilterChainCallback);
    _filters[1] = new CountFilterResponseThrowsError();

    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        _restLiFilterChain.onResponse(_mockFilterRequestContext, _mockFilterResponseContext);
        return null;
      }
    }).when(_mockFilterChainDispatcher).onRequestSuccess(eq(_mockRestLiRequestData), any(RestLiCallback.class));

    when(_mockFilterRequestContext.getRequestData()).thenReturn(_mockRestLiRequestData);
    when(_mockFilterResponseContext.getResponseData()).thenReturn(_mockRestLiResponseData);
    when(_mockFilterResponseContextFactory.fromThrowable(any(Throwable.class))).thenReturn(_mockFilterResponseContext);
    when(_mockRestLiResponseData.getResponseEnvelope()).thenReturn(mock(RestLiResponseEnvelope.class));

    _restLiFilterChain.onRequest(_mockFilterRequestContext, _mockFilterResponseContextFactory);

    verifySecondFilterResponseException();
  }

  @SuppressWarnings(value="unchecked")
  @Test
  public void testFilterInvocationResponseErrorThrowsError() throws Exception
  {
    _restLiFilterChain = new RestLiFilterChain(Arrays.asList(_filters),
        _mockFilterChainDispatcher, _mockFilterChainCallback);
    _filters[1] = new CountFilterResponseErrorThrowsError();

    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        _restLiFilterChain.onResponse(_mockFilterRequestContext, _mockFilterResponseContext);
        return null;
      }
    }).when(_mockFilterChainDispatcher).onRequestSuccess(eq(_mockRestLiRequestData), any(RestLiCallback.class));

    when(_mockFilterRequestContext.getRequestData()).thenReturn(_mockRestLiRequestData);
    when(_mockFilterResponseContext.getResponseData()).thenReturn(_mockRestLiResponseData);
    when(_mockFilterResponseContextFactory.fromThrowable(any(Throwable.class))).thenReturn(_mockFilterResponseContext);
    when(_mockRestLiResponseData.getResponseEnvelope()).thenReturn(mock(RestLiResponseEnvelope.class));

    _restLiFilterChain.onRequest(_mockFilterRequestContext, _mockFilterResponseContextFactory);

    verifySecondFilterResponseException();
  }

  @SuppressWarnings(value="unchecked")
  private void verifySecondFilterResponseException()
  {
    assertFilterCounts(_filters[0], 1, 0, 1);
    assertFilterCounts(_filters[1], 1, 1, 0);
    assertFilterCounts(_filters[2], 1, 1, 0);

    verify(_mockFilterChainDispatcher).onRequestSuccess(eq(_mockRestLiRequestData), any(RestLiCallback.class));
    verify(_mockFilterChainCallback).onError(any(TestFilterException.class), eq(_mockRestLiResponseData));
    verify(_mockFilterRequestContext).getRequestData();
    verify(_mockFilterResponseContext, times(5)).getResponseData();

    verifyNoMoreInteractions(_mockFilterChainCallback, _mockFilterRequestContext, _mockRestLiRequestData);
  }

  @SuppressWarnings(value="unchecked")
  @Test
  public void testFilterInvocationResponseErrorFixesError() throws Exception
  {
    _restLiFilterChain = new RestLiFilterChain(Arrays.asList(_filters),
        _mockFilterChainDispatcher, _mockFilterChainCallback);
    _filters[1] = new CountFilterResponseErrorFixesError();
    _filters[2] = new CountFilterResponseErrorOnError();

    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        _restLiFilterChain.onResponse(_mockFilterRequestContext, _mockFilterResponseContext);
        return null;
      }
    }).when(_mockFilterChainDispatcher).onRequestSuccess(eq(_mockRestLiRequestData), any(RestLiCallback.class));

    when(_mockFilterRequestContext.getRequestData()).thenReturn(_mockRestLiRequestData);
    when(_mockFilterResponseContext.getResponseData()).thenReturn(_mockRestLiResponseData);
    when(_mockRestLiResponseData.getResponseEnvelope()).thenReturn(mock(RestLiResponseEnvelope.class));

    _restLiFilterChain.onRequest(_mockFilterRequestContext, _mockFilterResponseContextFactory);

    assertFilterCounts(_filters[0], 1, 1, 0);
    assertFilterCounts(_filters[1], 1, 0, 1);
    assertFilterCounts(_filters[2], 1, 1, 0);

    verify(_mockFilterChainDispatcher).onRequestSuccess(eq(_mockRestLiRequestData), any(RestLiCallback.class));
    verify(_mockFilterChainCallback).onResponseSuccess(eq(_mockRestLiResponseData));
    verify(_mockFilterRequestContext).getRequestData();
    verify(_mockFilterResponseContext, times(4)).getResponseData();
    verify(_mockRestLiResponseData.getResponseEnvelope()).setExceptionInternal(any(RestLiServiceException.class));

    verifyNoMoreInteractions(_mockFilterChainCallback, _mockFilterRequestContext, _mockRestLiRequestData);
  }

  @SuppressWarnings(value="unchecked")
  @Test
  public void testFilterInvocationLastResponseErrorFixesError() throws Exception
  {
    _restLiFilterChain = new RestLiFilterChain(Arrays.asList(_filters),
        _mockFilterChainDispatcher, _mockFilterChainCallback);
    _filters[0] = new CountFilterResponseErrorFixesError();
    _filters[1] = new CountFilterResponseErrorOnError();

    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        _restLiFilterChain.onResponse(_mockFilterRequestContext, _mockFilterResponseContext);
        return null;
      }
    }).when(_mockFilterChainDispatcher).onRequestSuccess(eq(_mockRestLiRequestData), any(RestLiCallback.class));

    when(_mockFilterRequestContext.getRequestData()).thenReturn(_mockRestLiRequestData);
    when(_mockFilterResponseContext.getResponseData()).thenReturn(_mockRestLiResponseData);
    when(_mockRestLiResponseData.getResponseEnvelope()).thenReturn(mock(RestLiResponseEnvelope.class));

    _restLiFilterChain.onRequest(_mockFilterRequestContext, _mockFilterResponseContextFactory);

    assertFilterCounts(_filters[0], 1, 0, 1);
    assertFilterCounts(_filters[1], 1, 1, 0);
    assertFilterCounts(_filters[2], 1, 1, 0);

    verify(_mockFilterChainDispatcher).onRequestSuccess(eq(_mockRestLiRequestData), any(RestLiCallback.class));
    verify(_mockFilterChainCallback).onResponseSuccess(eq(_mockRestLiResponseData));
    verify(_mockFilterRequestContext).getRequestData();
    verify(_mockFilterResponseContext, times(4)).getResponseData();
    verify(_mockRestLiResponseData.getResponseEnvelope()).setExceptionInternal(any(RestLiServiceException.class));

    verifyNoMoreInteractions(_mockFilterChainCallback, _mockFilterRequestContext, _mockRestLiRequestData);
  }

  @SuppressWarnings(value="unchecked")
  @Test
  public void testFilterInvocationOnError() throws Exception
  {
    _restLiFilterChain = new RestLiFilterChain(Arrays.asList(_filters),
        _mockFilterChainDispatcher, _mockFilterChainCallback);
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        _restLiFilterChain.onError(new TestFilterException(), _mockFilterRequestContext,
                                   _mockFilterResponseContext);
        return null;
      }
    }).when(_mockFilterChainDispatcher).onRequestSuccess(eq(_mockRestLiRequestData), any(RestLiCallback.class));

    when(_mockFilterRequestContext.getRequestData()).thenReturn(_mockRestLiRequestData);
    when(_mockFilterResponseContext.getResponseData()).thenReturn(_mockRestLiResponseData);
    when(_mockRestLiResponseData.getResponseEnvelope()).thenReturn(mock(RestLiResponseEnvelope.class));

    _restLiFilterChain.onRequest(_mockFilterRequestContext, _mockFilterResponseContextFactory);

    assertFilterCounts(_filters[0], 1, 0, 1);
    assertFilterCounts(_filters[1], 1, 0, 1);
    assertFilterCounts(_filters[2], 1, 0, 1);

    verify(_mockFilterChainDispatcher).onRequestSuccess(eq(_mockRestLiRequestData), any(RestLiCallback.class));
    verify(_mockFilterChainCallback).onError(any(TestFilterException.class), eq(_mockRestLiResponseData));
    verify(_mockFilterRequestContext).getRequestData();
    verify(_mockFilterResponseContext, times(7)).getResponseData();
    verify(_mockRestLiResponseData.getResponseEnvelope(), times(3)).setExceptionInternal(any(RestLiServiceException.class));

    verifyNoMoreInteractions(_mockFilterChainCallback, _mockRequestAttachmentReader, _mockFilterRequestContext,
                             _mockRestLiRequestData);

  }

  @SuppressWarnings(value="unchecked")
  @Test
  public void testNoFilters() throws Exception
  {

    final RestLiFilterChain emptyFilterChain = new RestLiFilterChain(null,
        _mockFilterChainDispatcher, _mockFilterChainCallback);

    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        emptyFilterChain.onResponse(_mockFilterRequestContext, _mockFilterResponseContext);
        return null;
      }
    }).when(_mockFilterChainDispatcher).onRequestSuccess(eq(_mockRestLiRequestData), any(RestLiCallback.class));

    when(_mockFilterRequestContext.getRequestData()).thenReturn(_mockRestLiRequestData);
    when(_mockFilterResponseContext.getResponseData()).thenReturn(_mockRestLiResponseData);
    when(_mockFilterResponseContextFactory.fromThrowable(any(Throwable.class))).thenReturn(_mockFilterResponseContext);

    emptyFilterChain.onRequest(_mockFilterRequestContext, _mockFilterResponseContextFactory);

    verify(_mockFilterChainDispatcher).onRequestSuccess(eq(_mockRestLiRequestData), any(RestLiCallback.class));
    verify(_mockFilterChainCallback).onResponseSuccess(_mockRestLiResponseData);
    verify(_mockFilterRequestContext).getRequestData();
    verify(_mockFilterResponseContext).getResponseData();

    verifyNoMoreInteractions(_mockFilterChainCallback, _mockFilterRequestContext, _mockRestLiRequestData);
  }

  private void assertFilterCounts(CountFilter filter, int expectedNumRequests, int expectedNumResponses,
                                  int expectedNumErrors)
  {
    assertEquals(filter.getNumRequests(), expectedNumRequests);
    assertEquals(filter.getNumResponses(), expectedNumResponses);
    assertEquals(filter.getNumErrors(), expectedNumErrors);
  }
}
