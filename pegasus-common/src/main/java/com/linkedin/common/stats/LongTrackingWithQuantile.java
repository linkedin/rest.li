package com.linkedin.common.stats;

import java.util.Arrays;


public class LongTrackingWithQuantile implements LongTracker
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

  private final LongTracking _longTracking;

  public LongTrackingWithQuantile()
  {
    this(DEFAULT_MAX_CAPACITY, DEFAULT_INITIAL_CAPACITY, DEFAULT_GROWTH_FACTOR);
  }

  public LongTrackingWithQuantile(final int maxCapacity, int initialCapacity, double growthFactor)
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
    _longTracking = new LongTracking();

    reset();
  }

  @Override
  public void reset()
  {
    _longTracking.reset();

    _sortedEnd = 0;
    _nextIndex = 0;
    _keepRatio = 1;
  }

  @Override
  public void addValue(long value)
  {
    _longTracking.addValue(value);

    if (_keepRatio > 1 && (_longTracking.getCount() % _keepRatio) != 0)
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

  @Override
  public LongStats getStats() {
    return new LongStats(_longTracking.getCount(), _longTracking.getAverage(),
        _longTracking.getStandardDeviation(),
        _longTracking.getMinimum(), _longTracking.getMaximum(),
        get50Pct(), get90Pct(), get95Pct(), get99Pct());
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

  public long getPercentile(double pct)
  {
    if (_longTracking.getCount() == 0)
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
}
