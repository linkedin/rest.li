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


import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.HeaderUtil;
import com.linkedin.restli.internal.server.AugmentedRestLiResponseData;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.RestLiServiceException;
import java.util.HashMap;
import java.util.Map;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author kparikh
 */
public class TestErrorResponseBuilder
{
  @DataProvider(name = "testData")
  public Object[][] dataProvider()
  {
    return new Object[][]
        {
            {AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion()},
            {AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion()}
        };
  }

  @Test(dataProvider = "testData")
  public void testBuilder(ProtocolVersion protocolVersion)
  {
    Map<String, String> headers = ResponseBuilderUtil.getHeaders();
    Map<String, String> expectedHeaders = new HashMap<String, String>(headers);
    expectedHeaders.put(HeaderUtil.getErrorResponseHeaderName(protocolVersion), RestConstants.HEADER_VALUE_ERROR);

    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor();
    RoutingResult routingResult = new RoutingResult(null, mockDescriptor);

    RuntimeException runtimeException = new RuntimeException("Internal server error!");
    RestLiServiceException serviceException = new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                                                                         runtimeException);

    ErrorResponseBuilder errorResponseBuilder = new ErrorResponseBuilder();
    AugmentedRestLiResponseData responseData = errorResponseBuilder.buildRestLiResponseData(null,
                                                                                            routingResult,
                                                                                            serviceException,
                                                                                            headers);
    PartialRestResponse restResponse = errorResponseBuilder.buildResponse(routingResult, responseData);

    EasyMock.verify(mockDescriptor);
    ErrorResponse errorResponse = (ErrorResponse)restResponse.getEntity();
    Assert.assertEquals(errorResponse.getStatus(), new Integer(500));
    Assert.assertTrue(errorResponse.getMessage().contains(runtimeException.getMessage()));
  }

  private ResourceMethodDescriptor getMockResourceMethodDescriptor()
  {
    ResourceMethodDescriptor mockDescriptor = EasyMock.createMock(ResourceMethodDescriptor.class);
    EasyMock.expect(mockDescriptor.getMethodType()).andReturn(ResourceMethod.GET);
    EasyMock.replay(mockDescriptor);
    return mockDescriptor;
  }
}
