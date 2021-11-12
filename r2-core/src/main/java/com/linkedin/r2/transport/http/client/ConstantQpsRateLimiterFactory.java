package com.linkedin.r2.transport.http.client;

import java.time.temporal.ChronoUnit;


public interface ConstantQpsRateLimiterFactory {
  ConstantQpsRateLimiter getRateLimiter(int bufferCapacity, int bufferTtl, ChronoUnit bufferTtlUnit);
}
