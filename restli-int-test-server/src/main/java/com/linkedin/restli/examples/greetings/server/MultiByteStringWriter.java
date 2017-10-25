/*
 * Copyright (c) 2017 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.linkedin.restli.examples.greetings.server;


import com.linkedin.data.ByteString;
import com.linkedin.r2.message.stream.entitystream.WriteHandle;
import com.linkedin.r2.message.stream.entitystream.Writer;
import com.linkedin.util.ArgumentUtil;


/**
 * A writer that produce content based on the ByteString body in multiple writes.
 */
public class MultiByteStringWriter implements Writer
{
  private static final int PART_LENGTH = 2;  // 2 bytes for each write

  private WriteHandle _wh;
  private final ByteString _content;
  private final int _total;
  private int _offset = 0;                   // current content offset pointer, start with 0, end at _total

  public MultiByteStringWriter(ByteString content)
  {
    ArgumentUtil.notNull(content, "content");
    _content = content;
    _total = content.length();
  }

  @Override
  public void onInit(WriteHandle wh)
  {
    _wh = wh;
  }

  @Override
  public void onWritePossible()
  {
    // Write more
    while(_wh.remaining() >  0 && _offset < _total)
    {
      int min = Math.min(PART_LENGTH, _total - _offset);
      _wh.write(_content.copySlice(_offset, min));
      _offset += min;
    }

    // Wrote everything
    if (_offset >= _total)
    {
      _wh.done();
    }
  }

  @Override
  public void onAbort(Throwable ex)
  {
    // do nothing
  }
}