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

import com.linkedin.restli.common.ContentType;
import java.util.Collections;
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
  private final CompressionOption _responseCompressionOverride;
  private final ContentType _contentType;
  private final List<ContentType> _acceptTypes;
  private final boolean _acceptResponseAttachments;
  private boolean _forceWildCardProjections;

  public static final RestliRequestOptions DEFAULT_OPTIONS
      = new RestliRequestOptions(ProtocolVersionOption.USE_LATEST_IF_AVAILABLE, null, null, null, null, false, false);

  public static final RestliRequestOptions FORCE_USE_NEXT_OPTION =
      new RestliRequestOptions(ProtocolVersionOption.FORCE_USE_NEXT, null, null, null, null, false, false);

  public static final RestliRequestOptions FORCE_USE_PREV_OPTION =
      new RestliRequestOptions(ProtocolVersionOption.FORCE_USE_PREVIOUS, null, null, null, null, false, false);

  public static final RestliRequestOptions DEFAULT_MULTIPLEXER_OPTIONS = new RestliRequestOptions(
      ProtocolVersionOption.USE_LATEST_IF_AVAILABLE,
      null,
      null,
      ContentType.JSON,
      Collections.singletonList(ContentType.JSON),
      false,
      false);

  /**
   * Content type and accept types (if not null) passed in this constructor will take precedence over the corresponding configuration set
   * at {@link RestClient}. Note that this form of configuration at {@link RestClient} is deprecated, therefore please consider using
   * {@link RestliRequestOptionsBuilder} to construct a new instance.
   * @param protocolVersionOption protocol version
   * @param requestCompressionOverride request compression override
   * @param responseCompressionOverride response compression override
   * @param contentType request content type
   * @param acceptTypes list of accept types for response
   * @param acceptResponseAttachments This should only be set if clients want to handle streaming attachments
   *                                  in responses from servers. Otherwise this should not be set. Note that setting
   *                                  this allows servers to send back potentially large blobs of data which clients
   *                                  are responsible for consuming.
   * @param forceWildCardProjections  If set to true, then any projection fields set in the request query params
   *                                  are ignored and forcibly set to wildcard projection.
   */
  RestliRequestOptions(ProtocolVersionOption protocolVersionOption,
                       CompressionOption requestCompressionOverride,
                       CompressionOption responseCompressionOverride,
                       ContentType contentType,
                       List<ContentType> acceptTypes,
                       boolean acceptResponseAttachments,
                       boolean forceWildCardProjections)
  {
    _protocolVersionOption =
        (protocolVersionOption == null) ? ProtocolVersionOption.USE_LATEST_IF_AVAILABLE : protocolVersionOption;
    _requestCompressionOverride = requestCompressionOverride;
    _responseCompressionOverride = responseCompressionOverride;
    _contentType = contentType;
    _acceptTypes = acceptTypes;
    _acceptResponseAttachments = acceptResponseAttachments;
    _forceWildCardProjections = forceWildCardProjections;
  }

  public ProtocolVersionOption getProtocolVersionOption()
  {
    return _protocolVersionOption;
  }

  public CompressionOption getRequestCompressionOverride()
  {
    return _requestCompressionOverride;
  }

  public List<ContentType> getAcceptTypes()
  {
    return _acceptTypes;
  }

  public ContentType getContentType()
  {
    return _contentType;
  }

  public CompressionOption getResponseCompressionOverride()
  {
    return _responseCompressionOverride;
  }

  public boolean getAcceptResponseAttachments()
  {
    return _acceptResponseAttachments;
  }

  public boolean getForceWildCardProjections()
  {
    return _forceWildCardProjections;
  }

  public void setForceWildCardProjections(boolean shouldForce)
  {
    _forceWildCardProjections = shouldForce;
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

    if (_acceptResponseAttachments != that._acceptResponseAttachments)
    {
      return false;
    }
    if (_acceptTypes != null ? !_acceptTypes.equals(that._acceptTypes) : that._acceptTypes != null)
    {
      return false;
    }
    if (_contentType != that._contentType)
    {
      return false;
    }
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
    if (_forceWildCardProjections != that._forceWildCardProjections)
    {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode()
  {
    int result = _protocolVersionOption.hashCode();
    result = 31 * result + (_requestCompressionOverride != null ? _requestCompressionOverride.hashCode() : 0);
    result = 31 * result + (_responseCompressionOverride != null ? _responseCompressionOverride.hashCode() : 0);
    result = 31 * result + (_contentType != null ? _contentType.hashCode() : 0);
    result = 31 * result + (_acceptTypes != null ? _acceptTypes.hashCode() : 0);
    result = 31 * result + (_acceptResponseAttachments ? 1 : 0);
    result = 31 * result + (_forceWildCardProjections ? 1 : 0);
    return result;
  }

  @Override
  public String toString()
  {
    return "RestliRequestOptions{" +
        "_protocolVersionOption=" + _protocolVersionOption +
        ", _requestCompressionOverride=" + _requestCompressionOverride +
        ", _responseCompressionOverride=" + _responseCompressionOverride +
        ", _contentType=" + _contentType +
        ", _acceptTypes=" + _acceptTypes +
        ", _acceptResponseAttachments=" + _acceptResponseAttachments +
        ", _forceWildCardProjections=" + _forceWildCardProjections +
        '}';
  }
}