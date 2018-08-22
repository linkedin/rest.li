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


package com.linkedin.restli.internal.server.response;


import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author kparikh
 */
public class TestUpdateResponseBuilder
{
  private static final Map<ResourceMethod, EmptyResponseBuilder<?>> BUILDERS = new HashMap<>();
  static
  {
    BUILDERS.put(ResourceMethod.UPDATE, new UpdateResponseBuilder());
    BUILDERS.put(ResourceMethod.DELETE, new DeleteResponseBuilder());
  }

  @Test(dataProvider = "builderData")
  public <D extends RestLiResponseData<? extends EmptyResponseEnvelope>> void testBuilder(ResourceMethod resourceMethod)
  {
    HttpStatus status = HttpStatus.S_200_OK;
    UpdateResponse updateResponse = new UpdateResponse(status);
    Map<String, String> headers = ResponseBuilderUtil.getHeaders();

    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor();
    RoutingResult routingResult = new RoutingResult(null, mockDescriptor);

    @SuppressWarnings("unchecked")
    EmptyResponseBuilder<D> updateResponseBuilder = (EmptyResponseBuilder<D>) BUILDERS.get(resourceMethod);
    D responseData = updateResponseBuilder.buildRestLiResponseData(null,
        routingResult,
        updateResponse,
        headers,
        Collections.emptyList());
    RestLiResponse restLiResponse = updateResponseBuilder.buildResponse(routingResult, responseData);

    EasyMock.verify(mockDescriptor);
    Assert.assertEquals(responseData.getResourceMethod(), resourceMethod);
    Assert.assertEquals(responseData.getResponseEnvelope().getResourceMethod(), resourceMethod);
    ResponseBuilderUtil.validateHeaders(restLiResponse, headers);
    Assert.assertEquals(restLiResponse.getStatus(), status);
  }

  @DataProvider
  public Object[][] builderData()
  {
    return BUILDERS.keySet().stream().map(m -> new Object[] {m}).toArray(Object[][]::new);
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
      updateResponseBuilder.buildRestLiResponseData(null, routingResult, updateResponse, headers, Collections.emptyList());
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
    EasyMock.replay(mockDescriptor);
    return mockDescriptor;
  }
}
