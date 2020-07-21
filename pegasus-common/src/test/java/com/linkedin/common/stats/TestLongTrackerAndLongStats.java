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
 * $Id: TestLongTrackingAndLongStats.java 151859 2010-11-19 21:43:47Z slim $
 */
package com.linkedin.common.stats;

import static org.testng.Assert.assertEquals;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Swee Lim
 * @version $Rev: 151859 $
 */


public class TestLongTrackerAndLongStats
{
  LongTracking _tracking;
  LongTrackingWithQuantile _trackingWithQuantile;

  @BeforeMethod
  protected void setUp() throws Exception
  {
    _tracking = new LongTracking();
    _trackingWithQuantile = new LongTrackingWithQuantile();
  }

  @Test
  public void testIncreasingLinearly()
  {
    long begin = 1000000;
    long count = 1000000;
    long end   = begin + count;

    long sum = 0;
    long sumSquares = 0;
    for (long i = begin; i < end; ++i)
    {
      _tracking.addValue(i);
      _trackingWithQuantile.addValue(i);
      sum += i;
      sumSquares += i * i;
    }
    double average = (double) sum / (double) count;
    double variance = (double) sumSquares / (double) count - average * average;
    double stddev = Math.sqrt(variance);

    LongStats stats = _tracking.getStats();
    LongStats statsWithQuantile = _trackingWithQuantile.getStats();

    assertEquals(stats.getCount(), count, "Count is incorrect");
    assertEquals(average, stats.getAverage(), 0.0001, "Average is incorrect");
    assertEquals(stddev, stats.getStandardDeviation(), 0.0001, "Standard deviation is incorrect");
    assertEquals(stats.getMinimum(), begin, "Minimum is incorrect");
    assertEquals(stats.getMaximum(), end - 1, "Maximum is incorrect");

    assertEquals(statsWithQuantile.getCount(), count, "Count is incorrect");
    assertEquals(average, statsWithQuantile.getAverage(), 0.0001, "Average is incorrect");
    assertEquals(stddev, statsWithQuantile.getStandardDeviation(), 0.0001, "Standard deviation is incorrect");
    assertEquals(statsWithQuantile.getMinimum(), begin, "Minimum is incorrect");
    assertEquals(statsWithQuantile.getMaximum(), end - 1, "Maximum is incorrect");

    assertEquals(begin + count * 0.50, statsWithQuantile.get50Pct(), 1000.0, "50 percentile is incorrect");
    assertEquals(begin + count * 0.90, statsWithQuantile.get90Pct(), 1000.0, "90 percentile is incorrect");
    assertEquals(begin + count * 0.95, statsWithQuantile.get95Pct(), 1000.0, "95 percentile is incorrect");
    assertEquals(begin + count * 0.99, statsWithQuantile.get99Pct(), 1000.0, "99 percentile is incorrect");
  }

  @Test public void testDecreasingLinearly()
  {
    long begin = 2000000;
    long count = 1000000;
    long end   = begin - count;

    long sum = 0;
    long sumSquares = 0;
    for (long i = begin; i > end; --i)
    {
      _tracking.addValue(i);
      _trackingWithQuantile.addValue(i);
      sum += i;
      sumSquares += i * i;
    }
    double average = (double) sum / (double) count;
    double variance = (double) sumSquares / (double) count - average * average;
    double stddev = Math.sqrt(variance);

    LongStats stats = _tracking.getStats();
    LongStats statsWithQuantile = _trackingWithQuantile.getStats();

    assertEquals(stats.getCount(), count, "Count is incorrect");
    assertEquals(average, stats.getAverage(), 0.0001, "Average is incorrect");
    assertEquals(stddev, stats.getStandardDeviation(), 0.0001, "Standard deviation is incorrect");
    assertEquals(stats.getMinimum(), end + 1, "Minimum is incorrect");
    assertEquals(stats.getMaximum(), begin, "Maximum is incorrect");

    assertEquals(statsWithQuantile.getCount(), count, "Count is incorrect");
    assertEquals(average, statsWithQuantile.getAverage(), 0.0001, "Average is incorrect");
    assertEquals(stddev, statsWithQuantile.getStandardDeviation(), 0.0001, "Standard deviation is incorrect");
    assertEquals(statsWithQuantile.getMinimum(), end + 1, "Minimum is incorrect");
    assertEquals(statsWithQuantile.getMaximum(), begin, "Maximum is incorrect");

    assertEquals(end + count * 0.50, statsWithQuantile.get50Pct(), 1000.0, "50 percentile is incorrect");
    assertEquals(end + count * 0.90, statsWithQuantile.get90Pct(), 1000.0, "90 percentile is incorrect");
    assertEquals(end + count * 0.95, statsWithQuantile.get95Pct(), 1000.0, "95 percentile is incorrect");
    assertEquals(end + count * 0.99, statsWithQuantile.get99Pct(), 1000.0, "99 percentile is incorrect");
  }

  @Test public void testRandom()
  {
    long begin = 1000000;
    long count = 2000000;
    double tolerance = 0.05 * count;

    long sum = 0;
    long sumSquares = 0;
    long min = 0;
    long max = 0;
    for (long i = 0; i < count; ++i)
    {
      long value = (long) (Math.random() * count) + begin;
      _tracking.addValue(value);
      _trackingWithQuantile.addValue(value);
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

    LongStats stats = _tracking.getStats();
    LongStats statsWithQuantile = _trackingWithQuantile.getStats();

    assertEquals(stats.getCount(), count, "Count is incorrect");
    assertEquals(average, stats.getAverage(), 0.0001, "Average is incorrect");
    assertEquals(stddev, stats.getStandardDeviation(), 0.0001, "Standard deviation is incorrect");
    assertEquals(stats.getMinimum(), min, "Minimum is incorrect");
    assertEquals(stats.getMaximum(), max, "Maximum is incorrect");

    assertEquals(statsWithQuantile.getCount(), count, "Count is incorrect");
    assertEquals(average, statsWithQuantile.getAverage(), 0.0001, "Average is incorrect");
    assertEquals(stddev, statsWithQuantile.getStandardDeviation(), 0.0001, "Standard deviation is incorrect");
    assertEquals(statsWithQuantile.getMinimum(), min, "Minimum is incorrect");
    assertEquals(statsWithQuantile.getMaximum(), max, "Maximum is incorrect");

    assertEquals(begin + count * 0.50, statsWithQuantile.get50Pct(), tolerance, "50 percentile is incorrect");
    assertEquals(begin + count * 0.90, statsWithQuantile.get90Pct(), tolerance, "90 percentile is incorrect");
    assertEquals(begin + count * 0.95, statsWithQuantile.get95Pct(), tolerance, "95 percentile is incorrect");
    assertEquals(begin + count * 0.99, statsWithQuantile.get99Pct(), tolerance, "99 percentile is incorrect");
  }

  @Test public void testConstant()
  {
    long value = 1000000;
    long count = 2000000;

    long sum = 0;
    long sumSquares = 0;
    for (long i = 0; i < count; ++i)
    {
      _tracking.addValue(value);
      _trackingWithQuantile.addValue(value);
      sum += value;
      sumSquares += value * value;
    }
    double average = (double) sum / (double) count;
    double variance = (double) sumSquares / (double) count - average * average;
    double stddev = Math.sqrt(variance);

    LongStats stats = _tracking.getStats();
    LongStats statsWithQuantile = _trackingWithQuantile.getStats();

    assertEquals(stats.getCount(), count, "Count is incorrect");
    assertEquals(value, stats.getAverage(), 0.0001, "Average is incorrect");
    assertEquals(0, stats.getStandardDeviation(), 0.0001, "Standard deviation is incorrect");
    assertEquals(stats.getMinimum(), value, "Minimum is incorrect");
    assertEquals(stats.getMaximum(), value, "Maximum is incorrect");

    assertEquals(statsWithQuantile.getCount(), count, "Count is incorrect");
    assertEquals(value, statsWithQuantile.getAverage(), 0.0001, "Average is incorrect");
    assertEquals(0, statsWithQuantile.getStandardDeviation(), 0.0001, "Standard deviation is incorrect");
    assertEquals(statsWithQuantile.getMinimum(), value, "Minimum is incorrect");
    assertEquals(statsWithQuantile.getMaximum(), value, "Maximum is incorrect");

    assertEquals(statsWithQuantile.get50Pct(), value, "50 percentile is incorrect");
    assertEquals(statsWithQuantile.get90Pct(), value, "90 percentile is incorrect");
    assertEquals(statsWithQuantile.get95Pct(), value, "95 percentile is incorrect");
    assertEquals(statsWithQuantile.get99Pct(), value, "99 percentile is incorrect");
  }

  @Test public void testPerformance()
  {
    final int numInstances = 1000;
    LongTrackingWithQuantile[] instances = new LongTrackingWithQuantile[numInstances];
    for (int i = 0; i < numInstances; ++i)
    {
      instances[i] = new LongTrackingWithQuantile();
      for (int j = 0; j < instances[i].getMaxCapacity(); ++j)
      {
        long value = (long) (Math.random() * 10000);
        instances[i].addValue(value);
      }
    }

    long startTime = System.currentTimeMillis();
    for (int i = 0; i < numInstances; ++i)
    {
      instances[i].getStats();
    }
    long endTime = System.currentTimeMillis();
    double secs = (endTime - startTime) / 1000.0;
    double sortsPerSecond = numInstances / secs;
    double avgLatencyMillis = 1.0 / sortsPerSecond * 1000.0;

    System.out.println("Sorted " + numInstances + " with "
        + instances[0].getMaxCapacity() + " values in " + secs + " seconds, "
        + sortsPerSecond + " sorts/second, latency " + avgLatencyMillis + " milliseconds");

  }

  @Test
  public void testNoOpLongTracker()
  {
    NoopLongTracker tracker = NoopLongTracker.instance();
    tracker.reset();
    tracker.addValue(42L);
    LongStats stats = tracker.getStats();
    assertEquals(stats.get50Pct(), 0L);
    assertEquals(stats.get90Pct(), 0L);
    assertEquals(stats.get95Pct(), 0L);
    assertEquals(stats.get99Pct(), 0L);
    assertEquals(stats.getAverage(), 0.0D);
    assertEquals(stats.getCount(), 0);
    assertEquals(stats.getMaximum(), 0L);
    assertEquals(stats.getMinimum(), 0L);
    assertEquals(stats.getStandardDeviation(), 0.0D);


  }
}
