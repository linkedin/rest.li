/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.r2.disruptor;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.filter.message.stream.StreamFilter;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.util.ArgumentUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An R2 {@link RestFilter} and {@link StreamFilter} implementation that performs the
 * disrupt specified in the {@link DisruptContext} stored inside the {@link RequestContext}.
 * The filter implements the follow types of disrupt on the request path.
 * <li>
 *   Delay: the filter schedules a task to resume the filter chain after the specified
 *   number of milliseconds in the {@link DisruptContext}
 * </li>
 * <li>
 *   Timeout: the filter schedules a task to invoke onError on the {@link NextFilter} with
 *   {@link TimeoutException}
 * </li>
 * <li>
 *   Error: the filter schedules a task to invoke onError on the {@link NextFilter} with
 *   the {@link Throwable} object specified in the {@link DisruptContext}
 * </li>
 *
 * @author Sean Sheng
 * @version $Revision$
 */
public class DisruptFilter implements StreamFilter, RestFilter
{
  private static final Logger LOG = LoggerFactory.getLogger(DisruptFilter.class);

  /**
   * Scheduler used to simulate delays in request pipeline. Do not use this to perform actual tasks.
   */
  private final ScheduledExecutorService _scheduler;

  /**
   * Executor used to perform actual tasks like send a request or returning an error.
   */
  private final ExecutorService _executor;

  /**
   * Request timeout configured for the current filter chain.
   */
  private final int _requestTimeout;

  private final Clock _clock;

  public DisruptFilter(ScheduledExecutorService scheduler, ExecutorService executor, int requestTimeout,
      Clock clock) {
    ArgumentUtil.notNull(scheduler, "scheduler");
    ArgumentUtil.notNull(executor, "executor");

    _scheduler = scheduler;
    _executor = executor;
    _requestTimeout = requestTimeout;
    _clock = clock;
  }

  @Deprecated
  public DisruptFilter(ScheduledExecutorService scheduler, ExecutorService executor, int requestTimeout) {
    this(scheduler, executor, requestTimeout, Clock.systemUTC());
  }

  @Override
  public void onStreamRequest(StreamRequest req,
      RequestContext requestContext,
      Map<String, String> wireAttrs,
      NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    disruptRequest(req, requestContext, wireAttrs, nextFilter);
  }

  @Override
  public void onStreamResponse(StreamResponse res,
      RequestContext requestContext,
      Map<String, String> wireAttrs,
      NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    disruptResponse(res, requestContext, wireAttrs, nextFilter);
  }

  @Override
  public void onRestRequest(RestRequest req,
      RequestContext requestContext,
      Map<String, String> wireAttrs,
      NextFilter<RestRequest, RestResponse> nextFilter)
  {
    disruptRequest(req, requestContext, wireAttrs, nextFilter);
  }

  @Override
  public void onRestResponse(RestResponse res,
      RequestContext requestContext,
      Map<String, String> wireAttrs,
      NextFilter<RestRequest, RestResponse> nextFilter)
  {
    disruptResponse(res, requestContext, wireAttrs, nextFilter);
  }

  private <REQ extends Request, RES extends Response> void disruptRequest(
      REQ req,
      RequestContext requestContext,
      Map<String, String> wireAttrs,
      NextFilter<REQ, RES> nextFilter)
  {
    final DisruptContext context = (DisruptContext) requestContext.getLocalAttr(DisruptContext.DISRUPT_CONTEXT_KEY);
    if (context == null)
    {
      nextFilter.onRequest(req, requestContext, wireAttrs);
      return;
    }

    try {
      switch (context.mode()) {
        case DELAY:
          DisruptContexts.DelayDisruptContext delayContext = (DisruptContexts.DelayDisruptContext) context;
          _scheduler.schedule(() -> {
            try {
              _executor.execute(() -> nextFilter.onRequest(req, requestContext, wireAttrs));
            } catch (RejectedExecutionException e) {
              LOG.error("Unable to continue filter chain execution after {} disrupt.", context.mode(), e);
            }
          }, delayContext.delay(), TimeUnit.MILLISECONDS);
          break;
        case ERROR:
          DisruptContexts.ErrorDisruptContext errorContext = (DisruptContexts.ErrorDisruptContext) context;
          _scheduler.schedule(() -> {
            try {
              DisruptedException throwable = new DisruptedException("Request is disrupted with an error response");
              _executor.execute(() -> nextFilter.onError(throwable, requestContext, wireAttrs));
            } catch (RejectedExecutionException e) {
              LOG.error("Unable to continue filter chain execution after {} disrupt.", context.mode(), e);
            }
          }, errorContext.latency(), TimeUnit.MILLISECONDS);
          break;
        case TIMEOUT:
          _scheduler.schedule(() -> {
            try {
              _executor.execute(() -> nextFilter.onError(
                  new TimeoutException("Exceeded request timeout of " + _requestTimeout + "ms due to disrupt"),
                  requestContext, wireAttrs));
            } catch (RejectedExecutionException e) {
              LOG.error("Unable to continue filter chain execution after {} disrupt.", context.mode(), e);
            }
          }, _requestTimeout, TimeUnit.MILLISECONDS);
          break;
        case MINIMUM_DELAY:
          DisruptContexts.MinimumDelayDisruptContext minimumDelayDisruptContext =
              (DisruptContexts.MinimumDelayDisruptContext) context;
          minimumDelayDisruptContext.requestStartTime(_clock.millis());
          nextFilter.onRequest(req, requestContext, wireAttrs);
          break;
        default:
          LOG.warn("Unrecognized disrupt mode {}", context.mode());
          nextFilter.onRequest(req, requestContext, wireAttrs);
          break;
      }
    } catch (RejectedExecutionException e) {
      LOG.warn("Unable to perform {} disrupt", context.mode(), e);
      nextFilter.onRequest(req, requestContext, wireAttrs);
    }
  }

  private <REQ extends Request, RES extends Response> void disruptResponse(
      RES res,
      RequestContext requestContext,
      Map<String, String> wireAttrs,
      NextFilter<REQ, RES> nextFilter)
  {
    final DisruptContext context = (DisruptContext) requestContext.getLocalAttr(DisruptContext.DISRUPT_CONTEXT_KEY);
    if (context == null)
    {
      nextFilter.onResponse(res, requestContext, wireAttrs);
      return;
    }

    switch (context.mode()) {
      case MINIMUM_DELAY:
        DisruptContexts.MinimumDelayDisruptContext minimumDelayContext =
            (DisruptContexts.MinimumDelayDisruptContext) context;

        final long startTime = minimumDelayContext.requestStartTime();
        final long totalDelay = _clock.millis() - startTime;
        long remainingDelay = minimumDelayContext.delay() - totalDelay;

        if (startTime == 0)
        {
          LOG.error("Failed to get request start time. Unable to apply {}.", context.mode());
          remainingDelay = 0;
        }
        else if (remainingDelay < 0)
        {
          LOG.debug("Total delay of {}ms is more than requested delay of {}ms. Skipping disruption.", totalDelay,
              minimumDelayContext.delay());
          remainingDelay = 0;
        }

        try {
          _scheduler.schedule(() -> {
            _executor.execute(() -> nextFilter.onResponse(res, requestContext, wireAttrs));
          }, remainingDelay, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
          LOG.warn("Unable to perform {} disrupt", context.mode(), e);
          nextFilter.onResponse(res, requestContext, wireAttrs);
        }
        break;
      case DELAY:
      case ERROR:
      case TIMEOUT:
        // intentional fall-through.
        // no action required for the above disrupt modes.
        nextFilter.onResponse(res, requestContext, wireAttrs);
        break;
      default:
        LOG.warn("Unrecognized disrupt mode {}", context.mode());
        nextFilter.onResponse(res, requestContext, wireAttrs);
        break;
    }
  }
}
