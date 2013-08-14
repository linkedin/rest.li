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

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.HttpMethod;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.client.RestResponseDecoder;
import com.linkedin.restli.internal.common.IllegalMaskException;
import com.linkedin.restli.internal.common.URIMaskUtil;


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

  private final URI                    _uri;
  private final ResourceMethod         _method;
  private final RecordTemplate         _input;
  private final RestResponseDecoder<T> _decoder;
  private final Map<String, String>    _headers;
  private final ResourceSpec           _resourceSpec;
  private final DataMap                _queryParams;
  private final List<String>           _resourcePath;
  private final String                 _methodName; // needed to identify finders and actions. null for everything else

  public Request(URI uri,
                 ResourceMethod method,
                 RecordTemplate input,
                 Map<String, String> headers,
                 RestResponseDecoder<T> decoder,
                 ResourceSpec resourceSpec)
  {
    this(uri, method, input, headers, decoder, resourceSpec, Collections.<String>emptyList());
  }

  public Request(URI uri,
                 ResourceMethod method,
                 RecordTemplate input,
                 Map<String, String> headers,
                 RestResponseDecoder<T> decoder,
                 ResourceSpec resourceSpec,
                 DataMap queryParams)
  {
    this(uri, method, input, headers, decoder, resourceSpec, queryParams, Collections.<String>emptyList());
  }

  public Request(URI uri,
                 ResourceMethod method,
                 RecordTemplate input,
                 Map<String, String> headers,
                 RestResponseDecoder<T> decoder,
                 ResourceSpec resourceSpec,
                 List<String> resourcePath)
  {
    this(uri, method, input, headers, decoder, resourceSpec, null, resourcePath);
  }

  public Request(URI uri,
                 ResourceMethod method,
                 RecordTemplate input,
                 Map<String, String> headers,
                 RestResponseDecoder<T> decoder,
                 ResourceSpec resourceSpec,
                 DataMap queryParams,
                 List<String> resourcePath)
  {
    this(uri, method, input, headers, decoder, resourceSpec, queryParams, resourcePath, null);
  }

  public Request(URI uri,
                 ResourceMethod method,
                 RecordTemplate input,
                 Map<String, String> headers,
                 RestResponseDecoder<T> decoder,
                 ResourceSpec resourceSpec,
                 DataMap queryParams,
                 List<String> resourcePath,
                 String methodName)
  {
    _uri = uri;
    _method = method;
    _input = input;
    _decoder = decoder;
    _headers = headers == null ? null : Collections.unmodifiableMap(headers);
    _resourceSpec = resourceSpec;

    if (queryParams == null)
    {
      _queryParams = null;
    }
    else
    {
      _queryParams = new DataMap(queryParams);
      _queryParams.makeReadOnly();
    }

    _resourcePath = resourcePath;
    _methodName = methodName;
  }

  public URI getUri()
  {
    return _uri;
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
   * @return the resource path parts as a list.
   */
  public List<String> getResourcePath()
  {
    return _resourcePath;
  }

  public ResourceMethod getMethod()
  {
    return _method;
  }

  public Map<String, String> getHeaders()
  {
    return _headers;
  }

  public RecordTemplate getInput()
  {
    return _input;
  }

  public RestResponseDecoder<T> getResponseDecoder()
  {
    return _decoder;
  }

  public ResourceSpec getResourceSpec()
  {
    return _resourceSpec;
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

  public DataMap getQueryParams()
  {
    return _queryParams;
  }

  /**
   * This method is to be exposed in the extending classes when appropriate
   */
  protected Set<PathSpec> getFields()
  {
    String fieldsString = (String) _queryParams.get(RestConstants.FIELDS_PARAM);
    if (fieldsString == null || fieldsString.trim().isEmpty())
    {
      return Collections.emptySet();
    }

    try
    {
      return URIMaskUtil.decodeMaskUriFormat(new StringBuilder(fieldsString))
                        .getOperations()
                        .keySet();
    }
    catch (IllegalMaskException e)
    {
      // Should never happen as the field value is formed by the framework code.
      throw new IllegalArgumentException("Invalid fields parameter value: "
          + fieldsString);
    }
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

    Request<?> request = (Request<?>) obj;

    if (_headers != null? !_headers.equals(request._headers) : request._headers != null)
    {
      return false;
    }
    if (_input != null? !_input.equals(request._input) : request._input != null)
    {
      return false;
    }
    if (_resourcePath != null? !_resourcePath.equals(request._resourcePath) : request._resourcePath != null)
    {
      return false;
    }
    if (_method != request._method)
    {
      return false;
    }

    return _uri.equals(request._uri);
  }

  @Override
  public int hashCode()
  {
    int result = _uri.hashCode();
    result = 31 * result + _method.hashCode();
    result = 31 * result + (_input != null? _input.hashCode() : 0);
    result = 31 * result + (_headers != null? _headers.hashCode() : 0);
    result = 31 * result + (_resourcePath != null? _resourcePath.hashCode() : 0);
    return result;
  }

  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getName());
    sb.append("{_headers=").append(_headers);
    sb.append(", _input=").append(_input);
    sb.append(", _method=").append(_method);
    sb.append(", _uri=").append(StringUtils.abbreviate(_uri.toString(), 256));
    sb.append('}');
    return sb.toString();
  }
}
