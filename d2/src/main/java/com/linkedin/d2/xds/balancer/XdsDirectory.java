package com.linkedin.d2.xds.balancer;

import com.linkedin.common.callback.Callback;
import com.linkedin.d2.balancer.Directory;
import com.linkedin.d2.xds.XdsClient;
import indis.XdsD2;
import io.grpc.Status;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.linkedin.d2.xds.XdsClient.*;


public class XdsDirectory implements Directory
{
  private final XdsClient _xdsClient;
  private final ConcurrentMap<String, String> _serviceNames = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, String> _clusterNames = new ConcurrentHashMap<>();
  private final AtomicReference<WildcardD2ClusterOrServiceNameResourceWatcher> _watcher = new AtomicReference<>();
  /**
   * A flag that shows whether the service/cluster names data is being updated. Requests to the data should wait until
   * the update is done.
   */
  private final AtomicBoolean _isUpdating = new AtomicBoolean(true);
  /**
   * This lock will be released when the service and cluster names data have been updated and is ready to serve.
   * If the data is being updated, requests to access the data will wait indefinitely. Callers could set a shorter
   * timeout on the callback passed in to getServiceNames or getClusterNames, as needed.
   */
  private final Object _dataReadyLock = new Object();

  public XdsDirectory(XdsClient xdsClient)
  {
    _xdsClient = xdsClient;
  }

  public void start() {
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
        _isUpdating.compareAndSet(false, true);
        if (update == EMPTY_D2_CLUSTER_OR_SERVICE_NAME_UPDATE)
        { // invalid data, ignore
          return;
        }
        XdsD2.D2ClusterOrServiceName nameData = update.getNameData();
        if (!nameData.getClusterName().isEmpty())
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
        synchronized (_dataReadyLock)
        {
          _isUpdating.compareAndSet(true, false);
          _dataReadyLock.notifyAll();
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

  private void waitAndRespond(boolean isForService, Callback<List<String>> callback) {
    // Changes in the corresponding map will be reflected in the returned collection
    Collection<String> names = isForService ? _serviceNames.values() : _clusterNames.values();

    synchronized (_dataReadyLock)
    {
      while (_isUpdating.get())
      {
        try
        {
          _dataReadyLock.wait();
        } catch (InterruptedException e)
        {
          // do nothing
        }
      }
      callback.onSuccess(new ArrayList<>(names));
    }
  }
}
