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

import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator;
import com.linkedin.data.ByteString;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.entitystream.EntityStream;
import com.linkedin.entitystream.EntityStreams;
import java.util.concurrent.CompletionStage;


/**
 * An {@link StreamDataCodec} for SMILE backed by Jackson's non blocking SMILE parser and generator.
 *
 * @author kramgopa
 */
public class JacksonSmileStreamDataCodec implements StreamDataCodec
{
  private final int _bufferSize;
  protected final SmileFactory _smileFactory;

  public JacksonSmileStreamDataCodec(int bufferSize)
  {
    this(new SmileFactory(), bufferSize);

    // Enable name and string sharing by default.
    _smileFactory.enable(SmileGenerator.Feature.CHECK_SHARED_NAMES);
    _smileFactory.enable(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES);
  }

  @SuppressWarnings("deprecation")
  public JacksonSmileStreamDataCodec(SmileFactory smileFactory, int bufferSize)
  {
    _smileFactory = smileFactory;
    _bufferSize = bufferSize;

    // String interning is disabled by default since it causes GC issues. Note that we are using the deprecated disable
    // method instead of JsonFactoryBuilder here preserves compatibility with some runtimes that pin jackson to a
    // lower 2.x version. The method should still be available throughout jackson-core 2.x
    _smileFactory.disable(SmileFactory.Feature.INTERN_FIELD_NAMES);
  }

  @Override
  public CompletionStage<DataMap> decodeMap(EntityStream<ByteString> entityStream)
  {
    JacksonSmileDataDecoder<DataMap> decoder =
        new JacksonSmileDataDecoder<>(_smileFactory, AbstractDataDecoder.START_OBJECT_TOKEN);
    entityStream.setReader(decoder);
    return decoder.getResult();
  }

  @Override
  public CompletionStage<DataList> decodeList(EntityStream<ByteString> entityStream)
  {
    JacksonSmileDataDecoder<DataList> decoder =
        new JacksonSmileDataDecoder<>(_smileFactory, AbstractDataDecoder.START_ARRAY_TOKEN);
    entityStream.setReader(decoder);
    return decoder.getResult();
  }

  @Override
  public EntityStream<ByteString> encodeMap(DataMap map)
  {
    return EntityStreams.newEntityStream(new JacksonSmileDataEncoder(_smileFactory, map, _bufferSize));
  }

  @Override
  public EntityStream<ByteString> encodeList(DataList list)
  {
    return EntityStreams.newEntityStream(new JacksonSmileDataEncoder(_smileFactory, list, _bufferSize));
  }
}
