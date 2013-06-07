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


import com.linkedin.restli.server.util.FileClassNameScanner;

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
  private final Set<String> _resourceClassNames = new HashSet<String>();
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

  public Set<String> getResourceClassNamesSet()
  {
    return Collections.unmodifiableSet(_resourceClassNames);
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
   * @param packageNames set of package names to be scanned
   */
  public void setResourcePackageNamesSet(final Set<String> packageNames)
  {
    if (packageNames != null && !packageNames.isEmpty())
    {
      _resourcePackageNames.clear();
      _resourcePackageNames.addAll(packageNames);
    }
  }

  /**
   * @param packageNames array of package names to be scanned
   */
  public void addResourcePackageNames(final String... packageNames)
  {
    for (String pkg : packageNames)
    {
      _resourcePackageNames.add(pkg);
    }
  }

  /**
   * @param classNames set of class names to be loaded
   */
  public void setResourceClassNamesSet(final Set<String> classNames)
  {
    if (classNames != null && !classNames.isEmpty())
    {
      _resourceClassNames.clear();
      _resourceClassNames.addAll(classNames);
    }
  }

  /**
   * @param classNames array of specific resource class names to be loaded
   */
  public void addResourceClassNames(final String... classNames)
  {
    for (String clazz : classNames)
    {
      _resourceClassNames.add(clazz);
    }
  }

  /**
   * @param paths
   */
  public void addResourceDir(final String... paths)
  {
    for (String path : paths)
    {
      for (String clazz : FileClassNameScanner.scan(path))
      {
        _resourceClassNames.add(clazz);
      }
    }
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
