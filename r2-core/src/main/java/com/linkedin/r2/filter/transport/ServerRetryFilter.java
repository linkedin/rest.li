/*
   Copyright (c) 2016 LinkedIn Corp.

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

/* $Id$ */
package com.linkedin.r2.filter.transport;

import com.linkedin.r2.RetriableRequestException;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.filter.message.stream.StreamFilter;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Filter implementation that processes a retrieable response. Our contracts requires user to throw
 * {@link RetriableRequestException} when they want to request a retry. This filter catches that exception
 * and converts it to a wire attributes that will be sent back to the client side.
 *
 * @author Xialin Zhu
 * @see ClientRetryFilter
 */
public class ServerRetryFilter implements RestFilter, StreamFilter
{
  private static final Logger LOG = LoggerFactory.getLogger(ServerRetryFilter.class);

  @Override
  public void onRestError(Throwable ex,
      RequestContext requestContext,
      Map<String, String> wireAttrs,
      NextFilter<RestRequest, RestResponse> nextFilter)
  {
    processError(ex, requestContext, wireAttrs, nextFilter);
  }

  @Override
  public void onStreamError(Throwable ex,
      RequestContext requestContext,
      Map<String, String> wireAttrs,
      NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    processError(ex, requestContext, wireAttrs, nextFilter);
  }

  private <REQ extends Request, RES extends Response> void processError(Throwable ex,
      RequestContext requestContext,
      Map<String, String> wireAttrs,
      NextFilter<REQ, RES> nextFilter)
  {
    Throwable cause = ex.getCause();
    while (cause != null)
    {
      if (cause instanceof RetriableRequestException)
      {
        String message = cause.getMessage();
        LOG.debug("RetriableRequestException caught! Error message: {}", message);
        wireAttrs.put(R2Constants.RETRY_MESSAGE_ATTRIBUTE_KEY, message);
        break;
      }
      cause = cause.getCause();
    }

    nextFilter.onError(ex, requestContext, wireAttrs);
  }
}
