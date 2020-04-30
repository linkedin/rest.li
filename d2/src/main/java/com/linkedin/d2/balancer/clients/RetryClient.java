/*
   Copyright (c) 2016 LinkedIn Corp.

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

package com.linkedin.d2.balancer.clients;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.d2.balancer.D2Client;
import com.linkedin.d2.balancer.D2ClientConfig;
import com.linkedin.d2.balancer.D2ClientDelegator;
import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy.ExcludedHostHints;
import com.linkedin.data.ByteString;
import com.linkedin.r2.RetriableRequestException;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.FullEntityObserver;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.Future;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DynamicClient} with retry feature. The callback passed in will be decorated with
 * another callback that will try to send the request again to another host in the cluster
 * instead of returning response when the response is a retriable failure.
 *
 * Only instantiated when retry in {@link D2ClientConfig} is enabled. Need to be used together with
 * {@link com.linkedin.r2.filter.transport.ClientRetryFilter}
 *
 * Note: RetryClient records the {@link EntityStream} for {@link StreamRequest} so the entity will
 * be buffered in memory even if #streamRequest is invoked.
 *
 * @author Xialin Zhu
 */
public class RetryClient extends D2ClientDelegator
{
  private static final Logger LOG = LoggerFactory.getLogger(RetryClient.class);

  private final int _limit;

  public RetryClient(D2Client d2Client, int limit)
  {
    super(d2Client);
    _limit = limit;
    LOG.debug("Retry client created with limit set to: ", _limit);
  }

  @Override
  public Future<RestResponse> restRequest(RestRequest request)
  {
    return restRequest(request, new RequestContext());
  }

  @Override
  public Future<RestResponse> restRequest(RestRequest request, RequestContext requestContext)
  {
    final FutureCallback<RestResponse> future = new FutureCallback<RestResponse>();
    restRequest(request, requestContext, future);
    return future;
  }

  @Override
  public void restRequest(RestRequest request, Callback<RestResponse> callback)
  {
    restRequest(request, new RequestContext(), callback);
  }

  @Override
  public void restRequest(final RestRequest request,
                          final RequestContext requestContext,
                          final Callback<RestResponse> callback)
  {
    final Callback<RestResponse> transportCallback = new RestRetryRequestCallback(request, requestContext, callback);
    _d2Client.restRequest(request, requestContext, transportCallback);
  }

  @Override
  public void streamRequest(StreamRequest request, Callback<StreamResponse> callback)
  {
    streamRequest(request, new RequestContext(), callback);
  }

  @Override
  public void streamRequest(StreamRequest request, RequestContext requestContext, Callback<StreamResponse> callback)
  {
    final Callback<StreamResponse> transportCallback = new StreamRetryRequestCallback(request, requestContext, callback);
    _d2Client.streamRequest(request, requestContext, transportCallback);
  }

  /**
   * Callback implementation for Retry {@link StreamRequest} and {@link StreamResponse}
   */
  private class StreamRetryRequestCallback extends RetryRequestCallback<StreamRequest, StreamResponse>
  {
    // Acts as the memory barrier for content
    private volatile boolean _recorded = false;
    private ByteString _content = null;

    public StreamRetryRequestCallback(StreamRequest request, RequestContext context, Callback<StreamResponse> callback)
    {
      super(request, context, callback);

      final FullEntityObserver observer = new FullEntityObserver(new Callback<ByteString>()
      {
        @Override
        public void onError(Throwable e)
        {
          if (_recorded)
          {
            return;
          }
          LOG.warn("Failed to record request's entity for retrying.");
          _content = null;
          _recorded = true;
        }

        @Override
        public void onSuccess(ByteString result)
        {
          if (_recorded)
          {
            return;
          }
          _content = result;
          _recorded = true;
        }
      });
      request.getEntityStream().addObserver(observer);
    }

    @Override
    public boolean doRetryRequest(StreamRequest request, RequestContext context)
    {
      if (_recorded == true && _content != null)
      {
        final StreamRequest newRequest = request.builder().build(EntityStreams.newEntityStream(new ByteStringWriter(_content)));
        _d2Client.streamRequest(newRequest, new RequestContext(context), this);
        return true;
      }

      LOG.warn("Request's entity has not been recorded before retrying.");
      return false;
    }
  }

  /**
   * Callback implementation for Retry {@link RestRequest} and {@link RestResponse}
   */
  private class RestRetryRequestCallback extends RetryRequestCallback<RestRequest, RestResponse>
  {
    public RestRetryRequestCallback(RestRequest request, RequestContext context, Callback<RestResponse> callback)
    {
      super(request, context, callback);
    }

    @Override
    public boolean doRetryRequest(RestRequest request, RequestContext context)
    {
      _d2Client.restRequest(request, context, this);
      return true;
    }
  }

  /**
   * Abstract callback implementation of retry requests.
   *
   * @param <REQ> Retry request type.
   * @param <RESP> Retry response type.
   */
  private abstract class RetryRequestCallback<REQ extends Request, RESP extends Response> implements Callback<RESP>
  {
    private final REQ _request;
    private final RequestContext _context;
    private final Callback<RESP> _callback;

    public RetryRequestCallback(REQ request, RequestContext context, Callback<RESP> callback)
    {
      _request = request;
      _context = context;
      _callback = callback;
    }

    @Override
    public void onSuccess(RESP result)
    {
      ExcludedHostHints.clearRequestContextExcludedHosts(_context);
      _callback.onSuccess(result);
    }

    @Override
    public void onError(Throwable e)
    {
      // Retry will be triggered if and only if:
      // 1. A RetriableRequestException is thrown
      // 2. There is no target host hint
      boolean retry = false;
      if (isRetryException(e))
      {
        URI targetHostUri = KeyMapper.TargetHostHints.getRequestContextTargetHost(_context);
        if (targetHostUri == null)
        {
          Set<URI> exclusionSet = ExcludedHostHints.getRequestContextExcludedHosts(_context);
          int attempts = exclusionSet.size();
          if (attempts <= _limit)
          {
            LOG.warn("A retriable exception happens. Going to retry. This is attempt {}. Current exclusion set: ",
                attempts, ". Current exclusion set: " + exclusionSet);
            retry = doRetryRequest(_request, _context);
          }
          else
          {
            LOG.warn("Retry limit exceeded. This request will fail.");
          }
        }
      }
      if (!retry)
      {
        ExcludedHostHints.clearRequestContextExcludedHosts(_context);
        _callback.onError(e);
      }
    }

    private boolean isRetryException(Throwable e)
    {
      return ExceptionUtils.indexOfType(e, RetriableRequestException.class) != -1;
    }

    /**
     * Retries a specific request.
     *
     * @param request Request to retry.
     * @param context Context of the retry request.
     * @return {@code true} if a request can be retried; {@code false} otherwise;
     */
    public abstract boolean doRetryRequest(REQ request, RequestContext context);
  }
}
