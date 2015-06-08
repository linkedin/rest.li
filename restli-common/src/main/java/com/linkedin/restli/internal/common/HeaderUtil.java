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


import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;


/**
 * @author Keren Jin
 */
public class HeaderUtil
{
  /**
   * This is used by Multiplexer to determine which request headers should not be inherited from the envelope request by individual request.
   */
  public final static List<String> NONINHERITABLE_REQUEST_HEADERS = Arrays.asList("content-length");

  /**
   * This is used by MultiplexedRequestBuilder when constructing individual responses on the client side.
   * This list is used to determine which response headers should not be inherited from the envelope request by individual response.
   */
  public final static List<String> NONINHERITABLE_RESPONSE_HEADERS = Arrays.asList("content-length");

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
   * Get Rest.li ID HTTP header value of a {@link com.linkedin.r2.message.MessageHeaders} according to its protocol version.
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
   * Get Rest.li error response HTTP header value of a {@link com.linkedin.r2.message.MessageHeaders} according to its protocol version.
   *
   * @param headers headers map
   * @return error header value
   */
  public static String getErrorResponseHeaderValue(Map<String, String> headers)
  {
    return headers.get(getErrorResponseHeaderName(ProtocolVersionUtil.extractProtocolVersion(headers)));
  }

  /**
   * Given a set of headers, return a new set of headers with specified headers removed.
   * This function will not modify the input parameters.
   *
   * @param headers the original set of headers.
   * @param headerNames header names to remove. Since HTTP header names are case insensitive, header name will be
   *                    matched case-insensitively.
   * @return a new set of headers with the given headerNames removed.
   */
  public static Map<String, String> removeHeaders(Map<String, String> headers, Collection<String> headerNames)
  {
    if (headers == null || headerNames == null  || headerNames.isEmpty())
    {
      return headers;
    }
    Set<String> headersToRemove = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    headersToRemove.addAll(headerNames);
    Map<String, String> newHeaders = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    for(Map.Entry<String, String> header : headers.entrySet())
    {
      String name = header.getKey();
      if (!headersToRemove.contains(name))
      {
        newHeaders.put(name, header.getValue());
      }
    }
    return newHeaders;
  }

  /**
   * Merge two set of headers. The second set of headers will override the first set of headers for any header with the same (case-insensitive) name
   * This function will not modify the input parameters.
   *
   * @param headers1 initial set of headers
   * @param headers2 headers to be added to header1. If a header name in header2 also exists in header1, header2's header value
   *                 will be used.
   * @return merged headers
   */
  public static Map<String, String> mergeHeaders(Map<String, String> headers1, Map<String, String> headers2)
  {
    TreeMap<String, String> combinedHeaders = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    if (headers2 != null)
    {
      combinedHeaders.putAll(headers2);
    }
    if (headers1 != null)
    {
      for (Map.Entry<String, String> header : headers1.entrySet())
      {
        String name = header.getKey();
        if (!combinedHeaders.containsKey(name))
        {
          combinedHeaders.put(name, header.getValue());
        }
      }
    }
    return combinedHeaders;
  }
}
