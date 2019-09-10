/*
   Copyright (c) 2019 LinkedIn Corp.

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


package test.r2.integ.clientserver.providers;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.RestRequestHandler;
import com.linkedin.r2.transport.common.Server;
import com.linkedin.r2.transport.common.StreamRequestHandler;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.http.server.HttpServerFactory;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import test.r2.integ.clientserver.providers.client.ClientProvider;
import test.r2.integ.clientserver.providers.client.Http2ClientProvider;
import test.r2.integ.clientserver.providers.client.Https2ClientProvider;
import test.r2.integ.clientserver.providers.server.ServerCreationContext;
import test.r2.integ.clientserver.providers.server.ServerProvider;


/**
 * @author Nizar Mankulangara
 */
public abstract class AbstractServiceTest
{
  protected static final long LARGE_BYTES_NUM = 1024 * 1024 * 1024;
  protected static final long SMALL_BYTES_NUM = 1024 * 1024 * 64;
  protected static final long TINY_BYTES_NUM = 1024 * 64;
  protected static final byte BYTE = 100;
  protected static final long INTERVAL = 20;
  protected ScheduledExecutorService _scheduler;
  protected ExecutorService _executor;

  protected final ClientProvider _clientProvider;
  protected final ServerProvider _serverProvider;
  protected final int _port;

  protected Client _client;
  protected Server _server;

  public AbstractServiceTest(ClientProvider clientProvider, ServerProvider serverProvider, int port)
  {
    _clientProvider = clientProvider;
    _serverProvider = serverProvider;
    _port = port;
  }


  @BeforeClass
  public void setup() throws Exception
  {
    _scheduler = Executors.newSingleThreadScheduledExecutor();
    _executor = Executors.newCachedThreadPool();
    _client = createClient();
    _server = createServer();
    _server.start();
  }

  @AfterClass
  public void tearDown() throws Exception
  {
    tearDown(_client, _server);
    _clientProvider.tearDown();
  }

  protected Client createClient() throws Exception
  {
    return _clientProvider.createClient(getClientFilterChain(), getHttpClientProperties());
  }

  protected Client createClient(FilterChain filterChain) throws Exception
  {
    return _clientProvider.createClient(filterChain, getHttpClientProperties());
  }

  protected Server createServer() throws Exception
  {
    ServerCreationContext context = new ServerCreationContext(getServerFilterChain(), _port,
        getTransportDispatcher(), getServerTimeout());
    return _serverProvider.createServer(context);
  }

  protected void tearDown(Client client, Server server) throws Exception
  {
    try
    {
      tearDown(client);

      _scheduler.shutdown();
      _executor.shutdown();
      _clientProvider.tearDown();
    }
    finally
    {
      if (server != null)
      {
        server.stop();
        server.waitForStop();
      }

      // By de-referencing test specific objects - making sure the GC will reclaim all the test data inside these objects.
      _client = null;
      _server = null;
    }
  }

  protected void tearDown(Client client) throws Exception
  {
    if (client != null)
    {
      final FutureCallback<None> callback = new FutureCallback<>();
      client.shutdown(callback);
      callback.get();
    }
  }

  protected FilterChain getServerFilterChain()
  {
    return FilterChains.empty();
  }

  protected FilterChain getClientFilterChain()
  {
    return FilterChains.empty();
  }

  protected abstract TransportDispatcher getTransportDispatcher();

  protected int getServerTimeout()
  {
    return HttpServerFactory.DEFAULT_ASYNC_TIMEOUT;
  }

  protected Map<String, Object> getHttpClientProperties()
  {
    HashMap<String, Object> properties = new HashMap<>();
    return properties;
  }

  public static class HeaderEchoHandler implements RestRequestHandler, StreamRequestHandler
  {
    protected static final String MULTI_VALUE_HEADER_NAME = "MultiValuedHeader";
    protected static final String MULTI_VALUE_HEADER_COUNT_HEADER = "MultiValuedHeaderCount";

    @Override
    public void handleRequest(RestRequest request, RequestContext requestContext, Callback<RestResponse> callback)
    {
      System.out.println("Server, handleRestRequest");
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

    public void handleRequest(StreamRequest request, RequestContext requestContext, Callback<StreamResponse> callback) {
      System.out.println("Server, handleStreamRequest");
      StreamResponseBuilder builder = new StreamResponseBuilder()
          .setStatus(RestStatus.OK)
          .setHeaders(request.getHeaders())
          .setCookies(request.getCookies());

      List<String> multiValuedHeaders = request.getHeaderValues(MULTI_VALUE_HEADER_NAME);
      if (multiValuedHeaders != null)
      {
        builder.setHeader(MULTI_VALUE_HEADER_COUNT_HEADER, String.valueOf(multiValuedHeaders.size()));
      }

      callback.onSuccess(builder.build(EntityStreams.emptyStream()));
    }
  }

  protected URI getHttpUri(URI relativeUri)
  {
    return _clientProvider.createHttpURI(_port, relativeUri);
  }

  //Http2 Stream based channel is available on http2 new pipeline
  protected boolean isHttp2StreamBasedChannel()
  {
    if(_clientProvider instanceof Http2ClientProvider || _clientProvider instanceof Https2ClientProvider)
    {
      return _clientProvider.getUsePipelineV2();
    }

    return false;
  }

}
