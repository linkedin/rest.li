/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.r2.message.timing;

import com.linkedin.r2.message.RequestContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class offers methods to manage timings for a request, which provides the capability to record latencies for
 * some specific phases during processing requests.
 *
 * @see RequestContext
 * @author Xialin Zhu
 */
public class TimingContextUtil
{
  private static final Logger LOG = LoggerFactory.getLogger(TimingContextUtil.class);

  public static final String TIMINGS_KEY_NAME = "timings";

  /**
   * Looks for all timing records in the RequestContext, initiate one if not present.
   * @param context RequestContext for the request
   * @return URI for target service hint, or null if no hint is present in the RequestContext
   */
  @SuppressWarnings("unchecked")
  public static Map<TimingKey, TimingContext> getTimingsMap(RequestContext context)
  {
    Map<TimingKey, TimingContext> timings = (Map<TimingKey, TimingContext>)context.getLocalAttr(TIMINGS_KEY_NAME);
    if (timings == null)
    {
      timings = new ConcurrentHashMap<>();
      context.putLocalAttr(TIMINGS_KEY_NAME, timings);
    }

    return timings;
  }

  /**
   * Mark a timing event and record it to the request context.
   * If it's the first time this {@link TimingKey} appears, a new timing records will be created. Current time
   * will be used as the starting time of this record.
   * If it's the second time, the existing timing records will be updated with the duration, the amount of time
   * between current time and its starting time.
   * No action will be taken if the same key is called more than twice.
   * @param requestContext Timing records will be saved to this request context
   * @param timingKey Timing records will be identified by this key
   */
  public static void markTiming(RequestContext requestContext, TimingKey timingKey)
  {
    Map<TimingKey, TimingContext> timings = getTimingsMap(requestContext);
    if (timings.containsKey(timingKey))
    {
      timings.get(timingKey).complete();
    }
    else
    {
      timings.put(timingKey, new TimingContext(timingKey));
    }
  }

  /**
   * Mark a timing event and record it to the request context, with a specified duration number.
   * This method may be used when you don't have access to the request context and have to record time manually.
   * Warning will be issued if this timing key already exists.
   * @param requestContext Timing records will be saved to this request context
   * @param timingKey Timing records will be identified by this key
   * @param duration Duration of timing record to be added
   */
  public static void markTiming(RequestContext requestContext, TimingKey timingKey, long duration)
  {
    Map<TimingKey, TimingContext> timings = getTimingsMap(requestContext);
    if (!timings.containsKey(timingKey))
    {
      timings.put(timingKey, new TimingContext(timingKey, duration));
    }
    else
    {
      LOG.warn("This timing key already exists");
    }
  }

  /**
   * A timing context records the duration for a specific phase in processing a request
   */
  public static class TimingContext
  {
    private final TimingKey _timingKey;

    private final long _startTimeNano;

    private transient long _durationNano;

    public TimingContext(TimingKey timingKey)
    {
      _timingKey = timingKey;
      _startTimeNano = System.nanoTime();
      _durationNano = -1;
    }

    public TimingContext(TimingKey timingKey, long durationNano)
    {
      _timingKey = timingKey;
      _startTimeNano = -1;
      _durationNano = durationNano;
    }

    public TimingKey getName()
    {
      return _timingKey;
    }

    /**
     * Return the duration of this record.
     * @return Duration of this record, or -1 if the records is never completed.
     */
    public long getDurationNano()
    {
      return _durationNano;
    }

    /**
     * Complete a record. Warning will be issued if it's already completed.
     */
    public void complete()
    {
      if (_durationNano == -1)
      {
        _durationNano = System.nanoTime() - _startTimeNano;
      }
      else
      {
        LOG.warn("Trying to complete an already completed timing. This call will have no effects.");
      }
    }
  }
}
