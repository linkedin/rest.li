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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Specific tests for ConstantQpsDarkClusterStrategy partition-aware rate limiting functionality.
 * Tests that different partitions get separate rate limiters and that rate limiting works correctly per partition.
 */
public class TestConstantQpsPartitionAware
{
  private static final String SOURCE_CLUSTER_NAME = "sourceCluster";
  private static final String DARK_CLUSTER_NAME = "darkCluster";
  
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
  public void testSeparateRateLimitersPerPartition()
  {
    // Setup different host counts per partition
    mockClusterInfoProvider.putHttpsClusterCount(SOURCE_CLUSTER_NAME, 10);
    mockClusterInfoProvider.putHttpsClusterCount(DARK_CLUSTER_NAME, 5);
    
    mockPartitionInfoProvider.setPartitionMapping("/partition0", 0);
    mockPartitionInfoProvider.setPartitionMapping("/partition1", 1);
    
    // Track how many rate limiters are created
    AtomicInteger rateLimiterCount = new AtomicInteger(0);
    Supplier<ConstantQpsRateLimiter> rateLimiterSupplier = () -> {
      rateLimiterCount.incrementAndGet();
      EvictingCircularBuffer buffer = new EvictingCircularBuffer(100, Integer.MAX_VALUE, ChronoUnit.DAYS, executor);
      ConstantQpsRateLimiter limiter = new ConstantQpsRateLimiter(executor, executor, executor, buffer);
      limiter.setBufferCapacity(100);
      limiter.setBufferTtl(Integer.MAX_VALUE, ChronoUnit.DAYS);
      return limiter;
    };
    
    ConstantQpsDarkClusterStrategy strategy = new ConstantQpsDarkClusterStrategy(
        SOURCE_CLUSTER_NAME, DARK_CLUSTER_NAME, 10.0f, baseDispatcher,
        new DoNothingNotifier(), mockClusterInfoProvider, mockPartitionInfoProvider, rateLimiterSupplier, 100, Integer.MAX_VALUE);
    
    // Make requests to different partitions
    RestRequest request0 = new RestRequestBuilder(URI.create("http://test.com/partition0")).build();
    RestRequest darkRequest0 = new RestRequestBuilder(URI.create("http://dark.com/partition0")).build();
    RequestContext context0 = new RequestContext();
    
    RestRequest request1 = new RestRequestBuilder(URI.create("http://test.com/partition1")).build();
    RestRequest darkRequest1 = new RestRequestBuilder(URI.create("http://dark.com/partition1")).build();
    RequestContext context1 = new RequestContext();
    
    // First request to partition 0 should create a rate limiter
    strategy.handleRequest(request0, darkRequest0, context0);
    Assert.assertEquals(rateLimiterCount.get(), 1, "First partition should create one rate limiter");
    
    // Second request to same partition should reuse the rate limiter
    strategy.handleRequest(request0, darkRequest0, new RequestContext());
    Assert.assertEquals(rateLimiterCount.get(), 1, "Same partition should reuse rate limiter");
    
    // Request to different partition should create a new rate limiter
    strategy.handleRequest(request1, darkRequest1, context1);
    Assert.assertEquals(rateLimiterCount.get(), 2, "Different partition should create new rate limiter");
    
    // Another request to partition 1 should reuse its rate limiter
    strategy.handleRequest(request1, darkRequest1, new RequestContext());
    Assert.assertEquals(rateLimiterCount.get(), 2, "Same partition should reuse its rate limiter");
  }

  @Test
  public void testLegacyConstructorUsesDefaultPartition()
  {
    mockClusterInfoProvider.putHttpsClusterCount(SOURCE_CLUSTER_NAME, 10);
    mockClusterInfoProvider.putHttpsClusterCount(DARK_CLUSTER_NAME, 5);
    
    // Create a single rate limiter for legacy constructor
    EvictingCircularBuffer buffer = new EvictingCircularBuffer(100, Integer.MAX_VALUE, ChronoUnit.DAYS, executor);
    ConstantQpsRateLimiter rateLimiter = new ConstantQpsRateLimiter(executor, executor, executor, buffer);
    rateLimiter.setBufferCapacity(100);
    rateLimiter.setBufferTtl(Integer.MAX_VALUE, ChronoUnit.DAYS);
    
    // Use legacy constructor (no partition info provider, single rate limiter)
    ConstantQpsDarkClusterStrategy strategy = new ConstantQpsDarkClusterStrategy(
        SOURCE_CLUSTER_NAME, DARK_CLUSTER_NAME, 10.0f, baseDispatcher,
        new DoNothingNotifier(), mockClusterInfoProvider, rateLimiter);
    
    RestRequest request = new RestRequestBuilder(URI.create("http://test.com/any")).build();
    RestRequest darkRequest = new RestRequestBuilder(URI.create("http://dark.com/any")).build();
    RequestContext context = new RequestContext();
    
    // Should work with legacy constructor (uses default partition)
    boolean result = strategy.handleRequest(request, darkRequest, context);
    Assert.assertTrue(result, "Legacy constructor should work with default partition");
  }

  @Test
  public void testPartitionIdCalculationWithDifferentHostCounts()
  {
    // Setup different host counts that would affect send rate calculation
    mockClusterInfoProvider = new MockClusterInfoProvider() {
      @Override
      public int getClusterCount(String clusterName, String scheme, int partitionId) throws ServiceUnavailableException {
        if (clusterName.equals(SOURCE_CLUSTER_NAME)) {
          return partitionId == 0 ? 10 : 20; // Different source counts per partition
        } else if (clusterName.equals(DARK_CLUSTER_NAME)) {
          return partitionId == 0 ? 5 : 10;  // Different dark counts per partition
        }
        return 1;
      }
    };
    
    mockPartitionInfoProvider.setPartitionMapping("/partition0", 0);
    mockPartitionInfoProvider.setPartitionMapping("/partition1", 1);
    
    Supplier<ConstantQpsRateLimiter> rateLimiterSupplier = () -> {
      EvictingCircularBuffer buffer = new EvictingCircularBuffer(100, Integer.MAX_VALUE, ChronoUnit.DAYS, executor);
      ConstantQpsRateLimiter limiter = new ConstantQpsRateLimiter(executor, executor, executor, buffer);
      limiter.setBufferCapacity(100);
      limiter.setBufferTtl(Integer.MAX_VALUE, ChronoUnit.DAYS);
      return limiter;
    };
    
    ConstantQpsDarkClusterStrategy strategy = new ConstantQpsDarkClusterStrategy(
        SOURCE_CLUSTER_NAME, DARK_CLUSTER_NAME, 10.0f, baseDispatcher,
        new DoNothingNotifier(), mockClusterInfoProvider, mockPartitionInfoProvider, rateLimiterSupplier, 100, Integer.MAX_VALUE);
    
    // Test requests to different partitions - should use different host counts for rate calculation
    RestRequest request0 = new RestRequestBuilder(URI.create("http://test.com/partition0")).build();
    RestRequest darkRequest0 = new RestRequestBuilder(URI.create("http://dark.com/partition0")).build();
    RequestContext context0 = new RequestContext();
    
    RestRequest request1 = new RestRequestBuilder(URI.create("http://test.com/partition1")).build();
    RestRequest darkRequest1 = new RestRequestBuilder(URI.create("http://dark.com/partition1")).build();
    RequestContext context1 = new RequestContext();
    
    // Both should succeed but with different rate calculations based on partition host counts
    boolean result0 = strategy.handleRequest(request0, darkRequest0, context0);
    boolean result1 = strategy.handleRequest(request1, darkRequest1, context1);
    
    Assert.assertTrue(result0, "Request to partition 0 should succeed");
    Assert.assertTrue(result1, "Request to partition 1 should succeed");
  }

  @Test
  public void testPartitionAccessorFailureFallsBackToDefault()
  {
    mockClusterInfoProvider.putHttpsClusterCount(SOURCE_CLUSTER_NAME, 10);
    mockClusterInfoProvider.putHttpsClusterCount(DARK_CLUSTER_NAME, 5);
    
    // Create partition info provider that throws exception for getPartitionAccessor
    PartitionInfoProvider faultyProvider = new PartitionInfoProvider() {
      @Override
      public PartitionAccessor getPartitionAccessor(String serviceName) throws ServiceUnavailableException {
        throw new ServiceUnavailableException(serviceName, "Test failure");
      }
      
      @Override
      public <K> com.linkedin.d2.balancer.util.HostToKeyMapper<K> getPartitionInformation(java.net.URI serviceUri, java.util.Collection<K> keys, int limitHostPerPartition, int hash) throws ServiceUnavailableException {
        throw new UnsupportedOperationException("Not implemented for test");
      }
    };
    
    AtomicInteger rateLimiterCount = new AtomicInteger(0);
    Supplier<ConstantQpsRateLimiter> rateLimiterSupplier = () -> {
      rateLimiterCount.incrementAndGet();
      EvictingCircularBuffer buffer = new EvictingCircularBuffer(100, Integer.MAX_VALUE, ChronoUnit.DAYS, executor);
      ConstantQpsRateLimiter limiter = new ConstantQpsRateLimiter(executor, executor, executor, buffer);
      limiter.setBufferCapacity(100);
      limiter.setBufferTtl(Integer.MAX_VALUE, ChronoUnit.DAYS);
      return limiter;
    };
    
    ConstantQpsDarkClusterStrategy strategy = new ConstantQpsDarkClusterStrategy(
        SOURCE_CLUSTER_NAME, DARK_CLUSTER_NAME, 10.0f, baseDispatcher,
        new DoNothingNotifier(), mockClusterInfoProvider, faultyProvider, rateLimiterSupplier, 100, Integer.MAX_VALUE);

    RestRequest request = new RestRequestBuilder(URI.create("http://test.com/any")).build();
    RestRequest darkRequest = new RestRequestBuilder(URI.create("http://dark.com/any")).build();
    RequestContext context = new RequestContext();
    
    // Should fall back to default partition and create one rate limiter
    boolean result = strategy.handleRequest(request, darkRequest, context);
    Assert.assertTrue(result, "Should handle request despite partition accessor failure");
    Assert.assertEquals(rateLimiterCount.get(), 1, "Should create rate limiter for default partition");
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

