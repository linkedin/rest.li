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

import com.linkedin.data.ByteString;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.ReadHandle;
import com.linkedin.r2.message.stream.entitystream.Reader;
import com.linkedin.r2.message.stream.entitystream.WriteHandle;
import com.linkedin.r2.message.stream.entitystream.Writer;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * This class pipes a {@link com.linkedin.r2.message.stream.entitystream.EntityStream} to
 * a different {@link com.linkedin.r2.message.stream.entitystream.EntityStream} in which
 * the data is compressed.
 *
 * @author Ang Xu
 */
abstract class StreamingDeflater implements Reader, Writer
{
  private ReadHandle _rh;
  private WriteHandle _wh;
  private OutputStream _out;
  private BufferedWriterOutputStream _writerOutputStream;
  private volatile boolean _readCancelled = false;

  private final EntityStream _underlying;

  public StreamingDeflater(EntityStream underlying)
  {
    _underlying = underlying;
  }

  /********* Reader Impl *********/

  @Override
  public void onInit(ReadHandle rh)
  {
    _rh = rh;
  }

  @Override
  public void onDataAvailable(ByteString data)
  {
    if (!_readCancelled)
    {
      try
      {
        data.write(_out);
        if (_writerOutputStream.needMore())
        {
          _rh.request(1);
        }
      }
      catch (IOException e)
      {
        _wh.error(e);
        cancel();
      }
    }
    // otherwise, drop bytes.
  }

  @Override
  public void onDone()
  {
    try
    {
      _out.close();
    }
    catch (IOException e)
    {
      _wh.error(e);
    }
  }

  @Override
  public void onError(Throwable e)
  {
    _wh.error(e);
  }

  /********* Writer Impl *********/

  @Override
  public void onInit(WriteHandle wh)
  {
    try
    {
      _wh = wh;
      _writerOutputStream = new BufferedWriterOutputStream();
      _out = createOutputStream(_writerOutputStream);
      _underlying.setReader(this);
    }
    catch (IOException e)
    {
      _wh.error(e);
      cancel();
    }
  }

  @Override
  public void onWritePossible()
  {
    _writerOutputStream.writeIfPossible();
  }

  @Override
  public void onAbort(Throwable e)
  {
    cancel();
  }

  private void cancel()
  {
    _rh.cancel();
    _readCancelled = true;
  }

  abstract protected OutputStream createOutputStream(OutputStream out) throws IOException;

  private class BufferedWriterOutputStream extends OutputStream
  {
    private static final int BUF_SIZE = R2Constants.DEFAULT_DATA_CHUNK_SIZE;

    private final Queue<ByteString> _data = new ConcurrentLinkedQueue<>();
    private final byte[] _buffer = new byte[BUF_SIZE];
    private int _writeIndex = 0;
    private boolean _done = false;

    @Override
    public void write(int b) throws IOException
    {
      _buffer[_writeIndex++] = (byte) b;
      if (_writeIndex == BUF_SIZE)
      {
        _data.add(ByteString.copy(_buffer));
        _writeIndex = 0;
        writeIfPossible();
      }
    }

    @Override
    public void close() throws IOException
    {
      if (_writeIndex > 0) // flush remaining bytes
      {
        _data.add(ByteString.copy(_buffer, 0, _writeIndex));
      }
      _done = true;
      writeIfPossible();
    }

    public synchronized void writeIfPossible()
    {
      while(_wh.remaining() > 0)
      {
        if (_data.isEmpty())
        {
          if (_done)
          {
            _wh.done();
          }
          else
          {
            _rh.request(1);
          }
          return;
        }
        else
        {
          _wh.write(_data.poll());
        }
      }
    }

    public boolean needMore()
    {
      return _wh.remaining() > 0 && _data.isEmpty();
    }
  }
}
