package com.linkedin.d2.balancer.util.hashing.simulator.subsetting.deterministic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class DeterministicAperture
{
  private final Ring _ring;
  private final Coordinate _coordinate;
  private final Map<Integer, Double> _trackerClientsSubset;

  public DeterministicAperture(List<Integer> trackerClients, Coordinate coordinate)
  {
    _ring = new Ring(trackerClients.size());
    _coordinate = coordinate;
    double apertureWidth = apertureWidth(coordinate.getUnitWidth(), _ring.getUnitWidth(), 10);
    _trackerClientsSubset = calculateSubset(trackerClients, coordinate.getOffset(), apertureWidth);
  }

  private Map<Integer, Double> calculateSubset(List<Integer> trackerClients, double offset, double apertureWidth)
  {
    List<Integer> indices = _ring.getIndices(offset, apertureWidth);

    return indices.stream()
        .collect(Collectors.toMap(trackerClients::get,
                                  i -> (double) Math.round(_ring.getWeight(i, offset, apertureWidth) * 1000) / 1000));
  }

  public Map<Integer, Double> getTrackerClientsSubset() {
    return _trackerClientsSubset;
  }

  private static double apertureWidth(double clientUnitWidth, double hostUnitWidth, int defaultApertureSize)
  {
    int adjustedApertureSize = (int) Math.ceil(defaultApertureSize * hostUnitWidth / clientUnitWidth);
    double adjustedApertureWidth = adjustedApertureSize * clientUnitWidth;

    return Double.min(1, adjustedApertureWidth);
  }

  public static class Ring
  {
    private static final double EPSILON = 1e-5;

    private final int _numberOfPoints;
    private final double _unitWidth;

    public Ring(int numberOfPoints)
    {
      assert(numberOfPoints > 0);
      _numberOfPoints = numberOfPoints;
      _unitWidth = 1.0 / numberOfPoints;
    }

    public int getIndex(double offset)
    {
      return (int) Math.floor(offset * _numberOfPoints) % _numberOfPoints;
    }

    private static boolean isEqual(double a, double b, double epsilon)
    {
      return Math.abs(a - b) <= epsilon;
    }

    public int getRange(double offset, double width)
    {
      assert(width <= 1.0);

      if (width == 1.0)
      {
        return _numberOfPoints;
      }
      else
      {
        int begin = getIndex(offset);
        int end = getIndex((offset + width) % 1.0);

        if (begin == end)
        {
          if (width > _unitWidth)
          {
            return _numberOfPoints;
          }
          else
          {
            return 1;
          }
        }
        else
        {
          double beginWeight = getWeight(begin, offset, width);
          double endWeight = getWeight(end, offset, width);

          int adjustedBegin = isEqual(beginWeight, 0, EPSILON) ? begin + 1 : begin;
          int adjustedEnd = isEqual(endWeight, 0, EPSILON) ? end : end + 1;

          int diff = adjustedEnd - adjustedBegin;
          if (diff <= 0)
          {
            return diff + _numberOfPoints;
          }
          else
          {
            return diff;
          }
        }
      }
    }

    public double getWeight(int index, double offset, double width)
    {
      assert(index < _numberOfPoints);
      assert(width >= 0 && width <= 1);

      double ringSegmentStart = index * _unitWidth;
      ringSegmentStart = (ringSegmentStart + 1 < offset + width) ? (ringSegmentStart + 1) : ringSegmentStart;

      return intersect(ringSegmentStart, ringSegmentStart + _unitWidth, offset, offset + width) / _unitWidth;
    }

    public List<Integer> getIndices(double offset, double width)
    {
      List<Integer> indices = new ArrayList<>();
      int begin = getIndex(offset);
      int range = getRange(offset, width);

      while (range > 0)
      {
        int index = begin % _numberOfPoints;
        indices.add(index);
        begin += 1;
        range -= 1;
      }

      return indices;
    }

    private double intersect(double start0, double end0, double start1, double end1)
    {
      double length = Double.min(end0, end1) - Double.max(start0, start1);
      return Double.max(0, length);
    }

    public double getUnitWidth()
    {
      return _unitWidth;
    }

    public int getNumberOfPoints()
    {
      return _numberOfPoints;
    }
  }

}
