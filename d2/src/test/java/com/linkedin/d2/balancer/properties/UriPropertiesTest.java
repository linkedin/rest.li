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

package com.linkedin.d2.balancer.properties;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.linkedin.d2.util.TestDataHelper.*;


public class UriPropertiesTest
{
  @Test
  public void testUriProperties()
  {
    Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<>();
    uriData.put(URI_1, MAP_1);
    uriData.put(URI_2, MAP_2);
    uriData.put(URI_3, MAP_3);

    String clusterName = "TestCluster";
    UriProperties properties = new UriProperties(clusterName, uriData);

    // test construction
    Assert.assertEquals(clusterName, properties.getClusterName());
    Assert.assertEquals(properties.getPartitionDesc(), uriData);
    Assert.assertEquals(properties.Uris(), uriData.keySet());
    Assert.assertEquals(properties.getPartitionDataMap(URI_1), MAP_1);
    Assert.assertEquals(properties.getPartitionDataMap(URI_2), MAP_2);
    Assert.assertEquals(properties.getPartitionDataMap(URI_3), MAP_3);

    // test getUriBySchemeAndPartition
    Set<URI> set = new HashSet<>(1);
    set.add(URI_1);
    Assert.assertEquals(properties.getUriBySchemeAndPartition("http", 0), set);
    set.add(URI_2);
    Assert.assertEquals(properties.getUriBySchemeAndPartition("http", 1), set);
    set.clear();
    set.add(URI_3);
    Assert.assertEquals(properties.getUriBySchemeAndPartition("https", 1), set);
    Assert.assertNull(properties.getUriBySchemeAndPartition("rtp", 0));
    Assert.assertNull(properties.getUriBySchemeAndPartition("http", 2));

    // test unmodifiability
    Map<URI, Map<Integer, PartitionData>> partitionDesc = properties.getPartitionDesc();
    Map<Integer, PartitionData> partitionDataMap = properties.getPartitionDataMap(URI_1);
    URI testUri = URI.create("test");
    try
    {
      partitionDesc.put(testUri, null);
      Assert.fail("Should not be modifiable");
    }
    catch (UnsupportedOperationException ignored)
    {

    }
    try
    {
      partitionDataMap.put(1, new PartitionData(1));
      Assert.fail("Should not be modifiable");
    }
    catch (UnsupportedOperationException ignored)
    {

    }
  }
}
