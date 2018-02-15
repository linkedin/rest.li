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

package com.linkedin.entitystream;

/**
 * An object that represents the reactive stream of entities.
 *
 * Each EntityStream can have one {@link Writer}, multiple {@link Observer}s and
 * exactly one {@link Reader}. The data flow of a stream is Reader
 * driven: that is, if a Reader doesn't request data, there is no data flow.
 * The EntityStream is responsible to pass the data request from the Reader to the Writer and to pass the data
 * from the Writer to the Reader and Observers.
 */
public interface EntityStream<T>
{
  /**
   * Add observer to this stream.
   *
   * @param o the Observer
   * @throws IllegalStateException if entity stream already has a reader set
   */
  void addObserver(Observer<? super T> o);

  /**
   * Set reader for this stream.
   *
   * @param r the Reader of this stream
   * @throws IllegalStateException if there is already a reader
   */
  void setReader(Reader<? super T> r);
}
