/*
   Copyright (c) 2024 LinkedIn Corp.

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

package com.linkedin.common.callback;

import java.util.concurrent.CompletableFuture;


/**
 * A {@link Callback} adapter that wraps a {@link CompletableFuture} and propagates callbacks to it.
 */
public class CompletableFutureCallbackAdapter<T> implements Callback<T>
{
  private final CompletableFuture<T> _future;

  public CompletableFutureCallbackAdapter(CompletableFuture<T> future)
  {
    _future = future;
  }

  @Override
  public void onError(Throwable e)
  {
    _future.completeExceptionally(e);
  }

  @Override
  public void onSuccess(T result)
  {
    _future.complete(result);
  }
}
