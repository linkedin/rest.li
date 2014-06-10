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

package com.linkedin.restli.client;


/**
 * @author kparikh
 */
public enum ProtocolVersionOption
{
  /**
   * Use the next version of the Rest.li protocol to encode requests, regardless of the version running on the server.
   * The next version of the Rest.li protocol is the version currently under development. This option should typically
   * NOT be used for production services.
   * CAUTION: this can cause requests to fail if the server does not understand the next version of the protocol.
   * "Next version" is defined as {@link com.linkedin.restli.internal.common.AllProtocolVersions#NEXT_PROTOCOL_VERSION}.
   */
  FORCE_USE_NEXT,

  /**
   * Use the latest version of the Rest.li protocol to encode requests, regardless of the version running on the server.
   * CAUTION: this can cause requests to fail if the server does not understand the latest version of the protocol.
   * "Latest version" is defined as {@link com.linkedin.restli.internal.common.AllProtocolVersions#LATEST_PROTOCOL_VERSION}.
   */
  FORCE_USE_LATEST,

  /**
   * Use the latest version of the Rest.li protocol if the server supports it.
   * If the server version is less than the baseline Rest.li protocol version then fail the request.
   * If the server version is greater than the next Rest.li protocol version then fail the request.
   * If the server is between the baseline and the latest version then use the server version to encode the request.
   * If the server version is greater than or equal to the latest protocol version then use that to encode the request.
   * "Baseline version" is defined as {@link com.linkedin.restli.internal.common.AllProtocolVersions#BASELINE_PROTOCOL_VERSION}.
   * "Latest version" is defined as {@link com.linkedin.restli.internal.common.AllProtocolVersions#LATEST_PROTOCOL_VERSION}
   * "Next version" is defined as {@link com.linkedin.restli.internal.common.AllProtocolVersions#NEXT_PROTOCOL_VERSION}.
   */
  USE_LATEST_IF_AVAILABLE
}
