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
import com.linkedin.restli.client.response.CreateResponse;
import com.linkedin.restli.common.IdResponse;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Abstract builder class for constructing {@link ResponseFuture}s
 *
 * @author kparikh
 *
 * @param <K> key type of the mocked response
 * @param <V> entity type of the mocked response
 */
public abstract class MockAbstractResponseFutureBuilder<K, V extends RecordTemplate>
{
  private V _entity;
  private int _status;
  private Map<String, String> _headers;
  private ProtocolVersion _protocolVersion;

  protected V getEntity()
  {
    return _entity;
  }

  protected int getStatus()
  {
    return _status;
  }

  protected K getId()
  {
    if (_entity instanceof CreateResponse<?>)
    {
      @SuppressWarnings("unchecked")
      final CreateResponse<K> createResponse = (CreateResponse<K>) _entity;
      return createResponse.getId();
    }
    else if (_entity instanceof IdResponse<?>)
    {
      @SuppressWarnings("unchecked")
      final IdResponse<K> idResponse = (IdResponse<K>) _entity;
      return idResponse.getId();
    }
    else
    {
      return null;
    }
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
  public MockAbstractResponseFutureBuilder<K, V> setEntity(V entity)
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
  public MockAbstractResponseFutureBuilder<K, V> setStatus(int status)
  {
    _status = status;
    return this;
  }

  /**
   * Set the headers
   *
   * @param headers
   * @return
   * @throws IllegalArgumentException when trying to set {@link RestConstants#HEADER_ID} or {@link RestConstants#HEADER_RESTLI_ID}.
   */
  public MockAbstractResponseFutureBuilder<K, V> setHeaders(Map<String, String> headers)
  {
    if (headers != null)
    {
      final String headerName;
      if (headers.containsKey(RestConstants.HEADER_ID))
      {
        headerName = RestConstants.HEADER_ID;
      }
      else if (headers.containsKey(RestConstants.HEADER_RESTLI_ID))
      {
        headerName = RestConstants.HEADER_RESTLI_ID;
      }
      else
      {
        headerName = null;
      }

      if (headerName != null)
      {
        throw new IllegalArgumentException("Illegal to set the \"" + headerName + "\" header. This header is reserved for the ID returned from create method on the resource.");
      }
    }

    _headers = headers;
    return this;
  }

  /**
   * Sets the Rest.li {@link ProtocolVersion}
   *
   * @param protocolVersion
   * @return
   */
  public MockAbstractResponseFutureBuilder<K, V> setProtocolVersion(ProtocolVersion protocolVersion)
  {
    _protocolVersion = protocolVersion;
    return this;
  }

  /**
   * Build a {@link ResponseFuture}
   *
   * @return a {@link ResponseFuture} constructed using the setters
   */
  public abstract ResponseFuture<V> build();

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
