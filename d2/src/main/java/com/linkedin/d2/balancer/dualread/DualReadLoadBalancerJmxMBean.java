/*
   Copyright (c) 2023 LinkedIn Corp.

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

package com.linkedin.d2.balancer.dualread;

public interface DualReadLoadBalancerJmxMBean
{
  // Error count is incremented only when data of the same version is unequal
  int getServicePropertiesErrorCount();

  int getClusterPropertiesErrorCount();

  @Deprecated
  int getUriPropertiesErrorCount();

  // Evict count is incremented when cache grows to the max size and entries get evicted.
  int getServicePropertiesEvictCount();

  int getClusterPropertiesEvictCount();

  @Deprecated
  int getUriPropertiesEvictCount();

  // Entries become out of sync when:
  // 1) data of the same version is unequal.
  // OR. 2) data of a newer version is received in one cache before the other cache receives the older version to compare.
  // Note that entries in each cache are counted individually.
  // For example: A1 != A2 is considered as TWO entries being out of sync.
  int getServicePropertiesOutOfSyncCount();

  int getClusterPropertiesOutOfSyncCount();

  @Deprecated
  int getUriPropertiesOutOfSyncCount();

  // Similarity is calculated as the ratio of the number of URIs that are common between the two LBs to the total
  // number of URIs in both LBs.
  double getUriPropertiesSimilarity();

  // Returns the ClusterMatchRecord for the given clusterName.
  UriPropertiesDualReadMonitor.ClusterMatchRecord getClusterMatchRecord(String clusterName);
}
