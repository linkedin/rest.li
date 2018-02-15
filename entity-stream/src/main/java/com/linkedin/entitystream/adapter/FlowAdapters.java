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

package com.linkedin.entitystream.adapter;


import com.linkedin.entitystream.EntityStream;
import com.linkedin.entitystream.Writer;
import com.linkedin.java.util.concurrent.Flow.Publisher;
import com.linkedin.java.util.concurrent.Flow.Subscriber;
import com.linkedin.entitystream.EntityStreams;
import com.linkedin.entitystream.Reader;


/**
 * A utility to bridge between EntityStream and Java 9's Flow reactive streaming interface.
 */
public class FlowAdapters
{
  public static <T> EntityStream<T> fromPublisher(Publisher<T> publisher)
  {
    return new PublisherEntityStream<>(publisher);
  }

  public static <T> Reader<T> fromSubscriber(Subscriber<T> subscriber)
  {
    return new SubscriberReader<>(subscriber);
  }

  public static <T> Subscriber<T> toSubscriber(Reader<T> reader)
  {
    return new ReaderSubscriber<>(reader);
  }

  public static <T> Publisher<T> toPublisher(EntityStream<T> entityStream)
  {
    return new EntityStreamPublisher<>(entityStream);
  }

  public static <T> Publisher<T> toPublisher(Writer<T> writer)
  {
    return new EntityStreamPublisher<>(EntityStreams.newEntityStream(writer));
  }
}
