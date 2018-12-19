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

import com.linkedin.data.ByteString;
import com.linkedin.data.ChunkedByteStringWriter;
import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import com.linkedin.data.codec.CodecDataProviders;
import com.linkedin.data.codec.KSONDataCodec;
import com.linkedin.entitystream.EntityStream;
import com.linkedin.entitystream.EntityStreams;
import com.linkedin.entitystream.Writer;
import java.util.concurrent.ExecutionException;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class TestKSONBinaryDataDecoder
{
  private final KSONDataCodec KSON_BINARY_CODEC = new KSONDataCodec(true);

  @Test(dataProvider = "codecData", dataProviderClass = CodecDataProviders.class)
  public void testDecoder(String testName, DataComplex dataComplex) throws Exception
  {
    byte[] bytes = TestUtil.dataComplexToBytes(KSON_BINARY_CODEC, dataComplex);
    DataComplex decodedDataComplex = decode(bytes);
    assertEquals(TestUtil.dataComplexToBytes(KSON_BINARY_CODEC, decodedDataComplex), bytes);
  }

  @Test(dataProvider = "numbersData", dataProviderClass = CodecDataProviders.class)
  public void testNumbers(Object number) throws Exception
  {
    DataMap dataMap = new DataMap();
    dataMap.put("number", number);
    byte[] bytes = TestUtil.dataComplexToBytes(KSON_BINARY_CODEC, dataMap);
    assertEquals(decode(bytes), dataMap);
  }

  @Test
  public void testIntValues() throws Exception
  {
    int inc = (Integer.MAX_VALUE - Integer.MAX_VALUE / 100) / 10000;
    for (int i = Integer.MAX_VALUE / 100; i <= Integer.MAX_VALUE && i > 0; i += inc)
    {
      DataMap dataMap = new DataMap();
      dataMap.put("int", i);
      byte[] bytes = TestUtil.dataComplexToBytes(KSON_BINARY_CODEC, dataMap);
      DataMap decodedMap = (DataMap) decode(bytes);
      assertEquals(decodedMap.getInteger("int"), Integer.valueOf(i));
    }
    for (int i = Integer.MIN_VALUE; i <= Integer.MIN_VALUE / 100 && i < 0; i += inc)
    {
      DataMap dataMap = new DataMap();
      dataMap.put("int", i);
      byte[] bytes = TestUtil.dataComplexToBytes(KSON_BINARY_CODEC, dataMap);
      DataMap decodedMap = (DataMap) decode(bytes);
      assertEquals(decodedMap.getInteger("int"), Integer.valueOf(i));
    }
  }

  @Test
  public void testLongValues() throws Exception
  {
    long longInc = (Long.MAX_VALUE - Long.MAX_VALUE / 100L) / 10000L;
    for (long i = Long.MAX_VALUE / 100L; i <= Long.MAX_VALUE && i > 0; i += longInc)
    {
      DataMap dataMap = new DataMap();
      dataMap.put("long", i);
      byte[] bytes = TestUtil.dataComplexToBytes(KSON_BINARY_CODEC, dataMap);
      DataMap decodedMap = (DataMap) decode(bytes);
      assertEquals(decodedMap.getLong("long"), Long.valueOf(i));
    }
    for (long i = Long.MIN_VALUE; i <= Long.MIN_VALUE / 100L && i < 0; i += longInc)
    {
      DataMap dataMap = new DataMap();
      dataMap.put("long", i);
      byte[] bytes = TestUtil.dataComplexToBytes(KSON_BINARY_CODEC, dataMap);
      DataMap decodedMap = (DataMap) decode(bytes);
      assertEquals(decodedMap.getLong("long"), Long.valueOf(i));
    }
  }

  @Test
  public void testInvalidMap() throws Exception
  {
    DataList dataList = new DataList();
    dataList.add(1);
    dataList.add(2);
    dataList.add(4);
    byte[] bytes = TestUtil.dataComplexToBytes(KSON_BINARY_CODEC, dataList);
    decode(bytes);

    try
    {
      decodeMap(bytes);
      fail("Parsing list as map.");
    }
    catch (ExecutionException e)
    {
      // Expected.
    }
  }

  @Test
  public void testInvalidList() throws Exception
  {
    DataMap dataMap = new DataMap();
    dataMap.put("key", true);
    byte[] bytes = TestUtil.dataComplexToBytes(KSON_BINARY_CODEC, dataMap);
    decode(bytes);

    try
    {
      decodeList(bytes);
      fail("Parsing map as list");
    }
    catch (ExecutionException e)
    {
      // Expected.
    }
  }

  private static DataComplex decode(byte[] bytes) throws Exception
  {
    KSONDataDecoder<DataComplex> decoder = new KSONDataDecoder<>(true);
    return decode(bytes, decoder);
  }

  private static void decodeMap(byte[] bytes) throws Exception
  {
    KSONDataDecoder<DataMap> decoder = new KSONDataDecoder<>(true, false, null);
    decode(bytes, decoder);
  }

  private static void decodeList(byte[] bytes) throws Exception
  {
    KSONDataDecoder<DataList> decoder = new KSONDataDecoder<>(true, true, null);
    decode(bytes, decoder);
  }

  private static <T extends DataComplex> T decode(byte[] bytes, KSONDataDecoder<T> decoder) throws Exception
  {
    Writer<ByteString> writer = new ChunkedByteStringWriter(bytes, 3);
    EntityStream<ByteString> entityStream = EntityStreams.newEntityStream(writer);
    entityStream.setReader(decoder);

    return decoder.getResult().toCompletableFuture().get();
  }
}
