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

package com.linkedin.d2.discovery.stores.toggling;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.discovery.stores.PropertyStore;
import com.linkedin.d2.discovery.stores.PropertyStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.d2.discovery.util.LogUtil.info;
import static com.linkedin.d2.discovery.util.LogUtil.warn;

public class TogglingStore<T> implements PropertyStore<T>
{
  private static final Logger    _log = LoggerFactory.getLogger(TogglingStore.class);

  private final PropertyStore<T> _store;
  private volatile boolean       _enabled;

  public TogglingStore(PropertyStore<T> store)
  {
    _store = store;

    setEnabled(store != null);
  }

  public PropertyStore<T> getStore()
  {
    return _store;
  }

  public boolean isEnabled()
  {
    return _enabled;
  }

  public void setEnabled(boolean enabled)
  {
    info(_log, _store, " toggled, where enabled = ", enabled);

    _enabled = enabled;
  }

  @Override
  public T get(String listenTo) throws PropertyStoreException
  {
    if (_enabled)
    {
      return _store.get(listenTo);
    }
    else
    {
      warn(_log, _store, " ignored get request: ", listenTo);
    }

    return null;
  }

  @Override
  public void put(String listenTo, T discoveryProperties) throws PropertyStoreException
  {
    if (_enabled)
    {
      _store.put(listenTo, discoveryProperties);
    }
    else
    {
      warn(_log, _store, " ignored put request: ", listenTo);
    }
  }

  @Override
  public void remove(String listenTo) throws PropertyStoreException
  {
    if (_enabled)
    {
      _store.remove(listenTo);
    }
    else
    {
      warn(_log, _store, " ignored remove request: ", listenTo);
    }
  }

  @Override
  public void start(Callback<None> callback)
  {
    _store.start(callback);
  }

  @Override
  public void shutdown(Callback<None> callback)
  {
    if (_enabled)
    {
      _store.shutdown(callback);
    }
    else
    {
      warn(_log, _store, " shutdown called on disabled store");

      callback.onSuccess(None.none());
    }
  }

  @Override
  public String toString()
  {
    return "TogglingStore [_enabled=" + _enabled + ", _store=" + _store + "]";
  }
}
