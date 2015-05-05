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
import com.linkedin.restli.internal.server.RestLiResponseEnvelope;
import com.linkedin.restli.internal.server.ResponseType;
import com.linkedin.restli.server.RestLiServiceException;

import java.util.Map;


/**
 * Response for {@link com.linkedin.restli.internal.server.ResponseType#SINGLE_ENTITY}.
 *
 * The invariants of {@link com.linkedin.restli.internal.server.RestLiResponseEnvelope}
 * is maintained, with the further condition that a record template is available whenever
 * there are no top level exceptions.
 *
 * @author erli
 */
public final class RecordResponseEnvelope extends RestLiResponseEnvelope
{
  private RecordTemplate _recordResponse;

  /**
   * Sets an entity response with no triggered exception.
   *
   * @param httpStatus http status of the response.
   * @param response entity of the response.
   * @param headers headers of the response.
   */
  public RecordResponseEnvelope(HttpStatus httpStatus, RecordTemplate response, Map<String, String> headers)
  {
    super(httpStatus, headers);
    _recordResponse = response;
  }

  /**
   * Sets a failed entity response with an exception.
   *
   * @param exception caused the response failure.
   * @param headers headers of the response.
   */
  public RecordResponseEnvelope(RestLiServiceException exception, Map<String, String> headers)
  {
    super(exception, headers);
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
   * @param httpStatus http status of the response.
   * @param response entity of the response.
   */
  public void setRecord(RecordTemplate response, HttpStatus httpStatus)
  {
    super.setStatus(httpStatus);
    _recordResponse = response;
  }

  /**
   * Sets the exception of this response with an exception.
   *
   * @param exception caused the response failure.
   */
  public void setException(RestLiServiceException exception)
  {
    super.setException(exception);
    _recordResponse = null;
  }

  @Override
  public ResponseType getResponseType()
  {
    return ResponseType.SINGLE_ENTITY;
  }

  @Override
  public RecordResponseEnvelope getRecordResponseEnvelope()
  {
    return this;
  }

  @Override
  public CollectionResponseEnvelope getCollectionResponseEnvelope()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public CreateCollectionResponseEnvelope getCreateCollectionResponseEnvelope()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public BatchResponseEnvelope getBatchResponseEnvelope()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public EmptyResponseEnvelope getEmptyResponseEnvelope()
  {
    throw new UnsupportedOperationException();
  }
}
