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

package com.linkedin.restli.server;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class BatchResult<K, V> implements Map<K, V>
{
  private final Map<K, V> _data;
  private final Map<K, RestLiServiceException> _errors;

  public BatchResult(final Map<K, V> data, final Map<K, RestLiServiceException> errors)
  {
    _data = data;
    _errors = errors;
  }

  @Override
  public int size()
  {
    return _data.size();
  }

  @Override
  public boolean isEmpty()
  {
    return _data.isEmpty();
  }

  @Override
  public boolean containsKey(final Object key)
  {
    return _data.containsKey(key);
  }

  @Override
  public boolean containsValue(final Object value)
  {
    return _data.containsValue(value);
  }

  @Override
  public V get(final Object key)
  {
    return _data.get(key);
  }

  @Override
  public V put(final K key, final V value)
  {
    return _data.put(key, value);
  }

  @Override
  public V remove(final Object key)
  {
    return _data.remove(key);
  }

  @Override
  public void putAll(final Map<? extends K, ? extends V> m)
  {
    _data.putAll(m);
  }

  @Override
  public void clear()
  {
    _data.clear();
  }

  @Override
  public Set<K> keySet()
  {
    return _data.keySet();
  }

  @Override
  public Collection<V> values()
  {
    return _data.values();
  }

  @Override
  public Set<Entry<K, V>> entrySet()
  {
    return _data.entrySet();
  }

  public Map<K, RestLiServiceException> getErrors()
  {
    return Collections.unmodifiableMap(_errors);
  }
}
