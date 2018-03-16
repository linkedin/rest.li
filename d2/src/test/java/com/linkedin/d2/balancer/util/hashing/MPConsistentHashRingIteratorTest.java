/*
   Copyright (c) 2017 LinkedIn Corp.

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.testng.Assert;
import org.testng.annotations.Test;


public class MPConsistentHashRingIteratorTest {

  private final Random _random = new Random();

  private static Map<URI, Integer> buildPointsMap(int numHosts, int numPointsPerHost) {
    return IntStream.range(0, numHosts)
        .boxed()
        .collect(Collectors.toMap(key -> URI.create(String.format("app-%04d.linkedin.com", key)),
            value -> numPointsPerHost));
  }

  @Test
  public void testFirstItem() {
    Ring<URI> ring = new MPConsistentHashRing<URI>(buildPointsMap(100, 100));
    int key = _random.nextInt();
    Iterator<URI> iter = ring.getIterator(key);
    Assert.assertTrue(iter.hasNext());
    Assert.assertTrue(iter.next() == ring.get(key));
  }

  @Test
  public void testOtherItems() {
    Map<URI, Integer> pointsMap = buildPointsMap(100, 100);
    Ring<URI> ring = new MPConsistentHashRing<URI>(pointsMap);
    int key = _random.nextInt();
    Iterator<URI> iter = ring.getIterator(key);
    int iterations = 0;
    Set<URI> iterResults = new HashSet<>();
    while (iter.hasNext()) {
      iterResults.add(iter.next());
      iterations++;
    }

    //test iteration should equal to number of hosts so no duplicates
    Assert.assertTrue(iterations == 100);

    for (URI uri : pointsMap.keySet()) {
      Assert.assertTrue(iterResults.contains(uri));
    }
  }

  @Test
  public void testAgainstOldIterator() {
    Map<URI, Integer> pointsMap = buildPointsMap(100, 100);
    Ring<URI> ring = new MPConsistentHashRing<URI>(pointsMap);
    int key = _random.nextInt();

    Iterator<URI> oldIter = ((MPConsistentHashRing<URI>) ring).getOrderedIterator(key);
    Iterator<URI> newIter = ring.getIterator(key);

    Assert.assertTrue(oldIter.next() == newIter.next());
  }

  @Test(enabled = false)
  public void testNewIterPerformance() {
    int repeat = 10;
    Map<URI, Integer> pointsMap = buildPointsMap(4, 100);
    Ring<URI> ring = new MPConsistentHashRing<URI>(pointsMap, 21, 10);
    long start = 0;
    long end = 0;

    start = System.currentTimeMillis();
    for (int i = 0; i < repeat; i++) {
      int key = _random.nextInt();
      Iterator<URI> iter = ((MPConsistentHashRing<URI>) ring).getOrderedIterator(key);
      while (iter.hasNext()) {
        iter.next();
      }
    }
    end = System.currentTimeMillis();

    long elapsedOld = end - start;

    start = System.currentTimeMillis();
    for (int i = 0; i < repeat; i++) {
      int key = _random.nextInt();
      Iterator<URI> iter = ring.getIterator(key);
      while (iter.hasNext()) {
        iter.next();
      }
    }
    end = System.currentTimeMillis();

    long elapsedNew = end - start;

    Assert.assertTrue(elapsedNew < elapsedOld);
  }
}
