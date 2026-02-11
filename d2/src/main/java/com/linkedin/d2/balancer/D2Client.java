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

package com.linkedin.d2.balancer;


import com.linkedin.common.callback.Callback;
import com.linkedin.r2.transport.common.StartableClient;
import java.net.URI;


/**
 * D2Client interface provides a way for anyone to interact with the  admin tools
 * for accessing D2 internals. In this case, Facilities provides a way for the user to
 * investigate D2 directory structure (either in Zookeeper or in FileStore or other store
 * implementation of D2) and few others. In short, Facilities is like an admin tools to D2 internals.
 *
 * It does NOT provide ability to serve rest or rpc request. For that ability we have
 * the Client interface.
 * @see com.linkedin.r2.transport.common.Client
 *
 * @author David Hoa
 * @version $Revision: $
 */

public interface D2Client extends StartableClient
{
  Facilities getFacilities();

  /**
   * Given a D2 URI, returns the cluster name associated with the service resolved from the URI.
   * The callback will receive a non-null cluster name on success, or an error if the cluster name
   * cannot be resolved.
   *
   * @param uri the D2 URI whose service's cluster name is to be resolved
   * @param callback a callback that will receive the non-null cluster name string on success, or an error on failure
   */
  default void getClusterName(URI uri, Callback<String> callback)
  {
    callback.onError(new UnsupportedOperationException("getClusterName is not supported"));
  }
}
