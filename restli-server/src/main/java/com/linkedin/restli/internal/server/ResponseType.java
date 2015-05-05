/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.internal.server;


import com.linkedin.restli.common.ResourceMethod;

import java.util.Arrays;
import java.util.List;

import static com.linkedin.restli.common.ResourceMethod.GET;
import static com.linkedin.restli.common.ResourceMethod.ACTION;
import static com.linkedin.restli.common.ResourceMethod.CREATE;
import static com.linkedin.restli.common.ResourceMethod.GET_ALL;
import static com.linkedin.restli.common.ResourceMethod.FINDER;
import static com.linkedin.restli.common.ResourceMethod.BATCH_CREATE;
import static com.linkedin.restli.common.ResourceMethod.BATCH_GET;
import static com.linkedin.restli.common.ResourceMethod.BATCH_UPDATE;
import static com.linkedin.restli.common.ResourceMethod.BATCH_PARTIAL_UPDATE;
import static com.linkedin.restli.common.ResourceMethod.BATCH_DELETE;
import static com.linkedin.restli.common.ResourceMethod.PARTIAL_UPDATE;
import static com.linkedin.restli.common.ResourceMethod.UPDATE;
import static com.linkedin.restli.common.ResourceMethod.DELETE;
import static com.linkedin.restli.common.ResourceMethod.OPTIONS;


/**
 * This enum type is a wrapper enum type that describes the
 * type of response. The intended use case is to determine
 * which enveloped object is accessible in
 * {@link com.linkedin.restli.server.RestLiResponseData}.
 *
 * @author nshankar
 * @author erli
 */
public enum ResponseType
{
  /**
   * Used for {@link com.linkedin.restli.server.RestLiResponseData#getRecordResponseEnvelope()}
   */
  SINGLE_ENTITY(GET, ACTION, CREATE),

  /**
   * Used for {@link com.linkedin.restli.server.RestLiResponseData#getCollectionResponseEnvelope()}
   */
  GET_COLLECTION(GET_ALL, FINDER),

  /**
   * Used for {@link com.linkedin.restli.server.RestLiResponseData#getCreateCollectionResponseEnvelope()}
   */
  CREATE_COLLECTION(BATCH_CREATE),

  /**
   * Used for {@link com.linkedin.restli.server.RestLiResponseData#getBatchResponseEnvelope()}
   */
  BATCH_ENTITIES(BATCH_GET, BATCH_UPDATE, BATCH_PARTIAL_UPDATE, BATCH_DELETE),

  /**
   * Used for {@link com.linkedin.restli.server.RestLiResponseData#getEmptyResponseEnvelope()}
   */
  STATUS_ONLY(PARTIAL_UPDATE, UPDATE, DELETE, OPTIONS);

  private ResponseType(ResourceMethod... types)
  {
    _methodTypes = Arrays.asList(types);
  }

  /**
   * Convenience method to return the ResponseType based on
   * resource method type.
   *
   * @param type represents a resource method type.
   * @return the corresponding ResponseType of the resource method.
   */
  public static ResponseType fromMethodType(ResourceMethod type)
  {
    if (type == null)
    {
      return STATUS_ONLY;
    }

    for (ResponseType responseType : values())
    {
      if (responseType._methodTypes.contains(type))
      {
        return responseType;
      }
    }

    throw new UnsupportedOperationException("Unexpected resource method found: " + type.toString());
  }

  private final List<ResourceMethod> _methodTypes;
}
