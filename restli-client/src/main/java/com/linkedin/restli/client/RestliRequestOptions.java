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
 * Represents custom options for a {@link com.linkedin.restli.client.Request}.
 *
 * @author kparikh
 */
public class RestliRequestOptions
{
  private final ProtocolVersionOption _protocolVersionOption;

  public static final RestliRequestOptions DEFAULT_OPTIONS
      = new RestliRequestOptions(ProtocolVersionOption.USE_LATEST_IF_AVAILABLE);

  RestliRequestOptions(ProtocolVersionOption protocolVersionOption)
  {
    _protocolVersionOption =
        (protocolVersionOption == null) ? ProtocolVersionOption.USE_LATEST_IF_AVAILABLE : protocolVersionOption;
  }

  public ProtocolVersionOption getProtocolVersionOption()
  {
    return _protocolVersionOption;
  }

  @Override
  public int hashCode()
  {
    return _protocolVersionOption.hashCode();
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj == this)
    {
      return true;
    }
    if (obj == null)
    {
      return false;
    }
    if (!(obj instanceof RestliRequestOptions))
    {
      return false;
    }
    RestliRequestOptions other = (RestliRequestOptions)obj;
    return _protocolVersionOption == other._protocolVersionOption;
  }

  @Override
  public String toString()
  {
    return "{_protocolVersionOption: " + _protocolVersionOption.toString() + "}";
  }
}
