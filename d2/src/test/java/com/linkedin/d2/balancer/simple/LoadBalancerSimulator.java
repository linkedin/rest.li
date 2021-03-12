package com.linkedin.d2.balancer.simple;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.clients.RewriteLoadBalancerClient;
import com.linkedin.d2.balancer.event.EventEmitter;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyConfig;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyFactoryV3;
import com.linkedin.d2.balancer.util.URIRequest;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.d2.discovery.event.PropertyEventThread.PropertyEventShutdownCallback;
import com.linkedin.d2.discovery.event.SynchronousExecutorService;
import com.linkedin.d2.discovery.stores.mock.MockStore;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.r2.util.NamedThreadFactory;
import com.linkedin.util.clock.Clock;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;



/**
 * LoadBalancerSimulator simulates the transporting delays of different hosts for d2
 * degraderloadbalancer debugging, testing and verifications.
 *
 * The simulator requires 5 inputs:
 * . ServiceProperties, ClusterProperties and UriProperties: represent the d2 configurations.
 * . DelayGenerator: provides the delays for each given Uri
 * . QPSGenerator: provides the number of queries per interval
 *
 * To control the simulator:
 * . Asynchronous call: run(long duration) and runUntil(long untilTime)
 * . Synchronous call: runWait(long duration)
 * . stop()
 *
 * To check the status:
 * . getClientCounters(): returns the hits for each URI during last interval
 * . getPoints(): returns the hashring points for each URI
 *
 */

public class LoadBalancerSimulator
{
  private static final Logger _log = LoggerFactory.getLogger(LoadBalancerSimulator.class);

  private final MockStore<ServiceProperties> _serviceRegistry = new MockStore<>();
  private final MockStore<ClusterProperties> _clusterRegistry = new MockStore<>();
  private final MockStore<UriProperties> _uriRegistry = new MockStore<>();
  private final SimpleLoadBalancer _loadBalancer;
  private final SimpleLoadBalancerState _loadBalancerState;

  private final TimedValueGenerator<String, Long> _delayGenerator;
  private final QPSGenerator _qpsGenerator;

  private final ClockedExecutor _clockedExecutor;
  private final ScheduledExecutorService _syncExecutorService;

  private final Map<URI, Integer> _clientCounters = new HashMap<>();

  // the delay in milliseconds to schedule the first request
  private final int INIT_SCHEDULE_DELAY = 10;
  // How often to reschedule next set of requests
  private final long SCHEDULE_INTERVAL = DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS;
  private final ScheduledExecutorService _d2Executor;

  /**
   * Return the expected delay at the given time
   */
  interface DelayGenerator<T>
  {
    long nextDelay(T t);
  }

  /**
   * Return the number of queries for the next interval
   */
  interface QPSGenerator
  {
    int nextQPS();
  }

  /**
   * For a stream of values which changes periodically, get the value at the specific time
   */
  interface TimedValueGenerator<T, R>
  {
    R getValue(T t,  long time, TimeUnit unit);
  }

  LoadBalancerSimulator(ServiceProperties serviceProperties, ClusterProperties clusterProperties,
      UriProperties uriProperties, TimedValueGenerator<String, Long> delayGenerator,
      QPSGenerator qpsGenerator, EventEmitter eventEmitter) throws ExecutionException, InterruptedException
  {
    _syncExecutorService = new SynchronousExecutorService();
    _clockedExecutor = new ClockedExecutor();
    _d2Executor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("ZK properties D2 for Test"));

    // mock the properties to pass in simulation info
    Map<String, Object> transportProperty = new HashMap<>(serviceProperties.getTransportClientProperties());
    transportProperty.put("ClockedExecutor", _clockedExecutor);
    Map<String, Object> strategyProperty = new HashMap<>(serviceProperties.getLoadBalancerStrategyProperties());
    strategyProperty.put(PropertyKeys.CLOCK, _clockedExecutor);
    strategyProperty.put(PropertyKeys.HTTP_LB_QUARANTINE_EXECUTOR_SERVICE, _clockedExecutor);

    ServiceProperties updatedServiceProperties = new ServiceProperties(serviceProperties.getServiceName(),
        serviceProperties.getClusterName(), serviceProperties.getPath(),
        serviceProperties.getLoadBalancerStrategyList(),
        strategyProperty, transportProperty,
        serviceProperties.getDegraderProperties(),
        serviceProperties.getPrioritizedSchemes(),
        serviceProperties.getBanned());

    _serviceRegistry.put(serviceProperties.getServiceName(), updatedServiceProperties);
    _clusterRegistry.put(serviceProperties.getClusterName(), clusterProperties);
    _uriRegistry.put(serviceProperties.getClusterName(), uriProperties);

    _delayGenerator = delayGenerator;
    _qpsGenerator = qpsGenerator;

    // construct loadBalancer and start it
    Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories =
        new HashMap<>();
    Map<String, TransportClientFactory> clientFactories = new HashMap<>();


    loadBalancerStrategyFactories.put("degraderV3", new DegraderLoadBalancerStrategyFactoryV3(
        null, null, eventEmitter, Collections.emptyList()));
    DelayClientFactory delayClientFactory = new DelayClientFactory();
    clientFactories.put("http", delayClientFactory);
    clientFactories.put("https", delayClientFactory);

    _loadBalancerState = new SimpleLoadBalancerState(_syncExecutorService,
            _uriRegistry,
            _clusterRegistry,
            _serviceRegistry,
            clientFactories,
            loadBalancerStrategyFactories);

    _loadBalancer = new SimpleLoadBalancer(_loadBalancerState, 5, TimeUnit.SECONDS, _d2Executor);

    FutureCallback<None> balancerCallback = new FutureCallback<None>();
    _loadBalancer.start(balancerCallback);
    balancerCallback.get();

    // schedule the RequestTask, which starts new set of requests repeatedly at the given interval
    _clockedExecutor.scheduleWithFixedDelay(new RequestTask(updatedServiceProperties.getServiceName()),
        INIT_SCHEDULE_DELAY, SCHEDULE_INTERVAL, TimeUnit.MILLISECONDS);
  }

  public void shutdown() throws Exception
  {
    _clockedExecutor.shutdown();

    final CountDownLatch latch = new CountDownLatch(1);

    PropertyEventShutdownCallback callback = () -> latch.countDown();

    _loadBalancer.shutdown(callback);

    if (!latch.await(60, TimeUnit.SECONDS))
    {
      Assert.fail("unable to shutdown state");
    }

    _log.info("LoadBalancer Shutdown @ {}", _clockedExecutor.currentTimeMillis());
  }

  public void updateUriProperties(UriProperties uriProperties)
  {
    _uriRegistry.put(uriProperties.getClusterName(), uriProperties);
  }

  /**
   * Run the simulation until no task in the queue or stopped by explicitly call (Async)
   * @return
   */
  public Future<Void> run()
  {
    return run(0);
  }

  /**
   * Run the simulation for the provided duration (Async)
   * @param duration
   * @return
   */
  public Future<Void> run(long duration)
  {
    return _clockedExecutor.run(duration <= 0 ? 0 : _clockedExecutor._currentTimeMillis + duration);
  }

  /**
   * Run the simulation until the givenTime (Async)
   * @param expectedTime
   * @return
   */
  public Future<Void> runUntil(long expectedTime)
  {
    return _clockedExecutor.run(expectedTime);
  }

  /**
   * Run the simulation for the given duration (Sync)
   * @param duration
   */
  public void runWait(long duration)
  {
    Future<Void> running = run(duration);
    if (running != null)
    {
      try
      {
        running.get();
      }
      catch (InterruptedException | ExecutionException e)
      {
        _log.error("Simulation error: ", e);
      }
    }
  }

  public void stop()
  {
    _clockedExecutor.stop();
  }

  public Map<URI, Integer> getClientCounters()
  {
    return _clientCounters;
  }

  public Clock getClock()
  {
    return _clockedExecutor;
  }

  public ScheduledExecutorService getExecutorService()
  {
    return _clockedExecutor;
  }

  public ClockedExecutor getClockedExecutor()
  {
    return _clockedExecutor;
  }

  public SimpleLoadBalancerState getLoadBalancerState()
  {
    return _loadBalancerState;
  }

  /**
   * Given a serviceName and partition number, return the hashring points for each URI
   * @param serviceName
   * @param partition
   * @return
   * @throws ServiceUnavailableException
   */
  public Map<URI, Integer> getPoints(String serviceName, int partition) throws ServiceUnavailableException
  {
    URI serviceUri = URI.create("d2://" + serviceName);
    Ring<URI> ring = _loadBalancer.getRings(serviceUri).get(partition);
    Map<URI, Integer> pointsMap = new HashMap<>();
    Random random = new Random();
    Iterator<URI> iter = ring.getIterator(random.nextInt());

    iter.forEachRemaining(uri -> pointsMap.compute(uri, (k, v) -> v == null ? 1: v + 1));

    return pointsMap;
  }

  /**
   * Get the point for the given uri
   * @param serviceName
   * @param partition
   * @param uri
   * @return
   */
  public int getPoint(String serviceName, int partition, URI uri)
  {
    try
    {
      Map<URI, Integer> points = getPoints(serviceName, partition);
      return points.getOrDefault(uri, 0);
    }
    catch (ServiceUnavailableException e)
    {
      return 0;
    }
  }

  public int getPoint(String serviceName, int partition, String uriString)
  {
    return getPoint(serviceName, partition, URI.create("http://" + uriString));
  }

  /**
   * Get the hitting percentage of the given uri (ie 'uri count'/'total inquiries')
   * @param uri
   * @return
   */
  public double getCountPercent(URI uri)
  {
    return getPercentageFromMap(uri, getClientCounters());
  }

  private double getPercentageFromMap(URI uri, Map<URI, Integer> map)
  {
    if (!map.containsKey(uri))
    {
      return 0.0;
    }
    Integer total = map.values().stream().reduce(0, Integer::sum);
    if (total == 0)
    {
      return 0.0;
    }
    return 1.0 * map.get(uri) / total;
  }

  /**
   * A runnable task to send out request
   */
  private class RequestTask implements Runnable
  {
    private String _serviceName;

    public RequestTask(String serviceName)
    {
      _serviceName = serviceName;
    }

    @Override
    public void run()
    {
      int qps = 0;
      Map<URI, Long> uriDelays = new HashMap<>();

      _clientCounters.clear();
      try
      {
        qps = _qpsGenerator.nextQPS();
      }
      catch(IllegalArgumentException e)
      {
        return;
      }

      for (int i = 0; i < qps; ++i)
      {
        // construct the requests
        URIRequest uriRequest = new URIRequest("d2://" + _serviceName + "/" + i);
        RestRequest restRequest = new RestRequestBuilder(uriRequest.getURI()).build();
        RequestContext requestContext = new RequestContext();

        RewriteLoadBalancerClient client = null;
        try
        {
          client = (RewriteLoadBalancerClient) _loadBalancer.getClient(restRequest, requestContext);
        }
        catch (ServiceUnavailableException e)
        {
          _log.error("Could not find service for request " + restRequest.getURI(), e);
          Assert.fail("Failed to find the service");
        }

        TransportCallback<RestResponse> restCallback = (response) -> {
          // assertFalse(response.hasError());
          _log.debug("Got response for {} @ {}", response.getResponse(), _clockedExecutor.currentTimeMillis());
          // Do nothing for now for the response
        };

        URI clientUri = client.getUri();

        _log.debug("Adding trackerclient for {}", clientUri);

        // Increase the counter for each URI
        _clientCounters.compute(clientUri, (k, v) -> v == null ? 1 : v + 1);

        // send out the request
        client.restRequest(restRequest, requestContext, Collections.emptyMap(), restCallback);
      }
    }
  }

  /**
   * A simulated TransportClient, which schedules a delayed task to return the response.
   */
  @SuppressWarnings("unchecked")
  private static class DelayClientFactory implements TransportClientFactory
  {
    @Override
    public TransportClient getClient(Map<String, ? extends Object> properties)
    {
      ClockedExecutor clockedExecutor = (ClockedExecutor) properties.get("ClockedExecutor");
      TimedValueGenerator<String, Long> delayGen = (TimedValueGenerator<String, Long>) properties.get("DelayGenerator");
      TimedValueGenerator<String, String> errorGen = (TimedValueGenerator<String, String>) properties.get("ErrorGenerator");

      return new DelayClient(clockedExecutor, delayGen, errorGen);
    }

    /**
     * DelayClient is a TransportClient that can delay the response with a given time
     */
    private class DelayClient implements TransportClient
    {
      final private ClockedExecutor _clockedExecutor;
      final private TimedValueGenerator<String, Long> _delayGen;
      final private TimedValueGenerator<String, String> _errorGen;

      DelayClient(ClockedExecutor executor, TimedValueGenerator<String, Long> delayGen, TimedValueGenerator<String, String> errorGen)
      {
        _clockedExecutor = executor;
        _delayGen = delayGen;
        _errorGen = errorGen;
      }

      @Override
      public void streamRequest(StreamRequest request,
          RequestContext requestContext,
          Map<String, String> wireAttrs,
          TransportCallback<StreamResponse> callback)
      {
        throw new IllegalArgumentException("StreamRequest is not supported yet");
      }

      @Override
      public void restRequestStreamResponse(RestRequest request, RequestContext requestContext,
          Map<String, String> wireAttrs, TransportCallback<StreamResponse> callback) {
        throw new IllegalArgumentException("RestRequestStreamResponse is not supported yet");
      }

      @Override
      public void restRequest(RestRequest request,
          RequestContext requestContext,
          Map<String, String> wireAttrs,
          TransportCallback<RestResponse> callback)
      {
        Long delay = _delayGen.getValue(request.getURI().getAuthority(), _clockedExecutor.currentTimeMillis(),
            TimeUnit.MILLISECONDS);
        _clockedExecutor.schedule(new Runnable() {
          @Override
          public void run()
          {
            RestResponseBuilder restResponseBuilder = new RestResponseBuilder().setEntity(request.getURI().getRawPath().getBytes());
            if (_errorGen != null) {
              String retError = _errorGen.getValue(request.getURI().getAuthority(), _clockedExecutor.currentTimeMillis(),
                  TimeUnit.MILLISECONDS);
              if (retError != null)
              {
                restResponseBuilder.setStatus(500); // only 500 errors are counted
                RestException restException = new RestException(restResponseBuilder.build(), new Throwable(retError));
                callback.onResponse(TransportResponseImpl.error(restException));
                return;
              }
            }
            callback.onResponse(TransportResponseImpl.success(restResponseBuilder.build()));
          }
        }, delay, TimeUnit.MILLISECONDS);
      }

      @Override
      public void shutdown(Callback<None> callback)
      {
        callback.onSuccess(None.none());
      }
    }

    @Override
    public void shutdown(Callback<None> callback)
    {
      callback.onSuccess(None.none());
    }
  }

  /**
   * A simulated service executor and clock
   */
  public static class ClockedExecutor implements Clock, ScheduledExecutorService
  {
    private volatile long _currentTimeMillis = 0l;
    private volatile Boolean _stopped = true;
    private volatile long _runUntil = 0l;
    private PriorityBlockingQueue<ClockedTask> _taskList = new PriorityBlockingQueue<>();
    private ExecutorService _executorService = Executors.newFixedThreadPool(1);

    public Future<Void> run(long untilTime)
    {
      if (!_stopped)
      {
        throw new IllegalArgumentException("Already Started!");
      }
      if (_taskList.isEmpty())
      {
        return null;
      }
      _stopped = false;
      _runUntil = untilTime;

      Future<Void> taskExecutor = _executorService.submit(() -> {
        while (!_stopped && !_taskList.isEmpty() && (_runUntil <= 0l || _runUntil > _currentTimeMillis))
        {
          ClockedTask task = _taskList.peek();
          long expectTime = task.getScheduledTime();

          if (expectTime > _runUntil)
          {
            _currentTimeMillis = _runUntil;
            break;
          }

          _taskList.remove();

          if (expectTime > _currentTimeMillis)
          {
            _currentTimeMillis = expectTime;
          }
          _log.debug("Processing task " + task.toString() + " total {}, time {}",
              _taskList.size(), _currentTimeMillis);
          task.run();
          if (task.repeatCount() > 0 && !task.isCancelled() && !_stopped)
          {
            task.reschedule(_currentTimeMillis);
            _taskList.add(task);
          }
        }
        _stopped = true;
        return null;
      });
      return taskExecutor;
    }

    @Override
    public ScheduledFuture<Void> schedule(Runnable cmd, long delay, TimeUnit unit)
    {
      ClockedTask task = new ClockedTask("ScheduledTask", cmd, _currentTimeMillis + delay);
      _taskList.add(task);
      return task;
    }

    @Override
    public <Void> ScheduledFuture<Void> schedule(Callable<Void> callable, long delay, TimeUnit unit)
    {
      throw new IllegalArgumentException("Not supported yet!");
    }

    @Override
    public ScheduledFuture<Void> scheduleAtFixedRate(Runnable command, long initialDelay,
        long period, TimeUnit unit)
    {
      throw new IllegalArgumentException("Not supported yet!");
    }

    @Override
    public ScheduledFuture<Void> scheduleWithFixedDelay(Runnable cmd, long initDelay, long interval,
        TimeUnit unit)
    {
      ClockedTask task = new ClockedTask("scheduledWithDelayTask", cmd, _currentTimeMillis
          + unit.convert(initDelay, TimeUnit.MILLISECONDS), interval, Long.MAX_VALUE);
      _taskList.add(task);
      return task;
    }

    public void scheduleWithRepeat(Runnable cmd, long initDelay, long interval, long repeatTimes)
    {
      ClockedTask task = new ClockedTask("scheduledWithRepeatTask", cmd, _currentTimeMillis + initDelay, interval, repeatTimes);
      _taskList.add(task);
    }

    @Override
    public void execute(Runnable cmd)
    {
      ClockedTask task = new ClockedTask("executTask", cmd, _currentTimeMillis);
      _taskList.add(task);
    }

    public void stop()
    {
      _stopped = true;
    }

    @Override
    public void shutdown()
    {
      _stopped = true;
      _executorService.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow()
    {
      throw new IllegalArgumentException("Not supported yet!");
    }

    @Override
    public boolean isShutdown()
    {
      return _stopped;
    }

    @Override
    public boolean isTerminated()
    {
      return _stopped && _taskList.isEmpty();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException
    {
      run(unit.convert(timeout, TimeUnit.MILLISECONDS));
      return true;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task)
    {
      throw new IllegalArgumentException("Not supported yet!");
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result)
    {
      throw new IllegalArgumentException("Not supported yet!");
    }

    @Override
    public Future<?> submit(Runnable task)
    {
      if (task == null)
      {
        throw new NullPointerException();
      }
      RunnableFuture<Void> ftask =  new FutureTask<>(()->{}, null);
      // Simulation only: Run the task in current thread
      task.run();
      return ftask;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
        throws InterruptedException
    {
      throw new IllegalArgumentException("Not supported yet!");
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
        long timeout, TimeUnit unit)
        throws InterruptedException
    {
      throw new IllegalArgumentException("Not supported yet!");
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
        throws InterruptedException, ExecutionException
    {
      throw new IllegalArgumentException("Not supported yet!");
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
        long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException
    {
      throw new IllegalArgumentException("Not supported yet!");
    }

    @Override
    public long currentTimeMillis()
    {
      return _currentTimeMillis;
    }


    @Override
    public String toString()
    {
      return "ClockedExecutor [_currentTimeMillis: " + _currentTimeMillis + "_taskList:"
          + _taskList.stream().map(e -> e.toString()).collect(Collectors.joining(","));
    }

    private class ClockedTask implements Runnable, ScheduledFuture<Void>
    {
      final private String _name;
      private long _expectTimeMillis = 0l;
      private long _interval = 0l;
      private Runnable _task;
      private long _repeatTimes = 0l;
      private CountDownLatch _done;
      private boolean _cancelled = false;

      ClockedTask(String name, Runnable task, long scheduledTime)
      {
        this(name, task, scheduledTime, 0l, 0l);
      }

      ClockedTask(String name, Runnable task, long scheduledTime, long interval, long repeat)
      {
        _name = name;
        _task = task;
        _expectTimeMillis = scheduledTime;
        _interval = interval;
        _repeatTimes = repeat;
        _done = new CountDownLatch(1);
        _cancelled = false;
      }

      @Override
      public void run()
      {
        if (!_cancelled)
        {
          _task.run();
          _done.countDown();
        }
      }

      long repeatCount()
      {
        return _repeatTimes;
      }

      long getScheduledTime()
      {
        return _expectTimeMillis;
      }

      void reschedule(long currentTime)
      {
        if (!_cancelled && currentTime >= _expectTimeMillis && _repeatTimes-- > 0)
        {
          _expectTimeMillis += (_interval - (currentTime - _expectTimeMillis));
          _done = new CountDownLatch(1);
        }
      }

      @Override
      public boolean cancel(boolean mayInterruptIfRunning)
      {
        _cancelled = true;
        if (_done.getCount() > 0)
        {
          _done.countDown();
          return true;
        }
        return false;
      }

      @Override
      public boolean isCancelled()
      {
        return _cancelled;
      }

      @Override
      public boolean isDone()
      {
        return _done.getCount() == 0;
      }

      @Override
      public Void get() throws InterruptedException
      {
        _done.await();
        return null;
      }
      @Override
      public Void get(long timeout, TimeUnit unit) throws InterruptedException
      {
        _done.await(timeout, unit);
        return null;
      }
      @Override
      public long getDelay(TimeUnit unit)
      {
        return unit.convert(_expectTimeMillis - _currentTimeMillis, TimeUnit.MILLISECONDS);
      }

      @Override
      public int compareTo(Delayed other)
      {
        return (int) (getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS));
      }

      @Override
      public String toString()
      {
        return "ClockedTask [_name=" + _name + "_expectedTime=" + _expectTimeMillis
            + "_repeatTimes=" + _repeatTimes + "_interval=" + _interval + "]";
      }
    }
  }
}
