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

package com.linkedin.r2.transport.http.client.common;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;

/**
 * Interface of the Factory class to create the right instance of {@link ChannelPoolManagerImpl} given a set of transport
 * properties {@link ChannelPoolManagerKey}.
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public interface ChannelPoolManagerFactory
{
  /**
   * @param channelPoolManagerKey An object composed by all the transport client properties
   *                              to initialize the current client
   */
  ChannelPoolManager buildRest(ChannelPoolManagerKey channelPoolManagerKey);

  /**
   * @param channelPoolManagerKey An object composed by all the transport client properties
   *                              to initialize the current client
   */
  ChannelPoolManager buildStream(ChannelPoolManagerKey channelPoolManagerKey);

  /**
   * @param channelPoolManagerKey An object composed by all the transport client properties
   *                              to initialize the current client
   */
  ChannelPoolManager buildHttp2Stream(ChannelPoolManagerKey channelPoolManagerKey);

  /**
   * @param callback called when the shutdown is completed, the callback is guaranteed to be called
   */
  void shutdown(Callback<None> callback);
}
