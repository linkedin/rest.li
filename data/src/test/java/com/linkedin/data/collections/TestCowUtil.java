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


import java.util.Collection;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class TestCowUtil
{
  @Test
  public void testUnsafeClone()
  {
    final CowList<String> list = new CowList<String>();
    final CowSet<String> set = new CowSet<String>();
    final CowMap<String, String> map = new CowMap<String, String>();

    final CowList<String> listClone = CommonUtil.unsafeClone(list);
    final CowSet<String> setClone = CommonUtil.unsafeClone(set);
    final CowMap<String, String> mapClone = CommonUtil.unsafeClone(map);

    Assert.assertEquals(listClone, list);
    Assert.assertEquals(setClone, set);
    Assert.assertEquals(mapClone, map);

    mutateCollection(listClone);
    mutateCollection(setClone);
    mutateMap(mapClone);

    Assert.assertFalse(list.equals(listClone));
    Assert.assertFalse(set.equals(setClone));
    Assert.assertFalse(map.equals(mapClone));
  }

  @Test
  public void testUnsafeCloneSetReadOnly()
  {
    final CowList<String> list = new CowList<String>();
    final CowSet<String> set = new CowSet<String>();
    final CowMap<String, String> map = new CowMap<String, String>();

    final CowList<String> listClone = CommonUtil.unsafeCloneSetReadOnly(list);
    final CowSet<String> setClone = CommonUtil.unsafeCloneSetReadOnly(set);
    final CowMap<String, String> mapClone = CommonUtil.unsafeCloneSetReadOnly(map);

    Assert.assertEquals(listClone, list);
    Assert.assertEquals(setClone, set);
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
      mutateCollection(setClone);
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
    Assert.assertEquals(setClone, set);
    Assert.assertEquals(mapClone, map);

    mutateCollection(list);
    mutateCollection(set);
    mutateMap(map);

    Assert.assertFalse(list.equals(listClone));
    Assert.assertFalse(set.equals(setClone));
    Assert.assertFalse(map.equals(mapClone));
  }

  @Test
  public void testEmptyMap()
  {
    final CowMap<String, String> map = CowUtil.emptyMap();
    Assert.assertTrue(map.isEmpty());
    Assert.assertTrue(map.isReadOnly());

    try
    {
      mutateMap(map);
      Assert.fail("Should have thrown UnsupportedOperationException");
    }
    catch (UnsupportedOperationException e)
    {
      // Expected case
    }

    Assert.assertTrue(CowUtil.<Object, Object>emptyMap().isEmpty());
  }

  @Test
  public void testEmptySet()
  {
    final CowSet<String> set = CowUtil.emptySet();
    Assert.assertTrue(set.isEmpty());
    Assert.assertTrue(set.isReadOnly());

    try
    {
      mutateCollection(set);
      Assert.fail("Should have thrown UnsupportedOperationException");
    }
    catch (UnsupportedOperationException e)
    {
      // Expected case
    }

    Assert.assertTrue(CowUtil.<Object>emptySet().isEmpty());
  }

  @Test
  public void testEmptyList()
  {
    final CowList<String> list = CowUtil.emptyList();
    Assert.assertTrue(list.isEmpty());
    Assert.assertTrue(list.isReadOnly());

    try
    {
      mutateCollection(list);
      Assert.fail("Should have thrown UnsupportedOperationException");
    }
    catch (UnsupportedOperationException e)
    {
      // Expected case
    }

    Assert.assertTrue(CowUtil.<Object>emptyList().isEmpty());
  }

  private void mutateMap(CowMap<String, String> map)
  {
    map.put("key", "value");
  }

  private void mutateCollection(Collection<String> collection)
  {
    collection.add("test");
  }
}
