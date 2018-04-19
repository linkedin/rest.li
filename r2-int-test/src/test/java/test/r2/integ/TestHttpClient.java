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

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.data.ByteString;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.Server;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcherBuilder;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.common.HttpProtocolVersion;
import com.linkedin.r2.transport.http.server.HttpServerFactory;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import test.r2.integ.helper.EchoHandler;

import static org.testng.Assert.assertEquals;


/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class TestHttpClient
{
  private static final int PORT = 8080;
  private static final URI DISPATCHER_URI = URI.create("/");
  private static final URI REQUEST_URI = URI.create("http://localhost:" + PORT + "/");
  private static final int REQUEST_TIMEOUT = 1000;
  private static final boolean REST_OVER_STREAM = true;
  private static final boolean NOT_REST_OVER_STREAM = true;

  private HttpClientFactory _clientFactory;

  @DataProvider
  public Object[][] configs()
  {
    Map<String, String> http1ClientProperties = new HashMap<>();
    http1ClientProperties.put(HttpClientFactory.HTTP_PROTOCOL_VERSION, HttpProtocolVersion.HTTP_1_1.name());
    http1ClientProperties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, Integer.toString(REQUEST_TIMEOUT));

    Map<String, String> http2ClientProperties = new HashMap<>();
    http2ClientProperties.put(HttpClientFactory.HTTP_PROTOCOL_VERSION, HttpProtocolVersion.HTTP_2.name());
    http2ClientProperties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, Integer.toString(REQUEST_TIMEOUT));

    TransportDispatcher dispatcher = new TransportDispatcherBuilder()
        .addRestHandler(DISPATCHER_URI, new EchoHandler())
        .build();

    return new Object[][] {
        {
            new HttpServerFactory().createH2cServer(PORT, dispatcher, REST_OVER_STREAM),
            new TransportClientAdapter(_clientFactory.getClient(http1ClientProperties), REST_OVER_STREAM)
        },
        {
            new HttpServerFactory().createH2cServer(PORT, dispatcher, REST_OVER_STREAM),
            new TransportClientAdapter(_clientFactory.getClient(http2ClientProperties), REST_OVER_STREAM)
        },
        {
            new HttpServerFactory().createH2cServer(PORT, dispatcher, NOT_REST_OVER_STREAM),
            new TransportClientAdapter(_clientFactory.getClient(http1ClientProperties), NOT_REST_OVER_STREAM)
        },
        {
            new HttpServerFactory().createH2cServer(PORT, dispatcher, NOT_REST_OVER_STREAM),
            new TransportClientAdapter(_clientFactory.getClient(http2ClientProperties), NOT_REST_OVER_STREAM)
        },
    };
  }

  @BeforeClass
  private void init() throws Exception
  {
    _clientFactory = new HttpClientFactory.Builder().build();
  }

  @AfterClass
  private void cleanup() throws Exception
  {
    final FutureCallback<None> callback = new FutureCallback<None>();
    _clientFactory.shutdown(callback);
    callback.get();
  }

  @Test(dataProvider = "configs")
  public void testClient(Server server, Client client) throws Exception
  {
    try
    {
      server.start();
      RestRequestBuilder rb = new RestRequestBuilder(REQUEST_URI);
      rb.setMethod("GET");
      RestRequest request = rb.build();
      Future<RestResponse> f = client.restRequest(request);

      // This will block
      RestResponse response = f.get();
      final ByteString entity = response.getEntity();
      if (entity != null)
      {
        System.out.println(entity.asString("UTF-8"));
      } else
      {
        System.out.println("NOTHING!");
      }

      assertEquals(response.getStatus(), 200);

      final FutureCallback<None> callback = new FutureCallback<None>();
      client.shutdown(callback);
      callback.get();
    }
    finally
    {
      server.stop();
    }
  }

  @Test(dataProvider = "configs")
  public void testSimpleURI(Server server, Client client) throws Exception
  {
    try
    {
      server.start();
      // Note no trailing slash; the point of the test is to ensure this URI will
      // send a Request-URI of "/".
      URI uri = URI.create("http://localhost:" + PORT);
      RestRequestBuilder rb = new RestRequestBuilder(uri);
      rb.setMethod("GET");
      RestRequest request = rb.build();
      Future<RestResponse> f = client.restRequest(request);

      // This will block
      RestResponse response = f.get();

      assertEquals(response.getStatus(), 200);

      final FutureCallback<None> callback = new FutureCallback<None>();
      client.shutdown(callback);
      callback.get();
    }
    finally
    {
      server.stop();
    }
  }
}
