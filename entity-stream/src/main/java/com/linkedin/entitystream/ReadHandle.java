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
 * This is the handle used by the {@link Reader} to request data from an {@link EntityStream}.
 *
 * The Reader should invoke the methods {@link #request(int)} and {@link #cancel()} in a thread-safe manner.
 *
 * @author Zhenkai Zhu
 */
public interface ReadHandle
{
  /**
   * This method signals the writer of the EntityStream that it can write more data.
   *
   * @param n the additional number of data chunks that the writer is permitted to write
   * @throws IllegalArgumentException if n is not positive
   */
  void request(int n);

  /**
   * This method cancels the stream.
   */
  void cancel();
}
