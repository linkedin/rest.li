/*
   Copyright (c) 2023 LinkedIn Corp.

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

package com.linkedin.d2.balancer.dualread;

import com.google.common.util.concurrent.RateLimiter;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SystemClock;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Checks and manages the global and per-service dual read state.
 * Provides monitoring of the dual read data.
 * The dual read state is broken down into global and per-service state. Per-service dual read
 * mode has a higher priority. Only if per-service dual read mode is not defined, global
 * dual read mode will be used.
 */
@SuppressWarnings("UnstableApiUsage")
public class DualReadStateManager
{
  private static final Logger LOG = LoggerFactory.getLogger(DualReadStateManager.class);
  private static final int DUAL_READ_MODE_SWITCH_MIN_INTERVAL = 10;

  // Stores service-level dual read mode
  private final ConcurrentMap<String, DualReadModeProvider.DualReadMode> _serviceDualReadModes;
  // Stores cluster-level dual read mode
  private final ConcurrentMap<String, DualReadModeProvider.DualReadMode> _clusterDualReadModes;
  private final DualReadModeProvider _dualReadModeProvider;
  private final ScheduledExecutorService _executorService;
  private final RateLimiter _rateLimiter;
  // Stores global dual read mode
  private volatile DualReadModeProvider.DualReadMode _dualReadMode = DualReadModeProvider.DualReadMode.OLD_LB_ONLY;
  private final Set<DualReadModeWatcher> _globalDualReadModeWatchers;
  private final ConcurrentMap<String, Set<DualReadModeWatcher>> _serviceDualReadModeWatchers;
  private final ConcurrentMap<String, Set<DualReadModeWatcher>> _clusterDualReadModeWatchers;

  private final DualReadLoadBalancerJmx _dualReadLoadBalancerJmx;

  private final DualReadLoadBalancerMonitor.UriPropertiesDualReadMonitor _uriPropertiesDualReadMonitor;
  private final DualReadLoadBalancerMonitor.ServicePropertiesDualReadMonitor _servicePropertiesDualReadMonitor;
  private final DualReadLoadBalancerMonitor.ClusterPropertiesDualReadMonitor _clusterPropertiesDualReadMonitor;


  public DualReadStateManager(DualReadModeProvider dualReadModeProvider, ScheduledExecutorService executorService)
  {
    _dualReadLoadBalancerJmx = new DualReadLoadBalancerJmx();
    Clock clock = SystemClock.instance();
    _uriPropertiesDualReadMonitor = new DualReadLoadBalancerMonitor.UriPropertiesDualReadMonitor(
        _dualReadLoadBalancerJmx, clock);
    _servicePropertiesDualReadMonitor = new DualReadLoadBalancerMonitor.ServicePropertiesDualReadMonitor(
        _dualReadLoadBalancerJmx, clock);
    _clusterPropertiesDualReadMonitor = new DualReadLoadBalancerMonitor.ClusterPropertiesDualReadMonitor(
        _dualReadLoadBalancerJmx, clock);
    _serviceDualReadModes = new ConcurrentHashMap<>();
    _clusterDualReadModes = new ConcurrentHashMap<>();
    _dualReadModeProvider = dualReadModeProvider;
    _executorService = executorService;
    _rateLimiter = RateLimiter.create((double) 1 / DUAL_READ_MODE_SWITCH_MIN_INTERVAL);
    _globalDualReadModeWatchers = ConcurrentHashMap.newKeySet();
    _serviceDualReadModeWatchers = new ConcurrentHashMap<>();
    _clusterDualReadModeWatchers = new ConcurrentHashMap<>();
  }

  public void updateGlobal(DualReadModeProvider.DualReadMode mode)
  {
    boolean updated = _dualReadMode != mode;
    _dualReadMode = mode;
    if (updated) {
      LOG.info("Global dual read mode updated: {}", mode);
      notifyGlobalWatchers();
    }
  }

  public void updateService(String service, DualReadModeProvider.DualReadMode mode)
  {
    DualReadModeProvider.DualReadMode oldMode = _serviceDualReadModes.put(service, mode);
    if (oldMode != mode) {
      LOG.info("Dual read mode for service {} updated: {}", service, mode);
      notifyServiceWatchers(service);
    }
  }

  public void updateCluster(String cluster, DualReadModeProvider.DualReadMode mode)
  {
    DualReadModeProvider.DualReadMode oldMode = _clusterDualReadModes.put(cluster, mode);
    if (oldMode != mode) {
      LOG.info("Dual read mode for cluster {} updated: {}", cluster, mode);
      notifyClusterWatchers(cluster);
    }
  }

  public DualReadModeProvider.DualReadMode getGlobalDualReadMode()
  {
    checkAndSwitchMode(null);
    return _dualReadMode;
  }

  public DualReadModeProvider.DualReadMode getServiceDualReadMode(String d2ServiceName)
  {
    checkAndSwitchMode(d2ServiceName);
    return _serviceDualReadModes.getOrDefault(d2ServiceName, _dualReadMode);
  }

  public DualReadModeProvider.DualReadMode getClusterDualReadMode(String d2ClusterName)
  {
    return _clusterDualReadModes.getOrDefault(d2ClusterName, _dualReadMode);
  }

  public <T> void reportData(String propertyName, T property, boolean fromNewLb)
  {
    _executorService.execute(() ->
    {
      if (property instanceof ServiceProperties)
      {
        reportServicePropertiesData(propertyName, (ServiceProperties) property, fromNewLb);
      }
      else if (property instanceof ClusterProperties)
      {
        reportClusterPropertiesData(propertyName, (ClusterProperties) property, fromNewLb);
      }
      else if (property instanceof UriProperties)
      {
        reportUriPropertiesData(propertyName, (UriProperties) property, fromNewLb);
      }
      else
      {
        LOG.warn("Unknown property type: " + property);
      }
    });
  }

  private void reportServicePropertiesData(String propertyName, ServiceProperties property, boolean fromNewLb)
  {
    if (_serviceDualReadModes.getOrDefault(propertyName, _dualReadMode) == DualReadModeProvider.DualReadMode.DUAL_READ)
    {
      _servicePropertiesDualReadMonitor.reportData(propertyName, property, String.valueOf(property.getVersion()), fromNewLb);
    }
  }

  private void reportClusterPropertiesData(String propertyName, ClusterProperties property, boolean fromNewLb)
  {
    if (_clusterDualReadModes.getOrDefault(propertyName, _dualReadMode) == DualReadModeProvider.DualReadMode.DUAL_READ)
    {
      _clusterPropertiesDualReadMonitor.reportData(propertyName, property, String.valueOf(property.getVersion()), fromNewLb);
    }
  }

  private void reportUriPropertiesData(String propertyName, UriProperties property, boolean fromNewLb)
  {
    if (_clusterDualReadModes.getOrDefault(propertyName, _dualReadMode) == DualReadModeProvider.DualReadMode.DUAL_READ)
    {
      String version = property.getVersion() + "|" + property.Uris().size();
      _uriPropertiesDualReadMonitor.reportData(propertyName, property, version, fromNewLb);
    }
  }

  /**
   * Asynchronously check and update the dual read mode for the given D2 service
   * @param d2ServiceName the name of the D2 service
   */
  public void checkAndSwitchMode(String d2ServiceName)
  {
    _executorService.execute(() ->
    {
      boolean shouldCheck = _rateLimiter.tryAcquire();
      if (shouldCheck)
      {
        // Check and switch global dual read mode
        updateGlobal(_dualReadModeProvider.getDualReadMode());

        // Check and switch service-level dual read mode
        if (d2ServiceName != null)
        {
          updateService(d2ServiceName, _dualReadModeProvider.getDualReadMode(d2ServiceName));
        }
      }
    });
  }

  public DualReadLoadBalancerJmx getDualReadLoadBalancerJmx()
  {
    return _dualReadLoadBalancerJmx;
  }

  public DualReadModeProvider getDualReadModeProvider()
  {
    return _dualReadModeProvider;
  }

  public void addGlobalWatcher(DualReadModeWatcher watcher)
  {
    _globalDualReadModeWatchers.add(watcher);
  }

  public void addServiceWatcher(String serviceName, DualReadModeWatcher watcher)
  {
    Set<DualReadModeWatcher> watchers = _serviceDualReadModeWatchers.computeIfAbsent(serviceName, k -> ConcurrentHashMap.newKeySet());
    watchers.add(watcher);
  }

  public void addClusterWatcher(String clusterName, DualReadModeWatcher watcher)
  {
    Set<DualReadModeWatcher> watchers = _clusterDualReadModeWatchers.computeIfAbsent(clusterName, k -> ConcurrentHashMap.newKeySet());
    watchers.add(watcher);
  }

  public void removeServiceWatcher(String serviceName, DualReadModeWatcher watcher)
  {
    Set<DualReadModeWatcher> watchers = _serviceDualReadModeWatchers.get(serviceName);
    if (watchers != null)
    {
      watchers.remove(watcher);
    }
  }

  public void removeClusterWatcher(String clusterName, DualReadModeWatcher watcher)
  {
    Set<DualReadModeWatcher> watchers = _clusterDualReadModeWatchers.get(clusterName);
    if (watchers != null)
    {
      watchers.remove(watcher);
    }
  }

  private void notifyGlobalWatchers()
  {
    notifyWatchers(_globalDualReadModeWatchers);
  }

  private void notifyServiceWatchers(String serviceName)
  {
    notifyWatchers(_serviceDualReadModeWatchers.get(serviceName));
  }

  private void notifyClusterWatchers(String clusterName)
  {
    notifyWatchers(_clusterDualReadModeWatchers.get(clusterName));
  }

  private void notifyWatchers(Set<DualReadModeWatcher> watchers)
  {
    if (watchers != null)
    {
      for (DualReadModeWatcher w : watchers)
      {
        w.onChanged();
      }
    }
  }

  public interface DualReadModeWatcher
  {
    void onChanged();
  }
}
