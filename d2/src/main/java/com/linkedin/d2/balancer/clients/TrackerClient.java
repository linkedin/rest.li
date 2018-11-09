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
import com.linkedin.d2.balancer.LoadBalancerClient;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.PropertyKeys;
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
import com.linkedin.util.clock.SystemClock;
import com.linkedin.util.degrader.CallCompletion;
import com.linkedin.util.degrader.CallTracker;
import com.linkedin.util.degrader.CallTrackerImpl;
import com.linkedin.util.degrader.Degrader;
import com.linkedin.util.degrader.DegraderControl;
import com.linkedin.util.degrader.DegraderImpl;
import com.linkedin.util.degrader.DegraderImpl.Config;
import com.linkedin.util.degrader.ErrorType;

import java.net.ConnectException;
import java.net.URI;
import java.nio.channels.ClosedChannelException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.d2.discovery.util.LogUtil.debug;

// TODO if we ever want to get rid of ties to linkedin-specific code, we'll need to move/redo call tracker/call completion/degrader

public class TrackerClient implements LoadBalancerClient
{
  public static final String DEFAULT_ERROR_STATUS_REGEX = "(5..)";
  public static final Pattern DEFAULT_ERROR_STATUS_PATTERN = Pattern.compile(DEFAULT_ERROR_STATUS_REGEX);

  private static final Logger      _log = LoggerFactory.getLogger(TrackerClient.class);

  private final TransportClient _wrappedClient;
  // The keys for the maps are partitionIds
  private final Map<Integer, PartitionState> _partitionStates;
  private final CallTracker     _callTracker;
  private final URI             _uri;
  private final Pattern         _errorStatusPattern;

  public TrackerClient(URI uri, Map<Integer, PartitionData> partitionDataMap, TransportClient wrappedClient)
  {
    this(uri, partitionDataMap, wrappedClient, SystemClock.instance(), null,
        DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS, DEFAULT_ERROR_STATUS_REGEX);
  }

  public TrackerClient(URI uri, Map<Integer, PartitionData> partitionDataMap, TransportClient wrappedClient,
                       Clock clock, Config config)
  {
    this(uri, partitionDataMap, wrappedClient, clock, config,
        DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS, DEFAULT_ERROR_STATUS_REGEX);
  }

  public TrackerClient(URI uri, Map<Integer, PartitionData> partitionDataMap, TransportClient wrappedClient,
      Clock clock, Config config, long interval, String errorStatusRegex)
  {
    this(uri, partitionDataMap, wrappedClient, clock, config, interval, errorStatusRegex, null);
  }

  public TrackerClient(URI uri, Map<Integer, PartitionData> partitionDataMap, TransportClient wrappedClient,
                       Clock clock, Config config, long interval, String errorStatusRegex,
                       Map<String, Object> uriSpecificProperties)
  {
    _uri = uri;
    _wrappedClient = wrappedClient;
    _callTracker = new CallTrackerImpl(interval, clock);
    Pattern errorPattern;
    try
    {
      errorPattern = Pattern.compile(errorStatusRegex != null ? errorStatusRegex : DEFAULT_ERROR_STATUS_REGEX);
    }
    catch (PatternSyntaxException ex)
    {
      _log.warn("Invalid error status regex: {}. Falling back to default regex: {}", errorStatusRegex, DEFAULT_ERROR_STATUS_REGEX);
      errorPattern = DEFAULT_ERROR_STATUS_PATTERN;
    }
    _errorStatusPattern = errorPattern;

    if (config == null)
    {
      config = new Config();
    }

    config.setCallTracker(_callTracker);
    config.setClock(clock);
    // The overrideDropRate will be globally determined by the DegraderLoadBalancerStrategy.
    config.setOverrideDropRate(0.0);

    if (uriSpecificProperties == null)
    {
      uriSpecificProperties = new HashMap<>();
    }
    if (uriSpecificProperties.containsKey(PropertyKeys.DO_NOT_SLOW_START)
        && Boolean.parseBoolean(uriSpecificProperties.get(PropertyKeys.DO_NOT_SLOW_START).toString()))
    {
      config.setInitialDropRate(DegraderImpl.DEFAULT_DO_NOT_SLOW_START_INITIAL_DROP_RATE);
    }

      /* TrackerClient contains state for each partition, but they actually share the same DegraderImpl
       *
       * There used to be a deadlock if each partition has its own DegraderImpl:
       * getStats() and rolloverStats() in DegraderImpl are both synchronized. getstats() will check whether
       * the state is stale, and if yes a rollover event will be delivered which will call rolloverStats() in all
       * DegraderImpl within this CallTracker. Therefore, when multiple threads are calling getStats() simultaneously,
       * one thread may try to grab a lock which is already acquired by another.
       *
       * An example:
       * Suppose we have two threads, and here is the execution sequence:
       * 1. Thread 1 (DegraderImpl 1): grab its lock, enter getStats()
       * 2. Thread 2 (DegraderImpl 2): grab its lock, enter getStats()
       * 3. Thread 1: PendingEvent is delivered to all registered StatsRolloverEventListener, so it will call rolloverStats()
       *    in both DegraderImpl 1 and DegraderImpl 2. But the lock of DegraderImpl 2 has already been acquired by thread 2
       * 4. Same happens for thread 2. Deadlock.
       *
       * Solution:
       * Currently all DegraderImpl within the same CallTracker actually share exactly the same information,
       * so we just use create one instance of DegraderImpl, and use it for all partitions.
       *
       * Pros and Cons:
       * Deadlocks will be gone since there will be only one DegraderImpl.
       * However, now it becomes harder to have different configurations for different partitions.
       */
    int mapSize = partitionDataMap.size();
    Map<Integer, PartitionState>partitionStates = new HashMap<Integer, PartitionState>(mapSize * 2);
    config.setName("TrackerClient Degrader: " + uri);
    DegraderImpl degrader = new DegraderImpl(config);
    DegraderControl degraderControl = new DegraderControl(degrader);
    for (Map.Entry<Integer, PartitionData> entry : partitionDataMap.entrySet())
    {
      int partitionId = entry.getKey();
      PartitionState partitionState = new PartitionState(entry.getValue(), degrader, degraderControl);
      partitionStates.put(partitionId, partitionState);
    }
    _partitionStates = Collections.unmodifiableMap(partitionStates);
    debug(_log, "created tracker client: ", this);
  }

  @Override
  public void restRequest(RestRequest request,
                          RequestContext requestContext,
                          Map<String, String> wireAttrs,
                          TransportCallback<RestResponse> callback)
  {
    _wrappedClient.restRequest(request, requestContext, wireAttrs, new TrackerClientRestCallback(callback, _callTracker.startCall()));
  }

  @Override
  public void streamRequest(StreamRequest request,
                            RequestContext requestContext,
                            Map<String, String> wireAttrs,
                            TransportCallback<StreamResponse> callback)
  {
    _wrappedClient.streamRequest(request, requestContext, wireAttrs, new TrackerClientStreamCallback(callback, _callTracker.startCall()));
  }

  @Override
  public void shutdown(Callback<None> callback)
  {
    _wrappedClient.shutdown(callback);
  }

  public Double getPartitionWeight(int partitionId)
  {
    PartitionData partitionData = getPartitionState(partitionId).getPartitionData();

    return partitionData == null ? null : partitionData.getWeight();
  }

  public TransportClient getWrappedClient()
  {
    return _wrappedClient;
  }

  public CallTracker getCallTracker()
  {
    return _callTracker;
  }

  public Degrader getDegrader(int partitionId)
  {
    return getPartitionState(partitionId).getDegrader();
  }

  public DegraderControl getDegraderControl(int partitionId)
  {

    return getPartitionState(partitionId).getDegraderControl();
  }

  public Map<Integer, PartitionData> getParttitionDataMap()
  {
    Map<Integer, PartitionData> partitionDataMap = new HashMap<Integer, PartitionData>();
    for (Map.Entry<Integer, PartitionState> entry : _partitionStates.entrySet())
    {
      partitionDataMap.put(entry.getKey(), entry.getValue().getPartitionData());
    }
    return partitionDataMap;
  }

  private PartitionState getPartitionState(int partitionId)
  {
    PartitionState partitionState = _partitionStates.get(partitionId);
    if (partitionState == null)
    {
      String msg = "PartitionState does not exist for partitionId: " + partitionId + ". The current states are " + _partitionStates;
      throw new IllegalStateException(msg);
    }
    return partitionState;
  }

  @Override
  public URI getUri()
  {
    return _uri;
  }

  @Override
  public String toString()
  {
    return "TrackerClient [_uri=" + _uri + ", _partitionStates=" + _partitionStates + "]";
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
        return matchErrorStatus(restException.getResponse().getStatus());
      }
    }
    else if (throwable instanceof StreamException)
    {
      StreamException streamException = (StreamException) throwable;
      if (streamException.getResponse() != null)
      {
        return matchErrorStatus(streamException.getResponse().getStatus());
      }
    }
    // default to false
    return false;
  }

  private boolean matchErrorStatus(int status) {
    return _errorStatusPattern.matcher(Integer.toString(status)).matches();
  }

  // we organize all data of a partition together so we don't have to maintain multiple maps in tracker client
  private class PartitionState
  {
    private final Degrader _degrader;
    private final DegraderControl _degraderControl;
    private final PartitionData _partitionData;

    PartitionState(PartitionData partitionData, Degrader degrader, DegraderControl degraderControl)
    {
      _partitionData = partitionData;
      _degrader = degrader;
      _degraderControl = degraderControl;
    }

    Degrader getDegrader()
    {
      return _degrader;
    }

    DegraderControl getDegraderControl()
    {
      return _degraderControl;
    }

    PartitionData getPartitionData()
    {
      return _partitionData;
    }

    @Override
    public String toString()
    {
      StringBuilder sb = new StringBuilder();
      sb.append("{_partitionData = ");
      sb.append(_partitionData);
      sb.append(", _degrader = " + _degrader);
      sb.append(", degraderMinCallCount = " + _degraderControl.getMinCallCount());
      sb.append("}");
      return sb.toString();
    }
  }
}
