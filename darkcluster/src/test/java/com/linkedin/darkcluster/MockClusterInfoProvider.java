/*
   Copyright (c) 2020 LinkedIn Corp.

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

package com.linkedin.darkcluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.linkedin.d2.DarkClusterConfig;
import com.linkedin.d2.DarkClusterConfigMap;
import com.linkedin.d2.balancer.LoadBalancerClusterListener;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.util.ClusterInfoProvider;

class MockClusterInfoProvider implements ClusterInfoProvider
{
  Map<String, DarkClusterConfigMap> lookupMap = new HashMap<>();
  List<LoadBalancerClusterListener> clusterListeners = new ArrayList<>();

  @Override
  public int getClusterCount(String clusterName, String scheme, int partitionId)
    throws ServiceUnavailableException
  {
    return 0;
  }

  @Override
  public int getHttpsClusterCount(String clusterName)
    throws ServiceUnavailableException
  {
    return 0;
  }

  @Override
  public DarkClusterConfigMap getDarkClusterConfigMap(String clusterName)
    throws ServiceUnavailableException
  {
    return lookupMap.get(clusterName);
  }

  @Override
  public void registerClusterListener(LoadBalancerClusterListener clusterListener)
  {
    clusterListeners.add(clusterListener);
  }

  /**
   * add the ability to add a dark cluster to a source cluster's darkClusterConfigMap
   */
  void addDarkClusterConfig(String sourceClusterName, String darkClusterName, DarkClusterConfig darkClusterConfig)
  {
    DarkClusterConfigMap darkClusterConfigMap = (lookupMap.containsKey(sourceClusterName)) ? lookupMap.get(sourceClusterName) :
      new DarkClusterConfigMap();

    darkClusterConfigMap.put(darkClusterName, darkClusterConfig);
    lookupMap.put(sourceClusterName, darkClusterConfigMap);
  }

  void triggerClusterRefresh(String clusterName)
  {
    for (LoadBalancerClusterListener listener : clusterListeners)
    {
      listener.onClusterAdded(clusterName);
    }
  }

  void triggerClusterRemove(String clusterName)
  {
    for (LoadBalancerClusterListener listener : clusterListeners)
    {
      listener.onClusterRemoved(clusterName);
    }
  }
}
