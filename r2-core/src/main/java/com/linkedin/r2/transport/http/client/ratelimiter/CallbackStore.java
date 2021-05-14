package com.linkedin.r2.transport.http.client.ratelimiter;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;


public interface CallbackStore {
  void put(Callback<None> callback);

  Callback<None> get() throws NoSuchElementException;
}
