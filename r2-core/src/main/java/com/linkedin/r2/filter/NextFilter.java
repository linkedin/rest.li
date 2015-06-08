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

/* $Id$ */
package com.linkedin.r2.filter;

import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.Response;

import java.util.Map;

/**
 * Abstraction for the next filter in a chain of filters.  This interface simplifies
 * The polymorphism of the {@link Request} and {@link Response} message hierarchies.
 *
 * @author Chris Pettitt
 * @version $Revision$
 *
 * @param <REQ> The {@link Request} subtype for the next filter.
 * @param <RES> The {@link Response} subtype for the next filter.
 */
public interface NextFilter<REQ extends Request, RES extends Response>
{
  /**
   * Invoke the appropriate request-handling method of the {@link NextFilter}.
   *
   * @param req the request message to be filtered.
   * @param requestContext the {@link RequestContext} for the request.
   * @param wireAttrs the wire attributes for the request.
   */
  void onRequest(REQ req, RequestContext requestContext, Map<String, String> wireAttrs);

  /**
   * Invoke the appropriate response-handling method of the {@link NextFilter}.
   *
   * @param res the response message to be filtered.
   * @param requestContext the {@link RequestContext} for the response.
   * @param wireAttrs the wire attributes for the response.
   */
  void onResponse(RES res, RequestContext requestContext, Map<String, String> wireAttrs);

  /**
   * Invoke the appropriate error-handling method of the {@link NextFilter}.
   *
   * @param ex the throwable representation of the error.
   * @param requestContext the {@link RequestContext} for the error.
   * @param wireAttrs the wire attributes for the error.
   */
  void onError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs);
}
