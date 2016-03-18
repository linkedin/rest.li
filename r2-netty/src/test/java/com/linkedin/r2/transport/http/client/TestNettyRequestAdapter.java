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

/**
 * $Id: $
 */

package com.linkedin.r2.transport.http.client;

import com.linkedin.data.ByteString;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;

import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @auther Sean Sheng
 */
public class TestNettyRequestAdapter
{
  private static final String ANY_URI = "http://localhost:8080/foo/bar";
  private static final String ANY_ENTITY = "\"name\": \"value\"";
  private static final String ANY_COOKIE = "anyCookie";

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
    Assert.assertEquals(nettyRequest.getUri(), "/foo/bar");
    Assert.assertEquals(nettyRequest.getMethod(), HttpMethod.POST);
    Assert.assertEquals(nettyRequest.getProtocolVersion(), HttpVersion.HTTP_1_1);
    Assert.assertEquals(nettyRequest.headers().get("Content-Length"), Integer.toString(restRequestBuilder.getEntity().length()));
    Assert.assertEquals(nettyRequest.headers().get("Content-Type"), "application/json");
    Assert.assertEquals(nettyRequest.headers().get("Cookie"), ANY_COOKIE);
  }

  @Test
  public void testStreamToNettyRequest() throws Exception
  {
    StreamRequestBuilder streamRequestBuilder = new StreamRequestBuilder(new URI(ANY_URI));
    streamRequestBuilder.setMethod("POST");
    streamRequestBuilder.setHeader("Content-Length", Integer.toString(ANY_ENTITY.length()));
    streamRequestBuilder.setHeader("Content-Type", "application/json");
    streamRequestBuilder.setCookies(Collections.singletonList("anyCookie"));
    StreamRequest streamRequest = streamRequestBuilder.build(
        EntityStreams.newEntityStream(new ByteStringWriter(ByteString.copy(ANY_ENTITY.getBytes()))));

    HttpRequest nettyRequest = NettyRequestAdapter.toNettyRequest(streamRequest);
    Assert.assertEquals(nettyRequest.getUri(), "/foo/bar");
    Assert.assertEquals(nettyRequest.getMethod(), HttpMethod.POST);
    Assert.assertEquals(nettyRequest.getProtocolVersion(), HttpVersion.HTTP_1_1);
    Assert.assertNull(nettyRequest.headers().get("Content-Length"));
    Assert.assertEquals(nettyRequest.headers().get("Content-Type"), "application/json");
    Assert.assertEquals(nettyRequest.headers().get("Cookie"), ANY_COOKIE);
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
}
