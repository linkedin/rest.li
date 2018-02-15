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
import com.linkedin.entitystream.Observer;
import com.linkedin.entitystream.Reader;
import com.linkedin.r2.message.stream.entitystream.EntityStream;


/**
 * An EntityStream of ByteString adapted from ByteString-specific EntityStream.
 */
class ByteStringToGenericEntityStream implements com.linkedin.entitystream.EntityStream<ByteString>
{
  private final EntityStream _entityStream;

  ByteStringToGenericEntityStream(EntityStream entityStream)
  {
    _entityStream = entityStream;
  }

  @Override
  public void addObserver(Observer<? super ByteString> o)
  {
    _entityStream.addObserver(EntityStreamAdapters.fromGenericObserver(o));
  }

  @Override
  public void setReader(Reader<? super ByteString> r)
  {
    _entityStream.setReader(EntityStreamAdapters.fromGenericReader(r));
  }
}
