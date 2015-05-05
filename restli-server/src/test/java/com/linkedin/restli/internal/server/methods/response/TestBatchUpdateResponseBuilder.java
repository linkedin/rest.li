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
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.server.RestLiResponseEnvelope;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.easymock.EasyMock;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author kparikh
 */
public class TestBatchUpdateResponseBuilder
{
  @DataProvider(name = "testData")
  public Object[][] dataProvider()
  {
    CompoundKey c1 = new CompoundKey().append("a", "a1").append("b", 1);
    CompoundKey c2 = new CompoundKey().append("a", "a2").append("b", 2);
    CompoundKey c3 = new CompoundKey().append("a", "a3").append("b", 3);
    Map<CompoundKey, UpdateResponse> results = new HashMap<CompoundKey, UpdateResponse>();
    results.put(c1, new UpdateResponse(HttpStatus.S_202_ACCEPTED));
    results.put(c2, new UpdateResponse(HttpStatus.S_202_ACCEPTED));

    RestLiServiceException restLiServiceException = new RestLiServiceException(HttpStatus.S_404_NOT_FOUND);
    Map<CompoundKey, RestLiServiceException> errors = Collections.singletonMap(c3, restLiServiceException);

    BatchUpdateResult<CompoundKey, Foo> batchUpdateResult =
        new BatchUpdateResult<CompoundKey, Foo>(results, errors);

    Map<CompoundKey, UpdateResponse> keyOverlapResults = new HashMap<CompoundKey, UpdateResponse>();
    keyOverlapResults.put(c1, new UpdateResponse(HttpStatus.S_202_ACCEPTED));
    keyOverlapResults.put(c2, new UpdateResponse(HttpStatus.S_202_ACCEPTED));
    keyOverlapResults.put(c3, new UpdateResponse(HttpStatus.S_404_NOT_FOUND));
    BatchUpdateResult<CompoundKey, Foo> keyOverlapBatchUpdateResult =
        new BatchUpdateResult<CompoundKey, Foo>(keyOverlapResults, errors);

    UpdateStatus updateStatus = new UpdateStatus().setStatus(202);
    ErrorResponse errorResponse = new ErrorResponse().setStatus(404);

    Map<String, UpdateStatus> expectedProtocol1Results = new HashMap<String, UpdateStatus>();
    expectedProtocol1Results.put("a=a1&b=1", updateStatus);
    expectedProtocol1Results.put("a=a2&b=2", updateStatus);
    Map<String, ErrorResponse> expectedProtocol1Errors = new HashMap<String, ErrorResponse>();
    expectedProtocol1Errors.put("a=a3&b=3", errorResponse);

    Map<String, UpdateStatus> expectedProtocol2Results = new HashMap<String, UpdateStatus>();
    expectedProtocol2Results.put("(a:a1,b:1)", updateStatus);
    expectedProtocol2Results.put("(a:a2,b:2)", updateStatus);
    Map<String, ErrorResponse> expectedProtocol2Errors = new HashMap<String, ErrorResponse>();
    expectedProtocol2Errors.put("(a:a3,b:3)", errorResponse);

    return new Object[][]
        {
            {batchUpdateResult, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), expectedProtocol1Results, expectedProtocol1Errors},
            {batchUpdateResult, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), expectedProtocol2Results, expectedProtocol2Errors},
            {keyOverlapBatchUpdateResult, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), expectedProtocol2Results, expectedProtocol2Errors}
        };
  }

  @Test(dataProvider = "testData")
  @SuppressWarnings("unchecked")
  public void testBuilder(Object results,
                          ProtocolVersion protocolVersion,
                          Map<String, UpdateStatus> expectedResults,
                          Map<String, ErrorResponse> expectedErrors)
  {
    ResourceContext mockContext = getMockResourceContext(protocolVersion);
    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor();
    RoutingResult routingResult = new RoutingResult(mockContext, mockDescriptor);

    Map<String, String> headers = ResponseBuilderUtil.getHeaders();

    BatchUpdateResponseBuilder batchUpdateResponseBuilder = new BatchUpdateResponseBuilder(new ErrorResponseBuilder());
    RestLiResponseEnvelope responseData = batchUpdateResponseBuilder.buildRestLiResponseData(null,
                                                                                                  routingResult,
                                                                                                  results,
                                                                                                  headers);
    PartialRestResponse restResponse = batchUpdateResponseBuilder.buildResponse(routingResult, responseData);

    BatchResponse<UpdateStatus> batchResponse = (BatchResponse<UpdateStatus>) restResponse.getEntity();
    EasyMock.verify(mockContext, mockDescriptor);
    ResponseBuilderUtil.validateHeaders(restResponse, headers);
    Assert.assertEquals(batchResponse.getResults(), expectedResults);
    Assert.assertEquals(batchResponse.getErrors().size(), expectedErrors.size());
    for (Map.Entry<String, ErrorResponse> entry: batchResponse.getErrors().entrySet())
    {
      String key = entry.getKey();
      ErrorResponse value = entry.getValue();
      Assert.assertEquals(value.getStatus(), expectedErrors.get(key).getStatus());
    }
  }

  @Test
  public void testContextErrors()
  {
    BatchUpdateResponseBuilder builder = new BatchUpdateResponseBuilder(new ErrorResponseBuilder());
    ServerResourceContext context = EasyMock.createMock(ServerResourceContext.class);
    Map<Object,RestLiServiceException> errors = new HashMap<Object, RestLiServiceException>();
    RestLiServiceException exception = new RestLiServiceException(HttpStatus.S_402_PAYMENT_REQUIRED);
    errors.put("foo", exception);
    EasyMock.expect(context.getBatchKeyErrors()).andReturn(errors);
    EasyMock.replay(context);
    RoutingResult routingResult = new RoutingResult(context, null);
    RestLiResponseEnvelope envelope = builder.buildRestLiResponseData(null,
                                                                      routingResult,
                                                                      new BatchUpdateResult<Object, Integer>(Collections.<Object, UpdateResponse>emptyMap()),
                                                                      Collections.<String, String>emptyMap());
    Assert.assertEquals(envelope.getBatchResponseEnvelope().getBatchResponseMap().get("foo").getException(),
                        exception);
    Assert.assertEquals(envelope.getBatchResponseEnvelope().getBatchResponseMap().size(), 1);
  }

  @DataProvider(name = "unsupportedNullKeyMapData")
  public Object[][] unsupportedNullKeyMapData()
  {
    final CompoundKey c1 = new CompoundKey().append("a", "a1").append("b", 1);
    final Map<CompoundKey, UpdateResponse> results = new ConcurrentHashMap<CompoundKey, UpdateResponse>();
    results.put(c1, new UpdateResponse(HttpStatus.S_202_ACCEPTED));

    final BatchUpdateResult<CompoundKey, Foo> batchUpdateResult =
        new BatchUpdateResult<CompoundKey, Foo>(results, new ConcurrentHashMap<CompoundKey, RestLiServiceException>());
    final UpdateStatus updateStatus = new UpdateStatus().setStatus(202);

    final Map<String, UpdateStatus> expectedProtocol1Results = new HashMap<String, UpdateStatus>();
    expectedProtocol1Results.put("a=a1&b=1", updateStatus);
    final Map<String, UpdateStatus> expectedProtocol2Results = new HashMap<String, UpdateStatus>();
    expectedProtocol2Results.put("(a:a1,b:1)", updateStatus);

    return new Object[][]
        {
            {batchUpdateResult, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), expectedProtocol1Results},
            {batchUpdateResult, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), expectedProtocol2Results}
        };
  }

 /* Note that we use also need to test using java.util.concurrent.ConcurrentHashMap. This is because rest.li checks
  * for the presence of nulls returned from maps which are returned from resource methods. The checking for nulls
  * is prone to a NullPointerException since contains(null) can throw an NPE from certain map implementations such as
  * java.util.concurrent.ConcurrentHashMap. We want to make sure our check for the presence of nulls is done in a
  * way that doesn't throw an NullPointerException.
  */
  @Test(dataProvider = "unsupportedNullKeyMapData")
  @SuppressWarnings("unchecked")
  public void unsupportedNullKeyMapTest(Object results, ProtocolVersion protocolVersion, Map<String, UpdateStatus> expectedResults)
  {
    ResourceContext mockContext = getMockResourceContext(protocolVersion);
    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor();
    RoutingResult routingResult = new RoutingResult(mockContext, mockDescriptor);

    Map<String, String> headers = ResponseBuilderUtil.getHeaders();

    BatchUpdateResponseBuilder batchUpdateResponseBuilder = new BatchUpdateResponseBuilder(new ErrorResponseBuilder());
    RestLiResponseEnvelope responseData = batchUpdateResponseBuilder.buildRestLiResponseData(null,
        routingResult,
        results,
        headers);
    PartialRestResponse restResponse = batchUpdateResponseBuilder.buildResponse(routingResult, responseData);

    BatchResponse<UpdateStatus> batchResponse = (BatchResponse<UpdateStatus>) restResponse.getEntity();
    EasyMock.verify(mockContext, mockDescriptor);
    ResponseBuilderUtil.validateHeaders(restResponse, headers);
    Assert.assertEquals(batchResponse.getResults(), expectedResults);
  }

  private static ResourceContext getMockResourceContext(ProtocolVersion protocolVersion)
  {
    ServerResourceContext mockContext = EasyMock.createMock(ServerResourceContext.class);
    EasyMock.expect(mockContext.getBatchKeyErrors()).andReturn(Collections.<Object, RestLiServiceException>emptyMap()).once();
    EasyMock.expect(mockContext.getRestliProtocolVersion()).andReturn(protocolVersion).once();
    EasyMock.replay(mockContext);
    return mockContext;
  }

  private static ResourceMethodDescriptor getMockResourceMethodDescriptor()
  {
    ResourceMethodDescriptor mockDescriptor = EasyMock.createMock(ResourceMethodDescriptor.class);

    EasyMock.replay(mockDescriptor);
    return mockDescriptor;
  }
}
