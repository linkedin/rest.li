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

package com.linkedin.d2.balancer.config;

import com.linkedin.d2.discovery.PropertyBuilder;
import com.linkedin.d2.discovery.stores.PropertyStoreAsync;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class ConfigWriter<T>
{
  private final PropertyStoreAsync<T> _store;
  private final PropertyBuilder<T> _builder;
  private final Map<String,Map<String,Object>> _source;
  private final Map<String,Object> _defaultMap;
  private final long _timeout;
  private final TimeUnit _timeoutUnit;

  public ConfigWriter(PropertyStoreAsync<T> store, PropertyBuilder<T> builder,
                      Map<String,Map<String,Object>> source,
                      Map<String,Object> defaultMap,
                      long timeout, TimeUnit timeoutUnit)
  {
    _store = store;
    _builder = builder;
    _source = source;
    _defaultMap = defaultMap;
    _timeout = timeout;
    _timeoutUnit = timeoutUnit;
  }

  public void writeConfig() throws ExecutionException, TimeoutException, InterruptedException
  {
    FutureCallback<None> callback = new FutureCallback<None>();
    _store.start(callback);
    callback.get(_timeout, _timeoutUnit);

    for (String key : _source.keySet())
    {

      Map<String,Object> map = merge(_source.get(key), _defaultMap);

      T properties = _builder.fromMap(map);
      FutureCallback<None> putCallback = new FutureCallback<None>();
      _store.put(key, properties, putCallback);
      putCallback.get(_timeout, _timeoutUnit);
    }

    FutureCallback<None> shutdownCallback = new FutureCallback<None>();
    _store.shutdown(shutdownCallback);
    shutdownCallback.get(_timeout, _timeoutUnit);
  }

  public static Map<String,Object> merge(Map<String,Object> source, Map<String,Object> defaultMap)
  {
    if (source == null)
    {
      source = Collections.emptyMap();
    }
    if (defaultMap == null)
    {
      defaultMap = Collections.emptyMap();
    }
    Map<String,Object> result = new HashMap<String,Object>(defaultMap);
    for (String key : source.keySet())
    {
      Object sourceValue = source.get(key);
      Object resultValue = result.get(key);
      if (resultValue instanceof Map || sourceValue instanceof Map)
      {
        @SuppressWarnings(value="unchecked")
        Map<String,Object> subDefault = (Map<String,Object>)resultValue;

        @SuppressWarnings(value="unchecked")
        Map<String,Object> subSource = (Map<String,Object>)sourceValue;

        result.put(key, merge(subSource, subDefault));
      }
      else
      {
        result.put(key, sourceValue);
      }
    }
    return result;
  }


}
