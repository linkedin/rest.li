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
  private List<CollectionCreateResponseItem> _createResponses;
  private final boolean _isGetAfterCreate;

  /**
   * This constructor has a configuration boolean for whether or not this is a CREATE + GET (i.e. this constructor
   * creates a BatchCreateResponse that contains the newly created data) as opposed to a normal CREATE. true = CREATE +
   * GET, false = CREATE.
   * @param createResponses List of created responses.
   * @param isGetAfterCreate Boolean flag denoting whether or not this is a CREATE + GET.
   */
  BatchCreateResponseEnvelope(HttpStatus status, List<CollectionCreateResponseItem> createResponses, boolean isGetAfterCreate)
  {
    super(status);
    _createResponses = createResponses;
    _isGetAfterCreate = isGetAfterCreate;
  }

  BatchCreateResponseEnvelope(RestLiServiceException exception, boolean isGetAfterCreate)
  {
    super(exception);
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
    super.setStatus(httpStatus);
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

    // For success response
    private CreateIdStatus<?> _recordResponse;

    // For exception response
    private RestLiServiceException _exception;

    // HttpStatus should always be set either from the success response or the exception.
    private HttpStatus _httpStatus;

    /**
     * Instantiates an entry within a collection create response without triggered exception.
     *
     * @param response value of the entry.
     */
    public CollectionCreateResponseItem(CreateIdStatus<?> response)
    {
      _recordResponse = response;

      _exception = null;

      _httpStatus = HttpStatus.fromCode(response.getStatus());
    }

    /**
     * Instantiates a failed entry within a collection create response.
     *
     * @param exception the exception that triggered the failure.
     */
    public CollectionCreateResponseItem(RestLiServiceException exception)
    {
      _exception = exception;

      _recordResponse = null;

      _httpStatus = exception.getStatus();
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
      return _recordResponse != null ? _recordResponse.getKey() : null;
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

    /**
     * Gets the HTTP status of this entry. It's either set in the success response or in the exception.
     */
    public HttpStatus getStatus()
    {
      return _httpStatus;
    }
  }
}
