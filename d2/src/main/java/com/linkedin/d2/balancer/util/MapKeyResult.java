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

package com.linkedin.d2.balancer.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class includes the mapping result for keys, and the unmapped keys and the errors that
 * prevent them from being mapped.
 * There are now two types of errors:
 * FAIL_TO_FIND_PARTITION, i.e. we can not find the partition for this key;
 * this error could result from error in partition configuration or the illegal keys
 * (e.g. not a long when long is required, etc).
 * NO_HOST_AVAILABLE_IN_PARTITION, this error happens when the partition exists, but no server is serving this
 * partition (no server registered, or all servers are bad, or request dropping is taking place)
 */

public class MapKeyResult<T, K>
{
  public static enum ErrorType
  {
    FAIL_TO_FIND_PARTITION,
    NO_HOST_AVAILABLE_IN_PARTITION,
  }

  public static class UnmappedKey<K>
  {
    private final K _key;
    private final ErrorType _errorType;

    public UnmappedKey(K key, ErrorType errorType)
    {
      _key = key;
      _errorType = errorType;
    }

    public K getKey()
    {
      return _key;
    }

    public ErrorType getErrorType()
    {
      return _errorType;
    }

    @Override
    public int hashCode()
    {
      int hashCode = _key == null ? 1 : _key.hashCode() * 31;
      hashCode = 31 * hashCode * (_errorType == null ? 1 : _errorType.hashCode());
      return hashCode;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object o)
    {
      if (o == null || !(o instanceof  UnmappedKey))
      {
        return false;
      }
      UnmappedKey<K> u = (UnmappedKey<K>) o;
      return this._errorType.equals(u._errorType) &&
          this._key.equals(u._key);
    }
  }

  private final Map<T, Collection<K>> _mapResult;
  private final Collection<UnmappedKey<K>> _unmappedKeys;

  public MapKeyResult(Map<T, Collection<K>> mapResult, Collection<UnmappedKey<K>> unMappedKeys)
  {
    Map<T, Collection<K>> mapResultTmp = new HashMap<T, Collection<K>>(mapResult.size() * 2);
    for (Map.Entry<T, Collection<K>> entry : mapResult.entrySet())
    {
      mapResultTmp.put(entry.getKey(), Collections.unmodifiableCollection(entry.getValue()));
    }
    _mapResult = Collections.unmodifiableMap(mapResultTmp);
    _unmappedKeys = Collections.unmodifiableCollection(unMappedKeys);
  }

  public Map<T, Collection<K>> getMapResult()
  {
    return _mapResult;
  }

  public Collection<UnmappedKey<K>> getUnmappedKeys()
  {
    return _unmappedKeys;
  }
}
