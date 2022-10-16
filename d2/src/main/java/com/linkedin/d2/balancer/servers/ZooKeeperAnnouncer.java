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

/**
 * $Id: $
 */

package com.linkedin.d2.balancer.servers;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ScheduledExecutorService;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.d2.discovery.event.LogOnlyServiceDiscoveryEventEmitter;
import com.linkedin.d2.discovery.event.ServiceDiscoveryEventEmitter;
import com.linkedin.d2.discovery.event.ServiceDiscoveryEventEmitter.StatusUpdateActionType;
import com.linkedin.d2.discovery.event.D2ServiceDiscoveryEventHelper;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperEphemeralStore;
import com.linkedin.util.ArgumentUtil;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ZooKeeperAnnouncer combines a ZooKeeperServer with a configured "desired state", and
 * allows the server to be brought up/down in that state.  The desired state can also
 * be manipulated, for example to allow for administrative manipulation.
 * @author Steven Ihde
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */

public class ZooKeeperAnnouncer implements D2ServiceDiscoveryEventHelper
{
  public static final boolean DEFAULT_DARK_WARMUP_ENABLED = false;
  public static final int DEFAULT_DARK_WARMUP_DURATION = 0;
  public static final String DEFAULT_DARK_WARMUP_CLUSTER_NAME = null;

  private final ZooKeeperServer _server;
  private static final Logger _log = LoggerFactory.getLogger(ZooKeeperAnnouncer.class);
  private volatile String _cluster;
  private volatile URI _uri;
  /**
   * Ephemeral znode path and its data announced for the regular cluster and uri. It will be used as the tracingId in Service discovery status related tracking events.
   * It's updated ONLY at mark-ups: (including regular mark-up and changing uri data by marking down then marking up again)
   *   1. on mark-up success, the path is set to the created node path, and the data is the node data.
   *   2. on mark-up failure, the path is set to a failure path like "/d2/uris/ClusterA/hostA-FAILURE", and the data is the one that was attempted to save.
   * Mark-downs will NOT clear them, so that we could emit mark down event with the node path and data that was deleted (or failed to delete).
   * Since ZookeeperAnnouncer managed to keep only one mark-up running at a time, there won't be raced updates.
   * NOTE: service discovery active change event has to be emitted AFTER mark-up/down complete because the znode path and data will be set during the mark up/down.
   * (by {@link ZooKeeperEphemeralStore} thru {@link ZooKeeperEphemeralStore.ZookeeperNodePathAndDataCallback}).
   */
  private final AtomicReference<String> _znodePathRef = new AtomicReference<>(); // path of the zookeeper node created for this announcement
  private final AtomicReference<String> _znodeDataRef = new AtomicReference<>(); // data in the zookeeper node created for this announcement

  /**
   * Mark up/down startAt timestamp for the regular cluster, since ZookeeperAnnouncer managed to keep only one mark-up running at a time,
   * there won't be raced update. NOTE that one mark-up could actually have multiple operations inside (like a markDown then a markUp),
   * for tracing we want to count the time spent for the whole process, so we need to mark the start time here instead of in ZookeeperServer.
   */
  private final AtomicLong _markUpStartAtRef = new AtomicLong(Long.MAX_VALUE);
  private final AtomicLong _markDownStartAtRef = new AtomicLong(Long.MAX_VALUE);

  private volatile Map<Integer, PartitionData> _partitionDataMap;
  private volatile Map<String, Object> _uriSpecificProperties;

  private ServiceDiscoveryEventEmitter _eventEmitter;

  /**
   * Field that indicates if the user requested the server to be up or down. If it is requested to be up,
   * it will try to bring up the server again on ZK if the connection goes down, or a new store is set
   */
  private boolean _isUp;

  // Field to indicate if warm up was started. If it is true, it will try to end the warm up
  // by marking down on ZK if the connection goes down
  private boolean _isWarmingUp;

  // Field to indicate whether the mark up operation is being retried after a connection loss
  private boolean _isRetryWarmup;

  private final Deque<Callback<None>> _pendingMarkDown;
  private final Deque<Callback<None>> _pendingMarkUp;

  // Queue to store pending mark down for warm-up cluster
  private final Deque<Callback<None>> _pendingWarmupMarkDown;

  private Runnable _nextOperation;
  private boolean _isRunningMarkUpOrMarkDown;
  private volatile boolean _shuttingDown;

  private volatile boolean _markUpFailed;

  // ScheduledExecutorService to schedule the end of dark warm-up, defaults to null
  private ScheduledExecutorService _executorService;

  // Boolean flag to indicate if dark warm-up is enabled, defaults to false
  private boolean _isDarkWarmupEnabled;

  // String to store the name of the dark warm-up cluster, defaults to null
  private String _warmupClusterName;
  // Similar as _znodePath and _znodeData above but for the warm up cluster.
  private final AtomicReference<String> _warmupClusterZnodePathRef = new AtomicReference<>();
  private final AtomicReference<String> _warmupClusterZnodeDataRef = new AtomicReference<>();

  // Same as the start timestamps for the regular cluster above.
  private final AtomicLong _warmupClusterMarkUpStartAtRef = new AtomicLong(Long.MAX_VALUE);
  private final AtomicLong _warmupClusterMarkDownStartAtRef = new AtomicLong(Long.MAX_VALUE);

  // Field to store the dark warm-up time duration in seconds, defaults to zero
  private int _warmupDuration;

  public ZooKeeperAnnouncer(ZooKeeperServer server)
  {
    this(server, true);
  }

  public ZooKeeperAnnouncer(ZooKeeperServer server, boolean initialIsUp)
  {
    this(server, initialIsUp, DEFAULT_DARK_WARMUP_ENABLED, DEFAULT_DARK_WARMUP_CLUSTER_NAME, DEFAULT_DARK_WARMUP_DURATION, (ScheduledExecutorService) null);
  }

  public ZooKeeperAnnouncer(ZooKeeperServer server, boolean initialIsUp,
      boolean isDarkWarmupEnabled, String warmupClusterName, int warmupDuration, ScheduledExecutorService executorService)
  {
    this(server, initialIsUp, isDarkWarmupEnabled, warmupClusterName, warmupDuration, executorService,
        new LogOnlyServiceDiscoveryEventEmitter()); // default to use log-only event emitter
  }

  public ZooKeeperAnnouncer(ZooKeeperServer server, boolean initialIsUp,
      boolean isDarkWarmupEnabled, String warmupClusterName, int warmupDuration, ScheduledExecutorService executorService, ServiceDiscoveryEventEmitter eventEmitter)
  {
    _server = server;
    // initialIsUp is used for delay mark up. If it's false, there won't be markup when the announcer is started.
    _isUp = initialIsUp;
    _isWarmingUp = false;
    _isRetryWarmup = false;
    _pendingMarkDown = new ArrayDeque<>();
    _pendingMarkUp = new ArrayDeque<>();
    _pendingWarmupMarkDown = new ArrayDeque<>();

    _isDarkWarmupEnabled = isDarkWarmupEnabled;
    _warmupClusterName = warmupClusterName;
    _warmupDuration = warmupDuration;
    _executorService = executorService;
    _eventEmitter = eventEmitter;

    _server.setServiceDiscoveryEventHelper(this);
  }

  /**
   * Start the announcer. Needs to be called whenever there is
   * a new zk session established.
   */
  public synchronized void start(Callback<None> callback)
  {
    if (_isUp)
    {
      markUp(callback);
    }
    else
    {
      callback.onSuccess(None.none());
    }
    // No need to manually markDown since we are getting a brand new session
  }

  public synchronized void shutdown()
  {
    _shuttingDown = true;
  }

  /**
   * Retry last failed markUp or markDown operation if there is any. This method needs
   * to be called whenever the zookeeper connection is lost and then back again(zk session
   * is still valid).
   */
  /* package private */
  synchronized void retry(Callback<None> callback)
  {
    // If we have pending operations failed because of a connection loss,
    // retry the last one.
    // If markDown for warm-up cluster is pending, complete it
    // Since markUp for warm-up cluster is best effort, we do not register its failure and so do not retry it
    if(!_pendingWarmupMarkDown.isEmpty() && _isWarmingUp)
    {
      // complete the markDown on warm-up cluster and start the markUp on regular cluster
      _isRetryWarmup = true;
      markUp(callback);
    }

    // Note that we use _isUp to record the last requested operation, so changing
    // its value should be the first operation done in #markUp and #markDown.
    if (!_pendingMarkDown.isEmpty() || !_pendingMarkUp.isEmpty())
    {
      if (_isUp)
      {
        markUp(callback);
      }
      else
      {
        markDown(callback);
      }
    }
    // No need to retry the successful operation because the ephemeral node
    // will not go away if we were marked up.
  }

  public void reset(final Callback<None> callback)
  {
    markDown(new Callback<None>()
    {
      @Override
      public void onSuccess(None none)
      {
        markUp(callback);
      }

      @Override
      public void onError(Throwable e)
      {
        callback.onError(e);
      }
    });
  }

  public synchronized void markUp(final Callback<None> callback)
  {
    _pendingMarkUp.add(callback);
    _isUp = true;
    runNowOrEnqueue(() -> doMarkUp(callback));
  }

  private synchronized void doMarkUp(Callback<None> callback)
  {
    final Callback<None> markUpCallback = new Callback<None>()
    {
      @Override
      public void onError(Throwable e)
      {
        emitSDStatusActiveUpdateIntentAndWriteEvents(_cluster, true, false, _markUpStartAtRef.get());
        if (e instanceof KeeperException.ConnectionLossException || e instanceof KeeperException.SessionExpiredException)
        {
          _log.warn("failed to mark up uri {} for cluster {} due to {}.", _uri, _cluster, e.getClass().getSimpleName());
          // Setting to null because if that connection dies, when don't want to continue making operations before
          // the connection is up again.
          // When the connection will be up again, the ZKAnnouncer will be restarted and it will read the _isUp
          // value and start markingUp again if necessary
          _nextOperation = null;
          _isRunningMarkUpOrMarkDown = false;

          // A failed state is not relevant here because the connection has also been lost; when it is restored the
          // announcer will retry as expected.
          _markUpFailed = false;
        }
        else
        {
          _log.error("failed to mark up uri {}", _uri, e);
          _markUpFailed = true;
          callback.onError(e);
          runNextMarkUpOrMarkDown();
        }
      }

      @Override
      public void onSuccess(None result)
      {
        emitSDStatusActiveUpdateIntentAndWriteEvents(_cluster, true, true, _markUpStartAtRef.get());
        _markUpFailed = false;
        _log.info("markUp for uri = {} on cluster {} succeeded.", _uri, _cluster);
        // Note that the pending callbacks we see at this point are
        // from the requests that are filed before us because zookeeper
        // guarantees the ordering of callback being invoked.
        synchronized (ZooKeeperAnnouncer.this)
        {
          // drain _pendingMarkUp with successful result.

          // TODO: in case multiple markup are lined up, and after the success of the current markup there could be
          // another markup with a change. We should not want to drain all of the pendingMarkUp because in case of
          // failure of the next markup (which would bare the data changes) with an non-connection related exception,
          // the user will never be notified of the failure.
          // We are currently not aware of such non-connection related exception, but it is a case that could require
          // attention in the future.
          drain(_pendingMarkUp, null);

          if (_isUp)
          {
            // drain _pendingMarkDown with CancellationException.
            drain(_pendingMarkDown, new CancellationException("Cancelled markDown because a more recent markUp request succeeded."));
          }
        }
        runNextMarkUpOrMarkDown();
      }
    };


    final Callback<None> warmupMarkDownCallback = new Callback<None>()
    {
      @Override
      public void onError(Throwable e)
      {
        emitSDStatusActiveUpdateIntentAndWriteEvents(_warmupClusterName, false, false, _warmupClusterMarkDownStartAtRef.get());
        // It is important here to retry the markDown for warm-up cluster.
        // We cannot go ahead to markUp the regular cluster, as the warm-up cluster to uris association has not been deleted
        // from the zookeeper store.
        if (e instanceof KeeperException.ConnectionLossException || e instanceof KeeperException.SessionExpiredException)
        {
          _log.warn("failed to markDown uri {} on warm-up cluster {} due to {}.", _uri, _warmupClusterName, e.getClass().getSimpleName());
          // Setting to null because if that connection dies, we don't want to continue making operations before
          // the connection is up again.
          // When the connection will be up again, the ZKAnnouncer will be restarted and it will read the _isWarmingUp
          // value and mark down warm-up cluster again if necessary
          _nextOperation = null;
          _isRunningMarkUpOrMarkDown = false;
        }
        else
        {
          //continue to mark up to the regular cluster
          _markUpStartAtRef.set(System.currentTimeMillis());
          _server.markUp(_cluster, _uri, _partitionDataMap, _uriSpecificProperties, markUpCallback);
        }
      }

      @Override
      public void onSuccess(None result)
      {
        emitSDStatusActiveUpdateIntentAndWriteEvents(_warmupClusterName, false, true, _warmupClusterMarkDownStartAtRef.get());
        // Mark _isWarmingUp to false to indicate warm up has completed
        _isWarmingUp = false;

        synchronized (ZooKeeperAnnouncer.this)
        {
          // Clear the queue for pending markDown requests for warm-up cluster as the current request has completed
          // and the pending callbacks we see at this point are from the requests that are filed before us because
          // zookeeper guarantees the ordering of callback being invoked.
          _pendingWarmupMarkDown.clear();
        }
        _log.info("markDown for uri {} on warm-up cluster {} has completed, now marking up regular cluster {}", _uri, _warmupClusterName, _cluster);
        _markUpStartAtRef.set(System.currentTimeMillis());
        _server.markUp(_cluster, _uri, _partitionDataMap, _uriSpecificProperties, markUpCallback);
      }
    };


    final Callback<None> doWarmupCallback = new Callback<None>()
    {
      @Override
      public void onError(Throwable e)
      {
        emitSDStatusActiveUpdateIntentAndWriteEvents(_warmupClusterName, true, false, _warmupClusterMarkUpStartAtRef.get());
        if (e instanceof KeeperException.ConnectionLossException || e instanceof KeeperException.SessionExpiredException)
        {
          _log.warn("failed to mark up uri {} for warm-up cluster {} due to {}.", _uri, _cluster, e.getClass().getSimpleName());
          // Setting to null because if that connection dies, we don't want to continue making operations before
          // the connection is up again.
          // When the connection will be up again, the ZKAnnouncer will be restarted and it will read the _isUp
          // value and start markingUp again if necessary
          _nextOperation = null;
          _isRunningMarkUpOrMarkDown = false;

          // A failed state is not relevant here because the connection has also been lost; when it is restored the
          // announcer will retry as expected.
          _markUpFailed = false;
        }
        else
        {
          // Try markUp to regular cluster. We give up on the attempt to warm up in this case.
          _log.warn("failed to mark up uri {} for warm-up cluster {}", _uri, e);
          _markUpStartAtRef.set(System.currentTimeMillis());
          _server.markUp(_cluster, _uri, _partitionDataMap, _uriSpecificProperties, markUpCallback);
        }
      }

      @Override
      public void onSuccess(None result)
      {
        emitSDStatusActiveUpdateIntentAndWriteEvents(_warmupClusterName, true, true, _warmupClusterMarkUpStartAtRef.get());
        _log.info("markUp for uri {} on warm-up cluster {} succeeded", _uri, _warmupClusterName);
        // Mark _isWarmingUp to true to indicate warm up is in progress
        _isWarmingUp = true;
        // Add mark down as pending, so that in case of ZK connection loss, on retry there is a mark down attempted
        // for the warm-up cluster
        _pendingWarmupMarkDown.add(warmupMarkDownCallback);
        // Run warm-up for _warmupDuration seconds and then schedule a mark down for the warm-up cluster
        _log.debug("warm-up will run for {} seconds.", _warmupDuration);
        _executorService.schedule(() -> {
            _warmupClusterMarkDownStartAtRef.set(System.currentTimeMillis());
            _server.markDown(_warmupClusterName, _uri, warmupMarkDownCallback);
          }, _warmupDuration, TimeUnit.SECONDS);
      }
    };

    _log.info("overrideMarkUp is called for uri = " + _uri);
    if (_isRetryWarmup)
    {
      // If the connection with ZooKeeper was lost during warm-up and is re-established after the warm-up duration completed,
      // then complete the pending markDown for the warm-up cluster and announce to the regular cluster
      if (_isWarmingUp)
      {
        _warmupClusterMarkDownStartAtRef.set(System.currentTimeMillis());
        _server.markDown(_warmupClusterName, _uri, warmupMarkDownCallback);
      }
      // Otherwise, if the connection with ZooKeeper was lost during warm-up but was re-established before the warm-up duration completed,
      // then during that request itself the markDown for the warm-up cluster has completed
    }
    else if (_isDarkWarmupEnabled && _warmupDuration > 0 && _warmupClusterName != null && _executorService != null)
    {
      _log.info("Starting dark warm-up with cluster {}", _warmupClusterName);
      _warmupClusterMarkUpStartAtRef.set(System.currentTimeMillis());
      _server.markUp(_warmupClusterName, _uri, _partitionDataMap, _uriSpecificProperties, doWarmupCallback);
    }
    else
    {
      _markUpStartAtRef.set(System.currentTimeMillis());
      _server.markUp(_cluster, _uri, _partitionDataMap, _uriSpecificProperties, markUpCallback);
    }
  }

  public synchronized void markDown(final Callback<None> callback)
  {
    _pendingMarkDown.add(callback);
    _isUp = false;
    runNowOrEnqueue(() -> doMarkDown(callback));
  }

  private synchronized void doMarkDown(Callback<None> callback)
  {
    _markDownStartAtRef.set(System.currentTimeMillis());
    _server.markDown(_cluster, _uri, new Callback<None>()
    {
      @Override
      public void onError(Throwable e)
      {
        emitSDStatusActiveUpdateIntentAndWriteEvents(_cluster, false, false, _markDownStartAtRef.get());
        if (e instanceof KeeperException.ConnectionLossException || e instanceof KeeperException.SessionExpiredException)
        {
          _log.warn("failed to mark down uri {} due to {}.", _uri, e.getClass().getSimpleName());
          _nextOperation = null;
          _isRunningMarkUpOrMarkDown = false;
        }
        else
        {
          callback.onError(e);
          runNextMarkUpOrMarkDown();
        }
      }

      @Override
      public void onSuccess(None result)
      {
        emitSDStatusActiveUpdateIntentAndWriteEvents(_cluster, false, true, _markDownStartAtRef.get());
        _log.info("markDown for uri = {} succeeded.", _uri);
        // Note that the pending callbacks we see at this point are
        // from the requests that are filed before us because zookeeper
        // guarantees the ordering of callback being invoked.
        synchronized (ZooKeeperAnnouncer.this)
        {
          // drain _pendingMarkDown with successful result.
          drain(_pendingMarkDown, null);

          if (!_isUp)
          {
            // drain _pendingMarkUp with CancellationException.
            drain(_pendingMarkUp, new CancellationException("Cancelled markUp because a more recent markDown request succeeded."));
          }
        }
        runNextMarkUpOrMarkDown();
      }
    });
    _log.info("overrideMarkDown is called for uri = " + _uri);
  }

  // ################################## Concurrency Util Section ##################################

  private synchronized void runNowOrEnqueue(Runnable requestedOperation)
  {
    if (_shuttingDown)
    {
      return;
    }
    if (_isRunningMarkUpOrMarkDown)
    {
      // we are still running markup at least once so if weight or other config changed, we are making sure to pick it up
      _nextOperation = requestedOperation;
      return;
    }
    _isRunningMarkUpOrMarkDown = true;
    requestedOperation.run();
  }

  private synchronized void runNextMarkUpOrMarkDown()
  {
    Runnable operation = _nextOperation;
    _nextOperation = null;
    _isRunningMarkUpOrMarkDown = false;
    if (operation != null)
    {
      operation.run();
    }
  }

  private void drain(Deque<Callback<None>> callbacks, @Nullable Throwable t)
  {
    for (; !callbacks.isEmpty(); )
    {
      try
      {
        if (t != null)
        {
          callbacks.poll().onError(t);
        }
        else
        {
          callbacks.poll().onSuccess(None.none());
        }
      }
      catch (Throwable throwable)
      {
        _log.error("Unexpected throwable from markUp/markDown callback.", throwable);
      }
    }
  }

  // ################################## Properties Section ##################################

  public void setStore(ZooKeeperEphemeralStore<UriProperties> store)
  {
    store.setZnodePathAndDataCallback((cluster, path, data) -> {
      if (cluster.equals(_cluster)) {
        _znodePathRef.set(path);
        _znodeDataRef.set(data);
      } else if (cluster.equals(_warmupClusterName)) {
        _warmupClusterZnodePathRef.set(path);
        _warmupClusterZnodeDataRef.set(data);
      } else {
        _log.warn("znode path and data callback is called with unknown cluster: " + cluster + ", node path: " + path + ", and data: " + data);
      }
    });
    _server.setStore(store);
  }

  public synchronized void changeWeight(final Callback<None> callback, boolean doNotSlowStart)
  {
    _server.changeWeight(_cluster, _uri, _partitionDataMap, doNotSlowStart, getOperationCallback(callback, "changeWeight"));
    _log.info("changeWeight called for uri = {}.", _uri);
  }

  public synchronized void setDoNotLoadBalance(final Callback<None> callback, boolean doNotLoadBalance)
  {
    _server.addUriSpecificProperty(_cluster, "setDoNotLoadBalance", _uri, _partitionDataMap, PropertyKeys.DO_NOT_LOAD_BALANCE, doNotLoadBalance, getOperationCallback(callback, "setDoNotLoadBalance"));
    _log.info("setDoNotLoadBalance called for uri = {}.", _uri);
  }

  private Callback<None> getOperationCallback(Callback<None> callback, String operation)
  {
    return new Callback<None>()
    {
      @Override
      public void onError(Throwable e)
      {
        _log.warn(operation + " for uri = {} failed.", _uri);
        callback.onError(e);
      }

      @Override
      public void onSuccess(None result)
      {
        _log.info(operation + " for uri = {} succeeded.", _uri);
        callback.onSuccess(result);
      }
    };
  }

  public String getCluster()
  {
    return _cluster;
  }

  public void setCluster(String cluster)
  {
    _cluster = cluster;
  }

  public String getUri()
  {
    return _uri.toString();
  }

  public void setUri(String uri)
  {
    _uri = URI.create(uri);
  }

  public void setUriSpecificProperties(Map<String, Object> uriSpecificProperties)
  {
    _uriSpecificProperties = Collections.unmodifiableMap(uriSpecificProperties);
  }

  public Map<String, Object> getUriSpecificProperties()
  {
    return (_uriSpecificProperties == null) ? Collections.<String, Object>emptyMap() : _uriSpecificProperties;
  }

  /**
   * This is not the cleanest way of setting weight or partition data. However,
   * this simplifies object create by presenting only one method and by forcing
   * users to set either weight or partition data, but not both.
   *
   * @param data could be either a double or a map of Integer to PartitionData
   */
  public void setWeightOrPartitionData(Object data)
  {
    ArgumentUtil.notNull(data, "weightOrPartitionData");
    if (data instanceof Number)
    {
      setWeight(((Number) data).doubleValue());
    }
    else
    {
      try
      {
        @SuppressWarnings("unchecked")
        Map<Integer, PartitionData> partitionDataMap = (Map<Integer, PartitionData>) data;
        setPartitionData(partitionDataMap);
      }
      catch (ClassCastException e)
      {
        throw new IllegalArgumentException("data: " + data + " is not an instance of Map", e);
      }
    }
  }

  public void setWeight(double weight)
  {
    int numberOfPartitions = getNumberOfPartitions();

    if (numberOfPartitions > 1)
    {
      throw new IllegalArgumentException("When a single announcer is serving multiple partitions, you cannot call "
                                           + "setWeight since it would change the weight for multiple partitions. The partitionData should be changed instead.");
    }

    int partitionId = DefaultPartitionAccessor.DEFAULT_PARTITION_ID;
    if (numberOfPartitions == 1)
    {
      partitionId = getPartitionData().entrySet().iterator().next().getKey();
    }

    Map<Integer, PartitionData> partitionDataMap = new HashMap<>(1);
    partitionDataMap.put(partitionId, new PartitionData(weight));
    _partitionDataMap = Collections.unmodifiableMap(partitionDataMap);
  }

  public void setPartitionData(Map<Integer, PartitionData> partitionData)
  {
    _partitionDataMap = Collections.unmodifiableMap(new HashMap<>(partitionData));
  }

  public Map<Integer, PartitionData> getPartitionData()
  {
    return _partitionDataMap;
  }

  private int getNumberOfPartitions()
  {
    Map<Integer, PartitionData> partitionDataMap = getPartitionData();
    return partitionDataMap == null ? 0 : partitionDataMap.size();
  }

  public boolean isMarkUpFailed()
  {
    return _markUpFailed;
  }

  public void setEventEmitter(ServiceDiscoveryEventEmitter emitter) {
    _eventEmitter = emitter;
  }

  @Override
  public void emitSDStatusActiveUpdateIntentAndWriteEvents(String cluster, boolean isMarkUp, boolean succeeded, long startAt) {
    if (_eventEmitter == null) {
      _log.info("Service discovery event emitter in ZookeeperAnnouncer is null. Skipping emitting events.");
      return;
    }

    if (startAt == Long.MAX_VALUE) {
      _log.warn("Error in startAt timestamp. Skipping emitting events.");
    }

    ImmutablePair<String, String> pathAndData = getZnodePathAndData(cluster);
    if (pathAndData.left == null) {
      _log.warn("Failed to emit SDStatusWriteEvent. Missing znode path and data.");
      return;
    }
    long timeNow = System.currentTimeMillis();
    // D2's mark-down is actually a mark-running action (running but not serving traffic), but since D2 removes hosts
    // in "running" status on ZK, which is a mark-down action, so we use mark-down action in D2.
    StatusUpdateActionType actionType = isMarkUp ? StatusUpdateActionType.MARK_READY : StatusUpdateActionType.MARK_DOWN;
    // NOTE: For D2, tracingId is the same as the ephemeral znode path, and the node data version is always 0 since uri node data is never updated
    // (instead update is done by removing old node and creating a new node).
    _eventEmitter.emitSDStatusActiveUpdateIntentEvent(Collections.singletonList(cluster), actionType, false, pathAndData.left, startAt);
    _eventEmitter.emitSDStatusWriteEvent(cluster, _uri.getHost(), _uri.getPort(), actionType, _server.getConnectString(), pathAndData.left, pathAndData.right,
        succeeded ? 0 : null, pathAndData.left, succeeded, timeNow);
  }

  private ImmutablePair<String, String> getZnodePathAndData(String cluster) {
    String nodePath = null;
    String nodeData = null;
    if (cluster.equals(_cluster)) {
      nodePath = _znodePathRef.get();
      nodeData = _znodeDataRef.get();
    } else if (cluster.equals(_warmupClusterName)) {
      nodePath = _warmupClusterZnodePathRef.get();
      nodeData = _warmupClusterZnodeDataRef.get();
    } else {
      _log.warn("Node path and data can't be found with unknown cluster: " + cluster + ". Ignored.");
    }
    return new ImmutablePair<>(nodePath, nodeData);
  }
}
