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
import com.linkedin.data.ByteString;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.entitystream.EntityStream;
import com.linkedin.entitystream.EntityStreams;

import java.util.concurrent.CompletionStage;


/**
 * An {@link StreamDataCodec} for JSON backed by Jackson's JSON parser and generator.
 *
 * @author Xiao Ma
 */
public class JacksonStreamDataCodec implements StreamDataCodec
{
  /**
   * Default factory to be shared between encoder and decoder. This is done to maximize factory reuse for
   * performance reasons as recommended by Jackson authors.
   *
   * <a href="https://github.com/FasterXML/jackson-docs/wiki/Presentation:-Jackson-Performance">Jackson Performance</a>
   */
  static final JsonFactory JSON_FACTORY = new JsonFactory();

  protected final int _bufferSize;

  public JacksonStreamDataCodec(int bufferSize)
  {
    _bufferSize = bufferSize;
  }

  @Override
  public CompletionStage<DataMap> decodeMap(EntityStream<ByteString> entityStream)
  {
    JacksonJsonDataMapDecoder decoder = new JacksonJsonDataMapDecoder();
    entityStream.setReader(decoder);
    return decoder.getResult();
  }

  @Override
  public CompletionStage<DataList> decodeList(EntityStream<ByteString> entityStream)
  {
    JacksonJsonDataListDecoder decoder = new JacksonJsonDataListDecoder();
    entityStream.setReader(decoder);
    return decoder.getResult();
  }

  @Override
  public EntityStream<ByteString> encodeMap(DataMap map)
  {
    JacksonJsonDataEncoder encoder = new JacksonJsonDataEncoder(map, _bufferSize);
    return EntityStreams.newEntityStream(encoder);
  }

  @Override
  public EntityStream<ByteString> encodeList(DataList list)
  {
    JacksonJsonDataEncoder encoder = new JacksonJsonDataEncoder(list, _bufferSize);
    return EntityStreams.newEntityStream(encoder);
  }
}
