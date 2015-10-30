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
package com.linkedin.r2.message;


import com.linkedin.r2.message.rest.RestMethod;
import com.linkedin.r2.transport.http.common.HttpConstants;
import com.linkedin.util.ArgumentUtil;

import java.net.URI;


/**
 * @author Chris Pettitt
 * @author Zhenkai Zhu
 * @version $Revision$
 */
public abstract class BaseRequestBuilder<B extends BaseRequestBuilder<B>>
        extends BaseMessageBuilder<B>
        implements MessageHeadersBuilder<B>
{
  private URI _uri;

  private String _method = RestMethod.GET;

  /**
   * Constructs a new builder using the given uri.
   *
   * @param uri the URI for the resource involved in the request
   */
  public BaseRequestBuilder(URI uri)
  {
    setURI(uri);
  }

  /**
   * Copies the values from the supplied request. Changes to this builder will not be reflected
   * in the original message.
   *
   * @param request the request to copy
   */
  public BaseRequestBuilder(Request request)
  {
    super(request);

    setURI(request.getURI());
    setMethod(request.getMethod());
  }

  public URI getURI()
  {
    return _uri;
  }

  public B setURI(URI uri)
  {
    ArgumentUtil.notNull(uri, "uri");

    _uri = uri;
    return thisBuilder();
  }

  /**
   * Sets the REST method for this request.
   *
   * @param method the REST method to set
   * @return this builder
   * @see com.linkedin.r2.message.rest.RestMethod
   */
  public B setMethod(String method)
  {
    ArgumentUtil.notNull(method, "method");

    _method = method;
    return thisBuilder();
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
