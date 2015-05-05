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


import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.IdResponse;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.server.RestLiResponseEnvelope;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiServiceException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author kparikh
 */
public class TestCreateResponseBuilder
{
  @DataProvider(name = "testData")
  public Object[][] dataProvider()
  {
    return new Object[][]
        {
            {AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "/foo/a=a&b=1", "a=a&b=1"},
            {AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/foo/(a:a,b:1)", "(a:a,b:1)"}
        };
  }

  @Test(dataProvider = "testData")
  public void testBuilder(ProtocolVersion protocolVersion, String location, String id) throws URISyntaxException
  {
    CompoundKey compoundKey = new CompoundKey().append("a", "a").append("b", 1);
    CreateResponse createResponse = new CreateResponse(compoundKey);
    IdResponse<CompoundKey> idResponse = new IdResponse<CompoundKey>(compoundKey);
    RestRequest restRequest = new RestRequestBuilder(new URI("/foo")).build();
    Map<String, String> headers = ResponseBuilderUtil.getHeaders();
    headers.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, protocolVersion.toString());
    // the headers passed in are modified
    Map<String, String> expectedHeaders = new HashMap<String, String>(headers);

    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor();
    ResourceContext mockContext = getMockResourceContext(protocolVersion);
    RoutingResult routingResult = new RoutingResult(mockContext, mockDescriptor);

    CreateResponseBuilder createResponseBuilder = new CreateResponseBuilder();
    RestLiResponseEnvelope responseData = createResponseBuilder.buildRestLiResponseData(restRequest,
                                                                                             routingResult,
                                                                                             createResponse,
                                                                                             headers);
    PartialRestResponse partialRestResponse = createResponseBuilder.buildResponse(routingResult, responseData);

    expectedHeaders.put(RestConstants.HEADER_LOCATION, location);
    if (protocolVersion.equals(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion()))
    {
      expectedHeaders.put(RestConstants.HEADER_ID, id);
    }
    else
    {
      expectedHeaders.put(RestConstants.HEADER_RESTLI_ID, id);
    }

    EasyMock.verify(mockContext, mockDescriptor);
    ResponseBuilderUtil.validateHeaders(partialRestResponse, expectedHeaders);
    Assert.assertEquals(partialRestResponse.getStatus(), HttpStatus.S_201_CREATED);
    Assert.assertEquals(partialRestResponse.getEntity(), idResponse);
  }

  @Test
  public void testBuilderException()
      throws URISyntaxException
  {
    CompoundKey compoundKey = new CompoundKey().append("a", "a").append("b", 1);
    CreateResponse createResponse = new CreateResponse(compoundKey, null);
    RestRequest restRequest = new RestRequestBuilder(new URI("/foo")).build();
    ProtocolVersion protocolVersion = AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion();
    Map<String, String> headers = ResponseBuilderUtil.getHeaders();
    headers.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, protocolVersion.toString());

    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor();
    ResourceContext mockContext = getMockResourceContext(protocolVersion);
    RoutingResult routingResult = new RoutingResult(mockContext, mockDescriptor);

    CreateResponseBuilder createResponseBuilder = new CreateResponseBuilder();
    try
    {
      createResponseBuilder.buildRestLiResponseData(restRequest, routingResult, createResponse, headers);
      Assert.fail("buildRestLiResponseData should have thrown an exception because the status is null!");
    }
    catch (RestLiServiceException e)
    {
      Assert.assertTrue(e.getMessage().contains("Unexpected null encountered. HttpStatus is null inside of a CreateResponse from the resource method: "));
    }
  }


  private static ResourceMethodDescriptor getMockResourceMethodDescriptor()
  {
    ResourceMethodDescriptor mockDescriptor = EasyMock.createMock(ResourceMethodDescriptor.class);
    EasyMock.replay(mockDescriptor);
    return mockDescriptor;
  }

  private static ResourceContext getMockResourceContext(ProtocolVersion protocolVersion)
  {
    ServerResourceContext mockContext = EasyMock.createMock(ServerResourceContext.class);
    EasyMock.expect(mockContext.getRestliProtocolVersion()).andReturn(protocolVersion).once();
    EasyMock.replay(mockContext);
    return mockContext;
  }
}
