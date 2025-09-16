package com.linkedin.d2.balancer.properties;

import com.google.common.base.MoreObjects;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Method level properties is configuration for supporting service/method level overrides in gRPC.
 */
public class MethodLevelProperties {
  // name list to configure service and method names
  private final List<Name> _names;
  // transport client properties overrides per configured service and method names
  private final Map<String, Object> _transportClientProperties;
  // service metadata properties overrides per configured service and method names
  private final Map<String, Object> _serviceMetadataProperties;

  public MethodLevelProperties(List<Name> names, Map<String, Object> transportClientProperties, Map<String, Object> serviceMetadataProperties) {
    _names = names;
    _transportClientProperties = transportClientProperties;
    _serviceMetadataProperties = serviceMetadataProperties;
  }

  public List<Name> getNames() {
    return _names;
  }

  public Map<String, Object> getTransportClientProperties() {
    return _transportClientProperties;
  }

  public Map<String, Object> getServiceMetadataProperties() {
    return _serviceMetadataProperties;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("names", _names)
        .add("transportClientProperties", _transportClientProperties )
        .add("serviceMetadataProperties", _serviceMetadataProperties)
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(_names, _transportClientProperties, _serviceMetadataProperties);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    MethodLevelProperties other = (MethodLevelProperties) obj;
    return Objects.equals(_names, other._names)
        && Objects.equals(_transportClientProperties, other._transportClientProperties)
        && Objects.equals(_serviceMetadataProperties, other._serviceMetadataProperties);
  }
}
