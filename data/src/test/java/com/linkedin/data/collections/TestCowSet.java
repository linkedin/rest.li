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

/* $Id$ */
package com.linkedin.data.collections;


import java.util.Iterator;
import junit.framework.Assert;
import org.testng.annotations.Test;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class TestCowSet
{
  @Test
  public void testAdd()
  {
    final CowSet<String> set = new CowSet<String>();

    Assert.assertEquals(0, set.size());
    Assert.assertFalse(set.contains("test"));

    Assert.assertTrue(set.add("test"));
    Assert.assertTrue(set.contains("test"));
    Assert.assertEquals(1, set.size());

    Assert.assertFalse(set.add("test"));
    Assert.assertTrue(set.contains("test"));
    Assert.assertEquals(1, set.size());
  }

  @Test
  public void testRemove()
  {
    final CowSet<String> set = new CowSet<String>();
    set.add("test");

    Assert.assertTrue(set.remove("test"));
    Assert.assertFalse(set.contains("test"));
    Assert.assertEquals(0, set.size());

    Assert.assertFalse(set.remove("test"));
    Assert.assertFalse(set.contains("test"));
    Assert.assertEquals(0, set.size());
  }

  @Test
  public void testReadOnly()
  {
    final CowSet<String> set = new CowSet<String>();
    Assert.assertFalse(set.isReadOnly());

    set.add("test");

    Assert.assertTrue(set.contains("test"));
    Assert.assertEquals(1, set.size());

    set.setReadOnly();
    Assert.assertTrue(set.isReadOnly());

    try
    {
      set.add("test");
      Assert.fail("Should have thrown UnsupportedOperationException");
    }
    catch (UnsupportedOperationException e)
    {
      // Expected case
    }

    try
    {
      set.remove("test");
      Assert.fail("Should have thrown UnsupportedOperationException");
    }
    catch (UnsupportedOperationException e)
    {
      // Expected case
    }

    Assert.assertTrue(set.contains("test"));
    Assert.assertEquals(1, set.size());
  }

  @Test
  public void testClone() throws CloneNotSupportedException
  {
    final CowSet<String> set1 = new CowSet<String>();
    set1.add("test");

    @SuppressWarnings("unchecked")
    final CowSet<String> set2 = (CowSet<String>)set1.clone();
    @SuppressWarnings("unchecked")
    final CowSet<String> set3 = (CowSet<String>)set2.clone();

    Assert.assertEquals(set1, set2);
    Assert.assertEquals(set2, set3);

    set2.add("test2");
    Assert.assertFalse(set1.contains("test2"));
    Assert.assertTrue(set2.contains("test2"));
    Assert.assertFalse(set3.contains("test2"));
    Assert.assertFalse(set2.equals(set1));
    Assert.assertFalse(set2.equals(set3));

    set3.add("test3");
    Assert.assertFalse(set1.contains("test3"));
    Assert.assertFalse(set2.contains("test3"));
    Assert.assertTrue(set3.contains("test3"));
    Assert.assertFalse(set3.equals(set1));
    Assert.assertFalse(set3.equals(set2));

    set1.remove("test");
    Assert.assertFalse(set1.contains("test"));
    Assert.assertTrue(set2.contains("test"));
    Assert.assertTrue(set3.contains("test"));
    Assert.assertFalse(set1.equals(set2));
    Assert.assertFalse(set1.equals(set3));
  }

  @Test
  public void testModifyThroughIterator()
  {
    final CowSet<String> set = new CowSet<String>();
    set.add("test");
    set.setReadOnly();

    final Iterator<String> it = set.iterator();
    it.next();

    try
    {
      it.remove();
      Assert.fail("Should have thrown UnsupportedOperationException");
    }
    catch (UnsupportedOperationException e)
    {
      // Expected behavior
    }
  }
}
