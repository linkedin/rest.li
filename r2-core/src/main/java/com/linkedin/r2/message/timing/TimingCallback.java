/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.r2.message.timing;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.message.RequestContext;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * A thin wrapper around {@link Callback} that marks some {@link TimingKey}s before invoking the wrapped callback.
 *
 * @param <T> callback template type
 *
 * @author Evan Williams
 */
public class TimingCallback<T> implements Callback<T>
{
  private final Callback<T> _callback;
  private final RequestContext _requestContext;
  private final List<Node> _timingKeys;

  /**
   * Representation of a timing key "mark" action, which may be a "begin mark" or an "end mark".
   */
  private static class Node
  {
    Mode _mode;
    TimingKey _timingKey;

    enum Mode
    {
      BEGIN,
      END
    }

    Node(Mode mode, TimingKey timingKey)
    {
      _mode = mode;
      _timingKey = timingKey;
    }
  }

  /**
   * Use {@link TimingCallback.Builder} instead.
   */
  private TimingCallback()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Use {@link TimingCallback.Builder} instead.
   */
  private TimingCallback(Callback<T> callback, RequestContext requestContext, List<Node> timingKeys)
  {
    _callback = callback;
    _requestContext = requestContext;
    _timingKeys = timingKeys;
  }

  @Override
  public void onError(Throwable e) {
    markTimings();
    _callback.onError(e);
  }

  @Override
  public void onSuccess(T result) {
    markTimings();
    _callback.onSuccess(result);
  }

  /**
   * Marks all the timing keys included in this callback.
   */
  private void markTimings()
  {
    for (Node node : _timingKeys)
    {
      switch (node._mode)
      {
        case BEGIN:
          TimingContextUtil.beginTiming(_requestContext, node._timingKey);
          break;
        case END:
          TimingContextUtil.endTiming(_requestContext, node._timingKey);
          break;
      }
    }
  }

  /**
   * Builder for {@link TimingCallback}.
   *
   * @param <T> callback template type
   */
  public static class Builder<T>
  {
    private final Callback<T> _callback;
    private final RequestContext _requestContext;
    private List<Node> _timingKeys;

    public Builder(Callback<T> callback, RequestContext requestContext)
    {
      _callback = callback;
      _requestContext = requestContext;
      _timingKeys = new LinkedList<>();
    }

    /**
     * Adds a {@link TimingKey} to be marked using {@link TimingContextUtil#beginTiming(RequestContext, TimingKey)}
     * once this builder's callback is invoked. Note that keys will be marked in the same order they are added.
     * @param timingKey timing key
     */
    public Builder<T> addBeginTimingKey(TimingKey timingKey)
    {
      _timingKeys.add(new Node(Node.Mode.BEGIN, timingKey));
      return this;
    }

    /**
     * Adds a {@link TimingKey} to be marked using {@link TimingContextUtil#endTiming(RequestContext, TimingKey)}
     * once this builder's callback is invoked. Note that keys will be marked in the same order they are added.
     * @param timingKey timing key
     */
    public Builder<T> addEndTimingKey(TimingKey timingKey)
    {
      _timingKeys.add(new Node(Node.Mode.END, timingKey));
      return this;
    }

    /**
     * Builds the callback. If no timing keys were added or if all the timing keys added will be ignored, then this
     * builder will simply return the originally provided callback without wrapping it. Timing keys will only be ignored
     * if they are excluded by the {@link TimingImportance} threshold found in the {@link RequestContext}, if it exists.
     * @return a wrapped {@link TimingCallback} or the originally provided callback
     */
    public Callback<T> build()
    {
      if (_callback == null)
      {
        throw new IllegalStateException("Missing callback");
      }

      if (_requestContext == null)
      {
        throw new IllegalStateException("Missing request context");
      }

      TimingImportance timingImportanceThreshold = (TimingImportance) _requestContext
          .getLocalAttr(TimingContextUtil.TIMING_IMPORTANCE_THRESHOLD_KEY_NAME);

      // If a timing importance threshold is specified, filter out keys excluded by it
      if (timingImportanceThreshold != null)
      {
        _timingKeys = _timingKeys.stream()
            .filter(node -> TimingContextUtil.checkTimingImportanceThreshold(_requestContext, node._timingKey))
            .collect(Collectors.toList());
      }

      // If no timing keys remain after being filtered, simply return the originally provided callback
      if (_timingKeys.isEmpty())
      {
        return _callback;
      }

      return new TimingCallback<>(_callback, _requestContext, _timingKeys);
    }
  }
}
