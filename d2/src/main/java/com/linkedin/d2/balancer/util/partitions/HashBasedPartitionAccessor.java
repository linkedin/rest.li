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
import com.linkedin.d2.balancer.util.hashing.MD5Hash;

public class HashBasedPartitionAccessor extends AbstractPartitionAccessor
{
  final private HashBasedPartitionProperties _properties;
  public HashBasedPartitionAccessor(HashBasedPartitionProperties properties)
  {
    super(properties.getPartitionKeyRegex(), properties.getPartitionCount() - 1);
    _properties = properties;
  }

  @Override
  public int getPartitionId(String key)
      throws PartitionAccessException
  {
    HashBasedPartitionProperties.HashAlgorithm hashAlgorithm = _properties.getHashAlgorithm();
    long longKey;
    switch(hashAlgorithm)
    {
      case MODULO:
        try
        {
          longKey = Long.parseLong(key);
        }
        catch (NumberFormatException e)
        {
          throw new PartitionAccessException("Using MODULO hash function. Keys should be long values, but failed to parse key to long: " + key);
        }
        break;
      case MD5:
        MD5Hash hashFunction = new MD5Hash();
        String[] keyStrings = new String[1];
        keyStrings[0] = key;
        longKey = hashFunction.hashLong(keyStrings);
        break;
      default:
        // impossible to happen
        throw new PartitionAccessException("Unsupported hash algorithm");
    }

    return Math.abs((int)(longKey % _properties.getPartitionCount()));
  }
}
