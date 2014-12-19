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

package com.linkedin.restli.client;


import com.linkedin.data.DataComplex;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.jersey.api.uri.UriTemplate;
import com.linkedin.restli.client.uribuilders.RestliUriBuilderUtil;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.HttpMethod;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.client.QueryParamsUtil;
import com.linkedin.restli.internal.client.RestResponseDecoder;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.URIParamUtils;

import java.lang.reflect.Array;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;


/**
 * A type-bound Request for a resource.
 *
 * @param <T> response entity template class
 *
 * @author Eran Leshem
 */
public class Request<T>
{
  private static final Pattern SLASH_PATTERN = Pattern.compile("/");

  /*
  A Request object should not have a URI anymore. The _uri and _hasUri fields are present here only because of
  older clients using one of the deprecated constructors that have a URI parameter. If one of these constructors are
  used we set the _uri field to the passed in URI and set _hasUri to true.

  Similarly, if someone calls the deprecated getUri method we will either
    (a) return _uri if _hasUri has been set to true
    (b) generate a uri using the RestliUriBuilderUtil, and store it in _uri

  These two fields will be removed in the future.
  */
  private URI                          _uri;
  private boolean                      _hasUri;

  private final ResourceMethod         _method;
  private final RecordTemplate         _inputRecord;
  private final RestResponseDecoder<T> _decoder;
  private final Map<String, String>    _headers;
  private final ResourceSpec           _resourceSpec;
  private final Map<String, Object>    _queryParams;
  private final String                 _methodName; // needed to identify finders and actions. null for everything else
  private final String                 _baseUriTemplate;
  private final Map<String, Object>    _pathKeys;
  private final RestliRequestOptions   _requestOptions;

  /**
   * @deprecated Please use {@link #Request(com.linkedin.restli.common.ResourceMethod, com.linkedin.data.template.RecordTemplate, java.util.Map, com.linkedin.restli.internal.client.RestResponseDecoder, com.linkedin.restli.common.ResourceSpec, java.util.Map, String, String, java.util.Map, RestliRequestOptions)}
   * @param uri
   * @param method
   * @param inputRecord
   * @param headers
   * @param decoder
   * @param resourceSpec
   */
  @Deprecated
  public Request(URI uri,
                 ResourceMethod method,
                 RecordTemplate inputRecord,
                 Map<String, String> headers,
                 RestResponseDecoder<T> decoder,
                 ResourceSpec resourceSpec)
  {
    this(uri, method, inputRecord, headers, decoder, resourceSpec, Collections.<String>emptyList());
  }

  /**
   * @deprecated Please use {@link #Request(com.linkedin.restli.common.ResourceMethod, com.linkedin.data.template.RecordTemplate, java.util.Map, com.linkedin.restli.internal.client.RestResponseDecoder, com.linkedin.restli.common.ResourceSpec, java.util.Map, String, String, java.util.Map, RestliRequestOptions)}
   * @param uri
   * @param method
   * @param inputRecord
   * @param headers
   * @param decoder
   * @param resourceSpec
   * @param queryParams
   */
  @Deprecated
  public Request(URI uri,
                 ResourceMethod method,
                 RecordTemplate inputRecord,
                 Map<String, String> headers,
                 RestResponseDecoder<T> decoder,
                 ResourceSpec resourceSpec,
                 DataMap queryParams)
  {
    this(uri, method, inputRecord, headers, decoder, resourceSpec, queryParams, Collections.<String>emptyList());
  }

  /**
   * @deprecated Please use {@link #Request(com.linkedin.restli.common.ResourceMethod, com.linkedin.data.template.RecordTemplate, java.util.Map, com.linkedin.restli.internal.client.RestResponseDecoder, com.linkedin.restli.common.ResourceSpec, java.util.Map, String, String, java.util.Map, RestliRequestOptions)}
   * @param uri
   * @param method
   * @param inputRecord
   * @param headers
   * @param decoder
   * @param resourceSpec
   * @param resourcePath
   */
  @Deprecated
  public Request(URI uri,
                 ResourceMethod method,
                 RecordTemplate inputRecord,
                 Map<String, String> headers,
                 RestResponseDecoder<T> decoder,
                 ResourceSpec resourceSpec,
                 List<String> resourcePath)
  {
    this(uri, method, inputRecord, headers, decoder, resourceSpec, null, resourcePath);
  }

  /**
   * @deprecated Please use {@link #Request(com.linkedin.restli.common.ResourceMethod, com.linkedin.data.template.RecordTemplate, java.util.Map, com.linkedin.restli.internal.client.RestResponseDecoder, com.linkedin.restli.common.ResourceSpec, java.util.Map, String, String, java.util.Map, RestliRequestOptions)}
   * @param uri
   * @param method
   * @param inputRecord
   * @param headers
   * @param decoder
   * @param resourceSpec
   * @param queryParams
   * @param resourcePath
   */
  @Deprecated
  public Request(URI uri,
                 ResourceMethod method,
                 RecordTemplate inputRecord,
                 Map<String, String> headers,
                 RestResponseDecoder<T> decoder,
                 ResourceSpec resourceSpec,
                 DataMap queryParams,
                 List<String> resourcePath)
  {
    this(uri, method, inputRecord, headers, decoder, resourceSpec, queryParams, resourcePath, null);
  }

  /**
   * @deprecated Please use {@link #Request(com.linkedin.restli.common.ResourceMethod, com.linkedin.data.template.RecordTemplate, java.util.Map, com.linkedin.restli.internal.client.RestResponseDecoder, com.linkedin.restli.common.ResourceSpec, java.util.Map, String, String, java.util.Map, RestliRequestOptions)}.
   *             Please take a look at the constructor for any of the subclasses of {@link com.linkedin.restli.client.Request} to see how this can be done. Example {@link com.linkedin.restli.client.GetRequest#GetRequest(java.util.Map, Class, Object, java.util.Map, com.linkedin.restli.common.ResourceSpec, String, java.util.Map, RestliRequestOptions)}
   * @param uri
   * @param method
   * @param inputRecord
   * @param headers
   * @param decoder
   * @param resourceSpec
   * @param queryParams
   * @param resourcePath
   * @param methodName
   */
  @Deprecated
  public Request(URI uri,
                 ResourceMethod method,
                 RecordTemplate inputRecord,
                 Map<String, String> headers,
                 RestResponseDecoder<T> decoder,
                 ResourceSpec resourceSpec,
                 DataMap queryParams,
                 List<String> resourcePath,
                 String methodName)
  {
    _method = method;
    _inputRecord = inputRecord;
    _headers = (headers == null) ? null : Collections.unmodifiableMap(headers);
    _decoder = decoder;
    _queryParams = (queryParams == null) ? null : Collections.unmodifiableMap(queryParams);
    _resourceSpec = resourceSpec;
    _methodName = methodName;
    _baseUriTemplate = null;
    _pathKeys = null;
    _uri = uri;
    _hasUri = true;
    _requestOptions = RestliRequestOptions.DEFAULT_OPTIONS;
  }

  Request(ResourceMethod method,
          RecordTemplate inputRecord,
          Map<String, String> headers,
          RestResponseDecoder<T> decoder,
          ResourceSpec resourceSpec,
          Map<String, Object> queryParams,
          String methodName,
          String baseUriTemplate,
          Map<String, Object> pathKeys,
          RestliRequestOptions requestOptions)
  {
    _method = method;
    _inputRecord = inputRecord;
    _decoder = decoder;
    _headers = headers == null ? null : Collections.unmodifiableMap(headers);
    _resourceSpec = resourceSpec;

    if (queryParams == null)
    {
      _queryParams = null;
    }
    else
    {
      _queryParams = getReadOnlyQueryParams(queryParams);
    }

    _methodName = methodName;

    _baseUriTemplate = baseUriTemplate;

    _pathKeys = (pathKeys == null) ? null : Collections.unmodifiableMap(pathKeys);

    _uri = null;
    _hasUri = false;

    if (_baseUriTemplate != null && _pathKeys != null)
    {
      validatePathKeys();
    }

    _requestOptions = (requestOptions == null) ? RestliRequestOptions.DEFAULT_OPTIONS : requestOptions;
  }

  /**
   * Validates that a key is present on the request for a resource that requires one, and is absent otherwise.
   * @param key the key
   */
  protected void validateKeyPresence(Object key)
  {
    if (getResourceSpec().isKeylessResource())
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
    }
  }

  /**
   * Converts the query params to read only.
   * @param queryParams the passed in query params
   * @return a read only version of the query params
   */
  private Map<String, Object> getReadOnlyQueryParams(Map<String, Object> queryParams)
  {
    for (Map.Entry<String, Object> entry: queryParams.entrySet())
    {
      String key = entry.getKey();
      Object value = entry.getValue();
      queryParams.put(key, getReadOnly(value));
    }
    return Collections.unmodifiableMap(queryParams);
  }

  /**
   * Returns a read only version of {@code value}
   * @param value the object we want to get a read only version of
   * @return a read only version of {@code value}
   */
  private Object getReadOnly(Object value)
  {
    if (value == null)
    {
      return null;
    }

    if (value instanceof Object[])
    {
      // array of non-primitives
      Object[] arr = (Object[])value;
      List<Object> list = new ArrayList<Object>(arr.length);
      for (Object o: arr)
      {
        list.add(getReadOnly(o));
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
    else if (value instanceof ComplexResourceKey)
    {
      ((ComplexResourceKey) value).makeReadOnly();
      return value;
    }
    else if (value instanceof CompoundKey)
    {
      ((CompoundKey) value).makeReadOnly();
      return value;
    }
    else if (value instanceof DataTemplate)
    {
      Object data = ((DataTemplate) value).data();
      if (data instanceof DataComplex)
      {
        ((DataComplex) data).makeReadOnly();
      }
      // we don't try to make other types of data read only.
      return value;
    }
    else if (value instanceof Iterable)
    {
      List<Object> list = new ArrayList<Object>();
      for (Object o: (Iterable)value)
      {
        list.add(getReadOnly(o));
      }
      return Collections.unmodifiableList(list);
    }

    return value;
  }

  /**
   * Validates that all path keys in the URI template are present. If not, an {@link IllegalStateException} is thrown.
   */
  private void validatePathKeys()
  {
    UriTemplate template = new UriTemplate(getBaseUriTemplate());
    for (String key: template.getTemplateVariables())
    {
      Object value = getPathKeys().get(key);
      if (value == null)
      {
        throw new IllegalStateException("Missing path key: " + key);
      }
    }
  }

  /**
   * @deprecated Requests are now built using {@link com.linkedin.restli.client.uribuilders.RestliUriBuilder}.
   * We do not recommend calling this method as URI generation is an expensive process and is dependent on the version
   * of Rest.li protocol being used.
   *
   * If you need a string representation of the URI information, use {@link com.linkedin.restli.client.util.RestliRequestUriSignature}.
   *
   * If you must generate a URI, please use {@link com.linkedin.restli.client.uribuilders.RestliUriBuilderUtil#createUriBuilder(Request, com.linkedin.restli.common.ProtocolVersion)}.
   * Note that the generated URI should not be used for making requests because it is tied to specific wire protocol version.
   *
   * @return the URI for this request.
   */
  @Deprecated
  public URI getUri()
  {
    if (_hasUri)
    {
      return _uri;
    }
    else
    {
      if (_uri == null)
      {
        // if someone calls this method w/o manually setting a URI in the constructor we will generate a URI using the
        // current default Rest.li version in the builders and cache it.
        _uri = RestliUriBuilderUtil.createUriBuilder(this).build();
      }
      return _uri;
    }
  }

  /**
   * THIS METHOD WILL BE REMOVED ONCE {@link #getUri()} has been removed.
   *
   * @return True if a legacy constructor was used to construct this {@link Request} object. False otherwise.
   */
  boolean hasUri()
  {
    return _hasUri;
  }

  public String getMethodName()
  {
    return _methodName;
  }

  /**
   * Returns the resource path parts as a list.
   *
   * The resource path of a root resource with a URI of "x/key1" has a resource is a list with one part: ["x"].
   *
   * The resource path of a sub-resource with a URI of "x/key1/y/key2" is a list with two parts: ["x", "y"].
   *
   * The resource path of a simple sub-resource with a URI of "x/key1/y/z/key2/t" is a list with
   * four parts: ["x", "y", "z", "t"].
   *
   * @deprecated Please use {@link #getPathKeys()} and {@link #getBaseUriTemplate()}
   *
   * @return the resource path parts as a list.
   */
  @Deprecated
  public List<String> getResourcePath()
  {
    UriTemplate template = new UriTemplate(_baseUriTemplate);
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

  public ResourceMethod getMethod()
  {
    return _method;
  }

  public Map<String, String> getHeaders()
  {
    return _headers;
  }

  /**
   * @deprecated Please use {@link #getInputRecord()} instead
   *
   * @return
   */
  @Deprecated
  public RecordTemplate getInput()
  {
    return _inputRecord;
  }

  public RecordTemplate getInputRecord()
  {
    return _inputRecord;
  }

  public RestResponseDecoder<T> getResponseDecoder()
  {
    return _decoder;
  }

  public ResourceSpec getResourceSpec()
  {
    return _resourceSpec;
  }

  public String getBaseUriTemplate()
  {
    return _baseUriTemplate;
  }

  public Map<String, Object> getPathKeys()
  {
    return _pathKeys;
  }

  /**
   * @see HttpMethod#isSafe()
   */
  public boolean isSafe()
  {
    return _method.getHttpMethod().isSafe();
  }

  /**
   * @see HttpMethod#isIdempotent()
   */
  public boolean isIdempotent()
  {
    return _method.getHttpMethod().isIdempotent();
  }

  /**
   * @deprecated Please use {@link #getQueryParamsObjects()} instead
   * @return
   */
  @Deprecated
  public DataMap getQueryParams()
  {
    return QueryParamsUtil.convertToDataMap(_queryParams);
  }

  public Map<String, Object> getQueryParamsObjects()
  {
    return _queryParams;
  }

  public RestliRequestOptions getRequestOptions()
  {
    return _requestOptions;
  }

  /**
   * This method is to be exposed in the extending classes when appropriate
   */
  protected Set<PathSpec> getFields()
  {
    @SuppressWarnings("unchecked")
    List<PathSpec> fieldsList = (List<PathSpec>) _queryParams.get(RestConstants.FIELDS_PARAM);
    if (fieldsList == null)
    {
      return Collections.emptySet();
    }
    return new HashSet<PathSpec>(fieldsList);
  }

  /**
   * Get the name of the service for this request
   * @return the service name for this request
   */
  String getServiceName()
  {
    if (_baseUriTemplate != null)
    {
      return URIParamUtils.extractPathComponentsFromUriTemplate(_baseUriTemplate)[0];
    }
    return "";
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
    {
      return true;
    }

    if (obj == null || getClass() != obj.getClass())
    {
      return false;
    }

    Request<?> other = (Request<?>) obj;

    if (_hasUri && other._hasUri)
    {
      // both requests were constructed using the old constructor
      return areOldFieldsEqual(other) && _uri.equals(other._uri);
    }

    if (_hasUri || other._hasUri)
    {
      // if one of them was constructed using the new while the other was constructed using the old constructor we
      // assume that they are not equal.
      return false;
    }

    return areNewFieldsEqual(other);
  }

  /**
   * Checks if the old fields are equal
   *
   * @param other
   * @return
   */
  private boolean areOldFieldsEqual(Request<?> other)
  {
    if (_headers != null? !_headers.equals(other._headers) : other._headers != null)
    {
      return false;
    }
    if (_inputRecord != null? !_inputRecord.equals(other._inputRecord) : other._inputRecord != null)
    {
      return false;
    }
    if (_method != other._method)
    {
      return false;
    }
    return true;
  }

  /**
   * Checks if the new fields are equal
   *
   * @param other
   * @return
   */
  private boolean areNewFieldsEqual(Request<?> other)
  {
    if (!areOldFieldsEqual(other))
    {
      return false;
    }
    if (_baseUriTemplate != null? !_baseUriTemplate.equals(other._baseUriTemplate) : other._baseUriTemplate != null)
    {
      return false;
    }
    if (_pathKeys != null? !_pathKeys.equals(other._pathKeys) : other._pathKeys != null)
    {
      return false;
    }
    if (_resourceSpec != null? !_resourceSpec.equals(other._resourceSpec) : other._resourceSpec != null)
    {
      return false;
    }
    if (_queryParams != null? !_queryParams.equals(other._queryParams) : other._queryParams != null)
    {
      return false;
    }
    if (_methodName != null? !_methodName.equals(other._methodName) : other._methodName != null)
    {
      return false;
    }
    if (_requestOptions != null? !_requestOptions.equals(other._requestOptions) : other._requestOptions != null)
    {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode()
  {
    int hashCode;
    if (_hasUri)
    {
      // request was constructed using an old constructor
      hashCode = _uri.hashCode();
      hashCode = (hashCode * 31) + oldHashCode();
    }
    else
    {
      hashCode = newHashCode();
    }
    return hashCode;
  }

  /**
   * Computes the hashCode using the old fields
   * @return
   */
  private int oldHashCode()
  {
    int result = _method.hashCode();
    result = 31 * result + (_inputRecord != null? _inputRecord.hashCode() : 0);
    result = 31 * result + (_headers != null? _headers.hashCode() : 0);
    return result;
  }

  /**
   * Computes the hashCode using the new fields
   * @return
   */
  private int newHashCode()
  {
    int hashCode = oldHashCode();
    hashCode = 31 * hashCode + (_baseUriTemplate != null? _baseUriTemplate.hashCode() : 0);
    hashCode = 31 * hashCode + (_pathKeys != null? _pathKeys.hashCode() : 0);
    hashCode = 31 * hashCode + (_resourceSpec != null ? _resourceSpec.hashCode() : 0);
    hashCode = 31 * hashCode + (_queryParams != null ? _queryParams.hashCode() : 0);
    hashCode = 31 * hashCode + (_methodName != null ? _methodName.hashCode() : 0);
    hashCode = 31 * hashCode + (_requestOptions != null ? _requestOptions.hashCode() : 0);
    return hashCode;
  }

  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getName());
    sb.append("{_headers=").append(_headers);
    sb.append(", _input=").append(_inputRecord);
    sb.append(", _method=").append(_method);
    if (_hasUri)
    {
      // request was constructed using an old constructor
      sb.append(", _uri=").append(StringUtils.abbreviate(_uri.toString(), 256));
      sb.append("}");
    }
    else
    {
      sb.append(", _baseUriTemplate=").append(_baseUriTemplate);
      sb.append(", _methodName=").append(_methodName);
      sb.append(", _pathKeys=").append(_pathKeys);
      sb.append(", _queryParams=").append(_queryParams);
      sb.append(", _requestOptions=").append(_requestOptions);
      sb.append('}');
    }
    return sb.toString();
  }
}
