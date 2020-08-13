package com.linkedin.d2.balancer.util;

import com.linkedin.util.clock.Clock;
import org.slf4j.Logger;

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
