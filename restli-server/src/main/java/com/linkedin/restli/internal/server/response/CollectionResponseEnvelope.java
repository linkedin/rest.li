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
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.internal.server.RestLiResponseEnvelope;
import com.linkedin.restli.internal.server.ResponseType;
import com.linkedin.restli.server.RestLiServiceException;

import java.util.List;
import java.util.Map;


/**
 * Response for {@link com.linkedin.restli.internal.server.ResponseType#GET_COLLECTION}.
 * Lists passed to constructors and setters are kept by reference.
 *
 * The invariants of {@link com.linkedin.restli.internal.server.RestLiResponseEnvelope}
 * is maintained, with the further condition that a collection response,
 * custom metadata, and response paging is available whenever
 * there are no top level exceptions.
 *
 * @author erli
 */
public final class CollectionResponseEnvelope extends RestLiResponseEnvelope
{
  private List<? extends RecordTemplate> _collectionResponse;
  private RecordTemplate _collectionResponseCustomMetadata;
  private CollectionMetadata _collectionResponsePaging;

  /**
   * Sets a collection response without triggered exception.
   *
   * @param collectionResponse The entities of the request.
   * @param collectionResponsePaging Paging for the collection response.
   * @param collectionResponseCustomMetadata the custom metadata used for this collection response.
   * @param headers of the response.
   */
  public CollectionResponseEnvelope(List<? extends RecordTemplate> collectionResponse,
                                    CollectionMetadata collectionResponsePaging,
                                    RecordTemplate collectionResponseCustomMetadata,
                                    Map<String, String> headers)
  {
    super(HttpStatus.S_200_OK, headers);
    setCollectionResponse(HttpStatus.S_200_OK, collectionResponse, collectionResponsePaging, collectionResponseCustomMetadata);
  }

  /**
   * Sets a failed collection response with an exception.
   *
   * @param exception caused the response failure.
   * @param headers of the response.
   */
  public CollectionResponseEnvelope(RestLiServiceException exception,
                                    Map<String, String> headers)
  {
    super(exception, headers);
    _collectionResponse = null;
    _collectionResponseCustomMetadata = null;
    _collectionResponsePaging = null;
  }

  /**
   * Returns the list of collection responses for this request.
   *
   * @return the items of this collection response.
   */
  public List<? extends RecordTemplate> getCollectionResponse()
  {
    return _collectionResponse;
  }

  /**
   * Returns the collection metadata for this collection.
   *
   * @return the collection metadata of this collection response.
   */
  public CollectionMetadata getCollectionResponsePaging()
  {
    return _collectionResponsePaging;
  }

  /**
   * Returns additional custom metadata for this collection response.
   *
   * @return the custom metadata for this collection response.
   */
  public RecordTemplate getCollectionResponseCustomMetadata()
  {
    return _collectionResponseCustomMetadata;
  }

  /**
   * Sets a collection response with no triggered exception.
   *
   * @param httpStatus the status of the request.
   * @param collectionResponse The entities of the request.
   * @param collectionResponsePaging Paging for the collection response.
   * @param collectionResponseCustomMetadata the custom metadata used for this collection response.
   */
  public void setCollectionResponse(HttpStatus httpStatus,
                                    List<? extends RecordTemplate> collectionResponse,
                                    CollectionMetadata collectionResponsePaging,
                                    RecordTemplate collectionResponseCustomMetadata)
  {
    super.setStatus(httpStatus);
    _collectionResponse = collectionResponse;
    _collectionResponsePaging = collectionResponsePaging;
    _collectionResponseCustomMetadata = collectionResponseCustomMetadata;
  }

  /**
   * Sets a failed collection response with an exception.
   *
   * @param exception caused the response failure.
   */
  public void setException(RestLiServiceException exception)
  {
    super.setException(exception);
    _collectionResponse = null;
    _collectionResponsePaging = null;
    _collectionResponseCustomMetadata = null;
  }

  @Override
  public ResponseType getResponseType()
  {
    return ResponseType.GET_COLLECTION;
  }

  @Override
  public RecordResponseEnvelope getRecordResponseEnvelope()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public CollectionResponseEnvelope getCollectionResponseEnvelope()
  {
    return this;
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
