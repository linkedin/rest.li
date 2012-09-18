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


import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author dellamag
 */
public class RestLiConfig
{
  private final Set<String> _resourcePackageNames = new HashSet<String>();
  private URI _serverNodeUri = URI.create("");
  private RestLiDocumentationRequestHandler _documentationRequestHandler = null;

  /**
   * Constructor.
   */
  public RestLiConfig()
  {
    this (Collections.<String, Object>emptyMap());
  }

  /**
   * @param mapConfig not currently used
   */
  public RestLiConfig(final Map<String, Object> mapConfig)
  {
  }

  public Set<String> getResourcePackageNamesSet()
  {
    return Collections.unmodifiableSet(_resourcePackageNames);
  }

  /**
   * @param commaDelimitedResourcePackageNames comma-delimited package names list
   */
  public void setResourcePackageNames(final String commaDelimitedResourcePackageNames)
  {
    if (commaDelimitedResourcePackageNames != null &&
        ! "".equals(commaDelimitedResourcePackageNames.trim()))
    {
      _resourcePackageNames.clear();
      addResourcePackageNames(commaDelimitedResourcePackageNames.split(","));
    }
  }

  /**
   * @param packageNames set of package names
   */
  public void setResourcePackageNamesSet(final Set<String> packageNames)
  {
    if (packageNames != null && packageNames.size() > 0)
    {
      _resourcePackageNames.clear();
      _resourcePackageNames.addAll(packageNames);
    }
  }

  /**
   * @param packageNames array of package names
   */
  public void addResourcePackageNames(final String... packageNames)
  {
    for (String pkg : packageNames)
    {
      _resourcePackageNames.add(pkg);
    }
  }

  /**
   * This method is retained for API compatibility, but has no effect.
   * Dynamic class loading is handled with the current thread's contextClassloader.
   * @param classLoader ignored
   */
  @Deprecated
  public void setClassLoader(final ClassLoader classLoader)
  {

  }

  public URI getServerNodeUri()
  {
    return _serverNodeUri;
  }

  public void setServerNodeUri(final URI serverNodeUri)
  {
    _serverNodeUri = serverNodeUri;
  }

  public RestLiDocumentationRequestHandler getDocumentationRequestHandler()
  {
    return _documentationRequestHandler;
  }

  public void setDocumentationRequestHandler(final RestLiDocumentationRequestHandler handler)
  {
    _documentationRequestHandler = handler;
  }
}
