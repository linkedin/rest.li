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
import com.linkedin.data.ChunkedByteStringCollector;
import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import com.linkedin.data.codec.CodecDataProviders;
import com.linkedin.data.codec.KSONDataCodec;
import com.linkedin.entitystream.CollectingReader;
import com.linkedin.entitystream.EntityStream;
import com.linkedin.entitystream.EntityStreams;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class TestKSONDataEncoder
{
  private static final KSONDataCodec KSON_TEXT_CODEC = new KSONDataCodec(false);
  private static final KSONDataCodec KSON_BINARY_CODEC = new KSONDataCodec(true);

  @Test(dataProvider = "codecData", dataProviderClass = CodecDataProviders.class)
  public void testTextEncoder(String testName, DataComplex dataComplex) throws Exception

  {
    assertEquals(actualEncode(dataComplex, false), TestUtil.dataComplexToBytes(KSON_TEXT_CODEC, dataComplex));
  }

  @Test(dataProvider = "codecData", dataProviderClass = CodecDataProviders.class)
  public void testBinaryEncoder(String testName, DataComplex dataComplex) throws Exception
  {
    assertEquals(actualEncode(dataComplex, true), TestUtil.dataComplexToBytes(KSON_BINARY_CODEC, dataComplex));
  }

  private byte[] actualEncode(DataComplex data, boolean encodeBinary) throws Exception
  {
    KSONDataEncoder
        encoder = data instanceof DataMap ? new KSONDataEncoder((DataMap) data, 3, encodeBinary)
        : new KSONDataEncoder((DataList) data, 3, encodeBinary);
    EntityStream<ByteString> entityStream = EntityStreams.newEntityStream(encoder);
    CollectingReader<ByteString, ?, ChunkedByteStringCollector.Result> reader =
        new CollectingReader<>(new ChunkedByteStringCollector());
    entityStream.setReader(reader);

    return reader.getResult().toCompletableFuture().get().data;
  }
}