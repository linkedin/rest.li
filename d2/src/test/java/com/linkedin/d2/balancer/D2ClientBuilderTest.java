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
}
