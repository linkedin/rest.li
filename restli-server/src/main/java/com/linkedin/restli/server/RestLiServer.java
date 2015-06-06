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

import com.linkedin.common.callback.Callback;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.parseq.Engine;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.util.URIUtil;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.ProtocolVersionUtil;
import com.linkedin.restli.internal.server.RestLiCallback;
import com.linkedin.restli.internal.server.RestLiMethodInvoker;
import com.linkedin.restli.internal.server.RestLiResponseHandler;
import com.linkedin.restli.internal.server.RestLiRouter;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.filter.FilterRequestContextInternal;
import com.linkedin.restli.internal.server.filter.FilterRequestContextInternalImpl;
import com.linkedin.restli.internal.server.methods.response.ErrorResponseBuilder;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor.InterfaceType;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.model.RestLiApiBuilder;
import com.linkedin.restli.server.filter.ResponseFilter;
import com.linkedin.restli.server.multiplexer.MultiplexedRequestHandler;
import com.linkedin.restli.server.multiplexer.MultiplexedRequestHandlerImpl;
import com.linkedin.restli.server.resources.PrototypeResourceFactory;
import com.linkedin.restli.server.resources.ResourceFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dellamag
 * @author Zhenkai Zhu
 * @author nshankar
 */
//TODO: Remove this once use of InvokeAware has been discontinued.
@SuppressWarnings("deprecation")
public class RestLiServer extends BaseRestServer
{
  public static final String DEBUG_PATH_SEGMENT = "__debug";

  private static final Logger log = LoggerFactory.getLogger(RestLiServer.class);

  private final RestLiConfig _config;
  private final RestLiRouter _router;
  private final ResourceFactory _resourceFactory;
  private final RestLiMethodInvoker _methodInvoker;
  private final RestLiResponseHandler _responseHandler;
  private final RestLiDocumentationRequestHandler _docRequestHandler;
  private final MultiplexedRequestHandler _multiplexedRequestHandler;
  private final ErrorResponseBuilder _errorResponseBuilder;
  private final Map<String, RestLiDebugRequestHandler> _debugHandlers;
  private final List<ResponseFilter> _responseFilters;
  private final List<InvokeAware> _invokeAwares;
  private boolean _isDocInitialized = false;

  public RestLiServer(final RestLiConfig config)
  {
    this(config, new PrototypeResourceFactory());
  }

  public RestLiServer(final RestLiConfig config, final ResourceFactory resourceFactory)
  {
    this(config, resourceFactory, null);
  }

  public RestLiServer(final RestLiConfig config, final ResourceFactory resourceFactory, final Engine engine)
  {
    this(config, resourceFactory, engine, null);
  }

  @Deprecated
  public RestLiServer(final RestLiConfig config,
                      final ResourceFactory resourceFactory,
                      final Engine engine,
                      final List<InvokeAware> invokeAwares)
  {
    super(config);
    _config = config;
    _errorResponseBuilder = new ErrorResponseBuilder(config.getErrorResponseFormat(), config.getInternalErrorMessage());
    _resourceFactory = resourceFactory;
    _rootResources = new RestLiApiBuilder(config).build();
    _resourceFactory.setRootResources(_rootResources);
    _router = new RestLiRouter(_rootResources);
    _methodInvoker =
        new RestLiMethodInvoker(_resourceFactory, engine, _errorResponseBuilder, config.getRequestFilters());
    _responseHandler =
        new RestLiResponseHandler.Builder().setErrorResponseBuilder(_errorResponseBuilder)
                                           .build();
    _docRequestHandler = config.getDocumentationRequestHandler();
    _debugHandlers = new HashMap<String, RestLiDebugRequestHandler>();
    if (config.getResponseFilters() != null)
    {
      _responseFilters = config.getResponseFilters();
    }
    else
    {
      _responseFilters = new ArrayList<ResponseFilter>();
    }
    for (RestLiDebugRequestHandler debugHandler : config.getDebugRequestHandlers())
    {
      _debugHandlers.put(debugHandler.getHandlerId(), debugHandler);
    }

    _multiplexedRequestHandler = new MultiplexedRequestHandlerImpl(this, engine);
    // verify that if there are resources using the engine, then the engine is not null
    if (engine == null)
    {
      for (ResourceModel model : _rootResources.values())
      {
        for (ResourceMethodDescriptor desc : model.getResourceMethodDescriptors())
        {
          final InterfaceType type = desc.getInterfaceType();
          if (type == InterfaceType.PROMISE || type == InterfaceType.TASK)
          {
            final String fmt =
                "ParSeq based method %s.%s, but no engine given. "
                    + "Check your RestLiServer construction, spring wiring, "
                    + "and container-pegasus-restli-server-cmpt version.";
            log.warn(String.format(fmt, model.getResourceClass().getName(), desc.getMethod().getName()));
          }
        }
      }
    }
    _invokeAwares =
        (invokeAwares == null) ? Collections.<InvokeAware> emptyList() : Collections.unmodifiableList(invokeAwares);
  }

  public Map<String, ResourceModel> getRootResources()
  {
    return Collections.unmodifiableMap(_rootResources);
  }

  /**
   * @see BaseRestServer#doHandleRequest(com.linkedin.r2.message.rest.RestRequest,
   *      com.linkedin.r2.message.RequestContext, com.linkedin.common.callback.Callback)
   */
  @Override
  protected void doHandleRequest(final RestRequest request,
                                 final RequestContext requestContext,
                                 final Callback<RestResponse> callback)
  {
    if (isDocumentationRequest(request))
    {
      handleDocumentationRequest(request, callback);
    }
    else if (isMultiplexedRequest(request))
    {
      handleMultiplexedRequest(request, requestContext, callback);
    }
    else
    {
      RestLiDebugRequestHandler debugHandlerForRequest = findDebugRequestHandler(request);

      if (debugHandlerForRequest != null)
      {
        handleDebugRequest(debugHandlerForRequest, request, requestContext, callback);
      }
      else
      {
        handleResourceRequest(request,
                              requestContext,
                              new RequestExecutionCallbackAdapter<RestResponse>(callback),
                              false);
      }
    }
  }

  private boolean isSupportedProtocolVersion(ProtocolVersion clientProtocolVersion,
                                             ProtocolVersion lowerBound,
                                             ProtocolVersion upperBound)
  {
    int lowerCheck = clientProtocolVersion.compareTo(lowerBound);
    int upperCheck = clientProtocolVersion.compareTo(upperBound);
    return lowerCheck >= 0 && upperCheck <= 0;
  }

  /**
   * Ensures that the Rest.li protocol version used by the client is valid
   *
   * (assume the protocol version used by the client is "v")
   *
   * v is valid if {@link com.linkedin.restli.internal.common.AllProtocolVersions#OLDEST_SUPPORTED_PROTOCOL_VERSION}
   * <= v <= {@link com.linkedin.restli.internal.common.AllProtocolVersions#NEXT_PROTOCOL_VERSION}
   *
   * @param request
   *          the incoming request from the client
   * @throws RestLiServiceException
   *           if the protocol version used by the client is not valid based on the rules described
   *           above
   */
  private void ensureRequestUsesValidRestliProtocol(final RestRequest request) throws RestLiServiceException
  {
    ProtocolVersion clientProtocolVersion = ProtocolVersionUtil.extractProtocolVersion(request.getHeaders());
    ProtocolVersion lowerBound = AllProtocolVersions.OLDEST_SUPPORTED_PROTOCOL_VERSION;
    ProtocolVersion upperBound = AllProtocolVersions.NEXT_PROTOCOL_VERSION;
    if (!isSupportedProtocolVersion(clientProtocolVersion, lowerBound, upperBound))
    {
      throw new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, "Rest.li protocol version "
          + clientProtocolVersion + " used by the client is not supported!");
    }
  }

  private void handleDebugRequest(final RestLiDebugRequestHandler debugHandler,
                                  final RestRequest request,
                                  final RequestContext requestContext,
                                  final Callback<RestResponse> callback)
  {
    debugHandler.handleRequest(request, requestContext, new RestLiDebugRequestHandler.ResourceDebugRequestHandler()
    {
      @Override
      public void handleRequest(final RestRequest request,
                                final RequestContext requestContext,
                                final RequestExecutionCallback<RestResponse> callback)
      {
        // Create a new request at this point from the debug request by removing the path suffix
        // starting with "__debug".
        String fullPath = request.getURI().getPath();
        int debugSegmentIndex = fullPath.indexOf(DEBUG_PATH_SEGMENT);

        RestRequestBuilder requestBuilder = new RestRequestBuilder(request);

        UriBuilder uriBuilder = UriBuilder.fromUri(request.getURI());
        uriBuilder.replacePath(request.getURI().getPath().substring(0, debugSegmentIndex - 1));
        requestBuilder.setURI(uriBuilder.build());

        handleResourceRequest(requestBuilder.build(), requestContext, callback, true);
      }
    }, callback);
  }

  private void handleResourceRequest(final RestRequest request,
                                     final RequestContext requestContext,
                                     final RequestExecutionCallback<RestResponse> callback,
                                     final boolean isDebugMode)
  {
    try
    {
      ensureRequestUsesValidRestliProtocol(request);
    }
    catch (RestLiServiceException e)
    {
      final RestLiCallback<Object> restLiCallback =
          new RestLiCallback<Object>(request, null, _responseHandler, callback, null, null);
      restLiCallback.onError(e, createEmptyExecutionReport());
      return;
    }
    final RoutingResult method;
    try
    {
      method = _router.process(request, requestContext);
    }
    catch (Exception e)
    {
      final RestLiCallback<Object> restLiCallback =
          new RestLiCallback<Object>(request, null, _responseHandler, callback, null, null);
      restLiCallback.onError(e, createEmptyExecutionReport());
      return;
    }
    final RequestExecutionCallback<RestResponse> wrappedCallback = notifyInvokeAwares(method, callback);

    final FilterRequestContextInternal filterContext =
        new FilterRequestContextInternalImpl((ServerResourceContext) method.getContext(), method.getResourceMethod());
    final RestLiCallback<Object> restLiCallback =
        new RestLiCallback<Object>(request, method, _responseHandler, wrappedCallback, _responseFilters, filterContext);
    try
    {
      _methodInvoker.invoke(method, request, restLiCallback, isDebugMode, filterContext);
    }
    catch (Exception e)
    {
      restLiCallback.onError(e, createEmptyExecutionReport());
    }
  }

  /**
   * Invoke {@link InvokeAware#onInvoke(ResourceContext, RestLiMethodContext)} of registered invokeAwares.
   * @return A new callback that wraps the originalCallback, which invokes desired callbacks of invokeAwares after the method invocation finishes
   */
  private RequestExecutionCallback<RestResponse> notifyInvokeAwares(final RoutingResult routingResult,
                                                                    final RequestExecutionCallback<RestResponse> originalCallback)
  {
    if (!_invokeAwares.isEmpty())
    {
      final List<Callback<RestResponse>> invokeAwareCallbacks = new ArrayList<Callback<RestResponse>>();
      for (InvokeAware invokeAware : _invokeAwares)
      {
        invokeAwareCallbacks.add(invokeAware.onInvoke(routingResult.getContext(), routingResult.getResourceMethod()));
      }

      return new RequestExecutionCallback<RestResponse>()
      {
        @Override
        public void onSuccess(RestResponse result, RequestExecutionReport executionReport)
        {
          for (Callback<RestResponse> callback : invokeAwareCallbacks)
          {
            callback.onSuccess(result);
          }
          originalCallback.onSuccess(result, executionReport);
        }

        @Override
        public void onError(Throwable error, RequestExecutionReport executionReport)
        {
          for (Callback<RestResponse> callback : invokeAwareCallbacks)
          {
            callback.onError(error);
          }
          originalCallback.onError(error, executionReport);
        }
      };
    }

    return originalCallback;
  }


  private boolean isMultiplexedRequest(RestRequest request) {
    return _multiplexedRequestHandler.isMultiplexedRequest(request);
  }

  private void handleMultiplexedRequest(RestRequest request, RequestContext requestContext, Callback<RestResponse> callback) {
    _multiplexedRequestHandler.handleRequest(request, requestContext, callback);
  }

  private boolean isDocumentationRequest(RestRequest request) {
    return _docRequestHandler != null && _docRequestHandler.isDocumentationRequest(request);
  }

  private void handleDocumentationRequest(final RestRequest request, final Callback<RestResponse> callback)
  {
    try
    {
      synchronized (this)
      {
        if (!_isDocInitialized)
        {
          _docRequestHandler.initialize(_config, _rootResources);
          _isDocInitialized = true;
        }
      }

      final RestResponse response = _docRequestHandler.processDocumentationRequest(request);
      callback.onSuccess(response);
    }
    catch (Exception e)
    {
      final RestLiCallback<Object> restLiCallback =
          new RestLiCallback<Object>(request,
                                     null,
                                     _responseHandler,
                                     new RequestExecutionCallbackAdapter<RestResponse>(callback),
                                     null,
                                     null);
      restLiCallback.onError(e, createEmptyExecutionReport());
    }
  }

  private RestLiDebugRequestHandler findDebugRequestHandler(RestRequest request)
  {
    String[] pathSegments = URIUtil.tokenizePath(request.getURI().getPath());
    String debugHandlerId = null;
    RestLiDebugRequestHandler resultDebugHandler = null;

    for (int i = 0; i < pathSegments.length; ++i)
    {
      String pathSegment = pathSegments[i];
      if (pathSegment.equals(DEBUG_PATH_SEGMENT))
      {
        if (i < pathSegments.length - 1)
        {
          debugHandlerId = pathSegments[i + 1];
        }

        break;
      }
    }

    if (debugHandlerId != null)
    {
      resultDebugHandler = _debugHandlers.get(debugHandlerId);
    }

    return resultDebugHandler;
  }

  private static RequestExecutionReport createEmptyExecutionReport()
  {
    return new RequestExecutionReportBuilder().build();
  }

  private class RequestExecutionCallbackAdapter<T> implements RequestExecutionCallback<T>
  {
    private final Callback<T> _wrappedCallback;

    public RequestExecutionCallbackAdapter(Callback<T> wrappedCallback)
    {
      _wrappedCallback = wrappedCallback;
    }

    @Override
    public void onError(Throwable e, RequestExecutionReport executionReport)
    {
      _wrappedCallback.onError(e);
    }

    @Override
    public void onSuccess(T result, RequestExecutionReport executionReport)
    {
      _wrappedCallback.onSuccess(result);
    }
  }
}
