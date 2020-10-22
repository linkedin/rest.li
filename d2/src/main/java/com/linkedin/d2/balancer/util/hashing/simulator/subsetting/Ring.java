package com.linkedin.d2.balancer.util.hashing.simulator.subsetting;

import java.util.ArrayList;
import java.util.List;


public class Ring {
  private final List<Double> slices;

  private final int numberOfPoints;
  private final double unitWidth;

  public Ring(int numberOfPoints)
  {
    this.numberOfPoints = numberOfPoints;
    unitWidth = 1.0 / numberOfPoints;
    slices = new ArrayList<>();

    for (int i = 0; i < numberOfPoints; i++)
    {
      slices.add(unitWidth * i);
    }
  }


}
