/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.d2.balancer.util;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.Directory;
import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.LoadBalancerWithFacilities;
import com.linkedin.d2.balancer.WarmUpService;
import com.linkedin.d2.balancer.clients.TestClient;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.util.hashing.HashRingProvider;
import com.linkedin.d2.balancer.util.partitions.PartitionInfoProvider;
import com.linkedin.d2.discovery.event.PropertyEventThread.PropertyEventShutdownCallback;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Dummy LoadBalancer counting the number of requests done
 */
public class TestLoadBalancer implements LoadBalancerWithFacilities, WarmUpService
{

  private final AtomicInteger _requestCount = new AtomicInteger();
  private final AtomicInteger _completedRequestCount = new AtomicInteger();
  private int _warmUpDelayMs = 0;
  private int _serviceDataDelayMs = 0;
  private boolean _shouldThrowOnGetClient = false;

  private final int DELAY_STANDARD_DEVIATION = 5; //ms
  private final ScheduledExecutorService _executorService = Executors.newSingleThreadScheduledExecutor();

  public TestLoadBalancer() {}

  public TestLoadBalancer(boolean shouldThrowOnGetClient) {
    _shouldThrowOnGetClient = shouldThrowOnGetClient;
  }

  public TestLoadBalancer(int warmUpDelayMs)
  {
    this(warmUpDelayMs, 0);
  }

  public TestLoadBalancer(int warmUpDelayMs, int serviceDataDelayMs)
  {
    _warmUpDelayMs = warmUpDelayMs;
    _serviceDataDelayMs = serviceDataDelayMs;
  }

  @Override
  public void getClient(Request request, RequestContext requestContext, Callback<TransportClient> clientCallback)
  {
    if (_shouldThrowOnGetClient)
    {
      clientCallback.onError(new TimeoutException());
    } else {
      clientCallback.onSuccess(new TestClient());
    }
  }

  @Override
  public void warmUpService(String serviceName, Callback<None> callback)
  {
    double g = Math.min(1.0, Math.max(-1.0, new Random().nextGaussian()));
    int actualDelay = Math.max(0,
        _warmUpDelayMs + ((int) g * DELAY_STANDARD_DEVIATION)); // +/- DELAY_STANDARD_DEVIATION ms
    _requestCount.incrementAndGet();
    _executorService.schedule(() ->
    {
      _completedRequestCount.incrementAndGet();
      callback.onSuccess(None.none());
    }, actualDelay, TimeUnit.MILLISECONDS);
  }

  @Override
  public void start(Callback<None> callback)
  {
    callback.onSuccess(None.none());
  }

  @Override
  public void shutdown(PropertyEventShutdownCallback shutdown)
  {
    shutdown.done();
  }

  @Override
  public void getLoadBalancedServiceProperties(String serviceName, Callback<ServiceProperties> clientCallback)
  {
    if (_serviceDataDelayMs > 0)
    {
      try {
        Thread.sleep(_serviceDataDelayMs);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    clientCallback.onSuccess(new ServiceProperties(serviceName, "clustername", "/foo", Arrays.asList("rr")));
  }

  AtomicInteger getRequestCount()
  {
    return _requestCount;
  }

  AtomicInteger getCompletedRequestCount()
  {
    return _completedRequestCount;
  }

  @Override
  public Directory getDirectory()
  {
    return null;
  }

  @Override
  public PartitionInfoProvider getPartitionInfoProvider()
  {
    return null;
  }

  @Override
  public HashRingProvider getHashRingProvider()
  {
    return null;
  }

  @Override
  public KeyMapper getKeyMapper()
  {
    return null;
  }

  @Override
  public TransportClientFactory getClientFactory(String scheme)
  {
    return null;
  }

  @Override
  public ClusterInfoProvider getClusterInfoProvider()
  {
    return null;
  }
}
