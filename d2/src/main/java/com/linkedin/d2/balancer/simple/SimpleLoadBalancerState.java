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

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.Callbacks;
import com.linkedin.common.callback.SimpleCallback;
import com.linkedin.common.util.MapUtil;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.LoadBalancerClusterListener;
import com.linkedin.d2.balancer.LoadBalancerState;
import com.linkedin.d2.balancer.LoadBalancerStateItem;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.strategies.degrader.DegraderConfigFactory;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyConfig;
import com.linkedin.d2.balancer.util.ClientFactoryProvider;
import com.linkedin.d2.balancer.util.LoadBalancerUtil;
import com.linkedin.d2.balancer.util.RateLimitedLogger;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessor;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessorRegistry;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessorRegistryImpl;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import com.linkedin.d2.discovery.event.PropertyEventBusImpl;
import com.linkedin.d2.discovery.event.PropertyEventPublisher;
import com.linkedin.d2.discovery.event.PropertyEventThread.PropertyEvent;
import com.linkedin.d2.discovery.event.PropertyEventThread.PropertyEventShutdownCallback;
import com.linkedin.internal.common.util.CollectionUtils;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SystemClock;
import com.linkedin.util.degrader.DegraderImpl;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.d2.discovery.util.LogUtil.debug;
import static com.linkedin.d2.discovery.util.LogUtil.info;
import static com.linkedin.d2.discovery.util.LogUtil.trace;
import static com.linkedin.d2.discovery.util.LogUtil.warn;

public class SimpleLoadBalancerState implements LoadBalancerState, ClientFactoryProvider
{
  private static final Logger                                                            _log =
                                                                                                  LoggerFactory.getLogger(SimpleLoadBalancerState.class);
  private static final int DEGRADER_RATELIMITEDLOG_RATE_MS = 20000;

  private final UriLoadBalancerSubscriber _uriSubscriber;
  private final ClusterLoadBalancerSubscriber _clusterSubscriber;
  private final ServiceLoadBalancerSubscriber _serviceSubscriber;

  private final PropertyEventBus<UriProperties> _uriBus;
  private final PropertyEventBus<ClusterProperties> _clusterBus;
  private final PropertyEventBus<ServiceProperties> _serviceBus;

  private final Map<String, LoadBalancerStateItem<UriProperties>>                        _uriProperties;
  private final Map<String, ClusterInfoItem>                                             _clusterInfo;
  private final Map<String, LoadBalancerStateItem<ServiceProperties>>                    _serviceProperties;

  private final AtomicLong                                                               _version;

  private final Map<String, Set<String>>                                                 _servicesPerCluster;

  /**
   * Single-threaded executor service intended to execute non-blocking calls only
   */
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

  /**
   * List of listeners that want to be notified when cluster changes happen.
   */
  private final List<LoadBalancerClusterListener>                                    _clusterListeners;

  private final SSLContext    _sslContext;
  private final SSLParameters _sslParameters;
  private final boolean       _isSSLEnabled;


  private final SslSessionValidatorFactory _sslSessionValidatorFactory;

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
         Collections.<String, Map<String, Object>>emptyMap(),
         new PartitionAccessorRegistryImpl(),
         validationStrings -> null);
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
         new PartitionAccessorRegistryImpl(),
         validationStrings -> null);
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
                                 Map<String, Map<String, Object>> clientServicesConfig,
                                 PartitionAccessorRegistry partitionAccessorRegistry,
                                 SslSessionValidatorFactory sessionValidatorFactory)
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
      partitionAccessorRegistry,
      sessionValidatorFactory);
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
                                 PartitionAccessorRegistry partitionAccessorRegistry,
                                 SslSessionValidatorFactory sessionValidatorFactory)
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
    _uriSubscriber = new UriLoadBalancerSubscriber(uriBus, this);

    _clusterBus = clusterBus;
    _clusterSubscriber = new ClusterLoadBalancerSubscriber(this, clusterBus, partitionAccessorRegistry);

    _serviceBus = serviceBus;
    _serviceSubscriber = new ServiceLoadBalancerSubscriber(serviceBus, this);

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
    _sslSessionValidatorFactory = sessionValidatorFactory;
    _clusterListeners =
      Collections.synchronizedList(new ArrayList<LoadBalancerClusterListener>());
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
  public void registerClusterListener(final LoadBalancerClusterListener listener)
  {
    trace(_log, "register listener: ", listener);

    _executor.execute(new PropertyEvent("add cluster listener for state")
    {
      @Override
      public void innerRun()
      {
        _clusterListeners.add(listener);
      }
    });
  }

  @Override
  public void unregisterClusterListener(final LoadBalancerClusterListener listener)
  {
    trace(_log, "unregister listener: ", listener);

    _executor.execute(new PropertyEvent("remove cluster listener for state")
    {
      @Override
      public void innerRun()
      {
        _clusterListeners.remove(listener);
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
        // Need to shutdown loadBalancerStrategies before the transportClients are shutdown
        for (Map<String, LoadBalancerStrategy> strategyEntry : _serviceStrategies.values())
        {
          strategyEntry.values().forEach(LoadBalancerStrategy::shutdown);
        }

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

        // When SimpleLoadBalancerState is shutdown, all the strategies and clients are effectively removed,
        // so it is needed to notify all the listeners
        for (SimpleLoadBalancerStateListener listener : _listeners)
        {
          // Notify the strategy removal
          for (Map.Entry<String, Map<String, LoadBalancerStrategy>> serviceStrategy : _serviceStrategies.entrySet())
          {
            for (Map.Entry<String, LoadBalancerStrategy> strategyEntry : serviceStrategy.getValue().entrySet())
            {
              listener.onStrategyRemoved(serviceStrategy.getKey(), strategyEntry.getKey(), strategyEntry.getValue());
            }

            // Also notify the client removal
            Map<URI, TrackerClient> trackerClients = _trackerClients.get(serviceStrategy.getKey());
            if (trackerClients != null)
            {
              for (TrackerClient client : trackerClients.values())
              {
                listener.onClientRemoved(serviceStrategy.getKey(), client);
              }
            }
          }
        }

        // When SimpleLoadBalancerStateis shutdown, all the cluster listener also need to be notified.
        for (LoadBalancerClusterListener clusterListener : _clusterListeners)
        {
          for (String clusterName : _clusterInfo.keySet())
          {
            clusterListener.onClusterRemoved(clusterName);
          }
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

  List<SimpleLoadBalancerStateListener> getListeners()
  {
    return _listeners;
  }

  Map<String, Set<String>> getServicesPerCluster()
  {
    return _servicesPerCluster;
  }

  Map<String, Map<URI, TrackerClient>> getTrackerClients()
  {
    return _trackerClients;
  }

  Map<String, LoadBalancerStateItem<UriProperties>> getUriProperties()
  {
    return _uriProperties;
  }

  Map<String, ClusterInfoItem> getClusterInfo()
  {
    return _clusterInfo;
  }

  public Map<String, LoadBalancerStateItem<ServiceProperties>> getServiceProperties()
  {
    return _serviceProperties;
  }

  public long getVersion()
  {
    return _version.get();
  }

  public AtomicLong getVersionAccess()
  {
    return _version;
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
        // if this scheme is not supported (ie https not enabled) don't add it to the list
        if ("https".equals(scheme) && !_isSSLEnabled)
        {
          continue;
        }

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

  void removeTrackerClients(String clusterName)
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

  TrackerClient buildTrackerClient(String serviceName, URI uri, Map<Integer, PartitionData> partitionDataMap,
                                 DegraderImpl.Config config, Clock clk, long callTrackerInterval,
                                 String errorStatusPattern, Map<String, Object> uriSpecificProperties)
  {
    Map<String,TransportClient> clientsByScheme = _serviceClients.get(serviceName);
    if (clientsByScheme == null)
    {
      _log.error("buildTrackerClient: unknown service name {} for URI {} and partitionDataMap {}",
          new Object[]{ serviceName, uri, partitionDataMap });
      return null;
    }
    if (uri == null || uri.getScheme() == null)
    {
      _log.error("Error: could not extract scheme from URI: {}", uri);
      return null;
    }
    TransportClient client = clientsByScheme.get(uri.getScheme().toLowerCase());
    if (client == null)
    {
      // logging this at debug because there may be situations where a service may want some of its
      // clients talking https while others are ok using http.
      _log.debug("No TransportClient for scheme {}, service {}, URI {} and partitionDataMap {}. " +
                  "This client may not be configured to handle URIs in this scheme.",
            new Object[]{uri.getScheme(), serviceName, uri, partitionDataMap });
      return null;
    }
    TrackerClient trackerClient = new TrackerClient(uri, partitionDataMap, client, clk, config, callTrackerInterval,
                                                    errorStatusPattern, uriSpecificProperties);
    return trackerClient;
  }

  private Map<String, TransportClient> createAndInsertTransportClientTo(ServiceProperties serviceProperties)
  {
    Map<String, Object> transportClientProperties = new HashMap<>(serviceProperties.getTransportClientProperties());
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
              if (schemes.size() == 1)
              {
                // throw exception when https is the only scheme specified
                throw new IllegalStateException(
                    "SSL enabled but required SSLContext and SSLParameters" + "were not both present.");
              }
              // Do not create the transport client for https.
              continue;
            }
          }
          else
          {
            // don't create this transport client if ssl isn't enabled. If the https transport client
            // is requested later on, buildTrackerClient will catch this situation and log an error.
            continue;
          }
        }

        if (factory != null)
        {
          final String clusterName = serviceProperties.getClusterName();
          transportClientProperties.put(HttpClientFactory.HTTP_SERVICE_NAME, serviceProperties.getServiceName());
          transportClientProperties.put(HttpClientFactory.HTTP_POOL_STATS_NAME_PREFIX, clusterName);
          TransportClient client = _sslSessionValidatorFactory == null ? factory.getClient(transportClientProperties)
              : new ClusterAwareTransportClient(clusterName, factory.getClient(transportClientProperties),
                  getClusterInfo(), _sslSessionValidatorFactory);
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

  static long getTrackerClientInterval(ServiceProperties serviceProperties)
  {
    long trackerClientInterval = DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS;
    if (serviceProperties != null && serviceProperties.getLoadBalancerStrategyProperties() != null)
    {
      trackerClientInterval = MapUtil.getWithDefault(serviceProperties.getLoadBalancerStrategyProperties(),
                             PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS,
                             DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS,
                             Long.class);
    }
    return trackerClientInterval;
  }

  static String getErrorStatusPattern(ServiceProperties serviceProperties)
  {
    String pattern = TrackerClient.DEFAULT_ERROR_STATUS_REGEX;
    if (serviceProperties != null && serviceProperties.getLoadBalancerStrategyProperties() != null)
    {
      pattern = MapUtil.getWithDefault(serviceProperties.getLoadBalancerStrategyProperties(),
          PropertyKeys.HTTP_LB_ERROR_STATUS_REGEX, TrackerClient.DEFAULT_ERROR_STATUS_REGEX, String.class);
    }
    return pattern;
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
    Clock clk = SystemClock.instance();

    if (serviceProperties.getDegraderProperties() != null && !serviceProperties.getDegraderProperties().isEmpty())
    {
      config = DegraderConfigFactory.toDegraderConfig(serviceProperties.getDegraderProperties());
      config.setLogger(new RateLimitedLogger(_log, DEGRADER_RATELIMITEDLOG_RATE_MS, clk));
    }
    else
    {
      debug(_log, "trying to see if there's a special degraderImpl properties but serviceInfo.getDegraderImpl() is null"
          + " for service name = " + serviceName + " so we'll set config to default");
    }

    if (serviceProperties.getLoadBalancerStrategyProperties() != null)
    {
      Map<String, Object> loadBalancerStrategyProperties =
          serviceProperties.getLoadBalancerStrategyProperties();
      clk = MapUtil.getWithDefault(loadBalancerStrategyProperties, PropertyKeys.CLOCK, SystemClock.instance(), Clock.class);
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
      String errorStatusPattern = getErrorStatusPattern(serviceProperties);
      for (URI uri : uris)
      {
        TrackerClient trackerClient = buildTrackerClient(serviceName, uri, uriProperties.getPartitionDataMap(uri),
                                                       config, clk, trackerClientInterval, errorStatusPattern,
                                                       uriProperties.getUriSpecificProperties().get(uri));
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

  void shutdownClients(String serviceName)
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
      // if switching from HTTP_ONLY to HTTPS_ONLY or vice versa and the service has a high QPS,
      // it could experience ServiceUnavailable exception for a limited period of time given by the fact
      // that clients are not replaced atomically and that the request accesses shared data structures in different
      // moments in time and not atomically
      List<String> schemes = serviceProperties.getPrioritizedSchemes();
      for (String scheme : schemes)
      {
        Map<String, Object> loadBalancerStrategyProperties =
            new HashMap<String, Object>(serviceProperties.getLoadBalancerStrategyProperties());
        // Save the service path as a property -- Quarantine may need this info to construct correct
        // health checking path
        loadBalancerStrategyProperties.put(PropertyKeys.PATH, serviceProperties.getPath());
        // Also save the clusterName as a property
        loadBalancerStrategyProperties.put(PropertyKeys.CLUSTER_NAME, serviceProperties.getClusterName());

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
      // shutdown strategies before notification
      oldStrategies.values().forEach(LoadBalancerStrategy::shutdown);

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

  /**
   * ClusterLoadBalancerSubscriber will call this on handlePut
   */
  void notifyClusterListenersOnAdd(String clusterName)
  {
    for (LoadBalancerClusterListener clusterListener : _clusterListeners)
    {
      clusterListener.onClusterAdded(clusterName);
    }
  }

  /**
   * ClusterLoadBalancerSubscriber will call this on handleRemove
   */
  void notifyClusterListenersOnRemove(String clusterName)
  {
    for (LoadBalancerClusterListener clusterListener : _clusterListeners)
    {
      clusterListener.onClusterRemoved(clusterName);
    }
  }
}
