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
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.http.server.HttpServer;
import com.linkedin.r2.transport.http.server.HttpServerFactory;
import com.linkedin.restli.docgen.DefaultDocumentationRequestHandler;
import com.linkedin.restli.examples.groups.server.api.GroupMembershipMgr;
import com.linkedin.restli.examples.groups.server.api.GroupMgr;
import com.linkedin.restli.examples.groups.server.impl.HashGroupMembershipMgr;
import com.linkedin.restli.examples.groups.server.impl.HashMapGroupMgr;
import com.linkedin.restli.examples.groups.server.rest.impl.GroupsRestApplication;
import com.linkedin.restli.server.DelegatingTransportDispatcher;
import com.linkedin.restli.server.RestLiConfig;
import com.linkedin.restli.server.RestLiServer;
import com.linkedin.restli.server.mock.InjectMockResourceFactory;
import com.linkedin.restli.server.mock.SimpleBeanProvider;
import com.linkedin.restli.server.resources.ResourceFactory;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

/**
 * @author dellamag
 */
public class RestLiIntTestServer
{
  public static final int PORT = 1338;

  public static void main(String[] args) throws IOException
  {
    final int numCores = Runtime.getRuntime().availableProcessors();
    final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(numCores + 1);
    final Engine engine = new EngineBuilder()
        .setTaskExecutor(scheduler)
        .setTimerScheduler(scheduler)
        .build();

    HttpServer server = createServer(engine);

    server.start();

    System.out.println("HttpServer running on port " + PORT + ". Press any key to stop server");
    System.in.read();

    server.stop();
    engine.shutdown();
  }

  public static HttpServer createServer(final Engine engine)
  {
    Properties properties = new Properties();
    properties.put("log4j.rootLogger", "INFO");
    PropertyConfigurator.configure(properties);
    BasicConfigurator.configure();

    RestLiConfig config = new RestLiConfig();
    config.addResourcePackageNames("com.linkedin.restli.examples.groups.server.rest.impl",
                                   "com.linkedin.restli.examples.greetings.server",
                                   "com.linkedin.restli.examples.typeref.server"
                                   );
    config.setServerNodeUri(URI.create("http://localhost:1338"));
    config.setDocumentationRequestHandler(new DefaultDocumentationRequestHandler());

    GroupMembershipMgr membershipMgr = new HashGroupMembershipMgr();
    GroupMgr groupMgr = new HashMapGroupMgr(membershipMgr);
    GroupsRestApplication app = new GroupsRestApplication(groupMgr, membershipMgr);
    SimpleBeanProvider beanProvider = new SimpleBeanProvider();
    beanProvider.add("GroupsRestApplication", app);
    //using InjectMockResourceFactory to keep examples spring-free
    ResourceFactory factory = new InjectMockResourceFactory(beanProvider);

    TransportDispatcher dispatcher = new DelegatingTransportDispatcher(new RestLiServer(config, factory, engine));
    return new HttpServerFactory(FilterChains.empty()).createServer(PORT, dispatcher);
  }
}
