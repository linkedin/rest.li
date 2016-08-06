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
import java.util.concurrent.CompletableFuture;


/**
 * A simple RestLi filter that computes the request processing time and prints it to standard out.
 *
 * @author nshankar
 */
public class RestLiExampleFilter implements Filter
{
  private static final String START_TIME = RestLiExampleFilter.class.getName() + ".StartTime";

  @Override
  public CompletableFuture<Void> onRequest(FilterRequestContext requestContext)
  {
    requestContext.getFilterScratchpad().put(START_TIME, System.nanoTime());
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Void> onResponse(FilterRequestContext requestContext,
                                            FilterResponseContext responseContext)
  {
    final Long startTime = (Long) requestContext.getFilterScratchpad().get(START_TIME);
    System.out.println(String.format("Request processing time: %d us", (System.nanoTime() - startTime) / 1000));
    return CompletableFuture.completedFuture(null);
  }
}
