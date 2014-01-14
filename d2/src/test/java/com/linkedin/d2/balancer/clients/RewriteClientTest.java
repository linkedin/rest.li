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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import com.linkedin.r2.message.RequestContext;
import org.testng.annotations.Test;

import com.linkedin.d2.balancer.clients.TrackerClientTest.TestClient;
import com.linkedin.d2.balancer.clients.TrackerClientTest.TestTransportCallback;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rpc.RpcRequest;
import com.linkedin.r2.message.rpc.RpcRequestBuilder;
import com.linkedin.r2.message.rpc.RpcResponse;

public class RewriteClientTest
{
  @Test(groups = { "small", "back-end" })
  public void testClient() throws URISyntaxException
  {
    URI uri = URI.create("http://test.linkedin.com/test");
    String serviceName = "HistoryService";
    TestClient wrappedClient = new TestClient();
    RewriteClient client = new RewriteClient(serviceName, uri, wrappedClient);

    assertEquals(client.getUri(), uri);
    assertEquals(client.getServiceName(), serviceName);
    assertEquals(client.getWrappedClient(), wrappedClient);

    RestRequest restRequest = new RestRequestBuilder(URI.create("d2://HistoryService/getCube")).build();
    Map<String, String> restWireAttrs = new HashMap<String, String>();
    TestTransportCallback<RestResponse> restCallback =
        new TestTransportCallback<RestResponse>();

    client.restRequest(restRequest, new RequestContext(), restWireAttrs, restCallback);

    assertFalse(restCallback.response.hasError());
    assertEquals(wrappedClient.restRequest.getHeaders(), restRequest.getHeaders());
    assertEquals(wrappedClient.restRequest.getEntity(), restRequest.getEntity());
    assertEquals(wrappedClient.restRequest.getMethod(), restRequest.getMethod());

    // check the rewrite
    assertEquals(wrappedClient.restRequest.getURI(),
                 URI.create("http://test.linkedin.com/test/getCube"));
    assertEquals(wrappedClient.restWireAttrs, restWireAttrs);

    RpcRequest rpcRequest = new RpcRequestBuilder(URI.create("d2://HistoryService/getCube")).build();
    Map<String, String> rpcWireAttrs = new HashMap<String, String>();
    TestTransportCallback<RpcResponse> rpcCallback =
        new TestTransportCallback<RpcResponse>();

    client.rpcRequest(rpcRequest, new RequestContext(), rpcWireAttrs, rpcCallback);

    assertTrue(rpcCallback.response.hasError());
    assertEquals(wrappedClient.rpcRequest.getURI(),
                 URI.create("http://test.linkedin.com/test/getCube"));
    assertEquals(wrappedClient.rpcRequest.getEntity(), rpcRequest.getEntity());
    assertEquals(wrappedClient.rpcWireAttrs, rpcWireAttrs);
  }

  @Test
  public void testWithQueryAndFragment()
  {
    URI uri = URI.create("http://test.linkedin.com/test");
    String serviceName = "HistoryService";
    TestClient wrappedClient = new TestClient();
    RewriteClient client = new RewriteClient(serviceName, uri, wrappedClient);

    assertEquals(client.getUri(), uri);
    assertEquals(client.getServiceName(), serviceName);
    assertEquals(client.getWrappedClient(), wrappedClient);

    RestRequest restRequest = new RestRequestBuilder(URI.create("d2://HistoryService/getCube?bar=baz#fragId")).build();
    Map<String, String> restWireAttrs = new HashMap<String, String>();
    TestTransportCallback<RestResponse> restCallback =
        new TestTransportCallback<RestResponse>();

    client.restRequest(restRequest, new RequestContext(), restWireAttrs, restCallback);

    assertFalse(restCallback.response.hasError());
    assertEquals(wrappedClient.restRequest.getHeaders(), restRequest.getHeaders());
    assertEquals(wrappedClient.restRequest.getEntity(), restRequest.getEntity());
    assertEquals(wrappedClient.restRequest.getMethod(), restRequest.getMethod());
    assertEquals(wrappedClient.restRequest.getURI(), URI.create("http://test.linkedin.com/test/getCube?bar=baz#fragId"));

  }

  @Test
  public void testWithQueryEscaping()
  {
    URI uri = URI.create("http://test.linkedin.com/test");
    String serviceName = "HistoryService";
    TestClient wrappedClient = new TestClient();
    RewriteClient client = new RewriteClient(serviceName, uri, wrappedClient);

    assertEquals(client.getUri(), uri);
    assertEquals(client.getServiceName(), serviceName);
    assertEquals(client.getWrappedClient(), wrappedClient);

    RestRequest restRequest = new RestRequestBuilder(URI.create("d2://HistoryService/getCube?ids=foo%3D1%26bar%3D1&ids=foo%3D2%26bar%3D2")).build();
    Map<String, String> restWireAttrs = new HashMap<String, String>();
    TestTransportCallback<RestResponse> restCallback =
            new TestTransportCallback<RestResponse>();

    client.restRequest(restRequest, new RequestContext(), restWireAttrs, restCallback);

    assertFalse(restCallback.response.hasError());
    assertEquals(wrappedClient.restRequest.getHeaders(), restRequest.getHeaders());
    assertEquals(wrappedClient.restRequest.getEntity(), restRequest.getEntity());
    assertEquals(wrappedClient.restRequest.getMethod(), restRequest.getMethod());
    assertEquals(wrappedClient.restRequest.getURI(), URI.create("http://test.linkedin.com/test/getCube?ids=foo%3D1%26bar%3D1&ids=foo%3D2%26bar%3D2"));

  }

  @Test
  public void testWithEverything()
  {
    URI uri = URI.create("http://username:password@test.linkedin.com:9876/test");
    String serviceName = "HistoryService";
    TestClient wrappedClient = new TestClient();
    RewriteClient client = new RewriteClient(serviceName, uri, wrappedClient);

    assertEquals(client.getUri(), uri);
    assertEquals(client.getServiceName(), serviceName);
    assertEquals(client.getWrappedClient(), wrappedClient);

    RestRequest restRequest = new RestRequestBuilder(URI.create("d2://HistoryService/getCube?bar=baz#fragId")).build();
    Map<String, String> restWireAttrs = new HashMap<String, String>();
    TestTransportCallback<RestResponse> restCallback =
        new TestTransportCallback<RestResponse>();

    client.restRequest(restRequest, new RequestContext(), restWireAttrs, restCallback);

    assertFalse(restCallback.response.hasError());
    assertEquals(wrappedClient.restRequest.getHeaders(), restRequest.getHeaders());
    assertEquals(wrappedClient.restRequest.getEntity(), restRequest.getEntity());
    assertEquals(wrappedClient.restRequest.getMethod(), restRequest.getMethod());
    assertEquals(wrappedClient.restRequest.getURI(), URI.create("http://username:password@test.linkedin.com:9876/test/getCube?bar=baz#fragId"));
  }

  @Test
  public void testPathAppend()
  {
    URI uri = URI.create("http://test.linkedin.com:9876/test");
    String serviceName = "HistoryService";
    TestClient wrappedClient = new TestClient();
    RewriteClient client = new RewriteClient(serviceName, uri, wrappedClient);

    assertEquals(client.getUri(), uri);
    assertEquals(client.getServiceName(), serviceName);
    assertEquals(client.getWrappedClient(), wrappedClient);

    RestRequest restRequest;
    Map<String, String> restWireAttrs = new HashMap<String, String>();
    TestTransportCallback<RestResponse> restCallback =
            new TestTransportCallback<RestResponse>();

    restRequest = new RestRequestBuilder(URI.create("d2://HistoryService")).build();
    client.restRequest(restRequest, new RequestContext(), restWireAttrs, restCallback);

    checkRewrite(wrappedClient, restRequest, restCallback, "http://test.linkedin.com:9876/test");

    restRequest = new RestRequestBuilder(URI.create("d2://HistoryService/")).build();
    client.restRequest(restRequest, new RequestContext(), restWireAttrs, restCallback);

    checkRewrite(wrappedClient, restRequest, restCallback, "http://test.linkedin.com:9876/test/");

    restRequest = new RestRequestBuilder(URI.create("d2://HistoryService//")).build();
    client.restRequest(restRequest, new RequestContext(), restWireAttrs, restCallback);

    checkRewrite(wrappedClient, restRequest, restCallback, "http://test.linkedin.com:9876/test//");

    restRequest = new RestRequestBuilder(URI.create("d2://HistoryService/foo")).build();
    client.restRequest(restRequest, new RequestContext(), restWireAttrs, restCallback);

    checkRewrite(wrappedClient, restRequest, restCallback, "http://test.linkedin.com:9876/test/foo");

    restRequest = new RestRequestBuilder(URI.create("d2://HistoryService/foo/")).build();
    client.restRequest(restRequest, new RequestContext(), restWireAttrs, restCallback);

    checkRewrite(wrappedClient, restRequest, restCallback, "http://test.linkedin.com:9876/test/foo/");
  }

  private void checkRewrite(TestClient wrappedClient,
                            RestRequest restRequest,
                            TestTransportCallback<RestResponse> restCallback,
                            String expectedURI)
  {
    assertFalse(restCallback.response.hasError());
    assertEquals(wrappedClient.restRequest.getHeaders(), restRequest.getHeaders());
    assertEquals(wrappedClient.restRequest.getEntity(), restRequest.getEntity());
    assertEquals(wrappedClient.restRequest.getMethod(), restRequest.getMethod());
    assertEquals(wrappedClient.restRequest.getURI(), URI.create(expectedURI));
  }
}
