/*
   Copyright (c) 2016 LinkedIn Corp.

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

/**
 * $Id: $
 */

package com.linkedin.r2.transport.http.client;

/**
 * Provides a handle for each {@link AsyncPool} object to be returned or disposed
 * back to the pool they were created.
 */
public interface AsyncPoolHandle<T>
{
  /**
   * Releases the handle and {@code AsyncPool#put} the object back to the pool
   */
  void release();

  /**
   * Releases the handle and {@code AsyncPool#dispose} the object back the pool
   */
  void dispose();

  /**
   * Gets the reference to the {@link AsyncPool} where the object was originally created
   * @return Reference to the async pool
   */
  AsyncPool<T> pool();
}
