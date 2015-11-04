/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.server.multiplexer;


import com.linkedin.data.template.StringMap;
import com.linkedin.parseq.BaseTask;
import com.linkedin.parseq.Context;
import com.linkedin.parseq.promise.Promise;
import com.linkedin.parseq.promise.Promises;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.multiplexer.IndividualRequest;
import com.linkedin.restli.internal.common.HeaderUtil;

import java.util.Map;


/**
 * A task responsible for inheriting common headers from an envelope request to an {@link com.linkedin.restli.common.multiplexer.IndividualRequest}
 *
 * This task will catch all known failures and fail the task with an {@link IndividualResponseException}. Any other unexpected exception
 * can also cause the task to fail.
 *
 * @author Gary Lin
 */
/* package private */ final class InheritEnvelopeRequestTask extends BaseTask<IndividualRequest>
{
  private final RestRequest _envelopeRequest;
  private final BaseTask<IndividualRequest> _individualRequest;

  /* package private */ InheritEnvelopeRequestTask(RestRequest envelopeRequest, BaseTask<IndividualRequest> individualRequest)
  {
    _envelopeRequest = envelopeRequest;
    _individualRequest = individualRequest;
  }

  @Override
  protected Promise<? extends IndividualRequest> run(Context context) throws Throwable
  {
    if (_individualRequest.isFailed())
    {
      return Promises.error(_individualRequest.getError());
    }

    IndividualRequest individualRequest = _individualRequest.get();
    inheritHeaders(individualRequest, _envelopeRequest);
    return Promises.value(individualRequest);
  }

  private static void inheritHeaders(IndividualRequest individualRequest, RestRequest envelopeRequest)
  {
    Map<String, String> envelopeHeaders = HeaderUtil.removeHeaders(envelopeRequest.getHeaders(), HeaderUtil.NONINHERITABLE_REQUEST_HEADERS);
    if (envelopeHeaders.size() > 0)
    {
      individualRequest.setHeaders(new StringMap(HeaderUtil.mergeHeaders(envelopeHeaders, individualRequest.getHeaders())));
    }
  }
}
