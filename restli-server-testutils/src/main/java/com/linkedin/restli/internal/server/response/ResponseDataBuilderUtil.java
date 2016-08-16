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

import java.net.HttpCookie;
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

  public static RestLiResponseData buildGetResponseData(HttpStatus status, RecordTemplate getResponse)
  {
    final RestLiResponseDataImpl responseData =
        new RestLiResponseDataImpl(status, new HashMap<String, String>(), new ArrayList<HttpCookie>());
    responseData.setResponseEnvelope(new GetResponseEnvelope(getResponse, responseData));
    return responseData;
  }

  public static RestLiResponseData buildGetResponseData(RestLiServiceException exception)
  {
    final RestLiResponseDataImpl responseData =
        new RestLiResponseDataImpl(exception, new HashMap<String, String>(), new ArrayList<HttpCookie>());
    responseData.setResponseEnvelope(new GetResponseEnvelope(null, responseData));
    return responseData;
  }

  public static RestLiResponseData buildCreateResponseData(HttpStatus status, RecordTemplate createResponse)
  {
    final RestLiResponseDataImpl responseData =
        new RestLiResponseDataImpl(status, new HashMap<String, String>(), new ArrayList<HttpCookie>());
    responseData.setResponseEnvelope(new CreateResponseEnvelope(createResponse, responseData));
    return responseData;
  }

  public static RestLiResponseData buildCreateResponseData(RestLiServiceException exception)
  {
    final RestLiResponseDataImpl responseData =
        new RestLiResponseDataImpl(exception, new HashMap<String, String>(), new ArrayList<HttpCookie>());
    responseData.setResponseEnvelope(new CreateResponseEnvelope(null, responseData));
    return responseData;
  }

  public static RestLiResponseData buildActionResponseData(HttpStatus status, RecordTemplate actionResponse)
  {
    final RestLiResponseDataImpl responseData =
        new RestLiResponseDataImpl(status, new HashMap<String, String>(), new ArrayList<HttpCookie>());
    responseData.setResponseEnvelope(new ActionResponseEnvelope(actionResponse, responseData));
    return responseData;
  }

  public static RestLiResponseData buildActionResponseData(RestLiServiceException exception)
  {
    final RestLiResponseDataImpl responseData =
        new RestLiResponseDataImpl(exception, new HashMap<String, String>(), new ArrayList<HttpCookie>());
    responseData.setResponseEnvelope(new ActionResponseEnvelope(null, responseData));
    return responseData;
  }

  public static RestLiResponseData buildFinderResponseData(HttpStatus status,
                                                           List<? extends RecordTemplate> collectionResponse,
                                                           CollectionMetadata collectionResponsePaging,
                                                           RecordTemplate collectionResponseCustomMetadata)
  {
    final RestLiResponseDataImpl responseData =
        new RestLiResponseDataImpl(status, new HashMap<String, String>(), new ArrayList<HttpCookie>());
    responseData.setResponseEnvelope(
        new FinderResponseEnvelope(collectionResponse, collectionResponsePaging, collectionResponseCustomMetadata,
                                   responseData));
    return responseData;
  }

  public static RestLiResponseData buildFinderResponseData(RestLiServiceException exception)
  {
    final RestLiResponseDataImpl responseData =
        new RestLiResponseDataImpl(exception, new HashMap<String, String>(), new ArrayList<HttpCookie>());
    responseData.setResponseEnvelope(new FinderResponseEnvelope(null, null, null, responseData));
    return responseData;
  }

  public static RestLiResponseData buildGetAllResponseData(HttpStatus status,
                                                           List<? extends RecordTemplate> collectionResponse,
                                                           CollectionMetadata collectionResponsePaging,
                                                           RecordTemplate collectionResponseCustomMetadata)
  {
    final RestLiResponseDataImpl responseData =
        new RestLiResponseDataImpl(status, new HashMap<String, String>(), new ArrayList<HttpCookie>());
    responseData.setResponseEnvelope(
        new GetAllResponseEnvelope(collectionResponse, collectionResponsePaging, collectionResponseCustomMetadata,
                                   responseData));
    return responseData;
  }

  public static RestLiResponseData buildGetAllResponseData(RestLiServiceException exception)
  {
    final RestLiResponseDataImpl responseData =
        new RestLiResponseDataImpl(exception, new HashMap<String, String>(), new ArrayList<HttpCookie>());
    responseData.setResponseEnvelope(new GetAllResponseEnvelope(null, null, null, responseData));
    return responseData;
  }

  public static RestLiResponseData buildUpdateResponseData(HttpStatus status)
  {
    final RestLiResponseDataImpl responseData =
        new RestLiResponseDataImpl(status, new HashMap<String, String>(), new ArrayList<HttpCookie>());
    responseData.setResponseEnvelope(new UpdateResponseEnvelope(responseData));
    return responseData;
  }

  public static RestLiResponseData buildUpdateResponseData(RestLiServiceException exception)
  {
    final RestLiResponseDataImpl responseData =
        new RestLiResponseDataImpl(exception, new HashMap<String, String>(), new ArrayList<HttpCookie>());
    responseData.setResponseEnvelope(new UpdateResponseEnvelope(responseData));
    return responseData;
  }

  public static RestLiResponseData buildPartialUpdateResponseData(HttpStatus status)
  {
    final RestLiResponseDataImpl responseData =
        new RestLiResponseDataImpl(status, new HashMap<String, String>(), new ArrayList<HttpCookie>());
    responseData.setResponseEnvelope(new PartialUpdateResponseEnvelope(responseData));
    return responseData;
  }

  public static RestLiResponseData buildPartialUpdateResponseData(RestLiServiceException exception)
  {
    final RestLiResponseDataImpl responseData =
        new RestLiResponseDataImpl(exception, new HashMap<String, String>(), new ArrayList<HttpCookie>());
    responseData.setResponseEnvelope(new PartialUpdateResponseEnvelope(responseData));
    return responseData;
  }

  public static RestLiResponseData buildOptionsResponseData(HttpStatus status)
  {
    final RestLiResponseDataImpl responseData =
        new RestLiResponseDataImpl(status, new HashMap<String, String>(), new ArrayList<HttpCookie>());
    responseData.setResponseEnvelope(new OptionsResponseEnvelope(responseData));
    return responseData;
  }

  public static RestLiResponseData buildOptionsResponseData(RestLiServiceException exception)
  {
    final RestLiResponseDataImpl responseData =
        new RestLiResponseDataImpl(exception, new HashMap<String, String>(), new ArrayList<HttpCookie>());
    responseData.setResponseEnvelope(new OptionsResponseEnvelope(responseData));
    return responseData;
  }

  public static RestLiResponseData buildDeleteResponseData(HttpStatus status)
  {
    final RestLiResponseDataImpl responseData =
        new RestLiResponseDataImpl(status, new HashMap<String, String>(), new ArrayList<HttpCookie>());
    responseData.setResponseEnvelope(new DeleteResponseEnvelope(responseData));
    return responseData;
  }

  public static RestLiResponseData buildDeleteResponseData(RestLiServiceException exception)
  {
    final RestLiResponseDataImpl responseData =
        new RestLiResponseDataImpl(exception, new HashMap<String, String>(), new ArrayList<HttpCookie>());
    responseData.setResponseEnvelope(new DeleteResponseEnvelope(responseData));
    return responseData;
  }

  public static RestLiResponseData buildBatchCreateResponseData(HttpStatus status,
                                                                List<BatchCreateResponseEnvelope.CollectionCreateResponseItem> responseItems)
  {
    final RestLiResponseDataImpl responseData =
        new RestLiResponseDataImpl(status, new HashMap<String, String>(), new ArrayList<HttpCookie>());
    responseData.setResponseEnvelope(new BatchCreateResponseEnvelope(responseItems, responseData));
    return responseData;
  }

  public static RestLiResponseData buildBatchCreateResponseData(RestLiServiceException exception)
  {
    final RestLiResponseDataImpl responseData =
        new RestLiResponseDataImpl(exception, new HashMap<String, String>(), new ArrayList<HttpCookie>());
    responseData.setResponseEnvelope(new BatchCreateResponseEnvelope(null, responseData));
    return responseData;
  }

  public static RestLiResponseData buildBatchGetResponseData(HttpStatus status,
                                                             Map<? extends Object, BatchResponseEnvelope.BatchResponseEntry> batchResponseMap)
  {
    final RestLiResponseDataImpl responseData =
        new RestLiResponseDataImpl(status, new HashMap<String, String>(), new ArrayList<HttpCookie>());
    responseData.setResponseEnvelope(new BatchUpdateResponseEnvelope(batchResponseMap, responseData));
    return responseData;
  }

  public static RestLiResponseData buildBatchGetResponseData(RestLiServiceException exception)
  {
    final RestLiResponseDataImpl responseData =
        new RestLiResponseDataImpl(exception, new HashMap<String, String>(), new ArrayList<HttpCookie>());
    responseData.setResponseEnvelope(new BatchGetResponseEnvelope(null, responseData));
    return responseData;
  }

  public static RestLiResponseData buildBatchUpdateResponseData(HttpStatus status,
                                                                Map<? extends Object, BatchResponseEnvelope.BatchResponseEntry> batchResponseMap)
  {
    final RestLiResponseDataImpl responseData =
        new RestLiResponseDataImpl(status, new HashMap<String, String>(), new ArrayList<HttpCookie>());
    responseData.setResponseEnvelope(new BatchUpdateResponseEnvelope(batchResponseMap, responseData));
    return responseData;
  }

  public static RestLiResponseData buildBatchUpdateResponseData(RestLiServiceException exception)
  {
    final RestLiResponseDataImpl responseData =
        new RestLiResponseDataImpl(exception, new HashMap<String, String>(), new ArrayList<HttpCookie>());
    responseData.setResponseEnvelope(new BatchUpdateResponseEnvelope(null, responseData));
    return responseData;
  }

  public static RestLiResponseData buildBatchPartialUpdateResponseData(HttpStatus status,
                                                                       Map<? extends Object, BatchResponseEnvelope.BatchResponseEntry> batchResponseMap)
  {
    final RestLiResponseDataImpl responseData =
        new RestLiResponseDataImpl(status, new HashMap<String, String>(), new ArrayList<HttpCookie>());
    responseData.setResponseEnvelope(new BatchPartialUpdateResponseEnvelope(batchResponseMap, responseData));
    return responseData;
  }

  public static RestLiResponseData buildBatchPartialUpdateResponseData(RestLiServiceException exception)
  {
    final RestLiResponseDataImpl responseData =
        new RestLiResponseDataImpl(exception, new HashMap<String, String>(), new ArrayList<HttpCookie>());
    responseData.setResponseEnvelope(new BatchPartialUpdateResponseEnvelope(null, responseData));
    return responseData;
  }

  public static RestLiResponseData buildBatchDeleteResponseData(HttpStatus status,
                                                                Map<? extends Object, BatchResponseEnvelope.BatchResponseEntry> batchResponseMap)
  {
    final RestLiResponseDataImpl responseData =
        new RestLiResponseDataImpl(status, new HashMap<String, String>(), new ArrayList<HttpCookie>());
    responseData.setResponseEnvelope(new BatchDeleteResponseEnvelope(batchResponseMap, responseData));
    return responseData;
  }

  public static RestLiResponseData buildBatchDeleteResponseData(RestLiServiceException exception)
  {
    final RestLiResponseDataImpl responseData =
        new RestLiResponseDataImpl(exception, new HashMap<String, String>(), new ArrayList<HttpCookie>());
    responseData.setResponseEnvelope(new BatchDeleteResponseEnvelope(null, responseData));
    return responseData;
  }
}
