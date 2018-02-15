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

package com.linkedin.entitystream;

/**
 * A {@link Writer} implementation that just writes provided entity once to the stream.
 *
 * @param <T> The entity type.
 */
public class SingletonWriter<T> implements Writer<T>
{
  private T _entity;
  private WriteHandle<? super T> _writeHandle;
  private boolean _done;

  public SingletonWriter(T entity)
  {
    _entity = entity;
    _done = false;
  }

  @Override
  public void onInit(WriteHandle<? super T> wh)
  {
    _writeHandle = wh;
  }

  @Override
  public void onWritePossible()
  {
    while (_writeHandle.remaining() > 0)
    {
      if (!_done)
      {
        _done = true;
        _writeHandle.write(_entity);
      }
      else
      {
        _writeHandle.done();
      }
    }
  }

  @Override
  public void onAbort(Throwable e)
  {
    // Nothing to clean up.
  }
}
