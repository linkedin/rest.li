package com.linkedin.d2.balancer.properties;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import javax.annotation.Nullable;


/**
 * Name properties
 */
public class Name {
  // gRPC service name, example: com.linkedin.app.foo.protobuf.servicename
  private final String _service;
  // gRPC method name
  private final String _method;

  public Name(String service, @Nullable String method) {
    _service = service;
    _method = method;
  }

  public String getService() {
    return _service;
  }

  public String getMethod() {
    return _method;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("service", _service)
        .add("method", _method)
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(_service, _method);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    Name other = (Name) obj;
    return Objects.equals(_service, other._service)
        && Objects.equals(_method, other._method);
  }
}
