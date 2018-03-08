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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
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

  private ReadHandle _readHandle;
  private CompletableFuture<R> _completable;
  private A _intermediateResult;

  public CollectingReader(Collector<? super T, A, ? extends R> collector)
  {
    _collector = collector;
  }

  @Override
  public void onInit(ReadHandle rh)
  {
    _readHandle = rh;
    _completable = new CompletableFuture<>();

    try
    {
      _intermediateResult = _collector.supplier().get();
    }
    catch (Throwable e)
    {
      handleException(e);
    }

    rh.request(1);
  }

  @Override
  public void onDataAvailable(T data)
  {
    try
    {
      _collector.accumulator().accept(_intermediateResult, data);
    }
    catch (Throwable e)
    {
      handleException(e);
    }

    _readHandle.request(1);
  }

  @Override
  public void onDone()
  {
    R result;
    try
    {
      result = _collector.finisher().apply(_intermediateResult);
    }
    catch (Throwable e)
    {
      handleException(e);
      return;
    }

    _completable.complete(result);
  }

  @Override
  public void onError(Throwable e)
  {
    // No need to cancel reading.
    _completable.completeExceptionally(e);
  }

  private void handleException(Throwable e)
  {
    _readHandle.cancel();
    _completable.completeExceptionally(e);
  }

  public CompletionStage<R> getResult()
  {
    return _completable;
  }
}
