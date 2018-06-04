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
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.attachments.RestLiAttachmentReader;
import com.linkedin.restli.common.attachments.RestLiAttachmentReaderException;
import com.linkedin.restli.internal.common.AttachmentUtils;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.response.ErrorResponseBuilder;
import com.linkedin.restli.internal.server.response.ResponseUtils;
import com.linkedin.restli.internal.server.response.RestLiResponse;
import com.linkedin.restli.server.resources.ResourceFactory;

import java.util.Collections;
import java.util.Map;
import javax.activation.MimeTypeParseException;
import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;


/**
 * A {@link StreamRestLiServer} that's capable of handling request and response attachments, if there are any.
 *
 * @author Karim Vidhani
 * @author Xiao Ma
 */
class AttachmentHandlingRestLiServer extends StreamRestLiServer
{
  AttachmentHandlingRestLiServer(RestLiConfig config,
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
  }

  @Override
  protected void handleResourceRequest(StreamRequest request,
      RequestContext requestContext,
      Callback<StreamResponse> callback)
  {
    if (!handleRequestAttachments(request, requestContext, callback))
    {
      //If we get here this means that the content-type is missing (which is supported to maintain backwards compatibility)
      //or that it exists and is something other than multipart/related. This means we can read the entire payload into memory
      //and reconstruct the RestRequest.
      super.handleResourceRequest(request, requestContext, callback);
    }
  }

  /**
   * Handles multipart/related request as Rest.li payload with attachments.
   *
   * @return Whether or not the request is a multipart/related Rest.li request with attachments.
   */
  private boolean handleRequestAttachments(StreamRequest request,
      RequestContext requestContext,
      Callback<StreamResponse> callback)
  {
    //At this point we need to check the content-type to understand how we should handle the request.
    String header = request.getHeader(RestConstants.HEADER_CONTENT_TYPE);
    if (header != null)
    {
      ContentType contentType;
      try
      {
        contentType = new ContentType(header);
      }
      catch (ParseException e)
      {
        callback.onError(Messages.toStreamException(RestException.forError(400,
            "Unable to parse Content-Type: " + header)));
        return true;
      }

      if (contentType.getBaseType().equalsIgnoreCase(RestConstants.HEADER_VALUE_MULTIPART_RELATED))
      {
        //We need to reconstruct a RestRequest that has the first part of the multipart/related payload as the
        //traditional rest.li payload of a RestRequest.
        final MultiPartMIMEReader multiPartMIMEReader = MultiPartMIMEReader.createAndAcquireStream(request);
        RoutingResult routingResult;
        try
        {
          routingResult = getRoutingResult(request, requestContext);
        }
        catch (Exception e)
        {
          callback.onError(buildPreRoutingStreamException(e, request));
          return true;
        }
        final TopLevelReaderCallback firstPartReader = new TopLevelReaderCallback(routingResult, callback, multiPartMIMEReader, request);
        multiPartMIMEReader.registerReaderCallback(firstPartReader);
        return true;
      }
    }

    return false;
  }

  private class TopLevelReaderCallback implements MultiPartMIMEReaderCallback
  {
    private final RoutingResult _routingResult;
    private final RestRequestBuilder _restRequestBuilder;
    private volatile ByteString _requestPayload = null;
    private final MultiPartMIMEReader _multiPartMIMEReader;
    private final Callback<StreamResponse> _streamResponseCallback;

    private TopLevelReaderCallback(RoutingResult routingResult,
        final Callback<StreamResponse> streamResponseCallback,
        final MultiPartMIMEReader multiPartMIMEReader,
        final StreamRequest streamRequest)
    {
      _routingResult = routingResult;
      _restRequestBuilder = new RestRequestBuilder(streamRequest);
      _streamResponseCallback = streamResponseCallback;
      _multiPartMIMEReader = multiPartMIMEReader;
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
                                                                                            "Unknown Content-Type for first part of multipart/related payload: " + contentTypeString)));
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
        ServerResourceContext context = _routingResult.getContext();
        context.setRequestAttachmentReader(new RestLiAttachmentReader(_multiPartMIMEReader));

        // Debug request should have already been handled and attachment is not supported.
        RestRequest restRequest = _restRequestBuilder.build();
        _fallback.handleResourceRequest(restRequest,
            _routingResult, toRestResponseCallback(_streamResponseCallback, context));
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
      RestRequest restRequest = _restRequestBuilder.build();
      //We have no attachments so we pass null for the reader.
      // Debug request should have already handled by one of the request handlers.
      _fallback.handleResourceRequest(restRequest,
          _routingResult, toRestResponseCallback(_streamResponseCallback, _routingResult.getContext()));
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

    FirstPartReaderCallback(final TopLevelReaderCallback topLevelReaderCallback,
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

  @Override
  protected Callback<RestLiResponse> toRestLiResponseCallback(Callback<StreamResponse> callback,
      RoutingResult routingResult,
      com.linkedin.restli.common.ContentType contentType)
  {
    return new AttachmentHandlingStreamToRestLiResponseCallbackAdapter(callback, routingResult, contentType);
  }

  private static class AttachmentHandlingStreamToRestLiResponseCallbackAdapter extends StreamToRestLiResponseCallbackAdapter
  {
    private final RoutingResult _routingResult;

    AttachmentHandlingStreamToRestLiResponseCallbackAdapter(Callback<StreamResponse> callback,
        RoutingResult routingResult,
        com.linkedin.restli.common.ContentType contentType)
    {
      super(callback, contentType);
      _routingResult = routingResult;
    }

    @Override
    protected StreamResponse convertResponse(RestLiResponse restLiResponse)
        throws Exception
    {
      RestLiResponseAttachments responseAttachments = _routingResult.getContext().getResponseAttachments();
      if (responseAttachments != null && responseAttachments.getMultiPartMimeWriterBuilder().getCurrentSize() > 0)
      {
        RestResponse structuredFirstPart = ResponseUtils.buildResponse(_routingResult, restLiResponse);
        return createStreamResponseWithAttachment(structuredFirstPart, responseAttachments);
      }
      else
      {
        return super.convertResponse(restLiResponse);
      }
    }
  }

  /**
   * It is important to note that the server's response may include attachments so we factor that into
   * consideration upon completion of this request.
   */
  @Override
  protected Callback<RestResponse> toRestResponseCallback(Callback<StreamResponse> callback, ServerResourceContext context)
  {
    return new AttachmentHandlingStreamToRestResponseCallbackAdapter(callback, context);
  }

  private static class AttachmentHandlingStreamToRestResponseCallbackAdapter extends StreamToRestResponseCallbackAdapter
  {
    private final ServerResourceContext _context;

    AttachmentHandlingStreamToRestResponseCallbackAdapter(Callback<StreamResponse> callback, ServerResourceContext context)
    {
      super(callback);
      _context = context;
    }

    @Override
    public Throwable convertError(final Throwable e)
    {
      drainRequestAttachments(_context.getRequestAttachmentReader());
      drainResponseAttachments(_context.getResponseAttachments(), e);

      //At this point, 'e' must be a RestException. It's a bug in the rest.li framework if this is not the case; at which
      //point a 500 will be returned.
      return super.convertError(e);
    }

    @Override
    protected StreamResponse convertResponse(RestResponse response)
        throws Exception
    {
      RestLiResponseAttachments responseAttachments = _context.getResponseAttachments();
      if (responseAttachments != null && responseAttachments.getMultiPartMimeWriterBuilder().getCurrentSize() > 0)
      {
        return createStreamResponseWithAttachment(response, responseAttachments);
      }
      else
      {
        return super.convertResponse(response);
      }
    }
  }

  private static StreamResponse createStreamResponseWithAttachment(RestResponse structuredFirstPart, RestLiResponseAttachments attachments)
  {
    //Construct the StreamResponse and invoke the callback. The RestResponse entity should be the first part.
    //There may potentially be attachments included in the response. Note that unlike the client side request builders,
    //here it is possible to have a non-null attachment list with 0 attachments due to the way the builder in
    //RestLiResponseAttachments works. Therefore we have to make sure its a non zero size as well.
    final ByteStringWriter firstPartWriter = new ByteStringWriter(structuredFirstPart.getEntity());
    final MultiPartMIMEWriter multiPartMIMEWriter = AttachmentUtils.createMultiPartMIMEWriter(firstPartWriter,
        structuredFirstPart.getHeader(RestConstants.HEADER_CONTENT_TYPE),
        attachments.getMultiPartMimeWriterBuilder());

    //Ensure that any headers or cookies from the RestResponse make into the outgoing StreamResponse. The exception
    //of course being the Content-Type header which will be overridden by MultiPartMIMEStreamResponseFactory.
    return MultiPartMIMEStreamResponseFactory.generateMultiPartMIMEStreamResponse(AttachmentUtils.RESTLI_MULTIPART_SUBTYPE,
        multiPartMIMEWriter,
        Collections.emptyMap(),
        structuredFirstPart.getHeaders(),
        structuredFirstPart.getStatus(),
        structuredFirstPart.getCookies());
  }

  //For the request side a number of things could happen which require us to fully absorb and drain the request.
  //For example, there could be a bad request, framework level exception or an exception in the request filter chain.
  //We must drain the entire incoming request because if we don't, then the connection will remain open until a timeout
  //occurs. This can potentially act as a denial of service and take down a host by exhausting it of file descriptors.
  private static void drainRequestAttachments(RestLiAttachmentReader requestAttachmentReader)
  {
    //Since this is eventually sent back as a success, we need to
    //drain any request attachments as well as any response attachments.
    //Normally this is done by StreamResponseCallbackAdaptor's onError, but
    //this is sent back as a success so we handle it here instead.
    if (requestAttachmentReader != null && !requestAttachmentReader.haveAllAttachmentsFinished())
    {
      try
      {
        //Here we simply call drainAllAttachments. At this point the current callback assigned is likely the
        //TopLevelReaderCallback in RestLiServer. When this callback is notified that draining is completed (via
        //onDrainComplete()), then no action is taken (which is what is desired).
        //
        //We can go ahead and send the error back to the client while we continue to drain the
        //bytes in the background. Note that it could be the case that even though there is an exception thrown,
        //that application code could still be reading these attachments. In such a case we would not be able to call
        //drainAllAttachments() successfully. Therefore we handle this exception and swallow.
        requestAttachmentReader.drainAllAttachments();
      }
      catch (RestLiAttachmentReaderException readerException)
      {
        //Swallow here.
        //It could be the case that the application code is still absorbing attachments.
        //We back off and send the original response to the client. If the application code is not doing this,
        //there is a chance for a resource leak. In such a case the framework can do nothing else.
      }
    }
  }

  //For the response side, a number of things could happen which require us to fully absorb and drain any response
  //attachments provided by the resource method. For example, the resource throws an exception after setting attachments
  //or there is an exception in the framework when sending the response back (i.e response filters). In these cases
  //we must drain all these attachments because some of these attachments could potentially be chained from other servers,
  //thereby hogging resources until timeouts occur.
  private static void drainResponseAttachments(RestLiResponseAttachments responseAttachments, Throwable e)
  {
    //Drop all attachments to send back on the ground as well.
    if (responseAttachments != null)
    {
      responseAttachments.getMultiPartMimeWriterBuilder().build().abortAllDataSources(e);
    }
  }
}
