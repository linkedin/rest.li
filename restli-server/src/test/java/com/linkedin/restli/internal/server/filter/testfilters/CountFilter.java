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

package com.linkedin.restli.internal.server.filter.testfilters;

import com.linkedin.restli.server.filter.Filter;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.FilterResponseContext;
import java.util.concurrent.CompletableFuture;


public class CountFilter implements Filter
{
  protected int _numRequests;
  protected int _numResponses;
  protected int _numErrors;

  public CountFilter()
  {
    _numRequests = 0;
    _numResponses = 0;
    _numErrors = 0;
  }

  @Override
  public CompletableFuture<Void> onRequest(final FilterRequestContext requestContext)
  {
    _numRequests++;
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Void> onResponse(final FilterRequestContext requestContext,
                                            final FilterResponseContext responseContext)
  {
    _numResponses++;
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Void> onError(Throwable t, final FilterRequestContext requestContext,
                                         final FilterResponseContext responseContext)
  {
    _numErrors++;
    return completedFutureWithError(new TestFilterException());
  }

  public int getNumRequests() {
    return _numRequests;
  }

  public int getNumResponses() {
    return _numResponses;
  }

  public int getNumErrors() {
    return _numErrors;
  }

  /**
   * Helper method for generating completed futures that have errors
   *
   * @param t The error
   * @return A completed exceptionally future
   */
  protected CompletableFuture<Void> completedFutureWithError(Throwable t)
  {
    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(t);
    return future;
  }
}
