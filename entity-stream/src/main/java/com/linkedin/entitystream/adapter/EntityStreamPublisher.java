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
import com.linkedin.java.util.concurrent.Flow.Publisher;
import com.linkedin.java.util.concurrent.Flow.Subscriber;

import java.util.concurrent.atomic.AtomicBoolean;


/**
 * A {@link Publisher} implementation based on {@link EntityStream}, which can only be subscribed once.
 */
class EntityStreamPublisher<T> implements Publisher<T>
{
  private final EntityStream<T> _entityStream;
  private AtomicBoolean _subscribed = new AtomicBoolean(false);

  /**
   * Create an instance with an existing {@link EntityStream}, which must be initialized and
   * ready to be read.
   */
  EntityStreamPublisher(EntityStream<T> entityStream)
  {
    _entityStream = entityStream;
  }

  @Override
  public void subscribe(Subscriber<? super T> subscriber)
  {
    if (_subscribed.getAndSet(true))
    {
      throw new IllegalStateException("Only one subscriber is allowed for this publisher.");
    }

    _entityStream.setReader(new SubscriberReader<>(subscriber));
  }
}
