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

package com.linkedin.restli.server.filter;


import java.util.concurrent.CompletableFuture;

/**
 * Restli filters.
 *
 * @author gye
 *
 */
public interface Filter
{
  /**
   * Method to be invoked for each request.
   *
   * Do not implement if filter doesn't do anything on requests. If the user does not implement this method, the default
   * method will be called. The default method returns a completed future, which upon being returned will cause the
   * filter chain to invoke the subsequent filter's onRequest() method.
   *
   * Filters should return a {@link CompletableFuture}&#60;{@link Void}&#62; to indicate the completion status.
   * <ul>
   *   <li>Filters that execute synchronously should return a completed (successful or exceptionally) future.</li>
   *   <li>
   *     Filters that execute asynchronously should return a future and complete it (successful or exceptionally) when
   *     execution finishes.
   *   </li>
   * </ul>
   *
   * CompletableFuture requires an argument to be passed in when complete() is invoked. When you complete the future
   * successfully, null should be passed in as an argument because the CompletableFuture's type is Void.
   *
   * Not completing the future, either successfully or exceptionally, will cause the request processing to hang.
   *
   * If a future completes exceptionally, the request fails and the current filter's onError() will be invoked with an
   * error response.
   *
   * @param requestContext the {@link FilterRequestContext} of the request.
   * @return {@link CompletableFuture}&#60;{@link Void}&#62; - future result of filter execution.
   */
  default CompletableFuture<Void> onRequest(final FilterRequestContext requestContext)
  {
    return CompletableFuture.completedFuture(null);
  }

  /**
   * Method to be invoked for each response.
   *
   * Do not implement if filter doesn't do anything on responses. If the user does not implement this method, the
   * default method will be called. The default method returns a completed future, which upon being returned will cause
   * the filter chain to invoke the subsequent filter's onResponse() method.
   *
   * Filters should return a {@link CompletableFuture}&#60;{@link Void}&#62; to indicate the completion status.
   * <ul>
   *   <li>Filters that execute synchronously should return a completed (successful or exceptionally) future.</li>
   *   <li>
   *     Filters that execute asynchronously should return a future and complete it (successful or exceptionally) when
   *     execution finishes.
   *   </li>
   * </ul>
   *
   * CompletableFuture requires an argument to be passed in when complete() is invoked. When you complete the future
   * successfully, null should be passed in as an argument because the CompletableFuture's type is Void.
   *
   * Not completing the future, either successfully or exceptionally, will cause the response processing to hang.
   *
   * If a future completes exceptionally, the response will be converted into an error response and the next filter's
   * onError() will be invoked with the error response.
   *
   * @param requestContext the {@link FilterRequestContext} of the request that led to this response.
   * @param responseContext the {@link FilterResponseContext} of this response.
   * @return {@link CompletableFuture}&#60;{@link Void}&#62; - future result of filter execution.
   */
  default CompletableFuture<Void> onResponse(final FilterRequestContext requestContext,
                                             final FilterResponseContext responseContext)
  {
    return CompletableFuture.completedFuture(null);
  }

  /**
   * Method to be invoked for exceptions being thrown.
   *
   * Do not implement if filter doesn't do anything on errors. If the user does not implement this method, the default
   * method will be called. The default method returns an exceptionally completed future, which upon being returned will
   * cause the filter chain to invoke the subsequent filter's onError() method.
   *
   * Filters should return a {@link CompletableFuture}&#60;{@link Void}&#62; to indicate the completion status.
   * <ul>
   *   <li>Filters that execute synchronously should return a completed (successful or exceptionally) future.</li>
   *   <li>
   *     Filters that execute asynchronously should return a future and complete it (successful or exceptionally) when
   *     execution finishes.
   *   </li>
   * </ul>
   *
   * CompletableFuture requires an argument to be passed in when complete() is invoked. When you complete the future
   * successfully, null should be passed in as an argument because the CompletableFuture's type is Void.
   *
   * Not completing the future, either successfully or exceptionally, will cause the error processing to hang.
   *
   * The future should be completed exceptionally to pass the error response onto the next filter. After the last filter
   * in the chain completes exceptionally, it will cause an error response to be sent to the client.
   *
   * The future should be completed normally by calling complete() to convert the error response into a success
   * response. This will cause the next filter's onResponse() to be invoked instead of onError(). The filter should
   * generate/set appropriate response data before doing so - this means the filter should set a non-null status and
   * data for the response context.
   *
   * @param th the {@link Throwable} that caused the error.
   * @param requestContext the {@link FilterRequestContext} of the request.
   * @param responseContext the {@link FilterResponseContext} of the response.
   * @return {@link CompletableFuture}&#60;{@link Void}&#62; - future result of filter execution.
   */
  default CompletableFuture<Void> onError(Throwable th, final FilterRequestContext requestContext,
                                          final FilterResponseContext responseContext)
  {
    CompletableFuture<Void> future = new CompletableFuture<Void>();
    future.completeExceptionally(th);
    return future;
  }
}
