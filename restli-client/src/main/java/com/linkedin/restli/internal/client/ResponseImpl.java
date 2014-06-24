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


import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.client.response.CreateResponse;
import com.linkedin.restli.common.IdResponse;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.ProtocolVersionUtil;
import com.linkedin.restli.internal.common.URIParamUtils;

import java.net.URI;
import java.util.Collections;
import java.util.Map;


/**
 * Response for a REST request.
 *
 * @param <T> response entity template class
 *
 * @author dellamag
 * @author Eran Leshem
 */
public class ResponseImpl<T> implements Response<T>
{
  private int _status = 102;  // SC_PROCESSING
  private final Map<String, String> _headers;
  private T _entity;
  private RestLiResponseException _error;

  ResponseImpl(Response<T> origin, RestLiResponseException error)
  {
    this(origin, origin.getEntity());
    _error = error;
  }

  ResponseImpl(int status, Map<String, String> headers, RestLiResponseException error)
  {
    this(status, headers);
    _error = error;
  }

  public ResponseImpl(Response<?> origin, T entity)
  {
    this(origin.getStatus(), origin.getHeaders());
    _entity = entity;
  }

  public ResponseImpl(int status,
                      Map<String, String> headers,
                      T entity,
                      RestLiResponseException error)
  {
    this(status, headers);
    _entity = entity;
    _error = error;
  }

  ResponseImpl(int status, Map<String, String> headers)
  {
    _status = status;
    _headers = headers;
  }

  /**
   * Returns response status.
   */
  @Override public int getStatus()
  {
    return _status;
  }

  /**
   * Returns response body entity.
   */
  @Override public T getEntity()
  {
    return _entity;
  }

  public void setEntity(T entity)
  {
    _entity = entity;
  }

  /**
   * Returns a response header.
   *
   * @param name header name
   */
  @Override public String getHeader(String name)
  {
    return _headers.get(name);
  }

  @Override
  public Map<String, String> getHeaders()
  {
    return Collections.unmodifiableMap(_headers);
  }

  /**
   * Specific getter for the 'X-LinkedIn-Id' header
   *
   * @deprecated
   * @see {@link com.linkedin.restli.client.Response#getId()}
   */
  @Override
  @Deprecated
  public String getId()
  {
    if (_entity instanceof CreateResponse<?> || _entity instanceof IdResponse<?>)
    {
      final Object key;
      if (_entity instanceof CreateResponse<?>)
      {
        key = ((CreateResponse<?>) _entity).getId();
      }
      else
      {
        key = ((IdResponse<?>) _entity).getId();
      }
      final ProtocolVersion protocolVersion = ProtocolVersionUtil.extractProtocolVersion(_headers);
      return URIParamUtils.encodeKeyForHeader(key, protocolVersion);
    }
    else
    {
      return null;
    }
  }

  /**
   * Specific getter for the 'Location' header
   */
  @Override
  public URI getLocation()
  {
    return URI.create(getHeader(RestConstants.HEADER_LOCATION));
  }

  @Override
  public RestLiResponseException getError()
  {
    return _error;
  }

  @Override
  public boolean hasError()
  {
    return _error != null;
  }
}
