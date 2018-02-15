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
import com.linkedin.r2.message.stream.entitystream.Writer;


/**
 * This class provides adapters between {@link com.linkedin.entitystream.EntityStream} of {@link ByteString} and
 * <code>ByteString</code>-specific {@link EntityStream}.
 */
public class EntityStreamAdapters
{
  /**
   * Adapts an EntityStream of ByteString to a ByteString-specific EntityStream.
   */
  public static EntityStream fromGenericEntityStream(com.linkedin.entitystream.EntityStream<ByteString> entityStream)
  {
    return new GenericToByteStringEntityStream(entityStream);
  }

  /**
   * Adapts a ByteString-specific EntityStream to an EntityStream of ByteString.
   */
  public static com.linkedin.entitystream.EntityStream<ByteString> toGenericEntityStream(EntityStream entityStream)
  {
    return new ByteStringToGenericEntityStream(entityStream);
  }

  /**
   * Adapts a Reader of ByteString to a ByteString-specific Reader.
   */
  public static Reader fromGenericReader(com.linkedin.entitystream.Reader<? super ByteString> reader)
  {
    return new GenericToByteStringReader(reader);
  }

  /**
   * Adapts a ByteString-specific Reader to a Reader of ByteString.
   */
  public static com.linkedin.entitystream.Reader<ByteString> toGenericReader(Reader reader)
  {
    return new ByteStringToGenericReader(reader);
  }

  /**
   * Adapts an Observer of ByteString to a ByteString-specific Observer.
   */
  public static Observer fromGenericObserver(com.linkedin.entitystream.Observer<? super ByteString> observer)
  {
    return new GenericToByteStringObserver(observer);
  }

  /**
   * Adapts a ByteString-specific Observer to an Observer of ByteString.
   */
  public static com.linkedin.entitystream.Observer<ByteString> toGenericObserver(Observer observer)
  {
    return new ByteStringToGenericObserver(observer);
  }

  /**
   * Adapts a Writer of ByteString to a ByteString-specific Writer.
   */
  public static Writer fromGenericWriter(com.linkedin.entitystream.Writer<ByteString> writer)
  {
    return new GenericToByteStringWriter(writer);
  }

  /**
   * Adapts a ByteString-specific Writer to a Writer of ByteString.
   */
  public static com.linkedin.entitystream.Writer<ByteString> toGenericWriter(Writer writer)
  {
    return new ByteStringToGenericWriter(writer);
  }
}
