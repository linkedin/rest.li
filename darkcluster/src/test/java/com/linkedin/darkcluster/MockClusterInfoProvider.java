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

public class MockClusterInfoProvider implements ClusterInfoProvider
{
  Map<String, DarkClusterConfigMap> lookupMap = new HashMap<>();
  List<LoadBalancerClusterListener> clusterListeners = new ArrayList<>();
  Map<String, Integer> clusterHttpsCount = new HashMap<>();

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
    return clusterHttpsCount.getOrDefault(clusterName, 1);
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

  @Override
  public void unregisterClusterListener(LoadBalancerClusterListener clusterListener)
  {
    clusterListeners.remove(clusterListener);
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

  void notifyListenersClusterAdded(String clusterName)
  {
    for (LoadBalancerClusterListener listener : clusterListeners)
    {
      listener.onClusterAdded(clusterName);
    }
  }

  void notifyListenersClusterRemoved(String clusterName)
  {
    for (LoadBalancerClusterListener listener : clusterListeners)
    {
      listener.onClusterRemoved(clusterName);
    }
  }

  /**
   * add the ability to return httpsClusterCounts for a clusterName
   */
  void putHttpsClusterCount(String clusterName, Integer httpsCount)
  {
    // overwrites if anything is already there for this clusterName
    clusterHttpsCount.put(clusterName, httpsCount);
  }
}
