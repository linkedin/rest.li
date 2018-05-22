/*
    Copyright (c) 2017 LinkedIn Corp.

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

import com.linkedin.restli.internal.server.RestLiCallback;
import com.linkedin.restli.server.RestLiRequestData;


/**
 * This class dispatches the request processing after filter chain iterates all its request filters.
 *
 * @author xma
 */
public interface FilterChainDispatcher
{
  /**
   * Method to be called after a filter chain successfully iterates to the end of the request side.
   *
   * @param requestData the {@link RestLiRequestData} of the request.
   * @param restLiCallback the {@link RestLiCallback} to be called after the RestLi method has been invoked.
   */
  void onRequestSuccess(final RestLiRequestData requestData, final RestLiCallback restLiCallback);

}