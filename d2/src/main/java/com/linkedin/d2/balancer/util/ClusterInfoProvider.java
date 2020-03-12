/*
   Copyright (c) 2019 LinkedIn Corp.

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

import com.linkedin.d2.DarkClusterConfigMap;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;


/**
 * ClusterInfoProvider provides a mechanism to access detailed cluster information from the D2 infrastructure.
 *
 * @author David Hoa
 * @version $Revision: $
 */
public interface ClusterInfoProvider
{

  /**
   * Obtain d2 cluster count
   * @return int
   */
  int getClusterCount(String clusterName, String scheme, int partitionId) throws ServiceUnavailableException;

  /**
   * Helpful utility method for default behavior
   */
  default int getHttpsClusterCount(String clusterName) throws ServiceUnavailableException
  {
    return getClusterCount(clusterName, PropertyKeys.HTTPS_SCHEME, DefaultPartitionAccessor.DEFAULT_PARTITION_ID);
  }

  /**
   * Get the DarkClusterConfigMap for a particular d2 cluster. This is needed to iterate through the dark clusters that correspond
   * to a regular d2 cluster.
   *
   * @param clusterName
   * @return
   * @throws ServiceUnavailableException
   */
  default DarkClusterConfigMap getDarkClusterConfigMap(String clusterName) throws ServiceUnavailableException
  {
    return new DarkClusterConfigMap();
  }
}
