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

package com.linkedin.d2.discovery.event;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.Assert;
import org.testng.annotations.Test;


/**
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class PropertyEventBusRequestsThrottlerTest
{

  @Test(timeOut = 10000)
  public void testAllowZeroRequests() throws InterruptedException, ExecutionException, TimeoutException
  {
    TestSubscriber testSubscriber = new TestSubscriber();
    TestEventBus testEventBus = new TestEventBus(testSubscriber);
    PropertyEventBusRequestsThrottler<String> propertyEventBusRequestsThrottler =
      new PropertyEventBusRequestsThrottler<>(testEventBus, testSubscriber, new ArrayList<>(), 5, false);

    FutureCallback<None> callback = new FutureCallback<>();
    propertyEventBusRequestsThrottler.sendRequests(callback);
    callback.get(1000, TimeUnit.MILLISECONDS);
  }

  @Test(timeOut = 10000)
  public void testThrottling() throws InterruptedException, ExecutionException, TimeoutException
  {
    TestSubscriber testSubscriber = new TestSubscriber();
    TestEventBus testZkEventBus = new TestEventBus(testSubscriber, 50);

    final int nRequests = 100;

    PropertyEventBusRequestsThrottler<String> propertyEventBusRequestsThrottler =
      new PropertyEventBusRequestsThrottler<>(testZkEventBus, testSubscriber, generateNKeys(nRequests),
        PropertyEventBusRequestsThrottler.DEFAULT_MAX_CONCURRENT_REQUESTS, false);

    FutureCallback<None> callback = new FutureCallback<>();
    propertyEventBusRequestsThrottler.sendRequests(callback);

    boolean triggeredAtLeastOnce = false;
    while (!callback.isDone())
    {
      int currentConcurrentRequests =
        testZkEventBus.getRequestCount().get() - testSubscriber.getCompletedRequestCount().get();
      if (currentConcurrentRequests > 0)
      {
        triggeredAtLeastOnce = true;
      }
      if (currentConcurrentRequests > PropertyEventBusRequestsThrottler.DEFAULT_MAX_CONCURRENT_REQUESTS)
      {
        Assert.fail("The concurrent requests (" + currentConcurrentRequests + ") are greater than the allowed ("
          + PropertyEventBusRequestsThrottler.DEFAULT_MAX_CONCURRENT_REQUESTS + ")");
      }
      Thread.sleep(50);
    }

    callback.get(1000, TimeUnit.MILLISECONDS);

    Assert.assertTrue(triggeredAtLeastOnce);
    Assert.assertEquals(nRequests, testZkEventBus.getRequestCount().get());
    Assert.assertEquals(nRequests, testSubscriber.getCompletedRequestCount().get());
  }

  /**
   * Tests that if the requests are not throttled it makes a large amount of concurrent calls
   */
  @Test(timeOut = 10000)
  public void testThrottlingUnlimitedRequests() throws InterruptedException, ExecutionException, TimeoutException
  {
    TestSubscriber testSubscriber = new TestSubscriber();
    TestEventBus testZkEventBus = new TestEventBus(testSubscriber, 50);

    final int nRequests = 100;

    int concurrentRequestsHugeNumber = 999999999;
    int concurrentRequestsCheckHigher = PropertyEventBusRequestsThrottler.DEFAULT_MAX_CONCURRENT_REQUESTS;

    PropertyEventBusRequestsThrottler<String> propertyEventBusRequestsThrottler =
      new PropertyEventBusRequestsThrottler<>(testZkEventBus, testSubscriber, generateNKeys(nRequests),
        concurrentRequestsHugeNumber, false);

    FutureCallback<None> callback = new FutureCallback<>();
    propertyEventBusRequestsThrottler.sendRequests(callback);

    boolean triggeredAtLeastOnce = false;
    while (!callback.isDone() && !triggeredAtLeastOnce)
    {
      int currentConcurrentRequests =
        testZkEventBus.getRequestCount().get() - testSubscriber.getCompletedRequestCount().get();
      if (currentConcurrentRequests > concurrentRequestsCheckHigher)
      {
        triggeredAtLeastOnce = true;
      }
      Thread.sleep(50);
    }

    callback.get(1000, TimeUnit.MILLISECONDS);

    Assert.assertTrue(triggeredAtLeastOnce);
    Assert.assertEquals(nRequests, testZkEventBus.getRequestCount().get());
    Assert.assertEquals(nRequests, testSubscriber.getCompletedRequestCount().get());
  }

  // #################### Utils ####################

  List<String> generateNKeys(int n)
  {
    List<String> keys = new ArrayList<>();
    IntStream.range(0, n).forEach(i -> keys.add("key" + i));
    return keys;
  }

  private class TestSubscriber implements PropertyEventSubscriber<String>
  {
    final AtomicInteger _completedRequestCount = new AtomicInteger();

    @Override
    public void onInitialize(String propertyName, String propertyValue)
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void onAdd(String propertyName, String propertyValue)
    {
      _completedRequestCount.incrementAndGet();
    }

    @Override
    public void onRemove(String propertyName)
    {
      throw new UnsupportedOperationException();
    }

    public AtomicInteger getCompletedRequestCount()
    {
      return _completedRequestCount;
    }
  }

  private class TestEventBus implements PropertyEventBus<String>
  {

    private final TestSubscriber _subscriberToIncrementCount;

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    final AtomicInteger _requestCount = new AtomicInteger();
    private int _delayMs = 0;
    private final int DELAY_STANDARD_DEVIATION = 10; //ms

    public TestEventBus(TestSubscriber subscriberToIncrementCount)
    {
      _subscriberToIncrementCount = subscriberToIncrementCount;
    }

    public TestEventBus(TestSubscriber subscriberToIncrementCount, int delayMs)
    {
      _subscriberToIncrementCount = subscriberToIncrementCount;
      _delayMs = delayMs;
    }

    @Override
    public void register(Set<String> propertyNames, PropertyEventSubscriber<String> subscriber)
    {
      for (String propertyName : propertyNames)
      {
        // the _subscriberToIncrementCount is needed because the throttler will try to register
        // the same properties also on a internal subscriber, which would cause a double count
        if (_subscriberToIncrementCount.equals(subscriber))
        {
          _requestCount.incrementAndGet();
        }
        executorService.schedule(() -> {
          subscriber.onAdd(propertyName, "randomValue");
        }, getConsistentDelayForProp(propertyName), TimeUnit.MILLISECONDS);
      }
    }

    /**
     * Since for the same prop, register is called twice (one for the throttler,
     * one for the external bus), and they should be called at the same time,
     * we need consistent delay for a specific prop
     */
    Map<String, Integer> _consistentDelayForPropMap = new ConcurrentHashMap<>();

    private int getConsistentDelayForProp(String prop)
    {
      return _consistentDelayForPropMap.computeIfAbsent(prop, s -> Math.max(0, _delayMs
        // any kind of random delay works for the test
        + ((int) new Random().nextGaussian() * DELAY_STANDARD_DEVIATION)));
    }

    public AtomicInteger getRequestCount()
    {
      return _requestCount;
    }

    // #################### Unsupported operations section ####################

    @Override
    public void register(PropertyEventSubscriber<String> subscriber)
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void unregister(PropertyEventSubscriber<String> subscriber)
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void unregister(Set<String> propertyNames, PropertyEventSubscriber<String> subscriber)
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setPublisher(PropertyEventPublisher<String> publisher)
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void publishInitialize(String prop, String value)
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void publishAdd(String prop, String value)
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void publishRemove(String prop)
    {
      throw new UnsupportedOperationException();
    }
  }
}
