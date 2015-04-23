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

package com.linkedin.d2.balancer.zkfs;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.Directory;
import com.linkedin.d2.balancer.Facilities;
import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.LoadBalancer;
import com.linkedin.d2.balancer.LoadBalancerWithFacilities;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.util.ClientFactoryProvider;
import com.linkedin.d2.balancer.util.DirectoryProvider;
import com.linkedin.d2.balancer.util.HostToKeyMapper;
import com.linkedin.d2.balancer.util.KeyMapperProvider;
import com.linkedin.d2.balancer.util.MapKeyResult;
import com.linkedin.d2.balancer.util.TogglingLoadBalancer;
import com.linkedin.d2.balancer.util.hashing.ConsistentHashKeyMapper;
import com.linkedin.d2.balancer.util.hashing.HashRingProvider;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessor;
import com.linkedin.d2.balancer.util.partitions.PartitionInfoProvider;
import com.linkedin.d2.discovery.event.PropertyEventThread;
import com.linkedin.d2.discovery.stores.zk.ZKConnection;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.util.NamedThreadFactory;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * LoadBalancer which manages the total lifecycle of a ZooKeeper connection.  It connects to
 * ZK, switches back and forth between discovery information from ZK and backup discovery information
 * from the filesystem when temporarily disconnected from ZK, and reconnects and rebuilds state
 * if the ZK session expires due to extended disconnect.
 * @author Steven Ihde
 * @version $Revision: $
 */

public class ZKFSLoadBalancer
        implements LoadBalancerWithFacilities, DirectoryProvider, KeyMapperProvider, HashRingProvider, PartitionInfoProvider,
        ClientFactoryProvider
{
  private static final Logger LOG = LoggerFactory.getLogger(ZKFSLoadBalancer.class);

  private final String _connectString;
  private final int _sessionTimeout;
  private final int _initialZKTimeout;
  private final boolean _shutdownAsynchronously;
  private final boolean _isSymlinkAware;
  private final AtomicReference<Callback<None>> _startupCallback = new AtomicReference<Callback<None>>();
  private final TogglingLoadBalancerFactory _loadBalancerFactory;
  private final File _zkFlagFile;
  private final ZKFSDirectory _directory;
  private volatile long _delayedExecution;
  private final ScheduledExecutorService _executor;
  private final KeyMapper _keyMapper;

  /**
   * The current ZooKeeper connection.  May be in the process of starting.
   */
  private volatile ZKConnection _zkConnection;

  /**
   * The currently active LoadBalancer.  LoadBalancer will not be assigned to this field until
   * it has been sucessfully started, except the first time.
   */
  private volatile LoadBalancer _currentLoadBalancer;

  public static interface TogglingLoadBalancerFactory
  {
    TogglingLoadBalancer createLoadBalancer(ZKConnection connection, ScheduledExecutorService executorService);
  }

  /**
   *
   * @param zkConnectString Connect string listing ZK ensemble hosts in ZK format
   * @param sessionTimeout timeout (in milliseconds) of ZK session.  This controls how long
   * the session will last while connectivity between client and server is interrupted; if an
   * interruption lasts longer, the session must be recreated and state may have been lost
   * @param initialZKTimeout initial timeout for connecting to ZK; if no connection is established
   * within this time, falls back to backup stores
   * @param factory Factory configured to create appropriate ZooKeeper session-specific
   * @param zkFlagFile if non-null, the path to a File whose existence is used as a flag
   * to suppress the use of ZooKeeper stores.
   * LoadBalancer instances
   */
  public ZKFSLoadBalancer(String zkConnectString,
                          int sessionTimeout,
                          int initialZKTimeout,
                          TogglingLoadBalancerFactory factory,
                          String zkFlagFile,
                          String basePath)
  {
    this(zkConnectString, sessionTimeout, initialZKTimeout, factory, zkFlagFile, basePath, false);
  }

  /**
   *
   * @param zkConnectString Connect string listing ZK ensemble hosts in ZK format
   * @param sessionTimeout timeout (in milliseconds) of ZK session.  This controls how long
   * the session will last while connectivity between client and server is interrupted; if an
   * interruption lasts longer, the session must be recreated and state may have been lost
   * @param initialZKTimeout initial timeout for connecting to ZK; if no connection is established
   * within this time, falls back to backup stores
   * @param factory Factory configured to create appropriate ZooKeeper session-specific
   * @param zkFlagFile if non-null, the path to a File whose existence is used as a flag
   * to suppress the use of ZooKeeper stores.
   * @param shutdownAsynchronously if true, shutdown the zookeeper connection asynchronously.
   * LoadBalancer instances
   */
  public ZKFSLoadBalancer(String zkConnectString,
                          int sessionTimeout,
                          int initialZKTimeout,
                          TogglingLoadBalancerFactory factory,
                          String zkFlagFile,
                          String basePath,
                          boolean shutdownAsynchronously)
  {
    this(zkConnectString, sessionTimeout, initialZKTimeout, factory, zkFlagFile, basePath, shutdownAsynchronously, false);
  }

  /**
   *
   * @param zkConnectString Connect string listing ZK ensemble hosts in ZK format
   * @param sessionTimeout timeout (in milliseconds) of ZK session.  This controls how long
   * the session will last while connectivity between client and server is interrupted; if an
   * interruption lasts longer, the session must be recreated and state may have been lost
   * @param initialZKTimeout initial timeout for connecting to ZK; if no connection is established
   * within this time, falls back to backup stores
   * @param factory Factory configured to create appropriate ZooKeeper session-specific
   * @param zkFlagFile if non-null, the path to a File whose existence is used as a flag
   * to suppress the use of ZooKeeper stores.
   * @param shutdownAsynchronously if true, shutdown the zookeeper connection asynchronously.
   * @param isSymlinkAware if true, ZKConnection will be aware of and resolve the symbolic link for
   * any read operation.
   * LoadBalancer instances
   */
  public ZKFSLoadBalancer(String zkConnectString,
                          int sessionTimeout,
                          int initialZKTimeout,
                          TogglingLoadBalancerFactory factory,
                          String zkFlagFile,
                          String basePath,
                          boolean shutdownAsynchronously,
                          boolean isSymlinkAware)
  {
    _connectString = zkConnectString;
    _sessionTimeout = sessionTimeout;
    _initialZKTimeout = initialZKTimeout;
    _loadBalancerFactory = factory;
    if (zkFlagFile == null)
    {
      _zkFlagFile = null;
    }
    else
    {
      _zkFlagFile = new File(zkFlagFile);
    }
    _directory = new ZKFSDirectory(basePath);

    _shutdownAsynchronously = shutdownAsynchronously;
    _isSymlinkAware = isSymlinkAware;

    _executor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("D2 PropertyEventExecutor"));
    _keyMapper = new ConsistentHashKeyMapper(this, this);
    _delayedExecution = 1000;
  }

  public long getDelayedExecution()
  {
    return _delayedExecution;
  }

  public void setDelayedExecution(long milliseconds)
  {
    _delayedExecution = milliseconds;
  }

  @Override
  public TransportClient getClient(Request request, RequestContext requestContext) throws ServiceUnavailableException
  {
    return _currentLoadBalancer.getClient(request, requestContext);
  }

  @Override
  public void shutdown(final PropertyEventThread.PropertyEventShutdownCallback callback)
  {
    LOG.info("Shutting down");
    _currentLoadBalancer.shutdown(new PropertyEventThread.PropertyEventShutdownCallback()
    {
      @Override
      public void done()
      {
        try
        {
          LOG.info("Shutting down ZooKeeper connection");
          _zkConnection.shutdown();
        }
        catch (InterruptedException e)
        {
          LOG.warn("Unexpected exception during shutdown", e);
          Thread.currentThread().interrupt();
        }
        finally
        {
          LOG.info("Shutting down PropertyEvent executor");
          _executor.shutdown();
          callback.done();
        }
      }
    });
  }

  @Override
  public ServiceProperties getLoadBalancedServiceProperties(String serviceName)
      throws ServiceUnavailableException
  {
    if (_currentLoadBalancer == null)
    {
      return null;
    }
    return _currentLoadBalancer.getLoadBalancedServiceProperties(serviceName);
  }

  @Override
  public void start(final Callback<None> callback)
  {
    LOG.info("Starting ZKFSLoadBalancer");
    LOG.info("ZK connect string: {}", _connectString);
    LOG.info("ZK session timeout: {}ms", _sessionTimeout);
    LOG.info("ZK initial connect timeout: {}ms", _initialZKTimeout);
    if (_zkFlagFile == null)
    {
      LOG.info("ZK flag file not specified");
    }
    else
    {
      LOG.info("ZK flag file: {}", _zkFlagFile.getAbsolutePath());
      LOG.info("ZK currently suppressed by flag file: {}", suppressZK());
    }

    _zkConnection = new ZKConnection(_connectString, _sessionTimeout, _shutdownAsynchronously, _isSymlinkAware);
    final TogglingLoadBalancer balancer = _loadBalancerFactory.createLoadBalancer(_zkConnection, _executor);

    // _currentLoadBalancer will never be null except the first time this method is called.
    // In this case we want the not-yet-started load balancer to service client requests.  In
    // all other cases, we service requests from the old LoadBalancer until the new one is started
    if (_currentLoadBalancer == null)
    {
      _currentLoadBalancer = balancer;
    }

    Callback<None> wrapped = new Callback<None>()
    {
      @Override
      public void onSuccess(None none)
      {
        _currentLoadBalancer = balancer;
        callback.onSuccess(none);
      }

      @Override
      public void onError(Throwable e)
      {
        callback.onError(e);
      }
    };
    if (!_startupCallback.compareAndSet(null, wrapped))
    {
      throw new IllegalStateException("Startup already in progress");
    }
    _executor.execute(new PropertyEventThread.PropertyEvent("startup")
    {

      @Override
      public void innerRun()
      {
        _zkConnection.addStateListener(new ZKListener(balancer));
        try
        {
          _zkConnection.start();
        }
        catch (Exception e)
        {
          LOG.error("Failed to start ZooKeeper (bad configuration?), enabling backup stores", e);
          Callback<None> startupCallback = _startupCallback.getAndSet(null);
          // TODO this should never be null
          balancer.enableBackup(startupCallback);
          return;
        }

        LOG.info("Started ZooKeeper");
        _executor.schedule(new Runnable()
        {
          @Override
          public void run()
          {
            Callback<None> startupCallback = _startupCallback.getAndSet(null);
            if (startupCallback != null)
            {
              // Noone has enabled the stores yet either way
              LOG.error("No response from ZooKeeper within {}ms, enabling backup stores", _initialZKTimeout);
              balancer.enableBackup(startupCallback);
            }
          }
        }, _initialZKTimeout, TimeUnit.MILLISECONDS);
      }
    });
  }

  /**
   * Get a {@link Directory} associated with this load balancer's ZooKeeper connection.  The
   * directory will not operate until the load balancer is started.  The directory is
   * persistent across ZooKeeper connection expiration, just like the ZKFSLoadBalancer.
   * @return the Directory
   */
  @Override
  public Directory getDirectory()
  {
    return _directory;
  }

  @Override
  public PartitionInfoProvider getPartitionInfoProvider ()
  {
    return this;
  }

  /**
   * Get a {@link KeyMapper} associated with this load balancer's strategies.  The
   * KeyMapper will not operate until the load balancer is started.  The KeyMapper is
   * persistent across ZooKeeper connection expiration, just like the ZKFSLoadBalancer.
   * @return KeyMapper provided by this load balancer
   */
  @Override
  public KeyMapper getKeyMapper()
  {
    return _keyMapper;
  }

  @Override
  public <K> MapKeyResult<Ring<URI>, K> getRings(URI serviceUri, Iterable<K> keys) throws ServiceUnavailableException
  {
    checkLoadBalancer();
    return ((HashRingProvider)_currentLoadBalancer).getRings(serviceUri, keys);
  }

  @Override
  public Map<Integer, Ring<URI>> getRings(URI serviceUri) throws ServiceUnavailableException
  {
    checkLoadBalancer();
    return ((HashRingProvider)_currentLoadBalancer).getRings(serviceUri);
  }

  @Override
  public <K> HostToKeyMapper<K> getPartitionInformation(URI serviceUri, Collection<K> keys, int limitHostPerPartition, int hash) throws ServiceUnavailableException
  {
    checkPartitionInfoProvider();
    return ((PartitionInfoProvider)_currentLoadBalancer).getPartitionInformation(serviceUri, keys, limitHostPerPartition, hash);
  }

  @Override
  public PartitionAccessor getPartitionAccessor(URI serviceUri) throws ServiceUnavailableException
  {
    checkPartitionInfoProvider();
    return ((PartitionInfoProvider)_currentLoadBalancer).getPartitionAccessor(serviceUri);
  }

  public void checkLoadBalancer()
  {
    if (_currentLoadBalancer==null ||
        !(_currentLoadBalancer instanceof HashRingProvider))
    {
      throw new IllegalStateException("No HashRingProvider available to ZKFSLoadBalancer - this could be because the load balancer " +
          "is not yet initialized, or because it has been configured with strategies that do not support " +
          "consistent hashing.");
    }
  }

  private void checkPartitionInfoProvider()
  {
    if (_currentLoadBalancer == null || !(_currentLoadBalancer instanceof PartitionInfoProvider))
    {
      throw new IllegalStateException("No PartitionInfoProvider available to TogglingLoadBalancer - this could be because the load balancer " +
                                          "is not yet initialized, or because it has been configured with strategies that do not support " +
                                          "consistent hashing.");
    }
  }

  @Override
  public TransportClientFactory getClientFactory(String scheme)
  {
    if (_currentLoadBalancer==null ||
            !(_currentLoadBalancer instanceof ClientFactoryProvider))
    {
      throw new IllegalStateException("No ClientFactoryProvider available to ZKFSLoadBalancer - " +
                                              "this could be because the load balancer " +
                                              "is not yet initialized, or because it has been " +
                                              "configured with a LoadBalancer which does not" +
                                              "support obtaining client factories");
    }
    return ((ClientFactoryProvider)_currentLoadBalancer).getClientFactory(scheme);
  }

  /**
   * Gets the D2 facilities provided by this load balancer.
   * The facilities may only be used after the D2 layer has been initialized by calling
   * {@link ZKFSLoadBalancer}.start().  If the load balancer has not yet been initialized, method
   * calls on the objects obtained through this interface may result in RuntimeExceptions or other
   * errors as specified in each class's respective interface.
   *
   * @return Facilities provided by this load balancer
   */
  public Facilities getFacilities()
  {
    return this;
  }

  private boolean suppressZK()
  {
    return _zkFlagFile != null && _zkFlagFile.exists();
  }

  private Callback<None> getStartupOrLoggerCallback()
  {
    Callback<None> callback = _startupCallback.getAndSet(null);
    if (callback == null)
    {
      callback = new Callback<None>()
      {
        @Override
        public void onSuccess(None none)
        {
          LOG.info("Enabled stores");
        }

        @Override
        public void onError(Throwable e)
        {
          LOG.error("Failed to enable stores", e);
        }
      };
    }
    return callback;
  }

  private class ZKListener implements ZKConnection.StateListener
  {
    private final TogglingLoadBalancer _balancer;

    private ZKListener(TogglingLoadBalancer balancer)
    {
      _balancer = balancer;
    }

    @Override
    public void notifyStateChange(final Watcher.Event.KeeperState state)
    {
      LOG.info("ZooKeeper session {} received KeeperState {}",
               _zkConnection.getZooKeeper().getSessionId(), state);
      _executor.execute(new Runnable()
      {
        public void run()
        {
          switch (state)
          {
            case SyncConnected:
            {
              processSyncConnected(_balancer);
              break;
            }

            case Disconnected:
            {
              // TOGGLE OFF and let the bus/filestore respond to subscription requests
              LOG.info("Enabling backup stores");
              _balancer.enableBackup(getStartupOrLoggerCallback());
              break;
            }

            case Expired:
            {
              // We were disconnected for longer than the session timeout (or ZK lost our
              // session due to ZK crash or whatever). Thus, we may have missed some
              // notifications and our stored state (in the bus/FS store) is no longer valid
              reset(_balancer);
              break;
            }

            default:
            {
              LOG.info("Ignoring unknown state change {}", state);
              break;
            }
          }
        }
      });
    }
  }

  private void processSyncConnected(TogglingLoadBalancer balancer)
  {
    // TOGGLE ON, we don't need to reconstruct state
    if (suppressZK())
    {
      LOG.warn("ZooKeeper currently suppressed by flag file {}, enabling backup stores",
               _zkFlagFile.getAbsolutePath());
      balancer.enableBackup(getStartupOrLoggerCallback());
    }
    else
    {
      LOG.info("Enabling primary ZK stores");
      _directory.setConnection(_zkConnection);
      balancer.enablePrimary(new Callback<None>()
      {
        @Override
        public void onSuccess(None result)
        {
          getStartupOrLoggerCallback().onSuccess(result);
        }

        @Override
        public void onError(Throwable e)
        {
          //we will receive Disconnected shortly
          LOG.info("Ignored error enabling primary ZK stores; expecting Disconnected notification", e);
        }
      });
    }
  }

  private void reset(final LoadBalancer balancer)
  {

    LOG.info("Resetting LoadBalancerState");

    // Pretty unlikely that the startup callback is still pending when we
    // are notified of session expiration, but checking anyway...
    Callback<None> callback = _startupCallback.getAndSet(null);
    if (callback != null)
    {
      callback.onError(new KeeperException.SessionExpiredException());
    }

    callback = new Callback<None>()
    {
      @Override
      public void onSuccess(None none)
      {
        LOG.info("Successfully reset LoadBalancer after ZooKeeper session expiration");

        // We shut down the old LoadBalancer here, which shuts down resources it created,
        // but not the stores we created.
        // However there is a concurrency edge case that we should handle here by delaying the shutdown.
        // See example below:
        //
        // Thread 1 in DynamicClient.java         Thread 2 in ZKFSLoadBalancer.java
        // client = _balancer.getClient()
        //                                        We receive a call to reset the load balancer.
        //                                        ZKListener shuts down load balancer which eventually
        //                                        shutdown all clients
        // client.send() -> ERROR because
        // the client is in shut down state
        // so we need to delay the the shutdown until we replace the _currentLoadBalancer with
        // the new LoadBalancer and the old client holder in DynamicClient has finished calling send()
        _executor.schedule(new Runnable()
        {
          @Override
          public void run()
          {
            balancer.shutdown(new PropertyEventThread.PropertyEventShutdownCallback()
            {
              @Override
              public void done()
              {
                LOG.info("Shut down old LoadBalancer after ZooKeeper session expiration");
              }
            });
          }
        }, _delayedExecution, TimeUnit.MILLISECONDS);

      }

      @Override
      public void onError(Throwable e)
      {
        LOG.error("Failed to reset LoadBalancer after ZooKeeper session expiration");
      }
    };

    // create a new load balancer and swap the old balancer with the new load balancer then the callback
    // will shutdown the old balancer
    start(callback);
  }

  public void reset()
  {
    reset(_currentLoadBalancer);
  }

  /**
   * Only for testing
   */
  ZKConnection zkConnection()
  {
    return _zkConnection;
  }

}
