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

package com.linkedin.d2.xds;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import com.linkedin.d2.discovery.event.PropertyEventPublisher;


public class XdsToClusterPropertiesPublisher implements PropertyEventPublisher<ClusterProperties>
{
  private final XdsToD2PropertiesAdaptor _adaptor;

  public XdsToClusterPropertiesPublisher(XdsToD2PropertiesAdaptor adaptor)
  {
    _adaptor = adaptor;
  }

  @Override
  public void setBus(PropertyEventBus<ClusterProperties> bus)
  {
    _adaptor.setClusterEventBus(bus);
  }

  @Override
  public void startPublishing(String clusterName)
  {
    _adaptor.listenToCluster(clusterName);
  }

  @Override
  public void stopPublishing(String clusterName)
  {
    // TODO
  }

  @Override
  public void start(Callback<None> callback)
  {
    callback.onSuccess(None.none());
  }

  @Override
  public void shutdown(Callback<None> callback)
  {
    callback.onSuccess(None.none());
  }
}
