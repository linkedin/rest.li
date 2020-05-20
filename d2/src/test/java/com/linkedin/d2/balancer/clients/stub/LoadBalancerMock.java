/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.d2.balancer.clients.stub;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.LoadBalancer;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.clients.TestClient;
import com.linkedin.d2.balancer.clients.TrackerClientTest;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.discovery.event.PropertyEventThread;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


public class LoadBalancerMock implements LoadBalancer
{

  private boolean _serviceUnavailable;
  private boolean _dontCallCallback;
  private final ScheduledExecutorService _scheduledExecutorService;
  public boolean  shutdown = false;


  public LoadBalancerMock(boolean serviceUnavailable)
  {
    this(serviceUnavailable, false);
  }

  public LoadBalancerMock(boolean serviceUnavailable, boolean dontCallCallback)
  {
    this(serviceUnavailable, dontCallCallback, Executors.newSingleThreadScheduledExecutor());
  }

  public LoadBalancerMock(boolean serviceUnavailable, boolean dontCallCallback, ScheduledExecutorService scheduledExecutorService)
  {
    _serviceUnavailable = serviceUnavailable;
    _dontCallCallback = dontCallCallback;
    _scheduledExecutorService = scheduledExecutorService;
  }

  @Override
  public void getClient(Request request, RequestContext requestContext, Callback<TransportClient> clientCallback)
  {
    if (_serviceUnavailable)
    {
      clientCallback.onError(new ServiceUnavailableException("bad", "bad"));
      return;
    }

    clientCallback.onSuccess(new TestClient(true, _dontCallCallback,
      TestClient.DEFAULT_REQUEST_TIMEOUT, _scheduledExecutorService));
  }

  @Override
  public void start(Callback<None> callback)
  {
    callback.onSuccess(None.none());
  }

  @Override
  public void shutdown(PropertyEventThread.PropertyEventShutdownCallback shutdown)
  {
    this.shutdown = true;
    shutdown.done();
  }

  @Override
  public void getLoadBalancedServiceProperties(String serviceName, Callback<ServiceProperties> clientCallback)
  {
    Map<String, Object> transportClientProperties = new HashMap<>();
    transportClientProperties.put(PropertyKeys.HTTP_REQUEST_TIMEOUT, TestClient.DEFAULT_REQUEST_TIMEOUT);

    ServiceProperties test = new ServiceProperties(serviceName, serviceName + "Cluster", "/" + serviceName,
      Collections.singletonList("test"), Collections.emptyMap(), transportClientProperties, Collections.emptyMap(),
      Collections.emptyList(), Collections.emptySet());
    clientCallback.onSuccess(test);
  }
}
