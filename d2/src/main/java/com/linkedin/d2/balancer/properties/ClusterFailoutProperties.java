package com.linkedin.d2.balancer.properties;

import java.util.Map;
import java.util.List;
import com.linkedin.d2.balancer.clusterfailout.ClusterFailoutRedirectConfig;
import com.linkedin.d2.balancer.clusterfailout.ClusterFailoutBucketConfig;
import java.util.stream.Collectors;

/**
 * Configuration for a service's cluster level failout properties. These properties are used to control
 * the flow of traffic between datacenters.
 */
public class ClusterFailoutProperties
{
  private final List<ClusterFailoutRedirectConfig> _clusterFailoutRedirectConfigs;
  private final List<ClusterFailoutBucketConfig> _clusterFailoutBucketConfigs;

  public ClusterFailoutProperties(List<Map<String, Object>> clusterFailoutRedirectConfigs,
      List<Map<String, Object>> clusterFailoutBucketConfigs) {
    _clusterFailoutBucketConfigs = clusterFailoutBucketConfigs.stream().map(ClusterFailoutBucketConfig::new).collect(Collectors.toList());
    _clusterFailoutRedirectConfigs = clusterFailoutRedirectConfigs.stream().map(ClusterFailoutRedirectConfig::new).collect(Collectors.toList());
  }

  public List<ClusterFailoutRedirectConfig> getClusterFailoutRedirectConfigs() {
    return _clusterFailoutRedirectConfigs;
  }

  public List<ClusterFailoutBucketConfig> getClusterFailoutBucketConfigs() {
    return _clusterFailoutBucketConfigs;
  }

  @Override
  public String toString() {
    return "ClusterFailoutProperties [_clusterFailoutRedirectConfigs=" + _clusterFailoutRedirectConfigs
        + ", _clusterFailoutBucketConfigs=" + _clusterFailoutBucketConfigs
        + "]";
  }

  @Override
  public int hashCode() {
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
    if (!_clusterFailoutRedirectConfigs.equals(other.getClusterFailoutRedirectConfigs()))
      return false;
    if (!_clusterFailoutBucketConfigs.equals(other.getClusterFailoutBucketConfigs()))
      return false;
    return true;
  }
}
