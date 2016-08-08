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


import com.linkedin.restli.common.CreateIdStatus;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.server.ResponseType;
import com.linkedin.restli.server.RestLiServiceException;

import java.util.List;


/**
 * Contains response data for {@link ResourceMethod#BATCH_CREATE}.
 * Lists passed to constructors and setters are kept by reference.
 *
 * The invariants of {@link RestLiResponseEnvelope}
 * is maintained, with the further condition that a list of response is available whenever
 * there are no top level exceptions.
 *
 * @author erli
 * @author gye
 */
public class BatchCreateResponseEnvelope extends RestLiResponseEnvelope
{
  protected List<CollectionCreateResponseItem> _createResponses;
  private final boolean _isGetAfterCreate;

  /**
   * This constructor is for non CREATE + GET (i.e. this constructor creates a BatchCreateResponse that doesn't contain
   * the newly created data).
   *
   * @param createResponses List of created responses.
   * @param restLiResponseData Wrapper response data that is storing this envelope.
   */
  BatchCreateResponseEnvelope(List<CollectionCreateResponseItem> createResponses,
                              RestLiResponseDataImpl restLiResponseData)
  {
    this(createResponses, false, restLiResponseData);
  }

  /**
   * This constructor has a configuration boolean for whether or not this is a CREATE + GET (i.e. this constructor
   * creates a BatchCreateResponse that contains the newly created data) as opposed to a normal CREATE. true = CREATE +
   * GET, false = CREATE.
   *
   * @param createResponses List of created responses.
   * @param isGetAfterCreate Boolean flag denoting whether or not this is a CREATE + GET.
   * @param restLiResponseData Wrapper response data that is storign this envelope.
   */
  BatchCreateResponseEnvelope(List<CollectionCreateResponseItem> createResponses, boolean isGetAfterCreate,
                              RestLiResponseDataImpl restLiResponseData)
  {
    super(restLiResponseData);
    _createResponses = createResponses;
    _isGetAfterCreate = isGetAfterCreate;
  }

  @Override
  public ResourceMethod getResourceMethod()
  {
    return ResourceMethod.BATCH_CREATE;
  }

  /**
   * Returns whether or not this CREATE response also contains the newly created data (i.e. a GET after a CREATE).
   * Users can use getRecord() to retrieve the newly created data if this is a CREATE + GET. Otherwise, the user can
   * only use getRecord() to get the ID of the newly created data.
   *
   * @return boolean as to whether or not this response contains the newly created data.
   */
  public boolean isGetAfterCreate()
  {
    return _isGetAfterCreate;
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
   * Sets a batch create response with no execption.
   *
   * @param createResponse list of responses for each key.
   * @param httpStatus the HTTP status of the response.
   */
  public void setCreateResponse(List<CollectionCreateResponseItem> createResponse, HttpStatus httpStatus)
  {
    _restLiResponseData.setStatus(httpStatus);
    _createResponses = createResponse;
  }

  /**
   * Sets the data stored by this envelope to null.
   */
  @Override
  protected void clearData()
  {
    _createResponses = null;
  }

  /**
   * Returns the {@link ResponseType}
   *
   * @return {@link ResponseType}
   */
  @Override
  public ResponseType getResponseType()
  {
    return ResponseType.CREATE_COLLECTION;
  }

  /**
   * Represents entries in {@link BatchCreateResponseEnvelope}.
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
