/*
   Copyright (c) 2012 LinkedIn Corp.

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

package com.linkedin.restli.internal.server;


import com.linkedin.restli.internal.server.filter.RestLiFilterChain;
import com.linkedin.restli.internal.server.filter.RestLiFilterResponseContextFactory;
import com.linkedin.restli.server.RestLiResponseAttachments;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.FilterResponseContext;


/**
 * Used for callbacks from RestLiMethodInvoker. When the REST method completes its execution, it invokes RestLiCallback,
 * which sets off the filter chain responses and eventually a response is sent to the client.
 */
public class RestLiCallback
{
  private final RestLiFilterChain _filterChain;
  private final FilterRequestContext _filterRequestContext;
  private final RestLiFilterResponseContextFactory _filterResponseContextFactory;

  public RestLiCallback(final FilterRequestContext filterRequestContext,
                        final RestLiFilterResponseContextFactory filterResponseContextFactory,
                        final RestLiFilterChain filterChain)
  {
    _filterResponseContextFactory = filterResponseContextFactory;
    _filterChain = filterChain;
    _filterRequestContext = filterRequestContext;
  }

  public void onSuccess(final Object result, final RestLiResponseAttachments responseAttachments)
  {
    final FilterResponseContext responseContext;
    try
    {
      responseContext = _filterResponseContextFactory.fromResult(result);
    }
    catch (Exception e)
    {
      // Invoke the onError method if we run into any exception while creating the response context from result.
      // Note that due to the fact we are in onSuccess(), we assume the application code has absorbed, or is in the
      // process of absorbing any request attachments present.
      onError(e, responseAttachments);
      return;
    }
    // Now kick off the responses in the filter chain. Same note as above; we assume that the application code has
    // absorbed any request attachments present in the request.
    _filterChain.onResponse(_filterRequestContext, responseContext, responseAttachments);
  }

  public void onError(final Throwable e, final RestLiResponseAttachments responseAttachments)
  {
    final FilterResponseContext responseContext = _filterResponseContextFactory.fromThrowable(e);

    // Now kick off the response filters with error
    _filterChain.onError(e, _filterRequestContext, responseContext, responseAttachments);
  }
}
