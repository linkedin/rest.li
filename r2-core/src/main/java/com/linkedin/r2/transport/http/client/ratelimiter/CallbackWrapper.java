package com.linkedin.r2.transport.http.client.ratelimiter;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;


public interface CallbackWrapper {
  Callback<None> wrap(Callback<None> callback);
}
