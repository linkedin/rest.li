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

package com.linkedin.restli.example;


import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.http.server.HttpServer;
import com.linkedin.r2.transport.http.server.HttpServerFactory;
import com.linkedin.restli.docgen.DefaultDocumentationRequestHandler;
import com.linkedin.restli.example.impl.AlbumDatabaseImpl;
import com.linkedin.restli.example.impl.AlbumEntryDatabaseImpl;
import com.linkedin.restli.example.impl.PhotoDatabase;
import com.linkedin.restli.example.impl.PhotoDatabaseImpl;
import com.linkedin.restli.server.DelegatingTransportDispatcher;
import com.linkedin.restli.server.RestLiConfig;
import com.linkedin.restli.server.RestLiServer;
import com.linkedin.restli.server.mock.InjectMockResourceFactory;
import com.linkedin.restli.server.mock.SimpleBeanProvider;
import com.linkedin.restli.server.resources.ResourceFactory;

import java.io.IOException;
import java.net.URI;


/**
 * Basic example server to demonstrate a HTTP server with Rest.li feature.
 * Server will be listening on localhost:7279.
 *
 * @author dellamag
 */
public class RestLiExampleBasicServer
{
  public static void main(String[] args) throws Exception
  {
    final HttpServer server = createServer();

    startServer(server);

    System.out.println("Basic example server running on port " + SERVER_PORT + ". Press any key to stop server.");
    System.in.read();

    stopServer(server);
  }

  public static HttpServer createServer()
  {
    // create Rest.li resource class information and initialize documentation generator
    // only the resource classes in the specified package names are visible for public
    final RestLiConfig config = new RestLiConfig();
    config.addResourcePackageNames("com.linkedin.restli.example.impl");
    config.setServerNodeUri(URI.create(getServerUrl()));
    config.setDocumentationRequestHandler(new DefaultDocumentationRequestHandler());

    // demonstrate dynamic dependency injection
    final PhotoDatabase photoDb = new PhotoDatabaseImpl(10);
    final SimpleBeanProvider beanProvider = new SimpleBeanProvider();
    beanProvider.add("photoDb", photoDb);
    beanProvider.add("albumDb", new AlbumDatabaseImpl(10));
    beanProvider.add("albumEntryDb", new AlbumEntryDatabaseImpl(photoDb, 3));

    // using InjectMockResourceFactory to keep examples spring-free
    final ResourceFactory factory = new InjectMockResourceFactory(beanProvider);

    final TransportDispatcher dispatcher = new DelegatingTransportDispatcher(new RestLiServer(config, factory));
    return new HttpServerFactory(FilterChains.empty()).createServer(SERVER_PORT, dispatcher);
  }

  public static void startServer(HttpServer server) throws IOException
  {
    server.start();
  }

  public static void stopServer(HttpServer server) throws IOException
  {
    server.stop();
  }

  public static String getServerUrl()
  {
    return new StringBuilder("http://")
        .append(SERVER_HOSTNAME)
        .append(":")
        .append(SERVER_PORT)
        .toString();
  }

  private static final String SERVER_HOSTNAME = "localhost";
  private static final int SERVER_PORT = 7279;
}
