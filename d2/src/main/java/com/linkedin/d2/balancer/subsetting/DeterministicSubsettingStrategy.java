/*
   Copyright (c) 2021 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.linkedin.d2.balancer.subsetting;

import com.linkedin.d2.balancer.LoadBalancerState;
import com.linkedin.d2.balancer.util.hashing.MD5Hash;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import javax.annotation.concurrent.GuardedBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This strategy picks a subset from a collection of items with deterministic assignment based
 * on the following article:
 * <a href="https://blog.twitter.com/engineering/en_us/topics/infrastructure/2019/daperture-load-balancer.html">
 *   Deterministic Aperture: A distributed, load balancing algorithm</a>
 *
 * The items are placed on a destination ring in a distance proportional to their weights. Each client is also
 * placed on a peer ring in equidistant intervals. Overlaying the destination ring and the peer ring, each client
 * will select a subset of items in an order defined by walking the ring clockwise. The overlap can be fractional
 * to ensure that the overload item distribution is fair.
 */
public class DeterministicSubsettingStrategy<T extends Comparable<T>> implements SubsettingStrategy<T>
{
  public static final int WEIGHT_DECIMAL_PLACE = 5;
  private final Logger _log = LoggerFactory.getLogger(DeterministicSubsettingStrategy.class);

  private final DeterministicSubsettingMetadataProvider _metadataProvider;
  private final long _randomSeed;
  private final int _minSubsetSize;
  private final LoadBalancerState _state;

  private final Object _lock = new Object();

  @GuardedBy("_lock")
  private DeterministicSubsettingMetadata _currentMetadata;
  @GuardedBy("_lock")
  private long _currentVersion = -1;
  @GuardedBy("_lock")
  private Map<T, Double> _currentWeightedSubset;

  /**
   * Builds deterministic subsetting strategy
   *
   * @param metadataProvider  Provides peer cluster metadata information
   * @param clusterName The name of the peer cluster
   * @param minSubsetSize The minimum subset size to satisfy
   */
  public DeterministicSubsettingStrategy(DeterministicSubsettingMetadataProvider metadataProvider,
                                         String clusterName,
                                         int minSubsetSize,
                                         LoadBalancerState state)
  {
    _metadataProvider = metadataProvider;
    MD5Hash hashFunction = new MD5Hash();
    String[] keyTokens = {clusterName};
    _randomSeed = hashFunction.hashLong(keyTokens);
    _minSubsetSize = minSubsetSize;
    _state = state;
  }

  @Override
  public boolean isSubsetChanged(long version)
  {
    DeterministicSubsettingMetadata metadata = _metadataProvider.getSubsettingMetadata(_state);
    return metadata == null || !metadata.equals(_currentMetadata) || version != _currentVersion;
  }

  @Override
  public Map<T, Double> getWeightedSubset(Map<T, Double> weightMap, long version)
  {
    DeterministicSubsettingMetadata metadata = _metadataProvider.getSubsettingMetadata(_state);
    if (metadata != null)
    {
      synchronized (_lock)
      {
        if (metadata.equals(_currentMetadata) && version == _currentVersion)
        {
          return _currentWeightedSubset;
        }

        _currentVersion = version;
        _currentMetadata = metadata;

        List<T> points = new ArrayList<>(weightMap.keySet());
        Collections.sort(points);
        Collections.shuffle(points, new Random(_randomSeed));
        List<Double> weights = points.stream().map(weightMap::get).collect(Collectors.toList());
        Ring ring = new Ring(weights);

        double offset = metadata.getInstanceId() / (double) metadata.getTotalInstanceCount();
        double subsetSliceWidth = getSubsetSliceWidth(metadata.getTotalInstanceCount(), points.size());
        List<Integer> indices = ring.getIndices(offset, subsetSliceWidth);

        _currentWeightedSubset = indices.stream().collect(
            Collectors.toMap(points::get, i -> round(ring.getWeight(i, offset, subsetSliceWidth), WEIGHT_DECIMAL_PLACE)));
      }
      return _currentWeightedSubset;
    }
    else
    {
      _log.warn("Cannot retrieve metadata required for D2 subsetting. Revert to use all available hosts.");
      return null;
    }
  }

  private static double round(double value, int places)
  {
    BigDecimal bd = new BigDecimal(Double.toString(value));
    bd = bd.setScale(places, RoundingMode.HALF_UP);
    return bd.doubleValue();
  }

  private static boolean isEqual(double a, double b, double delta)
  {
    return Math.abs(a - b) <= delta;
  }

  private double getSubsetSliceWidth(int totalClientCount, int totalHostCount)
  {
    double clientUnitWidth = 1.0 / totalClientCount;
    double hostUnitWidth = 1.0 / totalHostCount;

    // Adjust the subset slice width as a multiple of client's unit width
    double adjustedSubsetSliceWidth = (int) Math.ceil(_minSubsetSize * hostUnitWidth / clientUnitWidth) * clientUnitWidth;

    return Double.min(1, adjustedSubsetSliceWidth);
  }

  private static class Ring
  {
    private static final double DELTA = 1e-5;

    private final int _totalPoints;
    private final List<Double> _weights;
    private final double _totalWeight;

    Ring(List<Double> weights)
    {
      _weights = weights;
      _totalPoints = weights.size();
      _totalWeight = weights.stream().mapToDouble(Double::doubleValue).sum();
    }

    /**
     * Get the indices of the slices that intersect with [offset, offset + width)
     */
    public List<Integer> getIndices(double offset, double width)
    {
      List<Integer> indices = new ArrayList<>();
      int begin = getIndex(offset);
      int range = getRange(offset, width);

      while (range > 0)
      {
        int index = begin % _totalPoints;
        indices.add(index);
        begin += 1;
        range -= 1;
      }

      return indices;
    }

    /**
     * Get the fractional width (0.0 - 1.0) of the slice at given index
     */
    private double getUnitWidth(int index)
    {
      return _weights.get(index) / _totalWeight;
    }

    /**
     * Get the total fractional width (0.0 - 1.0) from the slice at index 0 to the slice at given index
     */
    private double getWidthUntil(int index)
    {
      double weightsSum = 0;

      for (int i = 0; i < index; i++)
      {
        weightsSum += _weights.get(i);
      }

      return weightsSum / _totalWeight;
    }

    /**
     * Get the index of the slice at given offset (0.0 - 1.0)
     */
    public int getIndex(double offset)
    {
      double length = 0;
      int index = 0;
      while (index < _totalPoints)
      {
        length += getUnitWidth(index);
        // At slice boundary, return the next index
        if (isEqual(length, offset, Ring.DELTA))
        {
          return (index + 1) % _totalPoints;
        }
        else if (length > offset)
        {
          return index;
        }
        index++;
      }
      return 0;
    }

    /**
     * Get the number of slices included from offset to (offset + width)
     */
    private int getRange(double offset, double width)
    {
      if (width == 1.0)
      {
        return _totalPoints;
      }
      else
      {
        int begin = getIndex(offset);
        int end = getIndex((offset + width) % 1.0);

        if (begin == end)
        {
          // Wrap around the entire ring, so return all the points
          if (width > getUnitWidth(begin))
          {
            return _totalPoints;
          }
          // Only one index is included
          else
          {
            return 1;
          }
        }
        else
        {
          // If the slice at index end does not overlap with [offset, offset + width), it should not be included
          int adjustedEnd = isEqual(getWeight(end, offset, width), 0, Ring.DELTA) ? end : end + 1;
          int diff = adjustedEnd - begin;
          return diff <= 0 ? diff + _totalPoints : diff;
        }
      }
    }

    /**
     * Get the ratio of the intersection between slice at the given index and [offset, offset + width)
     */
    private double getWeight(int index, double offset, double width)
    {
      double unitWidth = getUnitWidth(index);
      if (unitWidth == 0.0)
      {
        return 0.0;
      }

      double ringSegmentStart = getWidthUntil(index);
      double ringSegmentEnd = ringSegmentStart + unitWidth;

      // If cases where [offset, offset + width) wraps around the ring, we take the compliment of it to
      // calculate the inverse intersection ratio, and subtract that from 1 to get the actual ratio
      if (offset + width > 1.0)
      {
        double start = (offset + width) % 1.0;
        double end = offset;
        return 1D - (intersect(ringSegmentStart, ringSegmentEnd, start, end) / unitWidth);
      }
      else
      {
        return intersect(ringSegmentStart, ringSegmentEnd, offset, offset + width) / unitWidth;
      }
    }

    /**
     * Get the fractional width (0.0 - 1.0) where [start0, end0) and [start1, end1) overlaps
     */
    private double intersect(double start0, double end0, double start1, double end1)
    {
      double length = Double.min(end0, end1) - Double.max(start0, start1);
      return Double.min(Double.max(0, length), 1.0);
    }
  }
}
