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
 * This is the handle for the {@link Writer} to write data to an {@link EntityStream}.
 *
 * The Writer should invoke the methods {@link #remaining}, {@link #write}, {@link #done} and {@link #error} in a
 * thread-safe manner.
 *
 * @author Zhenkai Zhu
 */
public interface WriteHandle<T>
{
  /**
   * This writes data into the EntityStream.
   * This call may have no effect if the stream has been aborted
   * @param data the data chunk to be written
   * @throws IllegalStateException if remaining capacity is 0, or done() or error() has been called
   * @throws IllegalStateException if called after done() or error() has been called
   */
  void write(final T data);

  /**
   * Signals that Writer has finished writing.
   * This call has no effect if the stream has been aborted or done() or error() has been called
   */
  void done();

  /**
   * Signals that the Writer has encountered an error.
   * This call has no effect if the stream has been aborted or done() or error() has been called
   * @param throwable the cause of the error.
   */
  void error(final Throwable throwable);

  /**
   * Returns the remaining capacity in number of data chunks
   *
   * Always returns 0 if the stream is aborted or finished with done() or error()
   *
   * @return the remaining capacity in number of data chunks
   */
  int remaining();
}
