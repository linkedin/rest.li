package com.linkedin.d2.balancer.util.hashing.simulator.subsetting.deterministic;

public class Coordinate
{
  private final double _offset;
  private final double _unitWidth;

  public Coordinate(double offset, double unitWidth)
  {
    _offset = offset;
    _unitWidth = unitWidth;
  }

  public double getOffset()
  {
    return _offset;
  }

  public double getUnitWidth()
  {
    return _unitWidth;
  }

  public static Coordinate fromInstaceId(int instanceId, int totalInstances)
  {
    double unitWidth = 1.0 / totalInstances;
    double offset = (instanceId * unitWidth) % 1.0;
    return new Coordinate(offset, unitWidth);
  }
}
