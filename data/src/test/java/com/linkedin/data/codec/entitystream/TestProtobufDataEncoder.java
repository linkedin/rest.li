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
import com.linkedin.data.ChunkedByteStringCollector;
import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import com.linkedin.data.codec.CodecDataProviders;
import com.linkedin.data.codec.ProtobufCodecOptions;
import com.linkedin.data.codec.ProtobufDataCodec;
import com.linkedin.entitystream.CollectingReader;
import com.linkedin.entitystream.EntityStream;
import com.linkedin.entitystream.EntityStreams;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


public class TestProtobufDataEncoder
{
  private static final ProtobufDataCodec CODEC = new ProtobufDataCodec(new ProtobufCodecOptions.Builder().build());

  @Test(dataProvider = "codecData", dataProviderClass = CodecDataProviders.class)
  public void testTextEncoder(String testName, DataComplex dataComplex) throws Exception

  {
    assertEquals(actualEncode(dataComplex), TestUtil.dataComplexToBytes(CODEC, dataComplex));
  }

  private byte[] actualEncode(DataComplex data) throws Exception
  {
    ProtobufDataEncoder
        encoder = data instanceof DataMap ? new ProtobufDataEncoder((DataMap) data, 3)
        : new ProtobufDataEncoder((DataList) data, 3);
    EntityStream<ByteString> entityStream = EntityStreams.newEntityStream(encoder);
    CollectingReader<ByteString, ?, ChunkedByteStringCollector.Result> reader =
        new CollectingReader<>(new ChunkedByteStringCollector());
    entityStream.setReader(reader);

    return reader.getResult().toCompletableFuture().get().data;
  }
}