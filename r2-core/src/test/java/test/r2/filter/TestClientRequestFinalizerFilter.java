/*
   Copyright (c) 2019 LinkedIn Corp.

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

package test.r2.filter;

import com.linkedin.r2.filter.ClientRequestFinalizerFilter;
import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.filter.message.stream.StreamFilter;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.Observer;
import com.linkedin.r2.util.RequestContextUtil;
import com.linkedin.r2.util.finalizer.RequestFinalizer;
import com.linkedin.r2.util.finalizer.RequestFinalizerManager;
import com.linkedin.r2.util.finalizer.RequestFinalizerManagerImpl;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;


/**
 * Tests for {@link ClientRequestFinalizerFilter}.
 *
 * @author Chris Zhang
 */
public class TestClientRequestFinalizerFilter
{
  private ClientRequestFinalizerFilter _requestFinalizerFilter;
  private TestFilter _firstFilter;
  private TestFilter _lastFilter;
  private FilterChain _filterChain;
  private AtomicInteger _index;
  private RequestContext _requestContext;
  private TestRequestFinalizer _testRequestFinalizer;

  @Mock
  StreamResponse _streamResponse;
  @Mock
  EntityStream _entityStream;

  @BeforeMethod
  public void setUp()
  {
    MockitoAnnotations.initMocks(this);

    _requestFinalizerFilter = new ClientRequestFinalizerFilter();
    _firstFilter = new TestFilter();
    _lastFilter = new TestFilter();
    _index = new AtomicInteger(0);
    _requestContext = new RequestContext();
    _testRequestFinalizer = new TestRequestFinalizer();

    List<RestFilter> restFilters = Arrays.asList(_firstFilter, _requestFinalizerFilter, _lastFilter);
    List<StreamFilter> streamFilters = Arrays.asList(_firstFilter, _requestFinalizerFilter, _lastFilter);
    _filterChain = FilterChains.create(restFilters, streamFilters);
  }

  @Test
  public void testRestRequestOrdering()
  {
    _filterChain.onRestRequest(null, _requestContext, null);
    registerRequestFinalizer();
    _filterChain.onRestResponse(null, _requestContext, null);

    assertExecutionOrders();
  }

  @Test
  public void testRestRequestErrorOrdering()
  {
    _filterChain.onRestRequest(null, _requestContext, null);
    registerRequestFinalizer();
    _filterChain.onRestError(null, _requestContext, null);

    assertExecutionOrders();
  }

  @Test
  public void testStreamRequestOrdering()
  {
    final AtomicReference<Observer> observerReference = new AtomicReference<>();
    final AtomicInteger addObserverExecutionOrder = new AtomicInteger(0);
    when(_streamResponse.getEntityStream())
        .thenReturn(_entityStream);
    doAnswer(invocation -> {
      addObserverExecutionOrder.set(_index.incrementAndGet());
      observerReference.set((Observer) invocation.getArguments()[0]);
      return null;
    }).when(_entityStream).addObserver(anyObject());

    _filterChain.onStreamRequest(null, _requestContext, null);
    registerRequestFinalizer();
    _filterChain.onStreamResponse(_streamResponse, _requestContext, null);
    observerReference.get().onDone();

    Assert.assertEquals(_firstFilter._onRequestExecutionOrder, 1);
    Assert.assertEquals(_lastFilter._onRequestExecutionOrder, 2);
    Assert.assertEquals(_lastFilter._onResponseExecutionOrder, 3);
    Assert.assertEquals(addObserverExecutionOrder.get(), 4, "Expected observer with RequestFinalizer "
        + "to be added before calling the next filter.");
    Assert.assertEquals(_firstFilter._onResponseExecutionOrder, 5);
    Assert.assertEquals(_testRequestFinalizer._executionOrder, 6, "Expected request finalizer to be "
        + "executed last.");
  }

  @Test
  public void testStreamRequestErrorOrdering()
  {
    _filterChain.onStreamRequest(null, _requestContext, null);
    registerRequestFinalizer();
    _filterChain.onStreamError(null, _requestContext, null);

    assertExecutionOrders();
  }

  @Test
  public void testNextFilterException()
  {
    _firstFilter = new TestFilter(true);
    _filterChain = FilterChains.createRestChain(_firstFilter, _requestFinalizerFilter, _lastFilter);

    _filterChain.onRestRequest(null, _requestContext, null);
    registerRequestFinalizer();
    _filterChain.onRestResponse(null, _requestContext, null);

    assertExecutionOrders();
  }

  @Test
  public void testMissingRequestFinalizerManagerOnRestResponse()
  {
    _filterChain.onRestRequest(null, _requestContext, null);
    registerRequestFinalizer();
    _requestContext = new RequestContext();

    _filterChain.onRestResponse(null, _requestContext, null);

    assertExecutionOrdersNoRequestFinalizer();
  }

  @Test
  public void testMissingRequestFinalizerManagerOnStreamResponse()
  {
    _filterChain.onStreamRequest(null, _requestContext, null);
    registerRequestFinalizer();
    _requestContext = new RequestContext();

    _filterChain.onStreamResponse(null, _requestContext, null);

    assertExecutionOrdersNoRequestFinalizer();
  }

  @Test
  public void testMissingRequestFinalizerManagerOnRestError()
  {
    _filterChain.onRestRequest(null, _requestContext, null);
    registerRequestFinalizer();
    _requestContext = new RequestContext();

    _filterChain.onRestError(null, _requestContext, null);

    assertExecutionOrdersNoRequestFinalizer();
  }

  @Test
  public void testMissingRequestFinalizerManagerOnStreamError()
  {
    _filterChain.onStreamRequest(null, _requestContext, null);
    registerRequestFinalizer();
    _requestContext = new RequestContext();

    _filterChain.onStreamError(null, _requestContext, null);

    assertExecutionOrdersNoRequestFinalizer();
  }

  @Test
  public void testExistingRequestFinalizerManager()
  {
    final RequestFinalizerManagerImpl manager = new RequestFinalizerManagerImpl(null, null);
    final AtomicBoolean atomicBoolean = new AtomicBoolean(false);
    manager.registerRequestFinalizer((request, response ,requestContext1, throwable) -> atomicBoolean.set(true));

    _requestContext.putLocalAttr(R2Constants.CLIENT_REQUEST_FINALIZER_MANAGER_REQUEST_CONTEXT_KEY, manager);

    _filterChain.onRestRequest(null, _requestContext, null);
    registerRequestFinalizer();
    _filterChain.onRestResponse(null, _requestContext, null);

    assertExecutionOrders();
    Assert.assertTrue(atomicBoolean.get(), "Expected the request finalizer registered before reaching the"
        + "ClientRequestFinalizerFilter to still be invoked.");
  }

  private void registerRequestFinalizer()
  {
    final RequestFinalizerManager manager = RequestContextUtil.getClientRequestFinalizerManager(_requestContext);
    manager.registerRequestFinalizer(_testRequestFinalizer);
  }

  private void assertExecutionOrders()
  {
    Assert.assertEquals(_firstFilter._onRequestExecutionOrder, 1);
    Assert.assertEquals(_lastFilter._onRequestExecutionOrder, 2);
    Assert.assertEquals(_lastFilter._onResponseExecutionOrder, 3);
    Assert.assertEquals(_firstFilter._onResponseExecutionOrder, 4);
    Assert.assertEquals(_testRequestFinalizer._executionOrder, 5, "Expected the request finalizer "
        + "to be executed last.");
  }

  private void assertExecutionOrdersNoRequestFinalizer()
  {
    Assert.assertEquals(_firstFilter._onRequestExecutionOrder, 1);
    Assert.assertEquals(_lastFilter._onRequestExecutionOrder, 2);
    Assert.assertEquals(_lastFilter._onResponseExecutionOrder, 3);
    Assert.assertEquals(_firstFilter._onResponseExecutionOrder, 4);
    Assert.assertEquals(_testRequestFinalizer._executionOrder, 0, "Expected the request finalizer "
        + "to be not be executed.");
  }

  private class TestRequestFinalizer implements RequestFinalizer
  {
    private int _executionOrder;

    @Override
    public void finalizeRequest(Request request, Response response, RequestContext requestContext, Throwable error)
    {
      _executionOrder = _index.incrementAndGet();
    }
  }

  private class TestFilter implements RestFilter, StreamFilter
  {
    private final boolean _throwExceptionOnResponse;

    private int _onRequestExecutionOrder;
    private int _onResponseExecutionOrder;

    private TestFilter()
    {
      this(false);
    }

    private TestFilter(boolean throwExceptionOnResponse)
    {
      _throwExceptionOnResponse = throwExceptionOnResponse;
    }

    @Override
    public void onRestRequest(RestRequest req, RequestContext requestContext, Map<String, String> wireAttrs,
        NextFilter<RestRequest, RestResponse> nextFilter)
    {
      handleRequest(req, requestContext, nextFilter);
    }

    @Override
    public void onRestResponse(RestResponse res, RequestContext requestContext, Map<String, String> wireAttrs,
        NextFilter<RestRequest, RestResponse> nextFilter)
    {
      handleResponse(res, requestContext, nextFilter);
    }

    @Override
    public void onRestError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs,
        NextFilter<RestRequest, RestResponse> nextFilter)
    {
      handleError(ex, requestContext, nextFilter);
    }

    @Override
    public void onStreamRequest(StreamRequest req, RequestContext requestContext, Map<String, String> wireAttrs,
        NextFilter<StreamRequest, StreamResponse> nextFilter)
    {
      handleRequest(req, requestContext, nextFilter);
    }

    @Override
    public void onStreamResponse(StreamResponse res, RequestContext requestContext, Map<String, String> wireAttrs,
        NextFilter<StreamRequest, StreamResponse> nextFilter)
    {
      if (_throwExceptionOnResponse)
      {
        throw new RuntimeException("Expected exception.");
      }
      handleResponse(res, requestContext, nextFilter);
    }

    @Override
    public void onStreamError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs,
        NextFilter<StreamRequest, StreamResponse> nextFilter)
    {
      handleError(ex, requestContext, nextFilter);
    }

    private <REQ extends Request, RES extends Response> void handleRequest(REQ request,
        RequestContext requestContext, NextFilter<REQ, RES> nextFilter)
    {
      _onRequestExecutionOrder = _index.incrementAndGet();

      nextFilter.onRequest(request, requestContext, null);
    }

    private <REQ extends Request, RES extends Response> void handleResponse(RES response, RequestContext requestContext,
        NextFilter<REQ, RES> nextFilter)
    {
      _onResponseExecutionOrder = _index.incrementAndGet();

      if (_throwExceptionOnResponse)
      {
        throw new RuntimeException("Expected exception.");
      }
      nextFilter.onResponse(response, requestContext, null);
    }

    private <REQ extends Request, RES extends Response> void handleError(Throwable error, RequestContext requestContext,
        NextFilter<REQ, RES> nextFilter)
    {
      _onResponseExecutionOrder = _index.incrementAndGet();

      if (_throwExceptionOnResponse)
      {
        throw new RuntimeException("Expected exception.");
      }
      nextFilter.onError(error, requestContext, null);
    }
  }
}
