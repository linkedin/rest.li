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
import com.linkedin.data.codec.DataCodec;
import com.linkedin.data.codec.JacksonLICORDataCodec;
import com.linkedin.entitystream.EntityStream;
import com.linkedin.entitystream.EntityStreams;
import com.linkedin.entitystream.Writer;
import java.util.concurrent.ExecutionException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class TestJacksonLICORDataDecoder
{
  private static final JacksonLICORDataCodec BINARY_CODEC = new JacksonLICORDataCodec(true);
  private static final JacksonLICORDataCodec TEXT_CODEC = new JacksonLICORDataCodec(false);

  @Test(dataProvider = "LICORCodecData", dataProviderClass = CodecDataProviders.class)
  public void testDecoder(String testName, DataComplex dataComplex, boolean useBinary) throws Exception
  {
    DataCodec codec = getCodec(useBinary);
    byte[] bytes = TestUtil.dataComplexToBytes(codec, dataComplex);
    DataComplex decodedDataComplex = decode(bytes, useBinary);
    assertEquals(TestUtil.dataComplexToBytes(codec, decodedDataComplex), bytes);
  }

  @Test(dataProvider = "LICORNumbersData", dataProviderClass = CodecDataProviders.class)
  public void testNumbers(Object number, boolean useBinary) throws Exception
  {
    DataMap dataMap = new DataMap();
    dataMap.put("number", number);
    byte[] bytes = TestUtil.dataComplexToBytes(getCodec(useBinary), dataMap);
    assertEquals(decode(bytes, useBinary), dataMap);
  }

  @Test(dataProvider = "LICORCodecs")
  public void testIntValues(boolean useBinary) throws Exception
  {
    int inc = (Integer.MAX_VALUE - Integer.MAX_VALUE / 100) / 10000;
    for (int i = Integer.MAX_VALUE / 100; i <= Integer.MAX_VALUE && i > 0; i += inc)
    {
      DataMap dataMap = new DataMap();
      dataMap.put("int", i);
      byte[] bytes = TestUtil.dataComplexToBytes(getCodec(useBinary), dataMap);
      DataMap decodedMap = (DataMap) decode(bytes, useBinary);
      assertEquals(decodedMap.getInteger("int"), Integer.valueOf(i));
    }
    for (int i = Integer.MIN_VALUE; i <= Integer.MIN_VALUE / 100 && i < 0; i += inc)
    {
      DataMap dataMap = new DataMap();
      dataMap.put("int", i);
      byte[] bytes = TestUtil.dataComplexToBytes(getCodec(useBinary), dataMap);
      DataMap decodedMap = (DataMap) decode(bytes, useBinary);
      assertEquals(decodedMap.getInteger("int"), Integer.valueOf(i));
    }
  }

  @Test(dataProvider = "LICORCodecs")
  public void testLongValues(boolean useBinary) throws Exception
  {
    long longInc = (Long.MAX_VALUE - Long.MAX_VALUE / 100L) / 10000L;
    for (long i = Long.MAX_VALUE / 100L; i <= Long.MAX_VALUE && i > 0; i += longInc)
    {
      DataMap dataMap = new DataMap();
      dataMap.put("long", i);
      byte[] bytes = TestUtil.dataComplexToBytes(getCodec(useBinary), dataMap);
      DataMap decodedMap = (DataMap) decode(bytes, useBinary);
      assertEquals(decodedMap.getLong("long"), Long.valueOf(i));
    }
    for (long i = Long.MIN_VALUE; i <= Long.MIN_VALUE / 100L && i < 0; i += longInc)
    {
      DataMap dataMap = new DataMap();
      dataMap.put("long", i);
      byte[] bytes = TestUtil.dataComplexToBytes(getCodec(useBinary), dataMap);
      DataMap decodedMap = (DataMap) decode(bytes, useBinary);
      assertEquals(decodedMap.getLong("long"), Long.valueOf(i));
    }
  }

  @Test(dataProvider = "LICORCodecs")
  public void testInvalidMap(boolean useBinary) throws Exception
  {
    DataList dataList = new DataList();
    dataList.add(1);
    dataList.add(2);
    dataList.add(4);
    byte[] bytes = TestUtil.dataComplexToBytes(getCodec(useBinary), dataList);
    decode(bytes, useBinary);

    try
    {
      decodeMap(bytes, useBinary);
      fail("Parsing list as map.");
    }
    catch (ExecutionException e)
    {
      // Expected.
    }
  }

  @Test(dataProvider = "LICORCodecs")
  public void testInvalidList(boolean useBinary) throws Exception
  {
    DataMap dataMap = new DataMap();
    dataMap.put("key", true);
    byte[] bytes = TestUtil.dataComplexToBytes(getCodec(useBinary), dataMap);
    decode(bytes, useBinary);

    try
    {
      decodeList(bytes, useBinary);
      fail("Parsing map as list");
    }
    catch (ExecutionException e)
    {
      // Expected.
    }
  }

  @DataProvider
  public static Object[][] LICORCodecs()
  {
    return new Object[][]{{true}, {false}};
  }

  private static DataCodec getCodec(boolean useBinary)
  {
    return useBinary ? BINARY_CODEC : TEXT_CODEC;
  }

  private static DataComplex decode(byte[] bytes, boolean useBinary) throws Exception
  {
    JacksonLICORDataDecoder<DataComplex> decoder = new JacksonLICORDataDecoder<>(useBinary);
    return decode(bytes, decoder);
  }

  private static void decodeMap(byte[] bytes, boolean useBinary) throws Exception
  {
    JacksonLICORDataDecoder<DataMap> decoder = new JacksonLICORDataDecoder<>(useBinary, false, null);
    decode(bytes, decoder);
  }

  private static void decodeList(byte[] bytes, boolean useBinary) throws Exception
  {
    JacksonLICORDataDecoder<DataList> decoder = new JacksonLICORDataDecoder<>(useBinary, true, null);
    decode(bytes, decoder);
  }

  private static <T extends DataComplex> T decode(byte[] bytes, JacksonLICORDataDecoder<T> decoder) throws Exception
  {
    Writer<ByteString> writer = new ChunkedByteStringWriter(bytes, 3);
    EntityStream<ByteString> entityStream = EntityStreams.newEntityStream(writer);
    entityStream.setReader(decoder);

    return decoder.getResult().toCompletableFuture().get();
  }
}
