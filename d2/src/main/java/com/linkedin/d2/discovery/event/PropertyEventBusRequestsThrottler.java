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

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.util.clock.SystemClock;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The class allows to make a series of requests to a bus with a concurrent call limiter
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class PropertyEventBusRequestsThrottler<T>
{

  private static final Logger LOG = LoggerFactory.getLogger(PropertyEventBusRequestsThrottler.class);

  /**
   * Default max of concurrent outstanding requests
   */
  public static final int DEFAULT_MAX_CONCURRENT_REQUESTS = 5;

  private final EventBusUpdaterSubscriber _eventBusUpdaterSubscriberSubscriber;
  private final PropertyEventBus<T> _eventBus;
  private final PropertyEventSubscriber<T> _externalSubscriber;
  private final List<String> _keysToFetch;
  private final boolean _logStatusEnabled;

  private final Map<String, Long> logTime = new ConcurrentHashMap<>();

  private final AtomicInteger _requestCompletedCount;
  private final AtomicInteger _requestStartedCount;
  private Callback<None> _callback;

  private final int _maxConcurrentRequests;

  public PropertyEventBusRequestsThrottler(PropertyEventBus<T> eventBus, PropertyEventSubscriber<T> externalSubscriber,
                                           List<String> keysToFetch, int maxConcurrentRequests, boolean logStatusEnabled)
  {
    _eventBus = eventBus;
    _externalSubscriber = externalSubscriber;
    _keysToFetch = keysToFetch;
    _logStatusEnabled = logStatusEnabled;

    _eventBusUpdaterSubscriberSubscriber = new EventBusUpdaterSubscriber();

    _maxConcurrentRequests = maxConcurrentRequests;

    _requestStartedCount = new AtomicInteger(0);
    _requestCompletedCount = new AtomicInteger(0);
  }

  /**
   * Once initialized, start sending the requests. The callback will be called once all the requests returned
   * a result (which is published on the {@link #_externalSubscriber}).
   *
   * If the bus never returns all the values, the callback will be never called
   */
  public void sendRequests(Callback<None> callback)
  {
    LOG.info("Event Bus Requests throttler started for {} keys at a {} load rate",
      _keysToFetch.size(), _maxConcurrentRequests);
    if (_keysToFetch.size() == 0)
    {
      callback.onSuccess(None.none());
      return;
    }
    _callback = callback;
    makeRequests(_maxConcurrentRequests);
  }

  private void makeRequests(int n)
  {
    int initial = _requestStartedCount.getAndAdd(n);
    if (_keysToFetch.size() < initial)
    {
      return;
    }
    if (_keysToFetch.size() < initial + n)
    {
      n = _keysToFetch.size() - initial;
    }
    HashSet<String> keys = new HashSet<>(_keysToFetch.subList(initial, initial + n));
    if (_logStatusEnabled || LOG.isDebugEnabled())
    {
      LOG.info("EventBus throttler fetching keys: {}", String.join(", ", keys));
    }
    for (String key : keys)
    {
      logTime.put(key, SystemClock.instance().currentTimeMillis());
    }

    // register the external subscriber to let the user receive all the values from the eventBus
    _eventBus.register(keys, _externalSubscriber);

    // register the internal subscriber, so we can fire the next requests
    _eventBus.register(keys, _eventBusUpdaterSubscriberSubscriber);
  }

  /**
   * Helper class that fires another call to the bus once a previous call completes
   */
  class EventBusUpdaterSubscriber implements PropertyEventSubscriber<T>
  {
    void next(String prop)
    {
      int index = _requestCompletedCount.incrementAndGet();

      Long startTime = logTime.get(prop);
      if (_logStatusEnabled || LOG.isDebugEnabled())
      {
        LOG.info("{}/{} Key {} fetched in {}ms",
          new Object[]{index, _keysToFetch.size(), prop, SystemClock.instance().currentTimeMillis() - startTime});
      }
      if (_keysToFetch.size() == index) {
        _callback.onSuccess(None.none());
        return;
      }
      makeRequests(1);
    }

    @Override
    public void onInitialize(String propertyName, T propertyValue)
    {
      next(propertyName);
    }

    @Override
    public void onAdd(String propertyName, T propertyValue)
    {
      next(propertyName);
    }

    @Override
    public void onRemove(String propertyName)
    {
      next(propertyName);
    }
  }
}