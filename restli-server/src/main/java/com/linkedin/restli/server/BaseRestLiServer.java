package com.linkedin.restli.server;

import com.linkedin.common.callback.Callback;
import com.linkedin.data.DataMap;
import com.linkedin.parseq.Engine;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.timing.FrameworkTimingKeys;
import com.linkedin.r2.message.timing.TimingContextUtil;
import com.linkedin.restli.common.ContentType;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.HeaderUtil;
import com.linkedin.restli.internal.common.ProtocolVersionUtil;
import com.linkedin.restli.internal.server.PathKeysImpl;
import com.linkedin.restli.internal.server.ResourceContextImpl;
import com.linkedin.restli.internal.server.RestLiMethodInvoker;
import com.linkedin.restli.internal.server.RestLiRouter;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.filter.FilterChainCallback;
import com.linkedin.restli.internal.server.filter.FilterChainCallbackImpl;
import com.linkedin.restli.internal.server.filter.FilterChainDispatcher;
import com.linkedin.restli.internal.server.filter.FilterChainDispatcherImpl;
import com.linkedin.restli.internal.server.filter.FilterRequestContextInternalImpl;
import com.linkedin.restli.internal.server.filter.RestLiFilterChain;
import com.linkedin.restli.internal.server.filter.RestLiFilterResponseContextFactory;
import com.linkedin.restli.internal.server.methods.MethodAdapterRegistry;
import com.linkedin.restli.internal.server.methods.arguments.RestLiArgumentBuilder;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.response.ErrorResponseBuilder;
import com.linkedin.restli.internal.server.response.RestLiResponse;
import com.linkedin.restli.internal.server.response.RestLiResponseException;
import com.linkedin.restli.internal.server.response.RestLiResponseHandler;
import com.linkedin.restli.internal.server.util.RestLiSyntaxException;
import com.linkedin.restli.internal.server.util.RestUtils;
import com.linkedin.restli.server.config.ResourceMethodConfig;
import com.linkedin.restli.server.config.ResourceMethodConfigProvider;
import com.linkedin.restli.server.filter.Filter;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.resources.ResourceFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;


/**
 * <code>BaseRestLiServer</code> provides some common functionality for implementing a Rest.li server as a
 * {@link com.linkedin.r2.transport.common.RestRequestHandler} or a {@link com.linkedin.r2.transport.common.StreamRequestHandler}.
 *
 * @author Nick Dellamaggiore
 * @author Nishanth Shankaran
 * @author Xiao Ma
 */
abstract class BaseRestLiServer
{
  private final RestLiRouter _router;
  private final RestLiMethodInvoker _methodInvoker;
  private final RestLiResponseHandler _responseHandler;
  private final ErrorResponseBuilder _errorResponseBuilder;
  private final List<Filter> _filters;
  private final Set<String> _customContentTypes;
  private final ResourceMethodConfigProvider _methodConfigProvider;

  BaseRestLiServer(RestLiConfig config,
      ResourceFactory resourceFactory,
      Engine engine,
      Map<String, ResourceModel> rootResources,
      ErrorResponseBuilder errorResponseBuilder)
  {
    _customContentTypes = config.getCustomContentTypes().stream()
        .map(ContentType::getHeaderKey)
        .collect(Collectors.toSet());

    _router = new RestLiRouter(rootResources, config);
    resourceFactory.setRootResources(rootResources);
    _methodInvoker = new RestLiMethodInvoker(resourceFactory, engine, config.getInternalErrorMessage());

    _errorResponseBuilder = errorResponseBuilder;
    _responseHandler = new RestLiResponseHandler(_errorResponseBuilder);

    _filters = config.getFilters() != null ? config.getFilters() : new ArrayList<>();
    _methodConfigProvider = ResourceMethodConfigProvider.build(config.getMethodConfig());
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
  private void ensureRequestUsesValidRestliProtocol(final Request request) throws RestLiServiceException
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

  protected RoutingResult getRoutingResult(Request request, RequestContext requestContext)
  {
    ensureRequestUsesValidRestliProtocol(request);

    try
    {
      ServerResourceContext context = new ResourceContextImpl(new PathKeysImpl(), request, requestContext);
      RestUtils.validateRequestHeadersAndUpdateResourceContext(request.getHeaders(), _customContentTypes, context);

      ResourceMethodDescriptor method = _router.process(context);
      ResourceMethodConfig methodConfig = _methodConfigProvider.apply(method);

      return new RoutingResult(context, method, methodConfig);
    }
    catch (RestLiSyntaxException e)
    {
      throw new RoutingException(e.getMessage(), HttpStatus.S_400_BAD_REQUEST.getCode());
    }
  }

  protected RestLiResponseException buildPreRoutingError(Throwable throwable, Request request)
  {
    Map<String, String> requestHeaders = request.getHeaders();
    Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    headers.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION,
        ProtocolVersionUtil.extractProtocolVersion(requestHeaders).toString());
    headers.put(HeaderUtil.getErrorResponseHeaderName(requestHeaders), RestConstants.HEADER_VALUE_ERROR);

    RestLiServiceException restLiServiceException = RestLiServiceException.fromThrowable(throwable);
    ErrorResponse errorResponse = _errorResponseBuilder.buildErrorResponse(restLiServiceException);
    RestLiResponse restLiResponse = new RestLiResponse.Builder()
        .status(restLiServiceException.getStatus())
        .entity(errorResponse)
        .headers(headers)
        .cookies(Collections.emptyList())
        .build();
    return new RestLiResponseException(throwable, restLiResponse);
  }

  /**
   * Handles a request by building arguments and invoking the Rest.li resource method. All the arguments are processed
   * by the filters in the filter chain before invoking the resource method. The result is also processed by the
   * filters after invoking the resource method.
   *
   * @param request   The request to handle. Only the URI, method, and the headers can be accessed from this request. The
   *                  body should have already been parse to a DataMap.
   * @param callback
   */
  protected final void handleResourceRequest(Request request,
      RoutingResult routingResult,
      DataMap entityDataMap,
      Callback<RestLiResponse> callback)
  {
    ServerResourceContext context = routingResult.getContext();
    ResourceMethodDescriptor method = routingResult.getResourceMethod();

    FilterRequestContext filterContext;
    RestLiArgumentBuilder argumentBuilder;
    try
    {
      argumentBuilder = lookupArgumentBuilder(method, _errorResponseBuilder);
      // Unstructured data is not available in the Rest.Li filters
      RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult, entityDataMap);
      filterContext = new FilterRequestContextInternalImpl(context, method, requestData);
    }
    catch (Exception e)
    {
      // would not trigger response filters because request filters haven't run yet
      callback.onError(buildPreRoutingError(e, request));
      return;
    }

    RestLiFilterResponseContextFactory filterResponseContextFactory =
        new RestLiFilterResponseContextFactory(request, routingResult, _responseHandler);

    FilterChainCallback filterChainCallback = new FilterChainCallbackImpl(routingResult,
        _responseHandler,
        callback,
        _errorResponseBuilder);
    FilterChainDispatcher filterChainDispatcher = new FilterChainDispatcherImpl(routingResult,
        _methodInvoker,
        argumentBuilder);

    RestLiFilterChain filterChain = new RestLiFilterChain(_filters, filterChainDispatcher, filterChainCallback);

    TimingContextUtil.beginTiming(routingResult.getContext().getRawRequestContext(),
        FrameworkTimingKeys.SERVER_REQUEST_RESTLI_FILTER_CHAIN.key());

    filterChain.onRequest(filterContext, filterResponseContextFactory);
  }

  private RestLiArgumentBuilder lookupArgumentBuilder(ResourceMethodDescriptor method,
      ErrorResponseBuilder errorResponseBuilder)
  {
    RestLiArgumentBuilder argumentBuilder = new MethodAdapterRegistry(errorResponseBuilder)
        .getArgumentBuilder(method.getType());
    if (argumentBuilder == null)
    {
      throw new IllegalArgumentException("Unsupported method type: " + method.getType());
    }
    return argumentBuilder;
  }
}
