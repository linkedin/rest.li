/*
   Copyright (c) 2014 LinkedIn Corp.

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

import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ConsistentHashRingIteratorTest
{
  public List<ConsistentHashRing.Point<Integer>> generatePoints(int num) {
    final List<ConsistentHashRing.Point<Integer>> points = new ArrayList<>();
    for (int i = 1; i <= num; ++i) {
      points.add(new ConsistentHashRing.Point<>(i, i));
    }
    return points;
  }

  @Test
  public void testIterationFromBeginning()
  {
    final List<ConsistentHashRing.Point<Integer>> objects = generatePoints(6);
    ConsistentHashRingIterator<Integer> iterator = new ConsistentHashRingIterator<>(objects, 0);

    verifyIterator(iterator, objects, 0);
  }

  @Test
  public void testIterationFromMiddle()
  {
    final List<ConsistentHashRing.Point<Integer>> objects = generatePoints(6);
    ConsistentHashRingIterator<Integer> iterator = new ConsistentHashRingIterator<>(objects, 3);

    verifyIterator(iterator, objects, 3);
  }

  @Test
  public void testIterationFromEnd()
  {
    final List<ConsistentHashRing.Point<Integer>> objects = generatePoints(6);
    ConsistentHashRingIterator<Integer> iterator = new ConsistentHashRingIterator<>(objects, 5);

    verifyIterator(iterator, objects, 5);
  }

  @Test
  public void testEmptyIterator()
  {
    final List<ConsistentHashRing.Point<Integer>> objects = new ArrayList<>();
    ConsistentHashRingIterator<Integer> iterator = new ConsistentHashRingIterator<>(objects, 0);

    verifyIterator(iterator, objects, 0);
  }

  public void verifyIterator(ConsistentHashRingIterator<Integer> iterator,
      List<ConsistentHashRing.Point<Integer>> objects, int from)
  {
    int current = from;
    for (int i = 0; i < objects.size(); i++)
    {
      Integer item = iterator.next();
      assertEquals(objects.get(current).getT(), item);
      current = (current + 1) % objects.size();
    }

    assertTrue(!iterator.hasNext());
  }
}
