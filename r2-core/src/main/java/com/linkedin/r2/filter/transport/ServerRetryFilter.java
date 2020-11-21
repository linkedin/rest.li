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

/* $Id$ */
package com.linkedin.r2.filter.transport;

import com.linkedin.r2.RetriableRequestException;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.filter.message.stream.StreamFilter;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.http.common.HttpConstants;
import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SystemClock;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter implementation that processes a retrieable response. Our contracts requires user to throw
 * {@link RetriableRequestException} when they want to request a retry. This filter catches that exception
 * and converts it to a wire attributes that will be sent back to the client side.
 *
 * @author Xialin Zhu
 * @see ClientRetryFilter
 */
public class ServerRetryFilter implements RestFilter, StreamFilter
{
  private static final Logger LOG = LoggerFactory.getLogger(ServerRetryFilter.class);
  private static final int DEFAULT_RETRY_LIMIT = 3;
  private static final long DEFAULT_UPDATE_INTERVAL_MS = TimeUnit.SECONDS.toMillis(5);
  private static final int DEFAULT_AGGREGATED_INTERVAL_NUM = 5;
  private static final double DEFAULT_MAX_REQUEST_RETRY_RATIO = 0.1;

  private final ServerRetryTracker _serverRetryTracker;

  public ServerRetryFilter()
  {
    this(SystemClock.instance(), DEFAULT_RETRY_LIMIT, DEFAULT_MAX_REQUEST_RETRY_RATIO, DEFAULT_UPDATE_INTERVAL_MS, DEFAULT_AGGREGATED_INTERVAL_NUM);
  }

  public ServerRetryFilter(Clock clock, int retryLimit, double maxRequestRetryRatio, long updateIntervalMs, int aggregatedIntervalNum)
  {
    _serverRetryTracker = new ServerRetryTracker(retryLimit, aggregatedIntervalNum, maxRequestRetryRatio, updateIntervalMs, clock);
  }

  @Override
  public void onRestRequest(RestRequest req,
      RequestContext requestContext,
      Map<String, String> wireAttrs,
      NextFilter<RestRequest, RestResponse> nextFilter)
  {
    updateRetryTracker(req);
    nextFilter.onRequest(req, requestContext, wireAttrs);
  }

  @Override
  public void onStreamRequest(StreamRequest req,
      RequestContext requestContext,
      Map<String, String> wireAttrs,
      NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    updateRetryTracker(req);
    nextFilter.onRequest(req, requestContext, wireAttrs);
  }

  @Override
  public void onRestError(Throwable ex,
      RequestContext requestContext,
      Map<String, String> wireAttrs,
      NextFilter<RestRequest, RestResponse> nextFilter)
  {
    processError(ex, requestContext, wireAttrs, nextFilter);
  }

  @Override
  public void onStreamError(Throwable ex,
      RequestContext requestContext,
      Map<String, String> wireAttrs,
      NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    processError(ex, requestContext, wireAttrs, nextFilter);
  }

  private <REQ extends Request, RES extends Response> void processError(Throwable ex,
      RequestContext requestContext,
      Map<String, String> wireAttrs,
      NextFilter<REQ, RES> nextFilter)
  {
    Throwable cause = ex.getCause();
    while (cause != null)
    {
      if (cause instanceof RetriableRequestException)
      {
        String message = cause.getMessage();
        if (_serverRetryTracker.isBelowRetryRatio())
        {
          LOG.debug("RetriableRequestException caught! Do retry. Error message: {}", message);
          wireAttrs.put(R2Constants.RETRY_MESSAGE_ATTRIBUTE_KEY, message);
        }
        else
        {
          LOG.debug("Max request retry ratio exceeded! Will not retry. Error message: {}", message);
          wireAttrs.remove(R2Constants.RETRY_MESSAGE_ATTRIBUTE_KEY);
        }
        break;
      }
      cause = cause.getCause();
    }

    nextFilter.onError(ex, requestContext, wireAttrs);
  }

  private void updateRetryTracker(Request req)
  {
    String retryAttemptsHeader = req.getHeader(HttpConstants.HEADER_NUMBER_OF_RETRY_ATTEMPTS);
    if (retryAttemptsHeader != null)
    {
      _serverRetryTracker.add(Integer.parseInt(retryAttemptsHeader));
    }
  }

  /**
   * Stores the number of requests categorized by number of retry attempts. It uses the information to estimate
   * a ratio of how many requests are being retried in the cluster. The ratio is then compared with
   * {@link ServerRetryTracker#_maxRequestRetryRatio} to make a decision on whether or not to retry in the
   * next interval. When calculating the ratio, it looks at the last {@link ServerRetryTracker#_aggregatedIntervalNum}
   * intervals by aggregating the recorded requests.
   */
  private static class ServerRetryTracker
  {
    private final int _retryLimit;
    private final int _aggregatedIntervalNum;
    private final double _maxRequestRetryRatio;
    private final long _updateIntervalMs;
    private final Clock _clock;

    private final Object _counterLock = new Object();
    private final Object _updateLock = new Object();

    @GuardedBy("_updateLock")
    private volatile long _lastRollOverTime;
    private boolean _isBelowRetryRatio;

    @GuardedBy("_counterLock")
    private final LinkedList<int[]> _retryAttemptsCounter;
    private final int[] _aggregatedRetryAttemptsCounter;

    private ServerRetryTracker(int retryLimit, int aggregatedIntervalNum, double maxRequestRetryRatio, long updateIntervalMs, Clock clock)
    {
      _retryLimit = retryLimit;
      _aggregatedIntervalNum = aggregatedIntervalNum;
      _maxRequestRetryRatio = maxRequestRetryRatio;
      _updateIntervalMs = updateIntervalMs;
      _clock = clock;

      _aggregatedRetryAttemptsCounter = new int[_retryLimit + 1];
      _retryAttemptsCounter = new LinkedList<>();
      _retryAttemptsCounter.add(new int[_retryLimit + 1]);
    }

    public void add(int numberOfRetryAttempts)
    {
      if (numberOfRetryAttempts <= _retryLimit)
      {
        synchronized (_counterLock)
        {
          _retryAttemptsCounter.getLast()[numberOfRetryAttempts] += 1;
        }
      } else
      {
        LOG.warn("Unexpected number of retry attempts: " + numberOfRetryAttempts + ", current retry limit: " + _retryLimit);
      }

      updateRetryDecision();
    }

    public void rollOverStats()
    {
      // rollover the current interval to the aggregated counter
      synchronized (_counterLock)
      {
        int[] intervalToAggregate = _retryAttemptsCounter.getLast();
        for (int i = 0; i < _retryLimit; i++)
        {
          _aggregatedRetryAttemptsCounter[i] += intervalToAggregate[i];
        }

        if (_retryAttemptsCounter.size() > _aggregatedIntervalNum)
        {
          // discard the oldest interval
          int[] intervalToDiscard = _retryAttemptsCounter.removeFirst();
          for (int i = 0; i < _retryLimit; i++)
          {
            _aggregatedRetryAttemptsCounter[i] -= intervalToDiscard[i];
          }
        }

        // append a new interval
        _retryAttemptsCounter.addLast(new int[_retryLimit + 1]);
      }
    }

    public boolean isBelowRetryRatio()
    {
      updateRetryDecision();
      return _isBelowRetryRatio;
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

          _isBelowRetryRatio = getRetryRatio() <= _maxRequestRetryRatio;
          _lastRollOverTime = currentTime;
        }
      }
    }

    private double getRetryRatio()
    {
      double retryRatioSum = 0.0;
      int i;

      for (i = 1; i <= _retryLimit; i++)
      {
        if (_aggregatedRetryAttemptsCounter[i] == 0 || _aggregatedRetryAttemptsCounter[i - 1] == 0)
        {
          break;
        }
        double ratio = (double) _aggregatedRetryAttemptsCounter[i] / _aggregatedRetryAttemptsCounter[i - 1];

        // We put more weights to the retry requests with larger number of attempts
        double adjustedRatio = Double.min(ratio * i, 1.0);
        retryRatioSum += adjustedRatio;
      }

      return i > 1 ? retryRatioSum / (i - 1) : 0.0;
    }
  }
}
