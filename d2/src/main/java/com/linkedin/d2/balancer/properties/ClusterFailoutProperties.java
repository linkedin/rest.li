package com.linkedin.d2.balancer.properties;

import java.util.Map;
import java.util.List;


/**
 * Configuration for a service's cluster level failout properties. These properties are used to control
 * the flow of traffic between datacenters.
 */
public class ClusterFailoutProperties
{
  /* Controls how requests from offline buckets are routed.
  * _clusterFailoutRedirectConfigs = [
  *   { destination: FabricUrn,
   *     weight: int
   *   },]
   */
  private final List<Map<String, Object>> _clusterFailoutRedirectConfigs;

  /* Controls which buckets are offline.
   * _clusterFailoutBucketConfigs = [
   * { fabric: FabricUrn,
   *   partition: string,
   *  bucketIdRange: IntRange,
   *  offlineAt: Time,
   *  onlineAt: optional Time
  }
   */
  private final List<Map<String, Object>> _clusterFailoutBucketConfigs;

  public ClusterFailoutProperties(List<Map<String, Object>> clusterFailoutRedirectConfigs,
      List<Map<String, Object>> clusterFailoutBucketConfigs) {
    _clusterFailoutBucketConfigs = clusterFailoutBucketConfigs;
    _clusterFailoutRedirectConfigs = clusterFailoutRedirectConfigs;
  }

  public List<Map<String, Object>> getClusterFailoutRedirectConfigs() {
    return _clusterFailoutRedirectConfigs;
  }

  public List<Map<String, Object>> getClusterFailoutBucketConfigs() {
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
