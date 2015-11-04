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
import com.linkedin.restli.common.multiplexer.IndividualResponse;
import com.linkedin.restli.server.RestLiServiceException;


/**
 * A task responsible for executing {@link MultiplexerSingletonFilter} on {@link com.linkedin.restli.common.multiplexer.IndividualResponse}
 *
 * This task will never fail
 *
 * @author Gary Lin
 */
/* package private */ final class ResponseFilterTask extends BaseTask<IndividualResponseWithCookies>
{
  private final MultiplexerSingletonFilter _multiplexerSingletonFilter;
  private final BaseTask<IndividualResponseWithCookies> _individualResponseWithCookies;

  /* package private */ ResponseFilterTask(MultiplexerSingletonFilter multiplexerSingletonFilter, BaseTask<IndividualResponseWithCookies> individualResponseWithCookies)
  {
    _multiplexerSingletonFilter = multiplexerSingletonFilter;
    _individualResponseWithCookies = individualResponseWithCookies;
  }

  @Override
  protected Promise<? extends IndividualResponseWithCookies> run(Context context) throws Throwable
  {
    IndividualResponseWithCookies individualResponseWithCookies = _individualResponseWithCookies.get();
    if (_multiplexerSingletonFilter != null)
    {
      try
      {
        IndividualResponse filteredResponse = _multiplexerSingletonFilter.filterIndividualResponse(individualResponseWithCookies.getIndividualResponse());
        return Promises.value(new IndividualResponseWithCookies(filteredResponse, individualResponseWithCookies.getCookies()));
      }
      catch(RestLiServiceException e)
      {
        return Promises.value(new IndividualResponseWithCookies(IndividualResponseException.createErrorIndividualResponse(e)));
      }
      catch(Exception e)
      {
        return Promises.value(new IndividualResponseWithCookies(IndividualResponseException.createInternalServerErrorIndividualResponse(e)));
      }
    }
    return Promises.value(individualResponseWithCookies);
  }
}