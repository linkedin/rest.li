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
import com.linkedin.restli.internal.server.ResponseType;

import java.util.List;


/**
 * Response for {@link com.linkedin.restli.internal.server.ResponseType#GET_COLLECTION}.
 * Lists passed to constructors and setters are kept by reference.
 *
 * The invariants of {@link RestLiResponseEnvelope}
 * is maintained, with the further condition that a collection response,
 * custom metadata, and response paging is available whenever
 * there are no top level exceptions.
 *
 * @author erli
 */
public abstract class CollectionResponseEnvelope extends RestLiResponseEnvelope
{
  protected List<? extends RecordTemplate> _collectionResponse;
  protected RecordTemplate _collectionResponseCustomMetadata;
  protected CollectionMetadata _collectionResponsePaging;

  /**
   * Sets a collection response without triggered exception.
   *
   * @param collectionResponse The entities of the request.
   * @param collectionResponsePaging Paging for the collection response.
   * @param collectionResponseCustomMetadata the custom metadata used for this collection response.
   * @param restLiResponseData wrapper response data that is storing this envelope.
   */
  protected CollectionResponseEnvelope(List<? extends RecordTemplate> collectionResponse,
                                       CollectionMetadata collectionResponsePaging,
                                       RecordTemplate collectionResponseCustomMetadata,
                                       RestLiResponseDataImpl restLiResponseData)
  {
    super(restLiResponseData);
    _collectionResponse = collectionResponse;
    _collectionResponsePaging = collectionResponsePaging;
    _collectionResponseCustomMetadata = collectionResponseCustomMetadata;
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
   * @param collectionResponse The entities of the request.
   * @param collectionResponsePaging Paging for the collection response.
   * @param collectionResponseCustomMetadata the custom metadata used for this collection response.
   * @param httpStatus the HTTP status of the response.
   */
  public void setCollectionResponse(List<? extends RecordTemplate> collectionResponse,
                                    CollectionMetadata collectionResponsePaging,
                                    RecordTemplate collectionResponseCustomMetadata,
                                    HttpStatus httpStatus)
  {
    _restLiResponseData.setStatus(httpStatus);
    _collectionResponse = collectionResponse;
    _collectionResponsePaging = collectionResponsePaging;
    _collectionResponseCustomMetadata = collectionResponseCustomMetadata;
  }

  /**
   * Sets the data stored by this envelope to null.
   */
  @Override
  protected void clearData()
  {
    _collectionResponse = null;
    _collectionResponsePaging = null;
    _collectionResponseCustomMetadata = null;
  }

  /**
   * Returns the {@link ResponseType}.
   *
   * @return {@link ResponseType}.
   */
  @Override
  public final ResponseType getResponseType()
  {
    return ResponseType.GET_COLLECTION;
  }
}
