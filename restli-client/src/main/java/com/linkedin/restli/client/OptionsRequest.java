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
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.internal.client.OptionsResponseDecoder;

import java.net.HttpCookie;
import java.util.List;
import java.util.Map;


/**
 * A request for fetching a resources interface definition and data schemas.
 *
 * @author jbetz
 */
public class OptionsRequest extends Request<OptionsResponse>
{
  public OptionsRequest(Map<String, String> headers,
                        List<HttpCookie> cookies,
                        Map<String, Object> queryParams,
                        Map<String, Class<?>> queryParamClasses,
                        ResourceSpec resourceSpec,
                        String baseUriTemplate,
                        Map<String, Object> pathKeys,
                        RestliRequestOptions requestOptions)
  {
    super(ResourceMethod.OPTIONS,
          null,
          headers,
          cookies,
          new OptionsResponseDecoder(),
          resourceSpec,
          queryParams,
          queryParamClasses,
          null,
          baseUriTemplate,
          pathKeys,
          requestOptions,
          null);
  }
}
