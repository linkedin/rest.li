package com.linkedin.d2.balancer.properties;

import com.google.common.base.MoreObjects;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Method level properties is configuration for supporting service/method level overrides in gRPC.
 */
public class MethodLevelProperties {
  // name properties, to configure service and method names
  private final List<NameProperties> _name;
  // transport client properties overrides per configured service and method names
  private final Map<String, Object> _transportClientProperties;
  // service metadata properties overrides per configured service and method names
  private final Map<String, Object> _serviceMetadataProperties;

  public MethodLevelProperties(List<NameProperties> name, Map<String, Object> transportClientProperties, Map<String, Object> serviceMetadataProperties) {
    _name = name;
    _transportClientProperties = transportClientProperties;
    _serviceMetadataProperties = serviceMetadataProperties;
  }

  public List<NameProperties> getName() {
    return _name;
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
        .add("nameProperties", _name)
        .add("transportClientProperties", _transportClientProperties )
        .add("serviceMetadataProperties", _serviceMetadataProperties)
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(_name, _transportClientProperties, _serviceMetadataProperties);
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
    return Objects.equals(_name, other._name)
        && Objects.equals(_transportClientProperties, other._transportClientProperties)
        && Objects.equals(_serviceMetadataProperties, other._serviceMetadataProperties);
  }
}
