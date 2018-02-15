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

package com.linkedin.entitystream.adapter;

import com.linkedin.entitystream.EntityStream;
import com.linkedin.entitystream.Observer;
import com.linkedin.java.util.concurrent.Flow.Publisher;
import com.linkedin.entitystream.Reader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * An {@link EntityStream} that bases on a {@link Publisher}.
 */
class PublisherEntityStream<T> implements EntityStream<T>
{
  private final Publisher<T> _publisher;
  private final List<Observer<? super T>> _observers = new ArrayList<>();
  private final AtomicBoolean hasReader = new AtomicBoolean(false);

  PublisherEntityStream(Publisher<T> publisher)
  {
    _publisher = publisher;
  }

  @Override
  public void addObserver(Observer<? super T> observer)
  {
    if (hasReader.get())
    {
      throw new IllegalStateException("Reader of this entity stream has already been set. No more observer is allowed.");
    }

    _observers.add(observer);
  }

  @Override
  public void setReader(Reader<? super T> reader)
  {
    if (hasReader.getAndSet(true))
    {
      throw new IllegalStateException("Reader of this entity stream has already been set");
    }

    _publisher.subscribe(new ReaderSubscriber<T>(reader, _observers));
  }
}
