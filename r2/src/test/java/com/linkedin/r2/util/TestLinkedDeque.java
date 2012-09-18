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

/**
 * $Id: $
 */

package com.linkedin.r2.util;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Random;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class TestLinkedDeque
{

  @Test
  public void testAdd()
  {
    List<Integer> control = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    LinkedDeque<Integer> q = new LinkedDeque<Integer>(control);

    control.add(99);
    q.add(99);

    Assert.assertEquals(q, control);
  }

  @Test
  public void testAddLast()
  {
    List<Integer> control = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    LinkedDeque<Integer> q = new LinkedDeque<Integer>(control);

    control.add(99);
    q.addLast(99);

    Assert.assertEquals(q, control);
  }

  @Test
  public void testAddFirst()
  {
    List<Integer> control = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    LinkedDeque<Integer> q = new LinkedDeque<Integer>(control);

    control.add(0, 99);
    q.addFirst(99);

    Assert.assertEquals(q, control);
  }

  @Test
  public void testOffer()
  {
    List<Integer> control = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    LinkedDeque<Integer> q = new LinkedDeque<Integer>(control);

    control.add(99);
    Assert.assertTrue(q.offer(99));

    Assert.assertEquals(q, control);
  }

  @Test
  public void testOfferLast()
  {
    List<Integer> control = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    LinkedDeque<Integer> q = new LinkedDeque<Integer>(control);

    control.add(99);
    Assert.assertTrue(q.offerLast(99));

    Assert.assertEquals(q, control);
  }

  @Test
  public void testOfferFirst()
  {
    List<Integer> control = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    LinkedDeque<Integer> q = new LinkedDeque<Integer>(control);

    control.add(0, 99);
    Assert.assertTrue(q.offerFirst(99));

    Assert.assertEquals(q, control);
  }

  @Test
  public void testRemove()
  {
    List<Integer> control = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    LinkedDeque<Integer> q = new LinkedDeque<Integer>(control);

    Assert.assertEquals(q.remove(), control.remove(0));
    Assert.assertEquals(q, control);
  }

  @Test
  public void testRemoveFirst()
  {
    List<Integer> control = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    LinkedDeque<Integer> q = new LinkedDeque<Integer>(control);

    Assert.assertEquals(q.removeFirst(), control.remove(0));
    Assert.assertEquals(q, control);
  }

  @Test
  public void testRemoveLast()
  {
    List<Integer> control = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    LinkedDeque<Integer> q = new LinkedDeque<Integer>(control);

    Assert.assertEquals(q.removeLast(), control.remove(control.size() - 1));
    Assert.assertEquals(q, control);
  }

  @Test
  public void testPoll()
  {
    List<Integer> control = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    LinkedDeque<Integer> q = new LinkedDeque<Integer>(control);

    Assert.assertEquals(q.poll(), control.remove(0));
    Assert.assertEquals(q, control);
  }

  @Test
  public void testPollFirst()
  {
    List<Integer> control = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    LinkedDeque<Integer> q = new LinkedDeque<Integer>(control);

    Assert.assertEquals(q.pollFirst(), control.remove(0));
    Assert.assertEquals(q, control);
  }

  @Test
  public void testPollLast()
  {
    List<Integer> control = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    LinkedDeque<Integer> q = new LinkedDeque<Integer>(control);

    Assert.assertEquals(q.pollLast(), control.remove(control.size() - 1));
    Assert.assertEquals(q, control);
  }

  @Test
  public void testPeek()
  {
    List<Integer> control = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    LinkedDeque<Integer> q = new LinkedDeque<Integer>(control);

    Assert.assertEquals(q.peek(), control.get(0));
    Assert.assertEquals(q, control);
  }

  @Test
  public void testPeekFirst()
  {
    List<Integer> control = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    LinkedDeque<Integer> q = new LinkedDeque<Integer>(control);

    Assert.assertEquals(q.peekFirst(), control.get(0));
    Assert.assertEquals(q, control);
  }

  @Test
  public void testPeekLast()
  {
    List<Integer> control = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    LinkedDeque<Integer> q = new LinkedDeque<Integer>(control);

    Assert.assertEquals(q.peekLast(), control.get(control.size() - 1));
    Assert.assertEquals(q, control);
  }

  @Test
  public void testEmptyRemove()
  {
    LinkedDeque<Object> q = new LinkedDeque<Object>();
    try
    {
      q.remove();
      Assert.fail("remove on empty queue should fail");
    }
    catch (NoSuchElementException e)
    {
      // Expected
    }
  }

  @Test
  public void testEmptyRemoveFirst()
  {
    LinkedDeque<Object> q = new LinkedDeque<Object>();
    try
    {
      q.removeFirst();
      Assert.fail("removeFirst on empty queue should fail");
    }
    catch (NoSuchElementException e)
    {
      // Expected
    }
  }

  @Test
  public void testEmptyRemoveLast()
  {
    LinkedDeque<Object> q = new LinkedDeque<Object>();
    try
    {
      q.removeLast();
      Assert.fail("removeLast on empty queue should fail");
    }
    catch (NoSuchElementException e)
    {
      // Expected
    }
  }

  @Test
  public void testEmptyPoll()
  {
    LinkedDeque<Object> q = new LinkedDeque<Object>();
    Assert.assertNull(q.poll(), "poll on empty queue should return null");
  }

  @Test
  public void testEmptyPollFirst()
  {
    LinkedDeque<Object> q = new LinkedDeque<Object>();
    Assert.assertNull(q.pollFirst(), "pollFirst on empty queue should return null");
  }

  @Test
  public void testEmptyPollLast()
  {
    LinkedDeque<Object> q = new LinkedDeque<Object>();
    Assert.assertNull(q.pollLast(), "pollLast on empty queue should return null");
  }

  @Test
  public void testEmptyPeek()
  {
    LinkedDeque<Object> q = new LinkedDeque<Object>();
    Assert.assertNull(q.peek(), "peek on empty queue should return null");
  }

  @Test
  public void testEmptyPeekFirst()
  {
    LinkedDeque<Object> q = new LinkedDeque<Object>();
    Assert.assertNull(q.peekFirst(), "peekFirst on empty queue should return null");
  }

  @Test
  public void testEmptyPeekLast()
  {
    LinkedDeque<Object> q = new LinkedDeque<Object>();
    Assert.assertNull(q.peekLast(), "peekLast on empty queue should return null");
  }

  @Test
  public void testAddNull()
  {
    LinkedDeque<Object> q = new LinkedDeque<Object>();
    try
    {
      q.add(null);
      Assert.fail("add null should have failed");
    }
    catch (NullPointerException e)
    {
      // expected
    }
  }

  @Test
  public void testAddFirstNull()
  {
    LinkedDeque<Object> q = new LinkedDeque<Object>();
    try
    {
      q.addFirst(null);
      Assert.fail("addFirst null should have failed");
    }
    catch (NullPointerException e)
    {
      // expected
    }
  }

  @Test
  public void testAddLastNull()
  {
    LinkedDeque<Object> q = new LinkedDeque<Object>();
    try
    {
      q.addLast(null);
      Assert.fail("addLast null should have failed");
    }
    catch (NullPointerException e)
    {
      // expected
    }
  }

  @Test
  public void testOfferNull()
  {
    LinkedDeque<Object> q = new LinkedDeque<Object>();
    try
    {
      q.offer(null);
      Assert.fail("offer null should have failed");
    }
    catch (NullPointerException e)
    {
      // expected
    }
  }

  @Test
  public void testOfferFirstNull()
  {
    LinkedDeque<Object> q = new LinkedDeque<Object>();
    try
    {
      q.offerFirst(null);
      Assert.fail("offerFirst null should have failed");
    }
    catch (NullPointerException e)
    {
      // expected
    }
  }

  @Test
  public void testOfferLastNull()
  {
    LinkedDeque<Object> q = new LinkedDeque<Object>();
    try
    {
      q.offerLast(null);
      Assert.fail("offerLast null should have failed");
    }
    catch (NullPointerException e)
    {
      // expected
    }
  }

  @Test
  public void testForwardGeneral()
  {
    LinkedDeque<Object> q = new LinkedDeque<Object>();
    Queue<Object> control = new ArrayDeque<Object>();

    for (int i = 0; i < 10; i++)
    {
      for (int j = 0; j < i; j++)
      {
        Object o = new Object();
        q.add(o);
        control.add(o);
        Assert.assertEquals(q, control);
      }
      for (int j = 0; j < i; j++)
      {
        Object o = q.remove();
        Object o2 = control.remove();

        Assert.assertEquals(o, o2);
        Assert.assertEquals(q, control);
      }
    }
  }

  @Test
  public void testReverseGeneral()
  {
    LinkedDeque<Object> q = new LinkedDeque<Object>();
    Deque<Object> control = new ArrayDeque<Object>();

    for (int i = 0; i < 10; i++)
    {
      for (int j = 0; j < i; j++)
      {
        Object o = new Object();
        q.addFirst(o);
        control.addFirst(o);
        Assert.assertEquals(q, control);
      }
      for (int j = 0; j < i; j++)
      {
        Object o = q.removeLast();
        Object o2 = control.removeLast();

        Assert.assertEquals(o, o2);
        Assert.assertEquals(q, control);
      }
    }
  }

  @Test
  public void testEquals()
  {
    List<Integer> list = Arrays.asList(1, 2, 3);
    LinkedDeque<Integer> q = new LinkedDeque<Integer>(list);
    Assert.assertEquals(q, list);
    Assert.assertEquals(new LinkedDeque<Integer>(), Collections.emptyList());
    Assert.assertNotSame(q, Collections.emptyList());
  }

  @Test
  public void testEarlyRemoveFails()
  {
    LinkedDeque<Integer> q = new LinkedDeque<Integer>(Arrays.asList(1,2,3));
    try
    {
      q.iterator().remove();
    }
    catch (IllegalStateException e)
    {
      // Expected
    }
  }

  @Test
  public void testDoubleRemoveFails()
  {
    LinkedDeque<Integer> q = new LinkedDeque<Integer>(Arrays.asList(1,2,3));
    Iterator<Integer> i = q.iterator();
    i.next();
    i.remove();
    try
    {
      i.remove();
    }
    catch (IllegalStateException e)
    {
      // Expected
    }
  }

  @Test
  public void testIteratorRemoveHead()
  {
    testIteratorRemoval(0, 3, true);
  }

  @Test
  public void testIteratorRemoveMiddle()
  {
    testIteratorRemoval(1, 3, true);
  }

  @Test
  public void testIteratorRemoveTail()
  {
    testIteratorRemoval(2, 3, true);
  }

  @Test
  public void testIteratorRemoval()
  {
    for (int i = 1; i < 10; i++)
    {
      for (int j = 0; j < i; j++)
      {
        testIteratorRemoval(j, i, true);
      }
    }
  }

  @Test
  public void testDescIteratorRemoveHead()
  {
    testIteratorRemoval(0, 3, false);
  }

  @Test
  public void testDescIteratorRemoveMiddle()
  {
    testIteratorRemoval(1, 3, false);
  }

  @Test
  public void testDescIteratorRemoveTail()
  {
    testIteratorRemoval(2, 3, false);
  }

  @Test
  public void testDescIteratorRemoval()
  {
    for (int i = 1; i < 10; i++)
    {
      for (int j = 0; j < i; j++)
      {
        testIteratorRemoval(j, i, false);
      }
    }
  }

  private void testIteratorRemoval(int target, int size, boolean ascending)
  {
    try
    {
      List<Integer> list = new ArrayList<Integer>(size);
      for (int i = 0; i < size; i++)
      {
        list.add(i);
      }
      LinkedDeque<Integer> q = new LinkedDeque<Integer>(list);
      Iterator<Integer> it = (ascending ? q.iterator() : q.descendingIterator());
      for (int i = 0; i < target + 1; i++)
      {
        it.next();
      }
      it.remove();

      list.remove(ascending ? target : size - target - 1);

      Assert.assertEquals(q, list, "Iterator " + (ascending ? "ascending" : "descending") +
              " removal of " + target + " failed with list size " + size);
    }
    catch (Exception e)
    {
      Assert.fail("Iterator " + (ascending ? "ascending" : "descending") + " removal of " +
                          target + " failed with list size " + size, e);
    }
  }

  // This tests the LinkedDeque-specific methods which remove nodes from the interior of the queue.
  @Test
  public void bigTest()
  {
    Random rand = new Random(9939393);

    List<Object> control = new ArrayList<Object>();

    List<LinkedDeque.Node<Object>> nodes = new ArrayList<LinkedDeque.Node<Object>>();
    LinkedDeque<Object> queue =new LinkedDeque<Object>();

    for (int i = 0; i < 100000; i++)
    {
      int r = rand.nextInt(10);
      if (r < 6 || control.isEmpty())
      {
        Object o = new Object();
        control.add(o);
        nodes.add(queue.addLastNode(o));
      }
      else
      {
        int index = rand.nextInt(control.size());
        Object o = control.remove(index);
        LinkedDeque.Node<Object> node = nodes.remove(index);
        Object o2 = queue.removeNode(node);
        Assert.assertEquals(o, o2, "objects were not equal");
      }
    }

    Assert.assertEquals(control.size(), nodes.size());
    Assert.assertEquals(control.size(), queue.size());

    int size = control.size();
    for (int i = 0; i < size; i++)
    {
      Object o = control.remove(0);
      Object o2 = queue.poll();
      Assert.assertEquals(o, o2);
    }
    Assert.assertTrue(control.isEmpty());
    Assert.assertTrue(queue.isEmpty());
  }
}
