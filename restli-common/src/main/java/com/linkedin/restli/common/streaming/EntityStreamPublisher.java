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
import com.linkedin.java.util.concurrent.Flow.Subscriber;
import com.linkedin.r2.message.stream.entitystream.EntityStream;

import java.util.concurrent.atomic.AtomicBoolean;


/**
 * A publisher implementation that bases on {@link EntityStream}, which can only be subscribed by
 * one subscriber. Invoking {@link #subscribe(Subscriber)} more than once raise error.
 */
class EntityStreamPublisher implements Publisher<ByteString>
{
  private final EntityStream _entityStream;
  private AtomicBoolean _subscribed = new AtomicBoolean(false);

  /**
   * Create an instance with an existing {@link EntityStream}, which must be initialized and
   * ready to be read.
   */
  public EntityStreamPublisher(EntityStream entityStream)
  {
    _entityStream = entityStream;
  }

  @Override
  public void subscribe(Subscriber<? super ByteString> subscriber)
  {
    if (subscriber == null)
    {
      throw new NullPointerException("Subscriber cannot be null.");
    }

    if (_subscribed.getAndSet(true))
    {
      throw new IllegalStateException("Only one subscriber is allowed for this publisher.");
    }

    _entityStream.setReader(new SubscriberReader(subscriber));
  }
}
