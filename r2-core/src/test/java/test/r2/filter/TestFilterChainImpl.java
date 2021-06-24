/*
   Copyright (c) 2012 LinkedIn Corp.

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

/* $Id$ */
package test.r2.filter;


import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.filter.message.stream.StreamFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.testutils.filter.RestCountFilter;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.linkedin.r2.testutils.filter.StreamCountFilter;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Chris Pettitt
 * @author Zhenkai Zhu
 * @version $Revision$
 */
public class TestFilterChainImpl
{
  @Test
  public void testRestRequestFilter()
  {
    final RestCountFilter restCountFilter = new RestCountFilter();
    final StreamCountFilter streamCountFilter = new StreamCountFilter();
    FilterChain fc = FilterChains.createRestChain(restCountFilter);
    fc = fc.addLast(streamCountFilter);

    fireRestRequest(fc);

    assertRestCounts(1, 0, 0, restCountFilter);
    assertStreamCounts(0, 0, 0, streamCountFilter);
  }

  @Test
  public void testRestResponseFilter()
  {
    final RestCountFilter restCountFilter = new RestCountFilter();
    final StreamCountFilter streamCountFilter = new StreamCountFilter();
    FilterChain fc = FilterChains.createRestChain(restCountFilter);
    fc = fc.addLast(streamCountFilter);

    fireRestResponse(fc);

    assertRestCounts(0, 1, 0, restCountFilter);
    assertStreamCounts(0, 0, 0, streamCountFilter);
  }

  @Test
  public void testRestErrorFilter()
  {
    final RestCountFilter restCountFilter = new RestCountFilter();
    final StreamCountFilter streamCountFilter = new StreamCountFilter();
    FilterChain fc = FilterChains.createRestChain(restCountFilter);
    fc = fc.addLast(streamCountFilter);

    fireRestError(fc);

    assertRestCounts(0, 0, 1, restCountFilter);
    assertStreamCounts(0, 0, 0, streamCountFilter);
  }

  @Test
  public void testStreamRequestFilter()
  {
    final RestCountFilter restCountFilter = new RestCountFilter();
    final StreamCountFilter streamCountFilter = new StreamCountFilter();
    FilterChain fc = FilterChains.createRestChain(restCountFilter);
    fc = fc.addLast(streamCountFilter);

    fireStreamRequest(fc);

    assertRestCounts(0, 0, 0, restCountFilter);
    assertStreamCounts(1, 0, 0, streamCountFilter);
  }

  @Test
  public void testStreamResponseFilter()
  {
    final RestCountFilter restCountFilter = new RestCountFilter();
    final StreamCountFilter streamCountFilter = new StreamCountFilter();
    FilterChain fc = FilterChains.createRestChain(restCountFilter);
    fc = fc.addLast(streamCountFilter);

    fireStreamResponse(fc);

    assertRestCounts(0, 0, 0, restCountFilter);
    assertStreamCounts(0, 1, 0, streamCountFilter);
  }

  @Test
  public void testStreamErrorFilter()
  {
    final RestCountFilter restCountFilter = new RestCountFilter();
    final StreamCountFilter streamCountFilter = new StreamCountFilter();
    FilterChain fc = FilterChains.createRestChain(restCountFilter);
    fc = fc.addLast(streamCountFilter);

    fireStreamError(fc);

    assertRestCounts(0, 0, 0, restCountFilter);
    assertStreamCounts(0, 0, 1, streamCountFilter);
  }

  @Test
  public void testChainRestRequestFilters()
  {
    final RestCountFilter filter1 = new RestCountFilter();
    final RestCountFilter filter2 = new RestCountFilter();
    final RestCountFilter filter3 = new RestCountFilter();
    final FilterChain fc = FilterChains.createRestChain(filter1, filter2, filter3);

    fireRestRequest(fc);
    assertRestCounts(1, 0, 0, filter1);
    assertRestCounts(1, 0, 0, filter2);
    assertRestCounts(1, 0, 0, filter3);
  }

  @Test
  public void testChainRestResponseFilters()
  {
    final RestCountFilter filter1 = new RestCountFilter();
    final RestCountFilter filter2 = new RestCountFilter();
    final RestCountFilter filter3 = new RestCountFilter();
    final FilterChain fc = FilterChains.createRestChain(filter1, filter2, filter3);

    fireRestResponse(fc);
    assertRestCounts(0, 1, 0, filter1);
    assertRestCounts(0, 1, 0, filter2);
    assertRestCounts(0, 1, 0, filter3);
  }

  @Test
  public void testChainRestErrorFilters()
  {
    final RestCountFilter filter1 = new RestCountFilter();
    final RestCountFilter filter2 = new RestCountFilter();
    final RestCountFilter filter3 = new RestCountFilter();
    final FilterChain fc = FilterChains.createRestChain(filter1, filter2, filter3);

    fireRestError(fc);
    assertRestCounts(0, 0, 1, filter1);
    assertRestCounts(0, 0, 1, filter2);
    assertRestCounts(0, 0, 1, filter3);
  }

  @Test
  public void testChainStreamRequestFilters()
  {
    final StreamCountFilter filter1 = new StreamCountFilter();
    final StreamCountFilter filter2 = new StreamCountFilter();
    final StreamCountFilter filter3 = new StreamCountFilter();
    final FilterChain fc = FilterChains.createStreamChain(filter1, filter2, filter3);

    fireStreamRequest(fc);
    assertStreamCounts(1, 0, 0, filter1);
    assertStreamCounts(1, 0, 0, filter2);
    assertStreamCounts(1, 0, 0, filter3);
  }

  @Test
  public void testChainStreamResponseFilters()
  {
    final StreamCountFilter filter1 = new StreamCountFilter();
    final StreamCountFilter filter2 = new StreamCountFilter();
    final StreamCountFilter filter3 = new StreamCountFilter();
    final FilterChain fc = FilterChains.createStreamChain(filter1, filter2, filter3);

    fireStreamResponse(fc);
    assertStreamCounts(0, 1, 0, filter1);
    assertStreamCounts(0, 1, 0, filter2);
    assertStreamCounts(0, 1, 0, filter3);
  }

  @Test
  public void testChainStreamErrorFilters()
  {
    final StreamCountFilter filter1 = new StreamCountFilter();
    final StreamCountFilter filter2 = new StreamCountFilter();
    final StreamCountFilter filter3 = new StreamCountFilter();
    final FilterChain fc = FilterChains.createStreamChain(filter1, filter2, filter3);

    fireStreamError(fc);
    assertStreamCounts(0, 0, 1, filter1);
    assertStreamCounts(0, 0, 1, filter2);
    assertStreamCounts(0, 0, 1, filter3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullRestFilter()
  {
    FilterChains.createRestChain(new RestCountFilter(), null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullStreamFilter()
  {
    FilterChains.createStreamChain(new StreamCountFilter(), null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullFilterInList()
  {
    List<RestFilter> restFilters = new ArrayList<>();
    restFilters.add(new RestCountFilter());
    restFilters.add(null);

    FilterChains.create(restFilters, Collections.<StreamFilter>emptyList());
  }

  @Test
  public void testFilterOrderRestChain()
  {
    AtomicInteger count = new AtomicInteger(0);
    CheckOrderFilter filter1 = new CheckOrderFilter(count);
    CheckOrderFilter filter2 = new CheckOrderFilter(count);
    final FilterChain fc = FilterChains.createRestChain(filter1, filter2);

    fireRestRequest(fc);
    Assert.assertEquals(filter1.getOrder(), 1);
    Assert.assertEquals(filter2.getOrder(), 2);

    fireRestResponse(fc);
    Assert.assertEquals(filter1.getOrder(), 4);
    Assert.assertEquals(filter2.getOrder(), 3);

    fireRestError(fc);
    Assert.assertEquals(filter1.getOrder(), 6);
    Assert.assertEquals(filter2.getOrder(), 5);
  }

  @Test
  public void testFilterOrderStreamChain()
  {
    AtomicInteger count = new AtomicInteger(0);
    CheckOrderFilter filter1 = new CheckOrderFilter(count);
    CheckOrderFilter filter2 = new CheckOrderFilter(count);
    final FilterChain fc = FilterChains.createStreamChain(filter1, filter2);

    fireStreamRequest(fc);
    Assert.assertEquals(filter1.getOrder(), 1);
    Assert.assertEquals(filter2.getOrder(), 2);

    fireStreamResponse(fc);
    Assert.assertEquals(filter1.getOrder(), 4);
    Assert.assertEquals(filter2.getOrder(), 3);

    fireStreamError(fc);
    Assert.assertEquals(filter1.getOrder(), 6);
    Assert.assertEquals(filter2.getOrder(), 5);
  }

  @Test
  public void testFilterOrderTwoChains()
  {
    AtomicInteger count = new AtomicInteger(0);
    CheckOrderFilter filter1 = new CheckOrderFilter(count);
    CheckOrderFilter filter2 = new CheckOrderFilter(count);
    List<RestFilter> restFilters = new ArrayList<>();
    restFilters.add(filter1);
    restFilters.add(filter2);

    List<StreamFilter> streamFilters = new ArrayList<>();
    streamFilters.add(filter1);
    streamFilters.add(filter2);

    final FilterChain fc = FilterChains.create(restFilters, streamFilters);

    fireRestRequest(fc);
    Assert.assertEquals(filter1.getOrder(), 1);
    Assert.assertEquals(filter2.getOrder(), 2);

    fireRestResponse(fc);
    Assert.assertEquals(filter1.getOrder(), 4);
    Assert.assertEquals(filter2.getOrder(), 3);

    fireRestError(fc);
    Assert.assertEquals(filter1.getOrder(), 6);
    Assert.assertEquals(filter2.getOrder(), 5);

    count.set(0);

    fireStreamRequest(fc);
    Assert.assertEquals(filter1.getOrder(), 1);
    Assert.assertEquals(filter2.getOrder(), 2);

    fireStreamResponse(fc);
    Assert.assertEquals(filter1.getOrder(), 4);
    Assert.assertEquals(filter2.getOrder(), 3);

    fireStreamError(fc);
    Assert.assertEquals(filter1.getOrder(), 6);
    Assert.assertEquals(filter2.getOrder(), 5);
  }

  private void fireRestRequest(FilterChain fc)
  {
    fc.onRestRequest(new RestRequestBuilder(URI.create("test")).build(),
        createRequestContext(), createWireAttributes()
    );
  }

  private void fireRestResponse(FilterChain fc)
  {
    fc.onRestResponse(new RestResponseBuilder().build(),
        createRequestContext(), createWireAttributes()
    );
  }

  private void fireRestError(FilterChain fc)
  {
    fc.onRestError(new Exception(),
                   createRequestContext(), createWireAttributes()
    );
  }

  private void fireStreamRequest(FilterChain fc)
  {
    fc.onStreamRequest(EasyMock.createMock(StreamRequest.class),
        createRequestContext(), createWireAttributes()
    );
  }

  private void fireStreamResponse(FilterChain fc)
  {
    fc.onStreamResponse(EasyMock.createMock(StreamResponse.class),
        createRequestContext(), createWireAttributes()
    );
  }

  private void fireStreamError(FilterChain fc)
  {
    fc.onStreamError(new Exception(),
        createRequestContext(), createWireAttributes()
    );
  }

  private Map<String, String> createWireAttributes()
  {
    return new HashMap<>();
  }

  private RequestContext createRequestContext()
  {
    return new RequestContext();
  }

  private void assertStreamCounts(int req, int res, int err, StreamCountFilter filter)
  {
    Assert.assertEquals(req, filter.getStreamReqCount());
    Assert.assertEquals(res, filter.getStreamResCount());
    Assert.assertEquals(err, filter.getStreamErrCount());
  }

  private void assertRestCounts(int req, int res, int err, RestCountFilter filter)
  {
    Assert.assertEquals(req, filter.getRestReqCount());
    Assert.assertEquals(res, filter.getRestResCount());
    Assert.assertEquals(err, filter.getRestErrCount());
  }

  private static class CheckOrderFilter implements RestFilter, StreamFilter
  {
    private final AtomicInteger _globalOrder;
    private int _recordedOrder;

    CheckOrderFilter(AtomicInteger globalOrder)
    {
      _globalOrder = globalOrder;
    }

    @Override
    public void onRestRequest(RestRequest req, RequestContext requestContext,
                              Map<String, String> wireAttrs,
                              NextFilter<RestRequest, RestResponse> nextFilter)
    {
      _recordedOrder = _globalOrder.incrementAndGet();
      nextFilter.onRequest(req, requestContext, wireAttrs);
    }

    @Override
    public void onRestResponse(RestResponse res, RequestContext requestContext,
                               Map<String, String> wireAttrs,
                               NextFilter<RestRequest, RestResponse> nextFilter)
    {
      _recordedOrder = _globalOrder.incrementAndGet();
      nextFilter.onResponse(res, requestContext, wireAttrs);
    }

    @Override
    public void onRestError(Throwable ex, RequestContext requestContext,
                            Map<String, String> wireAttrs,
                            NextFilter<RestRequest, RestResponse> nextFilter)
    {
      _recordedOrder = _globalOrder.incrementAndGet();
      nextFilter.onError(ex, requestContext, wireAttrs);
    }

    @Override
    public void onStreamRequest(StreamRequest req, RequestContext requestContext,
                                Map<String, String> wireAttrs,
                                NextFilter<StreamRequest, StreamResponse> nextFilter)
    {
      _recordedOrder = _globalOrder.incrementAndGet();
      nextFilter.onRequest(req, requestContext, wireAttrs);
    }

    @Override
    public void onStreamResponse(StreamResponse res, RequestContext requestContext,
                                 Map<String, String> wireAttrs,
                                 NextFilter<StreamRequest, StreamResponse> nextFilter)
    {
      _recordedOrder = _globalOrder.incrementAndGet();
      nextFilter.onResponse(res, requestContext, wireAttrs);
    }

    @Override
    public void onStreamError(Throwable ex, RequestContext requestContext,
                              Map<String, String> wireAttrs,
                              NextFilter<StreamRequest, StreamResponse> nextFilter)
    {
      _recordedOrder = _globalOrder.incrementAndGet();
      nextFilter.onError(ex, requestContext, wireAttrs);
    }

    public int getOrder()
    {
      return _recordedOrder;
    }
  }

}
