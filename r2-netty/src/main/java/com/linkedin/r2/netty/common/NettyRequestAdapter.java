/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.r2.netty.common;

import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.transport.http.common.HttpConstants;
import com.linkedin.r2.transport.http.util.CookieUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.util.AsciiString;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;

/**
 * Adapts R2 requests to Netty requests
 * @author Zhenkai Zhu
 */
public class NettyRequestAdapter
{
  private NettyRequestAdapter() {}

  /**
   * Adapts a RestRequest to Netty's HttpRequest
   * @param request  R2 rest request
   * @return Adapted HttpRequest.
   */
  public static HttpRequest toNettyRequest(RestRequest request) throws Exception
  {
    HttpMethod nettyMethod = HttpMethod.valueOf(request.getMethod());
    URL url = new URL(request.getURI().toString());
    String path = url.getFile();
    // RFC 2616, section 5.1.2:
    //   Note that the absolute path cannot be empty; if none is present in the original URI,
    //   it MUST be given as "/" (the server root).
    if (path.isEmpty())
    {
      path = "/";
    }

    ByteBuf content = Unpooled.wrappedBuffer(request.getEntity().asByteBuffer());
    HttpRequest nettyRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, nettyMethod, path, content);
    nettyRequest.headers().set(HttpConstants.CONTENT_LENGTH, request.getEntity().length());

    setHttpHeadersAndCookies(request, url, nettyRequest);

    return nettyRequest;
  }

  /**
   * Set Http Request Headers and Cookies on Netty's HttpRequest
   * @param request  R2 stream request
   * @param url Request Url
   * @param nettyRequest Netty HttpRequest
   */
  public static void setHttpHeadersAndCookies(RestRequest request, URL url, HttpRequest nettyRequest)
  {
    for (Map.Entry<String, String> entry : request.getHeaders().entrySet())
    {
      nettyRequest.headers().set(entry.getKey(), entry.getValue());
    }
    nettyRequest.headers().set(HttpHeaderNames.HOST, url.getAuthority());
    // RFC 6265
    //   When the user agent generates an HTTP/1.1 request, the user agent MUST
    //   NOT attach more than one Cookie header field.
    String encodedCookieHeaderValues = CookieUtil.clientEncode(request.getCookies());
    if (encodedCookieHeaderValues != null)
    {
      nettyRequest.headers().set(HttpConstants.REQUEST_COOKIE_HEADER_NAME, encodedCookieHeaderValues);
    }
  }

  /**
   * Adapts a StreamRequest to Netty's HttpRequest
   * @param request  R2 stream request
   * @return Adapted HttpRequest.
   */
  public static HttpRequest toNettyRequest(StreamRequest request) throws Exception
  {
    HttpMethod nettyMethod = HttpMethod.valueOf(request.getMethod());
    URL url = new URL(request.getURI().toString());
    String path = url.getFile();
    // RFC 2616, section 5.1.2:
    //   Note that the absolute path cannot be empty; if none is present in the original URI,
    //   it MUST be given as "/" (the server root).
    if (path.isEmpty())
    {
      path = "/";
    }

    HttpRequest nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, nettyMethod, path);
    nettyRequest.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);

    for (Map.Entry<String, String> entry : request.getHeaders().entrySet())
    {
      // RFC 7230, section 3.3.2
      //   A sender MUST NOT send a Content-Length header field in any message
      //   that contains a Transfer-Encoding header field.
      if (entry.getKey().equalsIgnoreCase(HttpHeaderNames.CONTENT_LENGTH.toString()))
      {
        continue;
      }

      nettyRequest.headers().set(entry.getKey(), entry.getValue());
    }
    nettyRequest.headers().set(HttpHeaderNames.HOST, url.getAuthority());
    // RFC 6265
    //   When the user agent generates an HTTP/1.1 request, the user agent MUST
    //   NOT attach more than one Cookie header field.
    String encodedCookieHeaderValues = CookieUtil.clientEncode(request.getCookies());
    if (encodedCookieHeaderValues != null)
    {
      nettyRequest.headers().set(HttpConstants.REQUEST_COOKIE_HEADER_NAME, encodedCookieHeaderValues);
    }

    return nettyRequest;
  }

  /**
   * The set of headers that should not be directly copied when converting headers from HTTP to HTTP/2.
   */
  private static final HashSet<String> HEADER_BLACKLIST = new HashSet<>();
  static {
    HEADER_BLACKLIST.add(HttpHeaderNames.CONNECTION.toString());
    @SuppressWarnings("deprecation")
    AsciiString keepAlive = HttpHeaderNames.KEEP_ALIVE;
    HEADER_BLACKLIST.add(keepAlive.toString());
    @SuppressWarnings("deprecation")
    AsciiString proxyConnection = HttpHeaderNames.PROXY_CONNECTION;
    HEADER_BLACKLIST.add(proxyConnection.toString());
    HEADER_BLACKLIST.add(HttpHeaderNames.TRANSFER_ENCODING.toString());
    HEADER_BLACKLIST.add(HttpHeaderNames.HOST.toString());
    HEADER_BLACKLIST.add(HttpHeaderNames.UPGRADE.toString());
    HEADER_BLACKLIST.add(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text().toString());
    HEADER_BLACKLIST.add(HttpConversionUtil.ExtensionHeaderNames.SCHEME.text().toString());
    HEADER_BLACKLIST.add(HttpConversionUtil.ExtensionHeaderNames.PATH.text().toString());
  }

  /**
   * Extracts fields from a {@link Request} and construct a {@link Http2Headers} instance.
   *
   * @param request StreamRequest to extract fields from
   * @return a new instance of Http2Headers
   * @throws {@link Exception}
   */
  public static <R extends Request> Http2Headers toHttp2Headers(R request) throws Exception
  {
    URI uri = request.getURI();
    URL url = new URL(uri.toString());

    String method = request.getMethod();
    String authority = url.getAuthority();
    String path = url.getFile();
    String scheme = uri.getScheme();

    // RFC 2616, section 5.1.2:
    //   Note that the absolute path cannot be empty; if none is present in the original URI,
    //   it MUST be given as "/" (the server root).
    path = path.isEmpty() ? "/" : path;

    final Http2Headers headers = new DefaultHttp2Headers()
        .method(method)
        .authority(authority)
        .path(path)
        .scheme(scheme);
    for (Map.Entry<String, String> entry : request.getHeaders().entrySet())
    {
      // Ignores HTTP/2 blacklisted headers
      if (HEADER_BLACKLIST.contains(entry.getKey().toLowerCase()))
      {
        continue;
      }

      // RFC 7540, section 8.1.2:
      //   ... header field names MUST be converted to lowercase prior to their
      //   encoding in HTTP/2.  A request or response containing uppercase
      //   header field names MUST be treated as malformed (Section 8.1.2.6).
      String name = entry.getKey().toLowerCase();
      String value = entry.getValue();
      headers.set(name, value);
    }

    // Split up cookies to allow for better header compression
    headers.set(HttpHeaderNames.COOKIE, request.getCookies());

    return headers;
  }
}
