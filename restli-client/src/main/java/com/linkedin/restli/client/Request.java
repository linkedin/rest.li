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


import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.jersey.api.uri.UriTemplate;
import com.linkedin.restli.common.HttpMethod;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceProperties;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.client.RestResponseDecoder;
import com.linkedin.restli.internal.common.ResourcePropertiesImpl;
import com.linkedin.restli.internal.common.URIParamUtils;

import java.net.HttpCookie;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;


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

  private final ResourceMethod              _method;
  private final RecordTemplate              _inputRecord;
  private final RestResponseDecoder<T>      _decoder;
  private final Map<String, String>         _headers;
  private final List<HttpCookie>            _cookies;
  private final ResourceSpec                _resourceSpec;
  private final ResourceProperties          _resourceProperties;
  private final Map<String, Object>         _queryParams;
  private final Map<String, Class<?>>       _queryParamClasses; // Used for coercing query params. In case of collection or iterable, contains the type parameter class.
  private final String                      _methodName; // needed to identify finders and actions. null for everything else
  private final String                      _baseUriTemplate;
  private final Map<String, Object>         _pathKeys;
  private final List<Object>                _streamingAttachments; //Usually null since streaming is rare. Creating an empty List is wasteful.
  private RestliRequestOptions              _requestOptions;

  Request(ResourceMethod method,
          RecordTemplate inputRecord,
          Map<String, String> headers,
          List<HttpCookie> cookies,
          RestResponseDecoder<T> decoder,
          ResourceSpec resourceSpec,
          Map<String, Object> queryParams,
          Map<String, Class<?>> queryParamClasses,
          String methodName,
          String baseUriTemplate,
          Map<String, Object> pathKeys,
          RestliRequestOptions requestOptions,
          List<Object> streamingAttachments)
  {
    _method = method;
    _inputRecord = inputRecord;
    _decoder = decoder;
    _headers = headers;
    _cookies = cookies;
    _resourceSpec = resourceSpec;

    if (resourceSpec != null)
    {
      _resourceProperties = new ResourcePropertiesImpl(resourceSpec.getSupportedMethods(),
                                                       resourceSpec.getKeyType(),
                                                       resourceSpec.getComplexKeyType(),
                                                       resourceSpec.getValueType(),
                                                       resourceSpec.getKeyParts());
    }
    else
    {
      _resourceProperties = null;
    }

    _queryParams = queryParams;
    _queryParamClasses = queryParamClasses;
    _methodName = methodName;
    _baseUriTemplate = baseUriTemplate;
    _pathKeys = pathKeys;

    if (_baseUriTemplate != null && _pathKeys != null)
    {
      validatePathKeys();
    }

    _requestOptions = (requestOptions == null) ? RestliRequestOptions.DEFAULT_OPTIONS : requestOptions;
    _streamingAttachments = streamingAttachments;
  }

  /**
   * Validates that a key is present on the request for a resource that requires one, and is absent otherwise.
   * @param key the key
   */
  protected void validateKeyPresence(Object key)
  {
    if (getResourceProperties().isKeylessResource())
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

  public String getMethodName()
  {
    return _methodName;
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
   * Get cookie straightly from the request
   *
   * @return cookies
   */
  public List<HttpCookie> getCookies()
  {
    return _cookies;
  }

  public RecordTemplate getInputRecord()
  {
    return _inputRecord;
  }

  public RestResponseDecoder<T> getResponseDecoder()
  {
    return _decoder;
  }

  /**
   * @deprecated Please use {@link #getResourceProperties()} instead.
   */
  @Deprecated
  public ResourceSpec getResourceSpec()
  {
    // When this method is removed, this class should be constructed solely from resource properties.
    return _resourceSpec;
  }

  public ResourceProperties getResourceProperties()
  {
    return _resourceProperties;
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

  public Map<String, Object> getQueryParamsObjects()
  {
    return _queryParams;
  }

  public Map<String, Class<?>> getQueryParamClasses()
  {
    return _queryParamClasses;
  }

  public RestliRequestOptions getRequestOptions()
  {
    return _requestOptions;
  }

  public void setRequestOptions(RestliRequestOptions requestOptions)
  {
    _requestOptions = (requestOptions == null) ? RestliRequestOptions.DEFAULT_OPTIONS : requestOptions;
  }

  /**
   * @return True if the request is streaming, false otherwise.
   */
  public boolean isStreaming()
  {
    return _streamingAttachments != null || _requestOptions.getAcceptResponseAttachments();
  }

  List<Object> getStreamingAttachments()
  {
    return _streamingAttachments;
  }

  /**
   * This method is to be exposed in the extending classes when appropriate
   */
  protected Set<PathSpec> getFields()
  {
    @SuppressWarnings("unchecked")
    Set<PathSpec> fieldsSet = (Set<PathSpec>) _queryParams.get(RestConstants.FIELDS_PARAM);
    if (fieldsSet == null)
    {
      return Collections.emptySet();
    }
    return fieldsSet;
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

    return areNewFieldsEqual((Request<?>) obj);
  }

  /**
   * Checks if the old fields are equal
   *
   * @param other
   * @return
   */
  private boolean areOldFieldsEqual(Request<?> other)
  {
    if (_headers != null ? !_headers.equals(other._headers) : other._headers != null)
    {
      return false;
    }
    if (_inputRecord != null ? !_inputRecord.equals(other._inputRecord) : other._inputRecord != null)
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
    if (_baseUriTemplate != null ? !_baseUriTemplate.equals(other._baseUriTemplate) : other._baseUriTemplate != null)
    {
      return false;
    }
    if (_pathKeys != null ? !_pathKeys.equals(other._pathKeys) : other._pathKeys != null)
    {
      return false;
    }
    if (_resourceSpec != null ? !_resourceSpec.equals(other._resourceSpec) : other._resourceSpec != null)
    {
      return false;
    }
    if (_queryParams != null ? !_queryParams.equals(other._queryParams) : other._queryParams != null)
    {
      return false;
    }
    if (_methodName != null ? !_methodName.equals(other._methodName) : other._methodName != null)
    {
      return false;
    }
    if (_requestOptions != null ? !_requestOptions.equals(other._requestOptions) : other._requestOptions != null)
    {
      return false;
    }
    if (_streamingAttachments != null ? !_streamingAttachments.equals(other._streamingAttachments) : other._streamingAttachments != null)
    {
      return false;
    }
    if (_cookies != null ? !_cookies.equals(other._cookies) : other._cookies != null)
    {
      return false;
    }

    return true;
  }

  /**
   * Computes the hashCode using the new fields
   * @return
   */
  @Override
  public int hashCode()
  {
    int hashCode = _method.hashCode();
    hashCode = 31 * hashCode + (_inputRecord != null ? _inputRecord.hashCode() : 0);
    hashCode = 31 * hashCode + (_headers != null ? _headers.hashCode() : 0);
    hashCode = 31 * hashCode + (_baseUriTemplate != null ? _baseUriTemplate.hashCode() : 0);
    hashCode = 31 * hashCode + (_pathKeys != null ? _pathKeys.hashCode() : 0);
    hashCode = 31 * hashCode + (_resourceSpec != null ? _resourceSpec.hashCode() : 0);
    hashCode = 31 * hashCode + (_queryParams != null ? _queryParams.hashCode() : 0);
    hashCode = 31 * hashCode + (_methodName != null ? _methodName.hashCode() : 0);
    hashCode = 31 * hashCode + (_requestOptions != null ? _requestOptions.hashCode() : 0);
    hashCode = 31 * hashCode + (_streamingAttachments != null ? _streamingAttachments.hashCode() : 0);
    hashCode = 31 * hashCode + (_cookies != null ? _cookies.hashCode() : 0);
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
    sb.append(", _baseUriTemplate=").append(_baseUriTemplate);
    sb.append(", _methodName=").append(_methodName);
    sb.append(", _pathKeys=").append(_pathKeys);
    sb.append(", _queryParams=").append(_queryParams);
    sb.append(", _requestOptions=").append(_requestOptions);
    sb.append(", _cookies=").append(_cookies);
    if (_streamingAttachments != null)
    {
      sb.append(", _streamingDataSources=");
      sb.append("(size=").append(_streamingAttachments.size()).append(")");
    }
    sb.append('}');
    return sb.toString();
  }

  /**
   * This method produces a string representation of this request by using only the data that cannot have
   * personally identifiable information(PII) or security sensitive content.
   * @return A representative string for this request free of PII and security sensitive content.
   */
  public String toSecureString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getName());
    sb.append("{_method=").append(_method);
    sb.append(", _baseUriTemplate=").append(_baseUriTemplate);
    sb.append(", _methodName=").append(_methodName);
    sb.append(", _requestOptions=").append(_requestOptions);
    sb.append('}');
    return sb.toString();
  }
}
