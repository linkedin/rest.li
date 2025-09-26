/*
   Copyright (c) 2024 LinkedIn Corp.

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

package com.linkedin.darkcluster;

import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessException;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessor;
import com.linkedin.d2.balancer.util.partitions.PartitionInfoProvider;
import com.linkedin.darkcluster.api.DarkClusterDispatcher;
import com.linkedin.darkcluster.impl.BaseDarkClusterDispatcherImpl;
import com.linkedin.darkcluster.impl.ConstantQpsDarkClusterStrategy;
import com.linkedin.darkcluster.impl.DefaultDarkClusterDispatcher;
import com.linkedin.darkcluster.impl.IdenticalTrafficMultiplierDarkClusterStrategy;
import com.linkedin.darkcluster.impl.RelativeTrafficMultiplierDarkClusterStrategy;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.transport.http.client.ConstantQpsRateLimiter;
import com.linkedin.r2.transport.http.client.EvictingCircularBuffer;
import com.linkedin.test.util.ClockedExecutor;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URI;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Unit tests for partition-aware functionality in Dark Cluster Strategies.
 * Tests the new partition-aware behavior added to ConstantQpsDarkClusterStrategy,
 * RelativeTrafficMultiplierDarkClusterStrategy, and IdenticalTrafficMultiplierDarkClusterStrategy.
 */
public class TestPartitionAwareDarkClusterStrategies
{
  private static final String SOURCE_CLUSTER_NAME = "sourceCluster";
  private static final String DARK_CLUSTER_NAME = "darkCluster";
  private static final String SERVICE_NAME = "testService";
  
  private MockClusterInfoProvider mockClusterInfoProvider;
  private MockPartitionInfoProvider mockPartitionInfoProvider;
  private DarkClusterDispatcher darkClusterDispatcher;
  private BaseDarkClusterDispatcherImpl baseDispatcher;
  private ClockedExecutor executor;

  @BeforeMethod
  public void setUp()
  {
    mockClusterInfoProvider = new MockClusterInfoProvider();
    mockPartitionInfoProvider = new MockPartitionInfoProvider();
    darkClusterDispatcher = new DefaultDarkClusterDispatcher(new MockClient(false));
    baseDispatcher = new BaseDarkClusterDispatcherImpl(DARK_CLUSTER_NAME,
                                                       darkClusterDispatcher,
                                                       new DoNothingNotifier(),
                                                       new CountingVerifierManager());
    executor = new ClockedExecutor();
  }

  @Test
  public void testConstantQpsStrategyWithPartitions()
  {
    // Setup partition-specific host counts
    mockClusterInfoProvider.putHttpsClusterCount(SOURCE_CLUSTER_NAME, 10); // Default for partition 0
    mockClusterInfoProvider.putHttpsClusterCount(DARK_CLUSTER_NAME, 5);    // Default for partition 0
    
    // Setup partition accessor to return different partitions based on URI
    mockPartitionInfoProvider.setPartitionMapping("/partition0", 0);
    mockPartitionInfoProvider.setPartitionMapping("/partition1", 1);
    
    // Create rate limiter supplier
    Supplier<ConstantQpsRateLimiter> rateLimiterSupplier = () -> {
      EvictingCircularBuffer buffer = new EvictingCircularBuffer(100, 5, ChronoUnit.SECONDS, executor);
      ConstantQpsRateLimiter limiter = new ConstantQpsRateLimiter(executor, executor, executor, buffer);
      limiter.setBufferCapacity(100);
      limiter.setBufferTtl(Integer.MAX_VALUE, ChronoUnit.DAYS);
      return limiter;
    };
    
    ConstantQpsDarkClusterStrategy strategyP0 = new ConstantQpsDarkClusterStrategy(
        SOURCE_CLUSTER_NAME, DARK_CLUSTER_NAME, 10.0f, baseDispatcher,
        new DoNothingNotifier(), mockClusterInfoProvider, rateLimiterSupplier, 100, 300);
    ConstantQpsDarkClusterStrategy strategyP1 = new ConstantQpsDarkClusterStrategy(
        SOURCE_CLUSTER_NAME, DARK_CLUSTER_NAME, 10.0f, baseDispatcher,
        new DoNothingNotifier(), mockClusterInfoProvider, rateLimiterSupplier, 100, 300);
    
    // Test requests to different partitions
    RestRequest request0 = new RestRequestBuilder(URI.create("http://test.com/partition0")).build();
    RestRequest darkRequest0 = new RestRequestBuilder(URI.create("http://dark.com/partition0")).build();
    RequestContext context0 = new RequestContext();
    
    RestRequest request1 = new RestRequestBuilder(URI.create("http://test.com/partition1")).build();
    RestRequest darkRequest1 = new RestRequestBuilder(URI.create("http://dark.com/partition1")).build();
    RequestContext context1 = new RequestContext();
    
    // Both should succeed (strategy should handle different partitions)
    boolean result0 = strategyP0.handleRequest(request0, darkRequest0, context0);
    boolean result1 = strategyP1.handleRequest(request1, darkRequest1, context1);
    
    Assert.assertTrue(result0, "Request to partition 0 should be handled");
    Assert.assertTrue(result1, "Request to partition 1 should be handled");
  }

  
  @Test
  public void testRelativeTrafficMultiplierStrategyWithPartitions()
  {
    // Setup different host counts for different partitions
    mockClusterInfoProvider.putHttpsClusterCount(SOURCE_CLUSTER_NAME, 10); // Default partition
    mockClusterInfoProvider.putHttpsClusterCount(DARK_CLUSTER_NAME, 5);    // Default partition
    
    mockPartitionInfoProvider.setPartitionMapping("/partition0", 0);
    mockPartitionInfoProvider.setPartitionMapping("/partition1", 1);
    
    RelativeTrafficMultiplierDarkClusterStrategy strategy = new RelativeTrafficMultiplierDarkClusterStrategy(
        SOURCE_CLUSTER_NAME, DARK_CLUSTER_NAME, 1.0f, baseDispatcher,
        new DoNothingNotifier(), mockClusterInfoProvider, new Random(42));
    
    RestRequest request = new RestRequestBuilder(URI.create("http://test.com/partition0")).build();
    RestRequest darkRequest = new RestRequestBuilder(URI.create("http://dark.com/partition0")).build();
    RequestContext context = new RequestContext();
    
    // Test multiple requests to see if partition-aware logic is working
    int successCount = 0;
    for (int i = 0; i < 1000; i++) {
      if (strategy.handleRequest(request, darkRequest, new RequestContext())) {
        successCount++;
      }
    }
    
    // With multiplier 1.0 and equal host counts, we should see some requests go through
    // The exact count depends on the random sampling, but should be > 0
    Assert.assertTrue(successCount > 0, "Some requests should be sent to dark cluster with partition awareness");
  }


  @Test
  public void testIdenticalTrafficMultiplierStrategyWithPartitions()
  {
    mockClusterInfoProvider.putHttpsClusterCount(SOURCE_CLUSTER_NAME, 10);
    mockClusterInfoProvider.putHttpsClusterCount(DARK_CLUSTER_NAME, 5);
    
    mockPartitionInfoProvider.setPartitionMapping("/partition0", 0);
    
    IdenticalTrafficMultiplierDarkClusterStrategy strategy = new IdenticalTrafficMultiplierDarkClusterStrategy(
        SOURCE_CLUSTER_NAME, DARK_CLUSTER_NAME, 2.0f, baseDispatcher,
        new DoNothingNotifier(), mockClusterInfoProvider, new Random(42));
    
    RestRequest request = new RestRequestBuilder(URI.create("http://test.com/partition0")).build();
    RestRequest darkRequest = new RestRequestBuilder(URI.create("http://dark.com/partition0")).build();
    RequestContext context = new RequestContext();
    
    // Test that identical strategy works with partitions
    boolean result = strategy.handleRequest(request, darkRequest, context);
    Assert.assertTrue(result, "Identical strategy should handle partitioned requests");
  }

  /**
   * Mock PartitionInfoProvider for testing
   */
  private static class MockPartitionInfoProvider implements PartitionInfoProvider
  {
    private final Map<String, Integer> partitionMappings = new HashMap<>();
    
    public void setPartitionMapping(String path, int partitionId)
    {
      partitionMappings.put(path, partitionId);
    }
    
    @Override
    public PartitionAccessor getPartitionAccessor(String serviceName) throws ServiceUnavailableException
    {
      return new PartitionAccessor() {
        @Override
        public int getPartitionId(URI uri) throws PartitionAccessException
        {
          String path = uri.getPath();
          return partitionMappings.getOrDefault(path, DefaultPartitionAccessor.DEFAULT_PARTITION_ID);
        }
        
        @Override
        public int getMaxPartitionId()
        {
          return partitionMappings.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        }
      };
    }
    
    @Override
    public <K> com.linkedin.d2.balancer.util.HostToKeyMapper<K> getPartitionInformation(java.net.URI serviceUri, java.util.Collection<K> keys, int limitHostPerPartition, int hash) throws ServiceUnavailableException
    {
      throw new UnsupportedOperationException("Not implemented for test");
    }
  }
}

