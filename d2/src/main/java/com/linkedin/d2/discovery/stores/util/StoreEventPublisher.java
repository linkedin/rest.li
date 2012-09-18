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

package com.linkedin.d2.discovery.stores.util;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.d2.discovery.event.PropertyEventBus;
import com.linkedin.d2.discovery.event.PropertyEventPublisher;
import com.linkedin.d2.discovery.event.PropertyEventThread;
import com.linkedin.d2.discovery.stores.PropertyStore;
import com.linkedin.d2.discovery.stores.PropertyStoreException;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

/**
 * Responds to startPublish requests by serving up the current value from an underlying store.
 * @param <T>
 */
public class StoreEventPublisher<T> implements PropertyEventPublisher<T>
{
  private static final Logger LOG = LoggerFactory.getLogger(StoreEventPublisher.class);
  private final PropertyStore<T> _store;
  private PropertyEventBus<T> _eventBus;

  public StoreEventPublisher(PropertyStore<T> store)
  {
    _store = store;
  }

  @Override
  public void setBus(PropertyEventBus<T> tPropertyEventBus)
  {
    _eventBus = tPropertyEventBus;
  }

  @Override
  public void startPublishing(String prop)
  {
    try
    {
      _eventBus.publishInitialize(prop, _store.get(prop));
    }
    catch (PropertyStoreException e)
    {
      LOG.error("Failed to get property " + prop + " from underlying store", e);
      // Publish null value to avoid hanging waiters; Listener interface does not contemplate errors
      _eventBus.publishInitialize(prop, null);
    }
  }

  @Override
  public void stopPublishing(String prop)
  {

  }

  @Override
  public void start(Callback<None> callback)
  {
    callback.onSuccess(None.none());
  }

  @Override
  public void shutdown(PropertyEventThread.PropertyEventShutdownCallback callback)
  {
    _store.shutdown(callback);
  }
}
