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


import com.linkedin.r2.filter.CompressionOption;

/**
 * Represents custom options for a {@link com.linkedin.restli.client.Request}.
 *
 * @author kparikh
 */
public class RestliRequestOptions
{
  private final ProtocolVersionOption _protocolVersionOption;
  private final CompressionOption _requestCompressionOverride;
  private final CompressionOption _responseCompressionOverride;

  public static final RestliRequestOptions DEFAULT_OPTIONS
      = new RestliRequestOptions(ProtocolVersionOption.USE_LATEST_IF_AVAILABLE, null, null);

  public static final RestliRequestOptions FORCE_USE_NEXT_OPTION =
      new RestliRequestOptions(ProtocolVersionOption.FORCE_USE_NEXT, null, null);

  // use {@link RestliRequestOptionsBuilder} to construct new instance.
  RestliRequestOptions(ProtocolVersionOption protocolVersionOption, CompressionOption requestCompressionOverride,
                       CompressionOption responseCompressionOverride)
  {
    _protocolVersionOption =
        (protocolVersionOption == null) ? ProtocolVersionOption.USE_LATEST_IF_AVAILABLE : protocolVersionOption;
    _requestCompressionOverride = requestCompressionOverride;
    _responseCompressionOverride = responseCompressionOverride;
  }

  public ProtocolVersionOption getProtocolVersionOption()
  {
    return _protocolVersionOption;
  }

  public CompressionOption getRequestCompressionOverride()
  {
    return _requestCompressionOverride;
  }

  public CompressionOption getResponseCompressionOverride()
  {
    return _responseCompressionOverride;
  }

  @Override
  public int hashCode()
  {
    int result = _protocolVersionOption != null ? _protocolVersionOption.hashCode() : 0;
    result = 31 * result + (_requestCompressionOverride != null ? _requestCompressionOverride.hashCode() : 0);
    result = 31 * result + (_responseCompressionOverride != null ? _responseCompressionOverride.hashCode() : 0);
    return result;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o)
    {
      return true;
    }
    if (o == null || getClass() != o.getClass())
    {
      return false;
    }

    RestliRequestOptions that = (RestliRequestOptions) o;

    if (_protocolVersionOption != that._protocolVersionOption)
    {
      return false;
    }
    if (_requestCompressionOverride != that._requestCompressionOverride)
    {
      return false;
    }
    if (_responseCompressionOverride != that._responseCompressionOverride)
    {
      return false;
    }

    return true;
  }

  @Override
  public String toString()
  {
    return "{_protocolVersionOption: " + _protocolVersionOption.toString()
        + ", _requestCompressionOverride: " + _requestCompressionOverride
        + ", _responseCompressionOverride: " + _responseCompressionOverride
        + '}';
  }
}
