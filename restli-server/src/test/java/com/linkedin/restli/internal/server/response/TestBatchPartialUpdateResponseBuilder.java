/*
   Copyright (c) 2018 LinkedIn Corp.

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

import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.transform.filter.request.MaskOperation;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.UpdateEntityStatus;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.server.ResponseType;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.BatchUpdateEntityResult;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.ProjectionMode;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.TestRecord;
import com.linkedin.restli.server.UpdateEntityResponse;
import com.linkedin.restli.server.UpdateResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;


/**
 * Tests for {@link BatchPartialUpdateResponseBuilder}.
 *
 * @author Evan Williams
 */
public class TestBatchPartialUpdateResponseBuilder
{
  /**
   * Ensures that the response builder can correctly process inputs without any errors. This involves varying result
   * types, status codes, and values of the {@link com.linkedin.restli.common.RestConstants#RETURN_ENTITY_PARAM} query
   * parameter.
   *
   * @param result result object to use as input to this builder.
   * @param isReturnEntityRequested semantic value of the "return entity" query parameter.
   * @param expectedRecords expected records in response data, if any.
   */
  @Test(dataProvider = "responseData")
  @SuppressWarnings("unchecked")
  public void testBuilder(BatchUpdateResult<Long, TestRecord> result, boolean isReturnEntityRequested, Map<?, RecordTemplate> expectedRecords)
  {
    Map<String, String> headers = ResponseBuilderUtil.getHeaders();

    RoutingResult routingResult = getMockRoutingResult(isReturnEntityRequested, null);

    BatchPartialUpdateResponseBuilder batchPartialUpdateResponseBuilder = new BatchPartialUpdateResponseBuilder(new ErrorResponseBuilder());
    RestLiResponseData<BatchPartialUpdateResponseEnvelope> responseData = batchPartialUpdateResponseBuilder.buildRestLiResponseData(null,
        routingResult,
        result,
        headers,
        Collections.emptyList());

    RestLiResponse restLiResponse = batchPartialUpdateResponseBuilder.buildResponse(routingResult, responseData);

    ResponseBuilderUtil.validateHeaders(restLiResponse, headers);
    Assert.assertEquals(restLiResponse.getStatus(), HttpStatus.S_200_OK);
    Assert.assertEquals(responseData.getResponseType(), ResponseType.BATCH_ENTITIES);
    Assert.assertEquals(responseData.getResourceMethod(), ResourceMethod.BATCH_PARTIAL_UPDATE);
    Assert.assertEquals(responseData.getResponseEnvelope().getResourceMethod(), ResourceMethod.BATCH_PARTIAL_UPDATE);

    final Map<Long, BatchResponseEnvelope.BatchResponseEntry> batchResponseMap = (Map<Long, BatchResponseEnvelope.BatchResponseEntry>) responseData.getResponseEnvelope().getBatchResponseMap();
    Assert.assertNotNull(batchResponseMap);

    for (Map.Entry<Long, BatchResponseEnvelope.BatchResponseEntry> entry : batchResponseMap.entrySet())
    {
      final HttpStatus expectedStatus = result.getResults().get(entry.getKey()).getStatus();
      BatchResponseEnvelope.BatchResponseEntry batchResponseEntry = entry.getValue();
      Assert.assertNotNull(batchResponseEntry);
      Assert.assertFalse(batchResponseEntry.hasException());
      Assert.assertEquals(batchResponseEntry.getStatus(), expectedStatus);

      UpdateStatus updateStatus = (UpdateStatus) batchResponseEntry.getRecord();
      Assert.assertNotNull(updateStatus);
      Assert.assertFalse(updateStatus.hasError());
      Assert.assertEquals(updateStatus.getStatus().intValue(), expectedStatus.getCode());

      if (updateStatus instanceof UpdateEntityStatus)
      {
        UpdateEntityStatus<TestRecord> updateEntityStatus = (UpdateEntityStatus<TestRecord>) updateStatus;
        Assert.assertEquals(updateEntityStatus.hasEntity(), isReturnEntityRequested);

        // If no entity is to be returned, then these should both be null
        RecordTemplate record = updateEntityStatus.getEntity();
        RecordTemplate expectedRecord = expectedRecords.get(entry.getKey()); // can be null
        Assert.assertEquals(record, expectedRecord);
      }
    }
  }

  @DataProvider(name = "responseData")
  private Object[][] provideResponseData()
  {
    TestRecord record = new TestRecord().setIntField(2147).setDoubleField(21.47).setFloatField(123F).setLongField(456L);

    return new Object[][]
        {
            { new BatchUpdateResult<>(Collections.singletonMap(1L, new UpdateResponse(HttpStatus.S_200_OK))), true, new HashMap<>() },
            { new BatchUpdateResult<>(Collections.singletonMap(1L, new UpdateResponse(HttpStatus.S_200_OK))), false, new HashMap<>() },
            { new BatchUpdateResult<>(Collections.singletonMap(1L, new UpdateResponse(HttpStatus.S_400_BAD_REQUEST))), true, new HashMap<>() },
            { new BatchUpdateResult<>(Collections.singletonMap(1L, new UpdateResponse(HttpStatus.S_400_BAD_REQUEST))), false, new HashMap<>() },
            { new BatchUpdateEntityResult<>(Collections.singletonMap(1L, new UpdateEntityResponse<>(HttpStatus.S_200_OK, record))), true, Collections.singletonMap(1L, record) },
            { new BatchUpdateEntityResult<>(Collections.singletonMap(1L, new UpdateEntityResponse<>(HttpStatus.S_200_OK, record))), false, new HashMap<>() },
            { new BatchUpdateEntityResult<>(Collections.singletonMap(1L, new UpdateEntityResponse<>(HttpStatus.S_200_OK, null))), false, new HashMap<>() }
        };
  }

  /**
   * Ensures that {@link BatchPartialUpdateResponseBuilder#buildRestLiResponseData} fails when incorrect inputs are given.
   * This includes a null results or errors map, a null key in either of those, or a null value in the errors map.
   *
   * @param result BatchUpdateResult object to use as input to the builder.
   */
  @Test(dataProvider = "responseExceptionData")
  public void testBuilderException(BatchUpdateResult<Long, TestRecord> result)
  {
    Map<String, String> headers = ResponseBuilderUtil.getHeaders();
    BatchPartialUpdateResponseBuilder batchPartialUpdateResponseBuilder = new BatchPartialUpdateResponseBuilder(new ErrorResponseBuilder());

    RoutingResult routingResult = getMockRoutingResult(true, null);

    try
    {
      batchPartialUpdateResponseBuilder.buildRestLiResponseData(null, routingResult, result, headers, Collections.emptyList());
      Assert.fail("buildRestLiResponseData should have failed because of a null HTTP status or a null entity.");
    }
    catch (RestLiServiceException e)
    {
      Assert.assertTrue(e.getMessage().contains("Unexpected null encountered."));
    }
  }

  @DataProvider(name = "responseExceptionData")
  private Object[][] provideResponseExceptionData()
  {
    return new Object[][]
        {
            { new BatchUpdateResult<>(null) }, // null results map
            { new BatchUpdateResult<>(Collections.singletonMap(1L, new UpdateResponse(HttpStatus.S_200_OK)), null) }, // null errors maps
            { new BatchUpdateResult<>(Collections.singletonMap(null, new UpdateResponse(HttpStatus.S_200_OK))) }, // null id in results map
            { new BatchUpdateResult<>(new HashMap<>(), Collections.singletonMap(null, new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST))) }, // null id in errors map
            { new BatchUpdateResult<>(new HashMap<>(), Collections.singletonMap(1L, null)) } // null value in errors map
        };
  }

  /**
   * Ensures that the returned entities are properly projected when a projection mask is passed into the response builder.
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testProjectionInBuildRestLiResponseData()
  {
    final Long id = 1L;

    TestRecord record = new TestRecord().setIntField(2147).setDoubleField(21.47).setFloatField(123F).setLongField(456L);
    BatchUpdateEntityResult<Long, TestRecord> result = new BatchUpdateEntityResult<>(Collections.singletonMap(id, new UpdateEntityResponse<>(HttpStatus.S_200_OK, record)));

    MaskTree maskTree = new MaskTree();
    maskTree.addOperation(new PathSpec("intField"), MaskOperation.POSITIVE_MASK_OP);

    Map<String, String> headers = ResponseBuilderUtil.getHeaders();
    RoutingResult routingResult = getMockRoutingResult(true, maskTree);

    BatchPartialUpdateResponseBuilder batchPartialUpdateResponseBuilder = new BatchPartialUpdateResponseBuilder(new ErrorResponseBuilder());
    RestLiResponseData<BatchPartialUpdateResponseEnvelope> responseData = batchPartialUpdateResponseBuilder.buildRestLiResponseData(null,
        routingResult,
        result,
        headers,
        Collections.emptyList());

    UpdateEntityStatus<TestRecord> updateEntityStatus = (UpdateEntityStatus<TestRecord>) responseData.getResponseEnvelope().getBatchResponseMap().get(id).getRecord();
    Assert.assertNotNull(updateEntityStatus);

    RecordTemplate returnedRecord = updateEntityStatus.getEntity();
    Assert.assertEquals(returnedRecord.data().size(), 1, "Expected response record to be projected down to one field.");
    Assert.assertEquals((int) returnedRecord.data().get("intField"), 2147, "Expected response record intField to match original.");
  }

  private static RoutingResult getMockRoutingResult(boolean isReturnEntityRequested, MaskTree projectionMask)
  {
    ServerResourceContext mockServerResourceContext = mock(ServerResourceContext.class);
    when(mockServerResourceContext.getProjectionMode()).thenReturn(ProjectionMode.AUTOMATIC);
    when(mockServerResourceContext.getProjectionMask()).thenReturn(projectionMask);
    when(mockServerResourceContext.isReturnEntityRequested()).thenReturn(isReturnEntityRequested);
    when(mockServerResourceContext.getRestliProtocolVersion()).thenReturn(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion());
    ResourceMethodDescriptor mockResourceMethodDescriptor = mock(ResourceMethodDescriptor.class);
    return new RoutingResult(mockServerResourceContext, mockResourceMethodDescriptor);
  }
}
