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

package com.linkedin.multipart.utils;


import com.linkedin.data.ByteString;
import com.linkedin.r2.message.stream.entitystream.WriteHandle;
import com.linkedin.r2.message.stream.entitystream.Writer;
import com.linkedin.util.ArgumentUtil;

import org.testng.Assert;


/**
 * Writes bytes out from a {@link com.linkedin.data.ByteString} based on customizable chunk sizes.
 *
 * @author Karim Vidhani
 */
public class VariableByteStringWriter implements Writer
{
  private final ByteString _content;
  private final int _chunkSize;
  private int _offset;
  private WriteHandle _wh;

  public VariableByteStringWriter(final ByteString content, final int chunkSize)
  {
    ArgumentUtil.notNull(content, "content");
    _content = content;
    _chunkSize = chunkSize;
    _offset = 0;
  }

  @Override
  public void onInit(WriteHandle wh)
  {
    _wh = wh;
  }

  @Override
  public void onWritePossible()
  {
    while (_wh.remaining() > 0)
    {
      if (_offset == _content.length())
      {
        _wh.done();
        break;
      }
      int bytesToWrite = Math.min(_chunkSize, _content.length() - _offset);
      _wh.write(_content.slice(_offset, bytesToWrite));
      _offset += bytesToWrite;
    }
  }

  @Override
  public void onAbort(Throwable ex)
  {
    Assert.fail();
  }
}