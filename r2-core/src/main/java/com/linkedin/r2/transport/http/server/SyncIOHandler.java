/*
   Copyright (c) 2015 LinkedIn Corp.

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


package com.linkedin.r2.transport.http.server;

import com.linkedin.data.ByteString;
import com.linkedin.r2.message.stream.entitystream.AbortedException;
import com.linkedin.r2.message.stream.entitystream.ReadHandle;
import com.linkedin.r2.message.stream.entitystream.Reader;
import com.linkedin.r2.message.stream.entitystream.WriteHandle;
import com.linkedin.r2.message.stream.entitystream.Writer;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.r2.filter.R2Constants.DEFAULT_DATA_CHUNK_SIZE;

/**
 * This IO handler works with pre-servlet-3.0 synchronous API (with synchronous IO).
 *
 * @author Zhenkai Zhu
 */
public class SyncIOHandler implements Writer, Reader
{
  private static final Logger LOG = LoggerFactory.getLogger(SyncIOHandler.class);
  protected static final String UNKNOWN_REMOTE_ADDRESS = "unknown";

  private final ServletInputStream _is;
  private final ServletOutputStream _os;
  private final int _maxBufferedChunks;
  private final BlockingQueue<Event> _eventQueue;
  private volatile WriteHandle _wh;
  private volatile ReadHandle _rh;
  private boolean _forceExit;
  private boolean _requestReadFinished;
  private boolean _responseWriteFinished;
  private final long _timeout;
  private final String _remoteAddress;
  private final boolean _logServletExceptions;

  @Deprecated
  public SyncIOHandler(ServletInputStream is, ServletOutputStream os, int maxBufferedChunks, long timeout)
  {
    this(is, os, UNKNOWN_REMOTE_ADDRESS, maxBufferedChunks, timeout, false);
  }

  public SyncIOHandler(ServletInputStream is, ServletOutputStream os, String remoteAddress, int maxBufferedChunks,
      long timeout, boolean logServletExceptions)
  {
    _is = is;
    _os = os;
    _remoteAddress = remoteAddress;
    _maxBufferedChunks = maxBufferedChunks;
    _eventQueue = new LinkedBlockingDeque<>();
    _requestReadFinished = false;
    _responseWriteFinished = false;
    _forceExit = false;
    _timeout = timeout;
    _logServletExceptions = logServletExceptions;
  }

  @Override
  public void onInit(WriteHandle wh)
  {
    _wh = wh;
  }

  @Override
  public void onWritePossible()
  {
    _eventQueue.add(Event.WriteRequestPossibleEvent);
  }

  @Override
  public void onAbort(Throwable ex)
  {
    _eventQueue.add(new Event(EventType.WriteRequestAborted, ex));
  }

  @Override
  public void onInit(ReadHandle rh)
  {
    _rh = rh;
    _rh.request(_maxBufferedChunks);
  }

  @Override
  public void onDataAvailable(ByteString data)
  {
    _eventQueue.add(new Event(EventType.ResponseDataAvailable, data));
  }

  @Override
  public void onDone()
  {
    _eventQueue.add(Event.FullResponseReceivedEvent);
  }

  @Override
  public void onError(Throwable e)
  {
    _eventQueue.add(new Event(EventType.ResponseDataError, e));
  }

  public void loop() throws ServletException, IOException
  {
    final long startTime = System.currentTimeMillis();
    byte[] buf = new byte[DEFAULT_DATA_CHUNK_SIZE];

    while(shouldContinue() && !_forceExit)
    {
      Event event;
      try
      {
        long timeSpent = System.currentTimeMillis() - startTime;
        long maxWaitTime = timeSpent < _timeout ? _timeout - timeSpent : 0;
        event = _eventQueue.poll(maxWaitTime, TimeUnit.MILLISECONDS);
        if (event == null)
        {
          throw new TimeoutException("Timeout after " + _timeout + " milliseconds.");
        }
      }
      catch (Exception ex)
      {
        throw new ServletException(ex);
      }

      switch (event.getEventType())
      {
        case ResponseDataAvailable:
        {
          ByteString data =  (ByteString) event.getData();
          long startWriteTime = System.currentTimeMillis();
          try
          {
            data.write(_os);
          }
          catch (Exception e)
          {
            long writeDuration = System.currentTimeMillis() - startWriteTime;
            if (_logServletExceptions)
            {
              final String message = String.format(
                  "Encountered servlet exception, state=ResponseDataAvailable,remote=%s,duration=%dms",
                  _remoteAddress, writeDuration);
              LOG.info(message, e);
            }
            throw e;
          }

          _rh.request(1);
          break;
        }
        case WriteRequestPossible:
        {
          while (_wh.remaining() > 0)
          {
            final int actualLen;

            long startWriteTime = System.currentTimeMillis();
            try
            {
              actualLen = _is.read(buf);
            }
            catch (Exception e)
            {
              long writeDuration = System.currentTimeMillis() - startWriteTime;
              if (_logServletExceptions)
              {
                final String message = String.format(
                    "Encountered servlet exception, state=WriteRequestPossible,remote=%s,duration=%dms",
                    _remoteAddress, writeDuration);
                LOG.info(message, e);
              }
              throw e;
            }

            if (actualLen < 0)
            {
              _wh.done();
              _requestReadFinished = true;
              break;
            }
            _wh.write(ByteString.copy(buf, 0, actualLen));
          }
          break;
        }
        case FullResponseReceived:
        {
          _os.close();
          _responseWriteFinished = true;
          break;
        }
        case ResponseDataError:
        {
          _os.close();
          _responseWriteFinished = true;
          break;

        }
        case WriteRequestAborted:
        {
          if (event.getData() instanceof AbortedException)
          {
            // reader cancels, we'll drain the stream on behalf of reader
            // we don't directly drain it here because we'd like to give other events
            // some opportunities to be executed; e.g. return an error response
            _eventQueue.add(Event.DrainRequestEvent);
          }
          else
          {
            // reader throws, which it shouldn't do, we cannot do much
            // TODO: do we want to be smarter and return server error response?
            throw new ServletException((Throwable)event.getData());
          }
          break;
        }
        case DrainRequest:
        {
          for (int i = 0; i < 10; i++)
          {
            final int actualLen;

            long startWriteTime = System.currentTimeMillis();
            try
            {
              actualLen = _is.read(buf);
            }
            catch (Exception e)
            {
              long writeDuration = System.currentTimeMillis() - startWriteTime;
              if (_logServletExceptions) {
                final String message = String.format(
                    "Encountered servlet exception, state=DrainRequest,remote=%s,duration=%dms",
                    _remoteAddress, writeDuration);
                LOG.info(message, e);
              }
              throw e;
            }

            if (actualLen < 0)
            {
              _requestReadFinished = true;
              break;
            }
          }
          if (!_requestReadFinished)
          {
            // add self back to event queue and give others a chance to run
            _eventQueue.add(Event.DrainRequestEvent);
          }
          break;
        }
        case ForceExit:
        {
          _forceExit = true;
          break;
        }
        default:
          throw new IllegalStateException("Unknown event type:" + event.getEventType());
      }
    }
  }

  protected boolean shouldContinue()
  {
    return !_responseWriteFinished || !_requestReadFinished;
  }

  protected boolean responseWriteFinished()
  {
    return _responseWriteFinished;
  }

  protected boolean requestReadFinished()
  {
    return _requestReadFinished;
  }

  protected void exitLoop()
  {
    _eventQueue.add(Event.ForceExitEvent);
  }

  private static enum  EventType
  {
    WriteRequestPossible,
    WriteRequestAborted,
    DrainRequest,
    FullResponseReceived,
    ResponseDataAvailable,
    ResponseDataError,
    ForceExit,
  }

  private static class Event
  {
    private final EventType _eventType;
    private final Object _data;

    static final Event WriteRequestPossibleEvent = new Event(EventType.WriteRequestPossible);
    static final Event FullResponseReceivedEvent = new Event(EventType.FullResponseReceived);
    static final Event DrainRequestEvent = new Event(EventType.DrainRequest);
    static final Event ForceExitEvent = new Event(EventType.ForceExit);

    Event(EventType eventType)
    {
      this(eventType, null);
    }

    Event(EventType eventType, Object data)
    {
      _eventType = eventType;
      _data = data;
    }

    EventType getEventType()
    {
      return _eventType;
    }

    Object getData()
    {
      return _data;
    }
  }
}
