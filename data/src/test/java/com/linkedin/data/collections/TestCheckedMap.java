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
    while(checkedMap._changeListeners.get(0).get() != null)
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
    Assert.assertTrue(checkedMap._changeListeners.size() < 1000);
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
    Assert.assertNull(map._changeListeners);
  }
}
