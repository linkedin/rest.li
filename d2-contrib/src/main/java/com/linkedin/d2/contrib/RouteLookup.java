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

/**
 * $Id: $
 */

package com.linkedin.d2.contrib;

import com.linkedin.common.callback.Callback;

/**
 * This interface is meant to be used in conjunction with a client that implements RoutingAwareClient.
 * This can be used when the correct d2 service name to route a request to is not known immediately
 * without additional action. For example, userA may map to serviceA, and userB maps to serviceB. In
 * this case, the userid can be used as the routeKey to make either an in memory decision or an
 * out of band call to determine what service name to return through the callback.
 *
 * @author David Hoa
 * @version $Revision: $
 */

public interface RouteLookup
{
  /**
   * This function can be used to return a different service name than the original service
   * name. The two primary inputs to modifying the original service name can be location and/or
   * routeKey. The new service name (as string) is returned through the Callback. The implementation can
   * do any action needed to return the new service name.
   *
   * This is a non-blocking interface.
   *
   * @param originalServiceName original service name
   * @param location destination descriptor, if needed
   * @param routeKey key used to determine a new service name.
   * @param callback used to return the new service name.
   */
  void run(String originalServiceName, String location, String routeKey, Callback<String> callback);
}
