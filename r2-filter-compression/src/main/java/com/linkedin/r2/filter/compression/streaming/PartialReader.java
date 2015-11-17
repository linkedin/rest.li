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

package com.linkedin.r2.filter.compression.streaming;

import com.linkedin.common.callback.Callback;
import com.linkedin.data.ByteString;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.ReadHandle;
import com.linkedin.r2.message.stream.entitystream.Reader;
import com.linkedin.r2.message.stream.entitystream.WriteHandle;
import com.linkedin.r2.message.stream.entitystream.Writer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Reads at least specified number of bytes from a {@link com.linkedin.r2.message.stream.entitystream.EntityStream}.
 *
 * @author Ang Xu
 */
public class PartialReader implements Reader
{
  private final int _numBytes;
  private final Callback<EntityStream[]> _callback;

  private final Queue<ByteString> _buffer = new LinkedList<ByteString>();
  private ReadHandle _rh;
  private WriteHandle _remainingWh;
  private int _readLen;
  private int _outstanding;


  public PartialReader(int numBytes, Callback<EntityStream[]> callback)
  {
    _numBytes = numBytes;
    _callback = callback;
    _readLen = 0;
  }

  @Override
  public void onInit(ReadHandle rh)
  {
    _rh = rh;
    _rh.request(1);
  }

  @Override
  public void onDataAvailable(ByteString data)
  {
    if (_remainingWh == null)
    {
      _buffer.add(data);
      _readLen += data.length();

      if (_readLen <= _numBytes)
      {
        _rh.request(1);
      }
      else
      {
        EntityStream stream = EntityStreams.newEntityStream(new ByteStringsWriter(_buffer));
        EntityStream remaining = EntityStreams.newEntityStream(new RemainingWriter());
        _callback.onSuccess(new EntityStream[] {stream, remaining});
      }
    }
    else
    {
      _outstanding--;
      _remainingWh.write(data);
      int diff = _remainingWh.remaining() - _outstanding;
      if (diff > 0)
      {
        _rh.request(diff);
        _outstanding += diff;
      }
    }
  }

  @Override
  public void onDone()
  {
    if (_remainingWh == null)
    {
      EntityStream stream = EntityStreams.newEntityStream(new ByteStringsWriter(_buffer));
      _callback.onSuccess(new EntityStream[] {stream});
    }
    else
    {
      _remainingWh.done();
    }
  }

  @Override
  public void onError(Throwable e)
  {
    if (_remainingWh == null)
    {
      _callback.onError(e);
    }
    else
    {
      _remainingWh.error(e);
    }
  }

  private class RemainingWriter implements Writer
  {
    @Override
    public void onInit(WriteHandle wh)
    {
      _remainingWh = wh;
    }

    @Override
    public void onWritePossible()
    {
      _outstanding = _remainingWh.remaining();
      _rh.request(_outstanding);
    }

    @Override
    public void onAbort(Throwable e)
    {

    }
  }
}
