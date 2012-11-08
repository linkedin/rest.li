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
import java.util.Map;
import java.util.Set;

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
  private final URI                    _uri;
  private final ResourceMethod         _method;
  private final RecordTemplate         _input;
  private final RestResponseDecoder<T> _decoder;
  private final Map<String, String>    _headers;
  private final ResourceSpec           _resourceSpec;
  private final DataMap                _queryParams;

  public Request(URI uri,
                 ResourceMethod method,
                 RecordTemplate input,
                 Map<String, String> headers,
                 RestResponseDecoder<T> decoder,
                 ResourceSpec resourceSpec)
  {
    this(uri, method, input, headers, decoder, resourceSpec, null);
  }

  public Request(URI uri,
                 ResourceMethod method,
                 RecordTemplate input,
                 Map<String, String> headers,
                 RestResponseDecoder<T> decoder,
                 ResourceSpec resourceSpec,
                 DataMap queryParams)
  {
    _uri = uri;
    _method = method;
    _input = input;
    _decoder = decoder;
    _headers = Collections.unmodifiableMap(headers);
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
  }

  public URI getUri()
  {
    return _uri;
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

    if (_input != null ? !_input.equals(request._input) : request._input != null)
    {
      return false;
    }

    if (_method != request._method)
    {
      return false;
    }

    if (!_decoder.equals(request._decoder))
    {
      return false;
    }

    return _uri.equals(request._uri);
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
  public int hashCode()
  {
    int result = _uri.hashCode();
    result = 31 * result + _method.hashCode();
    result = 31 * result + (_input != null ? _input.hashCode() : 0);
    result = 31 * result + _decoder.hashCode();
    return result;
  }

  @Override
  public String toString()
  {
    return "RequestImpl{" + "_input=" + _input + ", _uri=" + _uri + ", _method="
        + _method + ", _decoderEntityClass=" + _decoder.getEntityClass() + '}';
  }
}
