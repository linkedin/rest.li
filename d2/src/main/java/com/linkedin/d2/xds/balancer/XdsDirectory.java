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
  private final AtomicReference<WildcardD2ClusterOrServiceNameResourceWatcher> _watcher = new AtomicReference<>();
  /**
   * A flag that shows whether the service/cluster names data is being updated. Requests to the data should wait until
   * the update is done.
   */
  @VisibleForTesting
  final AtomicBoolean _isUpdating = new AtomicBoolean(true);
  /**
   * This lock will be released when the service and cluster names data have been updated and is ready to serve.
   * If the data is being updated, requests to read the data will wait until timeout and return the current data.
   * Callers can also set a shorter timeout when getting the result of the callback passed to getServiceNames or
   * getClusterNames, as needed.
   */
  private final Object _dataReadyLock = new Object();
  private static final Long DEFAULT_TIMEOUT = 10000L;

  public XdsDirectory(XdsClient xdsClient)
  {
    _xdsClient = xdsClient;
  }

  public void start() {
    LOG.debug("Starting. Setting isUpdating to true");
    _isUpdating.set(true); // initially set to true to block reads before the first (lazy) update completes
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
        _isUpdating.compareAndSet(false, true);
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
        _isUpdating.compareAndSet(false, true);
        // Don't need to differentiate between cluster and service names, will have no op on the map that doesn't
        // have the key. And the resource won't be both a cluster and a service name, since the two have different d2
        // path (/d2/clusters vs /d2/services).
        _clusterNames.remove(resourceName);
        _serviceNames.remove(resourceName);
      }

      @Override
      public void onAllResourcesProcessed()
      {
        _isUpdating.compareAndSet(true, false);
        synchronized (_dataReadyLock)
        {
          _dataReadyLock.notifyAll();
          LOG.debug("notified all threads waiting on lock");
        }
      }

      @Override
      public void onError(Status error)
      {
        // do nothing
      }
    };
  }

  private void waitAndRespond(boolean isForService, Callback<List<String>> callback)
  {
    if (_isUpdating.get())
    {
      // If the data is being updated, wait until timeout. Note that a shorter timeout can be set by the caller when
      // getting the result of the callback.
      synchronized (_dataReadyLock)
      {
        try
        {
          LOG.debug("Waiting on lock for data to be ready");
          _dataReadyLock.wait(DEFAULT_TIMEOUT);
        }
        catch (InterruptedException e)
        {
          callback.onError(e);
          Thread.currentThread().interrupt();
          throw new RuntimeException(e);
        }
      }
    }
    LOG.debug("Data is ready or timed out on waiting for update, responding to request");
    callback.onSuccess(new ArrayList<>(isForService ? _serviceNames.values() : _clusterNames.values()));
  }
}
