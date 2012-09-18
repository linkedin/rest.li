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

package com.linkedin.d2.discovery.event;

import static com.linkedin.d2.discovery.util.LogUtil.debug;
import static com.linkedin.d2.discovery.util.LogUtil.error;
import static com.linkedin.d2.discovery.util.LogUtil.info;
import static com.linkedin.d2.discovery.util.LogUtil.warn;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyEventThread extends Thread
{
  private static final Logger             _log =
                                                LoggerFactory.getLogger(PropertyEventThread.class);

  private BlockingQueue<PropertyEvent> _messages;

  public PropertyEventThread(String name)
  {
    this(name, Integer.MAX_VALUE);
  }

  public PropertyEventThread(String name, int size)
  {
    this(name, size, false);
  }

  public PropertyEventThread(String name, int size, boolean start)
  {
    _messages = new LinkedBlockingQueue<PropertyEvent>(size);

    setDaemon(true);
    setName("PropertyEventThread-" + getId() + "-" + name);

    if (start)
    {
      start();
    }
  }

  public int getRemainingCapacity()
  {
    return _messages.remainingCapacity();
  }

  public int getQueuedMessageCount()
  {
    return _messages.size();
  }

  @Override
  public void start()
  {
    info(_log, "starting thread: ", getName());

    super.start();
  }

  public boolean send(PropertyEvent message)
  {
    if (message == null)
    {
      warn(_log, "send called with null message");
    }
    else if (!isAlive())
    {
      error(_log, "sending message while thread ", getName(), " is down: ", message);
    }
    else
    {
      debug(_log, getName(), " got message: ", message);

      return _messages.add(message);
    }

    int remainingCapacity = getRemainingCapacity();

    if (remainingCapacity < 1000)
    {
      warn(_log,
           "remaining capacity for thread ",
           getName(),
           " is dangerously low: ",
           remainingCapacity);
    }

    return false;

  }

  @Override
  public void run()
  {
    boolean shutdown = false;

    while (!shutdown || _messages.size() > 0)
    {
      try
      {
        PropertyEvent message = _messages.poll(1, TimeUnit.SECONDS);

        if (message != null)
        {
          message.run();
        }
        else
        {
          debug(_log, "received null message in thread: ", getName());
        }
      }
      catch (InterruptedException e)
      {
        shutdown = true;

        info(_log, "thread ", getName(), " got interrupt, so shutting down");
      }
      catch (Exception e)
      {
        _log.error("Unhandled exception in event thread", e);
      }
    }

    info(_log, "thread ", getName(), " finished run() and is now dying");
  }

  // interfaces
  public static abstract class PropertyEvent implements Runnable
  {

    private String _description;

    public PropertyEvent(String description)
    {
      _description = description;
    }

    @Override
    public String toString()
    {
      return "PropertyEvent [_description=" + _description + "]";
    }
  }

  public static interface PropertyEventShutdownCallback
  {
    void done();
  }
}
