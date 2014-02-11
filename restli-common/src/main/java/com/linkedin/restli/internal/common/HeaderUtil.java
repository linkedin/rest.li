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

package com.linkedin.restli.internal.common;


import com.linkedin.r2.message.rest.RestMessage;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;

import java.util.Map;


/**
 * @author Keren Jin
 */
public class HeaderUtil
{
  /**
   * Get Rest.li ID HTTP header name per specified Rest.li protocol version.
   *
   * @param protocolVersion Rest.li protocol version of the request/response
   * @return corresponding ID header name
   */
  public static String getIdHeaderName(ProtocolVersion protocolVersion)
  {
    if (protocolVersion.compareTo(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion()) >= 0 &&
        protocolVersion.compareTo(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion()) < 0)
    {
      return RestConstants.HEADER_ID;
    }
    else
    {
      // in case the protocol version < 1.0.0 (e.g. incorrect request from a non-Java client), use the new header name (client may not understand it)
      return RestConstants.HEADER_RESTLI_ID;
    }
  }

  /**
   * Get Rest.li ID HTTP header name per specified Rest.li protocol version.
   *
   * @param headers headers map
   * @return corresponding ID header name
   */
  public static String getIdHeaderName(Map<String, String> headers)
  {
    return getIdHeaderName(ProtocolVersionUtil.extractProtocolVersion(headers));
  }

  /**
   * Get Rest.li ID HTTP header value of a {@link RestMessage} according to its protocol version.
   *
   * @param headers headers map
   * @return ID header value
   */
  public static String getIdHeaderValue(Map<String, String> headers)
  {
    return headers.get(getIdHeaderName(ProtocolVersionUtil.extractProtocolVersion(headers)));
  }

  /**
   * Get Rest.li error response HTTP header name per specified Rest.li protocol version.
   *
   * @param protocolVersion Rest.li protocol version of the request/response
   * @return corresponding error response header name
   */
  public static String getErrorResponseHeaderName(ProtocolVersion protocolVersion)
  {
    if (protocolVersion.compareTo(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion()) >= 0 &&
        protocolVersion.compareTo(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion()) < 0)
    {
      return RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE;
    }
    else
    {
      // in case the protocol version < 1.0.0 (e.g. incorrect request from a non-Java client), use the new header name (client may not understand it)
      return RestConstants.HEADER_RESTLI_ERROR_RESPONSE;
    }
  }

  /**
   * Get Rest.li ID HTTP header name per specified Rest.li protocol version.
   *
   * @param headers headers map
   * @return corresponding ID header name
   */
  public static String getErrorResponseHeaderName(Map<String, String> headers)
  {
    return getErrorResponseHeaderName(ProtocolVersionUtil.extractProtocolVersion(headers));
  }

  /**
   * Get Rest.li error response HTTP header value of a {@link RestMessage} according to its protocol version.
   *
   * @param headers headers map
   * @return error header value
   */
  public static String getErrorResponseHeaderValue(Map<String, String> headers)
  {
    return headers.get(getErrorResponseHeaderName(ProtocolVersionUtil.extractProtocolVersion(headers)));
  }
}
