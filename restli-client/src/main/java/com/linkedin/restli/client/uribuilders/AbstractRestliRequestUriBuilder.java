/*
   Copyright (c) 2013 LinkedIn Corp.

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

package com.linkedin.restli.client.uribuilders;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.linkedin.data.DataMap;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.jersey.api.uri.UriComponent;
import com.linkedin.jersey.api.uri.UriTemplate;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.internal.client.QueryParamsUtil;
import com.linkedin.restli.internal.common.URIParamUtils;
import java.net.URI;


/**
 * Abstract class for constructing URIs and related components for a {@link Request}
 *
 * @author kparikh
 */
abstract class AbstractRestliRequestUriBuilder<R extends Request<?>> implements RestliUriBuilder
{
  private static final Cache<String, URI> URI_TEMPLATE_STRING_TO_URI_CACHE = Caffeine.newBuilder()
      .maximumSize(1000)
      .build();
  protected final R _request;
  protected final ProtocolVersion _version;
  protected final CompoundKey _assocKey; // can be null

  private final String _uriPrefix;

  AbstractRestliRequestUriBuilder(R request, String uriPrefix, ProtocolVersion version)
  {
    this(request, uriPrefix, version, null);
  }

  /**
   *
   * @param request request for which we are creating a URI
   * @param uriPrefix
   * @param version the Rest.li version that will be used to generate the request URI
   * @param assocKey
   */
  AbstractRestliRequestUriBuilder(R request, String uriPrefix, ProtocolVersion version, CompoundKey assocKey)
  {
    if (request == null)
    {
      throw new IllegalArgumentException("Request cannot be null");
    }
    _request = request;
    _version = version;
    _assocKey = assocKey;
    _uriPrefix = (uriPrefix == null) ? "" : uriPrefix;
  }

  protected R getRequest()
  {
    return _request;
  }

  private String bindPathKeys()
  {
    UriTemplate template = _request.getUriTemplate();
    return template.createURI(URIParamUtils.encodePathKeysForUri(_request.getPathKeys(), _version));
  }

  private final String addPrefix(String uri)
  {
    return _uriPrefix + uri;
  }

  protected void appendKeyToPath(UriBuilder uriBuilder, Object key)
  {
    if (!_request.getResourceProperties().isKeylessResource())
    {
      uriBuilder.path(URIParamUtils.encodeKeyForUri(key, UriComponent.Type.PATH_SEGMENT, _version));
    }
  }

  protected void appendQueryParams(UriBuilder b)
  {
    DataMap params = QueryParamsUtil.convertToDataMap(_request.getQueryParamsObjects(),
                                                      _request.getQueryParamClasses(),
                                                      _version,
                                                      _request.getRequestOptions().getProjectionDataMapSerializer());
    URIParamUtils.addSortedParams(b, params, _version);
  }

  protected final void appendAssocKeys(UriBuilder uriBuilder)
  {
    if (_assocKey != null && _assocKey.getNumParts() != 0)
    {
      uriBuilder.path(URIParamUtils.encodeKeyForUri(_assocKey, UriComponent.Type.PATH_SEGMENT, _version));
    }
  }

  @Override
  public URI buildBaseUri()
  {
    return URI.create(bindPathKeys());
  }

  public URI buildBaseUriWithPrefix()
  {
    if (_request.getPathKeys().isEmpty())
    {
      // if path keys are empty we don't need to bind the path keys, we can directly use the request base uri template.
      return URI_TEMPLATE_STRING_TO_URI_CACHE.get(addPrefix(_request.getBaseUriTemplate()), template -> URI.create(template));
    }
    else
    {
      return URI.create(addPrefix(bindPathKeys()));
    }
  }
}
