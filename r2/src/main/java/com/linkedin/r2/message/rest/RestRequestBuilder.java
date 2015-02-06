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

/* $Id$ */
package com.linkedin.r2.message.rest;


import com.linkedin.r2.message.RequestBuilder;
import com.linkedin.r2.transport.http.common.HttpConstants;
import com.linkedin.util.ArgumentUtil;

import java.net.URI;


/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public final class RestRequestBuilder
        extends BaseRestMessageBuilder<RestRequestBuilder>
        implements RequestBuilder<RestRequestBuilder>, RestMessageBuilder<RestRequestBuilder>
{
  private URI _uri;

  private String _method = RestMethod.GET;

  /**
   * Constructs a new builder using the given uri.
   *
   * @param uri the URI for the resource involved in the request
   */
  public RestRequestBuilder(URI uri)
  {
    setURI(uri);
  }

  /**
   * Copies the values from the supplied request. Changes to this builder will not be reflected
   * in the original message.
   *
   * @param request the request to copy
   */
  public RestRequestBuilder(RestRequest request)
  {
    super(request);

    setURI(request.getURI());
    setMethod(request.getMethod());
  }

  @Override
  public URI getURI()
  {
    return _uri;
  }

  @Override
  public RestRequestBuilder setURI(URI uri)
  {
    ArgumentUtil.notNull(uri, "uri");

    _uri = uri;
    return this;
  }

  /**
   * Sets the REST method for this request.
   *
   * @param method the REST method to set
   * @return this builder
   * @see com.linkedin.r2.message.rest.RestMethod
   */
  public RestRequestBuilder setMethod(String method)
  {
    ArgumentUtil.notNull(method, "method");

    _method = method;
    return this;
  }

  /**
   * Returns the REST method for this request.
   *
   * @return the REST method for this request
   * @see com.linkedin.r2.message.rest.RestMethod
   */
  public String getMethod()
  {
    return _method;
  }

  @Override
  public RestRequest build()
  {
    return new RestRequestImpl(getEntity(), getHeaders(), getCookies(), getURI(), getMethod());
  }

  @Override
  public RestRequest buildCanonical()
  {
    return new RestRequestImpl(
        getEntity(), getCanonicalHeaders(), getCanonicalCookies(), getURI().normalize(), getMethod());
  }

  @Override
  protected void validateCookieHeader(String name)
  {
    if (name.equalsIgnoreCase(HttpConstants.REQUEST_COOKIE_HEADER_NAME))
    {
      String message = String.format(
          "Header %s are not allowed to be added as a request header.",
          HttpConstants.REQUEST_COOKIE_HEADER_NAME);
      throw new IllegalArgumentException(message);
    }
  }
}
