/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.r2.disruptor;

import java.util.concurrent.TimeoutException;


/**
 * Implementations of different {@link DisruptContext}s and provides factory methods
 * for creating each implementation.
 *
 * @author Sean Sheng
 * @version $Revision$
 */
public class DisruptContexts
{
  public static DisruptContext delay(long delay)
  {
    if (delay < 0)
    {
      throw new IllegalArgumentException("Delay cannot be smaller than 0");
    }
    return new DelayDisruptContext(delay);
  }

  public static DisruptContext minimumDelay(long delay)
  {
    if (delay < 0)
    {
      throw new IllegalArgumentException("Delay cannot be smaller than 0");
    }
    return new MinimumDelayDisruptContext(delay);
  }

  public static DisruptContext timeout()
  {
    return new TimeoutDisruptContext();
  }

  public static DisruptContext error(long latency)
  {
    if (latency < 0)
    {
      throw new IllegalArgumentException("Latency cannot be smaller than 0");
    }
    return new ErrorDisruptContext(latency);
  }

  /**
   * Disrupts the request by adding a certain amount of delay.
   */
  static class DelayDisruptContext extends DisruptContext
  {
    private final long _delay;

    public DelayDisruptContext(long delay)
    {
      this(DisruptMode.DELAY, delay);
    }

    public DelayDisruptContext(DisruptMode mode, long delay)
    {
      super(mode);
      _delay = delay;
    }

    public long delay()
    {
      return _delay;
    }
  }

  /**
   * Disrupts the request by adding a certain amount of delay if the total latency is less than the specified delay.
   */
  static class MinimumDelayDisruptContext extends DisruptContext
  {
    private final long _delay;

    /**
     * Records when the request was sent in ms.
     */
    private long _requestStartTime = 0;

    public MinimumDelayDisruptContext(long delay)
    {
      this(DisruptMode.MINIMUM_DELAY, delay);
    }

    public MinimumDelayDisruptContext(DisruptMode mode, long delay)
    {
      super(mode);
      _delay = delay;
    }

    public long delay()
    {
      return _delay;
    }

    public long requestStartTime()
    {
      return _requestStartTime;
    }

    public void requestStartTime(long requestStartTime)
    {
      _requestStartTime = requestStartTime;
    }
  }

  /**
   * Disrupts the request by returning a {@link TimeoutException} after service configured timeout.
   */
  static class TimeoutDisruptContext extends DisruptContext
  {
    public TimeoutDisruptContext()
    {
      super(DisruptMode.TIMEOUT);
    }
  }

  /**
   * Disrupts the request by returning an error after a certain amount of latency.
   */
  static class ErrorDisruptContext extends DisruptContext
  {
    private final long _latency;

    public ErrorDisruptContext(long latency)
    {
      super(DisruptMode.ERROR);
      _latency = latency;
    }

    public long latency()
    {
      return _latency;
    }
  }
}
