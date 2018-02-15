/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.data;


import com.linkedin.entitystream.Reader;
import com.linkedin.entitystream.WriteHandle;
import com.linkedin.entitystream.Writer;

import java.nio.charset.StandardCharsets;


/**
 * This {@link Writer} implementation writes the byte chunks as @{link ByteString}s of a fixed chunk size. This is
 * useful in testing to allow {@link Reader}s to receive data in multiple chunks.
 *
 */
public class ByteChunkWriter implements Writer<ByteString>
{
  private byte[] _bytes;
  private int _offset;
  private int _chunkSize;
  private WriteHandle<? super ByteString> _writeHandle;

  public ByteChunkWriter(byte[] bytes, int chunkSize)
  {
    _bytes = bytes;
    _chunkSize = chunkSize;
    _offset = 0;
  }

  public ByteChunkWriter(String s, int chunkSize)
  {
    this(s.getBytes(StandardCharsets.UTF_8), chunkSize);
  }

  @Override
  public void onInit(WriteHandle<? super ByteString> wh)
  {
    _writeHandle = wh;
  }

  @Override
  public void onWritePossible()
  {
    while (_writeHandle.remaining() > 0)
    {
      if (_offset < _bytes.length)
      {
        int length = Math.min(_chunkSize, _bytes.length - _offset);
        ByteString chunk = ByteString.copy(_bytes, _offset, length);
        _offset += length;
        _writeHandle.write(chunk);
      }
      else
      {
        _writeHandle.done();
      }
    }
  }

  @Override
  public void onAbort(Throwable ex)
  {
    // Nothing to clean up.
  }
}