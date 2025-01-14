package com.linkedin.d2.xds.balancer;

import com.linkedin.common.callback.Callback;
import com.linkedin.d2.balancer.Directory;
import com.linkedin.d2.xds.XdsClient;
import io.grpc.Status;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.linkedin.d2.xds.XdsClient.*;


public class XdsDirectory implements Directory
{
  private final XdsClient _xdsClient;
  private final ConcurrentMap<String, String> _serviceNames = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, String> _clusterNames = new ConcurrentHashMap<>();
  private XdsClient.WildcardD2ClusterOrServiceNameResourceWatcher _watcher;
  /**
   * If service/cluster names are empty, wait for a while before returning the names, callers could set a shorter
   * timeout on the callback they passed in to getServiceNames or getClusterNames, as needed
   */
  private static final Long DEFAULT_WAIT_TIME = 10000L;

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
    if (_watcher != null)
    {
      return;
    }
    _watcher = createNameWatcher();
    _xdsClient.watchAllXdsResources(_watcher);
  }

  private XdsClient.WildcardD2ClusterOrServiceNameResourceWatcher createNameWatcher()
  {
    return new XdsClient.WildcardD2ClusterOrServiceNameResourceWatcher()
    {

      @Override
      public void onChanged(String resourceName, XdsClient.D2ClusterOrServiceNameUpdate update)
      {
        if (update == EMPTY_D2_CLUSTER_OR_SERVICE_NAME_UPDATE)
        { // invalid data, ignore
          return;
        }
        D2ClusterOrServiceName nameData = update.getNameData();
        if (nameData.getClusterName() != null)
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
        // Don't need to differentiate between cluster and service names, will have no op on the map that doesn't
        // have the key. And the resource won't be both a cluster and a service name, since the two have different d2
        // path (/d2/clusters vs /d2/services).
        _clusterNames.remove(resourceName);
        _serviceNames.remove(resourceName);
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
    if (names.isEmpty())
    {
      try {
        wait(DEFAULT_WAIT_TIME);
      } catch (InterruptedException e) {
        // do nothing
      }
    }

    callback.onSuccess(new ArrayList<>(names));
  }
}
