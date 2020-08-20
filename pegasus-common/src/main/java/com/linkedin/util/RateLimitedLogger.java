package com.linkedin.util;

import com.linkedin.util.clock.Clock;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.Marker;


/**
 * Simple logger wrapper to provide rate limiting of log messages. The rate is controlled by the duration,
 * ie how often (in millisecond) the message should be logged. After one message logged, the rest of the
 * messages within that duration will be ignored.
 */

public class RateLimitedLogger implements Logger
{
  private static final long INIT_TIME = -1;

  private final Logger _loggerImpl;
  private final long _logRate;
  private final Clock _clock;

  private final AtomicLong _lastLog = new AtomicLong(INIT_TIME);

  public RateLimitedLogger(Logger loggerImpl, long logRate, Clock clock)
  {
    _loggerImpl = loggerImpl;
    _logRate = logRate;
    _clock = clock;
  }

  @Override
  public String getName()
  {
    return _loggerImpl.getName();
  }

  @Override
  public boolean isTraceEnabled()
  {
    return _loggerImpl.isTraceEnabled();
  }

  @Override
  public void trace(String msg)
  {
    if (logAllowed())
    {
      _loggerImpl.trace(msg);
    }
  }

  @Override
  public void trace(String format, Object... arguments)
  {
    if (logAllowed())
    {
      _loggerImpl.trace(format, arguments);
    }
  }

  @Override
  public void trace(String msg, Throwable t)
  {
    if (logAllowed())
    {
      _loggerImpl.trace(msg, t);
    }
  }

  @Override
  public void trace(String format, Object obj)
  {
    if (logAllowed())
    {
      _loggerImpl.trace(format, obj);
    }
  }

  @Override
  public void trace(String format, Object obj1, Object obj2)
  {
    if (logAllowed())
    {
      _loggerImpl.trace(format, obj1, obj2);
    }
  }

  @Override
  public boolean isTraceEnabled(Marker marker)
  {
    return _loggerImpl.isTraceEnabled(marker);
  }

  @Override
  public void trace(Marker marker, String msg)
  {
    if (logAllowed())
    {
      _loggerImpl.trace(marker, msg);
    }
  }

  @Override
  public void trace(Marker marker, String format, Object arg)
  {
    if (logAllowed())
    {
      _loggerImpl.trace(marker, format, arg);
    }
  }

  @Override
  public void trace(Marker marker, String format, Object arg1, Object arg2)
  {
    if (logAllowed())
    {
      _loggerImpl.trace(marker, format, arg1, arg2);
    }
  }

  @Override
  public void trace(Marker marker, String format, Object... arguments)
  {
    if (logAllowed())
    {
      _loggerImpl.trace(marker, format, arguments);
    }
  }

  @Override
  public void trace(Marker marker, String msg, Throwable t)
  {
    if (logAllowed())
    {
      _loggerImpl.trace(marker, msg, t);
    }
  }

  @Override
  public boolean isDebugEnabled()
  {
    return _loggerImpl.isDebugEnabled();
  }

  @Override
  public void debug(String msg)
  {
    if (logAllowed())
    {
      _loggerImpl.debug(msg);
    }
  }

  @Override
  public void debug(String format, Object... arguments)
  {
    if (logAllowed())
    {
      _loggerImpl.debug(format, arguments);
    }
  }

  @Override
  public void debug(String msg, Throwable t)
  {
    if (logAllowed())
    {
      _loggerImpl.debug(msg, t);
    }
  }

  @Override
  public void debug(String format, Object obj)
  {
    if (logAllowed())
    {
      _loggerImpl.debug(format, obj);
    }
  }

  @Override
  public void debug(String format, Object obj1, Object obj2)
  {
    if (logAllowed())
    {
      _loggerImpl.debug(format, obj1, obj2);
    }
  }

  @Override
  public boolean isDebugEnabled(Marker marker)
  {
    return _loggerImpl.isDebugEnabled(marker);
  }


  @Override
  public void debug(Marker marker, String msg)
  {
    if (logAllowed())
    {
      _loggerImpl.debug(marker, msg);
    }
  }

  @Override
  public void debug(Marker marker, String format, Object arg)
  {
    if (logAllowed())
    {
      _loggerImpl.debug(marker, format, arg);
    }
  }

  @Override
  public void debug(Marker marker, String format, Object arg1, Object arg2)
  {
    if (logAllowed())
    {
      _loggerImpl.debug(marker, format, arg1, arg2);
    }
  }

  @Override
  public void debug(Marker marker, String format, Object... arguments)
  {
    if (logAllowed())
    {
      _loggerImpl.debug(marker, format, arguments);
    }
  }

  @Override
  public void debug(Marker marker, String msg, Throwable t)
  {
    if (logAllowed())
    {
      _loggerImpl.debug(marker, msg, t);
    }
  }


  @Override
  public boolean isInfoEnabled()
  {
    return _loggerImpl.isInfoEnabled();
  }

  @Override
  public void info(String msg)
  {
    if (logAllowed())
    {
      _loggerImpl.info(msg);
    }
  }

  @Override
  public void info(String format, Object... arguments)
  {
    if (logAllowed())
    {
      _loggerImpl.info(format, arguments);
    }
  }

  @Override
  public void info(String msg, Throwable t)
  {
    if (logAllowed())
    {
      _loggerImpl.info(msg, t);
    }
  }

  @Override
  public void info(String format, Object obj)
  {
    if (logAllowed())
    {
      _loggerImpl.info(format, obj);
    }
  }

  @Override
  public void info(String format, Object obj1, Object obj2)
  {
    if (logAllowed())
    {
      _loggerImpl.info(format, obj1, obj2);
    }
  }

  @Override
  public boolean isInfoEnabled(Marker marker)
  {
    return _loggerImpl.isInfoEnabled(marker);
  }


  @Override
  public void info(Marker marker, String msg)
  {
    if (logAllowed())
    {
      _loggerImpl.info(marker, msg);
    }
  }

  @Override
  public void info(Marker marker, String format, Object arg)
  {
    if (logAllowed())
    {
      _loggerImpl.info(marker, format, arg);
    }
  }

  @Override
  public void info(Marker marker, String format, Object arg1, Object arg2)
  {
    if (logAllowed())
    {
      _loggerImpl.info(marker, format, arg1, arg2);
    }
  }

  @Override
  public void info(Marker marker, String format, Object... arguments)
  {
    if (logAllowed())
    {
      _loggerImpl.info(marker, format, arguments);
    }
  }

  @Override
  public void info(Marker marker, String msg, Throwable t)
  {
    if (logAllowed())
    {
      _loggerImpl.info(marker, msg, t);
    }
  }


  @Override
  public boolean isWarnEnabled()
  {
    return _loggerImpl.isWarnEnabled();
  }

  @Override
  public void warn(String msg)
  {
    if (logAllowed())
    {
      _loggerImpl.warn(msg);
    }
  }

  @Override
  public void warn(String format, Object... arguments)
  {
    if (logAllowed())
    {
      _loggerImpl.warn(format, arguments);
    }
  }

  @Override
  public void warn(String msg, Throwable t)
  {
    if (logAllowed())
    {
      _loggerImpl.warn(msg, t);
    }
  }

  @Override
  public void warn(String format, Object obj)
  {
    if (logAllowed())
    {
      _loggerImpl.warn(format, obj);
    }
  }

  @Override
  public void warn(String format, Object obj1, Object obj2)
  {
    if (logAllowed())
    {
      _loggerImpl.warn(format, obj1, obj2);
    }
  }


  @Override
  public boolean isWarnEnabled(Marker marker)
  {
    return _loggerImpl.isWarnEnabled(marker);
  }

  @Override
  public void warn(Marker marker, String msg)
  {
    if (logAllowed())
    {
      _loggerImpl.warn(marker, msg);
    }
  }

  @Override
  public void warn(Marker marker, String format, Object arg)
  {
    if (logAllowed())
    {
      _loggerImpl.warn(marker, format, arg);
    }
  }

  @Override
  public void warn(Marker marker, String format, Object arg1, Object arg2)
  {
    if (logAllowed())
    {
      _loggerImpl.warn(marker, format, arg1, arg2);
    }
  }

  @Override
  public void warn(Marker marker, String format, Object... arguments)
  {
    if (logAllowed())
    {
      _loggerImpl.warn(marker, format, arguments);
    }
  }

  @Override
  public void warn(Marker marker, String msg, Throwable t)
  {
    if (logAllowed())
    {
      _loggerImpl.warn(marker, msg, t);
    }
  }


  @Override
  public boolean isErrorEnabled()
  {
    return _loggerImpl.isErrorEnabled();
  }

  @Override
  public void error(String msg)
  {
    if (logAllowed())
    {
      _loggerImpl.error(msg);
    }
  }

  @Override
  public void error(String format, Object... arguments)
  {
    if (logAllowed())
    {
      _loggerImpl.error(format, arguments);
    }
  }

  @Override
  public void error(String msg, Throwable t)
  {
    if (logAllowed())
    {
      _loggerImpl.error(msg, t);
    }
  }

  @Override
  public void error(String format, Object obj)
  {
    if (logAllowed())
    {
      _loggerImpl.error(format, obj);
    }
  }

  @Override
  public void error(String format, Object obj1, Object obj2)
  {
    if (logAllowed())
    {
      _loggerImpl.error(format, obj1, obj2);
    }
  }

  @Override
  public boolean isErrorEnabled(Marker marker)
  {
    return _loggerImpl.isErrorEnabled(marker);
  }

  @Override
  public void error(Marker marker, String msg)
  {
    if (logAllowed())
    {
      _loggerImpl.error(marker, msg);
    }
  }

  @Override
  public void error(Marker marker, String format, Object arg)
  {
    if (logAllowed())
    {
      _loggerImpl.error(marker, format, arg);
    }
  }

  @Override
  public void error(Marker marker, String format, Object arg1, Object arg2)
  {
    if (logAllowed())
    {
      _loggerImpl.error(marker, format, arg1, arg2);
    }
  }

  @Override
  public void error(Marker marker, String format, Object... arguments)
  {
    if (logAllowed())
    {
      _loggerImpl.error(marker, format, arguments);
    }
  }

  @Override
  public void error(Marker marker, String msg, Throwable t)
  {
    if (logAllowed())
    {
      _loggerImpl.error(marker, msg, t);
    }
  }

  private boolean logAllowed()
  {
    final long now = _clock.currentTimeMillis();
    final long lastLog = _lastLog.get();
    return (lastLog == INIT_TIME || now - lastLog >= _logRate) && _lastLog.compareAndSet(lastLog, now);
  }
}
