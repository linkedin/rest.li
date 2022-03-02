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

import java.util.Map;
import java.util.List;

import java.util.stream.Collectors;
import java.util.Collections;

/**
 * Configuration for a service's cluster level failout properties. These properties are used to control
 * the flow of traffic between datacenters.
 */
public class ClusterFailoutProperties
{
  private final List<Map<String, Object>> _clusterFailoutRedirectConfigs;
  private final List<Map<String, Object>> _clusterFailoutBucketConfigs;

  public ClusterFailoutProperties(List<Map<String, Object>> clusterFailoutRedirectConfigs,
      List<Map<String, Object>> clusterFailoutBucketConfigs)
  {
    _clusterFailoutBucketConfigs = (clusterFailoutBucketConfigs != null) ? clusterFailoutBucketConfigs:Collections.emptyList();
    _clusterFailoutRedirectConfigs = (clusterFailoutRedirectConfigs != null) ? clusterFailoutRedirectConfigs:Collections.emptyList();
  }

  public List<Map<String, Object>> getClusterFailoutRedirectConfigs()
  {
    return _clusterFailoutRedirectConfigs;
  }

  public List<Map<String, Object>> getClusterFailoutBucketConfigs()
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
    if (other == null)
      return false;
    if ((_clusterFailoutRedirectConfigs == null && other.getClusterFailoutRedirectConfigs() != null) ||
        (_clusterFailoutBucketConfigs == null && other.getClusterFailoutBucketConfigs() != null))
      return false;

    return _clusterFailoutRedirectConfigs.equals(other.getClusterFailoutRedirectConfigs()) &&
          _clusterFailoutBucketConfigs.equals(other.getClusterFailoutBucketConfigs());
  }
}
