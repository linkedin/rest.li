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

import com.fasterxml.jackson.core.JsonFactory;
import com.linkedin.data.ChunkedByteStringCollector;
import com.linkedin.data.ByteString;
import com.linkedin.data.ChunkedByteStringWriter;
import com.linkedin.data.Data;
import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import com.linkedin.data.codec.CodecDataProviders;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.entitystream.CollectingReader;
import com.linkedin.entitystream.EntityStream;
import com.linkedin.entitystream.EntityStreams;

import com.linkedin.entitystream.Writer;
import java.io.IOException;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


public class TestJacksonJsonDataEncoder
{
  @Test(dataProvider = "codecData", dataProviderClass = CodecDataProviders.class)
  public void testEncoder(String testName, DataComplex dataComplex)
      throws Exception
  {
    byte[] expected = TestUtil.dataComplexToBytes(dataComplex);
    byte[] actual = encode(dataComplex);

    assertEquals(actual, expected);
  }

  /**
   * Test to make sure that field names are not interned by default.
   */
  @Test
  public void testStringInternDisabledByDefault() throws Exception
  {
    final String keyName = "testKey";
    DataMap dataMap = new DataMap();
    dataMap.put(keyName, 1);

    JacksonJsonDataEncoder encoder = new JacksonJsonDataEncoder(dataMap, 3);
    // make sure intern field names is disabled by default
    assertFalse(encoder._jsonFactory.isEnabled(JsonFactory.Feature.INTERN_FIELD_NAMES));

    EntityStream<ByteString> entityStream = EntityStreams.newEntityStream(encoder);
    CollectingReader<ByteString, ?, ChunkedByteStringCollector.Result> reader =
        new CollectingReader<>(new ChunkedByteStringCollector());
    entityStream.setReader(reader);

    byte[] encoded = reader.getResult().toCompletableFuture().get().data;

    JacksonJsonDataMapDecoder decoder = new JacksonJsonDataMapDecoder();
    // make sure intern field names is disabled
    assertFalse(decoder._jsonFactory.isEnabled(JsonFactory.Feature.INTERN_FIELD_NAMES));

    Writer<ByteString> writer = new ChunkedByteStringWriter(encoded, 3);
    entityStream = EntityStreams.newEntityStream(writer);
    entityStream.setReader(decoder);

    DataMap map = decoder.getResult().toCompletableFuture().get();

    final String key = map.keySet().iterator().next();
    assertEquals(key, keyName);
    assertNotSame(key, keyName);
  }

  private byte[] encode(DataComplex data)
      throws Exception
  {
    JacksonJsonDataEncoder encoder = data instanceof DataMap
        ? new JacksonJsonDataEncoder((DataMap) data, 3)
        : new JacksonJsonDataEncoder((DataList) data, 3);
    EntityStream<ByteString> entityStream = EntityStreams.newEntityStream(encoder);
    CollectingReader<ByteString, ?, ChunkedByteStringCollector.Result> reader = new CollectingReader<>(new ChunkedByteStringCollector());
    entityStream.setReader(reader);

    return reader.getResult().toCompletableFuture().get().data;
  }
}
