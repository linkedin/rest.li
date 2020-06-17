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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


public class TestSimpleLongTracker
{
  SimpleLongTracker _simpleLongTracker;

  @BeforeMethod
  protected void setUp()
  {
    _simpleLongTracker = new SimpleLongTracker();
  }

  @Test(dataProvider = "linearChange")
  public void testLinearChange(long begin, long count, int increaseBy)
  {
    long end   = begin + count;

    long sum = 0;
    long sumSquares = 0;
    for (long i = begin; i < end; i = i + increaseBy)
    {
      _simpleLongTracker.addValue(i);
      sum += i;
      sumSquares += i * i;
    }
    double average = (double) sum / (double) count;
    double variance = (double) sumSquares / (double) count - average * average;
    double stddev = Math.sqrt(variance);

    LongStats stats = _simpleLongTracker.getStats();

    assertEquals(stats.getCount(), count);
    assertEquals(stats.getAverage(), average);
    assertEquals(stats.getStandardDeviation(), stddev, 0.0001);
    assertEquals(stats.getMinimum(), begin);
    assertEquals(stats.getMaximum(), end - 1);

    assertEquals(stats.get50Pct(), -1L);
    assertEquals(stats.get90Pct(), -1L);
    assertEquals(stats.get95Pct(), -1L);
    assertEquals(stats.get99Pct(), -1L);
  }

  @DataProvider(name = "linearChange")
  Object[][] getLinearChange()
  {
    return new Object[][]
        {
            {1000000, 1000000, 1},
            {2000000, 1000000, -1}
        };
  }

  @Test public void testRandom()
  {
    long begin = 1000000;
    long count = 2000000;

    long sum = 0;
    long sumSquares = 0;
    long min = 0;
    long max = 0;
    for (long i = 0; i < count; ++i)
    {
      long value = (long) (Math.random() * count) + begin;
      _simpleLongTracker.addValue(value);
      sum += value;
      sumSquares += value * value;
      if (i == 0)
      {
        min = max = value;
      }
      else
      {
        min = Math.min(min, value);
        max = Math.max(max, value);
      }
    }
    double average = (double) sum / (double) count;
    double variance = (double) sumSquares / (double) count - average * average;
    double stddev = Math.sqrt(variance);

    LongStats stats = _simpleLongTracker.getStats();

    assertEquals(stats.getCount(), count);
    assertEquals(stats.getAverage(), average);
    assertEquals(stats.getStandardDeviation(), stddev, 0.0001);
    assertEquals(stats.getMinimum(), min);
    assertEquals(stats.getMaximum(), max);
  }

  @Test public void testConstant()
  {
    long value = 1000000;
    long count = 2000000;

    for (long i = 0; i < count; ++i)
    {
      _simpleLongTracker.addValue(value);
    }

    LongStats stats = _simpleLongTracker.getStats();

    assertEquals(stats.getCount(), count);
    assertEquals(stats.getAverage(), (double) value);
    assertEquals(stats.getStandardDeviation(), 0.0);
    assertEquals(stats.getMinimum(), value);
    assertEquals(stats.getMaximum(), value);
  }
}
