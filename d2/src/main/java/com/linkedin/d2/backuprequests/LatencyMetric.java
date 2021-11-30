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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.HdrHistogram.AbstractHistogram;
import org.HdrHistogram.ShortCountsHistogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a latency metric such that it has a small memory footprint,
 * writes don't block reads. Since writes are very fast spinning was used as a concurrency
 * control mechanism.
 * <p>
 * This class is thread safe.
 *
 * @author Jaroslaw Odzga (jodzga@linkedin.com)
 *
 */
public class LatencyMetric {

  private static final Logger LOG = LoggerFactory.getLogger(LatencyMetric.class);

  //ShortCountsHistogram takes ~40K with the parameters below
  public static final long LOWEST_DISCERNIBLE_VALUE = TimeUnit.MICROSECONDS.toNanos(1);
  public static final long HIGHEST_TRACKABLE_VALUE = TimeUnit.SECONDS.toNanos(100);
  public static final int NUMBER_OF_SIGNIFICANT_VALUE_DIGITS = 3;

  /*
   * Writes to histogram are very fast while reads (e.g. serializing entire histogram) can be
   * much slower. This is why we implement metric in such a way that reads does not block writes.
   * In order to achieve it we have two histograms: current and inactive. All writes are done to
   * current histogram. Inactive histogram is always empty. Read happens in the following way:
   * - current histogram is stored aside and replaced with inactive, all writes can immediately
   *   continue to now empty current
   * - read happens on previously put aside current, once read is completed, the histogram is reset
   *   and put back as inactive
   */
  private AtomicReference<AbstractHistogram> _current = new AtomicReference<>(
      new ShortCountsHistogram(LOWEST_DISCERNIBLE_VALUE, HIGHEST_TRACKABLE_VALUE, NUMBER_OF_SIGNIFICANT_VALUE_DIGITS));

  private AtomicReference<AbstractHistogram> _inactive = new AtomicReference<>(
      new ShortCountsHistogram(LOWEST_DISCERNIBLE_VALUE, HIGHEST_TRACKABLE_VALUE, NUMBER_OF_SIGNIFICANT_VALUE_DIGITS));

  /**
   * Records a latency. If histogram overflows the passed in overflownConsumer will be called.
   * Reference to the histogram should not be cached in any way because it is reused for performance reasons.
   * Histogram passed to the consumer includes stable, consistent view of all values accumulated since last
   * harvest or overflow.
   * <p>
   * This method is thread safe.
   * @param latencyNano
   * @param overflownConsumer
   */
  public void record(long latencyNano, Consumer<AbstractHistogram> overflownConsumer) {
    recordSafeValue(narrow(latencyNano), overflownConsumer);
  }

  /**
   * Make sure that recorded value is within a supported range.
   */
  private long narrow(long latencyNano) {
    if (latencyNano < LOWEST_DISCERNIBLE_VALUE) {
      return LOWEST_DISCERNIBLE_VALUE;
    }
    if (latencyNano > HIGHEST_TRACKABLE_VALUE) {
      return HIGHEST_TRACKABLE_VALUE;
    }
    return latencyNano;
  }

  private static <T> T claim(AtomicReference<T> ref) {
    T current;
    do {
      current = ref.get();
    } while (current == null || !ref.compareAndSet(current, null));
    return current;
  }

  private void recordSafeValue(long latencyNano, Consumer<AbstractHistogram> overflownConsumer) {
    AbstractHistogram current = claim(_current);
    try {
      current.recordValue(latencyNano);
      _current.set(current);
    } catch (IllegalStateException e) {
      //overflow handling
      AbstractHistogram inactive = claim(_inactive);
      inactive.recordValue(latencyNano);
      _current.set(inactive);  //unblock other writers
      try {
        overflownConsumer.accept(current);
      } catch (Throwable t) {
        LOG.error("failed to consume overflown histogram for latencies metric", t);
      } finally {
        current.reset();
        _inactive.set(current);
      }
    }
  }

  /**
   * Allows consuming histogram. Reference to the histogram should not be cached in any way because it is
   * reused for performance reasons.
   * Histogram passed to the consumer includes stable, consistent view
   * of all values accumulated since last harvest or overflow.
   * This method is thread safe.
   * @param consumer consumer for a harvested histogram
   */
  public void harvest(Consumer<AbstractHistogram> consumer) {
    AbstractHistogram current = claim(_current);
    _current.set(claim(_inactive)); //unblock other writers
    try {
      consumer.accept(current);
    } catch (Throwable t) {
      LOG.error("failed to consume histogram for latencies metric", t);
    } finally {
      current.reset();
      _inactive.set(current);
    }
  }

}
