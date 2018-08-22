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
import com.linkedin.restli.internal.server.ResponseType;
import com.linkedin.restli.server.RestLiServiceException;


/**
 * Contains response data for {@link ResourceMethod#PARTIAL_UPDATE}.
 *
 * @author gye
 * @author Evan Williams
 */
public class PartialUpdateResponseEnvelope extends RestLiResponseEnvelope
{
  private RecordTemplate _recordResponse;

  PartialUpdateResponseEnvelope(HttpStatus status)
  {
    this(status, null);
  }

  PartialUpdateResponseEnvelope(HttpStatus status, RecordTemplate response)
  {
    super(status);
    _recordResponse = response;
  }

  PartialUpdateResponseEnvelope(RestLiServiceException exception)
  {
    super(exception);
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
   * Dynamically determine what the {@link ResponseType} of this response envelope is depending on whether
   * an entity is being returned.
   *
   * @return response type
   */
  @Override
  public ResponseType getResponseType()
  {
    return _recordResponse == null ? ResponseType.STATUS_ONLY : ResponseType.SINGLE_ENTITY;
  }

  @Override
  public ResourceMethod getResourceMethod()
  {
    return ResourceMethod.PARTIAL_UPDATE;
  }

  @Override
  protected void clearData()
  {
    _recordResponse = null;
  }
}
