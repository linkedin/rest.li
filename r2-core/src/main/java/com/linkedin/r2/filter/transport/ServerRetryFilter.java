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
import com.linkedin.r2.util.ServerRetryTracker;
import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SystemClock;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter implementation that processes a retriable response. Our contracts requires user to throw
 * {@link RetriableRequestException} when they want to request a retry. This filter catches that exception
 * and converts it to a wire attributes that will be sent back to the client side.
 *
 * @author Xialin Zhu
 * @see ClientRetryFilter
 */
public class ServerRetryFilter implements RestFilter, StreamFilter
{
  private static final Logger LOG = LoggerFactory.getLogger(ServerRetryFilter.class);

  public static final int DEFAULT_RETRY_LIMIT = 3;
  public static final long DEFAULT_UPDATE_INTERVAL_MS = TimeUnit.SECONDS.toMillis(5);
  public static final int DEFAULT_AGGREGATED_INTERVAL_NUM = 5;
  public static final double DEFAULT_MAX_REQUEST_RETRY_RATIO = 0.1;

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
        if (!((RetriableRequestException) cause).getDoNotRetryOverride())
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
          }
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
}
