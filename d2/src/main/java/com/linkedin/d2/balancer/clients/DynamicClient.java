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

package com.linkedin.d2.balancer.clients;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.SuccessCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.D2Client;
import com.linkedin.d2.balancer.Facilities;
import com.linkedin.d2.balancer.LoadBalancer;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.util.LoadBalancerUtil;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.timing.TimingContextUtil;
import com.linkedin.r2.message.timing.TimingKey;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.timing.TimingImportance;
import com.linkedin.r2.message.timing.TimingNameConstants;
import com.linkedin.r2.transport.common.AbstractClient;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.d2.discovery.util.LogUtil.debug;
import static com.linkedin.d2.discovery.util.LogUtil.error;
import static com.linkedin.d2.discovery.util.LogUtil.info;
import static com.linkedin.d2.discovery.util.LogUtil.trace;
import static com.linkedin.d2.discovery.util.LogUtil.warn;


public class DynamicClient extends AbstractClient implements D2Client
{
  private static final Logger _log = LoggerFactory.getLogger(DynamicClient.class);

  private static final TimingKey TIMING_KEY = TimingKey.registerNewKey(TimingNameConstants.D2_TOTAL, TimingImportance.MEDIUM);

  private final LoadBalancer  _balancer;
  private final Facilities    _facilities;
  private final boolean       _restOverStream;

  public DynamicClient(LoadBalancer balancer, Facilities facilities)
  {
    this(balancer, facilities, R2Constants.DEFAULT_REST_OVER_STREAM);
  }

  public DynamicClient(LoadBalancer balancer, Facilities facilities, boolean restOverStream)
  {
    _balancer = balancer;
    _facilities = facilities;
    _restOverStream = restOverStream;
    debug(_log, "created dynamic client: ", this);
  }

  @Override
  public void restRequest(RestRequest request,
                          RequestContext requestContext,
                          final Callback<RestResponse> callback)
  {
    if (!_restOverStream)
    {
      Callback<RestResponse> loggerCallback = decorateLoggingCallback(callback, request, "rest");
      TimingContextUtil.markTiming(requestContext, TIMING_KEY);
      _balancer.getClient(request, requestContext,
        getClientCallback(request, requestContext, false, callback, client -> client.restRequest(request, requestContext, loggerCallback))
      );
    }
    else
    {
      super.restRequest(request, requestContext, callback);
    }
  }


  @Override
  public void streamRequest(StreamRequest request,
                          RequestContext requestContext,
                          final Callback<StreamResponse> callback)
  {
    Callback<StreamResponse> loggerCallback = decorateLoggingCallback(callback, request, "stream");

    _balancer.getClient(request, requestContext,
      getClientCallback(request, requestContext, true, callback, client -> client.streamRequest(request, requestContext, loggerCallback))
    );
  }

  @Override
  public void restRequestStreamResponse(RestRequest request, RequestContext requestContext,
      Callback<StreamResponse> callback) {
    Callback<StreamResponse> loggerCallback = decorateLoggingCallback(callback, request, "stream");

    _balancer.getClient(request, requestContext,
        getClientCallback(request, requestContext, true, callback, client -> client.restRequestStreamResponse(request, requestContext, loggerCallback))
    );
  }

  private Callback<TransportClient> getClientCallback(Request request, RequestContext requestContext, final boolean restOverStream, Callback<? extends Response> callback, SuccessCallback<Client> clientSuccessCallback)
  {
    return new Callback<TransportClient>()
    {
      @Override
      public void onError(Throwable e)
      {
        TimingContextUtil.markTiming(requestContext, TIMING_KEY);
        callback.onError(e);

        warn(_log, "unable to find service for: ", extractLogInfo(request));
      }

      @Override
      public void onSuccess(TransportClient client)
      {
        TimingContextUtil.markTiming(requestContext, TIMING_KEY);
        if (client != null)
        {
          clientSuccessCallback.onSuccess(new TransportClientAdapter(client, restOverStream));
        }
        else
        {
          callback.onError(new ServiceUnavailableException("PEGA_1000. Unknown: " + request.getURI(),
            "got null client from load balancer"));
        }
      }
    };
  }

  @Override
  public void start(Callback<None> callback)
  {
    _log.info("starting D2 client");
    _balancer.start(callback);
  }

  @Override
  public void shutdown(final Callback<None> callback)
  {
    info(_log, "shutting down dynamic client");

    _balancer.shutdown(() -> {
      info(_log, "dynamic client shutdown complete");

      callback.onSuccess(None.none());
    });
  }

  @Override
  public Facilities getFacilities()
  {
    return _facilities;
  }

  @Override
  public void getMetadata(URI uri, Callback<Map<String, Object>> callback)
  {
    if (_balancer == null)
    {
      callback.onSuccess(Collections.emptyMap());
      return;
    }
    String serviceName = LoadBalancerUtil.getServiceNameFromUri(uri);
    _balancer.getLoadBalancedServiceProperties(serviceName, new Callback<ServiceProperties>()
    {
      @Override
      public void onError(Throwable e)
      {
        error(_log, e);
        callback.onSuccess(Collections.emptyMap());
      }

      @Override
      public void onSuccess(ServiceProperties serviceProperties)
      {
        if (serviceProperties == null)
        {
          callback.onSuccess(Collections.emptyMap());
          return;
        }
        callback.onSuccess(Collections.unmodifiableMap(serviceProperties.getServiceMetadataProperties()));
      }
    });
  }

  private static <T> Callback<T> decorateLoggingCallback(final Callback<T> callback, Request request, final String type)
  {
    if (_log.isTraceEnabled())
    {
      trace(_log, type + " request: ", request);
      return new Callback<T>()
      {
        @Override
        public void onError(Throwable e)
        {
          callback.onError(e);
          trace(_log, type + " response error: ", e);
        }

        @Override
        public void onSuccess(final T result)
        {
          callback.onSuccess(result);
          trace(_log, type + " response success: ", result);
        }
      };
    }

    return callback;
  }

  private static String extractLogInfo(Request request)
  {
    return request.getClass().getName() + ": [" +
        "Service: " + LoadBalancerUtil.getServiceNameFromUri(request.getURI()) + ", " +
        "Method: " + request.getMethod() +
        "]";
  }
}
