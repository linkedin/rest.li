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


import com.linkedin.data.DataMap;
import com.linkedin.data.template.FieldDef;
import com.linkedin.restli.common.ActionResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.server.AugmentedRestLiResponseData;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.ActionResult;
import java.util.HashMap;
import java.util.Map;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author kparikh
 */
public class TestActionResponseBuilder
{
  private static final FieldDef<Long> LONG_RETURN = new FieldDef<Long>("longReturn", Long.class);

  @DataProvider(name = "testData")
  public Object[][] dataProvider()
  {
    return new Object[][]
        {
            {1L, HttpStatus.S_200_OK, 1L},
            {new ActionResult<Long>(1L, HttpStatus.S_202_ACCEPTED), HttpStatus.S_202_ACCEPTED, 1L}
        };
  }

  @Test(dataProvider = "testData")
  public void testBuilder(Object result, HttpStatus httpStatus, long returnValue)
  {
    Map<String, String> headers = ResponseBuilderUtil.getHeaders();
    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor();
    RoutingResult routingResult = new RoutingResult(null, mockDescriptor);

    ActionResponseBuilder actionResponseBuilder = new ActionResponseBuilder();
    AugmentedRestLiResponseData responseData = actionResponseBuilder.buildRestLiResponseData(null,
                                                                                             routingResult,
                                                                                             result,
                                                                                             headers);
    PartialRestResponse restResponse = actionResponseBuilder.buildResponse(routingResult, responseData);

    EasyMock.verify(mockDescriptor);
    ResponseBuilderUtil.validateHeaders(restResponse, headers);
    Assert.assertEquals(restResponse.getStatus(), httpStatus);
    Assert.assertEquals((restResponse.getEntity()), getActionResponse(returnValue));
  }

  @SuppressWarnings("unchecked")
  private static ResourceMethodDescriptor getMockResourceMethodDescriptor()
  {
    ResourceMethodDescriptor mockDescriptor = EasyMock.createMock(ResourceMethodDescriptor.class);

    EasyMock.expect(mockDescriptor.getActionReturnRecordDataSchema()).andReturn(LONG_RETURN.getField().getRecord()).once();
    EasyMock.expect(((FieldDef<Long>)mockDescriptor.getActionReturnFieldDef())).andReturn(LONG_RETURN).once();
    EasyMock.expect(mockDescriptor.getMethodType()).andReturn(ResourceMethod.ACTION).once();
    EasyMock.replay(mockDescriptor);
    return mockDescriptor;
  }

  private static ActionResponse<Long> getActionResponse(long returnValue)
  {
    DataMap dataMap = new DataMap();
    dataMap.put(LONG_RETURN.getName(), returnValue);
    return new ActionResponse<Long>(dataMap, LONG_RETURN, LONG_RETURN.getField().getRecord());
  }
}
