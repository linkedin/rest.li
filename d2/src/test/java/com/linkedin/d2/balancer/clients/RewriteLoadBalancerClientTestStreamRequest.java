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


import com.linkedin.d2.balancer.clients.TestClient;
import com.linkedin.d2.balancer.clients.TrackerClientTest.TestTransportCallback;
import com.linkedin.r2.message.RequestContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class RewriteLoadBalancerClientTestStreamRequest
{
  @Test(groups = { "small", "back-end" })
  public void testClient() throws URISyntaxException
  {
    URI uri = URI.create("http://test.linkedin.com/test");
    String serviceName = "HistoryService";
    TestClient wrappedClient = new TestClient();
    RewriteLoadBalancerClient client = new RewriteLoadBalancerClient(serviceName, uri, wrappedClient);

    assertEquals(client.getUri(), uri);
    assertEquals(client.getServiceName(), serviceName);

    StreamRequest streamRequest = new StreamRequestBuilder(URI.create("d2://HistoryService/getCube")).build(EntityStreams.emptyStream());
    Map<String, String> restWireAttrs = new HashMap<String, String>();
    TestTransportCallback<StreamResponse> restCallback =
        new TestTransportCallback<StreamResponse>();

    client.streamRequest(streamRequest, new RequestContext(), restWireAttrs, restCallback);

    assertFalse(restCallback.response.hasError());
    assertEquals(wrappedClient.streamRequest.getHeaders(), streamRequest.getHeaders());
    assertEquals(wrappedClient.streamRequest.getMethod(), streamRequest.getMethod());

    // check the rewrite
    assertEquals(wrappedClient.streamRequest.getURI(),
                 URI.create("http://test.linkedin.com/test/getCube"));
    assertEquals(wrappedClient.restWireAttrs, restWireAttrs);
  }

  @Test
  public void testWithQueryAndFragment()
  {
    URI uri = URI.create("http://test.linkedin.com/test");
    String serviceName = "HistoryService";
    TestClient wrappedClient = new TestClient();
    RewriteLoadBalancerClient client = new RewriteLoadBalancerClient(serviceName, uri, wrappedClient);

    assertEquals(client.getUri(), uri);
    assertEquals(client.getServiceName(), serviceName);

    StreamRequest streamRequest = getRequest("d2://HistoryService/getCube?bar=baz#fragId");
    Map<String, String> restWireAttrs = new HashMap<String, String>();
    TestTransportCallback<StreamResponse> restCallback =
        new TestTransportCallback<StreamResponse>();

    client.streamRequest(streamRequest, new RequestContext(), restWireAttrs, restCallback);

    assertFalse(restCallback.response.hasError());
    assertEquals(wrappedClient.streamRequest.getHeaders(), streamRequest.getHeaders());
    assertEquals(wrappedClient.streamRequest.getMethod(), streamRequest.getMethod());
    assertEquals(wrappedClient.streamRequest.getURI(), URI.create("http://test.linkedin.com/test/getCube?bar=baz#fragId"));

  }

  @Test
  public void testWithQueryEscaping()
  {
    String hostUri = "http://test.linkedin.com/test";
    String serviceName = "HistoryService";
    String pathWithQueryEscaping = "/getCube?ids=foo%3D1%26bar%3D1&ids=foo%3D2%26bar%3D2";
    testEscapingHelper(hostUri, serviceName, pathWithQueryEscaping);
  }

  @Test
  public void testEscapingWithoutQuery()
  {
    String hostUri = "http://test.linkedin.com/test";
    String serviceName = "socialActivitiesStats";
    String escapedPath = "/http%3A%2F%2Fwww.techmeme.com%2F131223%2Fp13";
    testEscapingHelper(hostUri, serviceName, escapedPath);
  }

  @Test
  public void testEscapingInFragment()
  {
    String hostUri = "http://test.linkedin.com/test";
    String serviceName = "socialActivitiesStats";
    String escapedPathWithFragment = "/http%3A%2F%2Fwww.techmeme.com%2F131223%2Fp13#http%3A%2F%2F55";
    testEscapingHelper(hostUri, serviceName, escapedPathWithFragment);
  }

  private void testEscapingHelper(String hostUri, String serviceName, String path)
  {
    URI uri = URI.create(hostUri);
    TestClient wrappedClient = new TestClient();
    RewriteLoadBalancerClient client = new RewriteLoadBalancerClient(serviceName, uri, wrappedClient);

    assertEquals(client.getUri(), uri);
    assertEquals(client.getServiceName(), serviceName);

    StreamRequest streamRequest = getRequest("d2://" + serviceName + path);
    Map<String, String> restWireAttrs = new HashMap<String, String>();
    TestTransportCallback<StreamResponse> restCallback =
        new TestTransportCallback<StreamResponse>();

    client.streamRequest(streamRequest, new RequestContext(), restWireAttrs, restCallback);

    assertFalse(restCallback.response.hasError());
    assertEquals(wrappedClient.streamRequest.getHeaders(), streamRequest.getHeaders());
    assertEquals(wrappedClient.streamRequest.getMethod(), streamRequest.getMethod());
    assertEquals(wrappedClient.streamRequest.getURI(), URI.create(hostUri + path));

  }

  @Test
  public void testWithEverything()
  {
    URI uri = URI.create("http://username:password@test.linkedin.com:9876/test");
    String serviceName = "HistoryService";
    TestClient wrappedClient = new TestClient();
    RewriteLoadBalancerClient client = new RewriteLoadBalancerClient(serviceName, uri, wrappedClient);

    assertEquals(client.getUri(), uri);
    assertEquals(client.getServiceName(), serviceName);

    StreamRequest streamRequest = getRequest("d2://HistoryService/getCube?bar=baz#fragId");
    Map<String, String> restWireAttrs = new HashMap<String, String>();
    TestTransportCallback<StreamResponse> restCallback =
        new TestTransportCallback<StreamResponse>();

    client.streamRequest(streamRequest, new RequestContext(), restWireAttrs, restCallback);

    assertFalse(restCallback.response.hasError());
    assertEquals(wrappedClient.streamRequest.getHeaders(), streamRequest.getHeaders());
    assertEquals(wrappedClient.streamRequest.getMethod(), streamRequest.getMethod());
    assertEquals(wrappedClient.streamRequest.getURI(), URI.create("http://username:password@test.linkedin.com:9876/test/getCube?bar=baz#fragId"));
  }

  @Test
  public void testPathAppend()
  {
    URI uri = URI.create("http://test.linkedin.com:9876/test");
    String serviceName = "HistoryService";
    TestClient wrappedClient = new TestClient();
    RewriteLoadBalancerClient client = new RewriteLoadBalancerClient(serviceName, uri, wrappedClient);

    assertEquals(client.getUri(), uri);
    assertEquals(client.getServiceName(), serviceName);

    StreamRequest streamRequest;
    Map<String, String> restWireAttrs = new HashMap<String, String>();
    TestTransportCallback<StreamResponse> restCallback =
            new TestTransportCallback<StreamResponse>();

    streamRequest = getRequest("d2://HistoryService");
    client.streamRequest(streamRequest, new RequestContext(), restWireAttrs, restCallback);

    checkRewrite(wrappedClient, streamRequest, restCallback, "http://test.linkedin.com:9876/test");

    streamRequest = getRequest("d2://HistoryService/");
    client.streamRequest(streamRequest, new RequestContext(), restWireAttrs, restCallback);

    checkRewrite(wrappedClient, streamRequest, restCallback, "http://test.linkedin.com:9876/test/");

    streamRequest = getRequest("d2://HistoryService//");
    client.streamRequest(streamRequest, new RequestContext(), restWireAttrs, restCallback);

    checkRewrite(wrappedClient, streamRequest, restCallback, "http://test.linkedin.com:9876/test//");

    streamRequest = getRequest("d2://HistoryService/foo");
    client.streamRequest(streamRequest, new RequestContext(), restWireAttrs, restCallback);

    checkRewrite(wrappedClient, streamRequest, restCallback, "http://test.linkedin.com:9876/test/foo");

    streamRequest = getRequest("d2://HistoryService/foo/");
    client.streamRequest(streamRequest, new RequestContext(), restWireAttrs, restCallback);

    checkRewrite(wrappedClient, streamRequest, restCallback, "http://test.linkedin.com:9876/test/foo/");
  }

  private void checkRewrite(TestClient wrappedClient,
                            StreamRequest streamRequest,
                            TestTransportCallback<StreamResponse> restCallback,
                            String expectedURI)
  {
    assertFalse(restCallback.response.hasError());
    assertEquals(wrappedClient.streamRequest.getHeaders(), streamRequest.getHeaders());
    assertEquals(wrappedClient.streamRequest.getMethod(), streamRequest.getMethod());
    assertEquals(wrappedClient.streamRequest.getURI(), URI.create(expectedURI));
  }

  private static StreamRequest getRequest(String uri)
  {
    return new StreamRequestBuilder(URI.create(uri)).build(EntityStreams.emptyStream());
  }
}
