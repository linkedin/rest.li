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

public class UriPropertiesTest
{
  @Test
  public void testUriProperties()
  {
    URI uri1 = URI.create("http://google.com");
    URI uri2 = URI.create("http://linkedin.com");
    URI uri3 = URI.create("https://linkedin.com");

    Map<Integer, PartitionData> map1 = new HashMap<>();
    map1.put(0, new PartitionData(1));
    map1.put(1, new PartitionData(2));

    Map<Integer, PartitionData> map2 = new HashMap<>();
    map2.put(1, new PartitionData(0.5));

    Map<Integer, PartitionData> map3 = new HashMap<>();
    map3.put(1, new PartitionData(2));
    map3.put(3, new PartitionData(3.5));
    map3.put(4, new PartitionData(1));

    Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<>();
    uriData.put(uri1, map1);
    uriData.put(uri2, map2);
    uriData.put(uri3, map3);

    String clusterName = "TestCluster";
    UriProperties properties = new UriProperties(clusterName, uriData);

    // test construction
    Assert.assertEquals(clusterName, properties.getClusterName());
    Assert.assertEquals(properties.getPartitionDesc(), uriData);
    Assert.assertEquals(properties.Uris(), uriData.keySet());
    Assert.assertEquals(properties.getPartitionDataMap(uri1), map1);
    Assert.assertEquals(properties.getPartitionDataMap(uri2), map2);
    Assert.assertEquals(properties.getPartitionDataMap(uri3), map3);

    // test getUriBySchemeAndPartition
    Set<URI> set = new HashSet<>(1);
    set.add(uri1);
    Assert.assertEquals(properties.getUriBySchemeAndPartition("http", 0), set);
    set.add(uri2);
    Assert.assertEquals(properties.getUriBySchemeAndPartition("http", 1), set);
    set.clear();
    set.add(uri3);
    Assert.assertEquals(properties.getUriBySchemeAndPartition("https", 1), set);
    Assert.assertNull(properties.getUriBySchemeAndPartition("rtp", 0));
    Assert.assertNull(properties.getUriBySchemeAndPartition("http", 2));

    // test unmodifiability
    Map<URI, Map<Integer, PartitionData>> partitionDesc = properties.getPartitionDesc();
    Map<Integer, PartitionData> partitionDataMap = properties.getPartitionDataMap(uri1);
    URI testUri = URI.create("test");
    try
    {
      partitionDesc.put(testUri, null);
      Assert.fail("Should not be modifiable");
    }
    catch (UnsupportedOperationException e)
    {

    }
    try
    {
      partitionDataMap.put(1, new PartitionData(1));
      Assert.fail("Should not be modifiable");
    }
    catch (UnsupportedOperationException e)
    {

    }
  }
}
