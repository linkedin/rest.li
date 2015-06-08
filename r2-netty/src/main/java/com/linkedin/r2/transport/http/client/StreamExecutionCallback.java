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

/**
 * $Id: $
 */

package com.linkedin.r2.transport.http.client;

import com.linkedin.data.ByteString;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.ReadHandle;
import com.linkedin.r2.message.stream.entitystream.Reader;
import com.linkedin.r2.message.stream.entitystream.WriteHandle;
import com.linkedin.r2.message.stream.entitystream.Writer;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A TransportCallback wrapper which ensures the #onResponse() method of the
 * wrapped callback is always invoked by the dedicated {@link ExecutorService}.
 *
 * @author Ang Xu
 * @version $Revision: $
 */
public class StreamExecutionCallback implements TransportCallback<StreamResponse>
{
  private static final Logger LOG = LoggerFactory.getLogger(StreamExecutionCallback.class);

  private final ExecutorService _executor;
  private AtomicReference<TransportCallback<StreamResponse>>  _callbackRef;
  private final Queue<Runnable> _taskQueue = new LinkedBlockingQueue<Runnable>();
  private final AtomicInteger _pending = new AtomicInteger(0);

  private final Runnable _eventLoop = new Runnable()
  {
    @Override
    public void run()
    {
      Runnable r = _taskQueue.poll();
      try
      {
        r.run();
      }
      catch (Throwable t)
      {
        LOG.error("Unexpected throwable in eventLoop.", t);
        Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), t);
      }
      finally
      {
        if (_pending.decrementAndGet() > 0)
        {
          _executor.execute(_eventLoop);
        }
      }
    }
  };

  /**
   * Construct a new instance.
   *
   * @param executor the {@link ExecutorService} used to execute the given {@link TransportCallback}.
   * @param callback the {@link TransportCallback} to be invoked on success or error.
   */
  public StreamExecutionCallback(ExecutorService executor, TransportCallback<StreamResponse> callback)
  {
    _executor = executor;
    _callbackRef = new AtomicReference<TransportCallback<StreamResponse>>(callback);
  }

  private void trySchedule(Runnable r)
  {
    _taskQueue.add(r);
    if (_pending.incrementAndGet() == 1)
    {
      _executor.execute(_eventLoop);
    }
  }

  @Override
  public void onResponse(TransportResponse<StreamResponse> response)
  {
    final TransportCallback<StreamResponse> callback = _callbackRef.getAndSet(null);
    if (callback != null)
    {
      final TransportResponse<StreamResponse> wrappedResponse;
      if (response.hasError())
      {
        wrappedResponse = response;
      }
      else
      {
        EventLoopConnector connector = new EventLoopConnector(response.getResponse().getEntityStream());
        StreamResponse newResponse = response.getResponse().builder().build(EntityStreams.newEntityStream(connector));
        wrappedResponse = TransportResponseImpl.success(newResponse, response.getWireAttributes());
      }

      trySchedule(new Runnable()
      {
        @Override
        public void run()
        {
          callback.onResponse(wrappedResponse);
        }
      });
    }
    else
    {
      LOG.warn("Received response {} while _callback is null. Ignored.", response.getResponse());
    }
  }


  private class EventLoopConnector implements Reader, Writer
  {
    private WriteHandle _wh;
    private ReadHandle _rh;
    private volatile int _outstanding;
    private volatile boolean _aborted;

    private final EntityStream _underlying;

    public EventLoopConnector(EntityStream underlying)
    {
      _outstanding = 0;
      _aborted = false;
      _underlying = underlying;
    }

    @Override
    public void onInit(ReadHandle rh)
    {
      _rh = rh;
    }

    @Override
    public void onInit(final WriteHandle wh)
    {
      _wh = wh;
      _underlying.setReader(this);
    }


    @Override
    public void onDataAvailable(final ByteString data)
    {
      if (!_aborted)
      {
        trySchedule(new Runnable()
        {
          @Override
          public void run()
          {
            _outstanding--;
            _wh.write(data);
            int diff = _wh.remaining() - _outstanding;
            if (diff > 0)
            {
              _rh.request(diff);
              _outstanding += diff;
            }
          }
        });
      }
    }

    @Override
    public void onDone()
    {
      trySchedule(new Runnable()
      {
        @Override
        public void run()
        {
          _wh.done();
        }
      });
    }

    @Override
    public void onError(final Throwable e)
    {
      trySchedule(new Runnable()
      {
        @Override
        public void run()
        {
          _wh.error(e);
        }
      });
    }

    @Override
    public void onWritePossible()
    {
      _outstanding = _wh.remaining();
      _rh.request(_outstanding);
    }

    @Override
    public void onAbort(Throwable e)
    {
      _aborted = true;
      _rh.cancel();
    }
  }
}
