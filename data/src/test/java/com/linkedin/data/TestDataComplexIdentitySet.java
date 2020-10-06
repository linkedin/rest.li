/*
   Copyright (c) 2020 LinkedIn Corp.

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

package com.linkedin.data;

import org.testng.Assert;
import org.testng.annotations.Test;


public class TestDataComplexIdentitySet
{
  @Test
  void testBasicOperations() throws CloneNotSupportedException
  {
    DataComplexIdentitySet set = new DataComplexIdentitySet();
    DataMap dataMap = new DataMap();

    // Adding a DataComplex that doesn't exist should return false.
    Assert.assertFalse(set.add(dataMap));

    // Adding a DataComplex that already exists should return true.
    Assert.assertTrue(set.add(dataMap));

    DataMap clone = dataMap.clone();

    // Adding a clone, ie. equal but not the same instance must return false.
    Assert.assertFalse(set.add(clone));

    // Remove the original map.
    set.remove(dataMap);

    // Ensure that the original map got removed by testing that adding it again returns false.
    Assert.assertFalse(set.add(dataMap));

    // Ensure that the clone still exists in the map by testing that adding it again returns true.
    Assert.assertTrue(set.add(clone));

    // Create a new map and override its hashcode to be same as the original map.
    DataMap sameHashCodeMap = new DataMap();
    sameHashCodeMap._dataComplexHashCode = dataMap.dataComplexHashCode();

    // Ensure that adding the same hashcode map returns false the first time since it doesn't yet exist.
    Assert.assertFalse(set.add(sameHashCodeMap));

    // Ensure that adding the same hashcode map again returns true.
    Assert.assertTrue(set.add(sameHashCodeMap));

    // Ensure that the original and cloned maps still exist.
    Assert.assertTrue(set.add(dataMap));
    Assert.assertTrue(set.add(clone));

    // Remove the same hashcode map.
    set.remove(sameHashCodeMap);

    // Ensure that the same hashcode map got removed by testing that adding it again returns false.
    Assert.assertFalse(set.add(sameHashCodeMap));

    // Remove both data map and same hash code map.
    set.remove(dataMap);
    set.remove(sameHashCodeMap);

    // Add datamap
    set.add(dataMap);

    // Add same hashcode map.
    set.add(sameHashCodeMap);

    // Remove datamap
    set.remove(dataMap);

    // Ensure that same hashcode map still exists.
    Assert.assertTrue(set.add(sameHashCodeMap));

    // Ensure that datamap got removed.
    Assert.assertFalse(set.add(dataMap));
  }
}
