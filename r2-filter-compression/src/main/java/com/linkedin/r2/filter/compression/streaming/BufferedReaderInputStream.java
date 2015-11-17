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
import com.linkedin.r2.message.stream.entitystream.ReadHandle;
import com.linkedin.r2.message.stream.entitystream.Reader;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


/**
 * An {@link InputStream} backed by an {@link com.linkedin.r2.message.stream.entitystream.EntityStream}.
 * Note: access to {@link InputStream} APIs are not thread-safe!
 *
 * @author Ang Xu
 */
class BufferedReaderInputStream extends InputStream implements Reader
{
  private static final int CAPACITY = 3;
  private static final ByteString EOS = ByteString.copy(new byte[1]);

  private final BlockingQueue<ByteString> _buffers = new ArrayBlockingQueue<ByteString>(CAPACITY+1);

  private boolean _closed = false;
  private volatile boolean _readFinished = false;
  private volatile Throwable _throwable = null;

  private byte[] _buffer = null;
  private int _readIndex = 0;
  private ReadHandle _rh;

  /********* InputStream Impl *********/

  @Override
  public int read() throws IOException
  {

    if (_throwable != null)
    {
      throw new IOException(_throwable);
    }
    else if (done())
    {
      return -1;
    }
    else if (_buffer != null) // fast path
    {
      int b = _buffer[_readIndex++] & 0xff;
      if (_readIndex >= _buffer.length)
      {
        _buffer = null;
        _readIndex = 0;
      }
      return b;
    }

    try
    {
      // if we reach here, it means there is no bytes available at this moment
      // and we haven't finished reading EntityStream yet. So we have to block
      // waiting for more bytes.
      final ByteString data = _buffers.take();
      if (data != EOS)
      {
        _buffer = data.copyBytes();
        _rh.request(1);
      }
    }
    catch (InterruptedException ex)
    {
      _throwable = ex;
    }
    // recursively call read() with the belief that
    // it should return immediately this time.
    return read();
  }

  @Override
  public int available()
  {
    int avail = _buffer == null ? 0 : _buffer.length - _readIndex;
    for (ByteString b : _buffers)
    {
      avail += b.length();
    }
    return avail;
  }

  @Override
  public void close()
  {
    _closed = true;
    _rh.cancel();
  }

  /**
   * Returns true if we have finished reading the backing EntityStream
   * and all buffered bytes have been read.
   */
  private boolean done()
  {
    return _closed ||
        (_readFinished && _buffer == null && _buffers.isEmpty());
  }

  /********* Reader Impl *********/

  @Override
  public void onInit(ReadHandle rh)
  {
    _rh = rh;
    _rh.request(CAPACITY);
  }

  @Override
  public void onDataAvailable(ByteString data)
  {
    _buffers.add(data);
  }

  @Override
  public void onDone()
  {
    _readFinished = true;
    // signal waiters that are waiting on _buffers.take().
    _buffers.add(EOS);
  }

  @Override
  public void onError(Throwable e)
  {
    _throwable = e;
    // signal waiters that are waiting on _buffers.take().
    _buffers.add(EOS);
  }
}
