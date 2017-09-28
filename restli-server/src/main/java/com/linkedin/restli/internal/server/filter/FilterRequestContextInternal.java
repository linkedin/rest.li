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

package com.linkedin.restli.internal.server.filter;

import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.filter.FilterRequestContext;

/**
 * @author nshankar
 * @deprecated Use {@link FilterRequestContextInternalImpl#FilterRequestContextInternalImpl(ServerResourceContext, ResourceMethodDescriptor, RestLiRequestData)}
 *             to pass <code>RestLiRequestData</code> to the constructor.
 */
@Deprecated
// TODO: This interface is no longer needed in Pegasus. Remove it after checking external usage.
public interface FilterRequestContextInternal extends FilterRequestContext
{
  /**
   * Set request data.
   *
   * @return Request data.
   * @deprecated Use {@link FilterRequestContextInternalImpl#FilterRequestContextInternalImpl(ServerResourceContext, ResourceMethodDescriptor, RestLiRequestData)}
   *             to pass <code>RestLiRequestData</code> to the constructor.
   */
  @Deprecated
  void setRequestData(RestLiRequestData data);
}
