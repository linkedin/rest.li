/*
   Copyright (c) 2020 LinkedIn Corp.

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

package com.linkedin.common.stats;

/**
 * Tracks count, average, standard deviation, minimum and maximum
 * in a memory-efficient way.
 *
 * If percentile information is required, use {@link LongTracking}.
 *
 * This class implementation is not synchronized. If concurrent access is required, it
 * must be synchronized externally.
 */
public class SimpleLongTracking implements LongTracker
{
  private int                 _count;
  private long                _min;
  private long                _max;
  private long                _sum;
  private long                _sumOfSquares;                  // Running sum of squares
                                                               // for call times, used for
                                                               // std deviation.

  @Override
  public void addValue(long value) {
    if (_count == 0)
    {
      _min = _max = value;
    }
    else if (value < _min)
    {
      _min = value;
    }
    else if (value > _max)
    {
      _max = value;
    }
    _sum += value;
    _sumOfSquares += value * value;
    _count++;
  }

  @Override
  public LongStats getStats()
  {
    return new LongStats(getCount(), getAverage(), getStandardDeviation(),
        getMinimum(), getMaximum(),
        -1L, -1L, -1L, -1L);
  }

  @Override
  public void reset()
  {
    _count = 0;
    _min = 0;
    _max = 0;
    _sum = 0;
    _sumOfSquares = 0;
  }

  protected int getCount()
  {
    return _count;
  }

  protected double getAverage()
  {
    return safeDivide(_sum, _count);
  }

  protected double getStandardDeviation()
  {
    double variation;
    variation = safeDivide(_sumOfSquares - _sum * getAverage(), getCount());
    return Math.sqrt(variation);
  }

  protected long getMinimum()
  {
    return _min;
  }

  protected long getMaximum()
  {
    return _max;
  }

  private static double safeDivide(final double numerator, final double denominator)
  {
    return denominator != 0 ? numerator / denominator : 0;
  }
}
