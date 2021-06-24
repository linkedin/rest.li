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
package com.linkedin.restli.server;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.r2.transport.common.StreamRequestHandlerAdapter;
import com.linkedin.r2.transport.http.server.AbstractR2Servlet;
import com.linkedin.r2.transport.http.server.AsyncR2Servlet;
import com.linkedin.restli.server.resources.PrototypeResourceFactory;
import com.linkedin.restli.server.resources.ResourceFactory;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;

import com.linkedin.r2.transport.http.server.RAPServlet;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Restli HttpServlet implementation.
 *
 * <p>Resource packages are provided as a servlet config init parameter in the web.xml.  If
 * providing multiple resource packages, separate them by with the ';' character.</p>
 *
 * <pre><code>
 * <web-app>
 * ...
 *  <servlet>
 *    <servlet-class>com.linkedin.restli.server.RestliServlet</servlet-class>
 *      <init-param>
 *        <param-name>resourcePackages</param-name>
 *        <param-value>com.example.package.one;com.example.package.two</param-value>
 *      </init-param>
 *      ...
 * </code></pre>
 *
 * <p>Async request handling is available for Servlet API 3.0 or greater and enabled by default for 3.0+.</p>
 *
 * <p>To use async in Servlet containers, async-supported must be set to true in your web.xml, e.g.:</p>
 *
 * <pre>
 * <code>
 *   <servlet>
 *     ...
 *     <async-supported>true</async-supported>
 *     ...
 *   </servlet>
 * </code>
 * </pre>
 *
 * <p>Async can be further configured by the rest.li uscAsync servlet param (which defaults to true for servlet API 3.0+
 * servlet containers).  And the asyncTimeout param can be set to a desired maximum timeout value in milliseconds, e.g.:</p>
 *
 * <pre><code>
 *   <init-param>
 *     <param-name>useAysnc</param-name>
 *     <param-value>true</param-value>
 *   </init-param>
 *   <init-param>
 *     <param-name>asyncTimeout</param-name>
 *     <param-value>30000</param-value>
 *   </init-param>
 * </code></pre>
 *
 * <p>A parseqThreadPoolSize param may also be optionally set using the same init-param style.  If absent,
 * the size defaults to the processor core count plus one.  This setting controls the thread pool used
 * for parseq outbound requests ONLY.</p>
 *
 * <p>To set the number of server threads please refer to the documentation for the particular servlet container
 * you are using.</p>
 *
 * <p>JSR-330 Dependency Injection (@Named, @Inject) is not available on Resource classes when using this servlet.
 * If dependency injection is needed, please see rest.li documentation about available integrations with dependency
 * injection frameworks (guice, spring...).</p>
 *
 * @author Joe Betz
 */
public class RestliServlet extends HttpServlet
{
  private static final Logger log = LoggerFactory.getLogger(RestliServlet.class);

  private static final long serialVersionUID = 1L;
  private static final String RESOURCE_PACKAGES_PARAM = "resourcePackages";
  private static final String PAR_SEQ_THREAD_POOL_SIZE = "parSeqThreadPoolSize";
  private static final String PACKAGE_PARAM_SEPARATOR = ";";
  private static final String USE_ASYNC_PARAM = "useAsync";
  private static final String ASYNC_TIMEOUT_PARAM = "asyncTimeout";
  public static final int DEFAULT_ASYNC_TIMEOUT = 30000;

  private HttpServlet _r2Servlet;

  @Override
  public void init(ServletConfig servletConfig) throws ServletException
  {
    super.init(servletConfig);
    _r2Servlet = buildR2ServletFromServletParams(servletConfig);
    _r2Servlet.init(servletConfig);
  }

  private AbstractR2Servlet buildR2ServletFromServletParams(ServletConfig servletConfig)
  {
    ResourceFactory resourceFactory = new PrototypeResourceFactory();

    RestLiConfig config = new RestLiConfig();
    config.setResourcePackageNamesSet(getResourcePackageSet(servletConfig));

    final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(getParseqThreadPoolSize(servletConfig));
    Engine engine = new EngineBuilder()
        .setTaskExecutor(scheduler)
        .setTimerScheduler(scheduler)
        .build();

    final RestLiServer restLiServer = new RestLiServer(
        config,
        resourceFactory,
        engine);
    DelegatingTransportDispatcher dispatcher = new DelegatingTransportDispatcher(restLiServer, restLiServer);

    boolean useAsync = getUseAsync(servletConfig);
    long asyncTimeOut = getAsyncTimeout(servletConfig);

    if (useAsync && servletConfig.getServletContext().getMajorVersion() < 3)
    {
      throw new IllegalArgumentException("This servlet is configured with useAsync=true, but the current servlet " +
                                             "context does not support the required Servlet API 3.0.");

    }
    if (!useAsync)
    {
      log.info("Initializing Rest.li with a thread based request handling.  Set useAsync=true on a Servlet API 3.0 container to enable Rest.li's async servlet.");
      return new RAPServlet(dispatcher);
    }
    else
    {
      log.info("Initializing Rest.li with an async request handling enabled.");
      return new AsyncR2Servlet(dispatcher, asyncTimeOut);
    }
  }

  private boolean getUseAsync(ServletConfig servletConfig)
  {
    String useAsync = servletConfig.getInitParameter(USE_ASYNC_PARAM);
    if(useAsync != null)
    {
      return Boolean.parseBoolean(useAsync);
    }
    else
    {
      return servletConfig.getServletContext().getMajorVersion() >= 3;
    }
  }

  private long getAsyncTimeout(ServletConfig servletConfig)
  {
    String asyncTimeout = servletConfig.getInitParameter(ASYNC_TIMEOUT_PARAM);
    if(asyncTimeout != null)
    {
      return Long.parseLong(asyncTimeout);
    }
    else
    {
      return DEFAULT_ASYNC_TIMEOUT;
    }
  }

  private Set<String> getResourcePackageSet(ServletConfig servletConfig)
  {
    String resourcePackages = servletConfig.getInitParameter(RESOURCE_PACKAGES_PARAM);
    Set<String> resourcePackageSet = new HashSet<>();
    for(String resourcePackage : resourcePackages.split(PACKAGE_PARAM_SEPARATOR))
    {
      resourcePackageSet.add(resourcePackage.trim());
    }
    return resourcePackageSet;
  }

  private int getParseqThreadPoolSize(ServletConfig servletConfig)
  {
    int threadPoolSize;

    String threadPoolSizeStr = servletConfig.getInitParameter(PAR_SEQ_THREAD_POOL_SIZE);
    if(StringUtils.isNumeric(threadPoolSizeStr) && !threadPoolSizeStr.isEmpty())
    {
      threadPoolSize = Integer.valueOf(threadPoolSizeStr);
    }
    else
    {
      final int numCores = Runtime.getRuntime().availableProcessors();
      threadPoolSize = numCores + 1;
    }
    return threadPoolSize;
  }

  @Override
  public void destroy()
  {
    _r2Servlet.destroy();
  }

  @Override
  public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException
  {
    _r2Servlet.service(req, res);
  }
}
