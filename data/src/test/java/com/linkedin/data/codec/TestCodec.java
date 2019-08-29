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

package com.linkedin.data.codec;

import com.linkedin.data.Data;
import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;

import com.linkedin.data.codec.symbol.InMemorySymbolTable;
import com.linkedin.data.codec.symbol.SymbolTable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import static org.testng.Assert.assertEquals;


public class TestCodec
{
  static final PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out));

  private void testDataCodec(DataCodec codec, DataMap map) throws IOException
  {
    StringBuilder sb1 = new StringBuilder();
    Data.dump("map", map, "", sb1);

    // test mapToBytes

    byte[] bytes = codec.mapToBytes(map);

    // test bytesToMap

    DataMap map2 = codec.bytesToMap(bytes);
    StringBuilder sb2 = new StringBuilder();
    Data.dump("map", map2, "", sb2);
    TestUtil.assertEquivalent(map2, map);

    // test writeMap

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(bytes.length * 2);
    codec.writeMap(map, outputStream);
    byte[] outputStreamBytes = outputStream.toByteArray();
    assertEquals(outputStreamBytes, bytes);

    // test readMap

    ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStreamBytes);
    DataMap map3 = codec.readMap(inputStream);
    StringBuilder sb3 = new StringBuilder();
    Data.dump("map", map3, "", sb3);

    TestUtil.assertEquivalent(map3, map);
    TestUtil.assertEquivalent(map3, map2);

    if (codec instanceof TextDataCodec)
    {
      TextDataCodec textCodec = (TextDataCodec) codec;

      // test mapToString

      String string = textCodec.mapToString(map);

      // test stringToMap

      DataMap map4 = textCodec.stringToMap(string);
      StringBuilder sb4 = new StringBuilder();
      Data.dump("map", map4, "", sb4);
      assertEquals(sb4.toString(), sb1.toString());

      // test writeMap

      StringWriter writer = new StringWriter();
      textCodec.writeMap(map, writer);
      assertEquals(writer.toString(), string);

      // test readMap

      StringReader reader = new StringReader(string);
      DataMap map5 = textCodec.readMap(reader);
      StringBuilder sb5 = new StringBuilder();
      Data.dump("map", map5, "", sb5);
    }
  }

  private void testDataCodec(DataCodec codec, DataList list) throws IOException
  {
    StringBuilder sb1 = new StringBuilder();
    Data.dump("list", list, "", sb1);

    // test listToBytes

    byte[] bytes = codec.listToBytes(list);

    // test bytesToList

    DataList list2 = codec.bytesToList(bytes);
    StringBuilder sb2 = new StringBuilder();
    Data.dump("list", list2, "", sb2);
    assertEquals(sb2.toString(), sb1.toString());

    // test writeList

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(bytes.length * 2);
    codec.writeList(list, outputStream);
    byte[] outputStreamBytes = outputStream.toByteArray();
    assertEquals(outputStreamBytes, bytes);

    // test readList

    ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStreamBytes);
    DataList list3 = codec.readList(inputStream);
    StringBuilder sb3 = new StringBuilder();
    Data.dump("list", list3, "", sb3);

    assertEquals(sb3.toString(), sb1.toString());


    if (codec instanceof TextDataCodec)
    {
      TextDataCodec textCodec = (TextDataCodec) codec;

      // test listToString

      String string = textCodec.listToString(list);

      // test stringToList

      DataList list4 = textCodec.stringToList(string);
      StringBuilder sb4 = new StringBuilder();
      Data.dump("list", list4, "", sb4);
      assertEquals(sb4.toString(), sb1.toString());

      // test writeList

      StringWriter writer = new StringWriter();
      textCodec.writeList(list, writer);
      assertEquals(writer.toString(), string);

      // test readList

      StringReader reader = new StringReader(string);
      DataList list5 = textCodec.readList(reader);
      StringBuilder sb5 = new StringBuilder();
      Data.dump("list", list5, "", sb5);
    }
  }

  void testDataCodec(DataCodec codec, DataComplex value) throws IOException
  {
    if (value.getClass() == DataMap.class)
    {
      testDataCodec(codec, (DataMap) value);
    }
    else
    {
      testDataCodec(codec, (DataList) value);
    }
  }

  private void timePerfTest(int count, Callable<?> func)
  {
    System.gc();
    long start = System.currentTimeMillis();
    int errors = 0;
    for (int i = 0; i < count; ++i)
    {
      try
      {
        func.call();
      }
      catch (Exception e)
      {
        errors++;
      }
    }
    long end = System.currentTimeMillis();
    long duration = end - start;
    double avgLatencyMsec = (double) duration / count;
    out.println(func + ", " + count + " calls in " + duration + " ms, latency per call " + avgLatencyMsec + " ms");
  }

  private void dataMapToBytesPerfTest(int count, final DataCodec codec, final DataMap map)
  {
    timePerfTest(count, new Callable<byte[]>()
    {
      public byte[] call() throws IOException
      {
        return codec.mapToBytes(map);
      }
      public String toString()
      {
        return "DataMap-to-bytes, " + codec.getClass().getName();
      }
    });
  }

  private void bytesToDataMapPerfTest(int count, final DataCodec codec, final byte[] bytes)
  {
    timePerfTest(count, new Callable<DataMap>()
    {
      public DataMap call() throws IOException
      {
        return codec.bytesToMap(bytes);
      }
      public String toString()
      {
        return"Bytes-to-DataMap, " + codec.getClass().getName();
      }
    });
  }

  private void perfTest(int count, DataMap map) throws IOException
  {
    List<DataCodec> codecs = new ArrayList<>();
    codecs.add(new JacksonDataCodec());
    codecs.add(new BsonDataCodec());
    codecs.add(new JacksonSmileDataCodec());
    Set<String> symbols = new HashSet<>();

    collectSymbols(symbols, map);
    final String sharedSymbolTableName = "SHARED";
    SymbolTable symbolTable = new InMemorySymbolTable(new ArrayList<>(symbols));

    JacksonLICORDataCodec.setSymbolTableProvider(symbolTableName -> {
      if (sharedSymbolTableName.equals(symbolTableName))
      {
        return symbolTable;
      }

      return null;
    });
    codecs.add(new JacksonLICORBinaryDataCodec(sharedSymbolTableName));
    codecs.add(new JacksonLICORTextDataCodec(sharedSymbolTableName));

    for (DataCodec codec : codecs)
    {
      byte[] bytes = codec.mapToBytes(map);
      out.println(codec.getClass().getName() + " serialized size " + bytes.length);
    }

    for (DataCodec codec : codecs)
    {
      dataMapToBytesPerfTest(count, codec, map);
    }

    for (DataCodec codec : codecs)
    {
      byte[] bytes = codec.mapToBytes(map);
      bytesToDataMapPerfTest(count, codec, bytes);
    }
  }

  public static void collectSymbols(Set<String> symbols, DataMap map)
  {
    for (Map.Entry<String, Object> entry : map.entrySet())
    {
      symbols.add(entry.getKey());

      Object value = entry.getValue();
      if (value instanceof DataMap)
      {
        collectSymbols(symbols, (DataMap) value);
      }
      else if (value instanceof DataList)
      {
        collectSymbols(symbols, (DataList) value);
      }
    }
  }

  public static void collectSymbols(Set<String> symbols, DataList list)
  {
    for (Object element : list)
    {
      if (element instanceof DataMap)
      {
        collectSymbols(symbols, (DataMap) element);
      }
      else if (element instanceof DataList)
      {
        collectSymbols(symbols, (DataList) element);
      }
    }
  }

  private static class JacksonLICORTextDataCodec extends JacksonLICORDataCodec
  {
    JacksonLICORTextDataCodec(String symbolTableName)
    {
      super(false, symbolTableName);
    }
  }

  private static class JacksonLICORBinaryDataCodec extends JacksonLICORDataCodec
  {
    JacksonLICORBinaryDataCodec(String symbolTableName)
    {
      super(true, symbolTableName);
    }
  }

  //@Test(dataProvider = "codecData", dataProviderClass = CodecDataProviders.class)
  public void perfTest(String testName, DataComplex value) throws IOException
  {
    if (value.getClass() == DataMap.class)
    {
      out.println("------------- " + testName + " -------------");
      perfTest(1000, (DataMap) value);
    }
  }

}
