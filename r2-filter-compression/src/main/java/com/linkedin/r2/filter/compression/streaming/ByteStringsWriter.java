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
import com.linkedin.r2.message.stream.entitystream.WriteHandle;
import com.linkedin.r2.message.stream.entitystream.Writer;
import java.util.Queue;


/**
 * @author Ang Xu
 */
public class ByteStringsWriter implements Writer
{
  private final Queue<ByteString> _contents;
  private WriteHandle _wh;

  public ByteStringsWriter(Queue<ByteString> contents)
  {
    _contents = contents;
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
      if (_contents.isEmpty())
      {
        _wh.done();
        return;
      }
      else
      {
        _wh.write(_contents.poll());
      }
    }
  }

  @Override
  public void onAbort(Throwable e)
  {

  }
}