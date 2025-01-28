package com.linkedin.d2.xds.balancer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.linkedin.common.callback.Callback;
import com.linkedin.d2.balancer.Directory;
import com.linkedin.d2.xds.XdsClient;
import indis.XdsD2;
import io.grpc.Status;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.d2.xds.XdsClient.*;


public class XdsDirectory implements Directory
{
  private static final Logger LOG = LoggerFactory.getLogger(XdsDirectory.class);
  private final XdsClient _xdsClient;
  @VisibleForTesting
  final ConcurrentMap<String, String> _serviceNames = new ConcurrentHashMap<>();
  @VisibleForTesting
  final ConcurrentMap<String, String> _clusterNames = new ConcurrentHashMap<>();
  @VisibleForTesting
  final AtomicReference<WildcardD2ClusterOrServiceNameResourceWatcher> _watcher = new AtomicReference<>();
  /**
   * A flag that shows whether the service/cluster names data is being updated. Requests to the data should wait until
   * the update is done.
   */
  @VisibleForTesting
  final AtomicBoolean _isUpdating = new AtomicBoolean(true);
  /**
   * The write lock will be released when the service and cluster names data have been updated and is ready to serve.
   * If the data is being updated, requests to read the data will wait indefinitely. Callers could set a shorter
   * timeout on the callback passed in to getServiceNames or getClusterNames, as needed.
   */
  private final ReadWriteLock _dataReadyLock = new ReentrantReadWriteLock();

  public XdsDirectory(XdsClient xdsClient)
  {
    _xdsClient = xdsClient;
  }

  public void start() {
    LOG.debug("Starting. Setting isUpdating to true and locking the write lock");
    _isUpdating.set(true);
    _dataReadyLock.writeLock().lock(); // initially locked to block reads before the first update completes
    addNameWatcher();
  }

  @Override
  public void getServiceNames(Callback<List<String>> callback)
  {
    addNameWatcher();
    waitAndRespond(true, callback);
  }

  @Override
  public void getClusterNames(Callback<List<String>> callback)
  {
    addNameWatcher();
    waitAndRespond(false, callback);
  }

  private void addNameWatcher()
  {
    if (_watcher.get() != null)
    {
      return;
    }
    boolean created = _watcher.compareAndSet(null, createNameWatcher());
    if (created)
    {
      _xdsClient.watchAllXdsResources(_watcher.get());
    }
  }

  private XdsClient.WildcardD2ClusterOrServiceNameResourceWatcher createNameWatcher()
  {
    return new XdsClient.WildcardD2ClusterOrServiceNameResourceWatcher()
    {

      @Override
      public void onChanged(String resourceName, XdsClient.D2ClusterOrServiceNameUpdate update)
      {
        if (_isUpdating.compareAndSet(false, true))
        {
          LOG.debug("onChanged locking write lock");
          _dataReadyLock.writeLock().lock();
        }
        if (EMPTY_D2_CLUSTER_OR_SERVICE_NAME_UPDATE.equals(update))
        { // invalid data, ignore. Logged in xds client.
          return;
        }
        XdsD2.D2ClusterOrServiceName nameData = update.getNameData();
        // the data is guaranteed valid by the xds client. It has a non-empty name in either clusterName or serviceName.
        if (!Strings.isNullOrEmpty(nameData.getClusterName()))
        {
          _clusterNames.put(resourceName, nameData.getClusterName());
        } else
        {
          _serviceNames.put(resourceName, nameData.getServiceName());
        }
      }

      @Override
      public void onRemoval(String resourceName)
      {
        if (_isUpdating.compareAndSet(false, true))
        {
          LOG.debug("onRemoval locking write lock");
          _dataReadyLock.writeLock().lock();
        }
        // Don't need to differentiate between cluster and service names, will have no op on the map that doesn't
        // have the key. And the resource won't be both a cluster and a service name, since the two have different d2
        // path (/d2/clusters vs /d2/services).
        _clusterNames.remove(resourceName);
        _serviceNames.remove(resourceName);
      }

      @Override
      public void onAllResourcesProcessed()
      {
        if (_isUpdating.compareAndSet(true, false))
        {
          LOG.debug("onAllResourcesProcessed unlocking write lock");
          _dataReadyLock.writeLock().unlock();
        }
      }

      @Override
      public void onError(Status error)
      {
        // do nothing
      }

      @Override
      public void onReconnect()
      {
        // do nothing
      }
    };
  }

  private void waitAndRespond(boolean isForService, Callback<List<String>> callback)
  {
    List<String> names;
    try
    {
      LOG.debug("Locking read lock. Blocking request");
      _dataReadyLock.readLock().lock();
      names = new ArrayList<>(isForService ? _serviceNames.values() : _clusterNames.values());
    }
    finally
    {
      LOG.debug("Unlocking read lock. Request unblocked");
      _dataReadyLock.readLock().unlock();
    }
    callback.onSuccess(names);
  }
}
