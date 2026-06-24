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

package com.linkedin.d2.balancer;

import com.linkedin.d2.balancer.zkfs.ZKFSUtil;
import com.linkedin.r2.transport.common.TransportClientFactory;
import java.util.Collections;
import java.util.Map;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class D2ClientBuilderTest
{

  @DataProvider(name = "servicePaths")
  public Object[][] createServicePaths()
  {
    return new Object[][]{
        {null, ZKFSUtil.SERVICE_PATH},
        {"", ZKFSUtil.SERVICE_PATH},
        {"testValue", "testValue"},
      };
  }

  @Test(dataProvider = "servicePaths")
  void testD2ServicePathNotNull(String d2ServicePath, String expectedD2ServicePath)
  {
    D2ClientBuilder d2ClientBuilder = new D2ClientBuilder();
    d2ClientBuilder.setD2ServicePath(d2ServicePath);

    d2ClientBuilder.setLoadBalancerWithFacilitiesFactory(config -> {
      Assert.assertEquals(config.d2ServicePath, expectedD2ServicePath);
      return Mockito.mock(LoadBalancerWithFacilities.class);
    });
  }

  @Test
  void testSubscribeToIndisObserverClusterPropagatesToConfig()
  {
    // The flag set on the builder must reach the D2ClientConfig handed to the load balancer factory, i.e. it
    // must be carried through the (large) D2ClientConfig constructor rather than dropped. Capture that config
    // during build() and assert the flag is set. (XdsLoadBalancerWithFacilitiesFactory then forwards it to
    // XdsToD2PropertiesAdaptor.setSubscribeToIndisObserverCluster, which TestXdsToD2PropertiesAdaptor covers.)
    boolean[] factoryInvoked = {false};

    new D2ClientBuilder()
        .setClientFactories(emptyClientFactories()) // keep the test hermetic: no real transport factories
        .setSubscribeToIndisObserverCluster(true)
        .setLoadBalancerWithFacilitiesFactory(config -> {
          factoryInvoked[0] = true;
          Assert.assertTrue(config.subscribeToIndisObserverCluster,
              "subscribeToIndisObserverCluster set on the builder should propagate to the config given to the factory");
          return Mockito.mock(LoadBalancerWithFacilities.class);
        })
        .build();

    Assert.assertTrue(factoryInvoked[0], "build() should have invoked the load balancer factory");
  }

  @Test
  void testSubscribeToIndisObserverClusterDefaultsFalse()
  {
    boolean[] factoryInvoked = {false};

    new D2ClientBuilder()
        .setClientFactories(emptyClientFactories())
        .setLoadBalancerWithFacilitiesFactory(config -> {
          factoryInvoked[0] = true;
          Assert.assertFalse(config.subscribeToIndisObserverCluster,
              "subscribeToIndisObserverCluster should default to false when not set on the builder");
          return Mockito.mock(LoadBalancerWithFacilities.class);
        })
        .build();

    Assert.assertTrue(factoryInvoked[0], "build() should have invoked the load balancer factory");
  }

  private static Map<String, TransportClientFactory> emptyClientFactories()
  {
    return Collections.emptyMap();
  }
}
