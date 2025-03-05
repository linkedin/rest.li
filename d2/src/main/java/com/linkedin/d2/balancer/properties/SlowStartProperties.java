package com.linkedin.d2.balancer.properties;

import com.google.common.base.MoreObjects;
import java.util.Objects;


/**
 * Slow start configuration properties for gRPC P2C load balancer.
 */
public class SlowStartProperties {
  // Whether the feature is disabled
  private final boolean _disabled;

  // The duration within which the weight and traffic would be fully ramped
  private final int _windowDurationSeconds;

  // Non-linearly affects the ramp speed, larger values lead to quicker ramping. Generally should be within [1,10].
  private final double _aggression;

  // The percentage of weight to start from, (0,1)
  private final double _minWeightPercent;

  public SlowStartProperties(boolean disabled, int windowDurationSeconds, double aggression, double minWeightPercent) {
    _disabled = disabled;
    _windowDurationSeconds = windowDurationSeconds;
    _aggression = aggression;
    _minWeightPercent = minWeightPercent;
  }

  public boolean isDisabled() {
    return _disabled;
  }

  public int getWindowDurationSeconds() {
    return _windowDurationSeconds;
  }

  public double getAggression() {
    return _aggression;
  }

  public double getMinWeightPercent() {
    return _minWeightPercent;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("disabled", _disabled)
        .add("windowDurationSeconds", _windowDurationSeconds)
        .add("aggression", _aggression)
        .add("minWeightPercent", _minWeightPercent)
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(_disabled, _windowDurationSeconds, _aggression, _minWeightPercent);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    SlowStartProperties oth = (SlowStartProperties) obj;
    return _disabled == oth._disabled
        && _windowDurationSeconds == oth._windowDurationSeconds
        && _aggression == oth._aggression
        && _minWeightPercent == oth._minWeightPercent;
  }
}
