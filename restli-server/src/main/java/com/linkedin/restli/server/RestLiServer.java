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
import com.linkedin.data.ByteString;
import com.linkedin.multipart.MultiPartMIMEReader;
import com.linkedin.multipart.MultiPartMIMEReaderCallback;
import com.linkedin.multipart.MultiPartMIMEStreamResponseFactory;
import com.linkedin.multipart.MultiPartMIMEWriter;
import com.linkedin.multipart.SinglePartMIMEReaderCallback;
import com.linkedin.multipart.exceptions.MultiPartIllegalFormatException;
import com.linkedin.parseq.Engine;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.Writer;
import com.linkedin.r2.transport.common.RestRequestHandler;
import com.linkedin.r2.transport.common.StreamRequestHandler;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.attachments.RestLiAttachmentReader;
import com.linkedin.restli.common.attachments.RestLiAttachmentReaderException;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.AttachmentUtils;
import com.linkedin.restli.internal.common.HeaderUtil;
import com.linkedin.restli.internal.common.ProtocolVersionUtil;
import com.linkedin.restli.internal.server.RestLiInternalException;
import com.linkedin.restli.internal.server.RestLiMethodInvoker;
import com.linkedin.restli.internal.server.RestLiRouter;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.filter.FilterChainCallback;
import com.linkedin.restli.internal.server.filter.FilterChainCallbackImpl;
import com.linkedin.restli.internal.server.filter.FilterChainDispatcher;
import com.linkedin.restli.internal.server.filter.FilterChainDispatcherImpl;
import com.linkedin.restli.internal.server.filter.FilterRequestContextInternal;
import com.linkedin.restli.internal.server.filter.FilterRequestContextInternalImpl;
import com.linkedin.restli.internal.server.filter.RestLiFilterChain;
import com.linkedin.restli.internal.server.filter.RestLiFilterResponseContextFactory;
import com.linkedin.restli.internal.server.methods.MethodAdapterRegistry;
import com.linkedin.restli.internal.server.methods.arguments.RestLiArgumentBuilder;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor.InterfaceType;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.model.RestLiApiBuilder;
import com.linkedin.restli.internal.server.response.ErrorResponseBuilder;
import com.linkedin.restli.internal.server.response.PartialRestResponse;
import com.linkedin.restli.internal.server.response.RestLiResponseHandler;
import com.linkedin.restli.internal.server.util.MIMEParse;
import com.linkedin.restli.internal.server.util.RestUtils;
import com.linkedin.restli.server.filter.Filter;
import com.linkedin.restli.server.multiplexer.MultiplexedRequestHandler;
import com.linkedin.restli.server.multiplexer.MultiplexedRequestHandlerImpl;
import com.linkedin.restli.server.resources.PrototypeResourceFactory;
import com.linkedin.restli.server.resources.ResourceFactory;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.activation.MimeTypeParseException;
import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author dellamag
 * @author Zhenkai Zhu
 * @author nshankar
 * @author Karim Vidhani
 */
public class RestLiServer implements RestRequestHandler, StreamRequestHandler
{
  private static final Logger log = LoggerFactory.getLogger(RestLiServer.class);

  private final RestLiRouter _router;
  private final RestLiMethodInvoker _methodInvoker;
  private final RestLiResponseHandler _responseHandler;
  private final List<RequestHandler> _requestHandlers;
  private final ErrorResponseBuilder _errorResponseBuilder;
  private final List<Filter> _filters;
  private final Set<String> _customMimeTypesSupported;

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
    _errorResponseBuilder = new ErrorResponseBuilder(config.getErrorResponseFormat(), config.getInternalErrorMessage());
    Map<String, ResourceModel> rootResources = new RestLiApiBuilder(config).build();
    resourceFactory.setRootResources(rootResources);
    _router = new RestLiRouter(rootResources);
    _methodInvoker = new RestLiMethodInvoker(resourceFactory, engine, _errorResponseBuilder);
    _responseHandler =
        new RestLiResponseHandler.Builder().setErrorResponseBuilder(_errorResponseBuilder)
            .build();
    _customMimeTypesSupported = config.getCustomContentTypes().stream().map(
        com.linkedin.restli.common.ContentType::getHeaderKey).collect(Collectors.toSet());
    if (config.getFilters() != null)
    {
      _filters = config.getFilters();
    }
    else
    {
      _filters = new ArrayList<>();
    }

    // verify that if there are resources using the engine, then the engine is not null
    if (engine == null)
    {
      for (ResourceModel model : rootResources.values())
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

    _requestHandlers = new ArrayList<>();

    // Add documentation request handler
    RestLiDocumentationRequestHandler docReqHandler = config.getDocumentationRequestHandler();
    if (docReqHandler != null)
    {
      docReqHandler.initialize(config, rootResources);
      _requestHandlers.add(docReqHandler);
    }

    // Add multiplexed request handler
    MultiplexedRequestHandler muxReqHandler = new MultiplexedRequestHandlerImpl(this,
        engine,
        config.getMaxRequestsMultiplexed(),
        config.getMultiplexedIndividualRequestHeaderWhitelist(),
        config.getMultiplexerSingletonFilter(),
        config.getMultiplexerRunMode(),
        _errorResponseBuilder);
    _requestHandlers.add(muxReqHandler);

    // Add debug request handlers
    for (RestLiDebugRequestHandler debugHandler : config.getDebugRequestHandlers())
    {
      _requestHandlers.add(new DelegatingDebugRequestHandler(debugHandler, this));
    }

    List<ResourceDefinitionListener> resourceDefinitionListeners = config.getResourceDefinitionListeners();
    if (resourceDefinitionListeners != null)
    {
      Map<String, ResourceDefinition> resourceDefinitions = Collections.unmodifiableMap(rootResources);
      for (ResourceDefinitionListener listener : resourceDefinitionListeners)
      {
        listener.onInitialized(resourceDefinitions);
      }
    }
  }

  /**
   * @see RestRequestHandler#handleRequest(RestRequest, RequestContext, Callback)
   */
  @Override
  public void handleRequest(final RestRequest request, final RequestContext requestContext,
      final Callback<RestResponse> callback)
  {
    try
    {
      doHandleRequest(request, requestContext, callback);
    }
    catch (Exception e)
    {
      log.error("Uncaught exception", e);
      callback.onError(e);
    }
  }

  private void doHandleRequest(final RestRequest request,
      final RequestContext requestContext,
      final Callback<RestResponse> callback)
  {
    //Until RestRequest is removed, this code path cannot accept content types or accept types that contain
    //multipart/related. This is because these types of requests will usually have very large payloads and therefore
    //would degrade server performance since RestRequest reads everything into memory.
    if (verifyAttachmentSupportNotNeeded(request, callback))
    {
      return; //If there is an exception return and do not continue.
    }

    for (RequestHandler handler : _requestHandlers)
    {
      if (handler.shouldHandle(request))
      {
        handler.handleRequest(request, requestContext, callback);
        return;
      }
    }

    handleResourceRequest(request,
        requestContext,
        new RestResponseExecutionCallbackAdapter(callback),
        null,
        false);
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

  void handleResourceRequest(final RestRequest request,
      final RequestContext requestContext,
      final RequestExecutionCallback<RestResponse> callback,
      final RestLiAttachmentReader attachmentReader,
      final boolean isDebugMode)
  {

    try
    {
      ensureRequestUsesValidRestliProtocol(request);
    }
    catch (RestLiServiceException e)
    {
      respondWithPreRoutingError(e, request, attachmentReader, callback);
      return;
    }
    final RoutingResult method;
    try
    {
      method = _router.process(request, requestContext, attachmentReader);
    }
    catch (Exception e)
    {
      respondWithPreRoutingError(e, request, attachmentReader, callback);
      return;
    }

    RequestExecutionReportBuilder requestExecutionReportBuilder = null;
    if (isDebugMode)
    {
      requestExecutionReportBuilder = new RequestExecutionReportBuilder();
    }

    final FilterRequestContextInternal filterContext =
        new FilterRequestContextInternalImpl((ServerResourceContext) method.getContext(), method.getResourceMethod());

    RestLiArgumentBuilder argumentBuilder;
    try
    {
      RestUtils.validateRequestHeadersAndUpdateResourceContext(request.getHeaders(),
                                                               _customMimeTypesSupported,
                                                               (ServerResourceContext)method.getContext());
      argumentBuilder = buildRestLiArgumentBuilder(method, _errorResponseBuilder);
      filterContext.setRequestData(argumentBuilder.extractRequestData(method, request));
    }
    catch (Exception e)
    {
      // would not trigger response filters because request filters haven't run yet
      callback.onError(e, requestExecutionReportBuilder == null ? null : requestExecutionReportBuilder.build(),
                              ((ServerResourceContext)method.getContext()).getRequestAttachmentReader(), null);
      return;
    }

    RestLiFilterResponseContextFactory filterResponseContextFactory =
        new RestLiFilterResponseContextFactory(request, method, _responseHandler);

    FilterChainCallback filterChainCallback = new FilterChainCallbackImpl(method,
        requestExecutionReportBuilder,
        attachmentReader,
        _responseHandler,
        callback,
        _errorResponseBuilder);
    FilterChainDispatcher filterChainDispatcher = new FilterChainDispatcherImpl(method,
        _methodInvoker,
        argumentBuilder,
        requestExecutionReportBuilder);

    RestLiFilterChain filterChain = new RestLiFilterChain(_filters, filterChainDispatcher, filterChainCallback);

    filterChain.onRequest(filterContext, filterResponseContextFactory);
  }

  private void respondWithPreRoutingError(Throwable throwable,
                                          RestRequest request,
                                          RestLiAttachmentReader attachmentReader,
                                          RequestExecutionCallback<RestResponse> callback)
  {
    Map<String, String> requestHeaders = request.getHeaders();
    Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    headers.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION,
        ProtocolVersionUtil.extractProtocolVersion(requestHeaders).toString());
    headers.put(HeaderUtil.getErrorResponseHeaderName(requestHeaders), RestConstants.HEADER_VALUE_ERROR);

    RestLiServiceException restLiServiceException = RestLiServiceException.fromThrowable(throwable);
    ErrorResponse errorResponse = _errorResponseBuilder.buildErrorResponse(restLiServiceException);
    PartialRestResponse partialRestResponse = new PartialRestResponse.Builder()
        .status(restLiServiceException.getStatus())
        .entity(errorResponse)
        .headers(headers)
        .cookies(Collections.emptyList())
        .build();
    RestException restException = _responseHandler.buildRestException(throwable, partialRestResponse);
    callback.onError(restException, createEmptyExecutionReport(), attachmentReader, null);
  }

  /**
   * Builder for building a {@link RestLiArgumentBuilder}
   *
   * @param method the REST method
   * @param errorResponseBuilder the {@link ErrorResponseBuilder}
   * @return a {@link RestLiArgumentBuilder}
   */
  private RestLiArgumentBuilder buildRestLiArgumentBuilder(RoutingResult method,
                                                           ErrorResponseBuilder errorResponseBuilder)
  {
    ResourceMethodDescriptor resourceMethodDescriptor = method.getResourceMethod();

    RestLiArgumentBuilder adapter = new MethodAdapterRegistry(errorResponseBuilder)
        .getArgumentBuilder(resourceMethodDescriptor.getType());
    if (adapter == null)
    {
      throw new IllegalArgumentException("Unsupported method type: " + resourceMethodDescriptor.getType());
    }
    return adapter;
  }

  private static RequestExecutionReport createEmptyExecutionReport()
  {
    return new RequestExecutionReportBuilder().build();
  }

  private class RestResponseExecutionCallbackAdapter implements RequestExecutionCallback<RestResponse>
  {
    private final Callback<RestResponse> _wrappedCallback;

    public RestResponseExecutionCallbackAdapter(Callback<RestResponse> wrappedCallback)
    {
      _wrappedCallback = wrappedCallback;
    }

    @Override
    public void onError(final Throwable e, final RequestExecutionReport executionReport,
                        final RestLiAttachmentReader requestAttachmentReader,
                        final RestLiResponseAttachments responseAttachments)
    {
      _wrappedCallback.onError(e);
    }

    @Override
    public void onSuccess(final RestResponse result, final RequestExecutionReport executionReport,
                          final RestLiResponseAttachments responseAttachments)
    {
      _wrappedCallback.onSuccess(result);
    }
  }

  /**
   * @see StreamRequestHandler#handleRequest(StreamRequest, RequestContext, Callback)
   */
  @Override
  public void handleRequest(StreamRequest request, RequestContext requestContext, Callback<StreamResponse> callback)
  {
    try
    {
      doHandleStreamRequest(request, requestContext, callback);
    }
    catch (Exception e)
    {
      log.error("Uncaught exception", e);
      callback.onError(e);
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //Streaming related functionality defined here.
  //In the future we will deprecate and remove RestRequest/RestResponse. Until then we need to be minimally invasive
  //while still offering existing functionality.

  private void doHandleStreamRequest(final StreamRequest request,
      final RequestContext requestContext,
      final Callback<StreamResponse> callback)
  {
    //Eventually - when RestRequest is removed, we will migrate all of these code paths to StreamRequest.

    for (RequestHandler handler : _requestHandlers)
    {
      if (handler.shouldHandle(request))
      {
        handler.handleRequest(request, requestContext, callback);
        return;
      }
    }

    //At this point we need to check the content-type to understand how we should handle the request.
    String header = request.getHeader(RestConstants.HEADER_CONTENT_TYPE);
    if (header != null)
    {
      ContentType contentType = null;
      try
      {
        contentType = new ContentType(header);
      }
      catch (ParseException e)
      {
        callback.onError(Messages.toStreamException(RestException.forError(400,
                                                                           "Unable to parse Content-Type: " + header)));
        return;
      }

      if (contentType.getBaseType().equalsIgnoreCase(RestConstants.HEADER_VALUE_MULTIPART_RELATED))
      {
        //We need to reconstruct a RestRequest that has the first part of the multipart/related payload as the
        //traditional rest.li payload of a RestRequest.
        final MultiPartMIMEReader multiPartMIMEReader = MultiPartMIMEReader.createAndAcquireStream(request);
        final TopLevelReaderCallback firstPartReader =
            new TopLevelReaderCallback(requestContext, callback, multiPartMIMEReader, request);
        multiPartMIMEReader.registerReaderCallback(firstPartReader);
        return;
      }
    }

    //If we get here this means that the content-type is missing (which is supported to maintain backwards compatibility)
    //or that it exists and is something other than multipart/related. This means we can read the entire payload into memory
    //and reconstruct the RestRequest.
    Messages.toRestRequest(request, new Callback<RestRequest>()
    {
      @Override
      public void onError(Throwable e)
      {
        callback.onError(e);
      }

      @Override
      public void onSuccess(RestRequest result)
      {
        //This callback is invoked once the incoming StreamRequest is converted into a RestRequest. We can now
        //move forward with this request.
        //It is important to note that the server's response may include attachments so we factor that into
        //consideration upon completion of this request.
        final StreamResponseCallbackAdaptor streamResponseCallbackAdaptor = new StreamResponseCallbackAdaptor(callback);

        // Debug request should have already been handled by one of the request handlers.
        handleResourceRequest(result, requestContext, streamResponseCallbackAdaptor, null, false);
      }
    });
  }

  private class TopLevelReaderCallback implements MultiPartMIMEReaderCallback
  {
    private final RestRequestBuilder _restRequestBuilder;
    private volatile ByteString _requestPayload = null;
    private final RequestContext _requestContext;
    private final Callback<StreamResponse> _streamResponseCallback;
    private final MultiPartMIMEReader _multiPartMIMEReader;
    private final StreamRequest _streamRequest;

    private TopLevelReaderCallback(final RequestContext requestContext,
                                   final Callback<StreamResponse> streamResponseCallback, final MultiPartMIMEReader multiPartMIMEReader,
                                   final StreamRequest streamRequest)
    {
      _restRequestBuilder = new RestRequestBuilder(streamRequest);
      _requestContext = requestContext;
      _streamResponseCallback = streamResponseCallback;
      _multiPartMIMEReader = multiPartMIMEReader;
      _streamRequest = streamRequest;
    }

    private void setRequestPayload(final ByteString requestPayload)
    {
      _requestPayload = requestPayload;
    }

    @Override
    public void onNewPart(MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader)
    {
      if (_requestPayload == null)
      {
        //The first time this is invoked we read in the first part.
        //At this point in time the Content-Type is still multipart/related for the artificially created RestRequest.
        //Therefore care must be taken to make sure that we propagate the Content-Type from the first part as the Content-Type
        //of the artificially created RestRequest.
        final Map<String, String> singlePartHeaders = singlePartMIMEReader.dataSourceHeaders(); //Case-insensitive map already.
        final String contentTypeString = singlePartHeaders.get(RestConstants.HEADER_CONTENT_TYPE);
        if (contentTypeString == null)
        {
          _streamResponseCallback.onError(Messages.toStreamException(RestException.forError(400,
                                                                                            "Incorrect multipart/related payload. First part must contain the Content-Type!")));
          return;
        }

        com.linkedin.restli.common.ContentType contentType;
        try
        {
          contentType = com.linkedin.restli.common.ContentType.getContentType(contentTypeString).orElse(null);
        }
        catch (MimeTypeParseException e)
        {
          _streamResponseCallback.onError(Messages.toStreamException(RestException.forError(400,
                                                                                            "Unable to parse Content-Type: " + contentTypeString)));
          return;
        }

        if (contentType == null)
        {
          _streamResponseCallback.onError(Messages.toStreamException(RestException.forError(415,
                                                                                            "Unknown Content-Type for first part of multipart/related payload: " + contentType.toString())));
          return;
        }

        //This will overwrite the multipart/related header.
        _restRequestBuilder.setHeader(RestConstants.HEADER_CONTENT_TYPE, contentTypeString);
        FirstPartReaderCallback firstPartReaderCallback = new FirstPartReaderCallback(this, singlePartMIMEReader);
        singlePartMIMEReader.registerReaderCallback(firstPartReaderCallback);
        singlePartMIMEReader.requestPartData();
      }
      else
      {
        //This is the beginning of the 2nd part, so pass this to the client.
        //It is also important to note that this callback (TopLevelReaderCallback) will no longer be used. Application
        //developers will have to register a new callback to continue reading from the multipart mime payload.
        //The only way that this callback could possibly be invoked again, is if an application developer directly invokes
        //drainAllAttachments() without registering a callback. This means that at some point in time in the future, this
        //callback will be invoked on onDrainComplete().

        _restRequestBuilder.setEntity(_requestPayload);

        // Debug request should have already been handled and attachment is not supported.
        handleResourceRequest(_restRequestBuilder.build(), _requestContext, new StreamResponseCallbackAdaptor(_streamResponseCallback),
            new RestLiAttachmentReader(_multiPartMIMEReader), false);
      }
    }

    @Override
    public void onFinished()
    {
      //Verify we actually had some parts. User attachments do not have to be present but for multipart/related
      //there must be atleast some payload.
      if (_requestPayload == null)
      {
        _streamResponseCallback.onError(Messages.toStreamException(RestException.forError(400,
                                                                                          "Did not receive any parts in the multipart mime request!")));
        return;
      }

      //At this point, this means that the multipart mime envelope didn't have any attachments (apart from the
      //json/pson payload). Technically the rest.li client would not create a payload like this, but to keep the protocol
      //somewhat flexible we will allow it.
      //If there had been more attachments, then onNewPart() above would be invoked and we would have passed the
      //attachment reader onto the framework.

      //It is also important to note that this callback (TopLevelReaderCallback) will no longer be used. We provide
      //null to the application developer since there are no attachments present. Therefore it is not possible for this
      //callback to ever be used again. This is a bit different then the onNewPart() case above because in that case
      //there is a valid non-null attachment reader provided to the resource method. In that case application developers
      //could call drainAllAttachments() without registering a callback which would then lead to onDrainComplete() being
      //invoked.

      _restRequestBuilder.setEntity(_requestPayload);
      //We have no attachments so we pass null for the reader.
      // Debug request should have already handled by one of the request handlers.
      handleResourceRequest(_restRequestBuilder.build(), _requestContext,
          new StreamResponseCallbackAdaptor(_streamResponseCallback), null, false);
    }

    @Override
    public void onDrainComplete()
    {
      //This happens when an application developer chooses to drain without registering a callback. Since this callback
      //is still bound to the MultiPartMIMEReader, we'll get the notification here that their desire to drain all the
      //attachments as completed. No action here is needed.
    }

    @Override
    public void onStreamError(Throwable throwable)
    {
      //At this point this could be a an exception thrown due to malformed data or this could be an exception thrown
      //due to an invocation of a callback. For example, an exception thrown due to an invocation of a callback could occur when
      //handleResourceRequest(). Though this should never happen  because handleResourceRequest() catches everything
      //and invokes the corresponding RequestExecutionCallback.
      if (throwable instanceof MultiPartIllegalFormatException)
      {
        //If its an illegally formed request, then we send back 400.
        _streamResponseCallback.onError(Messages.toStreamException(RestException.forError(400, "Illegally formed multipart payload")));
        return;
      }
      //Otherwise this is an internal server error. R2 will convert this to a 500 for us. As mentioned this should never happen.
      _streamResponseCallback.onError(throwable);
    }
  }

  private class FirstPartReaderCallback implements SinglePartMIMEReaderCallback
  {
    private final TopLevelReaderCallback _topLevelReaderCallback;
    private final MultiPartMIMEReader.SinglePartMIMEReader _singlePartMIMEReader;
    private final ByteString.Builder _builder = new ByteString.Builder();

    public FirstPartReaderCallback(
        final TopLevelReaderCallback topLevelReaderCallback,
        final MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader)
    {
      _topLevelReaderCallback = topLevelReaderCallback;
      _singlePartMIMEReader = singlePartMIMEReader;
    }

    @Override
    public void onPartDataAvailable(ByteString partData)
    {
      _builder.append(partData);
      _singlePartMIMEReader.requestPartData();
    }

    @Override
    public void onFinished()
    {
      _topLevelReaderCallback.setRequestPayload(_builder.build());
    }

    @Override
    public void onDrainComplete()
    {
      _topLevelReaderCallback.onStreamError(Messages.toStreamException(RestException.forError(500, "Serious error. " +
          "There should never be a call to drain part data when decoding the first part in a multipart mime response.")));
    }

    @Override
    public void onStreamError(Throwable throwable)
    {
      //No need to do anything as the MultiPartMIMEReader will also call onStreamError() on the top level callback
      //which will then call the response callback.
    }
  }

  private class StreamResponseCallbackAdaptor implements RequestExecutionCallback<RestResponse>
  {
    private final Callback<StreamResponse> _streamResponseCallback;

    private StreamResponseCallbackAdaptor(final Callback<StreamResponse> streamResponseCallback)
    {
      _streamResponseCallback = streamResponseCallback;
    }

    @Override
    public void onError(final Throwable e, final RequestExecutionReport executionReport,
                        final RestLiAttachmentReader requestAttachmentReader,
                        final RestLiResponseAttachments responseAttachments)
    {
      //Due to the exception we have to fully drain the request attachment and response attachments (if applicable).
      //This is necessary due to the following reasons:
      //
      //1. For the request side a number of things could happen which require us to fully absorb and drain the request.
      //For example, there could be a bad request, framework level exception or an exception in the request filter chain.
      //We must drain the entire incoming request because if we don't, then the connection will remain open until a timeout
      //occurs. This can potentially act as a denial of service and take down a host by exhausting it of file descriptors.
      //
      //2. For the response side, a number of things could happen which require us to fully absorb and drain any response
      //attachments provided by the resource method. For example, the resource throws an exception after setting attachments
      //or there is an exception in the framework when sending the response back (i.e response filters). In these cases
      //we must drain all these attachments because some of these attachments could potentially be chained from other servers,
      //thereby hogging resources until timeouts occur.

      if (requestAttachmentReader != null && !requestAttachmentReader.haveAllAttachmentsFinished())
      {
        //Here we simply call drainAllAttachments. At this point the current callback assigned is likely the
        //TopLevelReaderCallback in RestLiServer. When this callback is notified that draining is completed (via
        //onDrainComplete()), then no action is taken (which is what is desired).
        //
        //We can go ahead and send the error back to the client while we continue to drain the
        //bytes in the background. Note that it could be the case that even though there is an exception thrown,
        //that application code could still be reading these attachments. In such a case we would not be able to call
        //drainAllAttachments() successfully. Therefore we handle this exception and swallow.
        try
        {
          requestAttachmentReader.drainAllAttachments();
        }
        catch (RestLiAttachmentReaderException readerException)
        {
          //Swallow here.
          //It could be the case that the application code is still absorbing attachments.
          //We back off and send the error to the client. If the application code is not actively doing this, despite
          //seemingly beginning the process, there is a chance for a resource leak on the server. In such a case the framework
          //can do nothing else.
        }
      }

      //Drop all attachments to send back on the ground as well.
      if (responseAttachments != null)
      {
        responseAttachments.getMultiPartMimeWriterBuilder().build().abortAllDataSources(e);
      }

      //At this point, 'e' must be a RestException. It's a bug in the rest.li framework if this is not the case; at which
      //point a 500 will be returned.
      _streamResponseCallback.onError(Messages.toStreamException((RestException)e));
    }

    @Override
    public void onSuccess(final RestResponse result, final RequestExecutionReport executionReport,
                          final RestLiResponseAttachments responseAttachments)
    {
      if (responseAttachments != null)
      {
        if (responseAttachments.getReactiveDataWriter() != null)
        {
          /* This is an unstructured data response in reactive */

          ReactiveDataWriter reactiveWriter = responseAttachments.getReactiveDataWriter();

          EntityStream entityStream = EntityStreams.newEntityStream(reactiveWriter.getWriter());

          StreamResponseBuilder builder = new StreamResponseBuilder();
          builder.setCookies(result.getCookies());
          builder.setStatus(result.getStatus());

          if (reactiveWriter.getContentType() != null)
          {
            // The copy is required because result.getHeaders() returns an unmodifiable map
            Map<String, String> updatedHeaders = new HashMap<String, String>();
            updatedHeaders.putAll(result.getHeaders());
            updatedHeaders.put(RestConstants.HEADER_CONTENT_TYPE, reactiveWriter.getContentType());
            builder.setHeaders(updatedHeaders);
          }
          else
          {
            builder.setHeaders(result.getHeaders());
          }

          StreamResponse response = builder.build(entityStream);

          _streamResponseCallback.onSuccess(response);
        }
        else if (responseAttachments.getUnstructuredDataWriter() != null)
        {
          /* This is an unstructured data response */

          UnstructuredDataWriter writer = responseAttachments.getUnstructuredDataWriter();

          // OutputStream must be ByteArrayOutputStream
          if (!(writer.getOutputStream() instanceof ByteArrayOutputStream))
          {
            _streamResponseCallback.onError(new RestLiInternalException("OutputStream must be ByteArrayOutputStream"));
          }

          // Content-Type is required
          if (result.getHeaders().get(RestConstants.HEADER_CONTENT_TYPE) == null)
          {
            _streamResponseCallback.onError(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, "Content-Type is missing."));
          }

          ByteArrayOutputStream outputStream = (ByteArrayOutputStream) writer.getOutputStream();
          byte[] bytes = outputStream.toByteArray();
          ByteString byteString = ByteString.unsafeWrap(bytes);
          Writer byteStringWriter = new ByteStringWriter(byteString);
          EntityStream entityStream = EntityStreams.newEntityStream(byteStringWriter);

          StreamResponseBuilder builder = new StreamResponseBuilder();
          builder.setCookies(result.getCookies());
          builder.setStatus(result.getStatus());
          builder.setHeaders(result.getHeaders());
          StreamResponse response = builder.build(entityStream);

          _streamResponseCallback.onSuccess(response);
        }
        else if (responseAttachments.getMultiPartMimeWriterBuilder().getCurrentSize() > 0)
        {
          /* This is a writer response as attachments */

          //Construct the StreamResponse and invoke the callback. The RestResponse entity should be the first part.
          //There may potentially be attachments included in the response. Note that unlike the client side request builders,
          //here it is possible to have a non-null attachment list with 0 attachments due to the way the builder in
          //RestLiResponseAttachments works. Therefore we have to make sure its a non zero size as well.
          final ByteStringWriter firstPartWriter = new ByteStringWriter(result.getEntity());
          final MultiPartMIMEWriter multiPartMIMEWriter = AttachmentUtils.createMultiPartMIMEWriter(firstPartWriter,
                                                                                                    result.getHeader(
                                                                                                      RestConstants.HEADER_CONTENT_TYPE),
                                                                                                    responseAttachments.getMultiPartMimeWriterBuilder());

          //Ensure that any headers or cookies from the RestResponse make into the outgoing StreamResponse. The exception
          //of course being the Content-Type header which will be overridden by MultiPartMIMEStreamResponseFactory.
          final StreamResponse streamResponse = MultiPartMIMEStreamResponseFactory.generateMultiPartMIMEStreamResponse(
            AttachmentUtils.RESTLI_MULTIPART_SUBTYPE,
            multiPartMIMEWriter,
            Collections.<String, String>emptyMap(),
            result.getHeaders(),
            result.getStatus(),
            result.getCookies());
          _streamResponseCallback.onSuccess(streamResponse);
        }
      }
      else
      {
        _streamResponseCallback.onSuccess(Messages.toStreamResponse(result));
      }
    }
  }

  private boolean verifyAttachmentSupportNotNeeded(final Request request, final Callback<RestResponse> callback)
  {
    final Map<String, String> requestHeaders = request.getHeaders();
    try
    {
      final String contentTypeString = requestHeaders.get(RestConstants.HEADER_CONTENT_TYPE);
      if (contentTypeString != null)
      {
        final ContentType contentType = new ContentType(contentTypeString);
        if (contentType.getBaseType().equalsIgnoreCase(RestConstants.HEADER_VALUE_MULTIPART_RELATED))
        {
          callback.onError(RestException.forError(415, "This server cannot handle requests with a content type of multipart/related"));
          return true;
        }
      }
      final String acceptTypeHeader = requestHeaders.get(RestConstants.HEADER_ACCEPT);
      if (acceptTypeHeader != null)
      {
        final List<String> acceptTypes = MIMEParse.parseAcceptType(acceptTypeHeader);
        for (final String acceptType : acceptTypes)
        {
          if (acceptType.equalsIgnoreCase(RestConstants.HEADER_VALUE_MULTIPART_RELATED))
          {
            callback.onError(RestException.forError(406, "This server cannot handle requests with an accept type of multipart/related"));
            return true;
          }
        }
      }
    }
    catch (ParseException parseException)
    {
      callback.onError(RestException.forError(400, "Unable to parse content or accept types."));
      return true;
    }

    return false;
  }
}
