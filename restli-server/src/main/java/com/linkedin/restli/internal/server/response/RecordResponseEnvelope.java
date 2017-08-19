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

package com.linkedin.restli.internal.server.response;


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.internal.server.ResponseType;
import com.linkedin.restli.server.RestLiServiceException;


/**
 * Response for {@link com.linkedin.restli.internal.server.ResponseType#SINGLE_ENTITY}.
 *
 * The invariants of {@link RestLiResponseEnvelope}
 * is maintained, with the further condition that a record template is available whenever
 * there are no top level exceptions.
 *
 * @author erli
 */
public abstract class RecordResponseEnvelope extends RestLiResponseEnvelope
{
  private RecordTemplate _recordResponse;

  /**
   * Sets an entity response with no triggered exception.
   * @param response entity of the response.
   *
   */
  RecordResponseEnvelope(HttpStatus status, RecordTemplate response)
  {
    super(status);
    _recordResponse = response;
  }

  RecordResponseEnvelope(RestLiServiceException exception)
  {
    super(exception);
    _recordResponse = null;
  }

  /**
   * Retrieves the record of this response.
   *
   * @return the entity response.
   */
  public RecordTemplate getRecord()
  {
    return _recordResponse;
  }

  /**
   * Sets an entity response with no triggered exceptions.
   *
   * @param response entity of the response.
   * @param httpStatus the HTTP status of the response.
   */
  public void setRecord(RecordTemplate response, HttpStatus httpStatus)
  {
    super.setStatus(httpStatus);
    _recordResponse = response;
  }

  /**
   * Sets the data stored in this envelope to null.
   */
  @Override
  protected void clearData()
  {
    _recordResponse = null;
  }

  /**
   * Returns the {@link ResponseType}.
   *
   * @return {@link ResponseType}.
   */
  @Override
  public final ResponseType getResponseType()
  {
    return ResponseType.SINGLE_ENTITY;
  }
}
