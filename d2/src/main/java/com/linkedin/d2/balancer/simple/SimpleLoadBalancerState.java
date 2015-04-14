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

package com.linkedin.d2.balancer.simple;

import static com.linkedin.d2.discovery.util.LogUtil.debug;
import static com.linkedin.d2.discovery.util.LogUtil.info;
import static com.linkedin.d2.discovery.util.LogUtil.trace;
import static com.linkedin.d2.discovery.util.LogUtil.warn;

import com.linkedin.common.util.MapUtil;
import com.linkedin.d2.balancer.properties.ClientServiceConfigValidator;
import com.linkedin.d2.balancer.properties.AllowedClientPropertyKeys;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.strategies.degrader.DegraderConfigFactory;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyConfig;
import com.linkedin.internal.common.util.CollectionUtils;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.util.clock.SystemClock;
import com.linkedin.util.degrader.DegraderImpl;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.Callbacks;
import com.linkedin.common.callback.SimpleCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.LoadBalancerState;
import com.linkedin.d2.balancer.LoadBalancerStateItem;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.util.ClientFactoryProvider;
import com.linkedin.d2.balancer.util.LoadBalancerUtil;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessor;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessorFactory;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import com.linkedin.d2.discovery.event.PropertyEventBusImpl;
import com.linkedin.d2.discovery.event.PropertyEventPublisher;
import com.linkedin.d2.discovery.event.PropertyEventSubscriber;
import com.linkedin.d2.discovery.event.PropertyEventThread.PropertyEvent;
import com.linkedin.d2.discovery.event.PropertyEventThread.PropertyEventShutdownCallback;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.util.ClosableQueue;
import com.linkedin.r2.util.ConfigValueExtractor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

public class SimpleLoadBalancerState implements LoadBalancerState, ClientFactoryProvider
{
  private static final Logger                                                            _log =
                                                                                                  LoggerFactory.getLogger(SimpleLoadBalancerState.class);

  private final UriLoadBalancerSubscriber                                                _uriSubscriber;
  private final ClusterLoadBalancerSubscriber                                            _clusterSubscriber;
  private final ServiceLoadBalancerSubscriber                                            _serviceSubscriber;

  private final PropertyEventBus<UriProperties>                                          _uriBus;
  private final PropertyEventBus<ClusterProperties>                                      _clusterBus;
  private final PropertyEventBus<ServiceProperties>                                      _serviceBus;

  private final Map<String, LoadBalancerStateItem<UriProperties>>                        _uriProperties;
  private final Map<String, ClusterInfoItem>                                             _clusterInfo;
  private final Map<String, LoadBalancerStateItem<ServiceProperties>>                    _serviceProperties;

  private final AtomicLong                                                               _version;

  private final Map<String, Set<String>>                                                 _servicesPerCluster;
  private final ScheduledExecutorService                                                 _executor;
  private final List<SimpleLoadBalancerStateListener>                                    _listeners;

  private volatile long                                                                  _delayedExecution;
  /**
   * Map from service name => uri => tracker client.
   */
  private final Map<String, Map<URI, TrackerClient>>                                     _trackerClients;

  /**
   * Map from serviceName => schemeName.toLowerCase() => TransportClient
   */
  private final Map<String, Map<String, TransportClient>>                                _serviceClients;

  /**
   * Map from scheme => client factory. For example, http => HttpClientFactory.
   */
  private final Map<String, TransportClientFactory>                                      _clientFactories;

  /**
   * Map from load balancer name => load balancer factory. For example, degrader =>
   * DegraderLoadBalancerStrategyFactory.
   */
  private final Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> _loadBalancerStrategyFactories;

  /**
   * Map from service name => scheme => load balancer strategy. For example, browsemaps =>
   * http => degrader.
   */
  private final Map<String, Map<String, LoadBalancerStrategy>>                           _serviceStrategies;

  /**
   * Map from service name => list of scheme, strategy pairs
   * This is a lazily-populated cache of the results from getStrategiesForService()
   */
  private final Map<String, List<SchemeStrategyPair>>                                   _serviceStrategiesCache;

  private final SSLContext    _sslContext;
  private final SSLParameters _sslParameters;
  private final boolean       _isSSLEnabled;

  private static final String LIST_SEPARATOR = ",";

  /**
   * Map from service name => Map of properties for that service. This map is supplied by the client and will
   * override any server supplied config values. The inner map is a flat map (property name => property value) which
   * can include transport client properties, degrader properties etc. Our namespacing rules for property names
   * (e.g. http.loadBalancer.hashMethod, degrader.maxDropRate) allow the inner map to be flat.
   */
  private final Map<String, Map<String, Object>> _clientServicesConfig;

  // we put together the cluster properties and the partition accessor for a cluster so that we don't have to
  // maintain two seperate maps (which have to be in sync all the time)
  private class ClusterInfoItem
  {
    private final LoadBalancerStateItem<ClusterProperties> _clusterPropertiesItem;
    private final LoadBalancerStateItem<PartitionAccessor> _partitionAccessorItem;

    ClusterInfoItem(ClusterProperties clusterProperties, PartitionAccessor partitionAccessor)
    {
      long version = _version.incrementAndGet();
      _clusterPropertiesItem = new LoadBalancerStateItem<ClusterProperties>(clusterProperties,
                                                                            version,
                                                                            System.currentTimeMillis());
      _partitionAccessorItem = new LoadBalancerStateItem<PartitionAccessor>(partitionAccessor,
                                                                            version,
                                                                            System.currentTimeMillis());
    }

    LoadBalancerStateItem<ClusterProperties> getClusterPropertiesItem()
    {
      return _clusterPropertiesItem;
    }

    LoadBalancerStateItem<PartitionAccessor> getPartitionAccessorItem()
    {
      return _partitionAccessorItem;
    }

    @Override
    public String toString()
    {
      return "_clusterProperties = " + _clusterPropertiesItem.getProperty();
    }
  }

  /*
   * Concurrency considerations:
   *
   * Immutable: _clientFactories _loadBalancerStrategyFactories
   *
   * All event bus callbacks occur on a single thread. The following are mutated only
   * within event bus callbacks, but may be read from any thread at any time:
   * _uriProperties _clusterProperties _serviceProperties _servicesPerCluster
   * _trackerClients _serviceStrategies
   */

  public SimpleLoadBalancerState(ScheduledExecutorService executorService,
                                 PropertyEventPublisher<UriProperties> uriPublisher,
                                 PropertyEventPublisher<ClusterProperties> clusterPublisher,
                                 PropertyEventPublisher<ServiceProperties> servicePublisher,
                                 Map<String, TransportClientFactory> clientFactories,
                                 Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories)
  {
    this(executorService,
         uriPublisher,
         clusterPublisher,
         servicePublisher,
         clientFactories,
         loadBalancerStrategyFactories,
         null, null, false);
  }

  public SimpleLoadBalancerState(ScheduledExecutorService executorService,
                                 PropertyEventPublisher<UriProperties> uriPublisher,
                                 PropertyEventPublisher<ClusterProperties> clusterPublisher,
                                 PropertyEventPublisher<ServiceProperties> servicePublisher,
                                 Map<String, TransportClientFactory> clientFactories,
                                 Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories,
                                 SSLContext sslContext,
                                 SSLParameters sslParameters,
                                 boolean isSSLEnabled)
  {
    this(executorService,
         new PropertyEventBusImpl<UriProperties>(executorService, uriPublisher),
         new PropertyEventBusImpl<ClusterProperties>(executorService, clusterPublisher),
         new PropertyEventBusImpl<ServiceProperties>(executorService, servicePublisher),
         clientFactories,
         loadBalancerStrategyFactories,
         sslContext,
         sslParameters,
         isSSLEnabled,
         Collections.<String, Map<String, Object>>emptyMap());
  }

  public SimpleLoadBalancerState(ScheduledExecutorService executorService,
                                 PropertyEventBus<UriProperties> uriBus,
                                 PropertyEventBus<ClusterProperties> clusterBus,
                                 PropertyEventBus<ServiceProperties> serviceBus,
                                 Map<String, TransportClientFactory> clientFactories,
                                 Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories,
                                 SSLContext sslContext,
                                 SSLParameters sslParameters,
                                 boolean isSSLEnabled)
  {
    this(executorService,
         uriBus,
         clusterBus,
         serviceBus,
         clientFactories,
         loadBalancerStrategyFactories,
         sslContext,
         sslParameters,
         isSSLEnabled,
         Collections.<String, Map<String, Object>>emptyMap());
  }

  public SimpleLoadBalancerState(ScheduledExecutorService executorService,
                                 PropertyEventBus<UriProperties> uriBus,
                                 PropertyEventBus<ClusterProperties> clusterBus,
                                 PropertyEventBus<ServiceProperties> serviceBus,
                                 Map<String, TransportClientFactory> clientFactories,
                                 Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories,
                                 SSLContext sslContext,
                                 SSLParameters sslParameters,
                                 boolean isSSLEnabled,
                                 Map<String, Map<String, Object>> clientServicesConfig)
  {
    _executor = executorService;
    _uriProperties =
        new ConcurrentHashMap<String, LoadBalancerStateItem<UriProperties>>();
    _clusterInfo =
        new ConcurrentHashMap<String, ClusterInfoItem>();
    _serviceProperties =
        new ConcurrentHashMap<String, LoadBalancerStateItem<ServiceProperties>>();
    _version = new AtomicLong(0);

    _uriBus = uriBus;
    _uriSubscriber = new UriLoadBalancerSubscriber(uriBus);

    _clusterBus = clusterBus;
    _clusterSubscriber = new ClusterLoadBalancerSubscriber(clusterBus);

    _serviceBus = serviceBus;
    _serviceSubscriber = new ServiceLoadBalancerSubscriber(serviceBus);

    // We assume the factories themselves are immutable, therefore a shallow copy of the
    // maps
    // should be a completely immutable data structure.
    _clientFactories =
        Collections.unmodifiableMap(new HashMap<String, TransportClientFactory>(clientFactories));
    _loadBalancerStrategyFactories =
        Collections.unmodifiableMap(new HashMap<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>>(loadBalancerStrategyFactories));

    _servicesPerCluster = new ConcurrentHashMap<String, Set<String>>();
    _serviceStrategies =
        new ConcurrentHashMap<String, Map<String, LoadBalancerStrategy>>();
    _serviceStrategiesCache =
        new ConcurrentHashMap<String, List<SchemeStrategyPair>>();
    _trackerClients = new ConcurrentHashMap<String, Map<URI, TrackerClient>>();
    _serviceClients = new ConcurrentHashMap<String, Map<String, TransportClient>>();
    _listeners =
        Collections.synchronizedList(new ArrayList<SimpleLoadBalancerStateListener>());
    _delayedExecution = 1000;
    _sslContext = sslContext;
    _sslParameters = sslParameters;
    _isSSLEnabled = isSSLEnabled;
    _clientServicesConfig = validateClientServicesConfig(clientServicesConfig);
  }

  /**
   * Validates the keys in the inner map for the client supplied per service config.
   */
  private Map<String, Map<String, Object>> validateClientServicesConfig(Map<String, Map<String, Object>> clientServicesConfig)
  {
    Map<String, Map<String, Object>> validatedClientServicesConfig = new HashMap<String, Map<String, Object>>();
    for (Map.Entry<String, Map<String, Object>> entry: clientServicesConfig.entrySet())
    {
      String serviceName = entry.getKey();
      Map<String, Object> clientConfigForSingleService = entry.getValue();
      Map<String, Object> validatedClientConfigForSingleService = new HashMap<String, Object>();
      for (Map.Entry<String, Object> innerMapEntry: clientConfigForSingleService.entrySet())
      {
        String clientSuppliedConfigKey = innerMapEntry.getKey();
        Object clientSuppliedConfigValue = innerMapEntry.getValue();
        if (AllowedClientPropertyKeys.isAllowedConfigKey(clientSuppliedConfigKey))
        {
          validatedClientConfigForSingleService.put(clientSuppliedConfigKey, clientSuppliedConfigValue);
          info(_log, "Client supplied config key {} for service {}", new Object[]{clientSuppliedConfigKey, serviceName});
        }
      }
      if (!validatedClientConfigForSingleService.isEmpty())
      {
        validatedClientServicesConfig.put(serviceName, validatedClientConfigForSingleService);
      }
    }
    return validatedClientServicesConfig;
  }

  public void register(final SimpleLoadBalancerStateListener listener)
  {
    trace(_log, "register listener: ", listener);

    _executor.execute(new PropertyEvent("add listener for state")
    {
      @Override
      public void innerRun()
      {
        _listeners.add(listener);
      }
    });
  }

  public void unregister(final SimpleLoadBalancerStateListener listener)
  {
    trace(_log, "unregister listener: ", listener);

    _executor.execute(new PropertyEvent("remove listener for state")
    {
      @Override
      public void innerRun()
      {
        _listeners.remove(listener);
      }
    });
  }

  @Override
  public void start(final Callback<None> callback)
  {
    callback.onSuccess(None.none());
  }

  @Override
  public void shutdown(final PropertyEventShutdownCallback shutdown)
  {
    trace(_log, "shutdown");

    // shutdown all three registries, all tracker clients, and the event thread
    _executor.execute(new PropertyEvent("shutdown load balancer state")
    {
      @Override
      public void innerRun()
      {
        // put all tracker clients into a single set for convenience
        Set<TransportClient> transportClients = new HashSet<TransportClient>();

        for (Map<String, TransportClient> clientsByScheme : _serviceClients.values())
        {
          transportClients.addAll(clientsByScheme.values());
        }

        Callback<None> trackerCallback = Callbacks.countDown(Callbacks.<None>adaptSimple(new SimpleCallback()
        {
          @Override
          public void onDone()
          {
            shutdown.done();
          }
        }), transportClients.size());

        info(_log, "shutting down cluster clients");

        for (TransportClient transportClient : transportClients)
        {
          transportClient.shutdown(trackerCallback);
        }
      }
    });
  }

  @Override
  public void listenToService(final String serviceName,
                              final LoadBalancerStateListenerCallback callback)
  {
    trace(_log, "listenToService: ", serviceName);

    _serviceSubscriber.ensureListening(serviceName, callback);
  }

  @Override
  public void listenToCluster(final String clusterName,
                              final LoadBalancerStateListenerCallback callback)
  {
    trace(_log, "listenToCluster: ", clusterName);

    // wrap the callback since we need to wait for both uri and cluster listeners to
    // onInit before letting the callback know that we're done.
    final LoadBalancerStateListenerCallback wrappedCallback =
        new LoadBalancerStateListenerCallback()
        {
          private final AtomicInteger _count = new AtomicInteger(2);

          @Override
          public void done(int type, String name)
          {
            if (_count.decrementAndGet() <= 0)
            {
              callback.done(type, clusterName);
            }
          }
        };

    _clusterSubscriber.ensureListening(clusterName, wrappedCallback);
    _uriSubscriber.ensureListening(clusterName, wrappedCallback);
  }

  @Override
  public LoadBalancerStateItem<UriProperties> getUriProperties(String clusterName)
  {
    return _uriProperties.get(clusterName);
  }

  @Override
  public LoadBalancerStateItem<ClusterProperties> getClusterProperties(String clusterName)
  {
    ClusterInfoItem clusterInfoItem =  _clusterInfo.get(clusterName);
    return clusterInfoItem == null ? null : clusterInfoItem.getClusterPropertiesItem();
  }

  @Override
  public LoadBalancerStateItem<PartitionAccessor> getPartitionAccessor(String clusterName)
  {
    ClusterInfoItem clusterInfoItem =  _clusterInfo.get(clusterName);
    return clusterInfoItem == null ? null : clusterInfoItem.getPartitionAccessorItem();
  }

  @Override
  public LoadBalancerStateItem<ServiceProperties> getServiceProperties(String serviceName)
  {
    return _serviceProperties.get(serviceName);
  }

  public Map<String, LoadBalancerStateItem<ServiceProperties>> getServiceProperties()
  {
    return _serviceProperties;
  }

  public long getVersion()
  {
    return _version.get();
  }

  public int getClusterCount()
  {
    return _clusterInfo.size();
  }

  public int getClusterListenCount()
  {
    return _clusterSubscriber.propertyListenCount();
  }

  public int getListenerCount()
  {
    return _listeners.size();
  }

  public int getServiceCount()
  {
    return _serviceProperties.size();
  }

  public int getServiceListenCount()
  {
    return _serviceSubscriber.propertyListenCount();
  }

  public Set<String> getSupportedSchemes()
  {
    return _clientFactories.keySet();
  }

  public Set<String> getSupportedStrategies()
  {
    return _loadBalancerStrategyFactories.keySet();
  }

  public int getTrackerClientCount(String clusterName)
  {
    Set<String> serviceNames = _servicesPerCluster.get(clusterName);
    int count = 0;
    for (String serviceName : serviceNames)
    {
      count += LoadBalancerUtil.getOrElse(_trackerClients,
                                            serviceName,
                                            new HashMap<URI, TrackerClient>()).size();
    }
    return count;
  }

  public Set<String> getServicesForCluster(String clusterName)
  {
    Set<String> services = _servicesPerCluster.get(clusterName);
    if (services == null)
    {
      return Collections.emptySet();
    }
    else
    {
      return services;
    }
  }

  public int getUriCount()
  {
    return _uriProperties.size();
  }

  public void setVersion(final long version)
  {
    trace(_log, "setVersion: ", version);

    _executor.execute(new PropertyEvent("set version to: " + version)
    {
      @Override
      public void innerRun()
      {
        info(_log, "set global version to: ", version);

        _version.set(version);
      }
    });
  }

  @Override
  public boolean isListeningToCluster(String clusterName)
  {
    return _clusterSubscriber.isListeningToProperty(clusterName);
  }

  @Override
  public boolean isListeningToService(String serviceName)
  {
    return _serviceSubscriber.isListeningToProperty(serviceName);
  }

  public long getDelayedExecution()
  {
    return _delayedExecution;
  }

  public void setDelayedExecution(long delayedExecution)
  {
    _delayedExecution = delayedExecution;
  }

  @Override
  public TrackerClient getClient(String serviceName, URI uri)
  {
    Map<URI, TrackerClient> trackerClients = _trackerClients.get(serviceName);
    TrackerClient trackerClient = null;

    if (trackerClients != null)
    {
      trackerClient = trackerClients.get(uri);
    }
    else
    {
      warn(_log, "get client called on unknown service ", serviceName, ": ", uri);
    }

    return trackerClient;
  }

  public List<URI> getServerUrisForServiceName(String clusterName)
  {
    Map<URI, TrackerClient> trackerClients = _trackerClients.get(clusterName);
    if (trackerClients == null)
    {
      return Collections.emptyList();
    }
    else
    {
      return new ArrayList<URI>(trackerClients.keySet());
    }
  }

  @Override
  public TransportClient getClient(String serviceName, String scheme)
  {
    Map<String, TransportClient> transportClients = _serviceClients.get(serviceName);
    TransportClient transportClient = null;

    if (transportClients != null)
    {
      transportClient = transportClients.get(scheme.toLowerCase());
      if (transportClient == null)
      {
        warn(_log, "no generic transport client for service " + serviceName +
                " and scheme: " + scheme);
      }
    }
    else
    {
      warn(_log, "get client called on unknown service ", serviceName);
    }
    return transportClient;
  }

  @Override
  public LoadBalancerStrategy getStrategy(String serviceName, String scheme)
  {
    Map<String, LoadBalancerStrategy> strategies = _serviceStrategies.get(serviceName);
    LoadBalancerStrategy strategy = null;

    if (strategies != null)
    {
      strategy = strategies.get(scheme);
    }
    else
    {
      warn(_log, "get strategy called on unknown service ", serviceName);
    }

    return strategy;
  }

  @Override
  public List<SchemeStrategyPair> getStrategiesForService(String serviceName,
                                                           List<String> prioritizedSchemes)
  {
    List<SchemeStrategyPair> cached = _serviceStrategiesCache.get(serviceName);
    if ((cached != null) && !cached.isEmpty())
    {
      return cached;
    }
    else
    {

      List<SchemeStrategyPair> orderedStrategies = new ArrayList<SchemeStrategyPair>(prioritizedSchemes.size());
      for (String scheme : prioritizedSchemes)
      {
        // get the strategy for this service and scheme
        LoadBalancerStrategy strategy = getStrategy(serviceName, scheme);

        if (strategy != null)
        {
          orderedStrategies.add(new SchemeStrategyPair(scheme, strategy));
        }
        else
        {
          warn(_log,
               "unable to find a load balancer strategy for ",
               serviceName,
               " with scheme: ",
               scheme);
        }
      }
      _serviceStrategiesCache.put(serviceName, orderedStrategies);
      return orderedStrategies;
    }
  }

  @Override
  public TransportClientFactory getClientFactory(String scheme)
  {
    return _clientFactories.get(scheme);
  }

  public abstract class AbstractLoadBalancerSubscriber<T> implements
      PropertyEventSubscriber<T>
  {
    private final String                                                                  _name;
    private final int                                                                     _type;
    private final PropertyEventBus<T>                                                     _eventBus;
    private final ConcurrentMap<String, ClosableQueue<LoadBalancerStateListenerCallback>> _waiters =
                                                                                                       new ConcurrentHashMap<String, ClosableQueue<LoadBalancerStateListenerCallback>>();

    public AbstractLoadBalancerSubscriber(int type, PropertyEventBus<T> eventBus)
    {
      _name = this.getClass().getSimpleName();
      _type = type;
      _eventBus = eventBus;
    }

    public boolean isListeningToProperty(String propertyName)
    {
      ClosableQueue<LoadBalancerStateListenerCallback> waiters =
          _waiters.get(propertyName);
      return waiters != null && waiters.isClosed();
    }

    public int propertyListenCount()
    {
      return _waiters.size();
    }

    public void ensureListening(String propertyName,
                                LoadBalancerStateListenerCallback callback)
    {
      ClosableQueue<LoadBalancerStateListenerCallback> waiters =
          _waiters.get(propertyName);
      boolean register = false;
      if (waiters == null)
      {
        waiters = new ClosableQueue<LoadBalancerStateListenerCallback>();
        ClosableQueue<LoadBalancerStateListenerCallback> previous =
            _waiters.putIfAbsent(propertyName, waiters);
        if (previous == null)
        {
          // We are the very first to register
          register = true;
        }
        else
        {
          // Someone else beat us to it
          waiters = previous;
        }
      }
      // Ensure the callback is enqueued before registering with the bus
      if (!waiters.offer(callback))
      {
        callback.done(_type, propertyName);
      }
      if (register)
      {
        _eventBus.register(Collections.singleton(propertyName), this);
      }
    }

    @Override
    public void onAdd(final String propertyName, final T propertyValue)
    {
      trace(_log, _name, ".onAdd: ", propertyName, ": ", propertyValue);

      handlePut(propertyName, propertyValue);

      // if bad properties are received, then onInitialize()::handlePut might throw an exception and
      // the queue might not be closed. If the queue is not closed, then even if the underlying
      // problem with the properties is fixed and handlePut succeeds, new callbacks will be added
      // to the queue (in ensureListening) but never be triggered. We will attempt to close the
      // queue here if needed, and trigger any callbacks on that queue. If the queue is already
      // closed, it will return an empty list.
      List<LoadBalancerStateListenerCallback> queueList = _waiters.get(propertyName).ensureClosed();
      if (queueList != null)
      {
        for (LoadBalancerStateListenerCallback waiter : queueList)
        {
          waiter.done(_type, propertyName);
        }
      }
    }

    @Override
    public void onInitialize(final String propertyName, final T propertyValue)
    {
      trace(_log, _name, ".onInitialize: ", propertyName, ": ", propertyValue);

      handlePut(propertyName, propertyValue);

      for (LoadBalancerStateListenerCallback waiter : _waiters.get(propertyName).close())
      {
        waiter.done(_type, propertyName);
      }
    }

    @Override
    public void onRemove(final String propertyName)
    {
      trace(_log, _name, ".onRemove: ", propertyName);

      handleRemove(propertyName);

      // if we are removing this property, ensure that its corresponding queue is closed and
      // remove it's entry from _waiters. We are invoking down on the callbacks to indicate we
      // heard back from zookeeper, and that the callers can proceed (even if they subsequently get
      // a ServiceUnavailableException)
      List<LoadBalancerStateListenerCallback> queueList = _waiters.get(propertyName).ensureClosed();
      if (queueList != null)
      {
        for (LoadBalancerStateListenerCallback waiter : queueList)
        {
          waiter.done(_type, propertyName);
        }
      }
    }

    protected abstract void handlePut(String propertyName, T propertyValue);

    protected abstract void handleRemove(String name);
  }

  public class UriLoadBalancerSubscriber extends
      AbstractLoadBalancerSubscriber<UriProperties>
  {
    public UriLoadBalancerSubscriber(PropertyEventBus<UriProperties> uPropertyEventBus)
    {
      super(LoadBalancerStateListenerCallback.CLUSTER, uPropertyEventBus);
    }

    @Override
    protected void handlePut(final String listenTo, final UriProperties discoveryProperties)
    {
      // add tracker clients for uris that we aren't already tracking
      if (discoveryProperties != null)
      {
        String clusterName = discoveryProperties.getClusterName();

        Set<String> serviceNames = _servicesPerCluster.get(clusterName);
        //updates all the services that these uris provide
        if (serviceNames != null)
        {
          for (String serviceName : serviceNames)
          {
            Map<URI, TrackerClient> trackerClients =
                        _trackerClients.get(serviceName);
            if (trackerClients == null)
            {
              trackerClients = new ConcurrentHashMap<URI, TrackerClient>();
              _trackerClients.put(serviceName, trackerClients);
            }
            LoadBalancerStateItem<ServiceProperties> serviceProperties = _serviceProperties.get(serviceName);
            DegraderImpl.Config config = null;

            if (serviceProperties == null || serviceProperties.getProperty() == null ||
                serviceProperties.getProperty().getDegraderProperties() == null)
            {
              debug(_log, "trying to see if there's a special degraderImpl properties but serviceInfo is null " +
                            "for serviceName = " + serviceName + " so we'll set config to default");
            }
            else
            {
              Map<String, String> degraderImplProperties =
                  serviceProperties.getProperty().getDegraderProperties();
              config = DegraderConfigFactory.toDegraderConfig(degraderImplProperties);
            }
            long trackerClientInterval = getTrackerClientInterval (serviceProperties.getProperty());
            for (URI uri : discoveryProperties.Uris())
            {
              Map<Integer, PartitionData> partitionDataMap = discoveryProperties.getPartitionDataMap(uri);
              TrackerClient client = trackerClients.get(uri);
              if (client == null || !client.getParttitionDataMap().equals(partitionDataMap))
              {
                 client = getTrackerClient(serviceName,
                    uri,
                    partitionDataMap,
                    config,
                    trackerClientInterval);

                if (client != null)
                {
                  info(_log,
                       "adding new tracker client from updated uri properties: ",
                       client);

                  // notify listeners of the added client
                  for (SimpleLoadBalancerStateListener listener : _listeners)
                  {
                    listener.onClientAdded(serviceName, client);
                  }

                  trackerClients.put(uri, client);
                }
              }
            }
          }
        }

      }

      // replace the URI properties
      _uriProperties.put(listenTo,
                         new LoadBalancerStateItem<UriProperties>(discoveryProperties,
                                                                  _version.incrementAndGet(),
                                                                  System.currentTimeMillis()));

      // now remove URIs that we're tracking, but have been removed from the new uri
      // properties
      if (discoveryProperties != null)
      {
        Set<String> serviceNames = _servicesPerCluster.get(discoveryProperties.getClusterName());
        if (serviceNames != null)
        {
          for (String serviceName : serviceNames)
          {
            Map<URI, TrackerClient> trackerClients = _trackerClients.get(serviceName);
            if (trackerClients != null)
            {
              for (Iterator<URI> it = trackerClients.keySet().iterator(); it.hasNext();)
              {
                URI uri = it.next();

                if (!discoveryProperties.Uris().contains(uri))
                {
                  TrackerClient client = trackerClients.remove(uri);

                  info(_log, "removing dead tracker client: ", client);

                  // notify listeners of the removed client
                  for (SimpleLoadBalancerStateListener listener : _listeners)
                  {
                    listener.onClientRemoved(serviceName, client);
                  }
                  // We don't shut down the dead TrackerClient, because TrackerClients hold no
                  // resources and simply point to the common cluster client (from _serviceeClients).
                }
              }
            }
          }
        }
      }
      else
      {
        // uri properties was null, we'll just log the event and continues.
        // The reasoning is we might receive a null event when there's a problem
        // writing/reading cache file.
        warn(_log, "received a null uri properties for cluster: ", listenTo);
      }
    }

    @Override
    protected void handleRemove(final String listenTo)
    {
      _uriProperties.remove(listenTo);
      warn(_log, "received a uri properties event remove() for cluster: ", listenTo);
      removeTrackerClients(listenTo);
    }
  }

  private void removeTrackerClients(String clusterName)
  {
    // uri properties was null, so remove all tracker clients
    warn(_log, "removing all tracker clients for cluster: ", clusterName);
    Set<String> serviceNames = _servicesPerCluster.get(clusterName);
    if (serviceNames != null)
    {
      for (String serviceName : serviceNames)
      {
        Map<URI, TrackerClient> clients = _trackerClients.remove(serviceName);

        if (clients != null)
        {
          for (TrackerClient client : clients.values())
          {
            // notify listeners of the removed client
            for (SimpleLoadBalancerStateListener listener : _listeners)
            {
              listener.onClientRemoved(serviceName, client);
            }
          }
        }
      }
    }
  }

  public class ClusterLoadBalancerSubscriber extends
      AbstractLoadBalancerSubscriber<ClusterProperties>
  {

    public ClusterLoadBalancerSubscriber(PropertyEventBus<ClusterProperties> cPropertyEventBus)
    {
      super(LoadBalancerStateListenerCallback.CLUSTER, cPropertyEventBus);
    }

    @Override
    protected void handlePut(final String listenTo, final ClusterProperties discoveryProperties)
    {
      if (discoveryProperties != null)
      {
        _clusterInfo.put(listenTo, new ClusterInfoItem(discoveryProperties,
            PartitionAccessorFactory.getPartitionAccessor(discoveryProperties.getPartitionProperties())));
      }
      else
      {
        // still insert the ClusterInfoItem when discoveryProperties is null, but don't create accessor
        _clusterInfo.put(listenTo, new ClusterInfoItem(discoveryProperties, null));
      }
    }

    @Override
    protected void handleRemove(final String listenTo)
    {
      _clusterInfo.remove(listenTo);
    }
  }

  public class ServiceLoadBalancerSubscriber extends
      AbstractLoadBalancerSubscriber<ServiceProperties>
  {
    public ServiceLoadBalancerSubscriber(PropertyEventBus<ServiceProperties> eventBus)
    {
      super(LoadBalancerStateListenerCallback.SERVICE, eventBus);
    }

    @Override
    protected void handlePut(final String listenTo, final ServiceProperties discoveryProperties)
    {
      LoadBalancerStateItem<ServiceProperties> oldServicePropertiesItem =
          _serviceProperties.get(listenTo);

      _serviceProperties.put(listenTo,
                             new LoadBalancerStateItem<ServiceProperties>(discoveryProperties,
                                                                          _version.incrementAndGet(),
                                                                          System.currentTimeMillis()));

      // always refresh strategies when we receive service event
      if (discoveryProperties != null)
      {
        //if this service changes its cluster, we should update the cluster -> service map saying that
        //this service is no longer hosted in the old cluster.
        if (oldServicePropertiesItem != null)
        {
          ServiceProperties oldServiceProperties = oldServicePropertiesItem.getProperty();
          if (oldServiceProperties != null && oldServiceProperties.getClusterName() != null &&
              !oldServiceProperties.getClusterName().equals(discoveryProperties.getClusterName()))
          {
            Set<String> serviceNames =
                          _servicesPerCluster.get(oldServiceProperties.getClusterName());
            if (serviceNames != null)
            {
              serviceNames.remove(oldServiceProperties.getServiceName());
            }
          }
        }

        refreshServiceStrategies(discoveryProperties);
        refreshTransportClientsPerService(discoveryProperties);

        // refresh state for which services are on which clusters
        Set<String> serviceNames =
            _servicesPerCluster.get(discoveryProperties.getClusterName());

        if (serviceNames == null)
        {
          serviceNames =
              Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
          _servicesPerCluster.put(discoveryProperties.getClusterName(), serviceNames);
        }

        serviceNames.add(discoveryProperties.getServiceName());
      }
      else if (oldServicePropertiesItem != null)
      {
        // if we've replaced a service properties with null, update the cluster ->
        // service state that the service is no longer on its cluster.
        ServiceProperties oldServiceProperties = oldServicePropertiesItem.getProperty();

        if (oldServiceProperties != null)
        {
          Set<String> serviceNames =
              _servicesPerCluster.get(oldServiceProperties.getClusterName());

          if (serviceNames != null)
          {
            serviceNames.remove(oldServiceProperties.getServiceName());
          }
        }
      }

      if (discoveryProperties == null)
      {
        // we'll just ignore the event and move on.
        // we could receive a null if the file store properties cannot read/write a file.
        // in this case it's better to leave the state intact and not do anything
        _log.warn("We receive a null service properties for {}. ", listenTo);
      }
    }

    @Override
    protected void handleRemove(final String listenTo)
    {
      _log.warn("Received a service properties event to remove() for service = " + listenTo);
      LoadBalancerStateItem<ServiceProperties> serviceItem =
          _serviceProperties.remove(listenTo);

      if (serviceItem != null && serviceItem.getProperty() != null)
      {
        ServiceProperties serviceProperties = serviceItem.getProperty();

        // remove this service from the cluster -> services map
        Set<String> serviceNames =
            _servicesPerCluster.get(serviceProperties.getClusterName());

        if (serviceNames != null)
        {
          serviceNames.remove(serviceProperties.getServiceName());
        }

        shutdownClients(listenTo);

      }
    }
  }

  private TrackerClient getTrackerClient(String serviceName, URI uri, Map<Integer, PartitionData> partitionDataMap,
                                         DegraderImpl.Config config, long callTrackerInterval)
  {
    Map<String,TransportClient> clientsByScheme = _serviceClients.get(serviceName);
    if (clientsByScheme == null)
    {
      _log.error("getTrackerClient: unknown service name {} for URI {} and partitionDataMap {}",
          new Object[]{ serviceName, uri, partitionDataMap });
      return null;
    }
    TransportClient client = clientsByScheme.get(uri.getScheme().toLowerCase());
    if (client == null)
    {
      _log.error("getTrackerClient: invalid scheme for service {}, URI {} and partitionDataMap {}",
          new Object[]{ serviceName, uri, partitionDataMap });
      return null;
    }
    TrackerClient trackerClient = new TrackerClient(uri, partitionDataMap, client, SystemClock.instance(), config,
                                                    callTrackerInterval);
    return trackerClient;
  }

  private Map<String, TransportClient> createAndInsertTransportClientTo(ServiceProperties serviceProperties)
  {
    Map<String, Object> transportClientProperties = new HashMap<String,Object>(serviceProperties.getTransportClientProperties());

    Object allowedClientOverrideKeysObj = transportClientProperties.remove(PropertyKeys.ALLOWED_CLIENT_OVERRIDE_KEYS);
    Set<String> allowedClientOverrideKeys = new HashSet<String>(ConfigValueExtractor.buildList(allowedClientOverrideKeysObj, LIST_SEPARATOR));

    Map<String, Object> clientSuppliedServiceProperties = _clientServicesConfig.get(serviceProperties.getServiceName());
    if (clientSuppliedServiceProperties != null)
    {
      debug(_log, "Client supplied configs for service {}", new Object[]{serviceProperties.getServiceName()});

      // check for overrides
      for (String clientSuppliedKey: clientSuppliedServiceProperties.keySet())
      {
        // clients can only override config properties which have been allowed by the service
        if (allowedClientOverrideKeys.contains(clientSuppliedKey))
        {
          if (ClientServiceConfigValidator.isValidValue(transportClientProperties,
                                                        clientSuppliedServiceProperties,
                                                        clientSuppliedKey))
          {
            transportClientProperties.put(clientSuppliedKey, clientSuppliedServiceProperties.get(clientSuppliedKey));
            info(_log,
                 "Client overrode config property {} for service {}. This is being used to instantiate the Transport Client",
                 new Object[]{clientSuppliedKey, serviceProperties.getServiceName()});
          }
          else
          {
            warn(_log,
                 "Client supplied config property {} with an invalid value {} for service {}",
                 new Object[]{clientSuppliedKey,
                     clientSuppliedServiceProperties.get(clientSuppliedKey),
                     serviceProperties.getServiceName()});
          }
        }
      }
    }
    List<String> schemes = serviceProperties.getPrioritizedSchemes();
    Map<String,TransportClient> newTransportClients = new HashMap<String, TransportClient>();
    if (schemes != null && !schemes.isEmpty())
    {
      for (String scheme : schemes)
      {
        TransportClientFactory factory = _clientFactories.get(scheme);

        if ("https".equals(scheme))
        {
          if (_isSSLEnabled)
          {
            // if https is a prioritized scheme and SSL is enabled, then a SSLContext and SSLParameters
            // should have been passed in during creation.
            if (_sslContext != null && _sslParameters != null)
            {
              transportClientProperties.put(HttpClientFactory.HTTP_SSL_CONTEXT, _sslContext);
              transportClientProperties.put(HttpClientFactory.HTTP_SSL_PARAMS, _sslParameters);
            }
            else
            {
              _log.error("https specified as a prioritized scheme for service: " + serviceProperties.getServiceName() +
                        " but no SSLContext or SSLParameters have been configured.");
              throw new IllegalStateException("SSL enabled but required SSLContext and SSLParameters" +
                                                "were not both present.");
            }
          }
          else
          {
            // don't create this transport client if ssl isn't enabled. If the https transport client
            // is requested later on, getTrackerClient will catch this situation and log an error.
            continue;
          }
        }

        if (factory != null)
        {
          transportClientProperties.put(HttpClientFactory.HTTP_SERVICE_NAME, serviceProperties.getServiceName());
          TransportClient client = factory.getClient(transportClientProperties);
          newTransportClients.put(scheme.toLowerCase(), client);
        }
        else
        {
          _log.warn("Failed to find client factory for scheme {}", scheme);
        }
      }
    }
    else
    {
      _log.warn("Prioritized schemes is null for service properties = " + serviceProperties.getServiceName());
    }
    return newTransportClients;
  }

  private static long getTrackerClientInterval(ServiceProperties serviceProperties)
  {
    long trackerClientInterval = DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS;
    if (serviceProperties.getLoadBalancerStrategyProperties() != null)
    {
      trackerClientInterval = MapUtil.getWithDefault(serviceProperties.getLoadBalancerStrategyProperties(),
                             PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS,
                             DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS,
                             Long.class);
    }
    return trackerClientInterval;
  }

  void refreshTransportClientsPerService(ServiceProperties serviceProperties)
  {
    String serviceName = serviceProperties.getServiceName();
    //create new TransportClients
    Map<String,TransportClient> newTransportClients =  createAndInsertTransportClientTo(serviceProperties);

    // clients-by-scheme map is never edited, only replaced.
    newTransportClients = Collections.unmodifiableMap(newTransportClients);

    final Map<String, TransportClient> oldTransportClients = _serviceClients.put(serviceName, newTransportClients);

    // gets the information for configuring the parameter for how DegraderImpl should behave for
    // each tracker clients that we instantiate here. If there's no such information, then we'll instantiate
    // each tracker clients with default configuration
    DegraderImpl.Config config = null;
    if (serviceProperties.getDegraderProperties() != null && !serviceProperties.getDegraderProperties().isEmpty())
    {
      config = DegraderConfigFactory.toDegraderConfig(serviceProperties.getDegraderProperties());
    }
    else
    {
      debug(_log, "trying to see if there's a special degraderImpl properties but serviceInfo.getDegraderImpl() is null"
          + " for service name = " + serviceName + " so we'll set config to default");
    }

    Map<URI,TrackerClient> newTrackerClients;

    // update all tracker clients to use new configs
    LoadBalancerStateItem<UriProperties> uriItem = _uriProperties.get(serviceProperties.getClusterName());
    UriProperties uriProperties = uriItem == null ? null : uriItem.getProperty();
    if (uriProperties != null)
    {
      Set<URI> uris = uriProperties.Uris();
      // clients-by-uri map may be edited later by UriPropertiesListener.handlePut
      newTrackerClients = new ConcurrentHashMap<URI, TrackerClient>(
          CollectionUtils.getMapInitialCapacity(uris.size(), 0.75f), 0.75f, 1);
      long trackerClientInterval = getTrackerClientInterval (serviceProperties);
      for (URI uri : uris)
      {
        TrackerClient trackerClient = getTrackerClient(serviceName, uri, uriProperties.getPartitionDataMap(uri),
                                                       config, trackerClientInterval);
        if (trackerClient != null)
        {
          newTrackerClients.put(uri, trackerClient);
        }
      }
    }
    else
    {
      // clients-by-uri map may be edited later by UriPropertiesListener.handlePut
      newTrackerClients = new ConcurrentHashMap<URI, TrackerClient>(16, 0.75f, 1);
    }

    //override the oldTrackerClients with newTrackerClients
    _trackerClients.put(serviceName, newTrackerClients);
    // No need to shut down oldTrackerClients, because they all point directly to the TransportClient for the service
    // We do need to shut down the old transport clients
    shutdownTransportClients(oldTransportClients, serviceName);
  }

  private void shutdownClients(String serviceName)
  {
    _log.warn("shutting down all tracker clients and transport clients for service " + serviceName);

    //We need to remove all the tracker clients owned by this service. We don't need to shutdown
    //because trackerClient is just a wrapper of transport client which we'll shutdown next.
    Map<URI, TrackerClient> clients = _trackerClients.remove(serviceName);

    if (clients != null)
    {
      for (TrackerClient client : clients.values())
      {
        // notify listeners of the removed client
        for (SimpleLoadBalancerStateListener listener : _listeners)
        {
          listener.onClientRemoved(serviceName, client);
        }
      }
    }

    //we also need to shutdown the transport client owned by this service
    Map<String, TransportClient> schemeToTransportClients = _serviceClients.get(serviceName);
    shutdownTransportClients(schemeToTransportClients, serviceName);
  }

  private void shutdownTransportClients(final Map<String, TransportClient> schemeToTransportClients,
                                        final String serviceName)
  {
    // There is a concurrency edge case that we should handle here by delaying the shutdown.
    // Let's say there's a request to getClient() at the same time as new event coming to handlePut()
    // Thread 1                                                   Thread 2
    // _client = _loadBalancerState.getClient()
    //                                                            handlePut() is called
    //                                                            shutdown all transportClients
    // _client.sendRequest()
    //
    // ERROR because thread 1 sendRequest is sending a request to client that's being shutdown by thread 2
    // So if we introduce a delay before shutting down the old client, thread 1 sendRequest() will happen
    // after the call to getClient() so we won't have this problem.
    if (schemeToTransportClients != null)
    {
      _executor.schedule(new Runnable()
      {
        @Override
        public void run()
        {
          for (final Map.Entry<String, TransportClient> entry : schemeToTransportClients.entrySet())
          {
            Callback<None> callback = new Callback<None>()
            {
              @Override
              public void onError(Throwable e)
              {
                _log.warn("Failed to shut down old " + serviceName + " TransportClient with scheme = " + entry.getKey()
                    , e);
              }

              @Override
              public void onSuccess(None result)
              {
                _log.info("Shut down old " + serviceName + " TransportClient with scheme = " + entry.getKey());
              }
            };
            entry.getValue().shutdown(callback);
          }
        }
      }, _delayedExecution, TimeUnit.MILLISECONDS);
    }
  }

  void refreshServiceStrategies(ServiceProperties serviceProperties)
  {
    info(_log, "refreshing service strategies for service: ", serviceProperties);
    List<String> strategyList = serviceProperties.getLoadBalancerStrategyList();
    LoadBalancerStrategyFactory<? extends LoadBalancerStrategy> factory = null;
    if (strategyList != null && !strategyList.isEmpty())
    {
      // In this prioritized strategy list, pick the first one that is available. This is needed
      // so that a new strategy can be used as it becomes available in the client, rather than
      // waiting for all clients to update their code level before any clients can use it.
      for (String strategy : strategyList)
      {
        factory = _loadBalancerStrategyFactories.get(strategy);
        if (factory != null)
        {
          break;
        }
      }
    }
    // if we get here without a factory, then something might be wrong, there should always
    // be at least a default strategy in the list that is always available.
    // The intent is that the loadBalancerStrategyName will be replaced by the
    // loadBalancerStrategyList, and eventually the StrategyName will be removed from the code.
    // We don't issue a RuntimeException here because it's possible, when adding services (ie publishAdd),
    // to refreshServiceStrategies without the strategy existing yet.
    if (factory == null)
    {
      warn(_log,"No valid strategy found. ", serviceProperties);
    }

    Map<String, LoadBalancerStrategy> strategyMap = new ConcurrentHashMap<String, LoadBalancerStrategy>();

    if (factory != null && serviceProperties.getPrioritizedSchemes() != null &&
        !serviceProperties.getPrioritizedSchemes().isEmpty())
    {
      List<String> schemes = serviceProperties.getPrioritizedSchemes();
      for (String scheme : schemes)
      {
        Map<String, Object> loadBalancerStrategyProperties =
            new HashMap<String, Object>(serviceProperties.getLoadBalancerStrategyProperties());

        LoadBalancerStrategy strategy = factory.newLoadBalancer(
            serviceProperties.getServiceName(),
            loadBalancerStrategyProperties,
            serviceProperties.getDegraderProperties());

        strategyMap.put(scheme, strategy);
      }
    }
    else
    {
        warn(_log,
             "unable to find cluster or factory for ",
             serviceProperties,
             ": ",
             factory);

    }

    Map<String, LoadBalancerStrategy> oldStrategies =
        _serviceStrategies.put(serviceProperties.getServiceName(), strategyMap);
    _serviceStrategiesCache.remove(serviceProperties.getServiceName());

    info(_log,
         "removing strategies ",
         serviceProperties.getServiceName(),
         ": ",
         oldStrategies);

    info(_log,
         "putting strategies ",
         serviceProperties.getServiceName(),
         ": ",
         strategyMap);

    // notify listeners of the removed strategy
    if (oldStrategies != null)
    {
      for (SimpleLoadBalancerStateListener listener : _listeners)
      {
        for (Map.Entry<String, LoadBalancerStrategy> oldStrategy : oldStrategies.entrySet())
        {
          listener.onStrategyRemoved(serviceProperties.getServiceName(),
                                     oldStrategy.getKey(),
                                     oldStrategy.getValue());
        }

      }
    }

    // we need to inform the listeners of the strategy removal before the strategy add, otherwise
    // they will get confused and remove what was just added.
    if (!strategyMap.isEmpty())
    {
      for (SimpleLoadBalancerStateListener listener : _listeners)
      {
        // notify listeners of the added strategy
        for (Map.Entry<String, LoadBalancerStrategy> newStrategy : strategyMap.entrySet())
        {
          listener.onStrategyAdded(serviceProperties.getServiceName(),
                                   newStrategy.getKey(),
                                   newStrategy.getValue());
        }
      }
    }
  }

  public interface SimpleLoadBalancerStateListener
  {
    void onStrategyAdded(String serviceName, String scheme, LoadBalancerStrategy strategy);

    void onStrategyRemoved(String serviceName,
                           String scheme,
                           LoadBalancerStrategy strategy);

    void onClientAdded(String serviceName, TrackerClient client);

    void onClientRemoved(String serviceName, TrackerClient client);
  }

}
