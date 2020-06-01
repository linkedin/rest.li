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
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.ProtobufCodecOptions;
import com.linkedin.entitystream.EntityStream;
import com.linkedin.entitystream.EntityStreams;
import java.util.concurrent.CompletionStage;


/**
 * An {@link StreamDataCodec} for Protocol Buffers backed by Non-blocking protobuf parser and generator.
 *
 * @author amgupta1
 */
public class ProtobufStreamDataCodec implements StreamDataCodec
{
  protected final int _bufferSize;
  protected final ProtobufCodecOptions _options;

  public ProtobufStreamDataCodec(int bufferSize)
  {
    this(bufferSize, new ProtobufCodecOptions.Builder().setEnableASCIIOnlyStrings(true).build());
  }

  public ProtobufStreamDataCodec(int bufferSize, ProtobufCodecOptions options)
  {
    _bufferSize = bufferSize;
    _options = options;
  }

  @Override
  public CompletionStage<DataMap> decodeMap(EntityStream<ByteString> entityStream)
  {
    ProtobufDataDecoder<DataMap> decoder =
        new ProtobufDataDecoder<>(_options.getSymbolTable(), AbstractDataDecoder.START_OBJECT_TOKEN);
    entityStream.setReader(decoder);
    return decoder.getResult();
  }

  @Override
  public CompletionStage<DataList> decodeList(EntityStream<ByteString> entityStream)
  {
    ProtobufDataDecoder<DataList> decoder =
        new ProtobufDataDecoder<>(_options.getSymbolTable(), AbstractDataDecoder.START_ARRAY_TOKEN);
    entityStream.setReader(decoder);
    return decoder.getResult();
  }

  @Override
  public EntityStream<ByteString> encodeMap(DataMap map)
  {
    return EntityStreams.newEntityStream(new ProtobufDataEncoder(map, _bufferSize, _options));
  }

  @Override
  public EntityStream<ByteString> encodeList(DataList list)
  {
    return EntityStreams.newEntityStream(new ProtobufDataEncoder(list, _bufferSize, _options));
  }
}
