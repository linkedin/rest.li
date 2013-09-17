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

/**
 * $Id: $
 */

package com.linkedin.restli.client;


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.jersey.api.uri.UriTemplate;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.client.URIUtil;
import com.linkedin.restli.internal.common.QueryParamsDataMap;
import com.linkedin.restli.internal.common.URLEscaper;
import com.linkedin.restli.internal.common.URLEscaper.Escaping;
import com.linkedin.util.ArgumentUtil;

import java.lang.reflect.Array;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * @author Josh Walker
 * @version $Revision: $
 */

public abstract class AbstractRequestBuilder<K, V, R extends Request<?>> implements RequestBuilder<R>
{
  private static final Pattern SLASH_PATTERN = Pattern.compile("/");
  protected static final char HEADER_DELIMITER = ',';

  private final String                _baseURITemplate;
  protected final ResourceSpec        _resourceSpec;
  protected final CompoundKey         _assocKey    = new CompoundKey();
  protected final Map<String, String> _pathKeys    = new HashMap<String, String>();
  protected final DataMap             _queryParams = new DataMap();
  protected Map<String, String>       _headers     = new HashMap<String, String>();

  protected AbstractRequestBuilder(String baseURITemplate, ResourceSpec resourceSpec)
  {
    _baseURITemplate = baseURITemplate;
    _resourceSpec = resourceSpec;
  }

  /**
   * Create a header with the specified value if there is no existing name
   * Otherwise, overwrite the existing header with the specified value to the existing value
   *
   * @param name name of the header
   * @param value value of the header
   * @return this {@link AbstractRequestBuilder}
   */
  public AbstractRequestBuilder<K, V, R> header(String name, String value)
  {
    setHeader(name, value);
    return this;
  }

  /**
   * Create a header with the specified value if there is no existing name
   * Otherwise, append the specified value to the existing value, delimited by comma
   *
   * @param name name of the header
   * @param value value of the header
   */
  public void addHeader(String name, String value)
  {
    final String currValue = _headers.get(name);
    final String newValue = currValue == null ? value : currValue + HEADER_DELIMITER + value;
    _headers.put(name, newValue);
  }

  /**
   * Create a header with the specified value if there is no existing name
   * Otherwise, overwrite the existing header with the specified value to the existing value
   *
   * @param name name of the header
   * @param value value of the header
   */
  public void setHeader(String name, String value)
  {
    _headers.put(name, value);
  }

  /**
   * Use the specified headers to replace the existing headers
   * All old headers will be lost
   *
   * @param headers new headers
   */
  public void setHeaders(Map<String, String> headers)
  {
    _headers = new HashMap<String, String>(headers);
  }

  protected void addReqParam(String key, Object value)
  {
    ArgumentUtil.notNull(value, "value");
    addParam(key, value);
  }

  public AbstractRequestBuilder<K, V, R> pathKey(String name, Object value)
  {
    addPathKey(name, keyToString(value, Escaping.NO_ESCAPING));
    return this;
  }

  /*
   * Note that this method overrides the value at the given key, rather than adds to the collection
   * of values for it.
   */
  protected void addParam(String key, Object value)
  {
    if (value != null)
    {
      if (value instanceof Object[])
      {
        _queryParams.put(key, new DataList(coerceIterable(Arrays.asList((Object[]) value))));
      }
      else if (value.getClass().isArray())
      { // not an array of objects but still an array - must be an array of primitives
        _queryParams.put(key, new DataList(stringify(value)));
      }
      else if (value instanceof DataTemplate)
      {
        @SuppressWarnings("rawtypes")
        DataTemplate dataTemplate = (DataTemplate)value;
        _queryParams.put(key, dataTemplate.data());
      }
      else if (value instanceof Iterable)
      {
        _queryParams.put(key, new DataList(coerceIterable((Iterable<?>) value)));
      }
      else
      {
        _queryParams.put(key, DataTemplateUtil.stringify(value));
      }
    }
  }

  /**
   * To be called from the extending BatchXXXRequestBuilder classes that implement
   * ids(K...) or inputs()
   *
   * @param ids
   */
  protected final void addKeys(Collection<K> ids)
  {
    if (ids == null)
    {
      throw new IllegalArgumentException("null id");
    }

    Set<Object> allIds = new HashSet<Object>();
    for (K id : ids)
    {
      addKey(allIds, id);
    }

    DataList existingIds = (DataList)_queryParams.get(RestConstants.QUERY_BATCH_IDS_PARAM);
    if (existingIds != null && !existingIds.isEmpty())
    {
      allIds.addAll(existingIds);
    }

    _queryParams.put(RestConstants.QUERY_BATCH_IDS_PARAM,
                     new DataList(new ArrayList<Object>(allIds)));

  }

  /**
   * Adds the provided id to the provided set of objects that can be put into a DataMap (i.e. DataObject of String)
   * If the id is an instance of ComplexResourceKey, convert to DataMap, otherwise stringify.
   */
  private void addKey(Set<Object> ids, Object id)
  {
    if (id == null) {
      throw new IllegalArgumentException("Null key");
    }

    Object castId =
        id instanceof ComplexResourceKey<?, ?>
            ? ((ComplexResourceKey<?, ?>) id).toDataMap() : DataTemplateUtil.stringify(id);

    ids.add(castId);
  }

  /**
   * Add an individual key to the DataList of keys, which will be later resolved into a collection
   * of individual query parameters.
   */
  protected final void addKey(K id)
  {
    addKeys(Collections.singletonList(id));
  }

  /**
   * Depending on whether or not this is a complex resource key, return either a full (key + params)
   * string representation of the key (which for ComplexResource includes both key and params), or
   * simply key.toString() (which for ComplexResourceKey only uses key part).
   */
  protected String keyToString(K key)
  {
    return keyToString(key, Escaping.NO_ESCAPING);
  }

  protected void appendKeyToPath(UriBuilder uriBuilder, Object key)
  {
    if (isKeylessResource())
    {
      if (key != null)
      {
        throw new IllegalArgumentException("id is not allowed in this key-less resource request");
      }
    }
    else
    {
      if (key == null)
      {
        throw new IllegalArgumentException("id required to build this request");
      }

      uriBuilder.path(keyToString(key, Escaping.URL_ESCAPING));
    }
  }

  private static String keyToString(Object key, Escaping escaping)
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

  /**
   * given an iterable of objects returns a list of (non-null) Objects,
   * which can be Strings or DataMap
   * */
  private static List<Object> coerceIterable(Iterable<?> values)
  {
    assert values != null;
    List<Object> objects =
        new ArrayList<Object>(values instanceof Collection ? ((Collection<?>) values).size() : 10);
    for (Object value : values)
    {
      if (value != null)
      {
        objects.add(coerceObject(value));
      }
    }
    return objects;
  }

  private static Object coerceObject(Object value)
  {
    if (value instanceof DataTemplate)
    {
      @SuppressWarnings("rawtypes")
      DataTemplate result = (DataTemplate)value;
      return result.data();
    }
    else
    {
      return DataTemplateUtil.stringify(value);
    }
  }

  /** given an array of primitives returns a collection of strings */
  private static List<String> stringify(Object array)
  {
    assert array != null && array.getClass().isArray();
    int len = Array.getLength(array);
    List<String> strings = new ArrayList<String>(len);
    for (int i = 0; i < len; ++i)
    {
      Object value = Array.get(array, i);
      if (value != null)
      {
        strings.add(value.toString());
      }
    }
    return strings;
  }

  protected void addPathKey(String key, Object value)
  {
    if (value == null)
    {
      throw new IllegalArgumentException("Path key must be non-null");
    }
    _pathKeys.put(key, DataTemplateUtil.stringify(value));
  }

  protected void addAssocKey(String key, Object value)
  {
    _assocKey.append(key, value);
  }

  protected void appendQueryParams(UriBuilder b)
  {
    QueryParamsDataMap.addSortedParams(b, _queryParams);
  }

  protected final void appendAssocKeys(UriBuilder uriBuilder)
  {
    uriBuilder.path(_assocKey.toString());
  }

  protected URI bindPathKeys()
  {
    UriTemplate template = new UriTemplate(_baseURITemplate);
    for (String key : template.getTemplateVariables())
    {
      if (!_pathKeys.containsKey(key))
      {
        throw new IllegalStateException("Missing path key: '" + key + "'");
      }
    }
    Map<String, String> escapedKeys = new HashMap<String, String>();
    for(Map.Entry<String, String> key : _pathKeys.entrySet())
    {
      escapedKeys.put(key.getKey(), URLEscaper.escape(key.getValue(), Escaping.URL_ESCAPING));
    }

    return URI.create(template.createURI(escapedKeys));
  }

  protected List<String> getResourcePath()
  {
    UriTemplate template = new UriTemplate(_baseURITemplate);
    List<String> resourcePath = new ArrayList<String>(1);
    String[] pathParts = SLASH_PATTERN.split(template.createURI(Collections.<String, String>emptyMap()));

    for (String pathPart : pathParts)
    {
      if (!pathPart.equals(""))
      {
        resourcePath.add(pathPart);
      }
    }

    return resourcePath;
  }

  protected void addFields(PathSpec... fieldPaths)
  {
    if (_queryParams.containsKey(RestConstants.FIELDS_PARAM))
    {
      throw new IllegalStateException("Fields already set on this request: "
                                          + _queryParams.get(RestConstants.FIELDS_PARAM));
    }

    addParam(RestConstants.FIELDS_PARAM, URIUtil.encodeFields(fieldPaths));
  }

  protected boolean isComplexKeyResource()
  {
    return _resourceSpec.getKeyClass() == ComplexResourceKey.class;
  }

  protected boolean isKeylessResource()
  {
    return _resourceSpec.getKeyClass() == null;
  }

  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getName());
    sb.append("{_assocKey=").append(_assocKey);
    sb.append(", _baseURITemplate='").append(_baseURITemplate).append('\'');
    sb.append(", _headers=").append(_headers);
    sb.append(", _pathKeys=").append(_pathKeys);
    sb.append(", _resourceSpec=").append(_resourceSpec);
    sb.append(", _queryParams=").append(getBoundedString(_queryParams, 32));
    sb.append('}');
    return sb.toString();
  }

  private static String getBoundedString(Map<?, ?> map, int maxEntryCount)
  {
    if (map == null || map.size() < maxEntryCount)
    {
      return String.valueOf(map);
    }

    return new ArrayList<Map.Entry<?, ?>>(map.entrySet()).subList(0, maxEntryCount).toString() + " (truncated)";
  }
}
