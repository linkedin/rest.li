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
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.rpc.RpcRequest;
import com.linkedin.r2.message.rpc.RpcRequestBuilder;
import com.linkedin.r2.message.rpc.RpcResponse;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SettableClock;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TrackerClientTest
{
  @Test(groups = { "small", "back-end" })
  public void testClient() throws URISyntaxException
  {
    URI uri = URI.create("http://test.qa.com:1234/foo");
    double weight = 3d;
    TestClient wrappedClient = new TestClient();
    Clock clock = new SettableClock();
    Map<Integer, PartitionData> partitionDataMap = new HashMap<Integer, PartitionData>(2);
    partitionDataMap.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(3d));
    TrackerClient client = new TrackerClient(uri, partitionDataMap, wrappedClient, clock, null);

    assertEquals(client.getUri(), uri);
    Double clientWeight = client.getPartitionWeight(DefaultPartitionAccessor.DEFAULT_PARTITION_ID);
    assertEquals(clientWeight, weight);
    assertEquals(client.getWrappedClient(), wrappedClient);

    RestRequest restRequest = new RestRequestBuilder(uri).build();
    Map<String, String> restWireAttrs = new HashMap<String, String>();
    TestTransportCallback<RestResponse> restCallback =
        new TestTransportCallback<RestResponse>();

    client.restRequest(restRequest, new RequestContext(), restWireAttrs, restCallback);

    assertFalse(restCallback.response.hasError());
    assertEquals(wrappedClient.restRequest, restRequest);
    assertEquals(wrappedClient.restWireAttrs, restWireAttrs);

    RpcRequest rpcRequest = new RpcRequestBuilder(uri).build();
    Map<String, String> rpcWireAttrs = new HashMap<String, String>();
    TestTransportCallback<RpcResponse> rpcCallback =
        new TestTransportCallback<RpcResponse>();

    client.rpcRequest(rpcRequest, new RequestContext(), rpcWireAttrs, rpcCallback);

    assertTrue(rpcCallback.response.hasError());
    assertEquals(wrappedClient.rpcRequest, rpcRequest);
    assertEquals(wrappedClient.rpcWireAttrs, rpcWireAttrs);
    assertEquals(client.getCallTracker().getCurrentErrorCountTotal(), 1);
    assertEquals(client.getCallTracker().getCurrentCallCountTotal(), 2);
  }

  public static class TestClient implements TransportClient
  {
    public RestRequest                     restRequest;
    public RequestContext                  restRequestContext;
    public Map<String, String>             restWireAttrs;
    public TransportCallback<RestResponse> restCallback;

    public RpcRequest                      rpcRequest;
    public RequestContext                  rpcRequestContext;
    public Map<String, String>             rpcWireAttrs;
    public TransportCallback<RpcResponse>  rpcCallback;

    public boolean                         shutdownCalled;

    @Override
    public void restRequest(RestRequest request,
                            RequestContext requestContext,
                            Map<String, String> wireAttrs,
                            TransportCallback<RestResponse> callback)
    {
      restRequest = request;
      restRequestContext = requestContext;
      restWireAttrs = wireAttrs;
      restCallback = callback;

      callback.onResponse(TransportResponseImpl.<RestResponse> success(new RestResponseBuilder().build(),
                                                                       wireAttrs));
    }

    @Override
    public void rpcRequest(RpcRequest request,
                           RequestContext requestContext,
                           Map<String, String> wireAttrs,
                           TransportCallback<RpcResponse> callback)
    {
      rpcRequest = request;
      rpcRequestContext = requestContext;
      rpcWireAttrs = wireAttrs;
      rpcCallback = callback;

      callback.onResponse(TransportResponseImpl.<RpcResponse> error(new Exception("fail"),
                                                                    wireAttrs));
    }

    @Override
    public void shutdown(Callback<None> callback)
    {
      shutdownCalled = true;

      callback.onSuccess(None.none());
    }
  }

  public static class TestTransportCallback<T> implements TransportCallback<T>
  {
    public TransportResponse<T> response;

    @Override
    public void onResponse(TransportResponse<T> response)
    {
      this.response = response;
    }
  }

  public static class TestCallback<T> implements Callback<T>
  {
    public Throwable e;
    public T         t;

    @Override
    public void onError(Throwable e)
    {
      this.e = e;
    }

    @Override
    public void onSuccess(T t)
    {
      this.t = t;
    }
  }
}
