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

package com.linkedin.r2.message.stream.entitystream.adapter;

import com.linkedin.data.ByteString;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.Observer;
import com.linkedin.r2.message.stream.entitystream.Reader;


/**
 * A ByteString-specific EntityStream adapted from an EntityStream of ByteString.
 */
class GenericToByteStringEntityStream implements EntityStream
{
  private final com.linkedin.entitystream.EntityStream<ByteString> _entityStream;

  GenericToByteStringEntityStream(com.linkedin.entitystream.EntityStream<ByteString> entityStream)
  {
    _entityStream = entityStream;
  }

  @Override
  public void addObserver(Observer o)
  {
    _entityStream.addObserver(EntityStreamAdapters.toGenericObserver(o));
  }

  @Override
  public void setReader(Reader r)
  {
    _entityStream.setReader(EntityStreamAdapters.toGenericReader(r));
  }
}
