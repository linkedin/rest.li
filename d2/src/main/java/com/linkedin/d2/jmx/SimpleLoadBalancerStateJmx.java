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

package com.linkedin.d2.jmx;

import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.discovery.stores.zk.SymlinkUtil;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.linkedin.d2.balancer.LoadBalancerState.NullStateListenerCallback;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState;


public class SimpleLoadBalancerStateJmx implements SimpleLoadBalancerStateJmxMBean
{
  private final SimpleLoadBalancerState _state;
  private final LoadBalancerStateOtelMetricsProvider _otelMetricsProvider;
  private String _clientName = "-";

  @Deprecated
  public SimpleLoadBalancerStateJmx(SimpleLoadBalancerState state)
  {
    this(state, new NoOpLoadBalancerStateOtelMetricsProvider());
  }

  public SimpleLoadBalancerStateJmx(SimpleLoadBalancerState state, LoadBalancerStateOtelMetricsProvider otelMetricsProvider)
  {
    _state = state;
    _otelMetricsProvider = otelMetricsProvider == null ? new NoOpLoadBalancerStateOtelMetricsProvider() : otelMetricsProvider;
  }

  public void setClientName(String clientName)
  {
    _clientName = clientName == null ? "-" : clientName;
  }

  public String getClientName()
  {
    return _clientName;
  }

  @Override
  public int getClusterCount()
  {
  int regular = _state.getClusterCount();
  long symlink = _state.getClusters().stream().filter(SymlinkUtil::isSymlinkNodeOrPath).count();
  _otelMetricsProvider.recordClusterCount(_clientName, (long) regular, symlink);
  return regular;
  }

  @Override
  public long getSymlinkClusterCount()
  {
  long symlink = _state.getClusters().stream().filter(SymlinkUtil::isSymlinkNodeOrPath).count();
  _otelMetricsProvider.recordClusterCount(_clientName, (long) _state.getClusterCount(), symlink);
  return symlink;
  }

  @Override
  public int getClusterListenCount()
  {
    return _state.getClusterListenCount();
  }

  @Override
  public int getListenerCount()
  {
    return _state.getListenerCount();
  }

  @Override
  public int getServiceCount()
  {
  int count = _state.getServiceCount();
  _otelMetricsProvider.recordServiceCount(_clientName, (long) count);
  return count;
  }

  @Override
  public int getServiceListenCount()
  {
    return _state.getServiceListenCount();
  }

  @Override
  public List<String> getSupportedSchemes()
  {
    return new ArrayList<>(_state.getSupportedSchemes());
  }

  @Override
  public List<String> getSupportedStrategies()
  {
    return new ArrayList<>(_state.getSupportedStrategies());
  }

  @Override
  public int getTrackerClientCount(String clusterName)
  {
    return _state.getTrackerClientCount(clusterName);
  }

  @Override
  public int getUriCount()
  {
    return _state.getUriCount();
  }

  @Override
  public void setVersion(long version)
  {
    _state.setVersion(version);
  }

  @Override
  public String getUriProperty(String clusterName)
  {
    return _state.getUriProperties(clusterName) + "";
  }

  @Override
  public String getServiceProperty(String serviceName)
  {
    return _state.getServiceProperties(serviceName) + "";
  }

  @Override
  public String getClusterProperty(String clusterName)
  {
    return _state.getClusterProperties(clusterName) + "";
  }

  @Override
  public long getVersion()
  {
    return _state.getVersion();
  }

  @Override
  public boolean isListeningToCluster(String clusterName)
  {
    return _state.isListeningToCluster(clusterName);
  }

  @Override
  public boolean isListeningToService(String serviceName)
  {
    return _state.isListeningToService(serviceName);
  }

  @Override
  public void listenToCluster(String clusterName)
  {
    _state.listenToCluster(clusterName, new NullStateListenerCallback());
  }

  @Override
  public long getDelayedExecution()
  {
    return _state.getDelayedExecution();
  }

  @Override
  public void setDelayedExecution(long milliseconds)
  {
    _state.setDelayedExecution(milliseconds);
  }

  @Override
  public String getServerUrisForServiceName(String serviceName)
  {
    return _state.getServerUrisForServiceName(serviceName).toString();
  }

  @Override
  public String getTrackerClientInformation(String trackerClientUri, String serviceName)
      throws URISyntaxException
  {
    URI uri = new URI(trackerClientUri);
    TrackerClient client = _state.getClient(serviceName, uri);
    return client.toString();
  }

  @Override
  public String getInternalMappingServicesForClusterName(String clusterName)
  {
    return _state.getServicesForCluster(clusterName).toString();
  }

  @Override
  public void listenToService(String serviceName)
  {
    _state.listenToService(serviceName, new NullStateListenerCallback());
  }
}
