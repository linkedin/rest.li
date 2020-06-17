/*
   Copyright (c) 2012 LinkedIn Corp.

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
 * This is a simple implementation of {@link LongTracker}.
 * This implementation will not count percentile latencies in order to save memory.
 */
public class SimpleLongTracker implements LongTracker
{
  private static final long DEFAULT_PERCENTILE_LATENCY = -1L;

  private int                 _count;
  private long                _min;
  private long                _max;
  private long                _sum;
  private long                _sumOfSquares;

  public SimpleLongTracker()
  {
    reset();
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

  @Override
  public void addValue(long value)
  {
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
                         getMinimum(), getMaximum(), DEFAULT_PERCENTILE_LATENCY, DEFAULT_PERCENTILE_LATENCY,
                         DEFAULT_PERCENTILE_LATENCY, DEFAULT_PERCENTILE_LATENCY);
  }

  int getCount()
  {
    return _count;
  }

  double getAverage()
  {
    return safeDivide(_sum, _count);
  }

  double getStandardDeviation()
  {
    double variation;
    variation = safeDivide(_sumOfSquares - _sum * getAverage(), getCount());
    return Math.sqrt(variation);
  }

  long getMinimum()
  {
    return _min;
  }

  long getMaximum()
  {
    return _max;
  }

  static double safeDivide(final double numerator, final double denominator)
  {
    return denominator != 0 ? numerator / denominator : 0;
  }
}

