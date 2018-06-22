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

package com.linkedin.d2.balancer;

import com.linkedin.d2.balancer.util.hashing.HashRingProvider;
import com.linkedin.d2.balancer.util.partitions.PartitionInfoProvider;
import com.linkedin.r2.transport.common.TransportClientFactory;

/**
 * Facilities provides a mechanism to access additional capabilities provided by the D2 infrastructure.
 *
 * @author Josh Walker
 * @version $Revision: $
 */
public interface Facilities
{
  /**
   * Obtain d2 service directory
   * @return Directory
   */
  Directory getDirectory();

  /**
   * Obtain partition info provider
   * @return PartitionInfoProvider
   */
  PartitionInfoProvider getPartitionInfoProvider();

  /**
   * Obtain hashRing provider
   * @return HashRingProvider
   */
  HashRingProvider getHashRingProvider();

  /**
   * Obtain d2 key mapping facility
   * @return KeyMapper
   */
  KeyMapper getKeyMapper();

  /**
   * Obtain TransportClientFactory for
   * specified scheme
   * @return TransportClientFactory for given scheme, or null if no factory is configured in d2
   */
  TransportClientFactory getClientFactory(String scheme);
}
