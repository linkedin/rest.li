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
import javax.annotation.Nonnull;
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
  private final ConcurrentMap<String, RateLimiter> _serviceToRateLimiterMap;
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
    _serviceToRateLimiterMap = new ConcurrentHashMap<>();
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
      notifyGlobalWatchers(_dualReadMode);
    }
  }

  public void updateService(String service, DualReadModeProvider.DualReadMode mode)
  {
    DualReadModeProvider.DualReadMode oldMode = _serviceDualReadModes.put(service, mode);
    if (oldMode != mode) {
      LOG.info("Dual read mode for service {} updated: {}", service, mode);
      notifyServiceWatchers(service, mode);
    }
  }

  public void updateCluster(String cluster, DualReadModeProvider.DualReadMode mode)
  {
    DualReadModeProvider.DualReadMode oldMode = _clusterDualReadModes.put(cluster, mode);
    if (oldMode != mode) {
      LOG.info("Dual read mode for cluster {} updated: {}", cluster, mode);
      notifyClusterWatchers(cluster, mode);
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
    if (_executorService.isShutdown())
    {
      LOG.info("Dual read mode executor is shut down already. Skipping getting the latest dual read mode.");
      return;
    }
    if (d2ServiceName == null)
    {
      return;
    }
    _executorService.execute(() ->
    {
      RateLimiter serviceRateLimiter = _serviceToRateLimiterMap.computeIfAbsent(d2ServiceName,
          key -> RateLimiter.create((double) 1 / DUAL_READ_MODE_SWITCH_MIN_INTERVAL));
      boolean shouldCheck = serviceRateLimiter.tryAcquire();
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

  // Add watchers watching for global dual read mode. The watcher will be notified when the global dual read mode changes.
  public void addGlobalWatcher(DualReadModeWatcher watcher)
  {
    _globalDualReadModeWatchers.add(watcher);
  }

  // Add watchers watching for dual read mode of a service. The watcher will be notified when the dual read mode changes.
  public void addServiceWatcher(String serviceName, DualReadModeWatcher watcher)
  {
    Set<DualReadModeWatcher> watchers = _serviceDualReadModeWatchers.computeIfAbsent(serviceName, k -> ConcurrentHashMap.newKeySet());
    watchers.add(watcher);
  }

  // Add watchers watching for dual read mode of a cluster. The watcher will be notified when the dual read mode changes.
  public void addClusterWatcher(String clusterName, DualReadModeWatcher watcher)
  {
    Set<DualReadModeWatcher> watchers = _clusterDualReadModeWatchers.computeIfAbsent(clusterName, k -> ConcurrentHashMap.newKeySet());
    watchers.add(watcher);
  }

  // Remove watchers for dual read mode of a service.
  public void removeServiceWatcher(String serviceName, DualReadModeWatcher watcher)
  {
    Set<DualReadModeWatcher> watchers = _serviceDualReadModeWatchers.get(serviceName);
    if (watchers != null)
    {
      watchers.remove(watcher);
    }
  }

  // Remove watchers for dual read mode of a cluster.
  public void removeClusterWatcher(String clusterName, DualReadModeWatcher watcher)
  {
    Set<DualReadModeWatcher> watchers = _clusterDualReadModeWatchers.get(clusterName);
    if (watchers != null)
    {
      watchers.remove(watcher);
    }
  }

  private void notifyGlobalWatchers(DualReadModeProvider.DualReadMode mode)
  {
    notifyWatchers(_globalDualReadModeWatchers, mode);
  }

  private void notifyServiceWatchers(String serviceName, DualReadModeProvider.DualReadMode mode)
  {
    notifyWatchers(_serviceDualReadModeWatchers.get(serviceName), mode);
  }

  private void notifyClusterWatchers(String clusterName, DualReadModeProvider.DualReadMode mode)
  {
    notifyWatchers(_clusterDualReadModeWatchers.get(clusterName), mode);
  }

  private static void notifyWatchers(Set<DualReadModeWatcher> watchers, DualReadModeProvider.DualReadMode mode)
  {
    if (watchers != null)
    {
      for (DualReadModeWatcher w : watchers)
      {
        w.onChanged(mode);
      }
    }
  }

  public interface DualReadModeWatcher
  {
    void onChanged(@Nonnull DualReadModeProvider.DualReadMode mode);
  }
}
