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

import java.util.List;


/**
 * Represents custom options for a {@link com.linkedin.restli.client.Request}.
 *
 * @author kparikh
 */
public class RestliRequestOptions
{
  private final ProtocolVersionOption _protocolVersionOption;
  private final CompressionOption _requestCompressionOverride;
  private final RestClient.ContentType _contentType;
  private final List<RestClient.AcceptType> _acceptTypes;


  public static final RestliRequestOptions DEFAULT_OPTIONS
      = new RestliRequestOptions(ProtocolVersionOption.USE_LATEST_IF_AVAILABLE, null);

  public static final RestliRequestOptions FORCE_USE_NEXT_OPTION =
      new RestliRequestOptions(ProtocolVersionOption.FORCE_USE_NEXT, null);

  /**
   * Since per-request content type and accept types are not specified, we will use their values from the corresponding configuration
   * set at {@link RestClient}. Note that this form of configuration at {@link RestClient} is deprecated, therefore please consider using
   * {@link RestliRequestOptionsBuilder} to construct a new instance.
   * @param protocolVersionOption protocol version
   * @param requestCompressionOverride request compression override
   */
  RestliRequestOptions(ProtocolVersionOption protocolVersionOption, CompressionOption requestCompressionOverride)
  {
    this(protocolVersionOption, requestCompressionOverride, null, null);
  }

  /**
   * Content type and accept types (if not null) passed in this constructor will take precedence over the corresponding configuration set
   * at {@link RestClient}. Note that this form of configuration at {@link RestClient} is deprecated, therefore please consider using
   * {@link RestliRequestOptionsBuilder} to construct a new instance.
   * @param protocolVersionOption protocol version
   * @param requestCompressionOverride request compression override
   * @param contentType request content type
   * @param acceptTypes list of accept types for response
   */
  RestliRequestOptions(ProtocolVersionOption protocolVersionOption,
                       CompressionOption requestCompressionOverride,
                       RestClient.ContentType contentType,
                       List<RestClient.AcceptType> acceptTypes)
  {
    _protocolVersionOption =
        (protocolVersionOption == null) ? ProtocolVersionOption.USE_LATEST_IF_AVAILABLE : protocolVersionOption;
    _requestCompressionOverride = requestCompressionOverride;
    _contentType = contentType;
    _acceptTypes = acceptTypes;
  }

  public ProtocolVersionOption getProtocolVersionOption()
  {
    return _protocolVersionOption;
  }

  public CompressionOption getRequestCompressionOverride()
  {
    return _requestCompressionOverride;
  }

  public List<RestClient.AcceptType> getAcceptTypes()
  {
    return _acceptTypes;
  }

  public RestClient.ContentType getContentType()
  {
    return _contentType;
  }

  @Override
  public int hashCode()
  {
    int result = _protocolVersionOption != null ? _protocolVersionOption.hashCode() : 0;
    result = 31 * result + (_requestCompressionOverride != null ? _requestCompressionOverride.hashCode() : 0);
    result = 31 * result + (_contentType != null ? _contentType.hashCode() : 0);
    result = 31 * result + (_acceptTypes != null ? _acceptTypes.hashCode() : 0);
    return result;
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
    if (_protocolVersionOption != other._protocolVersionOption)
    {
      return false;
    }
    if (_requestCompressionOverride != other._requestCompressionOverride)
    {
      return false;
    }
    if (_contentType != other._contentType)
    {
      return false;
    }
    if (_acceptTypes != null ? !_acceptTypes.equals(other._acceptTypes) : other._acceptTypes != null)
    {
      return false;
    }
    return true;
  }

  @Override
  public String toString()
  {
    return "{_protocolVersionOption: "
        + _protocolVersionOption.toString()
        + ", _requestCompressionOverride: "
        + (_requestCompressionOverride != null ? _requestCompressionOverride.toString() : "null")
        + ", _contentType: "
        + (_contentType != null ? _contentType.toString() : "null")
        + ", _acceptTypes: "
        + (_acceptTypes != null ? _acceptTypes.toString() : "null")
        + "}";
  }
}
