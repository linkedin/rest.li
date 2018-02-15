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
 * Observer passively observes the data flow of an {@link EntityStream}
 * i.e. observer cannot drive the flow of data in the EntityStream.
 *
 * It is expected that the methods {@link #onDataAvailable}, {@link #onDone} and {@link #onError} are invoked in a
 * thread-safe manner.
 *
 * @author Zhenkai Zhu
 */
public interface Observer<T>
{
  /**
   * This is called when a new chunk of data is written to the stream by the writer.
   * @param data data written by the writer
   */
  void onDataAvailable(T data);

  /**
   * This is called when the writer finished writing.
   */
  void onDone();

  /**
   * This is called when an error has happened.
   *
   * @param e the cause of the error. If the error is caused by Reader cancelling
   *          reading, this Throwable should be an {@link AbortedException}.
   */
  void onError(Throwable e);
}
