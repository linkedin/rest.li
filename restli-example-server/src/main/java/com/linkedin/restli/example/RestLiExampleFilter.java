/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.example;


import com.linkedin.restli.server.filter.Filter;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.FilterResponseContext;
import com.linkedin.restli.server.filter.NextRequestFilter;
import com.linkedin.restli.server.filter.NextResponseFilter;


/**
 * A simple RestLi filter that computes the request processing time and prints it to standard out.
 *
 * @author nshankar
 */
public class RestLiExampleFilter implements Filter
{
  private static final String START_TIME = RestLiExampleFilter.class.getName() + ".StartTime";

  @Override
  public void onRequest(FilterRequestContext requestContext, NextRequestFilter nextRequestFilter)
  {
    requestContext.getFilterScratchpad().put(START_TIME, System.nanoTime());
    nextRequestFilter.onRequest(requestContext);
  }

  @Override
  public void onResponse(FilterRequestContext requestContext,
                         FilterResponseContext responseContext,
                         NextResponseFilter nextResponseFilter)
  {
    final Long startTime = (Long) requestContext.getFilterScratchpad().get(START_TIME);
    System.out.println(String.format("Request processing time: %d us", (System.nanoTime() - startTime) / 1000));
    nextResponseFilter.onResponse(requestContext, responseContext);
  }
}
