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

/* $Id$ */
package com.linkedin.r2.transport.common.bridge.common;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class TransportResponseImpl<T> implements TransportResponse<T>
{
  private final T _response;
  private final Throwable _error;
  private final Map<String, String> _wireAttrs;

  /**
   * Factory method to construct a new {@link TransportResponse} for a successful response.
   *
   * @param response the response message to be wrapped.
   * @param <T> the Response subclass to be used for this instance.
   * @return a new {@link TransportResponse} which contains the specified response message.
   */
  public static <T> TransportResponse<T> success(T response)
  {
    return new TransportResponseImpl<T>(response, null, new HashMap<String, String>());
  }

  /**
   * Factory method to construct a new {@link TransportResponse} for a successful response.
   *
   * @param response the response message to be wrapped.
   * @param wireAttrs the wire attributes of the response.
   * @param <T> the Response subclass to be used for this instance.
   * @return a new {@link TransportResponse} which contains the specified response message.
   */
  public static <T> TransportResponse<T> success(T response, Map<String, String> wireAttrs)
  {
    return new TransportResponseImpl<T>(response, null, wireAttrs);
  }

  /**
   * Factory method to construct a new {@link TransportResponse} for an error.
   *
   * @param error the {@link Throwable} error to be wrapped.
   * @param wireAttrs the wire attributes of the response.
   * @param <T> the Response subclass to be used for this instance.
   * @return a new {@link TransportResponse} which contains the specified error.
   */
  public static <T> TransportResponse<T> error(Throwable error, Map<String, String> wireAttrs)
  {
    return new TransportResponseImpl<T>(null, error, wireAttrs);
  }

  /**
   * Factory method to construct a new {@link TransportResponse} for an error.
   *
   * @param error the {@link Throwable} error to be wrapped.
   * @param <T> the Response subclass to be used for this instance.
   * @return a new {@link TransportResponse} which contains the specified error.
   */
  public static <T> TransportResponse<T> error(Throwable error)
  {
    return new TransportResponseImpl<T>(null, error, new HashMap<String, String>());
  }

  private TransportResponseImpl(T response, Throwable error, Map<String, String> wireAttrs)
  {
    assert (response != null && error == null) || (response == null && error != null);

    _response = response;
    _error = error;
    _wireAttrs = wireAttrs;
  }

  @Override
  public T getResponse()
  {
    return _response;
  }

  @Override
  public boolean hasError()
  {
    return _error != null;
  }

  @Override
  public Throwable getError()
  {
    return _error;
  }

  @Override
  public Map<String, String> getWireAttributes()
  {
    return _wireAttrs;
  }
}
