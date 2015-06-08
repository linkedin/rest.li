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

package com.linkedin.r2.transport.http.client;

import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.transport.http.common.HttpConstants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;

import java.net.URL;
import java.util.Map;

/**
 * Adapts R2 requests to Netty requests
 * @auther Zhenkai Zhu
 */

/* package private */ class NettyRequestAdapter
{
  private NettyRequestAdapter() {}

  /**
   * Adapts a RestRequest to Netty's HttpRequest
   * @param request  R2 rest request
   * @return Adapted HttpRequest.
   */
  static HttpRequest toNettyRequest(RestRequest request) throws Exception
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


    for (Map.Entry<String, String> entry : request.getHeaders().entrySet())
    {
      nettyRequest.headers().set(entry.getKey(), entry.getValue());
    }
    nettyRequest.headers().set(HttpHeaders.Names.HOST, url.getAuthority());
    nettyRequest.headers().set(HttpConstants.REQUEST_COOKIE_HEADER_NAME, request.getCookies());

    return nettyRequest;
  }

  /**
   * Adapts a StreamRequest to Netty's HttpRequest
   * @param request  R2 stream request
   * @return Adapted HttpRequest.
   */
  static HttpRequest toNettyRequest(StreamRequest request) throws Exception
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
    nettyRequest.headers().set(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);

    for (Map.Entry<String, String> entry : request.getHeaders().entrySet())
    {
      nettyRequest.headers().set(entry.getKey(), entry.getValue());
    }
    nettyRequest.headers().set(HttpHeaders.Names.HOST, url.getAuthority());
    nettyRequest.headers().set(HttpConstants.REQUEST_COOKIE_HEADER_NAME, request.getCookies());

    return nettyRequest;
  }
}
