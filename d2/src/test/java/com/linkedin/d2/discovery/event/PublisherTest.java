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

/**
 * $Id: $
 */

package com.linkedin.d2.discovery.event;

import com.linkedin.d2.discovery.stores.PropertyStore;
import com.linkedin.d2.discovery.stores.PropertyStoreException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public abstract class PublisherTest
{

  protected static final int BUS_UPDATE_TIMEOUT = 30;

  /**
   *
   * @return publisher being tested
   */
  protected abstract PropertyEventPublisher<String> getPublisher();

  /**
   *
   * @return a property store which can write into the same property space as the publisher
   * is watching.  This should be as reasonably disconnected as possible from the publisher,
   * i.e., it should not be the same store that backs the publisher.
   */
  protected abstract PropertyStore<String> getStore();

  @Test
  public void testNewProperty() throws PropertyStoreException, TimeoutException, InterruptedException
  {
    final String KEY = "someKey";
    final String VALUE = "someValue";
    PropertyEventPublisher<String> pub = getPublisher();

    MockBusSink bus = new MockBusSink();

    pub.setBus(bus);

    // Publisher should publish an initial null
    pub.startPublishing(KEY);
    bus.awaitInit(KEY, null, BUS_UPDATE_TIMEOUT, TimeUnit.SECONDS);

    // After updating, publisher should publish the new value
    PropertyStore<String> store = getStore();
    store.put(KEY, VALUE);
    bus.awaitAdd(KEY, VALUE, BUS_UPDATE_TIMEOUT, TimeUnit.SECONDS);
  }

  @Test
  public void testExistingProperty() throws PropertyStoreException, TimeoutException, InterruptedException
  {
    final String KEY = "someKey";
    final String VALUE = "someValue";

    PropertyStore<String> store = getStore();
    store.put(KEY, VALUE);
    Assert.assertEquals(store.get(KEY), VALUE);

    MockBusSink bus = new MockBusSink();

    PropertyEventPublisher<String> pub = getPublisher();
    pub.setBus(bus);

    pub.startPublishing(KEY);
    bus.awaitInit(KEY, VALUE, BUS_UPDATE_TIMEOUT, TimeUnit.SECONDS);

    store.remove(KEY);
    bus.awaitRemove(KEY, BUS_UPDATE_TIMEOUT, TimeUnit.SECONDS);
  }

  protected static class MockBusSink implements PropertyEventBus<String>
  {
    private final Lock _lock = new ReentrantLock();
    private final Condition _initCondition = _lock.newCondition();
    private final Condition _addCondition = _lock.newCondition();
    private final Condition _removeCondition = _lock.newCondition();

    private Map<String,String> _currentValues = new HashMap<String, String>();

    public void awaitInit(String key, String value, long timeout, TimeUnit timeoutUnit)
            throws InterruptedException, TimeoutException
    {
      Date deadline = new Date(System.currentTimeMillis() + timeoutUnit.toMillis(timeout));
      _lock.lock();
      try
      {
        while (!compare(_currentValues.get(key), value))
        {
          if (!_initCondition.awaitUntil(deadline))
          {
            throw new TimeoutException();
          }
        }
      }
      finally
      {
        _lock.unlock();
      }
    }

    public void awaitAdd(String key, String value, long timeout, TimeUnit timeoutUnit)
            throws InterruptedException, TimeoutException
    {
      Date deadline = new Date(System.currentTimeMillis() + timeoutUnit.toMillis(timeout));
      _lock.lock();
      try
      {
        while (!compare(_currentValues.get(key), value))
        {
          if (!_addCondition.awaitUntil(deadline))
          {
            throw new TimeoutException();
          }
        }
      }
      finally
      {
        _lock.unlock();
      }
    }

    public void awaitRemove(String key, long timeout, TimeUnit timeoutUnit)
            throws InterruptedException, TimeoutException
    {
      Date deadline = new Date(System.currentTimeMillis() + timeoutUnit.toMillis(timeout));
      _lock.lock();
      try
      {
        while (!compare(_currentValues.get(key), null))
        {
          if (!_removeCondition.awaitUntil(deadline))
          {
            throw new TimeoutException();
          }
        }
      }
      finally
      {
        _lock.unlock();
      }
    }

    private static boolean compare(Object a, Object b)
    {
      if (a == null)
      {
        return b == null;
      }
      return a.equals(b);
    }

    @Override
    public void publishInitialize(String prop, String value)
    {
      _lock.lock();
      try
      {
        _currentValues.put(prop, value);
        _initCondition.signalAll();
      }
      finally
      {
        _lock.unlock();
      }
    }

    @Override
    public void publishAdd(String prop, String value)
    {
      _lock.lock();
      try
      {
        _currentValues.put(prop, value);
        _addCondition.signalAll();
      }
      finally
      {
        _lock.unlock();
      }
    }

    @Override
    public void publishRemove(String prop)
    {
      _lock.lock();
      try
      {
        _currentValues.remove(prop);
        _removeCondition.signalAll();
      }
      finally
      {
        _lock.unlock();
      }
    }

    @Override
    public void register(PropertyEventSubscriber<String> stringPropertyEventSubscriber)
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void unregister(PropertyEventSubscriber<String> stringPropertyEventSubscriber)
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void register(Set<String> propertyNames,
                         PropertyEventSubscriber<String> stringPropertyEventSubscriber)
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void unregister(Set<String> propertyNames,
                           PropertyEventSubscriber<String> stringPropertyEventSubscriber)
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setPublisher(PropertyEventPublisher<String> stringPropertyEventPublisher)
    {
      throw new UnsupportedOperationException();
    }
  }
}
