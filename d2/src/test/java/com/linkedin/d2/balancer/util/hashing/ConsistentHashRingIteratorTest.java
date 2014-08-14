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

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ConsistentHashRingIteratorTest
{
  @Test
  public void testIterationFromBeginning()
  {
    final Integer[] objects = new Integer[]{1, 2, 3, 4, 5, 6};
    ConsistentHashRingIterator<Integer> iterator = new ConsistentHashRingIterator<Integer>(objects, 0);

    verifyIterator(iterator, objects, 0);
  }

  @Test
  public void testIterationFromMiddle()
  {
    final Integer[] objects = new Integer[]{1, 2, 3, 4, 5, 6};
    ConsistentHashRingIterator<Integer> iterator = new ConsistentHashRingIterator<Integer>(objects, 3);

    verifyIterator(iterator, objects, 3);
  }

  @Test
  public void testIterationFromEnd()
  {
    final Integer[] objects = new Integer[]{1, 2, 3, 4, 5, 6};
    ConsistentHashRingIterator<Integer> iterator = new ConsistentHashRingIterator<Integer>(objects, 5);

    verifyIterator(iterator, objects, 5);
  }

  @Test
  public void testEmptyIterator()
  {
    final Integer[] objects = new Integer[]{};
    ConsistentHashRingIterator<Integer> iterator = new ConsistentHashRingIterator<Integer>(objects, 0);

    verifyIterator(iterator, objects, 0);
  }

  public void verifyIterator(ConsistentHashRingIterator<Integer> iterator, Integer[] objects, int from)
  {
    int current = from;
    for (int i = 0; i < objects.length; i++)
    {
      Integer item = iterator.next();
      assertEquals(objects[current], item);
      current = (current + 1) % objects.length;
    }

    assertTrue(!iterator.hasNext());
  }
}
