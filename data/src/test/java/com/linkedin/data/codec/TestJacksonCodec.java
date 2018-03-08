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

package com.linkedin.data.codec;


import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.linkedin.data.Data;
import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.TestData;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.Instantiatable;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/**
 * Tests specific to {@link JacksonDataCodec}
 */
public class TestJacksonCodec extends TestCodec
{
  @Test(dataProvider = "codecData", dataProviderClass = CodecDataProviders.class)
  public void testJacksonDataCodec(String testName, DataComplex dataComplex) throws IOException
  {
    JacksonDataCodec codec = new JacksonDataCodec();
    testDataCodec(codec, dataComplex);
  }

  @Test
  public void testJacksonDataCodec() throws IOException
  {
    JacksonDataCodec codec = new JacksonDataCodec();
    testDataCodec(codec, TestData.referenceDataMap1);

    DataList list1 = codec.bytesToList("[7,27,279]".getBytes());
    assertEquals(list1, new DataList(Arrays.asList(7, 27, 279)));

    DataList list2 = new DataList(Arrays.asList(321, 21, 1));
    assertEquals(codec.listToBytes(list2), "[321,21,1]".getBytes());

    DataMap map3 = codec.stringToMap("{ \"a\" : null }");
    // out.println(map3.getError());
    assertSame(map3.get("a"), Data.NULL);

    DataMap map4 = codec.stringToMap("{ \"b\" : 123456789012345678901234567890 }");
    // out.println(map4.getError());
    assertTrue(map4.getError().indexOf(" value: 123456789012345678901234567890, token: VALUE_NUMBER_INT, number type: BIG_INTEGER not parsed.") != -1);

    DataMap map5 = codec.stringToMap("{ \"a\" : null, \"b\" : 123456789012345678901234567890 }");
    // out.println(map5.getError());
    assertTrue(map5.getError().indexOf(" value: 123456789012345678901234567890, token: VALUE_NUMBER_INT, number type: BIG_INTEGER not parsed.") != -1);

    // Test comments
    codec.setAllowComments(true);
    DataMap map6 = codec.stringToMap("/* abc */ { \"a\" : \"b\" }");
    assertEquals(map6.get("a"), "b");

    // Test getStringEncoding
    String encoding = codec.getStringEncoding();
    assertEquals(encoding, "UTF-8");
    assertEquals(encoding, JsonEncoding.UTF8.getJavaName());
  }

  @Test(dataProvider = "codecNumbersData", dataProviderClass = CodecDataProviders.class)
  public void testJacksonCodecNumbers(String json, Map<String, Object> map) throws IOException
  {
    JacksonDataCodec codec = new JacksonDataCodec();
    DataMap dataMap = codec.stringToMap(json);
    for (Map.Entry<String, Object> entry : map.entrySet())
    {
      Object value = dataMap.get(entry.getKey());
      assertEquals(value, entry.getValue());
      assertEquals(value.getClass(), entry.getValue().getClass());
    }
  }

  @Test
  public void testIntValues() throws IOException
  {
    JacksonDataCodec codec = new JacksonDataCodec();

    // more JACKSON-targeted int value tests
    int inc = (Integer.MAX_VALUE - Integer.MAX_VALUE / 100) / 10000;
    for (int i = Integer.MAX_VALUE / 100; i <= Integer.MAX_VALUE && i > 0; i += inc)
    {
      String json = "{ \"int\" : " + i + " }";
      DataMap dataMap = codec.stringToMap(json);
      assertEquals(dataMap.getInteger("int"), Integer.valueOf(i));
    }
    for (int i = Integer.MIN_VALUE; i <= Integer.MIN_VALUE / 100 && i < 0; i += inc)
    {
      String json = "{ \"int\" : " + i + " }";
      DataMap dataMap = codec.stringToMap(json);
      assertEquals(dataMap.getInteger("int"), Integer.valueOf(i));
    }
  }

  @Test
  public void testLongValues() throws IOException
  {
    JacksonDataCodec codec = new JacksonDataCodec();

    // more JACKSON long value tests
    long longInc = (Long.MAX_VALUE - Long.MAX_VALUE/100l) / 10000l;
    for (long i = Long.MAX_VALUE/100l ; i <= Long.MAX_VALUE && i > 0; i += longInc)
    {
      String json = "{ \"long\" : " + i + " }";
      DataMap dataMap = codec.stringToMap(json);
      assertEquals(dataMap.getLong("long"), Long.valueOf(i));
    }
    for (long i = Long.MIN_VALUE ; i <= Long.MIN_VALUE/100l && i < 0; i += longInc)
    {
      String json = "{ \"long\" : " + i + " }";
      DataMap dataMap = codec.stringToMap(json);
      assertEquals(dataMap.getLong("long"), Long.valueOf(i));
    }
  }

  /**
   * Test to make sure that field names are not interned.
   *
   * @throws IOException
   */
  @Test
  public void testNoStringIntern() throws IOException
  {
    final String keyName = "testKey";
    final String json = "{ \"" + keyName + "\" : 1 }";
    final byte[] jsonAsBytes = json.getBytes(Data.UTF_8_CHARSET);

    {
      final JsonFactory jsonFactory = new JsonFactory();
      final JacksonDataCodec codec = new JacksonDataCodec(jsonFactory);
      // make sure intern field names is not enabled
      assertFalse(jsonFactory.isEnabled(JsonFactory.Feature.INTERN_FIELD_NAMES));
      assertTrue(jsonFactory.isEnabled(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES));
      final DataMap map = codec.bytesToMap(jsonAsBytes);
      final String key = map.keySet().iterator().next();
      assertNotSame(key, keyName);
    }

    {
      final JsonFactory jsonFactory = new JsonFactory();
      final JacksonDataCodec codec = new JacksonDataCodec(jsonFactory);
      // enable intern field names
      jsonFactory.enable(JsonFactory.Feature.INTERN_FIELD_NAMES);
      assertTrue(jsonFactory.isEnabled(JsonFactory.Feature.INTERN_FIELD_NAMES));
      assertTrue(jsonFactory.isEnabled(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES));
      final DataMap map = codec.bytesToMap(jsonAsBytes);
      final String key = map.keySet().iterator().next();
      assertSame(key, keyName);
    }
  }

  @Test(dataProvider = "longKeyFromByteSource", dataProviderClass = CodecDataProviders.class)
  public void testLongKeyFromByteSource(byte[] bytes) throws IOException
  {
    final JacksonDataCodec codec = new JacksonDataCodec();
    final DataMap map = codec.bytesToMap(bytes);
    Assert.assertEquals(map.keySet().iterator().next().length(), 262146);
  }

  @Test(expectedExceptions = IOException.class)
  public void testJacksonDataCodecErrorEmptyInput() throws IOException
  {
    final JacksonDataCodec codec = new JacksonDataCodec();
    final ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
    codec.readMap(in);
  }

  @Test(expectedExceptions = DataDecodingException.class)
  public void testJacksonDataCodecErrorToList() throws IOException
  {
    final JacksonDataCodec codec = new JacksonDataCodec();
    codec.bytesToList("{\"A\": 1}".getBytes());
  }

  @Test(expectedExceptions = DataDecodingException.class)
  public void testJacksonDataCodecErrorToMap() throws IOException
  {
    final JacksonDataCodec codec = new JacksonDataCodec();
    codec.bytesToMap("[1, 2, 3]".getBytes());
  }

  @Test
  public void testPrettyPrinter()
      throws IOException
  {
    JacksonDataCodec codec = new JacksonDataCodec();
    codec.setPrettyPrinter(new StatefulPrettyPrinter());

    DataMap dataMap = new DataMap();
    String s1 = codec.mapToString(dataMap);
    String s2 = codec.mapToString(dataMap);

    assertNotEquals(s1, s2);

    codec.setPrettyPrinter(new InstantiableStatefulPrettyPrinter());

    s1 = codec.mapToString(dataMap);
    s2 = codec.mapToString(dataMap);

    assertEquals(s1, s2);
  }

  class StatefulPrettyPrinter implements PrettyPrinter
  {
    private int _count;

    @Override
    public void writeRootValueSeparator(JsonGenerator gen) {}

    @Override
    public void writeStartObject(JsonGenerator gen)
        throws IOException
    {
      gen.writeRaw(String.valueOf(_count++));
    }

    @Override
    public void writeEndObject(JsonGenerator gen, int nrOfEntries) {}

    @Override
    public void writeObjectEntrySeparator(JsonGenerator gen) {}

    @Override
    public void writeObjectFieldValueSeparator(JsonGenerator gen) {}

    @Override
    public void writeStartArray(JsonGenerator gen) {}

    @Override
    public void writeEndArray(JsonGenerator gen, int nrOfValues) {}

    @Override
    public void writeArrayValueSeparator(JsonGenerator gen) {}

    @Override
    public void beforeArrayValues(JsonGenerator gen) {}

    @Override
    public void beforeObjectEntries(JsonGenerator gen) {}
  }

  class InstantiableStatefulPrettyPrinter extends StatefulPrettyPrinter implements Instantiatable<InstantiableStatefulPrettyPrinter>
  {

    @Override
    public InstantiableStatefulPrettyPrinter createInstance()
    {
      return new InstantiableStatefulPrettyPrinter();
    }
  }
}
