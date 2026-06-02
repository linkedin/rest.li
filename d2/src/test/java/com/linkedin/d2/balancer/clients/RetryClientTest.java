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

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.d2.balancer.D2ClientConfig;
import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.LoadBalancerState;
import com.linkedin.d2.balancer.PartitionedLoadBalancerTestState;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy.ExcludedHostHints;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyConfig;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyV3;
import com.linkedin.d2.balancer.util.hashing.HashFunction;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessException;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessor;
import com.linkedin.data.ByteString;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.util.NamedThreadFactory;
import com.linkedin.test.util.ClockedExecutor;
import com.linkedin.util.clock.SettableClock;
import com.linkedin.util.clock.SystemClock;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;


/**
 * Created by xzhu on 8/27/14.
 */
public class RetryClientTest
{
  private static final ByteString CONTENT = ByteString.copy(new byte[8092]);

  private ScheduledExecutorService _executor;

  @BeforeSuite
  public void initialize()
  {
    _executor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("D2 PropertyEventExecutor for Tests"));
  }

  @AfterSuite
  public void shutdown()
  {
    _executor.shutdown();
  }

  @Test
  public void testRestRetry() throws Exception
  {
    SimpleLoadBalancer balancer = prepareLoadBalancer(Arrays.asList("http://test.linkedin.com/retry1", "http://test.linkedin.com/good"),
        HttpClientFactory.UNLIMITED_CLIENT_REQUEST_RETRY_RATIO);

    DynamicClient dynamicClient = new DynamicClient(balancer, null);
    RetryClient client = new RetryClient(
        dynamicClient,
        balancer,
        D2ClientConfig.DEFAULT_RETRY_LIMIT,
        RetryClient.DEFAULT_UPDATE_INTERVAL_MS,
        RetryClient.DEFAULT_AGGREGATED_INTERVAL_NUM,
        SystemClock.instance(),
        true,
        false);
    URI uri = URI.create("d2://retryService?arg1arg2");
    RestRequest restRequest = new RestRequestBuilder(uri).setEntity(CONTENT).build();
    DegraderTrackerClientTest.TestCallback<RestResponse> restCallback = new DegraderTrackerClientTest.TestCallback<>();
    client.restRequest(restRequest, restCallback);

    assertNull(restCallback.e);
    assertNotNull(restCallback.t);
  }

  @Test
  public void testStreamRetry() throws Exception
  {
    SimpleLoadBalancer balancer = prepareLoadBalancer(Arrays.asList("http://test.linkedin.com/retry1", "http://test.linkedin.com/good"),
        HttpClientFactory.UNLIMITED_CLIENT_REQUEST_RETRY_RATIO);

    DynamicClient dynamicClient = new DynamicClient(balancer, null);
    RetryClient client = new RetryClient(
        dynamicClient,
        balancer,
        D2ClientConfig.DEFAULT_RETRY_LIMIT,
        RetryClient.DEFAULT_UPDATE_INTERVAL_MS,
        RetryClient.DEFAULT_AGGREGATED_INTERVAL_NUM,
        SystemClock.instance(),
        true,
        true);
    URI uri = URI.create("d2://retryService?arg1arg2");
    StreamRequest streamRequest = new StreamRequestBuilder(uri).build(EntityStreams.newEntityStream(new ByteStringWriter(CONTENT)));
    DegraderTrackerClientTest.TestCallback<StreamResponse> restCallback = new DegraderTrackerClientTest.TestCallback<>();
    client.streamRequest(streamRequest, restCallback);

    assertNull(restCallback.e);
    assertNotNull(restCallback.t);
  }

  @Test
  public void testIgnoreStreamRetry() throws Exception
  {
    // Only include retry URIs: stream retry is disabled so "good" is never needed and its presence
    // caused non-deterministic host selection (consistent-hash could pick "good" first).
    SimpleLoadBalancer balancer = prepareLoadBalancer(Arrays.asList("http://test.linkedin.com/retry1"),
        HttpClientFactory.UNLIMITED_CLIENT_REQUEST_RETRY_RATIO);

    DynamicClient dynamicClient = new DynamicClient(balancer, null);
    RetryClient client = new RetryClient(
        dynamicClient,
        balancer,
        D2ClientConfig.DEFAULT_RETRY_LIMIT,
        RetryClient.DEFAULT_UPDATE_INTERVAL_MS,
        RetryClient.DEFAULT_AGGREGATED_INTERVAL_NUM,
        SystemClock.instance(),
        true,
        false);
    URI uri = URI.create("d2://retryService?arg1arg2");
    StreamRequest streamRequest = new StreamRequestBuilder(uri).build(EntityStreams.newEntityStream(new ByteStringWriter(CONTENT)));
    DegraderTrackerClientTest.TestCallback<StreamResponse> restCallback = new DegraderTrackerClientTest.TestCallback<>();
    client.streamRequest(streamRequest, restCallback);

    assertNull(restCallback.t);
    assertNotNull(restCallback.e);
    assertTrue(restCallback.e.getMessage().contains("Data not available"));
  }

  @Test
  public void testRestException() throws Exception
  {
    SimpleLoadBalancer balancer = prepareLoadBalancer(Arrays.asList("http://test.linkedin.com/retry1", "http://test.linkedin.com/bad"),
        HttpClientFactory.UNLIMITED_CLIENT_REQUEST_RETRY_RATIO);

    DynamicClient dynamicClient = new DynamicClient(balancer, null);
    RetryClient client = new RetryClient(
        dynamicClient,
        balancer,
        D2ClientConfig.DEFAULT_RETRY_LIMIT,
        RetryClient.DEFAULT_UPDATE_INTERVAL_MS,
        RetryClient.DEFAULT_AGGREGATED_INTERVAL_NUM,
        SystemClock.instance(),
        true,
        false);
    URI uri = URI.create("d2://retryService?arg1=empty&arg2=empty");
    RestRequest restRequest = new RestRequestBuilder(uri).build();
    DegraderTrackerClientTest.TestCallback<RestResponse> restCallback = new DegraderTrackerClientTest.TestCallback<>();

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
    SimpleLoadBalancer balancer = prepareLoadBalancer(Arrays.asList("http://test.linkedin.com/retry1", "http://test.linkedin.com/bad"),
        HttpClientFactory.UNLIMITED_CLIENT_REQUEST_RETRY_RATIO);

    DynamicClient dynamicClient = new DynamicClient(balancer, null);
    RetryClient client = new RetryClient(
        dynamicClient,
        balancer,
        D2ClientConfig.DEFAULT_RETRY_LIMIT,
        RetryClient.DEFAULT_UPDATE_INTERVAL_MS,
        RetryClient.DEFAULT_AGGREGATED_INTERVAL_NUM,
        SystemClock.instance(),
        true,
        true);
    URI uri = URI.create("d2://retryService?arg1=empty&arg2=empty");
    StreamRequest streamRequest = new StreamRequestBuilder(uri).build(EntityStreams.emptyStream());
    DegraderTrackerClientTest.TestCallback<StreamResponse> streamCallback = new DegraderTrackerClientTest.TestCallback<>();

    RequestContext context = new RequestContext();
    KeyMapper.TargetHostHints.setRequestContextTargetHost(context, URI.create("http://test.linkedin.com/bad"));
    client.streamRequest(streamRequest, context, streamCallback);

    assertNull(streamCallback.t);
    assertNotNull(streamCallback.e);
    assertTrue(streamCallback.e.getMessage().contains("exception happens"), streamCallback.e.getMessage());
  }

  @Test
  public void testRestRetryOverLimit() throws Exception
  {
    SimpleLoadBalancer balancer = prepareLoadBalancer(Arrays.asList("http://test.linkedin.com/retry1", "http://test.linkedin.com/retry2"),
        HttpClientFactory.UNLIMITED_CLIENT_REQUEST_RETRY_RATIO);

    DynamicClient dynamicClient = new DynamicClient(balancer, null);
    RetryClient client = new RetryClient(
        dynamicClient,
        balancer,
        1,
        RetryClient.DEFAULT_UPDATE_INTERVAL_MS,
        RetryClient.DEFAULT_AGGREGATED_INTERVAL_NUM,
        SystemClock.instance(),
        true,
        false);
    URI uri = URI.create("d2://retryService?arg1=empty&arg2=empty");
    RestRequest restRequest = new RestRequestBuilder(uri).build();
    DegraderTrackerClientTest.TestCallback<RestResponse> restCallback = new DegraderTrackerClientTest.TestCallback<>();
    client.restRequest(restRequest, restCallback);

    assertNull(restCallback.t);
    assertNotNull(restCallback.e);
    assertTrue(restCallback.e.getMessage().contains("Data not available"));
  }

  @Test
  public void testStreamRetryOverLimit() throws Exception
  {
    SimpleLoadBalancer balancer = prepareLoadBalancer(Arrays.asList("http://test.linkedin.com/retry1", "http://test.linkedin.com/retry2"),
        HttpClientFactory.UNLIMITED_CLIENT_REQUEST_RETRY_RATIO);

    DynamicClient dynamicClient = new DynamicClient(balancer, null);
    RetryClient client = new RetryClient(
        dynamicClient,
        balancer,
        1,
        RetryClient.DEFAULT_UPDATE_INTERVAL_MS,
        RetryClient.DEFAULT_AGGREGATED_INTERVAL_NUM,
        SystemClock.instance(),
        true,
        true);
    URI uri = URI.create("d2://retryService?arg1=empty&arg2=empty");
    StreamRequest streamRequest = new StreamRequestBuilder(uri).build(EntityStreams.emptyStream());
    DegraderTrackerClientTest.TestCallback<StreamResponse> streamCallback = new DegraderTrackerClientTest.TestCallback<>();
    client.streamRequest(streamRequest, streamCallback);

    assertNull(streamCallback.t);
    assertNotNull(streamCallback.e);
    assertTrue(streamCallback.e.getMessage().contains("Data not available"));
  }

  @Test
  public void testRestRetryNoAvailableHosts() throws Exception
  {
    SimpleLoadBalancer balancer = prepareLoadBalancer(Arrays.asList("http://test.linkedin.com/retry1", "http://test.linkedin.com/retry2"),
        HttpClientFactory.UNLIMITED_CLIENT_REQUEST_RETRY_RATIO);

    DynamicClient dynamicClient = new DynamicClient(balancer, null);
    RetryClient client = new RetryClient(
        dynamicClient,
        balancer,
        D2ClientConfig.DEFAULT_RETRY_LIMIT,
        RetryClient.DEFAULT_UPDATE_INTERVAL_MS,
        RetryClient.DEFAULT_AGGREGATED_INTERVAL_NUM,
        SystemClock.instance(),
        true,
        false);
    URI uri = URI.create("d2://retryService?arg1=empty&arg2=empty");
    RestRequest restRequest = new RestRequestBuilder(uri).build();
    DegraderTrackerClientTest.TestCallback<RestResponse> restCallback = new DegraderTrackerClientTest.TestCallback<>();
    client.restRequest(restRequest, restCallback);

    assertNull(restCallback.t);
    assertNotNull(restCallback.e);
    assertTrue(restCallback.e.toString().contains("retryService is in a bad state"));
  }

  @Test
  public void testStreamRetryNoAvailableHosts() throws Exception
  {
    SimpleLoadBalancer balancer = prepareLoadBalancer(Arrays.asList("http://test.linkedin.com/retry1", "http://test.linkedin.com/retry2"),
        HttpClientFactory.UNLIMITED_CLIENT_REQUEST_RETRY_RATIO);

    DynamicClient dynamicClient = new DynamicClient(balancer, null);
    RetryClient client = new RetryClient(
        dynamicClient,
        balancer,
        D2ClientConfig.DEFAULT_RETRY_LIMIT,
        RetryClient.DEFAULT_UPDATE_INTERVAL_MS,
        RetryClient.DEFAULT_AGGREGATED_INTERVAL_NUM,
        SystemClock.instance(),
        true,
        true);
    URI uri = URI.create("d2://retryService?arg1=empty&arg2=empty");
    StreamRequest streamRequest = new StreamRequestBuilder(uri).build(EntityStreams.emptyStream());
    FutureCallback<StreamResponse> streamCallback = new FutureCallback<>();
    client.streamRequest(streamRequest, streamCallback);

    try
    {
      streamCallback.get();
    }
    catch (ExecutionException e)
    {
      assertTrue(e.toString().contains("retryService is in a bad state"), e.getMessage());
    }
  }

  @Test
  public void testRestRetryExceedsClientRetryRatio() throws Exception
  {
    // OrderedLoadBalancerStrategy guarantees retry1 → retry2 → good selection order,
    // making the test fully deterministic regardless of JVM consistent-hash ring layout.
    SimpleLoadBalancer balancer = prepareLoadBalancerOrdered(
        Arrays.asList("http://test.linkedin.com/retry1",
                      "http://test.linkedin.com/retry2",
                      "http://test.linkedin.com/good"),
        HttpClientFactory.DEFAULT_MAX_CLIENT_REQUEST_RETRY_RATIO);
    SettableClock clock = new SettableClock();
    DynamicClient dynamicClient = new DynamicClient(balancer, null);
    RetryClient client = new RetryClient(
        dynamicClient,
        balancer,
        D2ClientConfig.DEFAULT_RETRY_LIMIT,
        RetryClient.DEFAULT_UPDATE_INTERVAL_MS,
        RetryClient.DEFAULT_AGGREGATED_INTERVAL_NUM,
        clock,
        true,
        false);
    URI uri1 = URI.create("d2://retryService1?arg1=empty&arg2=empty");
    URI uri2 = URI.create("d2://retryService2?arg1=empty&arg2=empty");

    // This request will be retried (retry1 → retry2 → good) and succeed.
    DegraderTrackerClientTest.TestCallback<RestResponse> restCallback = new DegraderTrackerClientTest.TestCallback<>();
    client.restRequest(new RestRequestBuilder(uri1).build(), new RequestContext(), restCallback);

    assertNull(restCallback.e);
    assertNotNull(restCallback.t);

    // This request will not be retried because the retry ratio is exceeded.
    clock.addDuration(RetryClient.DEFAULT_UPDATE_INTERVAL_MS);

    restCallback = new DegraderTrackerClientTest.TestCallback<>();
    client.restRequest(new RestRequestBuilder(uri1).build(), new RequestContext(), restCallback);

    assertNull(restCallback.t);
    assertNotNull(restCallback.e);
    assertTrue(restCallback.e.getMessage().contains("Data not available"));

    // If the client sends request to a different service endpoint, the retry ratio should not interfere.
    restCallback = new DegraderTrackerClientTest.TestCallback<>();
    client.restRequest(new RestRequestBuilder(uri2).build(), new RequestContext(), restCallback);

    assertNull(restCallback.e);
    assertNotNull(restCallback.t);

    // After 5s interval, retry counter is reset and this request will be retried again.
    clock.addDuration(RetryClient.DEFAULT_UPDATE_INTERVAL_MS * RetryClient.DEFAULT_AGGREGATED_INTERVAL_NUM);

    restCallback = new DegraderTrackerClientTest.TestCallback<>();
    client.restRequest(new RestRequestBuilder(uri1).build(), new RequestContext(), restCallback);

    assertNull(restCallback.e);
    assertNotNull(restCallback.t);
  }

  @Test
  public void testRestRetryUnlimitedClientRetryRatio() throws Exception
  {
    SimpleLoadBalancer balancer = prepareLoadBalancer(Arrays.asList("http://test.linkedin.com/retry1", "http://test.linkedin.com/good"),
        HttpClientFactory.UNLIMITED_CLIENT_REQUEST_RETRY_RATIO);
    ClockedExecutor clock = new ClockedExecutor();
    DynamicClient dynamicClient = new DynamicClient(balancer, null);
    RetryClient client = new RetryClient(
        dynamicClient,
        balancer,
        D2ClientConfig.DEFAULT_RETRY_LIMIT,
        RetryClient.DEFAULT_UPDATE_INTERVAL_MS,
        RetryClient.DEFAULT_AGGREGATED_INTERVAL_NUM,
        clock,
        true,
        false);
    URI uri = URI.create("d2://retryService?arg1=empty&arg2=empty");
    RestRequest restRequest = new RestRequestBuilder(uri).build();

    clock.scheduleWithFixedDelay(() ->
    {
      DegraderTrackerClientTest.TestCallback<RestResponse> restCallback = new DegraderTrackerClientTest.TestCallback<>();
      client.restRequest(restRequest, restCallback);

      // This request will be retried and route to the good host
      assertNull(restCallback.e);
      assertNotNull(restCallback.t);
    }, 0, 100, TimeUnit.MILLISECONDS);

    clock.runFor(RetryClient.DEFAULT_UPDATE_INTERVAL_MS * 2);
  }

  /**
   * Like {@link #prepareLoadBalancer} but uses {@link OrderedLoadBalancerStrategy} instead of
   * {@link DegraderLoadBalancerStrategyV3}. Hosts are always selected in the order given, making
   * tests that depend on pick order fully deterministic.
   */
  public SimpleLoadBalancer prepareLoadBalancerOrdered(List<String> uris, double maxClientRequestRetryRatio)
      throws URISyntaxException
  {
    String serviceName = "retryService";
    String clusterName = "cluster";
    String path = "";
    String strategyName = "degrader";

    Map<URI, Map<Integer, PartitionData>> partitionDescriptions = new HashMap<>();
    List<URI> orderedUris = new ArrayList<>();
    for (String uri : uris)
    {
      URI foo = URI.create(uri);
      orderedUris.add(foo);
      Map<Integer, PartitionData> foo1Data = new HashMap<>();
      foo1Data.put(0, new PartitionData(1.0));
      partitionDescriptions.put(foo, foo1Data);
    }

    LoadBalancerStrategy strategy = new OrderedLoadBalancerStrategy(orderedUris);
    List<LoadBalancerState.SchemeStrategyPair> orderedStrategies = new ArrayList<>();
    orderedStrategies.add(new LoadBalancerState.SchemeStrategyPair("http", strategy));

    PartitionAccessor accessor = new TestRetryPartitionAccessor();

    return new SimpleLoadBalancer(new PartitionedLoadBalancerTestState(
        clusterName, serviceName, path, strategyName, partitionDescriptions, orderedStrategies,
        accessor, maxClientRequestRetryRatio
    ), _executor);
  }

  /**
   * Deterministic {@link LoadBalancerStrategy} for testing. Always selects the first host from a
   * fixed ordered list that is not already in {@link ExcludedHostHints}, then marks it excluded so
   * subsequent retries advance to the next host in order.
   */
  private static class OrderedLoadBalancerStrategy implements LoadBalancerStrategy
  {
    private final List<URI> _orderedUris;

    OrderedLoadBalancerStrategy(List<URI> orderedUris)
    {
      _orderedUris = orderedUris;
    }

    @Override
    public String getName()
    {
      return "ordered";
    }

    @Override
    public TrackerClient getTrackerClient(Request request, RequestContext requestContext,
        long clusterGenerationId, int partitionId, Map<URI, TrackerClient> trackerClients)
    {
      Set<URI> excluded = ExcludedHostHints.getRequestContextExcludedHosts(requestContext);
      if (excluded == null)
      {
        excluded = Collections.emptySet();
      }
      for (URI uri : _orderedUris)
      {
        if (!excluded.contains(uri) && trackerClients.containsKey(uri))
        {
          ExcludedHostHints.addRequestContextExcludedHost(requestContext, uri);
          return trackerClients.get(uri);
        }
      }
      return null;
    }

    @Override
    public Ring<URI> getRing(long clusterGenerationId, int partitionId, Map<URI, TrackerClient> trackerClients)
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public HashFunction<Request> getHashFunction()
    {
      return null;
    }
  }

  public SimpleLoadBalancer prepareLoadBalancer(List<String> uris, double maxClientRequestRetryRatio) throws URISyntaxException
  {
    String serviceName = "retryService";
    String clusterName = "cluster";
    String path = "";
    String strategyName = "degrader";

    // setup partition
    Map<URI,Map<Integer, PartitionData>> partitionDescriptions = new HashMap<>();
    for (String uri : uris)
    {
      final URI foo = URI.create(uri);
      Map<Integer, PartitionData> foo1Data = new HashMap<>();
      // ensure that we first route to the retry uris before the good uris
      double weight = uri.contains("good") ? 0.1 : 1.0;
      foo1Data.put(0, new PartitionData(weight));
      partitionDescriptions.put(foo, foo1Data);
    }

    DegraderLoadBalancerStrategyV3 strategy = new DegraderLoadBalancerStrategyV3(
        new DegraderLoadBalancerStrategyConfig(5000), serviceName,
        null, Collections.emptyList());
    List<LoadBalancerState.SchemeStrategyPair> orderedStrategies = new ArrayList<>();
    orderedStrategies.add(new LoadBalancerState.SchemeStrategyPair("http", strategy));

    PartitionAccessor accessor = new TestRetryPartitionAccessor();

    SimpleLoadBalancer balancer = new SimpleLoadBalancer(new PartitionedLoadBalancerTestState(
            clusterName, serviceName, path, strategyName, partitionDescriptions, orderedStrategies,
            accessor, maxClientRequestRetryRatio
    ), _executor);

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
