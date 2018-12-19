package com.linkedin.restli.server;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.CallbackAdapter;
import com.linkedin.data.ByteString;
import com.linkedin.data.codec.entitystream.StreamDataCodec;
import com.linkedin.entitystream.EntityStream;
import com.linkedin.entitystream.EntityStreams;
import com.linkedin.parseq.Engine;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamException;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.message.stream.entitystream.adapter.EntityStreamAdapters;
import com.linkedin.r2.transport.common.StreamRequestHandler;
import com.linkedin.restli.common.ContentType;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.CookieUtil;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.response.ErrorResponseBuilder;
import com.linkedin.restli.internal.server.response.RestLiResponse;
import com.linkedin.restli.internal.server.response.RestLiResponseException;
import com.linkedin.restli.internal.server.response.ResponseUtils;
import com.linkedin.restli.restspec.ResourceEntityType;
import com.linkedin.restli.server.resources.ResourceFactory;
import com.linkedin.restli.server.util.UnstructuredDataUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import javax.activation.MimeTypeParseException;


/**
 * A Rest.li server as a {@link StreamRequestHandler}. It uses {@link com.linkedin.r2.message.stream.entitystream.Reader}
 * and {@link com.linkedin.r2.message.stream.entitystream.Writer} to process the request and provide the response,
 * respectively. However, if streaming processing is not possible, it adapts the {@link StreamRequest} and {@link StreamResponse}
 * to fully-buffered {@link RestRequest} and {@link RestResponse} and falls back to a {@link RestRestLiServer}.
 *
 * @author Zhenkai Zhu
 * @author Xiao Ma
 */
class StreamRestLiServer extends BaseRestLiServer implements StreamRequestHandler
{
  private static final Logger log = LoggerFactory.getLogger(StreamRestLiServer.class);
  final RestRestLiServer _fallback;
  private boolean _useStreamCodec;

  StreamRestLiServer(RestLiConfig config,
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

    _useStreamCodec = config.isUseStreamCodec();
    _fallback = new RestRestLiServer(config,
        resourceFactory, engine,
        rootResources,
        errorResponseBuilder);
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

  private void doHandleStreamRequest(final StreamRequest request,
      final RequestContext requestContext,
      final Callback<StreamResponse> callback)
  {
    Optional<NonResourceRequestHandler> nonResourceRequestHandler = _fallback.getNonResourceRequestHandlers().stream()
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

  protected void handleResourceRequest(StreamRequest request,
      RequestContext requestContext,
      Callback<StreamResponse> callback)
  {
    RoutingResult routingResult;
    try
    {
      routingResult = getRoutingResult(request, requestContext);
    }
    catch (Exception e)
    {
      callback.onError(buildPreRoutingStreamException(e, request));
      return;
    }

    if (routingResult.getResourceMethod().getResourceModel().getResourceEntityType() == ResourceEntityType.STRUCTURED_DATA)
    {
      handleStructuredDataResourceRequest(request, routingResult, callback);
    }
    else
    {
      handleUnstructuredDataResourceRequest(request, routingResult, callback);
    }
  }

  StreamException buildPreRoutingStreamException(Throwable throwable, StreamRequest request)
  {
    RestLiResponseException restLiException = buildPreRoutingError(throwable, request);
    return Messages.toStreamException(ResponseUtils.buildRestException(restLiException));
  }

  private void handleStructuredDataResourceRequest(StreamRequest request,
      RoutingResult routingResult,
      Callback<StreamResponse> callback)
  {
    ContentType reqContentType, respContentType;
    try
    {
      // TODO: We should throw exception instead of defaulting to JSON when the request content type is non-null and
      // unrecognized. This behavior was inadvertently changed in commit d149605e4181349b64180bdfe0b4d24a294dc6f6
      // when this logic was moved from DataMapUtils.readMapWithExceptions() to DataMapConverter.dataMapToByteString().
      reqContentType = ContentType.getContentType(request.getHeader(RestConstants.HEADER_CONTENT_TYPE))
          .orElse(ContentType.JSON);

      String respMimeType = routingResult.getContext().getResponseMimeType();
      respContentType = ContentType.getContentType(respMimeType)
          .orElseThrow(() -> new RestLiServiceException(HttpStatus.S_406_NOT_ACCEPTABLE, "Requested mime type for encoding is not supported. Mimetype: " + respMimeType));
    }
    catch (MimeTypeParseException e)
    {
      callback.onError(e);
      return;
    }
    StreamDataCodec reqCodec = reqContentType.getStreamCodec(request.getHeaders());
    StreamDataCodec respCodec = respContentType.getStreamCodec(routingResult.getContext().getResponseHeaders());

    if (_useStreamCodec && reqCodec != null && respCodec != null)
    {
      reqCodec.decodeMap(EntityStreamAdapters.toGenericEntityStream(request.getEntityStream()))
          .handle((dataMap, e) -> {
            Throwable error = null;
            if (e == null)
            {
              try
              {
                handleResourceRequest(request,
                    routingResult,
                    dataMap,
                    toRestLiResponseCallback(callback, routingResult, respContentType));
              }
              catch (Throwable throwable)
              {
                error = throwable;
              }
            }
            else
            {
              error = new RoutingException("Cannot parse request entity", HttpStatus.S_400_BAD_REQUEST.getCode(), e);
            }

            if (error != null)
            {
              log.error("Fail to handle structured stream request", error);
              callback.onError(error);
            }

            return null; // handle function requires a return statement although there is no more completion stage.
          });
    }
    else
    {
      // Fallback to fully-buffered request and response processing.
      Messages.toRestRequest(request)
          .handle((restRequest, e) ->
          {
            if (e == null)
            {
              try
              {
                _fallback.handleResourceRequest(restRequest, routingResult,
                    toRestResponseCallback(callback, routingResult.getContext()));
              }
              catch (Throwable throwable)
              {
                e = throwable;
              }
            }

            if (e != null)
            {
              log.error("Fail to handle structured toRest request", e);
              callback.onError(e);
            }

            return null; // handle function requires a return statement although there is no more completion stage.
          });
    }
  }

  protected Callback<RestLiResponse> toRestLiResponseCallback(Callback<StreamResponse> callback,
      RoutingResult routingResult,
      ContentType contentType)
  {
    return new StreamToRestLiResponseCallbackAdapter(callback, contentType);
  }

  static class StreamToRestLiResponseCallbackAdapter extends CallbackAdapter<StreamResponse, RestLiResponse>
  {
    private final ContentType _contentType;

    StreamToRestLiResponseCallbackAdapter(Callback<StreamResponse> callback, ContentType contentType)
    {
      super(callback);
      _contentType = contentType;
    }

    @Override
    protected StreamResponse convertResponse(RestLiResponse restLiResponse)
        throws Exception
    {
      StreamResponseBuilder responseBuilder = new StreamResponseBuilder()
          .setHeaders(restLiResponse.getHeaders())
          .setCookies(CookieUtil.encodeSetCookies(restLiResponse.getCookies()))
          .setStatus(restLiResponse.getStatus().getCode());

      EntityStream<ByteString> entityStream;
      if (restLiResponse.hasData())
      {
        responseBuilder.setHeader(RestConstants.HEADER_CONTENT_TYPE, _contentType.getHeaderKey());
        entityStream = _contentType.getStreamCodec(restLiResponse.getHeaders()).encodeMap(restLiResponse.getDataMap());
      }
      else
      {
        entityStream = EntityStreams.emptyStream();
      }

      return responseBuilder.build(EntityStreamAdapters.fromGenericEntityStream(entityStream));
    }

    @Override
    protected Throwable convertError(Throwable e)
    {
      if (e instanceof RestLiResponseException)
      {
        RestLiResponseException responseException = (RestLiResponseException) e;
        StreamDataCodec streamDataCodec =
            _contentType.getStreamCodec(responseException.getRestLiResponse().getHeaders());
        return ResponseUtils.buildStreamException(responseException, streamDataCodec);
      }
      else
      {
        return super.convertError(e);
      }
    }
  }

  protected Callback<RestResponse> toRestResponseCallback(Callback<StreamResponse> callback, ServerResourceContext context)
  {
    return new StreamToRestResponseCallbackAdapter(callback);
  }

  static class StreamToRestResponseCallbackAdapter extends CallbackAdapter<StreamResponse, RestResponse>
  {
    StreamToRestResponseCallbackAdapter(Callback<StreamResponse> callback)
    {
      super(callback);
    }

    @Override
    protected StreamResponse convertResponse(RestResponse response)
        throws Exception
    {
      return Messages.toStreamResponse(response);
    }

    @Override
    protected Throwable convertError(Throwable error)
    {
      return error instanceof RestException
          ? Messages.toStreamException((RestException) error)
          : error;
    }
  }

  private void handleUnstructuredDataResourceRequest(StreamRequest request,
      RoutingResult routingResult,
      Callback<StreamResponse> callback)
  {
    routingResult.getContext().setRequestEntityStream(
        EntityStreamAdapters.toGenericEntityStream(request.getEntityStream()));
    handleResourceRequest(request,
        routingResult,
        null,
        new UnstructuredDataStreamToRestLiResponseCallbackAdapter(callback, routingResult.getContext()));
  }

  private static class UnstructuredDataStreamToRestLiResponseCallbackAdapter extends CallbackAdapter<StreamResponse, RestLiResponse>
  {
    private final ServerResourceContext _context;

    private UnstructuredDataStreamToRestLiResponseCallbackAdapter(Callback<StreamResponse> callback,
        ServerResourceContext context)
    {
      super(callback);
      _context = context;
    }

    @Override
    protected StreamResponse convertResponse(RestLiResponse restLiResponse)
        throws Exception
    {
      StreamResponseBuilder responseBuilder = new StreamResponseBuilder()
          .setHeaders(restLiResponse.getHeaders())
          .setCookies(CookieUtil.encodeSetCookies(restLiResponse.getCookies()))
          .setStatus(restLiResponse.getStatus().getCode());

      EntityStream<ByteString> entityStream = _context.getResponseEntityStream();
      if (entityStream != null)
      {
        // Unstructured data response
        // Content-Type is required
        if (restLiResponse.getHeaders().get(RestConstants.HEADER_CONTENT_TYPE) == null)
        {
          throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, "Content-Type is missing.");
        }
      }
      else
      {
        entityStream = EntityStreams.emptyStream();
      }

      return responseBuilder.build(EntityStreamAdapters.fromGenericEntityStream(entityStream));
    }

    @Override
    protected Throwable convertError(Throwable e)
    {
      if (e instanceof RestLiResponseException)
      {
        return Messages.toStreamException(ResponseUtils.buildRestException((RestLiResponseException) e));
      }
      else
      {
        return super.convertError(e);
      }
    }
  }
}
