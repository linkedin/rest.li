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

import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.timing.TimingContextUtil;
import com.linkedin.r2.message.timing.TimingKey;
import java.util.Map;


/**
 * A decorated {@link NextFilter} that marks the end of timing record of the previous filter.
 *
 * @author Xialin Zhu
 */
/* package private */ class TimedNextFilter<REQ extends Request, RES extends Response> implements NextFilter<REQ, RES>
{
  private final TimingKey _timingKey;

  private final NextFilter<REQ, RES> _nextFilter;

  public TimedNextFilter(TimingKey timingKey, NextFilter<REQ, RES> nextFilter)
  {
    _timingKey = timingKey;
    _nextFilter = nextFilter;
  }

  @Override
  public void onError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs)
  {
    TimingContextUtil.markTiming(requestContext, _timingKey);
    _nextFilter.onError(ex, requestContext, wireAttrs);
  }

  @Override
  public void onRequest(REQ req, RequestContext requestContext, Map<String, String> wireAttrs)
  {
    TimingContextUtil.markTiming(requestContext, _timingKey);
    _nextFilter.onRequest(req, requestContext, wireAttrs);
  }

  @Override
  public void onResponse(RES res, RequestContext requestContext, Map<String, String> wireAttrs)
  {
    TimingContextUtil.markTiming(requestContext, _timingKey);
    _nextFilter.onResponse(res, requestContext, wireAttrs);
  }
}
