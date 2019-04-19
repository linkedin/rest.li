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

package com.linkedin.r2.netty.common;

import com.linkedin.data.ByteString;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;

import com.linkedin.r2.netty.common.NettyRequestAdapter;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;

import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.util.AsciiString;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;

import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Sean Sheng
 */
public class TestNettyRequestAdapter
{
  private static final String ANY_URI = "http://localhost:8080/foo/bar?q=baz";
  private static final String ANY_ENTITY = "\"name\": \"value\"";
  private static final String ANY_COOKIE = "anyCookie=anyCookieValue";
  private static final String INVALID_COOKIE = "invalidCookie";
  private static final String ANY_HEADER = "anyHeader";
  private static final List<String> ANY_COOKIES = new ArrayList<>(
                                                                  Arrays.asList("Cookie111=111",
                                                                      "Cookie11=11",
                                                                      "Cookie1=1",
                                                                      "MultipleCookie1=MC1;MultipleCookie2=MC2",
                                                                      "invalidCookie")
                                                                  );
  private static final String ENCODED_COOKIES_HEADER_VALUE = "Cookie111=111;Cookie11=11;Cookie1=1;MultipleCookie1=MC1;MultipleCookie2=MC2;invalidCookie";


  @Test
  public void testRestToNettyRequest() throws Exception
  {
    RestRequestBuilder restRequestBuilder = new RestRequestBuilder(new URI(ANY_URI));
    restRequestBuilder.setMethod("POST");
    restRequestBuilder.setEntity(ByteString.copyString(ANY_ENTITY, Charset.defaultCharset()));
    restRequestBuilder.setHeader("Content-Length", Integer.toString(restRequestBuilder.getEntity().length()));
    restRequestBuilder.setHeader("Content-Type", "application/json");
    restRequestBuilder.setCookies(Collections.singletonList(ANY_COOKIE));
    RestRequest restRequest = restRequestBuilder.build();

    HttpRequest nettyRequest = NettyRequestAdapter.toNettyRequest(restRequest);
    Assert.assertEquals(nettyRequest.uri(), "/foo/bar?q=baz");
    Assert.assertEquals(nettyRequest.method(), HttpMethod.POST);
    Assert.assertEquals(nettyRequest.protocolVersion(), HttpVersion.HTTP_1_1);
    Assert.assertEquals(nettyRequest.headers().get("Content-Length"), Integer.toString(restRequestBuilder.getEntity().length()));
    Assert.assertEquals(nettyRequest.headers().get("Content-Type"), "application/json");
    Assert.assertEquals(nettyRequest.headers().get("Cookie"), ANY_COOKIE);
  }

  @Test
  public void testRestToNettyRequestWithMultipleCookies() throws Exception
  {
    RestRequestBuilder restRequestBuilder = new RestRequestBuilder(new URI(ANY_URI));

    restRequestBuilder.setCookies(ANY_COOKIES);

    RestRequest restRequest = restRequestBuilder.build();
    HttpRequest nettyRequest = NettyRequestAdapter.toNettyRequest(restRequest);
    Assert.assertEquals(nettyRequest.headers().get("Cookie"), ENCODED_COOKIES_HEADER_VALUE);
  }

  @Test
  public void testStreamToNettyRequest() throws Exception
  {
    StreamRequestBuilder streamRequestBuilder = new StreamRequestBuilder(new URI(ANY_URI));
    streamRequestBuilder.setMethod("POST");
    streamRequestBuilder.setHeader("Content-Length", Integer.toString(ANY_ENTITY.length()));
    streamRequestBuilder.setHeader("Content-Type", "application/json");
    streamRequestBuilder.setCookies(Collections.singletonList(ANY_COOKIE));
    StreamRequest streamRequest = streamRequestBuilder.build(
        EntityStreams.newEntityStream(new ByteStringWriter(ByteString.copy(ANY_ENTITY.getBytes()))));

    HttpRequest nettyRequest = NettyRequestAdapter.toNettyRequest(streamRequest);
    Assert.assertEquals(nettyRequest.uri(), "/foo/bar?q=baz");
    Assert.assertEquals(nettyRequest.method(), HttpMethod.POST);
    Assert.assertEquals(nettyRequest.protocolVersion(), HttpVersion.HTTP_1_1);
    Assert.assertNull(nettyRequest.headers().get("Content-Length"));
    Assert.assertEquals(nettyRequest.headers().get("Content-Type"), "application/json");
    Assert.assertEquals(nettyRequest.headers().get("Cookie"), ANY_COOKIE);
  }

  @Test
  public void testStreamToNettyRequestWithMultipleCookies() throws Exception
  {
    StreamRequestBuilder streamRequestBuilder = new StreamRequestBuilder(new URI(ANY_URI));

    streamRequestBuilder.setCookies(ANY_COOKIES);

    StreamRequest streamRequest = streamRequestBuilder.build(
        EntityStreams.newEntityStream(new ByteStringWriter(ByteString.copy(ANY_ENTITY.getBytes()))));

    HttpRequest nettyRequest = NettyRequestAdapter.toNettyRequest(streamRequest);
    Assert.assertEquals(nettyRequest.headers().get("Cookie"), ENCODED_COOKIES_HEADER_VALUE);
  }

  @Test
  public void testStreamToNettyRequestContentLengthIgnoreCase() throws Exception
  {
    StreamRequestBuilder streamRequestBuilder = new StreamRequestBuilder(new URI(ANY_URI));
    streamRequestBuilder.setHeader("CONTENT-LENGTH", Integer.toString(ANY_ENTITY.length()));
    StreamRequest streamRequest = streamRequestBuilder.build(
        EntityStreams.newEntityStream(new ByteStringWriter(ByteString.copy(ANY_ENTITY.getBytes()))));

    HttpRequest nettyRequest = NettyRequestAdapter.toNettyRequest(streamRequest);
    Assert.assertNull(nettyRequest.headers().get("Content-Length"));
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

  @Test
  public void testStreamToHttp2HeadersBlacklist() throws Exception
  {
    StreamRequestBuilder streamRequestBuilder = new StreamRequestBuilder(new URI(ANY_URI));
    HEADER_BLACKLIST.forEach(header -> streamRequestBuilder.addHeaderValue(header, ANY_HEADER));
    StreamRequest request = streamRequestBuilder.build(
        EntityStreams.newEntityStream(new ByteStringWriter(ByteString.copy(ANY_ENTITY.getBytes()))));

    Http2Headers headers = NettyRequestAdapter.toHttp2Headers(request);
    Assert.assertNotNull(headers);

    HEADER_BLACKLIST.forEach(header -> Assert.assertFalse(headers.contains(header), header));
  }

  @Test
  public void testStreamToHttp2HeadersPseudoHeaders() throws Exception
  {
    StreamRequestBuilder streamRequestBuilder = new StreamRequestBuilder(new URI(ANY_URI));
    StreamRequest request = streamRequestBuilder.build(
        EntityStreams.newEntityStream(new ByteStringWriter(ByteString.copy(ANY_ENTITY.getBytes()))));

    Http2Headers headers = NettyRequestAdapter.toHttp2Headers(request);
    Assert.assertNotNull(headers);

    Assert.assertEquals(headers.authority(), "localhost:8080");
    Assert.assertEquals(headers.method(), "GET");
    Assert.assertEquals(headers.path(), "/foo/bar?q=baz");
    Assert.assertEquals(headers.scheme(), "http");
  }

  @Test
  public void testStreamToHttp2HeadersRegularHeaders() throws Exception
  {
    StreamRequestBuilder streamRequestBuilder = new StreamRequestBuilder(new URI(ANY_URI));
    streamRequestBuilder.setHeader("header1", "value1");
    streamRequestBuilder.setHeader("header2", "value2");
    StreamRequest request = streamRequestBuilder.build(
        EntityStreams.newEntityStream(new ByteStringWriter(ByteString.copy(ANY_ENTITY.getBytes()))));

    Http2Headers headers = NettyRequestAdapter.toHttp2Headers(request);
    Assert.assertNotNull(headers);

    Assert.assertEquals(headers.get("header1"), "value1");
    Assert.assertEquals(headers.get("header2"), "value2");
  }

  @Test
  public void testStreamToHttp2HeadersCookies() throws Exception
  {
    StreamRequestBuilder streamRequestBuilder = new StreamRequestBuilder(new URI(ANY_URI));
    IntStream.range(0, 10).forEach(i -> streamRequestBuilder.addCookie(ANY_COOKIE));
    StreamRequest request = streamRequestBuilder.build(
        EntityStreams.newEntityStream(new ByteStringWriter(ByteString.copy(ANY_ENTITY.getBytes()))));

    Http2Headers headers = NettyRequestAdapter.toHttp2Headers(request);
    Assert.assertNotNull(headers);

    List<CharSequence> cookies = headers.getAll(HttpHeaderNames.COOKIE);
    Assert.assertNotNull(cookies);
    Assert.assertEquals(cookies.size(), 10);
  }
}
