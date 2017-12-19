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
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.Reader;
import com.linkedin.r2.message.stream.entitystream.Writer;


/**
 * Util to bridge between R2's EntityStream and Java 9's Flow reactive streaming interface.
 */
public class FlowBridge
{
  public static EntityStream fromPublisher(Publisher<ByteString> publisher)
  {
    return new PublisherEntityStream(publisher);
  }

  public static Reader fromSubscriber(Subscriber<ByteString> subscriber)
  {
    return new SubscriberReader(subscriber);
  }

  public static Subscriber<ByteString> toSubscriber(Reader reader)
  {
    return new ReaderSubscriber(reader);
  }

  public static Publisher<ByteString> toPublisher(EntityStream entityStream)
  {
    return new EntityStreamPublisher(entityStream);
  }

  public static Publisher<ByteString> toPublisher(Writer writer)
  {
    return new EntityStreamPublisher(EntityStreams.newEntityStream(writer));
  }
}
