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
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class RewriteLoadBalancerClientTest
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

    RestRequest restRequest = new RestRequestBuilder(URI.create("d2://" + serviceName + path)).build();
    Map<String, String> restWireAttrs = new HashMap<String, String>();
    TestTransportCallback<RestResponse> restCallback =
        new TestTransportCallback<RestResponse>();

    client.restRequest(restRequest, new RequestContext(), restWireAttrs, restCallback);

    assertFalse(restCallback.response.hasError());
    assertEquals(wrappedClient.restRequest.getHeaders(), restRequest.getHeaders());
    assertEquals(wrappedClient.restRequest.getEntity(), restRequest.getEntity());
    assertEquals(wrappedClient.restRequest.getMethod(), restRequest.getMethod());
    assertEquals(wrappedClient.restRequest.getURI(), URI.create(hostUri + path));

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
    RewriteLoadBalancerClient client = new RewriteLoadBalancerClient(serviceName, uri, wrappedClient);

    assertEquals(client.getUri(), uri);
    assertEquals(client.getServiceName(), serviceName);

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
