/*
   Copyright (c) 2020 LinkedIn Corp.

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

package com.linkedin.d2.balancer.util;

import java.net.URI;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestHostOverrideList {
  private static final String CLUSTER1 = "Cluster1";
  private static final String CLUSTER2 = "Cluster2";
  private static final String SERVICE1 = "Service1";
  private static final String SERVICE2 = "Service2";
  private static final URI URI1 = URI.create("https://uri1/path");
  private static final URI URI2 = URI.create("https://uri2/path");

  @Test
  public void testClusterOverride() {
    HostOverrideList overrides = new HostOverrideList();
    overrides.addClusterOverride(CLUSTER1, URI1);

    Assert.assertEquals(overrides.getOverride(CLUSTER1, SERVICE1), URI1);
    Assert.assertEquals(overrides.getOverride(CLUSTER1, SERVICE2), URI1);
    Assert.assertNull(overrides.getOverride(CLUSTER2, SERVICE1));
    Assert.assertNull(overrides.getOverride(CLUSTER2, SERVICE2));
  }

  @Test
  public void testServiceOverride() {
    HostOverrideList overrides = new HostOverrideList();
    overrides.addServiceOverride(SERVICE1, URI1);

    Assert.assertEquals(overrides.getOverride(CLUSTER1, SERVICE1), URI1);
    Assert.assertEquals(overrides.getOverride(CLUSTER2, SERVICE1), URI1);
    Assert.assertNull(overrides.getOverride(CLUSTER1, SERVICE2));
    Assert.assertNull(overrides.getOverride(CLUSTER2, SERVICE2));
  }

  @Test
  public void testOverride() {
    HostOverrideList overrides = new HostOverrideList();
    overrides.addOverride(URI1);

    Assert.assertEquals(overrides.getOverride(CLUSTER1, SERVICE1), URI1);
    Assert.assertEquals(overrides.getOverride(CLUSTER1, SERVICE2), URI1);
    Assert.assertEquals(overrides.getOverride(CLUSTER2, SERVICE1), URI1);
    Assert.assertEquals(overrides.getOverride(CLUSTER2, SERVICE2), URI1);
  }

  @Test
  public void testOverrideOrder() {
    HostOverrideList overrides = new HostOverrideList();
    overrides.addServiceOverride(SERVICE1, URI1);
    overrides.addServiceOverride(SERVICE1, URI2);
    Assert.assertEquals(overrides.getOverride(CLUSTER1, SERVICE1), URI1);
  }
}
