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

package test.r2.integ;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.message.rest.RestUtil;
import com.linkedin.r2.transport.common.RestRequestHandler;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcherBuilder;
import com.linkedin.r2.transport.http.common.HttpConstants;
import com.linkedin.r2.transport.http.server.HttpServer;
import com.linkedin.r2.transport.http.server.HttpServerFactory;
import java.util.Arrays;
import java.util.List;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class TestHttpServer
{
  private static final int PORT = 8088;

  private HttpServer _server;

  @BeforeTest
  public void setup() throws IOException
  {
    final TransportDispatcher dispatcher = new TransportDispatcherBuilder()
            .addRestHandler(URI.create("/error"), new ErrorHandler())
            .addRestHandler(URI.create("/foobar"), new FoobarHandler())
            .addRestHandler(URI.create("/headerEcho"), new HeaderEchoHandler())
            .build();

    _server = new HttpServerFactory().createServer(PORT, dispatcher);
    _server.start();
  }

  @AfterTest
  public void tearDown() throws IOException
  {
    if (_server != null) {
      _server.stop();
    }
  }

  @Test
  public void testSuccess() throws Exception
  {
    HttpURLConnection c = (HttpURLConnection)new URL("http://localhost:" + PORT + "/foobar").openConnection();
    assertEquals(c.getResponseCode(), RestStatus.OK);
    InputStream in = c.getInputStream();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buf = new byte[1024];
    for (int r; (r = in.read(buf)) != -1; ) {
      baos.write(buf, 0, r);
    }
    String response = new String(baos.toByteArray());
    assertEquals(response, "Hello, world!");
  }

  @Test
  public void testException() throws Exception
  {
    HttpURLConnection c2 = (HttpURLConnection)new URL("http://localhost:" + PORT + "/error").openConnection();
    assertEquals(c2.getResponseCode(), RestStatus.INTERNAL_SERVER_ERROR);
  }

  @Test
  public void testHeaderEcho() throws Exception
  {
    HttpURLConnection c = (HttpURLConnection)new URL("http://localhost:" + PORT + "/headerEcho").openConnection();
    c.setRequestProperty("Header1", "foo");
    c.setRequestProperty("Header2", "bar");
    assertEquals(c.getHeaderField("header1"), "foo");
    assertEquals(c.getHeaderField("header2"), "bar");
  }

  @Test
  public void testMultiValuedHeaderEcho() throws Exception
  {
    final String header = "MultiValuedHeader";
    final List<String> values = Arrays.asList(new String[]{ "foo", "bar", "baz", "qux" });
    HttpURLConnection c = (HttpURLConnection)new URL("http://localhost:" + PORT + "/headerEcho").openConnection();
    for (String v : values)
    {
      c.addRequestProperty(header, v);
    }
    // we know the headers are going to be folded into one line its way back.
    List<String> echoValues = RestUtil.getHeaderValues(c.getHeaderField(header));
    assertEquals(echoValues.size(), values.size());
    assertTrue(echoValues.containsAll(values));
  }

  @Test
  public void testCookieEcho() throws Exception
  {
    String cookie = "sdsc=1%3A1SZM1shxDNbLt36wZwCgPgvN58iw%3D; Path=/; Domain=.linkedin.com; HTTPOnly";
    HttpURLConnection c = (HttpURLConnection)new URL("http://localhost:" + PORT + "/headerEcho").openConnection();
    c.setRequestProperty(HttpConstants.REQUEST_COOKIE_HEADER_NAME, cookie);
    assertEquals(c.getHeaderField(HttpConstants.RESPONSE_COOKIE_HEADER_NAME), cookie);
  }

  @Test
  public void testMultipleCookiesEcho() throws Exception
  {
    final List<String> cookies = Arrays.asList(new String[]
      {
        "_lipt=deleteMe; Expires=Thu, 01-Jan-1970 00:00:10 GMT; Path=/",
        "lang=\"v=2&lang=en-us&c=\"; Version=1; Domain=linkedin.com; Path=/"
      });
    HttpURLConnection c = (HttpURLConnection)new URL("http://localhost:" + PORT + "/headerEcho").openConnection();
    for (String cookie : cookies)
    {
      c.addRequestProperty(HttpConstants.REQUEST_COOKIE_HEADER_NAME, cookie);
    }
    List<String> cookiesEcho = c.getHeaderFields().get(HttpConstants.RESPONSE_COOKIE_HEADER_NAME);
    assertEquals(cookiesEcho.size(), cookies.size());
    assertTrue(cookiesEcho.containsAll(cookies));
  }

  private static class ErrorHandler implements RestRequestHandler
  {
    @Override
    public void handleRequest(RestRequest request, RequestContext requestContext, Callback<RestResponse> callback)
    {
      throw new RuntimeException("error for testing");
    }
  }

  private static class FoobarHandler implements RestRequestHandler
  {
    @Override
    public void handleRequest(RestRequest request, RequestContext requestContext, Callback<RestResponse> callback)
    {
      RestResponseBuilder builder = new RestResponseBuilder();
      builder.setStatus(RestStatus.OK);
      builder.setEntity("Hello, world!".getBytes());
      RestResponse response = builder.build();
      callback.onSuccess(response);
    }
  }

  private static class HeaderEchoHandler implements RestRequestHandler
  {
    @Override
    public void handleRequest(RestRequest request, RequestContext requestContext, Callback<RestResponse> callback)
    {
      final RestResponse response = new RestResponseBuilder()
        .setStatus(RestStatus.OK)
        .setEntity("Hello World".getBytes())
        .setHeaders(request.getHeaders())
        .setCookies(request.getCookies())
        .build();
      callback.onSuccess(response);
    }
  }
}
