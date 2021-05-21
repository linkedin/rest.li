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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.testng.annotations.Test;

import static com.linkedin.data.collections.TestCommonList.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TestCowList
{
  @Test
  public void testCopyOnWrite() throws CloneNotSupportedException
  {
    CowList<Integer> list1 = new CowList<Integer>(referenceList1);
    testAgainstReferenceList1(list1);
    assertEquals(list1.getRefCounted().getRefCount(), 0);

    CowList<Integer> list2 = list1.clone();
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertTrue(list2.getRefCounted() == list1.getRefCounted());
    testAgainstReferenceList1(list2);
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertTrue(list2.getRefCounted() == list1.getRefCounted());

    CowList<Integer> list3 = list1.clone();
    assertEquals(list1.getRefCounted().getRefCount(), 2);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    testAgainstReferenceList1(list3);
    assertEquals(list1.getRefCounted().getRefCount(), 2);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());

    list3.contains(referenceStart1);
    list3.get(1);
    list3.indexOf(referenceList1.get(3));
    list3.isEmpty();
    list3.lastIndexOf(referenceList1.get(3));
    list3.size();
    list3.toArray();
    list3.toArray(new Integer[0]);
    list3.equals("a");
    list3.equals(list1);
    list3.hashCode();
    list3.iterator();
    list3.listIterator();
    list3.listIterator(1);
    list3.subList(1, referenceCount1 - 1);
    list3.containsAll(referenceList1);
    list3.toString();
    assertTrue(list3.getRefCounted() == list1.getRefCounted());

    int size = list2.size();
    assertEquals(size, referenceList1.size());
    list2.add(-99);
    assertEquals(list2.get(size).intValue(), -99);
    contains(list2, -99);
    ++size;
    assertEquals(list2.size(), size);
    notContain(list1, -99);
    notContain(list3, -99);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list2.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list2.getRefCounted().getRefCount(), 0);

    CowList<Integer> list4 = list1.clone();
    list4.add(0, -99);
    assertEquals(list4.get(0).intValue(), -99);
    notContain(list1, -99);
    notContain(list3, -99);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list4.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list4.getRefCounted().getRefCount(), 0);

    CowList<Integer> list5 = list1.clone();
    list5.addAll(referenceList2);
    containsReferenceList2(list5);
    assertEquals(list5.get(list5.size() - referenceCount2), referenceList2.get(0));
    assertEquals(list5.get(list5.size() - 1), referenceList2.get(referenceList2.size() - 1));
    notContain(list1, referenceStart2);
    notContain(list1, referenceStart2 + referenceCount2 - 1);
    notContain(list3, referenceStart2);
    notContain(list3, referenceStart2 + referenceCount2 - 1);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list5.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list5.getRefCounted().getRefCount(), 0);

    CowList<Integer> list6 = list1.clone();
    list6.addAll(0, referenceList2);
    containsReferenceList2(list6);
    assertEquals(list6.get(0), referenceList2.get(0));
    assertEquals(list6.get(referenceCount2 - 1), referenceList2.get(referenceList2.size() - 1));
    notContain(list1, referenceStart2);
    notContain(list1, referenceStart2 + referenceCount2 - 1);
    notContain(list3, referenceStart2);
    notContain(list3, referenceStart2 + referenceCount2 - 1);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list6.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list6.getRefCounted().getRefCount(), 0);

    CowList<Integer> list7 = list1.clone();
    assertFalse(list7.isEmpty());
    list7.clear();
    assertTrue(list7.isEmpty());
    assertEquals(list7.size(), 0);
    assertFalse(list1.isEmpty());
    assertFalse(list3.isEmpty());
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list7.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list7.getRefCounted().getRefCount(), 0);

    CowList<Integer> list8 = list1.clone();
    list8.remove(0);
    assertEquals(list8.get(0).intValue(), referenceStart1 + 1);
    contains(list1, referenceStart1);
    contains(list3, referenceStart1);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list8.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list8.getRefCounted().getRefCount(), 0);

    CowList<Integer> list9 = list1.clone();
    list9.remove(Integer.valueOf(referenceStart1 + 1));
    assertEquals(list9.get(1).intValue(), referenceStart1 + 2);
    contains(list1, referenceStart1 + 1);
    contains(list3, referenceStart1 + 1);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list9.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list9.getRefCounted().getRefCount(), 0);

    CowList<Integer> list10 = list1.clone();
    assertFalse(list10.isEmpty());
    list10.removeAll(referenceList1.subList(0, 5));
    assertEquals(list10.size(), referenceCount1 - 5);
    assertEquals(list1.size(), referenceCount1);
    assertEquals(list3.size(), referenceCount1);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list10.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list10.getRefCounted().getRefCount(), 0);

    CowList<Integer> list11 = list1.clone();
    assertFalse(list11.isEmpty());
    list11.retainAll(referenceList1.subList(1, referenceCount1));
    assertEquals(list11.get(0).intValue(), referenceStart1 + 1);
    assertEquals(list11.size(), referenceCount1 - 1);
    assertTrue(list1.equals(referenceList1));
    assertTrue(list3.equals(referenceList1));
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list11.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list11.getRefCounted().getRefCount(), 0);

    CowList<Integer> list12 = list1.clone();
    list12.set(0, -66);
    assertEquals(list12.get(0).intValue(), -66);
    notContain(list1, -66);
    notContain(list3, -66);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list12.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list12.getRefCounted().getRefCount(), 0);

    list12.invalidate();
    assertTrue(list12.getRefCounted() == null);

    list12 = list1.clone();
    assertEquals(list1.getRefCounted().getRefCount(), 2);
    assertTrue(list12.getRefCounted() == list1.getRefCounted());
    list12.invalidate();
    assertEquals(list1.getRefCounted().getRefCount(), 1);


    /*
     * Iterators
     */

    CowList<Integer> list13 = list1.clone();
    Iterator<Integer> it13 = list13.iterator();
    assertTrue(it13.hasNext());
    assertEquals(it13.next().intValue(), referenceStart1);
    assertTrue(it13.hasNext());
    assertEquals(it13.next().intValue(), referenceStart1 + 1);
    assertEquals(list1.getRefCounted().getRefCount(), 2);
    it13.remove();
    assertEquals(list13.get(0).intValue(), referenceStart1);
    assertEquals(list13.get(1).intValue(), referenceStart1 + 2);
    contains(list1, referenceStart1 + 1);
    contains(list3, referenceStart1 + 1);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list13.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list13.getRefCounted().getRefCount(), 0);

    CowList<Integer> list14 = list1.clone();
    ListIterator<Integer> it14 = list14.listIterator();
    assertTrue(it14.hasNext());
    assertEquals(it14.next().intValue(), referenceStart1);
    assertTrue(it14.hasNext());
    assertEquals(it14.next().intValue(), referenceStart1 + 1);
    assertEquals(list1.getRefCounted().getRefCount(), 2);
    it14.add(-88);
    assertEquals(list14.get(1).intValue(), referenceStart1 + 1);
    assertEquals(list14.get(2).intValue(), -88);
    assertEquals(list14.get(3).intValue(), referenceStart1 + 2);
    notContain(list1, -88);
    notContain(list3, -88);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list14.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list14.getRefCounted().getRefCount(), 0);

    CowList<Integer> list15 = list1.clone();
    ListIterator<Integer> it15 = list15.listIterator(1);
    assertTrue(it15.hasNext());
    assertEquals(it15.next().intValue(), referenceStart1 + 1);
    assertEquals(list1.getRefCounted().getRefCount(), 2);
    it15.remove();
    assertEquals(list15.get(0).intValue(), referenceStart1);
    assertEquals(list15.get(1).intValue(), referenceStart1 + 2);
    contains(list1, referenceStart1 + 1);
    contains(list3, referenceStart1 + 1);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list15.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list15.getRefCounted().getRefCount(), 0);

    CowList<Integer> list16 = list1.clone();
    ListIterator<Integer> it16 = list16.listIterator();
    assertTrue(it16.hasNext());
    assertEquals(it16.next().intValue(), referenceStart1);
    assertTrue(it16.hasNext());
    assertEquals(it16.next().intValue(), referenceStart1 + 1);
    assertEquals(list1.getRefCounted().getRefCount(), 2);
    it16.set(-88);
    assertEquals(list16.get(0).intValue(), referenceStart1);
    assertEquals(list16.get(1).intValue(), -88);
    assertEquals(list16.get(2).intValue(), referenceStart1 + 2);
    notContain(list1, -88);
    notContain(list3, -88);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list16.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list16.getRefCounted().getRefCount(), 0);

    /*
     * SubList
     */

    CowList<Integer> list20 = list1.clone();
    List<Integer> sublist20 = list20.subList(0, referenceCount1 - 1);

    sublist20.contains(referenceStart1);
    sublist20.get(1);
    sublist20.indexOf(referenceList1.get(3));
    sublist20.isEmpty();
    sublist20.lastIndexOf(referenceList1.get(3));
    sublist20.size();
    sublist20.toArray();
    sublist20.toArray(new Integer[0]);
    sublist20.equals("a");
    sublist20.equals(list1);
    sublist20.hashCode();
    sublist20.iterator();
    sublist20.listIterator();
    sublist20.listIterator(1);
    sublist20.subList(1, referenceCount1 - 1);
    sublist20.containsAll(referenceList1);
    sublist20.toString();
    assertTrue(list20.getRefCounted() == list1.getRefCounted());

    assertEquals(sublist20.get(0).intValue(), referenceStart1);
    assertEquals(sublist20.get(sublist20.size() - 1).intValue(), referenceStart1 + referenceCount1 - 2);
    assertEquals(sublist20.size(), referenceCount1 - 1);
    assertEquals(list20.size(), referenceCount1);
    assertEquals(list1.size(), referenceCount1);
    assertEquals(list3.size(), referenceCount1);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list20.getRefCounted() == list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 2);
    sublist20.add(-88);
    assertEquals(sublist20.get(sublist20.size() - 1).intValue(), -88);
    assertEquals(list20.get(sublist20.size() - 1).intValue(), -88);
    assertFalse(list1.contains(-88));
    assertFalse(list3.contains(-88));
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list20.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list20.getRefCounted().getRefCount(), 0);

    CowList<Integer> list21 = list1.clone();
    List<Integer> sublist21 = list21.subList(0, referenceCount1 - 1);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list21.getRefCounted() == list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 2);
    sublist21.add(0, -88);
    assertEquals(sublist21.get(0).intValue(), -88);
    assertEquals(list21.get(0).intValue(), -88);
    assertFalse(list1.contains(-88));
    assertFalse(list3.contains(-88));
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list21.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list21.getRefCounted().getRefCount(), 0);

    CowList<Integer> list22 = list1.clone();
    List<Integer> sublist22 = list22.subList(1, 1);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list22.getRefCounted() == list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 2);
    sublist22.addAll(referenceList2);
    assertEquals(sublist22.get(0).intValue(), referenceStart2);
    assertEquals(list22.get(1).intValue(), referenceStart2);
    assertEquals(sublist22.get(referenceCount2 - 1).intValue(), referenceStart2 + referenceCount2 - 1);
    assertEquals(list22.get(1 + referenceCount2 - 1).intValue(), referenceStart2 + referenceCount2 - 1);
    assertFalse(list1.contains(referenceStart2));
    assertFalse(list3.contains(referenceStart2));
    assertFalse(list1.contains(referenceStart2 + referenceCount2 - 1));
    assertFalse(list3.contains(referenceStart2 + referenceCount2 - 1));
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list22.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list22.getRefCounted().getRefCount(), 0);

    CowList<Integer> list23 = list1.clone();
    List<Integer> sublist23 = list23.subList(1, 2);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list23.getRefCounted() == list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 2);
    sublist23.addAll(1, referenceList2);
    assertEquals(sublist23.get(1).intValue(), referenceStart2);
    assertEquals(list23.get(2).intValue(), referenceStart2);
    assertEquals(sublist23.get(1 + referenceCount2 - 1).intValue(), referenceStart2 + referenceCount2 - 1);
    assertEquals(list23.get(2 + referenceCount2 - 1).intValue(), referenceStart2 + referenceCount2 - 1);
    assertFalse(list1.contains(referenceStart2));
    assertFalse(list3.contains(referenceStart2));
    assertFalse(list1.contains(referenceStart2 + referenceCount2 - 1));
    assertFalse(list3.contains(referenceStart2 + referenceCount2 - 1));
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list23.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list23.getRefCounted().getRefCount(), 0);

    CowList<Integer> list24 = list1.clone();
    List<Integer> sublist24 = list24.subList(1, 3);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list24.getRefCounted() == list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 2);
    sublist24.clear();
    assertEquals(sublist24.size(), 0);
    assertEquals(list24.get(0).intValue(), referenceStart1);
    assertEquals(list24.get(1).intValue(), referenceStart1 + 3);
    assertTrue(list1.contains(referenceStart1 + 1));
    assertTrue(list3.contains(referenceStart1 + 1));
    assertTrue(list1.contains(referenceStart1 + 2));
    assertTrue(list3.contains(referenceStart1 + 2));
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list24.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list24.getRefCounted().getRefCount(), 0);

    CowList<Integer> list25 = list1.clone();
    List<Integer> sublist25 = list25.subList(1, 4);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list25.getRefCounted() == list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 2);
    sublist25.remove(1);
    assertEquals(sublist25.size(), 2);
    assertEquals(sublist25.get(0).intValue(), referenceStart1 + 1);
    assertEquals(sublist25.get(1).intValue(), referenceStart1 + 3);
    assertEquals(list25.get(1).intValue(), referenceStart1 + 1);
    assertEquals(list25.get(2).intValue(), referenceStart1 + 3);
    assertTrue(list1.contains(referenceStart1 + 2));
    assertTrue(list3.contains(referenceStart1 + 2));
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list25.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list25.getRefCounted().getRefCount(), 0);

    CowList<Integer> list26 = list1.clone();
    List<Integer> sublist26 = list26.subList(1, 4);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list26.getRefCounted() == list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 2);
    sublist26.remove(sublist26.get(1));
    assertEquals(sublist26.size(), 2);
    assertEquals(sublist26.get(0).intValue(), referenceStart1 + 1);
    assertEquals(sublist26.get(1).intValue(), referenceStart1 + 3);
    assertEquals(list26.get(1).intValue(), referenceStart1 + 1);
    assertEquals(list26.get(2).intValue(), referenceStart1 + 3);
    assertTrue(list1.contains(referenceStart1 + 2));
    assertTrue(list3.contains(referenceStart1 + 2));
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list26.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list26.getRefCounted().getRefCount(), 0);

    CowList<Integer> list27 = list1.clone();
    List<Integer> sublist27 = list27.subList(1, 4);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list27.getRefCounted() == list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 2);
    sublist27.removeAll(referenceList1.subList(2, 3));
    assertEquals(sublist27.size(), 2);
    assertEquals(sublist27.get(0).intValue(), referenceStart1 + 1);
    assertEquals(sublist27.get(1).intValue(), referenceStart1 + 3);
    assertEquals(list27.get(1).intValue(), referenceStart1 + 1);
    assertEquals(list27.get(2).intValue(), referenceStart1 + 3);
    assertTrue(list1.contains(referenceStart1 + 2));
    assertTrue(list3.contains(referenceStart1 + 2));
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list27.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list27.getRefCounted().getRefCount(), 0);

    CowList<Integer> list28 = list1.clone();
    List<Integer> sublist28 = list28.subList(1, 4);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list28.getRefCounted() == list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 2);
    List<Integer> sublist28a = new ArrayList<Integer>(referenceList1.subList(1, 4));
    sublist28a.remove(1);
    sublist28.retainAll(sublist28a);
    assertEquals(sublist28.size(), 2);
    assertEquals(sublist28.get(0).intValue(), referenceStart1 + 1);
    assertEquals(sublist28.get(1).intValue(), referenceStart1 + 3);
    assertEquals(list28.get(1).intValue(), referenceStart1 + 1);
    assertEquals(list28.get(2).intValue(), referenceStart1 + 3);
    assertTrue(list1.contains(referenceStart1 + 2));
    assertTrue(list3.contains(referenceStart1 + 2));
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list28.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list28.getRefCounted().getRefCount(), 0);

    CowList<Integer> list29 = list1.clone();
    List<Integer> sublist29 = list29.subList(1, referenceCount1 - 1).subList(0, 3);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list29.getRefCounted() == list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 2);
    List<Integer> sublist29a = new ArrayList<Integer>(referenceList1.subList(1, 4));
    sublist29a.remove(1);
    sublist29.retainAll(sublist29a);
    assertEquals(sublist29.size(), 2);
    assertEquals(sublist29.get(0).intValue(), referenceStart1 + 1);
    assertEquals(sublist29.get(1).intValue(), referenceStart1 + 3);
    assertEquals(list29.get(1).intValue(), referenceStart1 + 1);
    assertEquals(list29.get(2).intValue(), referenceStart1 + 3);
    assertTrue(list1.contains(referenceStart1 + 2));
    assertTrue(list3.contains(referenceStart1 + 2));
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list29.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list29.getRefCounted().getRefCount(), 0);

    /*
     * SubList Iterators
     */

    CowList<Integer> list40 = list1.clone();
    List<Integer> sublist40 = list40.subList(1, 4);
    Iterator<Integer> it40 = sublist40.iterator();
    assertTrue(it40.hasNext());
    assertEquals(it40.next().intValue(), referenceStart1 + 1);
    assertEquals(list1.getRefCounted().getRefCount(), 2);
    it40.remove();
    assertFalse(sublist40.contains(referenceStart1 + 1));
    assertEquals(sublist40.get(0).intValue(), referenceStart1 + 2);
    assertEquals(list40.get(0).intValue(), referenceStart1);
    assertEquals(list40.get(1).intValue(), referenceStart1 + 2);
    contains(list1, referenceStart1 + 1);
    contains(list3, referenceStart1 + 1);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list40.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list40.getRefCounted().getRefCount(), 0);

    CowList<Integer> list41 = list1.clone();
    List<Integer> sublist41 = list41.subList(1, 4);
    ListIterator<Integer> it41 = sublist41.listIterator();
    assertTrue(it41.hasNext());
    assertEquals(it41.next().intValue(), referenceStart1 + 1);
    assertEquals(list1.getRefCounted().getRefCount(), 2);
    it41.add(-88);
    assertTrue(sublist41.contains(-88));
    assertEquals(sublist41.get(1).intValue(), -88);
    assertEquals(list41.get(1).intValue(), referenceStart1 + 1);
    assertEquals(list41.get(2).intValue(), -88);
    assertEquals(list41.get(3).intValue(), referenceStart1 + 2);
    notContain(list1, -88);
    notContain(list3, -88);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list41.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list41.getRefCounted().getRefCount(), 0);

    CowList<Integer> list42 = list1.clone();
    List<Integer> sublist42 = list42.subList(1, 4);
    ListIterator<Integer> it42 = sublist42.listIterator(1);
    assertTrue(it42.hasNext());
    assertEquals(it42.next().intValue(), referenceStart1 + 2);
    assertEquals(list1.getRefCounted().getRefCount(), 2);
    it42.remove();
    assertFalse(sublist42.contains(referenceStart1 + 2));
    assertEquals(list42.get(1).intValue(), referenceStart1 + 1);
    assertEquals(list42.get(2).intValue(), referenceStart1 + 3);
    contains(list1, referenceStart1 + 2);
    contains(list3, referenceStart1 + 2);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list42.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list42.getRefCounted().getRefCount(), 0);

    CowList<Integer> list43 = list1.clone();
    List<Integer> sublist43 = list43.subList(1, 4);
    ListIterator<Integer> it43 = sublist43.listIterator();
    assertTrue(it43.hasNext());
    assertEquals(it43.next().intValue(), referenceStart1 + 1);
    assertEquals(list1.getRefCounted().getRefCount(), 2);
    it43.set(-88);
    assertEquals(sublist43.get(0).intValue(), -88);
    assertEquals(sublist43.get(1).intValue(), referenceStart1 + 2);
    assertEquals(list43.get(0).intValue(), referenceStart1);
    assertEquals(list43.get(1).intValue(), -88);
    assertEquals(list43.get(2).intValue(), referenceStart1 + 2);
    notContain(list1, -88);
    notContain(list3, -88);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list43.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list43.getRefCounted().getRefCount(), 0);

    CowList<Integer> list44 = list1.clone();
    List<Integer> sublist44 = list44.subList(1, referenceCount1 - 1);
    List<Integer> subsublist44 = sublist44.subList(1, 5);
    ListIterator<Integer> it44 = subsublist44.listIterator();
    assertTrue(it44.hasNext());
    assertEquals(it44.next().intValue(), referenceStart1 + 2);
    assertTrue(it44.hasNext());
    assertEquals(it44.next().intValue(), referenceStart1 + 3);
    assertEquals(list1.getRefCounted().getRefCount(), 2);
    it44.set(-88);
    assertEquals(subsublist44.get(1).intValue(), -88);
    assertEquals(subsublist44.get(2).intValue(), referenceStart1 + 4);
    assertEquals(sublist44.get(1).intValue(), referenceStart1 + 2);
    assertEquals(sublist44.get(2).intValue(), -88);
    assertEquals(sublist44.get(3).intValue(), referenceStart1 + 4);
    assertEquals(list44.get(2).intValue(), referenceStart1 + 2);
    assertEquals(list44.get(3).intValue(), -88);
    assertEquals(list44.get(4).intValue(), referenceStart1 + 4);
    notContain(list1, -88);
    notContain(list3, -88);
    assertTrue(list3.getRefCounted() == list1.getRefCounted());
    assertTrue(list44.getRefCounted() != list1.getRefCounted());
    assertEquals(list1.getRefCounted().getRefCount(), 1);
    assertEquals(list44.getRefCounted().getRefCount(), 0);
  }
}
