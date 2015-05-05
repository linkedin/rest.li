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
import com.linkedin.restli.common.CreateIdStatus;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.internal.server.RestLiResponseEnvelope;
import com.linkedin.restli.internal.server.ResponseType;
import com.linkedin.restli.server.RestLiServiceException;

import java.util.List;
import java.util.Map;


/**
 * Response for {@link com.linkedin.restli.internal.server.ResponseType#CREATE_COLLECTION}.
 * Lists passed to constructors and setters are kept by reference.
 *
 * The invariants of {@link com.linkedin.restli.internal.server.RestLiResponseEnvelope}
 * is maintained, with the further condition that a list of response is available whenever
 * there are no top level exceptions.
 *
 * @author erli
 */
public final class CreateCollectionResponseEnvelope extends RestLiResponseEnvelope
{
  private List<CollectionCreateResponseItem> _createResponses;

  /**
   * Sets a batch create response with no triggered exception.
   *
   * @param createResponse List of responses for each key.
   * @param headers of the response.
   */
  public CreateCollectionResponseEnvelope(List<CollectionCreateResponseItem> createResponse, Map<String, String> headers)
  {
    super(HttpStatus.S_200_OK, headers);
    _createResponses = createResponse;
  }

  /**
   * Sets a failed top level response with an exception indicating the entire request failed.
   *
   * @param exception caused the response failure.
   */
  public CreateCollectionResponseEnvelope(RestLiServiceException exception, Map<String, String> headers)
  {
    super(exception, headers);
    _createResponses = null;
  }

  /**
   * Returns the list of items created, possibly with errors.
   *
   * @return the list of results for each item created, possibly with errors.
   */
  public List<CollectionCreateResponseItem> getCreateResponses()
  {
    return _createResponses;
  }

  /**
   * Sets a batch create response with no execption..
   *
   * @param httpStatus status of the request.
   * @param createResponse list of responses for each key.
   */
  public void setCreateResponse(HttpStatus httpStatus, List<CollectionCreateResponseItem> createResponse)
  {
    super.setStatus(httpStatus);
    _createResponses = createResponse;
  }

  /**
   * Sets a failed top level response with an exception indicating the entire request failed.
   *
   * @param exception caused the response failure.
   */
  public void setException(RestLiServiceException exception)
  {
    super.setException(exception);
    _createResponses = null;
  }

  @Override
  public ResponseType getResponseType()
  {
    return ResponseType.CREATE_COLLECTION;
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
    return this;
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

  /**
   * Represents entries in {@link CreateCollectionResponseEnvelope}.
   *
   */
  public static final class CollectionCreateResponseItem
  {
    // The following sets of variables should be disjoint, e.g.
    // if one group is set the other group should all be null.
    // For correct response
    private CreateIdStatus<?> _recordResponse;

    // For exception response
    private Object _id;
    private RestLiServiceException _exception;

    /**
     * Instantiates an entry within a collection create response without triggered exception.
     *
     * @param response value of the entry.
     */
    public CollectionCreateResponseItem(CreateIdStatus response)
    {
      setCollectionCreateResponseItem(response);
    }

    /**
     * Instantiates a failed entry within a collection create response.
     *
     * @param exception the exception that triggered the failure.
     * @param id represents the key of the failed entry.
     */
    public CollectionCreateResponseItem(RestLiServiceException exception, Object id)
    {
      setCollectionCreateResponseItem(exception, id);
    }

    /**
     * Sets the entry to a response without a triggered exception.
     *
     * @param response the response value to set this entry to.
     */
    public void setCollectionCreateResponseItem(CreateIdStatus<?> response)
    {
      _recordResponse = response;

      _id = null;
      _exception = null;
    }

    /**
     * Sets the entry to a failed response.
     *
     * @param exception the exception that caused the entry to fail.
     * @param id is the id for the failed entry.
     */
    public void setCollectionCreateResponseItem(RestLiServiceException exception, Object id)
    {
      _exception = exception;
      _id = id;

      _recordResponse = null;
    }

    /**
     * Returns the value of an entry without a triggered exception, or null otherwise.
     *
     * @return the object representing the result of an entry.
     */
    public CreateIdStatus<?> getRecord()
    {
      return _recordResponse;
    }

    /**
     * Returns the id of the entry.
     *
     * @return the Id of the entry if an exception was triggered.
     */
    public Object getId()
    {
      return _id;
    }

    /**
     * Determines if the entry is a failure.
     *
     * @return true if the entry contains an exception, false otherwise.
     */
    public boolean isErrorResponse()
    {
      return _exception != null;
    }

    /**
     * Returns the exception of this entry.
     *
     * @return the exception cause of this entry.
     */
    public RestLiServiceException getException()
    {
      return _exception;
    }
  }
}
