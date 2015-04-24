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

package com.linkedin.r2.transport.common;

import java.util.Map;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;

/**
 * Factory for TransportClients.  The factory is expected to be immutable.
 *
 * @author Chris Riccomini
 * @version $Revision$
 */
public interface TransportClientFactory
{
  /**
   * Create a new {@link TransportClient} with the specified properties.
   *
   * @param properties map of properties for the {@link TransportClient}
   * @return an appropriate {@link TransportClient} instance, as specified by the properties.
   */
  TransportClient getClient(Map<String, ? extends Object> properties);

  /**
   * Shutdown this factory.
   *
   * @param callback {@link Callback} which is invoked when shutdown is complete.
   */
  void shutdown(Callback<None> callback);
}
