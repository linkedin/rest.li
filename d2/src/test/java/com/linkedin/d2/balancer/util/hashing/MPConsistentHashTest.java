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

package com.linkedin.d2.balancer.util.hashing;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Ang Xu
 */
public class MPConsistentHashTest
{
  static final int TOTAL_COUNT = 1000000;

  @Test
  public void testFairness()
  {
    Map<Integer, Integer> dist = getDistribution(2, 1);
    Assert.assertTrue(getPeakToAvg(dist) < 1.1);
  }

  @Test
  public void testFairness2()
  {
    Map<Integer, Integer> dist = getDistribution(10, 1);
    Assert.assertTrue(getPeakToAvg(dist) < 1.1);
  }

  @Test
  public void testFairness3()
  {
    Map<Integer, Integer> dist = getDistribution(100, 1);
    Assert.assertTrue(getPeakToAvg(dist) < 1.1);
  }

  @Test
  public void testFairness4()
  {
    Map<Integer, Integer> dist = getDistribution(2, 100);
    Assert.assertTrue(getPeakToAvg(dist) < 1.1);
  }

  @Test
  public void testFairness5()
  {
    Map<Integer, Integer> dist = getDistribution(10, 100);
    Assert.assertTrue(getPeakToAvg(dist) < 1.1);
  }

  @Test
  public void testFairness6()
  {
    Map<Integer, Integer> dist = getDistribution(100, 100);
    Assert.assertTrue(getPeakToAvg(dist) < 1.1);
  }

  @Test
  public void testUnequalWeight()
  {
    int i = 12345;
    int j = 67890;
    double epsilon = 0.1;
    Map<Integer, Integer> pointsMap = new HashMap<>();
    pointsMap.put(i, 1);
    pointsMap.put(j, 100);
    Map<Integer, Integer> dist = getDistribution(pointsMap);

    double expectedPercentageI = 1.0f / 101;
    double expectedPercentageJ = 100.0f / 101;
    double actualPercentageI = (double)dist.get(i) / TOTAL_COUNT;
    double actualPercentageJ = (double)dist.get(j) / TOTAL_COUNT;

    Assert.assertTrue(Math.abs(actualPercentageI - expectedPercentageI) < expectedPercentageI * epsilon);
    Assert.assertTrue(Math.abs(actualPercentageJ - expectedPercentageJ) < expectedPercentageJ * epsilon);
  }

  @Test
  public void testUnequalWeight2()
  {
    double epsilon = 0.1;
    Map<Integer, Integer> pointsMap = new HashMap<>();
    pointsMap.put(1, 10);
    pointsMap.put(2, 20);
    pointsMap.put(3, 30);
    pointsMap.put(4, 40);

    Map<Integer, Integer> dist = getDistribution(pointsMap);

    double percent1 = (double)dist.get(1) / TOTAL_COUNT;
    double percent2 = (double)dist.get(2) / TOTAL_COUNT;
    double percent3 = (double)dist.get(3) / TOTAL_COUNT;
    double percent4 = (double)dist.get(4) / TOTAL_COUNT;

    Assert.assertTrue(Math.abs(percent1 - 0.1) < 0.1 * epsilon);
    Assert.assertTrue(Math.abs(percent2 - 0.2) < 0.2 * epsilon);
    Assert.assertTrue(Math.abs(percent3 - 0.3) < 0.3 * epsilon);
    Assert.assertTrue(Math.abs(percent4 - 0.4) < 0.4 * epsilon);
  }

  @Test
  public void testHashRingIterator()
  {
    Map<URI, Integer> pointsMap = new HashMap<>();
    pointsMap.put(URI.create("www.linkedin.com"), 100);
    pointsMap.put(URI.create("www.google.com"), 67);
    pointsMap.put(URI.create("www.facebook.com"), 33);
    pointsMap.put(URI.create("www.microsoft.com"), 15);
    MPConsistentHashRing<URI> hashRing = new MPConsistentHashRing<>(pointsMap);
    int key = new Random().nextInt();
    Iterator<URI> iter = hashRing.getIterator(key);

    while (iter.hasNext()) {
      URI nextUri = iter.next();
      Assert.assertEquals(nextUri, hashRing.get(key));
      // rebuild hash ring without the nextUri
      pointsMap.remove(nextUri);
      hashRing = new MPConsistentHashRing<>(pointsMap);
    }
    
    Assert.assertTrue(pointsMap.isEmpty());
  }


  private Map<Integer, Integer> getDistribution(int numHosts, int pointsPerHost)
  {
    Map<Integer, Integer> pointsMap = new HashMap<>();
    for (int i = 0; i < numHosts; i++)
    {
      pointsMap.put(i, pointsPerHost);
    }
    return getDistribution(pointsMap);
  }

  private Map<Integer, Integer> getDistribution(Map<Integer, Integer> pointsMap)
  {
    MPConsistentHashRing<Integer> hashRing = new MPConsistentHashRing<>(pointsMap);

    Map<Integer, Integer> counts = new HashMap<>();
    for (int i = 0; i < TOTAL_COUNT; i++)
    {
      Integer object = hashRing.get(i);
      int count = counts.computeIfAbsent(object, k -> 0);
      counts.put(object, count+1);
    }
    return counts;
  }

  private double getPeakToAvg(Map<Integer, Integer> distribution)
  {
    if (distribution.isEmpty())
    {
      return 0.0f;
    }

    int maxCount = Integer.MIN_VALUE;
    for (Integer count : distribution.values())
    {
      if (count > maxCount)
      {
        maxCount = count;
      }
    }

    double avgCount = TOTAL_COUNT / distribution.size();
    System.out.println((double) maxCount / avgCount);
    return maxCount / avgCount;
  }

}
