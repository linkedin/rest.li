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


import com.linkedin.r2.message.rest.RestException;
import com.linkedin.restli.common.multiplexer.IndividualRequest;
import com.linkedin.restli.common.multiplexer.IndividualRequestMap;
import com.linkedin.restli.common.multiplexer.IndividualResponse;


/**
 * Filter used by multiplexer to perform additional processing
 * on each IndividualRequest and IndividualResponse.
 *
 * @author Gary Lin
 */
public interface MultiplexerSingletonFilter
{
  /**
   * This method is called with all the individual requests after multiplexer extracts them from the multiplexed payload.
   *
   * @param individualRequests All of the individual requests.
   * @return filtered requests.
   * @throws RestException if the individual requests cannot be handled together.
   */
  default IndividualRequestMap filterRequests(IndividualRequestMap individualRequests) throws RestException
  {
    return individualRequests;
  }
  /**
   * This method is called after multiplexer extracts each of the IndividualRequest(s) from the multiplexed request payload.
   * The returned IndividualRequest object is used to constructed the synthetic request, which will then be sent to the
   * subsequent Restli pipeline.
   * @param request original IndividualRequest. Depending on the implementation, filter may modify attributes on this parameter directly.
   * @return filtered IndividualRequest. The returned IndividualRequest will be the reference to the request parameter.
   */
  IndividualRequest filterIndividualRequest(IndividualRequest request);

  /**
   * This method is called on each IndividualResponse before constructing the final multiplexed response payload.
   * The IndividualResponse returned by this function will be inserted into the final multiplexed response payload and
   * returned back to the client.
   * @param response original IndividualResponse. Depending on the implementation, filter may modify attributes on this parameter directly.
   * @return filtered IndividualResponse. The returned IndividualResponse will be the reference to the response parameter.
   */
  IndividualResponse filterIndividualResponse(IndividualResponse response);
}
