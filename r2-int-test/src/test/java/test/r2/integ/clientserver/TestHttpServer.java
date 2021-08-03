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

package test.r2.integ.clientserver;

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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import test.r2.integ.clientserver.providers.AbstractServiceTest;
import test.r2.integ.clientserver.providers.ClientServerConfiguration;
import test.r2.integ.clientserver.providers.client.ClientProvider;
import test.r2.integ.clientserver.providers.server.ServerProvider;

import static org.testng.Assert.*;


/**
 * @author Steven Ihde
 * @author Nizar Mankulangara
 * @version $Revision: $
 */

public class TestHttpServer extends AbstractServiceTest
{
  private static final String MULTI_VALUE_HEADER_NAME = "MultiValuedHeader";
  private static final String MULTI_VALUE_HEADER_COUNT_HEADER = "MultiValuedHeaderCount";

  @Factory(dataProvider = "allHttp", dataProviderClass = ClientServerConfiguration.class)
  public TestHttpServer(ClientProvider clientProvider, ServerProvider serverProvider, int port)
  {
    super(clientProvider, serverProvider, port);
  }

  @Override
  protected TransportDispatcher getTransportDispatcher()
  {
    return new TransportDispatcherBuilder()
        .addRestHandler(URI.create("/error"), new ErrorHandler())
        .addRestHandler(URI.create("/headerEcho"), new HeaderEchoHandler())
        .addRestHandler(URI.create("/foobar"), new FoobarHandler(_scheduler))
        .build();
  }

  @Test
  public void testSuccess() throws Exception
  {
    HttpURLConnection c = (HttpURLConnection) getUrl("/foobar").openConnection();
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

  private URL getUrl(String relativeUrl) throws Exception
  {
    return new URL( _clientProvider.createHttpURI(_port,new URI(relativeUrl)).toString());
  }

  @Test
  public void testPost() throws Exception
  {
    HttpURLConnection c = (HttpURLConnection)getUrl("/foobar").openConnection();
    c.setRequestMethod("POST");
    c.setDoInput(true);
    c.setDoOutput(true);
    OutputStream os = c.getOutputStream();
    os.write(1);
    os.close();
    c.connect();
    assertEquals(c.getResponseCode(), RestStatus.OK);
  }

  @Test
  public void testException() throws Exception
  {
    HttpURLConnection c2 = (HttpURLConnection)getUrl("/error").openConnection();
    assertEquals(c2.getResponseCode(), RestStatus.INTERNAL_SERVER_ERROR);
  }

  @Test
  public void testHeaderEcho() throws Exception
  {
    HttpURLConnection c = (HttpURLConnection)getUrl("/headerEcho").openConnection();
    c.setRequestProperty("Header1", "foo");
    c.setRequestProperty("Header2", "bar");
    assertEquals(c.getHeaderField("header1"), "foo");
    assertEquals(c.getHeaderField("header2"), "bar");
  }

  @Test
  public void testMultiValuedHeaderEcho() throws Exception
  {
    final List<String> values = Arrays.asList(new String[]{ "foo", "bar", "baz", "qux" });
    HttpURLConnection c = (HttpURLConnection)getUrl("/headerEcho").openConnection();
    for (String v : values)
    {
      c.addRequestProperty(MULTI_VALUE_HEADER_NAME, v);
    }

    // check the number of header values received at the server side
    String valueCount = c.getHeaderField(MULTI_VALUE_HEADER_COUNT_HEADER);
    assertEquals(Integer.parseInt(valueCount), values.size());


    // check the number of header values received at client side
    // we know the headers are going to be folded into one line its way back.
    List<String> echoValues = RestUtil.getHeaderValues(c.getHeaderField(MULTI_VALUE_HEADER_NAME));
    assertEquals(new HashSet<>(echoValues), new HashSet<>(values));
  }

  @Test
  public void testCookieEcho() throws Exception
  {
    String cookie = "sdsc=1%3A1SZM1shxDNbLt36wZwCgPgvN58iw%3D; Path=/; Domain=.linkedin.com; HTTPOnly";
    HttpURLConnection c = (HttpURLConnection)getUrl( "/headerEcho").openConnection();
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
    HttpURLConnection c = (HttpURLConnection)getUrl("/headerEcho").openConnection();
    for (String cookie : cookies)
    {
      c.addRequestProperty(HttpConstants.REQUEST_COOKIE_HEADER_NAME, cookie);
    }
    List<String> cookiesEcho = c.getHeaderFields().get(HttpConstants.RESPONSE_COOKIE_HEADER_NAME);
    assertEquals(new HashSet<>(cookiesEcho), new HashSet<>(cookies));
  }

  protected static class ErrorHandler implements RestRequestHandler
  {
    @Override
    public void handleRequest(RestRequest request, RequestContext requestContext, Callback<RestResponse> callback)
    {
      throw new RuntimeException("error for testing");
    }
  }

  protected static class FoobarHandler implements RestRequestHandler
  {
    ScheduledExecutorService _scheduler;
    FoobarHandler(ScheduledExecutorService scheduler)
    {
      _scheduler = scheduler;
    }
    @Override
    public void handleRequest(RestRequest request, RequestContext requestContext, final Callback<RestResponse> callback)
    {
      RestResponseBuilder builder = new RestResponseBuilder();
      builder.setStatus(RestStatus.OK);
      builder.setEntity("Hello, world!".getBytes());
      final RestResponse response = builder.build();
      _scheduler.schedule(new Runnable()
      {
        @Override
        public void run()
        {
          callback.onSuccess(response);
        }
      }, 5, TimeUnit.MILLISECONDS);
    }
  }

  protected static class HeaderEchoHandler implements RestRequestHandler
  {
    @Override
    public void handleRequest(RestRequest request, RequestContext requestContext, Callback<RestResponse> callback)
    {
      final RestResponseBuilder builder = new RestResponseBuilder()
          .setStatus(RestStatus.OK)
          .setEntity("Hello World".getBytes())
          .setHeaders(request.getHeaders())
          .setCookies(request.getCookies());


      List<String> multiValuedHeaders = request.getHeaderValues(MULTI_VALUE_HEADER_NAME);
      if (multiValuedHeaders != null)
      {
        builder.setHeader(MULTI_VALUE_HEADER_COUNT_HEADER, String.valueOf(multiValuedHeaders.size()));
      }
      callback.onSuccess(builder.build());
    }
  }
}
