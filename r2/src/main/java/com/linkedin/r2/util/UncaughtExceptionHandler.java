/**
 * $Id: $
 */

package com.linkedin.r2.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UncaughtExceptionHandler
 *
 * This class can be set on a thread to log uncaught exceptions.
 *
 * @see Thread#setUncaughtExceptionHandler
 *
 * @author David Hoa
 * @version $Revision: $
 */

public class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler
{
  private static final Logger _log = LoggerFactory.getLogger(UncaughtExceptionHandler.class);

  @Override
  public void uncaughtException(Thread t, Throwable e)
  {
    _log.error("exception from thread: " + t, e);
    throw new RuntimeException(e);
  }
}
