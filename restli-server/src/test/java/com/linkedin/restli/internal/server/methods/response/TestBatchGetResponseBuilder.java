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
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.TestConstants;
import com.linkedin.restli.internal.server.AugmentedRestLiResponseData;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.BatchResult;
import com.linkedin.restli.server.ProjectionMode;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiServiceException;
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
public class TestBatchGetResponseBuilder
{
  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "testData")
  public Object[][] dataProvider()
  {
    Map<CompoundKey, Foo> results = new HashMap<CompoundKey, Foo>();
    CompoundKey c1 = new CompoundKey().append("a", "a1").append("b", 1);
    CompoundKey c2 = new CompoundKey().append("a", "a2").append("b", 2);
    CompoundKey c3 = new CompoundKey().append("a", "a3").append("b", 3);
    Foo record1 = new Foo().setStringField("record1");
    Foo record2 = new Foo().setStringField("record2");
    results.put(c1, record1);
    results.put(c2, record2);

    Map<String, Foo> protocol1TransformedResults = new HashMap<String, Foo>();
    protocol1TransformedResults.put("a=a1&b=1", record1);
    protocol1TransformedResults.put("a=a2&b=2", record2);

    Map<String, Foo> protocol2TransformedResults = new HashMap<String, Foo>();
    protocol2TransformedResults.put("(a:a1,b:1)", record1);
    protocol2TransformedResults.put("(a:a2,b:2)", record2);

    Map<String, ErrorResponse> protocol1Errors = Collections.singletonMap("a=a3&b=3", new ErrorResponse().setStatus(404));
    Map<String, ErrorResponse> protocol2Errors = Collections.singletonMap("(a:a3,b:3)", new ErrorResponse().setStatus(404));

    Map<CompoundKey, HttpStatus> statuses = new HashMap<CompoundKey, HttpStatus>();
    statuses.put(c1, HttpStatus.S_200_OK);
    statuses.put(c2, HttpStatus.S_200_OK);
    Map<CompoundKey, RestLiServiceException> exceptions = new HashMap<CompoundKey, RestLiServiceException>();
    exceptions.put(c3, new RestLiServiceException(HttpStatus.S_404_NOT_FOUND));
    BatchResult<CompoundKey, Foo> batchResult = new BatchResult<CompoundKey, Foo>(results, statuses, exceptions);
    Map<Object, RestLiServiceException> exceptionsWithUntypedKey = new HashMap<Object, RestLiServiceException>(exceptions);

    return new Object[][]
        {
            {results, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), protocol1TransformedResults, protocol1Errors, exceptionsWithUntypedKey},
            {results, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), protocol2TransformedResults, protocol2Errors, exceptionsWithUntypedKey},
            {batchResult, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), protocol1TransformedResults, protocol1Errors, exceptionsWithUntypedKey},
            {batchResult, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), protocol2TransformedResults, protocol2Errors, exceptionsWithUntypedKey},
        };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "testData")
  @SuppressWarnings("unchecked")
  public void testBuilder(Object results,
                          ProtocolVersion protocolVersion,
                          Map<String, Foo> expectedTransformedResult,
                          Map<String, ErrorResponse> expectedErrors,
                          Map<Object, RestLiServiceException> expectedExceptionsWithUntypedKey)
  {
    ResourceContext mockContext = getMockResourceContext(protocolVersion, expectedExceptionsWithUntypedKey);
    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor();
    RoutingResult routingResult = new RoutingResult(mockContext, mockDescriptor);

    Map<String, String> headers = getHeaders();

    BatchGetResponseBuilder responseBuilder = new BatchGetResponseBuilder(new ErrorResponseBuilder());
    AugmentedRestLiResponseData responseData = responseBuilder.buildRestLiResponseData(null,
                                                                                       routingResult,
                                                                                       results,
                                                                                       headers);
    PartialRestResponse restResponse = responseBuilder.buildResponse(routingResult, responseData);

    EasyMock.verify(mockContext, mockDescriptor);
    BatchResponse<Foo> entity = (BatchResponse<Foo>)restResponse.getEntity();
    Assert.assertEquals(entity.getResults(), expectedTransformedResult);
    Assert.assertEquals(entity.getErrors().size(), expectedErrors.size());
    for (Map.Entry<String, ErrorResponse> entry: entity.getErrors().entrySet())
    {
      String key = entry.getKey();
      ErrorResponse value = entry.getValue();
      Assert.assertEquals(value.getStatus(), expectedErrors.get(key).getStatus());
    }
  }

  @DataProvider(name = "exceptionTestData")
  public Object[][] exceptionDataProvider()
  {
    Map<Long, Foo> results = new HashMap<Long, Foo>();
    Foo f1 = new Foo().setStringField("f1");
    Foo f2 = new Foo().setStringField("f2");
    results.put(null, f1);
    results.put(1L, f2);

    BatchResult<Long, Foo> batchResult = new BatchResult<Long, Foo>(Collections.singletonMap(1L, f1),
                                                                    Collections.<Long, HttpStatus>singletonMap(null, HttpStatus.S_404_NOT_FOUND),
                                                                    null);
    return new Object[][]
        {
            {results, "Unexpected null encountered. Null key inside of the Map returned by the resource method: "},
            {batchResult, "Unexpected null encountered. Null key inside of the status map returned by the resource method: "}
        };
  }

  @Test(dataProvider = "exceptionTestData")
  public void testBuilderExceptions(Object results, String expectedErrorMessage)
  {
    // Protocol version doesn't matter here
    ResourceContext mockContext = getMockResourceContext(null, null);
    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor();
    RoutingResult routingResult = new RoutingResult(mockContext, mockDescriptor);

    Map<String, String> headers = getHeaders();

    BatchGetResponseBuilder responseBuilder = new BatchGetResponseBuilder(new ErrorResponseBuilder());
    try
    {
      responseBuilder.buildRestLiResponseData(null, routingResult, results, headers);
      Assert.fail("buildRestLiResponseData should have failed because of null elements!");
    }
    catch (RestLiServiceException e)
    {
      Assert.assertTrue(e.getMessage().contains(expectedErrorMessage));
    }
  }

  private static ResourceContext getMockResourceContext(ProtocolVersion protocolVersion,
                                                        Map<Object, RestLiServiceException> exceptions)
  {
    ServerResourceContext mockContext = EasyMock.createMock(ServerResourceContext.class);
    EasyMock.expect(mockContext.getBatchKeyErrors()).andReturn(exceptions).once();
    EasyMock.expect(mockContext.getProjectionMode()).andReturn(ProjectionMode.MANUAL).times(2);
    EasyMock.expect(mockContext.getProjectionMask()).andReturn(null).times(2);
    EasyMock.expect(mockContext.getRestliProtocolVersion()).andReturn(protocolVersion).once();
    EasyMock.replay(mockContext);
    return mockContext;
  }

  private static ResourceMethodDescriptor getMockResourceMethodDescriptor()
  {
    ResourceMethodDescriptor mockDescriptor = EasyMock.createMock(ResourceMethodDescriptor.class);
    EasyMock.expect(mockDescriptor.getMethodType()).andReturn(ResourceMethod.BATCH_GET).once();
    EasyMock.replay(mockDescriptor);
    return mockDescriptor;
  }

  private static Map<String, String> getHeaders()
  {
    Map<String, String> headers = new HashMap<String, String>();
    headers.put("h1", "v1");
    headers.put("h2", "v2");
    return headers;
  }
}
