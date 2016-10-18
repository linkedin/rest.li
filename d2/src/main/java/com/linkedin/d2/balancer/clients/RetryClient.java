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
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.D2Client;
import com.linkedin.d2.balancer.Facilities;
import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy.ExcludedHostHints;
import com.linkedin.r2.RetriableRequestException;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * {@link DynamicClient} with retry feature. The callback passed in will be decorated with
 * another callback that will try to send the request again to another host in the cluster
 * instead of returning response when the response is a retriable failure.
 *
 * Only instantiated when retry in {@link D2ClientConfig} is enabled. Need to be used together with
 * {@link com.linkedin.r2.filter.transport.ClientRetryFilter}
 *
 * @author Xialin Zhu
 */
public class RetryClient implements D2Client
{
  private static final Logger LOG = LoggerFactory.getLogger(RetryClient.class);

  private final D2Client _d2Client;

  private final int _limit;

  public RetryClient(D2Client d2Client, int limit)
  {
    _d2Client = d2Client;
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
    Callback<RestResponse> transportCallback = decorateCallback(request, requestContext, _d2Client::<RestRequest, RestResponse>restRequest, callback);

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
    Callback<StreamResponse> transportCallback = decorateCallback(request, requestContext, _d2Client::<StreamRequest, StreamResponse>streamRequest, callback);

    _d2Client.streamRequest(request, requestContext, transportCallback);
  }

  interface DecoratorClient<R, T>
  {
    void doRequest(R request, RequestContext requestContext, Callback<T> callback);
  }

  private <R, T> Callback<T> decorateCallback(R request, RequestContext requestContext, DecoratorClient<R, T> client, Callback<T> callback)
  {
    return new Callback<T>()
    {
      @Override
      public void onError(Throwable e)
      {
        // Retry will be triggered if and only if:
        // 1. A RetriableRequestException is thrown
        // 2. There is no target host hint
        boolean retry = false;
        if (e instanceof RetriableRequestException)
        {
          URI targetHostUri = KeyMapper.TargetHostHints.getRequestContextTargetHost(requestContext);
          if (targetHostUri == null)
          {
            Set<URI> exclusionSet = ExcludedHostHints.getRequestContextExcludedHosts(requestContext);
            int attempts = exclusionSet.size();
            if (attempts <= _limit)
            {
              LOG.warn("A retriable exception happens. Going to retry. This is attempt {}. Current exclusion set: ", attempts, ". Current exclusion set: " + exclusionSet);
              retry = true;

              client.doRequest(request, requestContext, this);
            }
            else
            {
              LOG.warn("Retry limit exceeded. This request will fail.");
            }
          }
        }
        if (!retry)
        {
          ExcludedHostHints.clearRequestContextExcludedHosts(requestContext);
          callback.onError(e);
        }
      }

      @Override
      public void onSuccess(T result)
      {
        ExcludedHostHints.clearRequestContextExcludedHosts(requestContext);
        callback.onSuccess(result);
      }
    };
  }

  @Override
  public void shutdown(Callback<None> callback)
  {
    _d2Client.shutdown(callback);
  }

  @Override
  public Map<String, Object> getMetadata(URI uri)
  {
    return _d2Client.getMetadata(uri);
  }

  @Override
  public Facilities getFacilities()
  {
    return _d2Client.getFacilities();
  }

  @Override
  public void start(Callback<None> callback)
  {
    _d2Client.start(callback);
  }
}
