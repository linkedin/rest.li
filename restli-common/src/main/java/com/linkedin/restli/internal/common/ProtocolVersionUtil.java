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


package com.linkedin.restli.internal.common;


import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;


/**
 * @author kparikh
 */
public class ProtocolVersionUtil
{
  /**
   * Extracts a {@link ProtocolVersion} from a {@link RestRequest}
   *
   * @param request the {@link RestRequest} we want to extract the {@link ProtocolVersion} from
   *
   * @return {@link AllProtocolVersions#RESTLI_PROTOCOL_1_0_0} if a protocol version is not present in the request header,
   *         {@link ProtocolVersion#ProtocolVersion(String)} otherwise
   */
  public static ProtocolVersion extractProtocolVersion(RestRequest request)
  {
    if (request == null)
    {
      throw new IllegalArgumentException("Request cannot be null!");
    }
    String protocolVersion = request.getHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION);
    if (protocolVersion == null)
    {
      // if no protocol version is present we assume that the 1.0.0 protocol was used in the request.
      return AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion();
    }
    return new ProtocolVersion(protocolVersion);
  }
}
