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


import com.linkedin.data.DataComplex;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.internal.common.util.CollectionUtils;
import com.linkedin.restli.client.base.BuilderBase;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.util.ArgumentUtil;

import java.lang.reflect.Array;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


/**
 * @author Josh Walker
 * @version $Revision: $
 */

public abstract class AbstractRequestBuilder<K, V, R extends Request<?>> extends BuilderBase implements RequestBuilder<R>
{
  protected static final char         HEADER_DELIMITER = ',';

  protected final ResourceSpec        _resourceSpec;

  private Map<String, String>         _headers     = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
  private List<HttpCookie>            _cookies     = new ArrayList<HttpCookie>();
  private final Map<String, Object>   _queryParams = new HashMap<String, Object>();
  private final Map<String, Class<?>> _queryParamClasses = new HashMap<String, Class<?>>();
  private final Map<String, Object>   _pathKeys    = new HashMap<String, Object>();
  private final CompoundKey           _assocKey    = new CompoundKey();

  protected AbstractRequestBuilder(String baseUriTemplate, ResourceSpec resourceSpec, RestliRequestOptions requestOptions)
  {
    super(baseUriTemplate, requestOptions);
    _resourceSpec = resourceSpec;
    _requestOptions = requestOptions;
  }

  /**
   * Create a header with the specified value if there is no existing name.
   * Otherwise, append the specified value to the existing value, delimited by comma
   *
   * @param name name of the header
   * @param value value of the header. If null, this method is no-op.
   */
  public AbstractRequestBuilder<K, V, R> addHeader(String name, String value)
  {
    if (value != null)
    {
      final String currValue = _headers.get(name);
      final String newValue = currValue == null ? value : currValue + HEADER_DELIMITER + value;
      _headers.put(name, newValue);
    }

    return this;
  }

  /**
   * Create a header with the specified value if there is no existing name
   * Otherwise, overwrite the existing header with the specified value to the existing value
   *
   * @param name name of the header
   * @param value value of the header
   */
  public AbstractRequestBuilder<K, V, R> setHeader(String name, String value)
  {
    _headers.put(name, value);

    return this;
  }

  /**
   * Retrieves the value of the specified header
   * @param name The name of the header to return
   * @return The value of the specified header.
   */
  protected String getHeader(String name)
  {
    return _headers.get(name);
  }

  /**
   * Use the specified headers to replace the existing headers
   * All old headers will be lost
   *
   * @param headers new headers
   */
  public AbstractRequestBuilder<K, V, R> setHeaders(Map<String, String> headers)
  {
    _headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    _headers.putAll(headers);
    return this;
  }

  /**
   * Base class method for adding the cookies
   * @return a new builder reference with new cookie added
   * @param cookie
   */
  public AbstractRequestBuilder<K, V, R> addCookie(HttpCookie cookie)
  {
    if (cookie != null)
      _cookies.add(cookie);
    return this;
  }

  /**
   * Base class method for setting the cookies
   * @return a new builder reference with newly set cookie
   * @param cookies
   */
  public AbstractRequestBuilder<K, V, R> setCookies(List<HttpCookie> cookies)
  {
    for (HttpCookie cookie : cookies)
    {
      addCookie(cookie);
    }
    return this;
  }

  /**
   * Base class method for removing the cookies
   * @return a new builder reference with empty cookie
   */
  public AbstractRequestBuilder<K, V, R> clearCookies()
  {
    _cookies = new ArrayList<HttpCookie>();
    return this;
  }

  /**
   * Retrieve the cookies in the request
   * @return cookies
   */
  protected List<HttpCookie> getCookies()
  {
    return _cookies;
  }

  public AbstractRequestBuilder<K, V, R> setReqParam(String key, Object value)
  {
    ArgumentUtil.notNull(value, "value");
    return setParam(key, value);
  }

  public AbstractRequestBuilder<K, V, R> setReqParam(String key, Object value, Class<?> clazz)
  {
    ArgumentUtil.notNull(value, "value");
    return setParam(key, value, clazz);
  }

  public AbstractRequestBuilder<K, V, R> setParam(String key, Object value)
  {
    if (value == null)
    {
      return this;
    }
    return setParam(key, value, value.getClass());
  }

  public AbstractRequestBuilder<K, V, R> setParam(String key, Object value, Class<?> clazz)
  {
    if (value == null)
    {
      return this;
    }
    _queryParams.put(key, value);
    _queryParamClasses.put(key, clazz);
    return this;
  }

  public AbstractRequestBuilder<K, V, R> addReqParam(String key, Object value)
  {
    ArgumentUtil.notNull(value, "value");
    return addParam(key, value);
  }

  public AbstractRequestBuilder<K, V, R> addReqParam(String key, Object value, Class<?> clazz)
  {
    ArgumentUtil.notNull(value, "value");
    return addParam(key, value, clazz);
  }

  public AbstractRequestBuilder<K, V, R> addParam(String key, Object value)
  {
    if (value == null)
    {
      return this;
    }
    return addParam(key, value, value.getClass());
  }

  @SuppressWarnings("unchecked")
  public AbstractRequestBuilder<K, V, R> addParam(String key, Object value, Class<?> clazz)
  {
    if (value == null)
    {
      return this;
    }

    final Object existingData = _queryParams.get(key);
    if (existingData == null)
    {
      final Collection<Object> newData = new ArrayList<Object>();
      newData.add(value);
      setParam(key, newData);
    }
    else if (existingData instanceof Collection)
    {
      ((Collection<Object>) existingData).add(value);
    }
    else if (existingData instanceof Iterable)
    {
      final Collection<Object> newData = new ArrayList<Object>();
      for (Object d : (Iterable) existingData)
      {
        newData.add(d);
      }
      newData.add(value);
      setParam(key, newData);
    }
    else
    {
      throw new IllegalStateException("Query parameter is already set to non-iterable value. Reset with null value then add new query parameter.");
    }
    _queryParamClasses.put(key, clazz);

    return this;
  }

  public AbstractRequestBuilder<K, V, R> pathKey(String name, Object value)
  {
    _pathKeys.put(name, value);
    return this;
  }

  /**
   * Sets {@link RestliRequestOptions} for this {@link Request}.
   * This method overrides any {@link RestliRequestOptions} that have already been set for this {@link Request}.
   * The recommended way to use this method would be to get the original {@link RestliRequestOptions} using the
   * {@link #getRequestOptions()} method, creating a new {@link RestliRequestOptionsBuilder} using the
   * {@link RestliRequestOptionsBuilder#RestliRequestOptionsBuilder(RestliRequestOptions)} constructor, modifying
   * the option you want to change, calling the {@link com.linkedin.restli.client.RestliRequestOptionsBuilder#build()}
   * method and setting that as the {@link RestliRequestOptions} for this method.
   *
   * @param options
   * @return
   */
  public AbstractRequestBuilder<K, V, R> setRequestOptions(RestliRequestOptions options)
  {
    _requestOptions = options;
    return this;
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
      throw new IllegalArgumentException("null ids");
    }

    @SuppressWarnings("unchecked")
    Set<K> existingIds = (Set<K>) _queryParams.get(RestConstants.QUERY_BATCH_IDS_PARAM);
    if (existingIds == null)
    {
      existingIds = new HashSet<K>();
      _queryParams.put(RestConstants.QUERY_BATCH_IDS_PARAM, existingIds);
    }
    for (K id: ids)
    {
      if (id == null)
      {
        throw new IllegalArgumentException("Null key");
      }
      existingIds.add(id);
    }
  }

  protected boolean hasParam(String parameterName)
  {
    return _queryParams.containsKey(parameterName);
  }

  protected Object getParam(String parameterName)
  {
    return _queryParams.get(parameterName);
  }

  /**
   * Add an individual key to the DataList of keys, which will be later resolved into a collection
   * of individual query parameters.
   */
  protected final void addKey(K id)
  {
    addKeys(Collections.singletonList(id));
  }

  protected void addAssocKey(String key, Object value)
  {
    _assocKey.append(key, value);
  }

  protected void addFields(PathSpec... fieldPaths)
  {
    if (_queryParams.containsKey(RestConstants.FIELDS_PARAM))
    {
      throw new IllegalStateException("Entity projection fields already set on this request: "
                                          + _queryParams.get(RestConstants.FIELDS_PARAM));
    }
    setParam(RestConstants.FIELDS_PARAM, fieldPaths);
  }

  protected void addMetadataFields(PathSpec... fieldPaths)
  {
    if (_queryParams.containsKey(RestConstants.METADATA_FIELDS_PARAM))
    {
      throw new IllegalStateException("Metadata projection fields already set on this request: "
          + _queryParams.get(RestConstants.METADATA_FIELDS_PARAM));
    }
    setParam(RestConstants.METADATA_FIELDS_PARAM, fieldPaths);
  }

  protected void addPagingFields(PathSpec... fieldPaths)
  {
    if (_queryParams.containsKey(RestConstants.PAGING_FIELDS_PARAM))
    {
      throw new IllegalStateException("Paging projection fields already set on this request: "
          + _queryParams.get(RestConstants.PAGING_FIELDS_PARAM));
    }
    setParam(RestConstants.PAGING_FIELDS_PARAM, fieldPaths);
  }

  /**
   * Returns a read-only copy of the query parameters. It uses the original data where it is immutable.
   * @return a read only version of the query params
   */
  protected Map<String, Object> buildReadOnlyQueryParameters()
  {
    return getReadOnlyQueryParameters(_queryParams);
  }

  static protected Map<String, Object> getReadOnlyQueryParameters(Map<String, Object> queryParams)
  {
    try
    {
      Map<String, Object> readOnlyCopy = new HashMap<String, Object>
          (CollectionUtils.getMapInitialCapacity(queryParams.size(), 0.75f), 0.75f);
      for (Map.Entry<String, Object> entry: queryParams.entrySet())
      {
        String key = entry.getKey();
        Object value = entry.getValue();
        readOnlyCopy.put(key, getReadOnlyJavaObject(value));
      }

      return Collections.unmodifiableMap(readOnlyCopy);
    }
    catch (CloneNotSupportedException cloneException)
    {
      throw new IllegalArgumentException("Query parameters cannot be cloned.", cloneException);
    }
  }

  protected Map<String, Class<?>> getQueryParamClasses()
  {
    return _queryParamClasses;
  }

  /**
   * Returns a read-only copy of the path keys. It uses the original data where it is immutable.
   * @return a read only version of the path keys.
   */
  protected Map<String, Object> buildReadOnlyPathKeys()
  {
    return getReadOnlyPathKeys(_pathKeys);
  }

  static protected Map<String, Object> getReadOnlyPathKeys(Map<String, Object> pathKeys)
  {
    try
    {
      Map<String, Object> readOnlyCopy = new HashMap<String, Object>(
          CollectionUtils.getMapInitialCapacity(pathKeys.size(), 0.75f), 0.75f);
      for (Map.Entry<String, Object> entry: pathKeys.entrySet())
      {
        String key = entry.getKey();
        Object value = entry.getValue();
        readOnlyCopy.put(key, getReadOnlyOrCopyKeyObject(value));
      }

      return Collections.unmodifiableMap(readOnlyCopy);
    }
    catch (CloneNotSupportedException cloneException)
    {
      throw new IllegalArgumentException("Path keys cannot be cloned.", cloneException);
    }
  }

  protected <T extends DataTemplate<?>> T getReadOnlyOrCopyDataTemplate(T value) throws CloneNotSupportedException
  {
    return getReadOnlyOrCopyDataTemplateObject(value);
  }

  @SuppressWarnings("unchecked")
  static private <D extends DataTemplate<?>> D getReadOnlyOrCopyDataTemplateObject(D value) throws CloneNotSupportedException
  {
    if (value == null)
    {
      return null;
    }

    Object data = value.data();
    if (data instanceof DataComplex)
    {
      DataComplex dataComplex = (DataComplex) data;
      if (!dataComplex.isMadeReadOnly())
      {
        value = (D) value.copy();
        ((DataComplex) value.data()).makeReadOnly();
      }
    }

    return value;
  }

  protected K getReadOnlyOrCopyKey(K key) throws CloneNotSupportedException
  {
    return getReadOnlyOrCopyKeyObject(key);
  }

  @SuppressWarnings("unchecked")
  static private <Key> Key getReadOnlyOrCopyKeyObject(Key key) throws CloneNotSupportedException
  {
    if (key instanceof ComplexResourceKey)
    {
      ComplexResourceKey<?, ?> complexKey = ((ComplexResourceKey) key);

      if (!complexKey.isReadOnly())
      {
        complexKey = complexKey.copy();
        complexKey.makeReadOnly();
        key = (Key) complexKey;
      }
    }
    else if (key instanceof CompoundKey)
    {
      CompoundKey compoundKey = ((CompoundKey) key);

      if (!compoundKey.isReadOnly())
      {
        compoundKey = compoundKey.copy();
        compoundKey.makeReadOnly();
        key = (Key) compoundKey;
      }
    }

    return key;
  }

  /**
   * Returns a read only version of {@code value}
   * @param value the object we want to get a read only version of
   * @return a read only version of {@code value}
   */
  @SuppressWarnings("unchecked")
  private static Object getReadOnlyJavaObject(Object value) throws CloneNotSupportedException
  {
    if (value == null)
    {
      return null;
    }

    if (value instanceof Object[])
    {
      // array of non-primitives
      Object[] arr = (Object[]) value;
      List<Object> list = new ArrayList<Object>(arr.length);
      for (Object o: arr)
      {
        list.add(getReadOnlyJavaObject(o));
      }
      return Collections.unmodifiableList(list);
    }
    else if (value.getClass().isArray())
    {
      // array of primitives
      int length = Array.getLength(value);
      List<Object> list = new ArrayList<Object>();
      for (int i = 0; i < length; i++)
      {
        list.add(Array.get(value, i));
      }
      return Collections.unmodifiableList(list);
    }
    else if (value instanceof ComplexResourceKey || value instanceof CompoundKey)
    {
      return getReadOnlyOrCopyKeyObject(value);
    }
    else if (value instanceof DataTemplate)
    {
      return getReadOnlyOrCopyDataTemplateObject((DataTemplate) value);
    }
    else if (value instanceof Iterable)
    {
      List<Object> list = new ArrayList<Object>();
      for (Object o: (Iterable)value)
      {
        list.add(getReadOnlyJavaObject(o));
      }
      return Collections.unmodifiableList(list);
    }

    return value;
  }

  protected CompoundKey buildReadOnlyAssocKey()
  {
    try
    {
      return getReadOnlyOrCopyKeyObject(_assocKey);
    }
    catch (CloneNotSupportedException cloneException)
    {
      throw new IllegalArgumentException("Assoc keys cannot be cloned.", cloneException);
    }
  }

  protected Map<String, String> buildReadOnlyHeaders()
  {
    return getReadOnlyHeaders(_headers);
  }

  protected List<HttpCookie> buildReadOnlyCookies()
  {
    return getReadOnlyCookies(_cookies);
  }

  static protected Map<String, String> getReadOnlyHeaders(Map<String, String> headers)
  {
    Map<String, String> copyHeaders = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    copyHeaders.putAll(headers);
    return Collections.unmodifiableMap(copyHeaders);
  }

  static protected List<HttpCookie> getReadOnlyCookies(List<HttpCookie> cookies)
  {
    return Collections.unmodifiableList(cookies);
  }

  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getName());
    sb.append("{_assocKey=").append(_assocKey);
    sb.append(", _baseURITemplate='").append(getBaseUriTemplate()).append('\'');
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
