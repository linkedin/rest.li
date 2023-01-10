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

package com.linkedin.d2.balancer.simple;

import com.linkedin.d2.balancer.LoadBalancerState;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import com.linkedin.d2.discovery.event.PropertyEventSubscriber;
import com.linkedin.r2.util.ClosableQueue;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.d2.discovery.util.LogUtil.trace;

public abstract class AbstractLoadBalancerSubscriber<T> implements
  PropertyEventSubscriber<T>
{
  private static final Logger _log = LoggerFactory.getLogger(AbstractLoadBalancerSubscriber.class);

  private final String                                                                  _name;
  private final int                                                                     _type;
  private final PropertyEventBus<T> _eventBus;
  private final ConcurrentMap<String, ClosableQueue<LoadBalancerState.LoadBalancerStateListenerCallback>> _waiters =
                                                                                                     new ConcurrentHashMap<>();

  public AbstractLoadBalancerSubscriber(int type, PropertyEventBus<T> eventBus)
  {
    _name = this.getClass().getSimpleName();
    _type = type;
    _eventBus = eventBus;
  }

  public boolean isListeningToProperty(String propertyName)
  {
    ClosableQueue<LoadBalancerState.LoadBalancerStateListenerCallback> waiters =
        _waiters.get(propertyName);
    return waiters != null && waiters.isClosed();
  }

  public int propertyListenCount()
  {
    return _waiters.size();
  }

  public void ensureListening(String propertyName,
                              LoadBalancerState.LoadBalancerStateListenerCallback callback)
  {
    ClosableQueue<LoadBalancerState.LoadBalancerStateListenerCallback> waiters =
        _waiters.get(propertyName);
    boolean register = false;
    if (waiters == null)
    {
      waiters = new ClosableQueue<>();
      ClosableQueue<LoadBalancerState.LoadBalancerStateListenerCallback> previous =
          _waiters.putIfAbsent(propertyName, waiters);
      if (previous == null)
      {
        // We are the very first to register
        register = true;
      }
      else
      {
        // Someone else beat us to it
        waiters = previous;
      }
    }
    // Ensure the callback is enqueued before registering with the bus
    if (!waiters.offer(callback))
    {
      callback.done(_type, propertyName);
    }
    if (register)
    {
      _eventBus.register(Collections.singleton(propertyName), this);
    }
  }

  /**
   * Tries to stop listening for property change.
   */
  public void tryStopListening(String propertyName, LoadBalancerState.LoadBalancerStateListenerCallback callback)
  {
    if (!isListeningToProperty(propertyName))
    {
      callback.done(_type, propertyName);
      return;
    }
    ClosableQueue<LoadBalancerState.LoadBalancerStateListenerCallback> waiterQueue =
        _waiters.get(propertyName);
    if (waiterQueue != null && !waiterQueue.isClosed())
    {
      // Watches is in the process of being established. Unregister now may cause unexpected race.
      callback.done(_type, propertyName);
      return;
    }

    // We need to remove waiters first. eventBus register/unregister is thread safe. Unregister only removes the first
    // occurrence of the subscriber in its listener queue. It is ok if a subscriber is registered again between waiter
    // removal and subscriber unregister. It will only remove the subscriber registered when waiter was initially added.
    waiterQueue = _waiters.remove(propertyName);
    if (waiterQueue != null)
    {
      _eventBus.unregister(Collections.singleton(propertyName), this);
    }
    callback.done(_type, propertyName);
  }

  @Override
  public void onAdd(final String propertyName, final T propertyValue)
  {
    trace(_log, _name, ".onAdd: ", propertyName, ": ", propertyValue);

    handlePut(propertyName, propertyValue);

    // if bad properties are received, then onInitialize()::handlePut might throw an exception and
    // the queue might not be closed. If the queue is not closed, then even if the underlying
    // problem with the properties is fixed and handlePut succeeds, new callbacks will be added
    // to the queue (in ensureListening) but never be triggered. We will attempt to close the
    // queue here if needed, and trigger any callbacks on that queue. If the queue is already
    // closed, it will return an empty list.
    List<LoadBalancerState.LoadBalancerStateListenerCallback> queueList = _waiters.get(propertyName).ensureClosed();
    if (queueList != null)
    {
      for (LoadBalancerState.LoadBalancerStateListenerCallback waiter : queueList)
      {
        waiter.done(_type, propertyName);
      }
    }
  }

  @Override
  public void onInitialize(final String propertyName, final T propertyValue)
  {
    trace(_log, _name, ".onInitialize: ", propertyName, ": ", propertyValue);

    handlePut(propertyName, propertyValue);

    for (LoadBalancerState.LoadBalancerStateListenerCallback waiter : _waiters.get(propertyName).close())
    {
      waiter.done(_type, propertyName);
    }
  }

  @Override
  public void onRemove(final String propertyName)
  {
    trace(_log, _name, ".onRemove: ", propertyName);

    handleRemove(propertyName);

    // if we are removing this property, ensure that its corresponding queue is closed and
    // remove it's entry from _waiters. We are invoking down on the callbacks to indicate we
    // heard back from zookeeper, and that the callers can proceed (even if they subsequently get
    // a ServiceUnavailableException)
    List<LoadBalancerState.LoadBalancerStateListenerCallback> queueList = _waiters.get(propertyName).ensureClosed();
    if (queueList != null)
    {
      for (LoadBalancerState.LoadBalancerStateListenerCallback waiter : queueList)
      {
        waiter.done(_type, propertyName);
      }
    }
  }

  protected abstract void handlePut(String propertyName, T propertyValue);

  protected abstract void handleRemove(String name);
}
