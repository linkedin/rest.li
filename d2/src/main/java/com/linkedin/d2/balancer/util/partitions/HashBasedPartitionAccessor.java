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

package com.linkedin.d2.balancer.util.partitions;

import com.linkedin.d2.balancer.properties.HashBasedPartitionProperties;
import com.linkedin.d2.balancer.util.hashing.HashFunction;
import com.linkedin.d2.balancer.util.hashing.MD5Hash;

public class HashBasedPartitionAccessor extends AbstractPartitionAccessor
{
  final private HashBasedPartitionProperties _properties;
  final private HashFunction<String[]> _hashFunction;

  public HashBasedPartitionAccessor(HashBasedPartitionProperties properties)
  {
    super(properties.getPartitionKeyRegex(), properties.getPartitionCount() - 1);
    _properties = properties;
    HashBasedPartitionProperties.HashAlgorithm hashAlgorithm = _properties.getHashAlgorithm();

    switch(hashAlgorithm)
    {
      case MODULO:
        _hashFunction = new ModuloHash();
        break;
      case MD5:
        _hashFunction = new MD5Hash();
        break;
      default:
        // impossible to happen
        throw new IllegalArgumentException("Unsupported hash algorithm: " + hashAlgorithm);
    }
  }

  @Override
  public int getPartitionId(String key)
      throws PartitionAccessException
  {
    try
    {
      long longKey = _hashFunction.hashLong(new String[]{key});

      return Math.abs((int) (longKey % _properties.getPartitionCount()));
    }
    catch (Exception ex)
    {
      throw new PartitionAccessException("Failed to getPartitionId", ex);
    }
  }

  private static class ModuloHash implements HashFunction<String[]>
  {
    @Override
    public int hash(String key[])
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public long hashLong(String key[])
    {
      try
      {
        return Long.parseLong(key[0]);
      }
      catch (NumberFormatException ex)
      {
        throw new IllegalArgumentException("Using MODULO hash function. Keys should be long values, but failed to parse key to long: " + key[0], ex);
      }
    }
  }

}
