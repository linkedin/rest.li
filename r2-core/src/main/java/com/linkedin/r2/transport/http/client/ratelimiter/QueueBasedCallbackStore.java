package com.linkedin.r2.transport.http.client.ratelimiter;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import java.util.Queue;


public class QueueBasedCallbackStore implements CallbackStore {
  private final Queue<Callback<None>> _queue;

  public QueueBasedCallbackStore(Queue<Callback<None>> queue) {
    _queue = queue;
  }

  public void put(Callback<None> callback)
  {
    _queue.offer(callback);
  }

  public Callback<None> get()
  {
    return _queue.poll();
  }
}
