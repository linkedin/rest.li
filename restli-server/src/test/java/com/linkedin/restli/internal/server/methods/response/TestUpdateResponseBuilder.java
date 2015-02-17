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


import com.linkedin.r2.message.Response;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.server.AugmentedRestLiResponseData;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import java.util.HashMap;
import java.util.Map;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author kparikh
 */
public class TestUpdateResponseBuilder
{
  @Test
  public void testBuilder()
  {
    HttpStatus status = HttpStatus.S_200_OK;
    UpdateResponse updateResponse = new UpdateResponse(status);
    Map<String, String> headers = ResponseBuilderUtil.getHeaders();

    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor();
    RoutingResult routingResult = new RoutingResult(null, mockDescriptor);

    UpdateResponseBuilder updateResponseBuilder = new UpdateResponseBuilder();
    AugmentedRestLiResponseData responseData = updateResponseBuilder.buildRestLiResponseData(null,
                                                                                             routingResult,
                                                                                             updateResponse,
                                                                                             headers);
    PartialRestResponse partialRestResponse = updateResponseBuilder.buildResponse(routingResult, responseData);

    EasyMock.verify(mockDescriptor);
    ResponseBuilderUtil.validateHeaders(partialRestResponse, headers);
    Assert.assertEquals(partialRestResponse.getStatus(), status);
  }

  @Test
  public void testBuilderException()
  {
    UpdateResponse updateResponse = new UpdateResponse(null);
    Map<String, String> headers = ResponseBuilderUtil.getHeaders();
    UpdateResponseBuilder updateResponseBuilder = new UpdateResponseBuilder();

    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor();
    RoutingResult routingResult = new RoutingResult(null, mockDescriptor);

    try
    {
      updateResponseBuilder.buildRestLiResponseData(null, routingResult, updateResponse, headers);
      Assert.fail("buildRestLiResponseData should have failed because of a null HTTP status!");
    }
    catch (RestLiServiceException e)
    {
      Assert.assertTrue(e.getMessage().contains("Unexpected null encountered. HttpStatus is null inside of a UpdateResponse returned by the resource method: "));
    }
  }

  private static ResourceMethodDescriptor getMockResourceMethodDescriptor()
  {
    ResourceMethodDescriptor mockDescriptor = EasyMock.createMock(ResourceMethodDescriptor.class);
    EasyMock.expect(mockDescriptor.getMethodType()).andReturn(ResourceMethod.UPDATE).once();
    EasyMock.replay(mockDescriptor);
    return mockDescriptor;
  }
}
