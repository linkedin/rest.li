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

package com.linkedin.r2.message.stream.entitystream.adapter;

import com.linkedin.data.ByteString;
import com.linkedin.r2.message.stream.entitystream.AbortedException;
import com.linkedin.r2.message.stream.entitystream.WriteHandle;
import com.linkedin.r2.message.stream.entitystream.Writer;


/**
 * A Writer of ByteString adapted from a ByteString-specific Writer.
 */
class ByteStringToGenericWriter implements com.linkedin.entitystream.Writer<ByteString>
{
  private final Writer _writer;

  ByteStringToGenericWriter(Writer writer)
  {
    _writer = writer;
  }

  @Override
  public void onInit(com.linkedin.entitystream.WriteHandle<? super ByteString> wh)
  {
    _writer.onInit(new WriteHandle()
    {
      @Override
      public void write(ByteString data)
      {
        wh.write(data);
      }

      @Override
      public void done()
      {
        wh.done();
      }

      @Override
      public void error(Throwable throwable)
      {
        wh.error(throwable);
      }

      @Override
      public int remaining()
      {
        return wh.remaining();
      }
    });
  }

  @Override
  public void onWritePossible()
  {
    _writer.onWritePossible();
  }

  @Override
  public void onAbort(Throwable e)
  {
    if (e.getClass().equals(com.linkedin.entitystream.AbortedException.class))
    {
      e = new AbortedException(e.getMessage(), e);
    }

    _writer.onAbort(e);
  }
}
