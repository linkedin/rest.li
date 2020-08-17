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

import com.linkedin.common.callback.Callback;
import com.linkedin.d2.DarkClusterConfigMap;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.LoadBalancerClusterListener;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;


/**
 * ClusterInfoProvider provides a mechanism to access detailed cluster information from the D2 infrastructure.
 * Implementations should implement at least getClusterCount, getDarkClusterConfigMap, registerClusterListener,
 * and unregisterClusterListener. Some have a default implementation for backwards compatibility reasons, but
 * should be regarded as required.
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
   * Get the DarkClusterConfigMap for a particular d2 cluster. This is needed to to find the dark clusters that correspond
   * to a regular d2 cluster.
   *
   * use the callback version instead to avoid blocking threads. Unfortunately, we can't put @Deprecated on
   * the method or in this javadoc because there's a bug in the jdk that won't suppress warnings on implementations.
   * This was supposedly fixed in https://bugs.java.com/bugdatabase/view_bug.do?bug_id=6480588 but the problem is still
   * in 8u172.
   * @param clusterName
   * @return
   * @throws ServiceUnavailableException
   */
  default DarkClusterConfigMap getDarkClusterConfigMap(String clusterName) throws ServiceUnavailableException
  {
    return new DarkClusterConfigMap();
  }

  /**
   * Get the DarkClusterConfigMap for a particular d2 cluster. This is needed to to find the dark clusters that correspond
   * to a regular d2 cluster.
   *
   * @param clusterName name of the source cluster
   * @param callback callback to invoke when the DarkClusterConfigMap is retrieved
   */
  void getDarkClusterConfigMap(String clusterName, Callback<DarkClusterConfigMap> callback);

  /**
   * Register a listener for Cluster changes. Listeners can refresh any internal state/cache after getting triggered.
   */
  default void registerClusterListener(LoadBalancerClusterListener clusterListener)
  {
  }

  /**
   * Unregister a cluster listener.
   */
  default void unregisterClusterListener(LoadBalancerClusterListener clusterListener)
  {
  }
}
