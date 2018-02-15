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
import com.linkedin.r2.message.stream.entitystream.Observer;


/**
 * A ByteString-specific Observer adapted from an Observer of ByteString.
 */
class GenericToByteStringObserver implements Observer
{
  private final com.linkedin.entitystream.Observer<? super ByteString> _observer;

  GenericToByteStringObserver(com.linkedin.entitystream.Observer<? super ByteString> observer)
  {
    _observer = observer;
  }

  @Override
  public void onDataAvailable(ByteString data)
  {
    _observer.onDataAvailable(data);
  }

  @Override
  public void onDone()
  {
    _observer.onDone();
  }

  @Override
  public void onError(Throwable e)
  {
    if (e.getClass().equals(AbortedException.class))
    {
      e = new com.linkedin.entitystream.AbortedException(e.getMessage(), e);
    }

    _observer.onError(e);
  }
}
