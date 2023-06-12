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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  }

  public void updateGlobal(DualReadModeProvider.DualReadMode mode)
  {
    _dualReadMode = mode;
    LOG.debug("Global dual read mode updated: " + mode);
  }

  public void updateService(String service, DualReadModeProvider.DualReadMode mode)
  {
    _serviceDualReadModes.put(service, mode);
    LOG.debug("Dual read mode for service " + service + " updated: " + mode);
  }

  public void updateCluster(String cluster, DualReadModeProvider.DualReadMode mode)
  {
    _clusterDualReadModes.put(cluster, mode);
    LOG.debug("Dual read mode for cluster " + cluster + " updated: " + mode);
  }

  public DualReadModeProvider.DualReadMode getDualReadMode()
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

        // Check and switch service-level dual read mode}
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
}
