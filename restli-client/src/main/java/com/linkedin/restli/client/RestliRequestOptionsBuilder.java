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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Builds {@link RestliRequestOptions}. NOT thread safe.
 *
 * @author kparikh
 */
public class RestliRequestOptionsBuilder
{
  private ProtocolVersionOption _protocolVersionOption;
  private CompressionOption _requestCompressionOverride;
  private ContentType _contentType;
  private List<ContentType> _acceptTypes;
  private CompressionOption _responseCompressionOverride;
  private boolean _acceptResponseAttachments = false;
  private ProjectionDataMapSerializer _projectionDataMapSerializer;

  public RestliRequestOptionsBuilder()
  {
  }

  public RestliRequestOptionsBuilder(RestliRequestOptions restliRequestOptions)
  {
    setProtocolVersionOption(restliRequestOptions.getProtocolVersionOption());
    setRequestCompressionOverride(restliRequestOptions.getRequestCompressionOverride());
    setResponseCompressionOverride(restliRequestOptions.getResponseCompressionOverride());
    setContentType(restliRequestOptions.getContentType());
    setAcceptTypes(restliRequestOptions.getAcceptTypes());
    setAcceptResponseAttachments(restliRequestOptions.getAcceptResponseAttachments());
    setProjectionDataMapSerializer(restliRequestOptions.getProjectionDataMapSerializer());
  }

  public RestliRequestOptionsBuilder setProtocolVersionOption(ProtocolVersionOption protocolVersionOption)
  {
    _protocolVersionOption = protocolVersionOption;
    return this;
  }

  public RestliRequestOptionsBuilder setRequestCompressionOverride(CompressionOption requestCompressionOverride)
  {
    _requestCompressionOverride = requestCompressionOverride;
    return this;
  }

  public RestliRequestOptionsBuilder setContentType(ContentType contentType)
  {
    _contentType = contentType;
    return this;
  }

  public RestliRequestOptionsBuilder setAcceptTypes(List<ContentType> acceptTypes)
  {
    if (acceptTypes != null)
    {
      _acceptTypes = new ArrayList<>(acceptTypes);
    }
    return this;
  }

  public RestliRequestOptionsBuilder addAcceptTypes(List<ContentType> acceptTypes)
  {
    if (_acceptTypes == null)
    {
      return setAcceptTypes(acceptTypes);
    }
    else
    {
      for (ContentType acceptType: acceptTypes)
      {
        if (!_acceptTypes.contains(acceptType))
        {
          _acceptTypes.add(acceptType);
        }
      }
    }
    return this;
  }

  public RestliRequestOptionsBuilder addAcceptType(ContentType acceptType)
  {
    if (_acceptTypes == null)
    {
      _acceptTypes = new ArrayList<>();
    }
    if (!_acceptTypes.contains(acceptType))
    {
      _acceptTypes.add(acceptType);
    }
    return this;
  }

  public RestliRequestOptionsBuilder setResponseCompressionOverride(CompressionOption responseCompressionOverride)
  {
    _responseCompressionOverride = responseCompressionOverride;
    return this;
  }

  public RestliRequestOptionsBuilder setAcceptResponseAttachments(boolean acceptResponseAttachments)
  {
    _acceptResponseAttachments = acceptResponseAttachments;
    return this;
  }

  public RestliRequestOptionsBuilder setProjectionDataMapSerializer(ProjectionDataMapSerializer serializer)
  {
    _projectionDataMapSerializer = serializer;
    return this;
  }

  public RestliRequestOptions build()
  {
    return new RestliRequestOptions(_protocolVersionOption, _requestCompressionOverride, _responseCompressionOverride,
        _contentType, _acceptTypes != null ? Collections.unmodifiableList(_acceptTypes) : null, _acceptResponseAttachments,
        _projectionDataMapSerializer != null ? _projectionDataMapSerializer : RestLiProjectionDataMapSerializer.DEFAULT_SERIALIZER);
  }

  public ProtocolVersionOption getProtocolVersionOption()
  {
    return _protocolVersionOption;
  }

  public CompressionOption getRequestCompressionOverride()
  {
    return _requestCompressionOverride;
  }

  public ContentType getContentType()
  {
    return _contentType;
  }

  public List<ContentType> getAcceptTypes()
  {
    return _acceptTypes != null ? Collections.unmodifiableList(_acceptTypes) : null;
  }

  public CompressionOption getResponseCompressionOverride()
  {
    return _responseCompressionOverride;
  }

  public boolean isAcceptResponseAttachments()
  {
    return _acceptResponseAttachments;
  }

  public ProjectionDataMapSerializer getProjectionDataMapSerializer()
  {
    return _projectionDataMapSerializer;
  }
}
