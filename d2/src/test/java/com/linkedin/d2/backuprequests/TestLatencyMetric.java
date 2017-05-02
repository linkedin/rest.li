/*
   Copyright (c) 2017 LinkedIn Corp.

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
package com.linkedin.d2.backuprequests;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.HdrHistogram.AbstractHistogram;
import org.HdrHistogram.ShortCountsHistogram;
import org.testng.annotations.Test;


public class TestLatencyMetric
{

  @Test
  public void testNoRecording()
  {
    LatencyMetric metric = new LatencyMetric();
    final AtomicLong totalCount = new AtomicLong();
    metric.harvest(h -> totalCount.set(h.getTotalCount()));
    assertEquals(totalCount.get(), 0L);
    metric.harvest(h -> totalCount.set(h.getTotalCount()));
    assertEquals(totalCount.get(), 0L);
  }

  @Test
  public void testShortCountHistorgramOverflow()
  {
    ShortCountsHistogram histogram = new ShortCountsHistogram(LatencyMetric.LOWEST_DISCERNIBLE_VALUE,
        LatencyMetric.HIGHEST_TRACKABLE_VALUE, LatencyMetric.NUMBER_OF_SIGNIFICANT_VALUE_DIGITS);
    for (int i = 0; i < Short.MAX_VALUE; i++)
    {
      histogram.recordValue(1000);
    }
    IllegalStateException expectedException = null;
    try
    {
      histogram.recordValue(1000);
    } catch (IllegalStateException e)
    {
      expectedException = e;
    }
    assertNotNull(expectedException);
  }

  @Test
  public void testRecording()
  {
    GaussianResponseTimeDistribution distribution =
        new GaussianResponseTimeDistribution(0, 100, 10, TimeUnit.MILLISECONDS);
    LatencyMetric metric = new LatencyMetric();
    for (int i = 0; i < Short.MAX_VALUE; i++)
    {
      metric.record(distribution.responseTimeNanos(), null);
    }
    AtomicReference<AbstractHistogram> histogram = new AtomicReference<>();
    metric.harvest(h -> histogram.set(h.copy()));
    assertEquals(histogram.get().getTotalCount(), Short.MAX_VALUE);
    assertEquals(histogram.get().getMean(), 100000000d, 10000000d);
  }

  @Test
  public void testOverflowRecording()
  {
    LatencyMetric metric = new LatencyMetric();
    for (int j = 0; j < 3; j++)
    {
      GaussianResponseTimeDistribution distribution =
          new GaussianResponseTimeDistribution(0, j * 100, 10, TimeUnit.MILLISECONDS);
      AtomicReference<AbstractHistogram> histogram = new AtomicReference<>();
      //record until overflow
      do
      {
        metric.record(distribution.responseTimeNanos(), h -> histogram.set(h.copy()));
      } while (histogram.get() == null);
      assertTrue(histogram.get().getTotalCount() > Short.MAX_VALUE);
      assertEquals(histogram.get().getMean(), j * 100000000d, 10000000d);
    }
  }

  @Test
  public void testReaderDoesNotBlockWriters() throws InterruptedException
  {
    final LatencyMetric metric = new LatencyMetric();
    final CountDownLatch latch1 = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(1);
    Thread t = new Thread(() -> {
      try
      {
        latch1.await(10, TimeUnit.SECONDS);
        //record
        for (int i = 0; i < 1000; i++)
        {
          metric.record(10000000, null);
        }
        latch2.countDown();
      } catch (Exception e)
      {
      }
    });
    t.start();
    metric.harvest(h -> {
      latch1.countDown();
      try
      {
        latch2.await(10, TimeUnit.SECONDS);
      } catch (Exception e)
      {
      }
    });
    AtomicReference<AbstractHistogram> histogram = new AtomicReference<>();
    metric.harvest(h -> histogram.set(h.copy()));
    assertEquals(histogram.get().getTotalCount(), 1000);
  }

}
