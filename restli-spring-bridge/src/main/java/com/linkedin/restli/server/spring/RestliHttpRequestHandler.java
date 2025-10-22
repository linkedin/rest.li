/*
   Copyright (c) 2013 LinkedIn Corp.

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
package com.linkedin.restli.server.spring;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.transport.FilterChainDispatcher;
import org.springframework.web.HttpRequestHandler;

import com.linkedin.restli.server.RestLiServer;
import com.linkedin.restli.server.RestLiConfig;
import com.linkedin.restli.server.DelegatingTransportDispatcher;

import com.linkedin.r2.transport.http.server.RAPServlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


/**
 * This is a Spring specific http request handler for rest.li that can be used to
 * create a rest.li servlet.
 *
 *
 * <p>
 * To enable spring dependency injection, this class must to be instantiated by
 * spring as a bean so that it's dependencies, particularly SpringInjectResourceFactory (which
 * is ApplicationContextAware and must be instantiated by spring), can be injected into it.
 * </p>
 *
 * <p>
 * Once you have a bean of this class, a rest.li servlet can to a servlet container by creating a
 * HttpRequestHandlerServlet servlet with it's servlet-name set to th name of the spring bean.
 *
 * For example, given a RestliHttpRequestHandler spring bean named "restliRequestHandler", this web.xml
 * will start it as a servlet:
 *
 * <pre>
 * {@code
 * <servlet>
 *    <servlet-name>restliRequestHandler</servlet-name>
 *    <servlet-class>org.springframework.web.context.support.HttpRequestHandlerServlet</servlet-class>
 * </servlet>
 *
 * <servlet-mapping>
 *   <servlet-name>restliRequestHandler</servlet-name>
 *   <url-pattern>/*</url-pattern>
 * </servlet-mapping>
 * }
 * </pre>
 *
 * For details, see: <a href="http://static.springsource.org/spring-framework/docs/3.2.0.RC1/api/org/springframework/web/context/support/HttpRequestHandlerServlet.html">HttpRequestHandlerServlet Javadoc</a>
 * </p>
 *
 * @author jpbetz
 */
public class RestliHttpRequestHandler implements HttpRequestHandler {

  private RAPServlet _r2Servlet;

  public RestliHttpRequestHandler(RestLiConfig config, SpringInjectResourceFactory injectResourceFactory)
  {
    this(config, injectResourceFactory, FilterChains.empty());
  }

  public RestliHttpRequestHandler(RestLiConfig config,
                                  SpringInjectResourceFactory injectResourceFactory,
                                  FilterChain filterChain)
  {
    this(config, injectResourceFactory, filterChain, null);
  }

  public RestliHttpRequestHandler(RestLiConfig config,
                                  SpringInjectResourceFactory injectResourceFactory,
                                  FilterChain filterChain,
                                  ScheduledExecutorService scheduledExecutorService)
  {
    Engine parseqEngine = null;
    if (scheduledExecutorService != null) {
      parseqEngine = new EngineBuilder()
              .setTaskExecutor(scheduledExecutorService)
              .setTimerScheduler(scheduledExecutorService)
              .build();
    }

    RestLiServer restLiServer = new RestLiServer(config, injectResourceFactory, parseqEngine);
    _r2Servlet = new RAPServlet(
            new FilterChainDispatcher(
                    new DelegatingTransportDispatcher(restLiServer, restLiServer),
                    filterChain
            )
    );
  }

  public RestliHttpRequestHandler(RAPServlet r2Servlet)
  {
    _r2Servlet = r2Servlet;
  }

  public void handleRequest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
  {
    _r2Servlet.service(req, res);
  }
}