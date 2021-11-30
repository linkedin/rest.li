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


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.it.IteratorTestData.SimpleTestData;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.transforms.Transform;
import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Test {@link Transformer}
 */
public class TestTransformer
{
  private static Transform<Object, Object> plusTenTransform = new Transform<Object, Object>()
      {
        @Override
        public Object apply(Object element)
        {
          Integer val = (Integer)element;
          return val + 10;
        }
      };

  @Test
  public void testTransformByPredicateAtPath() throws Exception
  {
    SimpleTestData data = IteratorTestData.createSimpleTestData();

    Builder.create(data.getDataElement(), IterationOrder.PRE_ORDER)
      .filterBy(Predicates.pathMatchesPattern("foo", Wildcard.ANY_ONE, "id"))
      .transform(plusTenTransform);

    assertEquals(data.getValue().getDataList("foo").getDataMap(0).getInteger("id").intValue(), 11);
    assertEquals(data.getValue().getDataList("foo").getDataMap(1).getInteger("id").intValue(), 12);
    assertEquals(data.getValue().getDataList("foo").getDataMap(2).getInteger("id").intValue(), 13);
  }

  @Test
  public void testReplaceByPredicateAtPath() throws Exception
  {
    SimpleTestData data = IteratorTestData.createSimpleTestData();

    Builder.create(data.getDataElement(), IterationOrder.PRE_ORDER)
    .filterBy(Predicates.and(Predicates.pathMatchesPathSpec(IteratorTestData.PATH_TO_ID), IteratorTestData.LESS_THAN_3_CONDITION))
    .replace(50);

    assertEquals(data.getValue().getDataList("foo").getDataMap(0).getInteger("id").intValue(), 50);
    assertEquals(data.getValue().getDataList("foo").getDataMap(1).getInteger("id").intValue(), 50);
  }

  @Test
  public void testReplaceByPredicate() throws Exception
  {
    SimpleTestData data = IteratorTestData.createSimpleTestData();

    Builder.create(data.getDataElement(), IterationOrder.PRE_ORDER)
    .filterBy(Predicates.pathMatchesPathSpec(IteratorTestData.PATH_TO_ID))
    .replace(100);

    assertEquals(data.getValue().getDataList("foo").getDataMap(0).getInteger("id").intValue(), 100);
    assertEquals(data.getValue().getDataList("foo").getDataMap(1).getInteger("id").intValue(), 100);
    assertEquals(data.getValue().getDataList("foo").getDataMap(2).getInteger("id").intValue(), 100);
  }

  @Test
  public void testReplaceAtPathNested() throws Exception
  {
    SimpleTestData data = IteratorTestData.createSimpleTestData();

    Builder.create(data.getDataElement(), IterationOrder.PRE_ORDER)
    .filterBy(Predicates.pathMatchesPathSpec(new PathSpec("nested", "nested")))
    .replace(new DataMap());

    int count = Builder.create(data.getDataElement(), IterationOrder.PRE_ORDER)
      .filterBy(Predicates.pathMatchesPathSpec(new PathSpec("nested", "nested", "foo")))
      .count();

    assertEquals(count, 0);
  }

  /**
   * Removes multiple nodes in a complex type, including non-leaf nodes.
   */
  @Test
  public void testReplaceByNameNested() throws Exception
  {
    SimpleTestData data = IteratorTestData.createSimpleTestData();

    Builder.create(data.getDataElement(), IterationOrder.PRE_ORDER)
    .filterBy(Predicates.nameEquals("foo"))
    .replace(new DataList());

    assertEquals(Builder.create(data.getDataElement(), IterationOrder.PRE_ORDER)
      .filterBy(Predicates.pathMatchesPathSpec(new PathSpec("nested", "nested", "foo", PathSpec.WILDCARD)))
      .count(), 0);

    assertEquals(Builder.create(data.getDataElement(), IterationOrder.PRE_ORDER)
      .filterBy(Predicates.pathMatchesPathSpec(new PathSpec("foo", PathSpec.WILDCARD)))
      .count(), 0);
  }

  @Test
  public void testReplaceBySchemaNameNested() throws Exception
  {
    SimpleTestData data = IteratorTestData.createSimpleTestData();

    Builder.create(data.getDataElement(), IterationOrder.PRE_ORDER)
        .filterBy(Predicates.dataSchemaNameEquals("Bar"))
        .replace(500);

    List<Object> accumulate = new ArrayList<>(Builder.create(data.getDataElement(), IterationOrder.PRE_ORDER)
        .filterBy(Predicates.pathMatchesPathSpec(new PathSpec("nested", "nested", "foo", PathSpec.WILDCARD)))
        .accumulateValues());

    assertEquals(accumulate.size(), 2);
    assertEquals(accumulate.get(0), 500);
    assertEquals(accumulate.get(1), 500);

    accumulate = new ArrayList<>(Builder.create(data.getDataElement(), IterationOrder.PRE_ORDER)
        .filterBy(Predicates.pathMatchesPathSpec(new PathSpec("foo", PathSpec.WILDCARD)))
        .accumulateValues());

    assertEquals(accumulate.size(), 3);
    assertEquals(accumulate.get(0), 500);
    assertEquals(accumulate.get(1), 500);
    assertEquals(accumulate.get(2), 500);
  }

  @Test
  public void testReplaceRoot() throws Exception
  {
    SimpleTestData data = IteratorTestData.createSimpleTestData();

    Object result = Builder.create(data.getDataElement(), IterationOrder.PRE_ORDER)
        .filterBy(Predicates.dataSchemaNameEquals("Foo"))
        .replace(new DataMap());

    assertTrue(result instanceof DataMap);
    assertEquals(((DataMap)result).size(), 0);
  }
}
