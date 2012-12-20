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

package com.linkedin.d2.balancer.util.hashing;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.Test;

public class ConsistentHashRingTest
{
  public static void main(String[] args)
  {
    new ConsistentHashRingTest().testManyItemsUnequalWeight();
  }

  @Test(groups = { "small", "back-end" })
  public void testZeroItems()
  {
    Map<String, Integer> zero = new HashMap<String, Integer>();
    ConsistentHashRing<String> test = new ConsistentHashRing<String>(zero);

    assertNull(test.get(0));

    zero.put("test", 0);
    test = new ConsistentHashRing<String>(zero);

    assertNull(test.get(100));
  }

  @Test(groups = { "small", "back-end" })
  public void testOneItem()
  {
    Map<String, Integer> one = new HashMap<String, Integer>();

    one.put("test", 100);

    ConsistentHashRing<String> test = new ConsistentHashRing<String>(one);

    // will generate ring:
    // [-2138377917, .., 2112547902]
    assertEquals(test.get(0), "test");
    int[] ring = test.getRing();

    // test low
    assertEquals(test.get(-2138377918), "test");

    // test high
    assertEquals(test.get(2112547903), "test");

    // test middle
    assertEquals(test.get(-2080272129), "test");

    // test direct hit
    assertEquals(test.get(-2080272130), "test");

    // test ring is sorted
    for (int i = 1; i < ring.length; ++i)
    {
      assertTrue(ring[i - 1] < ring[i]);
    }
  }

  @Test(groups = { "small", "back-end" })
  public void testManyItemsEqualWeight()
  {
    Map<String, Integer> many = new HashMap<String, Integer>();
    Map<String, AtomicInteger> counts = new HashMap<String, AtomicInteger>();

    for (int i = 0; i < 100; ++i)
    {
      many.put("test" + i, 10);
      counts.put("test" + i, new AtomicInteger());
    }

    ConsistentHashRing<String> test = new ConsistentHashRing<String>(many);

    assertNotNull(test.get(0));

    // verify that each test item has 10 points on the ring
    Object[] objects = test.getObjects();

    for (int i = 0; i < objects.length; ++i)
    {
      counts.get(objects[i].toString()).incrementAndGet();
    }

    for (Entry<String, AtomicInteger> count : counts.entrySet())
    {
      assertEquals(count.getValue().get(), 10);
    }
  }

  @Test(groups = { "small", "back-end" })
  public void testManyItemsUnequalWeight()
  {
    Map<Integer, Integer> many = new HashMap<Integer, Integer>();
    Map<Integer, AtomicInteger> counts = new HashMap<Integer, AtomicInteger>();

    for (int i = 0; i < 100; ++i)
    {
      many.put(i, i);
      counts.put(i, new AtomicInteger());
    }

    ConsistentHashRing<Integer> test = new ConsistentHashRing<Integer>(many);

    assertNotNull(test.get(0));

    // verify that each test item has proper points on the ring
    Object[] objects = test.getObjects();

    for (int i = 0; i < objects.length; ++i)
    {
      counts.get(objects[i]).incrementAndGet();
    }

    for (Entry<Integer, AtomicInteger> count : counts.entrySet())
    {
      assertEquals(count.getValue().get(), count.getKey().intValue());
    }
  }

  @Test(groups = { "small", "back-end" })
  public void test2ItemsWithOnePoint()
      throws URISyntaxException
  {
    Map<URI, Integer> pointsMap = new HashMap<URI, Integer>();
    URI uri1 = new URI("http://ext23.corp.linkedin.com:231/ajdi");
    URI uri2 = new URI("http://ext66.corp.linkedin.com:231/ajdi");
    pointsMap.put(uri1, 1);
    pointsMap.put(uri2, 1);
    ConsistentHashRing<URI> test = new ConsistentHashRing<URI>(pointsMap);
    //we will produce 2 points with value -590810423 for uri1 and 742698789 for uri2
    //test edge case
    URI lowEdgeUri = test.get(-600000000);
    URI highEdgeUri = test.get(800000000);
    URI midURI = test.get(0);
    assertEquals(lowEdgeUri, uri1);
    assertEquals(highEdgeUri, uri1);
    assertEquals(midURI, uri2);

  }
}
