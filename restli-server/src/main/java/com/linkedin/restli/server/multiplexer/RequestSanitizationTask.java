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
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.multiplexer.IndividualRequest;

import java.util.Map;
import java.util.Set;


/**
 * A task responsible for sanitizing the request (for example: check request headers passed in the individual requests that are whitelisted)
 * The task can fail if the individual request contains request headers that not in the whitelist.
 *
 * @author Gary Lin
 */
/* package private */ final class RequestSanitizationTask extends BaseTask<IndividualRequest>
{
  private final Set<String> _individualRequestHeaderWhitelist;
  private final IndividualRequest _individualRequest;

  /* package private */ RequestSanitizationTask(IndividualRequest individualRequest, Set<String> individualRequestHeaderWhitelist)
  {
    _individualRequestHeaderWhitelist = individualRequestHeaderWhitelist;
    _individualRequest = individualRequest;
  }

  @Override
  protected Promise<? extends IndividualRequest> run(Context context) throws Throwable
  {
    if (_individualRequest.hasHeaders() && _individualRequest.getHeaders().size() > 0)
    {
      for (Map.Entry<String, String> headerEntry : _individualRequest.getHeaders().entrySet())
      {
        String headerName = headerEntry.getKey();
        if (!_individualRequestHeaderWhitelist.contains(headerName))
        {
          return Promises.error(new IndividualResponseException(HttpStatus.S_400_BAD_REQUEST, String.format("Request header %s is not allowed in the individual request.", headerName)));
        }
      }
    }
    return Promises.value(_individualRequest);
  }
}