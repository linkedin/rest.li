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


import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.jersey.api.uri.UriTemplate;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.util.RestliBuilderUtils;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.internal.AllProtocolVersions;
import com.linkedin.restli.internal.client.QueryParamsUtil;
import com.linkedin.restli.internal.common.QueryParamsDataMap;
import com.linkedin.restli.internal.common.URLEscaper;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;


/**
 * Abstract class for constructing URIs and related components for a {@link Request}
 *
 * @author kparikh
 */
abstract class AbstractRestliRequestUriBuilder<R extends Request> implements RestliUriBuilder
{
  protected final R _request;
  protected final ProtocolVersion _version;
  protected final CompoundKey _assocKey; // can be null

  private final String _uriPrefix;

  AbstractRestliRequestUriBuilder(R request)
  {
    this(request, "", AllProtocolVersions.DEFAULT_PROTOCOL_VERSION, null);
  }

  AbstractRestliRequestUriBuilder(R request, String uriPrefix)
  {
    this(request, uriPrefix, AllProtocolVersions.DEFAULT_PROTOCOL_VERSION, null);
  }

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
    _version = (version == null ? AllProtocolVersions.DEFAULT_PROTOCOL_VERSION : version);
    _assocKey = assocKey;
    _uriPrefix = (uriPrefix == null) ? "" : uriPrefix;
  }

  protected R getRequest()
  {
    return _request;
  }

  private String bindPathKeys()
  {
    UriTemplate template = new UriTemplate(_request.getBaseUriTemplate());
    @SuppressWarnings("unchecked")
    Map<String, Object> pathKeys = _request.getPathKeys();
    Map<String, String> escapedKeys = new HashMap<String, String>();

    for(Map.Entry<String, Object> entry : pathKeys.entrySet())
    {
      String value = RestliBuilderUtils.keyToString(entry.getValue(), URLEscaper.Escaping.URL_ESCAPING, _version);
      if (value == null)
      {
        throw new IllegalArgumentException("Missing value for path key " + entry.getKey());
      }
      escapedKeys.put(entry.getKey(), value);
    }

    return template.createURI(escapedKeys);
  }

  private final String addPrefix(String uri)
  {
    return _uriPrefix + uri;
  }

  private static String keyToString(Object key, URLEscaper.Escaping escaping)
  {
    String result;
    if (key == null)
    {
      result = null;
    }
    else if (key instanceof ComplexResourceKey)
    {
      result = ((ComplexResourceKey<?,?>)key).toStringFull(escaping);
    }
    else if (key instanceof CompoundKey)
    {
      result = key.toString(); // already escaped
    }
    else
    {
      result = URLEscaper.escape(DataTemplateUtil.stringify(key), escaping);
    }
    return result;
  }

  protected void appendKeyToPath(UriBuilder uriBuilder, Object key)
  {
    if (!_request.getResourceSpec().isKeylessResource())
    {
      uriBuilder.path(keyToString(key, URLEscaper.Escaping.URL_ESCAPING));
    }
  }

  @SuppressWarnings("unchecked")
  protected void appendQueryParams(UriBuilder b)
  {
    QueryParamsDataMap.addSortedParams(b, QueryParamsUtil.convertToDataMap(_request.getQueryParamsObjects(),
                                                                           _version));
  }

  protected final void appendAssocKeys(UriBuilder uriBuilder)
  {
    if (_assocKey == null)
    {
      throw new IllegalArgumentException("_assocKey is null");
    }
    uriBuilder.path(_assocKey.toString());
  }

  @Override
  public URI buildBaseUri()
  {
    return URI.create(bindPathKeys());
  }


  public URI buildBaseUriWithPrefix()
  {
    return URI.create(addPrefix(bindPathKeys()));
  }
}
