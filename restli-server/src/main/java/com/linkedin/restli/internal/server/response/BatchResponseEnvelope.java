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

import java.util.Map;


/**
 * Response for {@link com.linkedin.restli.internal.server.ResponseType#BATCH_ENTITIES}.
 * Maps passed in setter and constructor for this class is kept by reference.
 *
 * The invariants of {@link RestLiResponseEnvelope}
 * is maintained, with the further condition that a map is available whenever
 * there are no top level exceptions.
 *
 * @author erli
 */
public abstract class BatchResponseEnvelope extends RestLiResponseEnvelope
{
  private Map<?, BatchResponseEntry> _batchResponseMap;

  /**
   * @param batchResponseMap map with entities of the response.
   *
   */
  BatchResponseEnvelope(HttpStatus status, Map<?, BatchResponseEntry> batchResponseMap)
  {
    super(status);
    _batchResponseMap = batchResponseMap;
  }

  BatchResponseEnvelope(RestLiServiceException exception)
  {
    super(exception);
  }

  /**
   * Sets a batch response without triggered exception.
   *
   * @param batchResponseMap map with entities of the response.
   * @param httpStatus the HTTP status of the response.
   */
  public void setBatchResponseMap(Map<?, BatchResponseEntry> batchResponseMap, HttpStatus httpStatus)
  {
    super.setStatus(httpStatus);
    _batchResponseMap = batchResponseMap;
  }

  /**
   * Returns the map of the batch response.
   *
   * @return the map of the entities of this request.
   */
  public Map<?, BatchResponseEntry> getBatchResponseMap()
  {
    return _batchResponseMap;
  }

  /**
   * Sets the data stored by this envelope to null.
   */
  @Override
  protected void clearData()
  {
    _batchResponseMap = null;
  }

  /**
   * Returns the {@link ResponseType}.
   *
   * @return {@link ResponseType}.
   */
  @Override
  public final ResponseType getResponseType()
  {
    return ResponseType.BATCH_ENTITIES;
  }

  /**
   * Represents an entry in a BatchResponse response.
   *
   */
  public static final class BatchResponseEntry
  {
    // The following sets of variables should be disjoint, e.g.
    // if one group is set the other group should all be null.

    // For valid responses:
    // Invariant: this record template must match the type of the
    // response it is meant to encapsulate.
    // For BatchGet, it must be whichever RecordTemplate the
    // resource method returns for each key. All items in the
    // underlying data map will be preserved.
    // For BatchUpdate, it must be an instanceof UpdateStatus;
    // otherwise, the content will be overwritten upon building
    // the RestLiResponse. The instance of UpdateStatus
    // will not actually honor the status code or error response
    // fields since there are corresponding sources of truth in
    // this class, but any other items in the underlying data map
    // will be preserved.
    private RecordTemplate _recordTemplate;

    // For exception responses:
    private RestLiServiceException _restLiServiceException;

    // Ideally, status should be among the valid response set of private
    // fields. However, to preserve backwards compatible behavior where
    // batch responses can set arbitrary status values in response/exceptions,
    // we will use the following schema:
    // If status is set explicitly, use this value. Otherwise, return the exception's
    // status if there is one present. Otherwise, return null.
    private HttpStatus _httpStatus;

    /**
     * Initiates an entry with the given status and value without triggered exceptions.
     *
     * @param httpStatus http status of the entry.
     * @param recordTemplate value of the entry.
     */
    public BatchResponseEntry(HttpStatus httpStatus, RecordTemplate recordTemplate)
    {
      _httpStatus = httpStatus;
      _recordTemplate = recordTemplate;
      _restLiServiceException = null;
    }

    /**
     * Initiates a failed entity entry with the given exception.
     *
     * @param httpStatus http status of the entry.
     * @param exception the exception for this entry. Included Http status has no effect (for now).
     */
    public BatchResponseEntry(HttpStatus httpStatus, RestLiServiceException exception)
    {
      _httpStatus = httpStatus;
      _recordTemplate = null;
      _restLiServiceException = exception;
    }

    /**
     * Returns the record of this entry.
     *
     * @return the value of the entry.
     */
    public RecordTemplate getRecord()
    {
      return _recordTemplate;
    }

    /**
     * Returns the status of this entry.
     *
     * @return the corresponding status of the entry.
     */
    public HttpStatus getStatus()
    {
      return _httpStatus;
    }

    /**
     * Determines if this entry has an exception.
     *
     * @return true if this entry is a failed entry, false otherwise.
     */
    public boolean hasException()
    {
      return _restLiServiceException != null;
    }

    /**
     * Returns the exception of this entry.
     *
     * @return the exception of this entry.
     */
    public RestLiServiceException getException()
    {
      return _restLiServiceException;
    }
  }
}
