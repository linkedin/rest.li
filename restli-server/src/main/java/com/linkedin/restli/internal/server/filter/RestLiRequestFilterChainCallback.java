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

package com.linkedin.restli.internal.server.filter;


import com.linkedin.restli.server.RestLiRequestData;


/**
 * Callback interface used by {@link RestLiRequestFilterChain} to indicate the result of invoking the request
 * filters.
 *
 * @author nshankar
 */
public interface RestLiRequestFilterChainCallback
{
  /**
   * Indicates the successful execution of all request filters.
   *
   * @param requestData {@link RestLiRequestData} that was returned from invoking the filters.
   */
  void onSuccess(RestLiRequestData requestData);

  /**
   * Indicates unsuccessful execution of request filters.
   *
   * @param throwable {@link Throwable} error that was encountered while invoking the request filter.
   */
  void onError(Throwable throwable);
}
