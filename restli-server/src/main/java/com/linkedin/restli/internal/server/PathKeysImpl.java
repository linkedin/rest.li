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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.linkedin.restli.server.PathKeys;

/**
 * @author dellamag
 */
public class PathKeysImpl implements MutablePathKeys
{
  private final Map<String, Object> _keyMap;
  private final Set<Object>         _batchKeys;

  /**
   * Default constructor.
   */
  public PathKeysImpl()
  {
    super();
    _keyMap = new HashMap<String, Object>(4);
    _batchKeys = new HashSet<Object>();
  }

  @Override
  public MutablePathKeys append(final String key, final Object value)
  {
    _keyMap.put(key, value);
    return this;
  }

  @Override
  public PathKeys appendBatchValue(final Object value)
  {
    _batchKeys.add(value);
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
  public Set<?> getBatchKeys()
  {
    return Collections.unmodifiableSet(_batchKeys);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Set<T> getBatchKeys(final Class<T> keyClass)
  {
    return (Set<T>) Collections.unmodifiableSet(_batchKeys);
  }

}
