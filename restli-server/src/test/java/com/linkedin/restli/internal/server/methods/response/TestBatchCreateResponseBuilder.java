/*
   Copyright (c) 2014 LinkedIn Corp.

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


package com.linkedin.restli.internal.server.methods.response;


import com.linkedin.pegasus.generator.examples.Foo;
import com.linkedin.r2.message.Response;
import com.linkedin.restli.common.BatchCreateIdResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.server.AugmentedRestLiResponseData;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.RestLiServiceException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author kparikh
 */
public class TestBatchCreateResponseBuilder
{
  @Test
  @SuppressWarnings("unchecked")
  public void testBuilder()
  {
    List<CreateResponse> createResponses = Arrays.asList(new CreateResponse(1L), new CreateResponse(2L));
    BatchCreateResult<Long, Foo> results =
        new BatchCreateResult<Long, Foo>(createResponses);
    Map<String, String> headers = ResponseBuilderUtil.getHeaders();

    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor();
    RoutingResult routingResult = new RoutingResult(null, mockDescriptor);

    BatchCreateResponseBuilder responseBuilder = new BatchCreateResponseBuilder(null);
    AugmentedRestLiResponseData responseData = responseBuilder.buildRestLiResponseData(null,
                                                                                       routingResult,
                                                                                       results,
                                                                                       headers);
    PartialRestResponse restResponse = responseBuilder.buildResponse(routingResult, responseData);

    EasyMock.verify(mockDescriptor);
    ResponseBuilderUtil.validateHeaders(restResponse, headers);
    Assert.assertEquals(restResponse.getEntity(),
                        new BatchCreateIdResponse<Long>((List<com.linkedin.restli.common.CreateIdStatus<Long>>) responseData.getCollectionResponse()));
    Assert.assertEquals(restResponse.getStatus(), HttpStatus.S_200_OK);
  }

  private static ResourceMethodDescriptor getMockResourceMethodDescriptor()
  {
    ResourceMethodDescriptor mockDescriptor = EasyMock.createMock(ResourceMethodDescriptor.class);
    EasyMock.expect(mockDescriptor.getMethodType()).andReturn(ResourceMethod.BATCH_CREATE).once();
    EasyMock.replay(mockDescriptor);
    return mockDescriptor;
  }

  @DataProvider(name = "testData")
  public Object[][] dataProvider()
  {
    return new Object[][]
        {
            {new BatchCreateResult<Long, Foo>(Arrays.asList(new CreateResponse(1L), null)),
                "Unexpected null encountered. Null element inside of List inside of a BatchCreateResult returned by the resource method: "},
            {new BatchCreateResult<Long, Foo>(null),
                "Unexpected null encountered. Null List inside of a BatchCreateResult returned by the resource method: "}
        };
  }

  @Test(dataProvider = "testData")
  public void testBuilderExceptions(Object result, String expectedErrorMessage)
  {
    Map<String, String> headers = ResponseBuilderUtil.getHeaders();
    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor();
    RoutingResult routingResult = new RoutingResult(null, mockDescriptor);
    BatchCreateResponseBuilder responseBuilder = new BatchCreateResponseBuilder(null);
    try
    {
      responseBuilder.buildRestLiResponseData(null, routingResult, result, headers);
      Assert.fail("buildRestLiResponseData should have thrown an exception because of null elements");
    }
    catch (RestLiServiceException e)
    {
      Assert.assertTrue(e.getMessage().contains(expectedErrorMessage));
    }
  }
}
