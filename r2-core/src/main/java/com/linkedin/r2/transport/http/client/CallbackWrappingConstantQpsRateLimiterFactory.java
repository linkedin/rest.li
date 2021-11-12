package com.linkedin.r2.transport.http.client;

import com.linkedin.r2.transport.http.client.ratelimiter.CallbackWrapper;
import com.linkedin.util.clock.Clock;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;


public class CallbackWrappingConstantQpsRateLimiterFactory implements ConstantQpsRateLimiterFactory {

  private final ScheduledExecutorService _scheduler;
  private final Executor _executor;
  private final Clock _clock;
  private final CallbackWrapper _callbackWrapper;

  public CallbackWrappingConstantQpsRateLimiterFactory(
      ScheduledExecutorService scheduler,
      Executor executor,
      Clock clock,
      CallbackWrapper wrapper)
  {
    _scheduler = scheduler;
    _executor = executor;
    _clock = clock;
    _callbackWrapper = wrapper;
  }

  public ConstantQpsRateLimiter getRateLimiter(int bufferCapacity, int bufferTtl, ChronoUnit bufferTtlUnit)
  {
    EvictingCircularBuffer buffer = new EvictingCircularBuffer(bufferCapacity, bufferTtl, bufferTtlUnit, _clock, _callbackWrapper);
    return new ConstantQpsRateLimiter(_scheduler, _executor, _clock, buffer);
  }
}
