/*
   Copyright (c) 2021 LinkedIn Corp.

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

import com.linkedin.data.DataMap;
import java.lang.ref.WeakReference;
import java.util.Collections;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author kbalasub
 */
public class TestCheckedMap
{
  @Test(timeOut = 3000)
  public void testPurgingStaleChangeListeners() throws InterruptedException
  {
    CheckedMap<String, Object> checkedMap = new CheckedMap<>();
    for (int i = 0; i < 1000; i++)
    {
      CheckedMap.ChangeListener<String, Object> listener = new CheckedMap.ChangeListener<String, Object>()
      {
        @Override
        public void onUnderlyingMapChanged(String key, Object value)
        {
          // Do nothing.
        }
      };
      checkedMap.addChangeListener(listener);
    }
    // Run gc to finalize weak references.
    while(checkedMap._changeListenerHead._object.get() != null)
    {
      System.gc();
    }
    // Sleep needed to ensure the reference queue is filled
    Thread.sleep(100);
    // Add one more to trigger and purge the change listeners list.
    checkedMap.addChangeListener((key, value) ->
    {
      // Do nothing.
    });
    Assert.assertTrue(sizeOf(checkedMap._changeListenerHead) < 1000);
  }

  private static int sizeOf(CheckedMap.WeakListNode<CheckedMap.ChangeListener<String, Object>> node)
  {
    int count = 0;
    while (node != null)
    {
      count++;
      node = node._next;
    }
    return count;
  }

  @Test
  public void testNoChangeListenerOnReadOnlyMap()
  {
    final DataMap map = new DataMap();
    map.setReadOnly();
    map.addChangeListener((key, value) ->
    {
      // Do nothing.
    });
    Assert.assertNull(map._changeListenerHead);
  }

  @Test
  public void testRemoveIf()
  {
    final DataMap map = new DataMap();
    map.put("key1", 100);
    map.put("key2", 200);
    map.put("key3", 500);

    Assert.assertFalse(map.removeIf(entry -> entry.getKey().equals("Unknown")));
    Assert.assertTrue(map.removeIf(entry -> entry.getKey().equals("key2") || ((Integer) entry.getValue() == 100)));
    Assert.assertEquals(map, Collections.singletonMap("key3", 500));
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testRemoveIfOnReadOnlyMap()
  {
    final DataMap map = new DataMap();
    map.put("key1", 100);
    map.setReadOnly();

    map.removeIf(entry -> entry.getKey().equals("Unknown"));
  }
}
