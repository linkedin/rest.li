/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.r2.message.stream.entitystream;

import com.linkedin.common.callback.Callback;
import com.linkedin.data.ByteString;


/**
 * Observes and buffers the {@link EntityStream} and invokes the callback with the buffered {@link ByteString}
 * when the EntityStream is done or the exception when the EntityStream encounters an error.
 */
public class FullEntityObserver implements Observer
{
  private final ByteString.Builder _builder;
  private final Callback<ByteString> _callback;

  /**
   * @param callback the callback to be invoked when the reader finishes assembling the full entity
   */
  public FullEntityObserver(Callback<ByteString> callback)
  {
    _callback = callback;
    _builder = new ByteString.Builder();
  }

  @Override
  public void onDataAvailable(ByteString data)
  {
    _builder.append(data);
  }

  @Override
  public void onDone()
  {
    final ByteString entity = _builder.build();
    _callback.onSuccess(entity);
  }

  @Override
  public void onError(Throwable ex)
  {
    _callback.onError(ex);
  }
}
