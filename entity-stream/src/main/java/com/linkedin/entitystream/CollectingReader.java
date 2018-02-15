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

import com.linkedin.common.callback.Callback;

import java.util.stream.Collector;


/**
 * A {@link Reader} implementation that uses a {@link Collector} to collect the entities in a stream and build a
 * result object.
 *
 * @param <T> the type of entities to collect
 * @param <A> the mutable accumulation type (often hidden as an implementation detail)
 * @param <R> the result type of the reduction operation
 */
public class CollectingReader<T, A, R> implements Reader<T>
{
  private final Collector<? super T, A, ? extends R> _collector;
  private final Callback<R> _callback;
  private ReadHandle _readHandle;
  private A _resultContainer;

  public CollectingReader(Collector<? super T, A, ? extends R> collector, Callback<R> callback)
  {
    _collector = collector;
    _callback = callback;
  }

  @Override
  public void onInit(ReadHandle rh)
  {
    _readHandle = rh;
    _resultContainer = _collector.supplier().get();
    rh.request(1);
  }

  @Override
  public void onDataAvailable(T data)
  {
    try
    {
      _collector.accumulator().accept(_resultContainer, data);
      _readHandle.request(1);
    }
    catch (Throwable e)
    {
      _readHandle.cancel();
      _callback.onError(e);
    }
  }

  @Override
  public void onDone()
  {
    try
    {
      R result = _collector.finisher().apply(_resultContainer);
      _callback.onSuccess(result);
    }
    catch (Throwable e)
    {
      _readHandle.cancel();
      _callback.onError(e);
    }
  }

  @Override
  public void onError(Throwable e)
  {
    _callback.onError(e);
  }
}
