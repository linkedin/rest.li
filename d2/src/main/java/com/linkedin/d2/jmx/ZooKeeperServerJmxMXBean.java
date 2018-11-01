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

package com.linkedin.d2.jmx;

import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.discovery.stores.PropertyStoreException;

import java.util.Map;

public interface ZooKeeperServerJmxMXBean
{
  // do not mark as deprecated yet; mark when we are ready to completely migrate to the new code
  void setMarkUp(String clusterName, String uri, double weight) throws PropertyStoreException;

  void setMarkup(String clusterName, String uri, Map<Integer, PartitionData> partitionDataMap) throws PropertyStoreException;

  void setMarkup(String clusterName, String uri, Map<Integer, PartitionData> partitionDataMap, Map<String, Object> uriSpecificProperties) throws PropertyStoreException;

  void setMarkDown(String clusterName, String uri) throws PropertyStoreException;

  /**
   * Change the weight of an existing host based on the given partitionDataMap.
   *
   * @param doNotSlowStart Flag to let clients know if slow start should be avoided for a host.
   */
  void setChangeWeight(String clusterName, String uri, Map<Integer, PartitionData> partitionDataMap, boolean doNotSlowStart) throws PropertyStoreException;
}
