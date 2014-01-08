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
   * Use the latest version of the Rest.li protocol, regardless of what the server supports.
   * Be careful when using this option as it might cause the request to fail if the server does not support the latest
   * Rest.li protocol.
   * The latest protocol version is defined as {@link com.linkedin.restli.common.RestConstants#LATEST_PROTOCOL_VERSION}.
   */
  FORCE_USE_LATEST,

  /**
   * Use the latest version of the Rest.li protocol if the server supports it. Use the default version otherwise.
   * The latest protocol version is defined as {@link com.linkedin.restli.common.RestConstants#LATEST_PROTOCOL_VERSION}.
   * The default protocol version is defined as
   * {@link com.linkedin.restli.common.RestConstants#DEFAULT_PROTOCOL_VERSION}.
   */
  USE_LATEST_IF_AVAILABLE
}
