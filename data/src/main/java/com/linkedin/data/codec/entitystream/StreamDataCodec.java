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
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.entitystream.EntityStream;

import java.util.concurrent.CompletionStage;


/**
 * An interface for decoding and encoding {@link DataMap} and {@link DataList} from and to an {@link EntityStream}
 * of ByteString.
 *
 * @author Xiao Ma
 */
public interface StreamDataCodec
{
  /**
   * Decodes a <code>DataMap</code> from the <code>EntityStream</code>. The result is passed asynchronously in the
   * {@link CompletionStage}.
   */
  CompletionStage<DataMap> decodeMap(EntityStream<ByteString> entityStream);

  /**
   * Decodes a <code>DataList</code> from the <code>EntityStream</code>. The result is passed asynchronously in the
   * {@link CompletionStage}.
   */
  CompletionStage<DataList> decodeList(EntityStream<ByteString> entityStream);

  /**
   * Encodes a <code>DataMap</code> to an <code>EntityStream</code>.
   */
  EntityStream<ByteString> encodeMap(DataMap map);

  /**
   * Encodes a <code>DataList</code> to an <code>EntityStream</code>.
   */
  EntityStream<ByteString> encodeList(DataList list);
}
