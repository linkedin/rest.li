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

package com.linkedin.d2.balancer.util;

import com.google.common.annotations.VisibleForTesting;
import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.LoadBalancerWithFacilities;
import com.linkedin.d2.balancer.LoadBalancerWithFacilitiesDelegator;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.WarmUpService;
import com.linkedin.d2.balancer.dualread.DualReadModeProvider;
import com.linkedin.d2.balancer.dualread.DualReadStateManager;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.balancer.util.downstreams.DownstreamServicesFetcher;
import com.linkedin.d2.discovery.event.PropertyEventThread;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.http.client.TimeoutCallback;
import com.linkedin.util.clock.SystemClock;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The WarmUpLoadBalancer warms up the internal {@link SimpleLoadBalancer} services/cluster list
 * before the client is announced as "started".
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class WarmUpLoadBalancer extends LoadBalancerWithFacilitiesDelegator {
  private static final Logger LOG = LoggerFactory.getLogger(WarmUpLoadBalancer.class);

  /**
   * Default max of concurrent outstanding warm up requests
   */
  public static final int DEFAULT_CONCURRENT_REQUESTS = 1;
  public static final int DEFAULT_SEND_REQUESTS_TIMEOUT_SECONDS = 60;

  private final ConcurrentLinkedDeque<Future<?>> _outstandingRequests;

  private WarmUpService _serviceWarmupper;
  private final String _d2FsDirPath;
  private final String _d2ServicePath;
  private final int _warmUpTimeoutMillis;
  private final int _concurrentRequests;
  private final ScheduledExecutorService _executorService;
  private final DownstreamServicesFetcher _downstreamServicesFetcher;
  private final DualReadStateManager _dualReadStateManager;
  private final boolean _isIndis; // whether warming up for Indis (false means warming up for ZK)
  private final String _printName; // name of this warmup load balancer based on it's indis or not.
  private volatile boolean _shuttingDown = false;
  private long _allStartTime;
  private List<String> _servicesToWarmUp = null;
  private Supplier<Long> _timeSupplier = () -> SystemClock.instance().currentTimeMillis();

  /**
   * Since the list might from the fetcher might not be complete (new behavior, old data, etc..), and the user might
   * require additional services at runtime, we have to store those services in such a way they are not cleared from the
   * cache at shutdown, otherwise it would incur in a penalty at the next deployment
   */
  private final Set<String> _usedServices;

  public WarmUpLoadBalancer(LoadBalancerWithFacilities balancer, WarmUpService serviceWarmupper,
      ScheduledExecutorService executorService, String d2FsDirPath, String d2ServicePath,
      DownstreamServicesFetcher downstreamServicesFetcher, int warmUpTimeoutSeconds, int concurrentRequests) {
    this(balancer, serviceWarmupper, executorService, d2FsDirPath, d2ServicePath, downstreamServicesFetcher,
        warmUpTimeoutSeconds, concurrentRequests, null, false);
  }

  public WarmUpLoadBalancer(LoadBalancerWithFacilities balancer, WarmUpService serviceWarmupper,
      ScheduledExecutorService executorService, String d2FsDirPath, String d2ServicePath,
      DownstreamServicesFetcher downstreamServicesFetcher, int warmUpTimeoutSeconds, int concurrentRequests,
      DualReadStateManager dualReadStateManager, boolean isIndis) {
    this(balancer, serviceWarmupper, executorService, d2FsDirPath, d2ServicePath, downstreamServicesFetcher,
        warmUpTimeoutSeconds * 1000, concurrentRequests, dualReadStateManager, isIndis, null);
  }

  @VisibleForTesting
  WarmUpLoadBalancer(LoadBalancerWithFacilities balancer, WarmUpService serviceWarmupper,
      ScheduledExecutorService executorService, String d2FsDirPath, String d2ServicePath,
      DownstreamServicesFetcher downstreamServicesFetcher, int warmUpTimeoutMillis, int concurrentRequests,
      DualReadStateManager dualReadStateManager, boolean isIndis, Supplier<Long> timeSupplierForTest)
  {
    super(balancer);
    _serviceWarmupper = serviceWarmupper;
    _executorService = executorService;
    _d2FsDirPath = d2FsDirPath;
    _d2ServicePath = d2ServicePath;
    _downstreamServicesFetcher = downstreamServicesFetcher;
    _warmUpTimeoutMillis = warmUpTimeoutMillis;
    _concurrentRequests = concurrentRequests;
    _outstandingRequests = new ConcurrentLinkedDeque<>();
    _usedServices = new HashSet<>();
    _dualReadStateManager = dualReadStateManager;
    _isIndis = isIndis;
    _printName = String.format("%s WarmUp", _isIndis ? "xDS" : "ZK");
    if (timeSupplierForTest != null)
    {
      _timeSupplier = timeSupplierForTest;
    }
  }

  @Override
  public void start(Callback<None> callback) {
    LOG.info("{} enabled", _printName);

    Callback<None> prepareWarmUpCallback = new Callback<None>() {
      @Override
      public void onError(Throwable e) {
        if (e instanceof TimeoutException)
        {
          LOG.info("{} hit timeout: {}ms. The WarmUp will continue in background", _printName, _warmUpTimeoutMillis);
          callback.onSuccess(None.none());
        }
        else
        {
          LOG.error("{} failed to fetch dual read mode, continuing warmup.", _printName, e);
        }
        continueWarmUp(callback);
      }

      @Override
      public void onSuccess(None result) {
        continueWarmUp(callback);
      }
    };

    _loadBalancer.start(new Callback<None>() {
      @Override
      public void onError(Throwable e) {
        callback.onError(e);
      }

      @Override
      public void onSuccess(None result) {
        _allStartTime = _timeSupplier.get();
        _executorService.submit(() -> prepareWarmUp(prepareWarmUpCallback));
      }
    });
  }

  private void prepareWarmUp(Callback<None> callback)
  {
    // not to be thread-safe, but just to be effectively final to be used in lambdas
    final AtomicBoolean hasTimedOut = new AtomicBoolean(false);

    try {
      _downstreamServicesFetcher.getServiceNames(serviceNames -> {
        // The downstreamServicesFetcher is the core group of the services that will be used during the lifecycle
        _usedServices.addAll(serviceNames);

        LOG.info("{} starting to fetch dual read mode with timeout: {}ms, for {} services: [{}]",
            _printName, _warmUpTimeoutMillis, serviceNames.size(), String.join(", ", serviceNames));

        _servicesToWarmUp = serviceNames;

        if (_dualReadStateManager != null)
        {
          // warm up dual read mode for the service and its belonging cluster. This is needed BEFORE fetching the actual
          // data of service/cluster/uri (in the WarmUpTask below), so that when the actual data is received, they can
          // be reported to dual read monitoring under dual read mode.
          DualReadModeProvider dualReadModeProvider = _dualReadStateManager.getDualReadModeProvider();
          _servicesToWarmUp = serviceNames.stream().filter(serviceName -> {
            DualReadModeProvider.DualReadMode dualReadMode = dualReadModeProvider.getDualReadMode(serviceName);
            _dualReadStateManager.updateService(serviceName, dualReadMode);

            boolean res = isModeToWarmUp(dualReadMode, _isIndis);
            if (!res)
            {
              LOG.info("{} skipping service: {} based on its dual read mode: {}",
                  _printName, serviceName, dualReadMode);
            }
            return res;
          }).collect(Collectors.toList());

          _servicesToWarmUp.forEach(serviceName -> {
            // check timeout before continue
            if (!hasTimedOut.get()
                && _timeSupplier.get() - _allStartTime > _warmUpTimeoutMillis)
            {
              hasTimedOut.set(true);
              callback.onError(new TimeoutException());
            }

            // To warm up the cluster dual read mode, we need to fetch the service data to know its belonging cluster.
            LOG.info("{} fetching service data for service: {}", _printName, serviceName);

            // NOTE: This call blocks!
            getLoadBalancedServiceProperties(serviceName, new Callback<ServiceProperties>() {
              @Override
              public void onError(Throwable e) {
                LOG.warn("{} failed to warm up dual read mode for service: {}", _printName, serviceName, e);
              }

              @Override
              public void onSuccess(ServiceProperties result) {
                _dualReadStateManager.updateCluster(result.getClusterName(),
                    _dualReadStateManager.getServiceDualReadMode(result.getServiceName()));
              }
            });
          });

          LOG.info("{} fetched dual read mode for {} services in {}ms. {} services need to warm up.",
              _printName, serviceNames.size(), _timeSupplier.get() - _allStartTime,
              _servicesToWarmUp.size());
        }

        if (!hasTimedOut.get())
        {
          callback.onSuccess(None.none());
        }
      });
    }
    catch (Exception e)
    {
      callback.onError(e);
    }
  }

  private void continueWarmUp(Callback<None> callback)
  {
    if (_servicesToWarmUp.isEmpty())
    {
      LOG.info("{} no services to warmup. Warmup completed", _printName);
      callback.onSuccess(None.none());
      return;
    }

    // guaranteeing that we are going to use a thread that is not going to cause a deadlock
    // the caller might call this method on other threads (e.g. the ZK thread) creating possible circular dependencies
    // resulting in malfunctions
    _executorService.execute(() -> warmUpServices(callback));
  }

  /**
   * When the D2 client is ready, fetch the service names and attempt to warmUp each service. If a request fails, it
   * will be ignored and the warm up process will continue
   */
  private void warmUpServices(Callback<None> startUpCallback)
  {
    long timeoutMilli = Math.max(0, _warmUpTimeoutMillis - (_timeSupplier.get() - _allStartTime));
    LOG.info("{} starting to warm up with timeout: {}ms for {} services: [{}]",
        _printName, timeoutMilli, _servicesToWarmUp.size(), String.join(", ", _servicesToWarmUp));

    Callback<None> timeoutCallback = new TimeoutCallback<>(_executorService, timeoutMilli, TimeUnit.MILLISECONDS,
        new Callback<None>()
        {
          @Override
          public void onError(Throwable e)
          {
            LOG.info("{} hit timeout after {}ms since initial start time, continuing startup. "
                    + "Warmup will continue in background",
                _printName, _timeSupplier.get() - _allStartTime, e);
            startUpCallback.onSuccess(None.none());
          }

          @Override
          public void onSuccess(None result)
          {
            LOG.info("{} completed", _printName);
            startUpCallback.onSuccess(None.none());
          }
        }, "This message will never be used, even in case of timeout, no exception should be passed up"
    );

    try
    {
      // the WarmUpTask fetches the cluster and uri data, since the service data is already fetched
      WarmUpTask warmUpTask = new WarmUpTask(_servicesToWarmUp, timeoutCallback);

      // get the min value because it makes no sense have an higher concurrency than the number of request to be made
      int concurrentRequests = Math.min(_servicesToWarmUp.size(), _concurrentRequests);
      IntStream.range(0, concurrentRequests)
          .forEach(i -> _outstandingRequests.add(_executorService.submit(warmUpTask::execute)));
    }
    catch (Exception e)
    {
      LOG.error("{} failed, continuing start up.", _printName, e);
      timeoutCallback.onSuccess(None.none());
    }
  }

  @Override
  public ClusterInfoProvider getClusterInfoProvider() {
    return _loadBalancer.getClusterInfoProvider();
  }

  private class WarmUpTask
  {
    private final AtomicInteger _requestCompletedCount;
    private final AtomicInteger _requestStartedCount;
    private Queue<String> _serviceNamesQueue;
    private Callback<None> _callback;
    private List<String> _serviceNames;

    /**
     * @param serviceNames list of service names
     * @param callback     the callback must be a timeoutCallback which guarantees that the onSuccess method is called only once
     */
    WarmUpTask(List<String> serviceNames,
               Callback<None> callback)
    {
      _serviceNames = serviceNames;
      _requestStartedCount = new AtomicInteger(0);
      _requestCompletedCount = new AtomicInteger(0);
      _serviceNamesQueue = new ConcurrentLinkedDeque<>(serviceNames);
      _callback = callback;
    }

    void execute()
    {
      final long startTime = _timeSupplier.get();

      final String serviceName = _serviceNamesQueue.poll();
      if (serviceName == null || _shuttingDown)
      {
        return;
      }

      LOG.info("{} starting to warm up service {}, started {}/{}",
          _printName, serviceName, _requestStartedCount.incrementAndGet(), _serviceNames.size());

      // for services that have warmed up dual read mode above, their service data will be stored in event bus already,
      // so warming up the service data will complete instantly.
      _serviceWarmupper.warmUpService(serviceName, new Callback<None>()
      {
        private void executeNextTask()
        {
          if (_requestCompletedCount.incrementAndGet() == _serviceNames.size())
          {
            LOG.info("{} completed warming up {} services in {}ms",
                _printName, _serviceNames.size(), _timeSupplier.get() - _allStartTime);
            _callback.onSuccess(None.none());
            _outstandingRequests.clear();
            return;
          }
          _outstandingRequests.add(_executorService.submit(() -> execute()));
        }

        @Override
        public void onError(Throwable e)
        {
          LOG.info("{} failed to warm up service {}, completed {}/{}, continuing with warm up",
            _printName, serviceName, _requestCompletedCount.get() + 1, _serviceNames.size(), e);
          executeNextTask();
        }

        @Override
        public void onSuccess(None result)
        {
          LOG.info("{} completed warming up service {} in {}ms, completed {}/{}",
              _printName, serviceName, _timeSupplier.get() - startTime,
              _requestCompletedCount.get() + 1, _serviceNames.size());
          executeNextTask();
        }
      });
    }
  }

  private static boolean isModeToWarmUp(DualReadModeProvider.DualReadMode mode, boolean isIndis)
  {
    return mode == DualReadModeProvider.DualReadMode.DUAL_READ
        || mode == (isIndis ?
        DualReadModeProvider.DualReadMode.NEW_LB_ONLY : DualReadModeProvider.DualReadMode.OLD_LB_ONLY);
  }

  @Override
  public void shutdown(PropertyEventThread.PropertyEventShutdownCallback shutdown)
  {
    // Indicate that shutdown has started
    _shuttingDown = true;

    // Cancel all outstanding requests
    _outstandingRequests.forEach(future -> future.cancel(true));
    _outstandingRequests.clear();

    // Shut down the executor service and wait for it to terminate
    _executorService.shutdown();
    try
    {
      // Wait for termination with a timeout
      if (!_executorService.awaitTermination(_warmUpTimeoutMillis, TimeUnit.MILLISECONDS))
      {
        // Force shutdown if termination takes too long
        _executorService.shutdownNow();
        // Wait again to ensure termination
        if (!_executorService.awaitTermination(_warmUpTimeoutMillis, TimeUnit.MILLISECONDS))
        {
          // Log a warning if the executor service did not terminate
          LOG.warn("Executor service did not terminate in a timely manner.");
        }
      }
    }
    catch (InterruptedException e)
    {
      // If interrupted, force shutdown and restore interrupt status
      _executorService.shutdownNow();
      Thread.currentThread().interrupt();
      LOG.error("Interrupted while waiting for executor service to terminate.", e);
    }

    // Proceed with cleanup only if all outstanding requests are completed
    if (completedOutStandingRequests())
    {
      FileSystemDirectory fsDirectory = new FileSystemDirectory(_d2FsDirPath, _d2ServicePath);
      fsDirectory.removeAllServicesWithExcluded(_usedServices);
      fsDirectory.removeAllClustersWithExcluded(getUsedClusters());
    }

    // Finalize shutdown of the load balancer
    _loadBalancer.shutdown(shutdown);
  }

  boolean completedOutStandingRequests()
  {
    return _outstandingRequests.isEmpty();
  }

  private Set<String> getUsedClusters()
  {
    Set<String> usedClusters = new HashSet<>();
    for (String usedService : _usedServices)
    {
      try
      {
        ServiceProperties loadBalancedServiceProperties = getLoadBalancedServiceProperties(usedService);

        usedClusters.add(
          loadBalancedServiceProperties
            .getClusterName());
      }
      catch (ServiceUnavailableException e)
      {
        LOG.error("This exception shouldn't happen at this point because all the data should be valid", e);
      }
    }
    return usedClusters;
  }

  @Override
  public TransportClient getClient(Request request, RequestContext requestContext) throws ServiceUnavailableException
  {
    TransportClient client = _loadBalancer.getClient(request, requestContext);

    String serviceName = LoadBalancerUtil.getServiceNameFromUri(request.getURI());
    _usedServices.add(serviceName);
    return client;
  }
}
