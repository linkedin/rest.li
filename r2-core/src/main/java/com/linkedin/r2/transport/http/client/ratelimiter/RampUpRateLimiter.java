/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.client.ratelimiter;

import com.linkedin.r2.transport.http.client.AsyncRateLimiter;

/**
 * A RampUpRateLimiter allows a smooth ramp up to get to a goal permitPerSeconds
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public interface RampUpRateLimiter extends AsyncRateLimiter
{

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
   * <p>
   * The rampUpPermitsPerSeconds allows having a smooth QPS ramp up from 0 (or whatever was the previous QPS), by
   * incrementing the QPS every second until reaching the target.
   *
   * @param permitsPerPeriod        Number of permits issued per period.
   * @param periodMilliseconds      Period in milliseconds permits will be issued.
   * @param burst                   Maximum number of permits can be issued at a time.
   * @param rampUpPermitsPerSeconds Maximum QPS by which it rate limiter can increase its throughput from second to second.
   */
  void setRate(double permitsPerPeriod, long periodMilliseconds, int burst, float rampUpPermitsPerSeconds);

}
