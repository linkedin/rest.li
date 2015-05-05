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


import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.internal.server.RestLiResponseEnvelope;
import com.linkedin.restli.internal.server.ResponseType;
import com.linkedin.restli.server.RestLiServiceException;

import java.util.Map;


/**
 * Response for {@link com.linkedin.restli.internal.server.ResponseType#STATUS_ONLY}.
 *
 * @author erli
 */
public final class EmptyResponseEnvelope extends RestLiResponseEnvelope
{
  /**
   * Instantiates a response with only an HttpStatus without a triggered exception.
   *
   * @param httpStatus of the response.
   * @param headers of the response.
   */
  public EmptyResponseEnvelope(HttpStatus httpStatus, Map<String, String> headers)
  {
    super(httpStatus, headers);
  }

  /**
   * Instantiates a failed response with only an HttpStatus.
   *
   * @param exception that triggered the failure.
   * @param headers of the response.
   */
  public EmptyResponseEnvelope(RestLiServiceException exception, Map<String, String> headers)
  {
    super(exception, headers);
  }

  @Override
  public ResponseType getResponseType()
  {
    return ResponseType.STATUS_ONLY;
  }

  @Override
  public RecordResponseEnvelope getRecordResponseEnvelope()
  {
    throw new UnsupportedOperationException();
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
    return this;
  }
}