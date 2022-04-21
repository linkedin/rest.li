/*
   Copyright (c) 2022 LinkedIn Corp.

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

import com.linkedin.d2.balancer.LoadBalancerStateItem;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.simple.ClusterInfoItem;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState;
import com.linkedin.d2.balancer.util.canary.CanaryDistributionProvider;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessor;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.junit.Assert;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;


public class JmxManagerTest {
  @Mock
  SimpleLoadBalancerState _mockedSimpleBalancerState;

  private JmxManager _jmxManager;
  private ClusterInfoItem _clusterInfoItem;
  private ClusterInfoJmx _clusterInfoJmx;
  private LoadBalancerStateItem<ServiceProperties> _servicePropertiesLBState;
  private ServicePropertiesJmx _servicePropertiesJmx;

  private void resetJmxObjects()
  {
    _clusterInfoItem =
        new ClusterInfoItem(_mockedSimpleBalancerState, new ClusterProperties("Foo"), new PartitionAccessor() {
          @Override
          public int getMaxPartitionId() {
            return 0;
          }

          @Override
          public int getPartitionId(URI uri) {
            return 0;
          }
        }, CanaryDistributionProvider.Distribution.CANARY);
    _clusterInfoJmx = new ClusterInfoJmx(_clusterInfoItem);
    _servicePropertiesLBState = new LoadBalancerStateItem<>(
        new ServiceProperties("Foo", "Bar", "/", Collections.singletonList("Random")),
        0,
        0,
        CanaryDistributionProvider.Distribution.CANARY
    );
    _servicePropertiesJmx = new ServicePropertiesJmx(_servicePropertiesLBState);
  }

  @BeforeMethod(firstTimeOnly = true)
  public void setUp()
  {
    MockitoAnnotations.initMocks(this);
    AtomicLong version = new AtomicLong(0);
    when(_mockedSimpleBalancerState.getVersionAccess()).thenReturn(version);
    _jmxManager = new JmxManager();
    resetJmxObjects();
  }

  @DataProvider(name = "getJmxBeansSourceObjects")
  public Object[][] getJmxBeansSourceObjects()
  {
    return new Object[][] {
        {_servicePropertiesLBState.getProperty()},
        {_clusterInfoItem}
    };
  }

  @Test(dataProvider = "getJmxBeansSourceObjects", invocationCount = 2)
  public void testRegisterJmxBeansSourceObjects(Object jmxBeanSourceObject)
  {
    String name = "Bar";
    ObjectName jmxObjName = null;
    try {
      jmxObjName = _jmxManager.getName(name);
    } catch (MalformedObjectNameException e) {
      Assert.fail("Unexpected bad JMX object name: " + e.getMessage());
    }
    if (jmxBeanSourceObject instanceof ServiceProperties) {
      LoadBalancerStateItem<ServiceProperties> servicePropertiesLoadBalancerStateItem =
          new LoadBalancerStateItem<>(
              (ServiceProperties) jmxBeanSourceObject,
              0,
              0,
              CanaryDistributionProvider.Distribution.CANARY);
      _jmxManager.registerServiceProperties(
          name, servicePropertiesLoadBalancerStateItem);
      try {
        Assert.assertEquals(
            _jmxManager.getMBeanServer().getAttribute(jmxObjName, "ServicePropertiesLBStateItem"),
            servicePropertiesLoadBalancerStateItem);
      } catch (Exception e) {
        Assert.fail("Failed to check MBean attribute: " + e.getMessage());
      }
    } else if (jmxBeanSourceObject instanceof ClusterInfoItem) {
      _jmxManager.registerClusterInfo(name, (ClusterInfoItem)jmxBeanSourceObject);
      try {
        Assert.assertEquals(
            _jmxManager.getMBeanServer().getAttribute(jmxObjName, "ClusterInfoItem"),
            jmxBeanSourceObject);
      } catch (Exception e) {
        Assert.fail("Failed to check MBean attribute: " + e.getMessage());
      }
    }
  }

  @DataProvider(name = "getJmxBeans")
  public Object[][] getJmxBeans()
  {
    return new Object[][] {
        {_servicePropertiesJmx},
        {_clusterInfoJmx}
    };
  }

  @Test(dataProvider = "getJmxBeans", invocationCount = 2)
  public void testRegisterJmxBeans(Object jmxBean)
  {
    String name = "Foo";
    ObjectName jmxObjName = null;
    try {
      jmxObjName = _jmxManager.getName(name);
    } catch (MalformedObjectNameException e) {
      Assert.fail("Unexpected bad JMX object name: " + e.getMessage());
    }
    if (jmxBean instanceof  ServicePropertiesJmx) {
      _jmxManager.registerServicePropertiesJmxBean(name, (ServicePropertiesJmx)jmxBean);
      try {
        Assert.assertEquals(
            _jmxManager.getMBeanServer().getAttribute(jmxObjName, "ServicePropertiesLBStateItem"),
            _servicePropertiesLBState);
      } catch (Exception e) {
        Assert.fail("Failed to check MBean attribute: " + e.getMessage());
      }
    } else if (jmxBean instanceof ClusterInfoJmx) {
      _jmxManager.registerClusterInfoJmxBean(name, (ClusterInfoJmx)jmxBean);
      try {
        Assert.assertEquals(
            _jmxManager.getMBeanServer().getAttribute(jmxObjName, "ClusterInfoItem"),
            _clusterInfoItem);
      } catch (Exception e) {
        Assert.fail("Failed to check MBean attribute: " + e.getMessage());
      }
    }
  }

  @Test(dataProvider = "getJmxBeans")
  public void testUnRegisterJmxBeans(Object jmxBean)
  {
    String name = "Foo";
    ObjectName jmxObjName = null;
    try {
      jmxObjName = _jmxManager.getName(name);
    } catch (MalformedObjectNameException e) {
      Assert.fail("Unexpected bad JMX object name: " + e.getMessage());
    }
    if (jmxBean instanceof  ServicePropertiesJmx) {
      _jmxManager.registerServicePropertiesJmxBean(name, (ServicePropertiesJmx)jmxBean);
      Assert.assertTrue(_jmxManager.getMBeanServer().isRegistered(jmxObjName));
      _jmxManager.unregister(name);
      Assert.assertFalse(_jmxManager.getMBeanServer().isRegistered(jmxObjName));
    } else if (jmxBean instanceof ClusterInfoJmx) {
      _jmxManager.registerClusterInfoJmxBean(name, (ClusterInfoJmx) jmxBean);
      Assert.assertTrue(_jmxManager.getMBeanServer().isRegistered(jmxObjName));
      _jmxManager.unregister(name);
      Assert.assertFalse(_jmxManager.getMBeanServer().isRegistered(jmxObjName));
    }
  }
}
