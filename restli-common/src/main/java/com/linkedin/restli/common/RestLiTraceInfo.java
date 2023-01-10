/*
   Copyright (c) 2022 LinkedIn Corp.

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

package com.linkedin.restli.common;

import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;


/**
 * This class exposes RestLi trace data within the requestContext to aid in request tracing.
 */
public class RestLiTraceInfo {
  private final String _requestTarget;
  private final String _requestOperation;
  private final String _baseUriTemplate;
  private final String _resourceMethodIdentifier;

  public static void inject(RequestContext requestContext, String requestTarget, String requestOperation,
      String baseUriTemplate, String resourceMethodIdentifier) {
    requestContext.putLocalAttr(R2Constants.RESTLI_TRACE_INFO,
        new RestLiTraceInfo(requestTarget, baseUriTemplate, resourceMethodIdentifier, requestOperation));
  }

  public static RestLiTraceInfo from(RequestContext requestContext) {
    return (RestLiTraceInfo) requestContext.getLocalAttr(R2Constants.RESTLI_TRACE_INFO);
  }

  private RestLiTraceInfo(String requestTarget, String baseUriTemplate, String resourceMethodIdentifier, String requestOperation) {
    _requestTarget = requestTarget;
    _requestOperation = requestOperation;
    _baseUriTemplate = baseUriTemplate;
    _resourceMethodIdentifier = resourceMethodIdentifier;
  }

  public String getRequestTarget() {
    return _requestTarget;
  }

  public String getRequestOperation() {
    return _requestOperation;
  }

  public String getBaseUriTemplate() {
    return _baseUriTemplate;
  }

  public String getResourceMethodIdentifier() {
    return _resourceMethodIdentifier;
  }
}
