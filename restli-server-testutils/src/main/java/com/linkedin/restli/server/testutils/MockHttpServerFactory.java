/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.server.testutils;


import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.logging.SimpleLoggingFilter;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.http.server.HttpJettyServer;
import com.linkedin.r2.transport.http.server.HttpServer;
import com.linkedin.r2.transport.http.server.HttpServerFactory;
import com.linkedin.restli.docgen.DefaultDocumentationRequestHandler;
import com.linkedin.restli.server.DelegatingTransportDispatcher;
import com.linkedin.restli.server.RestLiConfig;
import com.linkedin.restli.server.RestLiServer;
import com.linkedin.restli.server.mock.InjectMockResourceFactory;
import com.linkedin.restli.server.mock.SimpleBeanProvider;
import com.linkedin.restli.server.resources.ResourceFactory;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


/**
 * Factory to create a {@link HttpServer} that contains a {@link RestLiServer} to be used for testing a set of Rest.li
 * resources
 *
 * @author kparikh
 */
@SuppressWarnings("deprecation")
public class MockHttpServerFactory
{
  private static final String LOCALHOST = "http://localhost:";
  private static final int NUM_THREADS = 32;
  private static final int ASYNC_TIMEOUT = 5000;

  /**
   * Creates {@link RestLiConfig} to be used by a {@link RestLiServer}
   *
   * @param port the port the server will run on
   */
  private static RestLiConfig createConfig(int port)
  {
    RestLiConfig restLiConfig = new RestLiConfig();
    restLiConfig.setServerNodeUri(URI.create(LOCALHOST + port));
    restLiConfig.setDocumentationRequestHandler(new DefaultDocumentationRequestHandler());
    return restLiConfig;
  }

  /**
   * Creates a {@link ResourceFactory} to inject dependencies into your Rest.li resource
   *
   * @param beans
   */
  private static ResourceFactory createResourceFactory(Map<String, ?> beans)
  {
    SimpleBeanProvider beanProvider = new SimpleBeanProvider();
    for (Map.Entry<String, ?> entry: beans.entrySet())
    {
      beanProvider.add(entry.getKey(), entry.getValue());
    }
    return new InjectMockResourceFactory(beanProvider);
  }

  /**
   * Creates a {@link HttpServer} that contains a {@link RestLiServer} to be used for testing a set of Rest.li
   * resources.
   *
   * The {@link HttpServer} uses an empty {@link FilterChain} and uses "/" as the context path.
   *
   * If the server is run in async mode (by calling this function with the last parameter {@code true}), the
   * timeout used is {@link #ASYNC_TIMEOUT}.
   *
   * Both the async and sync servers will use {@link #NUM_THREADS} threads.
   *
   * The server is started by calling {@link com.linkedin.r2.transport.http.server.HttpServer#start()}
   * The server is stopped by calling {@link com.linkedin.r2.transport.http.server.HttpServer#stop()}
   *
   * @param port the port the server will run on on localhost
   * @param resourceClasses the Rest.li resource classes
   * @param beans beans you want to inject into your Rest.li resource.
   * @param enableAsync true if the server should be async, false otherwise
   */
  public static HttpServer create(int port,
                                  Set<Class<?>> resourceClasses,
                                  Map<String, ?> beans,
                                  boolean enableAsync)
  {
    RestLiConfig config = createConfig(port);
    Set<String> resourceClassNames = new HashSet<String>();
    for (Class<?> clazz: resourceClasses)
    {
      resourceClassNames.add(clazz.getName());
    }
    config.setResourceClassNamesSet(resourceClassNames);
    return create(port, config, beans, enableAsync);
  }

  /**
   * Creates a {@link HttpServer} that contains a {@link RestLiServer} to be used for testing a set of Rest.li
   * resources.
   *
   * The {@link HttpServer} uses an empty {@link FilterChain} and uses "/" as the context path.
   *
   * If the server is run in async mode (by calling this function with the last parameter {@code true}), the
   * timeout used is {@link #ASYNC_TIMEOUT}.
   *
   * Both the async and sync servers will use {@link #NUM_THREADS} threads.
   *
   * The server is started by calling {@link com.linkedin.r2.transport.http.server.HttpServer#start()}
   * The server is stopped by calling {@link com.linkedin.r2.transport.http.server.HttpServer#stop()}
   *
   * @param port the port the server will run on on localhost
   * @param resourcePackageNames the names of the packages that contain the Rest.li resources that you wish to serve
   * @param beans beans you want to inject into your Rest.li resource.
   * @param enableAsync true if the server should be async, false otherwise
   * @return a {@link HttpServer} created with the above parameters
   */
  public static HttpServer create(int port,
                                  String[] resourcePackageNames,
                                  Map<String, Object> beans,
                                  boolean enableAsync)
  {
    RestLiConfig config = createConfig(port);
    config.addResourcePackageNames(resourcePackageNames);
    return create(port, config, beans, enableAsync);
  }

  /**
   * Creates a {@link HttpServer} that contains a {@link RestLiServer} to be used for testing a set of Rest.li
   * resources.
   *
   * The {@link HttpServer} uses an empty {@link FilterChain} and uses "/" as the context path.
   *
   * If the server is run in async mode (by calling this function with the last parameter {@code true}), the
   * timeout used is {@link #ASYNC_TIMEOUT}.
   *
   * Both the async and sync servers will use {@link #NUM_THREADS} threads.
   *
   * @param port the port the server will run on on localhost
   * @param config the {@link RestLiConfig} to be used by the {@link RestLiServer}
   * @param beans beans you want to inject into your Rest.li resource.
   * @param enableAsync true if the server should be async, false otherwise
   */
  private static HttpServer create(int port, RestLiConfig config, Map<String, ?> beans, boolean enableAsync)
  {
    final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(NUM_THREADS);
    final ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
    EngineBuilder engineBuilder = new EngineBuilder().setTaskExecutor(scheduler).setTimerScheduler(scheduler);
    com.linkedin.parseq.AsyncCallableTask.register(engineBuilder, executor);

    final Engine engine = engineBuilder.build();
    ResourceFactory resourceFactory = createResourceFactory(beans);

    TransportDispatcher dispatcher = new DelegatingTransportDispatcher(new RestLiServer(config,
                                                                                        resourceFactory,
                                                                                        engine));

    final FilterChain fc = FilterChains.empty().addLastRest(new SimpleLoggingFilter());
    final HttpServer server = new HttpServerFactory(fc).createServer(port,
                                                               HttpServerFactory.DEFAULT_CONTEXT_PATH,
                                                               NUM_THREADS,
                                                               dispatcher,
                                                               enableAsync ? HttpJettyServer.ServletType.ASYNC_EVENT
                                                                   : HttpJettyServer.ServletType.RAP,
                                                               enableAsync ? ASYNC_TIMEOUT : -1);
    return new HttpServer()
    {
      @Override
      public void start()
          throws IOException
      {
        server.start();
      }

      @Override
      public void stop()
          throws IOException
      {
        server.stop();
        engine.shutdown();
        executor.shutdown();
        scheduler.shutdown();
      }

      @Override
      public void waitForStop()
          throws InterruptedException
      {
        server.waitForStop();
      }
    };
  }
}
