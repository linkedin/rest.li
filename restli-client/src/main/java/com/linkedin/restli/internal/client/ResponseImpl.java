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
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.IdEntityResponse;
import com.linkedin.restli.common.attachments.RestLiAttachmentReader;
import com.linkedin.restli.internal.common.ProtocolVersionUtil;
import com.linkedin.restli.internal.common.URIParamUtils;

import java.net.HttpCookie;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;


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
  private final List<HttpCookie> _cookies;
  private T _entity;
  private RestLiResponseException _error;
  private RestLiAttachmentReader  _attachmentReader;

  ResponseImpl(Response<T> origin, RestLiResponseException error)
  {
    this(origin, origin.getEntity());
    _error = error;
  }

  ResponseImpl(int status,
               Map<String, String> headers,
               List<HttpCookie> cookies,
               RestLiResponseException error)
  {
    this(status, headers, cookies);
    _error = error;
  }

  public ResponseImpl(Response<?> origin, T entity)
  {
    this(origin.getStatus(), origin.getHeaders(), origin.getCookies());
    _entity = entity;
  }

  public ResponseImpl(int status, Map<String, String> headers, List<HttpCookie> cookies, T entity, RestLiResponseException error)
  {
    this(status, headers, cookies);
    _entity = entity;
    _error = error;
  }

  ResponseImpl(int status, Map<String, String> headers, List<HttpCookie> cookies)
  {
    _status = status;
    _headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    _headers.putAll(headers);
    _cookies = new ArrayList<HttpCookie>(cookies);
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

  @Override
  public List<HttpCookie> getCookies()
  {
    return Collections.unmodifiableList(_cookies);
  }

  /**
   * Specific getter for the 'X-LinkedIn-Id' header
   *
   * @throws UnsupportedOperationException if the entity returned is a {@link CreateResponse} or {@link IdResponse}
   * and the key is a {@link ComplexResourceKey} or {@link CompoundKey}.
   *
   * @deprecated
   * @see {@link com.linkedin.restli.client.Response#getId()}
   */
  @Override
  @Deprecated
  public String getId()
  {
    if (_entity instanceof CreateResponse<?> || _entity instanceof IdResponse<?> || _entity instanceof IdEntityResponse<?, ?>)
    {
      final Object id = checkAndReturnId();
      final ProtocolVersion protocolVersion = ProtocolVersionUtil.extractProtocolVersion(_headers);
      return URIParamUtils.encodeKeyForHeader(id, protocolVersion);
    }
    else
    {
      return null;
    }
  }

  /**
   * Checks if getId() is supported for this type of {@link Response} and returns the ID if it is.
   *
   * @return The ID if it is supported.
   */
  private Object checkAndReturnId()
  {
    final Object id;
    final String castMessage;

    if (_entity instanceof CreateResponse)
    {
      CreateResponse<?> createResponse = (CreateResponse<?>)_entity;
      id = createResponse.getId();
      castMessage = "CreateResponse";
    }
    else if (_entity instanceof IdEntityResponse)
    {
      IdEntityResponse<?, ?> idEntityResponse = (IdEntityResponse<?, ?>)_entity;
      id = idEntityResponse.getId();
      castMessage = "IdEntityResponse";
    }
    else
    {
      IdResponse<?> idResponse = (IdResponse<?>)_entity;
      id = idResponse.getId();
      castMessage = "IdResponse";
    }
    if (id instanceof CompoundKey || id instanceof ComplexResourceKey)
    {
      String baseErrorMessage = "Cannot call getId() for complex or compound keys! Please cast the object returned by" +
          " getEntity() to a %s and call the getId() method on that.";
      throw new UnsupportedOperationException(String.format(baseErrorMessage, castMessage));
    }
    return id;
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

  @Override
  public boolean hasAttachments()
  {
    return _attachmentReader != null;
  }

  @Override
  public RestLiAttachmentReader getAttachmentReader()
  {
    return _attachmentReader;
  }

  public void setAttachmentReader(RestLiAttachmentReader attachmentReader)
  {
    _attachmentReader = attachmentReader;
  }
}
