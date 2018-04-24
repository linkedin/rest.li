/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.r2.event;

import com.linkedin.r2.transport.http.client.PoolStatsProvider;
import com.linkedin.r2.transport.http.common.HttpProtocolVersion;


/**
 * Fields in addition to {@link PoolStatsProvider} provided as a channel pool event.
 */
public interface ChannelPoolEventProvider extends PoolStatsProvider
{
  /**
   * Gets the cluster name the channel pool is associated to.
   * @return the name of the cluster.
   */
  String clusterName();

  /**
   * Whether channels in the pool are streaming enabled.
   * @return {@code true} if streaming is enabled; false otherwise.
   */
  boolean isStream();

  /**
   * Whether channels in the pool are TLS enabled.
   * @return {@code true} if TLS is enabled; false otherwise.
   */
  boolean isSecure();

  /**
   * The HTTP version the channels in the pool are using.
   * @return {@link HttpProtocolVersion} of the channels.
   */
  HttpProtocolVersion protocolVersion();
}
