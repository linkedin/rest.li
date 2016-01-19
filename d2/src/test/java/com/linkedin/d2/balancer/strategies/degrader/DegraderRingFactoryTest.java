/*
   Copyright (c) 2016 LinkedIn Corp.

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

package com.linkedin.d2.balancer.strategies.degrader;


import com.linkedin.d2.balancer.util.hashing.ConsistentHashRing.Point;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;


public class DegraderRingFactoryTest
{
  private static final int DEFAULT_PARTITION_ID = DefaultPartitionAccessor.DEFAULT_PARTITION_ID;
  private static final int DEFAULT_CONSISTENT_HASH_VERSION = 1;

  public static void main(String[] args) throws URISyntaxException,
          InterruptedException
  {
    DegraderRingFactoryTest test = new DegraderRingFactoryTest();

    test.testPointsCleanUp();
  }

  private Map<String, Integer> buildPointsMap(int numOfPoints)
  {
    Map<String, Integer> newMap = new HashMap<>();

    String baseUri = "http://test.linkedin.com:";
    for (int i=0; i < numOfPoints; ++i)
    {
      newMap.put(baseUri + 1000 + i, 100);
    }
    return newMap;
  }

  @Test(groups = { "small", "back-end" })
  public void testPointsCleanUp()
          throws URISyntaxException
  {
    Map<String, Integer> pointsMp = buildPointsMap(5);

    DegraderRingFactory<String> ringFactory = new DegraderRingFactory<>();
    Ring<String>  ring = ringFactory.createRing(pointsMp);
    assertNotNull(ring.get(1000));

    pointsMp.remove("http://test.linkedin.com:10001");
    pointsMp.remove("http://test.linkedin.com:10003");

    ring = ringFactory.createRing(pointsMp);
    assertNotNull(ring.get(1000));
    // factory should keep all the points
    Map<String, List<Point<String>>> pointsMap = ringFactory.getPointsMap();
    assertEquals(pointsMap.size(), 5);

    pointsMp.remove("http://test.linkedin.com:10004");
    ring = ringFactory.createRing(pointsMp);
    assertNotNull(ring.get(1000));

    // factory should clean up and build new points
    pointsMap = ringFactory.getPointsMap();
    assertEquals(pointsMap.size(), 2);
  }

  @Test(groups = { "small", "back-end" })
  public void testPointsCleanUpLarge()
      throws URISyntaxException
  {
    Map<String, Integer> pointsMp = buildPointsMap(19);

    DegraderRingFactory<String> ringFactory = new DegraderRingFactory<>();
    Ring<String>  ring = ringFactory.createRing(pointsMp);
    assertNotNull(ring.get(1000));

    pointsMp.remove("http://test.linkedin.com:10001");
    pointsMp.remove("http://test.linkedin.com:10003");
    pointsMp.remove("http://test.linkedin.com:10006");

    ring = ringFactory.createRing(pointsMp);
    assertNotNull(ring.get(1000));
    // factory should keep all the points
    Map<String, List<Point<String>>> pointsMap = ringFactory.getPointsMap();
    assertEquals(pointsMap.size(), 19);

    pointsMp.remove("http://test.linkedin.com:10009");
    ring = ringFactory.createRing(pointsMp);
    assertNotNull(ring.get(1000));

    // factory should clean up and build new points
    pointsMap = ringFactory.getPointsMap();
    assertEquals(pointsMap.size(), 15);
  }
}

