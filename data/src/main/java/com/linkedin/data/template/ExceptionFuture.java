/*
   Copyright (c) 2012 LinkedIn Corp.

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

package com.linkedin.data.template;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ExceptionFuture<V> implements Future<V>
{
  public Exception _exception;

  public ExceptionFuture(Exception exception)
  {
    _exception = exception;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning)
  {
    return false;
  }

  @Override
  public V get() throws InterruptedException,
      ExecutionException
  {
    throw new ExecutionException(_exception);
  }

  @Override
  public V get(long arg0, TimeUnit arg1) throws InterruptedException,
      ExecutionException,
      TimeoutException
  {
    throw new ExecutionException(_exception);
  }

  @Override
  public boolean isCancelled()
  {
    return false;
  }

  @Override
  public boolean isDone()
  {
    return true;
  }
}