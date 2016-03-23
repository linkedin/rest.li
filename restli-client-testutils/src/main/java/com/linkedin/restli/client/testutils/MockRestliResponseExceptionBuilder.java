/*
   Copyright (c) 2016 LinkedIn Corp.

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


import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.CookieUtil;
import java.io.IOException;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SuppressWarnings("serial")
public class MockRestliResponseExceptionBuilder
{
  private static final JacksonDataCodec CODEC = new JacksonDataCodec();
  private static final int DEFAULT_HTTP_STATUS = 500;

  private ErrorResponse _errorResponse;
  private Map<String, String> _headers;
  private List<HttpCookie> _cookies;
  private ProtocolVersion _version;

  public MockRestliResponseExceptionBuilder()
  {
    // defaults
    this._headers = new HashMap<>();
    this._cookies = new ArrayList<>();
    this._version = AllProtocolVersions.LATEST_PROTOCOL_VERSION;
    this._errorResponse = new ErrorResponse().setStatus(DEFAULT_HTTP_STATUS);
  }

  public RestLiResponseException build()
  {
    String errorHeaderName = _version.equals(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion())
        ? RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE : RestConstants.HEADER_RESTLI_ERROR_RESPONSE;

    Map<String, String> headers = new HashMap<String, String>();
    headers.put(errorHeaderName, "true");
    headers.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, _version.toString());
    headers.putAll(_headers);

    RestResponse restResponse = new RestResponseBuilder()
        .setEntity(mapToBytes(_errorResponse.data()))
        .setStatus(_errorResponse.hasStatus() ? _errorResponse.getStatus() : DEFAULT_HTTP_STATUS)
        .setHeaders(Collections.unmodifiableMap(headers))
        .setCookies(Collections.unmodifiableList(
            CookieUtil.encodeCookies(_cookies.isEmpty() ? Collections.emptyList() : _cookies)))
        .build();

    return new RestLiResponseException(restResponse, null, _errorResponse);
  }

  public MockRestliResponseExceptionBuilder setErrorResponse(ErrorResponse errorResponse)
  {
    if (errorResponse == null)
    {
      throw new IllegalArgumentException("errorResponse can't be null");
    }

    this._errorResponse = errorResponse;
    return this;
  }

  public MockRestliResponseExceptionBuilder setStatus(HttpStatus status)
  {
    if (status == null)
    {
      throw new IllegalArgumentException("status can't be null");
    }

    this._errorResponse.setStatus(status.getCode());
    return this;
  }

  public MockRestliResponseExceptionBuilder setHeaders(Map<String, String> headers)
  {
    if (headers == null)
    {
      throw new IllegalArgumentException("headers can't be null");
    }

    this._headers = headers;
    return this;
  }

  public MockRestliResponseExceptionBuilder setCookies(List<HttpCookie> cookies)
  {
    if (cookies == null)
    {
      throw new IllegalArgumentException("cookies can't be null");
    }

    this._cookies = cookies;
    return this;
  }

  public MockRestliResponseExceptionBuilder setProtocolVersion(ProtocolVersion version)
  {
    if (version == null)
    {
      throw new IllegalArgumentException("version can't be null");
    }

    this._version = version;
    return this;
  }

  private static byte[] mapToBytes(DataMap dataMap)
  {
    try
    {
      return CODEC.mapToBytes(dataMap);
    }
    catch (IOException exception)
    {
      throw new RuntimeException(exception);
    }
  }
}
