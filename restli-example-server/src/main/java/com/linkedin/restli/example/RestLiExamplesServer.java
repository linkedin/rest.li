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


import java.io.IOException;
import java.net.URI;

import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.http.server.HttpServer;
import com.linkedin.r2.transport.http.server.HttpServerFactory;
import com.linkedin.restli.docgen.DefaultDocumentationRequestHandler;
import com.linkedin.restli.example.impl.AlbumDatabase;
import com.linkedin.restli.example.impl.AlbumDatabaseImpl;
import com.linkedin.restli.example.impl.AlbumEntryDatabase;
import com.linkedin.restli.example.impl.AlbumEntryDatabaseImpl;
import com.linkedin.restli.example.impl.PhotoDatabase;
import com.linkedin.restli.example.impl.PhotoDatabaseImpl;
import com.linkedin.restli.server.DelegatingTransportDispatcher;
import com.linkedin.restli.server.RestLiConfig;
import com.linkedin.restli.server.RestLiServer;
import com.linkedin.restli.server.mock.InjectMockResourceFactory;
import com.linkedin.restli.server.mock.SimpleBeanProvider;
import com.linkedin.restli.server.resources.ResourceFactory;

/**
 * @author dellamag
 */
public class RestLiExamplesServer
{
  static final int PORT = 7279;

  public static void main(String[] args) throws IOException
  {
    HttpServer server = createServer();

    server.start();

    System.out.println("HttpServer running on port " + PORT + ". Press any key to stop server");
    System.in.read();

    server.stop();
  }

  public static HttpServer createServer()
  {
    RestLiConfig config = new RestLiConfig();
    config.addResourcePackageNames("com.linkedin.restli.example.impl");
    config.setServerNodeUri(URI.create("http://localhost:" + PORT));
    config.setDocumentationRequestHandler(new DefaultDocumentationRequestHandler());

    AlbumDatabase albumDb = new AlbumDatabaseImpl(10);
    AlbumEntryDatabase albumEntryDb = new AlbumEntryDatabaseImpl();
    PhotoDatabase photoDb = new PhotoDatabaseImpl(10);

    SimpleBeanProvider beanProvider = new SimpleBeanProvider();
    beanProvider.add("albumDb", albumDb);
    beanProvider.add("albumEntryDb", albumEntryDb);
    beanProvider.add("photoDb", photoDb);

    //using InjectMockResourceFactory to keep examples spring-free
    ResourceFactory factory = new InjectMockResourceFactory(beanProvider);

    TransportDispatcher dispatcher = new DelegatingTransportDispatcher(new RestLiServer(config, factory));
    return new HttpServerFactory(FilterChains.empty()).createServer(PORT, dispatcher);
  }
}
