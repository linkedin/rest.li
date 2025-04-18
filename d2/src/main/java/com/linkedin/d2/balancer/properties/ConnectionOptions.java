package com.linkedin.d2.balancer.properties;

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;


public class ConnectionOptions {
  private final int _connectionJitterSeconds;
  private final float _maxDelayedConnectionRatio;

  public ConnectionOptions(int connectionJitterSeconds, float maxDelayedConnectionRatio) {
    _connectionJitterSeconds = connectionJitterSeconds;
    _maxDelayedConnectionRatio = maxDelayedConnectionRatio;
  }

  public int getConnectionJitterSeconds() {
    return _connectionJitterSeconds;
  }

  public float getMaxDelayedConnectionRatio() {
    return _maxDelayedConnectionRatio;
  }

  @Override
  public String toString() {
    return "ConnectionOptions{" + "_connectionJitterSeconds=" + _connectionJitterSeconds
        + ", _maxDelayedConnectionRatio=" + _maxDelayedConnectionRatio + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConnectionOptions that = (ConnectionOptions) o;
    return _connectionJitterSeconds == that._connectionJitterSeconds
        && Float.compare(_maxDelayedConnectionRatio, that._maxDelayedConnectionRatio) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_connectionJitterSeconds, _maxDelayedConnectionRatio);
  }
}
