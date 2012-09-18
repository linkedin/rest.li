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

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.common.RestConstants;


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
  private Map<String, String> _headers;
  private T _entity;

  ResponseImpl(int status, Map<String, String> headers)	{
    _status = status;
    _headers = headers;
  }

  public ResponseImpl(Response<?> origin, T entity)	{
    _status = origin.getStatus();
    _headers = origin.getHeaders();
    _entity = entity;
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

  void setEntity(T entity)
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
   */
  @Override public String getId()
  {
    return getHeader(RestConstants.HEADER_ID);
  }

  /**
   * Specific getter for the 'Location' header
   */
  @Override public URI getLocation()
  {
    return URI.create(getHeader(RestConstants.HEADER_LOCATION));
  }
}
