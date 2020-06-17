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

/**
 * $Id: LongTracking.java 151859 2010-11-19 21:43:47Z slim $
 */
package com.linkedin.common.stats;

import java.util.Arrays;

import com.linkedin.common.util.ConfigHelper;


/**
 * @author Swee Lim
 * @version $Rev: 151859 $
 */

/**
 * Maintain a collection of values and provide the count, average, standard deviation,
 * minimum, maximum, percentile values for the collection.
 *
 * This class implementation is not synchronized. If concurrent access is required, it
 * must be synchronized externally.
 */
public class LongTracking extends SimpleLongTracker implements LongTracker
{
  private static final int    DEFAULT_INITIAL_CAPACITY = 1000;
  private static final double DEFAULT_GROWTH_FACTOR    = 2.0;
  private static final int    DEFAULT_MAX_CAPACITY     = 4000;

  private long[]              _buffer;
  private int                 _bufferSize;
  private final int           _initialCapacity;
  private final double        _growthFactor;
  private final int           _maxCapacity;

  private int                 _sortedEnd;
  private int                 _nextIndex;
  private int                 _keepRatio;

  public LongTracking()
  {
    this(DEFAULT_MAX_CAPACITY, DEFAULT_INITIAL_CAPACITY, DEFAULT_GROWTH_FACTOR);
  }

  public LongTracking(final Config config)
  {
    this(config.getMaxCapacity(), config.getInitialCapacity(), config.getGrowthFactor());
  }

  public LongTracking(final int maxCapacity, int initialCapacity, double growthFactor)
  {
    if (initialCapacity > maxCapacity || initialCapacity <= 0)
    {
      initialCapacity = maxCapacity;
    }
    if (growthFactor <= 1.0)
    {
      growthFactor = DEFAULT_GROWTH_FACTOR;
    }
    _buffer = new long[initialCapacity];
    _bufferSize = initialCapacity;
    _initialCapacity = initialCapacity;
    _growthFactor = growthFactor;
    _maxCapacity = maxCapacity;
    reset();
  }

  @Override
  public void reset()
  {
    super.reset();

    _sortedEnd = 0;
    _nextIndex = 0;
    _keepRatio = 1;
  }

  @Override
  public void addValue(long value)
  {
    super.addValue(value);

    if (_keepRatio > 1 && (getCount() % _keepRatio) != 0)
    {
      return;
    }

    if (_nextIndex >= _bufferSize)
    {
      if (_bufferSize < _maxCapacity)
      {
        grow();
      }
      else
      {
        dropHalf();
      }
    }
    _buffer[_nextIndex] = value;
    _nextIndex++;
  }

  public int getBufferSize()
  {
    return _bufferSize;
  }

  public int getInitialCapacity()
  {
    return _initialCapacity;
  }

  public double getGrowthFactor()
  {
    return _growthFactor;
  }

  public int getMaxCapacity()
  {
    return _maxCapacity;
  }

  @Override
  public LongStats getStats()
  {
    return new LongStats(getCount(), getAverage(), getStandardDeviation(),
                         getMinimum(), getMaximum(),
                         get50Pct(), get90Pct(), get95Pct(), get99Pct());
  }

  private long get50Pct()
  {
    return getPercentile(0.50);
  }

  private long get90Pct()
  {
    return getPercentile(0.90);
  }

  private long get95Pct()
  {
    return getPercentile(0.95);
  }

  private long get99Pct()
  {
    return getPercentile(0.99);
  }

  private long getPercentile(double pct)
  {
    if (getCount() == 0)
    {
      return 0;
    }
    if (_sortedEnd < _nextIndex)
    {
      Arrays.sort(_buffer, 0, _nextIndex);
      _sortedEnd = _nextIndex;
    }
    if (pct < 0.0)
    {
      pct = 0;
    }
    else if (pct > 1.0)
    {
      pct = 1.0;
    }
    int index = (int) Math.round(pct * (_sortedEnd - 1));
    return _buffer[index];
  }

  private void dropHalf()
  {
    _nextIndex = (_nextIndex + 1) / 2;
    _sortedEnd = (_sortedEnd + 1) / 2;
    _keepRatio += _keepRatio;

    int destIndex = 1;
    int lastSrcIndex = 0;
    // copy half the sorted values by skipping alternate
    while (destIndex < _nextIndex)
    {
      _buffer[destIndex++] = _buffer[lastSrcIndex += 2];
    }
  }

  private void grow()
  {
    int newBufferSize = (int) (_bufferSize * _growthFactor);
    if (newBufferSize == _bufferSize)
    {
      newBufferSize += DEFAULT_INITIAL_CAPACITY;
    }
    if (newBufferSize > _maxCapacity)
    {
      newBufferSize = _maxCapacity;
    }

    long[] newBuffer = new long[newBufferSize];
    System.arraycopy(_buffer, 0, newBuffer, 0, _nextIndex);
    _buffer = newBuffer;
    _bufferSize = newBufferSize;
  }

  public static class Config
  {
    private Integer _maxCapacity = DEFAULT_MAX_CAPACITY;
    private Integer _initialCapacity = DEFAULT_INITIAL_CAPACITY;
    private Double _growthFactor = DEFAULT_GROWTH_FACTOR;

    Config()
    {
    }

    public int getMaxCapacity()
    {
      return ConfigHelper.getRequired(_maxCapacity);
    }

    public void setMaxCapacity(final int maxCapacity)
    {
      _maxCapacity = maxCapacity;
    }

    public int getInitialCapacity()
    {
      return ConfigHelper.getRequired(_initialCapacity);
    }

    public void setInitialCapacity(final int initialCapacity)
    {
      _initialCapacity = initialCapacity;
    }

    public double getGrowthFactor()
    {
      return ConfigHelper.getRequired(_growthFactor);
    }

    public void setGrowthFactor(final double growthFactor)
    {
      _growthFactor = growthFactor;
    }
  }
}

