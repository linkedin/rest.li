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

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.LoadBalancerWithFacilities;
import com.linkedin.d2.balancer.LoadBalancerWithFacilitiesDelegator;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.WarmUpService;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The WarmUpLoadBalancer warms up the internal {@link SimpleLoadBalancer} services/cluster list
 * before the client is announced as "started".
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class WarmUpLoadBalancer extends LoadBalancerWithFacilitiesDelegator
{
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
  private final int _warmUpTimeoutSeconds;
  private final int _concurrentRequests;
  private final ScheduledExecutorService _executorService;
  private final DownstreamServicesFetcher _downstreamServicesFetcher;
  private volatile boolean _shuttingDown = false;

  /**
   * Since the list might from the fetcher might not be complete (new behavior, old data, etc..), and the user might
   * require additional services at runtime, we have to store those services in such a way they are not cleared from the
   * cache at shutdown, otherwise it would incur in a penalty at the next deployment
   */
  private final Set<String> _usedServices;

  public WarmUpLoadBalancer(LoadBalancerWithFacilities balancer, WarmUpService serviceWarmupper, ScheduledExecutorService executorService,
                            String d2FsDirPath, String d2ServicePath, DownstreamServicesFetcher downstreamServicesFetcher,
                            int warmUpTimeoutSeconds, int concurrentRequests)
  {
    super(balancer);
    _serviceWarmupper = serviceWarmupper;
    _executorService = executorService;
    _d2FsDirPath = d2FsDirPath;
    _d2ServicePath = d2ServicePath;
    _downstreamServicesFetcher = downstreamServicesFetcher;
    _warmUpTimeoutSeconds = warmUpTimeoutSeconds;
    _concurrentRequests = concurrentRequests;
    _outstandingRequests = new ConcurrentLinkedDeque<>();
    _usedServices = new HashSet<>();
  }

  @Override
  public void start(Callback<None> callback)
  {
    LOG.info("D2 WarmUp enabled");
    _loadBalancer.start(new Callback<None>()
    {
      @Override
      public void onError(Throwable e)
      {
        callback.onError(e);
      }

      @Override
      public void onSuccess(None result)
      {
        // guaranteeing that we are going to use a thread that is not going to cause a deadlock
        // the caller might call this method on other threads (e.g. the ZK thread) creating possible circular dependencies
        // resulting in malfunctions
        _executorService.execute(() -> warmUpServices(callback));
      }
    });
  }

  /**
   * When the D2 client is ready, fetch the service names and attempt to warmUp each service. If a request fails, it
   * will be ignored and the warm up process will continue
   */
  private void warmUpServices(Callback<None> startUpCallback)
  {
    Callback<None> timeoutCallback = new TimeoutCallback<>(_executorService, _warmUpTimeoutSeconds, TimeUnit.SECONDS, new Callback<None>()
    {
      @Override
      public void onError(Throwable e)
      {
        LOG.info("D2 WarmUp hit timeout, continuing startup. The WarmUp will continue in background", e);
        startUpCallback.onSuccess(None.none());
      }

      @Override
      public void onSuccess(None result)
      {
        LOG.info("D2 WarmUp completed");
        startUpCallback.onSuccess(None.none());
      }
    }, "This message will never be used, even in case of timeout, no exception should be passed up");

    _downstreamServicesFetcher.getServiceNames(serviceNames -> {
      try
      {
        // The downstreamServicesFetcher is the core group of the services that will be used during the lifecycle
        _usedServices.addAll(serviceNames);

        LOG.info("Trying to warmup {} services: [{}]", serviceNames.size(), String.join(", ", serviceNames));

        if (serviceNames.size() == 0)
        {
          timeoutCallback.onSuccess(None.none());
          return;
        }

        WarmUpTask warmUpTask = new WarmUpTask(serviceNames, timeoutCallback);

        // get the min value because it makes no sense have an higher concurrency than the number of request to be made
        int concurrentRequests = Math.min(serviceNames.size(), _concurrentRequests);
        IntStream.range(0, concurrentRequests)
          .forEach(i -> _outstandingRequests.add(_executorService.submit(warmUpTask::execute)));
      }
      catch (Exception e)
      {
        LOG.error("D2 WarmUp Failed, continuing start up.", e);
        timeoutCallback.onSuccess(None.none());
      }
    });
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
      final long startTime = SystemClock.instance().currentTimeMillis();

      final String serviceName = _serviceNamesQueue.poll();
      if (serviceName == null || _shuttingDown)
      {
        return;
      }

      LOG.info("{}/{} Starting to warm up service {}", new Object[]{_requestStartedCount.incrementAndGet(), _serviceNames.size(), serviceName});

      _serviceWarmupper.warmUpService(serviceName, new Callback<None>()
      {
        private void executeNextTask()
        {
          if (_requestCompletedCount.incrementAndGet() == _serviceNames.size())
          {
            _callback.onSuccess(None.none());
            _outstandingRequests.clear();
            return;
          }
          _outstandingRequests.add(_executorService.submit(() -> execute()));
        }

        @Override
        public void onError(Throwable e)
        {
          LOG.info(String.format("%s/%s Service %s failed to warm up, continuing with warm up",
            _requestCompletedCount.get() + 1, _serviceNames.size(), serviceName), e);
          executeNextTask();
        }

        @Override
        public void onSuccess(None result)
        {
          LOG.info("{}/{} Service {} warmed up in {}ms", new Object[]{_requestCompletedCount.get() + 1, _serviceNames.size(),
            serviceName, SystemClock.instance().currentTimeMillis() - startTime});
          executeNextTask();
        }
      });
    }
  }

  @Override
  public void shutdown(PropertyEventThread.PropertyEventShutdownCallback shutdown)
  {
    // avoid cleaning when you risk to have partial results since some of the services have not loaded yet
    if (_outstandingRequests.size() == 0)
    {
      // cleanup from unused services
      FileSystemDirectory fsDirectory = new FileSystemDirectory(_d2FsDirPath, _d2ServicePath);
      fsDirectory.removeAllServicesWithExcluded(_usedServices);
      fsDirectory.removeAllClustersWithExcluded(getUsedClusters());
    }

    _shuttingDown = true;
    _outstandingRequests.forEach(future -> future.cancel(true));
    _outstandingRequests.clear();
    _loadBalancer.shutdown(shutdown);
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

  @Override
  public void getClient(Request request, RequestContext requestContext, Callback<TransportClient> clientCallback)
  {
    _loadBalancer.getClient(request, requestContext, new Callback<TransportClient>() {
      @Override
      public void onError(Throwable e) {
        clientCallback.onError(e);
      }

      @Override
      public void onSuccess(TransportClient result) {
        String serviceName = LoadBalancerUtil.getServiceNameFromUri(request.getURI());
        _usedServices.add(serviceName);
        clientCallback.onSuccess(result);
      }
    });
  }

  @Override
  public void getLoadBalancedServiceProperties(String serviceName, Callback<ServiceProperties> clientCallback) {
    _loadBalancer.getLoadBalancedServiceProperties(serviceName, clientCallback);
  }
}
