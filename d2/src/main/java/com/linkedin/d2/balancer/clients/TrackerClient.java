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
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyConfig;
import com.linkedin.d2.balancer.util.LoadBalancerUtil;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.message.rpc.RpcRequest;
import com.linkedin.r2.message.rpc.RpcResponse;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SystemClock;
import com.linkedin.util.clock.Time;
import com.linkedin.util.degrader.CallCompletion;
import com.linkedin.util.degrader.CallTracker;
import com.linkedin.util.degrader.CallTrackerImpl;
import com.linkedin.util.degrader.Degrader;
import com.linkedin.util.degrader.DegraderControl;
import com.linkedin.util.degrader.DegraderImpl;
import com.linkedin.util.degrader.DegraderImpl.Config;
import com.linkedin.util.degrader.ErrorType;
import java.net.ConnectException;
import java.nio.channels.ClosedChannelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.linkedin.d2.discovery.util.LogUtil.debug;

// TODO if we ever want to get rid of ties to linkedin-specific code, we'll need to move/redo call tracker/call completion/degrader

public class TrackerClient implements LoadBalancerClient
{
  private static final Logger      _log = LoggerFactory.getLogger(TrackerClient.class);

  private final TransportClient _wrappedClient;
  // The keys for the maps are partitionIds
  private final Map<Integer, PartitionState> _partitionStates;
  private final CallTracker     _callTracker;
  private final URI             _uri;

  public TrackerClient(URI uri, Map<Integer, PartitionData> partitionDataMap, TransportClient wrappedClient)
  {
    this(uri, partitionDataMap, wrappedClient, SystemClock.instance(), null,
         DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS);
  }

  public TrackerClient(URI uri, Map<Integer, PartitionData> partitionDataMap, TransportClient wrappedClient,
                       Clock clock, Config config)
    {
      this(uri, partitionDataMap, wrappedClient, clock, config,
           DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS);
    }

  public TrackerClient(URI uri, Map<Integer, PartitionData> partitionDataMap, TransportClient wrappedClient,
                       Clock clock, Config config, long interval)
    {
      _uri = uri;
      _wrappedClient = wrappedClient;
      _callTracker = new CallTrackerImpl(interval, clock);

      if (config == null)
      {
        config = new Config();
      }

      config.setCallTracker(_callTracker);
      config.setClock(clock);
      // The overrideDropRate will be globally determined by the DegraderLoadBalancerStrategy.
      config.setOverrideDropRate(0.0);


      int mapSize = partitionDataMap.size();
      Map<Integer, PartitionState>partitionStates = new HashMap<Integer, PartitionState>(mapSize * 2);
      for (Map.Entry<Integer, PartitionData> entry : partitionDataMap.entrySet())
      {
        int partitionId = entry.getKey();
        config.setName("TrackerClient Degrader: " + uri + ", partitionId: " + partitionId);
        PartitionState partitionState = new PartitionState(entry.getValue(), config);
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
    _wrappedClient.restRequest(request,
                               requestContext,
                               wireAttrs,
                               new TrackerClientCallback<RestResponse>(callback,
                                                                       _callTracker.startCall()));
  }

  @Override
  public void rpcRequest(RpcRequest request,
                         RequestContext requestContext,
                         Map<String, String> wireAttrs,
                         TransportCallback<RpcResponse> callback)
  {
    _wrappedClient.rpcRequest(request,
                              requestContext,
                              wireAttrs,
                              new TrackerClientCallback<RpcResponse>(callback,
                                                                     _callTracker.startCall())
    );
  }

  @Override
  public void shutdown(Callback<None> callback)
  {
    _wrappedClient.shutdown(callback);
  }

  public Double getPartitionWeight(int partitionId)
  {
    // _partitionStates.get(partitionId) would not be null
    PartitionData partitionData = _partitionStates.get(partitionId).getPartitionData();

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
    // _partitionStates.get(partitionId) would not be null
    return _partitionStates.get(partitionId).getDegrader();
  }

  public DegraderControl getDegraderControl(int partitionId)
  {
    // _partitionStates.get(partitionId) would not be null
    return _partitionStates.get(partitionId).getDegraderControl();
  }

  @Override
  public URI getUri()
  {
    return _uri;
  }

  @Override
  public String toString()
  {
    return "TrackerClient [_callTracker=" + _callTracker
        + ", _uri=" + _uri + ", _partitionStates=" + _partitionStates + ", _wrappedClient=" + _wrappedClient + "]";
  }

  public class TrackerClientCallback<T> implements TransportCallback<T>
  {
    private TransportCallback<T> _wrappedCallback;
    private CallCompletion       _callCompletion;

    public TrackerClientCallback(TransportCallback<T> wrappedCallback,
                                 CallCompletion callCompletion)
    {
      _wrappedCallback = wrappedCallback;
      _callCompletion = callCompletion;
    }

    @Override
    public void onResponse(TransportResponse<T> response)
    {
      if (response.hasError())
      {
        Throwable throwable = response.getError();
        if (throwable instanceof RemoteInvocationException)
        {
          Throwable originalThrowable = LoadBalancerUtil.findOriginalThrowable(throwable);
          if (originalThrowable instanceof ConnectException)
          {
            _callCompletion.endCallWithError(ErrorType.CONNECT_EXCEPTION);
          }
          else if (originalThrowable instanceof ClosedChannelException)
          {
            _callCompletion.endCallWithError(ErrorType.CLOSED_CHANNEL_EXCEPTION);
          }
          else
          {
            _callCompletion.endCallWithError(ErrorType.REMOTE_INVOCATION_EXCEPTION);
          }
        }
        else
        {
          _callCompletion.endCallWithError();
        }
      }
      else
      {
        _callCompletion.endCall();
      }

      _wrappedCallback.onResponse(response);
    }
  }

  // we organize all data of a partition together so we don't have to maintain multiple maps in tracker client
  private class PartitionState
  {
    private final Degrader _degrader;
    private final DegraderControl _degraderControl;
    private final PartitionData _partitionData;

    PartitionState(PartitionData partitionData, Config config)
    {
      _partitionData = partitionData;
      DegraderImpl degrader = new DegraderImpl(config);
      DegraderControl degraderControl = new DegraderControl(degrader);
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
      sb.append("_degrader = " + _degrader);
      sb.append("}");
      return sb.toString();
    }
  }
}
