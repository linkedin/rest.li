/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.r2.netty.common;

import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SystemClock;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Scheduler to raise {@link TimeoutException} when streaming is timed out. During schedule execution
 * if there is an activity the {@link TimeoutException} will not be raised, instead the idle time check is again
 * scheduled to check in the next window.
 * @author Nizar Mankulangara
 */
public class StreamingTimeout
{
  public static final String STREAMING_TIMEOUT_MESSAGE = "Exceeded stream idle timeout of %sms";

  private final ScheduledExecutorService _scheduler;
  private final long _streamingTimeout;
  private final Channel _channel;
  private final Clock _clock;
  private final AtomicLong _lastActiveTime;
  private final Object _lock = new Object();

  private ScheduledFuture<?> _future;


  /**
   * Creates a new instance of {@link StreamingTimeout}.
   *
   * @param scheduler a scheduler executor service to check the streaming timeout
   * @param streamingTimeout The streaming timeout in milliseconds
   * @param channel The Channel on which the Timeout exception will be raised
   * @param clock Clock to get current time
   */
  public StreamingTimeout(ScheduledExecutorService scheduler, long streamingTimeout, final Channel channel, Clock clock)
  {
    _scheduler = scheduler;
    _streamingTimeout = streamingTimeout;
    _channel = channel;
    _clock = clock;

    _lastActiveTime = new AtomicLong(clock.currentTimeMillis());
    scheduleNextIdleTimeout();
  }

  public void refreshLastActiveTime()
  {
    _lastActiveTime.getAndSet(_clock.currentTimeMillis());
  }

  public void cancel()
  {
    synchronized (_lock)
    {
      if(_future != null)
      {
        _future.cancel(false);
      }
    }
  }

  private void raiseTimeoutIfIdle()
  {
    if (_clock.currentTimeMillis() - _lastActiveTime.get() < _streamingTimeout)
    {
      scheduleNextIdleTimeout();
    }
    else
    {
      _channel.pipeline().fireExceptionCaught(new TimeoutException(String.format(STREAMING_TIMEOUT_MESSAGE, _streamingTimeout)));
    }
  }

  private void scheduleNextIdleTimeout()
  {
    ScheduledFuture<?> future = _scheduler.schedule(this::raiseTimeoutIfIdle, getNextExecutionTime(), TimeUnit.MILLISECONDS);
    synchronized (_lock)
    {
      _future = future;
    }
  }

  private long getNextExecutionTime()
  {
    long timeElapsed = _clock.currentTimeMillis() -_lastActiveTime.get();
    long timeRemaining = _streamingTimeout - timeElapsed;
    return timeRemaining;
  }
}
