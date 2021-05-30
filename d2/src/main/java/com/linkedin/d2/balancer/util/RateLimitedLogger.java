package com.linkedin.d2.balancer.util;

import org.slf4j.Logger;

import java.time.Clock;

/**
 * @see com.linkedin.util.RateLimitedLogger
 */
@Deprecated
public class RateLimitedLogger extends com.linkedin.util.RateLimitedLogger
{

  public RateLimitedLogger(Logger loggerImpl, long logRate, Clock clock)
  {
    super(loggerImpl, logRate, clock);
  }
}
