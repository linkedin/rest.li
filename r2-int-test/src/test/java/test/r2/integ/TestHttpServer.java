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
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.transport.common.RestRequestHandler;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcherBuilder;
import com.linkedin.r2.transport.http.server.HttpServer;
import com.linkedin.r2.transport.http.server.HttpServerFactory;
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

  private static class ErrorHandler implements RestRequestHandler
  {

    @Override
    public void handleRequest(RestRequest request, Callback<RestResponse> callback)
    {
      throw new RuntimeException("error for testing");
    }
  }

  private static class FoobarHandler implements RestRequestHandler
  {
    @Override
    public void handleRequest(RestRequest request, Callback<RestResponse> callback)
    {
      RestResponseBuilder builder = new RestResponseBuilder();
      builder.setStatus(RestStatus.OK);
      builder.setEntity("Hello, world!".getBytes());
      RestResponse response = builder.build();
      callback.onSuccess(response);
    }
  }
}
