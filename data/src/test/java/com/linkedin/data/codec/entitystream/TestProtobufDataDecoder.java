/*
   Copyright (c) 2020 LinkedIn Corp.

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
import com.linkedin.data.codec.ProtobufCodecOptions;
import com.linkedin.data.codec.ProtobufDataCodec;
import com.linkedin.entitystream.EntityStream;
import com.linkedin.entitystream.EntityStreams;
import com.linkedin.entitystream.Writer;
import java.util.concurrent.ExecutionException;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


public class TestProtobufDataDecoder
{
  @Test(dataProvider = "protobufCodecData", dataProviderClass = CodecDataProviders.class)
  public void testDecoder(String testName, DataComplex dataComplex, boolean enableFixedLengthFloatDoubles)
      throws Exception {
    ProtobufDataCodec codec = new ProtobufDataCodec(
        new ProtobufCodecOptions.Builder().setEnableFixedLengthFloatDoubles(enableFixedLengthFloatDoubles)
            .setEnableASCIIOnlyStrings(true).build());
    byte[] bytes = TestUtil.dataComplexToBytes(codec, dataComplex);
    DataComplex decodedDataComplex = decode(bytes, 20);
    assertEquals(TestUtil.dataComplexToBytes(codec, decodedDataComplex), bytes);
  }

  @Test(dataProvider = "streamCodecData", dataProviderClass = CodecDataProviders.class)
  public void testDecoder(String testName, DataComplex dataComplex, int chunkSize)
      throws Exception {
    ProtobufDataCodec codec = new ProtobufDataCodec(
        new ProtobufCodecOptions.Builder().setEnableASCIIOnlyStrings(true).build());
    byte[] bytes = TestUtil.dataComplexToBytes(codec, dataComplex);
    DataComplex decodedDataComplex = decode(bytes, chunkSize);
    assertEquals(TestUtil.dataComplexToBytes(codec, decodedDataComplex), bytes);
  }

  @Test(dataProvider = "numbersData", dataProviderClass = CodecDataProviders.class)
  public void testNumbers(Object number) throws Exception
  {
    DataMap dataMap = new DataMap();
    dataMap.put("number", number);
    byte[] bytes =
        TestUtil.dataComplexToBytes(new ProtobufDataCodec(new ProtobufCodecOptions.Builder().build()), dataMap);
    assertEquals(decode(bytes, 20), dataMap);
  }

  @Test
  public void testIntValues() throws Exception
  {
    int inc = (Integer.MAX_VALUE - Integer.MAX_VALUE / 100) / 10000;
    for (int i = Integer.MAX_VALUE / 100; i <= Integer.MAX_VALUE && i > 0; i += inc)
    {
      DataMap dataMap = new DataMap();
      dataMap.put("int", i);
      byte[] bytes =
          TestUtil.dataComplexToBytes(new ProtobufDataCodec(new ProtobufCodecOptions.Builder().build()), dataMap);
      DataMap decodedMap = (DataMap) decode(bytes, 20);
      assertEquals(decodedMap.getInteger("int"), Integer.valueOf(i));
    }
    for (int i = Integer.MIN_VALUE; i <= Integer.MIN_VALUE / 100 && i < 0; i += inc)
    {
      DataMap dataMap = new DataMap();
      dataMap.put("int", i);
      byte[] bytes =
          TestUtil.dataComplexToBytes(new ProtobufDataCodec(new ProtobufCodecOptions.Builder().build()), dataMap);
      DataMap decodedMap = (DataMap) decode(bytes, 20);
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
      byte[] bytes =
          TestUtil.dataComplexToBytes(new ProtobufDataCodec(new ProtobufCodecOptions.Builder().build()), dataMap);
      DataMap decodedMap = (DataMap) decode(bytes, 20);
      assertEquals(decodedMap.getLong("long"), Long.valueOf(i));
    }
    for (long i = Long.MIN_VALUE; i <= Long.MIN_VALUE / 100L && i < 0; i += longInc)
    {
      DataMap dataMap = new DataMap();
      dataMap.put("long", i);
      byte[] bytes =
          TestUtil.dataComplexToBytes(new ProtobufDataCodec(new ProtobufCodecOptions.Builder().build()), dataMap);
      DataMap decodedMap = (DataMap) decode(bytes, 20);
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
    byte[] bytes =
        TestUtil.dataComplexToBytes(new ProtobufDataCodec(new ProtobufCodecOptions.Builder().build()), dataList);
    decode(bytes, 3);

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
    byte[] bytes =
        TestUtil.dataComplexToBytes(new ProtobufDataCodec(new ProtobufCodecOptions.Builder().build()), dataMap);
    decode(bytes, 3);

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

  private static DataComplex decode(byte[] bytes, int chunkSize) throws Exception
  {
    ProtobufDataDecoder<DataComplex> decoder = new ProtobufDataDecoder<>(null, AbstractDataDecoder.START_TOKENS);
    return decode(bytes, decoder, chunkSize);
  }

  private static DataMap decodeMap(byte[] bytes) throws Exception
  {
    ProtobufDataDecoder<DataMap> decoder =
        new ProtobufDataDecoder<>(null, AbstractDataDecoder.START_OBJECT_TOKEN);
    return decode(bytes, decoder, 3);
  }

  private static DataList decodeList(byte[] bytes) throws Exception
  {
    ProtobufDataDecoder<DataList> decoder =
        new ProtobufDataDecoder<>(null, AbstractDataDecoder.START_ARRAY_TOKEN);
    return decode(bytes, decoder, 3);
  }

  private static <T extends DataComplex> T decode(byte[] bytes, ProtobufDataDecoder<T> decoder, int chunkSize)
      throws Exception
  {
    Writer<ByteString> writer = new ChunkedByteStringWriter(bytes, chunkSize);
    EntityStream<ByteString> entityStream = EntityStreams.newEntityStream(writer);
    entityStream.setReader(decoder);

    return decoder.getResult().toCompletableFuture().get();
  }
}
