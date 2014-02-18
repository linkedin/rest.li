/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.client.testutils;


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.common.ProtocolVersion;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Abstract builder class for constructing {@link ResponseFuture}s
 *
 * @author kparikh
 */
public abstract class MockAbstractResponseFutureBuilder<T extends RecordTemplate>
{
  private T _entity;
  private int _status;
  private String _id;
  private Map<String, String> _headers;
  private ProtocolVersion _protocolVersion;

  protected T getEntity()
  {
    return _entity;
  }

  protected int getStatus()
  {
    return _status;
  }

  protected String getId()
  {
    return _id;
  }

  protected Map<String, String> getHeaders()
  {
    return _headers;
  }

  protected ProtocolVersion getProtocolVersion()
  {
    return _protocolVersion;
  }

  /**
   * Set the entity
   *
   * @param entity
   * @return
   */
  public MockAbstractResponseFutureBuilder<T> setEntity(T entity)
  {
    _entity = entity;
    return this;
  }

  /**
   * Set the HTTP status code
   *
   * @param status
   * @return
   */
  public MockAbstractResponseFutureBuilder<T> setStatus(int status)
  {
    _status = status;
    return this;
  }

  /**
   * Set the ID.
   *
   * @param id
   * @return
   */
  public MockAbstractResponseFutureBuilder<T> setId(String id)
  {
    _id = id;
    return this;
  }

  /**
   * Set the headers
   *
   * @param headers
   * @return
   */
  public MockAbstractResponseFutureBuilder<T> setHeaders(Map<String, String> headers)
  {
    _headers = headers;
    return this;
  }

  /**
   * Sets the Rest.li {@link ProtocolVersion}
   *
   * @param protocolVersion
   * @return
   */
  public MockAbstractResponseFutureBuilder<T> setProtocolVersion(ProtocolVersion protocolVersion)
  {
    _protocolVersion = protocolVersion;
    return this;
  }

  /**
   * Build a {@link ResponseFuture}
   *
   * @return a {@link ResponseFuture} constructed using the setters
   */
  public abstract ResponseFuture<T> build();

  /**
   * Wraps a {@link Response} in a {@link Future}
   *
   * @param response the {@link Response} to wrap
   * @param exception the exception we want to throw for {@link java.util.concurrent.Future#get()} or
   *                  {@link Future#get(long, java.util.concurrent.TimeUnit)}
   * @param <T>
   * @return
   */
  /*package private*/static <T> Future<Response<T>> buildFuture(final Response<T> response, final ExecutionException exception)
  {
    return new Future<Response<T>>()
    {
      @Override
      public boolean cancel(boolean mayInterruptIfRunning)
      {
        return false;
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

      @Override
      public Response<T> get()
          throws InterruptedException, ExecutionException
      {
        if (exception != null)
        {
          throw exception;
        }
        return response;
      }

      @Override
      public Response<T> get(long timeout, TimeUnit unit)
          throws InterruptedException, ExecutionException, TimeoutException
      {
        return get();
      }
    };
  }
}
