/*
   Copyright (c) 2012 LinkedIn Corp.

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

/**
 * $Id: $
 */

package com.linkedin.r2.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * an UncaughtExceptionHandler can be specified in the constructor if uncaught exceptions need to be
 * caught. If the NamedThreadFactory is used with an ExecutorService then the exceptions are caught
 * and stashed away, so there is no point in using specifying an UncaughtExceptionHandler in those cases.
 *
 * @author Steven Ihde
 * @version $Revision: $
 */

public class NamedThreadFactory implements ThreadFactory
{
  private static final AtomicInteger _instanceNumber = new AtomicInteger();
  private final AtomicInteger _threadNumber = new AtomicInteger();
  private final String _namePrefix;
  private final UncaughtExceptionHandler _uncaughtExceptionHandler;

  /**
   * Construct a new instance with the specified name.
   *
   * @param name the name to be used as part of the prefix for this factory.
   */
  public NamedThreadFactory(String name)
  {
    this(name, null);
  }

  public NamedThreadFactory(String name, UncaughtExceptionHandler uncaughtExceptionHandler)
  {
    _namePrefix = name + "-" + _instanceNumber.incrementAndGet();
    _uncaughtExceptionHandler = uncaughtExceptionHandler;
  }

  /**
   * Create a new Thread for the specified {@link Runnable}.
   *
   * @param runnable the {@link Runnable} to be executed by the new thread.
   * @return a new {@link Thread} instance
   */
  public Thread newThread(Runnable runnable)
  {
    Thread newThread;
    String name = _namePrefix + "-" + _threadNumber.incrementAndGet();

    newThread = new Thread(runnable, name);
    if (_uncaughtExceptionHandler != null)
    {
      newThread.setUncaughtExceptionHandler(_uncaughtExceptionHandler);
    }

    return newThread;
  }
}
