/*
   Copyright (c) 2014 LinkedIn Corp.

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


import com.linkedin.data.ByteString;
import com.linkedin.parseq.trace.Trace;
import com.linkedin.parseq.trace.codec.json.JsonTraceCodec;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.attachments.RestLiAttachmentReader;
import com.linkedin.restli.common.attachments.RestLiAttachmentReaderException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;


/**
 * The debug request handler for Parseq trace. It is used to get detailed task execution information
 * about the async Rest.li requests. This debug handler serves its results on two main paths:
 *
 *    http://<original request path>/__debug/parseqtrace/tracevis
 *    http://<original request path>/__debug/parseqtrace/raw
 *
 * The former path serves a tracevis visualization of the Parseq trace. If you make a request to this
 * tracevis path from a browser you will get the visualization rendered. Below is an example uri
 * which is a Rest.li get request with query parameter abc:
 *
 * Ex: http://greetings/1/__debug/parseqtrace/tracevis?abc=12345
 *
 * The latter of the two main request paths above is for obtaining the Parseq trace in JSON format. Below is an
 * example uri which is a Rest.li get request with query parameter abc:
 *
 * Ex: http://greetings/1/__debug/parseqtrace/raw?abc=12345
 *
 * This debug request handler works with all HTTP verbs supported by Rest.li (GET, PUT, POST, DELETE etc.)
 */
public class ParseqTraceDebugRequestHandler implements RestLiDebugRequestHandler
{
  private static final char PATH_SEP = '/';

  private static final String HANDLER_ID = "parseqtrace";

  //This debug request handler depends on the root folder name imported from parseq-tracevis tar artifact:"tracevis".
  private static final String ENTRY_PATH_SEGMENT_TRACEVIS = "tracevis";
  private static final String ENTRY_PATH_SEGMENT_RAW = "raw";

  private static final String TRACEVIS_PATH = HANDLER_ID + PATH_SEP + ENTRY_PATH_SEGMENT_TRACEVIS;
  private static final String RAW_PATH = HANDLER_ID + PATH_SEP + ENTRY_PATH_SEGMENT_RAW;

  private static final String HEADER_VALUE_TEXT_HTML = "text/html";
  private static final String HEADER_VALUE_TEXT_CSS = "text/css";
  private static final String HEADER_VALUE_APPLICATION_JS = "application/javascript";
  private static final String HEADER_VALUE_APPLICATION_JSON = "application/json";
  private static final String HEADER_VALUE_IMAGE_PNG = "image/png";

  private static final String TRACE_RENDER_SCRIPT = "<script>%s(\"%s\")</script>";

  //The constants pointing to Parseq Tracevis artifacts that this debug request handler depends on.
  private static final String TRACE_RENDER_FUNCTION = "renderTrace";
  private static final String ENTRY_PAGE = "trace.html";

  @Override
  public void handleRequest(final RestRequest request,
                            final RequestContext context,
                            final ResourceDebugRequestHandler resourceDebugRequestHandler,
                            final RestLiAttachmentReader attachmentReader,
                            final RequestExecutionCallback<RestResponse> callback)
  {
    //Find out the path coming after the "__debug" path segment
    String fullPath = request.getURI().getPath();
    int debugSegmentIndex = fullPath.indexOf(DelegatingDebugRequestHandler.DEBUG_PATH_SEGMENT);
    final String debugHandlerPath = fullPath.substring(
        debugSegmentIndex + DelegatingDebugRequestHandler.DEBUG_PATH_SEGMENT.length() + 1);

    assert (debugHandlerPath.startsWith(HANDLER_ID));

    //Decide whether this is a user issued debug request or a follow up static content request for tracevis html.
    if (debugHandlerPath.equals(TRACEVIS_PATH) || debugHandlerPath.equals(RAW_PATH))
    {
      //Execute the request as if it was a regular rest.li request through resource debug request handler.
      //By using the returned execution report shape the response accordingly.
      //Since we are executing the request as though its a normal rest.li request, we don't have to
      //drain the incoming request attachments. Our concerns are only the scenarios listed in the onError() and onSuccess()
      //in the callback.
      resourceDebugRequestHandler.handleRequest(request, context,
                                           new RequestExecutionCallback<RestResponse>()
                                           {
                                             @Override
                                             public void onError(Throwable e, RequestExecutionReport executionReport,
                                                                 RestLiAttachmentReader requestAttachmentReader,
                                                                 RestLiResponseAttachments responseAttachments)
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

                                               //Drop all attachments to send back on the ground as well.
                                               if (responseAttachments != null)
                                               {
                                                 responseAttachments.getResponseAttachmentsBuilder().build().abortAllDataSources(e);
                                               }

                                               sendDebugResponse(callback, executionReport, debugHandlerPath);
                                             }

                                             @Override
                                             public void onSuccess(RestResponse result, RequestExecutionReport executionReport,
                                                                   RestLiResponseAttachments responseAttachments)
                                             {
                                               //Since we aren't going to send any response attachments back, we need
                                               //to drain them here.
                                               if (responseAttachments != null)
                                               {
                                                 responseAttachments.getResponseAttachmentsBuilder().build()
                                                         .abortAllDataSources(new UnsupportedOperationException("Response attachments " +
                                                                 "may not be sent back for parseq trace debugging"));
                                               }

                                               sendDebugResponse(callback, executionReport, debugHandlerPath);
                                             }
                                           });
    }
    else
    {
      //We know that the request is a static content request. So here we figure out the internal path for the
      //JAR resource from the request path. A request uri such as "/__debug/parseqtrace/trace.html" translates to
      //"/tracevis/trace.html" for the resource path.
      String resourcePath = debugHandlerPath.replaceFirst(HANDLER_ID, ENTRY_PATH_SEGMENT_TRACEVIS);

      ClassLoader currentClassLoader = getClass().getClassLoader();

      InputStream resourceStream = currentClassLoader.getResourceAsStream(resourcePath);

      String mediaType = null;

      if (resourceStream != null)
      {
        mediaType = determineMediaType(resourcePath);
      }

      // If the requested file type is not supported by this debug request handler, return 404.
      if (mediaType == null)
      {
        callback.onError(new RestLiServiceException(HttpStatus.S_404_NOT_FOUND), null, null, null);
      }

      try
      {
        sendByteArrayAsResponse(callback, IOUtils.toByteArray(resourceStream), mediaType);
      }
      catch (IOException exception)
      {
        callback.onError(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, exception), null, null, null);
      }
    }
  }

  @Override
  public String getHandlerId()
  {
    return HANDLER_ID;
  }

  private void sendDebugResponse(final RequestExecutionCallback<RestResponse> callback,
                                 final RequestExecutionReport executionReport,
                                 final String path)
  {
    if (path.equals(TRACEVIS_PATH))
    {
      sendTracevisEntryPageAsResponse(callback, executionReport);
    }
    else
    {
      sendTraceRawAsResponse(callback, executionReport);
    }
  }

  private void sendTraceRawAsResponse(final RequestExecutionCallback<RestResponse> callback,
                                      final RequestExecutionReport executionReport)
  {
    String mediaType = HEADER_VALUE_APPLICATION_JSON;
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    try
    {
      Trace trace = executionReport.getParseqTrace();

      if (trace != null)
      {
        //Serialize the Parseq trace into JSON.
        JsonTraceCodec traceCodec = new JsonTraceCodec();
        traceCodec.encode(trace, outputStream);
      }
    }
    catch (IOException exception)
    {
      callback.onError(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, exception), null, null, null);
    }

    sendByteArrayAsResponse(callback, outputStream.toByteArray(), mediaType);
  }

  private void sendTracevisEntryPageAsResponse(final RequestExecutionCallback<RestResponse> callback,
                                               final RequestExecutionReport executionReport)
  {
    String mediaType = HEADER_VALUE_TEXT_HTML;

    ClassLoader currentClassLoader = getClass().getClassLoader();
    InputStream resourceStream = currentClassLoader.getResourceAsStream(
        ENTRY_PATH_SEGMENT_TRACEVIS + PATH_SEP + ENTRY_PAGE);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    try
    {
      IOUtils.copy(resourceStream, outputStream);
      Trace trace = executionReport.getParseqTrace();

      if (trace != null)
      {
        //Serialize the Parseq trace into JSON and then inject a javascript into the response
        //which will call the corresponding render function on the entry page html with the JSON
        //string.
        JsonTraceCodec traceCodec = new JsonTraceCodec();
        IOUtils.write(createTraceRenderScript(traceCodec.encode(trace)),
                      outputStream);
      }
    }
    catch (IOException exception)
    {
      callback.onError(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, exception), null, null, null);
    }

    sendByteArrayAsResponse(callback, outputStream.toByteArray(), mediaType);
  }

  private void sendByteArrayAsResponse(final RequestExecutionCallback<RestResponse> callback,
                                       final byte[] responseBytes,
                                       final String mediaType)
  {
    RestResponse staticContentResponse = new RestResponseBuilder().
                                          setStatus(HttpStatus.S_200_OK.getCode()).
                                          setHeader(RestConstants.HEADER_CONTENT_TYPE, mediaType).
                                          setEntity(ByteString.unsafeWrap(responseBytes)).
                                          build();
    callback.onSuccess(staticContentResponse, null, null);
  }

  private static String determineMediaType(String path)
  {
    int extensionIndex = path.lastIndexOf('.');

    if (extensionIndex != -1)
    {
      String extension = path.substring(extensionIndex);

      if (extension.equals(".html"))
      {
        return HEADER_VALUE_TEXT_HTML;
      }
      else if (extension.equals(".js"))
      {
        return HEADER_VALUE_APPLICATION_JS;
      }
      else if (extension.equals(".css"))
      {
        return HEADER_VALUE_TEXT_CSS;
      }
      else if (extension.equals(".png"))
      {
        return HEADER_VALUE_IMAGE_PNG;
      }
    }

    return null;
  }

  private static String createTraceRenderScript(String trace)
  {
    String result = String.format(
        TRACE_RENDER_SCRIPT,
        TRACE_RENDER_FUNCTION,
        StringEscapeUtils.escapeJavaScript(trace));

    return result;
  }
}
