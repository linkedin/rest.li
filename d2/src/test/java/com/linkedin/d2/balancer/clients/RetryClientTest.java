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
package com.linkedin.d2.balancer.clients;

import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.LoadBalancerState;
import com.linkedin.d2.balancer.PartitionedLoadBalancerTestState;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyConfig;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyV3;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessException;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessor;
import com.linkedin.data.ByteString;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import java.util.Arrays;
import java.util.Collections;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;


/**
 * Created by xzhu on 8/27/14.
 */
public class RetryClientTest
{
  private static final ByteString CONTENT = ByteString.copy(new byte[8092]);

  @Test
  public void testRestRetry() throws Exception
  {
    SimpleLoadBalancer balancer = prepareLoadBalancer(Arrays.asList("http://test.linkedin.com/retry1", "http://test.linkedin.com/good"));

    DynamicClient dynamicClient = new DynamicClient(balancer, null);
    RetryClient client = new RetryClient(dynamicClient, 3);
    URI uri = URI.create("d2://retryService?arg1arg2");
    RestRequest restRequest = new RestRequestBuilder(uri).setEntity(CONTENT).build();
    TrackerClientTest.TestCallback<RestResponse> restCallback = new TrackerClientTest.TestCallback<RestResponse>();
    client.restRequest(restRequest, restCallback);

    assertNull(restCallback.e);
    assertNotNull(restCallback.t);
  }

  @Test
  public void testStreamRetry() throws Exception
  {
    SimpleLoadBalancer balancer = prepareLoadBalancer(Arrays.asList("http://test.linkedin.com/retry1", "http://test.linkedin.com/good"));

    DynamicClient dynamicClient = new DynamicClient(balancer, null);
    RetryClient client = new RetryClient(dynamicClient, 3);
    URI uri = URI.create("d2://retryService?arg1arg2");
    StreamRequest streamRequest = new StreamRequestBuilder(uri).build(EntityStreams.newEntityStream(new ByteStringWriter(CONTENT)));
    TrackerClientTest.TestCallback<StreamResponse> restCallback = new TrackerClientTest.TestCallback<StreamResponse>();
    client.streamRequest(streamRequest, restCallback);

    assertNull(restCallback.e);
    assertNotNull(restCallback.t);
  }

  @Test
  public void testRestException() throws Exception
  {
    SimpleLoadBalancer balancer = prepareLoadBalancer(Arrays.asList("http://test.linkedin.com/retry1", "http://test.linkedin.com/bad"));

    DynamicClient dynamicClient = new DynamicClient(balancer, null);
    RetryClient client = new RetryClient(dynamicClient, 3);
    URI uri = URI.create("d2://retryService?arg1=empty&arg2=empty");
    RestRequest restRequest = new RestRequestBuilder(uri).build();
    TrackerClientTest.TestCallback<RestResponse> restCallback = new TrackerClientTest.TestCallback<RestResponse>();

    RequestContext context = new RequestContext();
    KeyMapper.TargetHostHints.setRequestContextTargetHost(context, URI.create("http://test.linkedin.com/bad"));
    client.restRequest(restRequest, context, restCallback);

    assertNull(restCallback.t);
    assertNotNull(restCallback.e);
    assertTrue(restCallback.e.getMessage().contains("exception happens"));
  }

  @Test
  public void testStreamException() throws Exception
  {
    SimpleLoadBalancer balancer = prepareLoadBalancer(Arrays.asList("http://test.linkedin.com/retry1", "http://test.linkedin.com/bad"));

    DynamicClient dynamicClient = new DynamicClient(balancer, null);
    RetryClient client = new RetryClient(dynamicClient, 3);
    URI uri = URI.create("d2://retryService?arg1=empty&arg2=empty");
    StreamRequest streamRequest = new StreamRequestBuilder(uri).build(EntityStreams.emptyStream());
    TrackerClientTest.TestCallback<StreamResponse> streamCallback = new TrackerClientTest.TestCallback<StreamResponse>();

    RequestContext context = new RequestContext();
    KeyMapper.TargetHostHints.setRequestContextTargetHost(context, URI.create("http://test.linkedin.com/bad"));
    client.streamRequest(streamRequest, context, streamCallback);

    assertNull(streamCallback.t);
    assertNotNull(streamCallback.e);
    assertTrue(streamCallback.e.getMessage().contains("exception happens"));
  }

  @Test
  public void testRestRetryOverLimit() throws Exception
  {
    SimpleLoadBalancer balancer = prepareLoadBalancer(Arrays.asList("http://test.linkedin.com/retry1", "http://test.linkedin.com/retry2"));

    DynamicClient dynamicClient = new DynamicClient(balancer, null);
    RetryClient client = new RetryClient(dynamicClient, 1);
    URI uri = URI.create("d2://retryService?arg1=empty&arg2=empty");
    RestRequest restRequest = new RestRequestBuilder(uri).build();
    TrackerClientTest.TestCallback<RestResponse> restCallback = new TrackerClientTest.TestCallback<RestResponse>();
    client.restRequest(restRequest, restCallback);

    assertNull(restCallback.t);
    assertNotNull(restCallback.e);
    assertTrue(restCallback.e.getMessage().contains("Data not available"));
  }

  @Test
  public void testStreamRetryOverLimit() throws Exception
  {
    SimpleLoadBalancer balancer = prepareLoadBalancer(Arrays.asList("http://test.linkedin.com/retry1", "http://test.linkedin.com/retry2"));

    DynamicClient dynamicClient = new DynamicClient(balancer, null);
    RetryClient client = new RetryClient(dynamicClient, 1);
    URI uri = URI.create("d2://retryService?arg1=empty&arg2=empty");
    StreamRequest streamRequest = new StreamRequestBuilder(uri).build(EntityStreams.emptyStream());
    TrackerClientTest.TestCallback<StreamResponse> streamCallback = new TrackerClientTest.TestCallback<StreamResponse>();
    client.streamRequest(streamRequest, streamCallback);

    assertNull(streamCallback.t);
    assertNotNull(streamCallback.e);
    assertTrue(streamCallback.e.getMessage().contains("Data not available"));
  }

  @Test
  public void testRestRetryNoAvailableHosts() throws Exception
  {
    SimpleLoadBalancer balancer = prepareLoadBalancer(Arrays.asList("http://test.linkedin.com/retry1", "http://test.linkedin.com/retry2"));

    DynamicClient dynamicClient = new DynamicClient(balancer, null);
    RetryClient client = new RetryClient(dynamicClient, 3);
    URI uri = URI.create("d2://retryService?arg1=empty&arg2=empty");
    RestRequest restRequest = new RestRequestBuilder(uri).build();
    TrackerClientTest.TestCallback<RestResponse> restCallback = new TrackerClientTest.TestCallback<RestResponse>();
    client.restRequest(restRequest, restCallback);

    assertNull(restCallback.t);
    assertNotNull(restCallback.e);
    assertTrue(restCallback.e.toString().contains("retryService is in a bad state"));
  }

  @Test
  public void testStreamRetryNoAvailableHosts() throws Exception
  {
    SimpleLoadBalancer balancer = prepareLoadBalancer(Arrays.asList("http://test.linkedin.com/retry1", "http://test.linkedin.com/retry2"));

    DynamicClient dynamicClient = new DynamicClient(balancer, null);
    RetryClient client = new RetryClient(dynamicClient, 3);
    URI uri = URI.create("d2://retryService?arg1=empty&arg2=empty");
    StreamRequest streamRequest = new StreamRequestBuilder(uri).build(EntityStreams.emptyStream());
    TrackerClientTest.TestCallback<StreamResponse> streamCallback = new TrackerClientTest.TestCallback<StreamResponse>();
    client.streamRequest(streamRequest, streamCallback);

    assertNull(streamCallback.t);
    assertNotNull(streamCallback.e);
    assertTrue(streamCallback.e.toString().contains("retryService is in a bad state"));
  }

  public SimpleLoadBalancer prepareLoadBalancer(List<String> uris) throws URISyntaxException
  {
    String serviceName = "retryService";
    String clusterName = "cluster";
    String path = "";
    String strategyName = "degrader";

    // setup partition
    Map<URI,Map<Integer, PartitionData>> partitionDescriptions = new HashMap<URI, Map<Integer, PartitionData>>();
    for (String uri : uris)
    {
      final URI foo = URI.create(uri);
      Map<Integer, PartitionData> foo1Data = new HashMap<Integer, PartitionData>();
      foo1Data.put(0, new PartitionData(1.0));
      partitionDescriptions.put(foo, foo1Data);
    }

    DegraderLoadBalancerStrategyV3 strategy = new DegraderLoadBalancerStrategyV3(
        new DegraderLoadBalancerStrategyConfig(5000), serviceName,
        null, Collections.emptyList());
    List<LoadBalancerState.SchemeStrategyPair> orderedStrategies = new ArrayList<LoadBalancerState.SchemeStrategyPair>();
    orderedStrategies.add(new LoadBalancerState.SchemeStrategyPair("http", strategy));

    PartitionAccessor accessor = new TestRetryPartitionAccessor();

    SimpleLoadBalancer balancer = new SimpleLoadBalancer(new PartitionedLoadBalancerTestState(
            clusterName, serviceName, path, strategyName, partitionDescriptions, orderedStrategies,
            accessor
    ));

    return balancer;
  }

  private class TestRetryPartitionAccessor implements PartitionAccessor
  {
    @Override
    public int getPartitionId(URI uri)
            throws PartitionAccessException
    {
      return 0;
    }

    @Override
    public int getPartitionId(String key)
            throws PartitionAccessException
    {
      return 0;
    }

    @Override
    public int getMaxPartitionId()
    {
      return 0;
    }

  }
}
