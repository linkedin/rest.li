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
import com.linkedin.data.codec.DataCodec;
import com.linkedin.data.codec.JacksonLICORDataCodec;
import com.linkedin.entitystream.CollectingReader;
import com.linkedin.entitystream.EntityStream;
import com.linkedin.entitystream.EntityStreams;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class TestJacksonLICORDataEncoder
{
  private static final JacksonLICORDataCodec TEXT_CODEC = new JacksonLICORDataCodec(false);
  private static final JacksonLICORDataCodec BINARY_CODEC = new JacksonLICORDataCodec(true);

  @Test(dataProvider = "LICORCodecData", dataProviderClass = CodecDataProviders.class)
  public void testTextEncoder(String testName, DataComplex dataComplex, boolean useBinary) throws Exception

  {
    DataCodec codec = useBinary ? BINARY_CODEC : TEXT_CODEC;
    assertEquals(actualEncode(dataComplex, useBinary), TestUtil.dataComplexToBytes(codec, dataComplex));
  }

  private byte[] actualEncode(DataComplex data, boolean encodeBinary) throws Exception
  {
    JacksonLICORDataEncoder
        encoder = data instanceof DataMap ? new JacksonLICORDataEncoder((DataMap) data, 3, encodeBinary)
        : new JacksonLICORDataEncoder((DataList) data, 3, encodeBinary);
    EntityStream<ByteString> entityStream = EntityStreams.newEntityStream(encoder);
    CollectingReader<ByteString, ?, ChunkedByteStringCollector.Result> reader =
        new CollectingReader<>(new ChunkedByteStringCollector());
    entityStream.setReader(reader);

    return reader.getResult().toCompletableFuture().get().data;
  }
}