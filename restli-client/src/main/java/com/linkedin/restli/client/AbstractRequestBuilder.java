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
import java.util.Map.Entry;
import java.util.Set;

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.jersey.api.uri.UriTemplate;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.client.URIUtil;
import com.linkedin.restli.internal.common.QueryParamsDataMap;
import com.linkedin.util.ArgumentUtil;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public abstract class AbstractRequestBuilder<K, V, R extends Request<?>> implements
    RequestBuilder<R>
{
  private final String                            _baseURITemplate;
  protected final Map<String, String>             _headers            = new HashMap<String, String>();
  protected final Map<String, Collection<String>> _queryParams        = new HashMap<String, Collection<String>>();
  protected final CompoundKey                     _assocKey           = new CompoundKey();
  protected Set<PathSpec>                         _fields;
  protected final Map<String, String>             _keys               = new HashMap<String, String>();
  protected final ResourceSpec                    _resourceSpec;
  // This is a DataMap to hold any schema-backed query parameters
  protected final DataMap                         _queryParamsDataMap = new DataMap();
  protected final DataList                        _keysDataList       = new DataList();

  protected AbstractRequestBuilder(String baseURITemplate, ResourceSpec resourceSpec)
  {
    _baseURITemplate = baseURITemplate;
    _resourceSpec = resourceSpec;
  }
  
  public AbstractRequestBuilder<K, V, R> header(String key, String value)
  {
    addHeader(key, value);
    return this;
  }

  protected void addHeader(String key, String value)
  {
    _headers.put(key, value);
  }

  protected void addReqParam(String key, Object value)
  {
    ArgumentUtil.notNull(value, "value");
    addParam(key, value);
  }
  
  public AbstractRequestBuilder<K, V, R> pathKey(String name, Object value)
  {
    addKey(name, value);
    return this;
  }

  protected void addParam(String key, Object value)
  {
    if (value != null)
    {
      if (value instanceof Object[])
      {
        _queryParams.put(key, stringify(Arrays.asList((Object[]) value)));
      }
      else if (value.getClass().isArray())
      { // not an array of objects but still an array - must be an array of primitives
        _queryParams.put(key, stringify(value));
      }
      else if (value instanceof DataTemplate)
      {
        @SuppressWarnings("rawtypes")
        DataTemplate dataTemplate = (DataTemplate)value;
        _queryParamsDataMap.put(key, dataTemplate.data());
      }
      else if (value instanceof Iterable)
      {
        _queryParams.put(key, stringify((Iterable<?>) value));
      }
      else
      {
        _queryParams.put(key, Collections.singleton(stringifySimpleValue(value)));
      }
    }
  }
  
  /**
   * To be called from the extending BatchXXXRequestBuilder classes that implement ids(K...) to branch
   * depending on whether the resource key is a complex one.
   * 
   * @param ids
   */
  protected final void addKeyParams(Collection<K> ids)
  {
    // If this is a complex key resource, use different query params naming and semantics
    // as described in {@link QueryParamsDataMap}
    if (isComplexKeyResource())
    {
      @SuppressWarnings("unchecked")
      Collection<ComplexResourceKey<?, ?>> complexKeyIds =
          (Collection<ComplexResourceKey<?, ?>>) ids;
      for (ComplexResourceKey<?, ?> id : complexKeyIds)
      {
        addComplexKey(id);
      }
      //addComplexKeyParams();
    }
    // Otherwise, just add all the key values to the "ids" query parameter
    else
    {
      addParam(RestConstants.QUERY_BATCH_IDS_PARAM, ids);
    }
  }
  
  /**
   * If any complex parameters have been added, add them to query parameters using
   * different naming and semantics as defined in {@link QueryParamsDataMap}
   */
  protected final void addComplexParams()
  {
    if (!_keysDataList.isEmpty())
    {
      _queryParamsDataMap.put(RestConstants.QUERY_BATCH_IDS_PARAM, _keysDataList);
    }
    // Resolves paths in the DataMap into individual query string parameters.
    Map<String, String> queryStringParams =
        QueryParamsDataMap.queryString(_queryParamsDataMap);

    for (Entry<String, String> entry : queryStringParams.entrySet())
    {
      addParam(entry.getKey(), entry.getValue());
    }
  }
  
  /**
   * Add an individual key to the DataList of keys, which will be later resolved into a collection
   * of individual query parameters.
   */
  protected final void addComplexKey(ComplexResourceKey<?, ?> id)
  {
    RecordTemplate key = id.getKey();
    RecordTemplate params = id.getParams();
    DataMap keyDataMap = new DataMap(key.data());
    if (params != null)
    {
      keyDataMap.put(RestConstants.COMPLEX_KEY_PARAMS, params.data());
    }
    _keysDataList.add(keyDataMap);
  }
  
  /**
   * Depending on whether or not this is a complex resource key, return either a full (key + params)
   * string representation of the key (which for ComplexResource includes both key and params), or 
   * simply key.toString() (which for ComplexResourceKey only uses key part).
   */
  protected String keyToString(K key)
  {
    if (key instanceof ComplexResourceKey) {
      return ((ComplexResourceKey<?,?>)key).toStringFull(); 
    }
    else
    {
      return key.toString();
    }
  }
  
  /**
   * Helper method to convert a collection of key Objects into a set of strings
   */
  protected static Set<String> toStringSet(Collection<?> ids)
  {
    Set<String> idStrings = new HashSet<String>(ids.size());
    for (Object id : ids)
    {
      if (id == null)
      {
        throw new IllegalArgumentException("null id");
      }
      idStrings.add(id.toString());
    }
    return idStrings;
  }
  
  /** given a iterable of objects returns a collection of (non-null) toStrings */
  private static Collection<String> stringify(Iterable<?> values)
  {
    assert values != null;
    Collection<String> strings =
        new ArrayList<String>(values instanceof Collection ? ((Collection<?>) values).size() : 10);
    for (Object value : values)
    {
      if (value != null)
      {
        strings.add(stringifySimpleValue(value));
      }
    }
    return strings;
  }

  private static String stringifySimpleValue(Object value)
  {
    Class<?> valueClass = value.getClass();
    if (DataTemplateUtil.hasCoercer(valueClass))
    {
      @SuppressWarnings("unchecked")
      Class<Object> fromClass = (Class<Object>) value.getClass();
      return DataTemplateUtil.coerceInput(value, fromClass, Object.class).toString();
    }
    return value.toString();
  }

  /** given an array of primitives returns a collection of strings */
  private static Collection<String> stringify(Object array)
  {
    assert array != null && array.getClass().isArray();
    int len = Array.getLength(array);
    Collection<String> strings = new ArrayList<String>(len);
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

  protected void addKey(String key, Object value)
  {
    if (value == null)
    {
      throw new IllegalArgumentException("Path key must be non-null");
    }
    _keys.put(key, value.toString());
  }

  protected void addAssocKey(String key, Object value)
  {
    _assocKey.append(key, value);
  }

  protected void appendQueryParams(UriBuilder b)
  {
    addComplexParams();
    if (! _queryParams.isEmpty())
    {
      List<String> keyList = new ArrayList<String>(_queryParams.keySet());
      Collections.sort(keyList);

      for (String key: keyList)
      {
        List<String> strings = new ArrayList<String>(_queryParams.get(key));
        Collections.sort(strings);
        for (String string : strings)
        {
          // force full encoding as UriBuilder.queryParam(..) won't encode percent signs followed by hex digits
          b.queryParam(URIUtil.encodeForQueryParam(key), URIUtil.encodeForQueryParam(string));
        }
      }
    }
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
      if (! _keys.containsKey(key))
      {
        throw new IllegalStateException("Missing path key: '" + key + "'");
      }
    }
    return URI.create(template.createURI(_keys));
  }

  protected void addFields(PathSpec... fieldPaths)
  {
    if (_queryParams.containsKey(RestConstants.FIELDS_PARAM))
    {
      throw new IllegalStateException("Fields already set on this request: " + _queryParams.get(RestConstants.FIELDS_PARAM));
    }

    _fields = new HashSet<PathSpec>(Arrays.asList(fieldPaths));
    addParam(RestConstants.FIELDS_PARAM, URIUtil.encodeFields(fieldPaths));
  }
  
  protected boolean isComplexKeyResource()
  {
    return _resourceSpec.getKeyClass() == ComplexResourceKey.class;
  }

}
