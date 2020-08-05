/*
   Copyright (c) 2019 LinkedIn Corp.

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
import com.linkedin.common.callback.CallbackAdapter;
import com.linkedin.data.DataMap;
import com.linkedin.parseq.Engine;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.timing.FrameworkTimingKeys;
import com.linkedin.r2.message.timing.TimingContextUtil;
import com.linkedin.r2.transport.common.RestRequestHandler;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.response.ErrorResponseBuilder;
import com.linkedin.restli.internal.server.response.ResponseUtils;
import com.linkedin.restli.internal.server.response.RestLiResponse;
import com.linkedin.restli.internal.server.response.RestLiResponseException;
import com.linkedin.restli.internal.server.util.DataMapUtils;
import com.linkedin.restli.server.multiplexer.MultiplexedRequestHandlerImpl;
import com.linkedin.restli.server.resources.ResourceFactory;
import com.linkedin.restli.server.symbol.RestLiSymbolTableRequestHandler;
import com.linkedin.restli.server.util.UnstructuredDataUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Rest.li server that handles the fully buffered {@link RestRequest}.
 *
 * @author Nick Dellamaggiore
 * @author Xiao Ma
 */
class RestRestLiServer extends BaseRestLiServer implements RestRequestHandler, RestToRestLiRequestHandler
{
  private static final Logger log = LoggerFactory.getLogger(RestRestLiServer.class);

  private final List<NonResourceRequestHandler> _nonResourceRequestHandlers;
  private final boolean _writableStackTrace;

  RestRestLiServer(RestLiConfig config,
      ResourceFactory resourceFactory,
      Engine engine,
      Map<String, ResourceModel> rootResources,
      ErrorResponseBuilder errorResponseBuilder)
  {
    super(config,
        resourceFactory,
        engine,
        rootResources,
        errorResponseBuilder);

    _nonResourceRequestHandlers = new ArrayList<>();

    // Add documentation request handler
    RestLiDocumentationRequestHandler docReqHandler = config.getDocumentationRequestHandler();
    if (docReqHandler != null)
    {
      docReqHandler.initialize(config, rootResources);
      _nonResourceRequestHandlers.add(docReqHandler);
    }

    // Add symbol table request handler
    _nonResourceRequestHandlers.add(new RestLiSymbolTableRequestHandler());

    // Add multiplexed request handler
    _nonResourceRequestHandlers.add(new MultiplexedRequestHandlerImpl(this,
        engine,
        config.getMaxRequestsMultiplexed(),
        config.getMultiplexedIndividualRequestHeaderWhitelist(),
        config.getMultiplexerSingletonFilter(),
        config.getMultiplexerRunMode(),
        errorResponseBuilder));

    // Add debug request handlers
    config.getDebugRequestHandlers().stream()
        .map(handler -> new DelegatingDebugRequestHandler(handler, this))
        .forEach(_nonResourceRequestHandlers::add);

    // Add custom request handlers
    config.getCustomRequestHandlers().forEach(_nonResourceRequestHandlers::add);
    _writableStackTrace = config.isWritableStackTrace();
  }

  List<NonResourceRequestHandler> getNonResourceRequestHandlers()
  {
    return _nonResourceRequestHandlers;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handleRequest(final RestRequest request, final RequestContext requestContext,
      final Callback<RestResponse> callback)
  {
    try
    {
      doHandleRequest(request, requestContext, callback);
    }
    catch (Throwable t)
    {
      log.error("Uncaught exception", t);
      callback.onError(t);
    }
  }

  @Override
  public void handleRequestWithRestLiResponse(final RestRequest request, final RequestContext requestContext,
      final Callback<RestLiResponse> callback)
  {
    try
    {
      if (_nonResourceRequestHandlers.stream().anyMatch(handler -> handler.shouldHandle(request)))
      {
        throw new RuntimeException("Non-resource endpoints don't support RestLiResponse");
      }

      handleResourceRequestWithRestLiResponse(request, requestContext, callback);
    }
    catch (Throwable t)
    {
      log.error("Uncaught exception", t);
      callback.onError(t);
    }
  }

  protected void doHandleRequest(final RestRequest request,
      final RequestContext requestContext,
      final Callback<RestResponse> callback)
  {
    Optional<NonResourceRequestHandler> nonResourceRequestHandler = _nonResourceRequestHandlers.stream()
        .filter(handler -> handler.shouldHandle(request))
        .findFirst();

    // TODO: Use Optional#ifPresentOrElse once we are on Java 9.
    if (nonResourceRequestHandler.isPresent())
    {
      nonResourceRequestHandler.get().handleRequest(request, requestContext, callback);
    }
    else
    {
      handleResourceRequest(request, requestContext, callback);
    }
  }

  void handleResourceRequest(RestRequest request, RequestContext requestContext, Callback<RestResponse> callback)
  {
    RoutingResult routingResult;
    try
    {
      routingResult = getRoutingResult(request, requestContext);
    }
    catch (Throwable t)
    {
      callback.onError(buildPreRoutingRestException(t, request));
      return;
    }

    handleResourceRequest(request, routingResult, callback);
  }

  private void handleResourceRequestWithRestLiResponse(RestRequest request, RequestContext requestContext,
      Callback<RestLiResponse> callback)
  {
    RoutingResult routingResult;
    try
    {
      routingResult = getRoutingResult(request, requestContext);
    }
    catch (Throwable t)
    {
      callback.onError(buildPreRoutingRestException(t, request));
      return;
    }

    handleResourceRequestWithRestLiResponse(request, routingResult, callback);
  }

  private RestException buildPreRoutingRestException(Throwable throwable, RestRequest request)
  {
    RestLiResponseException restLiException = buildPreRoutingError(throwable, request);
    return ResponseUtils.buildRestException(restLiException, _writableStackTrace);
  }

  protected void handleResourceRequest(RestRequest request,
      RoutingResult routingResult,
      Callback<RestResponse> callback)
  {
    handleResourceRequestWithRestLiResponse(request, routingResult,
        new RestLiToRestResponseCallbackAdapter(callback, routingResult, _writableStackTrace));
  }

  protected void handleResourceRequestWithRestLiResponse(RestRequest request, RoutingResult routingResult,
      Callback<RestLiResponse> callback)
  {
    DataMap entityDataMap = null;
    if (request.getEntity() != null && request.getEntity().length() > 0)
    {
      if (UnstructuredDataUtil.isUnstructuredDataRouting(routingResult))
      {
        callback.onError(buildPreRoutingError(
            new RoutingException("Unstructured Data is not supported in non-streaming Rest.li server",
                HttpStatus.S_400_BAD_REQUEST.getCode()), request));
        return;
      }
      try
      {
        final RequestContext requestContext = routingResult.getContext().getRawRequestContext();
        TimingContextUtil.beginTiming(requestContext, FrameworkTimingKeys.SERVER_REQUEST_RESTLI_DESERIALIZATION.key());
        entityDataMap = DataMapUtils.readMapWithExceptions(request);
        TimingContextUtil.endTiming(requestContext, FrameworkTimingKeys.SERVER_REQUEST_RESTLI_DESERIALIZATION.key());
      }
      catch (IOException e)
      {
        callback.onError(buildPreRoutingError(
            new RoutingException("Cannot parse request entity", HttpStatus.S_400_BAD_REQUEST.getCode(), e), request));
        return;
      }
    }

    handleResourceRequest(request,
        routingResult,
        entityDataMap,
        callback);
  }

  static class RestLiToRestResponseCallbackAdapter extends CallbackAdapter<RestResponse, RestLiResponse>
  {
    private final RoutingResult _routingResult;
    private final boolean _writableStackTrace;

    RestLiToRestResponseCallbackAdapter(Callback<RestResponse> callback, RoutingResult routingResult, Boolean writableStackTrace)
    {
      super(callback);
      _routingResult = routingResult;
      _writableStackTrace = writableStackTrace;
    }

    @Override
    protected RestResponse convertResponse(RestLiResponse restLiResponse)
    {
      final RequestContext requestContext = _routingResult.getContext().getRawRequestContext();
      TimingContextUtil.beginTiming(requestContext, FrameworkTimingKeys.SERVER_RESPONSE_RESTLI_SERIALIZATION.key());

      final RestResponse restResponse = ResponseUtils.buildResponse(_routingResult, restLiResponse);

      TimingContextUtil.endTiming(requestContext, FrameworkTimingKeys.SERVER_RESPONSE_RESTLI_SERIALIZATION.key());
      return restResponse;
    }

    @Override
    protected Throwable convertError(Throwable error)
    {
      final RequestContext requestContext = _routingResult.getContext().getRawRequestContext();
      TimingContextUtil.beginTiming(requestContext, FrameworkTimingKeys.SERVER_RESPONSE_RESTLI_ERROR_SERIALIZATION.key());

      final Throwable throwable = error instanceof RestLiResponseException
          ? ResponseUtils.buildRestException((RestLiResponseException) error, _writableStackTrace)
          : error;

      TimingContextUtil.endTiming(requestContext, FrameworkTimingKeys.SERVER_RESPONSE_RESTLI_ERROR_SERIALIZATION.key());
      return throwable;
    }
  }
}
