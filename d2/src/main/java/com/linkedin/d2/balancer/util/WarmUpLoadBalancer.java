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
import com.linkedin.d2.balancer.WarmUpService;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.discovery.event.PropertyEventThread;
import com.linkedin.r2.transport.http.client.TimeoutCallback;
import com.linkedin.util.clock.SystemClock;
import java.util.List;
import java.util.Queue;
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
 * <p>
 * It relies on the internal FileStore, which keeps a list of the called services in the previous runs.
 * As a consequence, if the service has not run previously on the current machine, there will be no warm up.
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class WarmUpLoadBalancer extends LoadBalancerWithFacilitiesDelegator
{
  private static final Logger LOG = LoggerFactory.getLogger(WarmUpLoadBalancer.class);

  /**
   * Default max of concurrent outstanding warm up requests
   */
  public static final int DEFAULT_CONCURRENT_REQUESTS = 20;
  public static final int DEFAULT_SEND_REQUESTS_TIMEOUT_SECONDS = 30;

  private final ConcurrentLinkedDeque<Future<?>> _outstandingRequests;

  private WarmUpService _serviceWarmupper;
  private final String _d2FsPath;
  private final String _d2ServicePath;
  private final int _warmUpTimeoutSeconds;
  private final int _concurrentRequests;
  private final ScheduledExecutorService _executorService;
  private volatile boolean _shuttingDown = false;

  public WarmUpLoadBalancer(LoadBalancerWithFacilities balancer, WarmUpService serviceWarmupper, ScheduledExecutorService executorService,
                            String d2FsDirPath, String d2ServicePath, int warmUpTimeoutSeconds, int concurrentRequests)
  {
    super(balancer);
    _serviceWarmupper = serviceWarmupper;
    _executorService = executorService;
    _d2FsPath = d2FsDirPath;
    _d2ServicePath = d2ServicePath;
    _warmUpTimeoutSeconds = warmUpTimeoutSeconds;
    _concurrentRequests = concurrentRequests;
    _outstandingRequests = new ConcurrentLinkedDeque<>();
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
        warmUpServices(callback);
      }
    });
  }

  /**
   * When the D2 client is ready, fetch the service names and attempt to warmUp each service. If a request fails, it
   * will be ignored and the warm up process will continue
   */
  private void warmUpServices(Callback<None> startUpCallback)
  {
    TimeoutCallback<None> timeoutCallback = new TimeoutCallback<>(_executorService, _warmUpTimeoutSeconds, TimeUnit.SECONDS, new Callback<None>()
    {
      @Override
      public void onError(Throwable e)
      {
        LOG.info("D2 WarmUp hit timeout, continuing startup. The WarmUp will continue in background");
        startUpCallback.onSuccess(None.none());
      }

      @Override
      public void onSuccess(None result)
      {
        LOG.info("D2 WarmUp completed");
        startUpCallback.onSuccess(None.none());
      }
    }, "This message will never be used, even in case of timeout, no exception should be passed up");

    try
    {
      FileSystemDirectory fsDirectory = new FileSystemDirectory(_d2FsPath, _d2ServicePath);
      List<String> serviceNames = fsDirectory.getServiceNames();

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
      LOG.error("D2 WarmUp Failed", e);
      timeoutCallback.onSuccess(None.none());
    }
  }

  private class WarmUpTask
  {
    private final AtomicInteger _requestCompletedCount;
    private final AtomicInteger _requestStartedCount;
    private Queue<String> _serviceNamesQueue;
    private TimeoutCallback<None> _callback;
    private List<String> _serviceNames;

    /**
     * @param serviceNames list of service names
     * @param callback     the callback must be a timeoutCallback which guarantees that the onSuccess method is called only once
     */
    WarmUpTask(List<String> serviceNames,
               TimeoutCallback<None> callback)
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
    _shuttingDown = true;
    _outstandingRequests.forEach(future -> future.cancel(true));
    _outstandingRequests.clear();
    _loadBalancer.shutdown(shutdown);
  }
}
