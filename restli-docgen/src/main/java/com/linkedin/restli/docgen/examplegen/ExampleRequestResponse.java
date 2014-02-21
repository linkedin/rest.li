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

package com.linkedin.restli.docgen.examplegen;

import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;


/**
 * @author jbetz@linkedin.com
 */
public class ExampleRequestResponse {
  private final RestRequest _request;
  private final RestResponse _response;

  public ExampleRequestResponse(RestRequest request, RestResponse response) {
    _request = request;
    _response = response;
  }

  public RestRequest getRequest() {
    return _request;
  }

  public RestResponse getResponse() {
    return _response;
  }
}
