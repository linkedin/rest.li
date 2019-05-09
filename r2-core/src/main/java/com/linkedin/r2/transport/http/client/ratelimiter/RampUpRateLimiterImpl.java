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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.transport.http.client.AsyncRateLimiter;
import com.linkedin.util.ArgumentUtil;


/**
 * Rate limiter decorator that allows growing linearly in QPS.
 * If a lower QPS is set, the change will take effect immediately
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class RampUpRateLimiterImpl implements RampUpRateLimiter
{

  private static final float DEFAULT_RAMP_UP_QPS = Integer.MAX_VALUE;
  private static final int ONE_SECOND_IN_MS = 1000;

  private final ScheduledExecutorService _scheduler;
  private final AtomicInteger _transactionId = new AtomicInteger(0);
  private final AsyncRateLimiter _asyncRateLimiter;
  private final Queue<Runnable> _setRatesQueue = new ConcurrentLinkedQueue<>();

  /**
   * Constructs a new instance of {@link RampUpRateLimiterImpl}.
   *
   * @param scheduler Scheduler used to execute the internal non-blocking event loop. MUST be single-threaded
   */
  public RampUpRateLimiterImpl(AsyncRateLimiter asyncRateLimiter, ScheduledExecutorService scheduler)
  {
    ArgumentUtil.ensureNotNull(asyncRateLimiter, "asyncRateLimiter");
    ArgumentUtil.ensureNotNull(scheduler, "scheduler");

    _asyncRateLimiter = asyncRateLimiter;
    _scheduler = scheduler;
  }

  /**
   * @see AsyncRateLimiter#setRate(double, long, int)
   * <p>
   * Unlimited ramp up by default
   */
  @Override
  public void setRate(double permitsPerPeriod, long period, int burst)
  {
    setRate(permitsPerPeriod, period, burst, DEFAULT_RAMP_UP_QPS);
  }

  /**
   * @see RampUpRateLimiter#setRate(double, long, int, float)
   */
  @Override
  public void setRate(double permitsPerPeriod, long periodMilliseconds, int burst, float rampUpPermitsPerSeconds)
  {
    ArgumentUtil.checkArgument(permitsPerPeriod >= 0, "permitsPerPeriod");
    ArgumentUtil.checkArgument(periodMilliseconds > 0, "periodMilliseconds");
    ArgumentUtil.checkArgument(burst > 0, "burst");
    ArgumentUtil.checkArgument(rampUpPermitsPerSeconds > 0, "rampUpPermitsPerSeconds");

    int operationId = _transactionId.incrementAndGet();

    _setRatesQueue.add(() -> setRateAndRampUp(operationId, permitsPerPeriod, periodMilliseconds, burst, rampUpPermitsPerSeconds));
    // running in single thread to avoid concurrency problems
    _scheduler.execute(this::runSetRates);
  }

  /**
   * Guarantees the order of of execution of the transactions
   */
  private void runSetRates()
  {
    Runnable poll;
    while ((poll = _setRatesQueue.poll()) != null)
    {
      poll.run();
    }
  }

  /**
   * Updates the Rate and starts the ramp up procedure
   * <p>
   * Must be run in single threaded environment
   *
   * @param transactionId           id of the current transaction, the last one will preempt the others
   * @param targetPermitsPerPeriod  target permitsPerPeriod that we aim to achieve after warm up
   * @param rampUpPermitsPerSeconds Maximum QPS by which it rate limiter can increase its throughput from second to second.
   */
  private void setRateAndRampUp(int transactionId, double targetPermitsPerPeriod, long periodMilliseconds, int burst, float rampUpPermitsPerSeconds)
  {

    Rate rate = getRate();
    double currentRate = rate.getEventsRaw() / rate.getPeriodRaw();
    double targetRate = targetPermitsPerPeriod / periodMilliseconds;

    // if we are reducing the rate we should apply it immediately
    if (targetRate <= currentRate)
    {
      doSetRate(targetPermitsPerPeriod, periodMilliseconds, burst);
      return;
    }

    // if it is not the current version anymore
    if (_transactionId.get() > transactionId)
    {
      return;
    }

    double nextTargetRate = Math.min(
      targetRate,
      // converting the rampUpPermitsPerSeconds from seconds to ms
      currentRate + rampUpPermitsPerSeconds / ONE_SECOND_IN_MS
    );

    doSetRate(nextTargetRate * periodMilliseconds, periodMilliseconds, burst);

    // continue ramping up if the target rate has not been reached yet
    if (nextTargetRate != targetRate)
    {
      _scheduler.schedule(() -> setRateAndRampUp(transactionId, targetPermitsPerPeriod, periodMilliseconds, burst, rampUpPermitsPerSeconds)
        // update every second being the rampUp in QPS
        , ONE_SECOND_IN_MS, TimeUnit.MILLISECONDS);
    }
  }

  private void doSetRate(double targetPermitsPerPeriod, long periodMilliseconds, int burst)
  {
    _asyncRateLimiter.setRate(targetPermitsPerPeriod, periodMilliseconds, burst);
  }

  @Override
  public void cancelAll(Throwable throwable)
  {
    _setRatesQueue.clear();
    _asyncRateLimiter.cancelAll(throwable);
  }

  // ############################################## Delegation Section ##############################################

  @Override
  public Rate getRate()
  {
    return _asyncRateLimiter.getRate();
  }

  @Override
  public void submit(Callback<None> callback) throws RejectedExecutionException
  {
    _asyncRateLimiter.submit(callback);
  }
}
