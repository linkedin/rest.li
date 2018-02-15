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
import com.linkedin.r2.message.stream.entitystream.ReadHandle;
import com.linkedin.r2.message.stream.entitystream.Reader;


/**
 * A ByteString-specific Reader adapted from a Reader of ByteString.
 */
class GenericToByteStringReader implements Reader
{
  private final com.linkedin.entitystream.Reader<? super ByteString> _reader;

  GenericToByteStringReader(com.linkedin.entitystream.Reader<? super ByteString> reader)
  {
    _reader = reader;
  }

  @Override
  public void onInit(ReadHandle rh)
  {
    _reader.onInit(new com.linkedin.entitystream.ReadHandle()
    {
      @Override
      public void request(int n)
      {
        rh.request(n);
      }

      @Override
      public void cancel()
      {
        rh.cancel();
      }
    });
  }

  @Override
  public void onDataAvailable(ByteString data)
  {
    _reader.onDataAvailable(data);
  }

  @Override
  public void onDone()
  {
    _reader.onDone();
  }

  @Override
  public void onError(Throwable e)
  {
    _reader.onError(e);
  }
}
