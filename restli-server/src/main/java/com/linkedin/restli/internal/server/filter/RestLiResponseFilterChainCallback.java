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


import com.linkedin.restli.common.attachments.RestLiAttachmentReader;
import com.linkedin.restli.server.RestLiResponseAttachments;
import com.linkedin.restli.server.RestLiResponseData;


/**
 * Callback interface used by {@link RestLiResponseFilterChain} to indicate the result of invoking response
 * filters.
 *
 * @author nshankar
 */
public interface RestLiResponseFilterChainCallback
{
  /**
   * Indicate the completion of execution of all {@link com.linkedin.restli.server.filter.ResponseFilter}s..
   *
   * @param responseData {@link RestLiResponseData} obtained from invoking response filters.
   * @param responseAttachments {@link com.linkedin.restli.server.RestLiResponseAttachments} attachments
   *                             that are sent back in the response.
   */
  void onCompletion(final RestLiResponseData responseData, final RestLiAttachmentReader requestAttachmentReader,
                    final RestLiResponseAttachments responseAttachments);
}
