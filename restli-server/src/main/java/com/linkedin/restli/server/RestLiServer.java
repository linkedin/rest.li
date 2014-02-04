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
import com.linkedin.parseq.Engine;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.internal.common.ProtocolVersionUtil;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.server.RestLiCallback;
import com.linkedin.restli.internal.server.RestLiMethodInvoker;
import com.linkedin.restli.internal.server.RestLiResponseHandler;
import com.linkedin.restli.internal.server.RestLiRouter;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.methods.response.ErrorResponseBuilder;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor.InterfaceType;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.model.RestLiApiBuilder;
import com.linkedin.restli.server.resources.PrototypeResourceFactory;
import com.linkedin.restli.server.resources.ResourceFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dellamag
 * @author Zhenkai Zhu
 */
public class RestLiServer extends BaseRestServer
{
  private static final Logger log = LoggerFactory.getLogger(RestLiServer.class);

  private final RestLiConfig _config;
  private final RestLiRouter _router;
  private final ResourceFactory _resourceFactory;
  private final RestLiMethodInvoker _methodInvoker;
  private final RestLiResponseHandler _responseHandler;
  private final RestLiDocumentationRequestHandler _docRequestHandler;
  private final ErrorResponseBuilder _errorResponseBuilder;
  private final List<InvokeAware> _invokeAwares;
  private boolean _isDocInitialized = false;

  public RestLiServer(final RestLiConfig config)
  {
    this(config, new PrototypeResourceFactory());
  }

  public RestLiServer(final RestLiConfig config, final ResourceFactory resourceFactory)
  {
    this(config, resourceFactory, null, null);
  }

  public RestLiServer(final RestLiConfig config,
                      final ResourceFactory resourceFactory,
                      final Engine engine)
  {
    this(config, resourceFactory, engine, null);
  }

  public RestLiServer(final RestLiConfig config,
                      final ResourceFactory resourceFactory,
                      final Engine engine,
                      final List<InvokeAware> invokeAwares)
  {
    super(config);

    _config = config;
    _errorResponseBuilder = new ErrorResponseBuilder(config.getErrorResponseFormat(),
                                                     config.getInternalErrorMessage());
    _resourceFactory = resourceFactory;
    _rootResources = new RestLiApiBuilder(config).build();
    _resourceFactory.setRootResources(_rootResources);
    _router = new RestLiRouter(_rootResources);
    _methodInvoker = new RestLiMethodInvoker(_resourceFactory, engine, _errorResponseBuilder);
    _responseHandler = new RestLiResponseHandler.Builder()
                                                .setErrorResponseBuilder(_errorResponseBuilder)
                                                .setPermissiveEncoding(config.getPermissiveEncoding())
                                                .build();
    _docRequestHandler = config.getDocumentationRequestHandler();
    _invokeAwares = (invokeAwares == null) ? Collections.<InvokeAware>emptyList() : Collections.unmodifiableList(invokeAwares);


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
            log.warn(String.format(fmt, model.getResourceClass().getName(), desc.getMethod()
                                                                                .getName()));
          }
        }
      }
    }
  }

  public Map<String, ResourceModel> getRootResources()
  {
    return Collections.unmodifiableMap(_rootResources);
  }

  /**
   * @see BaseRestServer#doHandleRequest(com.linkedin.r2.message.rest.RestRequest,
   * com.linkedin.r2.message.RequestContext, com.linkedin.common.callback.Callback)
   */
  @Override
  protected void doHandleRequest(final RestRequest request, final RequestContext requestContext,
                                 final Callback<RestResponse> callback)
  {
    if (_docRequestHandler == null || !_docRequestHandler.isDocumentationRequest(request))
    {
      handleResourceRequest(request, requestContext, callback);
    }
    else
    {
      handleDocumentationRequest(request, callback);
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
   * Ensures that the Rest.li protocol version used by the client is valid based on the config.
   *
   * (assume the protocol version used by the client is "v")
   *
   * If we are using {@link com.linkedin.restli.server.RestLiConfig.RestliProtocolCheck#STRICT} then
   * {@link AllProtocolVersions#BASELINE_PROTOCOL_VERSION} <= v <= {@link AllProtocolVersions#LATEST_PROTOCOL_VERSION}
   *
   * If we are using {@link com.linkedin.restli.server.RestLiConfig.RestliProtocolCheck#RELAXED} then
   * {@link AllProtocolVersions#BASELINE_PROTOCOL_VERSION} <= v <= {@link AllProtocolVersions#NEXT_PROTOCOL_VERSION}
   *
   * @param request the incoming request from the client
   * @throws RestLiServiceException if the protocol version used by the client is not valid based on the rules described
   *                                above
   */
  private void ensureRequestUsesValidRestliProtocol(final RestRequest request) throws RestLiServiceException
  {
    if (request != null)
    {
      ProtocolVersion clientProtocolVersion = ProtocolVersionUtil.extractProtocolVersion(request);
      ProtocolVersion lowerBound = AllProtocolVersions.BASELINE_PROTOCOL_VERSION;
      ProtocolVersion upperBound = AllProtocolVersions.LATEST_PROTOCOL_VERSION;
      if (_config.getRestliProtocolCheck() == RestLiConfig.RestliProtocolCheck.RELAXED)
      {
        upperBound = AllProtocolVersions.NEXT_PROTOCOL_VERSION;
      }
      if (!isSupportedProtocolVersion(clientProtocolVersion, lowerBound, upperBound))
      {
        throw new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, "Rest.li protocol version " +
            clientProtocolVersion + " used by the client is not supported!");
      }
    }
  }

  private void handleResourceRequest(final RestRequest request,
                                     final RequestContext requestContext,
                                     final Callback<RestResponse> callback)
  {
    try
    {
      ensureRequestUsesValidRestliProtocol(request);
    }
    catch (RestLiServiceException e)
    {
      final RestLiCallback<Object> restLiCallback =
          new RestLiCallback<Object>(request, null, _responseHandler, callback);
      restLiCallback.onErrorPre(e);
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
          new RestLiCallback<Object>(request, null, _responseHandler, callback);
      restLiCallback.onErrorPre(e);
      return;
    }

    final Callback<RestResponse> wrappedCallback = notifyInvokeAwares(method, callback);

    final RestLiCallback<Object> restLiCallback =
        new RestLiCallback<Object>(request, method, _responseHandler, wrappedCallback);

    try
    {
      _methodInvoker.invoke(method, request, restLiCallback);
    }
    catch (Exception e)
    {
      restLiCallback.onErrorPre(e);
    }
  }

  private void handleDocumentationRequest(final RestRequest request,
                                          final Callback<RestResponse> callback)
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
          new RestLiCallback<Object>(request, null, _responseHandler, callback);
      restLiCallback.onErrorPre(e);
    }
  }

  /**
   * Invoke {@link InvokeAware#onInvoke(ResourceContext, RestLiMethodContext)} of registered invokeAwares.
   * @return A new callback that wraps the originalCallback, which invokes desired callbacks of invokeAwares after the method invocation finishes
   */
  private Callback<RestResponse> notifyInvokeAwares(final RoutingResult routingResult, final Callback<RestResponse> originalCallback)
  {
    if (!_invokeAwares.isEmpty())
    {
      final List<Callback<RestResponse>> invokeAwareCallbacks = new ArrayList<Callback<RestResponse>>();
      for (InvokeAware invokeAware : _invokeAwares)
      {
        invokeAwareCallbacks.add(invokeAware.onInvoke(routingResult.getContext(), routingResult.getResourceMethod()));
      }

      return new Callback<RestResponse>()
      {
        @Override
        public void onSuccess(RestResponse result)
        {
          for (Callback<RestResponse> callback : invokeAwareCallbacks)
          {
            callback.onSuccess(result);
          }
          originalCallback.onSuccess(result);
        }

        @Override
        public void onError(Throwable error)
        {
          for (Callback<RestResponse> callback : invokeAwareCallbacks)
          {
            callback.onError(error);
          }
          originalCallback.onError(error);
        }
      };
    }

    return originalCallback;
  }
}
