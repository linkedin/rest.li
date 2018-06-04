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

package com.linkedin.restli.internal.server.response;

import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.IdResponse;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.internal.common.HeaderUtil;
import com.linkedin.restli.internal.common.ProtocolVersionUtil;
import com.linkedin.restli.internal.common.URIParamUtils;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * This class represents a Rest.li response object model before encoding with the payload represented by a
 * {@link RecordTemplate}.
 *
 * @author Josh Walker
 * @author nshankar
 */
public class RestLiResponse
{
  private final HttpStatus _status;
  private final RecordTemplate _record;
  private final Map<String, String> _headers;
  private final List<HttpCookie> _cookies;

  /**
   * Constructor is made private intentionally. Use builder to construct a new object of
   * RestLiResponse.
   *
   * @param status
   *          http response status
   * @param record
   *          response data
   * @param headers
   *          Response headers.
   * @param cookies
   */
  private RestLiResponse(final HttpStatus status, final RecordTemplate record, final Map<String, String> headers, final List<HttpCookie> cookies)
  {
    _record = record;
    _status = status;
    _cookies = cookies == null ? new ArrayList<>() : cookies;
    _headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    if (headers != null)
    {
      _headers.putAll(headers);
    }
  }

  /**
   * Obtain a mutable reference to the response headers.
   *
   * @return Reference to response header map.
   */
  public Map<String, String> getHeaders()
  {
    return _headers;
  }

  /**
   * Obtain a mutable reference to the response cookies.
   *
   * @return a cookie reference
   */
  public List<HttpCookie> getCookies()
  {
    return _cookies;
  }

  /**
   * Get value of a specific header.
   *
   * @param headerName
   *          Name of the header for which value is requested.
   * @return Value corresponding to the given header name. Null if no value is defined for the given
   *         header name.
   */
  public String getHeader(String headerName)
  {
    return _headers.get(headerName);
  }

  /**
   * @return true if response contains data, null otherwise.
   */
  public boolean hasData()
  {
    return _record != null && _record.data() != null;
  }

  /**
   * Obtain a reference to the underlying {@link DataMap} corresponding to the entity.
   *
   * @return Reference to the {@link DataMap} corresponding to the entity is entity is not null;
   *         else null.
   */
  public DataMap getDataMap()
  {
    return _record == null ? null : _record.data();
  }

  /**
   * Obtain the {@link HttpStatus}.
   *
   * @return {@link HttpStatus}.
   */
  public HttpStatus getStatus()
  {
    return _status;
  }

  /**
   * Obtain the record template.
   *
   * @return record template.
   */
  public RecordTemplate getEntity()
  {
    return _record;
  }

  public static class Builder
  {
    private HttpStatus _status = HttpStatus.S_200_OK;
    private RecordTemplate _record;
    private Map<String, String> _headers;
    private List<HttpCookie> _cookies;

    /**
     * Build with status.
     *
     * @param status
     *          HttpStatus
     * @return Reference to this object.
     */
    public Builder status(HttpStatus status)
    {
      _status = status;
      return this;
    }

    /**
     * Build with entity.
     *
     * @param record Entity in the form of a {@link RecordTemplate}
     * @return Reference to this object.
     */
    public Builder entity(RecordTemplate record)
    {
      _record = record;
      return this;
    }

    /** Build with cookies
     *
     * @param cookies in the form of a string list
     * @return Reference to this object.
     */
    public Builder cookies(List<HttpCookie> cookies)
    {
      _cookies = cookies;
      return this;
    }

    /**
     * Build with header map.
     *
     * @param headers
     *          Response headers in the form of a Map.
     * @return Reference to this object.
     */
    public Builder headers(Map<String, String> headers)
    {
      _headers = headers;
      return this;
    }

    /**
     * Construct a {@link RestLiResponse} based on the builder configuration.
     *
     * @return reference to the newly minted {@link RestLiResponse} object.
     */
    public RestLiResponse build()
    {
      if (_record instanceof IdResponse)
      {
        final IdResponse<?> idResponse = (IdResponse<?>) _record;
        final Object key = idResponse.getId();
        if (key != null)
        {
          final ProtocolVersion protocolVersion = ProtocolVersionUtil.extractProtocolVersion(_headers);
          _headers.put(HeaderUtil.getIdHeaderName(protocolVersion), URIParamUtils.encodeKeyForHeader(key, protocolVersion));
        }
      }

      return new RestLiResponse(_status, _record, _headers, _cookies);
    }
  }
}
