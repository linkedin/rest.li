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
package com.linkedin.restli.server.guice;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.linkedin.parseq.Engine;
import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.transport.FilterChainDispatcher;
import com.linkedin.r2.transport.http.server.R2Servlet;
import com.linkedin.restli.server.DelegatingTransportDispatcher;
import com.linkedin.restli.server.RestLiConfig;
import com.linkedin.restli.server.RestLiServer;
import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;

/**
 * RestliServlet for Guice.
 *
 * This class is intended to be instantiated by Guice and exposed to a servlet container via Guice's ServletModule.
 *
 * @author jpbetz
 */
@Singleton
public class GuiceRestliServlet extends HttpServlet
{
  private static final long serialVersionUID = 1L;
  private final HttpServlet _r2Servlet;

  @Inject
  public GuiceRestliServlet(RestLiConfig config, // required
                            GuiceInjectResourceFactory resourceFactory, // injected automatically by Guice
                            OptionalFilterChain filterChain,
                            OptionalEngine engine)
  {
    _r2Servlet = new R2Servlet(
        new FilterChainDispatcher(
            new DelegatingTransportDispatcher(
                new RestLiServer(config, resourceFactory, engine.value)),
            filterChain.value
        )
    );
  }

  // Handle optional params, this is somewhat burdonsome to express in Guice, for details see:
  // https://code.google.com/p/google-guice/wiki/FrequentlyAskedQuestions#How_can_I_inject_optional_parameters_into_a_constructor?
  public static class OptionalFilterChain {
    @Inject(optional = true)
    public FilterChain value = FilterChains.empty();
  }

  public static class OptionalEngine {
    @Inject(optional = true)
    public Engine value = null;
  }

  @Override
  public void init(ServletConfig servletConfig) throws ServletException
  {
    super.init(servletConfig);
    _r2Servlet.init(servletConfig);
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
