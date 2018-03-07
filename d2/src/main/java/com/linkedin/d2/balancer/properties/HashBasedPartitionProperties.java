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

package com.linkedin.d2.balancer.properties;

import com.linkedin.util.ArgumentUtil;

/**
 * The formula for partitionId would be like this:
 * partitionId = hash(partitionKey) % partitionCount
 * if hashAlgorithm is modulo, then we assume partitionKey is a long number
 * and partitionId = partitionKey % partitionCount
 */
public class HashBasedPartitionProperties implements PartitionProperties
{
  private final String              _partitionKeyRegex;
  private final int                 _partitionCount;
  // The HashAlgorithm to use is specified in cluster properties.
  // There is no default hash. Users must specify one.
  private final HashAlgorithm       _hashAlgorithm;

  public enum HashAlgorithm
  {
    MODULO, MD5, XXHASH
  }
  public HashBasedPartitionProperties(String partitionKeyRegex, int partitionCount, HashAlgorithm hashAlgorithm)
  {
    ArgumentUtil.notNull(partitionKeyRegex, PropertyKeys.PARTITION_KEY_REGEX);
    _partitionKeyRegex = partitionKeyRegex;
    _partitionCount = partitionCount;
    _hashAlgorithm = hashAlgorithm;
  }

  public String getPartitionKeyRegex()
  {
    return _partitionKeyRegex;
  }

  public int getPartitionCount()
  {
    return _partitionCount;
  }

  public HashAlgorithm getHashAlgorithm()
  {
    return _hashAlgorithm;
  }

  @Override
  public PartitionType getPartitionType()
  {
    return PartitionType.HASH;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
    {
      return true;
    }
    if (obj == null || getClass() != obj.getClass())
    {
      return false;
    }
    HashBasedPartitionProperties other = (HashBasedPartitionProperties) obj;
    if (!_partitionKeyRegex.equals(other._partitionKeyRegex))
    {
      return false;
    }
    if (_partitionCount != other._partitionCount)
    {
      return false;
    }
    if (_hashAlgorithm != other._hashAlgorithm)
    {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_partitionKeyRegex == null) ? 0 : _partitionKeyRegex.hashCode());
    result = prime * result + _partitionCount;
    result = prime * result + ((_hashAlgorithm == null) ? 0 : _hashAlgorithm.hashCode());
    return result;
  }
}
