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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.d2.discovery.event.PropertyEventSubscriber;
import com.linkedin.d2.discovery.stores.PropertyStore;
import com.linkedin.d2.discovery.stores.PropertyStoreException;

/**
 * Listens to property events and propagates them to an underlying store
 * @author Steven Ihde
 * @version $Revision: $
 */

public class StoreEventSubscriber<T> implements PropertyEventSubscriber<T>
{
  private static final Logger LOG = LoggerFactory.getLogger(StoreEventSubscriber.class);

  private final PropertyStore<T> _store;

  public StoreEventSubscriber(PropertyStore<T> store)
  {
    _store = store;
  }

  @Override
  public void onInitialize(String propertyName, T propertyValue)
  {
    try
    {
      _store.put(propertyName, propertyValue);
      LOG.debug("STORE EVENT SUBSCRIBER. OnInitialize. adding property to file store. propertyName:"+propertyName+" propertyValue:"+propertyValue);

    }
    catch (PropertyStoreException e)
    {
      LOG.error("Failed to write property " + propertyName + " to underlying store", e);
    }
  }

  @Override
  public void onAdd(String propertyName, T propertyValue)
  {
    try
    {
      _store.put(propertyName, propertyValue);
    }
    catch (PropertyStoreException e)
    {
      LOG.error("Failed to write property " + propertyName + " to underlying store", e);
    }
  }

  @Override
  public void onRemove(String propertyName)
  {
    try
    {
      _store.remove(propertyName);
    }
    catch (PropertyStoreException e)
    {
      LOG.error("Failed to remove property " + propertyName + " from underlying store", e);
    }
  }
}
