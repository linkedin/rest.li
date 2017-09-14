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

package com.linkedin.restli.internal.server.filter;


import com.linkedin.restli.server.RestLiResponseAttachments;
import com.linkedin.restli.server.RestLiResponseData;


/**
 * Interface for filter chain callbacks. When a filter chain finishes iterating, it will use this callback trigger the
 * required action.
 *
 * @author gye
 */
public interface FilterChainCallback
{
  /**
   *Method to be called after a filter chain successfully iterates to the end of the response side.
   *
   * @param responseData the {@link RestLiResponseData} of the response.
   * @param responseAttachments the {@link RestLiResponseAttachments} for the response.
   */
  void onResponseSuccess(final RestLiResponseData<?> responseData, final RestLiResponseAttachments responseAttachments);

  /**
   * Method to be called after a filter chain finishes iterating on the response side, but with an error.
   *
   * @param th the throwable that caused the error.
   * @param responseData the {@link RestLiResponseData} of the response.
   * @param responseAttachments the {@link RestLiResponseAttachments} of the response.
   */
  void onError(Throwable th, final RestLiResponseData<?> responseData, final RestLiResponseAttachments responseAttachments);
}
