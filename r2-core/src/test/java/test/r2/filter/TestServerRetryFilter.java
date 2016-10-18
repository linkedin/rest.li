/*
   Copyright (c) 2016 LinkedIn Corp.

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

import com.linkedin.r2.RetriableRequestException;
import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.filter.message.stream.StreamFilter;
import com.linkedin.r2.filter.transport.ServerRetryFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamException;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.testutils.filter.FilterUtil;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestServerRetryFilter
{
  @Test
  public void testRetryFilter()
  {
    String retryMessage = "this is a retry";
    ServerRetryFilter retryFilter = new ServerRetryFilter();
    RestFilter captureFilter = new RestFilter()
    {
      @Override
      public void onRestError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs,
          NextFilter<RestRequest, RestResponse> nextFilter)
      {
        Assert.assertEquals(wireAttrs.get(R2Constants.RETRY_MESSAGE_ATTRIBUTE_KEY), retryMessage);
      }
    };
    FilterChain filterChain = FilterChains.createRestChain(captureFilter, retryFilter);
    FilterUtil.fireRestError(filterChain, new RestException(null, new RetriableRequestException(retryMessage)), new HashMap<String, String>());
  }

  @Test
  public void testNestedException()
  {
    String retryMessage = "this is a retry";
    ServerRetryFilter retryFilter = new ServerRetryFilter();
    RestFilter captureFilter = new RestFilter()
    {
      @Override
      public void onRestError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs,
          NextFilter<RestRequest, RestResponse> nextFilter)
      {
        Assert.assertEquals(wireAttrs.get(R2Constants.RETRY_MESSAGE_ATTRIBUTE_KEY), retryMessage);
      }
    };
    FilterChain filterChain = FilterChains.createRestChain(captureFilter, retryFilter);
    Throwable nestedException = new RetriableRequestException(retryMessage);
    for (int i = 0; i < 5; i++)
    {
      nestedException = new RuntimeException(nestedException);
    }
    FilterUtil.fireRestError(filterChain, new RestException(null, nestedException), new HashMap<String, String>());
  }

  @Test
  public void testStreamRetryFilter()
  {
    String retryMessage = "this is a retry";
    ServerRetryFilter retryFilter = new ServerRetryFilter();
    StreamFilter captureFilter = new StreamFilter()
    {
      @Override
      public void onStreamError(Throwable ex,
          RequestContext requestContext,
          Map<String, String> wireAttrs,
          NextFilter<StreamRequest, StreamResponse> nextFilter)
      {
        Assert.assertEquals(wireAttrs.get(R2Constants.RETRY_MESSAGE_ATTRIBUTE_KEY), retryMessage);
      }
    };
    FilterChain filterChain = FilterChains.createStreamChain(captureFilter, retryFilter);
    FilterUtil.fireRestError(filterChain, new StreamException(null, new RetriableRequestException(retryMessage)), new HashMap<String, String>());
  }

  @Test
  public void testNotRetriableException()
  {
    ServerRetryFilter retryFilter = new ServerRetryFilter();
    RestFilter captureFilter = new RestFilter()
    {
      @Override
      public void onRestError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs,
          NextFilter<RestRequest, RestResponse> nextFilter)
      {
        Assert.assertNull(wireAttrs.get(R2Constants.RETRY_MESSAGE_ATTRIBUTE_KEY));
      }
    };
    FilterChain filterChain = FilterChains.createRestChain(captureFilter, retryFilter);
    FilterUtil.fireRestError(filterChain, new RuntimeException(new RuntimeException()), new HashMap<String, String>());
  }
}
