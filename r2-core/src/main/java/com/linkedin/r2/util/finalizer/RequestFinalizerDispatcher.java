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

package com.linkedin.r2.util.finalizer;

import com.linkedin.data.ByteString;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.Observer;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A {@link TransportDispatcher} decorator that places a {@link RequestFinalizerManager} into the
 * {@link RequestContext} to be executed at the end of a request. These are intended to be the last
 * executions that a server will process when serving a request.
 *
 * @author Chris Zhang
 */
public class RequestFinalizerDispatcher implements TransportDispatcher
{
  private static final Logger LOG = LoggerFactory.getLogger(RequestFinalizerDispatcher.class);

  private final TransportDispatcher _transportDispatcher;

  public RequestFinalizerDispatcher(TransportDispatcher transportDispatcher)
  {
    _transportDispatcher = transportDispatcher;
  }

  @Override
  public void handleRestRequest(RestRequest req, Map<String, String> wireAttrs, RequestContext requestContext,
      TransportCallback<RestResponse> callback)
  {
    _transportDispatcher.handleRestRequest(req, wireAttrs, requestContext,
        new RequestFinalizerTransportCallback<>(callback, requestContext, req));
  }

  @Override
  public void handleStreamRequest(StreamRequest req, Map<String, String> wireAttrs,
      RequestContext requestContext, TransportCallback<StreamResponse> callback)
  {
    _transportDispatcher.handleStreamRequest(req, wireAttrs, requestContext,
        new RequestFinalizerTransportCallback<>(callback, requestContext, req));
  }

  /**
   * {@link TransportCallback} decorator that executes {@link com.linkedin.r2.util.finalizer.RequestFinalizerManager}
   * at the end of the request. Used for REST requests.
   */
  private class RequestFinalizerTransportCallback<T extends Response> implements TransportCallback<T>
  {
    private final RequestFinalizerManagerImpl _manager;
    private final RequestContext _requestContext;
    private final Request _request;
    private final TransportCallback<T> _transportCallback;

    public RequestFinalizerTransportCallback(TransportCallback<T> transportCallback, RequestContext requestContext,
        Request request)
    {
      _manager = addRequestFinalizerManager(request, requestContext);
      _requestContext = requestContext;
      _request = request;
      _transportCallback = transportCallback;
    }

    private RequestFinalizerManagerImpl addRequestFinalizerManager(Request request, RequestContext requestContext)
    {
      RequestFinalizerManagerImpl manager = (RequestFinalizerManagerImpl) requestContext.getLocalAttr(
        R2Constants.SERVER_REQUEST_FINALIZER_MANAGER_REQUEST_CONTEXT_KEY);

      if (manager != null)
      {
        return manager;
      }
      else
      {
        manager = new RequestFinalizerManagerImpl(request, requestContext);
        requestContext.putLocalAttr(R2Constants.SERVER_REQUEST_FINALIZER_MANAGER_REQUEST_CONTEXT_KEY, manager);
        return manager;
      }
    }

    /**
     * For REST requests: Finalize the request immediately after invoking the decorated callback's #onResponse.
     * For STREAM requests: Add an observer to the entity stream that will finalize the request when streaming is finished.
     *
     * @param transportResponse {@link TransportResponse} to be passed to this callback.
     */
    @Override
    public void onResponse(TransportResponse<T> transportResponse)
    {
      final Response response = transportResponse.getResponse();
      final Throwable error = transportResponse.getError();
      final boolean isStream = response instanceof StreamResponse;

      if (isStream)
      {
        addObserver((StreamResponse) response, error);
      }

      boolean throwable = false;
      try
      {
        _transportCallback.onResponse(transportResponse);
      }
      catch (Throwable e)
      {
        LOG.warn("Encountered throwable invoking TransportCallback.", e);
        throwable = true;

        finalizeRequest(response, e);
      }

      if (!isStream && !throwable)
      {
        finalizeRequest(response, error);
      }
    }

    private void addObserver(StreamResponse streamResponse, Throwable error)
    {
      streamResponse.getEntityStream().addObserver(new Observer() {

        @Override
        public void onDataAvailable(ByteString data)
        {
          // do nothing
        }

        @Override
        public void onDone()
        {
          _manager.finalizeRequest(streamResponse, error);
        }

        @Override
        public void onError(Throwable e)
        {
          _manager.finalizeRequest(streamResponse,  e);
        }
      });
    }

    private void finalizeRequest(Response response, Throwable error)
    {
      final boolean finalized = _manager.finalizeRequest(response, error);

      if (!finalized)
      {
        LOG.warn("Request has already been finalized, but we expect this to be the first time.");
      }
    }
  }
}
