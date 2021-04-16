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
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.attachments.RestLiAttachmentReader;

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
  private final int _status;
  private final TreeMap<String, String> _headers;
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
    _headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    _headers.putAll(headers);
    _cookies = new ArrayList<>(cookies);
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
    return Collections.unmodifiableSortedMap(_headers);
  }

  @Override
  public List<HttpCookie> getCookies()
  {
    return Collections.unmodifiableList(_cookies);
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
