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

package com.linkedin.d2.balancer.util.hashing;

import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.util.MapKeyResult;

import com.linkedin.r2.message.Request;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
* @author Josh Walker
* @version $Revision: $
*/
public class StaticRingProvider implements HashRingProvider
{
  private final List<Ring<URI>> _rings;
  private HashFunction<Request> _hashFunction;

  public StaticRingProvider(Ring<URI> ring)
  {
    List<Ring<URI>> rings = new ArrayList<Ring<URI>>();
    rings.add(ring);
    _rings = Collections.unmodifiableList(rings);
    _hashFunction = null;
  }

  public StaticRingProvider(List<Ring<URI>> rings)
  {
    _rings = Collections.unmodifiableList(new ArrayList<Ring<URI>>(rings));
    _hashFunction = null;
  }

  @Override
  public <K> MapKeyResult<Ring<URI>, K> getRings(URI serviceUri, Iterable<K> keys)
      throws ServiceUnavailableException
  {
    if (_rings.size() < 1)
    {
      throw new ServiceUnavailableException("PEGA_1030. Ring not configured:", serviceUri.toString());
    }

    Map<Ring<URI>, Collection<K>> result = new HashMap<Ring<URI>, Collection<K>>();
    List<MapKeyResult.UnmappedKey<K>> unmappedKeys = new ArrayList<MapKeyResult.UnmappedKey<K>>();
    for (K key : keys)
    {
      // assume key could be parsed to int, just for simplicity, as this is only used in tests
      try
      {
        final long longK = Long.parseLong(key.toString());
        final int index = (int) longK % (_rings.size());
        Ring<URI> ring = _rings.get(index);
        Collection<K> set = result.get(ring);
        if (set == null)
        {
          set = new HashSet<K>();
          result.put(ring, set);
        }
        set.add(key);
      }
      catch(NumberFormatException e)
      {
        unmappedKeys.add(new MapKeyResult.UnmappedKey<K>(key, MapKeyResult.ErrorType.FAIL_TO_FIND_PARTITION));
      }
    }

    return new MapKeyResult<Ring<URI>, K>(result, unmappedKeys);
  }

  @Override
  public Map<Integer, Ring<URI>> getRings(URI serviceUri)
  {
    int partitionCount = _rings.size();
    Map<Integer, Ring<URI>> ringMap = new HashMap<Integer, Ring<URI>>(partitionCount * 2);
    for (int partitionId = 0; partitionId < partitionCount; partitionId++)
    {
      ringMap.put(partitionId, _rings.get(partitionId));
    }
    return ringMap;
  }

  public void setHashFunction(HashFunction<Request> func)
  {
    _hashFunction = func;
  }

  @Override
  public HashFunction<Request> getRequestHashFunction(String serviceName) throws ServiceUnavailableException
  {
    if (_hashFunction == null)
    {
      throw new RuntimeException("HashFunction is not set for StaticRingProvider");
    }
    return _hashFunction;
  }
}
