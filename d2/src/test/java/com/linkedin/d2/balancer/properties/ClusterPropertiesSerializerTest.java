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

import com.linkedin.d2.discovery.PropertySerializationException;
import java.util.Arrays;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class ClusterPropertiesSerializerTest
{
  public static void main(String[] args) throws PropertySerializationException
  {
    new ClusterPropertiesSerializerTest().testClusterPropertiesSerializer();
  }

  @Test(groups = { "small", "back-end" })
  public void testClusterPropertiesSerializer() throws PropertySerializationException
  {
    ClusterPropertiesJsonSerializer foo = new ClusterPropertiesJsonSerializer();
    List<String> schemes = new ArrayList<String>();
    Map<String, String> supProperties = new HashMap<String, String>();

    ClusterProperties property = new ClusterProperties("test");
    assertEquals(foo.fromBytes(foo.toBytes(property)), property);

    property = new ClusterProperties("test", schemes);
    assertEquals(foo.fromBytes(foo.toBytes(property)), property);

    supProperties.put("foo", "bar");
    property = new ClusterProperties("test", schemes, supProperties);
    assertEquals(foo.fromBytes(foo.toBytes(property)), property);

    property = new ClusterProperties("test", schemes, null);
    assertEquals(foo.fromBytes(foo.toBytes(property)), property);


    RangeBasedPartitionProperties rbp = new RangeBasedPartitionProperties("blah", 0, 5000000, 100);
    property = new ClusterProperties("test", schemes, supProperties, new HashSet<URI>(), rbp);
    assertEquals(foo.fromBytes(foo.toBytes(property)), property);

    HashBasedPartitionProperties hbp = new HashBasedPartitionProperties("blah", 150, HashBasedPartitionProperties.HashAlgorithm.valueOf("md5".toUpperCase()));
    property = new ClusterProperties("test", schemes, supProperties, new HashSet<URI>(), hbp);
    assertEquals(foo.fromBytes(foo.toBytes(property)), property);

    property = new ClusterProperties("test", schemes, supProperties, new HashSet<URI>(), NullPartitionProperties.getInstance(),
        Arrays.asList("principal1", "principal2"));
    assertEquals(foo.fromBytes(foo.toBytes(property)), property);

    try
    {
      new HashBasedPartitionProperties("blah", 150, HashBasedPartitionProperties.HashAlgorithm.valueOf("sha-256"));
      fail("Should throw exception for unsupported algorithms");
    }
    catch(IllegalArgumentException e){}
  }
}
