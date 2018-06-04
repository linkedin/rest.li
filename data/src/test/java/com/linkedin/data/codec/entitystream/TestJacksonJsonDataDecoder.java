/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.data.codec.entitystream;

import com.linkedin.data.ChunkedByteStringWriter;
import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import com.linkedin.data.codec.CodecDataProviders;
import com.linkedin.entitystream.EntityStream;
import com.linkedin.entitystream.EntityStreams;
import com.linkedin.entitystream.Writer;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;


public class TestJacksonJsonDataDecoder
{
  @Test(dataProvider = "codecData", dataProviderClass = CodecDataProviders.class)
  public void testDecoder(String testName, DataComplex dataComplex)
      throws Exception
  {
    StringBuilder expected = new StringBuilder();
    Data.dump("dataComplex", dataComplex, "", expected);

    byte[] bytes = TestUtil.dataComplexToBytes(dataComplex);

    StringBuilder actual = new StringBuilder();
    Data.dump("dataComplex", decode(bytes), "", actual);

    assertEquals(actual.toString(), expected.toString());
  }

  @Test(dataProvider = "codecNumbersData", dataProviderClass = CodecDataProviders.class)
  public void testJacksonCodecNumbers(String json, Map<String, Object> map)
      throws Exception
  {
    DataMap dataMap = (DataMap) decode(json.getBytes());
    for (Map.Entry<String, Object> entry : map.entrySet())
    {
      Object value = dataMap.get(entry.getKey());
      assertEquals(value, entry.getValue());
      assertEquals(value.getClass(), entry.getValue().getClass());
    }
  }

  @Test
  public void testIntValues()
      throws Exception
  {
    // more JACKSON-targeted int value tests
    int inc = (Integer.MAX_VALUE - Integer.MAX_VALUE / 100) / 10000;
    for (int i = Integer.MAX_VALUE / 100; i <= Integer.MAX_VALUE && i > 0; i += inc)
    {
      String json = "{ \"int\" : " + i + " }";
      DataMap dataMap = decodeMap(json.getBytes());
      assertEquals(dataMap.getInteger("int"), Integer.valueOf(i));
    }
    for (int i = Integer.MIN_VALUE; i <= Integer.MIN_VALUE / 100 && i < 0; i += inc)
    {
      String json = "{ \"int\" : " + i + " }";
      DataMap dataMap = decodeMap(json.getBytes());
      assertEquals(dataMap.getInteger("int"), Integer.valueOf(i));
    }
  }

  @Test
  public void testLongValues()
      throws Exception
  {
    // more JACKSON long value tests
    long longInc = (Long.MAX_VALUE - Long.MAX_VALUE/100l) / 10000l;
    for (long i = Long.MAX_VALUE/100l ; i <= Long.MAX_VALUE && i > 0; i += longInc)
    {
      String json = "{ \"long\" : " + i + " }";
      DataMap dataMap = decodeMap(json.getBytes());
      assertEquals(dataMap.getLong("long"), Long.valueOf(i));
    }
    for (long i = Long.MIN_VALUE ; i <= Long.MIN_VALUE/100l && i < 0; i += longInc)
    {
      String json = "{ \"long\" : " + i + " }";
      DataMap dataMap = decodeMap(json.getBytes());
      assertEquals(dataMap.getLong("long"), Long.valueOf(i));
    }
  }

  @Test(dataProvider = "longKeyFromByteSource", dataProviderClass = CodecDataProviders.class)
  public void testLongKeyFromByteSource(byte[] bytes)
      throws Exception
  {
    final DataMap map = decodeMap(bytes);
    Assert.assertEquals(map.keySet().iterator().next().length(), 262146);
  }

  @DataProvider
  public Object[][] invalidJson()
  {
    return new Object[][]
        {

            new Object[]
                {
                    "Top-level boolean value",
                    "true",
                },
            new Object[]
                {
                    "Top-level string Value",
                    "\"top-level primitive value is not supported.\"",
                },
            new Object[]
                {
                    "Missing key in a map",
                    "{\"key\": 1, 2}",
                },
            new Object[]
                {
                    "Missing key in a map",
                    "{[1, 2]}",
                },
            new Object[]
                {
                    "Key in a list",
                    "[\"key\": [1, 2]]",
                },
            new Object[]
                {
                    "Incomplete JSON",
                    "{\"foo\":[1, 2, 3]",
                },
            new Object[]
                {
                    "Extra tokens",
                    "{\"key\": 3}, 8",
                },
        };
  }

  @Test(dataProvider = "invalidJson", expectedExceptions = ExecutionException.class)
  public void testInvalidJson(String testDescription, String json)
      throws Exception
  {
    decode(json.getBytes());
  }

  @Test
  public void testInvalidMap()
      throws Exception
  {
    byte[] json = "[1, 2, 4]".getBytes();
    decode(json);

    try
    {
      decodeMap(json);
      fail("Parsing list as map.");
    }
    catch (ExecutionException e)
    {
      // Expected.
    }
  }

  @Test
  public void testInvalidList()
      throws Exception
  {
    byte[] json = "{\"key\": true}".getBytes();
    decode(json);

    try
    {
      decodeList(json);
      fail("Parsing map as list");
    }
    catch (ExecutionException e)
    {
      // Expected.
    }
  }

  @Test
  public void testEmptySource()
      throws Exception
  {
    assertNull(decode(new byte[0]));
    assertNull(decode(" \n".getBytes()));
  }

  private static DataComplex decode(byte[] bytes)
      throws Exception
  {
    JacksonJsonDataDecoder<DataComplex> decoder = new JacksonJsonDataDecoder<>();
    return decode(bytes, decoder);
  }

  private static DataMap decodeMap(byte[] bytes)
      throws Exception
  {
    JacksonJsonDataMapDecoder decoder = new JacksonJsonDataMapDecoder();
    return decode(bytes, decoder);
  }

  private static DataList decodeList(byte[] bytes)
      throws Exception
  {
    JacksonJsonDataListDecoder decoder = new JacksonJsonDataListDecoder();
    return decode(bytes, decoder);
  }

  private static <T extends DataComplex> T decode(byte[] bytes, JacksonJsonDataDecoder<T> decoder)
      throws Exception
  {
    Writer<ByteString> writer = new ChunkedByteStringWriter(bytes, 3);
    EntityStream<ByteString> entityStream = EntityStreams.newEntityStream(writer);
    entityStream.setReader(decoder);

    return decoder.getResult().toCompletableFuture().get();
  }
}
