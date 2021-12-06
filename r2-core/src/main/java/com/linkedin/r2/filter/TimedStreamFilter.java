/*
   Copyright (c) 2012 LinkedIn Corp.

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

package com.linkedin.r2.filter;

import com.linkedin.r2.filter.message.stream.StreamFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.timing.TimingContextUtil;
import com.linkedin.r2.message.timing.TimingKey;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.timing.TimingImportance;
import java.util.Map;

import static com.linkedin.r2.filter.TimedRestFilter.ON_ERROR_SUFFIX;
import static com.linkedin.r2.filter.TimedRestFilter.ON_REQUEST_SUFFIX;
import static com.linkedin.r2.filter.TimedRestFilter.ON_RESPONSE_SUFFIX;

/**
 * A decorated {@link StreamFilter} that marks the beginning of timing record of the filter.
 *
 * @author Xialin Zhu
 */
public class TimedStreamFilter implements StreamFilter
{
  private final StreamFilter _streamFilter;
  private final TimingKey _onRequestTimingKey;
  private final TimingKey _onResponseTimingKey;
  private final TimingKey _onErrorTimingKey;
  private boolean _shared;

  /**
   * Registers {@link TimingKey}s for {@link com.linkedin.r2.message.timing.TimingNameConstants#TIMED_STREAM_FILTER}.
   *
   * @param streamFilter Stream filter to decorate
   */
  public TimedStreamFilter(StreamFilter streamFilter)
  {
    _streamFilter = streamFilter;

    String filterClassName = _streamFilter.getClass().getSimpleName();
    String timingKeyPrefix = filterClassName + "-";
    String timingKeyPostfix = ":";

    _onRequestTimingKey = TimingKey.registerNewKey(TimingKey.getUniqueName(timingKeyPrefix + ON_REQUEST_SUFFIX + timingKeyPostfix),
        filterClassName, TimingImportance.LOW);
    _onResponseTimingKey = TimingKey.registerNewKey(TimingKey.getUniqueName(timingKeyPrefix + ON_RESPONSE_SUFFIX + timingKeyPostfix),
        filterClassName, TimingImportance.LOW);
    _onErrorTimingKey = TimingKey.registerNewKey(TimingKey.getUniqueName(timingKeyPrefix + ON_ERROR_SUFFIX + timingKeyPostfix),
        filterClassName, TimingImportance.LOW);
    _shared = false;
  }

  @Override
  public void onStreamRequest(StreamRequest req,
    RequestContext requestContext,
    Map<String, String> wireAttrs,
    NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    TimingContextUtil.markTiming(requestContext, _onRequestTimingKey);
    _streamFilter.onStreamRequest(req, requestContext, wireAttrs, new TimedNextFilter<>(_onRequestTimingKey, nextFilter));
  }

  @Override
  public void onStreamResponse(StreamResponse res,
    RequestContext requestContext,
    Map<String, String> wireAttrs,
    NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    TimingContextUtil.markTiming(requestContext, _onResponseTimingKey);
    _streamFilter.onStreamResponse(res, requestContext, wireAttrs, new TimedNextFilter<>(_onResponseTimingKey, nextFilter));
  }

  @Override
  public void onStreamError(Throwable ex,
    RequestContext requestContext,
    Map<String, String> wireAttrs,
    NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    TimingContextUtil.markTiming(requestContext, _onErrorTimingKey);
    _streamFilter.onStreamError(ex, requestContext, wireAttrs, new TimedNextFilter<>(_onErrorTimingKey, nextFilter));
  }

  public void setShared() {
    _shared = true;
  }

  public void onShutdown() {
    if (!_shared) {
      TimingKey.unregisterKey(_onErrorTimingKey);
      TimingKey.unregisterKey(_onRequestTimingKey);
      TimingKey.unregisterKey(_onResponseTimingKey);
    }
  }
}
