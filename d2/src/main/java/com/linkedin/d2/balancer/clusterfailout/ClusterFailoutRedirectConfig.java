package com.linkedin.d2.balancer.clusterfailout;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a simple data structure for controlling the weighting of how much traffic is sent to a given fabric.
 */

public class ClusterFailoutRedirectConfig
{
  // Fabric that this configuration is addressing.
  private final String _fabricUrn;
  // Relative weight for traffic distribution
  private final Integer _weight;

  private final static String FABRIC_URN_PROPERTY = "fabric";
  private final static String WEIGHT_PROPERTY = "weight";

  private static final Logger _logger = LoggerFactory.getLogger(
      ClusterFailoutRedirectConfig.class);

  public ClusterFailoutRedirectConfig(String fabricUrn, Integer weight) {
    _fabricUrn = fabricUrn;
    _weight = weight;
  }

  public static ClusterFailoutRedirectConfig createFromMap(Map<String, Object> configMap) {
    try {
      return new ClusterFailoutRedirectConfig((String) configMap.get(ClusterFailoutRedirectConfig.FABRIC_URN_PROPERTY),
          (Integer) configMap.get(ClusterFailoutRedirectConfig.WEIGHT_PROPERTY));
    } catch (Exception e) {
      _logger.error("Error while converting SLF properties: " + e.getMessage());
      return null;
    }
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
    return _fabricUrn.equals(other.getFabricUrn()) && _weight.equals(other.getWeight());
  }
}
