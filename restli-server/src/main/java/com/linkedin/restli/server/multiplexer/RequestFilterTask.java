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


import com.linkedin.parseq.BaseTask;
import com.linkedin.parseq.Context;
import com.linkedin.parseq.promise.Promise;
import com.linkedin.parseq.promise.Promises;
import com.linkedin.restli.common.multiplexer.IndividualRequest;
import com.linkedin.restli.server.RestLiServiceException;


/**
 * A task responsible for executing {@link MultiplexerSingletonFilter} on {@link com.linkedin.restli.common.multiplexer.IndividualRequest}
 *
 * This task will catch all known failures and fail the task with an {@link IndividualResponseException}. Any other unexpected exception
 * can also cause the task to fail.
 *
 * @author Gary Lin
 */
/* package private */ final class RequestFilterTask extends BaseTask<IndividualRequest>
{
  private final MultiplexerSingletonFilter _multiplexerSingletonFilter;
  private final BaseTask<IndividualRequest> _individualRequest;

  /* package private */ RequestFilterTask(MultiplexerSingletonFilter multiplexerSingletonFilter, BaseTask<IndividualRequest> individualRequest)
  {
    _multiplexerSingletonFilter = multiplexerSingletonFilter;
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
    if (_multiplexerSingletonFilter != null)
    {
      try
      {
        return Promises.value(_multiplexerSingletonFilter.filterIndividualRequest(individualRequest));
      }
      catch(RestLiServiceException e)
      {
        return Promises.error(new IndividualResponseException(e));
      }
      catch(Exception e)
      {
        return Promises.error(e);
      }
    }
    return Promises.value(individualRequest);
  }
}