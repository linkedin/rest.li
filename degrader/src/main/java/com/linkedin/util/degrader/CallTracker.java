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
 * $Id: CallTracker.java 143409 2010-10-12 01:10:33Z automerg $ */
package com.linkedin.util.degrader;


import com.linkedin.common.stats.LongStats;
import java.util.Map;


/**
 * @author Dave Messink
 * @version $Rev: 143409 $
 */

public interface CallTracker
{
  /**
   * Returns the statistics for the sampling interval.
   * @return the statistics for the specified index.
   */
  CallStats getCallStats();

  /**
   * Returns the sampling interval.
   * @return the sampling interval.
   */
  long getInterval();

  /**
   * Register a listener to get notifications when sampling occurs or on reset.
   * @param listener that will receive notifications.
   */
  void addStatsRolloverEventListener(StatsRolloverEventListener listener);

  /**
   * Remove a listener that was previously registered.
   * @param listener that will be removed.
   * @return true if listener was removed.
   */
  boolean removeStatsRolloverEventListener(StatsRolloverEventListener listener);

  /**
   * Resets all internal call statistics to initial values
   */
  void reset();

  /**
   * Returns the time of the last call.
   * @return the time of the last call.
   */
  long getTimeSinceLastCallStart();

  /**
   * Returns the time of the last reset.
   * @return the time of the last reset.
   */
  long getLastResetTime();

  /**
   * Returns the number of calls completed since last reset.
   * @return the number of calls completed since last reset.
   */
  long getCurrentCallCountTotal();

  /**
   * Returns the number of calls started since last reset.
   * @return the number of calls started since last reset.
   */
  long getCurrentCallStartCountTotal();

  /**
   * Returns the number of errors reported by calls since last reset.
   * @return the number of errors reported by calls since last reset.
   */
  long getCurrentErrorCountTotal();

  /**
   * Returns the number of errors per ErrorType reported by calls since last reset.
   * @return the number of errors per ErrorType reported by calls since last reset.
   */
  Map<ErrorType, Integer> getCurrentErrorTypeCountsTotal();

  /**
   * Returns the current number of concurrent calls.
   * @return the current number of concurrent calls.
   */
  int getCurrentConcurrency();

  /**
   * Tracks a single successful call
   * @param duration in milliseconds
   */
  void trackCall(long duration);

  /**
   * Tracks a single failed call
   * @param duration in milliseconds
   */
  void trackCallWithError(long duration);

  /**
   * Indicates the start of a method invocation
   * @return an object that can be used to indicate completion of the call
   */
  CallCompletion startCall();

  interface CallStats
  {
    /**
     * Returns whether stats is stale.
     * @param currentTimeMillis is the current time.
     * @return true if stats is stale.
     */
    boolean stale(long currentTimeMillis);

    /**
     * Returns the number of specific error type in the sample.
     * @return the number of specific error type in the sample
     */
    public Map<ErrorType, Integer> getErrorTypeCounts();

    /**
     * Returns the number of specific error type (since last reset) at the end of sampling interval.
     * @return the number of specific error type (since last reset) at the end of sampling interval.
     */
    public Map<ErrorType, Integer> getErrorTypeCountsTotal();

    /**
     * Returns the time at the start of the sampling interval.
     * @return the time at the start of the sampling interval.
     */
    long getIntervalStartTime();

    /**
     * Returns the time at the end of the sampling interval.
     * @return the time at the end of the sampling interval.
     */
    long getIntervalEndTime();

    /**
     * Return the sampling interval.
     * @return the sampling interval.
     */
    long getInterval();

    /**
     * Returns the number of calls completed in the sample.
     * @return the number of calls completed in the sample.
     */
    int getCallCount();

    /**
     * Returns the number of calls completed (since last reset) at the end of sampling interval.
     * @return the number of calls completed (since last reset) at the end of sampling interval.
     */
    long getCallCountTotal();

    /**
     * Returns the number of calls started in the sample.
     * @return the number of calls started in the sample.
     */
    int getCallStartCount();

    /**
     * Returns the number of calls started (since last reset) at the end of sampling interval.
     * @return the number of calls started (since last reset) at the end of sampling interval.
     */
    long getCallStartCountTotal();

    /**
     * Returns the calls per second of the sample.
     *
     * This derived data from call count divided by interval.
     *
     * @return the calls per second of the sample.
     */
    double getCallsPerSecond();

    /**
     * Returns the number of errors in the sample.
     * @return the number of errors in the sample.
     */
    int getErrorCount();

    /**
     * Returns the number of errors (since last reset) at the end of sampling interval.
     * @return the number of errors (since last reset) at the end of sampling interval.
     */
    long getErrorCountTotal();

    /**
     * Returns the error rate of the sample.
     *
     * This is derived data from error count divided by call count.
     *
     * @return the error rate of the sample.
     */
    double getErrorRate();

    /**
     * Returns the maximum number of concurrently active calls in the sample.
     * @return the maximum number of concurrently active calls in the sample.
     */
    int getConcurrentMax();

    /**
     * Returns the average start time of the outstanding requests at the end of the interval.
     * @return the average start time of the outstanding requests at the end of the interval.
     */
    long getOutstandingStartTimeAvg();

    /**
     * Returns the number of calls started but not ended at the end of the interval.
     * @return the number of calls started but not ended at the end of the interval.
     */
    int getOutstandingCount();

    /**
     * Returns the call time statistics of the sample.
     * @return the call time statistics of the sample.
     */
    LongStats getCallTimeStats();
  }

  interface StatsRolloverEventListener
  {
    /**
     * Listener for call stats rollover events
     *
     * @param statsRolloverEvent the event indicating that the call stats were rolled over
     */
    void onStatsRollover(StatsRolloverEvent statsRolloverEvent);
  }

  /**
   * StatsRollover event indicates that the queue stats were rolled over
   */
  interface StatsRolloverEvent
  {
    /**
     * Returns the statistics for the previous interval
     *
     * @return the statistics for the previous interval
     */
    CallStats getCallStats();

    /**
     * Returns whether the statistics for this interval were reset
     *
     * @return Returns true if the statistics for this interval were reset
     */
    boolean isReset();
  }
}
