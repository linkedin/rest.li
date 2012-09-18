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

package com.linkedin.restli.internal.client;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestLiDecodingException;
import com.linkedin.restli.common.ErrorResponse;


/**
 * Exposes the response from a REST operation to the client.
 *
 * @param <T> response entity template class
 *
 * @author Eran Leshem
 */
public class ResponseFutureImpl<T> implements ResponseFuture<T>
{
  private final Future<Response<T>> _responseFuture;


  public ResponseFutureImpl(Future<Response<T>> responseFuture)
  {
    _responseFuture = responseFuture;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning)
  {
    return _responseFuture.cancel(mayInterruptIfRunning);
  }

  @Override
  public boolean isCancelled()
  {
    return _responseFuture.isCancelled();
  }

  @Override
  public boolean isDone()
  {
    return _responseFuture.isDone();
  }

  @Override
  public Response<T> get() throws ExecutionException, InterruptedException
  {
    return _responseFuture.get();
  }

  @Override
  public Response<T> get(long timeout, TimeUnit unit) throws ExecutionException, TimeoutException, InterruptedException
  {
    return _responseFuture.get(timeout, unit);
  }

  @Override
  public Response<T> getResponse() throws RemoteInvocationException
  {
    try
    {
      return getResponseImpl(FutureDereferenceStrategy.IGNORE_TIMEOUT, 0, TimeUnit.MILLISECONDS);
    }
    catch (TimeoutException e)
    {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public Response<T> getResponse(long timeout, TimeUnit unit)
          throws RemoteInvocationException, TimeoutException
  {
    return getResponseImpl(FutureDereferenceStrategy.USE_TIMEOUT, timeout, unit);
  }


  private Response<T> getResponseImpl(FutureDereferenceStrategy dereferenceStrategy, long timeout, TimeUnit unit)
          throws TimeoutException, RemoteInvocationException
  {
    try
    {
      if (dereferenceStrategy.equals(FutureDereferenceStrategy.IGNORE_TIMEOUT))
      {
        return _responseFuture.get();
      }
      else
      {
        return _responseFuture.get(timeout, unit);
      }
    }
    catch (InterruptedException e)
    {
      throw new RemoteInvocationException(e);
    }
    catch (ExecutionException e)
    {
      // Always need to wrap it, because the cause likely came from a different thread
      // and is not suitable to throw on the current thread.
      throw ExceptionUtil.exceptionForThrowable(e.getCause(), true);
    }
  }

  @Override
  public String toString()
  {
    return "ResponseFutureImpl{_responseFuture=" + _responseFuture + '}';
  }

  private enum FutureDereferenceStrategy
  {
    USE_TIMEOUT,
    IGNORE_TIMEOUT
  }
}
