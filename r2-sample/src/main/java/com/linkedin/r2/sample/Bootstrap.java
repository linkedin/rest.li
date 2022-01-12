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

package com.linkedin.r2.sample;

import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.sample.echo.EchoServiceImpl;
import com.linkedin.r2.sample.echo.OnExceptionEchoService;
import com.linkedin.r2.sample.echo.ThrowingEchoService;
import com.linkedin.r2.sample.echo.rest.RestEchoServer;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.Server;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcherBuilder;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.common.HttpProtocolVersion;
import com.linkedin.r2.transport.http.server.HttpServerFactory;
import com.linkedin.r2.util.NamedThreadFactory;
import com.linkedin.pegasus.io.netty.channel.EventLoopGroup;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;


/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class Bootstrap
{
  private static final int HTTP_FREE_PORT = 0;
  private static final int HTTPS_PORT = 8443;
  private static final int NUMBER_OF_EXECUTOR_THREAD = 10;

  private static final URI ECHO_URI = URI.create("/echo");
  private static final URI ON_EXCEPTION_ECHO_URI = URI.create("/on-exception-echo");
  private static final URI THROWING_ECHO_URI = URI.create("/throwing-echo");
  private static final ScheduledExecutorService r2Scheduler = Executors.
      newScheduledThreadPool(NUMBER_OF_EXECUTOR_THREAD, new NamedThreadFactory("R2 Netty Scheduler"));

  // ##################### Server Section #####################

  // ############# HTTP1.1 Clear Section #############

  public static Server createHttpServer(int port, FilterChain filters)
  {
    return createHttpServer(port, filters, R2Constants.DEFAULT_REST_OVER_STREAM);
  }


  public static Server createHttpServer(int port, FilterChain filters, boolean restOverStream,
      TransportDispatcher dispatcher)
  {
    return createHttpServer(new HttpServerFactory(filters), port, restOverStream, dispatcher);
  }


  public static Server createHttpServer(HttpServerFactory serverFactory, int port,
      boolean restOverStream,TransportDispatcher dispatcher)
  {
    if (dispatcher == null)
    {
      dispatcher = createDispatcher();
    }

    return serverFactory.createServer(port, dispatcher, restOverStream);
  }

  public static Server createHttpServer(int port, FilterChain filters, boolean restOverStream)
  {
    return createHttpServer(port, filters, restOverStream, createDispatcher());
  }

  // ############# HTTP2 Clear Section #############

  public static Server createH2cServer(int port, FilterChain filters, boolean restOverStream)
  {
    return createH2cServer(port, filters, restOverStream, createDispatcher());
  }

  public static Server createH2cServer(int port, FilterChain filters, boolean restOverStream, TransportDispatcher dispatcher)
  {
    if(dispatcher == null)
      dispatcher = createDispatcher();

    return new HttpServerFactory(filters)
        .createH2cServer(port, dispatcher, restOverStream);
  }

  // ############# HTTPS 1.1 Section #############

  public static Server createHttpsServer(String keyStore, String keyStorePassword, FilterChain filters)
  {
    return createHttpsServer(HTTPS_PORT, keyStore, keyStorePassword, filters);
  }

  public static Server createHttpsServer(int sslPort, String keyStore, String keyStorePassword, FilterChain filters)
  {
    return createHttpsServer(sslPort, keyStore, keyStorePassword, filters, R2Constants.DEFAULT_REST_OVER_STREAM);
  }

  public static Server createHttpsServer(int sslPort, String keyStore, String keyStorePassword, FilterChain filters, boolean restOverStream)
  {
    return new HttpServerFactory(filters)
      .createHttpsServer(HTTP_FREE_PORT, sslPort, keyStore, keyStorePassword, createDispatcher(),
        HttpServerFactory.DEFAULT_SERVLET_TYPE, restOverStream);
  }

  public static Server createHttpsServer(int httpPort, int sslPort, String keyStore, String keyStorePassword,
      FilterChain filters, boolean restOverStream)
  {
    return createHttpsServer(httpPort, sslPort, keyStore, keyStorePassword, filters, restOverStream, createDispatcher());
  }

  public static Server createHttpsServer(int httpPort, int sslPort, String keyStore, String keyStorePassword,
      FilterChain filters, boolean restOverStream, TransportDispatcher dispatcher)
  {
    if (dispatcher == null)
    {
      dispatcher = createDispatcher();
    }

    return new HttpServerFactory(filters)
      .createHttpsServer(httpPort, sslPort, keyStore, keyStorePassword, dispatcher,
        HttpServerFactory.DEFAULT_SERVLET_TYPE, restOverStream);
  }

  // ############# HTTPS 2 Section #############

  public static Server createHttpsH2cServer(int httpPort, int sslPort, String keyStore, String keyStorePassword,
      FilterChain filters, boolean restOverStream)
  {
    return createHttpsH2cServer(httpPort, sslPort, keyStore, keyStorePassword, filters, restOverStream, createDispatcher());
  }

  public static Server createHttpsH2cServer(int httpPort, int sslPort, String keyStore, String keyStorePassword,
      FilterChain filters, boolean restOverStream, TransportDispatcher transportDispatcher)
  {
    if (transportDispatcher == null)
    {
      transportDispatcher = createDispatcher();
    }
    return new HttpServerFactory(filters)
      .createHttpsH2cServer(httpPort, sslPort, keyStore, keyStorePassword, transportDispatcher,
        HttpServerFactory.DEFAULT_SERVLET_TYPE, restOverStream);
  }

  // ##################### Client Section #####################

  // ############# HTTP1.1 Clear Section #############

  public static Client createHttpClient(HttpClientFactory httpClientFactory, boolean restOverStream)
  {
    return createHttpClient(httpClientFactory, restOverStream, null);
  }

  public static Client createHttpClient(HttpClientFactory httpClientFactory, boolean restOverStream, Map<String, Object> clientProperties)
  {
    HashMap<String, Object> properties = new HashMap<>();
    properties.put(HttpClientFactory.HTTP_PROTOCOL_VERSION, HttpProtocolVersion.HTTP_1_1.name());
    properties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, "10000");

    merge(properties, clientProperties);

    return createClient(restOverStream, properties, httpClientFactory);
  }

  public static Client createClient(boolean restOverStream, HashMap<String, Object> properties,
      HttpClientFactory httpClientFactory)
  {
    final TransportClient client = httpClientFactory.getClient(properties);
    return new TransportClientAdapter(client, restOverStream);
  }

  public static HttpClientFactory createHttpClientFactory(FilterChain filters, boolean usePipelineV2,
      EventLoopGroup eventLoopGroup)
  {
    return new HttpClientFactory.Builder().
        setEventLoopGroup(eventLoopGroup).
        setFilterChain(filters).
        setShutDownFactory(false).
        setScheduleExecutorService(r2Scheduler).
        setShutdownScheduledExecutorService(false).
        setUsePipelineV2(usePipelineV2).
        build();
  }

  private static void merge(HashMap<String, Object> defaultValues, Map<String, Object> override)
  {
    if (override != null && defaultValues!=null)
    {
       for(Map.Entry<String, Object> keyValue : override.entrySet())
       {
          defaultValues.put(keyValue.getKey(), keyValue.getValue());
       }
    }
  }

  public static Client createHttpClient(FilterChain filters)
  {
    return createHttpClient(createHttpClientFactory(filters, false,  null), R2Constants.DEFAULT_REST_OVER_STREAM);
  }


  // ############# HTTPS 1.1 Section #############

  public static Client createHttpsClient(HttpClientFactory httpClientFactory, boolean restOverStream, SSLContext sslContext,
      SSLParameters sslParameters)
  {
    return createHttpsClient(httpClientFactory, restOverStream, sslContext, sslParameters, null);
  }

  public static Client createHttpsClient(HttpClientFactory httpClientFactory, boolean restOverStream, SSLContext sslContext,
      SSLParameters sslParameters, Map<String, Object> clientProperties)
  {
    HashMap<String, Object> properties = new HashMap<>();
    properties.put(HttpClientFactory.HTTP_SSL_CONTEXT, sslContext);
    properties.put(HttpClientFactory.HTTP_SSL_PARAMS, sslParameters);
    properties.put(HttpClientFactory.HTTP_PROTOCOL_VERSION, HttpProtocolVersion.HTTP_1_1.name());
    properties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, "10000");

    merge(properties, clientProperties);

    return createClient(restOverStream, properties, httpClientFactory);
  }

  // ############# HTTP2 Clear Section #############

  public static Client createHttp2Client(HttpClientFactory httpClientFactory, boolean restOverStream)
  {
    return createHttp2Client(httpClientFactory, restOverStream, null);
  }

  public static Client createHttp2Client(HttpClientFactory httpClientFactory, boolean restOverStream,
      Map<String, Object> clientProperties)
  {
    HashMap<String, Object> properties = new HashMap<>();
    properties.put(HttpClientFactory.HTTP_PROTOCOL_VERSION, HttpProtocolVersion.HTTP_2.name());
    properties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, "10000");

    merge(properties, clientProperties);

    return createClient(restOverStream, properties, httpClientFactory);
  }

  // ############# HTTPS 2 Section #############

  public static Client createHttps2Client(HttpClientFactory httpClientFactory, boolean restOverStream, SSLContext sslContext,
      SSLParameters sslParameters)
  {
    return createHttps2Client(httpClientFactory, restOverStream, sslContext,
        sslParameters, null);
  }


  public static Client createHttps2Client(HttpClientFactory httpClientFactory, boolean restOverStream, SSLContext sslContext,
      SSLParameters sslParameters, Map<String, Object> clientProperties)
  {
    HashMap<String, Object> properties = new HashMap<>();
    properties.put(HttpClientFactory.HTTP_SSL_CONTEXT, sslContext);
    properties.put(HttpClientFactory.HTTP_SSL_PARAMS, sslParameters);
    properties.put(HttpClientFactory.HTTP_PROTOCOL_VERSION, HttpProtocolVersion.HTTP_2.name());

    merge(properties, clientProperties);

    return createClient(restOverStream, properties, httpClientFactory);
  }

  // ############# Tools Section #############

  public static URI createURI(int port, URI relativeURI, boolean isSsl)
  {
    String scheme = isSsl ? "https" : "http";
    String url = scheme + "://localhost:" + port;

    if (relativeURI != null)
    {
      url += relativeURI;
    }

    return URI.create(url);
  }


  public static URI createHttpURI(int port, URI relativeURI)
  {
    return createURI(port, relativeURI, false);
  }

  public static URI createHttpsURI(URI relativeURI)
  {
    return createHttpsURI(HTTPS_PORT, relativeURI);
  }

  public static URI createHttpsURI(int port, URI relativeURI)
  {
    return createURI(port, relativeURI, true);
  }

  public static URI getEchoURI()
  {
    return ECHO_URI;
  }

  public static URI getOnExceptionEchoURI()
  {
    return ON_EXCEPTION_ECHO_URI;
  }

  public static URI getThrowingEchoURI()
  {
    return THROWING_ECHO_URI;
  }

  private static TransportDispatcher createDispatcher()
  {
    return new TransportDispatcherBuilder()
            .addRestHandler(ECHO_URI, new RestEchoServer(new EchoServiceImpl()))
            .addRestHandler(ON_EXCEPTION_ECHO_URI, new RestEchoServer(new OnExceptionEchoService()))
            .addRestHandler(THROWING_ECHO_URI, new RestEchoServer(new ThrowingEchoService()))
            .build();
  }
}
