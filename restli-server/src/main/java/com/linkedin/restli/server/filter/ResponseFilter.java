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

package com.linkedin.restli.server.filter;


/**
 * A filter that processes outgoing responses from RestLi resources.
 *
 * @author nshankar
 */
public interface ResponseFilter
{
  /**
   * Response filter method to be invoked on a execution of the resource.
   *
   * @param requestContext     Reference to {@link FilterRequestContext}.
   * @param responseContext    Reference to {@link FilterResponseContext}.
   * @param nextResponseFilter The next filter in the chain.  Concrete implementations should invoke {@link
   *                           NextResponseFilter#onResponse} to continue the filter chain.
   */
  void onResponse(final FilterRequestContext requestContext,
                  final FilterResponseContext responseContext,
                  final NextResponseFilter nextResponseFilter);
}
