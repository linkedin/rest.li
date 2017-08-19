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


import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;

import com.linkedin.restli.server.RestLiServiceException;
import java.util.Map;


/**
 * Contains response data for {@link ResourceMethod#BATCH_DELETE}.
 *
 * @author gye
 */
public class BatchDeleteResponseEnvelope extends BatchResponseEnvelope
{
  /**
   * Instantiates a batch delete response envelope.
   *
   * @param batchResponseMap Map with entities of the response.
   */
  BatchDeleteResponseEnvelope(HttpStatus status, Map<?, BatchResponseEntry> batchResponseMap)
  {
    super(status, batchResponseMap);
  }

  BatchDeleteResponseEnvelope(RestLiServiceException exception)
  {
    super(exception);
  }

  @Override
  public ResourceMethod getResourceMethod()
  {
    return ResourceMethod.BATCH_DELETE;
  }
}
