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


import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.client.response.CreateResponse;
import com.linkedin.restli.common.IdResponse;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.client.ResponseImpl;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.HeaderUtil;
import com.linkedin.restli.internal.common.URIParamUtils;

import java.net.HttpCookie;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Collections;


/**
 * Builds a {@link Response} that can be used for tests.
 *
 * @author jflorencio
 * @author kparikh
 *
 * @param <K> key type of the mocked response
 * @param <V> entity type of the mocked response
 */
public class MockResponseBuilder<K, V>
{
  private V _entity;
  private int _status;
  private Map<String, String> _headers;
  private List<HttpCookie> _cookies;
  private RestLiResponseException _restLiResponseException;
  private ProtocolVersion _protocolVersion;

  private static final int DEFAULT_HTTP_STATUS = 200;

  /**
   * Set the entity
   *
   * @param entity the entity for the {@link Response}
   * @return
   */
  public MockResponseBuilder<K, V> setEntity(V entity)
  {
    _entity = entity;
    return this;
  }

  /**
   * Set the HTTP status code for the {@link Response}
   *
   * @param status the status code for the {@link Response}
   * @return
   */
  public MockResponseBuilder<K, V> setStatus(int status)
  {
    _status = status;
    return this;
  }

  /**
   * Set the headers for the {@link Response}
   *
   * @param headers the headers for the {@link Response}
   * @return
   * @throws IllegalArgumentException when trying to set {@link RestConstants#HEADER_ID} or {@link RestConstants#HEADER_RESTLI_ID}.
   */
  public MockResponseBuilder<K, V> setHeaders(Map<String, String> headers)
  {
    if (headers != null && (headers.containsKey(RestConstants.HEADER_ID) || headers.containsKey(RestConstants.HEADER_RESTLI_ID)))
    {
      throw new IllegalArgumentException("Illegal to directly access the ID header");
    }

    _headers = headers;
    return this;
  }

  public MockResponseBuilder<K, V> setCookies(List<HttpCookie> cookies)
  {
    _cookies = cookies == null ? Collections.<HttpCookie>emptyList(): cookies;
    return this;
  }

  /**
   * Set the {@link RestLiResponseException} for the {@link Response}
   *
   * @param restLiResponseException the {@link RestLiResponseException} for the {@link Response}
   * @return
   */
  public MockResponseBuilder<K, V> setRestLiResponseException(RestLiResponseException restLiResponseException)
  {
    _restLiResponseException = restLiResponseException;
    return this;
  }

  /**
   * Set the {@link ProtocolVersion} for the {@link Response}
   *
   * @param protocolVersion the {@link ProtocolVersion} for the {@link Response}
   * @return
   */
  public MockResponseBuilder<K, V> setProtocolVersion(ProtocolVersion protocolVersion)
  {
    _protocolVersion = protocolVersion;
    return this;
  }

  /**
   * Builds a {@link Response} that has been constructed using the setters in this class.
   *
   * @return the constructed {@link Response}
   */
  public Response<V> build()
  {
    Map<String, String> headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    if (_headers != null)
    {
      headers.putAll(_headers);
    }

    ProtocolVersion protocolVersion = (_protocolVersion == null) ?
        AllProtocolVersions.BASELINE_PROTOCOL_VERSION : _protocolVersion;
    headers.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, protocolVersion.toString());

    int status = (_status == 0) ? DEFAULT_HTTP_STATUS : _status;

    if (_entity instanceof CreateResponse || _entity instanceof IdResponse)
    {
      final K id;
      if (_entity instanceof CreateResponse)
      {
        @SuppressWarnings("unchecked")
        final CreateResponse<K> createResponse = (CreateResponse<K>) _entity;
        id = createResponse.getId();
      }
      else
      {
        @SuppressWarnings("unchecked")
        final IdResponse<K> idResponse = (IdResponse<K>) _entity;
        id = idResponse.getId();
      }
      headers.put(HeaderUtil.getIdHeaderName(protocolVersion), URIParamUtils.encodeKeyForBody(id, false, protocolVersion));
    }
    List<HttpCookie> cookies = _cookies == null ? Collections.<HttpCookie>emptyList() : _cookies;

    return new ResponseImpl<V>(status, headers, cookies, _entity, _restLiResponseException);
  }
}
