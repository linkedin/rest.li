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

import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.RetriableRequestException;
import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.filter.transport.ClientRetryFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.testutils.filter.FilterUtil;
import com.linkedin.r2.transport.http.client.WaiterTimeoutException;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestClientRetryFilter
{
  @Test
  public void testRetryFilter()
  {
    String retryMessage = "this is a retry";
    ClientRetryFilter clientRetryFilter = new ClientRetryFilter();
    RestFilter captureFilter = new RestFilter()
    {
      @Override
      public void onRestError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs,
          NextFilter<RestRequest, RestResponse> nextFilter)
      {
        Assert.assertTrue(ex instanceof RetriableRequestException);
        Assert.assertEquals(retryMessage, ex.getMessage());
      }
    };
    Map<String, String> wireAttributes = new HashMap<>();
    wireAttributes.put(R2Constants.RETRY_MESSAGE_ATTRIBUTE_KEY, retryMessage);
    FilterChain filterChain = FilterChains.createRestChain(captureFilter, clientRetryFilter);
    FilterUtil.fireRestError(filterChain, new RemoteInvocationException("exception"), wireAttributes);
  }

  @Test
  public void testNoWireAttribute()
  {
    ClientRetryFilter clientRetryFilter = new ClientRetryFilter();
    RemoteInvocationException exception = new RemoteInvocationException("exception");
    RestFilter captureFilter = new RestFilter()
    {
      @Override
      public void onRestError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs,
          NextFilter<RestRequest, RestResponse> nextFilter)
      {
        Assert.assertEquals(exception, ex);
      }
    };
    FilterChain filterChain = FilterChains.createRestChain(captureFilter, clientRetryFilter);
    FilterUtil.fireRestError(filterChain, exception, new HashMap<>());
  }

  @Test
  public void testClientSideRetriableException()
  {
    ClientRetryFilter clientRetryFilter = new ClientRetryFilter();
    RestFilter captureFilter = new RestFilter()
    {
      @Override
      public void onRestError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs,
          NextFilter<RestRequest, RestResponse> nextFilter)
      {
        Assert.assertTrue(ex instanceof RetriableRequestException);
        Assert.assertFalse(((RetriableRequestException) ex).getDoNotRetryOverride());
      }
    };
    FilterChain filterChain = FilterChains.createRestChain(captureFilter, clientRetryFilter);
    FilterUtil.fireRestError(filterChain, new WaiterTimeoutException("exception"), new HashMap<>());
  }
}
