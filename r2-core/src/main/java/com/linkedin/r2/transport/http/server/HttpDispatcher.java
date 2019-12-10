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

/* $Id$ */
package com.linkedin.r2.transport.http.server;

import com.linkedin.data.ByteString;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.BaseConnector;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.Observer;
import com.linkedin.r2.message.timing.FrameworkTimingKeys;
import com.linkedin.r2.message.timing.TimingContextUtil;
import com.linkedin.r2.transport.common.MessageType;
import com.linkedin.r2.transport.common.WireAttributeHelper;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.http.common.HttpBridge;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Chris Pettitt
 * @author Zhenkai Zhu
 * @version $Revision$
 */
public class HttpDispatcher
{
  private final TransportDispatcher _dispatcher;

  /**
   * Construct a new instance which delegates to the specified dispatcher.
   *
   * @param dispatcher the {@link com.linkedin.r2.transport.common.bridge.server.TransportDispatcher} to which requests are delegated.
   * @deprecated Use {@link HttpDispatcherFactory#create(TransportDispatcher)} instead.
   */
  @Deprecated
  public HttpDispatcher(TransportDispatcher dispatcher)
  {
    _dispatcher = dispatcher;
  }

  /**
   * handle a {@link com.linkedin.r2.message.rest.RestRequest}.
   * @see TransportDispatcher#handleRestRequest
   *
   * @param req the request to be handled.
   * @param callback the callback to be invoked with the response or error.
   */
  public void handleRequest(RestRequest req,
                            TransportCallback<RestResponse> callback)
  {
    handleRequest(req, new RequestContext(), callback);
  }

  /**
   * handle a {@link RestRequest} using the given request context.
   * @see TransportDispatcher#handleRestRequest
   *
   * @param req the request to be handled.
   * @param context the request context.
   * @param callback the callback to be invoked with the response or error.
   */
  public void handleRequest(RestRequest req,
                            RequestContext context,
                            TransportCallback<RestResponse> callback)
  {
    markOnRequestTimings(context);

    final Map<String, String> headers = new HashMap<String, String>(req.getHeaders());
    final Map<String, String> wireAttrs = WireAttributeHelper.removeWireAttributes(headers);

    try
    {
      MessageType.Type msgType = MessageType.getMessageType(wireAttrs, MessageType.Type.REST);
      switch (msgType)
      {
        default:
        case REST:
          _dispatcher.handleRestRequest(HttpBridge.toRestRequest(req, headers),
              wireAttrs,
              context, HttpBridge.httpToRestCallback(callback)
          );
      }
    }
    catch (Exception e)
    {
      callback.onResponse(TransportResponseImpl.<RestResponse>error(e, Collections.<String, String>emptyMap()));
    }
  }

  /**
   * handle a {@link com.linkedin.r2.message.stream.StreamRequest}.
   * @see com.linkedin.r2.transport.common.bridge.server.TransportDispatcher#handleStreamRequest
   *
   * @param req the request to be handled.
   * @param callback the callback to be invoked with the response or error.
   */
  public void handleRequest(StreamRequest req,
                            TransportCallback<StreamResponse> callback)
  {
    handleRequest(req, new RequestContext(), callback);
  }

  /**
   * handle a {@link com.linkedin.r2.message.stream.StreamRequest} using the given request context.
   * @see com.linkedin.r2.transport.common.bridge.server.TransportDispatcher#handleStreamRequest
   *
   * @param req the request to be handled.
   * @param context the request context.
   * @param callback the callback to be invoked with the response or error.
   */
  public void handleRequest(StreamRequest req,
                            RequestContext context,
                            final TransportCallback<StreamResponse> callback)
  {
    markOnRequestTimings(context);

    final Map<String, String> headers = new HashMap<String, String>(req.getHeaders());
    final Map<String, String> wireAttrs = WireAttributeHelper.removeWireAttributes(headers);

    final BaseConnector connector = new BaseConnector();
    try
    {
      MessageType.Type msgType = MessageType.getMessageType(wireAttrs, MessageType.Type.REST);
      switch (msgType)
      {
        default:
        case REST:
          req.getEntityStream().setReader(connector);
          StreamRequest newReq = req.builder().build(EntityStreams.newEntityStream(connector));

          // decorate the call back so that if response is error or response finishes streaming,
          // we cancel the request stream
          TransportCallback<StreamResponse> decorateCallback = new TransportCallback<StreamResponse>()
          {
            @Override
            public void onResponse(TransportResponse<StreamResponse> response)
            {
              // no need to check StreamException because that's handled by HttpBridge.httpToStreamCallback
              if (response.hasError())
              {
                connector.cancel();
              }
              else
              {
                Observer observer = new Observer()
                {
                  @Override
                  public void onDataAvailable(ByteString data)
                  {
                    // do nothing
                  }

                  @Override
                  public void onDone()
                  {
                    connector.cancel();
                  }

                  @Override
                  public void onError(Throwable e)
                  {
                    connector.cancel();
                  }
                };
                response.getResponse().getEntityStream().addObserver(observer);
              }

              callback.onResponse(response);
            }
          };
          _dispatcher.handleStreamRequest(HttpBridge.toStreamRequest(newReq, headers),
                                        wireAttrs,
                                        context, HttpBridge.httpToStreamCallback(decorateCallback)
          );
      }
    }
    catch (Exception e)
    {
      connector.cancel();
      callback.onResponse(TransportResponseImpl.<StreamResponse>error(e, Collections.<String, String>emptyMap()));
    }
  }

  private static void markOnRequestTimings(RequestContext requestContext)
  {
    TimingContextUtil.beginTiming(requestContext, FrameworkTimingKeys.SERVER_REQUEST.key());
    TimingContextUtil.beginTiming(requestContext, FrameworkTimingKeys.SERVER_REQUEST_R2.key());
  }
}
