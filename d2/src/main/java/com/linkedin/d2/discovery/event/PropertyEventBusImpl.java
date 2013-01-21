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

import com.linkedin.d2.discovery.event.PropertyEventThread.PropertyEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;


/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class PropertyEventBusImpl<T> implements PropertyEventBus<T>
{
  private final PropertyEventThread _thread;
  private PropertyEventPublisher<T> _publisher;
  private final Map<String,T> _properties = new HashMap<String,T>();
  private final Map<String,List<PropertyEventSubscriber<T>>> _subscribers = new HashMap<String,List<PropertyEventSubscriber<T>>>();
  private final List<PropertyEventSubscriber<T>> _allPropertySubscribers = new ArrayList<PropertyEventSubscriber<T>>();

  /*
   * Concurrency considerations:
   *
   * All data structures are unsynchronized. They are manipulated only by tasks submitted
   * to the executor, which is assumed to be single-threaded.
   */
  @Deprecated
  public PropertyEventBusImpl(PropertyEventThread thread)
  {
      _thread = thread;
  }

  @Deprecated
  public PropertyEventBusImpl(PropertyEventThread thread, PropertyEventPublisher<T> publisher)
  {
      this(thread);
      _publisher = publisher;
      _publisher.setBus(this);
  }

  public PropertyEventBusImpl(ExecutorService executorService)
  {
    _thread = new PropertyEventExecutor("PropertyEventBusImpl PropertyEventThread", executorService);
  }

  public PropertyEventBusImpl(ExecutorService executorService, PropertyEventPublisher<T> publisher)
  {
    this(executorService);
    _publisher = publisher;
    _publisher.setBus(this);
  }

  @Override
  public void register(final PropertyEventSubscriber<T> listener)
  {
    _thread.send(new PropertyEvent("PropertyEventBus.registerAll")
    {
      @Override
      public void innerRun()
      {
        _allPropertySubscribers.add(listener);
      }
    });
  }

  @Override
  public void unregister(final PropertyEventSubscriber<T> listener)
  {
    _thread.send(new PropertyEvent("PropertyEventBus.unregisterAll")
    {
      @Override
      public void innerRun()
      {
        _allPropertySubscribers.remove(listener);
      }
    });
  }

  @Override
  public void register(final Set<String> propertyNames,
                       final PropertyEventSubscriber<T> subscriber)
  {
    _thread.send(new PropertyEvent("PropertyEventBus.register " + propertyNames)
    {
      public void innerRun()
      {
        for (final String prop : propertyNames)
        {
          boolean initialized;
          boolean notifyPublisher = false;
          initialized = _properties.containsKey(prop);
          List<PropertyEventSubscriber<T>> listeners = _subscribers.get(prop);
          if (listeners == null)
          {
            listeners = new ArrayList<PropertyEventSubscriber<T>>();
            _subscribers.put(prop, listeners);
          }
          if (listeners.isEmpty())
          {
            notifyPublisher = true;
          }
          listeners.add(subscriber);
          if (initialized)
          {
            subscriber.onInitialize(prop, _properties.get(prop));
          }
          if (notifyPublisher)
          {
            _publisher.startPublishing(prop);
          }
        }
      }
    });
  }

  @Override
  public void unregister(final Set<String> propertyNames,
                         final PropertyEventSubscriber<T> subscriber)
  {
    _thread.send(new PropertyEvent("PropertyEventBus.unregister " + propertyNames)
    {
      public void innerRun()
      {
        for (final String prop : propertyNames)
        {
          List<PropertyEventSubscriber<T>> subscribers = _subscribers.get(prop);
          if (subscribers != null)
          {
            subscribers.remove(subscriber);
            if (subscribers.isEmpty())
            {
              _properties.remove(prop);
              _publisher.stopPublishing(prop);
            }
          }
        }
      }
    });
  }

  @Override
  public void setPublisher(final PropertyEventPublisher<T> publisher)
  {

    _thread.send(new PropertyEvent("PropertyEventBus.setPublisher")
    {
      public void innerRun()
      {
        if (_publisher != null)
        {
          for (String propertyName : _subscribers.keySet())
          {
            _publisher.stopPublishing(propertyName);
          }
        }
        _publisher = publisher;
        _publisher.setBus(PropertyEventBusImpl.this);
        for (String propertyName : _subscribers.keySet())
        {
          _publisher.startPublishing(propertyName);
        }
      }
    });
  }

  @Override
  public void publishInitialize(final String prop, final T value)
  {
    _thread.send(new PropertyEvent("PropertyEventBus.publishInitialize " + prop)
    {
      public void innerRun()
      {
        // Because the bus can switch publishers, a new publisher may consider an event
        // an "initialize", but if the bus has previously seen that property, we will treat
        // it as an "add" so that the publisher change will be transparent to the clients.
        boolean doAdd = _properties.containsKey(prop);
        _properties.put(prop, value);
        List<PropertyEventSubscriber<T>> waiters = subscribers(prop);
        for (final PropertyEventSubscriber<T> waiter : waiters)
        {
          if (doAdd)
          {
            waiter.onAdd(prop, value);
          }
          else
          {
            waiter.onInitialize(prop, value);
          }
        }
      }
    });
  }

  @Override
  public void publishAdd(final String prop, final T value)
  {
    _thread.send(new PropertyEvent("PropertyEventBus.publishAdd " + prop)
    {
      public void innerRun()
      {
        // Ignore unless the property has been initialized
        if (_properties.containsKey(prop))
        {
          _properties.put(prop, value);
          for (final PropertyEventSubscriber<T> subscriber : subscribers(prop))
          {
            subscriber.onAdd(prop, value);
          }
        }
      }
    });
  }

  @Override
  public void publishRemove(final String prop)
  {
    _thread.send(new PropertyEvent("PropertyEventBus.publishRemove " + prop)
    {
      public void innerRun()
      {
        // Ignore unless the property has been initialized
        if (_properties.containsKey(prop))
        {
          _properties.put(prop, null);
          for (final PropertyEventSubscriber<T> subscriber : subscribers(prop))
          {
            subscriber.onRemove(prop);
          }
        }
      }
    });
  }

  private List<PropertyEventSubscriber<T>> subscribers(String prop)
  {
    List<PropertyEventSubscriber<T>> subscribers = _subscribers.get(prop);
    if (subscribers == null)
    {
      return _allPropertySubscribers;
    }
    if (_allPropertySubscribers.isEmpty())
    {
      return subscribers;
    }
    List<PropertyEventSubscriber<T>> all =
        new ArrayList<PropertyEventSubscriber<T>>(subscribers.size()
            + _allPropertySubscribers.size());
    all.addAll(_allPropertySubscribers);
    all.addAll(subscribers);
    return all;
  }

  /**
   * This is really just for testing
   */
  public PropertyEventPublisher<T> getPublisher()
  {
    return _publisher;
  }

  private class PropertyEventExecutor extends PropertyEventThread
  {
    private final ExecutorService _executor;

    public PropertyEventExecutor(String name, ExecutorService executor)
    {
      super(name);
      _executor = executor;
    }

    @Override
    public boolean send(PropertyEvent message)
    {
      _executor.execute(message);
      return true;
    }
  }

}
