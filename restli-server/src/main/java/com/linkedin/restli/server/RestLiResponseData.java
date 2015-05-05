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

package com.linkedin.restli.server;


import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.internal.server.response.BatchResponseEnvelope;
import com.linkedin.restli.internal.server.response.CreateCollectionResponseEnvelope;
import com.linkedin.restli.internal.server.response.CollectionResponseEnvelope;
import com.linkedin.restli.internal.server.response.EmptyResponseEnvelope;
import com.linkedin.restli.internal.server.response.RecordResponseEnvelope;
import com.linkedin.restli.internal.server.ResponseType;
import java.util.Map;


/**
 * An abstraction that encapsulates outgoing response data.
 * This abstraction provides a number of response level getters
 * as well as a series of "Formatted" getters. Each one of these
 * getters will return an enveloped object representing the response.
 *
 * Calling the wrong getter method will generally invoke an
 * UnsupportedMethodException.
 *
 * @author nshankar
 * @author erli
 *
 */
public interface RestLiResponseData
{
  /**
   * Determine if the data corresponds to an error response.
   *
   * @return true if the response is an error response; else false.
   */
  boolean isErrorResponse();

  /**
   * Obtain the RestLiServiceException associated with the response data when the data is an error response.
   *
   * @return the RestLiServiceException if one exists; else null.
   */
  RestLiServiceException getServiceException();

  /**
   * Gets the status of the request.
   *
   * @return the http status.
   */
  HttpStatus getStatus();

  /**
   * Returns the response type of this response.
   *
   * @return the return type associated with this RestLiResponseData object.
   */
  ResponseType getResponseType();

  /**
   * Returns the enveloped view of this response as a RecordResponseEnvelope.
   *
   * @throws UnsupportedOperationException if this method is invoked for the wrong ResponseType.
   *
   * @return the enveloped response for GET, ACTION, and CREATE resource methods.
   */
  RecordResponseEnvelope getRecordResponseEnvelope();

  /**
   * Returns the enveloped view of this response as a CollectionResponseEnvelope.
   *
   * @throws UnsupportedOperationException if this method is invoked for the wrong ResponseType.
   *
   * @return the enveloped response for GET_ALL and FINDER resource methods.
   */
  CollectionResponseEnvelope getCollectionResponseEnvelope();

  /**
   * Returns the enveloped view of this response as a CreateCollectionResponseEnvelope.
   *
   * @throws UnsupportedOperationException if this method is invoked for the wrong ResponseType.
   *
   * @return the enveloped response for BATCH_CREATE resource methods.
   */
  CreateCollectionResponseEnvelope getCreateCollectionResponseEnvelope();

  /**
   * Returns the enveloped view of this response as a BatchResponseEnvelope.
   *
   * @throws UnsupportedOperationException if this method is invoked for the wrong ResponseType.
   *
   * @return the enveloped response for BATCH_GET, BATCH_UPDATE, BATCH_PARTIAL_UPDATE and BATCH_DELETE resource methods.
   */
  BatchResponseEnvelope getBatchResponseEnvelope();

  /**
   * Returns the enveloped view of this response as an EmptyResponseEnvelope.
   *
   * @throws UnsupportedOperationException if this method is invoked for the wrong ResponseType.
   *
   * @return the enveloped response for PARTIAL_UPDATE, UPDATE, DELETE and OPTIONS resource methods.
   */
  EmptyResponseEnvelope getEmptyResponseEnvelope();

  /**
   * Gets a mutable map of the headers of this response.
   *
   * @return a mutable map of string values that indicates the headers of this response.
   */
  Map<String, String> getHeaders();
}
