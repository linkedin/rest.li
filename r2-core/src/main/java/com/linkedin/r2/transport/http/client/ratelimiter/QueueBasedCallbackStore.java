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
import com.linkedin.util.ArgumentUtil;
import java.util.NoSuchElementException;
import java.util.Queue;


/**
 * A simple CallbackStore implementation that delegates to the provided Queue
 */
public class QueueBasedCallbackStore implements CallbackStore
{
  private final Queue<Callback<None>> _queue;

  public QueueBasedCallbackStore(Queue<Callback<None>> queue)
  {
    ArgumentUtil.ensureNotNull(queue, "queue cannot be null");
    _queue = queue;
  }

  public void put(Callback<None> callback)
  {
    _queue.offer(callback);
  }

  public Callback<None> get() throws NoSuchElementException
  {
    return _queue.remove();
  }
}
