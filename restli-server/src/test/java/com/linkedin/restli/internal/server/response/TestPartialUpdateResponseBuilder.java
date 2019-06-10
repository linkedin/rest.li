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
import com.linkedin.restli.internal.server.ResponseType;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.ProjectionMode;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.TestRecord;
import com.linkedin.restli.server.UpdateEntityResponse;
import com.linkedin.restli.server.UpdateResponse;
import java.util.Collections;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;


/**
 * Tests for {@link PartialUpdateResponseBuilder}.
 *
 * @author Evan Williams
 */
public class TestPartialUpdateResponseBuilder
{
  /**
   * Ensures that the response builder can correctly process inputs without any errors. This test involves varying
   * response object types, status codes, and {@link com.linkedin.restli.common.RestConstants#RETURN_ENTITY_PARAM}
   * query parameters.
   *
   * @param response UpdateResponse object to use as an input to the response builder.
   * @param expectedResponseType expected {@link ResponseType}.
   * @param isReturnEntityRequested semantic value of the "return entity" query parameter.
   * @param expectedRecord expected record in response data.
   */
  @Test(dataProvider = "responseData")
  public void testBuilder(UpdateResponse response, ResponseType expectedResponseType, boolean isReturnEntityRequested, RecordTemplate expectedRecord)
  {
    HttpStatus status = response.getStatus();
    Map<String, String> headers = ResponseBuilderUtil.getHeaders();

    RoutingResult routingResult = getMockRoutingResult(isReturnEntityRequested, null);

    PartialUpdateResponseBuilder partialUpdateResponseBuilder = new PartialUpdateResponseBuilder();
    RestLiResponseData<PartialUpdateResponseEnvelope> responseData = partialUpdateResponseBuilder.buildRestLiResponseData(null,
        routingResult,
        response,
        headers,
        Collections.emptyList());

    RestLiResponse restLiResponse = partialUpdateResponseBuilder.buildResponse(routingResult, responseData);

    Assert.assertEquals(responseData.getResourceMethod(), ResourceMethod.PARTIAL_UPDATE);
    Assert.assertEquals(responseData.getResponseEnvelope().getResourceMethod(), ResourceMethod.PARTIAL_UPDATE);
    Assert.assertEquals(responseData.getResponseEnvelope().getRecord(), expectedRecord);
    ResponseBuilderUtil.validateHeaders(restLiResponse, headers);
    Assert.assertEquals(restLiResponse.getStatus(), status);
    Assert.assertEquals(responseData.getResponseType(), expectedResponseType);
  }

  @DataProvider(name = "responseData")
  private Object[][] provideResponseData()
  {
    TestRecord record = new TestRecord().setIntField(2147).setDoubleField(21.47).setFloatField(123F).setLongField(456L);

    return new Object[][]
    {
        { new UpdateResponse(HttpStatus.S_200_OK), ResponseType.STATUS_ONLY, true, null },
        { new UpdateResponse(HttpStatus.S_200_OK), ResponseType.STATUS_ONLY, false, null },
        { new UpdateResponse(HttpStatus.S_400_BAD_REQUEST), ResponseType.STATUS_ONLY, true, null },
        { new UpdateResponse(HttpStatus.S_400_BAD_REQUEST), ResponseType.STATUS_ONLY, false, null },
        { new UpdateEntityResponse<>(HttpStatus.S_200_OK, record), ResponseType.SINGLE_ENTITY, true, record },
        { new UpdateEntityResponse<>(HttpStatus.S_200_OK, record), ResponseType.STATUS_ONLY, false, null },
        { new UpdateEntityResponse<>(HttpStatus.S_200_OK, null), ResponseType.STATUS_ONLY, false, null }
    };
  }

  /**
   * Ensures that the response builder fails when incorrect inputs are given. This includes
   * a null status or a null returned entity.
   *
   * @param response UpdateResponse object to use as input to the response builder.
   */
  @Test(dataProvider = "responseExceptionData")
  public void testBuilderException(UpdateResponse response)
  {
    Map<String, String> headers = ResponseBuilderUtil.getHeaders();
    PartialUpdateResponseBuilder partialUpdateResponseBuilder = new PartialUpdateResponseBuilder();

    RoutingResult routingResult = getMockRoutingResult(true, null);

    try
    {
      partialUpdateResponseBuilder.buildRestLiResponseData(null, routingResult, response, headers, Collections.emptyList());
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
            { new UpdateResponse(null) },
            { new UpdateEntityResponse<>(null, new TestRecord()) },
            { new UpdateEntityResponse<>(HttpStatus.S_400_BAD_REQUEST, null) },
            { new UpdateEntityResponse<>(HttpStatus.S_200_OK, null) },
            { new UpdateEntityResponse<>(null, null) }
        };
  }

  /**
   * Ensures that the returned entity is properly projected when a projection mask is passed into the response builder.
   */
  @Test
  public void testProjectionInBuildRestLiResponseData()
  {
    TestRecord record = new TestRecord().setIntField(2147).setDoubleField(21.47).setFloatField(123F).setLongField(456L);
    UpdateEntityResponse<TestRecord> response = new UpdateEntityResponse<>(HttpStatus.S_200_OK, record);

    MaskTree maskTree = new MaskTree();
    maskTree.addOperation(new PathSpec("intField"), MaskOperation.POSITIVE_MASK_OP);

    Map<String, String> headers = ResponseBuilderUtil.getHeaders();
    RoutingResult routingResult = getMockRoutingResult(true, maskTree);

    PartialUpdateResponseBuilder partialUpdateResponseBuilder = new PartialUpdateResponseBuilder();
    RestLiResponseData<PartialUpdateResponseEnvelope> responseData = partialUpdateResponseBuilder.buildRestLiResponseData(null,
        routingResult,
        response,
        headers,
        Collections.emptyList());

    RecordTemplate returnedRecord = responseData.getResponseEnvelope().getRecord();
    Assert.assertEquals(returnedRecord.data().size(), 1, "Expected response record to be projected down to one field.");
    Assert.assertEquals(returnedRecord.data().get("intField"), 2147, "Expected response record intField to match original.");
  }

  private static RoutingResult getMockRoutingResult(boolean isReturnEntityRequested, MaskTree projectionMask)
  {
    ServerResourceContext mockServerResourceContext = mock(ServerResourceContext.class);
    when(mockServerResourceContext.getProjectionMode()).thenReturn(ProjectionMode.AUTOMATIC);
    when(mockServerResourceContext.getProjectionMask()).thenReturn(projectionMask);
    when(mockServerResourceContext.isReturnEntityRequested()).thenReturn(isReturnEntityRequested);
    ResourceMethodDescriptor mockResourceMethodDescriptor = mock(ResourceMethodDescriptor.class);
    return new RoutingResult(mockServerResourceContext, mockResourceMethodDescriptor);
  }
}
