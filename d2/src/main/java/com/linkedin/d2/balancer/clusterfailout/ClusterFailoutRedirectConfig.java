package com.linkedin.d2.balancer.clusterfailout;

import java.util.Map;

/**
 * This class is a simple data structure for controlling the weighting of how much traffic is sent to a given fabric.
 */

public class ClusterFailoutRedirectConfig
{
  // Fabric that this configuration is addressing.
  private final String _fabricUrn;
  // Relative weight for traffic distribution
  private final Integer _weight;

  public ClusterFailoutRedirectConfig(String fabricUrn, Integer weight) {
    _fabricUrn = fabricUrn;
    _weight = weight;
  }

  public ClusterFailoutRedirectConfig(Map<String, Object> configMap) {
    _fabricUrn = (String)configMap.get("fabric");
    _weight = (Integer)configMap.get("weight");
  }

  public String getFabricUrn() {
    return _fabricUrn;
  }

  public Integer getWeight() {
    return _weight;
  }

  @Override
  public String toString() {
    return "ClusterFailoutRedirectConfig [_fabricUrn=" + _fabricUrn
        + ", _weight=" + _weight
        + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + _fabricUrn.hashCode();
    result = prime * result + _weight.hashCode();
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

    ClusterFailoutRedirectConfig other = (ClusterFailoutRedirectConfig) obj;
    if (!_fabricUrn.equals(other.getFabricUrn()))
      return false;
    if (!_weight.equals(other.getWeight()))
      return false;
    return true;
  }
}
