/*
   Copyright (c) 2019 LinkedIn Corp.

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

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.D2Client;
import com.linkedin.d2.balancer.D2ClientBuilder;
import com.linkedin.d2.balancer.LastSeenBalancerWithFacilitiesFactory;
import com.linkedin.d2.balancer.LoadBalancerWithFacilitiesFactory;
import com.linkedin.d2.balancer.ZKFSLoadBalancerWithFacilitiesFactory;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.discovery.stores.zk.SharedZkConnectionProviderTest;
import com.linkedin.d2.discovery.stores.zk.ZKServer;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.test.util.AssertionMethods;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class D2LoadBalancerJmxTest
{

  private static final String DUMMY_STRING = "dummyString";
  private ZKServer _zkServer;
  private static final int ZK_PORT = 2120;
  private static final int ZK_TIMEOUT = 5000;

  @DataProvider
  public Object[][] loadBalancerFactories()
  {
    return new Object[][]{{new LastSeenBalancerWithFacilitiesFactory()}, {new ZKFSLoadBalancerWithFacilitiesFactory()}};
  }

  /**
   * Verify that all components are registered correctly at LB creation
   */
  @Test(dataProvider = "loadBalancerFactories")
  private void testRegisteringJmx(LoadBalancerWithFacilitiesFactory lbWithFacilitiesFactory)
    throws Exception
  {
    setUpZK();

    JmxManager jmxManager = mock(JmxManager.class);

    FutureCallback<None> startCallback = new FutureCallback<>();
    D2Client d2Client = getD2Client(lbWithFacilitiesFactory, jmxManager);
    d2Client.start(startCallback);
    startCallback.get();

    verify(jmxManager, times(1)).registerLoadBalancer(any(), any());
    verify(jmxManager, times(1)).registerLoadBalancerState(any(), any());

    // uri, service and cluster stores
    verify(jmxManager, times(3)).registerFileStore(any(), any());

    // ZK might take a little before booting up and registering the stores
    AssertionMethods.assertWithTimeout(10000, () -> {
      // uri store
      verify(jmxManager, times(1)).registerZooKeeperEphemeralStore(any(), any());
      // service and cluster stores
      verify(jmxManager, times(2)).registerZooKeeperPermanentStore(any(), any());
    });

    tearDownZK();
  }

  private D2Client getD2Client(LoadBalancerWithFacilitiesFactory lbWithFacilitiesFactory, JmxManager jmxManager)
  {
    Map<String, TransportClientFactory> transportClientFactoryMap = new HashMap<>();
    transportClientFactoryMap.put("http", new SharedZkConnectionProviderTest.TestTransportClientFactory());

    return new D2ClientBuilder()
                   .setZkHosts("localhost:" + ZK_PORT)
                   .setZkSessionTimeout(ZK_TIMEOUT, TimeUnit.MILLISECONDS)
                   .setLoadBalancerWithFacilitiesFactory(lbWithFacilitiesFactory)
                   .setClientFactories(transportClientFactoryMap)
                   .setD2JmxManager(jmxManager)
                   .build();
  }

  /**
   * NOTE: when you find yourself modifying this test, make sure you are modifying it in a BACKWARD-COMPATIBLE way.
   */
  @Test
  private void testD2ClientJmxManagerRegisteringStrategies()
  {
    JmxManager mockJmxManager = mock(JmxManager.class);
    D2ClientJmxManager d2ClientJmxManager = new D2ClientJmxManager(DUMMY_STRING, mockJmxManager);

    SimpleLoadBalancerState simpleLoadBalancerState = mock(SimpleLoadBalancerState.class);
    d2ClientJmxManager.setSimpleLoadBalancerState(simpleLoadBalancerState);

    ArgumentCaptor<SimpleLoadBalancerState.SimpleLoadBalancerStateListener> captor =
      ArgumentCaptor.forClass(SimpleLoadBalancerState.SimpleLoadBalancerStateListener.class);

    // check it is registering the strategy correctly
    Mockito.verify(simpleLoadBalancerState).register(captor.capture());
    captor.getValue().onStrategyAdded(DUMMY_STRING, DUMMY_STRING, mock(LoadBalancerStrategy.class));

    verify(mockJmxManager, times(1)).registerLoadBalancerStrategy(anyString(), any());
    verify(mockJmxManager, times(0)).unregister(anyString());

    // check it is unregistering correctly
    captor.getValue().onStrategyRemoved(DUMMY_STRING, DUMMY_STRING, mock(LoadBalancerStrategy.class));
    verify(mockJmxManager, times(1)).registerLoadBalancerStrategy(anyString(), any());
    verify(mockJmxManager, times(1)).unregister(anyString());

    // this should not trigger anything in the current version
    captor.getValue().onClientAdded(DUMMY_STRING, mock(TrackerClient.class));
    captor.getValue().onClientRemoved(DUMMY_STRING, mock(TrackerClient.class));
    verify(mockJmxManager, times(1)).registerLoadBalancerStrategy(anyString(), any());
    verify(mockJmxManager, times(1)).unregister(anyString());
  }

  // #################################### life cycle ####################################

  public void setUpZK()
    throws Exception
  {
    try
    {
      _zkServer = new ZKServer(ZK_PORT);
      _zkServer.startup();
    }
    catch (IOException e)
    {
      Assert.fail("unable to instantiate real zk server on port " + ZK_PORT);
    }
  }

  public void tearDownZK()
    throws IOException
  {
    _zkServer.shutdown();
  }
}
