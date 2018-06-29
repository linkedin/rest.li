/*
   Copyright (c) 2016 LinkedIn Corp.

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
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * This builder utility class is used to build response data for testing filter implementations. Some filters are used
 * to modify response data and need to generate response data in tests for those filters.
 *
 * Each build method will return a response data containing a response envelope. For example, the buildGetResponseData
 * method will build a response data containing a GET response envelope. Note that the invariants are maintained for
 * both {@link RestLiResponseData} and {@link RestLiResponseEnvelope} - please read their Javadocs for more information.
 *
 * This class is helpful for creating response data in tests because both the response envelope setter inside response
 * data and the response envelope constructors are package private. Without this class, you cannot create response data
 * with response envelopes inside of them.
 *
 * This class is intended to be used as a test utility only.
 *
 * @author gye
 */
public final class ResponseDataBuilderUtil
{
  private ResponseDataBuilderUtil()
  {
    // private constructor to disable instantiation.
  }

  public static RestLiResponseData<GetResponseEnvelope> buildGetResponseData(HttpStatus status, RecordTemplate getResponse)
  {
    return new RestLiResponseDataImpl<>(new GetResponseEnvelope(status, getResponse), new HashMap<>(), new ArrayList<>());
  }

  public static RestLiResponseData<GetResponseEnvelope> buildGetResponseData(RestLiServiceException exception)
  {
    return new RestLiResponseDataImpl<>(new GetResponseEnvelope(exception), new HashMap<>(), new ArrayList<>());
  }

  public static RestLiResponseData<CreateResponseEnvelope> buildCreateResponseData(HttpStatus status, RecordTemplate createResponse)
  {
    return new RestLiResponseDataImpl<>(new CreateResponseEnvelope(status, createResponse, false), new HashMap<>(), new ArrayList<>());
  }

  public static RestLiResponseData<CreateResponseEnvelope> buildCreateResponseData(RestLiServiceException exception)
  {
    return new RestLiResponseDataImpl<>(new CreateResponseEnvelope(exception, false), new HashMap<>(), new ArrayList<>());
  }

  public static RestLiResponseData<ActionResponseEnvelope> buildActionResponseData(HttpStatus status, RecordTemplate actionResponse)
  {
    return new RestLiResponseDataImpl<>(new ActionResponseEnvelope(status, actionResponse), new HashMap<>(), new ArrayList<>());
  }

  public static RestLiResponseData<ActionResponseEnvelope> buildActionResponseData(RestLiServiceException exception)
  {
    return new RestLiResponseDataImpl<>(new ActionResponseEnvelope(exception), new HashMap<>(), new ArrayList<>());
  }

  public static RestLiResponseData<FinderResponseEnvelope> buildFinderResponseData(HttpStatus status,
                                                           List<? extends RecordTemplate> collectionResponse,
                                                           CollectionMetadata collectionResponsePaging,
                                                           RecordTemplate collectionResponseCustomMetadata)
  {
    return new RestLiResponseDataImpl<>(new FinderResponseEnvelope(status, collectionResponse, collectionResponsePaging,
        collectionResponseCustomMetadata), new HashMap<>(), new ArrayList<>());
  }

  public static RestLiResponseData<FinderResponseEnvelope> buildFinderResponseData(RestLiServiceException exception)
  {
    return new RestLiResponseDataImpl<>(new FinderResponseEnvelope(exception), new HashMap<>(), new ArrayList<>());
  }

  public static RestLiResponseData<BatchFinderResponseEnvelope> buildBatchFinderResponseData(HttpStatus status,
      List<BatchFinderResponseEnvelope.BatchFinderEntry> items)
  {
    return new RestLiResponseDataImpl<>(new BatchFinderResponseEnvelope(status, items),
        new HashMap<>(), new ArrayList<>());
  }

  public static RestLiResponseData<BatchFinderResponseEnvelope> buildBatchFinderResponseData(RestLiServiceException exception)
  {
    return new RestLiResponseDataImpl<>(new BatchFinderResponseEnvelope(exception), new HashMap<>(), new ArrayList<>());
  }

  public static RestLiResponseData<GetAllResponseEnvelope> buildGetAllResponseData(HttpStatus status,
                                                           List<? extends RecordTemplate> collectionResponse,
                                                           CollectionMetadata collectionResponsePaging,
                                                           RecordTemplate collectionResponseCustomMetadata)
  {
    return new RestLiResponseDataImpl<>(new GetAllResponseEnvelope(status, collectionResponse,
        collectionResponsePaging, collectionResponseCustomMetadata), new HashMap<>(), new ArrayList<>());
  }

  public static RestLiResponseData<GetAllResponseEnvelope> buildGetAllResponseData(RestLiServiceException exception)
  {
    return new RestLiResponseDataImpl<>(new GetAllResponseEnvelope(exception), new HashMap<>(), new ArrayList<>());
  }

  public static RestLiResponseData<UpdateResponseEnvelope> buildUpdateResponseData(HttpStatus status)
  {
    return new RestLiResponseDataImpl<>(new UpdateResponseEnvelope(status), new HashMap<>(), new ArrayList<>());
  }

  public static RestLiResponseData<UpdateResponseEnvelope> buildUpdateResponseData(RestLiServiceException exception)
  {
    return new RestLiResponseDataImpl<>(new UpdateResponseEnvelope(exception), new HashMap<>(), new ArrayList<>());
  }

  public static RestLiResponseData<PartialUpdateResponseEnvelope> buildPartialUpdateResponseData(HttpStatus status)
  {
    return new RestLiResponseDataImpl<>(new PartialUpdateResponseEnvelope(status), new HashMap<>(), new ArrayList<>());
  }

  public static RestLiResponseData<PartialUpdateResponseEnvelope> buildPartialUpdateResponseData(HttpStatus status, RecordTemplate entity)
  {
    return new RestLiResponseDataImpl<>(new PartialUpdateResponseEnvelope(status, entity), new HashMap<>(), new ArrayList<>());
  }

  public static RestLiResponseData<PartialUpdateResponseEnvelope> buildPartialUpdateResponseData(RestLiServiceException exception)
  {
    return new RestLiResponseDataImpl<>(new PartialUpdateResponseEnvelope(exception), new HashMap<>(), new ArrayList<>());
  }

  public static RestLiResponseData<OptionsResponseEnvelope> buildOptionsResponseData(HttpStatus status)
  {
    return new RestLiResponseDataImpl<>(new OptionsResponseEnvelope(status), new HashMap<>(), new ArrayList<>());
  }

  public static RestLiResponseData<OptionsResponseEnvelope> buildOptionsResponseData(RestLiServiceException exception)
  {
    return new RestLiResponseDataImpl<>(new OptionsResponseEnvelope(exception), new HashMap<>(), new ArrayList<>());
  }

  public static RestLiResponseData<DeleteResponseEnvelope> buildDeleteResponseData(HttpStatus status)
  {
    return new RestLiResponseDataImpl<>(new DeleteResponseEnvelope(status), new HashMap<>(), new ArrayList<>());
  }

  public static RestLiResponseData<DeleteResponseEnvelope> buildDeleteResponseData(RestLiServiceException exception)
  {
    return new RestLiResponseDataImpl<>(new DeleteResponseEnvelope(exception), new HashMap<>(), new ArrayList<>());
  }

  public static RestLiResponseData<BatchCreateResponseEnvelope> buildBatchCreateResponseData(HttpStatus status,
                                                                List<BatchCreateResponseEnvelope.CollectionCreateResponseItem> responseItems)
  {
    return new RestLiResponseDataImpl<>(new BatchCreateResponseEnvelope(status, responseItems,
        false), new HashMap<>(), new ArrayList<>());
  }

  public static RestLiResponseData<BatchCreateResponseEnvelope> buildBatchCreateResponseData(RestLiServiceException exception)
  {
    return new RestLiResponseDataImpl<>(new BatchCreateResponseEnvelope(exception, false), new HashMap<>(),
        new ArrayList<>());
  }

  public static RestLiResponseData<BatchGetResponseEnvelope> buildBatchGetResponseData(HttpStatus status,
                                                             Map<?, BatchResponseEnvelope.BatchResponseEntry> batchResponseMap)
  {
    return new RestLiResponseDataImpl<>(new BatchGetResponseEnvelope(status, batchResponseMap), new HashMap<>(),
        new ArrayList<>());
  }

  public static RestLiResponseData<BatchGetResponseEnvelope> buildBatchGetResponseData(RestLiServiceException exception)
  {
    return new RestLiResponseDataImpl<>(new BatchGetResponseEnvelope(exception), new HashMap<>(), new ArrayList<>());
  }

  public static RestLiResponseData<BatchUpdateResponseEnvelope> buildBatchUpdateResponseData(HttpStatus status,
                                                                Map<?, BatchResponseEnvelope.BatchResponseEntry> batchResponseMap)
  {
    return new RestLiResponseDataImpl<>(new BatchUpdateResponseEnvelope(status, batchResponseMap), new HashMap<>(),
        new ArrayList<>());
  }

  public static RestLiResponseData<BatchUpdateResponseEnvelope> buildBatchUpdateResponseData(RestLiServiceException exception)
  {
    return new RestLiResponseDataImpl<>(new BatchUpdateResponseEnvelope(exception), new HashMap<>(), new ArrayList<>());
  }

  public static RestLiResponseData<BatchPartialUpdateResponseEnvelope> buildBatchPartialUpdateResponseData(HttpStatus status,
                                                                       Map<?, BatchResponseEnvelope.BatchResponseEntry> batchResponseMap)
  {
    return new RestLiResponseDataImpl<>(new BatchPartialUpdateResponseEnvelope(status, batchResponseMap),
        new HashMap<>(), new ArrayList<>());
  }

  public static RestLiResponseData<BatchPartialUpdateResponseEnvelope> buildBatchPartialUpdateResponseData(RestLiServiceException exception)
  {
    return new RestLiResponseDataImpl<>(new BatchPartialUpdateResponseEnvelope(exception), new HashMap<>(),
        new ArrayList<>());
  }

  public static RestLiResponseData<BatchDeleteResponseEnvelope> buildBatchDeleteResponseData(HttpStatus status,
                                                                Map<?, BatchResponseEnvelope.BatchResponseEntry> batchResponseMap)
  {
    return new RestLiResponseDataImpl<>(new BatchDeleteResponseEnvelope(status, batchResponseMap), new HashMap<>(),
        new ArrayList<>());
  }

  public static RestLiResponseData<BatchDeleteResponseEnvelope> buildBatchDeleteResponseData(RestLiServiceException exception)
  {
    return new RestLiResponseDataImpl<>(new BatchDeleteResponseEnvelope(exception), new HashMap<>(), new ArrayList<>());
  }
}
