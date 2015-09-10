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


import com.linkedin.restli.internal.server.methods.response.ErrorResponseBuilder;
import com.linkedin.restli.server.filter.RequestFilter;
import com.linkedin.restli.server.filter.ResponseFilter;
import com.linkedin.restli.server.multiplexer.MultiplexerSingletonFilter;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Configuration for rest.li servers.
 *
 * @author dellamag
 */
public class RestLiConfig
{

  /**
   * Default value for the maximum number of individual requests allowed in a multiplexed request.
   */
  private static final int DEFAULT_MAX_REQUESTS_MULTIPLEXED = 20;

  /**
   * @deprecated There is no longer a notion of strict v.s. relaxed checking. The only check is that the version used
   * by the client is between {@link com.linkedin.restli.internal.common.AllProtocolVersions#OLDEST_SUPPORTED_PROTOCOL_VERSION}
   *  and {@link com.linkedin.restli.internal.common.AllProtocolVersions#NEXT_PROTOCOL_VERSION}
   */
  @Deprecated
  public static enum RestliProtocolCheck
  {
    /**
     * Check that the client supplied protocol version lies between
     * {@link com.linkedin.restli.internal.common.AllProtocolVersions#BASELINE_PROTOCOL_VERSION} and
     * {@link com.linkedin.restli.internal.common.AllProtocolVersions#NEXT_PROTOCOL_VERSION}.
     */
    RELAXED,

    /**
     * Check that the client supplied protocol version lies between
     * {@link com.linkedin.restli.internal.common.AllProtocolVersions#BASELINE_PROTOCOL_VERSION} and
     * {@link com.linkedin.restli.internal.common.AllProtocolVersions#LATEST_PROTOCOL_VERSION}.
     */
    STRICT;
  }

  private final Set<String> _resourcePackageNames = new HashSet<String>();
  private final Set<String> _resourceClassNames = new HashSet<String>();
  private URI _serverNodeUri = URI.create("");
  private RestLiDocumentationRequestHandler _documentationRequestHandler = null;
  private ErrorResponseFormat _errorResponseFormat = ErrorResponseFormat.FULL;
  private String _internalErrorMessage = ErrorResponseBuilder.DEFAULT_INTERNAL_ERROR_MESSAGE;
  private RestliProtocolCheck _restliProtocolCheck = RestliProtocolCheck.STRICT;
  private List<RestLiDebugRequestHandler> _debugRequestHandlers;
  private final List<RequestFilter> _requestFilters = new ArrayList<RequestFilter>();
  private final List<ResponseFilter> _responseFilters = new ArrayList<ResponseFilter>();
  private int _maxRequestsMultiplexed = DEFAULT_MAX_REQUESTS_MULTIPLEXED;
  private MultiplexerSingletonFilter _multiplexerSingletonFilter;

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
    _debugRequestHandlers = new ArrayList<RestLiDebugRequestHandler>();
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
   * @param packageNames collection of package names to be scanned
   */
  public void addResourcePackageNames(final Collection<String> packageNames)
  {
    _resourcePackageNames.addAll(packageNames);
  }

  /**
   * @param packageNames array of package names to be scanned
   */
  public void addResourcePackageNames(final String... packageNames)
  {
    _resourcePackageNames.addAll(Arrays.asList(packageNames));
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
   * @param classNames collection of specific resource class names to be loaded
   */
  public void addResourceClassNames(final Collection<String> classNames)
  {
    _resourceClassNames.addAll(classNames);
  }

  /**
   * @param classNames array of specific resource class names to be loaded
   */
  public void addResourceClassNames(final String... classNames)
  {
    _resourceClassNames.addAll(Arrays.asList(classNames));
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

  /**
   * Gets the list of debug request handlers in this Rest.li config.
   * @return the list of debug request handlers.
   */
  public List<RestLiDebugRequestHandler> getDebugRequestHandlers()
  {
    return _debugRequestHandlers;
  }

  /**
   * Adds a number of debug request handlers to this Rest.li config.
   * @param handlers The debug request handlers to add.
   */
  public void addDebugRequestHandlers(final RestLiDebugRequestHandler... handlers)
  {
    _debugRequestHandlers.addAll(Arrays.asList(handlers));
  }

  /**
   * Adds a number of debug request handlers to this Rest.li config.
   * @param handlers The debug request handlers to add.
   */
  public void addDebugRequestHandlers(final Collection<RestLiDebugRequestHandler> handlers)
  {
    _debugRequestHandlers.addAll(handlers);
  }

  /**
   * Sets the list of debug request handlers on this Rest.li config.
   * @param handlers The debug request handlers to set.
   */
  public void setDebugRequestHandlers(final List<RestLiDebugRequestHandler> handlers)
  {
    _debugRequestHandlers = new ArrayList<RestLiDebugRequestHandler>(handlers);
  }

  /**
   * Get the multiplexer singleton filter
   * @return a multiplexer singletson filter
   */
  public MultiplexerSingletonFilter getMultiplexerSingletonFilter()
  {
    return _multiplexerSingletonFilter;
  }

  /**
   * Set the multiplexer singleton filter.
   * @param multiplexerSingletonFilter the multiplexer singleton filter to set
   */
  public void setMultiplexerSingletonFilter(final MultiplexerSingletonFilter multiplexerSingletonFilter)
  {
    _multiplexerSingletonFilter = multiplexerSingletonFilter;
  }

  public ErrorResponseFormat getErrorResponseFormat()
  {
    return _errorResponseFormat;
  }

  public void setErrorResponseFormat(ErrorResponseFormat errorResponseFormat)
  {
    this._errorResponseFormat = errorResponseFormat;
  }

  public String getInternalErrorMessage()
  {
    return _internalErrorMessage;
  }

  public void setInternalErrorMessage(String internalErrorMessage)
  {
    _internalErrorMessage = internalErrorMessage;
  }

  /**
   * @deprecated There is no longer a notion of strict v.s. relaxed checking. The only check is that the version used
   * by the client is between {@link com.linkedin.restli.internal.common.AllProtocolVersions#OLDEST_SUPPORTED_PROTOCOL_VERSION}
   *  and {@link com.linkedin.restli.internal.common.AllProtocolVersions#NEXT_PROTOCOL_VERSION}
   * @param restliProtocolCheck
   */
  @Deprecated
  public void setRestliProtocolCheck(RestliProtocolCheck restliProtocolCheck)
  {
    if (restliProtocolCheck == null)
    {
      throw new IllegalArgumentException("Cannot be null!");
    }
    _restliProtocolCheck = restliProtocolCheck;
  }

  /**
   * @deprecated There is no longer a notion of strict v.s. relaxed checking. The only check is that the version used
   * by the client is between {@link com.linkedin.restli.internal.common.AllProtocolVersions#OLDEST_SUPPORTED_PROTOCOL_VERSION}
   *  and {@link com.linkedin.restli.internal.common.AllProtocolVersions#NEXT_PROTOCOL_VERSION}
   */
  @Deprecated
  public RestliProtocolCheck getRestliProtocolCheck()
  {
    return _restliProtocolCheck;
  }

  /**
   * Add request filters to the filter chain.
   *
   * @param filters
   *          Ordered list of filters to be added to the filter chain.
   */
  public void addRequestFilter(RequestFilter...filters)
  {
    _requestFilters.addAll(Arrays.asList(filters));
  }

  /**
   * Get a mutable reference to the request filter chain.
   *
   * @return Mutable reference to the request filter chain.
   */
  public List<RequestFilter> getRequestFilters()
  {
    return _requestFilters;
  }

  /**
   * Set the request filter chain.
   *
   * @param requestFilters
   *          Ordered list of request filters.
   */
  public void setRequestFilters(List<? extends RequestFilter> requestFilters)
  {
    if (requestFilters != null) {
      _requestFilters.clear();
      _requestFilters.addAll(requestFilters);
    }
  }

  /**
   * Add response filters to the filter chain.
   *
   * @param filters
   *          Ordered list of filters to be added to the filter chain.
   */
  public void addResponseFilter(ResponseFilter...filters)
  {
    _responseFilters.addAll(Arrays.asList(filters));
  }

  /**
   * Get a mutable reference to the response filter chain.
   *
   * @return Mutable reference to the response filter chain.
   */
  public List<ResponseFilter> getResponseFilters()
  {
    return _responseFilters;
  }

  /**
   * Set the response filter chain.
   *
   * @param responseFilters
   *          Ordered list of response filters.
   */
  public void setResponseFilters(List<? extends ResponseFilter> responseFilters)
  {
    if (responseFilters != null) {
      _responseFilters.clear();
      _responseFilters.addAll(responseFilters);
    }
  }

  /**
   * Get the maximum number of individual requests allowed in a multiplexed request.
   *
   * @return the maximum number of requests
   */
  public int getMaxRequestsMultiplexed()
  {
    return _maxRequestsMultiplexed;
  }

  /**
   * Sets the maximum number of individual requests allowed in a multiplexed request.
   *
   * @param maxRequestsMultiplexed the maximum number of requests
   */
  public void setMaxRequestsMultiplexed(int maxRequestsMultiplexed)
  {
    _maxRequestsMultiplexed = maxRequestsMultiplexed;
  }
}
