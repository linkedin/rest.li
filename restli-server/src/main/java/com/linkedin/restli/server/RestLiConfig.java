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

import com.linkedin.data.codec.DataCodec;
import com.linkedin.restli.common.ContentType;
import com.linkedin.restli.internal.server.methods.DefaultMethodAdapterProvider;
import com.linkedin.restli.internal.server.methods.MethodAdapterProvider;
import com.linkedin.restli.internal.server.response.ErrorResponseBuilder;
import com.linkedin.restli.server.config.RestLiMethodConfig;
import com.linkedin.restli.server.config.RestLiMethodConfigBuilder;
import com.linkedin.restli.server.filter.Filter;
import com.linkedin.restli.server.multiplexer.MultiplexerRunMode;
import com.linkedin.restli.server.multiplexer.MultiplexerSingletonFilter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


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
  public enum RestliProtocolCheck
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

  private final Set<String> _resourcePackageNames = new HashSet<>();
  private final Set<String> _resourceClassNames = new HashSet<>();
  private URI _serverNodeUri = URI.create("");
  private RestLiDocumentationRequestHandler _documentationRequestHandler = null;
  private ErrorResponseFormat _errorResponseFormat = ErrorResponseFormat.FULL;
  private String _internalErrorMessage = ErrorResponseBuilder.DEFAULT_INTERNAL_ERROR_MESSAGE;
  private RestliProtocolCheck _restliProtocolCheck = RestliProtocolCheck.STRICT;
  private List<RestLiDebugRequestHandler> _debugRequestHandlers = new ArrayList<>();
  private List<NonResourceRequestHandler> _customRequestHandlers = new ArrayList<>();
  private final List<Filter> _filters = new ArrayList<>();
  private int _maxRequestsMultiplexed = DEFAULT_MAX_REQUESTS_MULTIPLEXED;
  private Set<String> _individualRequestHeaderWhitelist = Collections.emptySet();
  private MultiplexerSingletonFilter _multiplexerSingletonFilter;
  private MultiplexerRunMode _multiplexerRunMode = MultiplexerRunMode.MULTIPLE_PLANS;
  private final List<ContentType> _customContentTypes = new LinkedList<>();
  private List<String> _supportedAcceptTypes;
  private final List<ResourceDefinitionListener> _resourceDefinitionListeners = new ArrayList<>();
  private boolean _useStreamCodec = false;

  // configuration for whether to validate any type of resource entity keys Ex. path keys or keys in batch request
  private boolean _validateResourceKeys = false;

  // config flag for determine restli server to fill-in default values or not
  private boolean _fillInDefaultValues = false;

  // resource method level configuration
  private RestLiMethodConfig _methodConfig;

  /** configuration for whether to attach stacktrace for {@link com.linkedin.r2.message.rest.RestException} */
  private boolean _writableStackTrace = true;
  private MethodAdapterProvider _methodAdapterProvider = null;

  /**
   * Constructor.
   */
  public RestLiConfig()
  {
    this(Collections.emptyMap());
  }

  /**
   * @param mapConfig not currently used
   * @deprecated Map of config properties is not supported. There is no replacement.
   */
  @Deprecated
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

      // The commas could have spaces around them like com.linkedin.foo , com.linkedin.bar.
      // For such cases, trim those spaces.
      addResourcePackageNames(
          Arrays.stream(commaDelimitedResourcePackageNames.split(","))
              .map(String::trim)
              .collect(Collectors.toSet()));
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
    _debugRequestHandlers = new ArrayList<>(handlers);
  }

  /**
   * Gets the list of custom request handlers in this Rest.li config.
   * @return the list of custom request handlers.
   */
  public List<NonResourceRequestHandler> getCustomRequestHandlers()
  {
    return _customRequestHandlers;
  }

  /**
   * Adds a number of custom request handlers to this Rest.li config.
   * @param handlers The custom request handlers to add.
   */
  public void addCustomRequestHandlers(final NonResourceRequestHandler... handlers)
  {
    _customRequestHandlers.addAll(Arrays.asList(handlers));
  }

  /**
   * Adds a number of custom request handlers to this Rest.li config.
   * @param handlers The custom request handlers to add.
   */
  public void addCustomRequestHandlers(final Collection<NonResourceRequestHandler> handlers)
  {
    _customRequestHandlers.addAll(handlers);
  }

  /**
   * Sets the list of custom request handlers on this Rest.li config.
   * @param handlers The custom request handlers to set.
   */
  public void setCustomRequestHandlers(final List<NonResourceRequestHandler> handlers)
  {
    if (handlers != null)
    {
      _customRequestHandlers = handlers;
    } else
    {
      throw new IllegalArgumentException("Invalid custom request handlers. Handlers can not be null.");
    }
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
   * Add filters to the filter list
   *
   * @param filters List of filters to be added to the filter list. Filters are the front of the list will be invoked
   *                first on requests and will be invoked last on responses.
   */
  public void addFilter(Filter...filters)
  {
    _filters.addAll(Arrays.asList(filters));
  }

  /**
   * Get a mutable reference to the filter list
   *
   * @return Mutable reference to the filter list
   */
  public List<Filter> getFilters()
  {
    return _filters;
  }

  /**
   * Sets the filters stored in the filter list. Filters at the front of the list will be invoked first on requests
   * and will be invoked last on responses
   *
   * @param filters List of filters.
   */
  public void setFilters(List<? extends Filter> filters)
  {
    if (filters != null) {
      _filters.clear();
      _filters.addAll(filters);
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

  /**
   * Get the set of request header names that are allowed to be used in an IndividualRequest of a multiplexed request.
   * @return a set of request header names
   */
  public Set<String> getMultiplexedIndividualRequestHeaderWhitelist()
  {
    return _individualRequestHeaderWhitelist;
  }

  /**
   * Set the set of request header names that are allowed to be used in an IndividualRequest of a multiplexed request.
   * @param headerNames a set of request header names
   */
  public void setMultiplexedIndividualRequestHeaderWhitelist(Set<String> headerNames)
  {
    _individualRequestHeaderWhitelist = (headerNames != null) ? headerNames : Collections.<String>emptySet();
  }

  public MultiplexerRunMode getMultiplexerRunMode()
  {
    return _multiplexerRunMode;
  }

  /**
   * Set the MultiplexedRequest run mode. MultiplexerRunMode specifies if all requests belonging to the
   * {@code MultiplexedRequest} will be executed as a single ParSeq plan ({@link MultiplexerRunMode#SINGLE_PLAN}) or if each request
   * that belongs to the {@code MultiplexedRequest} will be executed as a separate ParSeq plan ({@link MultiplexerRunMode#MULTIPLE_PLANS}).
   * {@link MultiplexerRunMode#SINGLE_PLAN} allows optimizations such as batching but it means that all tasks will be
   * executed in sequence. {@link MultiplexerRunMode#MULTIPLE_PLANS} can potentially speed up execution because requests
   * can execute physically in parallel but some ParSeq optimization will not work across different plans.
   * @param multiplexerRunMode
   */
  public void setMultiplexerRunMode(MultiplexerRunMode multiplexerRunMode)
  {
    _multiplexerRunMode = multiplexerRunMode;
  }

  public List<ContentType> getCustomContentTypes()
  {
    return _customContentTypes;
  }

  /**
   * Add the given custom ContentType as a supported Content-Type for the restli server.
   * @param contentType custom Content-Type to add.
   */
  public void addCustomContentType(ContentType contentType)
  {
    _customContentTypes.add(contentType);
  }

  /**
   * Add the given mimeType as a supported Content-Type for the restli server and register the given codec as the one to
   * use for serialization.
   * @param mimeType custom Content-Type to add.
   * @param codec CODEC to use for encoding/decoding.
   */
  public void addCustomContentType(String mimeType, DataCodec codec)
  {
    assert mimeType != null : "Mimetype cannot be null";
    assert codec != null : "Codec cannot be null";
    _customContentTypes.add(ContentType.createContentType(mimeType.toLowerCase(), codec));
  }

  /**
   * Adds a {@link ResourceDefinitionListener}. The listener is notified when {@link ResourceDefinition}s are initialized.
   */
  public void addResourceDefinitionListener(ResourceDefinitionListener listener)
  {
    _resourceDefinitionListeners.add(listener);
  }

  /**
   * Gets the <code>ResourceDefinitionListener</code>s.
   */
  List<ResourceDefinitionListener> getResourceDefinitionListeners()
  {
    return _resourceDefinitionListeners;
  }

  /**
   * Gets whether or not to use {@link com.linkedin.data.codec.entitystream.StreamDataCodec} to decode {@link com.linkedin.r2.message.stream.StreamRequest}
   * and encode {@link com.linkedin.r2.message.stream.StreamResponse}. If not, the implementation falls back to use
   * {@link DataCodec} on adapted {@link com.linkedin.r2.message.rest.RestRequest} and {@link com.linkedin.r2.message.rest.RestResponse}.
   */
  public boolean isUseStreamCodec()
  {
    return _useStreamCodec;
  }

  /**
   * Sets whether or not to use {@link com.linkedin.data.codec.entitystream.StreamDataCodec} to decode {@link com.linkedin.r2.message.stream.StreamRequest}
   * and encode {@link com.linkedin.r2.message.stream.StreamResponse}. If not, the implementation falls back to use
   * {@link DataCodec} on adapted {@link com.linkedin.r2.message.rest.RestRequest} and {@link com.linkedin.r2.message.rest.RestResponse}.
   */
  public void setUseStreamCodec(boolean useStreamCodec)
  {
    _useStreamCodec = useStreamCodec;
  }

  /**
   * Get resource method level configurations.
   * @return Resource method level configurations.
   */
  public RestLiMethodConfig getMethodConfig()
  {
    return _methodConfig;
  }

  /**
   * Set resource method level configurations.
   * @param methodConfig method level configurations.
   */
  public void setMethodConfig(RestLiMethodConfig methodConfig)
  {
    _methodConfig = methodConfig;
  }

  /**
   * Get whether resource key validation is enabled or not.
   */
  public boolean shouldValidateResourceKeys()
  {
    return _validateResourceKeys;
  }

  /**
   * Sets whether or not to enable resource key validation.
   */
  public void setValidateResourceKeys(boolean validateResourceKeys)
  {
    _validateResourceKeys = validateResourceKeys;
    setMethodConfig(
        new RestLiMethodConfigBuilder(getMethodConfig()).withShouldValidateResourceKeys(_validateResourceKeys).build());
  }

  /**
   * Get whether fill in stacktrace for {@link com.linkedin.r2.message.rest.RestException}
   */
  public boolean isWritableStackTrace()
  {
    return _writableStackTrace;
  }

  /**
   * Set whether fill in stacktrace for {@link com.linkedin.r2.message.rest.RestException}
   */
  public void setWritableStackTrace(boolean writableStackTrace)
  {
    _writableStackTrace = writableStackTrace;
  }

  /**
   * Get/Set for filling default values in restli response
   * check config to see if the data in result should fill in default in fields
   * @return boolean
   */
  public boolean shouldFillInDefaultValues()
  {
    return _fillInDefaultValues;
  }

  /**
   * set the flag to decide whether to fill in default values in result record's fields
   * @param fillInDefaultValues a boolean for the flag
   */
  public void setFillInDefaultValues(boolean fillInDefaultValues)
  {
    _fillInDefaultValues = fillInDefaultValues;
  }

  /**
   * Set a custom {@link MethodAdapterProvider} in the config.
   *
   * @param methodAdapterProvider a custom to be set in the config.
   */
  public void setMethodAdapterProvider(MethodAdapterProvider methodAdapterProvider)
  {
    _methodAdapterProvider = methodAdapterProvider;
  }

  /**
   * @return Return the custom {@link MethodAdapterProvider} in the config. Return null if no custom
   *   {@link MethodAdapterProvider} is provided, then the {@link DefaultMethodAdapterProvider} will be used for
   *   setting up rest.li server.
   */
  public MethodAdapterProvider getMethodAdapterProvider()
  {
    return Optional.ofNullable(_methodAdapterProvider)
            .orElse(new DefaultMethodAdapterProvider(new ErrorResponseBuilder(_errorResponseFormat)));
  }

  /**
   * Get list of supported mime types for response serialization.
   * @return list of mime types.
   */
  public List<String> getSupportedAcceptTypes()
  {
    return _supportedAcceptTypes;
  }

  /**
   * Sets list of supported mime types for response serialization.
   * @param supportedAcceptTypes list of mime types.
   */
  public void setSupportedAcceptTypes(List<String> supportedAcceptTypes)
  {
    _supportedAcceptTypes = supportedAcceptTypes;
  }
}
