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
import java.util.Map;

import static com.linkedin.r2.filter.TimedRestFilter.ON_ERROR_SUFFIX;
import static com.linkedin.r2.filter.TimedRestFilter.ON_REQUEST_SUFFIX;
import static com.linkedin.r2.filter.TimedRestFilter.ON_RESPONSE_SUFFIX;

/**
 * A decorated {@link StreamFilter} that marks the beginning of timing record of the filter.
 *
 * @author Xialin Zhu
 */
/* package private */ class TimedStreamFilter implements StreamFilter
{
  private final StreamFilter _streamFilter;
  private final TimingKey _onRequestTimingKey;
  private final TimingKey _onResponseTimingKey;
  private final TimingKey _onErrorTimingKey;

  public TimedStreamFilter(StreamFilter restFilter)
  {
    _streamFilter = restFilter;
    String timingKeyPrefix =  _streamFilter.getClass().getSimpleName() + "-" + hashCode() + "-";
    _onRequestTimingKey = TimingKey.registerNewKey(timingKeyPrefix + ON_REQUEST_SUFFIX);
    _onResponseTimingKey = TimingKey.registerNewKey(timingKeyPrefix + ON_RESPONSE_SUFFIX);
    _onErrorTimingKey = TimingKey.registerNewKey(timingKeyPrefix + ON_ERROR_SUFFIX);
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
}
