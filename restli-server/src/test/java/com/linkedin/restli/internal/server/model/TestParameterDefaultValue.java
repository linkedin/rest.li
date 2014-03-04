/*
   Copyright (c) 2013 LinkedIn Corp.

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

package com.linkedin.restli.internal.server.model;


import com.linkedin.data.ByteString;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.BooleanArray;
import com.linkedin.data.template.BooleanMap;
import com.linkedin.data.template.BytesArray;
import com.linkedin.data.template.BytesMap;
import com.linkedin.data.template.DoubleArray;
import com.linkedin.data.template.DoubleMap;
import com.linkedin.data.template.FloatArray;
import com.linkedin.data.template.FloatMap;
import com.linkedin.data.template.IntegerArray;
import com.linkedin.data.template.IntegerMap;
import com.linkedin.data.template.LongArray;
import com.linkedin.data.template.LongMap;
import com.linkedin.data.template.StringArray;
import com.linkedin.data.template.StringMap;
import com.linkedin.pegasus.generator.test.ArrayTest;
import com.linkedin.pegasus.generator.test.EnumFruits;
import com.linkedin.pegasus.generator.test.EnumFruitsArray;
import com.linkedin.pegasus.generator.test.FixedMD5;
import com.linkedin.pegasus.generator.test.FixedMD5Array;
import com.linkedin.pegasus.generator.test.RecordBar;
import com.linkedin.pegasus.generator.test.RecordBarArray;
import com.linkedin.pegasus.generator.test.RecordBarMap;
import com.linkedin.pegasus.generator.test.Union;
import com.linkedin.restli.server.ResourceConfigException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Keren Jin
 */
public class TestParameterDefaultValue
{
  TestParameterDefaultValue()
  {
    assert(_bytes16.length() == 16);
  }

  @Test
  public void testFixed()
  {
    final Object result = test(_bytes16, FixedMD5.class);
    Assert.assertEquals(result, new FixedMD5(_bytes16));
    Assert.assertSame(result.getClass(), FixedMD5.class);
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void testFailedFixed()
  {
    // cannot create fixed with mismatched data size
   test(_bytes16.substring(1), FixedMD5.class);
  }

  @Test
  public void testWrappedArray()
  {
    Object result;

    result = test("[\"Hello\", \"World\"]", StringArray.class);
    Assert.assertEquals(result, new StringArray(Arrays.asList("Hello", "World")));
    Assert.assertSame(result.getClass(), StringArray.class);

    result = test("[false, true]", BooleanArray.class);
    Assert.assertEquals(result, new BooleanArray(Arrays.asList(false, true)));
    Assert.assertSame(result.getClass(), BooleanArray.class);

    result = test("[1, 2, 3]", IntegerArray.class);
    Assert.assertEquals(result, new IntegerArray(Arrays.asList(1, 2, 3)));
    Assert.assertSame(result.getClass(), IntegerArray.class);

    result = test("[1.1, 2.2, 3.3]", IntegerArray.class);
    Assert.assertEquals(result, new IntegerArray(Arrays.asList(1, 2, 3)));
    Assert.assertSame(result.getClass(), IntegerArray.class);

    result = test("[2, 3, 4]", LongArray.class);
    Assert.assertEquals(result, new LongArray(Arrays.asList(2L, 3L, 4L)));
    Assert.assertSame(result.getClass(), LongArray.class);

    result = test("[1.1, 2.2, 3.3]", FloatArray.class);
    Assert.assertEquals(result, new FloatArray(Arrays.asList(1.1F, 2.2F, 3.3F)));
    Assert.assertSame(result.getClass(), FloatArray.class);

    result = test("[2.2, 3.3, 4.4]", DoubleArray.class);
    Assert.assertEquals(result, new DoubleArray(Arrays.asList(2.2D, 3.3D, 4.4D)));
    Assert.assertSame(result.getClass(), DoubleArray.class);

    result = test("[\"APPLE\", \"BANANA\"]", EnumFruitsArray.class);
    Assert.assertEquals(result, new EnumFruitsArray(Arrays.asList(EnumFruits.APPLE, EnumFruits.BANANA)));
    Assert.assertSame(result.getClass(), EnumFruitsArray.class);

    result = test("[" + _bytes16Quoted + ", " + _bytes16Quoted + "]", BytesArray.class);
    Assert.assertEquals(result, new BytesArray(Arrays.asList(ByteString.copyAvroString(_bytes16, true), ByteString.copyAvroString(_bytes16, true))));
    Assert.assertSame(result.getClass(), BytesArray.class);

    result = test("[" + _bytes16Quoted + ", " + _bytes16Quoted + "]", FixedMD5Array.class);
    Assert.assertEquals(result, new FixedMD5Array(Arrays.asList(new FixedMD5(_bytes16), new FixedMD5(_bytes16))));
    Assert.assertSame(result.getClass(), FixedMD5Array.class);

    result = test("[{\"string\": \"String in union\"}, {\"int\": 1}]", ArrayTest.UnionArrayArray.class);
    final ArrayTest.UnionArray fixture1 = new ArrayTest.UnionArray();
    fixture1.setString("String in union");
    final ArrayTest.UnionArray fixture2 = new ArrayTest.UnionArray();
    fixture2.setInt(1);
    Assert.assertEquals(result, new ArrayTest.UnionArrayArray(Arrays.asList(fixture1, fixture2)));
    Assert.assertSame(result.getClass(), ArrayTest.UnionArrayArray.class);

    result = test("[{\"location\": \"Sunnyvale\"}, {\"location\": \"Mountain View\"}]", RecordBarArray.class);
    final DataMap dataFixture1 = new DataMap();
    final DataMap dataFixture2 = new DataMap();
    dataFixture1.put("location", "Sunnyvale");
    dataFixture2.put("location", "Mountain View");
    Assert.assertEquals(result, new RecordBarArray(Arrays.asList(new RecordBar(dataFixture1), new RecordBar(dataFixture2))));
    Assert.assertSame(result.getClass(), RecordBarArray.class);
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void testFailedWrappedArray()
  {
    // validation should fail
    test("[\"Hello\", \"World\"]", IntegerArray.class);
  }

  @Test
  public void testWrappedMap()
  {
    Object result;

    result = test("{\"key1\": \"Hello\", \"key2\": \"World\"}", StringMap.class);
    final Map<String, String> stringFixture = new HashMap<String, String>();
    stringFixture.put("key1", "Hello");
    stringFixture.put("key2", "World");
    Assert.assertEquals(result, new StringMap(stringFixture));
    Assert.assertSame(result.getClass(), StringMap.class);

    result = test("{\"key1\": true, \"key2\": false}", BooleanMap.class);
    final Map<String, Boolean> booleanFixture = new HashMap<String, Boolean>();
    booleanFixture.put("key1", true);
    booleanFixture.put("key2", false);
    Assert.assertEquals(result, new BooleanMap(booleanFixture));
    Assert.assertSame(result.getClass(), BooleanMap.class);

    result = test("{\"key1\": 1, \"key2\": 2}", IntegerMap.class);
    final Map<String, Integer> integerFixture = new HashMap<String, Integer>();
    integerFixture.put("key1", 1);
    integerFixture.put("key2", 2);
    Assert.assertEquals(result, new IntegerMap(integerFixture));
    Assert.assertSame(result.getClass(), IntegerMap.class);

    result = test("{\"key1\": 2, \"key2\": 3}", LongMap.class);
    final Map<String, Long> longFixture = new HashMap<String, Long>();
    longFixture.put("key1", 2L);
    longFixture.put("key2", 3L);
    Assert.assertEquals(result, new LongMap(longFixture));
    Assert.assertSame(result.getClass(), LongMap.class);

    result = test("{\"key1\": 1.1, \"key2\": 2.2}", FloatMap.class);
    final Map<String, Float> floatFixture = new HashMap<String, Float>();
    floatFixture.put("key1", 1.1F);
    floatFixture.put("key2", 2.2F);
    Assert.assertEquals(result, new FloatMap(floatFixture));
    Assert.assertSame(result.getClass(), FloatMap.class);

    result = test("{\"key1\": 2.2, \"key2\": 3.3}", DoubleMap.class);
    final Map<String, Double> doubleFixture = new HashMap<String, Double>();
    doubleFixture.put("key1", 2.2D);
    doubleFixture.put("key2", 3.3D);
    Assert.assertEquals(result, new DoubleMap(doubleFixture));
    Assert.assertSame(result.getClass(), DoubleMap.class);

    result = test("{\"key1\": " + _bytes16Quoted + ", \"key2\": " + _bytes16Quoted + "}", BytesMap.class);
    final Map<String, ByteString> bytesFixture = new HashMap<String, ByteString>();
    bytesFixture.put("key1", ByteString.copyAvroString(_bytes16, true));
    bytesFixture.put("key2", ByteString.copyAvroString(_bytes16, true));
    Assert.assertEquals(result, new BytesMap(new DataMap(bytesFixture)));
    Assert.assertSame(result.getClass(), BytesMap.class);

    result = test("{\"key1\": {\"location\": \"Sunnyvale\"}, \"key2\": {\"location\": \"MTV\"}}", RecordBarMap.class);
    final DataMap dataFixture1 = new DataMap();
    final DataMap dataFixture2 = new DataMap();
    dataFixture1.put("location", "Sunnyvale");
    dataFixture2.put("location", "MTV");
    final RecordBar record1 = new RecordBar(dataFixture1);
    final RecordBar record2 = new RecordBar(dataFixture2);
    final Map<String, RecordBar> recordFixture = new HashMap<String, RecordBar>();
    recordFixture.put("key1", record1);
    recordFixture.put("key2", record2);
    Assert.assertEquals(result, new RecordBarMap(recordFixture));
    Assert.assertSame(result.getClass(), RecordBarMap.class);
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void testFailedWrappedMap()
  {
   test("{\"key1\": \"Hello\", \"key2\": \"World\"}", DoubleMap.class);
  }

  @Test
  public void testRecord()
  {
    final Map<String, Object> fixture;

    fixture = new HashMap<String, Object>();
    fixture.put("location", "LinkedIn");
    Assert.assertEquals(test("{\"location\": \"LinkedIn\"}", RecordBar.class), new RecordBar(new DataMap(fixture)));
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void testFailedRecord()
  {
    test("{\"wrongKey\": 1}", RecordBar.class);
  }

  @Test
  public void testUnion()
  {
    final Object result;

    result = test("{\"string\": \"String in union\"}", Union.class);
    final Union fixture = new Union();
    fixture.setString("String in union");
    Assert.assertEquals(result, fixture);
    Assert.assertSame(result.getClass(), Union.class);
  }

  private static <T> Object test(String value, Class<T> type)
  {
    return new Parameter<T>("", type, null, true, value, null, false, AnnotationSet.EMPTY).getDefaultValue();
  }

  private final String _bytes16 = "\u0001\u0002\u0003\u0004" +
                                  "\u0005\u0006\u0007\u0008" +
                                  "\u0009\n\u000B\u000C" +
                                  "\r\u000E\u000F\u0010";
  private final String _bytes16Quoted = "\"\\u0001\\u0002\\u0003\\u0004" +
                                        "\\u0005\\u0006\\u0007\\u0008" +
                                        "\\u0009\\n\\u000B\\u000C" +
                                        "\\r\\u000E\\u000F\\u0010\"";
}
