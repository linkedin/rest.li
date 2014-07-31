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

package com.linkedin.d2.balancer.clients;


import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class LazyClientTest
{
  @Test(groups = { "small", "back-end" })
  public void testClient()
  {
    LazyClientTestFactory factory = new LazyClientTestFactory();
    Map<String, String> properties = new HashMap<String, String>();
    LazyClient client = new LazyClient(properties, factory);

    properties.put("test", "exists");

    assertEquals(factory.getClientCount, 0);
    assertNull(factory.properties);

    client.restRequest(null, new RequestContext(), null, null);

    assertEquals(factory.getClientCount, 1);
    assertEquals(factory.properties, properties);

    client.restRequest(null, new RequestContext(), null, null);

    assertEquals(factory.getClientCount, 1);
    assertEquals(factory.restRequestCount, 2);
    assertEquals(factory.shutdownCount, 0);

    client.shutdown(null);

    assertEquals(factory.shutdownCount, 1);
  }

  public static class LazyClientTestFactory implements TransportClientFactory
  {
    public int                 getClientCount   = 0;
    public int                 restRequestCount = 0;
    public int                 shutdownCount    = 0;
    public Map<String, ? extends Object> properties;

    @Override
    public TransportClient getClient(Map<String, ? extends Object> properties)
    {
      ++getClientCount;
      this.properties = properties;

      return new TransportClient()
      {

        @Override
        public void restRequest(RestRequest request,
                                RequestContext requestContext,
                                Map<String, String> wireAttrs,
                                TransportCallback<RestResponse> callback)
        {
          ++restRequestCount;
        }

        @Override
        public void shutdown(Callback<None> callback)
        {
          ++shutdownCount;
        }
      };
    }

    @Override
    public void shutdown(Callback<None> callback)
    {
      callback.onSuccess(None.none());
    }
  }
}
