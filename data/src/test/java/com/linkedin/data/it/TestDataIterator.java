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


import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.element.DataElement;
import com.linkedin.data.element.SimpleDataElement;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.data.it.Predicates.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;


/**
 * @author slim
 */
public class TestDataIterator
{
  protected boolean debug = false;

  public Object jsonToObject(String s) throws IOException
  {
    JacksonDataCodec codec = new JacksonDataCodec();
    String input = "{ \"key\" : " + s + " }";
    DataMap map = codec.bytesToMap(input.getBytes(Data.UTF_8_CHARSET));
    return map.get("key");
  }

  public String traverse(DataElement element, IterationOrder order, boolean usePath)
  {
    String spaces = "                                          ";

    StringBuilder s = new StringBuilder();
    DataIterator it = Builder.create(element, order).dataIterator();
    DataElement current;
    while ((current = it.next()) != null)
    {
      int level = current.level();
      s.append(spaces, 0, level);
      if (usePath)
      {
        s.append("path=").append(current.pathAsString());
      }
      else
      {
        s.append("name=").append(current.getName());
      }
      s.append(", class=").append(current.getValue().getClass().getName());
      if ((current.getValue() instanceof DataComplex) == false)
      {
        s.append(", value=").append(current.getValue().toString());
      }
      s.append("\n");
    }
    return s.toString();
  }

  public void traverseAndCheck(Object root, String preExpected, String postExpected)
  {
    DataElement element = new SimpleDataElement(root, null);
    traverseAndCheckWithDataElement(element, preExpected, postExpected, false);
  }

  public void traverseAndCheckWithDataElement(DataElement element, String preExpected, String postExpected, boolean usePath)
  {
    String preResult = traverse(element, IterationOrder.PRE_ORDER, usePath);
    assertEquals(preResult, preExpected);
    String postResult = traverse(element, IterationOrder.POST_ORDER, usePath);
    assertEquals(postResult, postExpected);
  }

  @Test
  public void testNoSchemaDataMapRoot()
  {
    DataMap root = new DataMap();
    root.put("boolean", false);
    root.put("int", 1);
    root.put("long", 2L);
    root.put("float", 3.0f);
    root.put("double", 4.0);
    root.put("string", "foo");
    root.put("bytes", ByteString.copyAvroString("abc", false));

    String preExpected =
      "name=" + DataElement.ROOT_NAME + ", class=com.linkedin.data.DataMap\n" +
      " name=bytes, class=com.linkedin.data.ByteString, value=abc\n" +
      " name=int, class=java.lang.Integer, value=1\n" +
      " name=string, class=java.lang.String, value=foo\n" +
      " name=boolean, class=java.lang.Boolean, value=false\n" +
      " name=double, class=java.lang.Double, value=4.0\n" +
      " name=long, class=java.lang.Long, value=2\n" +
      " name=float, class=java.lang.Float, value=3.0\n";

    String postExpected =
      " name=bytes, class=com.linkedin.data.ByteString, value=abc\n" +
      " name=int, class=java.lang.Integer, value=1\n" +
      " name=string, class=java.lang.String, value=foo\n" +
      " name=boolean, class=java.lang.Boolean, value=false\n" +
      " name=double, class=java.lang.Double, value=4.0\n" +
      " name=long, class=java.lang.Long, value=2\n" +
      " name=float, class=java.lang.Float, value=3.0\n" +
      "name=" + DataElement.ROOT_NAME + ", class=com.linkedin.data.DataMap\n";

    traverseAndCheck(root, preExpected, postExpected);
  }

  @Test
  public void testNoSchemaDataListRoot()
  {
    DataList root = new DataList();
    root.add(false);
    root.add(1);
    root.add(2L);
    root.add(3.0f);
    root.add(4.0);
    root.add("foo");
    root.add(ByteString.copyAvroString("abc", false));

    String preExpected =
      "name=" + DataElement.ROOT_NAME + ", class=com.linkedin.data.DataList\n" +
      " name=0, class=java.lang.Boolean, value=false\n" +
      " name=1, class=java.lang.Integer, value=1\n" +
      " name=2, class=java.lang.Long, value=2\n" +
      " name=3, class=java.lang.Float, value=3.0\n" +
      " name=4, class=java.lang.Double, value=4.0\n" +
      " name=5, class=java.lang.String, value=foo\n" +
      " name=6, class=com.linkedin.data.ByteString, value=abc\n";

    String postExpected =
      " name=0, class=java.lang.Boolean, value=false\n" +
      " name=1, class=java.lang.Integer, value=1\n" +
      " name=2, class=java.lang.Long, value=2\n" +
      " name=3, class=java.lang.Float, value=3.0\n" +
      " name=4, class=java.lang.Double, value=4.0\n" +
      " name=5, class=java.lang.String, value=foo\n" +
      " name=6, class=com.linkedin.data.ByteString, value=abc\n" +
      "name=" + DataElement.ROOT_NAME + ", class=com.linkedin.data.DataList\n";

    traverseAndCheck(root, preExpected, postExpected);
  }

  @Test
  public void testNoSchemaNested() throws IOException
  {
    String[][] data =
      {
        {
          // map of array
          "{ \"aKey\" : [ 1, 2 ], \"bKey\" : [ 1.0, 2.0 ] }",
          // pre-order
          "name=" + DataElement.ROOT_NAME + ", class=com.linkedin.data.DataMap\n" +
          " name=bKey, class=com.linkedin.data.DataList\n" +
          "  name=0, class=java.lang.Double, value=1.0\n" +
          "  name=1, class=java.lang.Double, value=2.0\n" +
          " name=aKey, class=com.linkedin.data.DataList\n" +
          "  name=0, class=java.lang.Integer, value=1\n" +
          "  name=1, class=java.lang.Integer, value=2\n",
          // post-order
          "  name=0, class=java.lang.Double, value=1.0\n" +
          "  name=1, class=java.lang.Double, value=2.0\n" +
          " name=bKey, class=com.linkedin.data.DataList\n" +
          "  name=0, class=java.lang.Integer, value=1\n" +
          "  name=1, class=java.lang.Integer, value=2\n" +
          " name=aKey, class=com.linkedin.data.DataList\n" +
          "name=" + DataElement.ROOT_NAME + ", class=com.linkedin.data.DataMap\n"
        },
        {
          // array of maps
          "[ { \"aKey\" : 1 }, { \"bKey\" : 2.0 } ]",
          // pre-order
          "name=" + DataElement.ROOT_NAME + ", class=com.linkedin.data.DataList\n" +
          " name=0, class=com.linkedin.data.DataMap\n" +
          "  name=aKey, class=java.lang.Integer, value=1\n" +
          " name=1, class=com.linkedin.data.DataMap\n" +
          "  name=bKey, class=java.lang.Double, value=2.0\n",
          // post-order
          "  name=aKey, class=java.lang.Integer, value=1\n" +
          " name=0, class=com.linkedin.data.DataMap\n" +
          "  name=bKey, class=java.lang.Double, value=2.0\n" +
          " name=1, class=com.linkedin.data.DataMap\n" +
          "name=" + DataElement.ROOT_NAME + ", class=com.linkedin.data.DataList\n",
        }
      };

    for (String[] row : data)
    {
      Object o = jsonToObject(row[0]);
      traverseAndCheck(o, row[1], row[2]);
    }
  }

  @Test
  public void testNoSchemaWithParentDataElement()
  {
    DataMap root = new DataMap();
    root.put("boolean", false);
    root.put("int", 1);
    root.put("long", 2L);
    root.put("float", 3.0f);
    root.put("double", 4.0);
    root.put("string", "foo");
    root.put("bytes", ByteString.copyAvroString("abc", false));

    DataMap grandParent = new DataMap();
    DataMap parent = new DataMap();
    grandParent.put("child", parent);
    parent.put("child", root);

    DataElement grandParentElement = new SimpleDataElement(grandParent, null);
    DataElement parentElement = new SimpleDataElement(parent, "child", null, grandParentElement);
    DataElement element = new SimpleDataElement(root, "child", null, parentElement);

    String preExpected =
      "  path=/child/child, class=com.linkedin.data.DataMap\n" +
      "   path=/child/child/bytes, class=com.linkedin.data.ByteString, value=abc\n" +
      "   path=/child/child/int, class=java.lang.Integer, value=1\n" +
      "   path=/child/child/string, class=java.lang.String, value=foo\n" +
      "   path=/child/child/boolean, class=java.lang.Boolean, value=false\n" +
      "   path=/child/child/double, class=java.lang.Double, value=4.0\n" +
      "   path=/child/child/long, class=java.lang.Long, value=2\n" +
      "   path=/child/child/float, class=java.lang.Float, value=3.0\n";

    String postExpected =
      "   path=/child/child/bytes, class=com.linkedin.data.ByteString, value=abc\n" +
      "   path=/child/child/int, class=java.lang.Integer, value=1\n" +
      "   path=/child/child/string, class=java.lang.String, value=foo\n" +
      "   path=/child/child/boolean, class=java.lang.Boolean, value=false\n" +
      "   path=/child/child/double, class=java.lang.Double, value=4.0\n" +
      "   path=/child/child/long, class=java.lang.Long, value=2\n" +
      "   path=/child/child/float, class=java.lang.Float, value=3.0\n" +
      "  path=/child/child, class=com.linkedin.data.DataMap\n";

    traverseAndCheckWithDataElement(element, preExpected, postExpected, true);
  }

  public void assertEqualsByName(Builder builder, List<Object> expectedNames)
  {
    final List<Object> names = new ArrayList<Object>();
    builder.iterate(new Builder.Callback()
    {
      @Override
      public void callback(DataElement element)
      {
        names.add(element.getName());
      }
    });
    if (debug) TestUtil.out.println(names + " " + expectedNames);
    assertTrue(expectedNames.containsAll(names));
    assertEquals(names.size(), names.size());
  }

  public void assertEqualsByValue(Builder builder, List<Object> expectedValues)
  {
    final List<Object> values = new ArrayList<Object>();
    builder.iterate(new Builder.Callback()
    {
      @Override
      public void callback(DataElement element)
      {
        values.add(element.getValue());
      }
    });
    if (debug) TestUtil.out.println(values + " " + expectedValues);
    assertTrue(expectedValues.containsAll(values));
    assertEquals(values.size(), expectedValues.size());
  }

  public void assertEqualsByPath(Builder builder, List<String> expectedPaths)
  {
    final List<String> paths = new ArrayList<String>();
    final ArrayList<Object> pathAsList = new ArrayList<Object>();
    builder.iterate(new Builder.Callback()
    {
      @Override
      public void callback(DataElement element)
      {
        paths.add(element.pathAsString());
        Object[] pathAsArray = element.path();
        element.pathAsList(pathAsList);
        assertEquals(pathAsArray.length, pathAsList.size());
        for (int i = 0; i < pathAsArray.length; i++)
        {
          assertSame(pathAsArray[i], pathAsList.get(i));
        }
      }
    });
    if (debug) TestUtil.out.println(paths + " " + expectedPaths);
    assertTrue(expectedPaths.containsAll(paths));
    assertEquals(paths.size(), expectedPaths.size());
  }

  private static final Object[][] _orders = {
    { IterationOrder.PRE_ORDER },
    { IterationOrder.POST_ORDER }
  };

  @DataProvider(name = "orders")
  public Object[][] orders()
  {
    return _orders;
  }

  @Test(dataProvider = "orders")
  public void testValueInstanceOf(IterationOrder order) throws IOException
  {
    String input = "{ \"a\" : 1, \"b\" : \"string\", \"c\" : { \"d\" : 2, \"e\" : { \"f\" : \"foo\" } } }";

    Object[][] tests =
      {
        {
          Integer.class,
          Arrays.asList("a", "d")
        },
        {
          String.class,
          Arrays.asList("b", "f")
        },
        {
          DataMap.class,
          Arrays.asList("c", "e", DataElement.ROOT_NAME)
        }
      };

    Object o = jsonToObject(input);
    for (Object[] row : tests)
    {
      Builder builder = Builder.create(o, null, order).filterBy(valueInstanceOf((Class<?>) row[0]));
      @SuppressWarnings("unchecked")
      List<Object> expected = (List<Object>) row[1];
      assertEqualsByName(builder, expected);
    }
  }

  @Test(dataProvider = "orders")
  public void testValueEquals(IterationOrder order) throws IOException
  {
    String input = "{ \"a\" : 1, \"b\" : \"string\", \"c\" : { \"d\" : 2, \"e\" : { \"f\" : \"string\" } } }";

    Object[][] tests =
      {
        {
          1,
          Arrays.asList("a")
        },
        {
          "string",
          Arrays.asList("b", "f")
        },
        {
          2,
          Arrays.asList("d")
        }
      };

    Object o = jsonToObject(input);
    for (Object[] row : tests)
    {
      Builder builder = Builder.create(o, null, order).filterBy(valueEquals(row[0]));
      @SuppressWarnings("unchecked")
      List<Object> expected = (List<Object>) row[1];
      assertEqualsByName(builder, expected);
    }
  }

  @Test(dataProvider = "orders")
  public void testNameEquals(IterationOrder order) throws IOException
  {
    String input = "{ \"a\" : 1, \"b\" : \"string\", \"c\" : { \"a\" : 2, \"e\" : { \"b\" : \"foo\" }, \"f\" : [ 10, 11, 12 ] }, \"g\" : [ [ 20 ], 21, 23 ] }";

    Object[][] tests =
      {
        {
          "a",
          Arrays.asList(1, 2)
        },
        {
          "b",
          Arrays.asList("string", "foo")
        },
        {
          "e",
          Arrays.asList(jsonToObject("{ \"b\" : \"foo\" }"))
        },
        {
          0,
          Arrays.asList(10, jsonToObject("[ 20 ]"), 20)
        },
        {
          "f",
          Arrays.asList(jsonToObject("[ 10, 11, 12]"))
        }
      };

    Object o = jsonToObject(input);
    for (Object[] row : tests)
    {
      Builder builder = Builder.create(o, null, order).filterBy(nameEquals(row[0]));
      @SuppressWarnings("unchecked")
      List<Object> expected = (List<Object>) row[1];
      assertEqualsByValue(builder, expected);
    }
  }

  @Test(dataProvider = "orders")
  public void testPathMatchesPattern(IterationOrder order) throws IOException
  {
    String input =
        "{ \"i\" : 1, \n" +
        "  \"r\" : { \n" +
        "    \"i\" : 12, \n" +
        "    \"r\" : { \n" +
        "      \"i\" : 123, \n" +
        "      \"r\" : { \n" +
        "        \"i\" : 1234, \n" +
        "        \"r\" : { \n" +
        "          \"i\" : 12345, \n" +
        "          \"r\" : { \n" +
        "            \"i\" : 123456, \n" +
        "            \"r\" : { \n" +
        "              \"i\" : 1234567 \n" +
        "            }\n" +
        "          }\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "  }\n" +
        "}\n";


    Object[][] tests =
      {
        /* no wildcard */
        {
          // match /i
          new Object[] { "i" },
          Arrays.asList("/i")
        },
        {
          // match /r
          new Object[] { "r" },
          Arrays.asList("/r")
        },
        {
          // match /r/i
          new Object[] { "r", "i" },
          Arrays.asList("/r/i")
        },
        {
          // match /r/r
          new Object[] { "r", "r" },
          Arrays.asList("/r/r")
        },
        {
          // match /r/r/i
          new Object[] { "r", "r", "i" },
          Arrays.asList("/r/r/i")
        },
        {
          // match /r/r/r
          new Object[] { "r", "r", "r" },
          Arrays.asList("/r/r/r")
        },
        {
          // match /r/r/r/r
          new Object[] { "r", "r", "r", "r" },
          Arrays.asList("/r/r/r/r")
        },
        {
          // match /r/r/r/r/r
          new Object[] { "r", "r", "r", "r", "r" },
          Arrays.asList("/r/r/r/r/r")
        },
        {
          // match /r/r/r/r/r/r
          new Object[] { "r", "r", "r", "r", "r", "r" },
          Arrays.asList("/r/r/r/r/r/r")
        },

        /* ANY_ONE */
        {
          // match /?
          new Object[] { Wildcard.ANY_ONE },
          Arrays.asList("/i",
                        "/r")
        },
        {
          // match /?/?
          new Object[] { Wildcard.ANY_ONE, Wildcard.ANY_ONE },
          Arrays.asList("/r/i",
                        "/r/r")
        },
        {
          // match /?/?/?
          new Object[] { Wildcard.ANY_ONE, Wildcard.ANY_ONE, Wildcard.ANY_ONE },
          Arrays.asList("/r/r/i",
                        "/r/r/r")
        },
        {
          // match /?/?/?/?
          new Object[] { Wildcard.ANY_ONE, Wildcard.ANY_ONE, Wildcard.ANY_ONE, Wildcard.ANY_ONE },
          Arrays.asList("/r/r/r/i",
                        "/r/r/r/r")
        },
        {
          // match /?/i
          new Object[] { Wildcard.ANY_ONE, "i" },
          Arrays.asList("/r/i")
        },
        {
          // match /?/r
          new Object[] { Wildcard.ANY_ONE, "r" },
          Arrays.asList("/r/r")
        },
        {
          // match /?/?/i
          new Object[] { Wildcard.ANY_ONE, Wildcard.ANY_ONE, "i" },
          Arrays.asList("/r/r/i")
        },
        {
          // match /?/?/r
          new Object[] { Wildcard.ANY_ONE, Wildcard.ANY_ONE, "r" },
          Arrays.asList("/r/r/r")
        },
        {
          // match /?/?/?/r
          new Object[] { Wildcard.ANY_ONE, Wildcard.ANY_ONE, Wildcard.ANY_ONE, "r" },
          Arrays.asList("/r/r/r/r")
        },
        {
          // match /?/r/?/r
          new Object[] { Wildcard.ANY_ONE, "r", Wildcard.ANY_ONE, "r" },
          Arrays.asList("/r/r/r/r")
        },
        {
          // match /r/?
          new Object[] { "r", Wildcard.ANY_ONE },
          Arrays.asList("/r/i",
                        "/r/r")
        },
        {
          // match /r/r/?/?
          new Object[] { "r", "r", Wildcard.ANY_ONE, Wildcard.ANY_ONE },
          Arrays.asList("/r/r/r/i",
                        "/r/r/r/r")
        },

        /* ANY_ONE_OR_MORE */
        {
          // match /+
          new Object[] { Wildcard.ANY_ONE_OR_MORE },
          Arrays.asList("/i",
                        "/r",
                        "/r/i",
                        "/r/r",
                        "/r/r/i",
                        "/r/r/r",
                        "/r/r/r/i",
                        "/r/r/r/r",
                        "/r/r/r/r/i",
                        "/r/r/r/r/r",
                        "/r/r/r/r/r/i",
                        "/r/r/r/r/r/r",
                        "/r/r/r/r/r/r/i")
        },
        {
          // match /+/i
          new Object[] { Wildcard.ANY_ONE_OR_MORE, "i" },
          Arrays.asList("/r/i",
                        "/r/r/i",
                        "/r/r/r/i",
                        "/r/r/r/r/i",
                        "/r/r/r/r/r/i",
                        "/r/r/r/r/r/r/i")
        },
        {
          // match /+/r
          new Object[] { Wildcard.ANY_ONE_OR_MORE, "r" },
          Arrays.asList("/r/r",
                        "/r/r/r",
                        "/r/r/r/r",
                        "/r/r/r/r/r",
                        "/r/r/r/r/r/r")
        },
        {
          // match /+/r/+/i
          new Object[] { Wildcard.ANY_ONE_OR_MORE, "r", Wildcard.ANY_ONE_OR_MORE, "i" },
          Arrays.asList("/r/r/r/i",
                        "/r/r/r/r/i",
                        "/r/r/r/r/r/i",
                        "/r/r/r/r/r/r/i")
        },
        {
          // match /+/i
          new Object[] { Wildcard.ANY_ONE_OR_MORE, "i" },
          Arrays.asList("/r/i",
                        "/r/r/i",
                        "/r/r/r/i",
                        "/r/r/r/r/i",
                        "/r/r/r/r/r/i",
                        "/r/r/r/r/r/r/i")
        },
        {
          // match /r/+/r
          new Object[] { "r", Wildcard.ANY_ONE_OR_MORE, "r" },
          Arrays.asList("/r/r/r",
                        "/r/r/r/r",
                        "/r/r/r/r/r",
                        "/r/r/r/r/r/r")
        },
        {
          // match /r/+/r/r
          new Object[] { "r", Wildcard.ANY_ONE_OR_MORE, "r", "r" },
          Arrays.asList("/r/r/r/r",
                        "/r/r/r/r/r",
                        "/r/r/r/r/r/r")
        },
        {
          // match /r/r/+/r/r
          new Object[] { "r", "r", Wildcard.ANY_ONE_OR_MORE, "r", "r" },
          Arrays.asList("/r/r/r/r/r",
                        "/r/r/r/r/r/r")
        },
        {
          // match /r/+/r/+/r
          new Object[] { "r", Wildcard.ANY_ONE_OR_MORE, "r", Wildcard.ANY_ONE_OR_MORE, "r" },
          Arrays.asList("/r/r/r/r/r",
                        "/r/r/r/r/r/r")
        },


        /* ANY_ZERO_OR_ONE */
        {
          // match /~
          new Object[] { Wildcard.ANY_ZERO_OR_ONE },
          Arrays.asList("",
                        "/i",
                        "/r")
        },
        {
          // match /~/r
          new Object[] { Wildcard.ANY_ZERO_OR_ONE, "r" },
          Arrays.asList("/r",
                        "/r/r")
        },
        {
          // match /~/i
          new Object[] { Wildcard.ANY_ZERO_OR_ONE, "i" },
          Arrays.asList("/i",
                        "/r/i")
        },
        {
          // match /r/~/r
          new Object[] { "r", Wildcard.ANY_ZERO_OR_ONE, "r" },
          Arrays.asList("/r/r",
                        "/r/r/r")
        },
        {
          // match /r/~/r/r
          new Object[] { "r", Wildcard.ANY_ZERO_OR_ONE, "r", "r" },
          Arrays.asList("/r/r/r",
                        "/r/r/r/r")
        },
        {
          // match /r/r/~/r/r
          new Object[] { "r", "r", Wildcard.ANY_ZERO_OR_ONE, "r", "r" },
          Arrays.asList("/r/r/r/r",
                        "/r/r/r/r/r")
        },
        {
          // match /r/~/r/~/r
          new Object[] { "r", Wildcard.ANY_ZERO_OR_ONE, "r", Wildcard.ANY_ZERO_OR_ONE, "r" },
          Arrays.asList("/r/r/r",
                        "/r/r/r/r",
                        "/r/r/r/r/r")
        },
        {
          // match /r/~/r/~/i
          new Object[] { "r", Wildcard.ANY_ZERO_OR_ONE, "r", Wildcard.ANY_ZERO_OR_ONE, "i" },
          Arrays.asList("/r/r/i",
                        "/r/r/r/i",
                        "/r/r/r/r/i")
        },

        /* ANY_ZERO_OR_MORE*/
        {
          // match /*
          new Object[] { Wildcard.ANY_ZERO_OR_MORE },
          Arrays.asList("",
                        "/i",
                        "/r",
                        "/r/i",
                        "/r/r",
                        "/r/r/i",
                        "/r/r/r",
                        "/r/r/r/i",
                        "/r/r/r/r",
                        "/r/r/r/r/i",
                        "/r/r/r/r/r",
                        "/r/r/r/r/r/i",
                        "/r/r/r/r/r/r",
                        "/r/r/r/r/r/r/i")
        },
        {
          // match /*/i
          new Object[] { Wildcard.ANY_ZERO_OR_MORE, "i" },
          Arrays.asList("/i",
                        "/r/i",
                        "/r/r/i",
                        "/r/r/r/i",
                        "/r/r/r/r/i",
                        "/r/r/r/r/r/i",
                        "/r/r/r/r/r/r/i")
        },
        {
          // match /*/r
          new Object[] { Wildcard.ANY_ZERO_OR_MORE, "r" },
          Arrays.asList("/r",
                        "/r/r",
                        "/r/r/r",
                        "/r/r/r/r",
                        "/r/r/r/r/r",
                        "/r/r/r/r/r/r")
        },
        {
          // match /*/r/*/i
          new Object[] { Wildcard.ANY_ZERO_OR_MORE, "r", Wildcard.ANY_ZERO_OR_MORE, "i"},
          Arrays.asList("/r/i",
                        "/r/r/i",
                        "/r/r/r/i",
                        "/r/r/r/r/i",
                        "/r/r/r/r/r/i",
                        "/r/r/r/r/r/r/i")
        },
        {
          // match /r/*/r
          new Object[] { "r", Wildcard.ANY_ZERO_OR_MORE, "r" },
          Arrays.asList("/r/r",
                        "/r/r/r",
                        "/r/r/r/r",
                        "/r/r/r/r/r",
                        "/r/r/r/r/r/r")
        },
        {
          // match /r/*/r/r
          new Object[] { "r", Wildcard.ANY_ZERO_OR_MORE, "r", "r" },
          Arrays.asList("/r/r/r",
                        "/r/r/r/r",
                        "/r/r/r/r/r",
                        "/r/r/r/r/r/r")
        },
        {
          // match /r/r/*/r/r
          new Object[] { "r", "r", Wildcard.ANY_ZERO_OR_MORE, "r", "r" },
          Arrays.asList("/r/r/r/r",
                        "/r/r/r/r/r",
                        "/r/r/r/r/r/r")
        },
        {
          // match /r/*/r/*/r
          new Object[] { "r", Wildcard.ANY_ZERO_OR_MORE, "r", Wildcard.ANY_ZERO_OR_MORE, "r" },
          Arrays.asList("/r/r/r",
                        "/r/r/r/r",
                        "/r/r/r/r/r",
                        "/r/r/r/r/r/r")
        },
        {
          // match /r/*/r/*/i
          new Object[] { "r", Wildcard.ANY_ZERO_OR_MORE, "r", Wildcard.ANY_ZERO_OR_MORE, "i" },
          Arrays.asList("/r/r/i",
                        "/r/r/r/i",
                        "/r/r/r/r/i",
                        "/r/r/r/r/r/i",
                        "/r/r/r/r/r/r/i")
        },

        /* Combinations */
        {
          // match /*/?/i (same as /+/i
          new Object[] { Wildcard.ANY_ZERO_OR_MORE, Wildcard.ANY_ONE, "i" },
          Arrays.asList("/r/i",
                        "/r/r/i",
                        "/r/r/r/i",
                        "/r/r/r/r/i",
                        "/r/r/r/r/r/i",
                        "/r/r/r/r/r/r/i")
        },
        {
          // match /*/r/?/i
          new Object[] { Wildcard.ANY_ZERO_OR_MORE, "r", Wildcard.ANY_ONE, "i" },
          Arrays.asList("/r/r/i",
                        "/r/r/r/i",
                        "/r/r/r/r/i",
                        "/r/r/r/r/r/i",
                        "/r/r/r/r/r/r/i")
        },
        {
          // match /+/r/+/i
          new Object[] { Wildcard.ANY_ONE_OR_MORE, "r", Wildcard.ANY_ONE_OR_MORE, "i" },
          Arrays.asList("/r/r/r/i",
                        "/r/r/r/r/i",
                        "/r/r/r/r/r/i",
                        "/r/r/r/r/r/r/i")
        },
        {
          // match /?/?/~
          new Object[] { Wildcard.ANY_ONE, Wildcard.ANY_ONE, Wildcard.ANY_ZERO_OR_ONE },
          Arrays.asList("/r/i",
                        "/r/r",
                        "/r/r/i",
                        "/r/r/r")
        },
        {
          // match /?/?/~/~
          new Object[] { Wildcard.ANY_ONE, Wildcard.ANY_ONE, Wildcard.ANY_ZERO_OR_ONE, Wildcard.ANY_ZERO_OR_ONE },
          Arrays.asList("/r/i",
                        "/r/r",
                        "/r/r/i",
                        "/r/r/r",
                        "/r/r/r/i",
                        "/r/r/r/r")
        },
        {
          // match /*/?/i
          new Object[] { Wildcard.ANY_ZERO_OR_MORE, Wildcard.ANY_ONE, "i" },
          Arrays.asList("/r/i",
                        "/r/r/i",
                        "/r/r/r/i",
                        "/r/r/r/r/i",
                        "/r/r/r/r/r/i",
                        "/r/r/r/r/r/r/i")
        },
        {
          // match /*/r/?/i
          new Object[] { Wildcard.ANY_ZERO_OR_MORE, "r", Wildcard.ANY_ONE, "i" },
          Arrays.asList("/r/r/i",
                        "/r/r/r/i",
                        "/r/r/r/r/i",
                        "/r/r/r/r/r/i",
                        "/r/r/r/r/r/r/i")
        },
        {
          // match /?/r/+/i
          new Object[] { Wildcard.ANY_ONE, "r", Wildcard.ANY_ONE_OR_MORE, "i" },
          Arrays.asList("/r/r/r/i",
                        "/r/r/r/r/i",
                        "/r/r/r/r/r/i",
                        "/r/r/r/r/r/r/i")
        },
        {
          // match /?/?/~
          new Object[] { Wildcard.ANY_ONE, Wildcard.ANY_ONE, Wildcard.ANY_ZERO_OR_ONE },
          Arrays.asList("/r/i",
                        "/r/r",
                        "/r/r/i",
                        "/r/r/r")
        },
        {
          // match /?/?/~/~
          new Object[] { Wildcard.ANY_ONE, Wildcard.ANY_ONE, Wildcard.ANY_ZERO_OR_ONE, Wildcard.ANY_ZERO_OR_ONE },
          Arrays.asList("/r/i",
                        "/r/r",
                        "/r/r/i",
                        "/r/r/r",
                        "/r/r/r/i",
                        "/r/r/r/r")
        },
      };


    Object o = jsonToObject(input);
    for (Object[] row : tests)
    {
      Builder builder = Builder.create(o, null, order).filterBy(pathMatchesPattern((Object[]) row[0]));
      @SuppressWarnings("unchecked")
      List<String> expected = (List<String>) row[1];
      assertEqualsByPath(builder, expected);
    }
  }

  @Test(dataProvider = "orders")
  public void testHasChildWithNameValue(IterationOrder order) throws IOException
  {
    String input = "{ \"a\" : 2, \"b\" : \"foo\", \"c\" : { \"a\" : 2, \"e\" : { \"b\" : \"foo\", \"d\" : [ 0, 1, 2 ] } }, \"d\" : [ 0, 1, 2] }";

    Object[][] tests =
      {
        {
          "a", 2,
          Arrays.asList("c", DataElement.ROOT_NAME)
        },
        {
          "b", "foo",
          Arrays.asList("e", DataElement.ROOT_NAME)
        },
        {
          1, 1,
          Arrays.asList("d", DataElement.ROOT_NAME)
        }
      };

    Object o = jsonToObject(input);
    for (Object[] row : tests)
    {
      Builder builder = Builder.create(o, null, order).filterBy(hasChildWithNameValue(row[0], row[1]));
      @SuppressWarnings("unchecked")
      List<Object> expected = (List<Object>) row[2];
      assertEqualsByName(builder, expected);
    }
  }

  @Test(dataProvider = "orders")
  public void testDataSchemaTypeEquals(IterationOrder order) throws IOException
  {
    String input =
      "{\n" +
      "  \"a\" : 222,\n" +
      "  \"b\" : \"foo\",\n" +
      "  \"c\" : {\n" +
      "    \"a\" : 333,\n" +
      "    \"e\" : {\n" +
      "      \"b\" : \"bar\",\n" +
      "      \"d\" : [ 0, 1, 2 ]\n" +
      "    }\n" +
      "  },\n" +
      "  \"d\" : [ 10, 11, 12],\n" +
      "  \"f\" : {\n" +
      "    \"k1\" : \"v1\",\n" +
      "    \"k2\" : \"v2\"\n" +
      "  },\n" +
      "  \"u\" : {\n" +
      "    \"int\" : 123\n" +
      "  }\n" +
      "}";


    String schemaText =
      "{\n" +
      "  \"name\" : \"foo\",\n" +
      "  \"type\" : \"record\",\n" +
      "  \"fields\" : [\n" +
      "    { \"name\" : \"a\", \"type\" : \"int\" },\n" +
      "    { \"name\" : \"b\", \"type\" : \"string\" },\n" +
      "    { \"name\" : \"c\", \"type\" : \"foo\" },\n" +
      "    { \"name\" : \"d\", \"type\" : { \"type\" : \"array\", \"items\" : \"int\" } },\n" +
      "    { \"name\" : \"e\", \"type\" : \"foo\" },\n" +
      "    { \"name\" : \"f\", \"type\" : { \"type\" : \"map\", \"values\" : \"string\" } },\n" +
      "    { \"name\" : \"u\", \"type\" : [ \"int\", \"double\", \"string\" ] }\n" +
      "  ]\n" +
      "}\n";
    DataSchema schema = DataTemplateUtil.parseSchema(schemaText);

    Object[][] tests =
      {
        {
          DataSchema.Type.INT,
          Arrays.asList(222, 333, 0, 1, 2, 10, 11, 12, 123)
        },
        {
          DataSchema.Type.STRING,
          Arrays.asList("foo", "bar", "v1", "v2")
        }
      };

    Object o = jsonToObject(input);
    for (Object[] row : tests)
    {
      Builder builder = Builder.create(o, schema, order).filterBy(dataSchemaTypeEquals((DataSchema.Type) row[0]));
      @SuppressWarnings("unchecked")
      List<Object> expected = (List<Object>) row[1];
      assertEqualsByValue(builder, expected);
    }
  }

  @Test(dataProvider = "orders")
  public void testDataSchemaNameEquals(IterationOrder order) throws IOException
  {
    String input = "{ \"a\" : 222, \"b\" : \"foo\", \"c\" : { \"a\" : 333, \"e\" : { \"b\" : \"bar\", \"d\" : [ 0, 1, 2 ], \"f\" : \"WXYZ\", \"g\" : \"ORANGE\" }, \"f\" : \"wxyz\" }, \"d\" : [ 10, 11, 12 ], \"g\" : \"APPLE\" }";
    String schemaText =
      "{\n" +
      "  \"name\" : \"foo\",\n" +
      "  \"type\" : \"record\",\n" +
      "  \"fields\" : [\n" +
      "    { \"name\" : \"a\", \"type\" : \"int\" },\n" +
      "    { \"name\" : \"b\", \"type\" : \"string\" },\n" +
      "    { \"name\" : \"c\", \"type\" : \"foo\" },\n" +
      "    { \"name\" : \"d\", \"type\" : { \"type\" : \"array\", \"items\" : \"int\" } },\n" +
      "    { \"name\" : \"e\", \"type\" : \"foo\" },\n" +
      "    { \"name\" : \"f\", \"type\" : { \"name\" : \"xx.F\", \"type\" : \"fixed\", \"size\" : 4 } },\n" +
      "    { \"name\" : \"g\", \"type\" : { \"name\" : \"yy.G\", \"type\" : \"enum\", \"symbols\" : [ \"APPLE\", \"BANANA\", \"ORANGE\" ] } }\n" +
      "  ]\n" +
      "}\n";
    DataSchema schema = DataTemplateUtil.parseSchema(schemaText);

    Object[][] tests =
      {
        {
          "xx.F",
          Arrays.asList("WXYZ", "wxyz")
        },
        {
          "yy.G",
          Arrays.asList("ORANGE", "APPLE")
        }
      };

    Object o = jsonToObject(input);
    for (Object[] row : tests)
    {
      Builder builder = Builder.create(o, schema, order).filterBy(dataSchemaNameEquals((String) row[0]));
      @SuppressWarnings("unchecked")
      List<Object> expected = (List<Object>) row[1];
      assertEqualsByValue(builder, expected);
    }
  }

  @Test(dataProvider = "orders")
  public void testGetChild(IterationOrder order) throws IOException
  {
    String input = "{ \"a\" : { \"a1\" : \"A1\" }, \"b\" : [ 1.0, 2.0 ] }";
    Object o = jsonToObject(input);
    Builder.create(o, null, order).iterate(new Builder.Callback()
    {
      public void callback(DataElement e)
      {
        if (e.getName().equals("a"))
        {
          assertEquals(e.getChild("a1"), "A1");
          assertTrue(e.getChild("x") == null);
        }
        else if (e.getName().equals("b"))
        {
          assertEquals(e.getChild(0), 1.0);
          assertEquals(e.getChild(1), 2.0);
          assertTrue(e.getChild(-1) == null);
          assertTrue(e.getChild(2) == null);
        }
      }
    });
  }

  @Test(dataProvider = "orders")
  public void testBuilderCreate(final IterationOrder order) throws IOException
  {
    String input = "{ \"a\" : { \"a1\" : \"A1\" }, \"b\" : [ 1.0, 2.0 ] }";
    Object o = jsonToObject(input);
    Builder.create(o, null, order).iterate(new Builder.Callback()
    {
      public void callback(DataElement e)
      {
        if (e.getName().equals("a"))
        {
          Builder builder = Builder.create(e.getValue(), e.getSchema(), order);
          assertEqualsByName(builder, Arrays.asList((Object) DataElement.ROOT_NAME, "a1"));
          assertEqualsByPath(builder, Arrays.asList("", "/a1"));
          Builder builder2 = Builder.create(e, order);
          assertEqualsByName(builder2, Arrays.asList((Object) "a", "a1"));
          assertEqualsByPath(builder2, Arrays.asList("/a", "/a/a1"));
        }
        else if (e.getName().equals("b"))
        {
          Builder builder = Builder.create(e.getValue(), e.getSchema(), order);
          assertEqualsByName(builder, Arrays.asList((Object) DataElement.ROOT_NAME, 0, 1));
          assertEqualsByPath(builder, Arrays.asList("", "/0", "/1"));
          Builder builder2 = Builder.create(e, order);
          assertEqualsByName(builder2, Arrays.asList((Object) "b", 0, 1));
          assertEqualsByPath(builder2, Arrays.asList("/b", "/b/0", "/b/1"));
        }
      }
    });
  }

  @Test(dataProvider = "orders")
  public void testToString(final IterationOrder order) throws IOException
  {
    String input = "{ \"a\" : { \"a1\" : \"A1\" }, \"b\" : [ 1.0, 2.0 ] }";
    Object o = jsonToObject(input);
    Builder.create(o, null, order).iterate(new Builder.Callback()
    {
      public void callback(DataElement e)
      {
        if (e.getName().equals("a1"))
        {
          assertEquals(e.toString(), "/a/a1:A1");
        }
        else if (e.getName().equals(1))
        {
          assertEquals(e.toString(), "/b/1:2.0");
        }
      }
    });
  }

  @Test
  public void testSkipToSibling() throws IOException
  {
    // pre-order skipToSibling on a DataMap
    {
      String input = "{ \"aKey\" : [ 1, 2 ], \"bKey\" : [ 1.0, 2.0 ] }";
      Object o = jsonToObject(input);
      // use filterBy(alwaysTrue()) to make sure that FilterIterator.skipToSibling is exercised.
      DataIterator it = Builder.create(o, null, IterationOrder.PRE_ORDER).filterBy(alwaysTrue()).dataIterator();
      DataElement e;
      boolean onlyDataList = false;
      while ((e = it.next()) != null)
      {
        if (onlyDataList == false && e.getValue() instanceof DataList == false)
        {
          continue;
        }
        assertTrue(e.getValue() instanceof DataList);
        onlyDataList = true;
        it.skipToSibling();
      }
      assertTrue(onlyDataList);
    }
    // pre-order skipToSibling on an DataList
    {
      String input = "[ { \"aKey\" : 1 }, { \"bKey\" : 2.0 } ]";
      Object o = jsonToObject(input);
      DataIterator it = Builder.create(o, null, IterationOrder.PRE_ORDER).dataIterator();
      DataElement e;
      boolean onlyDataMap = false;
      while ((e = it.next()) != null)
      {
        if (onlyDataMap == false && e.getValue() instanceof DataMap == false)
        {
          continue;
        }
        assertTrue(e.getValue() instanceof DataMap);
        onlyDataMap = true;
        it.skipToSibling();
      }
      assertTrue(onlyDataMap);
    }
    // pre-order skipToSibling is no-op, because there is no children
    {
      String input = "[ 1, 2, 3 ]";
      Object o = jsonToObject(input);
      DataIterator it = Builder.create(o, null, IterationOrder.PRE_ORDER).dataIterator();
      DataElement e;
      boolean onlyInteger = false;
      while ((e = it.next()) != null)
      {
        if (onlyInteger == false && e.getValue() instanceof Integer == false)
        {
          continue;
        }
        assertTrue(e.getValue() instanceof Integer);
        onlyInteger = true;
        it.skipToSibling();
      }
      assertTrue(onlyInteger);
    }
    // pre-order skipToSibling is no-op, because there is no children
    {
      String input = "{ \"a\" : 1, \"b\" : 2, \"c\" : 3 }";
      Object o = jsonToObject(input);
      DataIterator it = Builder.create(o, null, IterationOrder.PRE_ORDER).dataIterator();
      DataElement e;
      boolean onlyInteger = false;
      while ((e = it.next()) != null)
      {
        if (onlyInteger == false && e.getValue() instanceof Integer == false)
        {
          continue;
        }
        assertTrue(e.getValue() instanceof Integer);
        onlyInteger = true;
        it.skipToSibling();
      }
      assertTrue(onlyInteger);
    }
    // post-order skipToSibling
    {
      String input = "{ \"aKey\" : [ 1, 2 ], \"bKey\" : [ 1.0, 2.0 ] }";
      Object o = jsonToObject(input);
      DataElement e;
      DataIterator it = Builder.create(o, null, IterationOrder.POST_ORDER).dataIterator();
      List<String> pathsWithSkip = new ArrayList<String>();
      while ((e = it.next()) != null)
      {
        pathsWithSkip.add(e.pathAsString());
        it.skipToSibling();
      }

      it = Builder.create(o, null, IterationOrder.POST_ORDER).dataIterator();
      List<String> pathsWithoutSkip = new ArrayList<String>();
      while ((e = it.next()) != null)
      {
        pathsWithoutSkip.add(e.pathAsString());
      }

      assertEquals(pathsWithSkip, pathsWithoutSkip);
    }
  }

  @Test
  public void testPredicates()
  {
    assertFalse(alwaysFalse().evaluate(null));
    assertTrue(alwaysTrue().evaluate(null));

    assertTrue(and().evaluate(null));
    assertTrue(and(alwaysTrue()).evaluate(null));
    assertFalse(and(alwaysFalse()).evaluate(null));
    assertTrue(and(alwaysTrue(), alwaysTrue()).evaluate(null));
    assertFalse(and(alwaysFalse(), alwaysTrue()).evaluate(null));
    assertFalse(and(alwaysTrue(), alwaysFalse()).evaluate(null));
    assertFalse(and(alwaysFalse(), alwaysFalse()).evaluate(null));
    assertTrue(and(alwaysTrue(), alwaysTrue(), alwaysTrue()).evaluate(null));
    assertFalse(and(alwaysFalse(), alwaysTrue(), alwaysTrue()).evaluate(null));
    assertFalse(and(alwaysTrue(), alwaysFalse(), alwaysTrue()).evaluate(null));
    assertFalse(and(alwaysTrue(), alwaysTrue(), alwaysFalse()).evaluate(null));
    assertFalse(and(alwaysFalse(), alwaysFalse(), alwaysFalse()).evaluate(null));

    assertTrue(and(new ArrayList<Predicate>()).evaluate(null));
    assertTrue(and(Arrays.asList(alwaysTrue())).evaluate(null));
    assertFalse(and(Arrays.asList(alwaysFalse())).evaluate(null));
    assertTrue(and(Arrays.asList(alwaysTrue(), alwaysTrue())).evaluate(null));
    assertFalse(and(Arrays.asList(alwaysFalse(), alwaysTrue())).evaluate(null));
    assertFalse(and(Arrays.asList(alwaysTrue(), alwaysFalse())).evaluate(null));
    assertFalse(and(Arrays.asList(alwaysFalse(), alwaysFalse())).evaluate(null));
    assertTrue(and(Arrays.asList(alwaysTrue(), alwaysTrue(), alwaysTrue())).evaluate(null));
    assertFalse(and(Arrays.asList(alwaysFalse(), alwaysTrue(), alwaysTrue())).evaluate(null));
    assertFalse(and(Arrays.asList(alwaysTrue(), alwaysFalse(), alwaysTrue())).evaluate(null));
    assertFalse(and(Arrays.asList(alwaysTrue(), alwaysTrue(), alwaysFalse())).evaluate(null));
    assertFalse(and(Arrays.asList(alwaysFalse(), alwaysFalse(), alwaysFalse())).evaluate(null));

    assertFalse(or().evaluate(null));
    assertTrue(or(alwaysTrue()).evaluate(null));
    assertFalse(or(alwaysFalse()).evaluate(null));
    assertTrue(or(alwaysTrue(), alwaysTrue()).evaluate(null));
    assertTrue(or(alwaysFalse(), alwaysTrue()).evaluate(null));
    assertTrue(or(alwaysTrue(), alwaysFalse()).evaluate(null));
    assertFalse(or(alwaysFalse(), alwaysFalse()).evaluate(null));
    assertTrue(or(alwaysTrue(), alwaysTrue(), alwaysTrue()).evaluate(null));
    assertTrue(or(alwaysTrue(), alwaysFalse(), alwaysFalse()).evaluate(null));
    assertTrue(or(alwaysFalse(), alwaysTrue(), alwaysFalse()).evaluate(null));
    assertTrue(or(alwaysFalse(), alwaysFalse(), alwaysTrue()).evaluate(null));
    assertFalse(or(alwaysFalse(), alwaysFalse(), alwaysFalse()).evaluate(null));

    assertFalse(or(new ArrayList<Predicate>()).evaluate(null));
    assertFalse(or(Arrays.asList(alwaysFalse())).evaluate(null));
    assertTrue(or(Arrays.asList(alwaysTrue(), alwaysTrue())).evaluate(null));
    assertTrue(or(Arrays.asList(alwaysFalse(), alwaysTrue())).evaluate(null));
    assertTrue(or(Arrays.asList(alwaysTrue(), alwaysFalse())).evaluate(null));
    assertFalse(or(Arrays.asList(alwaysFalse(), alwaysFalse())).evaluate(null));
    assertTrue(or(Arrays.asList(alwaysTrue(), alwaysTrue(), alwaysTrue())).evaluate(null));
    assertTrue(or(Arrays.asList(alwaysTrue(), alwaysFalse(), alwaysFalse())).evaluate(null));
    assertTrue(or(Arrays.asList(alwaysFalse(), alwaysTrue(), alwaysFalse())).evaluate(null));
    assertTrue(or(Arrays.asList(alwaysFalse(), alwaysFalse(), alwaysTrue())).evaluate(null));
    assertFalse(or(Arrays.asList(alwaysFalse(), alwaysFalse(), alwaysFalse())).evaluate(null));
  }

  @Test(dataProvider = "orders")
  public void testInitialEmptyDataComplexFullIteration(final IterationOrder order) throws IOException
  {
    String input = "[ {}, \"a\" ]";
    Object o = jsonToObject(input);
    DataIterator i = Builder.create(o, null, order).dataIterator();

    int count = 0;
    while (i.next() != null)
    {
      count++;
    }
    assertEquals(count, 3, "iteration of " + input + " returned " + count + " elements instead of 3");
  }

  @Test(dataProvider = "orders")
  public void testInitialEmptyDataComplexOrder(final IterationOrder order) throws IOException
  {
    String input = "{ \"a\": { \"b\": {} } }";
    Object o = jsonToObject(input);
    DataIterator i = Builder.create(o, null, order).dataIterator();

    DataElement e = i.next();
    if (order == IterationOrder.PRE_ORDER)
    {
      assertEquals(e.level(), 0, "preorder traversal of " + input + " started with level " + e.level());
    }
    else
    {
      assertEquals(e.level(), 2, "postorder traversal of " + input + " started with level " + e.level());
    }
  }

  public void benchmarkImmutable(int count, final IterationOrder order)
  {
    // create object to traverse
    int leafFanout = 10;
    int internalFanout = 4;

    DataMap root = new DataMap();
    for (int i = 0; i < internalFanout; ++i)
    {
      DataList level1 = new DataList();
      root.put("list_" + i, level1);
      for (int j = 0; j < internalFanout; ++j)
      {
        DataMap level2 = new DataMap();
        level1.add(level2);
        for (int k = 0; k < leafFanout; ++k)
        {
          level2.put("key_" + i + "_" + j + "_" + k, "value_" + k);
        }
      }
    }

    long startTime = System.currentTimeMillis();
    long next = 0;

    //System.gc();
    for (int i = 0; i < count; ++i)
    {
      DataIterator it = Builder.create(root, null, order).dataIterator();
      while (it.next() != null)
      {
        next++;
      }
    }

    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;
    double rate = (double) next / (double) duration;
    TestUtil.out.println("ObjectIterator benchmark " + Math.floor(rate * 1000.0) + " next/secs");
  }

  //@Test
  public void benchMark()
  {
    int samples = 5;
    int count = 10000;

    IterationOrder order = IterationOrder.PRE_ORDER;

    for (int i = 0; i < samples; ++i )
    {
      benchmarkImmutable(count, order);
    }
  }
}
