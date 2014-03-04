/*
 Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.data.template;


import com.linkedin.data.ByteString;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Keren Jin
 */
public class TestConvertArray
{
  @Test
  public void testSimpleArray() throws IOException
  {
    Object result;

    result = convert("[]", String[].class);
    Assert.assertEquals(result, new String[0]);

    result = convert("[\"A\", \"B\"]", String[].class);
    Assert.assertEquals(result, new String[]{"A", "B"});

    result = convert("[true, false]", boolean[].class);
    Assert.assertEquals(result, new boolean[] { true, false });
    Assert.assertSame(result.getClass(), boolean[].class);
    result = convert("[true, false]", Boolean[].class);
    Assert.assertEquals(result, new Boolean[] { true, false });
    Assert.assertSame(result.getClass(), Boolean[].class);

    result = convert("[1, 2, 3]", int[].class);
    Assert.assertEquals(result, new int[] { 1, 2, 3 });
    Assert.assertSame(result.getClass(), int[].class);
    result = convert("[4, 5, 6]", Integer[].class);
    Assert.assertEquals(result, new Integer[] { 4, 5, 6 });
    Assert.assertSame(result.getClass(), Integer[].class);

    result = convert("[1, 2, 3]", float[].class);
    Assert.assertEquals(result, new float[] { 1F, 2F, 3F });
    Assert.assertSame(result.getClass(), float[].class);

    result = convert("[1.1, 2.2, 3.3]", double[].class);
    Assert.assertEquals(result, new double[] { 1.1D, 2.2D, 3.3D });
    Assert.assertSame(result.getClass(), double[].class);
    result = convert("[1.1, 2.2, 3.3]", Double[].class);
    Assert.assertEquals(result, new Double[] { 1.1D, 2.2D, 3.3D });
    Assert.assertSame(result.getClass(), Double[].class);

    // float will be coerced to integer
    result = convert("[1.1, 2.2, 3.3]", int[].class);
    Assert.assertEquals(result, new int[] { 1, 2, 3 });
    Assert.assertSame(result.getClass(), int[].class);

    result = convert("[\"APPLE\", \"BANANA\"]", TestArrayTemplate.Fruits[].class);
    Assert.assertEquals(result, new TestArrayTemplate.Fruits[] { TestArrayTemplate.Fruits.APPLE, TestArrayTemplate.Fruits.BANANA });
    Assert.assertSame(result.getClass(), TestArrayTemplate.Fruits[].class);

    Assert.assertEquals(convert("[" + _bytes5Quoted + ", " + _bytes5Quoted + "]", ByteString[].class),
                        new ByteString[] { ByteString.copyAvroString(_bytes5, true), ByteString.copyAvroString(_bytes5, true) });
  }

  @Test
  public void testComplexArray() throws IOException
  {
    Object result;

    result = convert("[[1.1], [2.2]]", DoubleArray[].class);
    Assert.assertEquals(result, new DoubleArray[] { new DoubleArray(Arrays.asList(1.1D)), new DoubleArray(Arrays.asList(2.2D)) });
    Assert.assertSame(result.getClass(), DoubleArray[].class);

    result = convert("[[[1.1]], [[2.2]]]", DoubleArray[][].class);
    Assert.assertEquals(result, new DoubleArray[][] { { new DoubleArray(Arrays.asList(1.1D)) }, { new DoubleArray(Arrays.asList(2.2D)) } });
    Assert.assertSame(result.getClass(), DoubleArray[][].class);

    result = convert("[[\"APPLE\"], [\"BANANA\"]]", TestArrayTemplate.EnumArrayTemplate[].class);
    Assert.assertEquals(result, new TestArrayTemplate.EnumArrayTemplate[] {
      new TestArrayTemplate.EnumArrayTemplate(Arrays.asList(TestArrayTemplate.Fruits.APPLE)),
      new TestArrayTemplate.EnumArrayTemplate(Arrays.asList(TestArrayTemplate.Fruits.BANANA)) });
    Assert.assertSame(result.getClass(), TestArrayTemplate.EnumArrayTemplate[].class);

    result = convert("[" + _bytes5Quoted + ", " + _bytes5Quoted + "]", TestFixedTemplate.Fixed5[].class);
    Assert.assertEquals(result, new TestFixedTemplate.Fixed5[] { new TestFixedTemplate.Fixed5(_bytes5), new TestFixedTemplate.Fixed5(_bytes5) });
    Assert.assertSame(result.getClass(), TestFixedTemplate.Fixed5[].class);

    result = convert("[{\"A\": 3}, {\"B\": 4}]", IntegerMap[].class);
    final Map<String, Integer> integerFixture1 = new HashMap<String, Integer>();
    final Map<String, Integer> integerFixture2 = new HashMap<String, Integer>();
    integerFixture1.put("A", 3);
    integerFixture2.put("B", 4);
    Assert.assertEquals(result, new IntegerMap[] { new IntegerMap(integerFixture1), new IntegerMap(integerFixture2) });
    Assert.assertSame(result.getClass(), IntegerMap[].class);

    result = convert("[{\"string\": \"7,27\"}, {\"int\": 1}]", TestCustom.Union[].class);
    final TestCustom.Union[] fixture = new TestCustom.Union[] { new TestCustom.Union(), new TestCustom.Union() };
    fixture[0].setCustomPoint(new TestCustom.CustomPoint("7,27"));
    fixture[1].setInt(1);
    Assert.assertEquals(result, fixture);
    Assert.assertSame(result.getClass(), TestCustom.Union[].class);

    result = convert("[{\"bar\": \"Hello\"}, {\"bar\": \"World\"}]", TestArrayTemplate.FooRecord[].class);
    final DataMap dataFixture1 = new DataMap();
    final DataMap dataFixture2 = new DataMap();
    dataFixture1.put("bar", "Hello");
    dataFixture2.put("bar", "World");
    Assert.assertEquals(result, new TestArrayTemplate.FooRecord[]{ new TestArrayTemplate.FooRecord(dataFixture1), new TestArrayTemplate.FooRecord(dataFixture2) });
    Assert.assertSame(result.getClass(), TestArrayTemplate.FooRecord[].class);
  }

  private static Object convert(String listString, Class<?> targetClass) throws IOException
  {
    final DataList list = _codec.stringToList(listString);
    return DataTemplateUtil.convertDataListToArray(list, targetClass.getComponentType());
  }

  private static final JacksonDataCodec _codec = new JacksonDataCodec();
  private static final String _bytes5 = "\u0001\u0002\u0003\u0004\u0005";
  private static final String _bytes5Quoted = "\"\\u0001\\u0002\\u0003\\u0004\\u0005\"";
}
