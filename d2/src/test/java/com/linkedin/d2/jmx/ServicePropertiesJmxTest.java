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
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState;
import com.linkedin.d2.balancer.util.canary.CanaryDistributionProvider;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class ServicePropertiesJmxTest
{
  @Mock
  SimpleLoadBalancerState _mockedSimpleBalancerState;

  @BeforeTest
  public void setUp()
  {
    MockitoAnnotations.initMocks(this);
    AtomicLong _mockedSimpleBalancerVersion = new AtomicLong(0);
    Mockito.when(_mockedSimpleBalancerState.getVersionAccess()).thenReturn(_mockedSimpleBalancerVersion);
  }

  @DataProvider(name = "getCanaryDistributionPoliciesTestData")
  public Object[][] getCanaryDistributionPoliciesTestData() {
    return new Object[][] {
        {CanaryDistributionProvider.Distribution.STABLE, 0},
        {CanaryDistributionProvider.Distribution.CANARY, 1},
    };
  }

  @Test(dataProvider = "getCanaryDistributionPoliciesTestData")
  public void testGetCanaryDistributionPolicy(CanaryDistributionProvider.Distribution distribution, int expectedValue)
  {
    ServicePropertiesJmx servicePropertiesJmx = new ServicePropertiesJmx(
        new LoadBalancerStateItem<>(
            new ServiceProperties("Foo", "Bar", "/", Collections.singletonList("Random")),
            0,
            0,
            distribution
        )
    );
    Assert.assertEquals(servicePropertiesJmx.getCanaryDistributionPolicy(), expectedValue);
  }
}
