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

import com.linkedin.java.util.concurrent.Flow.Subscriber;
import com.linkedin.java.util.concurrent.Flow.Subscription;
import com.linkedin.entitystream.Observer;
import com.linkedin.entitystream.ReadHandle;
import com.linkedin.entitystream.Reader;

import java.util.Collections;
import java.util.List;


/**
 * A {@link Subscriber} that relays data from its subscription to
 * a {@link Reader} and optionally a list of {@link Observer}
 */
class ReaderSubscriber<T> implements Subscriber<T>
{
  private final Reader<? super T> _reader;
  private List<Observer<? super T>> _observers = Collections.emptyList();

  /**
   * Create an instance that relays data to a {@link Reader} and
   * a list of readonly {@link Observer}, which could be null or empty
   */
  ReaderSubscriber(Reader<? super T> reader, List<Observer<? super T>> observers)
  {
    _reader = reader;
    if (observers != null && observers.size() > 0)
    {
      _observers = Collections.unmodifiableList(observers);
    }
  }

  /**
   * Create an instance that relays data to a {@link Reader} only
   */
  ReaderSubscriber(Reader<T> reader)
  {
    this(reader, null);
  }

  @Override
  public void onSubscribe(Subscription subscription)
  {
    _reader.onInit(new ReadHandle()
    {
      @Override
      public void request(int n)
      {
        subscription.request(n);
      }

      @Override
      public void cancel()
      {
        subscription.cancel();
      }
    });
  }

  @Override
  public void onNext(T data)
  {
    for (Observer<? super T> observer : _observers)
    {
      observer.onDataAvailable(data);
    }
    _reader.onDataAvailable(data);
  }

  @Override
  public void onError(Throwable throwable)
  {
    for (Observer<? super T> observer : _observers)
    {
      observer.onError(throwable);
    }
    _reader.onError(throwable);
  }

  @Override
  public void onComplete()
  {
    for (Observer<? super T> observer : _observers)
    {
      observer.onDone();
    }
    _reader.onDone();
  }
}
