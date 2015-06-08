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

/**
 * $Id: $
 */

package com.linkedin.r2.transport.http.client;

import com.linkedin.data.ByteString;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.http.common.HttpConstants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author Ang Xu
 * @version $Revision: $
 */
public class TestRAPClientCodec
{
  private final static Charset CHARSET = Charset.forName("UTF-8");
  private final static String HOST = "localhost:8080";

  @DataProvider(name = "restRequest")
  public Object[][] createRequests()
  {
    return new Object[][]
        {
            { // the point is to make sure absolute path "/" will be added
                // even when the request uri is empty.
                "/",
                new RestRequestBuilder(createURI(""))
                    .setMethod("GET")
                    .build()},
            {
                "/",
                new RestRequestBuilder(createURI("/"))
                    .setMethod("PUT")
                    .setEntity(ByteString.copyString("entity", CHARSET))
                    .build()
            },
            {
                "/foo?q=1",
                new RestRequestBuilder(createURI("/foo?q=1"))
                    .setMethod("POST")
                    .setEntity(ByteString.copyString("{\"foo\":\"bar\"}", CHARSET))
                    .setHeader("Content-Type", "application/json")
                    .setHeader("Content-Length", "13")
                    .build()
            },
            {
                "/bar/1",
                new RestRequestBuilder(createURI("/bar/1"))
                    .setMethod("GET")
                    .addHeaderValue("Header", "1")
                    .addHeaderValue("Header", "2")
                    .addHeaderValue("Header", "3")
                    .build()
            },
            {
                "/baz",
                new RestRequestBuilder(createURI("/baz"))
                    .setMethod("GET")
                    .addCookie("cookie1=value1; path=/baz")
                    .addCookie("cookie2=value2")
                    .build()
            },
        };
  }

  @Test(dataProvider = "restRequest")
  public void testRequestEncoder(String uri, RestRequest request)
  {
    final EmbeddedChannel ch = new EmbeddedChannel(new RAPClientCodec());

    ch.writeOutbound(request);
    FullHttpRequest nettyRequest = (FullHttpRequest) ch.readOutbound();

    Assert.assertEquals(nettyRequest.getUri(), uri);
    Assert.assertEquals(nettyRequest.getMethod(), HttpMethod.valueOf(request.getMethod()));
    Assert.assertEquals(nettyRequest.content().toString(CHARSET), request.getEntity().asString(CHARSET));
    Assert.assertEquals(nettyRequest.headers().get(HttpHeaders.Names.HOST), HOST);
    assertList(nettyRequest.headers().getAll(HttpConstants.REQUEST_COOKIE_HEADER_NAME), request.getCookies());

    for (String name : request.getHeaders().keySet())
    {
      Assert.assertEquals(nettyRequest.headers().get(name), request.getHeader(name));
    }

    ch.finish();
  }

  @DataProvider(name = "responseData")
  public Object[][] createResponseData()
  {
    return new Object[][]
        {
            {
                200, "OK",
                new DefaultHttpHeaders(),
                new String[0]
            },
            {
                404, "Not Found",
                new DefaultHttpHeaders().add("Content-Type", "text/plain"),
                new String[]{ "cookie1=value1" }
            },
            {
                500, "Internal Server Error",
                new DefaultHttpHeaders()
                    .add("Content-Type", "text/plain")
                    .add("Header", "1")
                    .add("Header", "2")
                    .add("Header", "3"),
                new String[]
                    {
                        "cookie1=value1; path=/; expires=Saturday, 14-Feb-15 13:14:00 GMT",
                        "cookie2=value2; path=/foo",
                    }
            }
        };
  }


  @Test(dataProvider = "responseData")
  public void testResponseDecoder(int status, String entity, HttpHeaders headers, String[] cookies)
  {
    final EmbeddedChannel ch = new EmbeddedChannel(new RAPClientCodec());

    ByteBuf content = Unpooled.copiedBuffer(entity, CHARSET);
    FullHttpResponse nettyResponse =
        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(status), content);
    nettyResponse.headers().set(headers);
    for (String cookie : cookies)
    {
      nettyResponse.headers().add(HttpHeaders.Names.SET_COOKIE, cookie);
    }

    ch.writeInbound(nettyResponse);
    RestResponse response = (RestResponse) ch.readInbound();

    Assert.assertEquals(response.getStatus(), status);
    Assert.assertEquals(response.getEntity().asString(CHARSET), entity);
    assertList(response.getCookies(), nettyResponse.headers().getAll(HttpConstants.RESPONSE_COOKIE_HEADER_NAME));

    for (Map.Entry<String, String> header : nettyResponse.headers())
    {
      if (!header.getKey().equalsIgnoreCase(HttpConstants.RESPONSE_COOKIE_HEADER_NAME))
      {
        List<String> values = response.getHeaderValues(header.getKey());
        Assert.assertNotNull(values);
        Assert.assertTrue(values.contains(header.getValue()));
      }
    }
    // make sure the incoming ByteBuf is released
    Assert.assertEquals(content.refCnt(), 0);

    ch.finish();
  }

  @Test
  public void testDecodeException()
  {
    final EmbeddedChannel ch =
        new EmbeddedChannel(new HttpClientCodec(), new HttpObjectAggregator(65536), new RAPClientCodec());

    // When we received an invalid message, a decode exception should be thrown out of the
    // end of netty pipeline.
    String junk = "Not a HTTP message\r\n";
    try
    {
      ch.writeInbound(Unpooled.copiedBuffer(junk, CHARSET));
      Assert.fail("Should have thrown decode exception");
    }
    catch (Exception ex)
    {
      // expected.
    }
    ch.finish();
  }

  private static URI createURI(String relativeURI)
  {
    return URI.create("http://" + HOST + relativeURI);
  }

  private void assertList(List<String> actual, List<String> expected)
  {
    Assert.assertEquals(actual.size(), expected.size());
    Assert.assertTrue(actual.containsAll(expected));
  }
}
