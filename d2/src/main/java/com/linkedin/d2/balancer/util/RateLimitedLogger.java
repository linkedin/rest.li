package com.linkedin.d2.balancer.util;

import com.linkedin.util.clock.Clock;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;


/**
 * Simple logger wrapper to provide rate limiting of log messages. The rate is controlled by the duration,
 * ie how often (in millisecond) the message should be logged. After one message logged, the rest of the
 * messages within that duration will be ignored.
 */

public class RateLimitedLogger
{
  private static final long INIT_TIME = -1;

  private final Logger _logger;
  private final long _logRate;
  private final Clock _clock;

  private final AtomicLong _lastLog = new AtomicLong(INIT_TIME);

  public RateLimitedLogger(Logger logger, long logRate, Clock clock)
  {
    _logger = logger;
    _logRate = logRate;
    _clock = clock;
  }

  public boolean isTraceEnabled()
  {
    return _logger.isTraceEnabled();
  }

  public void trace(String msg)
  {
    if (logAllowed())
    {
      _logger.trace(msg);
    }
  }

  public void trace(String format, Object[] argArray)
  {
    if (logAllowed())
    {
      _logger.trace(format, argArray);
    }
  }

  public void trace(String msg, Throwable t)
  {
    if (logAllowed())
    {
      _logger.trace(msg, t);
    }
  }

  public void trace(String format, Object obj)
  {
    if (logAllowed())
    {
      _logger.trace(format, obj);
    }
  }

  public void trace(String format, Object obj1, Object obj2)
  {
    if (logAllowed())
    {
      _logger.trace(format, obj1, obj2);
    }
  }

  public boolean isDebugEnabled()
  {
    return _logger.isDebugEnabled();
  }

  public void debug(String msg)
  {
    if (logAllowed())
    {
      _logger.debug(msg);
    }
  }

  public void debug(String format, Object[] argArray)
  {
    if (logAllowed())
    {
      _logger.debug(format, argArray);
    }
  }

  public void debug(String msg, Throwable t)
  {
    if (logAllowed())
    {
      _logger.debug(msg, t);
    }
  }

  public void debug(String format, Object obj)
  {
    if (logAllowed())
    {
      _logger.debug(format, obj);
    }
  }

  public void debug(String format, Object obj1, Object obj2)
  {
    if (logAllowed())
    {
      _logger.debug(format, obj1, obj2);
    }
  }

  public boolean isInfoEnabled()
  {
    return _logger.isInfoEnabled();
  }

  public void info(String msg)
  {
    if (logAllowed())
    {
      _logger.info(msg);
    }
  }

  public void info(String format, Object[] argArray)
  {
    if (logAllowed())
    {
      _logger.info(format, argArray);
    }
  }

  public void info(String msg, Throwable t)
  {
    if (logAllowed())
    {
      _logger.info(msg, t);
    }
  }

  public void info(String format, Object obj)
  {
    if (logAllowed())
    {
      _logger.info(format, obj);
    }
  }

  public void info(String format, Object obj1, Object obj2)
  {
    if (logAllowed())
    {
      _logger.info(format, obj1, obj2);
    }
  }

  public boolean isWarnEnabled()
  {
    return _logger.isWarnEnabled();
  }

  public void warn(String msg)
  {
    if (logAllowed())
    {
      _logger.warn(msg);
    }
  }

  public void warn(String format, Object[] argArray)
  {
    if (logAllowed())
    {
      _logger.warn(format, argArray);
    }
  }

  public void warn(String msg, Throwable t)
  {
    if (logAllowed())
    {
      _logger.warn(msg, t);
    }
  }

  public void warn(String format, Object obj)
  {
    if (logAllowed())
    {
      _logger.warn(format, obj);
    }
  }

  public void warn(String format, Object obj1, Object obj2)
  {
    if (logAllowed())
    {
      _logger.warn(format, obj1, obj2);
    }
  }

  public boolean isErrorEnabled()
  {
    return _logger.isErrorEnabled();
  }

  public void error(String msg)
  {
    if (logAllowed())
    {
      _logger.error(msg);
    }
  }

  public void error(String format, Object[] argArray)
  {
    if (logAllowed())
    {
      _logger.error(format, argArray);
    }
  }

  public void error(String msg, Throwable t)
  {
    if (logAllowed())
    {
      _logger.error(msg, t);
    }
  }

  public void error(String format, Object obj)
  {
    if (logAllowed())
    {
      _logger.error(format, obj);
    }
  }

  public void error(String format, Object obj1, Object obj2)
  {
    if (logAllowed())
    {
      _logger.error(format, obj1, obj2);
    }
  }

  private boolean logAllowed()
  {
    final long now = _clock.currentTimeMillis();
    final long lastLog = _lastLog.get();
    return (lastLog == INIT_TIME || now - lastLog >= _logRate) && _lastLog.compareAndSet(lastLog, now);
  }
}
