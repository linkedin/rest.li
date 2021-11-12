package com.linkedin.r2.transport.http.client;

import java.time.temporal.ChronoUnit;


public class SharedBufferConstantQpsRateLimiterFactory implements ConstantQpsRateLimiterFactory {
  private final ConstantQpsRateLimiter _rateLimiter;

  public SharedBufferConstantQpsRateLimiterFactory(ConstantQpsRateLimiter rateLimiter)
  {
    _rateLimiter = rateLimiter;
  }

  public ConstantQpsRateLimiter getRateLimiter(int bufferCapacity, int bufferTtl, ChronoUnit bufferTtlUnit)
  {
    _rateLimiter.setBufferCapacity(bufferCapacity);
    _rateLimiter.setBufferTtl(bufferTtl, bufferTtlUnit);
    return _rateLimiter;
  }
}
