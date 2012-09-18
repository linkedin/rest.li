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


import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import com.linkedin.data.element.SimpleDataElement;
import com.linkedin.data.it.IteratorTestData.SimpleTestData;
import java.io.IOException;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


/**
 * Test for {@link Remover}.
 */
public class TestRemover
{
  public void testRemove(Object inputs[][]) throws IOException
  {
    for (Object[] row : inputs)
    {
      String inputJson = (String) row[0];
      Predicate predicate = (Predicate) row[1];
      String expectedJson = (String) row[2];

      DataMap inputMap = TestUtil.dataMapFromString(inputJson);
      
      DataMap resultMap = (DataMap) Builder.create(inputMap, null, IterationOrder.PRE_ORDER)
      .filterBy(predicate)
      .remove();

      DataMap expectedMap = expectedJson == null ? null : TestUtil.dataMapFromString(expectedJson);
      assertEquals(resultMap, expectedMap);
    }
  }

  @Test
  public void testRemoveFromList() throws IOException
  {
    Object inputs[][] =
      {
        {
          "{ \"array\" : [ 1, 1.0, 2, 2.0, 1, 1.0, \"a\" ] }",
          Predicates.valueInstanceOf(Integer.class),
          "{ \"array\" : [ 1.0, 2.0, 1.0, \"a\" ] }",
        },
        {
          "{ \"array\" : [ 1, 1.0, 2, 2.0, 1, 1.0 ] }",
          Predicates.valueInstanceOf(Number.class),
          "{ \"array\" : [ ] }",
        }
      };

    testRemove(inputs);
  }

  @Test
  public void testRemoveFromMap() throws IOException
  {
    Object inputs[][] =
      {
        {
          "{ \"f1\" : 1 }",
          Predicates.valueInstanceOf(Integer.class),
          "{ }",
        },
        {
          "{ \"f1\" : 1, \"f2\" : \"a\", \"f3\" : 2 }",
          Predicates.valueInstanceOf(Integer.class),
          "{ \"f2\" : \"a\" }",
        },
        {
          "{ \"f1\" : 1, \"f2\" : 1, \"f3\" : 1 }",
          Predicates.nameEquals("f2"),
          "{ \"f1\" : 1, \"f3\" : 1 }",
        }
      };

    testRemove(inputs);
  }

  @Test
  public void testRemoveNested() throws IOException
  {
    Object inputs[][] =
      {
        {
          "{ \"int1\" : 1, \"map1\" : { \"int2\" : 2, \"map2\" : { \"int3\" : 3, \"map3\" : { \"int4\" : 4 } } } }",
          Predicates.valueInstanceOf(Integer.class),
          "{ \"map1\" : { \"map2\" : { \"map3\" : { } } } }",
        },
        {
          "{ \"f\" : [ 1, [ 2, [ 3, [ 4, \"a\" ], \"b\"], \"c\" ], \"d\" ] }",
          Predicates.valueInstanceOf(Integer.class),
          "{ \"f\" : [ [ [ [ \"a\" ], \"b\"], \"c\"], \"d\" ] }",
        },
        {
          "{ \"int1\" : 1, \"map1\" : { \"int2\" : 2, \"map2\" : { \"int3\" : 3, \"map3\" : { \"int4\" : 4 } } } }",
          Predicates.valueInstanceOf(Integer.class),
          "{ \"map1\" : { \"map2\" : { \"map3\" : { } } } }",
        },
        {
          "{ \"array1\" : [ 2, { \"int3\" : 3, \"array3\" : [ 4, { \"int5\" : 5, \"array5\" : [ 6, \"a\" ] } ] } ] }",
          Predicates.valueInstanceOf(Integer.class),
          "{ \"array1\" : [ { \"array3\" : [ { \"array5\" : [ \"a\" ] } ] } ] }",
        },
      };

    testRemove(inputs);
  }

  @Test
  public void testRemoveALL() throws IOException
  {
    Object inputs[][] =
      {
        {
          "{ \"f1\" : 1 }",
          Predicates.alwaysTrue(),
          null,
        },
        {
          "{ \"f1\" : [ { \"a\" : 1 }, 2, \"b\" ] }",
          Predicates.alwaysTrue(),
          null,
        },
      };

    testRemove(inputs);
  }

  @Test
  public void testRemoveByPredicateAtPath() throws Exception
  {
    SimpleTestData data = IteratorTestData.createSimpleTestData();
    
    SimpleDataElement el = data.getDataElement();
    Builder.create(el.getValue(), el.getSchema(), IterationOrder.PRE_ORDER)
        .filterBy(Predicates.and(Predicates.pathMatchesPathSpec(IteratorTestData.PATH_TO_ID), IteratorTestData.LESS_THAN_3_CONDITION))
        .remove();
    
    assertTrue(data.getValue().getDataList("foo").getDataMap(0).getInteger("id") == null);
    assertTrue(data.getValue().getDataList("foo").getDataMap(1).getInteger("id") == null);
    assertEquals(3, data.getValue().getDataList("foo").getDataMap(2).getInteger("id").intValue());
  }

  @Test
  public void testRemoveByPredicate() throws Exception
  {
    SimpleTestData data = IteratorTestData.createSimpleTestData();
    
    SimpleDataElement el = data.getDataElement();
    Builder.create(el.getValue(), el.getSchema(), IterationOrder.PRE_ORDER)
    .filterBy(Predicates.pathMatchesPathSpec(IteratorTestData.PATH_TO_ID))
    .remove();

    assertTrue(data.getValue().getDataList("foo").getDataMap(0).getInteger("id") == null);
    assertTrue(data.getValue().getDataList("foo").getDataMap(1).getInteger("id") == null);
    assertTrue(data.getValue().getDataList("foo").getDataMap(2).getInteger("id") == null);
  }

  @Test
  public void testRemoveByPredicateWithPostOrder() throws Exception
  {
    SimpleTestData data = IteratorTestData.createSimpleTestData();
    
    SimpleDataElement el = data.getDataElement();
    Builder.create(el.getValue(), el.getSchema(), IterationOrder.POST_ORDER)
    .filterBy(Predicates.pathMatchesPathSpec(IteratorTestData.PATH_TO_ID))
    .remove();

    assertTrue(data.getValue().getDataList("foo").getDataMap(0).getInteger("id") == null);
    assertTrue(data.getValue().getDataList("foo").getDataMap(1).getInteger("id") == null);
    assertTrue(data.getValue().getDataList("foo").getDataMap(2).getInteger("id") == null);
  }
}
