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
package com.linkedin.d2.balancer.clients;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.backuprequests.BackupRequestsStrategyFromConfig;
import com.linkedin.d2.backuprequests.BackupRequestsStrategyStatsConsumer;
import com.linkedin.d2.backuprequests.BackupRequestsStrategyStatsProvider;
import com.linkedin.d2.backuprequests.TrackingBackupRequestsStrategy;
import com.linkedin.d2.balancer.D2Client;
import com.linkedin.d2.balancer.D2ClientConfig;
import com.linkedin.d2.balancer.D2ClientDelegator;
import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.LoadBalancer;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy.ExcludedHostHints;
import com.linkedin.d2.balancer.util.LoadBalancerUtil;
import com.linkedin.data.ByteString;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.FullEntityObserver;
import com.linkedin.r2.util.NamedThreadFactory;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.HdrHistogram.AbstractHistogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * {@link DynamicClient} with backup requests feature.
 *
 * Only instantiated when backupRequestsEnabled in {@link D2ClientConfig} is set to true.
 *
 * @author Jaroslaw Odzga (jodzga@linkedin.com)
 */
public class BackupRequestsClient extends D2ClientDelegator
{
  private static final Logger LOG = LoggerFactory.getLogger(BackupRequestsClient.class);

  public static final String BACKUP_REQUEST_ATTRIBUTE_NAME = "BackupRequest";

  private final LoadBalancer _loadBalancer;
  private final ScheduledExecutorService _executorService;
  private final ScheduledThreadPoolExecutor _latenciesNotifierExecutor;
  private final ScheduledFuture<?> _latenciesNotifier;
  private final boolean _isD2Async;

  // serviceName -> operation -> BackupRequestsStrategyFromConfig
  private final Map<String, Map<String, BackupRequestsStrategyFromConfig>> _strategies = new ConcurrentHashMap<>();
  private final Optional<BackupRequestsStrategyStatsConsumer> _statsConsumer;

  /*
   * When strategy get's removed for any reason there still might be in flight
   * requests made using that strategy. We want to capture latencies for those
   * requests. Eventually (whenever the latencies notifier get's called) we want to
   * notify stats consumer about those latencies. In order to do that we store
   * information about removed strategies until next latencies notification.
   * Notice that this is best effort - there is still a small chance that some
   * latencies are not recorded.
   */
  private final Map<FinalSweepLatencyNotification, FinalSweepLatencyNotification> _finalSweepLatencyNotification =
      new ConcurrentHashMap<>();

  // serviceName -> service config
  private final Map<String, List<Map<String, Object>>> _configs = new ConcurrentHashMap<>();

  public BackupRequestsClient(D2Client d2Client, LoadBalancer loadBalancer, ScheduledExecutorService executorService,
      BackupRequestsStrategyStatsConsumer statsConsumer, long notifyLatencyInterval, TimeUnit notifyLatencyIntervalUnit,
      boolean isD2Async)
  {
    super(d2Client);
    _loadBalancer = loadBalancer;
    _executorService = executorService;
    _statsConsumer = Optional.ofNullable(statsConsumer).map(BackupRequestsClient::toSafeConsumer);
    _latenciesNotifierExecutor =
        new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("backup-requests-latencies-notifier"));
    _latenciesNotifierExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
    _latenciesNotifierExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    _latenciesNotifierExecutor.setRemoveOnCancelPolicy(true);
    _latenciesNotifier = _latenciesNotifierExecutor.scheduleAtFixedRate(this::notifyLatencies, notifyLatencyInterval,
        notifyLatencyInterval, notifyLatencyIntervalUnit);
    _isD2Async = isD2Async;
  }

  private void notifyLatencies()
  {
    try
    {
      _strategies.forEach((serviceName, strategiesForOperations) -> strategiesForOperations
          .forEach((operation, strategy) -> strategy.getStrategy().ifPresent(st ->
          {
            notifyLatency(serviceName, operation, st);
            // We want to notify just once, so if entry is in both _strategies and _finalSweepLatencyNotification
            // we remove it from the _finalSweepLatencyNotification.
            _finalSweepLatencyNotification.remove(new FinalSweepLatencyNotification(serviceName, operation, st));
          })));
      _finalSweepLatencyNotification.forEach((key, value) ->
      {
        notifyLatency(key.getServiceName(), key.getOperation(), key.getStrategy());
        _finalSweepLatencyNotification.remove(key, value);
      });
    } catch (Throwable t)
    {
      LOG.error("Failed to notify latencies", t);
    }
  }

  private void notifyLatency(String serviceName, String operation, TrackingBackupRequestsStrategy strategy)
  {
    strategy.getLatencyWithoutBackup().harvest(histogram -> notifyLatency(serviceName, operation, histogram, false));
    strategy.getLatencyWithBackup().harvest(histogram -> notifyLatency(serviceName, operation, histogram, true));
  }

  private void notifyLatency(String serviceName, String operation, AbstractHistogram histogram, boolean withBackup)
  {
    _statsConsumer.ifPresent(consumer -> consumer.latencyUpdate(serviceName, operation, histogram, withBackup));
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
  public void restRequest(final RestRequest request, final RequestContext requestContext,
      final Callback<RestResponse> callback)
  {
    if (_isD2Async)
    {
      requestAsync(request, requestContext, _d2Client::restRequest, callback);
      return;
    }

    _d2Client.restRequest(request, requestContext,
        decorateCallbackSync(request, requestContext, _d2Client::restRequest, callback));
  }

  /*private*/ Optional<TrackingBackupRequestsStrategy> getStrategyAfterUpdate(final String serviceName, final String operation)
  {
    Map<String, BackupRequestsStrategyFromConfig> strategiesForOperation = _strategies.get(serviceName);
    if (strategiesForOperation != null)
    {
      BackupRequestsStrategyFromConfig backupRequestsStrategyFromConfig = strategiesForOperation.get(operation);
      if (backupRequestsStrategyFromConfig != null)
      {
        return backupRequestsStrategyFromConfig.getStrategy();
      }
    }
    LOG.debug("No backup requests strategy found");
    return Optional.empty();
  }

  private void updateServiceProperties(String serviceName, ServiceProperties serviceProperties)
  {
    List<Map<String, Object>> existing = _configs.get(serviceName);
    if (serviceProperties != null)
    {
      if (existing != serviceProperties.getBackupRequests())
      { // reference inequality check
        update(serviceName, serviceProperties.getBackupRequests());
        _configs.put(serviceName, serviceProperties.getBackupRequests());
      }
    }
  }

  /**
   * Send rest request with backup request support asynchronously
   * This method will make the D2 property call in async manner
   */
  private <R extends Request, T> void requestAsync(final R request, final RequestContext requestContext,
      DecoratorClient<R, T> client, final Callback<T> callback)
  {
    final String serviceName = LoadBalancerUtil.getServiceNameFromUri(request.getURI());
    final Object operationObject = requestContext.getLocalAttr(R2Constants.OPERATION);
    if (operationObject == null) {
      client.doRequest(request, requestContext, callback);
      return;
    }
    final String operation = operationObject.toString();
    Callback<Optional<TrackingBackupRequestsStrategy>> maybeStrategyCallback = new Callback<Optional<TrackingBackupRequestsStrategy>>() {
      @Override
      public void onError(Throwable e) {
        LOG.error("Error attempting to use backup requests, falling back to request without a backup", e);
        client.doRequest(request, requestContext, callback);
      }

      @Override
      public void onSuccess(Optional<TrackingBackupRequestsStrategy> maybeStrategy) {
        if (maybeStrategy.isPresent())
        {
          Callback<T> decoratedCallback =
              decorateCallbackWithBackupRequest(request, requestContext, client, callback, maybeStrategy.get(), serviceName, operation);
          client.doRequest(request, requestContext, decoratedCallback);
        }
        else
        {
          client.doRequest(request, requestContext, callback);
        }
      }
    };

    getStrategyAsync(serviceName, operation, maybeStrategyCallback);
  }

  /**
   * Get backup request strategy after the D2 Zookeeper blocking call finishes
   * TODO: Remove this blocking call once the async path has been verified
   */
  private Optional<TrackingBackupRequestsStrategy> getStrategySync(final String serviceName, final String operation)
  {
    try
    {
      ServiceProperties serviceProperties = _loadBalancer.getLoadBalancedServiceProperties(serviceName);
      updateServiceProperties(serviceName, serviceProperties);
    }
    catch (ServiceUnavailableException e)
    {
      LOG.debug("Failed to fetch backup requests strategy ", e);
    }

    return getStrategyAfterUpdate(serviceName, operation);
  }

  void getStrategyAsync(final String serviceName, final String operation, Callback<Optional<TrackingBackupRequestsStrategy>> callback) {
  Callback<ServiceProperties> servicePropertiesCallback = new Callback<ServiceProperties>() {
    @Override
    public void onError(Throwable e) {
      LOG.debug("Failed to fetch backup requests strategy", e);
      // Continue the call even if properties are not updated
      callback.onSuccess(Optional.empty());
    }

    @Override
    public void onSuccess(ServiceProperties serviceProperties) {
      updateServiceProperties(serviceName, serviceProperties);
      Optional<TrackingBackupRequestsStrategy> maybeStrategy = getStrategyAfterUpdate(serviceName, operation);
      callback.onSuccess(maybeStrategy);
    }
  };

  _loadBalancer.getLoadBalancedServiceProperties(serviceName, servicePropertiesCallback);
}

  /*
   * List<Map<String, Object>> backupRequestsConfigs is coming from
   * service properties, field backupRequests, see D2Service.pdsc
   */
  private void update(String serviceName, List<Map<String, Object>> backupRequestsConfigs)
  {
    Map<String, BackupRequestsStrategyFromConfig> strategiesForOperation =
        getOrCreateStrategiesForOperation(serviceName);

    Set<String> operationsInNewConfig = backupRequestsConfigs.stream()
        .map(config -> updateStrategy(serviceName, config, strategiesForOperation)).collect(Collectors.toSet());

    Set<Map.Entry<String, BackupRequestsStrategyFromConfig>> toRemove = strategiesForOperation.entrySet().stream()
        .filter(entry -> !operationsInNewConfig.contains(entry.getKey())).collect(Collectors.toSet());

    toRemove.forEach(entry -> entry.getValue().getStrategy().ifPresent(strategy ->
    {
      String operation = entry.getKey();
      strategiesForOperation.remove(operation);
      _statsConsumer.ifPresent(consumer -> consumer.removeStatsProvider(serviceName, operation, strategy));
      //make sure latencies for all outstanding requests get recorded
      FinalSweepLatencyNotification fsln = new FinalSweepLatencyNotification(serviceName, operation, strategy);
      _finalSweepLatencyNotification.put(fsln, fsln);
    }));
  }

  private Map<String, BackupRequestsStrategyFromConfig> getOrCreateStrategiesForOperation(String serviceName)
  {
    Map<String, BackupRequestsStrategyFromConfig> strategiesForOperation = _strategies.get(serviceName);
    if (strategiesForOperation == null)
    {
      strategiesForOperation = new ConcurrentHashMap<>();
      Map<String, BackupRequestsStrategyFromConfig> existing =
          _strategies.putIfAbsent(serviceName, strategiesForOperation);
      if (existing != null)
      {
        strategiesForOperation = existing;
      }
    }
    return strategiesForOperation;
  }

  private String updateStrategy(String serviceName, Map<String, Object> config,
      Map<String, BackupRequestsStrategyFromConfig> strategiesForOperation)
  {
    String operation = (String)config.get(PropertyKeys.OPERATION);
    strategiesForOperation.compute(operation,
        (op, existing) -> updateBackupRequestsStrategyFromConfig(serviceName, operation, existing, config));
    return operation;
  }

  private BackupRequestsStrategyFromConfig updateBackupRequestsStrategyFromConfig(String serviceName, String operation,
      BackupRequestsStrategyFromConfig existing, Map<String, Object> config)
  {
    if (existing == null)
    {
      BackupRequestsStrategyFromConfig newOne = new BackupRequestsStrategyFromConfig(config);
      newOne.getStrategy().ifPresent(statsProvider -> _statsConsumer
          .ifPresent(consumer -> consumer.addStatsProvider(serviceName, operation, statsProvider)));
      return newOne;
    } else
    {
      BackupRequestsStrategyFromConfig newOne = existing.update(config);
      if (newOne != existing)
      { //reference inequality
        _statsConsumer.ifPresent(consumer ->
        {
          existing.getStrategy().ifPresent(statsProvider ->
          {
            consumer.removeStatsProvider(serviceName, operation, statsProvider);
            // Make sure latencies for all outstanding requests get recorded
            FinalSweepLatencyNotification fsln =
                new FinalSweepLatencyNotification(serviceName, operation, statsProvider);
            _finalSweepLatencyNotification.put(fsln, fsln);
          });
          newOne.getStrategy()
              .ifPresent(statsProvider -> consumer.addStatsProvider(serviceName, operation, statsProvider));
        });
      }
      return newOne;
    }
  }

  @Override
  public void streamRequest(StreamRequest request, Callback<StreamResponse> callback)
  {
    streamRequest(request, new RequestContext(), callback);
  }

  @Override
  public void streamRequest(StreamRequest request, RequestContext requestContext, Callback<StreamResponse> callback)
  {
    // Buffering stream request raises concerns on memory usage and performance.
    // Currently only support backup requests with IS_FULL_REQUEST.
    if (!isFullRequest(requestContext)) {
      _d2Client.streamRequest(request, requestContext, callback);
      return;
    }
    if (!isBuffered(requestContext)) {
      final FullEntityObserver observer = new FullEntityObserver(new Callback<ByteString>()
      {
        @Override
        public void onError(Throwable e)
        {
          LOG.warn("Failed to record request's entity for retrying backup request.");
        }

        @Override
        public void onSuccess(ByteString result)
        {
          requestContext.putLocalAttr(R2Constants.BACKUP_REQUEST_BUFFERED_BODY, result);
        }
      });
      request.getEntityStream().addObserver(observer);
    }
    if (_isD2Async)
    {
      requestAsync(request, requestContext, _d2Client::streamRequest, callback);
      return;
    }

    _d2Client.streamRequest(request, requestContext,
        decorateCallbackSync(request, requestContext, _d2Client::streamRequest, callback));
  }

  private <R extends Request, T> Callback<T> decorateCallbackSync(R request, RequestContext requestContext,
      DecoratorClient<R, T> client, Callback<T> callback)
  {
    try
    {
      final String serviceName = LoadBalancerUtil.getServiceNameFromUri(request.getURI());
      final Object operationObject = requestContext.getLocalAttr(R2Constants.OPERATION);
      if (operationObject != null)
      {
        final String operation = operationObject.toString();
        final Optional<TrackingBackupRequestsStrategy> strategy = getStrategySync(serviceName, operation);
        if (strategy.isPresent())
        {
          return decorateCallbackWithBackupRequest(request, requestContext, client, callback, strategy.get(), serviceName, operation);
        }
        else
        {
          // return original callback and don't send backup request if there is no backup requests strategy
          // defined for this request
          return callback;
        }
      }
      else
      {
        // return original callback and don't send backup request if there is no operation declared in
        // request context
        return callback;
      }
    } catch (Throwable t)
    {
      LOG.error("Error attempting to use backup requests, falling back to request without a backup", t);
      return callback;
    }
  }

  private <R extends Request, T> Callback<T> decorateCallbackWithBackupRequest(R request, RequestContext requestContext,
      DecoratorClient<R, T> client, Callback<T> callback, TrackingBackupRequestsStrategy strategy, String serviceName, String operation)
  {
    final long startNano = System.nanoTime();

    URI targetHostUri = KeyMapper.TargetHostHints.getRequestContextTargetHost(requestContext);
    Boolean backupRequestAcceptable = KeyMapper.TargetHostHints.getRequestContextOtherHostAcceptable(requestContext);
    if (targetHostUri == null || (backupRequestAcceptable != null && backupRequestAcceptable))
    {
      Optional<Long> delayNano = strategy.getTimeUntilBackupRequestNano();
      if (delayNano.isPresent())
      {
        return new DecoratedCallback<>(request, requestContext, client, callback, strategy, delayNano.get(),
            _executorService, startNano, serviceName, operation);
      }
    }
    // return callback that updates backup strategy about latency if
    // 1. caller specified concrete target host but didn't set the flag to accept other hosts
    // 2. backup strategy is not ready yet
    return new Callback<T>()
    {
      @Override
      public void onSuccess(T result)
      {
        recordLatency();
        callback.onSuccess(result);
      }

      private void recordLatency()
      {
        long latency = System.nanoTime() - startNano;
        strategy.recordCompletion(latency);
        strategy.getLatencyWithoutBackup().record(latency,
            histogram -> notifyLatency(serviceName, operation, histogram, false));
        strategy.getLatencyWithBackup().record(latency,
            histogram -> notifyLatency(serviceName, operation, histogram, true));
      }

      @Override
      public void onError(Throwable e)
      {
        // disregard latency if request was not made
        if (!(e instanceof ServiceUnavailableException))
        {
          recordLatency();
        }
        callback.onError(e);
      }
    };
  }

  @Override
  public void shutdown(Callback<None> callback)
  {
    _latenciesNotifier.cancel(false);
    _latenciesNotifierExecutor.shutdown();
    _d2Client.shutdown(callback);
  }

  @FunctionalInterface
  private interface DecoratorClient<R, T>
  {
    void doRequest(R request, RequestContext requestContext, Callback<T> callback);
  }

  //Decorated callback that is used when backup requests are enabled
  private class DecoratedCallback<R extends Request, T> implements Callback<T>
  {

    private final AtomicBoolean _done = new AtomicBoolean(false);
    private final R _request;
    private final RequestContext _requestContext;
    private final RequestContext _backupRequestContext;
    private final DecoratorClient<R, T> _client;
    private final Callback<T> _callback;
    private final TrackingBackupRequestsStrategy _strategy;
    private final long _startNano;
    private final String _serviceName;
    private final String _operation;

    public DecoratedCallback(R request, RequestContext requestContext, DecoratorClient<R, T> client,
        Callback<T> callback, TrackingBackupRequestsStrategy strategy, long delayNano,
        ScheduledExecutorService executorService, long startNano, String serviceName, String operation)
    {
      _startNano = startNano;
      _request = request;
      _requestContext = requestContext;
      _backupRequestContext = requestContext.clone();
      _backupRequestContext.putLocalAttr(BACKUP_REQUEST_ATTRIBUTE_NAME, delayNano);
      // when making backup request, we would never want to go to the affinity routing host; we would have gone there in the primary request.
      KeyMapper.TargetHostHints.removeRequestContextTargetHost(_backupRequestContext);
      _client = client;
      _callback = callback;
      _strategy = strategy;
      _serviceName = serviceName;
      _operation = operation;
      executorService.schedule(this::maybeSendBackupRequest, delayNano, TimeUnit.NANOSECONDS);
    }

    @SuppressWarnings("unchecked")
    private void maybeSendBackupRequest()
    {
      Set<URI> exclusionSet = ExcludedHostHints.getRequestContextExcludedHosts(_requestContext);
      // exclusionSet should have been set by original request but it might be null e.g. if original
      // request has not been made yet
      if (exclusionSet != null)
      {
        exclusionSet.forEach(uri -> ExcludedHostHints.addRequestContextExcludedHost(_backupRequestContext, uri));
        if (_request instanceof StreamRequest && !isBuffered(_requestContext)) {
          return;
        }
        if (!_done.get() && _strategy.isBackupRequestAllowed())
        {
          boolean needCloneRC = false;
          R request = _request;
          if (_request instanceof StreamRequest) {
            StreamRequest req = (StreamRequest)_request;
            req = req.builder()
                .build(EntityStreams.newEntityStream(new ByteStringWriter(
                    (ByteString) _requestContext.getLocalAttr(R2Constants.BACKUP_REQUEST_BUFFERED_BODY)
                )));
            request = (R)req;
            needCloneRC = true;
          }
          _client.doRequest(request, needCloneRC ? _requestContext.clone(): _requestContext, new Callback<T>()
          {
            @Override
            public void onSuccess(T result)
            {
              if (_done.compareAndSet(false, true))
              {
                completeBackup();
                _callback.onSuccess(result);
              }
            }

            @Override
            public void onError(Throwable e)
            {
              // We don't fast fail if backup request failed because downstream is not available
              // because the original request might have been made successfully
              if (!(e instanceof ServiceUnavailableException) && _done.compareAndSet(false, true))
              {
                completeBackup();
                _callback.onError(e);
              }
            }

            private void completeBackup()
            {
              _strategy.backupRequestSuccess();
              _strategy.getLatencyWithBackup().record(System.nanoTime() - _startNano,
                  histogram -> notifyLatency(_serviceName, _operation, histogram, true));
            }
          });
        }
      }
    }

    @Override
    public void onSuccess(T result)
    {
      trackingCompletion(() -> _callback.onSuccess(result));
    }

    /*
     * This method guarantees that the completion is called only if not called by the backup
     */
    private void trackingCompletion(Runnable completion)
    {
      long latency = System.nanoTime() - _startNano;
      //feed backup request strategy with latency of the original request
      _strategy.recordCompletion(latency);
      if (_done.compareAndSet(false, true))
      {
        //if original request completed before backup then update both latency metrics
        _strategy.getLatencyWithBackup().record(latency,
            histogram -> notifyLatency(_serviceName, _operation, histogram, true));
        _strategy.getLatencyWithoutBackup().record(latency,
            histogram -> notifyLatency(_serviceName, _operation, histogram, false));
        completion.run();
      } else
      {
        /*
         * if backup request was faster then update only metric without backup because
         * the DecoratedCallback already updated the backup latency
         */
        _strategy.getLatencyWithoutBackup().record(latency,
            histogram -> notifyLatency(_serviceName, _operation, histogram, false));
      }
    }

    @Override
    public void onError(Throwable e)
    {
      trackingCompletion(() -> _callback.onError(e));
    }
  }

  private static BackupRequestsStrategyStatsConsumer toSafeConsumer(final BackupRequestsStrategyStatsConsumer consumer)
  {
    return new BackupRequestsStrategyStatsConsumer()
    {

      @Override
      public void removeStatsProvider(String service, String operation,
          BackupRequestsStrategyStatsProvider statsProvider)
      {
        try
        {
          consumer.removeStatsProvider(service, operation, statsProvider);
        } catch (Throwable t)
        {
          LOG.error("Error when calling BackupRequestsStrategyStatsConsumer", t);
        }
      }

      @Override
      public void addStatsProvider(String service, String operation, BackupRequestsStrategyStatsProvider statsProvider)
      {
        try
        {
          consumer.addStatsProvider(service, operation, statsProvider);
        } catch (Throwable t)
        {
          LOG.error("Error when calling BackupRequestsStrategyStatsConsumer", t);
        }
      }

      @Override
      public void latencyUpdate(String service, String operation, AbstractHistogram histogram, boolean withBackup)
      {
        try
        {
          consumer.latencyUpdate(service, operation, histogram, withBackup);
        } catch (Throwable t)
        {
          LOG.error("Error when calling BackupRequestsStrategyStatsConsumer", t);
        }
      }
    };
  }

  /*
   * Data structure used to store strategy with it's stats so that they can be reported upon next notification.
   */
  private static class FinalSweepLatencyNotification
  {
    private final String _serviceName;
    private final String _operation;
    private final TrackingBackupRequestsStrategy _strategy;

    public FinalSweepLatencyNotification(String serviceName, String operation, TrackingBackupRequestsStrategy strategy)
    {
      _serviceName = serviceName;
      _operation = operation;
      _strategy = strategy;
    }

    public String getServiceName()
    {
      return _serviceName;
    }

    public String getOperation()
    {
      return _operation;
    }

    public TrackingBackupRequestsStrategy getStrategy()
    {
      return _strategy;
    }

    @Override
    public int hashCode()
    {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((_operation == null) ? 0 : _operation.hashCode());
      result = prime * result + ((_serviceName == null) ? 0 : _serviceName.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj)
    {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      FinalSweepLatencyNotification other = (FinalSweepLatencyNotification) obj;
      if (_operation == null)
      {
        if (other._operation != null)
          return false;
      } else if (!_operation.equals(other._operation))
        return false;
      if (_serviceName == null)
      {
        if (other._serviceName != null)
          return false;
      } else if (!_serviceName.equals(other._serviceName))
        return false;
      return true;
    }

  }

  private static boolean isFullRequest(RequestContext requestContext)
  {
    Object isFullRequest = requestContext.getLocalAttr(R2Constants.IS_FULL_REQUEST);
    return isFullRequest != null && (Boolean)isFullRequest;
  }

  private static boolean isBuffered(RequestContext requestContext)
  {
    Object bufferedBody = requestContext.getLocalAttr(R2Constants.BACKUP_REQUEST_BUFFERED_BODY);
    return bufferedBody != null;
  }
}
