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
import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import com.linkedin.data.codec.CodecDataProviders;
import com.linkedin.data.codec.ProtobufCodecOptions;
import com.linkedin.data.codec.symbol.InMemorySymbolTable;
import com.linkedin.data.codec.symbol.SymbolTable;
import com.linkedin.entitystream.EntityStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import org.testng.annotations.Test;


public class TestStreamCodec
{

  @Test(dataProvider = "smallCodecData", dataProviderClass = CodecDataProviders.class)
  public void testDecoder(String testName, DataComplex dataComplex)
      throws Exception {
    List<StreamDataCodec> codecs = getCodecs(1, dataComplex);
    for (StreamDataCodec codec : codecs)
    {
      testDataCodec(codec, dataComplex);
    }
  }

  private void testDataCodec(StreamDataCodec codec, DataComplex value) throws Exception
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

  private void testDataCodec(StreamDataCodec codec, DataMap map) throws Exception
  {
    // test encoder
    EntityStream<ByteString> byteStream = codec.encodeMap(map);

    // test decoder
    CompletionStage<DataMap> result = codec.decodeMap(byteStream);
    TestUtil.assertEquivalent(result.toCompletableFuture().get(), map);
  }

  private void testDataCodec(StreamDataCodec codec, DataList list) throws Exception
  {
    // test listToBytes
    EntityStream<ByteString> byteStream = codec.encodeList(list);

    // test bytesToList
    CompletionStage<DataList> result = codec.decodeList(byteStream);
    TestUtil.assertEquivalent(result.toCompletableFuture().get(), list);
  }

  private List<StreamDataCodec> getCodecs(int bufferSize, DataComplex data)
  {
    List<StreamDataCodec> codecs = new ArrayList<>();
    codecs.add(new JacksonSmileStreamDataCodec(bufferSize));
    codecs.add(new JacksonStreamDataCodec(bufferSize));
    codecs.add(new ProtobufStreamDataCodec(bufferSize));
    Set<String> symbols = new HashSet<>();
    if (data instanceof DataMap) {
      collectSymbols(symbols, (DataMap) data);
    } else {
      collectSymbols(symbols, (DataList) data);
    }
    final String sharedSymbolTableName = "SHARED";
    SymbolTable symbolTable = new InMemorySymbolTable(sharedSymbolTableName, new ArrayList<>(symbols));

    codecs.add(new JacksonLICORStreamDataCodec(bufferSize, true, symbolTable));
    codecs.add(new ProtobufStreamDataCodec(bufferSize,
        new ProtobufCodecOptions.Builder().setSymbolTable(symbolTable).setEnableASCIIOnlyStrings(true).build()));
    return codecs;
  }

  private static void collectSymbols(Set<String> symbols, DataMap map)
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

  private static void collectSymbols(Set<String> symbols, DataList list)
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
}
