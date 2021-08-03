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

package com.linkedin.restli.internal.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author dellamag
 */
public class PathKeysImpl implements MutablePathKeys
{
  private final Map<String, Object> _keyMap;
  private Set<Object>               _batchKeys;

  /**
   * Default constructor.
   */
  public PathKeysImpl()
  {
    super();
    _keyMap = new HashMap<>(4);
  }

  @Override
  public MutablePathKeys append(final String key, final Object value)
  {
    _keyMap.put(key, value);
    return this;
  }

  @Override
  public MutablePathKeys setBatchKeys(final Set<Object> batchKeys)
  {
    _batchKeys = batchKeys;
    return this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T get(final String key)
  {
    return (T) _keyMap.get(key);
  }

  @Override
  public Integer getAsInt(final String key)
  {
    return (Integer) _keyMap.get(key);
  }

  @Override
  public Long getAsLong(final String key)
  {
    return (Long) _keyMap.get(key);
  }

  @Override
  public String getAsString(final String key)
  {
    return (String) _keyMap.get(key);
  }

  @Override
  public Map<String, Object> getKeyMap() {
    return Collections.unmodifiableMap(_keyMap);
  }

  @Deprecated
  @Override
  public Set<?> getBatchKeys()
  {
    if (_batchKeys == null)
    {
      return Collections.emptySet();
    }
    else
    {
      return Collections.unmodifiableSet(_batchKeys);
    }
  }

  @Deprecated
  @Override
  @SuppressWarnings("unchecked")
  public <T> Set<T> getBatchKeys(final Class<T> keyClass)
  {
    return (Set<T>) getBatchKeys();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Set<T> getBatchIds()
  {
    if (_batchKeys == null)
    {
      return null;
    }
    else
    {
      return Collections.unmodifiableSet((Set<T>) _batchKeys);
    }
  }
}
