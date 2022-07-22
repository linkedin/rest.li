package com.linkedin.d2.xds;

import com.google.protobuf.Value;
import io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints;
import io.envoyproxy.envoy.config.route.v3.VirtualHost;
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public abstract class XdsClient
{
  /**
   * Watcher interface for a single requested xDS resource.
   */
  interface ResourceWatcher
  {
    // TODO: Handle onError and onResourceNotExist
  }

  interface LdsResourceWatcher extends ResourceWatcher
  {
    void onChanged(LdsUpdate update);
  }

  interface RdsResourceWatcher extends ResourceWatcher
  {
    void onChanged(RdsUpdate update);
  }

  interface CdsResourceWatcher extends ResourceWatcher
  {
    void onChanged(CdsUpdate update);
  }

  interface EdsResourceWatcher extends ResourceWatcher
  {
    void onChanged(EdsUpdate update);
  }

  interface ResourceUpdate
  {
  }

  static final class LdsUpdate implements ResourceUpdate
  {
    // Http level api listener configuration.
    final HttpConnectionManager _httpConnectionManager;

    LdsUpdate(HttpConnectionManager httpConnectionManager)
    {
      _httpConnectionManager = httpConnectionManager;
    }

    public HttpConnectionManager getHttpConnectionManager()
    {
      return _httpConnectionManager;
    }

    @Override
    public String toString()
    {
      return "LdsUpdate{" + "_httpConnectionManager=" + _httpConnectionManager + '}';
    }

    @Override
    public int hashCode()
    {
      return Objects.hash(_httpConnectionManager);
    }

    @Override
    public boolean equals(Object o)
    {
      if (this == o)
      {
        return true;
      }
      if (o == null || getClass() != o.getClass())
      {
        return false;
      }
      LdsUpdate ldsUpdate = (LdsUpdate) o;
      return Objects.equals(_httpConnectionManager, ldsUpdate._httpConnectionManager);
    }
  }

  static final class RdsUpdate implements ResourceUpdate
  {
    // The list of virtual hosts that make up the route table.
    final List<VirtualHost> _virtualHosts;
    final Map<String, Value> _serviceProperties;

    RdsUpdate(List<VirtualHost> virtualHosts, Map<String, Value> serviceProperties)
    {
      _virtualHosts = Collections.unmodifiableList(new ArrayList<>(virtualHosts));
      _serviceProperties = serviceProperties;
    }

    public List<VirtualHost> getVirtualHosts()
    {
      return _virtualHosts;
    }

    public Map<String, Value> getServiceProperties()
    {
      return _serviceProperties;
    }

    @Override
    public String toString()
    {
      return "RdsUpdate{" + "_virtualHosts=" + _virtualHosts + ", _serviceProperties=" + _serviceProperties + '}';
    }

    @Override
    public int hashCode()
    {
      return Objects.hash(_virtualHosts, _serviceProperties);
    }

    @Override
    public boolean equals(Object o)
    {
      if (this == o)
      {
        return true;
      }
      if (o == null || getClass() != o.getClass())
      {
        return false;
      }
      RdsUpdate rdsUpdate = (RdsUpdate) o;
      return Objects.equals(_virtualHosts, rdsUpdate._virtualHosts) && Objects.equals(_serviceProperties,
          rdsUpdate._serviceProperties);
    }
  }

  /** xDS resource update for cluster-level configuration. */
  static final class CdsUpdate implements ResourceUpdate
  {
    final String _clusterName;
    final String _edsServiceName;
    final Map<String, Value> _clusterProperties;

    public CdsUpdate(String clusterName, String edsServiceName, Map<String, Value> clusterProperties)
    {
      _clusterName = clusterName;
      _edsServiceName = edsServiceName;
      _clusterProperties = clusterProperties;
    }

    public String getClusterName()
    {
      return _clusterName;
    }

    public String getEdsServiceName()
    {
      return _edsServiceName;
    }

    public Map<String, Value> getClusterProperties()
    {
      return _clusterProperties;
    }

    @Override
    public String toString()
    {
      return "CdsUpdate{" + "_clusterName='" + _clusterName + '\'' + ", _edsServiceName='" + _edsServiceName + '\''
          + ", _clusterProperties=" + _clusterProperties + '}';
    }

    @Override
    public int hashCode()
    {
      return Objects.hash(_clusterName, _edsServiceName, _clusterProperties);
    }

    @Override
    public boolean equals(Object o)
    {
      if (this == o)
      {
        return true;
      }
      if (o == null || getClass() != o.getClass())
      {
        return false;
      }
      CdsUpdate cdsUpdate = (CdsUpdate) o;
      return Objects.equals(_clusterName, cdsUpdate._clusterName) && Objects.equals(_edsServiceName,
          cdsUpdate._edsServiceName) && Objects.equals(_clusterProperties, cdsUpdate._clusterProperties);
    }
  }

  static final class EdsUpdate implements ResourceUpdate
  {
    final String _clusterName;
    final List<LocalityLbEndpoints> _localityLbEndpoints;

    EdsUpdate(String clusterName, List<LocalityLbEndpoints> localityLbEndpoints)
    {
      _clusterName = clusterName;
      _localityLbEndpoints = Collections.unmodifiableList(localityLbEndpoints);
    }

    public String getClusterName()
    {
      return _clusterName;
    }

    public List<LocalityLbEndpoints> getLocalityLbEndpoints()
    {
      return _localityLbEndpoints;
    }

    @Override
    public String toString()
    {
      return "EdsUpdate{" + "_clusterName='" + _clusterName + '\'' + ", _localityLbEndpoints="
          + _localityLbEndpoints + '}';
    }

    @Override
    public boolean equals(Object o)
    {
      if (this == o)
      {
        return true;
      }
      if (o == null || getClass() != o.getClass())
      {
        return false;
      }
      EdsUpdate that = (EdsUpdate) o;
      return Objects.equals(_clusterName, that._clusterName) && Objects.equals(_localityLbEndpoints,
          that._localityLbEndpoints);
    }

    @Override
    public int hashCode()
    {
      return Objects.hash(_clusterName, _localityLbEndpoints);
    }
  }

  /**
   * Registers a data watcher for the given LDS resource.
   */
  void watchLdsResource(String resourceName, LdsResourceWatcher watcher) {
    throw new UnsupportedOperationException();
  }
  /**
   * Registers a data watcher for the given RDS resource.
   */
  void watchRdsResource(String resourceName, RdsResourceWatcher watcher) {
    throw new UnsupportedOperationException();
  }

  /**
   * Registers a data watcher for the given CDS resource.
   */
  void watchCdsResource(String resourceName, CdsResourceWatcher watcher) {
    throw new UnsupportedOperationException();
  }

  /**
   * Registers a data watcher for the given EDS resource.
   */
  void watchEdsResource(String resourceName, EdsResourceWatcher watcher) {
    throw new UnsupportedOperationException();
  }

  // TODO: Handle cancel resource watch
}
