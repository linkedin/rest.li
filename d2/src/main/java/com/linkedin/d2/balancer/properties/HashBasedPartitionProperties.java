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
    MODULO, MD5
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

}
