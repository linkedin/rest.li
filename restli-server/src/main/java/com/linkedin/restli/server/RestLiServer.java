/*
    Copyright (c) 2018 LinkedIn Corp.

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
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.RestRequestHandler;
import com.linkedin.r2.transport.common.StreamRequestHandler;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.model.RestLiApiBuilder;
import com.linkedin.restli.internal.server.response.ErrorResponseBuilder;
import com.linkedin.restli.internal.server.response.RestLiResponse;
import com.linkedin.restli.internal.server.util.MIMEParse;
import com.linkedin.restli.server.resources.PrototypeResourceFactory;
import com.linkedin.restli.server.resources.ResourceFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;


/**
 * A Rest.li server that can handle both a {@link RestRequest} and a {@link StreamRequest}. Its implementation
 * delegates the call to the underlying {@link RestRestLiServer} and {@link StreamRestLiServer}, respectively.
 *
 * @author Xiao Ma
 */
public class RestLiServer implements RestRequestHandler, RestToRestLiRequestHandler,
                                     StreamRequestHandler, StreamToRestLiRequestHandler
{
  private static final Logger LOGGER = LoggerFactory.getLogger(RestLiServer.class);

  private final RestRestLiServer _restRestLiServer;
  private final StreamRestLiServer _streamRestLiServer;

  public RestLiServer(RestLiConfig config)
  {
    this(config, new PrototypeResourceFactory());
  }

  public RestLiServer(RestLiConfig config, ResourceFactory resourceFactory)
  {
    this(config, resourceFactory, null);
  }

  public RestLiServer(RestLiConfig config, ResourceFactory resourceFactory, Engine engine)
  {
    Map<String, ResourceModel> rootResources = new RestLiApiBuilder(config).build();

    // Notify listeners of the resource models.
    List<ResourceDefinitionListener> resourceDefinitionListeners = config.getResourceDefinitionListeners();
    if (resourceDefinitionListeners != null)
    {
      Map<String, ResourceDefinition> resourceDefinitions = Collections.unmodifiableMap(rootResources);
      for (ResourceDefinitionListener listener : resourceDefinitionListeners)
      {
        listener.onInitialized(resourceDefinitions);
      }
    }

    // Verify that if there are resources using the engine, then the engine is not null
    if (engine == null)
    {
      for (ResourceModel model : rootResources.values())
      {
        for (ResourceMethodDescriptor desc : model.getResourceMethodDescriptors())
        {
          final ResourceMethodDescriptor.InterfaceType type = desc.getInterfaceType();
          if (type == ResourceMethodDescriptor.InterfaceType.PROMISE || type == ResourceMethodDescriptor.InterfaceType.TASK)
          {
            final String fmt =
                "ParSeq based method %s.%s, but no engine given. "
                    + "Check your RestLiServer construction, spring wiring, "
                    + "and container-pegasus-restli-server-cmpt version.";
            LOGGER.warn(String.format(fmt, model.getResourceClass().getName(), desc.getMethod().getName()));
          }
        }
      }
    }

    ErrorResponseBuilder errorResponseBuilder = new ErrorResponseBuilder(config.getErrorResponseFormat());

    _restRestLiServer = new RestRestLiServer(config,
        resourceFactory,
        engine,
        rootResources,
        errorResponseBuilder);
    _streamRestLiServer = new AttachmentHandlingRestLiServer(config,
        resourceFactory,
        engine,
        rootResources,
        errorResponseBuilder);
  }

  @Override
  public void handleRequest(RestRequest request, RequestContext requestContext, Callback<RestResponse> callback)
  {
    //This code path cannot accept content types or accept types that contain
    //multipart/related. This is because these types of requests will usually have very large payloads and therefore
    //would degrade server performance since RestRequest reads everything into memory.
    if (!isMultipart(request, callback))
    {
      _restRestLiServer.handleRequest(request, requestContext, callback);
    }
  }

  @Override
  public void handleRequestWithRestLiResponse(RestRequest request, RequestContext requestContext, Callback<RestLiResponse> callback)
  {
    if (!isMultipart(request, callback))
    {
      _restRestLiServer.handleRequestWithRestLiResponse(request, requestContext, callback);
    }
  }

  @Override
  public void handleRequest(StreamRequest request, RequestContext requestContext, Callback<StreamResponse> callback)
  {
    _streamRestLiServer.handleRequest(request, requestContext, callback);
  }

  @Override
  public void handleRequestWithRestLiResponse(StreamRequest request, RequestContext requestContext, Callback<RestLiResponse> callback)
  {
    _streamRestLiServer.handleRequestWithRestLiResponse(request, requestContext, callback);
  }

  private boolean isMultipart(final Request request, final Callback<?> callback)
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
