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
import com.linkedin.data.template.TemplateOutputCastException;
import com.linkedin.pegasus.generator.test.Fixed16;
import com.linkedin.pegasus.generator.test.RecordBar;
import com.linkedin.pegasus.generator.test.RecordBarArray;
import com.linkedin.pegasus.generator.test.RecordBarMap;
import com.linkedin.pegasus.generator.test.Union;
import com.linkedin.restli.internal.common.ValueConverter;
import com.linkedin.restli.server.ResourceConfigException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


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
  public void testSimpleArray()
  {
    Object result;

    Assert.assertEquals(test("[\"A\", \"B\"]", String[].class), new String[]{"A", "B"});

    result = test("[true, false]", boolean[].class);
    Assert.assertEquals(result, new boolean[] { true, false });
    Assert.assertSame(result.getClass(), boolean[].class);
    result = test("[true, false]", Boolean[].class);
    Assert.assertEquals(result, new Boolean[] { true, false });
    Assert.assertSame(result.getClass(), Boolean[].class);

    result = test("[1, 2, 3]", int[].class);
    Assert.assertEquals(result, new int[] { 1, 2, 3 });
    Assert.assertSame(result.getClass(), int[].class);
    result = test("[4, 5, 6]", Integer[].class);
    Assert.assertEquals(result, new Integer[] { 4, 5, 6 });
    Assert.assertSame(result.getClass(), Integer[].class);

    result = test("[1, 2, 3]", float[].class);
    Assert.assertEquals(result, new float[] { 1F, 2F, 3F });
    Assert.assertSame(result.getClass(), float[].class);

    result = test("[1.1, 2.2, 3.3]", double[].class);
    Assert.assertEquals(result, new double[] { 1.1D, 2.2D, 3.3D });
    Assert.assertSame(result.getClass(), double[].class);
    result = test("[1.1, 2.2, 3.3]", Double[].class);
    Assert.assertEquals(result, new Double[] { 1.1D, 2.2D, 3.3D });
    Assert.assertSame(result.getClass(), Double[].class);

    result = test("[1.1, 2.2, 3.3]", int[].class);
    Assert.assertEquals(result, new int[] { 1, 2, 3 });
    Assert.assertSame(result.getClass(), int[].class);

    Assert.assertEquals(test("[" + _bytes16Quoted + ", " + _bytes16Quoted + "]", ByteString[].class),
                        new ByteString[] { ByteString.copyAvroString(_bytes16, true), ByteString.copyAvroString(_bytes16, true) });
  }

  @Test
  public void testComplexArray()
  {
    Object result;

    result = test("[[1.1], [2.2]]", DoubleArray[].class);
    Assert.assertEquals(result, new DoubleArray[] { new DoubleArray(Arrays.asList(1.1)), new DoubleArray(Arrays.asList(2.2)) });
    Assert.assertSame(result.getClass(), DoubleArray[].class);

    result = test("[" + _bytes16Quoted + ", " + _bytes16Quoted + "]", Fixed16[].class);
    Assert.assertEquals(result, new Fixed16[] { new Fixed16(_bytes16), new Fixed16(_bytes16) });
    Assert.assertSame(result.getClass(), Fixed16[].class);

    result = test("[{\"A\": 3}, {\"B\": 4}]", IntegerMap[].class);
    final Map<String, Integer> integerFixture1 = new HashMap<String, Integer>();
    final Map<String, Integer> integerFixture2 = new HashMap<String, Integer>();
    integerFixture1.put("A", 3);
    integerFixture2.put("B", 4);
    Assert.assertEquals(result, new IntegerMap[] { new IntegerMap(integerFixture1), new IntegerMap(integerFixture2) });
    Assert.assertSame(result.getClass(), IntegerMap[].class);

    result = test("[{\"location\": \"LinkedIn\"}, {\"location\": \"Mountain View\"}]", RecordBar[].class);
    final DataMap dataFixture1 = new DataMap();
    final DataMap dataFixture2 = new DataMap();
    dataFixture1.put("location", "LinkedIn");
    dataFixture2.put("location", "Mountain View");
    Assert.assertEquals(result, new RecordBar[]{ new RecordBar(dataFixture1), new RecordBar(dataFixture2) });
    Assert.assertSame(result.getClass(), RecordBar[].class);
  }

  @Test
  public void testFixed()
  {
    final Object result = test(_bytes16, Fixed16.class);
    Assert.assertEquals(result, new Fixed16(_bytes16));
    Assert.assertSame(result.getClass(), Fixed16.class);
  }

  @Test(expectedExceptions = TemplateOutputCastException.class)
  public void testFailedFixed()
  {
    // cannot create fixed with mismatched data size
   test(_bytes16.substring(1), Fixed16.class);
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

    result = test("[2, 3, 4]", LongArray.class);
    Assert.assertEquals(result, new LongArray(Arrays.asList(2L, 3L, 4L)));
    Assert.assertSame(result.getClass(), LongArray.class);

    result = test("[1.1, 2.2, 3.3]", FloatArray.class);
    Assert.assertEquals(result, new FloatArray(Arrays.asList(1.1F, 2.2F, 3.3F)));
    Assert.assertSame(result.getClass(), FloatArray.class);

    result = test("[2.2, 3.3, 4.4]", DoubleArray.class);
    Assert.assertEquals(result, new DoubleArray(Arrays.asList(2.2D, 3.3D, 4.4D)));
    Assert.assertSame(result.getClass(), DoubleArray.class);

    result = test("[" + _bytes16Quoted + ", " + _bytes16Quoted + "]", BytesArray.class);
    Assert.assertEquals(result, new BytesArray(Arrays.asList(ByteString.copyAvroString(_bytes16, true), ByteString.copyAvroString(_bytes16, true))));
    Assert.assertSame(result.getClass(), BytesArray.class);

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
    Map<String, Object> fixture;

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
    Object result;

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
