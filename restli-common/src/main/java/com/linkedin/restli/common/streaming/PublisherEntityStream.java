/*
 * Copyright (c) 2017 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.linkedin.restli.common.streaming;

import com.linkedin.data.ByteString;
import com.linkedin.java.util.concurrent.Flow.Publisher;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.Observer;
import com.linkedin.r2.message.stream.entitystream.Reader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * An {@link EntityStream} that bases on a {@link Publisher}
 */
class PublisherEntityStream implements EntityStream
{
  private final Publisher<ByteString> _publisher;
  private final List<Observer> _observers = new ArrayList<>();
  private final AtomicBoolean hasReader = new AtomicBoolean(false);

  PublisherEntityStream(Publisher<ByteString> publisher)
  {
    _publisher = publisher;
  }

  @Override
  public void addObserver(Observer o)
  {
    if (hasReader.get())
    {
      throw new IllegalStateException("Reader of this entity stream has already been set. No more observer is allowed.");
    }
    _observers.add(o);
  }

  @Override
  public void setReader(Reader reader)
  {
    if (reader == null)
    {
      throw new NullPointerException("Reader cannot be null");
    }
    if (hasReader.getAndSet(true))
    {
      throw new IllegalStateException("Reader of this entity stream has already been set");
    }
    _publisher.subscribe(new ReaderSubscriber(reader, _observers));
  }
}
