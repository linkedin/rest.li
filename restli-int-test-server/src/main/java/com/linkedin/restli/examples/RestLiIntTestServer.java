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

package com.linkedin.restli.examples;


import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.r2.filter.CompressionConfig;
import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.compression.ServerCompressionFilter;
import com.linkedin.r2.filter.logging.SimpleLoggingFilter;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.http.server.HttpJettyServer;
import com.linkedin.r2.transport.http.server.HttpServer;
import com.linkedin.r2.transport.http.server.HttpServerFactory;
import com.linkedin.restli.docgen.DefaultDocumentationRequestHandler;
import com.linkedin.restli.examples.groups.server.api.GroupMembershipMgr;
import com.linkedin.restli.examples.groups.server.api.GroupMgr;
import com.linkedin.restli.examples.groups.server.impl.HashGroupMembershipMgr;
import com.linkedin.restli.examples.groups.server.impl.HashMapGroupMgr;
import com.linkedin.restli.examples.groups.server.rest.impl.GroupsRestApplication;
import com.linkedin.restli.server.DelegatingTransportDispatcher;
import com.linkedin.restli.server.ParseqTraceDebugRequestHandler;
import com.linkedin.restli.server.RestLiConfig;
import com.linkedin.restli.server.RestLiServer;
import com.linkedin.restli.server.filter.Filter;
import com.linkedin.restli.server.mock.InjectMockResourceFactory;
import com.linkedin.restli.server.mock.SimpleBeanProvider;
import com.linkedin.restli.server.resources.ResourceFactory;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


/**
 * @author dellamag
 */
public class RestLiIntTestServer
{
  public static final int      DEFAULT_PORT           = 1338;
  public static final int      NO_COMPRESSION_PORT    = 1339;
  public static final int      FILTERS_PORT           = 1340;
  public static final String   supportedCompression   = "gzip,snappy,x-snappy-framed,bzip2,deflate";
  public static final String[] RESOURCE_PACKAGE_NAMES = {
      "com.linkedin.restli.examples.groups.server.rest.impl",
      "com.linkedin.restli.examples.greetings.server",
      "com.linkedin.restli.examples.instrumentation.server",
      "com.linkedin.restli.examples.typeref.server"  };

  public static void main(String[] args) throws IOException
  {
    final int numCores = Runtime.getRuntime().availableProcessors();
    final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(numCores + 1);
    final Engine engine = new EngineBuilder()
        .setTaskExecutor(scheduler)
        .setTimerScheduler(scheduler)
        .build();

    HttpServer server = createServer(engine, DEFAULT_PORT, supportedCompression);
    server.start();

    System.out.println("HttpServer running on port " + DEFAULT_PORT + ". Press any key to stop server");
    System.in.read();

    server.stop();
    engine.shutdown();
  }

  public static HttpServer createServer(final Engine engine, int port, String supportedCompression)
  {
    return createServer(engine, port, supportedCompression, false, -1);
  }

  public static HttpServer createServer(final Engine engine,
                                        int port,
                                        String supportedCompression,
                                        boolean useAsyncServletApi,
                                        int asyncTimeOut)
  {
    return createServer(engine, port, supportedCompression, useAsyncServletApi, asyncTimeOut, new RestLiConfig());
  }

  public static HttpServer createServer(final Engine engine,
                                        int port,
                                        String supportedCompression,
                                        boolean useAsyncServletApi,
                                        int asyncTimeOut,
                                        RestLiConfig config)
  {
    final FilterChain fc = FilterChains.empty().addLastRest(new ServerCompressionFilter(supportedCompression,
                                                                                        new CompressionConfig(0)))
        .addLastRest(new SimpleLoggingFilter());
    return createServer(engine, port, useAsyncServletApi, asyncTimeOut, null, fc, true, true, true, config);
  }

  public static HttpServer createServer(Engine engine,
                                        int port,
                                        boolean useAsyncServletApi,
                                        int asyncTimeOut,
                                        List<? extends Filter> filters,
                                        FilterChain filterChain,
                                        boolean restOverStream)
  {
    return createServer(engine, port, useAsyncServletApi, asyncTimeOut, filters, filterChain, restOverStream,
            true, true);
  }

  public static HttpServer createServer(Engine engine,
                                        int port,
                                        boolean useAsyncServletApi,
                                        int asyncTimeOut,
                                        List<? extends Filter> filters,
                                        FilterChain filterChain,
                                        boolean restOverStream,
                                        boolean useDocumentHandler,
                                        boolean useDebugHandler)
  {
    return createServer(engine, port, useAsyncServletApi, asyncTimeOut, filters, filterChain, restOverStream,
        useDocumentHandler, useDebugHandler, new RestLiConfig());
  }

  public static HttpServer createServer(Engine engine,
                                        int port,
                                        boolean useAsyncServletApi,
                                        int asyncTimeOut,
                                        List<? extends Filter> filters,
                                        FilterChain filterChain,
                                        boolean restOverStream,
                                        boolean useDocumentHandler,
                                        boolean useDebugHandler,
                                        RestLiConfig config)
  {
    config.addResourcePackageNames(RESOURCE_PACKAGE_NAMES);
    config.setServerNodeUri(URI.create("http://localhost:" + port));
    if (useDocumentHandler)
    {
      config.setDocumentationRequestHandler(new DefaultDocumentationRequestHandler());
    }
    if (useDebugHandler)
    {
      config.addDebugRequestHandlers(new ParseqTraceDebugRequestHandler());
    }
    config.setFilters(filters);
    config.setUseStreamCodec(Boolean.parseBoolean(System.getProperty("test.useStreamCodecServer", "false")));

    GroupMembershipMgr membershipMgr = new HashGroupMembershipMgr();
    GroupMgr groupMgr = new HashMapGroupMgr(membershipMgr);
    GroupsRestApplication app = new GroupsRestApplication(groupMgr, membershipMgr);
    SimpleBeanProvider beanProvider = new SimpleBeanProvider();
    beanProvider.add("GroupsRestApplication", app);
    //using InjectMockResourceFactory to keep examples spring-free
    ResourceFactory factory = new InjectMockResourceFactory(beanProvider);

    RestLiServer restLiServer = new RestLiServer(config, factory, engine);
    TransportDispatcher dispatcher = new DelegatingTransportDispatcher(restLiServer, restLiServer);

    return new HttpServerFactory(filterChain).createServer(port,
            HttpServerFactory.DEFAULT_CONTEXT_PATH,
            HttpServerFactory.DEFAULT_THREAD_POOL_SIZE,
            dispatcher,
            useAsyncServletApi ?
                    HttpJettyServer.ServletType.ASYNC_EVENT :
                    HttpJettyServer.ServletType.RAP,
            asyncTimeOut,
            restOverStream);
  }
}
