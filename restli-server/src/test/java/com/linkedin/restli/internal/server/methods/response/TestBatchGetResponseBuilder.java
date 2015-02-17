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
import com.linkedin.data.transform.filter.request.MaskOperation;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.pegasus.generator.examples.Foo;
import com.linkedin.pegasus.generator.examples.Fruits;
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
import java.util.concurrent.ConcurrentHashMap;

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
    Foo record1 = new Foo().setStringField("record1").setFruitsField(Fruits.APPLE);
    Foo projectedRecord1 = new Foo().setStringField("record1");
    Foo record2 = new Foo().setStringField("record2").setIntField(7);
    Foo projectedRecord2 = new Foo().setStringField("record2");
    results.put(c1, record1);
    results.put(c2, record2);

    DataMap projectionDataMap = new DataMap();
    projectionDataMap.put("stringField", MaskOperation.POSITIVE_MASK_OP.getRepresentation());
    MaskTree maskTree = new MaskTree(projectionDataMap);

    Map<String, Foo> protocol1TransformedResults = new HashMap<String, Foo>();
    protocol1TransformedResults.put("a=a1&b=1", record1);
    protocol1TransformedResults.put("a=a2&b=2", record2);
    Map<String, Foo> protocol1TransformedResultsWithProjection = new HashMap<String, Foo>();
    protocol1TransformedResultsWithProjection.put("a=a1&b=1", projectedRecord1);
    protocol1TransformedResultsWithProjection.put("a=a2&b=2", projectedRecord2);

    Map<String, Foo> protocol2TransformedResults = new HashMap<String, Foo>();
    protocol2TransformedResults.put("(a:a1,b:1)", record1);
    protocol2TransformedResults.put("(a:a2,b:2)", record2);
    Map<String, Foo> protocol2TransformedResultsWithProjection = new HashMap<String, Foo>();
    protocol2TransformedResultsWithProjection.put("(a:a1,b:1)", projectedRecord1);
    protocol2TransformedResultsWithProjection.put("(a:a2,b:2)", projectedRecord2);

    Map<String, ErrorResponse> protocol1Errors = Collections.singletonMap("a=a3&b=3", new ErrorResponse().setStatus(404));
    Map<String, ErrorResponse> protocol2Errors = Collections.singletonMap("(a:a3,b:3)", new ErrorResponse().setStatus(404));

    Map<CompoundKey, HttpStatus> statuses = new HashMap<CompoundKey, HttpStatus>();
    statuses.put(c1, HttpStatus.S_200_OK);
    statuses.put(c2, HttpStatus.S_200_OK);
    Map<CompoundKey, RestLiServiceException> exceptions = new HashMap<CompoundKey, RestLiServiceException>();
    exceptions.put(c3, new RestLiServiceException(HttpStatus.S_404_NOT_FOUND));
    BatchResult<CompoundKey, Foo> batchResult = new BatchResult<CompoundKey, Foo>(results, statuses, exceptions);
    Map<Object, RestLiServiceException> exceptionsWithUntypedKey = new HashMap<Object, RestLiServiceException>(exceptions);

    ProtocolVersion protocolVersion1 = AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion();
    ProtocolVersion protocolVersion2 = AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion();

    ProjectionMode auto = ProjectionMode.AUTOMATIC;
    ProjectionMode manual = ProjectionMode.MANUAL;

    return new Object[][]
        {
            // automatic projection mode with null mask tree
            {results, protocolVersion1, protocol1TransformedResults, protocol1Errors, exceptionsWithUntypedKey, null, auto},
            {results, protocolVersion2, protocol2TransformedResults, protocol2Errors, exceptionsWithUntypedKey, null, auto},
            {batchResult, protocolVersion1, protocol1TransformedResults, protocol1Errors, exceptionsWithUntypedKey, null, auto},
            {batchResult, protocolVersion2, protocol2TransformedResults, protocol2Errors, exceptionsWithUntypedKey, null, auto},

            // manual projection mode with null mask tree
            {results, protocolVersion1, protocol1TransformedResults, protocol1Errors, exceptionsWithUntypedKey, null, manual},
            {results, protocolVersion2, protocol2TransformedResults, protocol2Errors, exceptionsWithUntypedKey, null, manual},
            {batchResult, protocolVersion1, protocol1TransformedResults, protocol1Errors, exceptionsWithUntypedKey, null, manual},
            {batchResult, protocolVersion2, protocol2TransformedResults, protocol2Errors, exceptionsWithUntypedKey, null, manual},

            // manual projection mode with non-null mask tree
            {results, protocolVersion1, protocol1TransformedResults, protocol1Errors, exceptionsWithUntypedKey, maskTree, manual},
            {results, protocolVersion2, protocol2TransformedResults, protocol2Errors, exceptionsWithUntypedKey, maskTree, manual},
            {batchResult, protocolVersion1, protocol1TransformedResults, protocol1Errors, exceptionsWithUntypedKey, maskTree, manual},
            {batchResult, protocolVersion2, protocol2TransformedResults, protocol2Errors, exceptionsWithUntypedKey, maskTree, manual},

            // automatic projection mode with non-null mask tree
            {results, protocolVersion1, protocol1TransformedResultsWithProjection, protocol1Errors, exceptionsWithUntypedKey, maskTree, auto},
            {results, protocolVersion2, protocol2TransformedResultsWithProjection, protocol2Errors, exceptionsWithUntypedKey, maskTree, auto},
            {batchResult, protocolVersion1, protocol1TransformedResultsWithProjection, protocol1Errors, exceptionsWithUntypedKey, maskTree, auto},
            {batchResult, protocolVersion2, protocol2TransformedResultsWithProjection, protocol2Errors, exceptionsWithUntypedKey, maskTree, auto},
        };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "testData")
  @SuppressWarnings("unchecked")
  public void testBuilder(Object results,
                          ProtocolVersion protocolVersion,
                          Map<String, Foo> expectedTransformedResult,
                          Map<String, ErrorResponse> expectedErrors,
                          Map<Object, RestLiServiceException> expectedExceptionsWithUntypedKey,
                          MaskTree maskTree,
                          ProjectionMode projectionMode)
  {
    ResourceContext mockContext = getMockResourceContext(protocolVersion,
                                                         expectedExceptionsWithUntypedKey,
                                                         maskTree,
                                                         projectionMode);
    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor();
    RoutingResult routingResult = new RoutingResult(mockContext, mockDescriptor);

    Map<String, String> headers = ResponseBuilderUtil.getHeaders();

    BatchGetResponseBuilder responseBuilder = new BatchGetResponseBuilder(new ErrorResponseBuilder());
    AugmentedRestLiResponseData responseData = responseBuilder.buildRestLiResponseData(null,
                                                                                       routingResult,
                                                                                       results,
                                                                                       headers);
    PartialRestResponse restResponse = responseBuilder.buildResponse(routingResult, responseData);

    EasyMock.verify(mockContext, mockDescriptor);
    ResponseBuilderUtil.validateHeaders(restResponse, headers);
    Assert.assertEquals(restResponse.getStatus(), HttpStatus.S_200_OK);
    BatchResponse<Foo> entity = (BatchResponse<Foo>)restResponse.getEntity();
    Assert.assertEquals(entity.getResults(), expectedTransformedResult);
    if (results instanceof BatchResult)
    {
      Map<String, Integer> expectedStatuses = new HashMap<String, Integer>();
      for (String key: entity.getResults().keySet())
      {
        expectedStatuses.put(key, HttpStatus.S_200_OK.getCode());
      }
      Assert.assertEquals(entity.getStatuses(), expectedStatuses);
    }
    else
    {
      // if the resource returns a Map we don't have a separate status map in the BatchResponse
      Assert.assertEquals(entity.getStatuses(), Collections.emptyMap());
    }
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
    final String expectedMessage = "Unexpected null encountered. Null key inside of a Map returned by the resource method: ";
    return new Object[][]
        {
            {results, expectedMessage},
            {batchResult, expectedMessage}
        };
  }

  @Test(dataProvider = "exceptionTestData")
  public void testBuilderExceptions(Object results, String expectedErrorMessage)
  {
    // Protocol version doesn't matter here
    ResourceContext mockContext = getMockResourceContext(null, Collections.<Object, RestLiServiceException>emptyMap(),
        null, null);
    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor();
    RoutingResult routingResult = new RoutingResult(mockContext, mockDescriptor);

    Map<String, String> headers = ResponseBuilderUtil.getHeaders();

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

 /* Note that we use java.util.concurrent.ConcurrentHashMap when possible. This is because rest.li checks
  * for the presence of nulls returned from maps which are returned from resource methods. The checking for nulls
  * is prone to a NullPointerException since contains(null) can throw an NPE from certain map implementations such as
  * java.util.concurrent.ConcurrentHashMap. We want to make sure our check for the presence of nulls is done in a
  * way that doesn't throw an NullPointerException.
  */
  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "unsupportedNullKeyMapData")
  public Object[][] unsupportedNullKeyMapData()
  {
    Map<CompoundKey, Foo> results = new ConcurrentHashMap<CompoundKey, Foo>();
    CompoundKey c1 = new CompoundKey().append("a", "a1").append("b", 1);
    Foo record1 = new Foo().setStringField("record1").setFruitsField(Fruits.APPLE);
    results.put(c1, record1);

    Map<CompoundKey, HttpStatus> statuses = new ConcurrentHashMap<CompoundKey, HttpStatus>();
    statuses.put(c1, HttpStatus.S_200_OK);

    final BatchResult<CompoundKey, Foo> batchResult =
        new BatchResult<CompoundKey, Foo>(results, statuses, new ConcurrentHashMap<CompoundKey, RestLiServiceException>());

    final Map<String, Foo> protocol1TransformedResults = new ConcurrentHashMap<String, Foo>();
    protocol1TransformedResults.put("a=a1&b=1", record1);

    final Map<String, Foo> protocol2TransformedResults = new ConcurrentHashMap<String, Foo>();
    protocol2TransformedResults.put("(a:a1,b:1)", record1);

    ProtocolVersion protocolVersion1 = AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion();
    ProtocolVersion protocolVersion2 = AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion();

    return new Object[][]
        {
            {results, protocolVersion1, protocol1TransformedResults},
            {results, protocolVersion2, protocol2TransformedResults},
            {batchResult, protocolVersion1, protocol1TransformedResults},
            {batchResult, protocolVersion2, protocol2TransformedResults}
        };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "unsupportedNullKeyMapData")
  @SuppressWarnings("unchecked")
  public void unsupportedNullKeyMapTest(Object results, ProtocolVersion protocolVersion, Map<String, Foo> expectedTransformedResult)
  {
    ResourceContext mockContext = getMockResourceContext(protocolVersion,
        Collections.<Object, RestLiServiceException>emptyMap(), null, null);
    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor();
    RoutingResult routingResult = new RoutingResult(mockContext, mockDescriptor);

    Map<String, String> headers = ResponseBuilderUtil.getHeaders();

    BatchGetResponseBuilder responseBuilder = new BatchGetResponseBuilder(new ErrorResponseBuilder());
    AugmentedRestLiResponseData responseData = responseBuilder.buildRestLiResponseData(null,
        routingResult,
        results,
        headers);
    PartialRestResponse restResponse = responseBuilder.buildResponse(routingResult, responseData);

    ResponseBuilderUtil.validateHeaders(restResponse, headers);
    Assert.assertEquals(restResponse.getStatus(), HttpStatus.S_200_OK);
    BatchResponse<Foo> entity = (BatchResponse<Foo>)restResponse.getEntity();
    Assert.assertEquals(entity.getResults(), expectedTransformedResult);
    if (results instanceof BatchResult)
    {
      Map<String, Integer> expectedStatuses = new HashMap<String, Integer>();
      for (String key: entity.getResults().keySet())
      {
        expectedStatuses.put(key, HttpStatus.S_200_OK.getCode());
      }
      Assert.assertEquals(entity.getStatuses(), expectedStatuses);
    }
    else
    {
      // if the resource returns a Map we don't have a separate status map in the BatchResponse
      Assert.assertEquals(entity.getStatuses(), Collections.emptyMap());
    }
  }

  private static ResourceContext getMockResourceContext(ProtocolVersion protocolVersion,
                                                        Map<Object, RestLiServiceException> exceptions,
                                                        MaskTree maskTree,
                                                        ProjectionMode projectionMode)
  {
    ServerResourceContext mockContext = EasyMock.createMock(ServerResourceContext.class);
    EasyMock.expect(mockContext.getBatchKeyErrors()).andReturn(exceptions).once();
    EasyMock.expect(mockContext.getProjectionMode()).andReturn(projectionMode).times(2);
    EasyMock.expect(mockContext.getProjectionMask()).andReturn(maskTree).times(2);
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
}
