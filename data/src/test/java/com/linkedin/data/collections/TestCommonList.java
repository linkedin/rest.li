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

package com.linkedin.data.collections;


import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class TestCommonList
{
  public static final PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out));

  public static final int referenceStart1 = 100;
  public static final int referenceCount1 = 10;
  public static final int referenceStart2 = 1000;
  public static final int referenceCount2 = 10;

  @SuppressWarnings("serial")
  public static final List<Integer> referenceList1 = new ArrayList<Integer>()
  {
    {
      for (Integer i = 0; i < referenceCount1; ++i)
      {
        add(i + referenceStart1);
      }
    }
  };
  @SuppressWarnings("serial")
  public static final List<Integer> referenceList2 = new ArrayList<Integer>()
  {
    {
      for (Integer i = 0; i < referenceCount2; ++i)
      {
        add(i + referenceStart2);
      }
    }
  };

  public static void testAgainstReferenceList1(List<Integer> list)
  {
    for (int i = 0; i < referenceCount1; ++i)
    {
      assertEquals(list.get(i).intValue(), i + referenceStart1);
      contains(list, i + referenceStart1);
    }
    int i = 0;
    for (int it : list)
    {
      assertEquals(it, i + referenceStart1);
      ++i;
    }
    i = 0;
    for (ListIterator<Integer> it = list.listIterator(); it.hasNext(); ++i)
    {
      assertEquals(it.nextIndex(), i);
      int value = it.next();
      assertEquals(i + referenceStart1, value);
    }
    i = list.size() - 1;
    for (ListIterator<Integer> it = list.listIterator(list.size()); it.hasPrevious(); --i)
    {
      assertEquals(it.previousIndex(), i);
      int value = it.previous();
      assertEquals(i + referenceStart1, value);
    }
    assertTrue(list.containsAll(referenceList1));
    assertEquals(list.size(), referenceCount1);
    assertEquals(list.hashCode(), referenceList1.hashCode());
    assertEquals(list, referenceList1);
    Object[] a = list.toArray();
    Integer[] ia = list.toArray(new Integer[0]);
    for (i = 0; i < referenceCount1; ++i)
    {
      assertEquals(i + referenceStart1, ((Integer) a[i]).intValue());
      assertEquals(i + referenceStart1, ia[i].intValue());
    }
  }

  public static void containsReferenceList2(List<Integer> list)
  {
    for (int i = 0; i < referenceCount2; ++i)
    {
      contains(list, i + referenceStart2);
    }
    assertTrue(list.containsAll(referenceList2));
  }

  public static boolean containsUsingIterator(List<Integer> list, Integer value)
  {
    boolean found = false;
    for (int it : list)
    {
      if (value.equals(it))
        found = true;
    }
    return found;
  }

  public static boolean containsUsingListIteratorNext(List<Integer> list, Integer value)
  {
    boolean found = false;
    for (ListIterator<Integer> it = list.listIterator(); it.hasNext();)
    {
      if (it.next().equals(value))
        found = true;
    }
    return found;
  }

  public static boolean containsUsingListIteratorPrevious(List<Integer> list, Integer value)
  {
    boolean found = false;
    for (ListIterator<Integer> it = list.listIterator(list.size()); it.hasPrevious();)
    {
      if (it.previous().equals(value))
        found = true;
    }
    return found;
  }

  public static boolean containsUsingToArray(List<Integer> list, Integer value)
  {
    boolean found = false;
    Object[] a = list.toArray();
    for (int i = 0; i < a.length; ++i)
    {
      if (a[i].equals(value))
        found = true;
    }
    return found;
  }

  public static boolean containsUsingToArrayTyped(List<Integer> list, Integer value)
  {
    boolean found = false;
    Integer[] a = list.toArray(new Integer[0]);
    for (int i = 0; i < a.length; ++i)
    {
      if (a[i].equals(value))
        found = true;
    }
    return found;
  }

  public static void contains(List<Integer> list, Integer value)
  {
    assertTrue(list.contains(value));
    assertTrue(list.indexOf(value) != -1);
    assertTrue(list.lastIndexOf(value) != -1);
    assertTrue(containsUsingIterator(list, value));
    assertTrue(containsUsingListIteratorNext(list, value));
    assertTrue(containsUsingListIteratorPrevious(list, value));
    assertTrue(containsUsingToArray(list, value));
    assertTrue(containsUsingToArrayTyped(list, value));
    assertEquals(list.get(list.indexOf(value)), value);
    assertEquals(list.get(list.lastIndexOf(value)), value);
  }

  public static void notContain(List<Integer> list, Integer value)
  {
    assertFalse(list.contains(value));
    assertTrue(list.indexOf(value) == -1);
    assertTrue(list.lastIndexOf(value) == -1);
    assertFalse(containsUsingIterator(list, value));
    assertFalse(containsUsingListIteratorNext(list, value));
    assertFalse(containsUsingListIteratorPrevious(list, value));
    assertFalse(containsUsingToArray(list, value));
    assertFalse(containsUsingToArrayTyped(list, value));
  }

  public static void verifyReadOnly(CommonList<Integer> list)
  {
    assertTrue(list.isReadOnly());
    verifyReadOnlyList(list, true);
  }

  public static void verifyReadOnlyList(List<Integer> list, boolean testSubList)
  {
    Exception exc;

    try
    {
      exc = null;
      list.add(-1);
    }
    catch (UnsupportedOperationException e)
    {
      exc = e;
    }

    try
    {
      exc = null;
      list.add(0, -1);
    }
    catch (UnsupportedOperationException e)
    {
      exc = e;
    }

    assertTrue(exc != null);
    try
    {
      exc = null;
      list.addAll(referenceList2);
    }
    catch (UnsupportedOperationException e)
    {
      exc = e;
    }
    assertTrue(exc != null);

    exc = null;
    try
    {
      exc = null;
      list.clear();
    }
    catch (UnsupportedOperationException e)
    {
      exc = e;
    }
    assertTrue(exc != null);

    exc = null;
    try
    {
      exc = null;
      list.clear();
    }
    catch (UnsupportedOperationException e)
    {
      exc = e;
    }
    assertTrue(exc != null);

    exc = null;
    try
    {
      exc = null;
      Iterator<Integer> it = list.iterator();
      assertTrue(it.hasNext());
      assertTrue(it.next() != null);
      it.remove();
    }
    catch (UnsupportedOperationException e)
    {
      exc = e;
    }
    assertTrue(exc != null);

    exc = null;
    try
    {
      exc = null;
      ListIterator<Integer> it = list.listIterator();
      assertTrue(it.hasNext());
      assertTrue(it.next() != null);
      it.remove();
    }
    catch (UnsupportedOperationException e)
    {
      exc = e;
    }
    assertTrue(exc != null);

    exc = null;
    try
    {
      exc = null;
      ListIterator<Integer> it = list.listIterator();
      assertTrue(it.hasNext());
      assertTrue(it.next() != null);
      it.set(-1);
    }
    catch (UnsupportedOperationException e)
    {
      exc = e;
    }
    assertTrue(exc != null);

    exc = null;
    try
    {
      exc = null;
      ListIterator<Integer> it = list.listIterator();
      assertTrue(it.hasNext());
      it.add(-1);
    }
    catch (UnsupportedOperationException e)
    {
      exc = e;
    }
    assertTrue(exc != null);

    if (testSubList)
    {
      verifyReadOnlyList(list.subList(0, list.size() - 1), false);
    }
  }

  @Test(dataProvider = "factories")
  public void testConstructor(CommonListFactory factory)
  {
    CommonList<Integer> list1 = factory.create();
    assertTrue(list1.isEmpty());
    assertEquals(list1.size(), 0);
    list1.addAll(referenceList1);
    testAgainstReferenceList1(list1);

    CommonList<Integer> list2 = factory.create(referenceList1);
    testAgainstReferenceList1(list2);

    CommonList<Integer> list3 = factory.create(100);
    list3.addAll(referenceList1);
    testAgainstReferenceList1(list3);
  }

  @Test(dataProvider = "factories")
  public void testReadOnly(CommonListFactory factory)
  {
    CommonList<Integer> list1 = factory.create(referenceList1);
    list1.setReadOnly();
    testAgainstReferenceList1(list1);
    verifyReadOnly(list1);
  }

  @Test(dataProvider = "factories")
  public void testClone(CommonListFactory factory) throws CloneNotSupportedException
  {
    CommonList<Integer> list1 = factory.create(referenceList1);
    assertFalse(list1.isReadOnly());
    CommonList<Integer> list2 = list1.clone();
    assertFalse(list2.isReadOnly());
    list2.setReadOnly();
    assertTrue(list2.isReadOnly());
    CommonList<Integer> list3 = list2.clone();
    assertFalse(list3.isReadOnly());
    assertTrue(list2.isReadOnly());
    assertFalse(list1.isReadOnly());
  }

  static class Checker<E> implements ListChecker<E>
  {
    int checkCount = 0;
    CommonList<E> lastList;
    E lastValue;

    @Override
    public void check(CommonList<E> list, E value)
    {
      checkCount++;
      lastList = list;
      lastValue = value;
    }
  }

  @Test(dataProvider = "factories")
  public void testChecker(CommonListFactory factory) throws CloneNotSupportedException
  {
    Checker<Integer> checker1 = new Checker<>();

    CommonList<Integer> list1 = factory.create(checker1);
    assertEquals(checker1.checkCount, 0);

    Checker<Integer> checker2 = new Checker<>();
    CommonList<Integer> list2 = factory.create(referenceList1, checker2);
    int expected2 = referenceList1.size();
    assertEquals(checker2.checkCount, expected2);

    CommonList<Integer> list3 = list1.clone();
    Integer i = -1;
    list3.add(i);
    int expected1 = 1;
    assertEquals(checker1.checkCount, expected1);
    assertSame(checker1.lastList, list3);
    assertSame(checker1.lastValue, i);

    CommonList<Integer> list4 = list2.clone();
    i = -2;
    list4.add(i);
    expected2++;
    assertEquals(checker2.checkCount, expected2);
    assertSame(checker2.lastList, list4);
    assertSame(checker2.lastValue, i);

    int size = list4.size();
    factory.addWithoutChecking(list4, null);
    assertEquals(checker2.checkCount, expected2);
    assertSame(checker2.lastList, list4);
    assertSame(checker2.lastValue, i);
    assertTrue(list4.get(size) == null);
    assertEquals(list4.size(), size + 1);

    list4.add(4, -3);
    expected2++;
    assertEquals(checker2.checkCount, expected2);

    list4.addAll(referenceList1);
    expected2 += referenceList1.size();
    assertEquals(checker2.checkCount, expected2);

    list4.addAll(4, referenceList1);
    expected2 += referenceList1.size();
    assertEquals(checker2.checkCount, expected2);

    list4.set(4, -4);
    expected2++;
    assertEquals(checker2.checkCount, expected2);

    ListIterator<Integer> it1 = list4.listIterator();
    it1.next();
    it1.add(-5);
    expected2++;
    assertEquals(checker2.checkCount, expected2);

    it1.next();
    it1.set(-6);
    expected2++;
    assertEquals(checker2.checkCount, expected2);

    list4.subList(1,4).add(-7);
    expected2++;
    assertEquals(checker2.checkCount, expected2);

    list4.subList(1,4).add(1, -8);
    expected2++;
    assertEquals(checker2.checkCount, expected2);

    list4.subList(1,4).addAll(referenceList1);
    expected2 += referenceList1.size();
    assertEquals(checker2.checkCount, expected2);

    list4.subList(1,4).addAll(1, referenceList1);
    expected2 += referenceList1.size();
    assertEquals(checker2.checkCount, expected2);

    list4.subList(1,6).set(4, -4);
    expected2++;
    assertEquals(checker2.checkCount, expected2);

    it1 = list4.subList(1,6).listIterator();
    it1.next();
    it1.add(-9);
    expected2++;
    assertEquals(checker2.checkCount, expected2);

    it1.next();
    it1.set(-10);
    expected2++;
    assertEquals(checker2.checkCount, expected2);

    list4.subList(1,6).subList(1,3).add(-11);
    expected2++;
    assertEquals(checker2.checkCount, expected2);

    it1 = list4.subList(1,6).subList(1,3).listIterator();
    it1.next();
    it1.add(-9);
    expected2++;
    assertEquals(checker2.checkCount, expected2);

    it1.next();
    it1.set(-10);
    expected2++;
    assertEquals(checker2.checkCount, expected2);
  }

  @DataProvider(name = "factories")
  public Object[][] listFactories()
  {
    return new Object[][] {
      { new CowListFactory() },
      { new CheckedListFactory() }
    };
  }

  public interface CommonListFactory
  {
    <E> CommonList<E> create();
    <E> CommonList<E> create(int initialCapacity);
    <E> CommonList<E> create(List<E> list);
    <E> CommonList<E> create(ListChecker<E> checker);
    <E> CommonList<E> create(List<E> list, ListChecker<E> checker);
    <E> void addWithoutChecking(List<E> list, E value);
  }

  public static class CowListFactory implements CommonListFactory
  {
    public <E> CommonList<E> create()
    {
      return new CowList<>();
    }
    public <E> CommonList<E> create(int initialCapacity)
    {
      return new CowList<>(initialCapacity);
    }
    public <E> CommonList<E> create(List<E> list)
    {
      return new CowList<>(list);
    }
    public <E> CommonList<E> create(ListChecker<E> checker)
    {
      return new CowList<>(checker);
    }
    public <E> CommonList<E> create(List<E> list, ListChecker<E> checker)
    {
      return new CowList<>(list, checker);
    }
    public <E> void addWithoutChecking(List<E> list, E value)
    {
      ((CowList<E>) list).addWithoutChecking(value);
    }
  }

  public static class CheckedListFactory implements CommonListFactory
  {
    public <E> CommonList<E> create()
    {
      return new CheckedList<>();
    }
    public <E> CommonList<E> create(int initialCapacity)
    {
      return new CheckedList<>(initialCapacity);
    }
    public <E> CommonList<E> create(List<E> list)
    {
      return new CheckedList<>(list);
    }
    public <E> CommonList<E> create(ListChecker<E> checker)
    {
      return new CheckedList<>(checker);
    }
    public <E> CommonList<E> create(List<E> list, ListChecker<E> checker)
    {
      return new CheckedList<>(list, checker);
    }
    public <E> void addWithoutChecking(List<E> list, E value)
    {
      ((CheckedList<E>) list).addWithoutChecking(value);
    }
  }
}
