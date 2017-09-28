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


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.StringDataSchema;
import com.linkedin.data.template.InvalidAlternativeKeyException;
import com.linkedin.data.template.KeyCoercer;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.transform.filter.request.MaskOperation;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.pegasus.generator.examples.Foo;
import com.linkedin.pegasus.generator.examples.Fruits;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.TestConstants;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.server.AlternativeKey;
import com.linkedin.restli.server.BatchResult;
import com.linkedin.restli.server.ProjectionMode;
import com.linkedin.restli.server.RestLiResponseData;
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
    Map<CompoundKey, Foo> results = new HashMap<>();
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

    Map<String, Foo> protocol1TransformedResults = new HashMap<>();
    protocol1TransformedResults.put("a=a1&b=1", record1);
    protocol1TransformedResults.put("a=a2&b=2", record2);
    Map<String, Foo> protocol1TransformedResultsWithProjection = new HashMap<>();
    protocol1TransformedResultsWithProjection.put("a=a1&b=1", projectedRecord1);
    protocol1TransformedResultsWithProjection.put("a=a2&b=2", projectedRecord2);

    Map<String, Foo> protocol2TransformedResults = new HashMap<>();
    protocol2TransformedResults.put("(a:a1,b:1)", record1);
    protocol2TransformedResults.put("(a:a2,b:2)", record2);
    Map<String, Foo> protocol2TransformedResultsWithProjection = new HashMap<>();
    protocol2TransformedResultsWithProjection.put("(a:a1,b:1)", projectedRecord1);
    protocol2TransformedResultsWithProjection.put("(a:a2,b:2)", projectedRecord2);

    Map<String, ErrorResponse> protocol1Errors = Collections.singletonMap("a=a3&b=3", new ErrorResponse().setStatus(404));
    Map<String, ErrorResponse> protocol2Errors = Collections.singletonMap("(a:a3,b:3)", new ErrorResponse().setStatus(404));

    Map<CompoundKey, HttpStatus> statuses = new HashMap<>();
    statuses.put(c1, HttpStatus.S_200_OK);
    statuses.put(c2, HttpStatus.S_200_OK);
    Map<CompoundKey, RestLiServiceException> exceptions = new HashMap<>();
    exceptions.put(c3, new RestLiServiceException(HttpStatus.S_404_NOT_FOUND));
    BatchResult<CompoundKey, Foo> batchResult = new BatchResult<>(results, statuses, exceptions);
    Map<Object, RestLiServiceException> exceptionsWithUntypedKey = new HashMap<>(exceptions);

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
    ServerResourceContext mockContext = getMockResourceContext(protocolVersion,
                                                         expectedExceptionsWithUntypedKey,
                                                         null,
                                                         maskTree,
                                                         projectionMode);
    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor(null);
    RoutingResult routingResult = new RoutingResult(mockContext, mockDescriptor);

    Map<String, String> headers = ResponseBuilderUtil.getHeaders();

    BatchGetResponseBuilder responseBuilder = new BatchGetResponseBuilder(new ErrorResponseBuilder());
    RestLiResponseData<BatchGetResponseEnvelope> responseData = responseBuilder.buildRestLiResponseData(null,
                                                                                routingResult,
                                                                                results,
                                                                                headers,
                                                                                Collections.emptyList());
    PartialRestResponse restResponse = responseBuilder.buildResponse(routingResult, responseData);

    EasyMock.verify(mockContext, mockDescriptor);
    ResponseBuilderUtil.validateHeaders(restResponse, headers);
    Assert.assertEquals(restResponse.getStatus(), HttpStatus.S_200_OK);
    BatchResponse<Foo> entity = (BatchResponse<Foo>)restResponse.getEntity();
    Assert.assertEquals(entity.getResults(), expectedTransformedResult);
    if (results instanceof BatchResult)
    {
      Map<String, Integer> expectedStatuses = new HashMap<>();
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

  @Test
  public void testContextErrors()
  {
    BatchGetResponseBuilder builder = new BatchGetResponseBuilder(new ErrorResponseBuilder());
    ServerResourceContext context = EasyMock.createMock(ServerResourceContext.class);
    Map<Object, RestLiServiceException> errors = new HashMap<>();
    RestLiServiceException exception = new RestLiServiceException(HttpStatus.S_402_PAYMENT_REQUIRED);
    errors.put("foo", exception);
    EasyMock.expect(context.hasParameter("altkey")).andReturn(false);
    EasyMock.expect(context.getBatchKeyErrors()).andReturn(errors);
    EasyMock.replay(context);
    RoutingResult routingResult = new RoutingResult(context, null);
    RestLiResponseData<BatchGetResponseEnvelope> responseData = builder.buildRestLiResponseData(null,
                                                                      routingResult,
                                                                      new BatchResult<>(Collections.emptyMap(), Collections.emptyMap()),
                                                                      Collections.emptyMap(), Collections.emptyList());
    Assert.assertEquals(responseData.getResponseEnvelope().getBatchResponseMap().get("foo").getException(),
            exception);
    Assert.assertEquals(responseData.getResponseEnvelope().getBatchResponseMap().size(), 1);
  }

  @Test
  public void testAlternativeKeyBuilder()
  {
    Map<CompoundKey, Foo> rawResults = new HashMap<>();
    CompoundKey c1 = new CompoundKey().append("a", "a1").append("b", 1);
    CompoundKey c2 = new CompoundKey().append("a", "a2").append("b", 2);
    Foo record1 = new Foo().setStringField("record1").setFruitsField(Fruits.APPLE);
    Foo record2 = new Foo().setStringField("record2").setIntField(7);
    rawResults.put(c1, record1);
    rawResults.put(c2, record2);

    Map<String, AlternativeKey<?, ?>> alternativeKeyMap = new HashMap<>();
    alternativeKeyMap.put("alt", new AlternativeKey<>(new TestKeyCoercer(), String.class, new StringDataSchema()));

    Map<String, String> headers = ResponseBuilderUtil.getHeaders();

    ServerResourceContext mockContext = getMockResourceContext(AllProtocolVersions.LATEST_PROTOCOL_VERSION,
            Collections.emptyMap(),
            "alt", null, null);
    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor(alternativeKeyMap);
    RoutingResult routingResult = new RoutingResult(mockContext, mockDescriptor);

    BatchGetResponseBuilder batchGetResponseBuilder = new BatchGetResponseBuilder(null);
    RestLiResponseData<BatchGetResponseEnvelope> responseData = batchGetResponseBuilder.buildRestLiResponseData(null,
                                                                                      routingResult,
                                                                                      rawResults,
                                                                                      headers,
                                                                                      Collections.emptyList());
    PartialRestResponse restResponse = batchGetResponseBuilder.buildResponse(routingResult, responseData);

    EasyMock.verify(mockContext, mockDescriptor);
    ResponseBuilderUtil.validateHeaders(restResponse, headers);
    Assert.assertEquals(restResponse.getStatus(), HttpStatus.S_200_OK);
    @SuppressWarnings("unchecked")
    Map<String, Foo> results = ((BatchResponse<Foo>)restResponse.getEntity()).getResults();
    Assert.assertEquals(results.size(), 2);
    Assert.assertTrue(results.containsKey("aa1xb1"));
    Assert.assertTrue(results.containsKey("aa2xb2"));
  }

  @DataProvider(name = "exceptionTestData")
  public Object[][] exceptionDataProvider()
  {
    Map<Long, Foo> results = new HashMap<>();
    Foo f1 = new Foo().setStringField("f1");
    Foo f2 = new Foo().setStringField("f2");
    results.put(null, f1);
    results.put(1L, f2);

    BatchResult<Long, Foo> batchResult = new BatchResult<>(Collections.singletonMap(1L, f1),
                                                                    Collections.singletonMap(null, HttpStatus.S_404_NOT_FOUND),
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
    ServerResourceContext mockContext = getMockResourceContext(null, Collections.emptyMap(),
        null, null, null);
    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor(null);
    RoutingResult routingResult = new RoutingResult(mockContext, mockDescriptor);

    Map<String, String> headers = ResponseBuilderUtil.getHeaders();

    BatchGetResponseBuilder responseBuilder = new BatchGetResponseBuilder(new ErrorResponseBuilder());
    try
    {
      responseBuilder.buildRestLiResponseData(null, routingResult, results, headers,
                                              Collections.emptyList());
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
    Map<CompoundKey, Foo> results = new ConcurrentHashMap<>();
    CompoundKey c1 = new CompoundKey().append("a", "a1").append("b", 1);
    Foo record1 = new Foo().setStringField("record1").setFruitsField(Fruits.APPLE);
    results.put(c1, record1);

    Map<CompoundKey, HttpStatus> statuses = new ConcurrentHashMap<>();
    statuses.put(c1, HttpStatus.S_200_OK);

    final BatchResult<CompoundKey, Foo> batchResult =
        new BatchResult<>(results, statuses, new ConcurrentHashMap<>());

    final Map<String, Foo> protocol1TransformedResults = new ConcurrentHashMap<>();
    protocol1TransformedResults.put("a=a1&b=1", record1);

    final Map<String, Foo> protocol2TransformedResults = new ConcurrentHashMap<>();
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
    ServerResourceContext mockContext = getMockResourceContext(protocolVersion,
        Collections.emptyMap(), null, null, null);
    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor(null);
    RoutingResult routingResult = new RoutingResult(mockContext, mockDescriptor);

    Map<String, String> headers = ResponseBuilderUtil.getHeaders();

    BatchGetResponseBuilder responseBuilder = new BatchGetResponseBuilder(new ErrorResponseBuilder());
    RestLiResponseData<BatchGetResponseEnvelope> responseData = responseBuilder.buildRestLiResponseData(null,
                                                                              routingResult,
                                                                              results,
                                                                              headers,
                                                                              Collections.emptyList());
    PartialRestResponse restResponse = responseBuilder.buildResponse(routingResult, responseData);

    ResponseBuilderUtil.validateHeaders(restResponse, headers);
    Assert.assertEquals(restResponse.getStatus(), HttpStatus.S_200_OK);
    BatchResponse<Foo> entity = (BatchResponse<Foo>)restResponse.getEntity();
    Assert.assertEquals(entity.getResults(), expectedTransformedResult);
    if (results instanceof BatchResult)
    {
      Map<String, Integer> expectedStatuses = new HashMap<>();
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

  @Test
  public void testProjectionInBuildRestliResponseData()
  {
    MaskTree maskTree = new MaskTree();
    maskTree.addOperation(new PathSpec("fruitsField"), MaskOperation.POSITIVE_MASK_OP);

    ServerResourceContext mockContext = EasyMock.createMock(ServerResourceContext.class);
    EasyMock.expect(mockContext.hasParameter(RestConstants.ALT_KEY_PARAM)).andReturn(false);
    EasyMock.expect(mockContext.getProjectionMode()).andReturn(ProjectionMode.AUTOMATIC);
    EasyMock.expect(mockContext.getProjectionMask()).andReturn(maskTree);
    EasyMock.expect(mockContext.getBatchKeyErrors()).andReturn(Collections.emptyMap()).once();
    EasyMock.replay(mockContext);

    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor(null);
    RoutingResult routingResult = new RoutingResult(mockContext, mockDescriptor);

    Map<Integer, Foo> results = new HashMap<>();
    Foo value = new Foo().setStringField("value").setFruitsField(Fruits.APPLE);
    results.put(1, value);

    BatchGetResponseBuilder responseBuilder = new BatchGetResponseBuilder(new ErrorResponseBuilder());
    RestLiResponseData<BatchGetResponseEnvelope> responseData = responseBuilder.buildRestLiResponseData(null,
                                                                              routingResult,
                                                                              results,
                                                                              Collections.emptyMap(),
                                                                              Collections.emptyList());
    RecordTemplate record = responseData.getResponseEnvelope().getBatchResponseMap().get(1).getRecord();
    Assert.assertEquals(record.data().size(), 1);
    Assert.assertEquals(record.data().get("fruitsField"), Fruits.APPLE.toString());

    EasyMock.verify(mockContext);
  }

  private static ServerResourceContext getMockResourceContext(ProtocolVersion protocolVersion,
                                                        Map<Object, RestLiServiceException> exceptions,
                                                        String altKeyName,
                                                        MaskTree maskTree,
                                                        ProjectionMode projectionMode)
  {
    ServerResourceContext mockContext = EasyMock.createMock(ServerResourceContext.class);
    EasyMock.expect(mockContext.getBatchKeyErrors()).andReturn(exceptions).once();
    EasyMock.expect(mockContext.getProjectionMode()).andReturn(projectionMode).times(2);
    EasyMock.expect(mockContext.getProjectionMask()).andReturn(maskTree).times(2);
    EasyMock.expect(mockContext.getRestliProtocolVersion()).andReturn(protocolVersion).once();
    EasyMock.expect(mockContext.hasParameter(RestConstants.ALT_KEY_PARAM)).andReturn(altKeyName != null).anyTimes();
    if (altKeyName != null)
    {
      EasyMock.expect(mockContext.getParameter(RestConstants.ALT_KEY_PARAM)).andReturn(altKeyName).atLeastOnce();
    }
    EasyMock.replay(mockContext);
    return mockContext;
  }

  private static ResourceMethodDescriptor getMockResourceMethodDescriptor(Map<String, AlternativeKey<?, ?>> alternativeKeyMap)
  {
    ResourceMethodDescriptor mockDescriptor = EasyMock.createMock(ResourceMethodDescriptor.class);
    if (alternativeKeyMap != null)
    {
      EasyMock.expect(mockDescriptor.getResourceModel()).andReturn(getMockResourceModel(alternativeKeyMap)).atLeastOnce();
    }
    EasyMock.replay(mockDescriptor);
    return mockDescriptor;
  }

  private static ResourceModel getMockResourceModel(Map<String, AlternativeKey<?, ?>> alternativeKeyMap)
  {
    ResourceModel mockResourceModel = EasyMock.createMock(ResourceModel.class);
    EasyMock.expect(mockResourceModel.getAlternativeKeys()).andReturn(alternativeKeyMap).anyTimes();
    EasyMock.replay(mockResourceModel);
    return mockResourceModel;
  }

  private class TestKeyCoercer implements KeyCoercer<String, CompoundKey>
  {
    @Override
    public CompoundKey coerceToKey(String object) throws InvalidAlternativeKeyException
    {
      CompoundKey compoundKey = new CompoundKey();
      compoundKey.append("a", object.substring(1, 3));
      compoundKey.append("b", Integer.parseInt(object.substring(4, 5)));
      return compoundKey;
    }

    @Override
    public String coerceFromKey(CompoundKey object)
    {
      return "a" + object.getPart("a") + "xb" + object.getPart("b");
    }
  }
}
