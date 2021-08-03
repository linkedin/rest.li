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

package com.linkedin.data.it;


import com.linkedin.data.it.IteratorTestData.SimpleTestData;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Test {@link ValueAccumulator}
 */
public class TestValueAccumulator
{
  @Test
  public void testAccumulateByPath() throws Exception
  {
    SimpleTestData data = IteratorTestData.createSimpleTestData();

    List<Object> ids = new LinkedList<>();

    Builder.create(data.getDataElement(), IterationOrder.PRE_ORDER)
      .filterBy(Predicates.and(Predicates.pathMatchesPattern("foo", Wildcard.ANY_ONE, "id")))
      .accumulateValues(ids);

    assertEquals(3, ids.size());
    assertTrue(ids.contains(1));
    assertTrue(ids.contains(2));
    assertTrue(ids.contains(3));
  }

  @Test
  public void testAccumulateByPathAndFilter() throws Exception
  {
    SimpleTestData data = IteratorTestData.createSimpleTestData();

    List<Object> ids = new ArrayList<>(
        Builder.create(data.getDataElement(), IterationOrder.PRE_ORDER)
        .filterBy(Predicates.and(Predicates.pathMatchesPathSpec(IteratorTestData.PATH_TO_ID), IteratorTestData.LESS_THAN_3_CONDITION))
        .accumulateValues());

    assertEquals(2, ids.size());
    assertTrue(ids.contains(1));
    assertTrue(ids.contains(2));
  }
}
