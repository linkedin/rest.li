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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.servers.ZooKeeperAnnouncer;
import com.linkedin.d2.balancer.servers.ZooKeeperServer;

import org.junit.Assert;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * @author Nizar Mankulangara
 */
public class ZooKeeperAnnouncerJmxTest
{
  private ZooKeeperAnnouncerJmx _zooKeeperAnnouncerJmx;

  private static final String PARTITION_DATA_JSON =  "{\"1\":0.9,\"2\":1.5,\"29\":3.5}";

  @Mock
  ZooKeeperServer _zooKeeperServer;

  @BeforeTest
  protected void setUp() throws Exception
  {
    MockitoAnnotations.initMocks(this);
    _zooKeeperAnnouncerJmx = new ZooKeeperAnnouncerJmx(new ZooKeeperAnnouncer(_zooKeeperServer));
  }

  @Test
  public void setPartitionDataUsingJson() throws IOException
  {
    final Map<Integer,Double> partitionDataExpected = new HashMap<>();
    partitionDataExpected.put(1, 0.9);
    partitionDataExpected.put(2, 1.5);
    partitionDataExpected.put(29, 3.5);

    _zooKeeperAnnouncerJmx.setPartitionDataUsingJson(PARTITION_DATA_JSON);

    final Map<Integer, PartitionData> deserializedPartitionData = _zooKeeperAnnouncerJmx.getPartitionData();

    Assert.assertNotNull(deserializedPartitionData);
    Assert.assertEquals(deserializedPartitionData.size(), 3);
    for (Map.Entry<Integer,PartitionData> entry : deserializedPartitionData.entrySet())
    {
      Assert.assertTrue(partitionDataExpected.containsKey(entry.getKey()));
      PartitionData partitionData = deserializedPartitionData.get(entry.getKey());
      Assert.assertNotNull(partitionData);
      Assert.assertEquals(partitionDataExpected.get(entry.getKey()), (Double) partitionData.getWeight());
    }
  }
}
