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
import com.linkedin.common.callback.SuccessCallback;
import com.linkedin.common.util.MapUtil;
import com.linkedin.d2.balancer.D2Client;
import com.linkedin.d2.balancer.D2ClientConfig;
import com.linkedin.d2.balancer.D2ClientDelegator;
import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.LoadBalancer;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy.ExcludedHostHints;
import com.linkedin.d2.balancer.util.LoadBalancerUtil;
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
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.common.HttpConstants;
import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SystemClock;
import java.net.URI;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.ThreadSafe;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.checkerframework.checker.lock.qual.GuardedBy;
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
  public static final long DEFAULT_UPDATE_INTERVAL_MS = TimeUnit.SECONDS.toMillis(1);
  public static final int DEFAULT_AGGREGATED_INTERVAL_NUM = 5;
  private static final Logger LOG = LoggerFactory.getLogger(RetryClient.class);

  private final Clock _clock;
  private final LoadBalancer _balancer;
  private final int _limit;
  private final long _updateIntervalMs;
  private final int _aggregatedIntervalNum;

  ConcurrentMap<String, ClientRetryTracker> _retryTrackerMap;

  public RetryClient(D2Client d2Client, LoadBalancer balancer, int limit)
  {
    this(d2Client, balancer, limit, DEFAULT_UPDATE_INTERVAL_MS, DEFAULT_AGGREGATED_INTERVAL_NUM, SystemClock.instance());
  }

  public RetryClient(D2Client d2Client, LoadBalancer balancer, int limit, long updateIntervalMs, int aggregatedIntervalNum, Clock clock)
  {
    super(d2Client);
    _balancer = balancer;
    _limit = limit;
    _updateIntervalMs = updateIntervalMs;
    _aggregatedIntervalNum = aggregatedIntervalNum;
    _clock = clock;
    _retryTrackerMap = new ConcurrentHashMap<>();

    LOG.debug("Retry client created with limit={}", _limit);
  }

  @Override
  public Future<RestResponse> restRequest(RestRequest request)
  {
    return restRequest(request, new RequestContext());
  }

  @Override
  public Future<RestResponse> restRequest(RestRequest request, RequestContext requestContext)
  {
    final FutureCallback<RestResponse> future = new FutureCallback<>();
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
    RestRequest newRequest = request.builder()
        .addHeaderValue(HttpConstants.HEADER_NUMBER_OF_RETRY_ATTEMPTS, "0")
        .build();
    ClientRetryTracker retryTracker = updateRetryTracker(newRequest.getURI(), false);
    final Callback<RestResponse> transportCallback = new RestRetryRequestCallback(newRequest, requestContext, callback, retryTracker);
    _d2Client.restRequest(newRequest, requestContext, transportCallback);
  }

  @Override
  public void streamRequest(StreamRequest request, Callback<StreamResponse> callback)
  {
    streamRequest(request, new RequestContext(), callback);
  }

  @Override
  public void streamRequest(StreamRequest request, RequestContext requestContext, Callback<StreamResponse> callback)
  {
    StreamRequest newRequest = request.builder()
        .addHeaderValue(HttpConstants.HEADER_NUMBER_OF_RETRY_ATTEMPTS, "0")
        .build(request.getEntityStream());
    ClientRetryTracker retryTracker = updateRetryTracker(newRequest.getURI(), false);
    final Callback<StreamResponse> transportCallback = new StreamRetryRequestCallback(newRequest, requestContext, callback, retryTracker);
    _d2Client.streamRequest(newRequest, requestContext, transportCallback);
  }

  private ClientRetryTracker updateRetryTracker(URI uri, boolean isRetry)
  {
    String serviceName = LoadBalancerUtil.getServiceNameFromUri(uri);
    ClientRetryTracker retryTracker = _retryTrackerMap.computeIfAbsent(serviceName,
        k -> new ClientRetryTracker(_aggregatedIntervalNum, _updateIntervalMs, _clock, k));
    retryTracker.add(isRetry);
    return retryTracker;
  }

  /**
   * Callback implementation for Retry {@link StreamRequest} and {@link StreamResponse}
   */
  private class StreamRetryRequestCallback extends RetryRequestCallback<StreamRequest, StreamResponse>
  {
    // Acts as the memory barrier for content
    private volatile boolean _recorded = false;
    private ByteString _content = null;

    public StreamRetryRequestCallback(StreamRequest request, RequestContext context, Callback<StreamResponse> callback, ClientRetryTracker retryTracker)
    {
      super(request, context, callback, retryTracker);

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
    public boolean doRetryRequest(StreamRequest request, RequestContext context, int numberOfRetryAttempts)
    {
      if (_recorded == true && _content != null)
      {
        final StreamRequest newRequest = request.builder()
            .addHeaderValue(HttpConstants.HEADER_NUMBER_OF_RETRY_ATTEMPTS, Integer.toString(numberOfRetryAttempts))
            .build(EntityStreams.newEntityStream(new ByteStringWriter(_content)));
        updateRetryTracker(request.getURI(), true);
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
    public RestRetryRequestCallback(RestRequest request, RequestContext context, Callback<RestResponse> callback, ClientRetryTracker retryTracker)
    {
      super(request, context, callback, retryTracker);
    }

    @Override
    public boolean doRetryRequest(RestRequest request, RequestContext context, int numberOfRetryAttempts)
    {
      RestRequest newRequest = request.builder()
          .addHeaderValue(HttpConstants.HEADER_NUMBER_OF_RETRY_ATTEMPTS, Integer.toString(numberOfRetryAttempts))
          .build();
      updateRetryTracker(request.getURI(), true);
      _d2Client.restRequest(newRequest, context, this);
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
    private final ClientRetryTracker _retryTracker;

    public RetryRequestCallback(REQ request, RequestContext context, Callback<RESP> callback, ClientRetryTracker retryTracker)
    {
      _request = request;
      _context = context;
      _callback = callback;
      _retryTracker = retryTracker;
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
          if (exclusionSet == null || exclusionSet.isEmpty())
          {
            LOG.warn("Excluded hosts hint for retry is not set or is empty. This failed request will not be retried.");
          }
          else
          {
            int attempts = exclusionSet.size();
            if (attempts <= _limit)
            {
              retry = true;
              _retryTracker.isBelowRetryRatio(isBelowRetryRatio ->
              {
                boolean doRetry;
                if (isBelowRetryRatio)
                {
                  LOG.warn("A retriable exception occurred. Going to retry. This is attempt {}. Current exclusion set: {}",
                      attempts, exclusionSet);
                  doRetry = doRetryRequest(_request, _context, attempts);
                }
                else
                {
                  LOG.warn("Client retry ratio exceeded. This request will fail.");
                  doRetry = false;
                }
                if (!doRetry)
                {
                  ExcludedHostHints.clearRequestContextExcludedHosts(_context);
                  _callback.onError(e);
                }
              });
            }
            else
            {
              LOG.warn("Retry limit exceeded. This request will fail.");
            }
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
      Throwable[] throwables = ExceptionUtils.getThrowables(e);

      for (Throwable throwable: throwables)
      {
        if (throwable instanceof RetriableRequestException)
        {
          return !((RetriableRequestException) throwable).getDoNotRetryOverride();
        }
      }

      return false;
    }

    /**
     * Retries a specific request.
     *
     * @param request Request to retry.
     * @param context Context of the retry request.
     * @param numberOfRetryAttempts Number of retry attempts.
     * @return {@code true} if a request can be retried; {@code false} otherwise;
     */
    public abstract boolean doRetryRequest(REQ request, RequestContext context, int numberOfRetryAttempts);
  }

  /**
   * Stores the ratio of retry requests to total requests. It reads maxClientRequestRetryRatio
   * from {@link com.linkedin.d2.D2TransportClientProperties} and compares with the current retry ratio to
   * decide whether or not to retry in the next interval. When calculating the ratio, it looks at the last
   * {@link ClientRetryTracker#_aggregatedIntervalNum} intervals by aggregating the recorded requests.
   */
  @ThreadSafe
  private class ClientRetryTracker
  {
    private final int _aggregatedIntervalNum;
    private final long _updateIntervalMs;
    private final Clock _clock;
    private final String _serviceName;

    private final Object _counterLock = new Object();
    private final Object _updateLock = new Object();

    @GuardedBy("_updateLock")
    private volatile long _lastRollOverTime;
    @GuardedBy("_updateLock")
    private double _currentAggregatedRetryRatio;

    @GuardedBy("_counterLock")
    private final LinkedList<RetryCounter> _retryCounter;
    @GuardedBy("_counterLock")
    private final RetryCounter _aggregatedRetryCounter;

    private ClientRetryTracker(int aggregatedIntervalNum, long updateIntervalMs, Clock clock, String serviceName)
    {
      _aggregatedIntervalNum = aggregatedIntervalNum;
      _updateIntervalMs = updateIntervalMs;
      _clock = clock;
      _serviceName = serviceName;

      _lastRollOverTime = clock.currentTimeMillis();
      _currentAggregatedRetryRatio = 0;

      _aggregatedRetryCounter = new RetryCounter();
      _retryCounter = new LinkedList<>();
      _retryCounter.add(new RetryCounter());
    }

    public void add(boolean isRetry)
    {
      synchronized (_counterLock)
      {
        if (isRetry)
        {
          _retryCounter.getLast().addToRetryRequestCount(1);
        }

        _retryCounter.getLast().addToTotalRequestCount(1);
      }
      updateRetryDecision();
    }

    public void rollOverStats()
    {
      // rollover the current interval to the aggregated counter
      synchronized (_counterLock)
      {
        RetryCounter intervalToAggregate = _retryCounter.getLast();
        _aggregatedRetryCounter.addToTotalRequestCount(intervalToAggregate.getTotalRequestCount());
        _aggregatedRetryCounter.addToRetryRequestCount(intervalToAggregate.getRetryRequestCount());

        if (_retryCounter.size() > _aggregatedIntervalNum)
        {
          // discard the oldest interval
          RetryCounter intervalToDiscard = _retryCounter.removeFirst();
          _aggregatedRetryCounter.subtractFromTotalRequestCount(intervalToDiscard.getTotalRequestCount());
          _aggregatedRetryCounter.subtractFromRetryRequestCount(intervalToDiscard.getRetryRequestCount());
        }

        // append a new interval
        _retryCounter.addLast(new RetryCounter());
      }
    }

    public void isBelowRetryRatio(SuccessCallback<Boolean> callback)
    {
      _balancer.getLoadBalancedServiceProperties(_serviceName, new Callback<ServiceProperties>()
      {
        @Override
        public void onError(Throwable e)
        {
          LOG.warn("Failed to fetch transportClientProperties ", e);
          callback.onSuccess(_currentAggregatedRetryRatio <= HttpClientFactory.DEFAULT_MAX_CLIENT_REQUEST_RETRY_RATIO);
        }

        @Override
        public void onSuccess(ServiceProperties result)
        {
          Map<String, Object> transportClientProperties = result.getTransportClientProperties();
          double maxClientRequestRetryRatio;
          if (transportClientProperties == null)
          {
            maxClientRequestRetryRatio = HttpClientFactory.DEFAULT_MAX_CLIENT_REQUEST_RETRY_RATIO;
          }
          else
          {
            maxClientRequestRetryRatio = MapUtil.getWithDefault(transportClientProperties,
                PropertyKeys.HTTP_MAX_CLIENT_REQUEST_RETRY_RATIO,
                HttpClientFactory.DEFAULT_MAX_CLIENT_REQUEST_RETRY_RATIO, Double.class);
          }
          callback.onSuccess(_currentAggregatedRetryRatio <= maxClientRequestRetryRatio);
        }
      });
    }

    private void updateRetryDecision()
    {
      long currentTime = _clock.currentTimeMillis();

      synchronized (_updateLock)
      {
        // Check if the current interval is stale
        if (currentTime >= _lastRollOverTime + _updateIntervalMs)
        {
          // Rollover stale intervals until the current interval is reached
          for (long time = currentTime; time >= _lastRollOverTime + _updateIntervalMs; time -= _updateIntervalMs)
          {
            rollOverStats();
          }

          _currentAggregatedRetryRatio = getRetryRatio();
          _lastRollOverTime = currentTime;
        }
      }
    }

    private double getRetryRatio()
    {
      int aggregatedTotalCount = _aggregatedRetryCounter.getTotalRequestCount();
      int aggregatedRetryCount = _aggregatedRetryCounter.getRetryRequestCount();

      return aggregatedTotalCount == 0 ? 0 : (double) aggregatedRetryCount / aggregatedTotalCount;
    }
  }

  private static class RetryCounter
  {
    private int _retryRequestCount;
    private int _totalRequestCount;

    public RetryCounter()
    {
      _retryRequestCount = 0;
      _totalRequestCount = 0;
    }

    public int getRetryRequestCount()
    {
      return _retryRequestCount;
    }

    public int getTotalRequestCount()
    {
      return _totalRequestCount;
    }

    public void addToRetryRequestCount(int count)
    {
      _retryRequestCount += count;
    }

    public void addToTotalRequestCount(int count)
    {
      _totalRequestCount += count;
    }

    public void subtractFromRetryRequestCount(int count)
    {
      _retryRequestCount -= count;
    }

    public void subtractFromTotalRequestCount(int count)
    {
      _totalRequestCount -= count;
    }
  }
}