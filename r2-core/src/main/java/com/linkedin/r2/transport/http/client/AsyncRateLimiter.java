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

package com.linkedin.r2.transport.http.client;

import java.util.concurrent.RejectedExecutionException;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.transport.http.client.ratelimiter.Rate;


/**
 * An asynchronous rate limiter interface that supports running user supplied {@link Callback}
 * at a specific rate. The rate is specified as number of permits per period of time and can be
 * dynamically changed at anytime. The submitted callback's #onSuccess is invoked when the rate
 * limiter is ready. If #cancelAll is invoked, all pending and subsequent callbacks should be
 * invoked with #onError. The implementation should guarantee that either #onSuccess or #onError
 * is invoked but not both.
 *
 * @author Sean Sheng
 */
public interface AsyncRateLimiter
{
  /**
   * Submits a {@link Callback} to be executed at the earliest available time. The #onSuccess method
   * will be invoked when the callback is executed. If the implementation decides the callback cannot
   * be successfully invoked, callback's #onError will be invoked with {@link RejectedExecutionException}.
   *
   * @param callback Callback to be submitted
   */
  void submit(Callback<None> callback) throws RejectedExecutionException;

  /**
   * @return the current rate
   */
  Rate getRate();

  /**
   * Sets the execution rate as the number of permits over some period of time. The actual period length
   * is calculated based on the rate and burst allowed. If burst allowed is lower than the given permits
   * per period, the length of the period will be adjusted to account for the burst allowed. The minimum
   * period is one millisecond. If the specified events per period cannot satisfy the burst, an
   * {@link IllegalArgumentException} will be thrown.
   * <p>
   * For example, if the rate is specified as 100 events per second and the burst is set to 10, then
   * the rate will be created as 10 events per 100 milliseconds. However, if the rate is specified as
   * 2000 events per second and the burst is 1, since the minimum period is 1 millisecond, the burst
   * requirement cannot be satisfied. An IllegalArgumentException is thrown as a result.
   *
   * @param permitsPerPeriod Number of permits issued per period.
   * @param period           Period in milliseconds permits will be issued.
   * @param burst            Maximum number of permits can be issued at a time.
   */
  void setRate(double permitsPerPeriod, long period, int burst);

  /**
   * Keeping it for backward compatibility to not cause NoSuchMethodExceptions in libraries depending on this method
   *
   * @deprecated see setRate(double, long, int)
   */
  @Deprecated
  default void setRate(int permitsPerPeriod, long period, int burst)
  {
    setRate((double) permitsPerPeriod, period, burst);
  }

  /**
   * Cancels all pending {@link Callback}s and invokes the #onError method with a supplied {@link Throwable}.
   *
   * @param throwable Reason for cancelling all pending callbacks.
   */
  void cancelAll(Throwable throwable);

  /**
   * Returns how many requests are in the queue in this instant.
   * Returns -1 if the method is unimplemented
   */
  default int getPendingTasksCount(){
    return -1;
  };
}
