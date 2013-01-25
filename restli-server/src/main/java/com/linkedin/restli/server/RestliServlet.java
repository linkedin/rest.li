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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;

import com.linkedin.r2.transport.http.server.RAPServlet;

/**
 * Restli HttpServlet implementation.
 * 
 * Resource packages are provided as a servlet config init parameter in the web.xml.  If
 * providing multiple resource packages, separate them by with the ';' character.
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
 * @author Joe Betz
 */

public class RestliServlet extends HttpServlet
{
  private static final long serialVersionUID = 1L;
  private static final String RESOURCE_PACKAGES_PARAM = "resourcePackages";
  private static final String PACKAGE_PARAM_SEPARATOR = ";";

  private HttpServlet r2Servlet;

  @Override
  public void init(ServletConfig servletConfig) throws ServletException
  {
    super.init(servletConfig);

    String resourcePackages = servletConfig.getInitParameter(RESOURCE_PACKAGES_PARAM);
    Set<String> resourcePackageSet = new HashSet<String>();
    for(String resourcePackage : resourcePackages.split(PACKAGE_PARAM_SEPARATOR))
    {
      resourcePackageSet.add(resourcePackage.trim());
    }

    RestLiConfig config = new RestLiConfig();
    config.setResourcePackageNamesSet(resourcePackageSet);
    r2Servlet = new RAPServlet(new DelegatingTransportDispatcher(new RestLiServer(config)));
    r2Servlet.init(servletConfig);
  }

  @Override
  public void destroy()
  {
    r2Servlet.destroy();
  }

  @Override
  public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException
  {
    r2Servlet.service(req, res);
  }
}
