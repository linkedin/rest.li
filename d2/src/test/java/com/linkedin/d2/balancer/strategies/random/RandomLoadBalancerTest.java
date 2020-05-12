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

package com.linkedin.d2.balancer.strategies.random;

import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.r2.message.RequestContext;

import org.mockito.Mockito;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertNotNull;

public class RandomLoadBalancerTest
{

  @Test(groups = { "small", "back-end" })
  public void testRoundRobinBalancer()
  {
    RandomLoadBalancerStrategyFactory lbFactory = new RandomLoadBalancerStrategyFactory();
    RandomLoadBalancerStrategy rrLoadBalancer = lbFactory.newLoadBalancer(null);
    Map<Integer, PartitionData> partitionDataMap = new HashMap<Integer, PartitionData>(2);
    partitionDataMap.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
    TrackerClient trackerClient1 = Mockito.mock(TrackerClient.class);
    TrackerClient trackerClient2 = Mockito.mock(TrackerClient.class);
    Map<URI, TrackerClient> trackerClients = new HashMap<>();

    URI uri1 = URI.create("http://cluster-1/test");
    URI uri2 = URI.create("http://cluster-1/test");

    trackerClients.put(uri1, trackerClient1);
    trackerClients.put(uri2, trackerClient2);

    // test balancer with two clients, both available
    for (int i = 0; i < 100; ++i)
    {
      assertNotNull(rrLoadBalancer.getTrackerClient(null, new RequestContext(), 0, DefaultPartitionAccessor.DEFAULT_PARTITION_ID, trackerClients));
    }
  }
}
