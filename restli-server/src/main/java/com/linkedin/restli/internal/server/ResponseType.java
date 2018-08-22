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
   * Used for {@link com.linkedin.restli.internal.server.response.RecordResponseEnvelope}, and for
   * {@link com.linkedin.restli.internal.server.response.PartialUpdateResponseEnvelope} when returning an entity.
   */
  SINGLE_ENTITY,

  /**
   * Used for {@link com.linkedin.restli.internal.server.response.CollectionResponseEnvelope}.
   */
  GET_COLLECTION,

  /**
   * Used for {@link com.linkedin.restli.internal.server.response.BatchCreateResponseEnvelope}.
   */
  CREATE_COLLECTION,

  /**
   * Used for {@link com.linkedin.restli.internal.server.response.BatchResponseEnvelope}.
   */
  BATCH_ENTITIES,

  /**
   * Used for {@link com.linkedin.restli.internal.server.response.EmptyResponseEnvelope}, and for
   * {@link com.linkedin.restli.internal.server.response.PartialUpdateResponseEnvelope} when not returning an entity.
   */
  STATUS_ONLY
}
