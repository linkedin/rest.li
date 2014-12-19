/*
   Copyright (c) 2013 LinkedIn Corp.

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


import com.linkedin.restli.common.OptionsResponse;


/**
 * Builds an OptionsRequest.
 *
 * @author jbetz
 */
public class OptionsRequestBuilder extends AbstractRequestBuilder<Void, OptionsResponse, OptionsRequest>
{
  /**
   * @deprecated Please use {@link #OptionsRequestBuilder(String, RestliRequestOptions)}
   * @param baseUriTemplate
   */
  @Deprecated
  public OptionsRequestBuilder(String baseUriTemplate)
  {
    this(baseUriTemplate, RestliRequestOptions.DEFAULT_OPTIONS);
  }

  public OptionsRequestBuilder(String baseUriTemplate, RestliRequestOptions requestOptions)
  {
    super(baseUriTemplate, null, requestOptions);
  }

  @Override
  public OptionsRequest build()
  {
    return new OptionsRequest(_headers,
                              _queryParams,
                              _resourceSpec,
                              getBaseUriTemplate(),
                              _pathKeys,
                              getRequestOptions());
  }
}
