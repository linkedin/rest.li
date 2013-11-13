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


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Map;


/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class TestCheckedUtil
{
  @Test
  public void testUnsafeClone()
  {
    final CheckedList<String> list = new CheckedList<String>();
    final CheckedMap<String, String> map = new CheckedMap<String, String>();

    final CheckedList<String> listClone = CommonUtil.unsafeClone(list);
    final CheckedMap<String, String> mapClone = CommonUtil.unsafeClone(map);

    Assert.assertEquals(listClone, list);
    Assert.assertEquals(mapClone, map);

    mutateCollection(listClone);
    mutateMap(mapClone);

    Assert.assertFalse(list.equals(listClone));
    Assert.assertFalse(map.equals(mapClone));
  }

  @Test
  public void testUnsafeCloneSetReadOnly()
  {
    final CheckedList<String> list = new CheckedList<String>();
    final CheckedMap<String, String> map = new CheckedMap<String, String>();

    final CheckedList<String> listClone = CommonUtil.unsafeCloneSetReadOnly(list);
    final CheckedMap<String, String> mapClone = CommonUtil.unsafeCloneSetReadOnly(map);

    Assert.assertEquals(listClone, list);
    Assert.assertEquals(mapClone, map);

    try
    {
      mutateCollection(listClone);
      Assert.fail("Should have thrown UnsupportedOperationException");
    }
    catch (UnsupportedOperationException e)
    {
      // Expected case
    }

    try
    {
      mutateMap(mapClone);
      Assert.fail("Should have thrown UnsupportedOperationException");
    }
    catch (UnsupportedOperationException e)
    {
      // Expected case
    }

    Assert.assertEquals(listClone, list);
    Assert.assertEquals(mapClone, map);

    mutateCollection(list);
    mutateMap(map);

    Assert.assertFalse(list.equals(listClone));
    Assert.assertFalse(map.equals(mapClone));
  }

  @Test(expectedExceptions = AssertionError.class)
  public void testAddCycleWithAssertChecking()
  {
    final DataList list = new DataList();
    CheckedUtil.addWithoutChecking(list, list);
  }

  @Test(groups = "withoutAssertion")
  public void testAddCycleWithoutChecking()
  {
    testAddCycleWithAssertChecking();
  }

  @Test(expectedExceptions = AssertionError.class)
  public void testSetCycleWithAssertChecking()
  {
    final DataList list = new DataList();
    CheckedUtil.addWithoutChecking(list, "not cycle");
    CheckedUtil.setWithoutChecking(list, 0, list);
  }

  @Test(groups = "withoutAssertion")
  public void testSetCycleWithoutChecking()
  {
    testSetCycleWithAssertChecking();
  }

  @Test(expectedExceptions = AssertionError.class)
  public void testPutCycleWithAssertChecking()
  {
    final DataMap map = new DataMap();
    CheckedUtil.putWithoutChecking(map, "cycle", map);
  }

  @Test(groups = "withoutAssertion")
  public void testPutCycleWithoutChecking()
  {
    testPutCycleWithAssertChecking();
  }

  private void mutateMap(Map<String, String> map)
  {
    map.put("key", "value");
  }

  private void mutateCollection(Collection<String> collection)
  {
    collection.add("test");
  }
}
