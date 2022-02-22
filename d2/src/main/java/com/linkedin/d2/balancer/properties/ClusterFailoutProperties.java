package com.linkedin.d2.balancer.properties;

import java.util.Map;
import java.util.List;
import com.linkedin.d2.balancer.clusterfailout.ClusterFailoutRedirectConfig;
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


import com.linkedin.d2.balancer.clusterfailout.ClusterFailoutBucketConfig;
import java.util.stream.Collectors;
import java.util.Collections;

/**
 * Configuration for a service's cluster level failout properties. These properties are used to control
 * the flow of traffic between datacenters.
 */
public class ClusterFailoutProperties
{
  private final List<ClusterFailoutRedirectConfig> _clusterFailoutRedirectConfigs;
  private final List<ClusterFailoutBucketConfig> _clusterFailoutBucketConfigs;

  public ClusterFailoutProperties(List<Map<String, Object>> clusterFailoutRedirectConfigs,
      List<Map<String, Object>> clusterFailoutBucketConfigs)
  {
    _clusterFailoutBucketConfigs = Collections.unmodifiableList(
        clusterFailoutBucketConfigs.stream()
        .map(ClusterFailoutBucketConfig::createFromMap)
        .filter(o -> o != null)
        .collect(Collectors.toList()));
    _clusterFailoutRedirectConfigs = Collections.unmodifiableList(
        clusterFailoutRedirectConfigs.stream()
        .map(ClusterFailoutRedirectConfig::createFromMap)
        .filter(o -> o != null)
        .collect(Collectors.toList()));
  }

  public List<ClusterFailoutRedirectConfig> getClusterFailoutRedirectConfigs()
  {
    return _clusterFailoutRedirectConfigs;
  }

  public List<ClusterFailoutBucketConfig> getClusterFailoutBucketConfigs()
  {
    return _clusterFailoutBucketConfigs;
  }

  @Override
  public String toString()
  {
    return "ClusterFailoutProperties [_clusterFailoutRedirectConfigs=" + _clusterFailoutRedirectConfigs
        + ", _clusterFailoutBucketConfigs=" + _clusterFailoutBucketConfigs
        + "]";
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + _clusterFailoutRedirectConfigs.hashCode();
    result = prime * result + _clusterFailoutBucketConfigs.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;

    ClusterFailoutProperties other = (ClusterFailoutProperties) obj;
    return _clusterFailoutRedirectConfigs.equals(other.getClusterFailoutRedirectConfigs()) &&
          _clusterFailoutBucketConfigs.equals(other.getClusterFailoutBucketConfigs());
  }
}
