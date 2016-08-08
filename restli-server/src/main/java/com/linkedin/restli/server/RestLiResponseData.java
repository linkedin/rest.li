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
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.server.response.ActionResponseEnvelope;
import com.linkedin.restli.internal.server.response.BatchDeleteResponseEnvelope;
import com.linkedin.restli.internal.server.response.BatchGetResponseEnvelope;
import com.linkedin.restli.internal.server.response.BatchPartialUpdateResponseEnvelope;
import com.linkedin.restli.internal.server.response.BatchResponseEnvelope;
import com.linkedin.restli.internal.server.response.BatchUpdateResponseEnvelope;
import com.linkedin.restli.internal.server.response.BatchCreateResponseEnvelope;
import com.linkedin.restli.internal.server.response.CollectionResponseEnvelope;
import com.linkedin.restli.internal.server.response.CreateResponseEnvelope;
import com.linkedin.restli.internal.server.response.DeleteResponseEnvelope;
import com.linkedin.restli.internal.server.response.EmptyResponseEnvelope;
import com.linkedin.restli.internal.server.response.FinderResponseEnvelope;
import com.linkedin.restli.internal.server.response.GetAllResponseEnvelope;
import com.linkedin.restli.internal.server.response.GetResponseEnvelope;
import com.linkedin.restli.internal.server.response.OptionsResponseEnvelope;
import com.linkedin.restli.internal.server.response.PartialUpdateResponseEnvelope;
import com.linkedin.restli.internal.server.response.RecordResponseEnvelope;
import com.linkedin.restli.internal.server.ResponseType;

import com.linkedin.restli.internal.server.response.UpdateResponseEnvelope;
import java.net.HttpCookie;
import java.util.List;
import java.util.Map;


/**
 * An abstraction that encapsulates outgoing response data.
 * This abstraction provides a number of response level getters
 * as well as a series of "Formatted" getters. Each one of these
 * getters will return an enveloped object containing the response content.
 *
 * Calling the wrong getter method will generally invoke an
 * UnsupportedOperationException.
 *
 * @author nshankar
 * @author erli
 * @author gye
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
   * Gets a mutable map of the headers of this response.
   *
   * @return a mutable map of string values that indicates the headers of this response.
   */
  Map<String, String> getHeaders();

  /**
   * Gets a mutable list of cookies from this response.
   *
   * @return a mutable list of httpCookie objects from this response.
   */
  List<HttpCookie> getCookies();

  /**
   * Returns the response type of this response.
   *
   * @return the return type associated with this RestLiResponseData object.
   */
  ResponseType getResponseType();

  /**
   * Returns the resource method of this response.
   *
   * @return the resource method associated with this RestLiResponseData object.
   */
  ResourceMethod getResourceMethod();

  /**
   * Returns the response content for resource methods that fall under {@link ResponseType#SINGLE_ENTITY}.
   *
   * @throws UnsupportedOperationException if this method is invoked for the wrong ResponseType.
   *
   * @return the enveloped content for GET, ACTION, and CREATE resource methods.
   */
  RecordResponseEnvelope getRecordResponseEnvelope();

  /**
   * Returns the response content for resource methods that fall under {@link ResponseType#GET_COLLECTION}.
   *
   * @throws UnsupportedOperationException if this method is invoked for the wrong ResponseType.
   *
   * @return the enveloped content for GET_ALL and FINDER resource methods.
   */
  CollectionResponseEnvelope getCollectionResponseEnvelope();

  /**
   * Returns the response content for resource methods that fall under {@link ResponseType#BATCH_ENTITIES}.
   *
   * @throws UnsupportedOperationException if this method is invoked for the wrong ResponseType.
   *
   * @return the enveloped content for BATCH_GET, BATCH_UPDATE, BATCH_PARTIAL_UPDATE and BATCH_DELETE resource methods.
   */
  BatchResponseEnvelope getBatchResponseEnvelope();

  /**
   * Returns the response content for resource methods that fall under {@link ResponseType#STATUS_ONLY}.
   *
   * @throws UnsupportedOperationException if this method is invoked for the wrong ResponseType.
   *
   * @return the enveloped content for PARTIAL_UPDATE, UPDATE, DELETE and OPTIONS resource methods.
   */
  EmptyResponseEnvelope getEmptyResponseEnvelope();

  /**
   * Returns the response content for a {@link com.linkedin.restli.common.ResourceMethod#ACTION}.
   *
   * @throws UnsupportedOperationException if this method is invoked for the wrong ResourceMethod.
   *
   * @return the enveloped content for {@link com.linkedin.restli.common.ResourceMethod#ACTION}.
   */
  ActionResponseEnvelope getActionResponseEnvelope();

  /**
   * Returns the response content for a {@link com.linkedin.restli.common.ResourceMethod#BATCH_CREATE}.
   *
   * @throws UnsupportedOperationException if this method is invoked for the wrong ResourceMethod.
   *
   * @return the enveloped content for {@link com.linkedin.restli.common.ResourceMethod#BATCH_CREATE}.
   */
  BatchCreateResponseEnvelope getBatchCreateResponseEnvelope();

  /**
   * Returns the response content for a {@link com.linkedin.restli.common.ResourceMethod#BATCH_DELETE}.
   *
   * @throws UnsupportedOperationException if this method is invoked for the wrong ResourceMethod.
   *
   * @return the enveloped content for {@link com.linkedin.restli.common.ResourceMethod#BATCH_DELETE}.
   */
  BatchDeleteResponseEnvelope getBatchDeleteResponseEnvelope();

  /**
   * Returns the response content for a {@link com.linkedin.restli.common.ResourceMethod#BATCH_GET}.
   *
   * @throws UnsupportedOperationException if this method is invoked for the wrong ResourceMethod.
   *
   * @return the enveloped content for {@link com.linkedin.restli.common.ResourceMethod#BATCH_GET}.
   */
  BatchGetResponseEnvelope getBatchGetResponseEnvelope();

  /**
   * Returns the response content for a {@link com.linkedin.restli.common.ResourceMethod#BATCH_PARTIAL_UPDATE}.
   *
   * @throws UnsupportedOperationException if this method is invoked for the wrong ResourceMethod.
   *
   * @return the enveloped content for {@link com.linkedin.restli.common.ResourceMethod#BATCH_PARTIAL_UPDATE}.
   */
  BatchPartialUpdateResponseEnvelope getBatchPartialUpdateResponseEnvelope();

  /**
   * Returns the response content for a {@link com.linkedin.restli.common.ResourceMethod#BATCH_UPDATE}.
   *
   * @throws UnsupportedOperationException if this method is invoked for the wrong ResourceMethod.
   *
   * @return the enveloped content for {@link com.linkedin.restli.common.ResourceMethod#BATCH_UPDATE}.
   */
  BatchUpdateResponseEnvelope getBatchUpdateResponseEnvelope();

  /**
   * Returns the response content for a {@link com.linkedin.restli.common.ResourceMethod#CREATE}.
   *
   * @throws UnsupportedOperationException if this method is invoked for the wrong ResourceMethod.
   *
   * @return the enveloped content for {@link com.linkedin.restli.common.ResourceMethod#CREATE}.
   */
  CreateResponseEnvelope getCreateResponseEnvelope();

  /**
   * Returns the response content for a {@link com.linkedin.restli.common.ResourceMethod#DELETE}.
   *
   * @throws UnsupportedOperationException if this method is invoked for the wrong ResourceMethod.
   *
   * @return the enveloped content for {@link com.linkedin.restli.common.ResourceMethod#DELETE}.
   */
  DeleteResponseEnvelope getDeleteResponseEnvelope();

  /**
   * Returns the response content for a {@link com.linkedin.restli.common.ResourceMethod#FINDER}.
   *
   * @throws UnsupportedOperationException if this method is invoked for the wrong ResourceMethod.
   *
   * @return the enveloped content for {@link com.linkedin.restli.common.ResourceMethod#FINDER}.
   */
  FinderResponseEnvelope getFinderResponseEnvelope();

  /**
   * Returns the response content for a {@link com.linkedin.restli.common.ResourceMethod#GET_ALL}.
   *
   * @throws UnsupportedOperationException if this method is invoked for the wrong ResourceMethod.
   *
   * @return the enveloped content for {@link com.linkedin.restli.common.ResourceMethod#GET_ALL}.
   */
  GetAllResponseEnvelope getGetAllResponseEnvelope();

  /**
   * Returns the response content for a {@link com.linkedin.restli.common.ResourceMethod#GET}.
   *
   * @throws UnsupportedOperationException if this method is invoked for the wrong ResourceMethod.
   *
   * @return the enveloped content for {@link com.linkedin.restli.common.ResourceMethod#GET}.
   */
  GetResponseEnvelope getGetResponseEnvelope();

  /**
   * Returns the response content for a {@link com.linkedin.restli.common.ResourceMethod#OPTIONS}.
   *
   * @throws UnsupportedOperationException if this method is invoked for the wrong ResourceMethod.
   *
   * @return the enveloped content for {@link com.linkedin.restli.common.ResourceMethod#OPTIONS}.
   */
  OptionsResponseEnvelope getOptionsResponseEnvelope();

  /**
   * Returns the response content for a {@link com.linkedin.restli.common.ResourceMethod#PARTIAL_UPDATE}.
   *
   * @throws UnsupportedOperationException if this method is invoked for the wrong ResourceMethod.
   *
   * @return the enveloped content for {@link com.linkedin.restli.common.ResourceMethod#PARTIAL_UPDATE}.
   */
  PartialUpdateResponseEnvelope getPartialUpdateResponseEnvelope();

  /**
   * Returns the response content for a {@link com.linkedin.restli.common.ResourceMethod#UPDATE}.
   *
   * @throws UnsupportedOperationException if this method is invoked for the wrong ResourceMethod.
   *
   * @return the enveloped content for {@link com.linkedin.restli.common.ResourceMethod#UPDATE}.
   */
  UpdateResponseEnvelope getUpdateResponseEnvelope();
}
