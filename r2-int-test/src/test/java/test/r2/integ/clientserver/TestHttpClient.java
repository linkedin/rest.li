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

package test.r2.integ.clientserver;

import com.linkedin.common.callback.Callback;
import com.linkedin.data.ByteString;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.transport.common.RestRequestHandler;
import com.linkedin.r2.transport.common.StreamRequestHandler;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcherBuilder;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
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

public class TestHttpClient extends AbstractServiceTest
{
  private static final URI DISPATCHER_URI = URI.create("/");
  private static final int REQUEST_TIMEOUT = 1000;

  @Factory(dataProvider = "allCombinations", dataProviderClass = ClientServerConfiguration.class)
  public TestHttpClient(ClientProvider clientProvider, ServerProvider serverProvider, int port)
  {
    super(clientProvider, serverProvider, port);
  }


  @Test
  public void testClient() throws Exception
  {
    RestRequestBuilder rb = new RestRequestBuilder(getHttpUri(DISPATCHER_URI));
    rb.setMethod("GET");
    RestRequest request = rb.build();
    Future<RestResponse> f = _client.restRequest(request);

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
  }

  @Test
  public void testSimpleURI() throws Exception
  {
      // Note no trailing slash; the point of the test is to ensure this URI will
      // send a Request-URI of "/".
      URI uri = getHttpUri(null);
      RestRequestBuilder rb = new RestRequestBuilder(uri);
      rb.setMethod("GET");
      RestRequest request = rb.build();
      Future<RestResponse> f = _client.restRequest(request);

      // This will block
      RestResponse response = f.get();

      assertEquals(response.getStatus(), 200);
  }

  @Override
  protected TransportDispatcher getTransportDispatcher()
  {
    return new TransportDispatcherBuilder()
        .addRestHandler(DISPATCHER_URI, new EchoHandler())
        .addStreamHandler(DISPATCHER_URI, new EchoHandler())
        .build();
  }

  @Override
  protected Map<String, Object> getHttpClientProperties()
  {
    Map<String, Object> httpClientProperties = new HashMap<>();
    httpClientProperties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, Integer.toString(REQUEST_TIMEOUT));
    return httpClientProperties;
  }

  public class EchoHandler implements RestRequestHandler, StreamRequestHandler
  {
    @Override
    public void handleRequest(RestRequest request, RequestContext requestContext, final Callback<RestResponse> callback)
    {
      RestResponseBuilder builder = new RestResponseBuilder();
      callback.onSuccess(builder.setEntity(request.getEntity()).build());
    }

    @Override
    public void handleRequest(StreamRequest request, RequestContext requestContext, Callback<StreamResponse> callback)
    {
      StreamResponseBuilder builder = new StreamResponseBuilder();
      callback.onSuccess(builder.build(request.getEntityStream()));
    }
  }
}
