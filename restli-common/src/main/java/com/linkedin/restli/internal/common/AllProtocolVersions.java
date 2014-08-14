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

package com.linkedin.restli.internal.common;


import com.linkedin.restli.common.ProtocolVersion;


/**
 * @author Keren Jin
 */
public enum AllProtocolVersions
{
  RESTLI_PROTOCOL_1_0_0(new ProtocolVersion(1, 0, 0)),
  RESTLI_PROTOCOL_2_0_0(new ProtocolVersion(2, 0, 0));

  public static final ProtocolVersion OLDEST_SUPPORTED_PROTOCOL_VERSION = RESTLI_PROTOCOL_1_0_0.getProtocolVersion();
  public static final ProtocolVersion BASELINE_PROTOCOL_VERSION = RESTLI_PROTOCOL_1_0_0.getProtocolVersion();
  public static final ProtocolVersion LATEST_PROTOCOL_VERSION = RESTLI_PROTOCOL_1_0_0.getProtocolVersion();
  public static final ProtocolVersion NEXT_PROTOCOL_VERSION = RESTLI_PROTOCOL_2_0_0.getProtocolVersion();

  private final ProtocolVersion _protocolVersion;

  AllProtocolVersions(ProtocolVersion protocolVersion)
  {
    _protocolVersion = protocolVersion;
  }

  public ProtocolVersion getProtocolVersion()
  {
    return _protocolVersion;
  }
}
