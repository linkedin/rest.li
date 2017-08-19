/*
   Copyright (c) 2016 LinkedIn Corp.

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

package com.linkedin.restli.internal.server.response;


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.server.RestLiServiceException;


/**
 * Contains response data for {@link ResourceMethod#GET}.
 *
 * @author gye
 */
public class GetResponseEnvelope extends RecordResponseEnvelope
{
  /**
   * Instantiates a get response envelope.
   *
   * @param status   Status of the response.
   * @param response Entity of the response.
   */
  GetResponseEnvelope(HttpStatus status, RecordTemplate response)
  {
    super(status, response);
  }

  GetResponseEnvelope(RestLiServiceException exception)
  {
    super(exception);
  }

  @Override
  public ResourceMethod getResourceMethod()
  {
    return ResourceMethod.GET;
  }
}
