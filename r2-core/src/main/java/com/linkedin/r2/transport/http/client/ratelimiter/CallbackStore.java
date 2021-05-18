/*
   Copyright (c) 2021 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.client.ratelimiter;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import java.util.NoSuchElementException;


/**
 * A lightweight queue-like interface specifically for Callbacks
 */
public interface CallbackStore {

  /**
   * Buffers a Callback for later retrieval.
   * @param callback
   */
  void put(Callback<None> callback);

  /**
   * Provides a Callback previously stored through the put method.
   * This interface makes no recommendation of ordering between put and get calls.
   * @return Callback
   * @throws NoSuchElementException if the CallbackStore is empty
   */
  Callback<None> get() throws NoSuchElementException;
}
