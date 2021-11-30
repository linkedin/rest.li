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
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyConfig;
import com.linkedin.d2.balancer.util.LoadBalancerUtil;
import com.linkedin.data.ByteString;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamException;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.Observer;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import com.linkedin.util.clock.Clock;
import com.linkedin.util.degrader.CallCompletion;
import com.linkedin.util.degrader.CallTracker;
import com.linkedin.util.degrader.CallTrackerImpl;
import com.linkedin.util.degrader.ErrorType;

import java.net.ConnectException;
import java.net.URI;
import java.nio.channels.ClosedChannelException;
import java.util.Collections;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.d2.discovery.util.LogUtil.debug;


/**
 * Default {@link TrackerClient} implementation.
 */
public class TrackerClientImpl implements TrackerClient
{
  public static final String DEFAULT_ERROR_STATUS_REGEX = "(5..)";
  public static final Pattern DEFAULT_ERROR_STATUS_PATTERN = Pattern.compile(DEFAULT_ERROR_STATUS_REGEX);
  public static final long DEFAULT_CALL_TRACKER_INTERVAL = DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS;

  private static final Logger _log = LoggerFactory.getLogger(TrackerClient.class);

  private final TransportClient _transportClient;
  private final Map<Integer, PartitionData> _partitionData;
  private final URI _uri;
  private final Predicate<Integer> _isErrorStatus;
  private final ConcurrentMap<Integer, Double> _subsetWeightMap;
  private final boolean _doNotLoadBalance;
  final CallTracker _callTracker;

  private boolean _doNotSlowStart;

  private volatile CallTracker.CallStats _latestCallStats;

  public TrackerClientImpl(URI uri, Map<Integer, PartitionData> partitionDataMap, TransportClient transportClient,
      Clock clock, long interval, Predicate<Integer> isErrorStatus)
  {
    this(uri, partitionDataMap, transportClient, clock, interval, isErrorStatus, true, false, false);
  }

  public TrackerClientImpl(URI uri, Map<Integer, PartitionData> partitionDataMap, TransportClient transportClient,
      Clock clock, long interval, Predicate<Integer> isErrorStatus, boolean percentileTrackingEnabled, boolean doNotSlowStart, boolean doNotLoadBalance)
  {
    _uri = uri;
    _transportClient = transportClient;
    _callTracker = new CallTrackerImpl(interval, clock, percentileTrackingEnabled);
    _isErrorStatus = isErrorStatus;
    _partitionData = Collections.unmodifiableMap(partitionDataMap);
    _latestCallStats = _callTracker.getCallStats();
    _doNotSlowStart = doNotSlowStart;
    _subsetWeightMap = new ConcurrentHashMap<>();
    _doNotLoadBalance = doNotLoadBalance;

    _callTracker.addStatsRolloverEventListener(event -> _latestCallStats = event.getCallStats());

    debug(_log, "created tracker client: ", this);
  }

  @Override
  public CallTracker.CallStats getLatestCallStats()
  {
    return _latestCallStats;
  }

  @Override
  public void shutdown(Callback<None> callback)
  {
    _transportClient.shutdown(callback);
  }

  @Override
  public TransportClient getTransportClient()
  {
    return _transportClient;
  }

  @Override
  public Map<Integer, PartitionData> getPartitionDataMap()
  {
    return _partitionData;
  }

  @Override
  public void setSubsetWeight(int partitionId, double partitionWeight)
  {
    _subsetWeightMap.put(partitionId, partitionWeight);
  }

  @Override
  public double getSubsetWeight(int partitionId) {
    return _subsetWeightMap.getOrDefault(partitionId, 1D);
  }

  @Override
  public void restRequest(RestRequest request,
                          RequestContext requestContext,
                          Map<String, String> wireAttrs,
                          TransportCallback<RestResponse> callback)
  {
    _transportClient.restRequest(request, requestContext, wireAttrs, new TrackerClientRestCallback(callback, _callTracker.startCall()));
  }

  @Override
  public void streamRequest(StreamRequest request,
                            RequestContext requestContext,
                            Map<String, String> wireAttrs,
                            TransportCallback<StreamResponse> callback)
  {
    _transportClient.streamRequest(request, requestContext, wireAttrs, new TrackerClientStreamCallback(callback, _callTracker.startCall()));
  }

  @Override
  public URI getUri()
  {
    return _uri;
  }

  @Override
  public CallTracker getCallTracker()
  {
    return _callTracker;
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + " [_uri=" + _uri + ", _partitionData=" + _partitionData + "]";
  }

  private class TrackerClientRestCallback implements TransportCallback<RestResponse>
  {
    private TransportCallback<RestResponse> _wrappedCallback;
    private CallCompletion       _callCompletion;

    public TrackerClientRestCallback(TransportCallback<RestResponse> wrappedCallback,
                                 CallCompletion callCompletion)
    {
      _wrappedCallback = wrappedCallback;
      _callCompletion = callCompletion;
    }

    @Override
    public void onResponse(TransportResponse<RestResponse> response)
    {
      if (response.hasError())
      {
        Throwable throwable = response.getError();
        handleError(_callCompletion, throwable);
      }
      else
      {
        _callCompletion.endCall();
      }

      _wrappedCallback.onResponse(response);
    }
  }

  @Override
  public void setDoNotSlowStart(boolean doNotSlowStart)
  {
    _doNotSlowStart = doNotSlowStart;
  }

  @Override
  public boolean doNotSlowStart()
  {
    return _doNotSlowStart;
  }

  @Override
  public boolean doNotLoadBalance()
  {
    return _doNotLoadBalance;
  }

  private class TrackerClientStreamCallback implements TransportCallback<StreamResponse>
  {
    private TransportCallback<StreamResponse> _wrappedCallback;
    private CallCompletion       _callCompletion;

    public TrackerClientStreamCallback(TransportCallback<StreamResponse> wrappedCallback,
                                 CallCompletion callCompletion)
    {
      _wrappedCallback = wrappedCallback;
      _callCompletion = callCompletion;
    }

    @Override
    public void onResponse(TransportResponse<StreamResponse> response)
    {
      if (response.hasError())
      {
        Throwable throwable = response.getError();
        handleError(_callCompletion, throwable);
      }
      else
      {
        EntityStream entityStream = response.getResponse().getEntityStream();

        /**
         * Because D2 use call tracking to evaluate the health of the servers, we cannot use the finish time of the
         * response streaming as the stop time. Otherwise, the server's health would be considered bad even if the
         * problem is on the client side due to the back pressure feature. Use D2 proxy as an example.
         * Client A -> D2 proxy -> Server B. If Client A has congested network connection, D2 proxy would observe
         * longer call duration due to back pressure from A. However, if D2 proxy now prematurely downgrade
         * Server B's health, when another Client C calls the same service, D2 proxy would probably exclude Server B
         * due to the "bad" health.
         *
         * Hence, D2 would record the stop time as the time when the first part of the response arrives.
         * However, the streaming process may fail or timeout; so D2 would wait until the streaming finishes, and
         * update the latency if it's successful, or update the error count if it's not successful.
         * In this way, D2 still monitors the responsiveness of a server without the interference from the client
         * side events, and error counting still works as before.
         */
        _callCompletion.record();
        Observer observer = new Observer()
        {
          @Override
          public void onDataAvailable(ByteString data)
          {
          }

          @Override
          public void onDone()
          {
            _callCompletion.endCall();
          }

          @Override
          public void onError(Throwable e)
          {
            handleError(_callCompletion, e);
          }
        };
        entityStream.addObserver(observer);
      }

      _wrappedCallback.onResponse(response);
    }
  }

  private void handleError(CallCompletion callCompletion, Throwable throwable)
  {
    if (isServerError(throwable))
    {
      callCompletion.endCallWithError(ErrorType.SERVER_ERROR);
    }
    else if (throwable instanceof RemoteInvocationException)
    {
      Throwable originalThrowable = LoadBalancerUtil.findOriginalThrowable(throwable);
      if (originalThrowable instanceof ConnectException)
      {
        callCompletion.endCallWithError(ErrorType.CONNECT_EXCEPTION);
      }
      else if (originalThrowable instanceof ClosedChannelException)
      {
        callCompletion.endCallWithError(ErrorType.CLOSED_CHANNEL_EXCEPTION);
      }
      else if (originalThrowable instanceof TimeoutException)
      {
        callCompletion.endCallWithError(ErrorType.TIMEOUT_EXCEPTION);
      }
      else
      {
        callCompletion.endCallWithError(ErrorType.REMOTE_INVOCATION_EXCEPTION);
      }
    }
    else
    {
      callCompletion.endCallWithError();
    }
  }

  /**
   * Returns true if the given throwable indicates a server-side error.
   */
  private boolean isServerError(Throwable throwable)
  {
    if (throwable instanceof RestException)
    {
      RestException restException = (RestException) throwable;
      if (restException.getResponse() != null)
      {
        return _isErrorStatus.test(restException.getResponse().getStatus());
      }
    }
    else if (throwable instanceof StreamException)
    {
      StreamException streamException = (StreamException) throwable;
      if (streamException.getResponse() != null)
      {
        return _isErrorStatus.test(streamException.getResponse().getStatus());
      }
    }
    return false;
  }
}
