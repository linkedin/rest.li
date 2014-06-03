/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.client.base;


import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.internal.common.URIParamUtils;


/**
 * Base class for all generated builders.
 *
 * @author Keren Jin
 */
public class BuilderBase
{
  protected RestliRequestOptions _requestOptions;
  private final String _baseUriTemplate;

  protected BuilderBase(String baseUriTemplate, RestliRequestOptions requestOptions)
  {
    _baseUriTemplate = baseUriTemplate;
    _requestOptions = assignRequestOptions(requestOptions);
  }

  /**
   * Extract path components from baseUriTemplate. All template variables are ignored.
   * Leading and trailing slashes are ignored.
   *
   * For example, if baseUriTemplate is "/foo/{key1}/bar/{key2}/baz/", the returned array is
   * { "foo", "bar", "baz" }
   *
   * @return path components of baseUriTemplate
   */
  public String[] getPathComponents()
  {
    return URIParamUtils.extractPathComponentsFromUriTemplate(_baseUriTemplate);
  }

  /**
   * @return current {@link RestliRequestOptions} value
   */
  public RestliRequestOptions getRequestOptions()
  {
    return _requestOptions;
  }

  protected String getBaseUriTemplate()
  {
    return _baseUriTemplate;
  }

  private static RestliRequestOptions assignRequestOptions(RestliRequestOptions requestOptions)
  {
    if (requestOptions == null)
    {
      return RestliRequestOptions.DEFAULT_OPTIONS;
    }
    else
    {
      return requestOptions;
    }
  }
}
